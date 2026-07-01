# 10-C — Android System Design

> System design questions test your ability to think at scale.
> Interviewers at Staff+ level want to see trade-offs, not just solutions.

---

## Question 1: "Design an offline-first news feed."

**What they test:** Do you know how to handle network, cache, and sync together?

### Architecture

```
UI → ViewModel → UseCase → Repository
                               ├── Remote: Retrofit (NewsAPI)
                               └── Local:  Room (cache)
                                      ↑
                               WorkManager (background sync)
```

### The Repository — single source of truth

```kotlin
class NewsRepositoryImpl @Inject constructor(
    private val api: NewsApiService,
    private val dao: ArticleDao,
    private val networkMonitor: NetworkMonitor
) : NewsRepository {

    // Always read from Room — never directly from network
    // Room emits new data whenever WorkManager syncs
    override fun getTopHeadlines(): Flow<List<Article>> =
        dao.getArticles()                          // Flow from Room
            .onStart { refreshIfStale() }          // trigger refresh on first collect
            .map { entities -> entities.map { it.toArticle() } }

    private suspend fun refreshIfStale() {
        if (!networkMonitor.isOnline()) return    // offline: serve cache silently
        try {
            val remote = api.getTopHeadlines()
            dao.upsertAll(remote.articles.map { it.toEntity() })
        } catch (e: IOException) {
            // Network error: cache still served, user not blocked
        }
    }
}
```

### Background sync with WorkManager

```kotlin
class NewsSyncWorker(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {
    override suspend fun doWork(): Result {
        return try {
            val articles = api.getTopHeadlines().articles
            dao.upsertAll(articles.map { it.toEntity() })
            Result.success()
        } catch (e: Exception) {
            if (runAttemptCount < 3) Result.retry() else Result.failure()
        }
    }
}

// Schedule: sync every 15 minutes when on wifi, battery not low
val constraints = Constraints.Builder()
    .setRequiredNetworkType(NetworkType.UNMETERED)
    .setRequiresBatteryNotLow(true)
    .build()

val request = PeriodicWorkRequestBuilder<NewsSyncWorker>(15, TimeUnit.MINUTES)
    .setConstraints(constraints)
    .build()

WorkManager.getInstance(context).enqueueUniquePeriodicWork(
    "news_sync",
    ExistingPeriodicWorkPolicy.KEEP,
    request
)
```

### Follow-up: "What if the user edits data offline and there's a conflict when syncing?"
> Use a **last-write-wins** strategy with timestamps, or a **merge strategy** based on field-level diffs.
> Store a `locallyModifiedAt` field in Room. On sync, compare with server's `updatedAt`.
> Whichever is newer wins. For conflict-prone data (bookmarks), use a CRDT set (add-wins).

---

## Question 2: "Design a real-time chat screen (client-side architecture)."

**What they test:** WebSocket lifecycle, message ordering, optimistic UI.

```kotlin
// Message state — covers all cases
data class ChatMessage(
    val id: String,
    val text: String,
    val senderId: String,
    val timestamp: Long,
    val status: MessageStatus
)

enum class MessageStatus { SENDING, SENT, DELIVERED, READ, FAILED }

class ChatViewModel @Inject constructor(
    private val chatRepo: ChatRepository,
    private val webSocket: ChatWebSocket
) : ViewModel() {

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    init {
        // Collect incoming messages from WebSocket
        viewModelScope.launch {
            webSocket.incomingMessages.collect { incoming ->
                _messages.update { current ->
                    (current + incoming).sortedBy { it.timestamp }
                }
            }
        }
    }

    // Optimistic UI: show message immediately, update status after ACK
    fun sendMessage(text: String) {
        val tempId = UUID.randomUUID().toString()
        val optimistic = ChatMessage(
            id = tempId,
            text = text,
            senderId = "me",
            timestamp = System.currentTimeMillis(),
            status = MessageStatus.SENDING
        )

        // Show immediately — don't wait for server
        _messages.update { it + optimistic }

        viewModelScope.launch {
            val result = chatRepo.send(text)
            _messages.update { messages ->
                messages.map { msg ->
                    if (msg.id == tempId) {
                        when (result) {
                            is Result.Success -> msg.copy(id = result.data.id, status = MessageStatus.SENT)
                            is Result.Error   -> msg.copy(status = MessageStatus.FAILED)
                            else -> msg
                        }
                    } else msg
                }
            }
        }
    }

    override fun onCleared() {
        webSocket.disconnect()  // critical: close WebSocket when screen is gone
    }
}
```

### Follow-up: "How do you handle message ordering with network delays?"
> Use **vector clocks** or **server-assigned sequence numbers**.
> Never trust client timestamps for ordering — clocks drift.
> Sort by `serverSequence` if available, fall back to `timestamp`.

