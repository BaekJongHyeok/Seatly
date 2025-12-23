package kr.jiyeok.seatly.ui.component

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import kr.jiyeok.seatly.R

// 사용법: res/font/material_symbols_outlined.ttf 를 넣고 R.font.material_symbols_outlined 로 참조하세요.
private val MaterialSymbolsFamily = FontFamily(
    Font(R.font.material_symbols_outlined)
)

/**
 * Render a Material Symbol glyph by ligature name using the Material Symbols Outlined font.
 *
 * Note: This simple implementation avoids fontVariationSettings for maximum compatibility.
 * If you want variable axes (FILL/wght), we can add conditional support later depending on your Compose version.
 */
@Composable
fun MaterialSymbol(
    name: String,
    size: TextUnit = 20.sp,
    tint: Color = Color.Unspecified
) {
    val style = TextStyle(
        fontFamily = MaterialSymbolsFamily,
        fontSize = size,
        color = tint,
        textAlign = TextAlign.Center
    )
    Text(text = name, style = style)
}