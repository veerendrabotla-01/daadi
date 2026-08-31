-- Supabase Schema Setup for Daadi Game
-- Comprehensive schema for all data models.

-- Helper for JSONB columns
-- CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- 1. Users & Auth
CREATE TABLE IF NOT EXISTS users (
    id TEXT PRIMARY KEY,
    username TEXT NOT NULL,
    email TEXT UNIQUE NOT NULL,
    role TEXT DEFAULT 'publicuser',
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    total_games INTEGER DEFAULT 0,
    wins INTEGER DEFAULT 0,
    losses INTEGER DEFAULT 0,
    coins INTEGER DEFAULT 0,
    xp INTEGER DEFAULT 0,
    rating INTEGER DEFAULT 1000,
    is_banned BOOLEAN DEFAULT FALSE,
    is_reported BOOLEAN DEFAULT FALSE,
    is_verified BOOLEAN DEFAULT FALSE,
    shadow_banned BOOLEAN DEFAULT FALSE,
    reports_count INTEGER DEFAULT 0,
    internal_notes TEXT,
    permissions JSONB DEFAULT '[]'::jsonb,
    roles JSONB DEFAULT '[]'::jsonb,
    deleted_at TIMESTAMP WITH TIME ZONE,
    avatar_url TEXT,
    country_code TEXT,
    last_login TIMESTAMP WITH TIME ZONE,
    country TEXT,
    device_id TEXT,
    app_version TEXT,
    metadata JSONB
);

CREATE TABLE IF NOT EXISTS roles (
    id TEXT PRIMARY KEY,
    name TEXT NOT NULL,
    description TEXT
);

CREATE TABLE IF NOT EXISTS permissions (
    id TEXT PRIMARY KEY,
    name TEXT NOT NULL,
    description TEXT
);

CREATE TABLE IF NOT EXISTS role_permissions (
    role_id TEXT REFERENCES roles(id),
    permission_id TEXT REFERENCES permissions(id),
    PRIMARY KEY (role_id, permission_id)
);

CREATE TABLE IF NOT EXISTS login_history (
    id TEXT PRIMARY KEY,
    user_id TEXT REFERENCES users(id),
    ip_address TEXT,
    device_id TEXT,
    user_agent TEXT,
    location TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- 2. Matches & Tournaments
CREATE TABLE IF NOT EXISTS matches (
    id TEXT PRIMARY KEY,
    host_name TEXT NOT NULL,
    opponent_name TEXT NOT NULL,
    status TEXT NOT NULL,
    winner TEXT,
    moves_count INTEGER DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    moves_json TEXT,
    host_id TEXT,
    opponent_id TEXT,
    updated_at TIMESTAMP WITH TIME ZONE,
    match_type TEXT DEFAULT 'multiplayer',
    latency_ms INTEGER DEFAULT 0,
    is_ranked BOOLEAN DEFAULT FALSE,
    abandoned_by TEXT,
    server_region TEXT DEFAULT 'Asia-South',
    chat_logs_json TEXT
);

CREATE TABLE IF NOT EXISTS tournaments (
    id TEXT PRIMARY KEY,
    title TEXT NOT NULL,
    description TEXT,
    status TEXT NOT NULL,
    start_time TIMESTAMP WITH TIME ZONE,
    end_time TIMESTAMP WITH TIME ZONE,
    min_rank INTEGER DEFAULT 0,
    entry_fee INTEGER DEFAULT 0,
    prize_pool_coins INTEGER DEFAULT 0,
    max_participants INTEGER DEFAULT 0,
    bracket_data JSONB,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- 3. Support & Feedback
CREATE TABLE IF NOT EXISTS support_tickets (
    id TEXT PRIMARY KEY,
    user_id TEXT REFERENCES users(id),
    subject TEXT,
    message TEXT,
    status TEXT,
    priority TEXT,
    assigned_to TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE
);

CREATE TABLE IF NOT EXISTS ticket_replies (
    id TEXT PRIMARY KEY,
    ticket_id TEXT REFERENCES support_tickets(id),
    author_id TEXT,
    author_role TEXT,
    message TEXT,
    attachment_urls JSONB DEFAULT '[]'::jsonb,
    is_internal BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS feedback_v2 (
    id TEXT PRIMARY KEY,
    user_id TEXT REFERENCES users(id),
    content TEXT,
    category TEXT,
    rating INTEGER,
    sentiment TEXT,
    status TEXT,
    assigned_developer_id TEXT,
    internal_reply TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE
);

-- 4. Admin & Moderation
CREATE TABLE IF NOT EXISTS bans (
    id TEXT PRIMARY KEY,
    user_id TEXT REFERENCES users(id),
    reason TEXT,
    expires_at TIMESTAMP WITH TIME ZONE,
    created_by TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    is_active BOOLEAN DEFAULT TRUE
);

CREATE TABLE IF NOT EXISTS reports (
    id TEXT PRIMARY KEY,
    reporter_id TEXT REFERENCES users(id),
    reported_id TEXT REFERENCES users(id),
    reason TEXT,
    category TEXT DEFAULT 'general',
    priority TEXT DEFAULT 'medium',
    evidence_url TEXT,
    internal_comments TEXT,
    status TEXT,
    moderator_id TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE,
    attachment_urls JSONB DEFAULT '[]'::jsonb
);

-- (Add remaining tables for announcements, bi metrics, store, season pass, cms, etc. as needed)
-- This file serves as the comprehensive schema foundation.
