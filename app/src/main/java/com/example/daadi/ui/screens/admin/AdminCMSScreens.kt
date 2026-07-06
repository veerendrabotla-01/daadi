package com.example.daadi.ui.screens.admin



import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.text.TextStyle
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun AdminCMSCenter(adminViewModel: com.example.daadi.viewmodel.AdminViewModel, onBack: () -> Unit) {
    val cmsContent by adminViewModel.remoteConfigRepository.cmsContent.collectAsStateWithLifecycle()
    val isSyncing by adminViewModel.analyticsRepository.isSyncing.collectAsStateWithLifecycle()
    var selectedItem by remember { mutableStateOf<com.example.daadi.data.supabase.SupabaseCMSContent?>(null) }

    AdminFoundationScaffold(
        title = "Content Hub",
        adminViewModel = adminViewModel,
        onBack = if (selectedItem == null) onBack else { { selectedItem = null } },
        actions = {
            if (selectedItem == null) {
                IconButton(onClick = { /* New Post */ }) {
                    Icon(Icons.Default.PostAdd, contentDescription = "Create Content", tint = AdminDesign.Primary)
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            if (selectedItem == null) {
                if (isSyncing && cmsContent.isEmpty()) {
                    LazyColumn(modifier = Modifier.fillMaxSize().padding(AdminDesign.SpacingMedium)) {
                        items(8) { ShimmerItem(Modifier.padding(vertical = AdminDesign.SpacingSmall)) }
                    }
                } else if (cmsContent.isEmpty()) {
                    AdminEmptyState(
                        title = "No Content Found", 
                        description = "The CMS repository is currently empty. Initialize baseline assets to populate user-facing terminals."
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(AdminDesign.SpacingMedium),
                        verticalArrangement = Arrangement.spacedBy(AdminDesign.SpacingSmall)
                    ) {
                        item {
                            Text("PUBLISHED ASSETS", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Black, color = AdminDesign.OnSurfaceVariant)
                            Spacer(modifier = Modifier.height(AdminDesign.SpacingSmall))
                        }
                        items(cmsContent) { content ->
                            CMSContentCard(content, onClick = { selectedItem = content })
                        }
                    }
                }
            } else {
                CMSEditor(
                    content = selectedItem!!,
                    onBack = { selectedItem = null },
                    onSave = { updated -> 
                        adminViewModel.remoteConfigRepository.saveCMSContent(updated)
                        selectedItem = null
                    }
                )
            }
        }
    }
}

@Composable
fun CMSContentCard(content: com.example.daadi.data.supabase.SupabaseCMSContent, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        shape = AdminDesign.CardShape,
        elevation = CardDefaults.cardElevation(defaultElevation = AdminDesign.CardElevation),
        colors = CardDefaults.cardColors(containerColor = AdminDesign.Surface)
    ) {
        Row(modifier = Modifier.padding(AdminDesign.SpacingMedium), verticalAlignment = Alignment.CenterVertically) {
            Surface(
                modifier = Modifier.size(40.dp),
                shape = RoundedCornerShape(8.dp),
                color = AdminDesign.Primary.copy(alpha = 0.1f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = when (content.type) {
                            "patch_notes" -> Icons.Default.Description
                            "faq" -> Icons.Default.QuestionAnswer
                            "tutorial" -> Icons.Default.PlayCircle
                            else -> Icons.Default.Article
                        },
                        contentDescription = null,
                        tint = AdminDesign.Primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.width(AdminDesign.SpacingMedium))
            Column(modifier = Modifier.weight(1f)) {
                Text(content.title, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp, color = AdminDesign.OnSurface)
                Text(
                    text = "${content.type.uppercase()} » ${content.status.uppercase()}", 
                    fontSize = 10.sp, 
                    color = AdminDesign.OnSurfaceVariant,
                    fontWeight = FontWeight.Black
                )
            }
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = AdminDesign.OnSurfaceVariant.copy(alpha = 0.3f))
        }
    }
}

@Composable
fun CMSEditor(
    content: com.example.daadi.data.supabase.SupabaseCMSContent,
    onBack: () -> Unit,
    onSave: (com.example.daadi.data.supabase.SupabaseCMSContent) -> Unit
) {
    var title by remember { mutableStateOf(content.title) }
    var body by remember { mutableStateOf(content.body) }
    var status by remember { mutableStateOf(content.status) }

    Column(modifier = Modifier.fillMaxSize().padding(AdminDesign.SpacingMedium)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("CONTENT ORCHESTRATOR", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Black, color = AdminDesign.OnSurfaceVariant, modifier = Modifier.weight(1f))
            Button(
                onClick = { onSave(content.copy(title = title, body = body, status = status)) },
                shape = AdminDesign.ButtonShape,
                colors = ButtonDefaults.buttonColors(containerColor = AdminDesign.Primary)
            ) {
                Icon(Icons.Default.Publish, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(AdminDesign.SpacingSmall))
                Text("PUBLISH CHANGES", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
        Spacer(modifier = Modifier.height(AdminDesign.SpacingMedium))
        OutlinedTextField(
            value = title, 
            onValueChange = { title = it }, 
            label = { Text("ASSET TITLE") }, 
            modifier = Modifier.fillMaxWidth(),
            shape = AdminDesign.CardShape,
            textStyle = TextStyle(fontWeight = FontWeight.Bold)
        )
        Spacer(modifier = Modifier.height(AdminDesign.SpacingSmall))
        OutlinedTextField(
            value = body,
            onValueChange = { body = it },
            label = { Text("RAW CONTENT (MARKDOWN)") },
            modifier = Modifier.fillMaxWidth().weight(1f),
            shape = AdminDesign.CardShape,
            minLines = 10
        )
        Spacer(modifier = Modifier.height(AdminDesign.SpacingMedium))
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = AdminDesign.Surface,
            shape = AdminDesign.CardShape,
            border = BorderStroke(1.dp, AdminDesign.OnSurface.copy(alpha = 0.1f))
        ) {
            Row(modifier = Modifier.padding(AdminDesign.SpacingSmall), verticalAlignment = Alignment.CenterVertically) {
                Text("DEPLOYMENT STATUS:", fontSize = 10.sp, fontWeight = FontWeight.Black, color = AdminDesign.OnSurfaceVariant)
                Spacer(modifier = Modifier.width(AdminDesign.SpacingMedium))
                FilterChip(
                    selected = status == "draft", 
                    onClick = { status = "draft" }, 
                    label = { Text("DRAFT", fontSize = 10.sp) },
                    shape = CircleShape
                )
                Spacer(modifier = Modifier.width(AdminDesign.SpacingSmall))
                FilterChip(
                    selected = status == "published", 
                    onClick = { status = "published" }, 
                    label = { Text("PUBLISHED", fontSize = 10.sp) },
                    shape = CircleShape
                )
            }
        }
    }
}
