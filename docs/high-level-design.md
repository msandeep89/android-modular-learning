# High Level Design — Android Modular Learning Project

This diagram shows the complete architecture of the News Reader app across all 7 modules.
Each layer builds on the previous — from Gradle setup to a signed AAB with dynamic delivery.

```mermaid
flowchart TD
    subgraph PLAY["Google Play Store"]
        AAB["Android App Bundle (.aab)"]
        DELIVERY["Play Feature Delivery"]
        AAB --> DELIVERY
    end

    subgraph DEVICE["User Device"]
        BASE["Base APK\n(:app + :core + install-time features)"]
        OD["On-demand download\n(:feature-bookmarks)"]
        COND["Conditional download\n(:feature-premium — API 26+ only)"]
    end

    DELIVERY -->|always| BASE
    DELIVERY -->|user requests| OD
    DELIVERY -->|device qualifies| COND

    subgraph APP["✅ :app module (Module 01 + 02)"]
        ACT["MainActivity\n(Single Activity)"]
        NAV["NavHostFragment\n(Navigation Component)"]
        BOTTOM["BottomNavigationView"]
        ACT --> NAV
        ACT --> BOTTOM
    end

    subgraph CORE["✅ :core module (Module 03 + 04)"]
        RETROFIT["Retrofit\nNews API Client"]
        MODELS["Shared Models\nArticle, Category"]
        RESULT["Result&lt;T&gt; + UiState&lt;T&gt;"]
        HILT_CORE["Hilt Modules\n@SingletonComponent"]
        RETROFIT --- MODELS
        MODELS --- RESULT
        RESULT --- HILT_CORE
    end

    subgraph HOME["✅ :feature-home (install-time)"]
        HOME_FRAG["HomeFragment"]
        HOME_VM["HomeViewModel\n@HiltViewModel"]
        HOME_FRAG --> HOME_VM
    end

    subgraph SEARCH["✅ :feature-search (install-time)"]
        SEARCH_FRAG["SearchFragment"]
        SEARCH_VM["SearchViewModel"]
        SEARCH_FRAG --> SEARCH_VM
    end

    subgraph BOOKMARKS["⚡ :feature-bookmarks (on-demand)"]
        SPLIT["SplitInstallManager\nrequest + monitor"]
        BOOK_FRAG["BookmarksFragment\n(only accessible after install)"]
        SPLIT --> BOOK_FRAG
    end

    subgraph PREMIUM["🔒 :feature-premium (conditional)"]
        CHECK["Check if module installed"]
        PREM_FRAG["PremiumFragment"]
        CHECK --> PREM_FRAG
    end

    BASE --> ACT
    NAV -->|navigate| HOME_FRAG
    NAV -->|navigate| SEARCH_FRAG
    NAV -->|after install| BOOK_FRAG
    NAV -->|if eligible| PREM_FRAG
    HOME_VM --> CORE
    SEARCH_VM --> CORE
```

---

## Module Dependency Rules

```mermaid
flowchart LR
    APP[":app"] --> CORE[":core"]
    APP --> HOME[":feature-home"]
    APP --> SEARCH[":feature-search"]
    APP --> BOOKMARKS[":feature-bookmarks\n(dynamic)"]
    APP --> PREMIUM[":feature-premium\n(dynamic)"]
    HOME --> CORE
    SEARCH --> CORE
    BOOKMARKS --> APP
    PREMIUM --> APP

    style BOOKMARKS fill:#fff3e0,stroke:#f57c00
    style PREMIUM fill:#fce4ec,stroke:#c2185b
```

> **Key rule:** Feature modules depend on `:core`, never on each other.
> Dynamic feature modules depend on `:app` (reverse of library modules).

---

## Delivery Mode Summary

| Module | Gradle plugin | Delivery | Downloaded |
|--------|--------------|----------|-----------|
| `:app` | `com.android.application` | Always | At install |
| `:core` | `com.android.library` | Always (merged into base) | At install |
| `:feature-home` | `com.android.library` | Install-time | At install |
| `:feature-search` | `com.android.library` | Install-time | At install |
| `:feature-bookmarks` | `com.android.dynamic-feature` | On-demand | When user requests |
| `:feature-premium` | `com.android.dynamic-feature` | Conditional | If API >= 26 |
