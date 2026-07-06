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
                // RENDER LOGGED-IN PROFILE VIEWS
                val user = currentUser!!
                Text(
                    text = "AUTHENTICATED PLAYER",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFC75D27),
                    letterSpacing = 1.5.sp
                )
                Text(
                    text = user.username,
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 24.sp,
                    color = Color(0xFF5C2D0A)
                )
                Text(
                    text = user.email,
                    fontSize = 13.sp,
                    color = Color.Gray,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // Matchmaking Stats Registry Card
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFDF8)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                        .border(1.dp, Color(0xFFE5A93B).copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Global Ranking Statistics",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF8B5E3C)
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Total Duel Runs", fontSize = 11.sp, color = Color.Gray)
                                Text(
                                    "${user.totalGames}",
                                    fontWeight = FontWeight.Black,
                                    fontSize = 20.sp,
                                    color = Color(0xFF5C2D0A)
                                )
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Wins", fontSize = 11.sp, color = Color(0xFF2E7D32))
                                Text(
                                    "${user.wins}",
                                    fontWeight = FontWeight.Black,
                                    fontSize = 20.sp,
                                    color = Color(0xFF2E7D32)
                                )
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Losses", fontSize = 11.sp, color = Color(0xFFC62828))
                                Text(
                                    "${user.losses}",
                                    fontWeight = FontWeight.Black,
                                    fontSize = 20.sp,
                                    color = Color(0xFFC62828)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        Divider(color = Color.LightGray.copy(alpha = 0.3f))
                        Spacer(modifier = Modifier.height(12.dp))

                        // Win Rate calculation helper
                        val rate = if (user.totalGames > 0) ((user.wins.toFloat() / user.totalGames) * 100).toInt() else 0
                        Text(
                            text = "Win Rate: $rate%",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFC75D27)
                        )
                    }
                }

                // Authorized Role Indicator
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF8EE)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clickable(enabled = !isRefreshingProfile) {
                            isRefreshingProfile = true
                            sharedGameViewModel.authRepository.refreshUserProfile {
                                isRefreshingProfile = false
                            }
                        },
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        if (isRefreshingProfile) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = Color(0xFFC75D27)
                            )
                        } else {
                            Icon(
                                imageVector = if (user.role == "admin") Icons.Default.Info else Icons.Default.Check,
                                contentDescription = null,
                                tint = if (user.role == "admin") Color(0xFF2E7D32) else Color(0xFF1976D2),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isRefreshingProfile) "Syncing with Server..." else "Assigned Directory Role: ${user.role.uppercase()} (Tap to Sync)",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (user.role == "admin") Color(0xFF2E7D32) else Color(0xFF1976D2)
                        )
                        if (!isRefreshingProfile) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Sync Icon",
                                tint = Color.Gray,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }

                if (user.role == "admin") {
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = onNavigateToAdmin,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("admin_portal_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC75D27)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AdminPanelSettings,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Open Admin Command Center", fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                Button(
                    onClick = {
                        sharedGameViewModel.authRepository.logout()
                        localSuccessMsg = "Log out successful. Return to guest mode."
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("auth_logout_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC62828)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Log Out Profile Session", fontWeight = FontWeight.Bold, color = Color.White)
                }

                Spacer(modifier = Modifier.height(12.dp))

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
                        .fillMaxWidth()
                        .height(44.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFC62828)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Delete Profile & Wipe Data (DPDP Act)", fontSize = 12.sp, fontWeight = FontWeight.Bold)
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
