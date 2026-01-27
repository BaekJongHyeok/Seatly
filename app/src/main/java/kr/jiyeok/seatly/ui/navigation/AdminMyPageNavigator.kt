package kr.jiyeok.seatly.ui.navigation

import androidx.navigation.NavController

/**
 * 관리자 마이페이지 네비게이션
 */
object AdminMyPageRoutes {
    const val EDIT_PROFILE = "common/profile/edit"
    const val CAFE_DETAIL = "admin/cafe"
    const val NOTIFICATIONS = "common/notifications"
    const val APP_SETTINGS = "common/settings"
    const val DASHBOARD = "common/dashboard"
    const val LOGIN = "auth/login"
}

class AdminMyPageNavigator(private val navController: NavController) {

    // 프로필 수정 화면으로 이동
    fun navigateToEditProfile() {
        navController.navigate(AdminMyPageRoutes.EDIT_PROFILE)
    }

    // 카페 상세 화면으로 이동
    fun navigateToCafeDetail(cafeId: Long) {
        navController.navigate("${AdminMyPageRoutes.CAFE_DETAIL}/$cafeId")
    }

    // 알림 화면으로 이동
    fun navigateToNotifications() {
        navController.navigate(AdminMyPageRoutes.NOTIFICATIONS)
    }

    // 앱 설정 화면으로 이동
    fun navigateToAppSettings() {
        navController.navigate(AdminMyPageRoutes.APP_SETTINGS)
    }

    // 로그인 화면으로 이동 (백스택 초기화)
    fun navigateToLoginAndClearStack() {
        navController.navigate(AdminMyPageRoutes.LOGIN) {
            popUpTo(AdminMyPageRoutes.DASHBOARD) { inclusive = true }
        }
    }

    // 이전 화면으로 돌아가기
    fun goBack() {
        navController.popBackStack()
    }
}