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
| `:feature-ai-explorer` | On-demand | Side-by-side comparison of all on-device AI models |

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

### Module 09 — On-Device AI Model Explorer
**Goal:** Integrate and compare every major on-device AI ecosystem in one app —
understand what each model does, how to wire it up, and how it performs on real hardware.

The module is structured as a **tabbed explorer UI** — one tab per AI ecosystem.
Each tab has an input area, a run button, and an output panel showing the result + inference time.

#### Sub-module 09-A — ML Kit
Topics:
- Text Classification — classify news article as sports/tech/politics
- Entity Extraction — pull URLs, phone numbers, addresses from raw text
- Smart Reply — suggest 3 replies to a message
- Language Identification — detect language of pasted text
- Translation — translate detected text to English
- On-device vs cloud API trade-offs

Mini-project: "ML Kit Playground" tab — paste any text, tap a chip (classify / extract / reply / translate), see results in < 100ms.

#### Sub-module 09-B — MediaPipe
Topics:
- LLM Inference API — load Gemma 2B, stream tokens
- Text Classification — custom classifier via MediaPipe Tasks
- Image Classification — classify photo from camera or gallery
- Object Detection — real-time camera object detection
- Face Landmark Detection — 478 face points on camera preview
- Hand Gesture Recognition — detect thumbs up, pointing etc.
- `BaseOptions` and `TaskOptions` — how MediaPipe Tasks are configured

Mini-project: "MediaPipe Playground" tab — camera preview with a toggle row (object / face / hand / gesture). Swap detectors live without restarting the camera.

#### Sub-module 09-C — TensorFlow Lite
Topics:
- Loading a `.tflite` model from assets
- `Interpreter` API — input/output tensors
- Feature engineering for non-image models (URL classifier, spam detector)
- GPU delegate — speed up inference on Snapdragon/Mali GPUs
- NNAPI delegate — use device NPU
- Benchmarking: CPU vs GPU vs NNAPI inference time
- MobileBERT for question answering
- Custom model: train a phishing URL classifier, export to TFLite, run on-device

Mini-project: "TFLite Playground" tab — three demos: image classification (MobileNet), spam SMS detection (MobileBERT), and URL phishing score (custom feature-engineering model).

#### Sub-module 09-D — Gemini Nano (AICore)
Topics:
- Checking AICore availability at runtime (`AvailabilityInfo`)
- Graceful fallback when device doesn't support Gemini Nano
- `GenerativeModel` with `generateContent` and `generateContentStream`
- Summarisation API — summarise a long news article in 3 bullets
- Proofreading API — grammar + style correction
- Comparing Gemini Nano output quality vs Gemma 2B (MediaPipe)
- Supported devices: Pixel 8+, Samsung S24+

Mini-project: "Gemini Nano" tab — only enabled on supported devices (shows "not supported" card otherwise). Demos: summarise article, proofread text, free-form chat.

#### Sub-module 09-E — ONNX Runtime
Topics:
- ONNX Runtime for Android setup
- Loading `.onnx` models from assets
- Running Whisper Tiny for speech-to-text
- Sentence Transformers for semantic similarity (find similar news articles)
- Comparing ONNX vs TFLite for the same model

Mini-project: "ONNX Playground" tab — record voice, transcribe with Whisper Tiny. Type two sentences, get similarity score with Sentence Transformer.

#### Sub-module 09-F — Benchmarking & Device Comparison
Topics:
- Measuring inference latency (cold start vs warm)
- Memory usage per model
- Battery impact of continuous inference
- Results table: emulator vs mid-range device vs Pixel 8
- Which model to pick for which use case

Mini-project: Benchmark screen — runs the same input through all available models and renders a comparison table (latency, accuracy, model size, RAM used).

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
| 09-A | ML Kit Playground | 🔲 Not started |
| 09-B | MediaPipe Playground | 🔲 Not started |
| 09-C | TensorFlow Lite Playground | 🔲 Not started |
| 09-D | Gemini Nano (AICore) | 🔲 Not started |
| 09-E | ONNX Runtime Playground | 🔲 Not started |
| 09-F | Benchmarking & Device Comparison | 🔲 Not started |

---

## How to use this repo

Each module lives in its own folder (`module-01-multimodule-setup/`, etc.)
with a `README.md` (theory), Kotlin source code, and a PDF learning guide.

Work through them in order — each module adds a layer to the News Reader app.
By the end you will have a production-grade modular Android app with dynamic delivery.
