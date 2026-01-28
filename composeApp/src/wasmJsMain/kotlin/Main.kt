import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import kr.jiyeok.seatly.App
import kr.jiyeok.seatly.di.initKoin
import kotlinx.browser.document

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    // Initialize Koin DI
    initKoin()
    
    // Remove loading indicator
    val loadingElement = document.getElementById("loading")
    loadingElement?.remove()
    
    // Start Compose app
    ComposeViewport(document.getElementById("root")!!) {
        App()
    }
}
