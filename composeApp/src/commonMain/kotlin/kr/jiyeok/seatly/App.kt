package kr.jiyeok.seatly

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import kr.jiyeok.seatly.ui.navigation.RootNavigation
import kr.jiyeok.seatly.ui.theme.SeatlyTheme

@Composable
fun App() {
    SeatlyTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            RootNavigation()
        }
    }
}
