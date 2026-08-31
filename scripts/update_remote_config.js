const fs = require('fs');

const path = 'app/src/main/java/com/example/daadi/ui/screens/admin/AdminConfigScreens.kt';
let content = fs.readFileSync(path, 'utf8');

const regex = /val subTabs = listOf\("Variables", "App Versions", "Maintenance"\)/;
const replacement = 'val subTabs = listOf("Variables", "Feature Flags", "Kill Switches", "App Versions", "Maintenance")';

if (regex.test(content)) {
    content = content.replace(regex, replacement);
    content = content.replace(/if \(activeSubTab == 0\) \{/, `if (activeSubTab == 0 || activeSubTab == 1 || activeSubTab == 2) {`);
    
    // adjust logic inside variables tab
    content = content.replace(/val filteredSettings = remember\(settings, selectedCategory\) \{[\s\S]*?\}\n\s+if \(filteredSettings\.isEmpty\(\)\)/, `val filteredSettings = remember(settings, selectedCategory, activeSubTab) {
                            settings.filter { item ->
                                val expectedType = when (activeSubTab) {
                                    1 -> "feature_flag"
                                    2 -> "kill_switch"
                                    else -> "variable"
                                }
                                val actualType = item.type ?: "variable"
                                if (actualType != expectedType) return@filter false
                                
                                when (selectedCategory) {
                                    "SYSTEM" -> item.key.contains("maintenance") || item.key.contains("broadcast") || item.key.contains("setting")
                                    "MULTIPLIERS" -> item.key.contains("multiplier") || item.key.contains("rate")
                                    "FEATURES" -> item.key.contains("enabled") || item.key.contains("active") || item.key.contains("toggle")
                                    "ADS" -> item.key.contains("ads") || item.key.contains("monetization")
                                    "VERSION" -> item.key.contains("version") || item.key.contains("update")
                                    else -> true
                                }
                            }
                        }
                        if (filteredSettings.isEmpty())`);
                        
    content = content.replace(/else if \(activeSubTab == 1\) \{/, 'else if (activeSubTab == 3) {');
    
    // add Add Variable Dialog logic based on activeSubTab
    content = content.replace(/var type by remember \{ mutableStateOf\("variable"\) \}/, ''); // in case we already added it
    
    content = content.replace(/var key by remember \{ mutableStateOf\(""\) \}\n\s+var value by remember \{ mutableStateOf\(""\) \}\n\s+var desc by remember \{ mutableStateOf\(""\) \}/, 
        `var key by remember { mutableStateOf("") }
        var value by remember { mutableStateOf("") }
        var desc by remember { mutableStateOf("") }
        val type = when (activeSubTab) {
            1 -> "feature_flag"
            2 -> "kill_switch"
            else -> "variable"
        }`);
        
    content = content.replace(/adminViewModel\.remoteConfigRepository\.addSystemSetting\(key\.trim\(\), value\.trim\(\), desc\.trim\(\)\)/, 
        'adminViewModel.remoteConfigRepository.addSystemSetting(key.trim(), value.trim(), desc.trim(), type)');
        
    fs.writeFileSync(path, content, 'utf8');
    console.log('Updated Remote Config for Feature Flags and Kill Switches.');
} else {
    console.log('Could not find remote config tabs.');
}

