const fs = require('fs');

let mainPath = 'app/src/main/java/com/example/MainActivity.kt';
let mainContent = fs.readFileSync(mainPath, 'utf-8');

// Replace supabaseManager with adminViewModel or the repos directly.
// MainActivity creates AdminNavigator, GameAndEndScreens, etc.
// In MainActivity, we pass `application.supabaseManager`.
// Let's replace `supabaseManager` with the respective repos!

mainContent = mainContent.replace(/supabaseManager = application\.supabaseManager/g, ''); // We removed this
// Wait, MainActivity expects supabaseManager in AdminNavigator?
// AdminNavigator now expects adminViewModel!
mainContent = mainContent.replace(/AdminNavigator\(\s*supabaseManager\s*=\s*supabaseManager\s*\)/g, 'AdminNavigator(adminViewModel = adminViewModel)');
mainContent = mainContent.replace(/AdminNavigator\(supabaseManager\)/g, 'AdminNavigator(adminViewModel)');
// Wait, where is `adminViewModel` in MainActivity? We need to instantiate it!
mainContent = mainContent.replace(/val sharedGameViewModel: GameViewModel = viewModel\(factory = factory\)/g, 
`val sharedGameViewModel: GameViewModel = viewModel(factory = factory)
        val adminViewModel: com.example.daadi.viewmodel.AdminViewModel = viewModel(factory = factory)`);

mainContent = mainContent.replace(/supabaseManager = supabaseManager/g, 'gameViewModel = sharedGameViewModel');
mainContent = mainContent.replace(/supabaseManager/g, 'sharedGameViewModel'); // Fallback

fs.writeFileSync(mainPath, mainContent);

console.log('Fixed MainActivity');
