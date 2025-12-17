package kr.jiyeok.seatly.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import kr.jiyeok.seatly.ui.screen.dashboard.DashboardScreen
import kr.jiyeok.seatly.ui.screen.login.LoginScreen

@Composable
fun RootNavigation() {
    val navController = rememberNavController()
    NavHost(
        navController = navController,
        startDestination = "dashboard"
    ) {
        composable("dashboard") {
            DashboardScreen(navController = navController)
        }
        composable("login") {
            LoginScreen(navController = navController)
        }
    }
}
