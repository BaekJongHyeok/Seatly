package kr.jiyeok.seatly.util

import android.content.Context
import android.content.SharedPreferences

/**
 * Helper class for managing SharedPreferences operations related to authentication.
 * Provides reusable functions for getting and setting auto-login preferences.
 */
object SharedPreferencesHelper {

    private const val PREFS_NAME = "seatly_prefs"
    private const val KEY_AUTO_LOGIN = "auto_login"
    private const val KEY_SAVED_EMAIL = "saved_email"
    private const val KEY_SAVED_PASSWORD = "saved_password"
    private const val KEY_NOTIFICATION_ENABLED = "notification_enabled"

    /**
     * Get SharedPreferences instance
     */
    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    /**
     * Check if auto-login is enabled
     */
    fun isAutoLoginEnabled(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_AUTO_LOGIN, false)
    }

    /**
     * Get saved email for auto-login
     */
    fun getSavedEmail(context: Context): String {
        return getPrefs(context).getString(KEY_SAVED_EMAIL, "") ?: ""
    }

    /**
     * Get saved password for auto-login
     */
    fun getSavedPassword(context: Context): String {
        return getPrefs(context).getString(KEY_SAVED_PASSWORD, "") ?: ""
    }

    /**
     * Save auto-login credentials
     */
    fun saveAutoLoginCredentials(context: Context, email: String, password: String) {
        getPrefs(context).edit().apply {
            putBoolean(KEY_AUTO_LOGIN, true)
            putString(KEY_SAVED_EMAIL, email)
            putString(KEY_SAVED_PASSWORD, password)
            apply()
        }
    }

    /**
     * Clear auto-login credentials
     */
    fun clearAutoLoginCredentials(context: Context) {
        getPrefs(context).edit().apply {
            putBoolean(KEY_AUTO_LOGIN, false)
            remove(KEY_SAVED_EMAIL)
            remove(KEY_SAVED_PASSWORD)
            apply()
        }
    }

    /**
     * Enable auto-login without saving credentials yet
     */
    fun enableAutoLogin(context: Context) {
        getPrefs(context).edit().apply {
            putBoolean(KEY_AUTO_LOGIN, true)
            apply()
        }
    }

    /**
     * 알림 설정 상태 저장
     */
    fun saveNotificationEnabled(context: Context, enabled: Boolean) {
        getPrefs(context).edit().apply {
            putBoolean(KEY_NOTIFICATION_ENABLED, enabled)
            apply()
        }
    }

    /**
     * 알림 설정 상태 가져오기
     * @return 기본값은 true
     */
    fun getNotificationEnabled(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_NOTIFICATION_ENABLED, true)
    }
}
