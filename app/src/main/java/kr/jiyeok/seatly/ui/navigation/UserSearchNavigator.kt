package kr.jiyeok.seatly.ui.navigation

import androidx.navigation.NavController

/**
 * 사용자 카페 검색 화면 네비게이션
 */
object UserSearchRoutes {
    const val CAFE_DETAIL = "user/cafe"
}

class UserSearchNavigator(private val navController: NavController) {

    // 카페 상세 화면으로 이동
    fun navigateToCafeDetail(cafeId: Long) {
        navController.navigate("${UserSearchRoutes.CAFE_DETAIL}/$cafeId")
    }

    // 이전 화면으로 돌아가기
    fun goBack() {
        navController.popBackStack()
    }
}