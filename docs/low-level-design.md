# Low Level Design — Per Module Detail

> Jump to: [Module 08 — Local Gen AI](#module-08--local-gen-ai-mediapipe--gemini-nano)

---

## Module 01 — Multi-Module Project Setup

```mermaid
flowchart TD
    SETTINGS["settings.gradle.kts\ninclude :app :core :feature-home\n:feature-search :feature-bookmarks :feature-premium"]

    subgraph VERSIONS["libs.versions.toml (Version Catalog)"]
        VER["kotlin = '2.0.0'\nandroid-gradle = '8.5.0'\nhilt = '2.51.1'\nnavigation = '2.7.7'"]
    end

    subgraph APP_GRADLE[":app/build.gradle.kts"]
        A1["plugin: com.android.application"]
        A2["plugin: kotlin.android"]
        A3["plugin: hilt"]
        A4["dependencies: :core, :feature-home, :feature-search"]
        A5["dynamicFeatures: [:feature-bookmarks, :feature-premium]"]
    end

    subgraph CORE_GRADLE[":core/build.gradle.kts"]
        C1["plugin: com.android.library"]
        C2["Retrofit, Gson, Hilt, Coroutines"]
    end

    subgraph FEATURE_GRADLE[":feature-home/build.gradle.kts"]
        F1["plugin: com.android.library"]
        F2["deps: :core, Navigation, ViewModel"]
    end

    subgraph DYN_GRADLE[":feature-bookmarks/build.gradle.kts"]
        D1["plugin: com.android.dynamic-feature"]
        D2["deps: :app (NOT :core directly)"]
        D3["dist:onDemand in manifest"]
    end

    SETTINGS --> APP_GRADLE
    SETTINGS --> CORE_GRADLE
    SETTINGS --> FEATURE_GRADLE
    SETTINGS --> DYN_GRADLE
    VERSIONS -.->|referenced by| APP_GRADLE
    VERSIONS -.->|referenced by| CORE_GRADLE
```

**Key files:** `settings.gradle.kts`, `libs.versions.toml`, per-module `build.gradle.kts`

---

## Module 02 — Single Activity & Navigation

```mermaid
flowchart TD
    MAIN["MainActivity\n(only Activity in the app)"]

    subgraph LAYOUT["activity_main.xml"]
        HOST["FragmentContainerView\napp:navGraph=@navigation/nav_main\napp:defaultNavHost=true"]
        BOTTOM_NAV["BottomNavigationView\nHome | Search | Bookmarks"]
    end

    MAIN --> LAYOUT

    subgraph NAV_GRAPH["res/navigation/nav_main.xml"]
        HOME_DEST["homeFragment\n(startDestination)"]
        SEARCH_DEST["searchFragment"]
        BOOKMARKS_DEST["bookmarksFragment"]
        DETAIL_DEST["articleDetailFragment\nargument: articleId: String"]
    end

    HOME_DEST -->|action: toDetail| DETAIL_DEST
    SEARCH_DEST -->|action: toDetail| DETAIL_DEST

    NAV_CTRL["NavController\nfindNavController(R.id.nav_host)"]
    MAIN --> NAV_CTRL
    NAV_CTRL -->|navigate| NAV_GRAPH
    BOTTOM_NAV -->|setupWithNavController| NAV_CTRL

    SAFE_ARGS["Safe Args Plugin\ngenerates: HomeFragmentDirections.toDetail(articleId)"]
    NAV_GRAPH -.->|generates| SAFE_ARGS
```

**Key files:** `activity_main.xml`, `nav_main.xml`, `build.gradle.kts` (safeargs plugin)

---

## Module 03 — Core Module & Shared Architecture

```mermaid
flowchart TD
    subgraph CORE[":core module"]
        subgraph NETWORK["network/"]
            RETROFIT["RetrofitClient\n(singleton, base URL)"]
            API["NewsApiService\n(@GET, @Query)"]
            RETROFIT --> API
        end

        subgraph MODEL["model/"]
            ARTICLE["data class Article\n(id, title, url, imageUrl, publishedAt)"]
            CATEGORY["enum class Category\n(TOP, SPORTS, TECH, BUSINESS)"]
        end

        subgraph COMMON["common/"]
            RESULT["sealed class Result&lt;T&gt;\n  Success(data: T)\n  Error(message: String)\n  Loading"]
            EXT["Extension functions\nString.toDate()\nView.show() / hide()"]
        end

        subgraph REPO["repository/"]
            NEWS_REPO["interface NewsRepository"]
            NEWS_REPO_IMPL["NewsRepositoryImpl\n  getTopHeadlines()\n  searchArticles(query)"]
            NEWS_REPO --> NEWS_REPO_IMPL
        end
    end

    API --> NEWS_REPO_IMPL
    NEWS_REPO_IMPL --> RESULT
    NEWS_REPO_IMPL --> ARTICLE
```

**Key files:** `RetrofitClient.kt`, `NewsApiService.kt`, `Article.kt`, `Result.kt`, `NewsRepository.kt`

---

## Module 04 — Hilt DI (Multi-Module)

```mermaid
flowchart TD
    subgraph APP_MOD[":app"]
        APP_CLASS["@HiltAndroidApp\nNewsReaderApp : Application"]
        MAIN_ACT["@AndroidEntryPoint\nMainActivity"]
    end

    subgraph CORE_MOD[":core — NetworkModule.kt"]
        NET_MOD["@Module @InstallIn(SingletonComponent)\nobject NetworkModule"]
        PROV_RETRO["@Provides @Singleton\nfun provideRetrofit(): Retrofit"]
        PROV_API["@Provides @Singleton\nfun provideNewsApiService(retrofit): NewsApiService"]
        PROV_REPO["@Binds @Singleton\nfun bindNewsRepository(impl): NewsRepository"]
        NET_MOD --> PROV_RETRO
        NET_MOD --> PROV_API
        NET_MOD --> PROV_REPO
    end

    subgraph FEATURE_MOD[":feature-home"]
        HOME_VM["@HiltViewModel\nclass HomeViewModel @Inject constructor(\n  private val repo: NewsRepository\n)"]
        HOME_FRAG["@AndroidEntryPoint\nHomeFragment\n  val vm: HomeViewModel by viewModels()"]
        HOME_FRAG --> HOME_VM
    end

    APP_CLASS -->|provides Hilt component| CORE_MOD
    PROV_REPO -->|injected into| HOME_VM
```

**Key files:** `NewsReaderApp.kt`, `NetworkModule.kt`, `HomeViewModel.kt`, `HomeFragment.kt`

---

## Module 05 — Play Feature Delivery (On-Demand)

```mermaid
flowchart TD
    USER["User taps 'Bookmarks'"] --> CHECK["Check: is :feature-bookmarks installed?\nsplitInstallManager.installedModules"]

    CHECK -->|yes| NAVIGATE["navController.navigate(R.id.bookmarksFragment)"]

    CHECK -->|no| REQUEST["SplitInstallManager\n.startInstall(SplitInstallRequest\n  .newBuilder()\n  .addModule('feature_bookmarks')\n  .build())"]

    REQUEST --> LISTENER["SplitInstallStateUpdatedListener"]

    LISTENER --> PENDING["PENDING → show spinner"]
    LISTENER --> DOWNLOADING["DOWNLOADING → show progress bar\nstatus.bytesDownloaded / totalBytesToDownload"]
    LISTENER --> INSTALLED["INSTALLED → navigate to Bookmarks"]
    LISTENER --> FAILED["FAILED → show error + retry button\nstatus.errorCode"]

    INSTALLED --> NAVIGATE

    subgraph MANIFEST[":feature-bookmarks/AndroidManifest.xml"]
        DIST["&lt;dist:module\n  dist:instant='false'&gt;\n  &lt;dist:delivery&gt;\n    &lt;dist:on-demand/&gt;\n  &lt;/dist:delivery&gt;\n&lt;/dist:module&gt;"]
    end
```

**Key files:** `BookmarkInstallViewModel.kt`, `SplitInstallManager`, `:feature-bookmarks/AndroidManifest.xml`

---

## Module 06 — Conditional Delivery & Module Removal

```mermaid
flowchart TD
    subgraph MANIFEST[":feature-premium/AndroidManifest.xml"]
        COND["&lt;dist:module&gt;\n  &lt;dist:delivery&gt;\n    &lt;dist:install-time&gt;\n      &lt;dist:conditions&gt;\n        &lt;dist:min-sdk dist:value='26'/&gt;\n        &lt;dist:user-countries dist:include='true'&gt;\n          &lt;dist:country dist:code='IN'/&gt;\n        &lt;/dist:user-countries&gt;\n      &lt;/dist:conditions&gt;\n    &lt;/dist:install-time&gt;\n  &lt;/dist:delivery&gt;\n&lt;/dist:module&gt;"]
    end

    subgraph REMOVAL["Module Removal (save storage)"]
        DEFER["splitInstallManager\n  .deferredUninstall(\n    listOf('feature_bookmarks')\n  )"]
    end

    subgraph CHECK_FLOW["Runtime check before navigating"]
        IS_INST["splitInstallManager\n  .installedModules\n  .contains('feature_premium')"]
        IS_INST -->|true| NAV["Navigate to premium"]
        IS_INST -->|false| PROMPT["Show 'Not available on your device' dialog"]
    end
```

**Key files:** `:feature-premium/AndroidManifest.xml`, `PremiumEntryViewModel.kt`

---

## Module 07 — Build, Sign & Ship AAB

```mermaid
flowchart TD
    CODE["Source Code"] --> GRADLE["./gradlew bundleRelease"]
    GRADLE --> AAB["app-release.aab"]

    subgraph SIGN["Signing (build.gradle.kts)"]
        KS["keystore.jks"]
        SIGN_CFG["signingConfigs {\n  release {\n    storeFile file(keystore.jks)\n    keyAlias ...\n    keyPassword ...\n  }\n}"]
    end

    AAB --> BUNDLETOOL["bundletool build-apks\n  --bundle=app-release.aab\n  --output=app.apks\n  --ks=keystore.jks"]

    BUNDLETOOL --> APKS["app.apks\n(local APK set for testing)"]

    APKS --> INSTALL["bundletool install-apks\n  --apks=app.apks\n  --device-id=emulator-5554"]

    INSTALL --> DEVICE["📱 App installed on device\nBase APK only — no :feature-bookmarks"]

    APKS --> SIM["Simulate on-demand install\nbundletool install-apks\n  --apks=app.apks\n  --modules=feature_bookmarks"]

    SIM --> DEVICE2["📱 :feature-bookmarks now installed\nas if downloaded from Play Store"]
```

**Key files:** `build.gradle.kts` (signingConfigs), `keystore.jks`, `bundletool` CLI

---

## Module 08 — Local Gen AI (MediaPipe + Gemini Nano)

### Two on-device AI paths

```mermaid
flowchart TD
    USER["User opens AI Chat"] --> CHECK_MOD["Is :feature-ai-chat installed?\nSplitInstallManager"]

    CHECK_MOD -->|no| DOWNLOAD_MOD["Download module + model\n~1-4 GB via Play Store"]
    DOWNLOAD_MOD --> READY["Module installed\nNavigate to ChatFragment"]
    CHECK_MOD -->|yes| READY

    READY --> DETECT["Detect best inference engine"]

    subgraph ENGINE["Inference Engine Selection"]
        DETECT -->|Android 14+ Pixel/Samsung| NANO["Gemini Nano\nvia Android AICore\n(system model, always available)"]
        DETECT -->|Android 8+, any device| MEDIAPIPE["MediaPipe LLM Inference\nGemma 2B (quantised, ~1.5 GB)\nor Phi-2 (~1.1 GB)"]
    end

    NANO --> INFER["LlmInferenceSession\nor InferenceSession"]
    MEDIAPIPE --> INFER

    INFER --> STREAM["generateAsync(prompt)\nstreams tokens one by one"]
    STREAM --> UI["ChatFragment\nupdates TextView token-by-token"]
```

---

### MediaPipe LLM Inference flow

```mermaid
flowchart TD
    subgraph SETUP["One-time setup (on first launch)"]
        ASSET["Model file in assets/\ngemma-2b-it-cpu-int4.bin"]
        LOAD["LlmInference.create(context, options)\n(runs on IO dispatcher — takes 5-15s)"]
        CACHE["Keep instance alive\nin ViewModel (don't recreate per message)"]
        ASSET --> LOAD --> CACHE
    end

    subgraph CHAT["Per-message inference"]
        INPUT["User types message"] --> BUILD["Build prompt\nwith chat history context"]
        BUILD --> ASYNC["llmInference.generateAsync(\n  prompt,\n  resultListener = { partial, done ->\n    _uiState.update { it + partial }\n  }\n)"]
        ASYNC --> TOKENS["Partial tokens streamed\nto StateFlow"]
        TOKENS --> COMPOSE["LazyColumn re-renders\nas tokens arrive"]
    end

    CACHE --> ASYNC
```

---

### Gemini Nano (AICore) flow — Android 14+

```mermaid
flowchart TD
    subgraph AVAIL["Check availability"]
        CHECK["GenerativeModel\n  .checkAvailability(context)"]
        CHECK -->|AVAILABLE| USE["Use Gemini Nano"]
        CHECK -->|UNAVAILABLE| FALL["Fallback to MediaPipe"]
        CHECK -->|DOWNLOADING| WAIT["Wait + show progress"]
    end

    subgraph INFERENCE["Gemini Nano inference"]
        MODEL["GenerativeModel(\n  modelName = 'gemini-nano'\n)"]
        SESSION["model.startChat(\n  history = previousMessages\n)"]
        SEND["session.sendMessageStream(prompt)"]
        COLLECT["flow.collect { chunk ->\n  append chunk.text to UI\n}"]
        MODEL --> SESSION --> SEND --> COLLECT
    end

    USE --> MODEL
```

---

### Architecture of :feature-ai-chat

```mermaid
flowchart TD
    subgraph FEATURE[":feature-ai-chat (dynamic-feature module)"]
        subgraph UI["ui/"]
            CHAT_FRAG["ChatFragment\n@AndroidEntryPoint"]
            CHAT_VM["ChatViewModel\n@HiltViewModel"]
            MSG_ADAPTER["MessageAdapter\n(RecyclerView)"]
            CHAT_FRAG --> CHAT_VM
            CHAT_FRAG --> MSG_ADAPTER
        end

        subgraph DOMAIN["domain/"]
            CHAT_REPO["interface ChatRepository\n  fun sendMessage(prompt): Flow&lt;String&gt;"]
        end

        subgraph DATA["data/"]
            MEDIAPIPE_REPO["MediaPipeChatRepository\n  implements ChatRepository\n  uses LlmInference"]
            NANO_REPO["GeminiNanoChatRepository\n  implements ChatRepository\n  uses GenerativeModel"]
            FACTORY["ChatRepositoryFactory\n  returns best available impl"]
            FACTORY --> MEDIAPIPE_REPO
            FACTORY --> NANO_REPO
        end

        subgraph MODEL_FILE["assets/"]
            BIN["gemma-2b-it-cpu-int4.bin\n(~1.5 GB — packaged in AAB,\ndelivered only with this module)"]
        end
    end

    CHAT_VM --> CHAT_REPO
    CHAT_REPO --> FACTORY
    FACTORY -->|loads| BIN
```

---

### Memory & performance rules

| Rule | Why |
|------|-----|
| Load model once in ViewModel, not per message | Model init takes 5–15s and uses ~1.5 GB RAM |
| Run inference on `Dispatchers.IO` | LLM inference blocks the thread — never run on Main |
| Use `generateAsync` with streaming | User sees response immediately, not after full generation |
| Clear model in `onCleared()` | Release RAM when user leaves the screen |
| Quantised INT4 model (not FP32) | ~4x smaller file, ~3x faster, minimal quality loss |
| Show RAM warning if device < 4 GB | Gemma 2B needs ~2 GB free RAM to run |

---

### AndroidManifest for :feature-ai-chat

```xml
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:dist="http://schemas.android.com/apk/distribution">

    <!-- On-demand: downloaded only when user opens AI Chat -->
    <dist:module
        dist:instant="false"
        dist:title="@string/title_feature_ai_chat">
        <dist:delivery>
            <dist:on-demand />
        </dist:delivery>
        <dist:fusing dist:include="true" />
    </dist:module>

</manifest>
```

**Key files:** `ChatFragment.kt`, `ChatViewModel.kt`, `MediaPipeChatRepository.kt`,
`GeminiNanoChatRepository.kt`, `ChatRepositoryFactory.kt`, `feature-ai-chat/AndroidManifest.xml`
