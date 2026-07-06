package com.example.daadi.data.repository.supabase

import com.squareup.moshi.Types
import com.example.daadi.data.supabase.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.Date
import java.util.UUID
import kotlinx.coroutines.delay
import android.util.Log

class AuthRepository(val network: SupabaseManager) {
    val isConfigured: Boolean get() = network.isConfigured
    val errorMessage: StateFlow<String?> get() = network.errorMessage

    fun userHasPermission(permission: String): Boolean {
        return network.userHasPermission(permission)
    }

    fun hasRole(role: String): Boolean {
            return network._currentUserRoles.value.contains(role)
        }

    val currentUser: StateFlow<SupabaseUser?> = network._currentUser.asStateFlow()

    val passwordResetRequired: StateFlow<Boolean> = network._passwordResetRequired.asStateFlow()

    fun clearPasswordResetRequired() {
        network._passwordResetRequired.value = false
    }

    fun processUserAndPromoteIfAdmin(user: SupabaseUser): SupabaseUser {
            // Removed local email-based auto-promotion for production security compliance (OWASP MASVS).
            // Roles and permissions must be managed strictly server-side.
            return user
        }

    fun signUp(email: String, username: String, pass: String, onResult: (Boolean, String?) -> Unit) {
            val trimmedEmail = email.trim()
            val trimmedUsername = username.trim()
            val trimmedPass = pass.trim()
            
            if (network.isConfigured) {
                network.scope.launch {
                    val signUpUrl = "${network.supabaseUrl}/auth/v1/signup"
                    val bodyMap = mapOf(
                        "email" to trimmedEmail,
                        "password" to trimmedPass,
                        "data" to mapOf("username" to trimmedUsername)
                    )
                    val json = network.moshi.adapter(Map::class.java).toJson(bodyMap)
                    val reqBody = json.toRequestBody("application/json".toMediaType())
    
                    val request = Request.Builder()
                        .url(signUpUrl)
                        .headers(network.getHeaders())
                        .post(reqBody)
                        .build()
    
                    network.client.newCall(request).enqueue(object : Callback {
                        override fun onFailure(call: Call, e: IOException) {
                            network.runOnMain { onResult(false, e.localizedMessage) }
                        }
    
                        override fun onResponse(call: Call, response: Response) {
                            val body = response.body?.string()
                            if (response.isSuccessful && body != null) {
                                val authMap = try {
                                    network.moshi.adapter(Map::class.java).fromJson(body)
                                } catch (e: Exception) { null }
                                
                                val userMap = authMap?.get("user") as? Map<*, *>
                                val authId = userMap?.get("id") as? String ?: authMap?.get("id") as? String ?: UUID.randomUUID().toString()
                                val accessToken = authMap?.get("access_token") as? String
                                
                                if (accessToken != null) {
                                    network.sessionToken = accessToken
                                    network.prefs.edit().putString("supabase_session_token", accessToken).apply()
                                }
    
                                val dateStr = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())
                                val userObj = SupabaseUser( id = authId,
                                    username = trimmedUsername,
                                    email = trimmedEmail,
                                    role = "publicuser", createdAt = dateStr,
                                    totalGames = 0,
                                    wins = 0,
                                    losses = 0
                                )
    
                                val postJson = network.userAdapter.toJson(userObj)
                                val postRequest = Request.Builder()
                                    .url("${network.supabaseUrl}/rest/v1/users")
                                    .headers(network.getHeaders())
                                    .post(postJson.toRequestBody("application/json".toMediaType()))
                                    .build()
    
                                network.client.newCall(postRequest).enqueue(object : Callback {
                                    override fun onFailure(call: Call, e: IOException) {
                                        network.runOnMain { 
                                            val finalUser = network.processUserAndPromoteIfAdmin(userObj)
                                            network._currentUser.value = finalUser
                                            network._currentAdminRole.value = finalUser.role
                                            onResult(true, null) 
                                        }
                                    }
                                    override fun onResponse(call: Call, r: Response) {
                                        network.runOnMain {
                                            val finalUser = network.processUserAndPromoteIfAdmin(userObj)
                                            network._currentUser.value = finalUser
                                            network._currentAdminRole.value = finalUser.role
                                            network.prefs.edit().putString("current_user_session", network.userAdapter.toJson(finalUser)).apply()
                                            network.fetchCurrentPermissions()
                                            network.scope.launch { network.fetchRemoteUsers() }
                                            onResult(true, null)
                                        }
                                    }
                                })
                            } else {
                                val errorMsg = try {
                                    val errorMap = network.moshi.adapter(Map::class.java).fromJson(body ?: "")
                                    errorMap?.get("msg") as? String ?: "Signup Failed: ${response.code}"
                                } catch (e: Exception) { "Signup Failed: ${response.code}" }
                                network.runOnMain { onResult(false, errorMsg) }
                            }
                        }
                    })
                }
            } else {
                // Local simulation:
                val dateStr = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())
                val userObj = SupabaseUser( id = "u_sim_" + (1000..9999).random(),
                    username = username,
                    email = email,
                    role = "publicuser", createdAt = dateStr,
                    totalGames = 0,
                    wins = 0,
                    losses = 0, isBanned = false,
                    isReported = false,
                    reportsCount = 0
                )
                network._users.value = network._users.value + userObj
                network.saveSimulatorUsers()
                val finalUser = network.processUserAndPromoteIfAdmin(userObj)
                network._currentUser.value = finalUser
                network._currentAdminRole.value = finalUser.role
                network.prefs.edit().putString("current_user_session", network.userAdapter.toJson(finalUser)).apply()
                network.runOnMain { onResult(true, null) }
            }
        }

    fun login(email: String, pass: String, onResult: (Boolean, String?) -> Unit) {
            val trimmedEmail = email.trim()
            val trimmedPass = pass.trim()
            
            if (network.isConfigured) {
                network.scope.launch {
                    val loginUrl = "${network.supabaseUrl}/auth/v1/token?grant_type=password"
                    val bodyMap = mapOf(
                        "email" to trimmedEmail,
                        "password" to trimmedPass
                    )
                    val json = network.moshi.adapter(Map::class.java).toJson(bodyMap)
                    val reqBody = json.toRequestBody("application/json".toMediaType())
    
                    val request = Request.Builder()
                        .url(loginUrl)
                        .headers(network.getHeaders())
                        .post(reqBody)
                        .build()
    
                    network.client.newCall(request).enqueue(object : Callback {
                        override fun onFailure(call: Call, e: IOException) {
                            network.runOnMain { onResult(false, e.localizedMessage) }
                        }
    
                        override fun onResponse(call: Call, response: Response) {
                            val body = response.body?.string()
                            if (response.isSuccessful && body != null) {
                                val authMap = try {
                                    network.moshi.adapter(Map::class.java).fromJson(body)
                                } catch (e: Exception) { null }
                                
                                val accessToken = authMap?.get("access_token") as? String
                                if (accessToken != null) {
                                    network.sessionToken = accessToken
                                    network.prefs.edit().putString("supabase_session_token", accessToken).apply()
                                }
    
                                val userMap = authMap?.get("user") as? Map<*, *>
                                val authId = userMap?.get("id") as? String ?: authMap?.get("id") as? String ?: ""
                                val metadata = userMap?.get("user_metadata") as? Map<*, *>
                                val extractedUsername = metadata?.get("username") as? String ?: trimmedEmail.split("@")[0]
    
                                network.scope.launch {
                                    val encodedEmail = java.net.URLEncoder.encode(trimmedEmail, "UTF-8")
                                    val userRequest = Request.Builder()
                                        .url("${network.supabaseUrl}/rest/v1/users?email=eq.$encodedEmail")
                                        .headers(network.getHeaders())
                                        .get()
                                        .build()
                                    network.client.newCall(userRequest).enqueue(object : Callback {
                                        override fun onFailure(call: Call, e: IOException) {
                                            network.runOnMain { onResult(false, "Login succeeded but failed to fetch profile: ${e.localizedMessage}") }
                                        }
                                        override fun onResponse(call: Call, r: Response) {
                                            val uBody = r.body?.string()
                                            if (r.isSuccessful && uBody != null) {
                                                try {
                                                    val list = network.userListAdapter.fromJson(uBody)
                                                    if (!list.isNullOrEmpty()) {
                                                        val matchedUser = list[0]
                                                        if (matchedUser.isBanned) {
                                                            network.runOnMain { onResult(false, "This player profile is currently banned by administrator.") }
                                                            return
                                                        }
                                                        val finalUser = network.processUserAndPromoteIfAdmin(matchedUser)
                                                        network.runOnMain {
                                                            network._currentUser.value = finalUser
                                                            network._currentAdminRole.value = finalUser.role
                                                            network.prefs.edit().putString("current_user_session", network.userAdapter.toJson(finalUser)).apply()
                                                            network.fetchCurrentPermissions()
                                                            onResult(true, null)
                                                        }
                                                    } else {
                                                        val dateStr = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())
                                                        val newUser = SupabaseUser( id = authId,
                                                            username = extractedUsername,
                                                            email = trimmedEmail,
                                                            role = "publicuser", createdAt = dateStr,
                                                            totalGames = 0,
                                                            wins = 0,
                                                            losses = 0
                                                        )
                                                        val finalNewUser = network.processUserAndPromoteIfAdmin(newUser)
                                                        val newUserJson = network.userAdapter.toJson(finalNewUser)
                                                        val createReq = Request.Builder()
                                                            .url("${network.supabaseUrl}/rest/v1/users")
                                                            .headers(network.getHeaders())
                                                            .post(newUserJson.toRequestBody("application/json".toMediaType()))
                                                            .build()
                                                        network.client.newCall(createReq).enqueue(object: Callback {
                                                            override fun onFailure(call: Call, e: IOException) {
                                                                network.runOnMain { 
                                                                    network._currentUser.value = finalNewUser
                                                                    network._currentAdminRole.value = finalNewUser.role
                                                                    onResult(true, null) 
                                                                }
                                                            }
                                                            override fun onResponse(call: Call, res: Response) {
                                                                network.runOnMain {
                                                                    network._currentUser.value = finalNewUser
                                                                    network._currentAdminRole.value = finalNewUser.role
                                                                    network.prefs.edit().putString("current_user_session", newUserJson).apply()
                                                                    network.fetchCurrentPermissions()
                                                                    onResult(true, null)
                                                                }
                                                            }
                                                        })
                                                    }
                                                } catch (e: Exception) {
                                                    network.runOnMain { onResult(false, "Failed to parse user profile: ${e.localizedMessage}") }
                                                }
                                            } else {
                                                network.runOnMain { onResult(false, "Failed to fetch user record (Code: ${r.code}).") }
                                            }
                                        }
                                    })
                                }
                            } else {
                                val errorMsg = try {
                                    val errorMap = network.moshi.adapter(Map::class.java).fromJson(body ?: "")
                                    errorMap?.get("error_description") as? String ?: errorMap?.get("msg") as? String ?: "Login Failed (${response.code})"
                                } catch (e: Exception) { "Invalid email or password." }
                                network.runOnMain { onResult(false, errorMsg) }
                            }
                        }
                    })
                }
            } else {
                // Local simulation:
                val matched = network._users.value.find { it.email == email }
                if (matched != null) {
                    if (matched.isBanned) {
                        network.runOnMain { onResult(false, "This player profile is currently banned by administrator.") }
                    } else {
                        val finalUser = network.processUserAndPromoteIfAdmin(matched)
                        network._currentUser.value = finalUser
                        network._currentAdminRole.value = finalUser.role
                        network.prefs.edit().putString("current_user_session", network.userAdapter.toJson(finalUser)).apply()
                        network.runOnMain { onResult(true, null) }
                    }
                } else {
                    network.runOnMain { onResult(false, "User not found. Try signing up with a new account!") }
                }
            }
        }

    fun signInWithOAuth(provider: String, context: android.content.Context, onResult: (Boolean, String?) -> Unit) {
            if (network.isConfigured) {
                val authorizeUrl = "${network.supabaseUrl}/auth/v1/authorize?provider=${provider.lowercase()}&redirect_to=daadi://auth-callback"
                try {
                    val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(authorizeUrl))
                    intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(intent)
                    onResult(true, "Launching browser for $provider Sign-In...")
                } catch (e: Exception) {
                    onResult(false, "Could not open web browser: ${e.localizedMessage}")
                }
            } else {
                // Mock OAuth login:
                val randomSuffix = (1000..9999).random()
                val oAuthName = if (provider.lowercase() == "google") "Google_Player_$randomSuffix" else "Facebook_Player_$randomSuffix"
                val oAuthEmail = if (provider.lowercase() == "google") "g_user_$randomSuffix@gmail.com" else "fb_user_$randomSuffix@facebook.com"
                
                val dateStr = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())
                val userObj = SupabaseUser( id = "u_sim_oauth_$randomSuffix",
                    username = oAuthName,
                    email = oAuthEmail,
                    role = "publicuser", createdAt = dateStr,
                    totalGames = 0,
                    wins = 0,
                    losses = 0, isBanned = false,
                    isReported = false,
                    reportsCount = 0
                )
                
                network._users.value = network._users.value + userObj
                network.saveSimulatorUsers()
                val finalUser = network.processUserAndPromoteIfAdmin(userObj)
                network._currentUser.value = finalUser
                network._currentAdminRole.value = finalUser.role
                network.prefs.edit().putString("current_user_session", network.userAdapter.toJson(finalUser)).apply()
                
                network.runOnMain {
                    onResult(true, "$provider login successful as ${finalUser.username}!")
                }
            }
        }

    fun resetPassword(email: String, onResult: (Boolean, String?) -> Unit) {
            if (network.isConfigured) {
                network.scope.launch {
                    val resetUrl = "${network.supabaseUrl}/auth/v1/recover?redirect_to=daadi://auth-callback"
                    val bodyMap = mapOf("email" to email)
                    val json = network.moshi.adapter(Map::class.java).toJson(bodyMap)
                    val reqBody = json.toRequestBody("application/json".toMediaType())
    
                    val request = Request.Builder()
                        .url(resetUrl)
                        .headers(network.getHeaders())
                        .post(reqBody)
                        .build()
    
                    network.client.newCall(request).enqueue(object : Callback {
                        override fun onFailure(call: Call, e: IOException) {
                            network.runOnMain { onResult(false, e.localizedMessage) }
                        }
    
                        override fun onResponse(call: Call, response: Response) {
                            if (response.isSuccessful) {
                                network.runOnMain { onResult(true, "Reset link sent! Check your email inbox.") }
                            } else {
                                val body = response.body?.string()
                                val errorMsg = try {
                                    val errorMap = network.moshi.adapter(Map::class.java).fromJson(body ?: "")
                                    errorMap?.get("msg") as? String ?: "Failed to send reset link."
                                } catch (e: Exception) { "Unknown error occurred" }
                                network.runOnMain { onResult(false, errorMsg) }
                            }
                        }
                    })
                }
            } else {
                // Local simulation
                val exists = network._users.value.any { it.email == email }
                if (exists) {
                    network.runOnMain { onResult(true, "[SIMULATION] Recovery email sent to $email") }
                } else {
                    network.runOnMain { onResult(false, "No account found with this email.") }
                }
            }
        }

    fun updatePassword(newPassword: String, onResult: (Boolean, String?) -> Unit) {
            if (network.isConfigured) {
                val token = network.sessionToken
                if (token.isNullOrBlank()) {
                    network.runOnMain { onResult(false, "No active session found to update password.") }
                    return
                }
                network.scope.launch {
                    val userUrl = "${network.supabaseUrl}/auth/v1/user"
                    val bodyMap = mapOf("password" to newPassword)
                    val json = network.moshi.adapter(Map::class.java).toJson(bodyMap)
                    val reqBody = json.toRequestBody("application/json".toMediaType())
    
                    val request = Request.Builder()
                        .url(userUrl)
                        .header("apikey", network.supabaseAnonKey)
                        .header("Authorization", "Bearer $token")
                        .header("Content-Type", "application/json")
                        .put(reqBody)
                        .build()
    
                    network.client.newCall(request).enqueue(object : Callback {
                        override fun onFailure(call: Call, e: IOException) {
                            network.runOnMain { onResult(false, e.localizedMessage) }
                        }
    
                        override fun onResponse(call: Call, response: Response) {
                            if (response.isSuccessful) {
                                network.runOnMain { onResult(true, "Password updated successfully!") }
                            } else {
                                val body = response.body?.string()
                                val errorMsg = try {
                                    val errorMap = network.moshi.adapter(Map::class.java).fromJson(body ?: "")
                                    errorMap?.get("msg") as? String ?: "Failed to update password."
                                } catch (e: Exception) { "Unknown error occurred" }
                                network.runOnMain { onResult(false, errorMsg) }
                            }
                        }
                    })
                }
            } else {
                // Local simulation
                val current = network._currentUser.value
                if (current != null) {
                    network.runOnMain { onResult(true, "[SIMULATION] Password updated successfully for ${current.username}!") }
                } else {
                    network.runOnMain { onResult(false, "No logged in user found to update password.") }
                }
            }
        }

    fun logout() {
            network.sessionToken = null
            network.prefs.edit().remove("supabase_session_token").apply()
            network._currentUser.value = null
            network._currentUserPermissions.value = emptySet()
            network._currentUserRoles.value = emptySet()
            network._currentAdminRole.value = "user"
            network.prefs.edit().remove("current_user_session").apply()
        }

    fun logAdminAction(action: String, target: String) {
        network.logAdminAction(action, target)
    }

    fun refreshUserProfile(onResult: (Boolean) -> Unit = {}
    
    ) {
            val user = network._currentUser.value ?: return
            if (!network.isConfigured) return
            
            network.scope.launch {
                val request = Request.Builder()
                    .url("${network.supabaseUrl}/rest/v1/users?id=eq.${user.id}")
                    .headers(network.getHeaders())
                    .get()
                    .build()
                
                network.client.newCall(request).enqueue(object : Callback {
                    override fun onFailure(call: Call, e: IOException) {
                        network.runOnMain { onResult(false) }
                    }
                    override fun onResponse(call: Call, response: Response) {
                        val body = response.body?.string()
                        if (response.isSuccessful && body != null) {
                            try {
                                val list = network.userListAdapter.fromJson(body)
                                if (!list.isNullOrEmpty()) {
                                    val matchedUser = list[0]
                                    network.runOnMain {
                                        val finalUser = network.processUserAndPromoteIfAdmin(matchedUser)
                                        network._currentUser.value = finalUser
                                        network._currentAdminRole.value = finalUser.role
                                        network.prefs.edit().putString("current_user_session", network.userAdapter.toJson(finalUser)).apply()
                                        network.fetchCurrentPermissions()
                                        onResult(true)
                                    }
                                } else {
                                    network.runOnMain { onResult(false) }
                                }
                            } catch (e: Exception) {
                                network.runOnMain { onResult(false) }
                            }
                        } else {
                            network.runOnMain { onResult(false) }
                        }
                    }
                })
            }
        }

    fun deleteUser(userId: String) {
            if (network.isConfigured) {
                network.deleteUserRemote(userId)
            } else {
                network._users.value = network._users.value.filter { it.id != userId }
                network.saveSimulatorUsers()
            }
        }

    fun updateUserVerification(userId: String, isVerified: Boolean) {
            if (!network.userHasPermission("manage_users")) return
            network.scope.launch {
                try {
                    val body = "{\"is_verified\": $isVerified}".toRequestBody("application/json".toMediaType())
                    val request = Request.Builder()
                        .url("${network.supabaseUrl}/rest/v1/users?id=eq.$userId")
                        .headers(network.getHeaders())
                        .patch(body)
                        .build()
                    network.client.newCall(request).execute().use { response ->
                        if (response.isSuccessful) {
                            network.fetchRemoteUsers()
                            network.logAdminAction("UPDATE_VERIFICATION", "User: $userId | Verified: $isVerified")
                        }
                    }
                } catch (e: Exception) {}
            }
        }

    fun setShadowBan(userId: String, enabled: Boolean) {
            if (!network.userHasPermission("moderate_users")) return
            network.scope.launch {
                try {
                    val body = "{\"shadow_banned\": $enabled}".toRequestBody("application/json".toMediaType())
                    val request = Request.Builder()
                        .url("${network.supabaseUrl}/rest/v1/users?id=eq.$userId")
                        .headers(network.getHeaders())
                        .patch(body)
                        .build()
                    network.client.newCall(request).execute().use { response ->
                        if (response.isSuccessful) {
                            network.fetchRemoteUsers()
                            network.logAdminAction("SHADOW_BAN", "User: $userId | Enabled: $enabled")
                        }
                    }
                } catch (e: Exception) {}
            }
        }

    fun updateInternalNotes(userId: String, notes: String) {
            if (!network.userHasPermission("moderate_users")) return
            network.scope.launch {
                try {
                    val updateMap = mapOf("internal_notes" to notes)
                    val body = network.moshi.adapter(Map::class.java).toJson(updateMap).toRequestBody("application/json".toMediaType())
                    val request = Request.Builder()
                        .url("${network.supabaseUrl}/rest/v1/users?id=eq.$userId")
                        .headers(network.getHeaders())
                        .patch(body)
                        .build()
                    network.client.newCall(request).execute().use { response ->
                        if (response.isSuccessful) {
                            network.fetchRemoteUsers()
                            network.logAdminAction("UPDATE_NOTES", "User: $userId")
                        }
                    }
                } catch (e: Exception) {}
            }
        }

    fun resetUsername(userId: String, newName: String) {
            if (!network.userHasPermission("manage_users")) return
            network.scope.launch {
                try {
                    val body = "{\"username\": \"$newName\"}".toRequestBody("application/json".toMediaType())
                    val request = Request.Builder()
                        .url("${network.supabaseUrl}/rest/v1/users?id=eq.$userId")
                        .headers(network.getHeaders())
                        .patch(body)
                        .build()
                    network.client.newCall(request).execute().use { response ->
                        if (response.isSuccessful) {
                            network.fetchRemoteUsers()
                            network.logAdminAction("RESET_USERNAME", "User: $userId | New: $newName")
                        }
                    }
                } catch (e: Exception) {}
            }
        }

    fun resetAvatar(userId: String, newAvatarUrl: String) {
            if (!network.userHasPermission("manage_users")) return
            network.scope.launch {
                try {
                    val body = "{\"avatar_url\": \"$newAvatarUrl\"}".toRequestBody("application/json".toMediaType())
                    val request = Request.Builder()
                        .url("${network.supabaseUrl}/rest/v1/users?id=eq.$userId")
                        .headers(network.getHeaders())
                        .patch(body)
                        .build()
                    network.client.newCall(request).execute().use { response ->
                        if (response.isSuccessful) {
                            network.fetchRemoteUsers()
                            network.logAdminAction("RESET_AVATAR", "User: $userId")
                        }
                    }
                } catch (e: Exception) {}
            }
        }

    fun forceLogout(userId: String) {
            if (!network.userHasPermission("manage_users")) return
            network.scope.launch {
                try {
                    val body = "{\"metadata\": {\"force_logout_at\": \"${System.currentTimeMillis()}\"}}".toRequestBody("application/json".toMediaType())
                    val request = Request.Builder()
                        .url("${network.supabaseUrl}/rest/v1/users?id=eq.$userId")
                        .headers(network.getHeaders())
                        .patch(body)
                        .build()
                    network.client.newCall(request).execute().use { response ->
                        if (response.isSuccessful) {
                            network.logAdminAction("FORCE_LOGOUT", userId)
                        }
                    }
                } catch (e: Exception) {}
            }
        }
}

