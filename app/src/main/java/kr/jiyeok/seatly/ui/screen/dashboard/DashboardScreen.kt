package kr.jiyeok.seatly.ui.screen.dashboard

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import kotlinx.coroutines.delay
import kr.jiyeok.seatly.R

@Composable
fun DashboardScreen(navController: NavController) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White),
        contentAlignment = Alignment.Center
    ) {
        // 오렌지 원형 배경
        Box(
            modifier = Modifier
                .size(120.dp)
                .clip(CircleShape)
                .background(
                    color = Color(0xFFFF6B4A),  // 오렌지색
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            // 커피컵 이미지
            Image(
                painter = painterResource(id = R.drawable.icon_seatly),
                contentDescription = "Seatly Logo",
                modifier = Modifier.size(80.dp)
            )
        }
    }

    // 2초 후 로그인 화면으로 이동
    LaunchedEffect(Unit) {
        delay(2000)
        navController.navigate("login") {
            popUpTo("dashboard") { inclusive = true }
        }
    }
}
