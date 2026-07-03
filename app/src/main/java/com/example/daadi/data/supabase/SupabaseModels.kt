package com.example.daadi.data.supabase

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

// Supabase Data Entities
@JsonClass(generateAdapter = true)
data class SupabaseUser(
    val id: String = "",
    val username: String = "",
    val email: String = "",
    val role: String = "user", // Kept for backward compatibility
    val createdAt: String = "",
    val totalGames: Int = 0,
    val wins: Int = 0,
    val losses: Int = 0,
    val coins: Int = 0,
    val xp: Int = 0,
    val rating: Int = 1000,
    val isBanned: Boolean = false,
    val isReported: Boolean = false,
    val isVerified: Boolean = false,
    val shadowBanned: Boolean = false,
    val reportsCount: Int = 0,
    val internalNotes: String? = null,
    val permissions: List<String> = emptyList(),
    val roles: List<String> = emptyList(),
    @Json(name = "deleted_at") val deletedAt: String? = null,
    @Json(name = "avatar_url") val avatarUrl: String? = null,
    @Json(name = "country_code") val countryCode: String? = null,
    @Json(name = "last_login") val lastLogin: String? = null,
    val country: String? = null,
    val deviceId: String? = null,
    val appVersion: String? = null,
    val metadata: Map<String, Any>? = null
)

@JsonClass(generateAdapter = true)
data class SupabaseSupportTicket(
    val id: String,
    @Json(name = "user_id") val userId: String,
    val subject: String,
    val message: String,
    val status: String, // 'open', 'in_progress', 'resolved', 'closed'
    val priority: String, // 'low', 'medium', 'high', 'critical'
    @Json(name = "assigned_to") val assignedTo: String?,
    @Json(name = "created_at") val createdAt: String,
    @Json(name = "updated_at") val updatedAt: String
)

@JsonClass(generateAdapter = true)
data class SupabaseFeedbackV2(
    val id: String,
    @Json(name = "user_id") val userId: String?,
    val content: String,
    val category: String,
    val rating: Int?,
    val sentiment: String?,
    val status: String,
    @Json(name = "assigned_developer_id") val assignedDeveloperId: String?,
    @Json(name = "internal_reply") val internalReply: String?,
    @Json(name = "created_at") val createdAt: String,
    @Json(name = "updated_at") val updatedAt: String
)

@JsonClass(generateAdapter = true)
data class SupabaseLoginHistory(
    val id: String,
    @Json(name = "user_id") val userId: String,
    @Json(name = "ip_address") val ipAddress: String?,
    @Json(name = "device_id") val deviceId: String?,
    @Json(name = "user_agent") val userAgent: String?,
    val location: String?,
    @Json(name = "created_at") val createdAt: String
)

@JsonClass(generateAdapter = true)
data class SupabaseMatch(
    val id: String,
    val hostName: String,
    val opponentName: String,
    val status: String, // "waiting", "playing", "finished", "paused", "terminated"
    val winner: String?,
    val movesCount: Int,
    val createdAt: String,
    val movesJson: String? = null,
    @Json(name = "host_id") val hostId: String? = null,
    @Json(name = "opponent_id") val opponentId: String? = null,
    @Json(name = "updated_at") val updatedAt: String? = null,
    @Json(name = "match_type") val matchType: String = "multiplayer",
    @Json(name = "latency_ms") val latencyMs: Int = 0,
    @Json(name = "is_ranked") val isRanked: Boolean = false,
    @Json(name = "abandoned_by") val abandonedBy: String? = null
)

@JsonClass(generateAdapter = true)
data class SupabaseBIDailyMetric(
    val date: String,
    val dau: Int,
    val wau: Int,
    val mau: Int,
    val sessions: Int,
    @Json(name = "revenue_usd") val revenueUsd: Double,
    @Json(name = "ad_impressions") val adImpressions: Int,
    @Json(name = "ad_clicks") val adClicks: Int,
    @Json(name = "retention_day_1") val retentionDay1: Double,
    @Json(name = "created_at") val createdAt: String
)

