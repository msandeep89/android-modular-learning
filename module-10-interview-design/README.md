# Module 10 — Android Interview Design Questions

> Targeted at **10+ year senior Android engineers** — Staff, Principal, Architect, EM roles.

This module simulates the most frequently asked design and architecture questions
in Android interviews at product companies (Google, Meta, Uber, Swiggy, PhonePe, CRED, etc.)

Each sub-module has:
- The interview question exactly as it's asked
- What the interviewer is actually testing
- A working Kotlin implementation you can run and explain
- Common follow-up questions with answers

---

## Sub-modules

| Sub-module | Topic | Difficulty |
|-----------|-------|-----------|
| [10-A](10-A-architecture-patterns/) | Architecture Patterns (MVVM, MVI, Clean Architecture) | ⭐⭐⭐ |
| [10-B](10-B-design-patterns/) | Design Patterns in Android (Singleton, Factory, Observer, Strategy) | ⭐⭐⭐ |
| [10-C](10-C-system-design/) | Android System Design (Feed, Chat, Offline-first, Search, Video) | ⭐⭐⭐⭐⭐ |
| [10-D](10-D-concurrency-patterns/) | Concurrency Patterns (StateFlow, SharedFlow, Channels, Structured Concurrency) | ⭐⭐⭐⭐ |
| [10-E](10-E-performance-patterns/) | Performance Patterns (DiffUtil, Paging 3, Memory Leaks, Profiling) | ⭐⭐⭐⭐ |

---

## How to use this module

1. Read the question at the top of each file — answer it out loud first
2. Then read the implementation and compare with your mental model
3. Read the "What the interviewer tests" section — know what signals they look for
4. Practice the follow-up questions — these decide if you get Staff vs Senior offer

---

## Most asked questions by role

### Senior Android Engineer
- MVVM vs MVI — when to use which?
- Design the offline-first news feed
- Explain structured concurrency with coroutines
- How does DiffUtil work internally?
- StateFlow vs SharedFlow vs LiveData — when to use each?

### Staff / Principal Android Engineer
- Design a real-time chat system (client architecture only)
- How would you design a multi-module architecture for a super-app?
- Design an image loading library from scratch
- How do you prevent memory leaks in a ViewPager with Fragments?
- How would you implement a plugin architecture in Android?

### Android Architect / EM
- How do you decide module boundaries in a large Android codebase?
- Design the architecture for a payments SDK used by 50 apps
- How do you enforce architecture rules across 20 engineers?
- How would you migrate a monolith app to multi-module without downtime?
- Design a feature flag system for Android
