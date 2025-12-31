package kr.jiyeok.seatly.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import java.util.concurrent.TimeUnit
import javax.inject.Named
import javax.inject.Singleton

/**
 * DebugNetworkModule
 *
 * - debug variant 에서 auth_okhttp 바인딩을 덮어씁니다.
 * - 기존 NetworkModule에서 제공하는 HttpLoggingInterceptor를 재사용하도록 설계했습니다.
 * - 이 파일은 반드시 src/debug/java/... 에만 위치시켜야 합니다.
 */
@Module
@InstallIn(SingletonComponent::class)
object DebugNetworkModule {

    @Provides
    @Singleton
    fun provideDebugMockInterceptor(): DebugMockInterceptor = DebugMockInterceptor()

    @Provides
    @Singleton
    @Named("auth_okhttp")
    fun provideDebugAuthOkHttpClient(
        mockInterceptor: DebugMockInterceptor,
        logging: HttpLoggingInterceptor // main NetworkModule의 제공자를 재사용
    ): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(mockInterceptor)   // 가장 먼저 추가해서 우선 가로챔
            .addInterceptor(logging)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(35, TimeUnit.SECONDS)
            .writeTimeout(35, TimeUnit.SECONDS)
            .build()
    }
}