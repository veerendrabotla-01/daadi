const fs = require('fs');

const path = 'app/src/main/java/com/example/daadi/ui/screens/admin/AdminCMSScreens.kt';
let content = fs.readFileSync(path, 'utf8');

const replacement = `
@Composable
fun CMSEditor(
    content: com.example.daadi.data.supabase.SupabaseCMSContent,
    onBack: () -> Unit,
    onSave: (com.example.daadi.data.supabase.SupabaseCMSContent) -> Unit
) {
    var title by remember { mutableStateOf(content.title) }
    var body by remember { mutableStateOf(content.body) }
    var status by remember { mutableStateOf(content.status) }
    var type by remember { mutableStateOf(content.type) }
    var category by remember { mutableStateOf(content.category ?: "") }
    var version by remember { mutableStateOf(content.version ?: "") }
    var author by remember { mutableStateOf(content.author ?: "") }
    var imageUrl by remember { mutableStateOf(content.imageUrl ?: "") }
    var videoUrl by remember { mutableStateOf(content.videoUrl ?: "") }
    var scheduledAt by remember { mutableStateOf(content.scheduledAt ?: "") }
    var expiryAt by remember { mutableStateOf(content.expiryAt ?: "") }

    var previewMode by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize().padding(AdminDesign.SpacingMedium)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Back") }
            Spacer(Modifier.width(8.dp))
            Text("CONTENT ORCHESTRATOR", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Black, color = AdminDesign.OnSurfaceVariant, modifier = Modifier.weight(1f))
            Button(
                onClick = { previewMode = !previewMode },
                shape = AdminDesign.ButtonShape,
                colors = ButtonDefaults.buttonColors(containerColor = AdminDesign.Secondary)
            ) {
                Text(if (previewMode) "EDIT" else "PREVIEW", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.width(AdminDesign.SpacingSmall))
            Button(
                onClick = { 
                    onSave(content.copy(
                        title = title, 
                        body = body, 
                        status = status,
                        type = type,
                        category = category.ifBlank { null },
                        version = version.ifBlank { null },
                        author = author.ifBlank { null },
                        imageUrl = imageUrl.ifBlank { null },
                        videoUrl = videoUrl.ifBlank { null },
                        scheduledAt = scheduledAt.ifBlank { null },
                        expiryAt = expiryAt.ifBlank { null }
                    )) 
                },
                shape = AdminDesign.ButtonShape,
                colors = ButtonDefaults.buttonColors(containerColor = AdminDesign.Primary)
            ) {
                Icon(Icons.Default.Publish, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(AdminDesign.SpacingSmall))
                Text("PUBLISH CHANGES", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
        Spacer(modifier = Modifier.height(AdminDesign.SpacingMedium))

        if (previewMode) {
            Card(modifier = Modifier.fillMaxWidth().weight(1f), shape = AdminDesign.CardShape, colors = CardDefaults.cardColors(containerColor = AdminDesign.Surface)) {
                Column(modifier = Modifier.padding(AdminDesign.SpacingMedium).verticalScroll(androidx.compose.foundation.rememberScrollState())) {
                    Text(title, fontWeight = FontWeight.Black, fontSize = 24.sp, color = AdminDesign.OnSurface)
                    Spacer(Modifier.height(8.dp))
                    Text(body, fontSize = 14.sp, color = AdminDesign.OnSurfaceVariant) // Basic preview (markdown rendering would go here)
                    if (imageUrl.isNotBlank()) {
                        Spacer(Modifier.height(8.dp))
                        Text("[Image: \${imageUrl}]", color = AdminDesign.Primary, fontSize = 12.sp)
                    }
                    if (videoUrl.isNotBlank()) {
                        Spacer(Modifier.height(8.dp))
                        Text("[Link/Video: \${videoUrl}]", color = AdminDesign.Secondary, fontSize = 12.sp)
                    }
                }
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxWidth().weight(1f)) {
                item {
                    OutlinedTextField(
                        value = title, 
                        onValueChange = { title = it }, 
                        label = { Text("ASSET TITLE") }, 
                        modifier = Modifier.fillMaxWidth(),
                        shape = AdminDesign.CardShape,
                        textStyle = TextStyle(fontWeight = FontWeight.Bold)
                    )
                    Spacer(modifier = Modifier.height(AdminDesign.SpacingSmall))
                }
                item {
                    OutlinedTextField(
                        value = body,
                        onValueChange = { body = it },
                        label = { Text("RAW CONTENT (MARKDOWN)") },
                        modifier = Modifier.fillMaxWidth().height(200.dp),
                        shape = AdminDesign.CardShape
                    )
                    Spacer(modifier = Modifier.height(AdminDesign.SpacingSmall))
                }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(AdminDesign.SpacingSmall)) {
                        OutlinedTextField(value = type, onValueChange = { type = it }, label = { Text("Type (patch_notes, faq, etc)") }, modifier = Modifier.weight(1f))
                        OutlinedTextField(value = category, onValueChange = { category = it }, label = { Text("Category") }, modifier = Modifier.weight(1f))
                    }
                    Spacer(modifier = Modifier.height(AdminDesign.SpacingSmall))
                }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(AdminDesign.SpacingSmall)) {
                        OutlinedTextField(value = version, onValueChange = { version = it }, label = { Text("App Version (e.g. 1.2.0)") }, modifier = Modifier.weight(1f))
                        OutlinedTextField(value = author, onValueChange = { author = it }, label = { Text("Author") }, modifier = Modifier.weight(1f))
                    }
                    Spacer(modifier = Modifier.height(AdminDesign.SpacingSmall))
                }
                item {
                    OutlinedTextField(value = imageUrl, onValueChange = { imageUrl = it }, label = { Text("Image URL / Screenshot") }, modifier = Modifier.fillMaxWidth())
                    Spacer(modifier = Modifier.height(AdminDesign.SpacingSmall))
                    OutlinedTextField(value = videoUrl, onValueChange = { videoUrl = it }, label = { Text("Video / Link URL") }, modifier = Modifier.fillMaxWidth())
                    Spacer(modifier = Modifier.height(AdminDesign.SpacingSmall))
                }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(AdminDesign.SpacingSmall)) {
                        OutlinedTextField(value = scheduledAt, onValueChange = { scheduledAt = it }, label = { Text("Scheduled At (YYYY-MM-DD HH:MM)") }, modifier = Modifier.weight(1f))
                        OutlinedTextField(value = expiryAt, onValueChange = { expiryAt = it }, label = { Text("Expiry At (YYYY-MM-DD HH:MM)") }, modifier = Modifier.weight(1f))
                    }
                    Spacer(modifier = Modifier.height(AdminDesign.SpacingSmall))
                }
            }
        }

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
                Spacer(modifier = Modifier.width(AdminDesign.SpacingSmall))
                FilterChip(
                    selected = status == "archived", 
                    onClick = { status = "archived" }, 
                    label = { Text("ARCHIVED", fontSize = 10.sp) },
                    shape = CircleShape
                )
            }
        }
    }
}
`;

content = content.replace(/@Composable\nfun CMSEditor\([\s\S]*?\}\n\}\n\}/, replacement.trim());
fs.writeFileSync(path, content, 'utf8');
console.log('Updated CMSEditor.');

