# Final Admin Certification - Daadi Admin Dashboard

## 1. Executive Summary
The custom administrative console in the Daadi application serves as a single, cohesive command center for managing game configurations, players, matches, economies, security logs, and AI behavior. 

This certification documents the standardization of the Admin design system, security boundaries, and user interactions.

---

## 2. Standardized Admin Components
To optimize compilation size and prevent UI styling drift, the Admin UI has been standardized against `AdminDesignSystem.kt`:

### 1. `QuickStatCard`
- **Location:** `AdminDesignSystem.kt`
- **Purpose:** Displays core metrics on the Admin Dashboard and User details with contextual alert handling (e.g. red highlight for active bans/reports).
- **Consolidated From:** Replaced duplicated and hardcoded versions in `AdminDashboardScreen.kt`.

### 2. `MetricMiniCard`
- **Location:** `AdminDesignSystem.kt`
- **Purpose:** Standardized KPI display blocks for BI Analytics, Device Center, and Crash center widgets.
- **Consolidated From:** Replaced duplicate declarations in `AdminBIAnalyticsScreen.kt`.

### 3. List Item Keying
- **Location:** All Lazy lists (User directory, Appeals queue, Banned list, Actions audit log, Audit trail).
- **Optimization:** Converted from default position-based indexing to stable, unique entity ID keys (`key = { it.id }`), drastically reducing recompositions and frame-rate drops during scrolling.

---

## 3. High-Risk Action Guardrails (Audit & Rollback)
All sensitive administrative actions require step-by-step confirmation prompts to prevent accidental service disruptions or data loss:

1. **User Moderation:**
   - Account bans and shadow bans demand high-fidelity dynamic confirm/cancel dialogues with detailed user messaging.
2. **Permanent Data Purging:**
   - Permanent delete/purge commands (`deleteUser`) are protected by critical warnings stating that the action is completely irreversible.
3. **Session Invalidation:**
   - Forced logouts and session terminations require direct administrative confirmation before tokens are discarded.
4. **Economic Center Adjustments:**
   - Large coin and XP modifications are wrapped inside a transactional dialogue requiring validation of values before submitting.

---

## 4. RBAC Verification
The admin portal strictly enforces Role-Based Access Control on both client and database sides:
- **Client Side:** Screens are hidden or conditionalized using `adminViewModel.authRepository.userHasPermission(...)` matches.
- **Database Side:** System triggers compare the requesting user's dynamic roles inside the `user_roles` and `role_permissions` schema maps before writing updates.
