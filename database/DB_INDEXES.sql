-- ============================================================================
-- HIGH-PERFORMANCE POSTGRESQL DATABASE INDEXES FOR DAADI APP (SUPABASE)
-- Designed for Scale: 100 -> 1,000,000+ Users
-- ============================================================================
--
-- Author: Principal Backend Architect & PostgreSQL DBA
-- Purpose: Optimize database read/write queries to run in O(log N) or better.
--          Prevent table scans, eliminate thread locks, and reduce CPU usage on Supabase.

-- ----------------------------------------------------------------------------
-- 1. USERS TABLE OPTIMIZATIONS
-- ----------------------------------------------------------------------------
-- Ensure O(1) lightning-fast auth & profile lookups by unique attributes.
CREATE UNIQUE INDEX IF NOT EXISTS idx_users_email_uniq ON public.users (email);
CREATE UNIQUE INDEX IF NOT EXISTS idx_users_username_uniq ON public.users (username);

-- Optimize the administrative global user dashboard sorting by registration date.
CREATE INDEX IF NOT EXISTS idx_users_created_at_desc ON public.users ("createdAt" DESC);

-- Optimize analytics and matchmaking stats query lookups.
CREATE INDEX IF NOT EXISTS idx_users_wins_losses ON public.users (wins DESC, losses DESC);

-- ----------------------------------------------------------------------------
-- 2. MATCHES TABLE OPTIMIZATIONS
-- ----------------------------------------------------------------------------
-- Optimize multiplayer matchmaking. Clients fetch "waiting" matches continuously.
-- This partial index ensures index size remains minimal by indexing only open lobbies.
CREATE INDEX IF NOT EXISTS idx_matches_matchmaking_waiting 
ON public.matches (status) 
WHERE status = 'waiting';

-- Optimize matchmaking and game history list retrieval for specific hosts and opponents.
CREATE INDEX IF NOT EXISTS idx_matches_host_id ON public.matches (host_id);
CREATE INDEX IF NOT EXISTS idx_matches_opponent_id ON public.matches (opponent_id);

-- Optimize game sorting for admin match panel.
CREATE INDEX IF NOT EXISTS idx_matches_created_at_desc ON public.matches ("createdAt" DESC);

-- ----------------------------------------------------------------------------
-- 3. BANS TABLE OPTIMIZATIONS
-- ----------------------------------------------------------------------------
-- Optimize check-ban-status queries executed on user login.
CREATE INDEX IF NOT EXISTS idx_bans_user_id_active ON public.bans (user_id) WHERE is_active = true;

-- Optimize historic sorting for administrative bans panel.
CREATE INDEX IF NOT EXISTS idx_bans_created_at_desc ON public.bans (created_at DESC);

-- ----------------------------------------------------------------------------
-- 4. REPORTS TABLE OPTIMIZATIONS
-- ----------------------------------------------------------------------------
-- Optimize loading user-reported logs for a specific target profile.
CREATE INDEX IF NOT EXISTS idx_reports_reported_id ON public.reports (reported_id);

-- Optimize chronological sorting for active report tickets.
CREATE INDEX IF NOT EXISTS idx_reports_created_at_desc ON public.reports (created_at DESC);

-- ----------------------------------------------------------------------------
-- 5. FEEDBACK TABLE OPTIMIZATIONS
-- ----------------------------------------------------------------------------
-- Optimize sorting feedback tickets chronologically.
CREATE INDEX IF NOT EXISTS idx_feedback_id_desc ON public.feedback (id DESC);

-- ----------------------------------------------------------------------------
-- 6. SYSTEM AUDIT LOGS OPTIMIZATIONS
-- ----------------------------------------------------------------------------
-- Optimize administrative timeline dashboard.
CREATE INDEX IF NOT EXISTS idx_audit_event_logs_created_at_desc ON public.audit_event_logs (created_at DESC);
CREATE INDEX IF NOT EXISTS idx_audit_logs_timestamp_desc ON public.audit_logs (created_at DESC);