@JsonClass(generateAdapter = true)
data class SupabaseBINotification(
    val id: String,
    val title: String,
    val body: String,
    @Json(name = "target_segment") val targetSegment: String,
    @Json(name = "target_region") val targetRegion: String?,
    @Json(name = "schedule_time") val scheduleTime: String?,
    @Json(name = "sent_at") val sentAt: String?,
    val status: String,
    @Json(name = "open_count") val openCount: Int,
    @Json(name = "failure_count") val failureCount: Int,
    @Json(name = "created_at") val createdAt: String
)

@JsonClass(generateAdapter = true)
data class SupabaseBIAppLog(
    val id: String,
    @Json(name = "user_id") val userId: String?,
    val level: String,
    val category: String,
    val message: String,
    @Json(name = "stack_trace") val stackTrace: String?,
    @Json(name = "device_info") val deviceInfo: Map<String, Any>?,
    @Json(name = "created_at") val createdAt: String
)

@JsonClass(generateAdapter = true)
data class SupabaseBIHealthMetric(
    val id: String,
    @Json(name = "service_name") val serviceName: String,
    val status: String,
    @Json(name = "latency_ms") val latencyMs: Int?,
    @Json(name = "cpu_usage") val cpuUsage: Double?,
    @Json(name = "ram_usage_mb") val ramUsageMb: Int?,
    @Json(name = "active_connections") val activeConnections: Int?,
    @Json(name = "recorded_at") val recordedAt: String
)

@JsonClass(generateAdapter = true)
data class SupabaseAnnouncement(
    val id: Int,
    val title: String,
    val content: String,
    val isActive: Boolean,
    val createdAt: String,
    @Json(name = "updated_at") val updatedAt: String? = null,
    val priority: String = "low",
    @Json(name = "expiry_at") val expiryAt: String? = null,
    @Json(name = "image_url") val imageUrl: String? = null,
    @Json(name = "display_type") val displayType: String = "banner"
)

@JsonClass(generateAdapter = true)
data class SupabaseBan(
    val id: String,
    @Json(name = "user_id") val userId: String,
    val reason: String,
    @Json(name = "expires_at") val expiresAt: String?,
    @Json(name = "created_by") val createdBy: String?,
    @Json(name = "created_at") val createdAt: String,
    @Json(name = "is_active") val isActive: Boolean
)

@JsonClass(generateAdapter = true)
data class SupabaseReport(
    val id: String,
    @Json(name = "reporter_id") val reporterId: String?,
    @Json(name = "reported_id") val reportedId: String,
    val reason: String,
    val category: String = "general",
    val priority: String = "medium",
    @Json(name = "evidence_url") val evidenceUrl: String?,
    @Json(name = "internal_comments") val internalComments: String? = null,
    val status: String,
    @Json(name = "moderator_id") val moderatorId: String?,
    @Json(name = "created_at") val createdAt: String,
    @Json(name = "updated_at") val updatedAt: String
)

@JsonClass(generateAdapter = true)
data class SupabaseAdminSession(
    val id: String,
    @Json(name = "admin_id") val adminId: String,
    @Json(name = "ip_address") val ipAddress: String?,
    @Json(name = "user_agent") val userAgent: String?,
    @Json(name = "last_active") val lastActive: String,
    @Json(name = "is_suspicious") val isSuspicious: Boolean,
    @Json(name = "terminated_at") val terminatedAt: String?
)

@JsonClass(generateAdapter = true)
data class SupabaseDeviceInfo(
    val id: String,
    @Json(name = "user_id") val userId: String,
    @Json(name = "device_id") val deviceId: String,
    val model: String?,
    @Json(name = "os_version") val osVersion: String?,
    @Json(name = "app_version") val appVersion: String?,
    @Json(name = "last_seen") val lastSeen: String
)

@JsonClass(generateAdapter = true)
data class SupabaseAppVersion(
    @Json(name = "version_code") val versionCode: Int,
    @Json(name = "version_name") val versionName: String,
    @Json(name = "is_mandatory") val isMandatory: Boolean,
    @Json(name = "min_supported_version") val minSupportedVersion: Int,
    @Json(name = "release_notes") val releaseNotes: String?,
    @Json(name = "created_at") val createdAt: String
)

