# 10-E — Performance Patterns

---

## Question 1: "How does DiffUtil work? Why is it better than notifyDataSetChanged()?"

```kotlin
// notifyDataSetChanged() — rebinds EVERY item, even unchanged ones
// Causes full RecyclerView re-layout: janky scrolling, no animations

// DiffUtil — computes minimal diff on a background thread
// Only rebinds changed items, animates additions/removals/moves

class ArticleDiffCallback : DiffUtil.ItemCallback<Article>() {

    // Called first: are these the same logical item?
    // Use a stable unique ID — NOT position
    override fun areItemsTheSame(old: Article, new: Article): Boolean =
        old.id == new.id

    // Called only if areItemsTheSame returns true
    // Are the contents identical? Uses equals() by default for data classes
    override fun areContentsTheSame(old: Article, new: Article): Boolean =
        old == new

    // Optional: return the specific field that changed for partial bind
    // This avoids re-binding the entire ViewHolder for a small change
    override fun getChangePayload(old: Article, new: Article): Any? {
        return when {
            old.isBookmarked != new.isBookmarked -> "bookmark_changed"
            old.title != new.title               -> "title_changed"
            else -> null
        }
    }
}

class ArticleAdapter : ListAdapter<Article, ArticleViewHolder>(ArticleDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ArticleViewHolder {
        val binding = ItemArticleBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ArticleViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ArticleViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    // Called for partial updates (when getChangePayload returns non-null)
    override fun onBindViewHolder(holder: ArticleViewHolder, position: Int, payloads: MutableList<Any>) {
        if (payloads.isEmpty()) {
            super.onBindViewHolder(holder, position, payloads)
            return
        }
        // Only update what changed
        payloads.forEach { payload ->
            when (payload) {
                "bookmark_changed" -> holder.updateBookmarkIcon(getItem(position).isBookmarked)
                "title_changed"    -> holder.updateTitle(getItem(position).title)
            }
        }
    }
}

// Submit from ViewModel — ListAdapter runs diff on background thread automatically
viewModel.articles.collect { articles ->
    adapter.submitList(articles)
}
```

**Follow-up: "What if two items have the same ID but different content?"**
> `areItemsTheSame` returns true → DiffUtil calls `areContentsTheSame` → returns false → triggers `onBindViewHolder` with payloads. The item is NOT removed and re-added; it's updated in-place.

---

## Question 2: "How do you prevent memory leaks in Android?"

```kotlin
// ── Leak 1: Fragment holds binding reference past onDestroyView ──────────────
class BadFragment : Fragment() {
    private lateinit var binding: FragmentBadBinding  // ← held forever

    override fun onCreateView(...) = FragmentBadBinding.inflate(inflater).root
    // binding still alive after onDestroyView → leaks views
}

class GoodFragment : Fragment() {
    private var _binding: FragmentGoodBinding? = null
    private val binding get() = _binding!!  // non-null accessor

    override fun onCreateView(...): View {
        _binding = FragmentGoodBinding.inflate(inflater)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null  // release view hierarchy
    }
}

// ── Leak 2: Static reference to Context ──────────────────────────────────────
object BadSingleton {
    lateinit var context: Context  // ← Activity/Fragment Context → leaked
}

object GoodSingleton {
    lateinit var context: Context  // use applicationContext only
    fun init(ctx: Context) { context = ctx.applicationContext }
}

// ── Leak 3: Unregistered listeners ───────────────────────────────────────────
class LeakyFragment : Fragment() {
    private val listener = SensorEventListener { /* ... */ }

    override fun onResume() {
        super.onResume()
        sensorManager.registerListener(listener, sensor, SENSOR_DELAY_UI)
    }

    // If you forget onPause, sensor manager holds reference to Fragment → leak
    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(listener)
    }
}

// ── Leak 4: Coroutine launched on GlobalScope ─────────────────────────────────
class LeakyViewModel : ViewModel() {
    fun badLoad() = GlobalScope.launch { /* never cancelled */ }
    fun goodLoad() = viewModelScope.launch { /* cancelled with ViewModel */ }
}

// ── Detect leaks ─────────────────────────────────────────────────────────────
// Add LeakCanary in debug builds — it reports leaks with full stack trace
// debugImplementation("com.squareup.leakcanary:leakcanary-android:2.14")
```

