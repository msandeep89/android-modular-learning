# Android Modular Architecture — Learning Project

A hands-on learning project for building a production-grade Android app using:
- **Multi-module architecture** (app + core + feature + dynamic-feature modules)
- **Single Activity** with Jetpack Navigation Component
- **Play Feature Delivery** — ship only what the user needs (on-demand, conditional, install-time)
- **Hilt** for dependency injection across modules
- **Android App Bundle (AAB)** for optimised delivery

Each module is self-contained with theory, working Kotlin code, and a mini-project
that builds on the previous one — ending with a fully modularised app with dynamic delivery.

---

## Architecture & Design

- [High Level Design](docs/high-level-design.md) — full system architecture across all modules
- [Low Level Design](docs/low-level-design.md) — detailed design per module

---

## The App We're Building

A **News Reader App** — simple enough to learn from, realistic enough to represent real projects.

| Feature Module | Delivery Type | Description |
|----------------|--------------|-------------|
| `:app` | Always installed | Shell — single Activity, NavHost |
| `:core` | Always installed | Shared models, utils, network, DI base |
| `:feature-home` | Install-time | Home feed — always available |
| `:feature-search` | Install-time | Search — always available |
| `:feature-bookmarks` | On-demand | User downloads only if they want bookmarks |
| `:feature-premium` | Conditional | Only delivered if user has premium entitlement |
| `:feature-ai-chat` | On-demand | Downloaded when user opens AI Chat; includes LLM model |

---

## Learning Plan

### Module 01 — Multi-Module Project Setup
**Goal:** Understand why and how to split an Android app into multiple Gradle modules.

Topics:
- Monolith vs multi-module — trade-offs
- Module types: `com.android.application`, `com.android.library`, `com.android.dynamic-feature`
- `settings.gradle.kts` — declaring modules
- `build.gradle.kts` per module — dependencies and configuration
- Version catalog (`libs.versions.toml`) — managing dependencies centrally
- Module dependency graph — who depends on whom

Mini-project: Create the multi-module skeleton — `:app`, `:core`, `:feature-home` modules with correct Gradle wiring.

---

### Module 02 — Single Activity & Navigation Component
**Goal:** Replace multi-activity navigation with a single Activity and NavGraph.

Topics:
- Why Single Activity? (shared ViewModel, predictable back stack, deep links)
- `NavHostFragment` and `NavController`
- Navigation graph XML — destinations and actions
- Passing arguments safely with Safe Args
- Back stack management
- Bottom navigation + NavController integration
- Nested navigation graphs (one per feature module)

Mini-project: Wire up `:feature-home` and `:feature-search` fragments under one Activity using Navigation Component.

---

### Module 03 — Core Module & Shared Architecture
**Goal:** Build the `:core` module that every feature module depends on.

Topics:
- What belongs in core: models, network client, base classes, extensions
- Preventing core from depending on feature modules (dependency inversion)
- Shared `Result<T>` wrapper for API responses
- Retrofit setup in core
- Resource class for UI state (Loading / Success / Error)
- Kotlin extension functions shared across modules

Mini-project: Implement `:core` with a Retrofit-based news API client and shared `Result<T>` and `UiState<T>` classes.

---

### Module 04 — Hilt Dependency Injection (Multi-Module)
**Goal:** Set up Hilt so dependencies flow correctly across module boundaries.

Topics:
- Hilt setup in multi-module (each module needs `@InstallIn`)
- `@HiltAndroidApp` in the app module
- `@Module` + `@InstallIn` + `@Provides` / `@Binds`
- Providing the Retrofit instance from `:core` to feature modules
- Scopes: `SingletonComponent`, `ActivityComponent`, `ViewModelComponent`
- Hilt ViewModel injection (`@HiltViewModel`)

Mini-project: Wire Hilt through `:app` → `:core` → `:feature-home` so the home screen gets its ViewModel injected with the news API.

---

### Module 05 — Play Feature Delivery (Dynamic Modules)
**Goal:** Convert feature modules to dynamic features and control their delivery.

