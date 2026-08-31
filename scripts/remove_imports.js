const fs = require('fs');
const path = require('path');

const uiDir = 'app/src/main/java/com/example/daadi/ui/screens/';

function walkDir(dir, callback) {
    fs.readdirSync(dir).forEach(f => {
        let dirPath = path.join(dir, f);
        let isDirectory = fs.statSync(dirPath).isDirectory();
        isDirectory ? walkDir(dirPath, callback) : callback(dirPath);
    });
}

walkDir(uiDir, function(filePath) {
    if (!filePath.endsWith('.kt')) return;
    let content = fs.readFileSync(filePath, 'utf-8');
    if (content.includes('import com.example.daadi.data.supabase.SupabaseManager')) {
        content = content.replace(/import com\.example\.daadi\.data\.supabase\.SupabaseManager\n?/g, '');
        fs.writeFileSync(filePath, content);
    }
});

let mainPath = 'app/src/main/java/com/example/MainActivity.kt';
let mainContent = fs.readFileSync(mainPath, 'utf-8');
mainContent = mainContent.replace(/import com\.example\.daadi\.data\.supabase\.SupabaseManager\n?/g, '');
fs.writeFileSync(mainPath, mainContent);

let vmPath = 'app/src/main/java/com/example/daadi/viewmodel/ViewModels.kt';
let vmContent = fs.readFileSync(vmPath, 'utf-8');
vmContent = vmContent.replace(/import com\.example\.daadi\.data\.supabase\.SupabaseManager\n?/g, '');
fs.writeFileSync(vmPath, vmContent);
