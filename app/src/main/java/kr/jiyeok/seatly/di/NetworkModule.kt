package kr.jiyeok.seatly.di

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import kr.jiyeok.seatly.BuildConfig
import kr.jiyeok.seatly.data.remote.ApiService
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

// ============ Configuration & Constants ============

object NetworkConfig {
    const val PRODUCTION_BASE_URL = "http://168.107.59.168:8080/api/"
    const val DEBUG_BASE_URL = "http://localhost:8080/"

    const val CONNECT_TIMEOUT_SECONDS = 30L
    const val READ_TIMEOUT_SECONDS = 30L
    const val WRITE_TIMEOUT_SECONDS = 30L

    fun getBaseUrl(): String {
        return if (BuildConfig.DEBUG) {
            PRODUCTION_BASE_URL
        } else {
            PRODUCTION_BASE_URL
        }
    }
}

// ============ Cookie Management ============

/**
 * CookieJar 구현체 - 쿠키를 SharedPreferences에 저장
 * 서버에서 Set-Cookie로 보낸 accessToken, refreshToken을 자동으로 저장하고
 * 이후 요청에서 자동으로 쿠키를 포함시킵니다
 */
class PersistentCookieJar(context: Context) : CookieJar {
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val cookieStore = mutableMapOf<String, MutableList<Cookie>>()

    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
        // ✅ 서버에서 Set-Cookie로 보낸 쿠키를 저장
        val host = url.host
        Log.d("CookieJar", "🍪 Saving ${cookies.size} cookies from $host")

        cookieStore[host] = cookies.toMutableList()

        // SharedPreferences에도 저장 (앱 재시작 후에도 유지)
        cookies.forEach { cookie ->
            Log.d("CookieJar", "  ✅ Saved: ${cookie.name} = ${cookie.value.take(20)}...")
            prefs.edit().putString("${host}_${cookie.name}", cookie.value).apply()
        }
    }

    override fun loadForRequest(url: HttpUrl): List<Cookie> {
        val host = url.host

        // 1️⃣ 메모리에 있는 쿠키 로드
        val cookies = cookieStore[host]?.filter { !it.expiresAt.isExpired() }?.toMutableList() ?: mutableListOf()

        // 2️⃣ SharedPreferences에서 쿠키 로드 (앱 재시작 후)
        val accessToken = prefs.getString("${host}_accessToken", null)
        val refreshToken = prefs.getString("${host}_refreshToken", null)
        val sessionId = prefs.getString("${host}_JSESSIONID", null)

        if (accessToken != null && !cookies.any { it.name == "accessToken" }) {
            Log.d("CookieJar", "🍪 Loading accessToken from SharedPreferences")
            cookies.add(createCookie(host, "accessToken", accessToken))
        }
        if (refreshToken != null && !cookies.any { it.name == "refreshToken" }) {
            Log.d("CookieJar", "🍪 Loading refreshToken from SharedPreferences")
            cookies.add(createCookie(host, "refreshToken", refreshToken))
        }
        if (sessionId != null && !cookies.any { it.name == "JSESSIONID" }) {
            Log.d("CookieJar", "🍪 Loading JSESSIONID from SharedPreferences")
            cookies.add(createCookie(host, "JSESSIONID", sessionId))
        }

        if (cookies.isNotEmpty()) {
            Log.d("CookieJar", "📤 Sending ${cookies.size} cookies for $host")
        }

        return cookies
    }

    private fun createCookie(host: String, name: String, value: String): Cookie {
        return Cookie.Builder()
            .name(name)
            .value(value)
            .domain(host)
            .path("/")
            .httpOnly()
            .build()
    }

    fun clearCookies() {
        Log.d("CookieJar", "🗑️ Clearing all cookies")
        prefs.edit().clear().apply()
        cookieStore.clear()
    }

    companion object {
        private const val PREFS_NAME = "seatly_cookies"
    }
}

// ============ Token Management ============

interface TokenProvider {
    fun getAccessToken(): String?
    fun getRefreshToken(): String?
    fun saveTokens(accessToken: String?, refreshToken: String?)
    fun clearTokens()
    fun isTokenExpired(): Boolean
}

class SharedPrefsTokenProvider(context: Context) : TokenProvider {
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    override fun getAccessToken(): String? = prefs.getString(KEY_ACCESS_TOKEN, null)
    override fun getRefreshToken(): String? = prefs.getString(KEY_REFRESH_TOKEN, null)

    override fun saveTokens(accessToken: String?, refreshToken: String?) {
        prefs.edit().apply {
            putString(KEY_ACCESS_TOKEN, accessToken)
            putString(KEY_REFRESH_TOKEN, refreshToken)
            apply()
        }
        Log.d("TokenProvider", "✅ Tokens saved")
    }

    override fun clearTokens() {
        prefs.edit().clear().apply()
        Log.d("TokenProvider", "🗑️ Tokens cleared")
    }

    override fun isTokenExpired(): Boolean {
        return getAccessToken().isNullOrEmpty()
    }

    companion object {
        private const val PREFS_NAME = "seatly_auth_prefs"
        private const val KEY_ACCESS_TOKEN = "access_token"
        private const val KEY_REFRESH_TOKEN = "refresh_token"
    }
}

// ============ Interceptors ============

/**
 * Logging Interceptor
 */
class LoggingInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): okhttp3.Response {
        if (!BuildConfig.DEBUG) {
            return chain.proceed(chain.request())
        }

        val request = chain.request()
        val startTime = System.currentTimeMillis()
        Log.d("OkHttp", "→ ${request.method} ${request.url}")

        val response = chain.proceed(request)
        val duration = System.currentTimeMillis() - startTime
        Log.d("OkHttp", "← ${response.code} (${duration}ms)")

        return response
    }
}

/**
 * Header Interceptor - 모든 요청에 공통 헤더 추가
 */
class HeaderInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): okhttp3.Response {
        val original = chain.request()
        val requestWithHeaders = original.newBuilder()
            .header("Accept", "application/json")
            .header("User-Agent", "Seatly-Android/${BuildConfig.VERSION_NAME}")
            .build()
        return chain.proceed(requestWithHeaders)
    }
}

// ============ DI Module ============

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideCookieJar(@ApplicationContext context: Context): CookieJar {
        return PersistentCookieJar(context)
    }

    @Provides
    @Singleton
    fun provideTokenProvider(@ApplicationContext context: Context): TokenProvider {
        return SharedPrefsTokenProvider(context)
    }

    @Provides
    @Singleton
    fun provideHeaderInterceptor(): HeaderInterceptor {
        return HeaderInterceptor()
    }

    @Provides
    @Singleton
    fun provideLoggingInterceptor(): HttpLoggingInterceptor {
        return HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.BODY
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
        }
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(
        cookieJar: CookieJar,
        headerInterceptor: HeaderInterceptor,
        loggingInterceptor: HttpLoggingInterceptor
    ): OkHttpClient {
        return OkHttpClient.Builder()
            .cookieJar(cookieJar)  // ✅ CookieJar 추가 - 자동으로 쿠키 관리
            .addInterceptor(headerInterceptor)
            .addInterceptor(loggingInterceptor)
            .connectTimeout(NetworkConfig.CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .readTimeout(NetworkConfig.READ_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .writeTimeout(NetworkConfig.WRITE_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl(NetworkConfig.getBaseUrl())
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    fun provideApiService(retrofit: Retrofit): ApiService {
        return retrofit.create(ApiService::class.java)
    }
}

// ============ Helper Extension ============

/**
 * 쿠키 만료 확인
 */
private fun Long.isExpired(): Boolean {
    return this < System.currentTimeMillis()
}
