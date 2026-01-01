package kr.jiyeok.seatly.data.remote.response

/**
 * Generic API Response Wrapper
 */
data class ApiResponse<T>(
    val success: Boolean,
    val message: String?,
    val data: T?
)

/**
 * Pagination Response
 */
data class PageResponse<T>(
    val content: List<T>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int
)

// =====================================================
// Auth Responses
// =====================================================

/**
 * POST /auth/login 응답
 * 로그인 성공 시 토큰과 유저 정보 함께 반환
 */
data class LoginResponse(
    val accessToken: String,
    val refreshToken: String,
    val expiresIn: Long,
    val user: UserResponseDto
)

/**
 * POST /auth/refresh 응답
 * 토큰 리프레시 응답
 */
data class TokenResponse(
    val accessToken: String,
    val refreshToken: String,
    val expiresIn: Long
)

// =====================================================
// User Responses
// =====================================================

/**
 * GET /user 응답
 * 로그인한 사용자 정보 조회
 */
data class UserResponseDto(
    val id: Long,
    val email: String,
    val name: String,
    val phone: String?,
    val imageUrl: String?,
    val favoriteCafeIds: List<Long> = emptyList(),
    val sessions: List<Session> = emptyList(),
    val timePassess: List<UserTimePass> = emptyList(),
    val role: String  // USER, ADMIN
)

/**
 * GET /users/{id} 응답 (관리자)
 * 사용자 정보 조회
 */
data class UserResponseDtoAdmin(
    val id: Long,
    val email: String,
    val name: String,
    val phone: String?,
    val imageUrl: String?
)

/**
 * GET /users?studyCafeId={id} 응답
 * 시간권이 남아있는 사용자 정보
 */
data class UserTimePassInfo(
    val userId: Long,
    val userName: String,
    val leftTime: Long
)

// =====================================================
// Session Responses
// =====================================================

/**
 * 사용자의 활성 세션 정보
 */
data class Session(
    val id: Long,
    val userId: Long,
    val seat: SeatDto,
    val studyCafe: StudyCafe,
    val startTime: Long
)

/**
 * GET /sessions 응답
 * 세션 목록 조회
 */
data class SessionDto(
    val id: Long,
    val seatId: String,
    val userId: Long?,
    val status: String,  // ASSIGNED, IN_USE
    val startTime: Long
)

/**
 * 사용자의 남은 시간권 정보
 */
data class UserTimePass(
    val studyCafeId: Long,
    val leftTime: Long,
    val totalTime: Long
)

// =====================================================
// Seat Responses
// =====================================================

/**
 * 좌석 정보
 * 세션에서 사용될 때는 studyCafeId, leftTime, totalTime 포함
 * 좌석 목록에서 사용될 때는 id, name, status, position 포함
 */
data class SeatDto(
    val id: String? = null,
    val name: String? = null,
    val status: String? = null,  // AVAILABLE, UNAVAILABLE
    val position: String? = null,  // window, middle, wall, etc.
    val studyCafeId: Long? = null,  // 세션 정보에서 사용
    val leftTime: Long? = null,  // 세션 정보에서 사용 (남은 시간, 초)
    val totalTime: Long? = null  // 세션 정보에서 사용 (전체 시간, 초)
)

/**
 * GET /study-cafes/{id}/seats 응답
 * 카페의 좌석 목록
 */
data class GetSeatResponse(
    val seats: List<SeatDto>
)

// =====================================================
// Study Cafe Responses
// =====================================================

/**
 * 스터디카페 요약 정보
 * GET /study-cafes 또는 GET /study-cafes/admin 응답
 */
data class StudyCafeSummaryDto(
    val id: Long,
    val name: String,
    val mainImageUrl: String?,  // imageUrls 중 첫번째 값
    val address: String
)

/**
 * GET /study-cafes/{id} 응답
 * 스터디카페 상세 정보
 */
data class StudyCafeDetailDto(
    val id: Long,
    val name: String,
    val address: String,
    val imageUrls: List<String> = emptyList(),
    val phoneNumber: String?,
    val facilities: List<String> = emptyList(),
    val openingHours: String,
    val description: String? = null
)

/**
 * POST /study-cafes, PATCH /study-cafes/{id} 요청
 * 스터디카페 생성/수정 요청
 */
data class StudyCafeDetailPost(
    val name: String?,
    val address: String?,
    val images: List<String> = emptyList(),
    val phoneNumber: String?,
    val facilities: List<String> = emptyList(),
    val openingHours: List<OpeningHourDto> = emptyList(),
    val description: String? = null
)

/**
 * 영업 시간 정보
 */
data class OpeningHourDto(
    val day: String,  // MON, TUE, WED, THU, FRI, SAT, SUN
    val openTime: String,  // HH:mm
    val closeTime: String  // HH:mm
)

/**
 * 세션과 함께 반환되는 카페 정보
 */
/**
 * 카페 간단 정보 (세션, 즐겨찾기 등에서 사용)
 */
data class StudyCafe(
    val id: Long,
    val name: String,
    val address: String,
    val mainImageUrl: String? = null,
    val phoneNumber: String? = null,
    val avgRating: Double? = null,
    val reviewCount: Int? = null,
    val latitude: Double? = null,  // 위도 (세션 정보에서 사용)
    val longitude: Double? = null  // 경도 (세션 정보에서 사용)
)

/**
 * GET /study-cafes/{id}/usage 응답
 * 카페 실시간 혼잡도
 */
data class UsageDto(
    val totalCount: Int,
    val useCount: Int
)

// =====================================================
// Image Responses
// =====================================================

/**
 * POST /images/upload 응답
 * 이미지 업로드 결과
 */
data class ImageUploadResponse(
    val imageId: String,
    val imageUrl: String
)