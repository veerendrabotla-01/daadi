-- DAADI APP FULL DATABASE SCHEMA
-- This script sets up all required tables and default configurations for the Daadi Android Application.
-- Run this in the Supabase SQL Editor.

-- 1. USERS TABLE
-- Stores profile information, statistics, and moderation status for all players.
CREATE TABLE IF NOT EXISTS public.users (
    id UUID PRIMARY KEY,
    username TEXT NOT NULL,
    email TEXT NOT NULL,
    role TEXT NOT NULL DEFAULT 'player', -- owner, super_admin, admin, moderator, support, community_manager, analytics, finance, read_only, player
    "createdAt" TEXT NOT NULL,
    "totalGames" INTEGER DEFAULT 0,
    wins INTEGER DEFAULT 0,
    losses INTEGER DEFAULT 0,
    "isBanned" BOOLEAN DEFAULT false,
    "isReported" BOOLEAN DEFAULT false,
    "reportsCount" INTEGER DEFAULT 0,
    mfa_enabled BOOLEAN DEFAULT false,
    last_login TIMESTAMP WITH TIME ZONE,
    ip_address TEXT,
    country_code TEXT,
    device_info JSONB,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- 2. MATCHES TABLE
-- Stores history of all games played online.
-- Note: id is TEXT because it can be a 6-digit Room Code or a generated ID.
CREATE TABLE IF NOT EXISTS public.matches (
    id TEXT PRIMARY KEY,
    "hostName" TEXT NOT NULL,
    "opponentName" TEXT NOT NULL,
    status TEXT NOT NULL DEFAULT 'waiting', -- 'waiting', 'playing', 'finished'
    winner TEXT,
    "movesCount" INTEGER DEFAULT 0,
    "createdAt" TEXT NOT NULL,
    "movesJson" TEXT
);

-- Ensure matches.id is TEXT (in case it was previously created as UUID in some environments)
ALTER TABLE public.matches ALTER COLUMN id TYPE TEXT USING id::text;

-- 3. ANNOUNCEMENTS TABLE
CREATE TABLE IF NOT EXISTS public.announcements (
    id BIGSERIAL PRIMARY KEY,
    title TEXT NOT NULL,
    content TEXT NOT NULL,
    "isActive" BOOLEAN DEFAULT true,
    "createdAt" TEXT NOT NULL
);

-- 4. SYSTEM SETTINGS TABLE
CREATE TABLE IF NOT EXISTS public.system_settings (
    key TEXT PRIMARY KEY,
    value TEXT NOT NULL,
    description TEXT
);

-- 5. AD CONFIGURATION TABLE
CREATE TABLE IF NOT EXISTS public.ad_configuration (
    id BIGINT PRIMARY KEY,
    "activeProvider" TEXT DEFAULT 'ADMOB',
    "bannerAdUnitId" TEXT,
    "interstitialAdUnitId" TEXT,
    "rewardedAdUnitId" TEXT,
    "interstitialFrequencyCap" INTEGER DEFAULT 3,
    "isMonetizationGlobalOverride" BOOLEAN DEFAULT true
);

-- 6. FEEDBACK TABLE
CREATE TABLE IF NOT EXISTS public.feedback (
    id BIGSERIAL PRIMARY KEY,
    username TEXT NOT NULL,
    content TEXT NOT NULL,
    category TEXT NOT NULL, -- 'bug', 'suggest', 'other'
    "createdAt" TEXT NOT NULL
);

-- 7. AUDIT TRAIL (Enhanced)
CREATE TABLE IF NOT EXISTS public.audit_logs (
    id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    actor_id UUID REFERENCES public.users(id),
    action_type TEXT NOT NULL, -- CREATE, UPDATE, DELETE, BAN, LOGIN, etc.
    target_table TEXT,
    target_id TEXT,
    old_value JSONB,
    new_value JSONB,
    reason TEXT,
    ip_address TEXT,
    user_agent TEXT,
    country TEXT,
    device_info TEXT,
    screen_name TEXT,
    session_id UUID,
    rollback_id UUID,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT timezone('utc'::text, now()) NOT NULL
);

-- 7.1 ADMIN SESSIONS
CREATE TABLE IF NOT EXISTS public.admin_sessions (
    id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    user_id UUID REFERENCES public.users(id) ON DELETE CASCADE,
    admin_id UUID REFERENCES public.users(id) ON DELETE CASCADE,
    ip_address TEXT,
    user_agent TEXT,
    last_active TIMESTAMP WITH TIME ZONE DEFAULT timezone('utc'::text, now()),
    is_suspicious BOOLEAN DEFAULT false,
    terminated_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT timezone('utc'::text, now()) NOT NULL
);

-- 8. SECURITY POLICIES (RLS)
-- Enable RLS for all production tables
ALTER TABLE public.users ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.matches ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.announcements ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.system_settings ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.ad_configuration ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.feedback ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.audit_logs ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.admin_sessions ENABLE ROW LEVEL SECURITY;

-- 9. DEFAULT SEED DATA
INSERT INTO public.system_settings (key, value, description)
VALUES 
('ads_launcher', 'on', 'Toggle for in-app advertisements display'),
('maintenance_mode', 'off', 'If on, prevents non-admin users from accessing multiplayer'),
('announcement_text', 'Welcome to the Daadi Pro Multiplayer Arena!', 'Global ticker text shown on the home screen'),
('ad_consent_force_eea_debug', 'off', 'Force EEA Geography for testing GDPR UMP consent form in debug builds'),
('xp_multiplier', '1.5', 'Global multiplier for game XP rewards'),
('coin_multiplier', '1.0', 'Global multiplier for coin rewards'),
('matchmaking_enabled', 'on', 'Toggle for multiplayer ranked matchmaking'),
('chat_enabled', 'on', 'Toggle for in-game and lobby text chat'),
('min_supported_version', '100', 'Minimum Android client version allowed to play'),
('latest_app_version', '1.0.0', 'Current latest client version published in stores')
ON CONFLICT (key) DO NOTHING;

INSERT INTO public.ad_configuration (id, "activeProvider", "bannerAdUnitId", "interstitialAdUnitId", "rewardedAdUnitId", "interstitialFrequencyCap", "isMonetizationGlobalOverride")
VALUES (1, 'ADMOB', 'ca-app-pub-3940256099942544/6300978111', 'ca-app-pub-3940256099942544/1033173712', 'ca-app-pub-3940256099942544/5224354917', 3, true)
ON CONFLICT (id) DO NOTHING;

INSERT INTO public.announcements (title, content, "isActive", "createdAt")
VALUES ('Welcome!', 'Welcome to the official Daadi online community. Have fun and play fair!', true, NOW()::text)
ON CONFLICT DO NOTHING;

-- ==========================================
-- PRODUCTION UPGRADE: RBAC, SECURITY & AUDIT
-- ==========================================

-- 1. RBAC SYSTEM TABLES
CREATE TABLE IF NOT EXISTS public.roles (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name TEXT UNIQUE NOT NULL,
    description TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS public.permissions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name TEXT UNIQUE NOT NULL,
    description TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS public.role_permissions (
    role_id UUID REFERENCES public.roles(id) ON DELETE CASCADE,
    permission_id UUID REFERENCES public.permissions(id) ON DELETE CASCADE,
    PRIMARY KEY (role_id, permission_id)
);

CREATE TABLE IF NOT EXISTS public.user_roles (
    user_id UUID REFERENCES public.users(id) ON DELETE CASCADE,
    role_id UUID REFERENCES public.roles(id) ON DELETE CASCADE,
    PRIMARY KEY (user_id, role_id)
);

-- 2. EXTEND EXISTING TABLES
ALTER TABLE public.users ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW();
ALTER TABLE public.users ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMP WITH TIME ZONE;
ALTER TABLE public.users ADD COLUMN IF NOT EXISTS last_login TIMESTAMP WITH TIME ZONE;
ALTER TABLE public.users ADD COLUMN IF NOT EXISTS avatar_url TEXT;
ALTER TABLE public.users ADD COLUMN IF NOT EXISTS country_code TEXT;
ALTER TABLE public.users ADD COLUMN IF NOT EXISTS metadata JSONB DEFAULT '{}'::jsonb;

ALTER TABLE public.matches ADD COLUMN IF NOT EXISTS host_id UUID;
ALTER TABLE public.matches ADD COLUMN IF NOT EXISTS opponent_id UUID;
ALTER TABLE public.matches ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW();

ALTER TABLE public.announcements ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW();

-- 3. HELPER FUNCTIONS FOR SECURITY
CREATE OR REPLACE FUNCTION public.is_admin()
RETURNS BOOLEAN AS $$
BEGIN
  RETURN (
    SELECT (role IN ('admin', 'superadmin')) FROM public.users WHERE id = auth.uid()
  );
END;
$$ LANGUAGE plpgsql SECURITY DEFINER SET search_path = public;

CREATE OR REPLACE FUNCTION public.is_moderator()
RETURNS BOOLEAN AS $$
BEGIN
  RETURN (
    SELECT (role IN ('moderator', 'admin', 'superadmin')) FROM public.users WHERE id = auth.uid()
  );
END;
$$ LANGUAGE plpgsql SECURITY DEFINER SET search_path = public;

CREATE OR REPLACE FUNCTION public.is_support()
RETURNS BOOLEAN AS $$
BEGIN
  RETURN (
    SELECT (role IN ('support', 'moderator', 'admin', 'superadmin')) FROM public.users WHERE id = auth.uid()
  );
END;
$$ LANGUAGE plpgsql SECURITY DEFINER SET search_path = public;

DROP FUNCTION IF EXISTS public.has_permission(TEXT);
CREATE OR REPLACE FUNCTION public.has_permission(perm_name TEXT)
RETURNS BOOLEAN AS $$
BEGIN
  RETURN EXISTS (
    SELECT 1 FROM public.user_roles ur
    JOIN public.role_permissions rp ON ur.role_id = rp.role_id
    JOIN public.permissions p ON rp.permission_id = p.id
    WHERE ur.user_id = auth.uid() AND p.name = perm_name
  ) OR (
    SELECT (role IN ('admin', 'superadmin')) FROM public.users WHERE id = auth.uid()
  );
END;
$$ LANGUAGE plpgsql SECURITY DEFINER SET search_path = public;

-- 4. RBAC SEED DATA
DO $$
DECLARE
    role_admin_id UUID;
    role_superadmin_id UUID;
    role_mod_id UUID;
    role_support_id UUID;
    role_user_id UUID;
    p TEXT;
    perms TEXT[] := ARRAY[
        'manage_users', 'manage_matches', 'manage_reports', 'manage_ads', 
        'manage_announcements', 'manage_flags', 'manage_remote_config', 
        'manage_roles', 'manage_permissions', 'view_audit_logs', 
        'delete_accounts', 'export_user_data', 'maintenance_control'
    ];
BEGIN
    INSERT INTO public.roles (name, description) VALUES 
    ('user', 'Standard player'),
    ('moderator', 'Can handle reports and bans'),
    ('support', 'Can assist users and view logs'),
    ('admin', 'Full system management'),
    ('superadmin', 'Total control including RBAC')
    ON CONFLICT (name) DO NOTHING;

    SELECT id INTO role_user_id FROM public.roles WHERE name = 'user';
    SELECT id INTO role_mod_id FROM public.roles WHERE name = 'moderator';
    SELECT id INTO role_support_id FROM public.roles WHERE name = 'support';
    SELECT id INTO role_admin_id FROM public.roles WHERE name = 'admin';
    SELECT id INTO role_superadmin_id FROM public.roles WHERE name = 'superadmin';

    FOREACH p IN ARRAY perms LOOP
        INSERT INTO public.permissions (name) VALUES (p) ON CONFLICT (name) DO NOTHING;
    END LOOP;

    INSERT INTO public.role_permissions (role_id, permission_id)
    SELECT role_admin_id, id FROM public.permissions ON CONFLICT DO NOTHING;

    INSERT INTO public.role_permissions (role_id, permission_id)
    SELECT role_superadmin_id, id FROM public.permissions ON CONFLICT DO NOTHING;

    INSERT INTO public.role_permissions (role_id, permission_id)
    SELECT role_mod_id, id FROM public.permissions WHERE name IN ('manage_reports', 'manage_matches') ON CONFLICT DO NOTHING;
END $$;

-- 5. ENABLE ROW LEVEL SECURITY
ALTER TABLE public.users ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.matches ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.announcements ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.system_settings ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.ad_configuration ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.feedback ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.audit_logs ENABLE ROW LEVEL SECURITY;

-- 6. SECURITY POLICIES

-- USERS POLICIES
DROP POLICY IF EXISTS "Public profiles are viewable by everyone" ON public.users;
CREATE POLICY "Public profiles are viewable by everyone" ON public.users FOR SELECT USING (deleted_at IS NULL);

DROP POLICY IF EXISTS "Users can update own profile" ON public.users;
CREATE POLICY "Users can update own profile" ON public.users FOR UPDATE USING (auth.uid() = id);

DROP POLICY IF EXISTS "Admins can manage all profiles" ON public.users;
CREATE POLICY "Admins can manage all profiles" ON public.users FOR ALL USING (public.is_admin());

-- MATCHES POLICIES
DROP POLICY IF EXISTS "Matches are viewable by participants" ON public.matches;
DROP POLICY IF EXISTS "Matches are viewable by everyone" ON public.matches;
CREATE POLICY "Matches are viewable by everyone" ON public.matches FOR SELECT USING (true);

DROP POLICY IF EXISTS "Anyone can insert matches" ON public.matches;
CREATE POLICY "Anyone can insert matches" ON public.matches FOR INSERT WITH CHECK (true);

DROP POLICY IF EXISTS "Only secure participants can update matches" ON public.matches;
DROP POLICY IF EXISTS "Anyone can update matches" ON public.matches;
CREATE POLICY "Anyone can update matches" ON public.matches FOR UPDATE USING (true);

DROP POLICY IF EXISTS "Anyone can delete matches" ON public.matches;
CREATE POLICY "Anyone can delete matches" ON public.matches FOR DELETE USING (true);

-- ANNOUNCEMENTS POLICIES
DROP POLICY IF EXISTS "Announcements are public" ON public.announcements;
CREATE POLICY "Announcements are public" ON public.announcements FOR SELECT USING ("isActive" = true OR public.is_support());

DROP POLICY IF EXISTS "Only admins manage announcements" ON public.announcements;
CREATE POLICY "Only admins manage announcements" ON public.announcements FOR ALL USING (public.is_admin());

-- SYSTEM SETTINGS POLICIES
DROP POLICY IF EXISTS "System settings are public" ON public.system_settings;
CREATE POLICY "System settings are public" ON public.system_settings FOR SELECT USING (true);

DROP POLICY IF EXISTS "Only admins manage system settings" ON public.system_settings;
CREATE POLICY "Only admins manage system settings" ON public.system_settings FOR ALL USING (public.is_admin());

-- AD CONFIGURATION POLICIES
DROP POLICY IF EXISTS "Ad configuration is public" ON public.ad_configuration;
CREATE POLICY "Ad configuration is public" ON public.ad_configuration FOR SELECT USING (true);

DROP POLICY IF EXISTS "Only admins manage ad config" ON public.ad_configuration;
CREATE POLICY "Only admins manage ad config" ON public.ad_configuration FOR ALL USING (public.is_admin());

-- FEEDBACK POLICIES
DROP POLICY IF EXISTS "Users can create feedback" ON public.feedback;
CREATE POLICY "Users can create feedback" ON public.feedback FOR INSERT WITH CHECK (true);

DROP POLICY IF EXISTS "Users view own feedback" ON public.feedback;
CREATE POLICY "Users view own feedback" ON public.feedback FOR SELECT USING (
    EXISTS (SELECT 1 FROM public.users u WHERE u.username = public.feedback.username AND u.id = auth.uid()) 
    OR public.is_support()
);

-- AUDIT LOGS POLICIES
DROP POLICY IF EXISTS "Admins can view audit logs" ON public.audit_logs;
CREATE POLICY "Admins can view audit logs" ON public.audit_logs FOR SELECT USING (public.is_admin());

-- 7. AUDIT LOGGING SYSTEM (V2)
CREATE TABLE IF NOT EXISTS public.audit_event_logs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    actor_id UUID,
    table_name TEXT NOT NULL,
    action_type TEXT NOT NULL,
    record_id TEXT NOT NULL,
    old_data JSONB,
    new_data JSONB,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

ALTER TABLE public.audit_event_logs ENABLE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS "Admins can view audit event logs" ON public.audit_event_logs;
CREATE POLICY "Admins can view audit event logs" ON public.audit_event_logs FOR SELECT USING (public.is_admin());

CREATE OR REPLACE FUNCTION public.audit_trigger_func()
RETURNS TRIGGER AS $$
DECLARE
    old_val JSONB := NULL;
    new_val JSONB := NULL;
    rec_id TEXT;
BEGIN
    IF (TG_OP = 'DELETE') THEN
        old_val := to_jsonb(OLD);
    ELSIF (TG_OP = 'UPDATE') THEN
        old_val := to_jsonb(OLD);
        new_val := to_jsonb(NEW);
    ELSIF (TG_OP = 'INSERT') THEN
        new_val := to_jsonb(NEW);
    END IF;

    CASE TG_TABLE_NAME
        WHEN 'system_settings' THEN
            IF (TG_OP = 'DELETE') THEN rec_id := OLD.key; ELSE rec_id := NEW.key; END IF;
        ELSE
            IF (TG_OP = 'DELETE') THEN rec_id := CAST(OLD.id AS TEXT); ELSE rec_id := CAST(NEW.id AS TEXT); END IF;
    END CASE;

    INSERT INTO public.audit_event_logs (actor_id, table_name, action_type, record_id, old_data, new_data)
    VALUES (auth.uid(), TG_TABLE_NAME, TG_OP, rec_id, old_val, new_val);

    RETURN NULL;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- APPLY AUDIT TRIGGERS
DROP TRIGGER IF EXISTS audit_users_trigger ON public.users;
CREATE TRIGGER audit_users_trigger AFTER INSERT OR UPDATE OR DELETE ON public.users FOR EACH ROW EXECUTE FUNCTION public.audit_trigger_func();

DROP TRIGGER IF EXISTS audit_matches_trigger ON public.matches;
CREATE TRIGGER audit_matches_trigger AFTER INSERT OR UPDATE OR DELETE ON public.matches FOR EACH ROW EXECUTE FUNCTION public.audit_trigger_func();

DROP TRIGGER IF EXISTS audit_settings_trigger ON public.system_settings;
CREATE TRIGGER audit_settings_trigger AFTER INSERT OR UPDATE OR DELETE ON public.system_settings FOR EACH ROW EXECUTE FUNCTION public.audit_trigger_func();

-- 8. AUTOMATIC TIMESTAMP MANAGEMENT
CREATE OR REPLACE FUNCTION public.handle_updated_at()
RETURNS TRIGGER AS $$
BEGIN
  NEW.updated_at = NOW();
  RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS set_updated_at_users ON public.users;
CREATE TRIGGER set_updated_at_users BEFORE UPDATE ON public.users FOR EACH ROW EXECUTE FUNCTION public.handle_updated_at();

DROP TRIGGER IF EXISTS set_updated_at_matches ON public.matches;
CREATE TRIGGER set_updated_at_matches BEFORE UPDATE ON public.matches FOR EACH ROW EXECUTE FUNCTION public.handle_updated_at();

-- 8.5. AUTOMATED USER PROFILING SYNC FROM AUTH SIGN-UPS (E.G. GOOGLE SIGN-IN)
CREATE OR REPLACE FUNCTION public.handle_new_user()
RETURNS TRIGGER AS $$
DECLARE
    username_val TEXT;
BEGIN
    username_val := COALESCE(
        NEW.raw_user_meta_data->>'name',
        NEW.raw_user_meta_data->>'full_name',
        NEW.raw_user_meta_data->>'user_name',
        split_part(NEW.email, '@', 1),
        'User_' || substring(NEW.id::text, 1, 8)
    );

    INSERT INTO public.users (
        id, 
        username, 
        email, 
        role, 
        "createdAt", 
        "totalGames", 
        wins, 
        losses, 
        "isBanned"
    )
    VALUES (
        NEW.id,
        username_val,
        COALESCE(NEW.email, ''),
        'player',
        NOW()::text,
        0,
        0,
        0,
        false
    )
    ON CONFLICT (id) DO UPDATE 
    SET 
        email = EXCLUDED.email,
        username = COALESCE(public.users.username, EXCLUDED.username);

    RETURN NEW;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER SET search_path = public;

DROP TRIGGER IF EXISTS on_auth_user_created ON auth.users;
CREATE TRIGGER on_auth_user_created
  AFTER INSERT ON auth.users
  FOR EACH ROW EXECUTE FUNCTION public.handle_new_user();

-- 8.6. AUTOMATED USER DELETION SYNC FROM AUTH DELETIONS
CREATE OR REPLACE FUNCTION public.handle_deleted_user()
RETURNS TRIGGER AS $$
BEGIN
    DELETE FROM public.users WHERE id = OLD.id;
    RETURN OLD;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER SET search_path = public;

DROP TRIGGER IF EXISTS on_auth_user_deleted ON auth.users;
CREATE TRIGGER on_auth_user_deleted
  AFTER DELETE ON auth.users
  FOR EACH ROW EXECUTE FUNCTION public.handle_deleted_user();

-- 9. RBAC BRIDGE TRIGGERS
CREATE OR REPLACE FUNCTION public.sync_user_role()
RETURNS TRIGGER AS $$
DECLARE
    role_id UUID;
BEGIN
    IF (TG_OP = 'INSERT') OR (NEW.role IS DISTINCT FROM OLD.role) THEN
        SELECT id INTO role_id FROM public.roles WHERE name = NEW.role;
        IF role_id IS NOT NULL THEN
            DELETE FROM public.user_roles WHERE user_id = NEW.id;
            INSERT INTO public.user_roles (user_id, role_id) VALUES (NEW.id, role_id);
        END IF;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

DROP TRIGGER IF EXISTS sync_user_role_trigger ON public.users;
CREATE TRIGGER sync_user_role_trigger AFTER INSERT OR UPDATE OF role ON public.users FOR EACH ROW EXECUTE FUNCTION public.sync_user_role();

-- 10. PERFORMANCE INDEXES
CREATE INDEX IF NOT EXISTS idx_users_username ON public.users(username);
CREATE INDEX IF NOT EXISTS idx_users_role ON public.users(role);
CREATE INDEX IF NOT EXISTS idx_matches_host_id ON public.matches(host_id);
CREATE INDEX IF NOT EXISTS idx_matches_opponent_id ON public.matches(opponent_id);
CREATE INDEX IF NOT EXISTS idx_audit_logs_actor ON public.audit_event_logs(actor_id);
CREATE INDEX IF NOT EXISTS idx_audit_logs_table ON public.audit_event_logs(table_name);

-- ==========================================
-- PRODUCTION UPGRADE PHASE 2: ADVANCED SECURITY & COMPLIANCE
-- ==========================================

-- 1. ADVANCED MODERATION TABLES
CREATE TABLE IF NOT EXISTS public.bans (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES public.users(id) ON DELETE CASCADE,
    reason TEXT NOT NULL,
    expires_at TIMESTAMP WITH TIME ZONE, -- NULL for permanent
    created_by UUID REFERENCES public.users(id),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    is_active BOOLEAN DEFAULT true
);

CREATE TABLE IF NOT EXISTS public.reports (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    reporter_id UUID NOT NULL REFERENCES public.users(id) ON DELETE SET NULL,
    reported_id UUID NOT NULL REFERENCES public.users(id) ON DELETE CASCADE,
    reason TEXT NOT NULL,
    evidence_url TEXT,
    status TEXT DEFAULT 'pending', -- 'pending', 'reviewed', 'resolved', 'dismissed'
    moderator_id UUID REFERENCES public.users(id),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- 2. SECURITY & TRACKING TABLES
CREATE TABLE IF NOT EXISTS public.device_info (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES public.users(id) ON DELETE CASCADE,
    device_id TEXT NOT NULL,
    model TEXT,
    os_version TEXT,
    app_version TEXT,
    last_seen TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    UNIQUE(user_id, device_id)
);

CREATE TABLE IF NOT EXISTS public.rate_limits (
    key TEXT PRIMARY KEY,
    request_count INTEGER DEFAULT 0,
    reset_at TIMESTAMP WITH TIME ZONE NOT NULL
);

-- 3. SYSTEM & COMPLIANCE TABLES
CREATE TABLE IF NOT EXISTS public.app_versions (
    version_code INTEGER PRIMARY KEY,
    version_name TEXT NOT NULL,
    is_mandatory BOOLEAN DEFAULT false,
    min_supported_version INTEGER DEFAULT 0,
    release_notes TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS public.maintenance_schedule (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    start_time TIMESTAMP WITH TIME ZONE NOT NULL,
    end_time TIMESTAMP WITH TIME ZONE NOT NULL,
    reason TEXT,
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS public.data_export_requests (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES public.users(id) ON DELETE CASCADE,
    status TEXT DEFAULT 'pending', -- 'pending', 'processing', 'completed', 'expired'
    download_url TEXT,
    expires_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- 4. BAN ENFORCEMENT & SESSION SECURITY
CREATE OR REPLACE FUNCTION public.check_user_ban()
RETURNS TRIGGER AS $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM public.bans 
        WHERE user_id = auth.uid() 
        AND is_active = true 
        AND (expires_at IS NULL OR expires_at > NOW())
    ) THEN
        RAISE EXCEPTION 'User is currently banned';
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- 5. GDPR COMPLIANCE FUNCTIONS
CREATE OR REPLACE FUNCTION public.request_account_deletion()
RETURNS VOID AS $$
BEGIN
    UPDATE public.users 
    SET deleted_at = NOW() + INTERVAL '30 days',
        metadata = metadata || jsonb_build_object('deletion_requested_at', NOW())
    WHERE id = auth.uid();
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

CREATE OR REPLACE FUNCTION public.cancel_account_deletion()
RETURNS VOID AS $$
BEGIN
    UPDATE public.users 
    SET deleted_at = NULL,
        metadata = metadata - 'deletion_requested_at'
    WHERE id = auth.uid();
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- 6. ENABLE RLS ON NEW TABLES
ALTER TABLE public.bans ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.reports ADD COLUMN IF NOT EXISTS priority TEXT DEFAULT 'medium';
ALTER TABLE public.reports ADD COLUMN IF NOT EXISTS internal_comments TEXT;
ALTER TABLE public.reports ADD COLUMN IF NOT EXISTS category TEXT DEFAULT 'general'; -- 'cheating', 'abuse', 'harassment', etc.
ALTER TABLE public.device_info ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.rate_limits ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.app_versions ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.maintenance_schedule ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.data_export_requests ENABLE ROW LEVEL SECURITY;

-- 7. ADVANCED POLICIES

-- BANS: Users view own, Moderators manage
DROP POLICY IF EXISTS "Users can view own bans" ON public.bans;
CREATE POLICY "Users can view own bans" ON public.bans FOR SELECT USING (user_id = auth.uid());
DROP POLICY IF EXISTS "Moderators manage bans" ON public.bans;
CREATE POLICY "Moderators manage bans" ON public.bans FOR ALL USING (public.is_moderator());

-- REPORTS: Reporter views own, Moderators manage
DROP POLICY IF EXISTS "Users can create reports" ON public.reports;
CREATE POLICY "Users can create reports" ON public.reports FOR INSERT WITH CHECK (auth.uid() = reporter_id);
DROP POLICY IF EXISTS "Reporters can view own reports" ON public.reports;
CREATE POLICY "Reporters can view own reports" ON public.reports FOR SELECT USING (reporter_id = auth.uid());
DROP POLICY IF EXISTS "Moderators manage reports" ON public.reports;
CREATE POLICY "Moderators manage reports" ON public.reports FOR ALL USING (public.is_moderator());

-- DEVICE INFO: Users manage own, Admins view
DROP POLICY IF EXISTS "Users manage own device info" ON public.device_info;
CREATE POLICY "Users manage own device info" ON public.device_info FOR ALL USING (user_id = auth.uid());
DROP POLICY IF EXISTS "Admins view device info" ON public.device_info;
CREATE POLICY "Admins view device info" ON public.device_info FOR SELECT USING (public.is_admin());

-- APP VERSIONS & MAINTENANCE: Public read, Admin manage
DROP POLICY IF EXISTS "Public read app versions" ON public.app_versions;
CREATE POLICY "Public read app versions" ON public.app_versions FOR SELECT USING (true);
DROP POLICY IF EXISTS "Admins manage app versions" ON public.app_versions;
CREATE POLICY "Admins manage app versions" ON public.app_versions FOR ALL USING (public.is_admin());
DROP POLICY IF EXISTS "Public read maintenance" ON public.maintenance_schedule;
CREATE POLICY "Public read maintenance" ON public.maintenance_schedule FOR SELECT USING (true);
DROP POLICY IF EXISTS "Admins manage maintenance" ON public.maintenance_schedule;
CREATE POLICY "Admins manage maintenance" ON public.maintenance_schedule FOR ALL USING (public.is_admin());

-- DATA EXPORTS: Users view own, Admins manage
DROP POLICY IF EXISTS "Users view own data exports" ON public.data_export_requests;
CREATE POLICY "Users view own data exports" ON public.data_export_requests FOR SELECT USING (user_id = auth.uid());
DROP POLICY IF EXISTS "Admins manage data exports" ON public.data_export_requests;
CREATE POLICY "Admins manage data exports" ON public.data_export_requests FOR ALL USING (public.is_admin());

-- 8. EXTENDED AUDIT LOGGING
DROP TRIGGER IF EXISTS audit_announcements_trigger ON public.announcements;
CREATE TRIGGER audit_announcements_trigger AFTER INSERT OR UPDATE OR DELETE ON public.announcements FOR EACH ROW EXECUTE FUNCTION public.audit_trigger_func();

DROP TRIGGER IF EXISTS audit_ads_trigger ON public.ad_configuration;
CREATE TRIGGER audit_ads_trigger AFTER INSERT OR UPDATE OR DELETE ON public.ad_configuration FOR EACH ROW EXECUTE FUNCTION public.audit_trigger_func();

DROP TRIGGER IF EXISTS audit_bans_trigger ON public.bans;
CREATE TRIGGER audit_bans_trigger AFTER INSERT OR UPDATE OR DELETE ON public.bans FOR EACH ROW EXECUTE FUNCTION public.audit_trigger_func();

-- 9. PERFORMANCE INDEXES PHASE 2
CREATE INDEX IF NOT EXISTS idx_users_deleted_at ON public.users(deleted_at) WHERE deleted_at IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_reports_reported_id ON public.reports(reported_id);
CREATE INDEX IF NOT EXISTS idx_reports_status ON public.reports(status);
CREATE INDEX IF NOT EXISTS idx_bans_user_id ON public.bans(user_id);
CREATE INDEX IF NOT EXISTS idx_device_info_user_id ON public.device_info(user_id);
CREATE INDEX IF NOT EXISTS idx_matches_status ON public.matches(status);

-- ==========================================
-- PRODUCTION UPGRADE PHASE 3: ECONOMY, RANKING & SUPPORT
-- ==========================================

-- 1. EXTEND USERS TABLE FOR ECONOMY & VERIFICATION
ALTER TABLE public.users ADD COLUMN IF NOT EXISTS coins INTEGER DEFAULT 0;
ALTER TABLE public.users ADD COLUMN IF NOT EXISTS xp INTEGER DEFAULT 0;
ALTER TABLE public.users ADD COLUMN IF NOT EXISTS rating INTEGER DEFAULT 1000;
ALTER TABLE public.users ADD COLUMN IF NOT EXISTS is_verified BOOLEAN DEFAULT false;
ALTER TABLE public.users ADD COLUMN IF NOT EXISTS internal_notes TEXT;
ALTER TABLE public.users ADD COLUMN IF NOT EXISTS shadow_banned BOOLEAN DEFAULT false;

-- 2. SUPPORT TICKETS TABLE
CREATE TABLE IF NOT EXISTS public.support_tickets (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES public.users(id) ON DELETE CASCADE,
    subject TEXT NOT NULL,
    message TEXT NOT NULL,
    status TEXT DEFAULT 'open', -- 'open', 'in_progress', 'resolved', 'closed'
    priority TEXT DEFAULT 'medium', -- 'low', 'medium', 'high', 'critical'
    assigned_to UUID REFERENCES public.users(id),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- 3. FEEDBACK V2 TABLE (Expanded for Sentiment & Assignment)
CREATE TABLE IF NOT EXISTS public.feedback_v2 (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID REFERENCES public.users(id) ON DELETE SET NULL,
    content TEXT NOT NULL,
    category TEXT NOT NULL, -- 'bug', 'suggestion', 'feature_request', 'complaint'
    rating INTEGER CHECK (rating >= 1 AND rating <= 5),
    sentiment TEXT, -- 'positive', 'neutral', 'negative'
    status TEXT DEFAULT 'pending', -- 'pending', 'under_review', 'planned', 'fixed', 'closed'
    assigned_developer_id UUID REFERENCES public.users(id),
    internal_reply TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- 4. USER LOGIN HISTORY
CREATE TABLE IF NOT EXISTS public.login_history (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES public.users(id) ON DELETE CASCADE,
    ip_address TEXT,
    device_id TEXT,
    user_agent TEXT,
    location TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- 5. RLS & POLICIES FOR PHASE 3
ALTER TABLE public.support_tickets ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.feedback_v2 ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.login_history ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS "Users manage own tickets" ON public.support_tickets;
CREATE POLICY "Users manage own tickets" ON public.support_tickets FOR ALL USING (user_id = auth.uid());
DROP POLICY IF EXISTS "Support views all tickets" ON public.support_tickets;
CREATE POLICY "Support views all tickets" ON public.support_tickets FOR ALL USING (public.is_support());

DROP POLICY IF EXISTS "Users can create feedback v2" ON public.feedback_v2;
CREATE POLICY "Users can create feedback v2" ON public.feedback_v2 FOR INSERT WITH CHECK (true);
DROP POLICY IF EXISTS "Admins manage feedback v2" ON public.feedback_v2;
CREATE POLICY "Admins manage feedback v2" ON public.feedback_v2 FOR ALL USING (public.is_admin());

DROP POLICY IF EXISTS "Users view own login history" ON public.login_history;
CREATE POLICY "Users view own login history" ON public.login_history FOR SELECT USING (user_id = auth.uid());
DROP POLICY IF EXISTS "Admins view all login history" ON public.login_history;
CREATE POLICY "Admins view all login history" ON public.login_history FOR SELECT USING (public.is_admin());

-- 6. TRIGGERS FOR PHASE 3
DROP TRIGGER IF EXISTS set_updated_at_tickets ON public.support_tickets;
CREATE TRIGGER set_updated_at_tickets BEFORE UPDATE ON public.support_tickets FOR EACH ROW EXECUTE FUNCTION public.handle_updated_at();

DROP TRIGGER IF EXISTS set_updated_at_feedback_v2 ON public.feedback_v2;
CREATE TRIGGER set_updated_at_feedback_v2 BEFORE UPDATE ON public.feedback_v2 FOR EACH ROW EXECUTE FUNCTION public.handle_updated_at();

-- 7. AUDIT LOGGING FOR PHASE 3
DROP TRIGGER IF EXISTS audit_tickets_trigger ON public.support_tickets;
CREATE TRIGGER audit_tickets_trigger AFTER INSERT OR UPDATE OR DELETE ON public.support_tickets FOR EACH ROW EXECUTE FUNCTION public.audit_trigger_func();

DROP TRIGGER IF EXISTS audit_feedback_v2_trigger ON public.feedback_v2;
CREATE TRIGGER audit_feedback_v2_trigger AFTER INSERT OR UPDATE OR DELETE ON public.feedback_v2 FOR EACH ROW EXECUTE FUNCTION public.audit_trigger_func();

-- 8. INDEXES FOR PHASE 3
CREATE INDEX IF NOT EXISTS idx_tickets_user_id ON public.support_tickets(user_id);
CREATE INDEX IF NOT EXISTS idx_tickets_status ON public.support_tickets(status);
CREATE INDEX IF NOT EXISTS idx_feedback_v2_status ON public.feedback_v2(status);
CREATE INDEX IF NOT EXISTS idx_login_history_user_id ON public.login_history(user_id);

-- FINAL SEED DATA FOR PHASE 3
-- Add a sample feedback
INSERT INTO public.feedback_v2 (content, category, rating, sentiment) 
VALUES ('Great app, but I wish there were more themes!', 'feature_request', 5, 'positive')
ON CONFLICT DO NOTHING;

-- ==========================================
-- PRODUCTION UPGRADE PHASE 4: GAME OPERATIONS & ANTI-CHEAT
-- ==========================================

-- 1. EXTEND MATCHES TABLE
ALTER TABLE public.matches ADD COLUMN IF NOT EXISTS match_type TEXT DEFAULT 'multiplayer'; -- 'multiplayer', 'ai', 'ranked', 'practice'
ALTER TABLE public.matches ADD COLUMN IF NOT EXISTS latency_ms INTEGER DEFAULT 0;
ALTER TABLE public.matches ADD COLUMN IF NOT EXISTS abandoned_by UUID REFERENCES public.users(id);
ALTER TABLE public.matches ADD COLUMN IF NOT EXISTS is_ranked BOOLEAN DEFAULT false;
ALTER TABLE public.matches ADD COLUMN IF NOT EXISTS replay_data JSONB;
ALTER TABLE public.matches ADD COLUMN IF NOT EXISTS chat_history JSONB;

-- 2. TOURNAMENTS TABLE
CREATE TABLE IF NOT EXISTS public.tournaments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    title TEXT NOT NULL,
    description TEXT,
    status TEXT DEFAULT 'scheduled', -- 'scheduled', 'ongoing', 'completed', 'cancelled'
    start_time TIMESTAMP WITH TIME ZONE,
    end_time TIMESTAMP WITH TIME ZONE,
    min_rank INTEGER DEFAULT 0,
    entry_fee INTEGER DEFAULT 0,
    prize_pool_coins INTEGER DEFAULT 0,
    max_participants INTEGER DEFAULT 16,
    bracket_data JSONB,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- 3. TOURNAMENT PARTICIPANTS
CREATE TABLE IF NOT EXISTS public.tournament_participants (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tournament_id UUID NOT NULL REFERENCES public.tournaments(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES public.users(id) ON DELETE CASCADE,
    rank INTEGER,
    score INTEGER DEFAULT 0,
    joined_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    UNIQUE(tournament_id, user_id)
);

-- 4. GAME EVENTS TABLE
CREATE TABLE IF NOT EXISTS public.game_events (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    title TEXT NOT NULL,
    type TEXT NOT NULL, -- 'double_xp', 'coin_bonus', 'seasonal_challenge'
    multiplier DECIMAL DEFAULT 1.0,
    start_time TIMESTAMP WITH TIME ZONE,
    end_time TIMESTAMP WITH TIME ZONE,
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- 5. ANTI-CHEAT LOGS
CREATE TABLE IF NOT EXISTS public.anti_cheat_logs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID REFERENCES public.users(id) ON DELETE CASCADE,
    match_id TEXT REFERENCES public.matches(id) ON DELETE SET NULL,
    violation_type TEXT NOT NULL, -- 'root_detected', 'emulator_detected', 'packet_tamper', 'speed_hack'
    severity TEXT DEFAULT 'low', -- 'low', 'medium', 'high', 'critical'
    metadata JSONB,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- 6. LEADERBOARD SNAPSHOTS
CREATE TABLE IF NOT EXISTS public.leaderboard_snapshots (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    scope TEXT NOT NULL, -- 'weekly', 'monthly', 'all_time'
    snapshot_data JSONB NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- 7. RLS & POLICIES FOR PHASE 4
ALTER TABLE public.tournaments ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.tournament_participants ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.game_events ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.anti_cheat_logs ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.leaderboard_snapshots ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS "Anyone can view active tournaments" ON public.tournaments;
CREATE POLICY "Anyone can view active tournaments" ON public.tournaments FOR SELECT USING (true);
DROP POLICY IF EXISTS "Admins manage tournaments" ON public.tournaments;
CREATE POLICY "Admins manage tournaments" ON public.tournaments FOR ALL USING (public.is_admin());

DROP POLICY IF EXISTS "Anyone can view game events" ON public.game_events;
CREATE POLICY "Anyone can view game events" ON public.game_events FOR SELECT USING (true);
DROP POLICY IF EXISTS "Admins manage game events" ON public.game_events;
CREATE POLICY "Admins manage game events" ON public.game_events FOR ALL USING (public.is_admin());

DROP POLICY IF EXISTS "Admins view anti-cheat logs" ON public.anti_cheat_logs;
CREATE POLICY "Admins view anti-cheat logs" ON public.anti_cheat_logs FOR SELECT USING (public.is_admin());

-- 8. INDEXES FOR PHASE 4
CREATE INDEX IF NOT EXISTS idx_tournaments_status ON public.tournaments(status);
CREATE INDEX IF NOT EXISTS idx_anti_cheat_user_id ON public.anti_cheat_logs(user_id);
CREATE INDEX IF NOT EXISTS idx_matches_type ON public.matches(match_type);

-- FINAL SEED DATA FOR PHASE 4
-- ==========================================
-- PRODUCTION UPGRADE PHASE 5: BUSINESS INTELLIGENCE & MONITORING
-- ==========================================

-- 1. ANALYTICS & REVENUE
CREATE TABLE IF NOT EXISTS public.bi_daily_metrics (
    date DATE PRIMARY KEY DEFAULT CURRENT_DATE,
    dau INTEGER DEFAULT 0,
    wau INTEGER DEFAULT 0,
    mau INTEGER DEFAULT 0,
    sessions INTEGER DEFAULT 0,
    revenue_usd DECIMAL(10, 2) DEFAULT 0.00,
    ad_impressions INTEGER DEFAULT 0,
    ad_clicks INTEGER DEFAULT 0,
    retention_day_1 DECIMAL(5, 2) DEFAULT 0.00,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- 2. PUSH NOTIFICATIONS
CREATE TABLE IF NOT EXISTS public.bi_notifications (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    title TEXT NOT NULL,
    body TEXT NOT NULL,
    target_segment TEXT DEFAULT 'all', -- 'all', 'active', 'inactive', 'new_users'
    target_region TEXT,
    schedule_time TIMESTAMP WITH TIME ZONE,
    sent_at TIMESTAMP WITH TIME ZONE,
    status TEXT DEFAULT 'scheduled', -- 'scheduled', 'sending', 'sent', 'failed'
    open_count INTEGER DEFAULT 0,
    failure_count INTEGER DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- 3. LOGS EXPLORER (CENTRALIZED)
CREATE TABLE IF NOT EXISTS public.bi_app_logs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID REFERENCES public.users(id) ON DELETE SET NULL,
    level TEXT NOT NULL, -- 'INFO', 'WARNING', 'ERROR', 'CRITICAL'
    category TEXT NOT NULL, -- 'NETWORK', 'ADS', 'SECURITY', 'CRASH', 'UI'
    message TEXT NOT NULL,
    stack_trace TEXT,
    device_info JSONB,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- 4. MONITORING & HEALTH
CREATE TABLE IF NOT EXISTS public.bi_health_metrics (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    service_name TEXT NOT NULL,
    status TEXT NOT NULL,
    latency_ms INTEGER,
    cpu_usage DECIMAL(5, 2),
    ram_usage_mb INTEGER,
    active_connections INTEGER,
    recorded_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- 5. ENHANCED ANNOUNCEMENTS (UPGRADE)
ALTER TABLE public.announcements ADD COLUMN IF NOT EXISTS priority TEXT DEFAULT 'low'; -- 'low', 'high', 'critical'
ALTER TABLE public.announcements ADD COLUMN IF NOT EXISTS expiry_at TIMESTAMP WITH TIME ZONE;
ALTER TABLE public.announcements ADD COLUMN IF NOT EXISTS image_url TEXT;
ALTER TABLE public.announcements ADD COLUMN IF NOT EXISTS display_type TEXT DEFAULT 'banner'; -- 'banner', 'popup', 'fullscreen'

-- 6. RLS & POLICIES FOR PHASE 5
ALTER TABLE public.bi_daily_metrics ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.bi_notifications ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.bi_app_logs ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.bi_health_metrics ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS "Admins view BI metrics" ON public.bi_daily_metrics;
CREATE POLICY "Admins view BI metrics" ON public.bi_daily_metrics FOR SELECT USING (public.is_admin());

DROP POLICY IF EXISTS "Admins manage BI notifications" ON public.bi_notifications;
CREATE POLICY "Admins manage BI notifications" ON public.bi_notifications FOR ALL USING (public.is_admin());

DROP POLICY IF EXISTS "Admins view app logs" ON public.bi_app_logs;
CREATE POLICY "Admins view app logs" ON public.bi_app_logs FOR SELECT USING (public.is_admin());

-- 7. SEED DATA FOR PHASE 5
INSERT INTO public.bi_daily_metrics (date, dau, mau, revenue_usd)
VALUES (CURRENT_DATE - INTERVAL '1 day', 45, 150, 12.50)
ON CONFLICT (date) DO NOTHING;

INSERT INTO public.bi_health_metrics (service_name, status, latency_ms)
VALUES ('Supabase API', 'HEALTHY', 12), ('Auth Service', 'HEALTHY', 8), ('Edge Functions', 'HEALTHY', 45)
ON CONFLICT DO NOTHING;


-- ==========================================
-- PRODUCTION UPGRADE PHASE 6: MISSING ENTERPRISE TABLES & SYNC
-- ==========================================

-- 1. USER CAMELCASE ATTRIBUTES FOR PARSER COMPATIBILITY
ALTER TABLE public.users ADD COLUMN IF NOT EXISTS "isVerified" BOOLEAN DEFAULT false;
ALTER TABLE public.users ADD COLUMN IF NOT EXISTS "shadowBanned" BOOLEAN DEFAULT false;
ALTER TABLE public.users ADD COLUMN IF NOT EXISTS "internalNotes" TEXT;
ALTER TABLE public.users ADD COLUMN IF NOT EXISTS "deviceId" TEXT;
ALTER TABLE public.users ADD COLUMN IF NOT EXISTS "appVersion" TEXT;
ALTER TABLE public.users ADD COLUMN IF NOT EXISTS country TEXT;
ALTER TABLE public.users ADD COLUMN IF NOT EXISTS permissions JSONB DEFAULT '[]'::jsonb NOT NULL;
ALTER TABLE public.users ADD COLUMN IF NOT EXISTS roles JSONB DEFAULT '[]'::jsonb NOT NULL;
ALTER TABLE public.admin_sessions ADD COLUMN IF NOT EXISTS admin_id UUID REFERENCES public.users(id) ON DELETE CASCADE;

-- 2. ADMIN INVITATIONS TABLE
CREATE TABLE IF NOT EXISTS public.admin_invitations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email TEXT NOT NULL,
    role TEXT NOT NULL DEFAULT 'moderator',
    permissions JSONB DEFAULT '[]'::jsonb NOT NULL,
    invited_by UUID REFERENCES public.users(id) ON DELETE SET NULL,
    status TEXT DEFAULT 'pending' NOT NULL, -- 'pending', 'accepted', 'revoked'
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW() NOT NULL
);

-- 3. ADMIN ACTIVITY TABLE
CREATE TABLE IF NOT EXISTS public.admin_activity (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    admin_id UUID NOT NULL REFERENCES public.users(id) ON DELETE CASCADE,
    action TEXT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW() NOT NULL
);

-- 4. ECONOMY TRANSACTIONS TABLE
CREATE TABLE IF NOT EXISTS public.economy_transactions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES public.users(id) ON DELETE CASCADE,
    amount INTEGER NOT NULL,
    currency TEXT NOT NULL, -- 'coins', 'xp'
    type TEXT NOT NULL, -- 'reward', 'purchase', 'penalty', 'adjustment'
    source TEXT NOT NULL, -- 'match', 'daily_reward', 'spin_wheel', 'admin'
    reason TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW() NOT NULL
);

-- 5. STORE ITEMS TABLE
CREATE TABLE IF NOT EXISTS public.store_items (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name TEXT NOT NULL,
    description TEXT,
    type TEXT NOT NULL, -- 'pack', 'bundle'
    price_usd DECIMAL(10, 2),
    price_coins INTEGER,
    content JSONB DEFAULT '{}'::jsonb NOT NULL,
    image_url TEXT,
    is_featured BOOLEAN DEFAULT false,
    discount_percentage INTEGER DEFAULT 0,
    expiry_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW() NOT NULL
);

-- 6. COUPONS TABLE
CREATE TABLE IF NOT EXISTS public.coupons (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code TEXT UNIQUE NOT NULL,
    discount_type TEXT NOT NULL, -- 'fixed', 'percentage'
    value DECIMAL(10, 2) NOT NULL,
    max_uses INTEGER,
    used_count INTEGER DEFAULT 0 NOT NULL,
    expires_at TIMESTAMP WITH TIME ZONE,
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW() NOT NULL
);

-- 7. DAILY REWARDS TABLE
CREATE TABLE IF NOT EXISTS public.daily_rewards (
    day INTEGER PRIMARY KEY,
    type TEXT NOT NULL, -- 'coins', 'xp', 'item'
    amount INTEGER NOT NULL,
    item_id UUID,
    image_url TEXT
);

-- 8. SPIN WHEEL REWARDS TABLE
CREATE TABLE IF NOT EXISTS public.spin_wheel_rewards (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    type TEXT NOT NULL,
    amount INTEGER NOT NULL,
    weight INTEGER DEFAULT 1 NOT NULL,
    image_url TEXT,
    color TEXT
);

-- 9. LIVEOPS EVENTS TABLE
CREATE TABLE IF NOT EXISTS public.liveops_events (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    title TEXT NOT NULL,
    description TEXT,
    type TEXT NOT NULL, -- 'xp_weekend', 'coin_rush', 'tournament', 'seasonal'
    xp_multiplier DECIMAL(5, 2) DEFAULT 1.0 NOT NULL,
    coin_multiplier DECIMAL(5, 2) DEFAULT 1.0 NOT NULL,
    start_time TIMESTAMP WITH TIME ZONE NOT NULL,
    end_time TIMESTAMP WITH TIME ZONE NOT NULL,
    is_active BOOLEAN DEFAULT true,
    metadata JSONB DEFAULT '{}'::jsonb
);

-- 10. SEASON PASSES TABLE
CREATE TABLE IF NOT EXISTS public.season_passes (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    title TEXT NOT NULL,
    start_time TIMESTAMP WITH TIME ZONE NOT NULL,
    end_time TIMESTAMP WITH TIME ZONE NOT NULL,
    is_active BOOLEAN DEFAULT true
);

-- 11. SEASON PASS TIERS TABLE
CREATE TABLE IF NOT EXISTS public.season_pass_tiers (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    season_pass_id UUID NOT NULL REFERENCES public.season_passes(id) ON DELETE CASCADE,
    tier_number INTEGER NOT NULL,
    xp_required INTEGER NOT NULL,
    free_reward_type TEXT,
    free_reward_amount INTEGER,
    premium_reward_type TEXT,
    premium_reward_amount INTEGER
);

-- 12. CMS CONTENT TABLE
CREATE TABLE IF NOT EXISTS public.cms_content (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    slug TEXT UNIQUE NOT NULL,
    title TEXT NOT NULL,
    body TEXT NOT NULL, -- Markdown format
    type TEXT NOT NULL, -- 'patch_notes', 'faq', 'privacy', 'terms', 'tutorial'
    image_url TEXT,
    video_url TEXT,
    status TEXT DEFAULT 'draft' NOT NULL, -- 'draft', 'published'
    published_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW() NOT NULL
);

-- 13. ENTERPRISE BI METRICS TABLE
CREATE TABLE IF NOT EXISTS public.bi_metrics (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    dau INTEGER DEFAULT 0 NOT NULL,
    wau INTEGER DEFAULT 0 NOT NULL,
    mau INTEGER DEFAULT 0 NOT NULL,
    retention_d1 DECIMAL(5, 2) DEFAULT 0.0 NOT NULL,
    retention_d7 DECIMAL(5, 2) DEFAULT 0.0 NOT NULL,
    retention_d30 DECIMAL(5, 2) DEFAULT 0.0 NOT NULL,
    total_revenue DECIMAL(12, 2) DEFAULT 0.0 NOT NULL,
    arpu DECIMAL(10, 4) DEFAULT 0.0 NOT NULL,
    arppu DECIMAL(10, 4) DEFAULT 0.0 NOT NULL,
    churn_rate DECIMAL(5, 2) DEFAULT 0.0 NOT NULL,
    country_dist JSONB DEFAULT '{}'::jsonb NOT NULL,
    device_dist JSONB DEFAULT '{}'::jsonb NOT NULL,
    version_dist JSONB DEFAULT '{}'::jsonb NOT NULL,
    recorded_at TIMESTAMP WITH TIME ZONE DEFAULT NOW() NOT NULL
);

-- 14. CRASH LOGS TABLE
CREATE TABLE IF NOT EXISTS public.crash_logs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    exception TEXT NOT NULL,
    stacktrace TEXT NOT NULL,
    user_id UUID REFERENCES public.users(id) ON DELETE SET NULL,
    device_model TEXT,
    os_version TEXT,
    app_version TEXT,
    status TEXT DEFAULT 'open' NOT NULL, -- 'open', 'resolved', 'ignored'
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW() NOT NULL
);

-- 15. FRAUD ALERTS TABLE
CREATE TABLE IF NOT EXISTS public.fraud_alerts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES public.users(id) ON DELETE CASCADE,
    type TEXT NOT NULL, -- 'coin_farming', 'bot_detection', 'referral_abuse', 'smurf'
    confidence DECIMAL(5, 2) NOT NULL,
    status TEXT DEFAULT 'pending' NOT NULL, -- 'pending', 'confirmed', 'dismissed'
    evidence JSONB DEFAULT '{}'::jsonb,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW() NOT NULL
);

-- 16. FINANCE REPORTS TABLE
CREATE TABLE IF NOT EXISTS public.finance_reports (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    revenue DECIMAL(12, 2) DEFAULT 0.00 NOT NULL,
    ads DECIMAL(12, 2) DEFAULT 0.00 NOT NULL,
    purchases DECIMAL(12, 2) DEFAULT 0.00 NOT NULL,
    refunds DECIMAL(12, 2) DEFAULT 0.00 NOT NULL,
    chargebacks DECIMAL(12, 2) DEFAULT 0.00 NOT NULL,
    forecast_next_month DECIMAL(12, 2) DEFAULT 0.00 NOT NULL,
    recorded_at TIMESTAMP WITH TIME ZONE DEFAULT NOW() NOT NULL
);

-- 17. QUEUE METRICS TABLE
CREATE TABLE IF NOT EXISTS public.queue_metrics (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    queue_name TEXT NOT NULL,
    size INTEGER DEFAULT 0 NOT NULL,
    retry_count INTEGER DEFAULT 0 NOT NULL,
    dead_letter_count INTEGER DEFAULT 0 NOT NULL,
    recorded_at TIMESTAMP WITH TIME ZONE DEFAULT NOW() NOT NULL
);

-- 18. DEVICE RECORDS TABLE
CREATE TABLE IF NOT EXISTS public.device_records (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    device_id TEXT NOT NULL,
    is_rooted BOOLEAN DEFAULT false,
    is_vpn BOOLEAN DEFAULT false,
    is_emulator BOOLEAN DEFAULT false,
    is_blocked BOOLEAN DEFAULT false,
    last_seen TIMESTAMP WITH TIME ZONE DEFAULT NOW() NOT NULL
);

-- 19. AI CONFIGURATIONS TABLE
CREATE TABLE IF NOT EXISTS public.ai_configs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    model TEXT DEFAULT 'gemini-1.5-flash' NOT NULL,
    temperature DECIMAL(3, 2) DEFAULT 0.70 NOT NULL,
    max_tokens INTEGER DEFAULT 256 NOT NULL,
    system_prompt TEXT NOT NULL,
    personality TEXT DEFAULT 'strategic' NOT NULL,
    is_staged BOOLEAN DEFAULT false NOT NULL,
    version INTEGER DEFAULT 1 NOT NULL
);


-- ==========================================
-- ROW LEVEL SECURITY (RLS) FOR NEW TABLES
-- ==========================================
ALTER TABLE public.admin_invitations ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.admin_activity ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.economy_transactions ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.store_items ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.coupons ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.daily_rewards ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.spin_wheel_rewards ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.liveops_events ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.season_passes ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.season_pass_tiers ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.cms_content ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.bi_metrics ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.crash_logs ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.fraud_alerts ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.finance_reports ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.queue_metrics ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.device_records ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.ai_configs ENABLE ROW LEVEL SECURITY;

-- Select policies
DROP POLICY IF EXISTS "Public read store items" ON public.store_items;
CREATE POLICY "Public read store items" ON public.store_items FOR SELECT USING (true);

DROP POLICY IF EXISTS "Public read coupons" ON public.coupons;
CREATE POLICY "Public read coupons" ON public.coupons FOR SELECT USING (is_active = true);

DROP POLICY IF EXISTS "Public read daily rewards" ON public.daily_rewards;
CREATE POLICY "Public read daily rewards" ON public.daily_rewards FOR SELECT USING (true);

DROP POLICY IF EXISTS "Public read spin wheel rewards" ON public.spin_wheel_rewards;
CREATE POLICY "Public read spin wheel rewards" ON public.spin_wheel_rewards FOR SELECT USING (true);

DROP POLICY IF EXISTS "Public read liveops events" ON public.liveops_events;
CREATE POLICY "Public read liveops events" ON public.liveops_events FOR SELECT USING (is_active = true);

DROP POLICY IF EXISTS "Public read season passes" ON public.season_passes;
CREATE POLICY "Public read season passes" ON public.season_passes FOR SELECT USING (is_active = true);

DROP POLICY IF EXISTS "Public read season pass tiers" ON public.season_pass_tiers;
CREATE POLICY "Public read season pass tiers" ON public.season_pass_tiers FOR SELECT USING (true);

DROP POLICY IF EXISTS "Public read cms content" ON public.cms_content;
CREATE POLICY "Public read cms content" ON public.cms_content FOR SELECT USING (status = 'published');

DROP POLICY IF EXISTS "Public read ai configs" ON public.ai_configs;
CREATE POLICY "Public read ai configs" ON public.ai_configs FOR SELECT USING (true);

DROP POLICY IF EXISTS "Users read own economy transactions" ON public.economy_transactions;
CREATE POLICY "Users read own economy transactions" ON public.economy_transactions FOR SELECT USING (user_id = auth.uid());

DROP POLICY IF EXISTS "Users read own device records" ON public.device_records;
CREATE POLICY "Users read own device records" ON public.device_records FOR SELECT USING (true);

-- Admin-only control policies
DROP POLICY IF EXISTS "Admins manage admin_invitations" ON public.admin_invitations;
CREATE POLICY "Admins manage admin_invitations" ON public.admin_invitations FOR ALL USING (public.is_admin());

DROP POLICY IF EXISTS "Admins manage admin_activity" ON public.admin_activity;
CREATE POLICY "Admins manage admin_activity" ON public.admin_activity FOR ALL USING (public.is_admin());

DROP POLICY IF EXISTS "Admins manage economy_transactions" ON public.economy_transactions;
CREATE POLICY "Admins manage economy_transactions" ON public.economy_transactions FOR ALL USING (public.is_admin());

DROP POLICY IF EXISTS "Admins manage store_items" ON public.store_items;
CREATE POLICY "Admins manage store_items" ON public.store_items FOR ALL USING (public.is_admin());

DROP POLICY IF EXISTS "Admins manage coupons" ON public.coupons;
CREATE POLICY "Admins manage coupons" ON public.coupons FOR ALL USING (public.is_admin());

DROP POLICY IF EXISTS "Admins manage daily_rewards" ON public.daily_rewards;
CREATE POLICY "Admins manage daily_rewards" ON public.daily_rewards FOR ALL USING (public.is_admin());

DROP POLICY IF EXISTS "Admins manage spin_wheel_rewards" ON public.spin_wheel_rewards;
CREATE POLICY "Admins manage spin_wheel_rewards" ON public.spin_wheel_rewards FOR ALL USING (public.is_admin());

DROP POLICY IF EXISTS "Admins manage liveops_events" ON public.liveops_events;
CREATE POLICY "Admins manage liveops_events" ON public.liveops_events FOR ALL USING (public.is_admin());

DROP POLICY IF EXISTS "Admins manage season_passes" ON public.season_passes;
CREATE POLICY "Admins manage season_passes" ON public.season_passes FOR ALL USING (public.is_admin());

DROP POLICY IF EXISTS "Admins manage season_pass_tiers" ON public.season_pass_tiers;
CREATE POLICY "Admins manage season_pass_tiers" ON public.season_pass_tiers FOR ALL USING (public.is_admin());

DROP POLICY IF EXISTS "Admins manage cms_content" ON public.cms_content;
CREATE POLICY "Admins manage cms_content" ON public.cms_content FOR ALL USING (public.is_admin());

DROP POLICY IF EXISTS "Admins manage bi_metrics" ON public.bi_metrics;
CREATE POLICY "Admins manage bi_metrics" ON public.bi_metrics FOR ALL USING (public.is_admin());

DROP POLICY IF EXISTS "Admins manage crash_logs" ON public.crash_logs;
CREATE POLICY "Admins manage crash_logs" ON public.crash_logs FOR ALL USING (public.is_admin());

DROP POLICY IF EXISTS "Admins manage fraud_alerts" ON public.fraud_alerts;
CREATE POLICY "Admins manage fraud_alerts" ON public.fraud_alerts FOR ALL USING (public.is_admin());

DROP POLICY IF EXISTS "Admins manage finance_reports" ON public.finance_reports;
CREATE POLICY "Admins manage finance_reports" ON public.finance_reports FOR ALL USING (public.is_admin());

DROP POLICY IF EXISTS "Admins manage queue_metrics" ON public.queue_metrics;
CREATE POLICY "Admins manage queue_metrics" ON public.queue_metrics FOR ALL USING (public.is_admin());

DROP POLICY IF EXISTS "Admins manage device_records" ON public.device_records;
CREATE POLICY "Admins manage device_records" ON public.device_records FOR ALL USING (public.is_admin());

DROP POLICY IF EXISTS "Admins manage ai_configs" ON public.ai_configs;
CREATE POLICY "Admins manage ai_configs" ON public.ai_configs FOR ALL USING (public.is_admin());


-- ==========================================
-- PERFORMANCE INDEXES FOR NEW TABLES
-- ==========================================
CREATE INDEX IF NOT EXISTS idx_economy_user_id ON public.economy_transactions(user_id);
CREATE INDEX IF NOT EXISTS idx_coupons_code ON public.coupons(code) WHERE is_active = true;
CREATE INDEX IF NOT EXISTS idx_liveops_start_end ON public.liveops_events(start_time, end_time);
CREATE INDEX IF NOT EXISTS idx_cms_slug ON public.cms_content(slug);
CREATE INDEX IF NOT EXISTS idx_crash_status ON public.crash_logs(status);
CREATE INDEX IF NOT EXISTS idx_fraud_user_id ON public.fraud_alerts(user_id);
CREATE INDEX IF NOT EXISTS idx_device_records_id ON public.device_records(device_id);


-- ==========================================
-- DEFAULT SEED DATA FOR NEW TABLES
-- ==========================================
INSERT INTO public.daily_rewards (day, type, amount, item_id, image_url) VALUES
(1, 'coins', 100, NULL, 'https://example.com/rewards/coin_small.png'),
(2, 'coins', 250, NULL, 'https://example.com/rewards/coin_medium.png'),
(3, 'xp', 50, NULL, 'https://example.com/rewards/xp_small.png'),
(4, 'coins', 500, NULL, 'https://example.com/rewards/coin_large.png'),
(5, 'xp', 150, NULL, 'https://example.com/rewards/xp_large.png'),
(6, 'coins', 1000, NULL, 'https://example.com/rewards/gold_sack.png'),
(7, 'item', 1, '00000000-0000-0000-0000-000000000001'::uuid, 'https://example.com/rewards/mystic_board.png')
ON CONFLICT (day) DO NOTHING;

INSERT INTO public.spin_wheel_rewards (type, amount, weight, color) VALUES
('coins', 100, 50, '#FFD700'),
('coins', 250, 30, '#FFC0CB'),
('xp', 50, 40, '#87CEEB'),
('coins', 500, 15, '#FFA500'),
('xp', 200, 10, '#98FB98'),
('coins', 2000, 2, '#FF0000')
ON CONFLICT DO NOTHING;

INSERT INTO public.ai_configs (model, temperature, max_tokens, system_prompt, personality, is_staged, version) VALUES
('gemini-1.5-flash', 0.70, 256, 'You are an expert strategic commentator guiding the Daadi Multiplayer Arena.', 'strategic', false, 1)
ON CONFLICT DO NOTHING;


