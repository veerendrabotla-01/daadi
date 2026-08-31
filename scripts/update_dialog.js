const fs = require('fs');

const path = 'app/src/main/java/com/example/daadi/ui/screens/SupabaseAuthScreen.kt';
let content = fs.readFileSync(path, 'utf8');

content = content.replace(
    /colors = TextFieldDefaults\.colors\(focusedContainerColor = Color\.White, unfocusedContainerColor = Color\.White\)/g,
    'colors = OutlinedTextFieldDefaults.colors(focusedContainerColor = Color.White, unfocusedContainerColor = Color.White, focusedTextColor = Color(0xFF5C2D0A), unfocusedTextColor = Color(0xFF5C2D0A), focusedBorderColor = Color(0xFF5C2D0A), focusedLabelColor = Color(0xFF5C2D0A))'
);

fs.writeFileSync(path, content, 'utf8');
console.log('Updated colors');