class UserRepository(val network: SupabaseManager, private val userDao: com.example.daadi.data.local.UserDao) {
    val users: StateFlow<List<SupabaseUser>> = network._users.asStateFlow()

    val cachedUsers: Flow<List<com.example.daadi.data.local.CachedUser>> = userDao.getAllUsers()

    val userLoginHistory: StateFlow<List<SupabaseLoginHistory>> = network._userLoginHistory.asStateFlow()

    fun toggleUserBan(userId: String, currentStatus: Boolean) {
            if (network.isConfigured) {
                network.scope.launch {
                    delay(400) // Transaction overhead simulation
                    network._profiles.update { list -> 
                        list.map { if (it.id == userId) it.copy( isBanned = !currentStatus) else it }
                    }
                    val user = network._profiles.value.find { it.id == userId }
                    network.logAdminAction(if (!currentStatus) "BAN_USER" else "UNBAN_USER", user?.email ?: userId)
                    network.toggleUserBanRemote(userId, currentStatus)
                    
                    // Update cache
                    network.fetchRemoteUsers()
                }
            } else {
                network._users.value = network._users.value.map {
                    if (it.id == userId) it.copy( isBanned = !currentStatus) else it
                }
                network.saveSimulatorUsers()
                val user = network._users.value.find { it.id == userId }
                network.logAdminAction(if (!currentStatus) "BAN_USER" else "UNBAN_USER", user?.username ?: userId)
            }
        }

