const fs = require('fs');

function fixImport(path) {
    let content = fs.readFileSync(path, 'utf-8');
    if (content.startsWith('import ')) {
        // Find the package declaration
        let pkgMatch = content.match(/package\s+[a-zA-Z0-9_.]+/);
        if (pkgMatch) {
            content = content.replace(pkgMatch[0], '');
            content = pkgMatch[0] + '\n\n' + content;
            fs.writeFileSync(path, content);
            console.log('Fixed ' + path);
        }
    }
}

let files = [
    'app/src/main/java/com/example/daadi/data/ads/AdManager.kt',
    'app/src/main/java/com/example/daadi/data/multiplayer/MultiplayerManager.kt',
    'app/src/main/java/com/example/daadi/ui/screens/GameAndEndScreens.kt',
    'app/src/main/java/com/example/daadi/ui/screens/HomeScreen.kt',
    'app/src/main/java/com/example/daadi/ui/screens/ModeAndDifficultyScreens.kt',
    'app/src/main/java/com/example/daadi/ui/screens/MultiplayerLobbyScreen.kt',
    'app/src/main/java/com/example/daadi/ui/screens/SplashScreen.kt',
    'app/src/main/java/com/example/daadi/ui/screens/StatsAndSettingsScreens.kt',
    'app/src/main/java/com/example/daadi/ui/screens/SupabaseAdminScreen.kt',
    'app/src/main/java/com/example/daadi/ui/screens/SupabaseAuthScreen.kt',
    'app/src/main/java/com/example/daadi/ui/screens/UserFeedbackScreen.kt',
    'app/src/main/java/com/example/daadi/ui/screens/admin/AdminAIAssistantScreen.kt',
    'app/src/main/java/com/example/daadi/ui/screens/admin/AdminAntiCheatScreens.kt',
    'app/src/main/java/com/example/daadi/ui/screens/admin/AdminBIAnalyticsScreen.kt',
    'app/src/main/java/com/example/daadi/ui/screens/admin/AdminBIPlatformScreens.kt',
    'app/src/main/java/com/example/daadi/ui/screens/admin/AdminCMSScreens.kt',
    'app/src/main/java/com/example/daadi/ui/screens/admin/AdminConfigScreens.kt',
    'app/src/main/java/com/example/daadi/ui/screens/admin/AdminCrashCenterScreen.kt',
    'app/src/main/java/com/example/daadi/ui/screens/admin/AdminDashboardScreen.kt',
    'app/src/main/java/com/example/daadi/ui/screens/admin/AdminDesignSystem.kt',
    'app/src/main/java/com/example/daadi/ui/screens/admin/AdminDeviceCenterScreen.kt',
    'app/src/main/java/com/example/daadi/ui/screens/admin/AdminEconomyScreens.kt',
    'app/src/main/java/com/example/daadi/ui/screens/admin/AdminEventScreens.kt',
    'app/src/main/java/com/example/daadi/ui/screens/admin/AdminFoundationScreens.kt',
    'app/src/main/java/com/example/daadi/ui/screens/admin/AdminFraudDetectionScreen.kt',
    'app/src/main/java/com/example/daadi/ui/screens/admin/AdminLeaderboardScreens.kt',
    'app/src/main/java/com/example/daadi/ui/screens/admin/AdminLiveOpsScreens.kt',
    'app/src/main/java/com/example/daadi/ui/screens/admin/AdminMatchScreens.kt',
    'app/src/main/java/com/example/daadi/ui/screens/admin/AdminMonitoringScreen.kt',
    'app/src/main/java/com/example/daadi/ui/screens/admin/AdminNavigator.kt',
    'app/src/main/java/com/example/daadi/ui/screens/admin/AdminRewardScreens.kt',
    'app/src/main/java/com/example/daadi/ui/screens/admin/AdminSafetyScreens.kt',
    'app/src/main/java/com/example/daadi/ui/screens/admin/AdminSeasonPassScreens.kt',
    'app/src/main/java/com/example/daadi/ui/screens/admin/AdminStoreScreens.kt',
    'app/src/main/java/com/example/daadi/ui/screens/admin/AdminSupportScreens.kt',
    'app/src/main/java/com/example/daadi/ui/screens/admin/AdminTournamentScreens.kt',
    'app/src/main/java/com/example/daadi/ui/screens/admin/AdminUserScreens.kt'
];

for (let file of files) {
    if (fs.existsSync(file)) {
        fixImport(file);
    }
}
