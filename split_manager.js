const fs = require('fs');
const path = require('path');

const inputPath = 'app/src/main/java/com/example/daadi/data/supabase/SupabaseManager.kt';
const content = fs.readFileSync(inputPath, 'utf-8');

console.log('File size:', content.length);