    fun syncUsersToCache() {
        if (!network.isConfigured) return
        network.scope.launch {
            val remoteUsers = network._users.value
            val cached = remoteUsers.map { 
                com.example.daadi.data.local.CachedUser(
                    id = it.id,
                    username = it.username,
                    email = it.email,
                    role = it.role,
                    createdAt = it.createdAt,
                    totalGames = it.totalGames,
                    wins = it.wins,
                    losses = it.losses,
                    coins = it.coins,
                    xp = it.xp,
                    rating = it.rating,
                    isBanned = it.isBanned,
                    isVerified = it.isVerified
                )
            }
            userDao.insertUsers(cached)
        }
    }

    fun toggleUserBan(userId: String) {
            if (network.isConfigured) {
                val user = network._users.value.find { it.id == userId } ?: return
                network.toggleUserBanRemote(userId, user.isBanned)
            } else {
                network._users.value = network._users.value.map {
                    if (it.id == userId) it.copy( isBanned = !it.isBanned) else it
                }
                network.saveSimulatorUsers()
            }
        }

    fun fetchRemoteUsers() {
        network.scope.launch { network.fetchRemoteUsers() }
    }

    fun fetchLoginHistory(userId: String) {
            if (!network.isConfigured) return
            network.scope.launch {
                try {
                    val request = Request.Builder()
                        .url("${network.supabaseUrl}/rest/v1/login_history?user_id=eq.$userId&select=*&order=created_at.desc&limit=20")
                        .headers(network.getHeaders())
                        .get()
                        .build()
                    network.client.newCall(request).execute().use { response ->
                        if (response.isSuccessful) {
                            val json = response.body?.string() ?: ""
                            network._userLoginHistory.value = network.loginHistoryListAdapter.fromJson(json) ?: emptyList()
                        }
                    }
                } catch (e: Exception) {}
            }
        }
}

class AdminRepository(val network: SupabaseManager) {
    val adminInvitations: StateFlow<List<SupabaseAdminInvitation>> = network._adminInvitations

    val adminActivities: StateFlow<List<SupabaseAdminActivity>> = network._adminActivities

    val adminSessions: StateFlow<List<SupabaseAdminSession>> = network._adminSessions

    val adminAuditLogs: StateFlow<List<AdminAuditLog>> = network.adminAuditLogs

    fun requestDataExport(modules: List<String>, format: String, onResult: (Boolean, String) -> Unit) {
        network.scope.launch {
            try {
                val userId = network.currentUser.value?.id ?: return@launch
                val exportRequest = mapOf(
                    "user_id" to userId,
                    "status" to "pending",
                    "modules" to modules,
                    "format" to format
                )
                val json = network.moshi.adapter(Map::class.java).toJson(exportRequest)
                val request = Request.Builder()
                    .url("${network.supabaseUrl}/rest/v1/data_export_requests")
                    .headers(network.getHeaders())
                    .post(json.toRequestBody("application/json".toMediaType()))
                    .build()
                val response = network.client.newCall(request).execute()
                if (response.isSuccessful) {
                    onResult(true, "Export request submitted successfully. You will be notified when it's ready.")
                } else {
                    onResult(false, "Failed to submit export request: ${response.code}")
                }
            } catch (e: Exception) {
                onResult(false, e.localizedMessage ?: "Network error")
            }
        }
    }

    private val _approvalRequests = MutableStateFlow<List<SupabaseApprovalRequest>>(emptyList())
    val approvalRequests: StateFlow<List<SupabaseApprovalRequest>> = _approvalRequests.asStateFlow()

    private val _scheduledTasks = MutableStateFlow<List<SupabaseScheduledTask>>(emptyList())
    val scheduledTasks: StateFlow<List<SupabaseScheduledTask>> = _scheduledTasks.asStateFlow()

