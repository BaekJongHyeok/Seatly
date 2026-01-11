package kr.jiyeok.seatly.ui.component.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// 색상 정의
private val ColorTextBlack = Color(0xFF000000)
private val ColorWhite = Color(0xFFFFFFFF)

/**
 * AppTopBar
 * 앱 내에서 사용하는 공통 규격의 TopBar
 */
@Composable
fun AppTopBar(
    title: String,
    leftContent: (@Composable (() -> Unit))? = null,
    rightContent: (@Composable (() -> Unit))? = null
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(ColorWhite)
            .padding(horizontal = 20.dp, vertical = 12.dp)
    ) {
        // 좌측 IconButton
        if (leftContent != null) {
            Box(
                modifier = Modifier.align(Alignment.CenterStart)
            ) {
                leftContent()
            }
        }

        // 중앙 타이틀
        Text(
            text = title,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = ColorTextBlack,
            modifier = Modifier.align(Alignment.Center)
        )

        // 우측 IconButton
        if (rightContent != null) {
            Box(
                modifier = Modifier.align(Alignment.CenterEnd)
            ) {
                rightContent()
            }
        }
    }
}