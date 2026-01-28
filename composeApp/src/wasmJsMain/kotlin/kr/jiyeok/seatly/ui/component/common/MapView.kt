package kr.jiyeok.seatly.ui.component.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import kr.jiyeok.seatly.ui.theme.ColorBorderLight
import kr.jiyeok.seatly.ui.theme.ColorTextGray

@Composable
actual fun MapView(
    modifier: Modifier,
    address: String,
    cafeName: String
) {
    Box(
        modifier = modifier.background(ColorBorderLight),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "지도는 모바일 앱에서 확인 가능합니다\n($address)",
            color = ColorTextGray,
            modifier = Modifier.align(Alignment.Center)
        )
    }
}
