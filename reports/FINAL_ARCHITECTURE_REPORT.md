# Final Architecture Report

## Overview
The Daadi application follows a modern, scalable, and reactive Android architecture using Kotlin and Jetpack Compose. The architecture is primarily based on the Model-View-ViewModel (MVVM) pattern combined with a robust Clean Architecture inspired data layer.

## Core Components
1.  **UI Layer (Jetpack Compose):**
    *   Fully declarative UI using Material Design 3.
    *   State is observed using `collectAsStateWithLifecycle()` ensuring UI components only recompose when necessary and respect the Android lifecycle.
    *   Extensive use of `Scaffold`, `LazyColumn`, and custom adaptive layouts for responsive design across phones, foldables, and tablets.
    *   Dark Mode and Light Mode are fully supported via dynamic `MaterialTheme` color schemes.

2.  **Presentation Layer (ViewModels):**
    *   `GameViewModel`: Manages the core game loop, multiplayer state, and local game logic.
    *   `AdminViewModel`: Handles the complex state for the extensive admin dashboard.
    *   ViewModels expose immutable `StateFlow` to the UI, strictly separating state mutation from state consumption.

3.  **Data Layer (Repositories & Managers):**
    *   `SupabaseManager`: Acts as the central hub for all remote data operations, encapsulating authentication, database queries, real-time subscriptions, and edge functions.
    *   `MultiplayerManager`: Specifically handles low-latency real-time interactions for multiplayer matches.
    *   `AdManager`: Encapsulates Google AdMob logic, handling initialization, pre-loading, and display of Interstitial and Rewarded ads.

4.  **Dependency Injection:**
    *   Dependencies are managed via Constructor Injection and a centralized `ViewModelFactory` in `DaadiApplication`, keeping the setup lightweight while maintaining testability.

## Concurrency & Threading
*   Kotlin Coroutines and Flows are used extensively.
*   Network operations are safely moved to the `Dispatchers.IO` thread using custom `runOnMain` and Coroutine scope wrappers in the network client.
*   UI updates are strictly confined to the Main thread.

## Scalability
*   The architecture is modularized by feature within the package structure (`ui`, `data`, `viewmodel`).
*   The `SupabaseManager` is designed to be split into domain-specific repositories if the file size grows further, ensuring long-term maintainability.
