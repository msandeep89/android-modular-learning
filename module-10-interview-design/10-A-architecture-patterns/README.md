# 10-A — Architecture Patterns

---

## Question 1: "Explain MVVM vs MVI. When would you choose one over the other?"

### What the interviewer tests
- Do you understand *why* these patterns exist, not just the acronym?
- Can you explain unidirectional data flow?
- Do you know the trade-offs?

### MVVM — Model View ViewModel

```
User Action → View → ViewModel → Repository → Model
                ↑                    ↓
                └──── StateFlow ─────┘
```

**When to use:** screens with simple state — a list, a form, a detail page.

```kotlin
// ViewModel exposes state via StateFlow
// View observes and renders — never pulls state manually
@HiltViewModel
class ArticleListViewModel @Inject constructor(
    private val repo: NewsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<UiState<List<Article>>>(UiState.Idle)
    val uiState: StateFlow<UiState<List<Article>>> = _uiState.asStateFlow()

    fun loadArticles() {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            _uiState.value = when (val result = repo.getTopHeadlines()) {
                is Result.Success -> UiState.Success(result.data)
                is Result.Error   -> UiState.Error(result.message)
                else              -> UiState.Error("Unexpected state")
            }
        }
    }
}

// Fragment: one collect, render everything
viewLifecycleOwner.lifecycleScope.launch {
    repeatOnLifecycle(Lifecycle.State.STARTED) {
        viewModel.uiState.collect { state ->
            when (state) {
                is UiState.Loading -> showLoading()
                is UiState.Success -> showArticles(state.data)
                is UiState.Error   -> showError(state.message)
                else -> Unit
            }
        }
    }
}
```

---

### MVI — Model View Intent

```
User Intent → ViewModel.reduce(state, intent) → New State → View renders
```

**When to use:** complex screens with many interactions, multi-step flows,
screens where you need to replay/time-travel state (e.g. payment flow, checkout).

```kotlin
// All possible user actions are sealed intents
sealed class ArticleIntent {
    data object LoadArticles : ArticleIntent()
    data class Search(val query: String) : ArticleIntent()
    data class BookmarkArticle(val articleId: String) : ArticleIntent()
    data object Refresh : ArticleIntent()
}

// State is a single immutable data class — no partial updates
data class ArticleState(
    val articles: List<Article> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val searchQuery: String = "",
    val bookmarkedIds: Set<String> = emptySet()
)

// One-shot events (navigation, toast) use a separate channel
sealed class ArticleEffect {
    data class ShowToast(val message: String) : ArticleEffect()
    data class NavigateToDetail(val articleId: String) : ArticleEffect()
}

@HiltViewModel
class ArticleMviViewModel @Inject constructor(
    private val repo: NewsRepository
) : ViewModel() {

    private val _state = MutableStateFlow(ArticleState())
    val state: StateFlow<ArticleState> = _state.asStateFlow()

    private val _effect = Channel<ArticleEffect>()
    val effect = _effect.receiveAsFlow()

    fun processIntent(intent: ArticleIntent) {
        when (intent) {
            is ArticleIntent.LoadArticles    -> loadArticles()
            is ArticleIntent.Search          -> search(intent.query)
            is ArticleIntent.BookmarkArticle -> bookmark(intent.articleId)
            is ArticleIntent.Refresh         -> loadArticles()
        }
    }

    private fun loadArticles() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            when (val result = repo.getTopHeadlines()) {
                is Result.Success -> _state.update {
                    it.copy(isLoading = false, articles = result.data)
                }
                is Result.Error -> {
                    _state.update { it.copy(isLoading = false, error = result.message) }
                    _effect.send(ArticleEffect.ShowToast(result.message))
                }
                else -> Unit
            }
        }
    }

    private fun bookmark(articleId: String) {
        _state.update { current ->
            val updated = if (articleId in current.bookmarkedIds)
                current.bookmarkedIds - articleId
            else
                current.bookmarkedIds + articleId
            current.copy(bookmarkedIds = updated)
        }
    }

    private fun search(query: String) {
        _state.update { it.copy(searchQuery = query) }
    }
}
```

