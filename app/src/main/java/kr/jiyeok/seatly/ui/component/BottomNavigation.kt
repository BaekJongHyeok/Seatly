package kr.jiyeok.seatly.ui.components
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun BottomNavigationBar(
    currentRoute: String?,
    onNavigate: (String) -> Unit
) {
    NavigationBar(
        modifier = Modifier
            .fillMaxWidth()
            .height(68.dp)
            .background(Color.White),
        containerColor = Color.White,
        contentColor = Color.Black,
        tonalElevation = 8.dp
    ) {
        NavigationBarItem(
            icon = {
                Icon(
                    imageVector = if(currentRoute == "home") Icons.Filled.Home else Icons.Outlined.Home,
                    contentDescription = "홈",
                    modifier = Modifier.size(24.dp)
                )
            },
            label = {
                Text(
                    "홈",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            },
            selected = currentRoute == "home",
            onClick = { onNavigate("home") },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = Color(0xFFFF6B4A),
                selectedTextColor = Color(0xFFFF6B4A),
                unselectedIconColor = Color(0xFFA0A0A0),
                unselectedTextColor = Color(0xFFA0A0A0),
                indicatorColor = Color.Transparent
            )
        )
        NavigationBarItem(
            icon = {
                Icon(
                    imageVector = if(currentRoute == "search") Icons.Filled.Search else Icons.Outlined.Search,
                    contentDescription = "검색",
                    modifier = Modifier.size(24.dp)
                )
            },
            label = {
                Text(
                    "검색",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            },
            selected = currentRoute == "search",
            onClick = { onNavigate("search") },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = Color(0xFFFF6B4A),
                selectedTextColor = Color(0xFFFF6B4A),
                unselectedIconColor = Color(0xFFA0A0A0),
                unselectedTextColor = Color(0xFFA0A0A0),
                indicatorColor = Color.Transparent
            )
        )
        NavigationBarItem(
            icon = {
                Icon(
                    imageVector = if(currentRoute == "reservation") Icons.Filled.DateRange else Icons.Outlined.DateRange,
                    contentDescription = "예약",
                    modifier = Modifier.size(24.dp)
                )
            },
            label = {
                Text(
                    "예약",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            },
            selected = currentRoute == "reservation",
            onClick = { onNavigate("reservation") },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = Color(0xFFFF6B4A),
                selectedTextColor = Color(0xFFFF6B4A),
                unselectedIconColor = Color(0xFFA0A0A0),
                unselectedTextColor = Color(0xFFA0A0A0),
                indicatorColor = Color.Transparent
            )
        )
        NavigationBarItem(
            icon = {
                Icon(
                    imageVector = if(currentRoute == "mypage") Icons.Filled.Person else Icons.Outlined.Person,
                    contentDescription = "마이페이지",
                    modifier = Modifier.size(24.dp)
                )
            },
            label = {
                Text(
                    "마이페이지",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            },
            selected = currentRoute == "mypage",
            onClick = { onNavigate("mypage") },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = Color(0xFFFF6B4A),
                selectedTextColor = Color(0xFFFF6B4A),
                unselectedIconColor = Color(0xFFA0A0A0),
                unselectedTextColor = Color(0xFFA0A0A0),
                indicatorColor = Color.Transparent
            )
        )
    }
}
