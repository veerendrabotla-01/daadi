const fs = require('fs');

const path = 'app/src/main/java/com/example/daadi/data/supabase/SupabaseManager.kt';
let content = fs.readFileSync(path, 'utf8');

// Replace onResult(false, e.localizedMessage)
content = content.replace(/onResult\(false, e\.localizedMessage\)/g, 'onResult(false, "An error occurred. Please try again.")');

// Replace onResult(false, "Some context: ${e.localizedMessage}")
content = content.replace(/onResult\(false, "([^"]*): \$\{e\.localizedMessage\}"\)/g, 'onResult(false, "$1. Please try again.")');
content = content.replace(/onResult\(false, "([^"]*)\$\{e\.localizedMessage\}"\)/g, 'onResult(false, "$1. Please try again.")');

// Special cases that didn't match
content = content.replace(/"feedback_v2: \$\{e\.localizedMessage\}"/g, '"feedback_v2: An error occurred."');
content = content.replace(/"feedback_v1: \$\{e\.localizedMessage\}"/g, '"feedback_v1: An error occurred."');

fs.writeFileSync(path, content, 'utf8');
console.log('Fixed error messages');
