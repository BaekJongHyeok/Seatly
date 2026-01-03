package kr.jiyeok.seatly.di

import android.content.Context
import android.content.SharedPreferences
import kr.jiyeok.seatly.BuildConfig
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.hilt.android.qualifiers.ApplicationContext
import kr.jiyeok.seatly.data.remote.ApiService
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

// DebugMockInterceptor 직접 import
// (만약 DebugMockInterceptor가 debug 소스셋에만 있다면, main 소스셋에서는 참조 불가능할 수 있음.
//  이 경우 현재 파일이 main에 있다면 리플렉션 유지, debug에 있다면 import 가능.
//  하지만 사용자 상황상 파일들이 섞여 있는 것으로 보이므로, 안전하게 아래와 같이 수정 권장)

interface TokenProvider {
    fun getAccessToken(): String?
    fun getRefreshToken(): String?
    fun saveTokens(accessToken: String?, refreshToken: String?)
    fun clearTokens()
}

class SharedPrefsTokenProvider(context: Context) : TokenProvider {
    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    override fun getAccessToken(): String? = prefs.getString(KEY_ACCESS_TOKEN, null)
    override fun getRefreshToken(): String? = prefs.getString(KEY_REFRESH_TOKEN, null)

    override fun saveTokens(accessToken: String?, refreshToken: String?) {
        prefs.edit()
            .putString(KEY_ACCESS_TOKEN, accessToken)
            .putString(KEY_REFRESH_TOKEN, refreshToken)
            .apply()
    }

    override fun clearTokens() {
        prefs.edit().clear().apply()
    }

    companion object {
        private const val PREFS_NAME = "seatly_auth_prefs"
        private const val KEY_ACCESS_TOKEN = "access_token"
        private const val KEY_REFRESH_TOKEN = "refresh_token"
    }
}

class AuthInterceptor(
    private val tokenProvider: TokenProvider
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): okhttp3.Response {
        val original: Request = chain.request()
        val accessToken = tokenProvider.getAccessToken()

        val request = if (!accessToken.isNullOrBlank()) {
            original.newBuilder()
                .header("Authorization", "Bearer $accessToken")
                .build()
        } else {
            original
        }
        return chain.proceed(request)
    }
}

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    private val BASE_URL: String = try {
        BuildConfig.SEATLY_BASE_URL
    } catch (t: Throwable) {
        "https://api.seatly.example/"
    }

    @Provides
    @Singleton
    fun provideTokenProvider(@ApplicationContext context: Context): TokenProvider {
        return SharedPrefsTokenProvider(context)
    }

    @Provides
    @Singleton
    fun provideAuthInterceptor(tokenProvider: TokenProvider): AuthInterceptor =
        AuthInterceptor(tokenProvider)

    @Provides
    @Singleton
    fun provideLoggingInterceptor(): HttpLoggingInterceptor {
        val logger = HttpLoggingInterceptor()
        logger.level = if (BuildConfig.DEBUG) {
            HttpLoggingInterceptor.Level.BODY
        } else {
            HttpLoggingInterceptor.Level.NONE
        }
        return logger
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(
        authInterceptor: AuthInterceptor,
        loggingInterceptor: HttpLoggingInterceptor
    ): OkHttpClient {
        val builder = OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(loggingInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)

        // ★ 수정됨: 리플렉션 대신 직접 추가 (권장)
        // 만약 컴파일 에러(Unresolved reference)가 난다면, DebugMockInterceptor 파일이
        // src/main/java가 아닌 src/debug/java에 있어서 그럴 수 있습니다.
        // 그럴 경우엔 기존의 리플렉션 코드를 사용하되 패키지명/클래스명을 정확히 확인해야 합니다.
        // 여기서는 파일들이 한곳에 있다고 가정하고 직접 추가합니다.
        if (BuildConfig.DEBUG) {
            builder.addInterceptor(DebugMockInterceptor())
        }

        return builder.build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    fun provideApiService(retrofit: Retrofit): ApiService =
        retrofit.create(ApiService::class.java)
}
