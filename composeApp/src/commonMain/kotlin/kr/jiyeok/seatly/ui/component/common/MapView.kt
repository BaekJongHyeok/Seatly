package kr.jiyeok.seatly.ui.component.common

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
expect fun MapView(
    modifier: Modifier,
    address: String,
    cafeName: String
)