@JsonClass(generateAdapter = true)
data class SupabaseMaintenanceSchedule(
    val id: String,
    @Json(name = "start_time") val startTime: String,
    @Json(name = "end_time") val endTime: String,
    val reason: String?,
    @Json(name = "is_active") val isActive: Boolean,
    @Json(name = "created_at") val createdAt: String
)

@JsonClass(generateAdapter = true)
data class SupabaseDataExportRequest(
    val id: String,
    @Json(name = "user_id") val userId: String,
    val status: String,
    @Json(name = "download_url") val downloadUrl: String?,
    @Json(name = "expires_at") val expiresAt: String?,
    @Json(name = "created_at") val createdAt: String
)

@JsonClass(generateAdapter = true)
data class SupabaseSystemSetting(
    val key: String,
    val value: String,
    val description: String
)

@JsonClass(generateAdapter = true)
data class SupabaseFeedback(
    val id: Int,
    val username: String,
    val content: String,
    val category: String, // "bug", "suggest", "other"
    val createdAt: String
)

@JsonClass(generateAdapter = true)
data class SupabaseTournament(
    val id: String,
    val title: String,
    val description: String?,
    val status: String,
    @Json(name = "start_time") val startTime: String?,
    @Json(name = "end_time") val endTime: String?,
    @Json(name = "min_rank") val minRank: Int,
    @Json(name = "entry_fee") val entryFee: Int,
    @Json(name = "prize_pool_coins") val prizePoolCoins: Int,
    @Json(name = "max_participants") val maxParticipants: Int,
    @Json(name = "bracket_data") val bracketData: Map<String, Any>?,
    @Json(name = "created_at") val createdAt: String
)

@JsonClass(generateAdapter = true)
data class SupabaseTournamentParticipant(
    val id: String,
    @Json(name = "tournament_id") val tournamentId: String,
    @Json(name = "user_id") val userId: String,
    val rank: Int?,
    val score: Int,
    @Json(name = "joined_at") val joinedAt: String
)

@JsonClass(generateAdapter = true)
data class SupabaseGameEvent(
    val id: String,
    val title: String,
    val type: String,
    val multiplier: Double,
    @Json(name = "start_time") val startTime: String?,
    @Json(name = "end_time") val endTime: String?,
    @Json(name = "is_active") val isActive: Boolean,
    @Json(name = "created_at") val createdAt: String
)

@JsonClass(generateAdapter = true)
data class SupabaseAntiCheatLog(
    val id: String,
    @Json(name = "user_id") val userId: String?,
    @Json(name = "match_id") val matchId: String?,
    @Json(name = "violation_type") val violationType: String,
    val severity: String,
    val metadata: Map<String, Any>?,
    @Json(name = "created_at") val createdAt: String
)

@JsonClass(generateAdapter = true)
data class SupabaseLeaderboardSnapshot(
    val id: String,
    val scope: String,
    @Json(name = "snapshot_data") val snapshotData: List<Map<String, Any>>,
    @Json(name = "created_at") val createdAt: String
)

@JsonClass(generateAdapter = true)
data class SupabaseAuditLog(
    val id: String,
    @Json(name = "actor_id") val actorId: String?,
    @Json(name = "action_type") val actionType: String,
    @Json(name = "target_table") val targetTable: String?,
    @Json(name = "target_id") val targetId: String?,
    @Json(name = "old_value") val oldValue: Map<String, Any>?,
    @Json(name = "new_value") val newValue: Map<String, Any>?,
    val reason: String?,
    @Json(name = "ip_address") val ipAddress: String?,
    @Json(name = "user_agent") val userAgent: String?,
    val country: String?,
    @Json(name = "device_info") val deviceInfo: String?,
    @Json(name = "screen_name") val screenName: String?,
    @Json(name = "session_id") val sessionId: String?,
    @Json(name = "created_at") val createdAt: String
)

