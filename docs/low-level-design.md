# Low Level Design — Per Module Detail

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