    fun fetchWorkflowApprovals() {
        network.scope.launch {
            try {
                val request = Request.Builder()
                    .url("${network.supabaseUrl}/rest/v1/approval_requests?select=*")
                    .headers(network.getHeaders())
                    .build()
                val response = network.client.newCall(request).execute()
                if (response.isSuccessful) {
                    val json = response.body?.string() ?: "[]"
                    val type = Types.newParameterizedType(List::class.java, SupabaseApprovalRequest::class.java)
                    val adapter = network.moshi.adapter<List<SupabaseApprovalRequest>>(type)
                    _approvalRequests.value = adapter.fromJson(json) ?: emptyList()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun fetchScheduledTasks() {
        network.scope.launch {
            try {
                val request = Request.Builder()
                    .url("${network.supabaseUrl}/rest/v1/scheduled_tasks?select=*")
                    .headers(network.getHeaders())
                    .build()
                val response = network.client.newCall(request).execute()
                if (response.isSuccessful) {
                    val json = response.body?.string() ?: "[]"
                    val type = Types.newParameterizedType(List::class.java, SupabaseScheduledTask::class.java)
                    val adapter = network.moshi.adapter<List<SupabaseScheduledTask>>(type)
                    _scheduledTasks.value = adapter.fromJson(json) ?: emptyList()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun approveRequest(requestId: String) {
        // Implementation for real API call
        network.scope.launch {
            try {
                val update = mapOf("status" to "approved")
                val jsonBody = network.moshi.adapter(Map::class.java).toJson(update)
                val request = Request.Builder()
                    .url("${network.supabaseUrl}/rest/v1/approval_requests?id=eq.$requestId")
                    .headers(network.getHeaders())
                    .patch(jsonBody.toRequestBody("application/json".toMediaType()))
                    .build()
                network.client.newCall(request).execute()
                fetchWorkflowApprovals()
            } catch (e: Exception) { e.printStackTrace() }
        }
    }

    fun rejectRequest(requestId: String) {
        // Implementation for real API call
        network.scope.launch {
            try {
                val update = mapOf("status" to "rejected")
                val jsonBody = network.moshi.adapter(Map::class.java).toJson(update)
                val request = Request.Builder()
                    .url("${network.supabaseUrl}/rest/v1/approval_requests?id=eq.$requestId")
                    .headers(network.getHeaders())
                    .patch(jsonBody.toRequestBody("application/json".toMediaType()))
                    .build()
                network.client.newCall(request).execute()
                fetchWorkflowApprovals()
            } catch (e: Exception) { e.printStackTrace() }
        }
    }

    fun promoteUserToRole(userId: String, role: String, permissions: List<String>) {
            if (!network.userHasPermission("assign_roles")) return
            network.scope.launch {
                val update = mapOf("role" to role, "permissions" to permissions)
                val json = network.moshi.adapter(Map::class.java).toJson(update)
                val request = Request.Builder()
                    .url("${network.supabaseUrl}/rest/v1/users?id=eq.$userId")
                    .headers(network.getHeaders())
                    .patch(json.toRequestBody("application/json".toMediaType()))
                    .build()
    
                network.client.newCall(request).enqueue(object : Callback {
                    override fun onFailure(call: Call, e: IOException) {}
                    override fun onResponse(call: Call, response: Response) {
                        if (response.isSuccessful) {
                            logAudit("PROMOTE_USER", "users", userId, newValue = update)
                            network.scope.launch { network.fetchRemoteUsers() }
                        }
                        response.close()
                    }
                })
            }
        }

    fun demoteAdmin(userId: String) {
            promoteUserToRole(userId, "player", emptyList())
        }

    fun terminateAdminSession(sessionId: String) {
            if (!network.userHasPermission("manage_admins")) return
            network.scope.launch {
                val update = mapOf("terminated_at" to java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", java.util.Locale.getDefault()).format(java.util.Date()))
                val json = network.moshi.adapter(Map::class.java).toJson(update)
                val request = Request.Builder()
                    .url("${network.supabaseUrl}/rest/v1/admin_sessions?id=eq.$sessionId")
                    .headers(network.getHeaders())
                    .patch(json.toRequestBody("application/json".toMediaType()))
                    .build()
    
                network.client.newCall(request).enqueue(object : Callback {
                    override fun onFailure(call: Call, e: IOException) {}
                    override fun onResponse(call: Call, response: Response) {
                        if (response.isSuccessful) {
                            logAudit("TERMINATE_SESSION", "admin_sessions", sessionId)
                            fetchAdminSessions()
                        }
                        response.close()
                    }
                })
            }
        }

    fun fetchAdminSessions() {
            if (!network.userHasPermission("view_logs")) return
            network.scope.launch {
                val request = Request.Builder()
                    .url("${network.supabaseUrl}/rest/v1/admin_sessions?select=*&order=created_at.desc")
                    .headers(network.getHeaders())
                    .get()
                    .build()
                network.client.newCall(request).enqueue(object : Callback {
                    override fun onFailure(call: Call, e: IOException) {}
                    override fun onResponse(call: Call, response: Response) {
                        if (response.isSuccessful) {
                            val sessions = network.sessionListAdapter.fromJson(response.body?.string() ?: "[]") ?: emptyList()
                            network._adminSessions.value = sessions
                        }
                        response.close()
                    }
                })
            }
        }

    val auditLogs: StateFlow<List<SupabaseAuditLog>> = network._auditLogs.asStateFlow()

    fun logAudit(
            action: String,
            targetTable: String? = null,
            targetId: String? = null,
            oldValue: Map<String, Any>? = null,
            newValue: Map<String, Any>? = null,
            reason: String? = null,
            screenName: String? = null
        ) {
            network.logAudit(action, targetTable, targetId, oldValue, newValue, reason, screenName)
        }
}

class AnalyticsRepository(val network: SupabaseManager) {
    suspend fun askAiAssistant(query: String): String = network.askAiAssistant(query)

    val biMetrics: StateFlow<List<SupabaseBIMetrics>> = network._biMetrics

    val crashLogs: StateFlow<List<SupabaseCrashLog>> = network._crashLogs

    val fraudAlerts: StateFlow<List<SupabaseFraudAlert>> = network._fraudAlerts

    val financeReports: StateFlow<List<SupabaseFinanceReport>> = network._financeReports

    val queueMetrics: StateFlow<List<SupabaseQueueMetric>> = network._queueMetrics

    val deviceRecords: StateFlow<List<SupabaseDeviceRecord>> = network._deviceRecords

    val isSyncing: StateFlow<Boolean> = network._isSyncing.asStateFlow()

    val healthMetrics: StateFlow<DbHealthMetrics> = network._healthMetrics.asStateFlow()

    val adTelemetry: StateFlow<AdTelemetry> = network._adTelemetry.asStateFlow()

    val biDailyMetrics: StateFlow<List<SupabaseBIDailyMetric>> = network._biDailyMetrics.asStateFlow()

    val biNotifications: StateFlow<List<SupabaseBINotification>> = network._biNotifications.asStateFlow()

    val biAppLogs: StateFlow<List<SupabaseBIAppLog>> = network._biAppLogs.asStateFlow()

    val biHealthMetrics: StateFlow<List<SupabaseBIHealthMetric>> = network._biHealthMetrics.asStateFlow()

    fun resetAdTelemetryRemote() {
            network.logAdminAction("FLUSH_AD_ANALYTICS", "system")
            network._adTelemetry.value = AdTelemetry( lastFlushTimestamp = System.currentTimeMillis())
            // In real env, we'd also zero out DB counters for aggregate analytics
        }

    fun incrementAdRequests() {
            network._adTelemetry.update { 
                val newReq = it.totalRequests + 1
                it.copy( totalRequests = newReq, fillRate = if (newReq > 0) (it.filledImpressions.toFloat() / newReq) * 100 else 0f
                )
            }
        }

    fun incrementAdImpressions() {
            network._adTelemetry.update {
                val newImp = it.filledImpressions + 1
                it.copy( filledImpressions = newImp, fillRate = if (it.totalRequests > 0) (newImp.toFloat() / it.totalRequests) * 100 else 100f, estimatedEcpm = (50..450).random() / 100f // Improved simulation of dynamic eCPM from floor price meditation
                )
            }
        }

    fun logBIEvent(category: String, message: String, level: String = "INFO", stackTrace: String? = null) {
            if (network.isConfigured) {
                val user = network.currentUser.value
                val payload = mutableMapOf(
                    "category" to category,
                    "message" to message,
                    "level" to level,
                    "created_at" to SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault()).format(Date())
                )
                if (user != null) payload["user_id"] = user.id
                if (stackTrace != null) payload["stack_trace"] = stackTrace
                
                val json = network.moshi.adapter(Map::class.java).toJson(payload)
                val request = Request.Builder()
                    .url("${network.supabaseUrl}/rest/v1/bi_app_logs")
                    .headers(network.getHeaders())
                    .post(json.toRequestBody("application/json".toMediaType()))
                    .build()
                network.client.newCall(request).enqueue(object : Callback {
                    override fun onFailure(call: Call, e: IOException) {
                        Log.e(network.tag, "Failed to network.log BI event: ${e.message}", e)
                    }
                    override fun onResponse(call: Call, response: Response) {
                        response.use {
                            if (!response.isSuccessful) {
                                Log.e(network.tag, "Log BI event failed: ${response.code}")
                            }
                        }
                    }
                } )
            }
        }

    fun fetchBIDailyMetrics() {
            if (!network.isConfigured) return
            network.scope.launch {
                try {
                    val request = Request.Builder()
                        .url("${network.supabaseUrl}/rest/v1/bi_daily_metrics?select=*&order=date.desc&limit=30")
                        .headers(network.getHeaders())
                        .get()
                        .build()
                    network.client.newCall(request).execute().use { response ->
                        if (response.isSuccessful) {
                            val json = response.body?.string() ?: ""
                            network._biDailyMetrics.value = network.biDailyMetricListAdapter.fromJson(json) ?: emptyList()
                        }
                    }
                } catch (e: Exception) {}
            }
        }

    fun fetchBINotifications() {
            if (!network.isConfigured) return
            network.scope.launch {
                try {
                    val request = Request.Builder()
                        .url("${network.supabaseUrl}/rest/v1/bi_notifications?select=*&order=created_at.desc")
                        .headers(network.getHeaders())
                        .get()
                        .build()
                    network.client.newCall(request).execute().use { response ->
                        if (response.isSuccessful) {
                            val json = response.body?.string() ?: ""
                            network._biNotifications.value = network.biNotificationListAdapter.fromJson(json) ?: emptyList()
                        }
                    }
                } catch (e: Exception) {}
            }
        }

    fun scheduleNotification(title: String, body: String, segment: String) {
            if (!network.userHasPermission("manage_notifications")) return
            network.scope.launch {
                try {
                    val bodyMap = mapOf(
                        "title" to title,
                        "body" to body,
                        "target_segment" to segment,
                        "status" to "scheduled",
                        "schedule_time" to SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).format(Date(System.currentTimeMillis() + 3600000))
                    )
                    val bodyReq = network.moshi.adapter(Map::class.java).toJson(bodyMap).toRequestBody("application/json".toMediaType())
                    val request = Request.Builder()
                        .url("${network.supabaseUrl}/rest/v1/bi_notifications")
                        .headers(network.getHeaders())
                        .post(bodyReq)
                        .build()
                    network.client.newCall(request).execute().use { response ->
                        if (response.isSuccessful) {
                            fetchBINotifications()
                            network.logAdminAction("SCHEDULE_NOTIFICATION", title)
                        }
                    }
                } catch (e: Exception) {}
            }
        }

    fun fetchBIAppLogs(category: String? = null) {
            if (!network.isConfigured) return
            network.scope.launch {
                try {
                    val url = if (category != null) {
                        "${network.supabaseUrl}/rest/v1/bi_app_logs?category=eq.$category&select=*&order=created_at.desc&limit=100"
                    } else {
                        "${network.supabaseUrl}/rest/v1/bi_app_logs?select=*&order=created_at.desc&limit=100"
                    }
                    val request = Request.Builder()
                        .url(url)
                        .headers(network.getHeaders())
                        .get()
                        .build()
                    network.client.newCall(request).execute().use { response ->
                        if (response.isSuccessful) {
                            val json = response.body?.string() ?: ""
                            network._biAppLogs.value = network.biAppLogListAdapter.fromJson(json) ?: emptyList()
                        }
                    }
                } catch (e: Exception) {}
            }
        }

    fun fetchBIHealthMetrics() {
            if (!network.isConfigured) return
            network.scope.launch {
                try {
                    val request = Request.Builder()
                        .url("${network.supabaseUrl}/rest/v1/bi_health_metrics?select=*&order=recorded_at.desc&limit=10")
                        .headers(network.getHeaders())
                        .get()
                        .build()
                    network.client.newCall(request).execute().use { response ->
                        if (response.isSuccessful) {
                            val json = response.body?.string() ?: ""
                            network._biHealthMetrics.value = network.biHealthMetricListAdapter.fromJson(json) ?: emptyList()
                        }
                    }
                } catch (e: Exception) {}
            }
        }
}

class RemoteGameRepository(val network: SupabaseManager, private val matchDao: com.example.daadi.data.local.MatchDao) {
    val matches: StateFlow<List<SupabaseMatch>> = network._matches.asStateFlow()

    val cachedMatches: Flow<List<com.example.daadi.data.local.CachedMatch>> = matchDao.getAllMatches()

    val antiCheatLogs: StateFlow<List<SupabaseAntiCheatLog>> = network._antiCheatLogs.asStateFlow()

    fun syncMatchesToCache() {
        if (!network.isConfigured) return
        network.scope.launch {
            val remoteMatches = network._matches.value
            val cached = remoteMatches.map {
                com.example.daadi.data.local.CachedMatch(
                    id = it.id,
                    hostName = it.hostName,
                    opponentName = it.opponentName,
                    status = it.status,
                    winner = it.winner,
                    movesCount = it.movesCount,
                    createdAt = it.createdAt
                )
            }
            matchDao.insertMatches(cached)
        }
    }

    fun deleteMatch(matchId: String) {
            // Can be handled or requested online
            if (network.isConfigured) {
                network.scope.launch {
                    val request = Request.Builder()
                        .url("${network.supabaseUrl}/rest/v1/matches?id=eq.$matchId")
                        .headers(network.getHeaders())
                        .delete()
                        .build()
                    network.client.newCall(request).enqueue(object : Callback {
                        override fun onFailure(call: Call, e: IOException) {}
                        override fun onResponse(call: Call, response: Response) {
                            if (response.isSuccessful) { network.scope.launch { network.fetchRemoteMatches() } }
                        }
                    })
                }
            } else {
                network._matches.value = network._matches.value.filter { it.id != matchId }
                network.saveSimulatorMatches()
            }
        }

    fun findWaitingMatch(onResult: (SupabaseMatch?) -> Unit) {
            if (!network.isConfigured) {
                onResult(null)
                return
            }
            network.scope.launch {
                val url = "${network.supabaseUrl}/rest/v1/matches?status=eq.waiting&limit=1"
                val request = Request.Builder()
                    .url(url)
                    .headers(network.getHeaders())
                    .get()
                    .build()
    
                network.client.newCall(request).enqueue(object : Callback {
                    override fun onFailure(call: Call, e: IOException) { onResult(null) }
                    override fun onResponse(call: Call, response: Response) {
                        val body = response.body?.string()
                        if (response.isSuccessful && body != null) {
                            try {
                                val list = network.matchListAdapter.fromJson(body)
                                network.runOnMain { onResult(list?.firstOrNull()) }
                            } catch (e: Exception) { network.runOnMain { onResult(null) } }
                        } else { network.runOnMain { onResult(null) } }
                    }
                })
            }
        }

    fun hostWaitingMatch(hostName: String, roomCode: String, onResult: (Boolean) -> Unit) {
            val dateString = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())
            if (network.isConfigured) {
                network.scope.launch {
                    val matchMap = mapOf(
                        "id" to roomCode,
                        "hostName" to hostName,
                        "opponentName" to "",
                        "status" to "waiting",
                        "movesCount" to 0,
                        "createdAt" to dateString
                    )
                    val json = network.moshi.adapter(Map::class.java).toJson(matchMap)
                    val request = Request.Builder()
                        .url("${network.supabaseUrl}/rest/v1/matches")
                        .headers(network.getHeaders())
                        .post(json.toRequestBody("application/json".toMediaType()))
                        .build()
                    network.client.newCall(request).enqueue(object : Callback {
                        override fun onFailure(call: Call, e: IOException) { network.runOnMain { onResult(false) } }
                        override fun onResponse(call: Call, response: Response) { 
                            network.runOnMain { onResult(response.isSuccessful) }
                            if (response.isSuccessful) {
                                network.scope.launch { network.fetchRemoteMatches() }
                            }
                        }
                    })
                }
            } else {
                onResult(true) // local sim success
            }
        }

    fun joinWaitingMatch(matchId: String, opponentName: String, onResult: (Boolean) -> Unit) {
            if (network.isConfigured) {
                network.scope.launch {
                    val update = mapOf(
                        "opponentName" to opponentName,
                        "status" to "playing"
                    )
                    val json = network.moshi.adapter(Map::class.java).toJson(update)
                    val request = Request.Builder()
                        .url("${network.supabaseUrl}/rest/v1/matches?id=eq.$matchId")
                        .headers(network.getHeaders())
                        .patch(json.toRequestBody("application/json".toMediaType()))
                        .build()
                    network.client.newCall(request).enqueue(object : Callback {
                        override fun onFailure(call: Call, e: IOException) { onResult(false) }
                        override fun onResponse(call: Call, response: Response) {
                            onResult(response.isSuccessful)
                            if (response.isSuccessful) {
                                network.scope.launch { network.fetchRemoteMatches() }
                            }
                        }
                    })
                }
            } else {
                onResult(true) // local sim success
            }
        }

    fun registerMatchResult(roomCode: String, hostName: String, opponentName: String, winnerName: String?, movesCount: Int) {
            val dateString = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())
            val statusString = "finished"
            val nextMatchId = "m_res_${(1000..9999).random()}"
            val matchObj = SupabaseMatch(nextMatchId, hostName, opponentName, statusString, winnerName, movesCount, dateString)
    
            if (network.isConfigured) {
                network.scope.launch {
                    val matchMap = mapOf(
                        "hostName" to hostName,
                        "opponentName" to opponentName,
                        "status" to statusString,
                        "winner" to winnerName,
                        "movesCount" to movesCount,
                        "createdAt" to dateString
                    )
                    val json = network.moshi.adapter(Map::class.java).toJson(matchMap)
                    val reqBody = json.toRequestBody("application/json".toMediaType())
    
                    val request = Request.Builder()
                        .url("${network.supabaseUrl}/rest/v1/matches")
                        .headers(network.getHeaders())
                        .post(reqBody)
                        .build()
    
                    network.client.newCall(request).enqueue(object : Callback {
                        override fun onFailure(call: Call, e: IOException) {
                            Log.e(network.tag, "Failed posting match result", e)
                        }
                        override fun onResponse(call: Call, response: Response) {
                            if (response.isSuccessful) {
                                network.scope.launch { network.fetchRemoteMatches() }
                            }
                        }
                    })
    
                    network.updateRemoteUserRankStats(hostName, winnerName == hostName, winnerName != null && winnerName != hostName)
                    if (opponentName.isNotBlank() && opponentName != "Guest") {
                        network.updateRemoteUserRankStats(opponentName, winnerName == opponentName, winnerName != null && winnerName != opponentName)
                    }
                }
            } else {
                network._matches.value = listOf(matchObj) + network._matches.value
                network.saveSimulatorMatches()
    
                network._users.value = network._users.value.map { u ->
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
                network.saveSimulatorUsers()
            }
        }

    fun updateMatchMoves(roomCode: String, movesJson: String) {
            if (network.isConfigured) {
                network.scope.launch {
                    val body = mapOf("movesJson" to movesJson)
                    val json = network.moshi.adapter(Map::class.java).toJson(body)
                    val request = Request.Builder()
                        .url("${network.supabaseUrl}/rest/v1/matches?id=eq.$roomCode")
                        .headers(network.getHeaders())
                        .patch(json.toRequestBody("application/json".toMediaType()))
                        .build()
    
                    network.client.newCall(request).enqueue(object : Callback {
                        override fun onFailure(call: Call, e: IOException) {
                            Log.e(network.tag, "Failed to update match moves: ${e.message}", e)
                        }
                        override fun onResponse(call: Call, response: Response) {
                            response.use {
                                if (!response.isSuccessful) {
                                    Log.e(network.tag, "Update match moves failed: ${response.code}")
                                }
                            }
                        }
                    } )
                }
            }
        }

    fun logAntiCheatViolation(matchId: String?, violationType: String, severity: String, details: String? = null) {
            if (network.isConfigured) {
                val user = network.currentUser.value ?: return
                val payload = mapOf(
                    "userId" to user.id,
                    "matchId" to matchId,
                    "violationType" to violationType,
                    "severity" to severity,
                    "details" to details,
                    "createdAt" to SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault()).format(Date())
                )
                val json = network.moshi.adapter(Map::class.java).toJson(payload)
                val request = Request.Builder()
                    .url("${network.supabaseUrl}/rest/v1/anti_cheat_logs")
                    .headers(network.getHeaders())
                    .post(json.toRequestBody("application/json".toMediaType()))
                    .build()
                network.client.newCall(request).enqueue(object : Callback {
                    override fun onFailure(call: Call, e: IOException) {
                        Log.e(network.tag, "Failed to network.log anti-cheat violation: ${e.message}", e)
                    }
                    override fun onResponse(call: Call, response: Response) {
                        response.use {
                            if (!response.isSuccessful) {
                                Log.e(network.tag, "Log anti-cheat violation failed: ${response.code}")
                            }
                        }
                    }
                } )
            }
        }

    fun fetchMatchDetails(matchId: String, onResult: (SupabaseMatch?) -> Unit) {
            if (network.isConfigured) {
                val request = Request.Builder()
                    .url("${network.supabaseUrl}/rest/v1/matches?id=eq.$matchId&select=*")
                    .headers(network.getHeaders())
                    .get()
                    .build()
    
                network.client.newCall(request).enqueue(object : Callback {
                    override fun onFailure(call: Call, e: IOException) {
                        network.runOnMain { onResult(null) }
                    }
                    override fun onResponse(call: Call, response: Response) {
                        val body = response.body?.string()
                        if (response.isSuccessful && body != null) {
                            try {
                                val list = network.matchListAdapter.fromJson(body)
                                network.runOnMain { onResult(list?.firstOrNull()) }
                            } catch (e: Exception) {
                                network.runOnMain { onResult(null) }
                            }
                        } else {
                            network.runOnMain { onResult(null) }
                        }
                    }
                })
            } else {
                onResult(null)
            }
        }

    fun fetchAntiCheatLogs() {
            if (!network.isConfigured) return
            network.scope.launch {
                try {
                    val request = Request.Builder()
                        .url("${network.supabaseUrl}/rest/v1/anti_cheat_logs?select=*&order=created_at.desc&limit=100")
                        .headers(network.getHeaders())
                        .get()
                        .build()
                    network.client.newCall(request).execute().use { response ->
                        if (response.isSuccessful) {
                            val json = response.body?.string() ?: ""
                            network._antiCheatLogs.value = network.antiCheatListAdapter.fromJson(json) ?: emptyList()
                        }
                    }
                } catch (e: Exception) {}
            }
        }

    fun updateMatchStatus(matchId: String, newStatus: String) {
            if (!network.userHasPermission("manage_matches")) return
            network.scope.launch {
                try {
                    val body = "{\"status\": \"$newStatus\"}".toRequestBody("application/json".toMediaType())
                    val request = Request.Builder()
                        .url("${network.supabaseUrl}/rest/v1/matches?id=eq.$matchId")
                        .headers(network.getHeaders())
                        .patch(body)
                        .build()
                    network.client.newCall(request).execute().use { response ->
                        if (response.isSuccessful) {
                            network.fetchRemoteMatches()
                            network.logAdminAction("MATCH_UPDATE", "$matchId -> $newStatus")
                        }
                    }
                } catch (e: Exception) {}
            }
        }
}

class EconomyRepository(val network: SupabaseManager) {
    val economyTransactions: StateFlow<List<SupabaseEconomyTransaction>> = network._economyTransactions

    val storeItems: StateFlow<List<SupabaseStoreItem>> = network._storeItems

    val coupons: StateFlow<List<SupabaseCoupon>> = network._coupons

    val dailyRewards: StateFlow<List<SupabaseDailyReward>> = network._dailyRewards

    val spinWheelRewards: StateFlow<List<SupabaseSpinWheelReward>> = network._spinWheelRewards

    fun fetchEconomyTransactions() {
        if (!network.isConfigured) return
        network.scope.launch { network.fetchEconomyTransactions() }
    }

    fun fetchStoreItems() {
        if (!network.isConfigured) return
        network.scope.launch { network.fetchStoreItems() }
    }

    fun fetchCoupons() {
        if (!network.isConfigured) return
        network.scope.launch { network.fetchCoupons() }
    }

    fun adjustUserEconomy(userId: String, coinsDelta: Int, xpDelta: Int) {
            if (!network.userHasPermission("manage_users")) return
            network.scope.launch {
                try {
                    val user = network._users.value.find { it.id == userId } ?: return@launch
                    val updateMap = mapOf("coins" to user.coins + coinsDelta, "xp" to user.xp + xpDelta)
                    val body = network.moshi.adapter(Map::class.java).toJson(updateMap).toRequestBody("application/json".toMediaType())
                    val request = Request.Builder()
                        .url("${network.supabaseUrl}/rest/v1/users?id=eq.$userId")
                        .headers(network.getHeaders())
                        .patch(body)
                        .build()
                    network.client.newCall(request).execute().use { response ->
                        if (response.isSuccessful) {
                            network.fetchRemoteUsers()
                            network.logAdminAction("ADJUST_ECONOMY", "User: $userId | Coins: $coinsDelta | XP: $xpDelta")
                        }
                    }
                } catch (e: Exception) {}
            }
        }

    fun adjustUserStats(userId: String, winsDelta: Int, lossesDelta: Int, ratingDelta: Int) {
            if (!network.userHasPermission("manage_users")) return
            network.scope.launch {
                try {
                    val user = network._users.value.find { it.id == userId } ?: return@launch
                    val updateMap = mapOf(
                        "wins" to user.wins + winsDelta,
                        "losses" to user.losses + lossesDelta,
                        "rating" to user.rating + ratingDelta
                    )
                    val body = network.moshi.adapter(Map::class.java).toJson(updateMap).toRequestBody("application/json".toMediaType())
                    val request = Request.Builder()
                        .url("${network.supabaseUrl}/rest/v1/users?id=eq.$userId")
                        .headers(network.getHeaders())
                        .patch(body)
                        .build()
                    network.client.newCall(request).execute().use { response ->
                        if (response.isSuccessful) {
                            network.fetchRemoteUsers()
                            network.logAdminAction("ADJUST_STATS", "User: $userId | Wins: $winsDelta | Losses: $lossesDelta | Rating: $ratingDelta")
                        }
                    }
                } catch (e: Exception) {}
            }
        }

    fun adjustUserEconomy(userId: String, amount: Int, currency: String, reason: String) {
            network.scope.launch {
                val transaction = mapOf(
                    "user_id" to userId,
                    "amount" to amount,
                    "currency" to currency,
                    "type" to "adjustment",
                    "source" to "admin",
                    "reason" to reason,
                    "created_at" to SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).format(Date())
                )
                val json = network.moshi.adapter(Map::class.java).toJson(transaction)
                val request = Request.Builder()
                    .url("${network.supabaseUrl}/rest/v1/economy_transactions")
                    .headers(network.getHeaders())
                    .post(json.toRequestBody("application/json".toMediaType()))
                    .build()
                network.client.newCall(request).enqueue(object : Callback {
                    override fun onFailure(call: Call, e: IOException) {}
                    override fun onResponse(call: Call, response: Response) {
                        if (response.isSuccessful) {
                            network.logAdminAction("ECONOMY_ADJUST", "$userId: $amount $currency")
                            network.fetchEconomyTransactions()
                        }
                        response.close()
                    }
                })
            }
        }

    fun createStoreItem(name: String, description: String, type: String, priceCoins: Int?, priceUsd: Double?, isFeatured: Boolean, discountPercentage: Int) {
            if (!network.isConfigured) return
            network.scope.launch {
                try {
                    val bodyMap = mutableMapOf<String, Any>(
                        "name" to name,
                        "description" to description,
                        "type" to type,
                        "is_featured" to isFeatured,
                        "discount_percentage" to discountPercentage,
                        "content" to emptyMap<String, Any>()
                    )
                    if (priceCoins != null) bodyMap["price_coins"] = priceCoins
                    if (priceUsd != null) bodyMap["price_usd"] = priceUsd
                    val body = network.moshi.adapter(Map::class.java).toJson(bodyMap).toRequestBody("application/json".toMediaType())
                    val request = Request.Builder()
                        .url("${network.supabaseUrl}/rest/v1/store_items")
                        .headers(network.getHeaders())
                        .post(body)
                        .build()
                    network.client.newCall(request).execute().use { response ->
                        if (response.isSuccessful) {
                            network.fetchStoreItems()
                            network.logAdminAction("CREATE_STORE_ITEM", name)
                        }
                    }
                } catch (e: Exception) {}
            }
        }

    fun deleteStoreItem(id: String) {
            if (!network.isConfigured) return
            network.scope.launch {
                try {
                    val request = Request.Builder()
                        .url("${network.supabaseUrl}/rest/v1/store_items?id=eq.$id")
                        .headers(network.getHeaders())
                        .delete()
                        .build()
                    network.client.newCall(request).execute().use { response ->
                        if (response.isSuccessful) {
                            network.fetchStoreItems()
                            network.logAdminAction("DELETE_STORE_ITEM", id)
                        }
                    }
                } catch (e: Exception) {}
            }
        }

    fun createCoupon(code: String, discountType: String, value: Double, maxUses: Int?) {
            if (!network.isConfigured) return
            network.scope.launch {
                try {
                    val bodyMap = mutableMapOf<String, Any>(
                        "code" to code,
                        "discount_type" to discountType,
                        "value" to value,
                        "used_count" to 0,
                        "is_active" to true
                    )
                    if (maxUses != null) bodyMap["max_uses"] = maxUses
                    val body = network.moshi.adapter(Map::class.java).toJson(bodyMap).toRequestBody("application/json".toMediaType())
                    val request = Request.Builder()
                        .url("${network.supabaseUrl}/rest/v1/coupons")
                        .headers(network.getHeaders())
                        .post(body)
                        .build()
                    network.client.newCall(request).execute().use { response ->
                        if (response.isSuccessful) {
                            network.fetchCoupons()
                            network.logAdminAction("CREATE_COUPON", code)
                        }
                    }
                } catch (e: Exception) {}
            }
        }

    fun deleteCoupon(id: String) {
            if (!network.isConfigured) return
            network.scope.launch {
                try {
                    val request = Request.Builder()
                        .url("${network.supabaseUrl}/rest/v1/coupons?id=eq.$id")
                        .headers(network.getHeaders())
                        .delete()
                        .build()
                    network.client.newCall(request).execute().use { response ->
                        if (response.isSuccessful) {
                            network.fetchCoupons()
                            network.logAdminAction("DELETE_COUPON", id)
                        }
                    }
                } catch (e: Exception) {}
            }
        }

    fun saveDailyReward(day: Int, type: String, amount: Int) {
            if (!network.isConfigured) return
            network.scope.launch {
                try {
                    val bodyMap = mapOf(
                        "day" to day,
                        "type" to type,
                        "amount" to amount
                    )
                    val body = network.moshi.adapter(Map::class.java).toJson(bodyMap).toRequestBody("application/json".toMediaType())
                    val request = Request.Builder()
                        .url("${network.supabaseUrl}/rest/v1/daily_rewards")
                        .headers(network.getHeaders())
                        .header("Prefer", "resolution=merge-duplicates")
                        .post(body)
                        .build()
                    network.client.newCall(request).execute().use { response ->
                        if (response.isSuccessful) {
                            network.fetchDailyRewards()
                            network.logAdminAction("SAVE_DAILY_REWARD", "Day: $day, Type: $type, Amount: $amount")
                        }
                    }
                } catch (e: Exception) {}
            }
        }

    fun fetchDailyRewards() {
        if (!network.isConfigured) return
        network.scope.launch { network.fetchDailyRewards() }
    }

    fun fetchSpinWheelRewards() {
        if (!network.isConfigured) return
        network.scope.launch { network.fetchSpinWheelRewards() }
    }
    fun saveSpinWheelReward(id: String, type: String, amount: Int, weight: Int) {
            if (!network.isConfigured) return
            network.scope.launch {
                try {
                    val bodyMap = mapOf(
                        "type" to type,
                        "amount" to amount,
                        "weight" to weight
                    )
                    val body = network.moshi.adapter(Map::class.java).toJson(bodyMap).toRequestBody("application/json".toMediaType())
                    val request = Request.Builder()
                        .url("${network.supabaseUrl}/rest/v1/spin_wheel_rewards?id=eq.$id")
                        .headers(network.getHeaders())
                        .patch(body)
                        .build()
                    network.client.newCall(request).execute().use { response ->
                        if (response.isSuccessful) {
                            network.fetchSpinWheelRewards()
                            network.logAdminAction("SAVE_SPIN_WHEEL_REWARD", "ID: $id, Type: $type, Amount: $amount, Weight: $weight")
                        }
                    }
                } catch (e: Exception) {}
            }
        }
}

class LiveOpsRepository(val network: SupabaseManager) {
    val liveOpsEvents: StateFlow<List<SupabaseLiveOpsEvent>> = network._liveOpsEvents

    val seasonPasses: StateFlow<List<SupabaseSeasonPass>> = network._seasonPasses

    val gameEvents: StateFlow<List<SupabaseGameEvent>> = network._gameEvents.asStateFlow()

    fun fetchLiveOpsEvents() {
        if (!network.isConfigured) return
        network.scope.launch { network.fetchLiveOpsEvents() }
    }

    fun fetchSeasonPasses() {
        if (!network.isConfigured) return
        network.scope.launch { network.fetchSeasonPasses() }
    }
    fun fetchGameEvents() {
            if (!network.isConfigured) return
            network.scope.launch {
                try {
                    val request = Request.Builder()
                        .url("${network.supabaseUrl}/rest/v1/game_events?select=*&order=created_at.desc")
                        .headers(network.getHeaders())
                        .get()
                        .build()
                    network.client.newCall(request).execute().use { response ->
                        if (response.isSuccessful) {
                            val json = response.body?.string() ?: ""
                            network._gameEvents.value = network.eventListAdapter.fromJson(json) ?: emptyList()
                        }
                    }
                } catch (e: Exception) {}
            }
        }

    fun createGameEvent(title: String, type: String, multiplier: Double, startTime: String, endTime: String) {
            if (!network.isConfigured) return
            network.scope.launch {
                val event = mapOf(
                    "title" to title,
                    "type" to type,
                    "multiplier" to multiplier,
                    "startTime" to startTime,
                    "endTime" to endTime,
                    "isActive" to true
                )
                val json = network.moshi.adapter(Map::class.java).toJson(event)
                val request = Request.Builder()
                    .url("${network.supabaseUrl}/rest/v1/game_events")
                    .headers(network.getHeaders())
                    .post(json.toRequestBody("application/json".toMediaType()))
                    .build()
                network.client.newCall(request).enqueue(object : Callback {
                    override fun onFailure(call: Call, e: IOException) {}
                    override fun onResponse(call: Call, response: Response) {
                        if (response.isSuccessful) fetchGameEvents()
                    }
                } )
            }
        }

    fun toggleGameEvent(eventId: String, isActive: Boolean) {
            if (!network.isConfigured) return
            network.scope.launch {
                val update = mapOf("isActive" to isActive)
                val json = network.moshi.adapter(Map::class.java).toJson(update)
                val request = Request.Builder()
                    .url("${network.supabaseUrl}/rest/v1/game_events?id=eq.$eventId")
                    .headers(network.getHeaders())
                    .patch(json.toRequestBody("application/json".toMediaType()))
                    .build()
                network.client.newCall(request).enqueue(object : Callback {
                    override fun onFailure(call: Call, e: IOException) {}
                    override fun onResponse(call: Call, response: Response) {
                        if (response.isSuccessful) fetchGameEvents()
                    }
                } )
            }
        }

    fun deleteGameEvent(eventId: String) {
            if (!network.isConfigured) return
            network.scope.launch {
                val request = Request.Builder()
                    .url("${network.supabaseUrl}/rest/v1/game_events?id=eq.$eventId")
                    .headers(network.getHeaders())
                    .delete()
                    .build()
                network.client.newCall(request).enqueue(object : Callback {
                    override fun onFailure(call: Call, e: IOException) {}
                    override fun onResponse(call: Call, response: Response) {
                        if (response.isSuccessful) fetchGameEvents()
                    }
                } )
            }
        }

    fun createLiveOpsEvent(title: String, description: String, type: String, xpMultiplier: Double, coinMultiplier: Double, startTime: String, endTime: String, isActive: Boolean) {
            if (!network.isConfigured) return
            network.scope.launch {
                try {
                    val bodyMap = mapOf(
                        "title" to title,
                        "description" to description,
                        "type" to type,
                        "xp_multiplier" to xpMultiplier,
                        "coin_multiplier" to coinMultiplier,
                        "start_time" to startTime,
                        "end_time" to endTime,
                        "is_active" to isActive
                    )
                    val body = network.moshi.adapter(Map::class.java).toJson(bodyMap).toRequestBody("application/json".toMediaType())
                    val request = Request.Builder()
                        .url("${network.supabaseUrl}/rest/v1/liveops_events")
                        .headers(network.getHeaders())
                        .post(body)
                        .build()
                    network.client.newCall(request).execute().use { response ->
                        if (response.isSuccessful) {
                            network.fetchLiveOpsEvents()
                            network.logAdminAction("CREATE_LIVEOPS_EVENT", title)
                        }
                    }
                } catch (e: Exception) {}
            }
        }

    fun deleteLiveOpsEvent(id: String) {
            if (!network.isConfigured) return
            network.scope.launch {
                try {
                    val request = Request.Builder()
                        .url("${network.supabaseUrl}/rest/v1/liveops_events?id=eq.$id")
                        .headers(network.getHeaders())
                        .delete()
                        .build()
                    network.client.newCall(request).execute().use { response ->
                        if (response.isSuccessful) {
                            network.fetchLiveOpsEvents()
                            network.logAdminAction("DELETE_LIVEOPS_EVENT", id)
                        }
                    }
                } catch (e: Exception) {}
            }
        }

    fun toggleLiveOpsEventActive(id: String, isActive: Boolean) {
            if (!network.isConfigured) return
            network.scope.launch {
                try {
                    val bodyMap = mapOf("is_active" to isActive)
                    val body = network.moshi.adapter(Map::class.java).toJson(bodyMap).toRequestBody("application/json".toMediaType())
                    val request = Request.Builder()
                        .url("${network.supabaseUrl}/rest/v1/liveops_events?id=eq.$id")
                        .headers(network.getHeaders())
                        .patch(body)
                        .build()
                    network.client.newCall(request).execute().use { response ->
                        if (response.isSuccessful) {
                            network.fetchLiveOpsEvents()
                            network.logAdminAction("TOGGLE_LIVEOPS_EVENT", "ID: $id, Active: $isActive")
                        }
                    }
                } catch (e: Exception) {}
            }
        }

    fun createSeasonPass(title: String, startTime: String, endTime: String, isActive: Boolean) {
            if (!network.isConfigured) return
            network.scope.launch {
                try {
                    val bodyMap = mapOf(
                        "title" to title,
                        "start_time" to startTime,
                        "end_time" to endTime,
                        "is_active" to isActive
                    )
                    val body = network.moshi.adapter(Map::class.java).toJson(bodyMap).toRequestBody("application/json".toMediaType())
                    val request = Request.Builder()
                        .url("${network.supabaseUrl}/rest/v1/season_passes")
                        .headers(network.getHeaders())
                        .post(body)
                        .build()
                    network.client.newCall(request).execute().use { response ->
                        if (response.isSuccessful) {
                            network.fetchSeasonPasses()
                            network.logAdminAction("CREATE_SEASON_PASS", title)
                        }
                    }
                } catch (e: Exception) {}
            }
        }

    fun deleteSeasonPass(id: String) {
            if (!network.isConfigured) return
            network.scope.launch {
                try {
                    val request = Request.Builder()
                        .url("${network.supabaseUrl}/rest/v1/season_passes?id=eq.$id")
                        .headers(network.getHeaders())
                        .delete()
                        .build()
                    network.client.newCall(request).execute().use { response ->
                        if (response.isSuccessful) {
                            network.fetchSeasonPasses()
                            network.logAdminAction("DELETE_SEASON_PASS", id)
                        }
                    }
                } catch (e: Exception) {}
            }
        }

    fun toggleSeasonPassActive(id: String, isActive: Boolean) {
            if (!network.isConfigured) return
            network.scope.launch {
                try {
                    val bodyMap = mapOf("is_active" to isActive)
                    val body = network.moshi.adapter(Map::class.java).toJson(bodyMap).toRequestBody("application/json".toMediaType())
                    val request = Request.Builder()
                        .url("${network.supabaseUrl}/rest/v1/season_passes?id=eq.$id")
                        .headers(network.getHeaders())
                        .patch(body)
                        .build()
                    network.client.newCall(request).execute().use { response ->
                        if (response.isSuccessful) {
                            network.fetchSeasonPasses()
                            network.logAdminAction("TOGGLE_SEASON_PASS", "ID: $id, Active: $isActive")
                        }
                    }
                } catch (e: Exception) {}
            }
        }
}

class SupportRepository(val network: SupabaseManager) {
    val feedbackV2: StateFlow<List<SupabaseFeedbackV2>> = network._feedbackV2.asStateFlow()

    val tickets: StateFlow<List<SupabaseSupportTicket>> = network._tickets.asStateFlow()

    fun submitFeedback(content: String, category: String, onResult: (Boolean, String?) -> Unit) {
            val dateStr = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())
            val username = network.currentUser.value?.username ?: "Guest"
            val userId = network.currentUser.value?.id
            val validUserId = if (userId.isNullOrBlank() || userId == "sim_user_id" || userId.length != 36) null else userId
    
            if (network.isConfigured) {
                network.scope.launch {
                    // 1. Submit to modern feedback_v2 table
                    val categoryMapped = when (category) {
                        "suggest" -> "suggestion"
                        "bug" -> "bug"
                        else -> "feature_request"
                    }
    
                    val feedbackV2Payload = mutableMapOf<String, Any?>(
                        "content" to content,
                        "category" to categoryMapped,
                        "rating" to 5,
                        "sentiment" to "neutral",
                        "status" to "pending"
                    )
                    if (validUserId != null) {
                        feedbackV2Payload["user_id"] = validUserId
                    }
    
                    val jsonV2 = network.moshi.adapter(Map::class.java).toJson(feedbackV2Payload)
                    val reqBodyV2 = jsonV2.toRequestBody("application/json".toMediaType())
    
                    fun trySubmitV2(useSessionToken: Boolean) {
                        val requestV2 = Request.Builder()
                            .url("${network.supabaseUrl}/rest/v1/feedback_v2")
                            .headers(network.getFeedbackHeaders(useSessionToken))
                            .post(reqBodyV2)
                            .build()
    
                        network.client.newCall(requestV2).enqueue(object : Callback {
                            override fun onFailure(call: Call, e: IOException) {
                                Log.e("SupabaseManager", "feedback_v2 (useSessionToken=$useSessionToken) failure: ${e.message}", e)
                                if (useSessionToken) {
                                    trySubmitV2(false)
                                } else {
                                    // Fallback: If feedback_v2 fails (e.g. table not initialized or schema mismatch), try legacy network.feedback table
                                    network.submitLegacyFeedback(content, category, dateStr, username, validUserId, "feedback_v2: ${e.localizedMessage}", onResult)
                                }
                            }
    
                            override fun onResponse(call: Call, response: Response) {
                                val body = response.body?.string()
                                if (response.isSuccessful) {
                                    fetchFeedbackV2()
                                    
                                    // Also submit a report if we have a valid logged in user (UUID check)
                                    if (validUserId != null) {
                                        val reportPayload = mapOf(
                                            "reporter_id" to validUserId,
                                            "reported_id" to validUserId,
                                            "reason" to "[FEEDBACK - ${category.uppercase()}] $content",
                                            "status" to "pending"
                                        )
                                        val reportJson = network.moshi.adapter(Map::class.java).toJson(reportPayload)
                                        val reportReqBody = reportJson.toRequestBody("application/json".toMediaType())
                                        
                                        val reportRequest = Request.Builder()
                                            .url("${network.supabaseUrl}/rest/v1/reports")
                                            .headers(network.getHeaders())
                                            .post(reportReqBody)
                                            .build()
                                            
                                        network.client.newCall(reportRequest).enqueue(object : Callback {
                                            override fun onFailure(callReport: Call, eReport: IOException) {}
                                            override fun onResponse(callReport: Call, responseReport: Response) {
                                                if (responseReport.isSuccessful) {
                                                    network.fetchReports()
                                                }
                                                responseReport.close()
                                            }
                                        })
                                    }
                                    
                                    // Also silently submit to legacy table for backwards compatibility
                                    network.submitLegacyFeedbackSilently(content, category, dateStr, username)
                                    
                                    network.runOnMain { onResult(true, null) }
                                } else {
                                    Log.e("SupabaseManager", "feedback_v2 (useSessionToken=$useSessionToken) error ${response.code}: $body")
                                    if (useSessionToken) {
                                        trySubmitV2(false)
                                    } else {
                                        // Fallback: If feedback_v2 fails, try legacy network.feedback table
                                        network.submitLegacyFeedback(content, category, dateStr, username, validUserId, "feedback_v2 (HTTP ${response.code}: ${body?.take(150)})", onResult)
                                    }
                                }
                                response.close()
                            }
                        })
                    }
    
                    trySubmitV2(true)
                }
            } else {
                val nextId = (network._feedback.value.maxOfOrNull { it.id } ?: 0) + 1
                val newFb = SupabaseFeedback(nextId, username, content, category, dateStr)
                network._feedback.value = network._feedback.value + newFb
                network.saveSimulatorFeedback()
                
                // Sim feedback_v2 too
                val nextV2Id = java.util.UUID.randomUUID().toString()
                val newFbV2 = SupabaseFeedbackV2( id = nextV2Id,
                    userId = validUserId, content = content,
                    category = when (category) {
                        "suggest" -> "suggestion"
                        "bug" -> "bug"
                        else -> "feature_request"
                    },
                    rating = 5,
                    sentiment = "neutral",
                    status = "pending",
                    assignedDeveloperId = null,
                    internalReply = null, createdAt = dateStr,
                    updatedAt = dateStr
                )
                network._feedbackV2.value = network._feedbackV2.value + newFbV2
                
                // Also submit a handled report
                val reporterId = network.currentUser.value?.id ?: "sim_reporter_id"
                val reportId = java.util.UUID.randomUUID().toString()
                val dateStr2 = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault()).format(Date())
                val newReport = SupabaseReport( id = reportId, reporterId = reporterId,
                    reportedId = reporterId,
                    reason = "[FEEDBACK - ${category.uppercase()}] $content",
                    evidenceUrl = null,
                    status = "pending",
                    moderatorId = null, createdAt = dateStr2,
                    updatedAt = dateStr2
                )
                network._reports.value = listOf(newReport) + network._reports.value
                network.saveSimulatorReports()
                
                network.runOnMain { onResult(true, null) }
            }
        }

    fun reportUserByName(
            username: String,
            reason: String = "Reported during online match for unsportsmanlike behavior or suspected cheating",
            onResult: (Boolean) -> Unit = {}
    
    ) {
            val user = network._users.value.find { it.username == username }
            if (user != null) {
                network.reportUser(user.id, reason, onResult)
            } else {
                onResult(false)
            }
        }

    fun fetchTickets() {
            if (!network.isConfigured) return
            network.scope.launch {
                try {
                    val request = Request.Builder()
                        .url("${network.supabaseUrl}/rest/v1/support_tickets?select=*&order=created_at.desc")
                        .headers(network.getHeaders())
                        .get()
                        .build()
                    network.client.newCall(request).execute().use { response ->
                        if (response.isSuccessful) {
                            val json = response.body?.string() ?: ""
                            network._tickets.value = network.ticketListAdapter.fromJson(json) ?: emptyList()
                        }
                    }
                } catch (e: Exception) {}
            }
        }

    fun fetchFeedbackV2() {
            if (!network.isConfigured) return
            network.scope.launch {
                try {
                    val request = Request.Builder()
                        .url("${network.supabaseUrl}/rest/v1/feedback_v2?select=*&order=created_at.desc")
                        .headers(network.getHeaders())
                        .get()
                        .build()
                    network.client.newCall(request).execute().use { response ->
                        if (response.isSuccessful) {
                            val json = response.body?.string() ?: ""
                            network._feedbackV2.value = network.feedbackV2ListAdapter.fromJson(json) ?: emptyList()
                        }
                    }
                } catch (e: Exception) {}
            }
        }
}

class TournamentRepository(val network: SupabaseManager) {
    val tournaments: StateFlow<List<SupabaseTournament>> = network._tournaments.asStateFlow()

    fun fetchTournaments() {
        if (!network.isConfigured) return
        network.scope.launch { network.fetchTournaments() }
    }

    fun createTournament(title: String, description: String, entryFee: Int, prize: Int) {
            if (!network.userHasPermission("manage_tournaments")) return
            network.scope.launch {
                try {
                    val bodyMap = mapOf(
                        "title" to title,
                        "description" to description,
                        "entry_fee" to entryFee,
                        "prize_pool_coins" to prize,
                        "status" to "scheduled",
                        "start_time" to SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).format(Date(System.currentTimeMillis() + 86400000))
                    )
                    val body = network.moshi.adapter(Map::class.java).toJson(bodyMap).toRequestBody("application/json".toMediaType())
                    val request = Request.Builder()
                        .url("${network.supabaseUrl}/rest/v1/tournaments")
                        .headers(network.getHeaders())
                        .post(body)
                        .build()
                    network.client.newCall(request).execute().use { response ->
                        if (response.isSuccessful) {
                            fetchTournaments()
                            network.logAdminAction("CREATE_TOURNAMENT", title)
                        }
                    }
                } catch (e: Exception) {}
            }
        }

    fun deleteTournament(id: String) {
            if (!network.isConfigured) return
            network.scope.launch {
                try {
                    val request = Request.Builder()
                        .url("${network.supabaseUrl}/rest/v1/tournaments?id=eq.$id")
                        .headers(network.getHeaders())
                        .delete()
                        .build()
                    network.client.newCall(request).execute().use { response ->
                        if (response.isSuccessful) {
                            fetchTournaments()
                            network.logAdminAction("DELETE_TOURNAMENT", id)
                        }
                    }
                } catch (e: Exception) {}
            }
        }

    fun updateTournamentStatus(id: String, status: String) {
            if (!network.isConfigured) return
            network.scope.launch {
                try {
                    val bodyMap = mapOf("status" to status)
                    val body = network.moshi.adapter(Map::class.java).toJson(bodyMap).toRequestBody("application/json".toMediaType())
                    val request = Request.Builder()
                        .url("${network.supabaseUrl}/rest/v1/tournaments?id=eq.$id")
                        .headers(network.getHeaders())
                        .patch(body)
                        .build()
                    network.client.newCall(request).execute().use { response ->
                        if (response.isSuccessful) {
                            fetchTournaments()
                            network.logAdminAction("UPDATE_TOURNAMENT_STATUS", "ID: $id, Status: $status")
                        }
                    }
                } catch (e: Exception) {}
            }
        }
}

class RemoteConfigRepository(val network: SupabaseManager) {
    val cmsContent: StateFlow<List<SupabaseCMSContent>> = network._cmsContent

