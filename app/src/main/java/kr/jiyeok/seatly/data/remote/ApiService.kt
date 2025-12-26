package kr.jiyeok.seatly.data.remote

import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.PATCH
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.PartMap
import kotlin.jvm.JvmSuppressWildcards
import kr.jiyeok.seatly.data.remote.request.*
import kr.jiyeok.seatly.data.remote.response.*

interface ApiService {

    // Authentication
    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): ApiResponse<LoginResponseDTO>

    @POST("auth/logout")
    suspend fun logout(): ApiResponse<Unit>

    @POST("auth/register")
    suspend fun register(@Body request: RegisterRequest): ApiResponse<UserResponseDto>

    @POST("auth/register/social")
    suspend fun socialRegister(@Body request: SocialRegisterRequest): ApiResponse<UserResponseDto>

    @POST("auth/password/forgot")
    suspend fun requestPasswordReset(@Body request: ForgotPasswordRequest): ApiResponse<Unit>

    @POST("auth/password/verify-code")
    suspend fun verifyPasswordResetCode(@Body request: VerifyCodeRequest): ApiResponse<Unit>

    @POST("auth/password/reset")
    suspend fun resetPassword(@Body request: ResetPasswordRequest): ApiResponse<Unit>

    // User
    @GET("users/me")
    suspend fun getCurrentUser(): ApiResponse<UserResponseDto>

    @GET("user-info")
    suspend fun getUserInfo(): ApiResponse<UserResponseDto>

    @PUT("users/me")
    suspend fun updateUser(@Body request: UpdateUserRequest): ApiResponse<UserResponseDto>

    @PATCH("users/me/password")
    suspend fun changePassword(@Body request: ChangePasswordRequest): ApiResponse<Unit>

    @DELETE("users/me")
    suspend fun deleteAccount(): ApiResponse<Unit>

    @GET("users/me/current-cafe")
    suspend fun getCurrentCafeUsage(): ApiResponse<CurrentCafeUsageDto>

    // Favorites & Recent
    @GET("users/me/favorites")
    suspend fun getFavoriteCafes(
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 20
    ): ApiResponse<PageResponse<StudyCafeSummaryDto>>

    @POST("users/me/favorites/{cafeId}")
    suspend fun addFavoriteCafe(@Path("cafeId") cafeId: Long): ApiResponse<Unit>

    @DELETE("users/me/favorites/{cafeId}")
    suspend fun removeFavoriteCafe(@Path("cafeId") cafeId: Long): ApiResponse<Unit>

    @GET("users/me/recent-cafes")
    suspend fun getRecentCafes(
        @Query("limit") limit: Int = 10
    ): ApiResponse<List<StudyCafeSummaryDto>>

    // Study Cafes
    @GET("study-cafes")
    suspend fun getStudyCafes(
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 20,
        @Query("search") search: String? = null,
        @Query("amenities") amenities: String? = null,
        @Query("openNow") openNow: Boolean? = null,
        @Query("sort") sort: String? = null,
        @Query("lat") lat: Double? = null,
        @Query("lng") lng: Double? = null
    ): ApiResponse<PageResponse<StudyCafeSummaryDto>>

    @GET("study-cafes/{id}/summary")
    suspend fun getCafeSummary(@Path("id") cafeId: Long): ApiResponse<StudyCafeSummaryDto>

    @GET("study-cafes/{id}")
    suspend fun getCafeDetail(@Path("id") cafeId: Long): ApiResponse<StudyCafeDetailDto>

    // Seats
    @GET("study-cafes/{id}/seats")
    suspend fun getCafeSeats(
        @Path("id") cafeId: Long,
        @Query("status") status: String? = null
    ): ApiResponse<List<SeatResponseDto>>

    @POST("study-cafes/{id}/seats/auto-assign")
    suspend fun autoAssignSeat(@Path("id") cafeId: Long): ApiResponse<SessionResponseDto>

    @POST("study-cafes/{id}/seats/reservation")
    suspend fun reserveSeat(
        @Path("id") cafeId: Long,
        @Body request: ReservationRequest
    ): ApiResponse<SessionResponseDto>

    // Sessions
    @POST("sessions/start")
    suspend fun startSession(@Body request: StartSessionRequest): ApiResponse<SessionResponseDto>

    @POST("sessions/{id}/end")
    suspend fun endSession(@Path("id") sessionId: Long): ApiResponse<SessionResponseDto>

    @GET("sessions")
    suspend fun getCurrentSessions(
        @Query("study_cafe_id") studyCafeId: Long? = null
    ): ApiResponse<List<SessionResponseDto>>

    // Admin
    @GET("admin/study-cafes")
    suspend fun getAdminCafes(
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 20,
        @Query("search") search: String? = null
    ): ApiResponse<PageResponse<StudyCafeSummaryDto>>

    @GET("admin/study-cafes/{id}")
    suspend fun getAdminCafeDetail(@Path("id") cafeId: Long): ApiResponse<StudyCafeDetailDto>

    @Multipart
    @POST("admin/study-cafes")
    suspend fun createCafe(
        @PartMap parts: Map<String, @JvmSuppressWildcards RequestBody>,
        @Part images: List<MultipartBody.Part> = emptyList()
    ): ApiResponse<StudyCafeDetailDto>

    @Multipart
    @PUT("admin/study-cafes/{id}")
    suspend fun updateCafe(
        @Path("id") cafeId: Long,
        @PartMap parts: Map<String, @JvmSuppressWildcards RequestBody>,
        @Part images: List<MultipartBody.Part> = emptyList()
    ): ApiResponse<StudyCafeDetailDto>

    @DELETE("admin/study-cafes/{id}")
    suspend fun deleteCafe(@Path("id") cafeId: Long): ApiResponse<Unit>

    @POST("admin/study-cafes/{id}/seats")
    suspend fun addSeat(
        @Path("id") cafeId: Long,
        @Body request: AddSeatRequest
    ): ApiResponse<SeatResponseDto>

    @PUT("admin/study-cafes/{cafeId}/seats/{seatId}")
    suspend fun editSeat(
        @Path("cafeId") cafeId: Long,
        @Path("seatId") seatId: Long,
        @Body request: EditSeatRequest
    ): ApiResponse<SeatResponseDto>

    @DELETE("admin/study-cafes/{cafeId}/seats/{seatId}")
    suspend fun deleteSeat(
        @Path("cafeId") cafeId: Long,
        @Path("seatId") seatId: Long
    ): ApiResponse<Unit>

    @DELETE("admin/sessions/{id}")
    suspend fun forceEndSession(@Path("id") sessionId: Long): ApiResponse<Unit>

    @DELETE("admin/study-cafes/{cafeId}/users/{userId}")
    suspend fun deleteUserFromCafe(
        @Path("cafeId") cafeId: Long,
        @Path("userId") userId: Long
    ): ApiResponse<Unit>
}