# 10-D — Concurrency Patterns

---

## Question 1: "StateFlow vs SharedFlow vs LiveData — when do you use each?"

```kotlin
class ConcurrencyDemoViewModel : ViewModel() {

    // ── StateFlow ────────────────────────────────────────────────────────────
    // Use for: UI state that always has a current value
    // - New collectors immediately get the current value (replay = 1)
    // - Best for: loading state, list data, user data
    private val _uiState = MutableStateFlow<UiState<List<Article>>>(UiState.Idle)
    val uiState: StateFlow<UiState<List<Article>>> = _uiState.asStateFlow()

    // ── SharedFlow ───────────────────────────────────────────────────────────
    // Use for: one-shot events that must NOT be re-delivered on re-subscription
    // - Default replay = 0: new collectors miss past events
    // - Best for: navigation, snackbar, dialog triggers
    private val _events = MutableSharedFlow<UiEvent>()
    val events: SharedFlow<UiEvent> = _events.asSharedFlow()

    fun navigateToDetail(id: String) {
        viewModelScope.launch {
            _events.emit(UiEvent.NavigateTo("detail/$id"))
        }
    }

    // ── LiveData ─────────────────────────────────────────────────────────────
    // Use for: when you need lifecycle-aware observation in legacy code
    // - Has built-in lifecycle handling without repeatOnLifecycle
    // - Cannot be used in pure Kotlin (requires Android dependency)
    // - Prefer StateFlow in new code
    val legacyData: LiveData<String> = MutableLiveData("initial")
}

sealed class UiEvent {
    data class NavigateTo(val route: String) : UiEvent()
    data class ShowSnackbar(val message: String) : UiEvent()
    data object ShowDialog : UiEvent()
}

// Collecting in Fragment — the critical pattern
class DemoFragment : Fragment() {
    private val vm: ConcurrencyDemoViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        // repeatOnLifecycle: stops collection when STOPPED, resumes when STARTED
        // Without it: collects in background, wastes resources or crashes
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {

                // Parallel collection with launch inside repeatOnLifecycle
                launch { vm.uiState.collect { render(it) } }
                launch { vm.events.collect { handle(it) } }
            }
        }
    }
}
```

---

## Question 2: "Explain structured concurrency. Why does it matter?"

```kotlin
// WITHOUT structured concurrency — fire and forget, no way to cancel
class BadViewModel : ViewModel() {
    fun doWork() {
        GlobalScope.launch {          // ← leaked coroutine — never cancelled
            val result = api.fetch()  // if ViewModel is destroyed, this still runs
            updateUi(result)          // crash: tries to update dead UI
        }
    }
}

// WITH structured concurrency — scope defines lifetime
class GoodViewModel : ViewModel() {
    fun doWork() {
        viewModelScope.launch {       // ← cancelled when ViewModel is cleared
            val result = api.fetch()
            _state.value = result     // safe: ViewModel is alive if we're here
        }
    }
}

// Parallel work with structured concurrency
fun loadDashboard() {
    viewModelScope.launch {
        // Both run concurrently — if either fails, both are cancelled
        val (articles, user) = awaitAll(
            async { repo.getArticles() },
            async { repo.getUser() }
        )
        // Arrives here only if BOTH succeed
        _state.value = DashboardState(articles, user)
    }
}

// Error handling with supervisorScope
// supervisorScope: one child failing does NOT cancel siblings
fun loadIndependentData() {
    viewModelScope.launch {
        supervisorScope {
            val articlesDeferred = async { repo.getArticles() }
            val adsDeferred      = async { repo.getAds() }

            val articles = try { articlesDeferred.await() } catch (e: Exception) { emptyList() }
            val ads      = try { adsDeferred.await()      } catch (e: Exception) { emptyList() }

            _state.value = FeedState(articles, ads)
        }
    }
}
```

**Follow-up: "What's the difference between coroutineScope and supervisorScope?"**
> `coroutineScope`: if one child fails, ALL siblings are cancelled. Use when all results are required.
> `supervisorScope`: children fail independently. Use when partial results are acceptable.

---

## Question 3: "How do you handle race conditions in Android?"

```kotlin
// Scenario: user rapidly taps Like button — avoid duplicate API calls

class ArticleViewModel @Inject constructor(
    private val repo: NewsRepository
) : ViewModel() {

    // Mutex prevents concurrent execution of the same critical section
    private val likeMutex = Mutex()

    fun likeArticle(articleId: String) {
        viewModelScope.launch {
            likeMutex.withLock {
                // Only one coroutine at a time can be inside here
                val current = repo.getArticle(articleId)
                repo.setLiked(articleId, !current.isLiked)
            }
        }
    }

    // Alternative: use a Channel to serialize events
    private val likeChannel = Channel<String>(capacity = Channel.CONFLATED)

    init {
        viewModelScope.launch {
            likeChannel.receiveAsFlow()
                .distinctUntilChanged()
                .collect { articleId ->
                    repo.toggleLike(articleId)
                }
        }
    }

    fun likeWithChannel(articleId: String) {
        likeChannel.trySend(articleId)  // CONFLATED: drops old value if channel is full
    }
}
```

---

## Question 4: "How do you safely share mutable state between coroutines?"

```kotlin
// WRONG — reading and writing from multiple coroutines without protection
var counter = 0
repeat(1000) {
    viewModelScope.launch(Dispatchers.Default) {
        counter++ // race condition — final value is unpredictable
    }
}

// CORRECT Option 1: confine state to a single thread
val singleThread = Executors.newSingleThreadExecutor().asCoroutineDispatcher()
var safeCounter = 0
repeat(1000) {
    viewModelScope.launch(singleThread) {
        safeCounter++ // only one thread touches it
    }
}

// CORRECT Option 2: use AtomicInteger for simple counters
val atomicCounter = AtomicInteger(0)
repeat(1000) {
    viewModelScope.launch(Dispatchers.Default) {
        atomicCounter.incrementAndGet() // thread-safe
    }
}

// CORRECT Option 3: use StateFlow (already thread-safe with update{})
private val _count = MutableStateFlow(0)
repeat(1000) {
    viewModelScope.launch(Dispatchers.Default) {
        _count.update { it + 1 } // thread-safe atomic update
    }
}
```
