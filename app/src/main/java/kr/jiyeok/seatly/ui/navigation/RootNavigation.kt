package kr.jiyeok.seatly.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import kr.jiyeok.seatly.presentation.viewmodel.password.PasswordRecoveryViewModel
import kr.jiyeok.seatly.ui.components.BottomNavigationBar
import kr.jiyeok.seatly.ui.screen.dashboard.DashboardScreen
import kr.jiyeok.seatly.ui.screen.detail.CafeDetailScreen
import kr.jiyeok.seatly.ui.screen.home.HomeScreen
import kr.jiyeok.seatly.ui.screen.login.LoginScreen
import kr.jiyeok.seatly.ui.screen.password.PasswordScreen_1
import kr.jiyeok.seatly.ui.screen.password.PasswordScreen_2
import kr.jiyeok.seatly.ui.screen.password.PasswordScreen_3
import kr.jiyeok.seatly.ui.screen.search.SearchScreen
import kr.jiyeok.seatly.ui.screen.signup.SignupScreen

@Composable
fun RootNavigation() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val showBottomNav = currentRoute in listOf("home", "search", "reservation", "mypage")

    val passwordVm: PasswordRecoveryViewModel = viewModel()

    Scaffold(
        bottomBar = {
            if (showBottomNav) {
                BottomNavigationBar(
                    currentRoute = currentRoute,
                    onNavigate = { route ->
                        navController.navigate(route) {
                            popUpTo(navController.graph.startDestinationId) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
        },
        modifier = Modifier.fillMaxSize()
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            NavHost(navController = navController, startDestination = "dashboard", modifier = Modifier.fillMaxSize()) {
                composable("dashboard") {
                    DashboardScreen(navController = navController)
                }

                composable("login") {
                    LoginScreen(navController = navController)
                }

                composable("signup") {
                    SignupScreen(
                        onBack = { navController.popBackStack() },
                        onNext = { email, password ->
                            navController.navigate("login")
                        }
                    )
                }

                composable("password_step1") {
                    PasswordScreen_1(
                        viewModel = passwordVm,
                        onBack = { navController.popBackStack() },
                        onNextNavigate = {
                            navController.navigate("password_step2")
                        }
                    )
                }

                composable("password_step2") {
                    PasswordScreen_2(
                        viewModel = passwordVm,
                        onBack = { nav_controller_pop(navController) },
                        onVerifiedNavigate = {
                            navController.navigate("password_step3")
                        }
                    )
                }

                composable("password_step3") {
                    PasswordScreen_3(
                        viewModel = passwordVm,
                        onBack = { nav_controller_pop(navController) },
                        onCompleteNavigate = {
                            navController.navigate("login") {
                                popUpTo("login") { inclusive = true }
                            }
                        }
                    )
                }

                composable("home") {
                    HomeScreen(navController = navController)
                }

                composable("search") {
                    SearchScreen(navController = navController)
                }

                composable("reservation") {
                    Box(modifier = Modifier.fillMaxSize())
                }

                composable("mypage") {
                    Box(modifier = Modifier.fillMaxSize())
                }

                composable(route = "cafe_detail/{cafeId}", arguments = listOf(navArgument("cafeId") { type = NavType.StringType })) { backStackEntry ->
                    val cafeId = backStackEntry.arguments?.getString("cafeId")
                    CafeDetailScreen(navController = navController, cafeId = cafeId)
                }
            }
        }
    }
}

// Small helper to keep code concise and explicit popBack behavior
private fun nav_controller_pop(navController: androidx.navigation.NavHostController) {
    navController.popBackStack()
}