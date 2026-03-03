package kr.jiyeok.seatly

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import dagger.hilt.android.AndroidEntryPoint
import kr.jiyeok.seatly.ui.theme.SeatlyTheme
import kr.jiyeok.seatly.ui.navigation.RootNavigation
import kr.jiyeok.seatly.util.NotificationHelper
import javax.inject.Inject
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject
    lateinit var notificationHelper: NotificationHelper

    override fun attachBaseContext(newBase: android.content.Context) {
        super.attachBaseContext(kr.jiyeok.seatly.util.LocaleHelper.onAttach(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // 알림 채널 생성
        notificationHelper.createNotificationChannel()

        // 알림 권한 요청 (Android 13+)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            requestPermissions(
                arrayOf(android.Manifest.permission.POST_NOTIFICATIONS),
                100
            )
        }

        setContent {
            SeatlyTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    RootNavigation()
                }
            }
        }
    }
}
