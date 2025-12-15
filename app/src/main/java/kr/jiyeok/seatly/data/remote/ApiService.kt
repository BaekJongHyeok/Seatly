package kr.jiyeok.seatly.data.remote

import kr.jiyeok.seatly.data.remote.request.ReservationRequest
import kr.jiyeok.seatly.data.remote.request.StartSessionRequest
import kr.jiyeok.seatly.data.remote.response.LoginResponseDTO
import kr.jiyeok.seatly.data.remote.response.SeatResponseDto
import kr.jiyeok.seatly.data.remote.response.SessionResponseDto
import kr.jiyeok.seatly.data.remote.response.StudyCafeResponseDto
import kr.jiyeok.seatly.data.remote.response.UserResponseDto
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface ApiService {
    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): ApiResponse<LoginResponseDTO>

    @GET("user-info")
    suspend fun getUserInfo(): ApiResponse<UserResponseDto>

    @GET("study-cafes")
    suspend fun getStudyCafes(): ApiResponse<List<StudyCafeResponseDto>>

    @GET("study-cafes/{id}/seats")
    suspend fun getCafeSeats(@Path("id") cafeId: Long): ApiResponse<List<SeatResponseDto>>

    @POST("study-cafes/{id}/seats/auto-assign")
    suspend fun autoAssignSeat(@Path("id") cafeId: Long): ApiResponse<SessionResponseDto>

    @POST("study-cafes/{id}/seats/reservation")
    suspend fun reserveSeat(
        @Path("id") cafeId: Long,
        @Body request: ReservationRequest
    ): ApiResponse<SessionResponseDto>

    @GET("sessions")
    suspend fun getCurrentSessions(
        @Query("study_cafe_id") studyCafeId: Long? = null
    ): ApiResponse<List<SessionResponseDto>>

    @POST("sessions/start")
    suspend fun startSession(@Body request: StartSessionRequest): ApiResponse<SessionResponseDto>

    @POST("sessions/{id}/end")
    suspend fun endSession(@Path("id") sessionId: Long): ApiResponse<SessionResponseDto>

    @DELETE("study-cafes/{cafeId}/seats/{seatId}")
    suspend fun deleteSeat(
        @Path("cafeId") cafeId: Long,
        @Path("seatId") seatId: Long
    ): ApiResponse<Unit>

    @POST("study-cafes/{id}/seats")
    suspend fun addSeat(
        @Path("id") cafeId: Long,
        @Body request: AddSeatRequest
    ): ApiResponse<SeatResponseDto>

    @DELETE("sessions/{id}")
    suspend fun forceEndSession(@Path("id") sessionId: Long): ApiResponse<Unit>

    @DELETE("study-cafes/{cafeId}/users/{userId}")
    suspend fun deleteUser(
        @Path("cafeId") cafeId: Long,
        @Path("userId") userId: Long
    ): ApiResponse<Unit>
}

data class ApiResponse<T>(
    val success: Boolean,
    val message: String,
    val data: T?
)

data class LoginRequest(
    val email: String,
    val password: String
)

data class AddSeatRequest(
    val seat_number: Int,
    val position: String,
    val type: String? = null
)
