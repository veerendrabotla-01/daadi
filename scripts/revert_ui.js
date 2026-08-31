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
    let isChanged = false;

    // Restore import
    if (!content.includes('import com.example.daadi.data.supabase.SupabaseManager')) {
        content = 'import com.example.daadi.data.supabase.SupabaseManager\n' + content;
        isChanged = true;
    }

    if (content.includes('adminViewModel: com.example.daadi.viewmodel.AdminViewModel')) {
        content = content.replace(/adminViewModel:\s*com\.example\.daadi\.viewmodel\.AdminViewModel/g, 'supabaseManager: SupabaseManager');
        isChanged = true;
    }
    if (content.includes('sharedGameViewModel: com.example.daadi.viewmodel.GameViewModel')) {
        content = content.replace(/sharedGameViewModel:\s*com\.example\.daadi\.viewmodel\.GameViewModel/g, 'supabaseManager: SupabaseManager');
        isChanged = true;
    }

    // Convert adminViewModel.xxxRepository.method back to supabaseManager.method
    // Actually, I can just match `adminViewModel.[a-zA-Z]+Repository\.([a-zA-Z0-9]+)`
    content = content.replace(/adminViewModel\.[a-zA-Z]+Repository\.([a-zA-Z0-9]+)/g, 'supabaseManager.$1');
    content = content.replace(/sharedGameViewModel\.[a-zA-Z]+Repository\.([a-zA-Z0-9]+)/g, 'supabaseManager.$1');
    content = content.replace(/app\.[a-zA-Z]+Repository\.([a-zA-Z0-9]+)/g, 'app.supabaseManager.$1');
    
    content = content.replace(/adminViewModel\s*=\s*adminViewModel/g, 'supabaseManager = supabaseManager');
    content = content.replace(/sharedGameViewModel\s*=\s*sharedGameViewModel/g, 'supabaseManager = supabaseManager');
    content = content.replace(/,\s*adminViewModel\)/g, ', supabaseManager)');
    content = content.replace(/,\s*sharedGameViewModel\)/g, ', supabaseManager)');

    if (isChanged) {
        fs.writeFileSync(filePath, content);
        console.log('Reverted ' + filePath);
    }
});

let adManagerPath = 'app/src/main/java/com/example/daadi/data/ads/AdManager.kt';
let adContent = fs.readFileSync(adManagerPath, 'utf-8');
adContent = adContent.replace(/val remoteConfigRepository[^,]*, val analyticsRepository[^\)]*/, 'val supabaseManager: SupabaseManager');
adContent = adContent.replace(/remoteConfigRepository\./g, 'supabaseManager.');
adContent = adContent.replace(/analyticsRepository\./g, 'supabaseManager.');
if (!adContent.includes('import com.example.daadi.data.supabase.SupabaseManager')) {
    adContent = 'import com.example.daadi.data.supabase.SupabaseManager\n' + adContent;
}
fs.writeFileSync(adManagerPath, adContent);

let multiManagerPath = 'app/src/main/java/com/example/daadi/data/multiplayer/MultiplayerManager.kt';
let multiContent = fs.readFileSync(multiManagerPath, 'utf-8');
multiContent = multiContent.replace(/val remoteGameRepository[^\)]*/, 'val supabaseManager: SupabaseManager');
multiContent = multiContent.replace(/remoteGameRepository\./g, 'supabaseManager.');
if (!multiContent.includes('import com.example.daadi.data.supabase.SupabaseManager')) {
    multiContent = 'import com.example.daadi.data.supabase.SupabaseManager\n' + multiContent;
}
fs.writeFileSync(multiManagerPath, multiContent);

console.log('UI reverted.');
