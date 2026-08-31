# Final Database Certification - Daadi

## 1. Relational Database Architecture
Daadi utilizes a relational PostgreSQL engine hosted on Supabase, structured around game models, player authentication profiles, and historical log ledgers.

The database architecture has been audited and certified against robust relational schemas (`schema.sql`) and optimized performance layers (`DB_INDEXES.sql`).

---

## 2. Forensic Schema Analysis

### Core Tables & Foreign Keys
- **`users` Table:** Serves as the central user profile entity. Linked to Supabase Auth UUIDs.
- **`matches` Table:** Tracks game session histories. References participating user IDs with relational foreign keys.
- **`audit_logs` Table:** Structured with absolute relational bindings:
  - References `users.id` as `user_id` to trace administrative actors.
  - Cascade configurations protect database integrity (if a user is purged, historical logs may preserve transaction records or cascade based on archival laws).
- **`user_roles` & `role_permissions` Tables:** Direct relationship mappings ensuring exact query joins during role checks.

---

## 3. High-Performance Indexing Layer
To support sub-millisecond query results during peak concurrency, indices have been implemented across high-frequency retrieval columns:

1. **User Scanning:**
   - Indices on `users(username)` and `users(email)` for high-speed lookups during logins and admin searches.
2. **Match History:**
   - Composite indices on match lists supporting search filters (e.g., matching player queries).
3. **Audit Ledger:**
   - Index on `audit_logs(created_at DESC)` and `audit_logs(user_id)` to speed up security analysis on large records.
4. **Appeals & Ban Queues:**
   - High-performance indexes on filterable columns like `is_banned`, `status`, and reference dates.

---

## 4. DB Constraints, Triggers & Functions
- **Triggers:** Automatic system level auditing of modifications to critical tables.
- **Constraints:** Relational constraints ensure coin/XP configurations cannot hold invalid (e.g., negative coin bounds where prohibited) values.
- **RLS (Row Level Security):** Ensures standard users cannot modify tables containing administrative configurations, role assignments, or audit logs directly from the client.
