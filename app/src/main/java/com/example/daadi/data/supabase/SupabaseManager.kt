package com.example.daadi.data.supabase

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.example.BuildConfig
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

// Supabase Data Entities
data class SupabaseUser(
    val id: String,
    val username: String,
    val email: String,
    val role: String, // "admin" or "user"
    val createdAt: String,
    val totalGames: Int,
    val wins: Int,
    val losses: Int,
    val isBanned: Boolean = false,
    val isReported: Boolean = false,
    val reportsCount: Int = 0
)

data class SupabaseMatch(
    val id: String,
    val hostName: String,
    val opponentName: String,
    val status: String, // "waiting", "playing", "finished"
    val winner: String?,
    val movesCount: Int,
    val createdAt: String
)

data class SupabaseAnnouncement(
    val id: Int,
    val title: String,
    val content: String,
    val isActive: Boolean,
    val createdAt: String
)

data class SupabaseSystemSetting(
    val key: String,
    val value: String,
    val description: String
)

class SupabaseManager(private val context: Context) {
    private val tag = "SupabaseManager"
    private val scope = CoroutineScope(Dispatchers.IO)
    private val prefs: SharedPreferences = context.getSharedPreferences("daadi_supabase_sim_prefs", Context.MODE_PRIVATE)

    // Moshi Setup
    private val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
    private val userListAdapter = moshi.adapter<List<SupabaseUser>>(Types.newParameterizedType(List::class.java, SupabaseUser::class.java))
    private val userAdapter = moshi.adapter(SupabaseUser::class.java)
    private val matchListAdapter = moshi.adapter<List<SupabaseMatch>>(Types.newParameterizedType(List::class.java, SupabaseMatch::class.java))
    private val matchAdapter = moshi.adapter(SupabaseMatch::class.java)
    private val announcementsAdapter = moshi.adapter<List<SupabaseAnnouncement>>(Types.newParameterizedType(List::class.java, SupabaseAnnouncement::class.java))
    private val announcementAdapter = moshi.adapter(SupabaseAnnouncement::class.java)
    private val settingsAdapter = moshi.adapter<List<SupabaseSystemSetting>>(Types.newParameterizedType(List::class.java, SupabaseSystemSetting::class.java))
    private val settingAdapter = moshi.adapter(SupabaseSystemSetting::class.java)

    // Fetch actual variables from BuildConfig injected via .env
    val supabaseUrl: String = try { BuildConfig.SUPABASE_URL } catch(e: Exception) { "" }
    val supabaseAnonKey: String = try { BuildConfig.SUPABASE_ANON_KEY } catch(e: Exception) { "" }

    val isConfigured: Boolean
        get() = supabaseUrl.isNotBlank() && 
                supabaseUrl != "SUPABASE_URL_PLACEHOLDER" && 
                supabaseAnonKey.isNotBlank() && 
                supabaseAnonKey != "SUPABASE_ANON_KEY_PLACEHOLDER"

    private val client = OkHttpClient()

    // Reactive UI States
    private val _users = MutableStateFlow<List<SupabaseUser>>(emptyList())
    val users: StateFlow<List<SupabaseUser>> = _users.asStateFlow()

    private val _matches = MutableStateFlow<List<SupabaseMatch>>(emptyList())
    val matches: StateFlow<List<SupabaseMatch>> = _matches.asStateFlow()

    private val _announcements = MutableStateFlow<List<SupabaseAnnouncement>>(emptyList())
    val announcements: StateFlow<List<SupabaseAnnouncement>> = _announcements.asStateFlow()

    private val _systemSettings = MutableStateFlow<List<SupabaseSystemSetting>>(emptyList())
    val systemSettings: StateFlow<List<SupabaseSystemSetting>> = _systemSettings.asStateFlow()

    // Active Admin ID & profile settings (For testing roles easily in APK)
    private val _currentAdminRole = MutableStateFlow("admin") // Can toggle to "user" dynamically
    val currentAdminRole: StateFlow<String> = _currentAdminRole.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    init {
        loadInitialData()
    }

    // Current User Session
    private val _currentUser = MutableStateFlow<SupabaseUser?>(null)
    val currentUser: StateFlow<SupabaseUser?> = _currentUser.asStateFlow()

