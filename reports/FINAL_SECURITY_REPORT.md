# Final Security Report

## Authentication & Identity
*   **Provider:** Supabase Auth (JWT-based).
*   **Session Management:** Handled natively by the Supabase Kotlin SDK. Tokens are securely stored in EncryptedSharedPreferences on the device.
*   **Role-Based Access Control (RBAC):** Implemented in the database and enforced at the UI level. Admin screens dynamically hide/show based on the user's explicit roles (e.g., `view_users`, `manage_economy`, `moderate_users`).

## Data Protection (RLS)
*   Row Level Security (RLS) is required on all Supabase tables.
*   **Users Table:** Users can only read/update their own profile data. Admins have a bypass via a secure service role or specific RLS admin policies.
*   **Matches Table:** Participants can read/write to active matches they are part of.
*   **Economy Table:** Strictly server-authoritative. Clients cannot write to currency balances; all transactions route through secure Edge Functions.

## Network Security
*   All communication over HTTPS (TLS 1.2+).
*   No hardcoded secrets in the source tree. `BuildConfig` is used to inject the `SUPABASE_URL` and `SUPABASE_ANON_KEY`.

## Anti-Cheat & Integrity
*   Client-side validation is supplemented by server-side verification.
*   Admin dashboard includes an Anti-Cheat monitor to track suspicious Win/Loss ratios, rapid connection toggles, and abnormal currency accumulation.

## Privacy & Compliance
*   No explicit PII (Personally Identifiable Information) is logged locally.
*   GDPR/CCPA deletion flows are integrated via the "Delete Account" option in the user profile, which cascades deletions across the database.
