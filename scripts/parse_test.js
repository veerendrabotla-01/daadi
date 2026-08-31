const fs = require('fs');
const content = fs.readFileSync('app/src/main/java/com/example/daadi/data/supabase/SupabaseManager.kt', 'utf-8');

const regex = /((?:private |suspend |public )?fun [^{]+)\{/g;
let match;
let count = 0;
while ((match = regex.exec(content)) !== null) {
    count++;
}
console.log('Functions found:', count);