### Key difference — answer this in one sentence:
> "MVVM has mutable state scattered across multiple LiveData/StateFlow variables.
> MVI enforces a single immutable state object reduced by intents — making state
> changes predictable, testable, and replayable."

---

## Question 2: "Walk me through Clean Architecture in Android."

### The layers

```
UI Layer          →  Fragments, ViewModels, Adapters
Domain Layer      →  Use Cases (business rules, pure Kotlin, no Android imports)
Data Layer        →  Repositories, Remote/Local Data Sources, DTOs
```

### Why Use Cases matter (the part most candidates miss)

```kotlin
// WITHOUT use case — ViewModel does too much, hard to test, hard to reuse
class BadViewModel(private val repo: NewsRepository) : ViewModel() {
    fun getBookmarkedTechArticles() {
        viewModelScope.launch {
            val all = repo.getArticles()
            val filtered = all.filter {
                it.category == Category.TECH && it.id in repo.getBookmarkedIds()
            }
            // ...
        }
    }
}

// WITH use case — business logic is isolated, testable, reusable
class GetBookmarkedTechArticlesUseCase @Inject constructor(
    private val articleRepo: ArticleRepository,
    private val bookmarkRepo: BookmarkRepository
) {
    // operator fun invoke() lets you call it like a function: useCase()
    suspend operator fun invoke(): List<Article> {
        val bookmarkedIds = bookmarkRepo.getBookmarkedIds()
        return articleRepo.getArticles()
            .filter { it.category == Category.TECH && it.id in bookmarkedIds }
    }
}

// ViewModel is now thin — just orchestrates and exposes state
@HiltViewModel
class CleanViewModel @Inject constructor(
    private val getBookmarkedTechArticles: GetBookmarkedTechArticlesUseCase
) : ViewModel() {
    fun load() {
        viewModelScope.launch {
            val articles = getBookmarkedTechArticles() // reads like English
        }
    }
}
```

### Follow-up questions
- "Can the domain layer import Android classes?" → **No. Pure Kotlin/Java only. This keeps it testable with plain JUnit.**
- "Where does Room go — domain or data?" → **Data layer. Domain defines the Repository interface; data layer implements it.**
- "What if you have 50 use cases — isn't that too many files?" → **Yes, at some point pragmatism wins. Combine simple CRUD use cases into one if they're always used together.**

---

## Question 3: "How do you handle communication between feature modules without creating circular dependencies?"

```kotlin
// WRONG — feature-home imports feature-bookmarks → circular dependency
// feature-bookmarks imports feature-home → compilation fails

// CORRECT — use a navigation contract in :core
// :core defines the interface, each module implements it

// In :core
interface FeatureNavigator {
    fun navigateToBookmarks(navController: NavController)
    fun navigateToSearch(navController: NavController, query: String)
    fun navigateToAiChat(navController: NavController)
}

// In :app (knows about all modules, wires them together)
@Singleton
class AppNavigator @Inject constructor() : FeatureNavigator {
    override fun navigateToBookmarks(navController: NavController) {
        navController.navigate(R.id.bookmarksFragment)
    }
    override fun navigateToSearch(navController: NavController, query: String) {
        val action = HomeFragmentDirections.toSearch(query)
        navController.navigate(action)
    }
    override fun navigateToAiChat(navController: NavController) {
        navController.navigate(R.id.aiChatFragment)
    }
}

// :feature-home injects FeatureNavigator (the interface from :core)
// It never imports :feature-search directly
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val navigator: FeatureNavigator,
    private val repo: NewsRepository
) : ViewModel() {
    fun onSearchClicked(navController: NavController) {
        navigator.navigateToSearch(navController, "")
    }
}
```
