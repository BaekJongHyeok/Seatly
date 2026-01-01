package kr.jiyeok.seatly.data.repository

import kr.jiyeok.seatly.data.remote.request.*
import kr.jiyeok.seatly.data.remote.response.*

/**
 * Seatly 앱의 모든 API 호출을 관리하는 Repository 인터페이스
 */
interface SeatlyRepository {

    // =====================================================
    // Authentication
    // =====================================================

    /**
     * POST /auth/login
     * 로그인
     */
    suspend fun login(request: LoginRequest): ApiResult<LoginResponse>

    /**
     * POST /auth/logout
     * 로그아웃
     */
    suspend fun logout(): ApiResult<Unit>

    /**
     * POST /auth/register
     * 회원가입
     */
    suspend fun register(request: RegisterRequest): ApiResult<Unit>

    /**
     * POST /auth/refresh
     * 토큰 리프레시
     */
    suspend fun refreshToken(request: RefreshTokenRequest): ApiResult<TokenResponse>

    /**
     * POST /auth/social-register (보류)
     * 소셜 로그인 회원가입
     */
    suspend fun socialRegister(request: SocialRegisterRequest): ApiResult<Unit>

    /**
     * POST /auth/forgot-password (보류)
     * 비밀번호 찾기 - 보안 코드 전송
     */
    suspend fun forgotPassword(request: ForgotPasswordRequest): ApiResult<Unit>

    /**
     * POST /auth/verify-code (보류)
     * 비밀번호 찾기 - 보안 코드 검증
     */
    suspend fun verifyCode(request: VerifyCodeRequest): ApiResult<Unit>

    // =====================================================
    // User
    // =====================================================

    /**
     * GET /user
     * 로그인한 사용자 정보 조회
     */
    suspend fun getUserInfo(): ApiResult<UserResponseDto>

    /**
     * PATCH /user
     * 사용자 정보 편집
     */
    suspend fun updateUserInfo(request: UpdateUserRequest): ApiResult<UserResponseDto>

    /**
     * DELETE /user
     * 회원탈퇴
     */
    suspend fun deleteAccount(): ApiResult<Unit>

    /**
     * PUT /user/{id}/password
     * 비밀번호 변경
     */
    suspend fun changePassword(userId: Long, request: ChangePasswordRequest): ApiResult<Unit>

    // =====================================================
    // Users (Admin)
    // =====================================================

    /**
     * GET /users?studyCafeId={id}
     * 현재 내가 관리하는 스터디카페에 시간권이 남아있는 사용자 목록
     */
    suspend fun getUsersWithTimePass(studyCafeId: Long): ApiResult<List<UserTimePassInfo>>

    /**
     * GET /users/{id}
     * 사용자 정보 조회 (관리자)
     */
    suspend fun getUserInfoAdmin(userId: Long): ApiResult<UserResponseDtoAdmin>

    // =====================================================
    // Sessions
    // =====================================================

    /**
     * GET /sessions
     * 세션 목록 조회
     */
    suspend fun getSessions(): ApiResult<List<SessionDto>>

    /**
     * PATCH /sessions/{id}/start
     * 좌석 이용 시작
     */
    suspend fun startSession(sessionId: Long): ApiResult<SessionDto>

    /**
     * DELETE /sessions/{id}
     * 좌석 이용 종료
     */
    suspend fun endSession(sessionId: Long): ApiResult<Unit>

    /**
     * POST /sessions/assign?seatId={seatId}
     * 좌석 선택 (수동 할당)
     */
    suspend fun assignSeat(seatId: String): ApiResult<SessionDto>

    /**
     * POST /sessions/auto-assign?seatId={seatId}
     * 좌석 자동 할당
     */
    suspend fun autoAssignSeat(seatId: String): ApiResult<SessionDto>

    // =====================================================
    // Study Cafes
    // =====================================================

    /**
     * GET /study-cafes
     * 전체 카페 목록 조회
     */
    suspend fun getStudyCafes(
        page: Int = 0,
        size: Int = 20
    ): ApiResult<PageResponse<StudyCafeSummaryDto>>

    /**
     * GET /study-cafes/{id}
     * 카페 상세 정보 조회
     */
    suspend fun getCafeDetail(cafeId: Long): ApiResult<StudyCafeDetailDto>

    /**
     * POST /study-cafes
     * 카페 추가 (관리자)
     */
    suspend fun createCafe(request: StudyCafeDetailPost): ApiResult<StudyCafeDetailDto>

    /**
     * PATCH /study-cafes/{id}
     * 카페 정보 수정 (관리자)
     */
    suspend fun updateCafe(
        cafeId: Long,
        request: StudyCafeDetailPost
    ): ApiResult<StudyCafeDetailDto>

    /**
     * DELETE /study-cafes/{id}
     * 카페 삭제 (관리자)
     */
    suspend fun deleteCafe(cafeId: Long): ApiResult<Unit>

    /**
     * POST /study-cafes/{id}/favorite
     * 카페 즐겨찾기 추가
     */
    suspend fun addFavoriteCafe(cafeId: Long): ApiResult<Unit>

    /**
     * DELETE /study-cafes/{id}/favorite
     * 카페 즐겨찾기 제거
     */
    suspend fun removeFavoriteCafe(cafeId: Long): ApiResult<Unit>

    /**
     * GET /study-cafes/{id}/usage
     * 카페 실시간 혼잡도 조회
     */
    suspend fun getCafeUsage(cafeId: Long): ApiResult<UsageDto>

    /**
     * DELETE /study-cafes/{id}/users/{userId}/time
     * 회원의 남은 시간권 삭제 (관리자)
     */
    suspend fun deleteUserTimePass(
        cafeId: Long,
        userId: Long
    ): ApiResult<Unit>

    /**
     * GET /study-cafes/admin
     * 관리자가 관리하는 카페 목록
     */
    suspend fun getAdminCafes(
        page: Int = 0,
        size: Int = 20
    ): ApiResult<PageResponse<StudyCafeSummaryDto>>

    // =====================================================
    // Seats
    // =====================================================

    /**
     * GET /study-cafes/{id}/seats
     * 좌석 정보 조회
     */
    suspend fun getCafeSeats(cafeId: Long): ApiResult<GetSeatResponse>

    /**
     * POST /study-cafes/{id}/seats
     * 좌석 추가 (관리자, 리스트로 여러 개 한번에)
     */
    suspend fun createSeats(
        cafeId: Long,
        request: CreateSeatRequest
    ): ApiResult<GetSeatResponse>

    /**
     * PATCH /study-cafes/{id}/seats
     * 좌석 정보 수정 (관리자, 리스트로 여러 개 한번에)
     */
    suspend fun updateSeats(
        cafeId: Long,
        request: UpdateSeatRequest
    ): ApiResult<GetSeatResponse>

    /**
     * DELETE /study-cafes/{id}/seats/{seatId}
     * 좌석 삭제 (관리자)
     */
    suspend fun deleteSeat(
        cafeId: Long,
        seatId: String
    ): ApiResult<Unit>

    // =====================================================
    // Images
    // =====================================================

    /**
     * POST /images/upload
     * 이미지 업로드
     */
    suspend fun uploadImage(file: okhttp3.MultipartBody.Part): ApiResult<ImageUploadResponse>

    /**
     * GET /images/{imageId}
     * 이미지 조회 (다운로드)
     */
    suspend fun getImage(imageId: String): ApiResult<ByteArray>

    /**
     * DELETE /images/{imageId}
     * 이미지 삭제
     */
    suspend fun deleteImage(imageId: String): ApiResult<Unit>
}