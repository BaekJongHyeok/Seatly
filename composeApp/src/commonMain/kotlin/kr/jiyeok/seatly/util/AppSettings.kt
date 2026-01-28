package kr.jiyeok.seatly.util

import com.russhwolf.settings.Settings

class AppSettings(private val settings: Settings) {

    companion object {
        private const val KEY_AUTO_LOGIN = "auto_login"
        private const val KEY_SAVED_EMAIL = "saved_email"
        private const val KEY_SAVED_PASSWORD = "saved_password"
        private const val KEY_NOTIFICATION_ENABLED = "notification_enabled"
    }

    fun isAutoLoginEnabled(): Boolean = settings.getBoolean(KEY_AUTO_LOGIN, false)

    fun getSavedEmail(): String = settings.getString(KEY_SAVED_EMAIL, "")

    fun getSavedPassword(): String = settings.getString(KEY_SAVED_PASSWORD, "")

    fun saveAutoLoginCredentials(email: String, password: String) {
        settings.putBoolean(KEY_AUTO_LOGIN, true)
        settings.putString(KEY_SAVED_EMAIL, email)
        settings.putString(KEY_SAVED_PASSWORD, password)
    }

    fun clearAutoLoginCredentials() {
        settings.putBoolean(KEY_AUTO_LOGIN, false)
        settings.remove(KEY_SAVED_EMAIL)
        settings.remove(KEY_SAVED_PASSWORD)
    }

    fun enableAutoLogin() {
        settings.putBoolean(KEY_AUTO_LOGIN, true)
    }

    fun saveNotificationEnabled(enabled: Boolean) {
        settings.putBoolean(KEY_NOTIFICATION_ENABLED, enabled)
    }

    fun getNotificationEnabled(): Boolean = settings.getBoolean(KEY_NOTIFICATION_ENABLED, true)
}
