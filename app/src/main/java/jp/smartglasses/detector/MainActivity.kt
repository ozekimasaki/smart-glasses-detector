package jp.smartglasses.detector

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import dagger.hilt.android.AndroidEntryPoint
import jp.smartglasses.detector.presentation.about.AboutScreen
import jp.smartglasses.detector.presentation.history.HistoryScreen
import jp.smartglasses.detector.presentation.main.MainScreen
import jp.smartglasses.detector.presentation.navigation.Screen
import jp.smartglasses.detector.presentation.onboarding.OnboardingScreen
import jp.smartglasses.detector.presentation.onboarding.OnboardingViewModel
import jp.smartglasses.detector.presentation.privacy.PrivacyScreen
import jp.smartglasses.detector.presentation.settings.SettingsScreen
import jp.smartglasses.detector.ui.theme.スマートグラス検出Theme

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            スマートグラス検出Theme {
                AppNavigation()
            }
        }
    }
}

@Composable
fun AppNavigation(
    navController: NavHostController = rememberNavController(),
    onboardingViewModel: OnboardingViewModel = hiltViewModel()
) {
    val onboardingCompleted by onboardingViewModel.onboardingCompleted.collectAsStateWithLifecycle()

    val completed = onboardingCompleted
    if (completed == null) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        }
        return
    }

    val startDestination = if (completed) Screen.Main.route else Screen.Onboarding.route

    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = Modifier.fillMaxSize()
    ) {
        composable(Screen.Onboarding.route) {
            OnboardingScreen(
                onComplete = {
                    onboardingViewModel.setOnboardingCompleted(true)
                    navController.navigate(Screen.Main.route) {
                        popUpTo(Screen.Onboarding.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Main.route) {
            MainScreen(
                onNavigate = { route ->
                    navController.navigate(route) {
                        launchSingleTop = true
                    }
                }
            )
        }

        composable(Screen.History.route) {
            HistoryScreen(
                onNavigate = { route ->
                    navController.navigate(route) {
                        launchSingleTop = true
                    }
                }
            )
        }

        composable(Screen.Settings.route) {
            SettingsScreen(
                onNavigate = { route ->
                    navController.navigate(route) {
                        launchSingleTop = true
                    }
                }
            )
        }

        composable(Screen.About.route) {
            AboutScreen(
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Privacy.route) {
            PrivacyScreen(
                onBack = { navController.popBackStack() }
            )
        }
    }
}
