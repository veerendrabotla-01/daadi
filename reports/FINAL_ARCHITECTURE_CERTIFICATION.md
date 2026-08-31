# Final Architecture Certification - Daadi

## 1. Architectural Model (Clean MVVM)
The Daadi codebase implements a highly structured, scalable MVVM (Model-View-ViewModel) architecture. Architectural layers maintain strict boundary separations:

```
┌────────────────────────────────────────────────────────┐
│                   Presentation Layer                   │
│  (Jetpack Compose UI, Themes, Admin Navigator/Screens) │
└───────────────────────────┬────────────────────────────┘
                            ▼
┌────────────────────────────────────────────────────────┐
│                      ViewModel                         │
│       (ViewModels, AdminViewModel, StateFlows)         │
└───────────────────────────┬────────────────────────────┘
                            ▼
┌────────────────────────────────────────────────────────┐
│                    Repository Layer                    │
│   (GameRepository, SupabaseRepositories, Caching)      │
└───────────────────────────┬────────────────────────────┘
                            ▼
┌────────────────────────────────────────────────────────┐
│                    Data Source Layer                   │
│    (SupabaseManager, SQLite Database, NetworkUtils)    │
└────────────────────────────────────────────────────────┘
```

---

## 2. Forensic Code Boundaries

### 1. View Layer (Presentation)
- Fully written in declarative Jetpack Compose.
- Does not contain database connection parameters or direct SQL references.
- Reads states via state flows and maps interactions directly to ViewModel function triggers.

### 2. ViewModel Layer (State & Orchestration)
- Governs state preservation.
- Exposes immutable StateFlow properties to the UI.
- Coordinates calls across multiple specialized repository subsystems (User directory, config center, analytics manager, safety desk).

### 3. Repository Layer (Data Abstraction)
- Hides the implementation details of whether data is sourced via active WebSockets, Supabase REST requests, or local cached database indices.
- Performs domain translations, protecting presentation code from parsing or modeling anomalies.

### 4. Data Layer (Data Sources)
- Governs connections to the server (`SupabaseManager`) and local SQLite/Room caches.
- Restructures raw payloads into structured model data objects.
