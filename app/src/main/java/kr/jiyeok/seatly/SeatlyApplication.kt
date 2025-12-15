package kr.jiyeok.seatly

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class SeatlyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
    }
}
