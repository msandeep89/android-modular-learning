# 10-B — Design Patterns in Android

---

## Question 1: "Implement a thread-safe Singleton in Kotlin."

```kotlin
// Option 1: Kotlin object — simplest, lazy by default, thread-safe
object DatabaseManager {
    fun query(sql: String): List<String> = listOf()
}

// Option 2: Double-checked locking (when you need constructor params)
class RetrofitClient private constructor(private val baseUrl: String) {

    companion object {
        @Volatile private var instance: RetrofitClient? = null

        fun getInstance(baseUrl: String): RetrofitClient =
            instance ?: synchronized(this) {
                instance ?: RetrofitClient(baseUrl).also { instance = it }
            }
    }
}

// Option 3: Holder pattern (lazy without synchronized overhead)
class Analytics private constructor() {
    companion object {
        val instance: Analytics by lazy { Analytics() }
    }
}
```

**Follow-up:** "Why @Volatile?" → Without it, the CPU can cache `instance` in a register.
Thread A writes the new value but Thread B still reads null from its cache. @Volatile
forces every read/write to go to main memory.

---

## Question 2: "Where have you used the Builder pattern in Android?"

```kotlin
// Android's AlertDialog is a classic Builder
AlertDialog.Builder(context)
    .setTitle("Delete article?")
    .setMessage("This cannot be undone.")
    .setPositiveButton("Delete") { _, _ -> viewModel.delete(articleId) }
    .setNegativeButton("Cancel", null)
    .show()

// Your own Builder — useful for network request config, notification, etc.
data class NotificationConfig(
    val title: String,
    val body: String,
    val channelId: String,
    val iconRes: Int,
    val autoCancel: Boolean,
    val deepLinkUri: String?
) {
    class Builder(private val title: String, private val body: String) {
        private var channelId: String = "default"
        private var iconRes: Int = R.drawable.ic_notification
        private var autoCancel: Boolean = true
        private var deepLinkUri: String? = null

        fun channel(id: String) = apply { channelId = id }
        fun icon(res: Int)      = apply { iconRes = res }
        fun noAutoCancel()      = apply { autoCancel = false }
        fun deepLink(uri: String) = apply { deepLinkUri = uri }

        fun build() = NotificationConfig(title, body, channelId, iconRes, autoCancel, deepLinkUri)
    }
}

// Usage reads like natural language
val config = NotificationConfig.Builder("Breaking News", "Earthquake in Turkey")
    .channel("news_alerts")
    .deepLink("newsreader://article/123")
    .build()
```

---

## Question 3: "Explain the Observer pattern. How does it relate to LiveData and StateFlow?"

```kotlin
// The pattern: Subject notifies Observers when state changes
// In Android: ViewModel (Subject) → Fragment (Observer)

// LiveData: lifecycle-aware Observer
class LiveDataViewModel : ViewModel() {
    private val _articles = MutableLiveData<List<Article>>()
    val articles: LiveData<List<Article>> = _articles

    fun load() { _articles.value = listOf() }
}

// Fragment observes only when STARTED — no leaks, no crashes
viewModel.articles.observe(viewLifecycleOwner) { articles ->
    adapter.submitList(articles)
}

// StateFlow: always has a value, collects like a Kotlin Flow
class FlowViewModel : ViewModel() {
    private val _articles = MutableStateFlow<List<Article>>(emptyList())
    val articles: StateFlow<List<Article>> = _articles.asStateFlow()
}

// Must use repeatOnLifecycle to match LiveData's lifecycle-awareness
viewLifecycleOwner.lifecycleScope.launch {
    repeatOnLifecycle(Lifecycle.State.STARTED) {
        viewModel.articles.collect { adapter.submitList(it) }
    }
}
```

**When to use which:**

| | LiveData | StateFlow | SharedFlow |
|---|---------|-----------|-----------|
| Has initial value | ✅ (null) | ✅ (required) | ❌ |
| Lifecycle-aware natively | ✅ | ❌ (use repeatOnLifecycle) | ❌ |
| Can replay | ❌ | Last value only | Configurable |
| Use for | Simple UI state | UI state (preferred) | One-shot events (nav, toast) |
| Testable without Android | ❌ | ✅ | ✅ |

---

## Question 4: "How would you implement the Strategy pattern in Android?"

```kotlin
// Scenario: your app supports multiple image compression strategies
// (user can pick quality vs speed)

interface CompressionStrategy {
    fun compress(bitmap: Bitmap): ByteArray
}

class HighQualityCompression : CompressionStrategy {
    override fun compress(bitmap: Bitmap): ByteArray {
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
        return stream.toByteArray()
    }
}

class FastCompression : CompressionStrategy {
    override fun compress(bitmap: Bitmap): ByteArray {
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 60, stream)
        return stream.toByteArray()
    }
}

class ImageUploader(private var strategy: CompressionStrategy) {
    // Strategy can be swapped at runtime — no subclassing needed
    fun setStrategy(s: CompressionStrategy) { strategy = s }

    fun upload(bitmap: Bitmap) {
        val bytes = strategy.compress(bitmap)
        // upload bytes...
    }
}

// Real Android use: retrofit call adapters, image loaders, analytics providers
```

---

## Question 5: "What is the Facade pattern? Give a real Android example."

```kotlin
// Facade hides complexity behind a simple interface
// Real example: AnalyticsManager hiding Firebase + Mixpanel + custom logging

class AnalyticsManager @Inject constructor(
    private val firebase: FirebaseAnalytics,
    private val mixpanel: MixpanelAPI,
    private val logger: DebugLogger
) {
    // Callers only call ONE method — don't need to know about 3 backends
    fun logEvent(event: AnalyticsEvent) {
        firebase.logEvent(event.name, event.toBundle())
        mixpanel.track(event.name, event.toJsonObject())
        logger.log("Analytics: ${event.name}")
    }
}

// Without facade: every feature imports Firebase, Mixpanel, and logger
// With facade: every feature just injects AnalyticsManager
```

---

## Question 6: "Explain Decorator pattern with an Android example."

```kotlin
// Decorator adds behaviour to an object without changing its class
// OkHttp interceptors are the classic Android example

class AuthInterceptor(private val tokenProvider: TokenProvider) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()
        val authenticated = original.newBuilder()
            .header("Authorization", "Bearer ${tokenProvider.getToken()}")
            .build()
        return chain.proceed(authenticated)  // decorates the request
    }
}

class LoggingInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        Log.d("HTTP", "--> ${request.method} ${request.url}")
        val response = chain.proceed(request)
        Log.d("HTTP", "<-- ${response.code} ${request.url}")
        return response
    }
}

// Each interceptor decorates the request/response chain
val client = OkHttpClient.Builder()
    .addInterceptor(AuthInterceptor(tokenProvider))
    .addInterceptor(LoggingInterceptor())
    .build()
```