    val announcements: StateFlow<List<SupabaseAnnouncement>> = network._announcements.asStateFlow()

    val systemSettings: StateFlow<List<SupabaseSystemSetting>> = network._systemSettings.asStateFlow()

    val adConfig: StateFlow<AdConfiguration> = network._adConfig.asStateFlow()

    val maintenanceMode: StateFlow<Boolean> = network._maintenanceMode.asStateFlow()

    val multiplayerEnabled: StateFlow<Boolean> = network._multiplayerEnabled.asStateFlow()

    val globalBroadcast: StateFlow<String?> = network._globalBroadcast.asStateFlow()

    val appVersions: StateFlow<List<SupabaseAppVersion>> = network._appVersions.asStateFlow()

    val maintenanceSchedules: StateFlow<List<SupabaseMaintenanceSchedule>> = network._maintenanceSchedules.asStateFlow()

    fun dispatchBroadcast(message: String) {
            network._globalBroadcast.value = message
            network.logAdminAction("DISPATCH_BROADCAST", message)
        }

    fun clearBroadcast() {
            network._globalBroadcast.value = null
            network.logAdminAction("CLEAR_BROADCAST", "system")
        }

    fun updateAdConfigurationRemote(config: AdConfiguration) {
            network.logAdminAction("COMMIT_AD_CONFIG", "Provider: ${config.activeProvider}")
            
            if (!network.isConfigured) {
                network._adConfig.value = config
                return
            }
    
            network.scope.launch {
                val json = network.moshi.adapter<AdConfiguration>(AdConfiguration::class.java).toJson(config)
                val reqBody = json.toRequestBody("application/json".toMediaType())
    
                val request = Request.Builder()
                    .url("${network.supabaseUrl}/rest/v1/ad_configuration?id=eq.1") // Assuming single row config
                    .headers(network.getHeaders())
                    .patch(reqBody)
                    .build()
    
                network.client.newCall(request).enqueue(object : Callback {
                    override fun onFailure(call: Call, e: IOException) {
                        Log.e(network.tag, "Failed to update ad configuration: ${e.message}", e)
                    }
                    override fun onResponse(call: Call, response: Response) {
                        response.use {
                            if (response.isSuccessful) {
                                network._adConfig.value = config
                            } else {
                                Log.e(network.tag, "Update ad configuration failed: ${response.code}")
                            }
                        }
                    }
                })
            }
        }

