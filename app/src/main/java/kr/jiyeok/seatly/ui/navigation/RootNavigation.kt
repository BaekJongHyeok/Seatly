package kr.jiyeok.seatly.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import kr.jiyeok.seatly.data.remote.enums.ERole
import kr.jiyeok.seatly.presentation.viewmodel.AuthViewModel
import kr.jiyeok.seatly.ui.component.common.BottomNavigationBar
import kr.jiyeok.seatly.ui.screen.admin.AdminMyPageScreen
import kr.jiyeok.seatly.ui.screen.admin.cafe.CreateCafeScreen
import kr.jiyeok.seatly.ui.screen.admin.cafe.UpdateCafeScreen
import kr.jiyeok.seatly.ui.screen.admin.seat.CurrentSeatScreen
import kr.jiyeok.seatly.ui.screen.admin.seat.SeatLayoutScreen
import kr.jiyeok.seatly.ui.screen.common.DashboardScreen
import kr.jiyeok.seatly.ui.screen.common.LoginScreen
import kr.jiyeok.seatly.ui.screen.common.password.PasswordScreen1
import kr.jiyeok.seatly.ui.screen.common.password.PasswordScreen2
import kr.jiyeok.seatly.ui.screen.common.password.PasswordScreen3
import kr.jiyeok.seatly.ui.screen.common.signup.SignupScreen
import kr.jiyeok.seatly.ui.screen.user.AppSettings
import kr.jiyeok.seatly.ui.screen.user.UserCafeDetailScreen
import kr.jiyeok.seatly.ui.screen.user.EditProfileScreen
import kr.jiyeok.seatly.ui.screen.user.UserHomeScreen
import kr.jiyeok.seatly.ui.screen.user.UserMyPageScreen
import kr.jiyeok.seatly.ui.screen.user.UserSearchScreen
import androidx.compose.runtime.collectAsState
import kr.jiyeok.seatly.ui.screen.admin.AdminHomeScreen
import kr.jiyeok.seatly.ui.screen.common.NotificationsScreen
import kr.jiyeok.seatly.ui.screen.user.AdminCafeDetailScreen

/**
 * Seatly 앱의 최상위 네비게이션 구조를 정의하는 컴포저블
 * 사용자 역할(일반 사용자/관리자)에 따라 다른 라우트와 UI를 제공
 *
 * 바텀 네비게이션: 각 역할별 메인 화면에서만 표시
 */
@Composable
fun RootNavigation() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val authViewModel: AuthViewModel = hiltViewModel()
    val userRole = authViewModel.userRole.collectAsState()
    val isAdmin = userRole.value == ERole.ADMIN

    // ADMIN, USER 에 따라 보여질 라우트 리스트
    val adminMainRoutes = listOf("admin/home", "admin/mypage")
    val userMainRoutes = listOf("user/home", "user/search", "user/mypage")

    // 현재 라우트가 메인 라우트들에 포함되지 않으면 BottomNavigationBar 표시 X
    val mainRoutes = if (isAdmin) adminMainRoutes else userMainRoutes

    Scaffold(modifier = Modifier.fillMaxSize()) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize()) {
            // 1. 화면 루트
            NavHost(
                navController = navController,
                startDestination = "common/dashboard",
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = paddingValues.calculateTopPadding())
            ) {
                defineCommonRoutes(navController)   // 공통
                defineAuthRoutes(navController, authViewModel)     // 인증
                defineUserRoutes(navController, authViewModel)     // User
                defineAdminRoutes(navController, authViewModel)    // Admin
            }

            // 2. 바텀 네비게이션 (콘텐츠 위에 Overlay)
            if (currentRoute in mainRoutes) {
                Box(Modifier.align(Alignment.BottomCenter)) {
                    BottomNavigationBar(
                        currentRoute = currentRoute,
                        onNavigate = { route -> navigateWithBackStackPolicy(navController, route) },
                        isAdmin = isAdmin
                    )
                }
            }
        }
    }
}

/**
 * 공통 라우트 정의
 * 모든 사용자가 접근 가능한 화면
 */
private fun NavGraphBuilder.defineCommonRoutes(navController: NavController) {
    composable("common/dashboard") {
        DashboardScreen(navController = navController)
    }

    composable("common/notifications") {
        NotificationsScreen(navController = navController)
    }

    composable("common/profile/edit") {
        EditProfileScreen(navController = navController)
    }

    composable("common/settings") {
        AppSettings(navController = navController)
    }
}

