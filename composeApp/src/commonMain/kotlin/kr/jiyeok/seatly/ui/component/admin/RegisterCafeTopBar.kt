package kr.jiyeok.seatly.ui.component.admin

import androidx.compose.foundation.layout.*
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import kr.jiyeok.seatly.ui.component.common.MaterialSymbol

@Composable
fun RegisterCafeTopBar(
    navController: NavController,
    title: String = "카페 등록",
    titleFontSize: TextUnit = 18.sp,
    titleColor: Color = Color.Unspecified,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    spacerSizeDp: Int = 36,
    backIconSizeSp: TextUnit = 24.sp
) {
    Box(modifier = modifier.fillMaxWidth()) {
        IconButton(
            onClick = { (onBack ?: { navController.popBackStack() }).invoke() },
            modifier = Modifier.align(Alignment.CenterStart)
        ) {
            MaterialSymbol(name = "arrow_back", size = backIconSizeSp)
        }
        Text(
            text = title,
            fontSize = titleFontSize,
            fontWeight = FontWeight.SemiBold,
            color = titleColor,
            modifier = Modifier.align(Alignment.Center)
        )
        // Keep a spacer to match previous layout balance
        Spacer(modifier = Modifier.size(spacerSizeDp.dp))
    }
}