    fun createAnnouncement(title: String, content: String, isActive: Boolean) {
            if (network.isConfigured) {
                network.createAnnouncementRemote(title, content, isActive)
            } else {
                val maxId = network._announcements.value.maxOfOrNull { it.id } ?: 0
                val nextId = maxId + 1
                val newAnn = SupabaseAnnouncement(
                    nextId,
                    title,
                    content,
                    isActive,
                    SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())
                )
                network._announcements.value = listOf(newAnn) + network._announcements.value
                network.saveSimulatorAnnouncements()
            }
        }

    fun toggleAnnouncementStatus(id: Int) {
            if (network.isConfigured) {
                val item = network._announcements.value.find { it.id == id } ?: return
                network.scope.launch {
                    val update = mapOf("isActive" to !item.isActive)
                    val json = network.moshi.adapter(Map::class.java).toJson(update)
                    val reqBody = json.toRequestBody("application/json".toMediaType())
    
                    val request = Request.Builder()
                        .url("${network.supabaseUrl}/rest/v1/announcements?id=eq.$id")
                        .headers(network.getHeaders())
                        .patch(reqBody)
                        .build()
    
                    network.client.newCall(request).enqueue(object : Callback {
                        override fun onFailure(call: Call, e: IOException) {}
                        override fun onResponse(call: Call, response: Response) {
                            if (response.isSuccessful) { network.scope.launch { network.fetchRemoteAnnouncements() } }
                        }
                    })
                }
            } else {
                network._announcements.value = network._announcements.value.map {
                    if (it.id == id) it.copy(isActive = !it.isActive) else it
                }
                network.saveSimulatorAnnouncements()
            }
        }

