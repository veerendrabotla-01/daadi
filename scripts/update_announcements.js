const fs = require('fs');

const path = 'app/src/main/java/com/example/daadi/ui/screens/admin/AdminConfigScreens.kt';
let content = fs.readFileSync(path, 'utf8');

const regex = /@Composable\s+fun AdminAnnouncementsScreen[\s\S]*?dismissButton = \{[^}]+\}\s*\)\s*\}/;

const replacement = `
@Composable
fun AdminAnnouncementsScreen(
    adminViewModel: com.example.daadi.viewmodel.AdminViewModel,
    onBack: () -> Unit
) {
    val announcements by adminViewModel.remoteConfigRepository.announcements.collectAsStateWithLifecycle()
    val isSyncing by adminViewModel.analyticsRepository.isSyncing.collectAsStateWithLifecycle()
    var showCreateDialog by remember { mutableStateOf(false) }

    AdminFoundationScaffold(
        title = "System Bulletins",
        adminViewModel = adminViewModel,
        onBack = onBack,
        actions = {
            IconButton(onClick = { showCreateDialog = true }) {
                Icon(Icons.Default.PostAdd, contentDescription = "Add Bulletin", tint = AdminDesign.Primary)
            }
        }
    ) { padding ->
        if (isSyncing && announcements.isEmpty()) {
            LazyColumn(modifier = Modifier.padding(padding).fillMaxSize().padding(AdminDesign.SpacingMedium)) {
                items(5) { ShimmerItem(Modifier.padding(vertical = AdminDesign.SpacingSmall)) }
            }
        } else if (announcements.isEmpty()) {
            AdminEmptyState(
                title = "No Active Bulletins", 
                description = "Communication channels are clear. Create a bulletin to notify all users of updates or events."
            )
        } else {
            LazyColumn(
                contentPadding = PaddingValues(AdminDesign.SpacingMedium),
                verticalArrangement = Arrangement.spacedBy(AdminDesign.SpacingSmall),
                modifier = Modifier.fillMaxSize().padding(padding)
            ) {
                items(announcements) { ann ->
                    AnnouncementCard(ann, adminViewModel)
                }
            }
        }
    }

    if (showCreateDialog) {
        AnnouncementCreateDialog(
            onDismiss = { showCreateDialog = false },
            onConfirm = { ann ->
                adminViewModel.remoteConfigRepository.createAnnouncementFull(ann)
                showCreateDialog = false
            }
        )
    }
}

@Composable
fun AnnouncementCard(ann: SupabaseAnnouncement, adminViewModel: com.example.daadi.viewmodel.AdminViewModel) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = AdminDesign.CardShape,
        elevation = CardDefaults.cardElevation(defaultElevation = AdminDesign.CardElevation),
        colors = CardDefaults.cardColors(containerColor = AdminDesign.Surface)
    ) {
        Column(modifier = Modifier.padding(AdminDesign.SpacingMedium)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    modifier = Modifier.size(32.dp),
                    shape = CircleShape,
                    color = if (ann.isActive) AdminDesign.Primary.copy(alpha = 0.1f) else AdminDesign.OnSurfaceVariant.copy(alpha = 0.1f)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (ann.isActive) Icons.Default.Campaign else Icons.Default.SpeakerNotesOff,
                            contentDescription = null,
                            tint = if (ann.isActive) AdminDesign.Primary else AdminDesign.OnSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(AdminDesign.SpacingSmall))
                Column(modifier = Modifier.weight(1f)) {
                    Text(ann.title, fontWeight = FontWeight.ExtraBold, color = AdminDesign.OnSurface)
                    Row {
                        Badge(containerColor = AdminDesign.Primary) { Text(ann.priority.uppercase(), fontSize = 8.sp, color = Color.White) }
                        Spacer(Modifier.width(4.dp))
                        if (!ann.isGlobal && ann.region != null) {
                            Badge(containerColor = AdminDesign.Secondary) { Text("REGION: \${ann.region?.uppercase()}", fontSize = 8.sp, color = Color.White) }
                            Spacer(Modifier.width(4.dp))
                        }
                        if (ann.userSegment != null) {
                            Badge(containerColor = AdminDesign.Tertiary) { Text("SEGMENT: \${ann.userSegment?.uppercase()}", fontSize = 8.sp, color = Color.White) }
                        }
                    }
                }
                Switch(
                    checked = ann.isActive,
                    onCheckedChange = { adminViewModel.remoteConfigRepository.toggleAnnouncementStatus(ann.id) },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = AdminDesign.Primary
                    )
                )
            }
            Spacer(modifier = Modifier.height(AdminDesign.SpacingSmall))
            Text(ann.content, fontSize = 12.sp, color = AdminDesign.OnSurfaceVariant)
            if (ann.imageUrl != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text("Attached Image: \${ann.imageUrl}", fontSize = 10.sp, color = AdminDesign.Primary)
            }
            if (ann.deepLink != null) {
                Spacer(modifier = Modifier.height(2.dp))
                Text("Deep Link: \${ann.deepLink}", fontSize = 10.sp, color = AdminDesign.Secondary)
            }
            Spacer(modifier = Modifier.height(AdminDesign.SpacingMedium))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text("Created: \${ann.createdAt}", fontSize = 9.sp, color = AdminDesign.OnSurfaceVariant, fontWeight = FontWeight.Bold)
                    if (ann.scheduledAt != null) {
                        Text("Scheduled: \${ann.scheduledAt}", fontSize = 9.sp, color = AdminDesign.OnSurfaceVariant, fontWeight = FontWeight.Bold)
                    }
                    if (ann.expiryAt != null) {
                        Text("Expires: \${ann.expiryAt}", fontSize = 9.sp, color = AdminDesign.Error, fontWeight = FontWeight.Bold)
                    }
                }
                IconButton(onClick = { adminViewModel.remoteConfigRepository.deleteAnnouncement(ann.id) }) {
                    Icon(Icons.Default.DeleteForever, contentDescription = "Delete", tint = AdminDesign.Error, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

@Composable
fun AnnouncementCreateDialog(onDismiss: () -> Unit, onConfirm: (SupabaseAnnouncement) -> Unit) {
    var title by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }
    var priority by remember { mutableStateOf("low") }
    var isGlobal by remember { mutableStateOf(true) }
    var region by remember { mutableStateOf("") }
    var userSegment by remember { mutableStateOf("") }
    var imageUrl by remember { mutableStateOf("") }
    var deepLink by remember { mutableStateOf("") }
    var scheduledAt by remember { mutableStateOf("") }
    var expiryAt by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Compose Global Bulletin", fontWeight = FontWeight.Black) },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(AdminDesign.SpacingSmall)) {
                item {
                    OutlinedTextField(
                        value = title, 
                        onValueChange = { title = it }, 
                        label = { Text("Bulletin Subject") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = AdminDesign.InputShape
                    )
                }
                item {
                    OutlinedTextField(
                        value = content, 
                        onValueChange = { content = it }, 
                        label = { Text("Broadcast Message") }, 
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3,
                        shape = AdminDesign.InputShape
                    )
                }
                item {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = isGlobal, onCheckedChange = { isGlobal = it })
                        Text("Global Broadcast")
                    }
                }
                if (!isGlobal) {
                    item {
                        OutlinedTextField(
                            value = region, 
                            onValueChange = { region = it }, 
                            label = { Text("Region Code (e.g. NA, EU)") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = AdminDesign.InputShape
                        )
                    }
                    item {
                        OutlinedTextField(
                            value = userSegment, 
                            onValueChange = { userSegment = it }, 
                            label = { Text("User Segment (e.g. VIP, NEW)") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = AdminDesign.InputShape
                        )
                    }
                }
                item {
                    OutlinedTextField(
                        value = priority, 
                        onValueChange = { priority = it }, 
                        label = { Text("Priority (low, medium, high)") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = AdminDesign.InputShape
                    )
                }
                item {
                    OutlinedTextField(
                        value = imageUrl, 
                        onValueChange = { imageUrl = it }, 
                        label = { Text("Attached Image URL (Optional)") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = AdminDesign.InputShape
                    )
                }
                item {
                    OutlinedTextField(
                        value = deepLink, 
                        onValueChange = { deepLink = it }, 
                        label = { Text("Deep Link (Optional)") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = AdminDesign.InputShape
                    )
                }
                item {
                    OutlinedTextField(
                        value = scheduledAt, 
                        onValueChange = { scheduledAt = it }, 
                        label = { Text("Scheduled Time (YYYY-MM-DD HH:MM) (Optional)") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = AdminDesign.InputShape
                    )
                }
                item {
                    OutlinedTextField(
                        value = expiryAt, 
                        onValueChange = { expiryAt = it }, 
                        label = { Text("Expiry Time (YYYY-MM-DD HH:MM) (Optional)") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = AdminDesign.InputShape
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val ann = SupabaseAnnouncement(
                        id = 0,
                        title = title,
                        content = content,
                        isActive = true,
                        createdAt = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date()),
                        priority = priority,
                        isGlobal = isGlobal,
                        region = if (region.isNotBlank() && !isGlobal) region else null,
                        userSegment = if (userSegment.isNotBlank() && !isGlobal) userSegment else null,
                        imageUrl = if (imageUrl.isNotBlank()) imageUrl else null,
                        deepLink = if (deepLink.isNotBlank()) deepLink else null,
                        scheduledAt = if (scheduledAt.isNotBlank()) scheduledAt else null,
                        expiryAt = if (expiryAt.isNotBlank()) expiryAt else null
                    )
                    onConfirm(ann) 
                },
                shape = AdminDesign.ButtonShape
            ) {
                Icon(Icons.Default.Send, contentDescription = null, Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("DISPATCH BULLETIN")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("CANCEL") }
        }
    )
}
`;

if (regex.test(content)) {
    content = content.replace(regex, replacement.trim());
    fs.writeFileSync(path, content, 'utf8');
    console.log('Replaced AdminAnnouncementsScreen and related UI.');
} else {
    console.log('Could not find AdminAnnouncementsScreen to replace.');
}
