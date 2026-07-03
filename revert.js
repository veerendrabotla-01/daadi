const fs = require('fs');

const networkContent = fs.readFileSync('app/src/main/java/com/example/daadi/data/supabase/SupabaseNetworkClient.kt', 'utf-8');
// Network content has beforeClass (imports) and class SupabaseNetworkClient(val context: Context) { ... }
const classStart = networkContent.indexOf('class SupabaseNetworkClient');
const beforeClass = networkContent.substring(0, classStart);

let chunks = JSON.parse(fs.readFileSync('parsed_chunks.json', 'utf-8'));

let out = beforeClass + '\nclass SupabaseManager(private val context: Context) {\n' + chunks.join('\n\n') + '\n}\n';

fs.writeFileSync('app/src/main/java/com/example/daadi/data/supabase/SupabaseManager.kt', out);
console.log('Restored SupabaseManager.kt');
