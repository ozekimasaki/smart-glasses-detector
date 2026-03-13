package jp.smartglasses.detector.presentation.navigation

sealed class Screen(val route: String) {
    object Onboarding : Screen("onboarding")
    object Main : Screen("main")
    object History : Screen("history")
    object Settings : Screen("settings")
    object About : Screen("about")
    object Privacy : Screen("privacy")
}
