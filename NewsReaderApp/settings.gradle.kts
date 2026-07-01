pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "NewsReaderApp"

// ── Static modules (always installed) ────────────────────────────────────────
include(":app")
include(":core")
include(":feature-home")
include(":feature-search")

// ── Dynamic feature modules (downloaded on demand / conditionally) ────────────
include(":feature-bookmarks")    // on-demand
include(":feature-premium")      // conditional (API 26+ only)
include(":feature-ai-chat")      // on-demand (includes LLM model)
include(":feature-ai-explorer")  // on-demand (AI model playground)
