# Final Google Play Store Certification - Daadi

## 1. Executive Summary
This document certifies that the Daadi game application, including its build structures and user interactions, complies with the Google Play Store developer guidelines, including target SDK specifications, user privacy guidelines, and safety policies.

---

## 2. Play Store Policy Compliance Metrics

### 1. Target SDK & API Levels
- **Current Target SDK:** API Level 34 (Android 14) or higher.
- **Minimum SDK support:** Clean target matching ensuring compatibility with older devices while using modern system security contexts on newer OS iterations.

### 2. User Data Privacy (Data Safety)
- **Data Deletion Policy Compliance:** Fully supported via both:
  - User-facing account deletion configurations.
  - Standardized administrative user purging (`deleteUser`), which removes profile records and stats from active servers.
- **Privacy Agreement Reference:** Centralized resource mappings for terms of service, platform privacy agreements, and opt-out preferences.

### 3. Permissions & Device Attestation
- **Declaration Minimization:** No unnecessary background location, contacts, or hardware permissions requested in the `AndroidManifest.xml`.
- **Dynamic Context Prompts:** Any dynamic permission request is preceded by context-rich screens informing the user why the resource is required.

### 4. Ads Policy Compliance
- **Family Policy Compliance:** Ad SDK integration (configured via `AdManager` and `AdComponents`) satisfies demographic requirements, checking age metrics before displaying high-intensity or personalized ads.
- **No Intrusive Ads:** Ads are placed gracefully inside standard containers rather than interrupting critical gameplay loops or causing accidental clicks.
