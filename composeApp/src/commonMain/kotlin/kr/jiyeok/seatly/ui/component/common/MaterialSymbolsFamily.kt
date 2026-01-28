package kr.jiyeok.seatly.ui.component.common

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.resources.Font
import seatly.composeapp.generated.resources.Res
import seatly.composeapp.generated.resources.material_symbols_outlined

private val materialSymbolsFamily: FontFamily
    @Composable
    get() = FontFamily(Font(Res.font.material_symbols_outlined))

@Composable
fun MaterialSymbol(
    name: String,
    size: TextUnit = 20.sp,
    tint: Color = Color.Unspecified
) {
    val style = TextStyle(
        fontFamily = materialSymbolsFamily,
        fontSize = size,
        color = tint,
        textAlign = TextAlign.Center
    )
    Text(text = name, style = style)
}
