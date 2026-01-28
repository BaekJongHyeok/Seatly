package kr.jiyeok.seatly.ui.component.common

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import kr.jiyeok.seatly.ui.theme.ColorPrimaryOrange
import kr.jiyeok.seatly.ui.theme.ColorTextBlack

@Composable
actual fun ExitBackHandler() {
    var showExitDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current

    // 뒤로가기 이벤트 가로채기
    BackHandler {
        showExitDialog = true
    }

    if (showExitDialog) {
        AlertDialog(
            onDismissRequest = { showExitDialog = false },
            title = {
                Text(text = "앱 종료", color = ColorTextBlack)
            },
            text = {
                Text(text = "앱을 종료하시겠습니까?", color = ColorTextBlack)
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showExitDialog = false
                        (context as? Activity)?.finish()
                    }
                ) {
                    Text(text = "종료", color = ColorPrimaryOrange)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showExitDialog = false }
                ) {
                    Text(text = "취소", color = ColorTextBlack)
                }
            },
            containerColor = androidx.compose.ui.graphics.Color.White
        )
    }
}
