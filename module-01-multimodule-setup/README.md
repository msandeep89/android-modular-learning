# Module 01 — Multi-Module Project Setup

## Purpose
Set up the complete Gradle skeleton for a production multi-module Android app.
No real UI or business logic yet — the goal is to get all 8 modules wired together,
compiling, and ready to receive code in subsequent modules.

---

## What is Implemented

### Project: NewsReaderApp

| Module | Plugin | Delivery | Purpose |
|--------|--------|---------|---------|
| `:app` | `com.android.application` | Always | Single Activity shell, NavHost, BottomNav |
| `:core` | `com.android.library` | Always (merged) | Shared models, Result<T>, UiState<T> |
| `:feature-home` | `com.android.library` | Install-time | Home feed stub |
| `:feature-search` | `com.android.library` | Install-time | Search stub |
| `:feature-bookmarks` | `com.android.dynamic-feature` | **On-demand** | Saved articles |
| `:feature-premium` | `com.android.dynamic-feature` | **Conditional** (API 26+) | Premium content |
| `:feature-ai-chat` | `com.android.dynamic-feature` | **On-demand** | On-device LLM chat |
| `:feature-ai-explorer` | `com.android.dynamic-feature` | **On-demand** | AI model playground |

### Key files created

| File | What it does |
|------|-------------|
| `settings.gradle.kts` | Declares all 8 modules — this is the entry point for Gradle |
| `gradle/libs.versions.toml` | Central version catalog — one place to update any dependency version |
| `build.gradle.kts` (root) | Applies plugins to all modules without activating them |
| `app/build.gradle.kts` | Declares `dynamicFeatures` — links dynamic modules to the base app |
| `core/model/Article.kt` | Shared `Article` data class and `Category` enum used by all features |
| `core/common/Result.kt` | `Result<T>` and `UiState<T>` sealed classes used by all ViewModels |
| `feature-*/AndroidManifest.xml` | Each dynamic module's manifest contains `<dist:module>` delivery config |

---

## Key Concepts

### Why libs.versions.toml?
Without a version catalog, each module's `build.gradle.kts` has hardcoded version strings.
When you update a library you'd have to change 8 files. With the catalog:
```toml
[versions]
retrofit = "2.11.0"            # change once here

[libraries]
retrofit = { group = "com.squareup.retrofit2", name = "retrofit", version.ref = "retrofit" }
```
Every module just writes `implementation(libs.retrofit)` — no version strings scattered around.

### Why do dynamic modules depend on :app and not :core?
Library modules (`:feature-home`) depend on `:core` directly.
Dynamic feature modules (`:feature-bookmarks`) must depend on `:app`.

```
:feature-home    →  depends on  →  :core      ✅ correct
:feature-bookmarks  →  depends on  →  :app    ✅ correct
:feature-bookmarks  →  depends on  →  :core   ❌ will cause build error
```

`:app` already depends on `:core`, so dynamic modules get `:core` transitively through `:app`.

### What is `dynamicFeatures` in :app?
```kotlin
// app/build.gradle.kts
dynamicFeatures += setOf(
    ":feature-bookmarks",
    ":feature-premium",
    ":feature-ai-chat",
    ":feature-ai-explorer"
)
```
This tells the Android Gradle Plugin that these modules are dynamic features of this app.
Without this list, the modules are unknown to the build system and won't be included in the AAB.

### Delivery modes in AndroidManifest.xml
```xml
<!-- On-demand: user must trigger download -->
<dist:delivery>
    <dist:on-demand />
</dist:delivery>

<!-- Conditional: auto-delivered if device meets conditions -->
<dist:delivery>
    <dist:install-time>
        <dist:conditions>
            <dist:min-sdk dist:value="26" />
        </dist:conditions>
    </dist:install-time>
</dist:delivery>
```

---

## How to Open in Android Studio

1. Open Android Studio
2. **File → Open** → select the `NewsReaderApp/` folder
3. Wait for Gradle sync to complete
4. Run on an emulator or device

The app will compile and launch showing Home and Search tabs (both show placeholder text).
Dynamic modules are declared but empty — they'll be filled in Modules 05–09.

---

## Project Structure
```
NewsReaderApp/
├── settings.gradle.kts              ← declares all 8 modules
├── build.gradle.kts                 ← root: plugin declarations
├── gradle.properties                ← JVM args, AndroidX flags
├── gradle/
│   └── libs.versions.toml          ← ALL dependency versions in one place
├── app/                             ← :app — Single Activity + NavHost
│   ├── build.gradle.kts            ← dynamicFeatures list lives here
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── res/
│       │   ├── layout/activity_main.xml
│       │   ├── navigation/nav_main.xml
│       │   ├── menu/menu_bottom_nav.xml
│       │   └── values/strings.xml, themes.xml
│       └── kotlin/com/sandeep/newsreader/
│           ├── NewsReaderApp.kt    ← @HiltAndroidApp
│           └── MainActivity.kt    ← @AndroidEntryPoint, NavController setup
├── core/                            ← :core — shared across all modules
│   └── src/main/kotlin/.../core/
│       ├── model/Article.kt        ← shared data model
│       └── common/Result.kt       ← Result<T> + UiState<T>
├── feature-home/                    ← install-time library module
├── feature-search/                  ← install-time library module
├── feature-bookmarks/               ← on-demand dynamic feature
├── feature-premium/                 ← conditional dynamic feature
├── feature-ai-chat/                 ← on-demand dynamic feature
└── feature-ai-explorer/             ← on-demand dynamic feature
```
