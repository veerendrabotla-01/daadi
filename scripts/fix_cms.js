const fs = require('fs');
const path = 'app/src/main/java/com/example/daadi/ui/screens/admin/AdminCMSScreens.kt';
let content = fs.readFileSync(path, 'utf8');

const regex = /LazyColumn\(\s*modifier = Modifier\.fillMaxSize\(\),\s*contentPadding = PaddingValues\(AdminDesign\.SpacingMedium\),\s*verticalArrangement = Arrangement\.spacedBy\(AdminDesign\.SpacingSmall\)\s*\) \{\s*item \{\s*Text\("PUBLISHED ASSETS", style = MaterialTheme\.typography\.labelSmall, fontWeight = FontWeight\.Black, color = AdminDesign\.OnSurfaceVariant\)\s*Spacer\(modifier = Modifier\.height\(AdminDesign\.SpacingSmall\)\)\s*OutlinedTextField\([\s\S]*?\)\s*Spacer\(modifier = Modifier\.height\(AdminDesign\.SpacingSmall\)\)\s*\}\s*val filteredCms = remember\(cmsContent, searchQuery\) \{[\s\S]*?\}\s*items\(filteredCms\) \{ content ->/;

const replacement = `
                        val filteredCms = remember(cmsContent, searchQuery) {
                            if (searchQuery.isBlank()) cmsContent
                            else cmsContent.filter { 
                                it.title.contains(searchQuery, ignoreCase = true) || 
                                it.type.contains(searchQuery, ignoreCase = true) ||
                                (it.author?.contains(searchQuery, ignoreCase = true) == true)
                            }
                        }
                        LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(AdminDesign.SpacingMedium),
                        verticalArrangement = Arrangement.spacedBy(AdminDesign.SpacingSmall)
                    ) {
                        item {
                            Text("PUBLISHED ASSETS", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Black, color = AdminDesign.OnSurfaceVariant)
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
                            Spacer(modifier = Modifier.height(AdminDesign.SpacingSmall))
                        }
                        items(filteredCms) { content ->`;

if (regex.test(content)) {
    content = content.replace(regex, replacement);
    fs.writeFileSync(path, content, 'utf8');
    console.log("Fixed CMS search context.");
} else {
    console.log("Regex didn't match.");
}
