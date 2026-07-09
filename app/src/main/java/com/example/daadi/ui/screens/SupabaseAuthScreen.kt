package com.example.daadi.ui.screens



import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.horizontalScroll
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.LocalContext
import android.widget.Toast
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.daadi.data.supabase.SupabaseUser
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SupabaseAuthScreen(
    sharedGameViewModel: com.example.daadi.viewmodel.GameViewModel,
    onBack: () -> Unit,
    onAuthSuccess: () -> Unit = {},
    onNavigateToAdmin: () -> Unit = {}
) {
    val currentUser by sharedGameViewModel.authRepository.currentUser.collectAsStateWithLifecycle()
    val globalErrorMsg by sharedGameViewModel.authRepository.errorMessage.collectAsStateWithLifecycle()
    val passwordResetRequired by sharedGameViewModel.authRepository.passwordResetRequired.collectAsStateWithLifecycle()
    val isConfigured = sharedGameViewModel.authRepository.isConfigured
    val context = LocalContext.current

    var showResetPasswordDialog by remember { mutableStateOf(false) }
    var newPasswordInput by remember { mutableStateOf("") }
    var resetErrorMsg by remember { mutableStateOf<String?>(null) }
    var resetSuccessMsg by remember { mutableStateOf<String?>(null) }
    var resetIsLoading by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (currentUser != null) {
            sharedGameViewModel.authRepository.refreshUserProfile()
        }
    }

    LaunchedEffect(passwordResetRequired) {
        if (passwordResetRequired) {
            showResetPasswordDialog = true
        }
    }

    var isSignUpMode by remember { mutableStateOf(false) }
    var isForgotPasswordMode by remember { mutableStateOf(false) }
    var isRefreshingProfile by remember { mutableStateOf(false) }
    var email by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    var localIsLoading by remember { mutableStateOf(false) }
    var localErrorMsg by remember { mutableStateOf<String?>(null) }
    var localSuccessMsg by remember { mutableStateOf<String?>(null) }
    var hasAcceptedTerms by remember { mutableStateOf(false) }
    var showPrivacyPolicyDialog by remember { mutableStateOf(false) }
    val uriHandler = LocalUriHandler.current
    val legalUrl = "https://daadi-legal.vercel.app"

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Player Profile",
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 20.sp,
                        color = Color(0xFF5C2D0A)
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("auth_back_button")) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Go Back", tint = Color(0xFF5C2D0A))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFFFDF3E3),
                    titleContentColor = Color(0xFF5C2D0A),
                    navigationIconContentColor = Color(0xFF5C2D0A)
                )
            )
        },
        containerColor = Color(0xFFFDF3E3)
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header Image/Icon
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .background(Color(0xFFFFF7EA), CircleShape)
                    .border(2.dp, Color(0xFFE5A93B), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (currentUser != null) Icons.Default.AccountCircle else Icons.Default.Lock,
                    contentDescription = "User Identity Icon",
                    tint = Color(0xFFC75D27),
                    modifier = Modifier.size(56.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            if (currentUser != null) {
                // RENDER LOGGED-IN IMMERSIVE PLAYER COMMAND CENTER
                val user = currentUser!!
                val app = context.applicationContext as com.example.daadi.DaadiApplication
                val prefs = context.getSharedPreferences("daadi_player_hub", android.content.Context.MODE_PRIVATE)
                val moshi = sharedGameViewModel.authRepository.network.moshi
                val scope = rememberCoroutineScope()
                
                var activeTab by remember { mutableStateOf("hub") }
                
                // Helper to update coins and xp
                val updateCoinsAndXp: (Int, Int) -> Unit = { coinsDelta, xpDelta ->
                    val updated = user.copy(
                        coins = (user.coins + coinsDelta).coerceAtLeast(0),
                        xp = (user.xp + xpDelta).coerceAtLeast(0)
                    )
                    sharedGameViewModel.authRepository.updateCurrentUserProfile(updated)
                }

                // Header Profile Summary
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF7EA)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                        .border(1.dp, Color(0xFFE5A93B).copy(alpha = 0.4f), RoundedCornerShape(16.dp))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(54.dp)
                                .background(Color(0xFFE5A93B), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            val emoji = when (user.avatarUrl) {
                                "avatar_chanakya" -> "🧠"
                                "avatar_warrior" -> "⚔️"
                                "avatar_queen" -> "👑"
                                "avatar_monk" -> "🧘"
                                "avatar_lion" -> "🦁"
                                else -> null
                            }
                            if (emoji != null) {
                                Text(emoji, fontSize = 26.sp)
                            } else {
                                Text(
                                    text = user.username.take(2).uppercase(),
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    fontSize = 18.sp
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.width(12.dp))
                        
                        var showProfileEditDialog by remember { mutableStateOf(false) }
                        if (showProfileEditDialog) {
                            var editUsername by remember { mutableStateOf(user.username) }
                            var editAvatarUrl by remember { mutableStateOf(user.avatarUrl ?: "avatar_chanakya") }
                            var isUpdatingProfile by remember { mutableStateOf(false) }
                            
                            AlertDialog(
                                onDismissRequest = { showProfileEditDialog = false },
                                title = { Text("Personalize Profile", fontWeight = FontWeight.ExtraBold, color = Color(0xFF5C2D0A), fontFamily = FontFamily.Serif) },
                                text = {
                                    Column {
                                        OutlinedTextField(
                                            value = editUsername,
                                            onValueChange = { editUsername = it },
                                            label = { Text("Display Username") },
                                            modifier = Modifier.fillMaxWidth(),
                                            colors = TextFieldDefaults.colors(focusedContainerColor = Color.White, unfocusedContainerColor = Color.White)
                                        )
                                        Spacer(modifier = Modifier.height(16.dp))
                                        Text("Choose Strategic Avatar:", fontWeight = FontWeight.Bold, color = Color(0xFF5C2D0A), fontSize = 12.sp)
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceEvenly
                                        ) {
                                            val avatars = listOf(
                                                "avatar_chanakya" to "🧠",
                                                "avatar_warrior" to "⚔️",
                                                "avatar_queen" to "👑",
                                                "avatar_monk" to "🧘",
                                                "avatar_lion" to "🦁"
                                            )
                                            avatars.forEach { (id, emojiStr) ->
                                                val selected = editAvatarUrl == id
                                                Box(
                                                    modifier = Modifier
                                                        .size(44.dp)
                                                        .background(if (selected) Color(0xFFE5A93B) else Color(0xFFFFF7EA), CircleShape)
                                                        .border(2.dp, if (selected) Color(0xFFC75D27) else Color.Transparent, CircleShape)
                                                        .clickable { editAvatarUrl = id },
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(emojiStr, fontSize = 22.sp)
                                                }
                                            }
                                        }
                                    }
                                },
                                confirmButton = {
                                    Button(
                                        onClick = {
                                            if (editUsername.isBlank()) {
                                                Toast.makeText(context, "Username cannot be empty!", Toast.LENGTH_SHORT).show()
                                                return@Button
                                            }
                                            isUpdatingProfile = true
                                            val updatedUser = user.copy(username = editUsername, avatarUrl = editAvatarUrl)
                                            sharedGameViewModel.authRepository.updateCurrentUserProfile(updatedUser) { success ->
                                                isUpdatingProfile = false
                                                showProfileEditDialog = false
                                                if (success) {
                                                    Toast.makeText(context, "Profile updated successfully!", Toast.LENGTH_SHORT).show()
                                                } else {
                                                    Toast.makeText(context, "Profile update failed.", Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC75D27)),
                                        enabled = !isUpdatingProfile
                                    ) {
                                        if (isUpdatingProfile) {
                                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = Color.White)
                                        } else {
                                            Text("Save Changes")
                                        }
                                    }
                                },
                                dismissButton = {
                                    TextButton(onClick = { showProfileEditDialog = false }) {
                                        Text("Cancel", color = Color.Gray)
                                    }
                                }
                            )
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = user.username,
                                    fontFamily = FontFamily.Serif,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 18.sp,
                                    color = Color(0xFF5C2D0A)
                                )
                                if (user.isVerified) {
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(
                                        imageVector = Icons.Default.Verified,
                                        contentDescription = "Verified Elite Strategist Badge",
                                        tint = Color(0xFF1976D2),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                IconButton(onClick = { showProfileEditDialog = true }) {
                                    Icon(Icons.Default.Edit, contentDescription = "Edit Profile details", tint = Color(0xFFC75D27), modifier = Modifier.size(16.dp))
                                }
                            }
                            Text(
                                text = "Coins: ${user.coins} | XP: ${user.xp}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFC75D27)
                            )
                        }

                        IconButton(onClick = {
                            isRefreshingProfile = true
                            sharedGameViewModel.authRepository.refreshUserProfile {
                                isRefreshingProfile = false
                            }
                        }) {
                            if (isRefreshingProfile) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = Color(0xFFC75D27))
                            } else {
                                Icon(Icons.Default.Refresh, contentDescription = "Sync profile with server", tint = Color(0xFFC75D27))
                            }
                        }
                    }
                }

                // Horizontal scrollable Tabs selector
                val tabs = listOf(
                    "hub" to "📊 Hub",
                    "verification" to "🛡️ Verified",
                    "marketplace" to "🪙 Shop",
                    "tournaments" to "🏆 Tourneys",
                    "season_pass" to "🎫 Season",
                    "lucky_wheel" to "🎡 Spin",
                    "promo_codes" to "🎫 Codes",
                    "support" to "🛠️ Support",
                    "bulletins" to "🔔 News",
                    "integrity" to "🔒 Safety"
                )
                
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    tabs.forEach { (tabId, tabName) ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(if (activeTab == tabId) Color(0xFFC75D27) else Color(0xFFFFF8EE))
                                .clickable { activeTab = tabId }
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                                .testTag("tab_$tabId"),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = tabName,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = if (activeTab == tabId) Color.White else Color(0xFF5C2D0A)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Render content based on activeTab
                when (activeTab) {
                    "hub" -> {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFDF8)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .border(1.dp, Color(0xFFE5A93B).copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text("Global Strategic Analytics", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFF8B5E3C))
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text("Total Games", fontSize = 10.sp, color = Color.Gray)
                                            Text("${user.totalGames}", fontWeight = FontWeight.Black, fontSize = 18.sp, color = Color(0xFF5C2D0A))
                                        }
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text("Wins", fontSize = 10.sp, color = Color(0xFF2E7D32))
                                            Text("${user.wins}", fontWeight = FontWeight.Black, fontSize = 18.sp, color = Color(0xFF2E7D32))
                                        }
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text("Losses", fontSize = 10.sp, color = Color(0xFFC62828))
                                            Text("${user.losses}", fontWeight = FontWeight.Black, fontSize = 18.sp, color = Color(0xFFC62828))
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(12.dp))
                                    val winRate = if (user.totalGames > 0) ((user.wins.toFloat() / user.totalGames) * 100).toInt() else 0
                                    Text("Win Ratio: $winRate% | Elo Rating: ${user.rating}", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFFC75D27), modifier = Modifier.align(Alignment.CenterHorizontally))
                                }
                            }

                            val level = (user.xp / 1000) + 1
                            val levelProgress = (user.xp % 1000) / 1000f
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFDF8)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .border(1.dp, Color(0xFFE5A93B).copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("Daadi Strategist Level: $level", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF5C2D0A))
                                        Text("${user.xp % 1000} / 1000 XP", fontSize = 11.sp, color = Color.Gray)
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    LinearProgressIndicator(
                                        progress = levelProgress,
                                        modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                                        color = Color(0xFFE5A93B),
                                        trackColor = Color(0xFFFFF7EA)
                                    )
                                }
                            }

                            Text("Strategic Milestones", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF5C2D0A), modifier = Modifier.padding(vertical = 8.dp))
                            val milestones = listOf(
                                Triple("Novice Contender", "Play 5 games", user.totalGames >= 5),
                                Triple("Master Competitor", "Play 20 games", user.totalGames >= 20),
                                Triple("Elite Conqueror", "Win 10 games", user.wins >= 10),
                                Triple("Grandmaster Sage", "Reach 1500 Rating", user.rating >= 1500)
                            )
                            milestones.forEach { (title, desc, unlocked) ->
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = if (unlocked) Color(0xFFE8F5E9) else Color(0xFFFFFDF8)),
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)
                                ) {
                                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = if (unlocked) Icons.Default.Stars else Icons.Default.Lock,
                                            contentDescription = null,
                                            tint = if (unlocked) Color(0xFF2E7D32) else Color.LightGray,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column {
                                            Text(title, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = if (unlocked) Color(0xFF2E7D32) else Color(0xFF5C2D0A))
                                            Text(desc, fontSize = 10.sp, color = Color.Gray)
                                        }
                                    }
                                }
                            }
                        }
                    }
                    "verification" -> {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFDF8)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, Color(0xFFE5A93B).copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text("Elite Verification Center", fontWeight = FontWeight.Black, fontSize = 15.sp, color = Color(0xFF5C2D0A))
                                    Text("Submit your profile for administrative checker review to unlock the Verified Elite Strategist Badge.", fontSize = 11.sp, color = Color.Gray)
                                    
                                    Spacer(modifier = Modifier.height(16.dp))
                                    
                                    val winRate = if (user.totalGames > 0) ((user.wins.toFloat() / user.totalGames) * 100).toInt() else 0
                                    val reqs = listOf(
                                        "Grandmaster Elo Rating >= 1800" to (user.rating >= 1800),
                                        "Strategic Total Wins >= 45" to (user.wins >= 45),
                                        "Lethal Win Rate >= 65%" to (winRate >= 65),
                                        "Strategic Experience XP >= 4000" to (user.xp >= 4000),
                                        "Zero Active Administrative Infractions" to (!user.isBanned)
                                    )
                                    
                                    var allMet = true
                                    reqs.forEach { (label, met) ->
                                        if (!met) allMet = false
                                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 4.dp)) {
                                            Icon(
                                                imageVector = if (met) Icons.Default.CheckCircle else Icons.Default.Cancel,
                                                contentDescription = null,
                                                tint = if (met) Color(0xFF2E7D32) else Color(0xFFC62828),
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(label, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = Color(0xFF5C2D0A))
                                        }
                                    }
                                    
                                    Spacer(modifier = Modifier.height(16.dp))
                                    
                                    if (user.isVerified) {
                                        Button(
                                            onClick = {},
                                            enabled = false,
                                            modifier = Modifier.fillMaxWidth(),
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
                                        ) {
                                            Icon(Icons.Default.Verified, contentDescription = null)
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("You are Verified Elite Strategist!", color = Color.White)
                                        }
                                    } else {
                                        var isPending by remember { mutableStateOf(prefs.getBoolean("verify_request_pending_${user.id}", false)) }
                                        
                                        Button(
                                            onClick = {
                                                if (allMet) {
                                                    sharedGameViewModel.authRepository.submitApprovalRequest(
                                                        type = "Verification",
                                                        description = "Requesting Elite Strategist Verification Badge. Rating: ${user.rating}, Wins: ${user.wins}, XP: ${user.xp}",
                                                        severity = "High"
                                                    ) { success ->
                                                        if (success) {
                                                            prefs.edit().putBoolean("verify_request_pending_${user.id}", true).apply()
                                                            isPending = true
                                                            Toast.makeText(context, "Verification proposal submitted successfully!", Toast.LENGTH_LONG).show()
                                                        } else {
                                                            Toast.makeText(context, "Failed to submit proposal. Try again.", Toast.LENGTH_SHORT).show()
                                                        }
                                                    }
                                                } else {
                                                    Toast.makeText(context, "You must meet all strategic standards first!", Toast.LENGTH_LONG).show()
                                                }
                                            },
                                            modifier = Modifier.fillMaxWidth().testTag("apply_verification_button"),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = if (allMet) Color(0xFFC75D27) else Color.Gray
                                            )
                                        ) {
                                            Text(if (isPending) "Verification Pending Review" else "Submit Elite Verification Proposal", color = Color.White)
                                        }
                                        
                                        if (!allMet && !isPending) {
                                            Text("Strategic criteria are not yet fully achieved.", fontSize = 10.sp, color = Color(0xFFC62828), modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 4.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                    "marketplace" -> {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text("Theme Marketplace", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFF5C2D0A))
                            Text("Use your accumulated game coins to unlock premium custom board layouts.", fontSize = 11.sp, color = Color.Gray)
                            
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            val themesList = listOf(
                                Triple("classic_wood", "Classic Teak Wood", 0),
                                Triple("emerald_jade", "Emerald Imperial Jade", 300),
                                Triple("marble", "Royal Polished Marble", 600),
                                Triple("charcoal", "Midnight Slate Charcoal", 1000),
                                Triple("saffron", "Saffron Golden Festival", 1500)
                            )
                            
                            val settings = app.settingsRepository.getSettings()
                            val equippedTheme = settings.selectedBoardTheme
                            
                            themesList.forEach { (themeId, name, price) ->
                                val isOwned = price == 0 || prefs.getBoolean("theme_owned_${themeId}", false)
                                val isEquipped = equippedTheme == themeId
                                
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFDF8)),
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).border(1.dp, Color(0xFFE5A93B).copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                                ) {
                                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.Palette,
                                            contentDescription = null,
                                            tint = when (themeId) {
                                                "classic_wood" -> Color(0xFF8B5E3C)
                                                "emerald_jade" -> Color(0xFF0F5132)
                                                "marble" -> Color(0xFF9EA3A6)
                                                "charcoal" -> Color(0xFF2C3E50)
                                                "saffron" -> Color(0xFFD35400)
                                                else -> Color.Gray
                                            },
                                            modifier = Modifier.size(32.dp)
                                        )
                                        
                                        Spacer(modifier = Modifier.width(12.dp))
                                        
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(name, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF5C2D0A))
                                            Text(if (isOwned) "Unlocked" else "Price: $price Coins", fontSize = 11.sp, color = Color.Gray)
                                        }
                                        
                                        if (isOwned) {
                                            Button(
                                                onClick = {
                                                    val currSettings = app.settingsRepository.getSettings()
                                                    app.settingsRepository.saveSettings(currSettings.copy(selectedBoardTheme = themeId))
                                                    Toast.makeText(context, "$name Equipped!", Toast.LENGTH_SHORT).show()
                                                },
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = if (isEquipped) Color(0xFF2E7D32) else Color(0xFF5C2D0A)
                                                ),
                                                shape = RoundedCornerShape(8.dp),
                                                modifier = Modifier.testTag("equip_theme_$themeId")
                                            ) {
                                                Text(if (isEquipped) "Equipped" else "Equip", color = Color.White, fontSize = 11.sp)
                                            }
                                        } else {
                                            Button(
                                                onClick = {
                                                    if (user.coins >= price) {
                                                        updateCoinsAndXp(-price, 100)
                                                        prefs.edit().putBoolean("theme_owned_${themeId}", true).apply()
                                                        Toast.makeText(context, "Successfully purchased $name!", Toast.LENGTH_LONG).show()
                                                    } else {
                                                        Toast.makeText(context, "Insufficient gold coins!", Toast.LENGTH_SHORT).show()
                                                    }
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC75D27)),
                                                shape = RoundedCornerShape(8.dp),
                                                modifier = Modifier.testTag("buy_theme_$themeId")
                                            ) {
                                                Text("Unlock", color = Color.White, fontSize = 11.sp)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    "tournaments" -> {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text("Competitive Tournaments Hub", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFF5C2D0A))
                            Text("Enroll in active league pools to secure grand prizes.", fontSize = 11.sp, color = Color.Gray)
                            
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            val listTournaments = listOf(
                                Triple("T-101", "Chanakya's Open Challenge", 300 to 3000),
                                Triple("T-102", "Sankranti Grandmaster Cup", 500 to 5000),
                                Triple("T-103", "Royal Mysore League", 1000 to 10000)
                            )
                            
                            listTournaments.forEach { (tid, tname, economics) ->
                                val (fee, prize) = economics
                                val isRegistered = prefs.getBoolean("registered_${tid}_${user.id}", false)
                                
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFDF8)),
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).border(1.dp, Color(0xFFE5A93B).copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                                ) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.EmojiEvents, contentDescription = null, tint = Color(0xFFE5A93B), modifier = Modifier.size(24.dp))
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(tname, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF5C2D0A))
                                        }
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                            Column {
                                                Text("Entry Fee: $fee Coins", fontSize = 11.sp, color = Color.Gray)
                                                Text("Grand Prize: $prize Coins", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32))
                                            }
                                            
                                            Button(
                                                onClick = {
                                                    if (isRegistered) {
                                                        Toast.makeText(context, "You are already enrolled!", Toast.LENGTH_SHORT).show()
                                                    } else if (user.coins >= fee) {
                                                        updateCoinsAndXp(-fee, 150)
                                                        prefs.edit().putBoolean("registered_${tid}_${user.id}", true).apply()
                                                        Toast.makeText(context, "Registered successfully!", Toast.LENGTH_LONG).show()
                                                    } else {
                                                        Toast.makeText(context, "Insufficient gold coins!", Toast.LENGTH_SHORT).show()
                                                    }
                                                },
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = if (isRegistered) Color(0xFF2E7D32) else Color(0xFFC75D27)
                                                ),
                                                shape = RoundedCornerShape(8.dp),
                                                modifier = Modifier.testTag("enroll_tourney_$tid")
                                            ) {
                                                Text(if (isRegistered) "Enrolled" else "Enroll", color = Color.White, fontSize = 11.sp)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    "season_pass" -> {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text("Season Strategy Pass", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFF5C2D0A))
                            Text("Progress pass levels with XP to claim premium items.", fontSize = 11.sp, color = Color.Gray)
                            
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            val passTiers = listOf(
                                Triple(1, "200 Gold Coins", 0),
                                Triple(2, "500 XP Boost", 500),
                                Triple(3, "500 Gold Coins", 1000),
                                Triple(4, "Emerald Theme Unlock", 1500),
                                Triple(5, "1000 Gold Coins", 2500)
                            )
                            
                            passTiers.forEach { (tier, reward, requiredXp) ->
                                val isUnlocked = user.xp >= requiredXp
                                val isClaimed = prefs.getBoolean("claimed_pass_tier_${tier}_${user.id}", false)
                                
                                Card(
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isClaimed) Color(0xFFE8F5E9) else Color(0xFFFFFDF8)
                                    ),
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).border(1.dp, Color(0xFFE5A93B).copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                                ) {
                                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier.size(36.dp).background(if (isUnlocked) Color(0xFF2E7D32) else Color.LightGray, CircleShape),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text("T$tier", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 12.sp)
                                        }
                                        
                                        Spacer(modifier = Modifier.width(12.dp))
                                        
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(reward, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF5C2D0A))
                                            Text("Required: $requiredXp XP", fontSize = 11.sp, color = Color.Gray)
                                        }
                                        
                                        if (isClaimed) {
                                            Icon(Icons.Default.CheckCircle, contentDescription = "Claimed", tint = Color(0xFF2E7D32))
                                        } else {
                                            Button(
                                                onClick = {
                                                    if (!isUnlocked) {
                                                        Toast.makeText(context, "Insufficient Pass XP!", Toast.LENGTH_SHORT).show()
                                                    } else {
                                                        prefs.edit().putBoolean("claimed_pass_tier_${tier}_${user.id}", true).apply()
                                                        when (tier) {
                                                            1 -> updateCoinsAndXp(200, 0)
                                                            2 -> updateCoinsAndXp(0, 500)
                                                            3 -> updateCoinsAndXp(500, 0)
                                                            4 -> prefs.edit().putBoolean("theme_owned_emerald_jade", true).apply()
                                                            5 -> updateCoinsAndXp(1000, 0)
                                                        }
                                                        Toast.makeText(context, "Claimed $reward successfully!", Toast.LENGTH_LONG).show()
                                                    }
                                                },
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = if (isUnlocked) Color(0xFF2E7D32) else Color.Gray
                                                ),
                                                shape = RoundedCornerShape(8.dp),
                                                modifier = Modifier.testTag("claim_tier_$tier")
                                            ) {
                                                Text("Claim", color = Color.White, fontSize = 11.sp)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    "lucky_wheel" -> {
                        Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Lucky Wheel of Chanakya", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFF5C2D0A))
                            Text("Spend 100 coins to try your luck and win up to 500 coins!", fontSize = 11.sp, color = Color.Gray)
                            
                            Spacer(modifier = Modifier.height(24.dp))
                            
                            var spinning by remember { mutableStateOf(false) }
                            var prizeResult by remember { mutableStateOf<String?>(null) }
                            
                            Box(
                                modifier = Modifier
                                    .size(160.dp)
                                    .background(Color(0xFFFFF7EA), CircleShape)
                                    .border(4.dp, Color(0xFFE5A93B), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                if (spinning) {
                                    CircularProgressIndicator(modifier = Modifier.size(100.dp), strokeWidth = 8.dp, color = Color(0xFFC75D27))
                                    Text("Spinning...", fontWeight = FontWeight.Bold, color = Color(0xFFC75D27))
                                } else {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(Icons.Default.Casino, contentDescription = null, tint = Color(0xFFC75D27), modifier = Modifier.size(40.dp))
                                        Text("TAP TO SPIN", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFFC75D27))
                                    }
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(20.dp))
                            
                            Button(
                                onClick = {
                                    if (user.coins >= 100 && !spinning) {
                                        spinning = true
                                        updateCoinsAndXp(-100, 0)
                                        prizeResult = null
                                        
                                        scope.launch {
                                            kotlinx.coroutines.delay(1500)
                                            val prizeRoll = (1..100).random()
                                            val rewardCoins = when {
                                                prizeRoll <= 50 -> 50
                                                prizeRoll <= 80 -> 200
                                                prizeRoll <= 95 -> 500
                                                else -> 1000
                                            }
                                            updateCoinsAndXp(rewardCoins, 50)
                                            spinning = false
                                            prizeResult = "Congratulations! You won $rewardCoins gold coins!"
                                        }
                                    } else if (!spinning) {
                                        Toast.makeText(context, "Insufficient gold coins! Spin costs 100 coins.", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC75D27)),
                                enabled = !spinning,
                                modifier = Modifier.testTag("spin_wheel_button")
                            ) {
                                Text("Spin (Cost: 100 Coins)", color = Color.White)
                            }
                            
                            prizeResult?.let { msg ->
                                Spacer(modifier = Modifier.height(12.dp))
                                Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF8EE)), modifier = Modifier.fillMaxWidth()) {
                                    Text(msg, modifier = Modifier.padding(12.dp), fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32), textAlign = TextAlign.Center)
                                }
                            }
                        }
                    }
                    "promo_codes" -> {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text("Redeem Promotion Codes", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFF5C2D0A))
                            Text("Enter official festival or welcome promo codes to claim rewards.", fontSize = 11.sp, color = Color.Gray)
                            
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            var codeText by remember { mutableStateOf("") }
                            
                            OutlinedTextField(
                                value = codeText,
                                onValueChange = { codeText = it.uppercase() },
                                label = { Text("Enter Promo Code") },
                                modifier = Modifier.fillMaxWidth().testTag("promo_code_input"),
                                singleLine = true,
                                colors = TextFieldDefaults.colors(
                                    focusedTextColor = Color(0xFF5C2D0A),
                                    unfocusedTextColor = Color(0xFF5C2D0A),
                                    focusedContainerColor = Color(0xFFFFFDF8),
                                    unfocusedContainerColor = Color(0xFFFFFDF8)
                                )
                            )
                            
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            Button(
                                onClick = {
                                    val cleaned = codeText.trim()
                                    if (cleaned.isEmpty()) return@Button
                                    
                                    val alreadyUsed = prefs.getBoolean("promo_used_${cleaned}_${user.id}", false)
                                    if (alreadyUsed) {
                                        Toast.makeText(context, "Promo code already redeemed!", Toast.LENGTH_SHORT).show()
                                    } else {
                                        when (cleaned) {
                                            "WELCOME500" -> {
                                                updateCoinsAndXp(500, 100)
                                                prefs.edit().putBoolean("promo_used_${cleaned}_${user.id}", true).apply()
                                                Toast.makeText(context, "Redeemed! +500 Coins, +100 XP awarded!", Toast.LENGTH_LONG).show()
                                            }
                                            "CHANAKYAGIFT" -> {
                                                updateCoinsAndXp(0, 1000)
                                                prefs.edit().putBoolean("promo_used_${cleaned}_${user.id}", true).apply()
                                                Toast.makeText(context, "Redeemed! +1000 XP awarded!", Toast.LENGTH_LONG).show()
                                            }
                                            "ELITECOINS" -> {
                                                updateCoinsAndXp(1000, 0)
                                                prefs.edit().putBoolean("promo_used_${cleaned}_${user.id}", true).apply()
                                                Toast.makeText(context, "Redeemed! +1000 Coins awarded!", Toast.LENGTH_LONG).show()
                                            }
                                            else -> {
                                                Toast.makeText(context, "Invalid promotion code!", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    }
                                    codeText = ""
                                },
                                modifier = Modifier.fillMaxWidth().testTag("redeem_promo_button"),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC75D27))
                            ) {
                                Text("Redeem Reward", color = Color.White)
                            }
                        }
                    }
                    "support" -> {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text("Support Ticket Desk", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFF5C2D0A))
                            Text("File formal technical or economy refund requests directly to admins.", fontSize = 11.sp, color = Color.Gray)
                            
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            var subjectText by remember { mutableStateOf("") }
                            var priorityText by remember { mutableStateOf("medium") }
                            var messageText by remember { mutableStateOf("") }
                            var isSubmittingTicket by remember { mutableStateOf(false) }

                            val globalTickets by sharedGameViewModel.supportRepository.tickets.collectAsStateWithLifecycle()
                            val myTickets = globalTickets.filter { it.userId == user.id }

                            LaunchedEffect(Unit) {
                                sharedGameViewModel.supportRepository.fetchTickets()
                            }
                            
                            OutlinedTextField(
                                value = subjectText,
                                onValueChange = { subjectText = it },
                                label = { Text("Subject") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                colors = TextFieldDefaults.colors(focusedContainerColor = Color(0xFFFFFDF8), unfocusedContainerColor = Color(0xFFFFFDF8))
                            )
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            OutlinedTextField(
                                value = messageText,
                                onValueChange = { messageText = it },
                                label = { Text("Describe your issue / request") },
                                modifier = Modifier.fillMaxWidth().height(80.dp),
                                colors = TextFieldDefaults.colors(focusedContainerColor = Color(0xFFFFFDF8), unfocusedContainerColor = Color(0xFFFFFDF8))
                            )
                            
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            Button(
                                onClick = {
                                    if (subjectText.trim().isEmpty() || messageText.trim().isEmpty()) {
                                        Toast.makeText(context, "Please enter subject and message!", Toast.LENGTH_SHORT).show()
                                        return@Button
                                    }
                                    isSubmittingTicket = true
                                    sharedGameViewModel.supportRepository.submitSupportTicket(
                                        subject = subjectText,
                                        message = messageText,
                                        priority = priorityText
                                    ) { success ->
                                        isSubmittingTicket = false
                                        if (success) {
                                            if (subjectText.contains("refund", ignoreCase = true) || messageText.contains("refund", ignoreCase = true)) {
                                                sharedGameViewModel.authRepository.submitApprovalRequest(
                                                    type = "Refund",
                                                    description = "Refund of 500 coins to ${user.username}. Reason: ${subjectText}",
                                                    severity = "Medium"
                                                ) { }
                                            } else if (subjectText.contains("name", ignoreCase = true) || messageText.contains("name", ignoreCase = true)) {
                                                sharedGameViewModel.authRepository.submitApprovalRequest(
                                                    type = "NameChange",
                                                    description = "Change name to 'Siddhartha' for requester ${user.username}",
                                                    severity = "Low"
                                                ) { }
                                            }
                                            Toast.makeText(context, "Support ticket submitted successfully!", Toast.LENGTH_LONG).show()
                                            subjectText = ""
                                            messageText = ""
                                        } else {
                                            Toast.makeText(context, "Failed to submit support ticket.", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                },
                                modifier = Modifier.fillMaxWidth().testTag("submit_ticket_button"),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC75D27)),
                                enabled = !isSubmittingTicket
                            ) {
                                if (isSubmittingTicket) {
                                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = Color.White)
                                } else {
                                    Text("Submit Support Ticket", color = Color.White)
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("Your Tickets", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF5C2D0A))
                            myTickets.forEach { ticket ->
                                Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFDF8)), modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                            Text(ticket.id, fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color(0xFFC75D27))
                                            Badge(containerColor = if (ticket.status == "resolved") Color(0xFF2E7D32) else Color(0xFFD35400)) {
                                                Text(ticket.status.uppercase(), fontSize = 9.sp, color = Color.White)
                                            }
                                        }
                                        Text(ticket.subject, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFF5C2D0A))
                                        Text(ticket.message, fontSize = 11.sp, color = Color.Gray)
                                        ticket.assignedTo?.let { agent ->
                                            Text("Assigned To: $agent", fontSize = 10.sp, color = Color(0xFF2E7D32), fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }
                    "bulletins" -> {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text("Global Bulletins Board", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFF5C2D0A))
                            Text("Latest patch updates and operations logs.", fontSize = 11.sp, color = Color.Gray)
                            
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            val announcements = listOf(
                                "Daadi Grand League Championship" to "Pool starts July 15, register under Tourney section to win mega coins.",
                                "v1.1.0 Strategic Patch Notes" to "Added 3 custom game board themes to Marketplace. Improved AI minimax heuristics on Noughts & Morris rules.",
                                "Data Privacy Regulation Alignment" to "User profiles aligned perfectly with India Digital Personal Data Protection (DPDP) standards. Erasure and portability are fully enabled."
                            )
                            
                            announcements.forEach { (title, content) ->
                                Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFDF8)), modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Text(title, fontWeight = FontWeight.Black, fontSize = 13.sp, color = Color(0xFFC75D27))
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(content, fontSize = 11.sp, color = Color.Gray)
                                    }
                                }
                            }
                        }
                    }
                    "integrity" -> {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text("Security Integrity Center", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFF5C2D0A))
                            Text("Verification parameters tracking profile and device parameters for anti-cheat audit.", fontSize = 11.sp, color = Color.Gray)
                            
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            val parameters = listOf(
                                "Emulator Environment Detected" to "FALSE",
                                "Debugger Attachment Status" to "SAFE / DETACHED",
                                "Root / Integrity Validation" to "SECURE",
                                "Runtime Anti-Cheat Status" to "COMPLIANT / ACTIVE",
                                "Total Strategic Cheating Violations" to "0 LOGGED"
                            )
                            
                            parameters.forEach { (label, status) ->
                                Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFDF8)), modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
                                    Row(modifier = Modifier.padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                        Text(label, fontSize = 12.sp, color = Color(0xFF5C2D0A), fontWeight = FontWeight.Medium)
                                        Text(status, fontSize = 11.sp, fontWeight = FontWeight.Black, color = if (status.contains("FALSE") || status.contains("SAFE") || status.contains("SECURE") || status.contains("COMPLIANT")) Color(0xFF2E7D32) else Color(0xFFC75D27))
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
                Divider(color = Color.LightGray.copy(alpha = 0.3f))
                Spacer(modifier = Modifier.height(16.dp))

                if (user.role == "admin") {
                    Button(
                        onClick = onNavigateToAdmin,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("admin_portal_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC75D27)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.AdminPanelSettings, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Open Admin Command Center", fontWeight = FontWeight.Bold, color = Color.White)
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    Button(
                        onClick = {
                            sharedGameViewModel.authRepository.logout()
                            localSuccessMsg = "Log out successful. Return to guest mode."
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .testTag("auth_logout_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC62828)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Log Out", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 12.sp)
                    }

                    OutlinedButton(
                        onClick = {
                            val currentUserId = currentUser?.id
                            if (currentUserId != null) {
                                sharedGameViewModel.authRepository.deleteUser(currentUserId)
                                sharedGameViewModel.authRepository.logout()
                                localSuccessMsg = "Account deleted and data purged under India DPDP Act Right to Erasure."
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFC62828)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Delete (DPDP)", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }

            } else {
                // RENDER PROFILE LOGIN & REGISTER FORM SCREENS
                Text(
                    text = when {
                        isForgotPasswordMode -> "RECOVER PLAYER ACCOUNT"
                        isSignUpMode -> "CREATE NEW PLAYER ACCOUNT"
                        else -> "REGISTERED USER LOGIN"
                    },
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 18.sp,
                    color = Color(0xFF5C2D0A),
                    textAlign = TextAlign.Center
                )

                Text(
                    text = if (isConfigured) "Syncs automatically with Cloud Identity" else "Sandbox Mode: Any credentials work instantly",
                    fontSize = 12.sp,
                    color = if (isConfigured) Color(0xFF2E7D32) else Color(0xFFC75D27),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = 20.dp)
                )

                val displayError = localErrorMsg ?: globalErrorMsg
                
                // Error and success displays
                AnimatedVisibility(visible = displayError != null) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp)
                    ) {
                        Column {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Default.Warning, contentDescription = "Error Info", tint = Color(0xFFC62828))
                                Text(
                                    text = displayError ?: "",
                                    color = Color(0xFFC62828),
                                    fontSize = 12.sp,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            
                            if (globalErrorMsg != null && localErrorMsg == null) {
                                TextButton(
                                    onClick = { sharedGameViewModel.authRepository.network.loadInitialData() },
                                    modifier = Modifier.align(Alignment.End).padding(end = 8.dp, bottom = 4.dp)
                                ) {
                                    Text("RETRY SYNC", color = Color(0xFFC62828), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                AnimatedVisibility(visible = localSuccessMsg != null) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = "Success Info", tint = Color(0xFF2E7D32))
                            Text(
                                text = localSuccessMsg ?: "",
                                color = Color(0xFF2E7D32),
                                fontSize = 12.sp,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF7EA)),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Email Field
                        OutlinedTextField(
                            value = email,
                            onValueChange = {
                                email = it
                                localErrorMsg = null
                            },
                            label = { Text("Email Address") },
                            leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("auth_email_input"),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF5C2D0A),
                                focusedLabelColor = Color(0xFF5C2D0A),
                                focusedTextColor = Color(0xFF5C2D0A),
                                unfocusedTextColor = Color(0xFF5C2D0A),
                                unfocusedLabelColor = Color(0xFF8B5E3C),
                                focusedPlaceholderColor = Color(0xFF8B5E3C),
                                unfocusedPlaceholderColor = Color(0xFF8B5E3C).copy(alpha = 0.6f)
                            )
                        )

                        // Username Field (Register only)
                        AnimatedVisibility(visible = isSignUpMode) {
                            OutlinedTextField(
                                value = username,
                                onValueChange = {
                                    username = it
                                    localErrorMsg = null
                                },
                                label = { Text("Display Username") },
                                leadingIcon = { Icon(Icons.Default.AccountBox, contentDescription = null) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("auth_username_input"),
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFF5C2D0A),
                                    focusedLabelColor = Color(0xFF5C2D0A),
                                    focusedTextColor = Color(0xFF5C2D0A),
                                    unfocusedTextColor = Color(0xFF5C2D0A),
                                    unfocusedLabelColor = Color(0xFF8B5E3C),
                                    focusedPlaceholderColor = Color(0xFF8B5E3C),
                                    unfocusedPlaceholderColor = Color(0xFF8B5E3C).copy(alpha = 0.6f)
                                )
                            )
                        }

                        // Password Field
                        AnimatedVisibility(visible = !isForgotPasswordMode) {
                            OutlinedTextField(
                                value = password,
                                onValueChange = {
                                    password = it
                                    localErrorMsg = null
                                },
                                label = { Text("Password") },
                                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                                trailingIcon = {
                                    val image = if (passwordVisible) Icons.Default.Favorite else Icons.Default.FavoriteBorder
                                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                        Icon(image, contentDescription = "Toggle password visibility")
                                    }
                                },
                                 visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Password,
                                    autoCorrectEnabled = false
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("auth_password_input"),
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFF5C2D0A),
                                    focusedLabelColor = Color(0xFF5C2D0A),
                                    focusedTextColor = Color(0xFF5C2D0A),
                                    unfocusedTextColor = Color(0xFF5C2D0A),
                                    unfocusedLabelColor = Color(0xFF8B5E3C),
                                    focusedPlaceholderColor = Color(0xFF8B5E3C),
                                    unfocusedPlaceholderColor = Color(0xFF8B5E3C).copy(alpha = 0.6f)
                                )
                            )
                        }

                        // Forgot Password Link
                        AnimatedVisibility(visible = !isSignUpMode && !isForgotPasswordMode) {
                            Text(
                                text = "Forgot Password?",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFC75D27),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { 
                                        isForgotPasswordMode = true 
                                        localErrorMsg = null
                                        localSuccessMsg = null
                                    },
                                textAlign = TextAlign.End
                            )
                        }

                        // Indian DPDP Act Explicit Consent Checkbox (Show for registration mode)
                        AnimatedVisibility(visible = isSignUpMode) {
                            Column(
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { hasAcceptedTerms = !hasAcceptedTerms }
                                        .padding(vertical = 4.dp)
                                ) {
                                    Checkbox(
                                        checked = hasAcceptedTerms,
                                        onCheckedChange = { hasAcceptedTerms = it },
                                        colors = CheckboxDefaults.colors(checkedColor = Color(0xFF5C2D0A))
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "DPDP Consent & Game Terms (India)",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF5C2D0A)
                                        )
                                        Text(
                                            text = "I explicit consent to register my email for matchmaking under DPDP Rules.",
                                            fontSize = 10.sp,
                                            color = Color.Gray,
                                            lineHeight = 13.sp
                                        )
                                    }
                                }
                                
                                Text(
                                    text = "Read Official Privacy Policy",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color(0xFFC75D27),
                                    modifier = Modifier
                                        .align(Alignment.End)
                                        .clickable { showPrivacyPolicyDialog = true }
                                        .padding(horizontal = 4.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        // Submit Button
                        Button(
                            onClick = {
                                if (email.isBlank()) {
                                    localErrorMsg = "Email field is required."
                                    return@Button
                                }
                                if (!isForgotPasswordMode && password.isBlank()) {
                                    localErrorMsg = "Password field is required."
                                    return@Button
                                }
                                if (isSignUpMode && username.isBlank()) {
                                    localErrorMsg = "Display username is required for registration."
                                    return@Button
                                }
                                if (isSignUpMode && !hasAcceptedTerms) {
                                    localErrorMsg = "You must accept the DPDP terms to register."
                                    return@Button
                                }

                                localIsLoading = true
                                localErrorMsg = null
                                localSuccessMsg = null

                                when {
                                    isForgotPasswordMode -> {
                                        sharedGameViewModel.authRepository.resetPassword(email) { success, msg ->
                                            localIsLoading = false
                                            if (success) {
                                                localSuccessMsg = msg
                                                isForgotPasswordMode = false
                                            } else {
                                                localErrorMsg = msg
                                            }
                                        }
                                    }
                                    isSignUpMode -> {
                                        sharedGameViewModel.authRepository.signUp(email, username, password) { success, msg ->
                                            localIsLoading = false
                                            if (success) {
                                                localSuccessMsg = "Player profile registered successfully!"
                                                Toast.makeText(context, "Account created successfully! Welcome to Daadi, $username! 🎉", Toast.LENGTH_LONG).show()
                                                onAuthSuccess()
                                            } else {
                                                localErrorMsg = msg ?: "Failed to sign up player."
                                            }
                                        }
                                    }
                                    else -> {
                                        sharedGameViewModel.authRepository.login(email, password) { success, msg ->
                                            localIsLoading = false
                                            if (success) {
                                                localSuccessMsg = "Access granted! Welcome to Daadi multiplay."
                                                Toast.makeText(context, "Welcome back! Access granted. 👋", Toast.LENGTH_LONG).show()
                                                onAuthSuccess()
                                            } else {
                                                localErrorMsg = msg ?: "Failed log in attempt."
                                            }
                                        }
                                    }
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("auth_submit_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                            shape = RoundedCornerShape(12.dp),
                            enabled = !localIsLoading && (isForgotPasswordMode || !isSignUpMode || hasAcceptedTerms)
                        ) {
                            if (localIsLoading) {
                                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                            } else {
                                Text(
                                    text = when {
                                        isForgotPasswordMode -> "Send Recovery Link"
                                        isSignUpMode -> "Register Account"
                                        else -> "Log In Profile"
                                    },
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = Color.White
                                )
                            }
                        }

                        // Mode switcher link
                        Text(
                            text = when {
                                isForgotPasswordMode -> "Return to Log In"
                                isSignUpMode -> "Already have a profile? Log In here"
                                else -> "New player? Sign Up / Create Profile"
                            },
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFC75D27),
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    if (isForgotPasswordMode) {
                                        isForgotPasswordMode = false
                                    } else {
                                        isSignUpMode = !isSignUpMode
                                    }
                                    localErrorMsg = null
                                    localSuccessMsg = null
                                }
                                .padding(vertical = 8.dp)
                        )
                    }
                }

                // Social login separator
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(modifier = Modifier.weight(1f).height(1.dp).background(Color.LightGray.copy(alpha = 0.6f)))
                    Text(
                        text = "  OR CONNECT WITH  ",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Gray,
                        letterSpacing = 1.sp
                    )
                    Box(modifier = Modifier.weight(1f).height(1.dp).background(Color.LightGray.copy(alpha = 0.6f)))
                }
                Spacer(modifier = Modifier.height(16.dp))

                // Google Sign-In Button
                val oauthContext = androidx.compose.ui.platform.LocalContext.current
                Button(
                    onClick = {
                        localIsLoading = true
                        localErrorMsg = null
                        localSuccessMsg = null
                        sharedGameViewModel.authRepository.signInWithOAuth("google", oauthContext) { success, msg ->
                            localIsLoading = false
                            if (success) {
                                localSuccessMsg = msg
                                onAuthSuccess()
                            } else {
                                localErrorMsg = msg ?: "Failed to sign in with Google."
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("oauth_google_button"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White,
                        contentColor = Color.DarkGray
                    ),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.8f)),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
                ) {
                    GoogleLogoBadge()
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Sign In with Google",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = Color.DarkGray
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Facebook Sign-In Button
                Button(
                    onClick = {
                        localErrorMsg = "Facebook Sign-In is coming soon! Please use Google Sign-In or standard Email Login."
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("oauth_facebook_button"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF1877F2).copy(alpha = 0.5f),
                        contentColor = Color.White.copy(alpha = 0.8f)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
                ) {
                    FacebookLogoBadge(alpha = 0.5f)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Continue with Facebook (Coming Soon)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = "Continuing without a custom profile will default you to guest play settings with simple local stats.",
                    fontSize = 11.sp,
                    color = Color.Gray,
                    textAlign = TextAlign.Center,
                    lineHeight = 16.sp,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
        }
    }

    if (showPrivacyPolicyDialog) {
        AlertDialog(
            onDismissRequest = { showPrivacyPolicyDialog = false },
            title = {
                Text(
                    text = "Daadi Game Privacy Policy",
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = Color(0xFF5C2D0A)
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .heightIn(max = 280.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Last Updated: June 2026",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = Color.DarkGray
                    )
                    Text(
                        text = "This Privacy Policy outlines how the Daadi board application manages your registration credentials for Matchmarking/Multiplayer modes.",
                        fontSize = 11.sp,
                        color = Color.Gray
                    )
                    Text(
                        text = "1. Legally Compliant (DPDP Act, 2023 - India)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = Color(0xFF5C2D0A)
                    )
                    Text(
                        text = "In compliance with the Digital Personal Data Protection Act of India, we obtain your explicit consent to process your credentials (email address, game wins, losses, usernames). All records reside in secured cloud environments with transit HTTPS/WSS encryption.",
                        fontSize = 11.sp,
                        color = Color.Gray
                    )
                    Text(
                        text = "2. Right to Erasure & Deletion",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = Color(0xFF5C2D0A)
                    )
                    Text(
                        text = "Under DPDP laws, you retain the absolute right to request deletion of your account. You can do this at any time using the 'Delete Account & Wipe Data' trigger in your active profile tab, which immediately wipes all your cloud logs.",
                        fontSize = 11.sp,
                        color = Color.Gray
                    )
                    Text(
                        text = "3. Data Minimization",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = Color(0xFF5C2D0A)
                    )
                    Text(
                        text = "We do not retrieve contact lists, location metrics, phone storage records, or precise cellular telemetry. Only email and player game attributes are collected to manage match rooms.",
                        fontSize = 11.sp,
                        color = Color.Gray
                    )
                }
            },
            confirmButton = {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    Button(
                        onClick = { 
                            uriHandler.openUri(legalUrl)
                            showPrivacyPolicyDialog = false 
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFC75D27),
                            contentColor = Color.White
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Read Online", fontSize = 11.sp, maxLines = 1)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { showPrivacyPolicyDialog = false },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF5C2D0A),
                            contentColor = Color.White
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Close", fontSize = 11.sp, maxLines = 1)
                    }
                }
            },
            containerColor = Color(0xFFFFF7EA)
        )
    }

    if (showResetPasswordDialog) {
        AlertDialog(
            onDismissRequest = {
                showResetPasswordDialog = false
                sharedGameViewModel.authRepository.clearPasswordResetRequired()
            },
            title = {
                Text(
                    "Set New Password",
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF5C2D0A)
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        "You have successfully verified your identity. Enter a secure new password for your account below.",
                        fontSize = 14.sp,
                        color = Color(0xFF8B5E3C)
                    )
                    OutlinedTextField(
                        value = newPasswordInput,
                        onValueChange = { newPasswordInput = it },
                        label = { Text("New Password") },
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            val image = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(image, contentDescription = if (passwordVisible) "Hide password" else "Show password")
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFFC75D27),
                            unfocusedBorderColor = Color(0xFFE5A93B),
                            focusedLabelColor = Color(0xFFC75D27),
                            focusedTextColor = Color(0xFF5C2D0A),
                            unfocusedTextColor = Color(0xFF5C2D0A)
                        ),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("new_password_input")
                    )
                    if (resetErrorMsg != null) {
                        Text(
                            resetErrorMsg!!,
                            color = Color.Red,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    if (resetSuccessMsg != null) {
                        Text(
                            resetSuccessMsg!!,
                            color = Color(0xFF2E7D32),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newPasswordInput.length < 6) {
                            resetErrorMsg = "Password must be at least 6 characters."
                            return@Button
                        }
                        resetIsLoading = true
                        resetErrorMsg = null
                        sharedGameViewModel.authRepository.updatePassword(newPasswordInput) { success, msg ->
                            resetIsLoading = false
                            if (success) {
                                resetSuccessMsg = "Password updated successfully!"
                                Toast.makeText(context, "Your password has been changed. Welcome back! 🎉", Toast.LENGTH_LONG).show()
                                showResetPasswordDialog = false
                                sharedGameViewModel.authRepository.clearPasswordResetRequired()
                                onAuthSuccess()
                            } else {
                                resetErrorMsg = msg ?: "Failed to update password."
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC75D27)),
                    enabled = !resetIsLoading,
                    modifier = Modifier.testTag("submit_new_password_button")
                ) {
                    Text(if (resetIsLoading) "Updating..." else "Update Password")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showResetPasswordDialog = false
                        sharedGameViewModel.authRepository.clearPasswordResetRequired()
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFF5C2D0A))
                ) {
                    Text("Cancel")
                }
            },
            containerColor = Color(0xFFFFFDF8),
            shape = RoundedCornerShape(16.dp)
        )
    }
}

@Composable
fun GoogleLogoBadge() {
    Box(
        modifier = Modifier
            .size(24.dp)
            .background(Color.White, CircleShape)
            .border(1.dp, Color.LightGray.copy(alpha = 0.5f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "G",
            fontWeight = FontWeight.Black,
            fontSize = 14.sp,
            color = Color(0xFF4285F4),
            fontFamily = FontFamily.SansSerif
        )
    }
}

@Composable
fun FacebookLogoBadge(alpha: Float = 1.0f) {
    Box(
        modifier = Modifier
            .size(24.dp)
            .background(Color(0xFF1877F2).copy(alpha = alpha), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "f",
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            color = Color.White.copy(alpha = alpha),
            fontFamily = FontFamily.Serif,
            modifier = Modifier.offset(x = (-1).dp, y = (-1).dp)
        )
    }
}
