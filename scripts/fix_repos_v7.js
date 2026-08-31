const fs = require('fs');
const path = 'app/src/main/java/com/example/daadi/data/repository/supabase/SupabaseRepositories.kt';
let content = fs.readFileSync(path, 'utf-8');

// 1. Fix incorrect network. prefixes
content = content.replace(/\bnetwork\.content\b/g, 'content');
content = content.replace(/\bnetwork\.id\b/g, 'id');
content = content.replace(/\bnetwork\.length\b/g, 'length');
content = content.replace(/\bnetwork\.roles\b/g, 'roles');

// 2. Fix nested network. (e.g. currentUser.value?.network.id)
content = content.replace(/currentUser\.value\?\.network\.id/g, 'currentUser.value?.id');

// 3. Fix askAiAssistant in AnalyticsRepository
if (content.includes('class AnalyticsRepository') && !content.includes('fun askAiAssistant')) {
    content = content.replace(/class AnalyticsRepository\(val network: SupabaseManager\) \{/, 'class AnalyticsRepository(val network: SupabaseManager) {\n    suspend fun askAiAssistant(query: String): String = network.askAiAssistant(query)\n');
}

// 4. Fix updateAdConfigurationRemote parameter type if needed
// (Already correct in ViewModels.kt call, but ensure it matches)

// 5. Fix hasPermission calls that might have been double-prefixed
content = content.replace(/network\.network\.hasPermission/g, 'network.hasPermission');

fs.writeFileSync(path, content);
console.log('Fixed SupabaseRepositories.kt v7');
