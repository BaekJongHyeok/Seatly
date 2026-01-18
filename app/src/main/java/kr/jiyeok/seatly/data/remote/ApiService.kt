package kr.jiyeok.seatly.data.remote

import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.http.*
import kotlin.jvm.JvmSuppressWildcards
import kr.jiyeok.seatly.data.remote.request.*
import kr.jiyeok.seatly.data.remote.response.*

interface ApiService {

    // =====================================================
    // AuthController - /auth
    // =====================================================

    /**
     * POST /auth/login
     * 로그인 성공 시 토큰과 유저 정보 함께 반환
     */
    @POST("auth/login")
    suspend fun login(
        @Body request: LoginRequest
    ): UserInfoSummaryDto

    /**
     * POST /auth/logout
     * 로그아웃
     */
    @POST("auth/logout")
    suspend fun logout(): Unit

    // =====================================================
    // UserController - /user
    // =====================================================

    /**
     * GET /user
     * 로그인한 사용자 정보 조회
     */
    @GET("user")
    suspend fun getUserInfo(): UserInfoSummaryDto

    /**
     * GET /user/study-cafes/favorite
     * 즐겨찾기 카페 아이디 조회
     */
    @GET("user/study-cafes/favorite")
    suspend fun getFavoriteCafes(): List<Long>

    /**
     * GET /user/time-passes
     * 내 시간권 조회
     */
    @GET("user/time-passes")
    suspend fun getMyTimePasses(): List<UserTimePass>

    /**
     * GET /user/sessions
     * 유저의 현재 세션 정보 조회
     */
    @GET("user/sessions")
    suspend fun getCurrentSessions(): List<SessionDto>

    /**
     * PATCH /user
     * 사용자 정보 편집 (이름, 전화번호, 이미지)
     */
    @PATCH("user")
    suspend fun updateUserInfo(
        @Body request: UpdateUserInfoRequest
    ): Unit

    /**
     * DELETE /user
     * 회원탈퇴
     */
    @DELETE("user")
    suspend fun deleteAccount(): Unit

    /**
     * POST /user
     * 회원가입
     */
    @POST("user")
    suspend fun register(
        @Body request: RegisterRequest
    ): Unit

    /**
     * PUT /user/password
     * 비밀번호 변경
     */
    @PUT("user/password")
    suspend fun changePassword(
        @Body request: ChangePasswordRequest
    ): Unit

    // =====================================================
    // UsersController - /users (Admin only)
    // =====================================================

    /**
     * GET /users?studyCafeId={id}
     * 현재 내가 관리하는 스터디카페에 시간권이 남아있는 사용자 목록
     */
    @GET("users")
    suspend fun getUsersInfo(
        @Query("studyCafeId") studyCafeId: Long
    ): List<UserTimePassInfo>

    /**
     * GET /users/{id}
     * 사용자 정보 조회 (관리자)
     */
    @GET("users/{id}")
    suspend fun getUsersInfoById(
        @Path("id") userId: Long
    ): UserInfoSummaryDto

    /**
     * POST /users/{id}/time
     * 관리지가 사용자에게 시간권 추가
     */
    @POST("users/{id}/time")
    suspend fun addUserTimePass(
        @Path("id") userId: Long
    ): Unit

    // =====================================================
    // SessionController - /sessions
    // =====================================================

    /**
     * GET /sessions
     * 세션 목록 조회 (좌석 목록과 결합해서 사용)
     */
    @GET("sessions")
    suspend fun getSessions(
        @Query("studyCafeId") studyCafeId: Long
    ): List<SessionDto>

    /**
     * PATCH /sessions/{id}/start
     * 좌석 이용 시작
     */
    @PATCH("sessions/{id}/start")
    suspend fun startSession(
        @Path("id") sessionId: Long
    ): SessionDto

    /**
     * DELETE /sessions/{id}
     * 좌석 이용 종료 (본인 또는 관리자만 가능)
     */
    @DELETE("sessions/{id}")
    suspend fun endSession(
        @Path("id") sessionId: Long
    ): Unit

    /**
     * POST /sessions/assign?seatId={seatId}
     * 좌석 선택 (수동 할당)
     */
    @POST("sessions/assign")
    suspend fun assignSeat(
        @Query("seatId") seatId: String
    ): SessionDto

    /**
     * POST /sessions/auto-assign?studyCafeId={studyCafeId}
     * 좌석 자동 할당
     */
    @POST("sessions/auto-assign")
    suspend fun autoAssignSeat(
        @Query("studyCafeId") studyCafeId: Long
    ): SessionDto

    // =====================================================
    // StudyCafeController - /study-cafes
    // =====================================================

