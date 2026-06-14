package com.example.daadi.ui.screens.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.daadi.data.supabase.SupabaseAnnouncement
import com.example.daadi.data.supabase.SupabaseManager
import com.example.daadi.data.supabase.SupabaseSystemSetting
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminSystemConfigScreen(
    supabaseManager: SupabaseManager,
    onBack: () -> Unit
) {
    val settings by supabaseManager.systemSettings.collectAsStateWithLifecycle()
    var editingItem by remember { mutableStateOf<SupabaseSystemSetting?>(null) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("App Configuration", fontWeight = FontWeight.Bold) },
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
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize().padding(padding)
        ) {
            items(settings) { item ->
                val isToggleable = item.value == "on" || item.value == "off" || item.value == "active" || item.value == "passive"
                Card(
                    onClick = { if (!isToggleable) editingItem = item },
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().border(1.dp, Color.LightGray.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                ) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(item.key.uppercase().replace("_", " "), fontWeight = FontWeight.Bold, color = Color(0xFF5C2D0A), fontSize = 13.sp)
                            Text(item.description, fontSize = 11.sp, color = Color.Gray)
                            if (!isToggleable) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(item.value, fontWeight = FontWeight.Black, fontSize = 16.sp, color = Color(0xFFC75D27))
                            }
                        }
                        
                        if (isToggleable) {
                            val isOn = item.value == "on" || item.value == "active"
                            Switch(
                                checked = isOn,
                                onCheckedChange = { checked ->
                                    val newVal = when(item.key) {
                                        "lobby_operational" -> if (checked) "active" else "passive"
                                        "tournament_registration" -> if (checked) "active" else "passive"
                                        else -> if (checked) "on" else "off"
                                    }
                                    supabaseManager.updateSystemSetting(item.key, newVal)
                                },
                                colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFFC75D27), checkedTrackColor = Color(0xFFFFCCBC))
                            )
                        } else {
                            Icon(Icons.Default.Edit, contentDescription = null, tint = Color.LightGray, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }
    }

    if (editingItem != null) {
        var newVal by remember { mutableStateOf(editingItem!!.value) }
        AlertDialog(
            onDismissRequest = { editingItem = null },
            title = { Text("Modify ${editingItem!!.key}") },
            text = {
                OutlinedTextField(
                    value = newVal,
                    onValueChange = { newVal = it },
                    label = { Text("New Value") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color(0xFF5C2D0A),
                        unfocusedTextColor = Color(0xFF5C2D0A),
                        focusedBorderColor = Color(0xFF5C2D0A),
                        focusedLabelColor = Color(0xFF5C2D0A)
                    )
                )
            },
            confirmButton = {
                Button(onClick = { supabaseManager.updateSystemSetting(editingItem!!.key, newVal); editingItem = null }) {
                    Text("Update")
                }
            },
            dismissButton = {
                TextButton(onClick = { editingItem = null }) { Text("Cancel") }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminAnnouncementsScreen(
    supabaseManager: SupabaseManager,
    onBack: () -> Unit
) {
    val announcements by supabaseManager.announcements.collectAsStateWithLifecycle()
    var showCreateDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Bulletins", fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = null) } },
                actions = {
                    IconButton(onClick = { showCreateDialog = true }) { Icon(Icons.Default.Add, contentDescription = null) }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFFFDF3E3),
                    titleContentColor = Color(0xFF5C2D0A),
                    navigationIconContentColor = Color(0xFF5C2D0A),
                    actionIconContentColor = Color(0xFF5C2D0A)
                )
            )
        },
        containerColor = Color(0xFFFDF3E3)
    ) { padding ->
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize().padding(padding)
        ) {
            items(announcements) { ann ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = if (ann.isActive) Color.White else Color(0xFFF5F5F5)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().border(1.dp, Color.LightGray.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(ann.title, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                            Switch(
                                checked = ann.isActive,
                                onCheckedChange = { supabaseManager.toggleAnnouncementStatus(ann.id) },
                                colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFF5C2D0A), checkedTrackColor = Color(0xFFD4A55A))
                            )
                        }
                        Text(ann.content, fontSize = 12.sp, color = Color.Gray)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text(ann.createdAt, fontSize = 10.sp, color = Color.LightGray)
                            IconButton(onClick = { supabaseManager.deleteAnnouncement(ann.id) }) {
                                Icon(Icons.Default.Delete, contentDescription = null, tint = Color.LightGray, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCreateDialog) {
        var title by remember { mutableStateOf("") }
        var content by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = { Text("New Announcement") },
            text = {
                Column {
                    OutlinedTextField(
                        value = title, 
                        onValueChange = { title = it }, 
                        label = { Text("Title") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color(0xFF5C2D0A),
                            unfocusedTextColor = Color(0xFF5C2D0A),
                            focusedBorderColor = Color(0xFF5C2D0A)
                        )
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = content, 
                        onValueChange = { content = it }, 
                        label = { Text("Content") }, 
                        minLines = 3,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color(0xFF5C2D0A),
                            unfocusedTextColor = Color(0xFF5C2D0A),
                            focusedBorderColor = Color(0xFF5C2D0A)
                        )
                    )
                }
            },
            confirmButton = {
                Button(onClick = { supabaseManager.createAnnouncement(title, content, true); showCreateDialog = false }) {
                    Text("Publish")
                }
            }
        )
    }
}