@JsonClass(generateAdapter = true)
data class SupabaseAdminInvitation(
    val id: String,
    val email: String,
    val role: String,
    val permissions: List<String>,
    @Json(name = "invited_by") val invitedBy: String,
    val status: String, // 'pending', 'accepted', 'revoked'
    @Json(name = "expires_at") val expiresAt: String,
    @Json(name = "created_at") val createdAt: String
)

@JsonClass(generateAdapter = true)
data class SupabaseAdminActivity(
    val id: String,
    @Json(name = "admin_id") val adminId: String,
    val action: String,
    @Json(name = "created_at") val createdAt: String
)

// Economy Models
@JsonClass(generateAdapter = true)
data class SupabaseEconomyTransaction(
    val id: String,
    @Json(name = "user_id") val userId: String,
    val amount: Int,
    val currency: String, // "coins", "xp"
    val type: String, // "reward", "purchase", "penalty", "adjustment"
    val source: String, // "match", "daily_reward", "spin_wheel", "admin"
    val reason: String?,
    @Json(name = "created_at") val createdAt: String
)

// Store Models
@JsonClass(generateAdapter = true)
data class SupabaseStoreItem(
    val id: String,
    val name: String,
    val description: String?,
    val type: String, // "pack", "bundle"
    @Json(name = "price_usd") val priceUsd: Double?,
    @Json(name = "price_coins") val priceCoins: Int?,
    val content: Map<String, Any>, // e.g., {"coins": 1000, "xp": 500}
    @Json(name = "image_url") val imageUrl: String?,
    @Json(name = "is_featured") val isFeatured: Boolean,
    @Json(name = "discount_percentage") val discountPercentage: Int,
    @Json(name = "expiry_at") val expiryAt: String?,
    @Json(name = "created_at") val createdAt: String
)

@JsonClass(generateAdapter = true)
data class SupabaseCoupon(
    val id: String,
    val code: String,
    @Json(name = "discount_type") val discountType: String, // "fixed", "percentage"
    val value: Double,
    @Json(name = "max_uses") val maxUses: Int?,
    @Json(name = "used_count") val usedCount: Int,
    @Json(name = "expires_at") val expiresAt: String?,
    @Json(name = "is_active") val isActive: Boolean,
    @Json(name = "created_at") val createdAt: String
)

// Reward Models
@JsonClass(generateAdapter = true)
data class SupabaseDailyReward(
    val day: Int,
    val type: String, // "coins", "xp", "item"
    val amount: Int,
    @Json(name = "item_id") val itemId: String?,
    @Json(name = "image_url") val imageUrl: String?
)

@JsonClass(generateAdapter = true)
data class SupabaseSpinWheelReward(
    val id: String,
    val type: String,
    val amount: Int,
    val weight: Int,
    @Json(name = "image_url") val imageUrl: String?,
    val color: String?
)

// LiveOps Models
@JsonClass(generateAdapter = true)
data class SupabaseLiveOpsEvent(
    val id: String,
    val title: String,
    val description: String?,
    val type: String, // "xp_weekend", "coin_rush", "tournament", "seasonal"
    @Json(name = "xp_multiplier") val xpMultiplier: Double,
    @Json(name = "coin_multiplier") val coinMultiplier: Double,
    @Json(name = "start_time") val startTime: String,
    @Json(name = "end_time") val endTime: String,
    @Json(name = "is_active") val isActive: Boolean,
    val metadata: Map<String, Any>?
)

// Season Pass Models
@JsonClass(generateAdapter = true)
data class SupabaseSeasonPass(
    val id: String,
    val title: String,
    @Json(name = "start_time") val startTime: String,
    @Json(name = "end_time") val endTime: String,
    @Json(name = "is_active") val isActive: Boolean
)

@JsonClass(generateAdapter = true)
data class SupabaseSeasonPassTier(
    val id: String,
    @Json(name = "season_pass_id") val seasonPassId: String,
    @Json(name = "tier_number") val tierNumber: Int,
    @Json(name = "xp_required") val xpRequired: Int,
    @Json(name = "free_reward_type") val freeRewardType: String?,
    @Json(name = "free_reward_amount") val freeRewardAmount: Int?,
    @Json(name = "premium_reward_type") val premiumRewardType: String?,
    @Json(name = "premium_reward_amount") val premiumRewardAmount: Int?
)