---

## Question 3: "How does Paging 3 work? When would you use it?"

```kotlin
// Paging 3: load data in chunks, only what the user can see + a buffer
// Never loads 10,000 items upfront

// Step 1: PagingSource — defines how to load one page
class ArticlePagingSource(
    private val api: NewsApiService,
    private val query: String
) : PagingSource<Int, Article>() {

    override fun getRefreshKey(state: PagingState<Int, Article>): Int? {
        // Return the page closest to the last visible item on refresh
        return state.anchorPosition?.let { anchor ->
            state.closestPageToPosition(anchor)?.prevKey?.plus(1)
                ?: state.closestPageToPosition(anchor)?.nextKey?.minus(1)
        }
    }

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Article> {
        val page = params.key ?: 1
        return try {
            val response = api.searchArticles(query = query, page = page, pageSize = params.loadSize)
            LoadResult.Page(
                data = response.articles,
                prevKey = if (page == 1) null else page - 1,
                nextKey = if (response.articles.isEmpty()) null else page + 1
            )
        } catch (e: IOException) {
            LoadResult.Error(e)
        }
    }
}

// Step 2: Repository creates Pager
class NewsRepositoryImpl {
    fun searchArticlesPaged(query: String): Flow<PagingData<Article>> =
        Pager(
            config = PagingConfig(
                pageSize = 20,
                prefetchDistance = 5,  // load next page when 5 items from end
                enablePlaceholders = false
            ),
            pagingSourceFactory = { ArticlePagingSource(api, query) }
        ).flow

}

// Step 3: ViewModel
@HiltViewModel
class SearchViewModel @Inject constructor(repo: NewsRepository) : ViewModel() {
    val articles: Flow<PagingData<Article>> =
        repo.searchArticlesPaged("android")
            .cachedIn(viewModelScope)  // survives config changes
}

// Step 4: PagingDataAdapter in Fragment
class ArticlePagingAdapter : PagingDataAdapter<Article, ArticleViewHolder>(ArticleDiffCallback()) {
    override fun onCreateViewHolder(...) = ArticleViewHolder(...)
    override fun onBindViewHolder(holder: ArticleViewHolder, position: Int) {
        getItem(position)?.let { holder.bind(it) }  // getItem can return null (placeholder)
    }
}

// Step 5: Collect and handle load states
viewLifecycleOwner.lifecycleScope.launch {
    repeatOnLifecycle(Lifecycle.State.STARTED) {
        launch {
            viewModel.articles.collectLatest { adapter.submitData(it) }
        }
        launch {
            adapter.loadStateFlow.collect { states ->
                val refresh = states.refresh
                progressBar.isVisible = refresh is LoadState.Loading
                retryButton.isVisible  = refresh is LoadState.Error
            }
        }
    }
}
```

---

## Question 4: "How do you optimise RecyclerView performance?"

| Technique | Code | Impact |
|-----------|------|--------|
| Use DiffUtil | `ListAdapter` with `DiffUtil.ItemCallback` | ✅ No full rebind |
| Stable IDs | `adapter.setHasStableIds(true)` | ✅ Better animation |
| Avoid overdraw | Flatten layouts, remove unnecessary backgrounds | ✅ Faster drawing |
| ViewHolder pattern | Cache `findViewById` calls in `ViewHolder` | ✅ No redundant lookup |
| RecycledViewPool | Share pool between nested RecyclerViews | ✅ Fewer inflations |
| Prefetch | `LinearLayoutManager.enableBrainDeadShit(true)` — default in API 25+ | ✅ Smoother scroll |
| Avoid heavy `onBindViewHolder` | No `Gson.fromJson` or `DateFormat.parse` in bind | ✅ No janky scroll |
| Image sizing | Load exact pixel size via Coil `size()` param | ✅ No rescaling |

```kotlin
// RecycledViewPool for nested horizontal lists (e.g. category rows in news feed)
val sharedPool = RecyclerView.RecycledViewPool()
sharedPool.setMaxRecycledViews(0, 10)  // type 0, keep 10 views ready

outerAdapter.onAttachRecycledViewPool = { innerRecyclerView ->
    innerRecyclerView.setRecycledViewPool(sharedPool)
}
```
