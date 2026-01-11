package kr.jiyeok.seatly.ui.component.common

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

data class NavItem(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

@Composable
fun BottomNavigationBar(
    currentRoute: String?,
    onNavigate: (String) -> Unit,
    isAdmin: Boolean
) {
    val navItems = if (isAdmin) {
        listOf(
            NavItem("admin/home", "홈", Icons.Filled.Home, Icons.Outlined.Home),
            NavItem("admin/cafe/list", "카페 관리", Icons.Filled.Search, Icons.Outlined.Search),
            NavItem("admin/mypage", "마이페이지", Icons.Filled.Person, Icons.Outlined.Person)
        )
    } else {
        listOf(
            NavItem("user/home", "홈", Icons.Filled.Home, Icons.Outlined.Home),
            NavItem("user/search", "검색", Icons.Filled.Search, Icons.Outlined.Search),
            NavItem("user/mypage", "마이페이지", Icons.Filled.Person, Icons.Outlined.Person)
        )
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
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
            navItems.forEach { item ->
                NavigationBarItem(
                    selected = currentRoute == item.route,
                    onClick = { onNavigate(item.route) },
                    icon = {
                        TabIconWithIndicator(
                            isSelected = currentRoute == item.route,
                            selectedIcon = item.selectedIcon,
                            unselectedIcon = item.unselectedIcon,
                            description = item.label
                        )
                    },
                    label = { TabLabel(item.label) },
                    colors = customNavColors()
                )
            }
        }
    }
}

@Composable
private fun TabIconWithIndicator(
    isSelected: Boolean,
    selectedIcon: ImageVector,
    unselectedIcon: ImageVector,
    description: String
) {
    Box(contentAlignment = Alignment.Center) {
        if (isSelected) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .offset(y = (-14).dp)
                    .width(32.dp)
                    .height(5.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color(0xFFFF6B4A))
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
    indicatorColor = Color.Transparent
)
