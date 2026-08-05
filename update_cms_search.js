const fs = require('fs');
const path = 'app/src/main/java/com/example/daadi/ui/screens/admin/AdminCMSScreens.kt';
let content = fs.readFileSync(path, 'utf8');

const regex = /var selectedItem by remember \{ mutableStateOf<com\.example\.daadi\.data\.supabase\.SupabaseCMSContent\?>\(null\) \}/;
const replacement = `var selectedItem by remember { mutableStateOf<com.example.daadi.data.supabase.SupabaseCMSContent?>(null) }
    var searchQuery by remember { mutableStateOf("") }`;

if (regex.test(content)) {
    content = content.replace(regex, replacement);
    
    // add search bar
    content = content.replace(/Text\("PUBLISHED ASSETS", style = MaterialTheme\.typography\.labelSmall, fontWeight = FontWeight\.Black, color = AdminDesign\.OnSurfaceVariant\)\n\s+Spacer\(modifier = Modifier\.height\(AdminDesign\.SpacingSmall\)\)/, `Text("PUBLISHED ASSETS", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Black, color = AdminDesign.OnSurfaceVariant)
                            Spacer(modifier = Modifier.height(AdminDesign.SpacingSmall))
                            OutlinedTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                placeholder = { Text("Search CMS by title, type, or author...") },
                                modifier = Modifier.fillMaxWidth(),
                                shape = AdminDesign.InputShape,
                                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                                singleLine = true
                            )
                            Spacer(modifier = Modifier.height(AdminDesign.SpacingSmall))`);

    // filter by search query
    content = content.replace(/items\(cmsContent\) \{ content ->/, `val filteredCms = remember(cmsContent, searchQuery) {
                            if (searchQuery.isBlank()) cmsContent
                            else cmsContent.filter { 
                                it.title.contains(searchQuery, ignoreCase = true) || 
                                it.type.contains(searchQuery, ignoreCase = true) ||
                                (it.author?.contains(searchQuery, ignoreCase = true) == true)
                            }
                        }
                        items(filteredCms) { content ->`);

    fs.writeFileSync(path, content, 'utf8');
    console.log('Updated CMS Search.');
} else {
    console.log('Could not find target for CMS Search.');
}
