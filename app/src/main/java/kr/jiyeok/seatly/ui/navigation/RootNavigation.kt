package kr.jiyeok.seatly.ui.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import kr.jiyeok.seatly.domain.model.ERole
import kr.jiyeok.seatly.presentation.viewmodel.AuthViewModel
import kr.jiyeok.seatly.ui.component.admin.AdminBottomNavigationBar
import kr.jiyeok.seatly.ui.components.BottomNavigationBar
import kr.jiyeok.seatly.ui.screen.common.DashboardScreen
import kr.jiyeok.seatly.ui.screen.user.CafeDetailScreen
import kr.jiyeok.seatly.ui.screen.user.HomeScreen
import kr.jiyeok.seatly.ui.screen.admin.AdminHomeScreen
import kr.jiyeok.seatly.ui.screen.admin.AdminMyPageScreen
import kr.jiyeok.seatly.ui.screen.common.LoginScreen
import kr.jiyeok.seatly.ui.screen.admin.cafe.RegisterCafeScreen1
import kr.jiyeok.seatly.ui.screen.admin.cafe.RegisterCafeScreen2
import kr.jiyeok.seatly.ui.screen.admin.cafe.StudyCafeListScreen
import kr.jiyeok.seatly.ui.screen.user.SearchScreen
import kr.jiyeok.seatly.ui.screen.common.signup.SignupScreen
import kr.jiyeok.seatly.ui.screen.admin.seat.SeatLayoutScreen
import kr.jiyeok.seatly.ui.screen.admin.seat.CurrentSeatScreen
import kr.jiyeok.seatly.ui.screen.admin.seat.ActivitiesScreen
import kr.jiyeok.seatly.ui.screen.common.password.PasswordScreen_1
import kr.jiyeok.seatly.ui.screen.common.password.PasswordScreen_2
import kr.jiyeok.seatly.ui.screen.common.password.PasswordScreen_3
import androidx.compose.foundation.layout.Box as ComposeBox

@Composable
fun RootNavigation(isOwner: Boolean = true) {
    val authViewModel: AuthViewModel = hiltViewModel()
    val userRole by authViewModel.userRole.collectAsState()
    
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // Determine ownerState based on user role from AuthViewModel
    val ownerState = userRole == ERole.ADMIN
    var isAuthenticated by remember { mutableStateOf(false) }

    val ownerBottomRoutes = listOf("dashboard", "home", "reservation_management", "payments", "settings")
    val userBottomRoutes = listOf("home", "search", "reservation", "mypage")

    val showBottomNav = if (ownerState) currentRoute in ownerBottomRoutes else currentRoute in userBottomRoutes

    Scaffold(
        bottomBar = {
            if (showBottomNav) {
                if (ownerState) {
                    AdminBottomNavigationBar(currentRoute = currentRoute, onNavigate = { route ->
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

                // ========================= 로그인 관련 =========================
                // 로그인
                composable("login") { LoginScreen(navController = navController) }

                // 회원 가입
                composable("signup") {
                    SignupScreen(onBack = { navController.popBackStack() }, onNext = { _, _ -> navController.navigate("login") })
                }

                // 비밀번호 찾기 1
                // NOTE: 경로명을 LoginScreen에서 사용하는 "password_step1"으로 통일
                composable("password_step1") {
                    PasswordScreen_1(onBack = { navController.popBackStack() }, onNextNavigate = { navController.navigate("password_step2") })
                }

                // 비밀번호 찾기 2
                composable("password_step2") {
                    PasswordScreen_2(onBack = { navController.popBackStack()}, onVerifiedNavigate = { navController.navigate("password_step3")} )
                }

                // 비밀번호 찾기 3
                composable("password_step3") {
                    PasswordScreen_3(onBack = { navController.popBackStack()}, onCompleteNavigate = { navController.navigate("login")})
                }

                // 알림
                composable("notifications") { ComposeBox(modifier = Modifier.fillMaxSize()) }


                // ========================= 유저 관련 =========================
                // 유저 홈
                composable("home") {
                    if (ownerState) AdminHomeScreen(navController = navController) else HomeScreen(navController = navController)
                }

                composable("dashboard") { if (ownerState) AdminHomeScreen(navController = navController) else DashboardScreen(navController = navController) }

                // 찜한 카페
                composable("favorites") { ComposeBox(modifier = Modifier.fillMaxSize()) }

                // 카페 검색
                composable("search") { SearchScreen(navController = navController) }

                // 카페 예약
                composable("reservation") { ComposeBox(modifier = Modifier.fillMaxSize()) }

                // 유저 마이페이지
                composable("mypage") { ComposeBox(modifier = Modifier.fillMaxSize()) }



                // ========================= 관리자 관련 =========================
                // 관리자 홈
                composable("admin_home") {
                    AdminHomeScreen(navController = navController)
                }

                // 등록 카페 리스트
                composable("cafe_list") {
                    StudyCafeListScreen(navController = navController)
                }

                // 카페 상세 정보
                composable(route = "cafe_detail/{cafeId}", arguments = listOf(navArgument("cafeId") { type = NavType.StringType })) { backStackEntry ->
                    val cafeId = backStackEntry.arguments?.getString("cafeId")
                    CafeDetailScreen(navController = navController, cafeId = cafeId)
//                    StudyCafeDetailScreen(navController = navController, cafeId = cafeId.toString())
                }

                // 카페 등록 1
                composable("register_cafe_1") {
                    RegisterCafeScreen1(navController = navController)
                }

                // 카페 등록 2
                composable("register_cafe_2") {
                    RegisterCafeScreen2(navController = navController)
                }

                // 좌석 편집
                composable("seat_management") {
                    SeatLayoutScreen(onSave = { seats ->
                        // TODO: persist seats
                        navController.popBackStack()
                    }, onBack = {
                        navController.popBackStack()
                    })
                }

                composable("admin_mypage") {
                    AdminMyPageScreen(navController = navController)
                }


                // Current seat screen (owner -> "좌석 관리")
                composable("current_seat") {
                    CurrentSeatScreen(navController = navController, onBack = { navController.popBackStack() })
                }

                // 최근 활동
                composable("recent_activities") {
                    ActivitiesScreen(navController = navController)
                }

                composable("reservation_management") { ComposeBox(modifier = Modifier.fillMaxSize()) }
                composable("settings") { ComposeBox(modifier = Modifier.fillMaxSize()) }


                composable("recent") { ComposeBox(modifier = Modifier.fillMaxSize()) }


                composable("current_usage_detail") { ComposeBox(modifier = Modifier.fillMaxSize()) }


            }
        }
    }
}