Topics:
- Android App Bundle (AAB) vs APK — why AAB is required for dynamic delivery
- `com.android.dynamic-feature` module type
- Delivery modes:
  - **Install-time** — included in base APK always
  - **On-demand** — downloaded when user requests it
  - **Conditional** — delivered based on device conditions (API level, country, etc.)
- `SplitInstallManager` — request, monitor, and handle module installs
- `SplitInstallStateUpdatedListener` — track download progress
- Handling install states: PENDING → DOWNLOADING → INSTALLED → FAILED
- Accessing on-demand module code after install
- Testing with `bundletool` locally (no Play Store needed)

Mini-project: Convert `:feature-bookmarks` to an on-demand module. Add a download button that triggers install, shows a progress indicator, and navigates into the module after install.

---

### Module 06 — Conditional Delivery & Module Removal
**Goal:** Deliver modules only to devices that qualify, and remove unused modules.

Topics:
- Conditional delivery conditions: min SDK, device feature, user country
- `<dist:conditions>` in module `AndroidManifest.xml`
- Removing installed on-demand modules to save storage (`SplitInstallManager.deferredUninstall`)
- Deferred install — install in background before user requests
- Handling missing modules gracefully (show install prompt if module not present)
- Fusing modules into the APK for older Play Store versions

Mini-project: Convert `:feature-premium` to a conditional module (only delivered to API 26+ devices). Add logic to check if the module is installed before navigating, and prompt install if not.

---

### Module 07 — Build, Sign & Ship the AAB
**Goal:** Build a signed AAB, test dynamic delivery locally, and prepare for Play Store upload.

Topics:
- `bundletool` — Google's tool for building and testing AABs locally
- Build variants: debug vs release
- Signing config in `build.gradle.kts`
- Building a local APK set from AAB (`bundletool build-apks`)
- Installing on a device from the APK set (`bundletool install-apks`)
- Simulating on-demand module download locally
- ProGuard / R8 rules for multi-module apps
- Play Store upload checklist

Mini-project: Build a signed AAB of the News Reader app, use bundletool to simulate install with and without the on-demand `:feature-bookmarks` module, and verify dynamic download works.

---

### Module 08 — Local Gen AI with MediaPipe & Gemini Nano
**Goal:** Run a large language model fully on-device — no internet, no API key, no server cost.

Topics:
- **Two approaches to on-device LLM on Android:**
  - **MediaPipe LLM Inference API** — run Gemma 2B/3B, Phi-2, Mistral on any Android 8+ device
  - **Android AICore / Gemini Nano** — system-level model on Android 14+ Pixel/Samsung devices
- Loading a model from assets or downloading at runtime
- Streaming token-by-token responses (`generateAsync`)
- Chat UI — message bubbles, typing indicator, streaming text
- Memory and performance considerations (model size, RAM, quantisation)
- Offloading inference to a background coroutine (never block the main thread)
- Model caching — load once, reuse across sessions
- Packaging as an **on-demand dynamic feature** (model files are 1–4 GB)
- Fallback strategy: use Gemini Nano if available, else MediaPipe

Mini-project: Add `:feature-ai-chat` as an on-demand dynamic module. When the user opens it, the module + model download in the background. Once ready, a chat screen lets the user ask questions about news articles — fully on-device, no API calls.

---

## Progress Tracker

| Module | Topic | Status |
|--------|-------|--------|
| 01 | Multi-Module Project Setup | 🔲 Not started |
| 02 | Single Activity & Navigation | 🔲 Not started |
| 03 | Core Module & Shared Architecture | 🔲 Not started |
| 04 | Hilt DI (Multi-Module) | 🔲 Not started |
| 05 | Play Feature Delivery (Dynamic Modules) | 🔲 Not started |
| 06 | Conditional Delivery & Module Removal | 🔲 Not started |
| 07 | Build, Sign & Ship the AAB | 🔲 Not started |
| 08 | Local Gen AI (MediaPipe + Gemini Nano) | 🔲 Not started |

---

## How to use this repo

Each module lives in its own folder (`module-01-multimodule-setup/`, etc.)
with a `README.md` (theory), Kotlin source code, and a PDF learning guide.

Work through them in order — each module adds a layer to the News Reader app.
By the end you will have a production-grade modular Android app with dynamic delivery.
