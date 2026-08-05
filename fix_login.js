const fs = require('fs');

const path = 'app/src/main/java/com/example/daadi/data/supabase/SupabaseManager.kt';
let content = fs.readFileSync(path, 'utf8');

content = content.replace(
    /val metadata = userMap\?\.get\("user_metadata"\) as\? Map<\*, \*>/,
    'val metadata = (userMap?.get("user_metadata") as? Map<*, *>) ?: (userMap?.get("raw_user_meta_data") as? Map<*, *>)'
);

fs.writeFileSync(path, content, 'utf8');
console.log('Fixed login metadata key');
