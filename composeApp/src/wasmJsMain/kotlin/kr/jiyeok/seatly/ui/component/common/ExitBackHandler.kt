package kr.jiyeok.seatly.ui.component.common

import androidx.compose.runtime.Composable

@Composable
actual fun ExitBackHandler() {
    // Web doesn't have a systemic back button like Android. 
    // Usually handled by browser's back button which navigates history.
    // For a demo, we can leave this empty or implement a custom behavior if needed.
}
