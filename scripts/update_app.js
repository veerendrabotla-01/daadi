const fs = require('fs');

let appPath = 'app/src/main/java/com/example/daadi/DaadiApplication.kt';
let app = fs.readFileSync(appPath, 'utf-8');
app = app.replace('import com.example.daadi.data.supabase.SupabaseManager', 'import com.example.daadi.data.supabase.SupabaseNetworkClient\nimport com.example.daadi.data.repository.supabase.*');
app = app.replace('val supabaseManager: SupabaseManager by lazy { SupabaseManager(this) }', 
`val supabaseNetworkClient: SupabaseNetworkClient by lazy { SupabaseNetworkClient(this) }
    val authRepository: AuthRepository by lazy { AuthRepository(supabaseNetworkClient) }
    val adminRepository: AdminRepository by lazy { AdminRepository(supabaseNetworkClient) }
    val analyticsRepository: AnalyticsRepository by lazy { AnalyticsRepository(supabaseNetworkClient) }
    val remoteGameRepository: RemoteGameRepository by lazy { RemoteGameRepository(supabaseNetworkClient) }
    val economyRepository: EconomyRepository by lazy { EconomyRepository(supabaseNetworkClient) }
    val liveOpsRepository: LiveOpsRepository by lazy { LiveOpsRepository(supabaseNetworkClient) }
    val supportRepository: SupportRepository by lazy { SupportRepository(supabaseNetworkClient) }
    val tournamentRepository: TournamentRepository by lazy { TournamentRepository(supabaseNetworkClient) }
    val remoteConfigRepository: RemoteConfigRepository by lazy { RemoteConfigRepository(supabaseNetworkClient) }
    val userRepository: UserRepository by lazy { UserRepository(supabaseNetworkClient) }`);

app = app.replace('val adManager: AdManager by lazy { AdManager(this, supabaseManager) }', 'val adManager: AdManager by lazy { AdManager(this, remoteConfigRepository, analyticsRepository) }');
fs.writeFileSync(appPath, app);

let vmPath = 'app/src/main/java/com/example/daadi/viewmodel/ViewModels.kt';
let vm = fs.readFileSync(vmPath, 'utf-8');
vm = vm.replace('val supabaseManager: com.example.daadi.data.supabase.SupabaseManager', 
`val authRepository: com.example.daadi.data.repository.supabase.AuthRepository,
    val analyticsRepository: com.example.daadi.data.repository.supabase.AnalyticsRepository,
    val remoteConfigRepository: com.example.daadi.data.repository.supabase.RemoteConfigRepository,
    val remoteGameRepository: com.example.daadi.data.repository.supabase.RemoteGameRepository,
    val supportRepository: com.example.daadi.data.repository.supabase.SupportRepository`);

vm = vm.substring(0, vm.lastIndexOf('class ViewModelFactory')) + 
`class ViewModelFactory(private val application: com.example.daadi.DaadiApplication) : androidx.lifecycle.ViewModelProvider.Factory {
    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return when {
            modelClass.isAssignableFrom(GameViewModel::class.java) -> GameViewModel(application.gameRepository, application.statsRepository, application.settingsRepository, application.soundManager, application.multiplayerManager, application.authRepository, application.analyticsRepository, application.remoteConfigRepository, application.remoteGameRepository, application.supportRepository)
            modelClass.isAssignableFrom(StatsViewModel::class.java) -> StatsViewModel(application.statsRepository)
            modelClass.isAssignableFrom(SettingsViewModel::class.java) -> SettingsViewModel(application.settingsRepository)
            modelClass.isAssignableFrom(AdminViewModel::class.java) -> AdminViewModel(application.authRepository, application.adminRepository, application.analyticsRepository, application.remoteGameRepository, application.economyRepository, application.liveOpsRepository, application.supportRepository, application.tournamentRepository, application.remoteConfigRepository, application.userRepository)
            else -> throw IllegalArgumentException("Unknown ViewModel class")
        } as T
    }
}
`;
fs.writeFileSync(vmPath, vm);