    fun deleteAnnouncement(id: Int) {
            if (network.isConfigured) {
                network.scope.launch {
                    val request = Request.Builder()
                        .url("${network.supabaseUrl}/rest/v1/announcements?id=eq.$id")
                        .headers(network.getHeaders())
                        .delete()
                        .build()
                    network.client.newCall(request).enqueue(object : Callback {
                        override fun onFailure(call: Call, e: IOException) {}
                        override fun onResponse(call: Call, response: Response) {
                            if (response.isSuccessful) { network.scope.launch { network.fetchRemoteAnnouncements() } }
                        }
                    })
                }
            } else {
                network._announcements.value = network._announcements.value.filter { it.id != id }
                network.saveSimulatorAnnouncements()
            }
        }

    fun updateSystemSetting(key: String, newValue: String) {
            if (network.isConfigured) {
                network.scope.launch {
                    val update = mapOf("value" to newValue)
                    val json = network.moshi.adapter(Map::class.java).toJson(update)
                    val reqBody = json.toRequestBody("application/json".toMediaType())
    
                    val request = Request.Builder()
                        .url("${network.supabaseUrl}/rest/v1/system_settings?key=eq.$key")
                        .headers(network.getHeaders())
                        .patch(reqBody)
                        .build()
    
                    network.client.newCall(request).enqueue(object : Callback {
                        override fun onFailure(call: Call, e: IOException) {}
                        override fun onResponse(call: Call, response: Response) {
                            if (response.isSuccessful) { network.scope.launch { network.fetchRemoteSettings() } }
                        }
                    })
                }
            } else {
                network._systemSettings.value = network._systemSettings.value.map {
                    if (it.key == key) it.copy(value = newValue) else it
                }
                network.saveSimulatorSettings()
            }
        }

    fun updateRemoteConfig(key: String, value: String) {
            updateSystemSetting(key, value)
            network.logAdminAction("REMOTE_CONFIG_UPDATE", "$key -> $value")
        }

    fun saveCMSContent(content: SupabaseCMSContent) {
            network.scope.launch {
                val json = network.moshi.adapter(SupabaseCMSContent::class.java).toJson(content)
                val request = Request.Builder()
                    .url("${network.supabaseUrl}/rest/v1/cms_content?id=eq.${content.id}")
                    .headers(network.getHeaders())
                    .patch(json.toRequestBody("application/json".toMediaType()))
                    .build()
                network.client.newCall(request).enqueue(object : Callback {
                    override fun onFailure(call: Call, e: IOException) {}
                    override fun onResponse(call: Call, response: Response) {
                        if (response.isSuccessful) network.fetchCMSContent()
                        response.close()
                    }
                })
            }
        }
}

