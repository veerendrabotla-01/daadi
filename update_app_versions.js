const fs = require('fs');

const path = 'app/src/main/java/com/example/daadi/data/repository/supabase/SupabaseRepositories.kt';
let content = fs.readFileSync(path, 'utf8');

const regex = /fun addAppVersion\(versionCode: Int, versionName: String, isMandatory: Boolean, minSupportedVersion: Int, releaseNotes: String\)/;
const replacement = 'fun addAppVersion(versionCode: Int, versionName: String, isMandatory: Boolean, minSupportedVersion: Int, releaseNotes: String, stagedRolloutPercentage: Int = 100)';

if (regex.test(content)) {
    content = content.replace(regex, replacement);
    content = content.replace(/minSupportedVersion = minSupportedVersion,\n\s*releaseNotes = releaseNotes,\n\s*createdAt/g, "minSupportedVersion = minSupportedVersion,\n            releaseNotes = releaseNotes,\n            stagedRolloutPercentage = stagedRolloutPercentage,\n            createdAt");
    fs.writeFileSync(path, content, 'utf8');
    console.log('Updated addAppVersion in repository.');
}

const pathUI = 'app/src/main/java/com/example/daadi/ui/screens/admin/AdminConfigScreens.kt';
let uiContent = fs.readFileSync(pathUI, 'utf8');

if (!uiContent.includes('stagedRollout = it')) {
    uiContent = uiContent.replace(/var notes by remember \{ mutableStateOf\(""\) \}/, 'var notes by remember { mutableStateOf("") }\n        var stagedRollout by remember { mutableStateOf("100") }');
    uiContent = uiContent.replace(/OutlinedTextField\(value = notes, onValueChange = \{ notes = it \}, label = \{ Text\("Release Notes"\) \}, modifier = Modifier\.fillMaxWidth\(\)\)/, 'OutlinedTextField(value = notes, onValueChange = { notes = it }, label = { Text("Release Notes") }, modifier = Modifier.fillMaxWidth())\n                    OutlinedTextField(value = stagedRollout, onValueChange = { stagedRollout = it }, label = { Text("Staged Rollout Percentage (0-100)") }, modifier = Modifier.fillMaxWidth())');
    uiContent = uiContent.replace(/adminViewModel\.remoteConfigRepository\.addAppVersion\(code, vName, isMandatory, min, notes\)/, 'adminViewModel.remoteConfigRepository.addAppVersion(code, vName, isMandatory, min, notes, stagedRollout.toIntOrNull() ?: 100)');
    uiContent = uiContent.replace(/Text\("Min Supported: Build \$\{ver\.minSupportedVersion\}", fontSize = 11\.sp, color = AdminDesign\.OnSurfaceVariant\)/, 'Text("Min Supported: Build ${ver.minSupportedVersion} | Rollout: ${ver.stagedRolloutPercentage}%", fontSize = 11.sp, color = AdminDesign.OnSurfaceVariant)');
    fs.writeFileSync(pathUI, uiContent, 'utf8');
    console.log('Updated AdminConfigScreens for Staged Rollout.');
}