    private fun loadCurrentUserSession() {
        val saved = prefs.getString("current_user_session", null)
        if (saved != null) {
            try {
                val loaded = userAdapter.fromJson(saved)
                _currentUser.value = loaded
                if (loaded != null && loaded.role == "admin") {
                    _currentAdminRole.value = "admin"
                }
            } catch (e: Exception) {}
        }
    }

    fun signUp(email: String, username: String, pass: String, onResult: (Boolean, String?) -> Unit) {
        if (isConfigured) {
            scope.launch {
                val signUpUrl = "$supabaseUrl/auth/v1/signup"
                val bodyMap = mapOf(
                    "email" to email,
                    "password" to pass,
                    "data" to mapOf("username" to username)
                )
                val json = moshi.adapter(Map::class.java).toJson(bodyMap)
                val reqBody = json.toRequestBody("application/json".toMediaType())

                val request = Request.Builder()
                    .url(signUpUrl)
                    .headers(getHeaders())
                    .post(reqBody)
                    .build()

                client.newCall(request).enqueue(object : Callback {
                    override fun onFailure(call: Call, e: IOException) {
                        onResult(false, e.localizedMessage)
                    }

                    override fun onResponse(call: Call, response: Response) {
                        val body = response.body?.string()
                        if (response.isSuccessful && body != null) {
                            // Register user model inside 'users' PostgREST table too!
                            val dateStr = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())
                            val userObj = SupabaseUser(
                                id = UUID.randomUUID().toString(),
                                username = username,
                                email = email,
                                role = "user",
                                createdAt = dateStr,
                                totalGames = 0,
                                wins = 0,
                                losses = 0,
                                isBanned = false,
                                isReported = false,
                                reportsCount = 0
                            )

                            // Post to Rest User endpoint
                            val postJson = userAdapter.toJson(userObj)
                            val postRequest = Request.Builder()
                                .url("$supabaseUrl/rest/v1/users")
                                .headers(getHeaders())
                                .post(postJson.toRequestBody("application/json".toMediaType()))
                                .build()

                            client.newCall(postRequest).enqueue(object : Callback {
                                override fun onFailure(call: Call, e: IOException) {
                                    onResult(true, null) // Auth succeeded anyway
                                }
                                override fun onResponse(call: Call, r: Response) {
                                    scope.launch {
                                        _currentUser.value = userObj
                                        prefs.edit().putString("current_user_session", postJson).apply()
                                        fetchRemoteUsers()
                                    }
                                    onResult(true, null)
                                }
                            })
                        } else {
                            onResult(false, "Signup Failed: Code ${response.code}")
                        }
                    }
                })
            }
        } else {
            // Local simulation:
            val dateStr = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())
            val userObj = SupabaseUser(
                id = "u_sim_" + (1000..9999).random(),
                username = username,
                email = email,
                role = "user",
                createdAt = dateStr,
                totalGames = 0,
                wins = 0,
                losses = 0,
                isBanned = false,
                isReported = false,
                reportsCount = 0
            )
            _users.value = _users.value + userObj
            saveSimulatorUsers()
            _currentUser.value = userObj
            prefs.edit().putString("current_user_session", userAdapter.toJson(userObj)).apply()
            onResult(true, null)
        }
    }

    fun login(email: String, pass: String, onResult: (Boolean, String?) -> Unit) {
        val trimmedEmail = email.trim()
        if (trimmedEmail.equals("veerendrabotla@gmail.com", ignoreCase = true) && pass == "Veeru01@2004") {
            val adminUser = SupabaseUser(
                id = "admin_veerendra",
                username = "VeerendraBotla",
                email = "veerendrabotla@gmail.com",
                role = "admin",
                createdAt = "2026-06-11 05:00",
                totalGames = 0,
                wins = 0,
                losses = 0,
                isBanned = false,
                isReported = false,
                reportsCount = 0
            )
            val currentList = _users.value.toMutableList()
            if (!currentList.any { it.email.equals("veerendrabotla@gmail.com", ignoreCase = true) }) {
                currentList.add(adminUser)
                _users.value = currentList
                saveSimulatorUsers()
            }
            _currentUser.value = adminUser
            _currentAdminRole.value = "admin"
            prefs.edit().putString("current_user_session", userAdapter.toJson(adminUser)).apply()
            onResult(true, null)
            return
        }

        if (isConfigured) {
            scope.launch {
                val loginUrl = "$supabaseUrl/auth/v1/token?grant_type=password"
                val bodyMap = mapOf(
                    "email" to email,
                    "password" to pass
                )
                val json = moshi.adapter(Map::class.java).toJson(bodyMap)
                val reqBody = json.toRequestBody("application/json".toMediaType())

                val request = Request.Builder()
                    .url(loginUrl)
                    .headers(getHeaders())
                    .post(reqBody)
                    .build()

                client.newCall(request).enqueue(object : Callback {
                    override fun onFailure(call: Call, e: IOException) {
                        onResult(false, e.localizedMessage)
                    }

                    override fun onResponse(call: Call, response: Response) {
                        val body = response.body?.string()
                        if (response.isSuccessful && body != null) {
                            // Find matching username from PostgREST 'users' table
                            scope.launch {
                                val userRequest = Request.Builder()
                                    .url("$supabaseUrl/rest/v1/users?email=eq.$email")
                                    .headers(getHeaders())
                                    .get()
                                    .build()
                                client.newCall(userRequest).enqueue(object : Callback {
                                    override fun onFailure(call: Call, e: IOException) {
                                        onResult(false, "Login succeeded but failed to fetch username")
                                    }
                                    override fun onResponse(call: Call, r: Response) {
                                        val uBody = r.body?.string()
                                        if (r.isSuccessful && uBody != null) {
                                            try {
                                                val list = userListAdapter.fromJson(uBody)
                                                if (!list.isNullOrEmpty()) {
                                                    val matchedUser = list[0]
                                                    if (matchedUser.isBanned) {
                                                        onResult(false, "This player profile is currently banned by administrator.")
                                                        return
                                                    }
                                                    _currentUser.value = matchedUser
                                                    prefs.edit().putString("current_user_session", userAdapter.toJson(matchedUser)).apply()
                                                    onResult(true, null)
                                                } else {
                                                    onResult(false, "User profile not found in public database. Try signing up.")
                                                }
                                            } catch (e: Exception) {
                                                onResult(false, "Failed to inspect user profile data.")
                                            }
                                        } else {
                                            onResult(false, "Login invalid or credentials rejected.")
                                        }
                                    }
                                })
                            }
                        } else {
                            onResult(false, "Invalid email or password.")
                        }
                    }
                })
            }
        } else {
            // Local simulation:
            val matched = _users.value.find { it.email == email }
            if (matched != null) {
                if (matched.isBanned) {
                    onResult(false, "This player profile is currently banned by administrator.")
                } else {
                    _currentUser.value = matched
                    prefs.edit().putString("current_user_session", userAdapter.toJson(matched)).apply()
                    onResult(true, null)
                }
            } else {
                onResult(false, "User not found. Try signing up with a new account!")
            }
        }
    }

    fun logout() {
        _currentUser.value = null
        prefs.edit().remove("current_user_session").apply()
    }

    fun setAdminRoleTesting(role: String) {
        _currentAdminRole.value = role
    }

    fun loadInitialData() {
        scope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            if (isConfigured) {
                // Read from real Supabase endpoints
                Log.d(tag, "Contacting remote Supabase API...")
                fetchRemoteUsers()
                fetchRemoteMatches()
                fetchRemoteAnnouncements()
                fetchRemoteSettings()
            } else {
                // Fallback to local storage simulator (fully functional so buttons are active!)
                Log.d(tag, "Supabase Credentials Missing. Loading robust local database simulation...")
                loadSimulatedData()
            }
            _isLoading.value = false
        }
    }

    // --- REAL SUPABASE NETWORK INTEGRATION (REST Client via OkHttp) ---

    private fun getHeaders(): Headers {
        return Headers.Builder()
            .add("apikey", supabaseAnonKey)
            .add("Authorization", "Bearer $supabaseAnonKey")
            .add("Content-Type", "application/json")
            .add("Prefer", "return=representation")
            .build()
    }

    private suspend fun fetchRemoteUsers() = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url("$supabaseUrl/rest/v1/users?select=*&order=createdAt.desc")
            .headers(getHeaders())
            .get()
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                _errorMessage.value = "Failed users sync: ${e.localizedMessage}"
            }

            override fun onResponse(call: Call, response: Response) {
                val body = response.body?.string()
                if (response.isSuccessful && body != null) {
                    try {
                        val parsed = userListAdapter.fromJson(body)
                        if (parsed != null) _users.value = parsed
                    } catch (e: Exception) {
                        Log.e(tag, "Parsed users error", e)
                    }
                }
            }
        })
    }

    private suspend fun fetchRemoteMatches() = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url("$supabaseUrl/rest/v1/matches?select=*&order=createdAt.desc")
            .headers(getHeaders())
            .get()
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e(tag, "Matches request failed", e)
            }

            override fun onResponse(call: Call, response: Response) {
                val body = response.body?.string()
                if (response.isSuccessful && body != null) {
                    try {
                        val parsed = matchListAdapter.fromJson(body)
                        if (parsed != null) _matches.value = parsed
                    } catch (e: Exception) {}
                }
            }
        })
    }

    private suspend fun fetchRemoteAnnouncements() = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url("$supabaseUrl/rest/v1/announcements?select=*&order=id.desc")
            .headers(getHeaders())
            .get()
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {}
            override fun onResponse(call: Call, response: Response) {
                val body = response.body?.string()
                if (response.isSuccessful && body != null) {
                    try {
                        val parsed = announcementsAdapter.fromJson(body)
                        if (parsed != null) _announcements.value = parsed
                    } catch (e: Exception) {}
                }
            }
        })
    }

    private suspend fun fetchRemoteSettings() = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url("$supabaseUrl/rest/v1/system_settings?select=*&order=key.asc")
            .headers(getHeaders())
            .get()
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {}
            override fun onResponse(call: Call, response: Response) {
                val body = response.body?.string()
                if (response.isSuccessful && body != null) {
                    try {
                        val parsed = settingsAdapter.fromJson(body)
                        if (parsed != null) _systemSettings.value = parsed
                    } catch (e: Exception) {}
                }
            }
        })
    }

    // --- REMOTE CRUD OPERATIONS ---

    fun createAnnouncementRemote(title: String, content: String, isActive: Boolean) {
        scope.launch {
            val announcement = mapOf(
                "title" to title,
                "content" to content,
                "isActive" to isActive,
                "createdAt" to SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
            )
            val json = moshi.adapter(Map::class.java).toJson(announcement)
            val reqBody = json.toRequestBody("application/json".toMediaType())

            val request = Request.Builder()
                .url("$supabaseUrl/rest/v1/announcements")
                .headers(getHeaders())
                .post(reqBody)
                .build()

            client.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {}
                override fun onResponse(call: Call, response: Response) {
                    if (response.isSuccessful) {
                        scope.launch { fetchRemoteAnnouncements() }
                    }
                }
            })
        }
    }

    fun updateUserRoleRemote(userId: String, newRole: String) {
        scope.launch {
            val update = mapOf("role" to newRole)
            val json = moshi.adapter(Map::class.java).toJson(update)
            val reqBody = json.toRequestBody("application/json".toMediaType())

            val request = Request.Builder()
                .url("$supabaseUrl/rest/v1/users?id=eq.$userId")
                .headers(getHeaders())
                .patch(reqBody)
                .build()

            client.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {}
                override fun onResponse(call: Call, response: Response) {
                    if (response.isSuccessful) {
                        scope.launch { fetchRemoteUsers() }
                    }
                }
            })
        }
    }

    fun toggleUserBanRemote(userId: String, currentBan: Boolean) {
        scope.launch {
            val update = mapOf("isBanned" to !currentBan)
            val json = moshi.adapter(Map::class.java).toJson(update)
            val reqBody = json.toRequestBody("application/json".toMediaType())

            val request = Request.Builder()
                .url("$supabaseUrl/rest/v1/users?id=eq.$userId")
                .headers(getHeaders())
                .patch(reqBody)
                .build()

            client.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {}
                override fun onResponse(call: Call, response: Response) {
                    if (response.isSuccessful) {
                        scope.launch { fetchRemoteUsers() }
                    }
                }
            })
        }
    }

    fun deleteUserRemote(userId: String) {
        scope.launch {
            val request = Request.Builder()
                .url("$supabaseUrl/rest/v1/users?id=eq.$userId")
                .headers(getHeaders())
                .delete()
                .build()

            client.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {}
                override fun onResponse(call: Call, response: Response) {
                    if (response.isSuccessful) {
                        scope.launch { fetchRemoteUsers() }
                    }
                }
            })
        }
    }


    // --- ROBUST IN-APP DATABASE SIMULATOR (No-Crash fallback) ---

    private fun loadSimulatedData() {
        // Users
        val usersSaved = prefs.getString("sim_users", null)
        if (usersSaved != null) {
            try {
                _users.value = userListAdapter.fromJson(usersSaved) ?: getMockUsersList()
            } catch (e: Exception) {
                _users.value = getMockUsersList()
            }
        } else {
            _users.value = getMockUsersList()
            saveSimulatorUsers()
        }

        // Matches
        val matchesSaved = prefs.getString("sim_matches", null)
        if (matchesSaved != null) {
            try {
                _matches.value = matchListAdapter.fromJson(matchesSaved) ?: getMockMatchesList()
            } catch (e: Exception) {
                _matches.value = getMockMatchesList()
            }
        } else {
            _matches.value = getMockMatchesList()
            saveSimulatorMatches()
        }

        // Announcements
        val annSaved = prefs.getString("sim_announcements", null)
        if (annSaved != null) {
            try {
                _announcements.value = announcementsAdapter.fromJson(annSaved) ?: getMockAnnouncementsList()
            } catch (e: Exception) {
                _announcements.value = getMockAnnouncementsList()
            }
        } else {
            _announcements.value = getMockAnnouncementsList()
            saveSimulatorAnnouncements()
        }

        // Settings
        val setSaved = prefs.getString("sim_settings", null)
        if (setSaved != null) {
            try {
                _systemSettings.value = settingsAdapter.fromJson(setSaved) ?: getMockSettingsList()
            } catch (e: Exception) {
                _systemSettings.value = getMockSettingsList()
            }
        } else {
            _systemSettings.value = getMockSettingsList()
            saveSimulatorSettings()
        }
    }

    private fun saveSimulatorUsers() {
        prefs.edit().putString("sim_users", userListAdapter.toJson(_users.value)).apply()
    }

    private fun saveSimulatorMatches() {
        prefs.edit().putString("sim_matches", matchListAdapter.toJson(_matches.value)).apply()
    }

    private fun saveSimulatorAnnouncements() {
        prefs.edit().putString("sim_announcements", announcementsAdapter.toJson(_announcements.value)).apply()
    }

    private fun saveSimulatorSettings() {
        prefs.edit().putString("sim_settings", settingsAdapter.toJson(_systemSettings.value)).apply()
    }

    private fun getMockUsersList(): List<SupabaseUser> {
        return listOf(
            SupabaseUser("u1", "Arjuna_Sage", "arjuna@gandiva.in", "admin", "2026-05-01 10:00", 98, 81, 17, false, false, 0),
            SupabaseUser("u2", "Chanakya_Guru", "chanakya@takshashila.org", "admin", "2026-05-02 12:00", 145, 130, 15, false, false, 0),
            SupabaseUser("u3", "Yudhisthira_King", "dharmaraja@indrasena.org", "user", "2026-05-15 15:30", 52, 28, 24, false, true, 3), // starts reported for demo
            SupabaseUser("u4", "Shakuni_Dice", "dice_trickster@gandhara.com", "user", "2026-05-20 09:12", 74, 52, 22, true, true, 8), // starts banned and reported
            SupabaseUser("u5", "Karna_Noble", "karna.shield@anga.in", "user", "2026-05-21 17:00", 85, 60, 25, false, false, 0),
            SupabaseUser("u6", "Bhishma_Pitamah", "grandfather@hastinapur.in", "user", "2026-05-22 08:00", 110, 95, 15, false, false, 0)
        )
    }

    private fun getMockMatchesList(): List<SupabaseMatch> {
        return listOf(
            SupabaseMatch("m1", "Arjuna_Sage", "Karna_Noble", "finished", "Arjuna_Sage", 19, "2026-06-11 11:20"),
            SupabaseMatch("m2", "Yudhisthira_King", "Shakuni_Dice", "finished", "Shakuni_Dice", 29, "2026-06-11 10:15"),
            SupabaseMatch("m3", "Chanakya_Guru", "Bhishma_Pitamah", "playing", null, 14, "2026-06-11 09:30"),
            SupabaseMatch("m4", "Bhishma_Pitamah", "Karna_Noble", "waiting", null, 0, "2026-06-11 08:45")
        )
    }

    private fun getMockAnnouncementsList(): List<SupabaseAnnouncement> {
        return listOf(
            SupabaseAnnouncement(
                1, 
                "🌸 SWAGAT HAIN: Daadi V1.0 🌸", 
                "Experience Nine Men's Morris in dynamic M3 glory. Sound effects & interactive real-time multiplay included!", 
                true, 
                "2026-06-11 00:01"
            ),
            SupabaseAnnouncement(
                2, 
                "📜 STATE AUDITING ACTIVATED 📜", 
                "Admins can now manage operational credentials, toggle server availability, and audit user permissions directly.", 
                true, 
                "2026-06-11 01:10"
            )
        )
    }

    private fun getMockSettingsList(): List<SupabaseSystemSetting> {
        return listOf(
            SupabaseSystemSetting("lobby_operational", "active", "Indicates the status of multiplay connections."),
            SupabaseSystemSetting("match_time_limit_sec", "30", "Match decision constraint before automatic move fallback."),
            SupabaseSystemSetting("hint_depth_limit", "4", "Max mathematical lookup level for AI tip evaluation."),
            SupabaseSystemSetting("tournament_registration", "passive", "Indicates whether official tournaments accept registrations.")
        )
    }


    // --- SIMULATED CLIENT OPERATIONS (WRITE INTERACTION COHESION) ---

    fun changeUserRole(userId: String, newRole: String) {
        if (isConfigured) {
            updateUserRoleRemote(userId, newRole)
        } else {
            _users.value = _users.value.map {
                if (it.id == userId) it.copy(role = newRole) else it
            }
            saveSimulatorUsers()
        }
    }

    fun toggleUserBan(userId: String) {
        if (isConfigured) {
            val user = _users.value.find { it.id == userId } ?: return
            toggleUserBanRemote(userId, user.isBanned)
        } else {
            _users.value = _users.value.map {
                if (it.id == userId) it.copy(isBanned = !it.isBanned) else it
            }
            saveSimulatorUsers()
        }
    }

    fun deleteUser(userId: String) {
        if (isConfigured) {
            deleteUserRemote(userId)
        } else {
            _users.value = _users.value.filter { it.id != userId }
            saveSimulatorUsers()
        }
    }

    fun deleteMatch(matchId: String) {
        // Can be simulated or requested online
        if (isConfigured) {
            scope.launch {
                val request = Request.Builder()
                    .url("$supabaseUrl/rest/v1/matches?id=eq.$matchId")
                    .headers(getHeaders())
                    .delete()
                    .build()
                client.newCall(request).enqueue(object : Callback {
                    override fun onFailure(call: Call, e: IOException) {}
                    override fun onResponse(call: Call, response: Response) {
                        if (response.isSuccessful) { scope.launch { fetchRemoteMatches() } }
                    }
                })
            }
        } else {
            _matches.value = _matches.value.filter { it.id != matchId }
            saveSimulatorMatches()
        }
    }

    fun createAnnouncement(title: String, content: String, isActive: Boolean) {
        if (isConfigured) {
            createAnnouncementRemote(title, content, isActive)
        } else {
            val maxId = _announcements.value.maxOfOrNull { it.id } ?: 0
            val nextId = maxId + 1
            val newAnn = SupabaseAnnouncement(
                nextId,
                title,
                content,
                isActive,
                SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())
            )
            _announcements.value = listOf(newAnn) + _announcements.value
            saveSimulatorAnnouncements()
        }
    }

    fun toggleAnnouncementStatus(id: Int) {
        if (isConfigured) {
            val item = _announcements.value.find { it.id == id } ?: return
            scope.launch {
                val update = mapOf("isActive" to !item.isActive)
                val json = moshi.adapter(Map::class.java).toJson(update)
                val reqBody = json.toRequestBody("application/json".toMediaType())

                val request = Request.Builder()
                    .url("$supabaseUrl/rest/v1/announcements?id=eq.$id")
                    .headers(getHeaders())
                    .patch(reqBody)
                    .build()

                client.newCall(request).enqueue(object : Callback {
                    override fun onFailure(call: Call, e: IOException) {}
                    override fun onResponse(call: Call, response: Response) {
                        if (response.isSuccessful) { scope.launch { fetchRemoteAnnouncements() } }
                    }
                })
            }
        } else {
            _announcements.value = _announcements.value.map {
                if (it.id == id) it.copy(isActive = !it.isActive) else it
            }
            saveSimulatorAnnouncements()
        }
    }

    fun deleteAnnouncement(id: Int) {
        if (isConfigured) {
            scope.launch {
                val request = Request.Builder()
                    .url("$supabaseUrl/rest/v1/announcements?id=eq.$id")
                    .headers(getHeaders())
                    .delete()
                    .build()
                client.newCall(request).enqueue(object : Callback {
                    override fun onFailure(call: Call, e: IOException) {}
                    override fun onResponse(call: Call, response: Response) {
                        if (response.isSuccessful) { scope.launch { fetchRemoteAnnouncements() } }
                    }
                })
            }
        } else {
            _announcements.value = _announcements.value.filter { it.id != id }
            saveSimulatorAnnouncements()
        }
    }

    fun updateSystemSetting(key: String, newValue: String) {
        if (isConfigured) {
            scope.launch {
                val update = mapOf("value" to newValue)
                val json = moshi.adapter(Map::class.java).toJson(update)
                val reqBody = json.toRequestBody("application/json".toMediaType())

                val request = Request.Builder()
                    .url("$supabaseUrl/rest/v1/system_settings?key=eq.$key")
                    .headers(getHeaders())
                    .patch(reqBody)
                    .build()

                client.newCall(request).enqueue(object : Callback {
                    override fun onFailure(call: Call, e: IOException) {}
                    override fun onResponse(call: Call, response: Response) {
                        if (response.isSuccessful) { scope.launch { fetchRemoteSettings() } }
                    }
                })
            }
        } else {
            _systemSettings.value = _systemSettings.value.map {
                if (it.key == key) it.copy(value = newValue) else it
            }
            saveSimulatorSettings()
        }
    }

    fun reportUser(userId: String) {
        if (isConfigured) {
            scope.launch {
                val user = _users.value.find { it.id == userId } ?: return@launch
                val update = mapOf(
                    "isReported" to true,
                    "reportsCount" to user.reportsCount + 1
                )
                val json = moshi.adapter(Map::class.java).toJson(update)
                val reqBody = json.toRequestBody("application/json".toMediaType())

                val request = Request.Builder()
                    .url("$supabaseUrl/rest/v1/users?id=eq.$userId")
                    .headers(getHeaders())
                    .patch(reqBody)
                    .build()

                client.newCall(request).enqueue(object : Callback {
                    override fun onFailure(call: Call, e: IOException) {}
                    override fun onResponse(call: Call, response: Response) {
                        if (response.isSuccessful) { scope.launch { fetchRemoteUsers() } }
                    }
                })
            }
        } else {
            _users.value = _users.value.map {
                if (it.id == userId) {
                    it.copy(isReported = true, reportsCount = it.reportsCount + 1)
                } else it
            }
            saveSimulatorUsers()
        }
    }

    fun dismissUserReports(userId: String) {
        if (isConfigured) {
            scope.launch {
                val update = mapOf(
                    "isReported" to false,
                    "reportsCount" to 0
                )
                val json = moshi.adapter(Map::class.java).toJson(update)
                val reqBody = json.toRequestBody("application/json".toMediaType())

                val request = Request.Builder()
                    .url("$supabaseUrl/rest/v1/users?id=eq.$userId")
                    .headers(getHeaders())
                    .patch(reqBody)
                    .build()

                client.newCall(request).enqueue(object : Callback {
                    override fun onFailure(call: Call, e: IOException) {}
                    override fun onResponse(call: Call, response: Response) {
                        if (response.isSuccessful) { scope.launch { fetchRemoteUsers() } }
                    }
                })
            }
        } else {
            _users.value = _users.value.map {
                if (it.id == userId) {
                    it.copy(isReported = false, reportsCount = 0)
                } else it
            }
            saveSimulatorUsers()
        }
    }

    fun registerMatchResult(roomCode: String, hostName: String, opponentName: String, winnerName: String?, movesCount: Int) {
        val dateString = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())
        val statusString = "finished"
        val nextMatchId = "m_res_${(1000..9999).random()}"
        val matchObj = SupabaseMatch(nextMatchId, hostName, opponentName, statusString, winnerName, movesCount, dateString)

        if (isConfigured) {
            scope.launch {
                val matchMap = mapOf(
                    "hostName" to hostName,
                    "opponentName" to opponentName,
                    "status" to statusString,
                    "winner" to winnerName,
                    "movesCount" to movesCount,
                    "createdAt" to dateString
                )
                val json = moshi.adapter(Map::class.java).toJson(matchMap)
                val reqBody = json.toRequestBody("application/json".toMediaType())

                val request = Request.Builder()
                    .url("$supabaseUrl/rest/v1/matches")
                    .headers(getHeaders())
                    .post(reqBody)
                    .build()

                client.newCall(request).enqueue(object : Callback {
                    override fun onFailure(call: Call, e: IOException) {
                        Log.e(tag, "Failed posting match result", e)
                    }
                    override fun onResponse(call: Call, response: Response) {
                        if (response.isSuccessful) {
                            scope.launch { fetchRemoteMatches() }
                        }
                    }
                })

                updateRemoteUserRankStats(hostName, winnerName == hostName, winnerName != null && winnerName != hostName)
                if (opponentName.isNotBlank() && opponentName != "Guest") {
                    updateRemoteUserRankStats(opponentName, winnerName == opponentName, winnerName != null && winnerName != opponentName)
                }
            }
        } else {
            _matches.value = listOf(matchObj) + _matches.value
            saveSimulatorMatches()

            _users.value = _users.value.map { u ->
                if (u.username == hostName) {
                    val isWin = winnerName == hostName
                    val isLoss = winnerName != null && winnerName != hostName
                    u.copy(
                        totalGames = u.totalGames + 1,
                        wins = u.wins + if (isWin) 1 else 0,
                        losses = u.losses + if (isLoss) 1 else 0
                    )
                } else if (u.username == opponentName) {
                    val isWin = winnerName == opponentName
                    val isLoss = winnerName != null && winnerName != opponentName
                    u.copy(
                        totalGames = u.totalGames + 1,
                        wins = u.wins + if (isWin) 1 else 0,
                        losses = u.losses + if (isLoss) 1 else 0
                    )
                } else {
                    u
                }
            }
            saveSimulatorUsers()
        }
    }

    private fun updateRemoteUserRankStats(username: String, isWin: Boolean, isLoss: Boolean) {
        scope.launch {
            val url = "$supabaseUrl/rest/v1/users?username=eq.$username"
            val request = Request.Builder().url(url).headers(getHeaders()).get().build()
            client.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {}
                override fun onResponse(call: Call, response: Response) {
                    val body = response.body?.string()
                    if (response.isSuccessful && !body.isNullOrBlank()) {
                        try {
                            val parsedList = userListAdapter.fromJson(body)
                            if (!parsedList.isNullOrEmpty()) {
                                val u = parsedList[0]
                                val patchBody = mapOf(
                                    "totalGames" to u.totalGames + 1,
                                    "wins" to u.wins + (if (isWin) 1 else 0),
                                    "losses" to u.losses + (if (isLoss) 1 else 0)
                                )
                                val patchJson = moshi.adapter(Map::class.java).toJson(patchBody)
                                val patchRequest = Request.Builder()
                                    .url("$supabaseUrl/rest/v1/users?id=eq.${u.id}")
                                    .headers(getHeaders())
                                    .patch(patchJson.toRequestBody("application/json".toMediaType()))
                                    .build()
                                client.newCall(patchRequest).enqueue(object : Callback {
                                    override fun onFailure(call: Call, e: java.io.IOException) {}
                                    override fun onResponse(call: Call, r: Response) {
                                        if (r.isSuccessful) { scope.launch { fetchRemoteUsers() } }
                                    }
                                })
                            }
                        } catch (e: Exception) {}
                    }
                }
            })
        }
    }
}
