package kr.jiyeok.seatly.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import kr.jiyeok.seatly.ui.components.BottomNavigationBar
import kr.jiyeok.seatly.ui.components.OwnerBottomNavigationBar
import kr.jiyeok.seatly.ui.screen.dashboard.DashboardScreen
import kr.jiyeok.seatly.ui.screen.detail.CafeDetailScreen
import kr.jiyeok.seatly.ui.screen.home.HomeScreen
import kr.jiyeok.seatly.ui.screen.home.OwnerHomeScreen
import kr.jiyeok.seatly.ui.screen.login.LoginScreen
import kr.jiyeok.seatly.ui.screen.manager.RegisterCafeScreen1
import kr.jiyeok.seatly.ui.screen.manager.RegisterCafeScreen2
import kr.jiyeok.seatly.ui.screen.manager.StudyCafeListScreen
import kr.jiyeok.seatly.ui.screen.search.SearchScreen
import kr.jiyeok.seatly.ui.screen.signup.SignupScreen
import kr.jiyeok.seatly.ui.screen.owner.SeatLayoutScreen
import kr.jiyeok.seatly.ui.screen.owner.CurrentSeatScreen
import kr.jiyeok.seatly.ui.screen.owner.ActivitiesScreen
import androidx.compose.foundation.layout.Box as ComposeBox

@Composable
fun RootNavigation(isOwner: Boolean = true) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    var ownerState by remember { mutableStateOf(isOwner) }
    var isAuthenticated by remember { mutableStateOf(false) }

    val ownerBottomRoutes = listOf("dashboard", "home", "reservation_management", "payments", "settings")
    val userBottomRoutes = listOf("home", "search", "reservation", "mypage")

    val showBottomNav = if (ownerState) currentRoute in ownerBottomRoutes else currentRoute in userBottomRoutes

    Scaffold(
        bottomBar = {
            if (showBottomNav) {
                if (ownerState) {
                    OwnerBottomNavigationBar(currentRoute = currentRoute, onNavigate = { route ->
                        navController.navigate(route) {
                            popUpTo(navController.graph.startDestinationId) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    })
                } else {
                    BottomNavigationBar(currentRoute = currentRoute, onNavigate = { route ->
                        navController.navigate(route) {
                            popUpTo(navController.graph.startDestinationId) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    })
                }
            }
        },
        modifier = Modifier.fillMaxSize()
    ) { paddingValues ->
        ComposeBox(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            NavHost(navController = navController, startDestination = "login", modifier = Modifier.fillMaxSize()) {
                composable("login") { LoginScreen(navController = navController) }
                composable("signup") {
                    SignupScreen(onBack = { navController.popBackStack() }, onNext = { _, _ -> navController.navigate("login") })
                }
                composable("home") {
                    if (ownerState) OwnerHomeScreen(navController = navController) else HomeScreen(navController = navController)
                }
                composable("dashboard") { if (ownerState) OwnerHomeScreen(navController = navController) else DashboardScreen(navController = navController) }
                composable("search") { SearchScreen(navController = navController) }
                composable("reservation") { ComposeBox(modifier = Modifier.fillMaxSize()) }
                composable("mypage") { ComposeBox(modifier = Modifier.fillMaxSize()) }


                // registered cafe list
                composable("cafe_list") {
                    StudyCafeListScreen(navController = navController)
                }

                // register cafe screen
                composable("register_cafe_1") {
                    RegisterCafeScreen1(navController = navController)
                }

                composable("register_cafe_2") {
                    RegisterCafeScreen2(navController = navController)
                }

                // seat editor (full screen)
                composable("seat_management") {
                    SeatLayoutScreen(onSave = { seats ->
                        // TODO: persist seats
                        navController.popBackStack()
                    }, onBack = {
                        navController.popBackStack()
                    })
                }

                // Current seat screen (owner -> "좌석 관리")
                composable("current_seat") {
                    CurrentSeatScreen(navController = navController, onBack = { navController.popBackStack() })
                }

                // Activities (full list) screen
                composable("recent_activities") {
                    ActivitiesScreen(navController = navController)
                }

                composable("reservation_management") { ComposeBox(modifier = Modifier.fillMaxSize()) }
                composable("payments") { ComposeBox(modifier = Modifier.fillMaxSize()) }
                composable("settings") { ComposeBox(modifier = Modifier.fillMaxSize()) }

                composable("favorites") { ComposeBox(modifier = Modifier.fillMaxSize()) }
                composable("recent") { ComposeBox(modifier = Modifier.fillMaxSize()) }
                composable("notifications") { ComposeBox(modifier = Modifier.fillMaxSize()) }
                composable("current_usage_detail") { ComposeBox(modifier = Modifier.fillMaxSize()) }

                composable(route = "cafe_detail/{cafeId}", arguments = listOf(navArgument("cafeId") { type = NavType.StringType })) { backStackEntry ->
                    val cafeId = backStackEntry.arguments?.getString("cafeId")
                    CafeDetailScreen(navController = navController, cafeId = cafeId)
                }
            }
        }
    }
}