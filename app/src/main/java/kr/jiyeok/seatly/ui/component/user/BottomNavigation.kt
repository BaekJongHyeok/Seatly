package kr.jiyeok.seatly.ui.component.user

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun BottomNavigationBar(
    currentRoute: String?,
    onNavigate: (String) -> Unit
) {
    // 둥근 모서리와 그림자를 위한 컨테이너 (Floating Style)
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, bottom = 16.dp) // 화면에서 띄우기
            .height(72.dp)
            .shadow(
                elevation = 12.dp,
                shape = RoundedCornerShape(36.dp),
                spotColor = Color.Black.copy(alpha = 0.1f)
            )
            .clip(RoundedCornerShape(36.dp)),
        color = Color.White,
        tonalElevation = 8.dp
    ) {
        NavigationBar(
            modifier = Modifier.fillMaxWidth(),
            containerColor = Color.White,
            contentColor = Color.Black,
            tonalElevation = 0.dp
        ) {
            // Home Item
            NavigationBarItem(
                selected = currentRoute == "home",
                onClick = { onNavigate("home") },
                icon = {
                    TabIconWithIndicator(
                        isSelected = currentRoute == "home",
                        selectedIcon = Icons.Filled.Home,
                        unselectedIcon = Icons.Outlined.Home,
                        description = "홈"
                    )
                },
                label = { TabLabel("홈") },
                colors = customNavColors()
            )

            // Search Item
            NavigationBarItem(
                selected = currentRoute == "search",
                onClick = { onNavigate("search") },
                icon = {
                    TabIconWithIndicator(
                        isSelected = currentRoute == "search",
                        selectedIcon = Icons.Filled.Search,
                        unselectedIcon = Icons.Outlined.Search,
                        description = "검색"
                    )
                },
                label = { TabLabel("검색") },
                colors = customNavColors()
            )

            // MyPage Item
            NavigationBarItem(
                selected = currentRoute == "mypage",
                onClick = { onNavigate("mypage") },
                icon = {
                    TabIconWithIndicator(
                        isSelected = currentRoute == "mypage",
                        selectedIcon = Icons.Filled.Person,
                        unselectedIcon = Icons.Outlined.Person,
                        description = "마이페이지"
                    )
                },
                label = { TabLabel("마이페이지") },
                colors = customNavColors()
            )
        }
    }
}

// ------------------------------------------------------------
// ✨ Helper Composables (재사용 및 스타일 통일)
// ------------------------------------------------------------

/**
 * 아이콘 위에 활성화 바(Indicator Bar)를 표시하는 컴포저블
 */
@Composable
private fun TabIconWithIndicator(
    isSelected: Boolean,
    selectedIcon: ImageVector,
    unselectedIcon: ImageVector,
    description: String
) {
    Box(contentAlignment = Alignment.Center) {
        // 활성화 상태일 때 상단에 바 표시
        if (isSelected) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .offset(y = (-14).dp) // 아이콘 위로 위치 조정
                    .width(32.dp) // 바 너비
                    .height(5.dp) // 바 높이
                    .clip(RoundedCornerShape(4.dp)) // 바 모서리 둥글게
                    .background(Color(0xFFFF6B4A)) // 테마 색상
            )
        }

        Icon(
            imageVector = if (isSelected) selectedIcon else unselectedIcon,
            contentDescription = description,
            modifier = Modifier.size(24.dp)
        )
    }
}

@Composable
private fun TabLabel(text: String) {
    Text(
        text = text,
        fontSize = 11.sp,
        fontWeight = FontWeight.Medium
    )
}

@Composable
private fun customNavColors() = NavigationBarItemDefaults.colors(
    selectedIconColor = Color(0xFFFF6B4A),
    selectedTextColor = Color(0xFFFF6B4A),
    unselectedIconColor = Color(0xFFA0A0A0),
    unselectedTextColor = Color(0xFFA0A0A0),
    indicatorColor = Color.Transparent // 기본 둥근 배경 제거 (중요)
)
