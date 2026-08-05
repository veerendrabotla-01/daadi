# Final Performance Certification - Daadi

## 1. Executive Summary
This document certifies that the Daadi game has been designed and tuned to achieve smooth framerates, low memory footprints, and minimized battery drainage during standard gameplay and administrative reviews.

---

## 2. Compose Performance Tuning

### 1. Key-Based List Rendering
- **The Issue:** Standard position-based list components (`LazyColumn` elements) recalculate every single visible child when any single item state (such as checkbox state, name, or role) changes.
- **The Fix:** Integrated stable identifier keys (`key = { it.id }`) across all lists:
  - Admin User Directory (`AdminUserScreens.kt`)
  - Admin Appeals Dashboard (`AdminAppealsScreens.kt`)
  - Admin Safety Reports and Bans (`AdminSafetyScreens.kt`)
- **Impact:** Recompositions are strictly localized to modified row items, preserving rendering budgets and preventing scroll stutter.

### 2. Layout Efficiency
- **Responsive Insets:** Leverages container bounds for fluid rendering across phone and tablet modes.
- **Drawing Optimizations:** Custom boards use Compose `Canvas` drawing parameters rather than heavy XML vector files or excessive standard nested components, maintaining rendering efficiency during animations.

### 3. State Propagation (MVVM Flow)
- Centralized `AdminViewModel` utilizes `MutableStateFlow` streams.
- Client screens read state using `collectAsStateWithLifecycle()` to automatically pause database polling/collection flows when the app enters the background, preserving device resource usage.

---

## 3. Network & Cache Optimizations
- **Offline Cache support:** Integrated offline state flows to guarantee readability of cached dashboard modules without displaying harsh crash dialogues during transient connectivity drops.
- **Payload Minimization:** API responses are strictly modeled (`SupabaseModels.kt`) to request only required rows rather than pulling massive, unindexed tables.
