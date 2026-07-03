package com.example.daadi.ui.screens

import com.example.daadi.data.supabase.SupabaseManager


import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Message
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserFeedbackScreen(
    supabaseManager: SupabaseManager,
    onBack: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var content by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("suggest") }
    var isSubmitting by remember { mutableStateOf(false) }
    var showSuccess by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("App Feedback", fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = null) } },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFFFDF3E3),
                    titleContentColor = Color(0xFF5C2D0A),
                    navigationIconContentColor = Color(0xFF5C2D0A)
                )
            )
        },
        containerColor = Color(0xFFFDF3E3)
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "Your feedback helps us build a better Daadi experience! Found a bug or have a suggestion?",
                fontSize = 14.sp,
                color = Color(0xFF8B5E3C),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Category Selection
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FeedbackTypeChip(
                    label = "Suggestion",
                    isSelected = category == "suggest",
                    icon = Icons.Default.Lightbulb,
                    modifier = Modifier.weight(1f),
                    onClick = { category = "suggest" }
                )
                FeedbackTypeChip(
                    label = "Bug",
                    isSelected = category == "bug",
                    icon = Icons.Default.BugReport,
                    modifier = Modifier.weight(1f),
                    onClick = { category = "bug" }
                )
                FeedbackTypeChip(
                    label = "Other",
                    isSelected = category == "other",
                    icon = Icons.Default.Message,
                    modifier = Modifier.weight(1f),
                    onClick = { category = "other" }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            OutlinedTextField(
                value = content,
                onValueChange = { content = it },
                label = { Text("Your Message") },
                placeholder = { Text("Describe your idea or the issue you found...") },
                modifier = Modifier.fillMaxWidth().height(200.dp),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color(0xFF5C2D0A),
                    unfocusedTextColor = Color(0xFF5C2D0A),
                    focusedBorderColor = Color(0xFF5C2D0A),
                    focusedLabelColor = Color(0xFF5C2D0A),
                    unfocusedLabelColor = Color(0xFF8B5E3C),
                    cursorColor = Color(0xFF5C2D0A),
                    focusedPlaceholderColor = Color(0xFF8B5E3C),
                    unfocusedPlaceholderColor = Color(0xFF8B5E3C).copy(alpha = 0.6f)
                )
            )

            Spacer(modifier = Modifier.height(32.dp))

            if (showSuccess) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        "Feedback submitted! Thank you for your support.",
                        color = Color(0xFF2E7D32),
                        modifier = Modifier.padding(16.dp),
                        fontWeight = FontWeight.Bold,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            Button(
                onClick = {
                    if (content.isNotBlank()) {
                        isSubmitting = true
                        supabaseManager.submitFeedback(content, category) { success, errMsg ->
                            isSubmitting = false
                            if (success) {
                                content = ""
                                showSuccess = true
                                android.widget.Toast.makeText(context, "Feedback submitted successfully!", android.widget.Toast.LENGTH_SHORT).show()
                            } else {
                                val displayError = if (!errMsg.isNullOrBlank()) "Error: $errMsg" else "Failed to submit feedback. Please try again."
                                android.widget.Toast.makeText(context, displayError, android.widget.Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().height(54.dp),
                enabled = content.isNotBlank() && !isSubmitting,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF5C2D0A),
                    contentColor = Color.White
                )
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                } else {
                    Text("Submit Feedback", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun FeedbackTypeChip(
    label: String,
    isSelected: Boolean,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) Color(0xFF5C2D0A) else Color.White
        ),
        shape = RoundedCornerShape(12.dp),
        modifier = modifier.height(80.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(icon, contentDescription = null, tint = if (isSelected) Color.White else Color(0xFF5C2D0A))
            Text(label, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = if (isSelected) Color.White else Color(0xFF5C2D0A))
        }
    }
}