// CMS Models
@JsonClass(generateAdapter = true)
data class SupabaseCMSContent(
    val id: String,
    val slug: String,
    val title: String,
    val body: String, // Markdown
    val type: String, // "patch_notes", "faq", "privacy", "terms", "tutorial"
    @Json(name = "image_url") val imageUrl: String?,
    @Json(name = "video_url") val videoUrl: String?,
    val status: String, // "draft", "published"
    @Json(name = "published_at") val publishedAt: String?,
    @Json(name = "created_at") val createdAt: String
)

// Enterprise Analytics Models
@JsonClass(generateAdapter = true)
data class SupabaseBIMetrics(
    val id: String,
    val dau: Int,
    val wau: Int,
    val mau: Int,
    @Json(name = "retention_d1") val retentionD1: Double,
    @Json(name = "retention_d7") val retentionD7: Double,
    @Json(name = "retention_d30") val retentionD30: Double,
    @Json(name = "total_revenue") val totalRevenue: Double,
    val arpu: Double,
    val arppu: Double,
    @Json(name = "churn_rate") val churnRate: Double,
    @Json(name = "country_dist") val countryDistribution: Map<String, Int>,
    @Json(name = "device_dist") val deviceDistribution: Map<String, Int>,
    @Json(name = "version_dist") val versionDistribution: Map<String, Int>,
    @Json(name = "recorded_at") val recordedAt: String
)

@JsonClass(generateAdapter = true)
data class SupabaseCrashLog(
    val id: String,
    val exception: String,
    val stacktrace: String,
    @Json(name = "user_id") val userId: String?,
    @Json(name = "device_model") val deviceModel: String?,
    @Json(name = "os_version") val osVersion: String?,
    @Json(name = "app_version") val appVersion: String?,
    val status: String, // "open", "resolved", "ignored"
    @Json(name = "created_at") val createdAt: String
)

@JsonClass(generateAdapter = true)
data class SupabaseFraudAlert(
    val id: String,
    @Json(name = "user_id") val userId: String,
    val type: String, // "coin_farming", "bot_detection", "referral_abuse", "smurf"
    val confidence: Double,
    val status: String, // "pending", "confirmed", "dismissed"
    val evidence: Map<String, Any>?,
    @Json(name = "created_at") val createdAt: String
)

@JsonClass(generateAdapter = true)
data class SupabaseFinanceReport(
    val id: String,
    val revenue: Double,
    val ads: Double,
    val purchases: Double,
    val refunds: Double,
    val chargebacks: Double,
    @Json(name = "forecast_next_month") val forecastNextMonth: Double,
    @Json(name = "recorded_at") val recordedAt: String
)

@JsonClass(generateAdapter = true)
data class SupabaseQueueMetric(
    val id: String,
    @Json(name = "queue_name") val queueName: String,
    val size: Int,
    @Json(name = "retry_count") val retryCount: Int,
    @Json(name = "dead_letter_count") val deadLetterCount: Int,
    @Json(name = "recorded_at") val recordedAt: String
)

@JsonClass(generateAdapter = true)
data class SupabaseDeviceRecord(
    val id: String,
    @Json(name = "device_id") val deviceId: String,
    @Json(name = "is_rooted") val isRooted: Boolean,
    @Json(name = "is_vpn") val isVpn: Boolean,
    @Json(name = "is_emulator") val isEmulator: Boolean,
    @Json(name = "is_blocked") val isBlocked: Boolean,
    @Json(name = "last_seen") val lastSeen: String
)

@JsonClass(generateAdapter = true)
data class SupabaseAiConfig(
    val id: String? = null,
    val model: String = "gemini-1.5-flash",
    val temperature: Float = 0.7f,
    val maxTokens: Int = 256,
    val systemPrompt: String = "You are an expert Daadi player and commentator.",
    val personality: String = "strategic",
    val isStaged: Boolean = false,
    val version: Int = 1
)
