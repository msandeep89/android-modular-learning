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

**Overall progress: 0 / 9 modules complete**

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

### 🔲 Module 08 — Local Gen AI (MediaPipe + Gemini Nano)
- [ ] Theory: MediaPipe LLM Inference API vs Gemini Nano / AICore
- [ ] Theory: quantised models (INT4 vs FP32), model size vs quality trade-offs
- [ ] `:feature-ai-chat` dynamic-feature module setup
- [ ] `AndroidManifest.xml` with `<dist:on-demand/>`
- [ ] `ChatRepositoryFactory` — detect best available inference engine
- [ ] `MediaPipeChatRepository` — load Gemma 2B, `generateAsync` with token streaming
- [ ] `GeminiNanoChatRepository` — `GenerativeModel` with `sendMessageStream`
- [ ] `ChatViewModel` — manage model lifecycle, expose `StateFlow<ChatUiState>`
- [ ] `ChatFragment` — streaming chat UI with typing indicator
- [ ] Model caching — load once, reuse across messages
- [ ] RAM check — warn user if device has < 4 GB free
- [ ] Inference on `Dispatchers.IO` — never block main thread
- [ ] Test with Gemma 2B model on emulator / real device
- [ ] README + PDF learning guide

### 🔲 Module 09 — On-Device AI Model Explorer

#### 09-A: ML Kit Playground
- [ ] Theory: ML Kit architecture, on-device vs cloud models
- [ ] Text Classification — classify news article category
- [ ] Entity Extraction — extract URLs, phones, addresses from text
- [ ] Smart Reply — generate 3 suggestions for a message
- [ ] Language Identification — detect language + confidence
- [ ] Translation — download language pack, translate offline
- [ ] ChipGroup UI to switch between demos

#### 09-B: MediaPipe Playground
- [ ] Theory: MediaPipe Tasks API, BaseOptions, delegates
- [ ] CameraX integration with ImageAnalysis use case
- [ ] Object Detection — bounding boxes on live camera
- [ ] Face Landmark Detection — 478-point mesh overlay
- [ ] Hand Landmark Detection — finger skeleton overlay
- [ ] Gesture Recognition — classify hand gestures
- [ ] Live toggle between detectors without restarting camera

#### 09-C: TFLite Playground
- [ ] Theory: TFLite Interpreter, delegates (GPU, NNAPI, CPU)
- [ ] MobileNet V3 — image classification from gallery
- [ ] GPU delegate setup — measure speedup vs CPU
- [ ] MobileBERT — SMS spam detection
- [ ] Custom URL phishing model — feature engineering + inference
- [ ] Benchmark CPU vs GPU vs NNAPI inference time

#### 09-D: Gemini Nano (AICore)
- [ ] Theory: AICore, supported devices, availability check
- [ ] `GenerativeModel.checkAvailability()` + handle all states
- [ ] Graceful fallback card for unsupported devices
- [ ] Summarisation API — 3-bullet article summary
- [ ] Proofreading API — grammar correction
- [ ] Free-form chat with `sendMessageStream`
- [ ] Compare quality vs Gemma 2B (MediaPipe)

#### 09-E: ONNX Runtime Playground
- [ ] Theory: ONNX format, ONNX Runtime for Android setup
- [ ] Whisper Tiny — record audio, transcribe speech to text
- [ ] Mel spectrogram preprocessing for Whisper
- [ ] Sentence Transformer (all-MiniLM-L6) — semantic similarity
- [ ] Compare ONNX vs TFLite for same model type

#### 09-F: Benchmark Screen
- [ ] Fixed benchmark input (same text/image for all models)
- [ ] Cold start vs warm inference timing
- [ ] Memory usage per model (`Debug.MemoryInfo`)
- [ ] Results table: model | output | cold | warm | RAM | size
- [ ] Device capability matrix shown in-app
- [ ] Recommendation card based on use case

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
