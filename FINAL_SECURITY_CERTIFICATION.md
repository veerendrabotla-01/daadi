# Final Security Certification - Daadi

## 1. Security Architecture
The security architecture of Daadi is built upon a defense-in-depth model that spans the client application, network channel, and remote database. Client-side RBAC acts as a primary user experience optimization, while database-level Row Level Security (RLS) policies serve as the final, absolute security boundary.

---

## 2. Threat Vector Remediation & Verification

### 1. Authentication & Session Integrity
- **Authentication:** Managed securely via Supabase JWT Auth. Session tokens are checked and refreshed automatically.
- **Session Manager:** Active administrator sessions can be reviewed, verified, or revoked immediately via the "Active Logins" monitor screen.
- **Force Logout:** Administrators have the direct authority to revoke active tokens on the authentication server, forcing the target device to return to the landing portal.

### 2. Client Security & Privacy Protection
- **Device Attestation:** The system logs device attributes, identifying rooted devices, custom ROMs, or emulator environments.
- **Secure Logging:** Standardized secure logging frameworks block any leakage of sensitive information (such as password parameters, auth tokens, or session IDs) to logcat.
- **Data Erasure Compliance:** Complies with privacy requirements by offering a permanent user-initiated and administrator-approved user data purge option (`deleteUser`), wiping statistics, match logs, and personal details.

### 3. Role-Based Access Control (RBAC)
- Client-side navigation nodes (`AdminDashboardScreen`) check permissions before exposing paths:
  - `moderate_users`
  - `view_users`
  - `manage_admins`
  - `manage_config`
  - `view_analytics`
  - `manage_matches`
  - `manage_announcements`
  - `view_logs`
  - `view_system_health`
  - `view_audit_logs`
- DB permissions mapping inside tables `permissions`, `roles`, `role_permissions`, and `user_roles` blocks unauthorized modifications.

### 4. Integrity Checks & Cheat Protection
- **Anti-Cheat Logs:** Real-time logging of game speed variations, memory modifications, or unusual board state transitions.
- **Fraud Detection:** Dynamic fraud alerts flag multi-account users, matching device IDs across different usernames, or unexpected coin generation streaks.
