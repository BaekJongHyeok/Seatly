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

    // =========== 로그인 ===========
    /**
     * 로그인
     */
    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): ApiResponse<LoginResponseDTO>


    // =========== 유저 관련 ===========
    /**
     * 사용자 기본 정보 + 스터디 카페 목록
     */
    @GET("user-info")
    suspend fun getUserInfo(): ApiResponse<UserResponseDto>


    // =========== 카페 관련 ===========
    /**
     * 스터디 카페 목록
     */
    @GET("study-cafes")
    suspend fun getStudyCafes(): ApiResponse<List<StudyCafeResponseDto>>

    /**
     * 스터디 카페 좌석 목록
     */
    @GET("study-cafes/{id}/seats")
    suspend fun getCafeSeats(@Path("id") cafeId: Long): ApiResponse<List<SeatResponseDto>>


    // =========== 좌석 관련 ===========
    /**
     * 좌석 자동 배정
     */
    @POST("study-cafes/{id}/seats/auto-assign")
    suspend fun autoAssignSeat(@Path("id") cafeId: Long): ApiResponse<SessionResponseDto>

    /**
     * 좌석 선택
     */
    @POST("study-cafes/{id}/seats/reservation")
    suspend fun reserveSeat(
        @Path("id") cafeId: Long,
        @Body request: ReservationRequest
    ): ApiResponse<SessionResponseDto>

    /**
     * 현재 좌석 이용 현황 조회
     * @param studyCafeId 스터디 카페 아이디
     */
    @GET("sessions")
    suspend fun getCurrentSessions(
        @Query("study_cafe_id") studyCafeId: Long? = null
    ): ApiResponse<List<SessionResponseDto>>


    // =========== 세션 관련 ===========
    /**
     * 이용 시작
     */
    @POST("sessions/start")
    suspend fun startSession(@Body request: StartSessionRequest): ApiResponse<SessionResponseDto>

    /**
     * 이용 종료
     */
    @POST("sessions/{id}/end")
    suspend fun endSession(@Path("id") sessionId: Long): ApiResponse<SessionResponseDto>


    // =========== 관리자 전용 ===========
    /**
     * 좌석 추가
     * @param request 좌석 정보
     */
    @POST("study-cafes/{id}/seats")
    suspend fun addSeat(
        @Path("id") cafeId: Long,
        @Body request: AddSeatRequest
    ): ApiResponse<SeatResponseDto>

    /**
     * 좌석 삭제
     */
    @DELETE("study-cafes/{cafeId}/seats/{seatId}")
    suspend fun deleteSeat(
        @Path("cafeId") cafeId: Long,
        @Path("seatId") seatId: Long
    ): ApiResponse<Unit>

    /**
     * 강제 종료
     */
    @DELETE("sessions/{id}")
    suspend fun forceEndSession(@Path("id") sessionId: Long): ApiResponse<Unit>

    /**
     * 회원 탈퇴
     */
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
