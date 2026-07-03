const fs = require('fs');

const inputFile = 'app/src/main/java/com/example/daadi/data/supabase/SupabaseManager.kt';
const content = fs.readFileSync(inputFile, 'utf-8');

// The class starts at: class SupabaseManager(private val context: Context) {
const classStart = content.indexOf('class SupabaseManager(private val context: Context) {');
const beforeClass = content.substring(0, classStart);
const classBody = content.substring(classStart + 'class SupabaseManager(private val context: Context) {'.length, content.lastIndexOf('}'));

// Parse class body into chunks
const chunks = [];
let i = 0;
while (i < classBody.length) {
    // Skip whitespace
    if (/\s/.test(classBody[i])) {
        i++;
        continue;
    }
    
    let start = i;
    
    // Find if it's a property, function, or init
    // A chunk ends when we hit a newline at depth 0, followed by another top-level declaration.
    // We'll track `{` and `}` depth.
    let depth = 0;
    let end = i;
    while (i < classBody.length) {
        if (classBody[i] === '{') depth++;
        if (classBody[i] === '}') {
            depth--;
            if (depth === 0) {
                // If this is the end of a block (like fun or init), the chunk ends after this.
                i++;
                end = i;
                break;
            }
        }
        
        // If depth is 0 and we see a newline, it might be the end of a simple property
        if (depth === 0 && classBody[i] === '\n') {
            // Check if next non-whitespace starts a new declaration
            let j = i + 1;
            while (j < classBody.length && /[ \t]/.test(classBody[j])) j++;
            if (j < classBody.length && classBody[j] === '\n') {
                // Empty line means end of chunk
                i = j;
                end = i;
                break;
            }
            const nextWord = classBody.substring(j, j + 20);
            if (/^(private |val |var |fun |suspend |init )/.test(nextWord)) {
                end = i;
                break;
            }
        }
        
        i++;
    }
    if (i === classBody.length && depth === 0) end = i;
    
    let chunkText = classBody.substring(start, end).trim();
    if (chunkText) {
        chunks.push(chunkText);
    }
}

console.log('Parsed', chunks.length, 'chunks from SupabaseManager.');
fs.writeFileSync('parsed_chunks.json', JSON.stringify(chunks, null, 2));
