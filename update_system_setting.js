const fs = require('fs');

const path = 'app/src/main/java/com/example/daadi/data/repository/supabase/SupabaseRepositories.kt';
let content = fs.readFileSync(path, 'utf8');

const regex = /fun addSystemSetting\(key: String, value: String, description: String\)/;
const replacement = 'fun addSystemSetting(key: String, value: String, description: String, type: String = "variable")';

if (regex.test(content)) {
    content = content.replace(regex, replacement);
    content = content.replace(/val update = mapOf\("key" to key, "value" to value, "description" to description\)/, 'val update = mapOf("key" to key, "value" to value, "description" to description, "type" to type)');
    fs.writeFileSync(path, content, 'utf8');
    console.log('Updated addSystemSetting in repository.');
}
