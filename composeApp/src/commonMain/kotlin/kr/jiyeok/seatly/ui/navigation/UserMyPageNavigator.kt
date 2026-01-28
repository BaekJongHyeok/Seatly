package kr.jiyeok.seatly.ui.navigation

import androidx.navigation.NavController

/**
 * 마이페이지 네비게이션
 */
object MyPageRoutes {
    const val EDIT_PROFILE = "common/profile/edit"
    const val FAVORITES = "user/favorites"
    const val CAFE_DETAIL = "user/cafe"
    const val NOTIFICATIONS = "common/notifications"
    const val APP_SETTINGS = "common/settings"
    const val DASHBOARD = "common/dashboard"
    const val LOGIN = "auth/login"
}

class UserMyPageNavigator(private val navController: NavController) {

    // 프로필 수정 화면으로 이동
    fun navigateToEditProfile() {
        navController.navigate(MyPageRoutes.EDIT_PROFILE)
    }

    // 즐겨찾기 전체보기 화면으로 이동
    fun navigateToFavorites() {
        navController.navigate(MyPageRoutes.FAVORITES)
    }

    // 카페 상세 화면으로 이동
    fun navigateToCafeDetail(cafeId: Long) {
        navController.navigate("${MyPageRoutes.CAFE_DETAIL}/$cafeId")
    }

    // 알림 화면으로 이동
    fun navigateToNotifications() {
        navController.navigate(MyPageRoutes.NOTIFICATIONS)
    }

    // 앱 설정 화면으로 이동
    fun navigateToAppSettings() {
        navController.navigate(MyPageRoutes.APP_SETTINGS)
    }

    // 로그인 화면으로 이동 (백스택 초기화)
    fun navigateToLoginAndClearStack() {
        navController.navigate(MyPageRoutes.LOGIN) {
            popUpTo(MyPageRoutes.DASHBOARD) { inclusive = true }
        }
    }

    // 이전 화면으로 돌아가기
    fun goBack() {
        navController.popBackStack()
    }
}
