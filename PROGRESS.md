# Learning Progress Tracker

Daily log of what has been implemented and what is remaining.

---

## Module Status

| Module | Topic | Status | Started | Completed |
|--------|-------|--------|---------|-----------|
| 01 | Multi-Module Project Setup | 🔲 Not started | — | — |
| 02 | Single Activity & Navigation | 🔲 Not started | — | — |
| 03 | Core Module & Shared Architecture | 🔲 Not started | — | — |
| 04 | Hilt DI (Multi-Module) | 🔲 Not started | — | — |
| 05 | Play Feature Delivery (Dynamic Modules) | 🔲 Not started | — | — |
| 06 | Conditional Delivery & Module Removal | 🔲 Not started | — | — |
| 07 | Build, Sign & Ship the AAB | 🔲 Not started | — | — |

**Overall progress: 0 / 7 modules complete**

---

## What is Implemented

_Nothing yet — start with Module 01!_

---

## What is Remaining

### 🔲 Module 01 — Multi-Module Project Setup
- [ ] Theory: monolith vs multi-module trade-offs
- [ ] Theory: module types (`application`, `library`, `dynamic-feature`)
- [ ] `settings.gradle.kts` — declaring all modules
- [ ] `libs.versions.toml` — version catalog setup
- [ ] `:app/build.gradle.kts` with `dynamicFeatures` list
- [ ] `:core/build.gradle.kts` as android library
- [ ] `:feature-home/build.gradle.kts` as android library
- [ ] `:feature-bookmarks/build.gradle.kts` as dynamic-feature
- [ ] Module dependency graph diagram
- [ ] README + PDF learning guide

### 🔲 Module 02 — Single Activity & Navigation
- [ ] `MainActivity` with `NavHostFragment`
- [ ] `BottomNavigationView` wired to `NavController`
- [ ] `nav_main.xml` with all destinations
- [ ] Safe Args plugin setup and usage
- [ ] Nested nav graphs (one per feature module)
- [ ] Article detail screen with argument passing
- [ ] Back stack handling
- [ ] README + PDF learning guide

### 🔲 Module 03 — Core Module & Shared Architecture
- [ ] `RetrofitClient` singleton in `:core`
- [ ] `NewsApiService` interface with suspend functions
- [ ] `Article` and `Category` data models
- [ ] `sealed class Result<T>` (Loading / Success / Error)
- [ ] `UiState<T>` for ViewModel state
- [ ] `NewsRepository` interface + `NewsRepositoryImpl`
- [ ] Shared Kotlin extension functions
- [ ] README + PDF learning guide

### 🔲 Module 04 — Hilt DI (Multi-Module)
- [ ] `@HiltAndroidApp` in Application class
- [ ] `NetworkModule` in `:core` — provides Retrofit, ApiService, Repository
- [ ] `@AndroidEntryPoint` on MainActivity and fragments
- [ ] `@HiltViewModel` on HomeViewModel
- [ ] Verify injection across module boundaries
- [ ] README + PDF learning guide

### 🔲 Module 05 — Play Feature Delivery (On-Demand)
- [ ] Convert `:feature-bookmarks` to `dynamic-feature`
- [ ] `AndroidManifest.xml` with `<dist:on-demand/>`
- [ ] `SplitInstallManager` — request module install
- [ ] `SplitInstallStateUpdatedListener` — track progress
- [ ] Progress UI (spinner → progress bar → navigate)
- [ ] Error handling with retry
- [ ] Test locally with `bundletool`
- [ ] README + PDF learning guide

### 🔲 Module 06 — Conditional Delivery & Module Removal
- [ ] `:feature-premium` dynamic-feature module
- [ ] Conditional manifest — `min-sdk` + `user-countries`
- [ ] Runtime check before navigating to premium
- [ ] `deferredUninstall()` to remove unused modules
- [ ] Graceful fallback if module not available
- [ ] README + PDF learning guide

### 🔲 Module 07 — Build, Sign & Ship the AAB
- [ ] `signingConfigs` in `build.gradle.kts`
- [ ] `./gradlew bundleRelease` — build signed AAB
- [ ] `bundletool build-apks` — generate local APK set
- [ ] `bundletool install-apks` — test on device/emulator
- [ ] Simulate on-demand install with bundletool
- [ ] ProGuard/R8 rules for multi-module
- [ ] README + PDF learning guide

---

## Daily Log

### 2026-07-01
- Initialized the project and pushed to GitHub
- Created 7-module learning plan
- Created HLD (full system architecture) and LLD (per-module diagrams)
- Set up PROGRESS.md and daily reminder workflow

---

> **Tip:** Check off tasks `[x]` as you complete them and add a daily log entry.
> Commit with: `git commit -m "docs: update progress YYYY-MM-DD"`
