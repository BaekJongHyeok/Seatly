package kr.jiyeok.seatly.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chair
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val Primary = Color(0xFFe95321)
private val Unselected = Color(0xFFA0A0A0)
private val Background = Color(0xFFFFFFFF)

/**
 * Owner bottom navigation: only shows
 * - 대시보드 (dashboard)
 * - 좌석 관리 (seat_management)
 * - 마이페이   (payments)
 */
@Composable
fun OwnerBottomNavigationBar(
    currentRoute: String?,
    onNavigate: (String) -> Unit
) {
    NavigationBar(
        modifier = Modifier
            .fillMaxWidth()
            .height(68.dp)
            .background(Background),
        containerColor = Background,
        contentColor = Color.Black,
        tonalElevation = 8.dp
    ) {
        // Dashboard
        NavigationBarItem(
            icon = { Icon(imageVector = Icons.Default.Home, contentDescription = "대시보드", modifier = Modifier.size(24.dp)) },
            label = { Text("대시보드", fontSize = 11.sp) },
            selected = currentRoute == "dashboard" || currentRoute == "home",
            onClick = { onNavigate("dashboard") },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = Primary,
                selectedTextColor = Primary,
                unselectedIconColor = Unselected,
                unselectedTextColor = Unselected,
                indicatorColor = Color.Transparent
            )
        )

        // Seat management
        NavigationBarItem(
            icon = { Icon(imageVector = Icons.Default.Chair, contentDescription = "좌석 관리", modifier = Modifier.size(24.dp)) },
            label = { Text("좌석 관리", fontSize = 11.sp) },
            selected = currentRoute == "seat_management" || currentRoute == "current_seat",
            onClick = { onNavigate("seat_management") },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = Primary,
                selectedTextColor = Primary,
                unselectedIconColor = Unselected,
                unselectedTextColor = Unselected,
                indicatorColor = Color.Transparent
            )
        )

        // MyPay (payments)
        NavigationBarItem(
            icon = { Icon(imageVector = Icons.Default.CreditCard, contentDescription = "마이페이", modifier = Modifier.size(24.dp)) },
            label = { Text("마이페이", fontSize = 11.sp) },
            selected = currentRoute == "payments" || currentRoute == "mypay",
            onClick = { onNavigate("payments") },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = Primary,
                selectedTextColor = Primary,
                unselectedIconColor = Unselected,
                unselectedTextColor = Unselected,
                indicatorColor = Color.Transparent
            )
        )
    }
}