package kr.jiyeok.seatly.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import kr.jiyeok.seatly.data.remote.enums.ERole
import kr.jiyeok.seatly.presentation.viewmodel.AuthViewModel
import kr.jiyeok.seatly.ui.component.admin.AdminBottomNavigationBar
import kr.jiyeok.seatly.ui.component.user.BottomNavigationBar
import kr.jiyeok.seatly.ui.screen.admin.AdminHomeScreen
import kr.jiyeok.seatly.ui.screen.admin.AdminMyPageScreen
import kr.jiyeok.seatly.ui.screen.admin.cafe.RegisterCafeScreen1
import kr.jiyeok.seatly.ui.screen.admin.cafe.RegisterCafeScreen2
import kr.jiyeok.seatly.ui.screen.admin.cafe.StudyCafeListScreen
import kr.jiyeok.seatly.ui.screen.admin.seat.ActivitiesScreen
import kr.jiyeok.seatly.ui.screen.admin.seat.CurrentSeatScreen
import kr.jiyeok.seatly.ui.screen.admin.seat.SeatLayoutScreen
import kr.jiyeok.seatly.ui.screen.common.DashboardScreen
import kr.jiyeok.seatly.ui.screen.common.LoginScreen
import kr.jiyeok.seatly.ui.screen.common.password.PasswordScreen_1
import kr.jiyeok.seatly.ui.screen.common.password.PasswordScreen_2
import kr.jiyeok.seatly.ui.screen.common.password.PasswordScreen_3
import kr.jiyeok.seatly.ui.screen.common.signup.SignupScreen
import kr.jiyeok.seatly.ui.screen.user.CafeDetailScreen
import kr.jiyeok.seatly.ui.screen.user.HomeScreen
import kr.jiyeok.seatly.ui.screen.user.MyPageScreen
import kr.jiyeok.seatly.ui.screen.user.SearchScreen
import androidx.compose.foundation.layout.Box as ComposeBox

@Composable
fun RootNavigation(isOwner: Boolean = true) {
    // ★ 최상위에서 AuthViewModel 생성 (이 인스턴스를 공유해야 함)
    val authViewModel: AuthViewModel = hiltViewModel()
    val userRole by authViewModel.userRole.collectAsState()

    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // Determine ownerState based on user role from AuthViewModel
    val ownerState = userRole == ERole.ADMIN

    val ownerBottomRoutes = listOf("dashboard", "home", "reservation_management", "payments", "settings")
    val userBottomRoutes = listOf("home", "search", "reservation", "mypage")

    val showBottomNav = if (ownerState) currentRoute in ownerBottomRoutes else currentRoute in userBottomRoutes

    Scaffold(
        modifier = Modifier.fillMaxSize()
        // bottomBar 제거: 콘텐츠 위에 오버레이하기 위함
    ) { paddingValues ->

        // 전체를 감싸는 Box (Overlay 구조)
        Box(modifier = Modifier.fillMaxSize()) {

            // 1. 메인 콘텐츠 (NavHost)
            // 상단 패딩만 적용하고, 하단은 전체 화면을 사용하도록 함
            NavHost(
                navController = navController,
                startDestination = "login",
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = paddingValues.calculateTopPadding())
            ) {
                // ===============================================================
                // 공통
                // ===============================================================
                // 대시보드
                composable("dashboard") {
                    if (ownerState) AdminHomeScreen(navController = navController)
                    else DashboardScreen(navController = navController)
                }

                // 알림
                composable("notifications") {
                    ComposeBox(modifier = Modifier.fillMaxSize())
                }

                // ===============================================================
                // Auth
                // ===============================================================
                // 로그인
                composable("login") {
                    LoginScreen(navController = navController, viewModel = authViewModel)
                }

                // 회원 가입
                composable("signup") {
                    SignupScreen(onBack = { navController.popBackStack() }, onNext = { _, _ -> navController.navigate("login") })
                }

                // 비밀번호 찾기 (step 1~3)
                composable("password_step1") {
                    PasswordScreen_1(onBack = { navController.popBackStack() }, onNextNavigate = { navController.navigate("password_step2") })
                }
                composable("password_step2") {
                    PasswordScreen_2(onBack = { navController.popBackStack()}, onVerifiedNavigate = { navController.navigate("password_step3")} )
                }
                composable("password_step3") {
                    PasswordScreen_3(onBack = { navController.popBackStack()}, onCompleteNavigate = { navController.navigate("login")})
                }

                // ===============================================================
                // User
                // ===============================================================
                // 홈
                composable("home") {
                    if (ownerState) {
                        AdminHomeScreen(navController = navController)
                    } else {
                        HomeScreen(navController = navController)
                    }
                }

                // 카페 검색
                composable("search") { SearchScreen(navController = navController) }

                // 마이페이지
                composable("mypage") {
                    MyPageScreen(navController = navController)
                }

                // 즐겨찾기
                composable("favorites") {
                    ComposeBox(modifier = Modifier.fillMaxSize())
                }

                composable("reservation") { ComposeBox(modifier = Modifier.fillMaxSize()) }

                // ===============================================================
                // Admin
                // ===============================================================
                // 홈
                composable("admin_home") { AdminHomeScreen(navController = navController) }

                composable("cafe_list") { StudyCafeListScreen(navController = navController) }

                composable(
                    route = "cafe_detail/{cafeId}",
                    arguments = listOf(navArgument("cafeId") { type = NavType.StringType })
                ) { backStackEntry ->
                    val cafeId = backStackEntry.arguments?.getString("cafeId")
                    CafeDetailScreen(navController = navController, cafeId = cafeId)
                }

                composable("register_cafe_1") { RegisterCafeScreen1(navController = navController) }
                composable("register_cafe_2") { RegisterCafeScreen2(navController = navController) }

                composable("seat_management") {
                    SeatLayoutScreen(
                        onSave = { seats -> navController.popBackStack() },
                        onBack = { navController.popBackStack() }
                    )
                }

                composable("admin_mypage") { AdminMyPageScreen(navController = navController) }

                composable("current_seat") {
                    CurrentSeatScreen(navController = navController, onBack = { navController.popBackStack() })
                }

                composable("recent_activities") { ActivitiesScreen(navController = navController) }
                composable("reservation_management") { ComposeBox(modifier = Modifier.fillMaxSize()) }
                composable("settings") { ComposeBox(modifier = Modifier.fillMaxSize()) }
                composable("recent") { ComposeBox(modifier = Modifier.fillMaxSize()) }
                composable("current_usage_detail") { ComposeBox(modifier = Modifier.fillMaxSize()) }
            }

            // 2. 바텀 네비게이션 (콘텐츠 위에 Overlay)
            if (showBottomNav) {
                Box(
                    modifier = Modifier.align(Alignment.BottomCenter)
                ) {
                    if (ownerState) {
                        AdminBottomNavigationBar(
                            currentRoute = currentRoute,
                            onNavigate = { route ->
                                navController.navigate(route) {
                                    popUpTo(navController.graph.startDestinationId) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )
                    } else {
                        BottomNavigationBar(
                            currentRoute = currentRoute,
                            onNavigate = { route ->
                                navController.navigate(route) {
                                    popUpTo(navController.graph.startDestinationId) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}
