package kr.jiyeok.seatly.ui.navigation

import androidx.navigation.NavController

/**
 * 사용자 홈 화면 네비게이션
 */
object UserHomeRoutes {
    const val NOTIFICATIONS = "common/notifications"
    const val SEARCH = "user/search"
    const val FAVORITES = "user/favorites"
    const val CAFE_DETAIL = "user/cafe"
}

class UserHomeNavigator(private val navController: NavController) {

    // 알림 화면으로 이동
    fun navigateToNotifications() {
        navController.navigate(UserHomeRoutes.NOTIFICATIONS)
    }

    // 검색 화면으로 이동
    fun navigateToSearch() {
        navController.navigate(UserHomeRoutes.SEARCH)
    }

    // 즐겨찾기 화면으로 이동
    fun navigateToFavorites() {
        navController.navigate(UserHomeRoutes.FAVORITES)
    }

    // 카페 상세 화면으로 이동
    fun navigateToCafeDetail(cafeId: Long) {
        navController.navigate("${UserHomeRoutes.CAFE_DETAIL}/$cafeId")
    }

    // 현재 화면으로 돌아가기
    fun goBack() {
        navController.popBackStack()
    }
}