    /**
     * GET /study-cafes
     * 전체 카페 목록 조회
     */
    @GET("study-cafes")
    suspend fun getStudyCafes(): List<StudyCafeSummaryDto>

    /**
     * GET /study-cafes/{id}
     * 카페 상세 정보 조회
     */
    @GET("study-cafes/{id}")
    suspend fun getCafeDetail(
        @Path("id") cafeId: Long
    ): StudyCafeDetailDto

    /**
     * POST /study-cafes
     * 카페 추가 (관리자)
     */
    @POST("study-cafes")
    suspend fun createCafe(
        @Body request: CreateCafeRequest
    ): Unit

    /**
     * PATCH /study-cafes/{id}
     * 카페 정보 수정 (관리자)
     */
    @PATCH("study-cafes/{id}")
    suspend fun updateCafe(
        @Path("id") cafeId: Long,
        @Body request: UpdateCafeRequest
    ): Unit

    /**
     * DELETE /study-cafes/{id}
     * 카페 삭제 (관리자)
     */
    @DELETE("study-cafes/{id}")
    suspend fun deleteCafe(
        @Path("id") cafeId: Long
    ): Unit

    /**
     * POST /study-cafes/{id}/favorite
     * 카페 즐겨찾기 추가
     */
    @POST("study-cafes/{id}/favorite")
    suspend fun addFavoriteCafe(
        @Path("id") cafeId: Long
    ): Unit

    /**
     * DELETE /study-cafes/{id}/favorite
     * 카페 즐겨찾기 제거
     */
    @DELETE("study-cafes/{id}/favorite")
    suspend fun removeFavoriteCafe(
        @Path("id") cafeId: Long
    ): Unit

    /**
     * GET /study-cafes/{id}/usage
     * 카페 실시간 혼잡도 조회
     */
    @GET("study-cafes/{id}/usage")
    suspend fun getCafeUsage(
        @Path("id") cafeId: Long
    ): UsageDto

    /**
     * DELETE /study-cafes/{id}/users/{userId}/time
     * 회원의 남은 시간권 삭제 (관리자)
     */
    @DELETE("study-cafes/{id}/users/{userId}/time")
    suspend fun deleteUserTimePass(
        @Path("id") cafeId: Long,
        @Path("userId") userId: Long
    ): Unit

    /**
     * GET /study-cafes/admin
     * 관리자가 관리하는 카페 목록
     */
    @GET("study-cafes/admin")
    suspend fun getAdminCafes(): List<StudyCafeSummaryDto>

    // =====================================================
    // SeatController - /study-cafes/{id}/seats
    // =====================================================

    /**
     * GET /study-cafes/{id}/seats
     * 좌석 정보 조회
     */
    @GET("study-cafes/{id}/seats")
    suspend fun getCafeSeats(
        @Path("id") cafeId: Long
    ): List<SeatDto>

    /**
     * POST /study-cafes/{id}/seats
     * 좌석 추가 (관리자)
     * 리스트로 여러 좌석 한번에 추가 가능
     */
    @POST("study-cafes/{id}/seats")
    suspend fun createSeats(
        @Path("id") cafeId: Long,
        @Body request: List<SeatCreate>
    ): Unit

    /**
     * PATCH /study-cafes/{id}/seats
     * 좌석 정보 수정 (관리자)
     * 리스트로 여러 좌석 한번에 수정 가능
     */
    @PATCH("study-cafes/{id}/seats")
    suspend fun updateSeats(
        @Path("id") cafeId: Long,
        @Body request: List<SeatUpdate>
    ): Unit

    /**
     * DELETE /study-cafes/{id}/seats/{seatId}
     * 좌석 삭제 (관리자)
     */
    @DELETE("study-cafes/{id}/seats/{seatId}")
    suspend fun deleteSeat(
        @Path("id") cafeId: Long,
        @Path("seatId") seatId: String
    ): Unit

    // =====================================================
    // ImageController - /images
    // =====================================================

    /**
     * POST /images/upload
     * 이미지 업로드
     */
    @Multipart
    @POST("images/upload")
    suspend fun uploadImage(
        @Part file: MultipartBody.Part
    ): ImageUploadResponse

    /**
     * GET /images/{imageId}
     * 이미지 조회 (다운로드)
     */
    @GET("images/{imageId}")
    suspend fun getImage(
        @Path("imageId") imageId: String
    ): ByteArray

    /**
     * DELETE /images/{imageId}
     * 이미지 삭제
     */
    @DELETE("images/{imageId}")
    suspend fun deleteImage(
        @Path("imageId") imageId: String
    ): Unit
}
