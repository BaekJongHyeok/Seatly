package kr.jiyeok.seatly.data.remote.request

// =====================================================
// Auth Requests
// =====================================================

/**
 * POST /auth/login 요청
 */
data class LoginRequest(
    val email: String,
    val password: String
)

/**
 * POST /auth/register 요청
 * 회원가입
 */
data class RegisterRequest(
    val email: String,
    val password: String,
    val name: String,
    val phone: String
)

/**
 * POST /auth/refresh 요청
 * 토큰 리프레시
 */
data class RefreshTokenRequest(
    val refreshToken: String
)

/**
 * 소셜 로그인 회원가입
 * POST /auth/social-register (보류)
 */
data class SocialRegisterRequest(
    val email: String,
    val name: String,
    val phone: String?,
    val provider: String  // KAKAO, GOOGLE, NAVER, etc.
)

/**
 * 비밀번호 찾기 - 보안 코드 전송 (보류)
 * POST /auth/forgot-password
 */
data class ForgotPasswordRequest(
    val email: String
)

/**
 * 비밀번호 찾기 - 보안 코드 검증 (보류)
 * POST /auth/verify-code
 */
data class VerifyCodeRequest(
    val email: String,
    val code: String
)

// =====================================================
// User Requests
// =====================================================

/**
 * PATCH /user 요청
 * 사용자 정보 편집
 */
data class UpdateUserRequest(
    val name: String?,
    val phone: String?,
    val imageUrl: String?
)

/**
 * PUT /user/{id}/password 요청
 * 비밀번호 변경
 */
data class ChangePasswordRequest(
    val currentPassword: String,
    val newPassword: String
)

// =====================================================
// Seat Requests
// =====================================================

/**
 * POST /study-cafes/{id}/seats 요청
 * 좌석 추가 (리스트로 여러 개 한번에)
 */
data class CreateSeatRequest(
    val seats: List<SeatCreate>
)

/**
 * 좌석 생성 정보
 */
data class SeatCreate(
    val name: String,
    val status: String,  // AVAILABLE, UNAVAILABLE
    val position: String  // window, middle, wall, etc.
)

/**
 * PATCH /study-cafes/{id}/seats 요청
 * 좌석 정보 수정 (리스트로 여러 개 한번에)
 */
data class UpdateSeatRequest(
    val seats: List<SeatUpdate>
)

/**
 * 좌석 수정 정보
 */
data class SeatUpdate(
    val id: String,
    val name: String,
    val status: String,  // AVAILABLE, UNAVAILABLE
    val position: String  // window, middle, wall, etc.
)

// =====================================================
// Study Cafe Requests
// =====================================================

/**
 * POST /study-cafes 요청
 * 카페 추가 (관리자)
 */
data class CreateCafeRequest(
    val name: String,
    val address: String,
    val images: List<String> = emptyList(),
    val phoneNumber: String?,
    val facilities: List<String> = emptyList(),
    val openingHours: List<OpeningHourRequest> = emptyList(),
    val description: String? = null
)

/**
 * PATCH /study-cafes/{id} 요청
 * 카페 정보 수정 (관리자)
 */
data class UpdateCafeRequest(
    val name: String?,
    val address: String?,
    val images: List<String> = emptyList(),
    val phoneNumber: String?,
    val facilities: List<String> = emptyList(),
    val openingHours: List<OpeningHourRequest> = emptyList(),
    val description: String? = null
)

/**
 * 영업 시간 요청
 */
data class OpeningHourRequest(
    val day: String,  // MON, TUE, WED, THU, FRI, SAT, SUN
    val openTime: String,  // HH:mm
    val closeTime: String  // HH:mm
)

// =====================================================
// Session Requests (for future use)
// =====================================================

/**
 * POST /sessions/assign 요청
 * 좌석 수동 할당 - Query parameter: seatId
 */
data class AssignSeatRequest(
    val seatId: String
)

/**
 * POST /sessions/auto-assign 요청
 * 좌석 자동 할당 - Query parameter: seatId
 */
data class AutoAssignSeatRequest(
    val seatId: String
)