---

## Question 3: "Design a search with autocomplete (debounce, cancellation)."

```kotlin
@HiltViewModel
class SearchViewModel @Inject constructor(
    private val repo: NewsRepository
) : ViewModel() {

    private val _query = MutableStateFlow("")
    private val _results = MutableStateFlow<UiState<List<Article>>>(UiState.Idle)
    val results: StateFlow<UiState<List<Article>>> = _results.asStateFlow()

    init {
        viewModelScope.launch {
            _query
                .debounce(300)             // wait 300ms after user stops typing
                .distinctUntilChanged()    // ignore duplicate queries
                .filter { it.length >= 2 } // minimum 2 characters
                .flatMapLatest { query ->  // cancel previous search if new query arrives
                    flow {
                        emit(UiState.Loading)
                        emit(
                            when (val r = repo.search(query)) {
                                is Result.Success -> UiState.Success(r.data)
                                is Result.Error   -> UiState.Error(r.message)
                                else -> UiState.Loading
                            }
                        )
                    }
                }
                .collect { _results.value = it }
        }
    }

    fun onQueryChanged(query: String) { _query.value = query }
}
```

**Key operators to explain:**
- `debounce(300)` — prevents API call on every keystroke
- `distinctUntilChanged()` — skips if query didn't change (e.g. user types "a" then deletes then types "a")
- `flatMapLatest` — **cancels the previous coroutine** when a new query arrives. This is the critical one most candidates miss.

---

## Question 4: "Design the architecture for a payments SDK used by 50 apps."

**What they test:** API surface design, versioning, security, testability.

```kotlin
// Public API surface — this is what partner apps see
// Keep it minimal — every public method is a contract you must maintain

interface PaymentSDK {
    fun initialize(context: Context, config: PaymentConfig)
    fun startPayment(activity: Activity, request: PaymentRequest, callback: PaymentCallback)
    fun getVersion(): String
}

// Config — use Builder so new fields don't break existing callers
data class PaymentConfig(
    val merchantId: String,
    val environment: Environment,
    val enableLogging: Boolean = false,    // optional, safe default
    val timeout: Duration = 30.seconds     // optional, safe default
) {
    enum class Environment { SANDBOX, PRODUCTION }

    class Builder(private val merchantId: String, private val env: Environment) {
        private var logging = false
        private var timeout = 30.seconds

        fun enableLogging() = apply { logging = true }
        fun timeout(d: Duration) = apply { timeout = d }
        fun build() = PaymentConfig(merchantId, env, logging, timeout)
    }
}

// Callback — sealed class prevents partner from adding unknown outcomes
sealed class PaymentResult {
    data class Success(val transactionId: String, val amount: Long) : PaymentResult()
    data class Failed(val errorCode: String, val message: String) : PaymentResult()
    data object Cancelled : PaymentResult()
}

fun interface PaymentCallback {
    fun onResult(result: PaymentResult)
}
```

**Follow-ups:**
- "How do you handle breaking changes?" → **Semantic versioning. Never remove/rename public API. Add new methods, deprecate old ones with @Deprecated(ReplaceWith).**
- "How do you test an SDK?" → **Provide a Sandbox environment + mock implementations of PaymentSDK for unit testing. Ship a test artifact: `com.yourcompany:payment-sdk-mock`.**
- "How do you secure it?" → **Certificate pinning, obfuscate with R8, no secrets in code, use Android Keystore for sensitive keys.**

---

## Question 5: "Design a feature flag system for Android."

```kotlin
// Feature flags control what's visible without a new release

interface FeatureFlags {
    suspend fun isEnabled(flag: Flag): Boolean
}

enum class Flag(val key: String, val defaultValue: Boolean) {
    NEW_HOME_UI("new_home_ui", false),
    AI_CHAT("ai_chat", false),
    DARK_MODE_V2("dark_mode_v2", true)
}

class FeatureFlagsImpl @Inject constructor(
    private val remoteConfig: FirebaseRemoteConfig,
    private val localOverrides: SharedPreferences  // for QA testing
) : FeatureFlags {

    override suspend fun isEnabled(flag: Flag): Boolean {
        // QA override takes priority (only in debug builds)
        if (BuildConfig.DEBUG && localOverrides.contains(flag.key)) {
            return localOverrides.getBoolean(flag.key, flag.defaultValue)
        }
        // Remote config (fetched on app start, cached 12 hours)
        return remoteConfig.getBoolean(flag.key)
    }
}

// Usage in ViewModel
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val flags: FeatureFlags
) : ViewModel() {

    val showNewHomeUi: StateFlow<Boolean> = flow {
        emit(flags.isEnabled(Flag.NEW_HOME_UI))
    }.stateIn(viewModelScope, SharingStarted.Eagerly, false)
}
```
