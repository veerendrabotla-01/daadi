const fs = require('fs');

const path = 'app/src/main/java/com/example/daadi/ui/screens/admin/AdminSupportScreens.kt';
let content = fs.readFileSync(path, 'utf8');

content = content.replace(
    /colors = TextFieldDefaults\.colors\(focusedContainerColor = Color\.White, unfocusedContainerColor = Color\.White\)/g,
    'colors = TextFieldDefaults.colors(focusedContainerColor = Color.White, unfocusedContainerColor = Color.White, focusedTextColor = Color.Black, unfocusedTextColor = Color.Black)'
);

fs.writeFileSync(path, content, 'utf8');
console.log('Fixed admin support colors');