/**
 * 인증 관련 라우트 정의
 */
private fun NavGraphBuilder.defineAuthRoutes(navController: NavController, viewModel: AuthViewModel) {
    // 로그인
    composable("auth/login") {
        LoginScreen(navController = navController, viewModel = viewModel)
    }

    // 회원 가입
    composable("auth/signup") {
        SignupScreen(
            navController = navController,
            viewModel = viewModel,
            onBack = { navController.popBackStack() },
            onNext = { _, _ -> navController.navigate("auth/login") }
        )
    }

    // 비밀번호 찾기 Step1
    composable("auth/password/step1") {
        PasswordScreen1(
            onBack = { navController.popBackStack() },
            onNextNavigate = { navController.navigate("auth/password/step2") }
        )
    }

    // 비밀번호 찾기 Step2
    composable("auth/password/step2") {
        PasswordScreen2(
            onBack = { navController.popBackStack() },
            onVerifiedNavigate = { navController.navigate("auth/password/step3") }
        )
    }

    // 비밀번호 찾기 Step3
    composable("auth/password/step3") {
        PasswordScreen3(
            onBack = { navController.popBackStack() },
            onCompleteNavigate = { navController.navigate("auth/login") }
        )
    }
}

/**
 * 일반 사용자 라우트 정의
 */
private fun NavGraphBuilder.defineUserRoutes(navController: NavController, authViewModel: AuthViewModel) {
    // ===============================================================
    // User
    // ===============================================================
    // 홈
    composable("user/home") {
        UserHomeScreen(navController = navController)
    }

    // 카페 검색
    composable("user/search") {
        UserSearchScreen(navController = navController)
    }

    // 마이페이지
    composable("user/mypage") {
        UserMyPageScreen(navController = navController, authViewModel = authViewModel)
    }

    // 즐겨찾기 카페 리스트
    composable("user/favorites") {
        Box(modifier = Modifier.fillMaxSize())
    }

    // 카페 상세 정보
    composable("user/cafe/{cafeId}", listOf(navArgument("cafeId") { type = NavType.StringType })) { backStackEntry ->
        val cafeId = backStackEntry.arguments?.getString("cafeId")
        UserCafeDetailScreen(navController = navController, cafeId = cafeId)
    }
}

/**
 * 관리자(카페 소유자) 라우트 정의
 */
private fun NavGraphBuilder.defineAdminRoutes(navController: NavController, authViewModel: AuthViewModel) {
    // 홈
    composable("admin/home") {
        AdminHomeScreen(navController = navController)
    }

    // 등록헌 카페 리스트
    composable("admin/cafe/list") {
//        StudyCafeListScreen(navController = navController)
    }

    // 마이페이지
    composable("admin/mypage") {
        AdminMyPageScreen(navController = navController, authViewModel = authViewModel)
    }

    // 카페 상세 페이지
    composable("admin/cafe/{cafeId}", listOf(navArgument("cafeId") { type = NavType.StringType })) { backStackEntry ->
        val cafeId = backStackEntry.arguments?.getString("cafeId")
        AdminCafeDetailScreen(navController = navController, cafeId = cafeId)
    }

    // 카페 등록
    composable("admin/cafe/create") {
        CreateCafeScreen(navController = navController)
    }

    // 카페 편집
    composable("admin/cafe/update/{cafeId}", listOf(navArgument("cafeId") { type = NavType.StringType })) { backStackEntry ->
        val cafeId = backStackEntry.arguments?.getString("cafeId")
        UpdateCafeScreen(navController = navController, cafeId = cafeId)
    }

    // 좌석 관리
    composable("admin/seat/management") {
        SeatLayoutScreen(
            onSave = { navController.popBackStack() },
            onBack = { navController.popBackStack() }
        )
    }

    // 현재 좌석
    composable("admin/seat/current") {
        CurrentSeatScreen(
            navController = navController,
            onBack = { navController.popBackStack() }
        )
    }
}

/**
 * 백스택 정책이 적용된 네비게이션 처리
 */
private fun navigateWithBackStackPolicy(navController: NavController, route: String) {
    navController.navigate(route) {
        popUpTo(navController.graph.startDestinationId) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}