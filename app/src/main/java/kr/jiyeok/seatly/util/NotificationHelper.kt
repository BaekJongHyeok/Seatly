package kr.jiyeok.seatly.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import kr.jiyeok.seatly.MainActivity
import kr.jiyeok.seatly.R
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationHelper @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        const val CHANNEL_ID_TIME_PASS = "channel_time_pass_requests"
        const val CHANNEL_NAME_TIME_PASS = "Time Pass Requests"
        const val NOTIFICATION_ID_TIME_PASS = 1001
    }

    /**
     * 알림 채널 생성
     */
    fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID_TIME_PASS,
                CHANNEL_NAME_TIME_PASS,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Receive notifications for time pass requests"
                enableVibration(true)
            }

            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    /**
     * 시간권 요청 알림 표시
     */
    fun showTimePassRequestNotification(title: String, message: String) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        
        val pendingIntent: PendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID_TIME_PASS)
            .setSmallIcon(R.mipmap.ic_launcher) // 앱 아이콘 사용 (또는 커스텀 아이콘)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        
        notificationManager.notify(NOTIFICATION_ID_TIME_PASS, builder.build())
    }
}
