const fs = require('fs');
let appPath = 'app/src/main/java/com/example/daadi/DaadiApplication.kt';
let app = fs.readFileSync(appPath, 'utf-8');

app = app.replace('import com.example.daadi.data.supabase.SupabaseNetworkClient\nimport com.example.daadi.data.repository.supabase.*', 'import com.example.daadi.data.supabase.SupabaseManager');
app = app.replace(/val supabaseNetworkClient:[\s\S]*val userRepository:[^\n]*/m, 'val supabaseManager: SupabaseManager by lazy { SupabaseManager(this) }');
app = app.replace('val adManager: AdManager by lazy { AdManager(this, remoteConfigRepository, analyticsRepository) }', 'val adManager: AdManager by lazy { AdManager(this, supabaseManager) }');
fs.writeFileSync(appPath, app);

let vmPath = 'app/src/main/java/com/example/daadi/viewmodel/ViewModels.kt';
let vm = fs.readFileSync(vmPath, 'utf-8');
vm = vm.replace(/val authRepository:[^\n]*\n[^\n]*\n[^\n]*\n[^\n]*\n[^\n]*/m, 'val supabaseManager: com.example.daadi.data.supabase.SupabaseManager');
vm = vm.replace('application.authRepository, application.analyticsRepository, application.remoteConfigRepository, application.remoteGameRepository, application.supportRepository', 'application.supabaseManager');
vm = vm.replace(/class ViewModelFactory[^]*$/, 
`class ViewModelFactory(private val application: DaadiApplication) : androidx.lifecycle.ViewModelProvider.Factory {
    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return when {
            modelClass.isAssignableFrom(GameViewModel::class.java) -> GameViewModel(application.gameRepository, application.statsRepository, application.settingsRepository, application.soundManager, application.multiplayerManager, application.supabaseManager)
            modelClass.isAssignableFrom(StatsViewModel::class.java) -> StatsViewModel(application.statsRepository)
            modelClass.isAssignableFrom(SettingsViewModel::class.java) -> SettingsViewModel(application.settingsRepository)
            else -> throw IllegalArgumentException("Unknown ViewModel class")
        } as T
    }
}
`);
fs.writeFileSync(vmPath, vm);

let mainPath = 'app/src/main/java/com/example/MainActivity.kt';
let main = fs.readFileSync(mainPath, 'utf-8');
main = main.replace(/val authRepository = application.authRepository/g, 'val supabaseManager = application.supabaseManager');
main = main.replace(/authRepository\.currentUser/g, 'supabaseManager.currentUser');
main = main.replace(/authRepository\.hasPermission/g, 'supabaseManager.hasPermission');

main = main.replace(/adminViewModel\s*=\s*adminViewModel/g, 'supabaseManager = supabaseManager');
main = main.replace(/AdminNavigator\(adminViewModel\)/g, 'AdminNavigator(supabaseManager = supabaseManager)');

fs.writeFileSync(mainPath, main);

console.log('Reverted App, ViewModels, MainActivity');
