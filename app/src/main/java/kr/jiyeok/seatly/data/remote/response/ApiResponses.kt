package kr.jiyeok.seatly.data.remote.response

import kr.jiyeok.seatly.data.remote.enums.EFacility
import kr.jiyeok.seatly.data.remote.enums.ERole
import kr.jiyeok.seatly.data.remote.enums.ESeatStatus
import kr.jiyeok.seatly.data.remote.enums.EStatus

/**
 * Generic API Response Wrapper
 */
data class ApiResponse<T>(
    val success: Boolean,
    val message: String?,
    val data: T?
)

// =====================================================
// User Responses
// =====================================================

/**
 * GET /user 응답
 * 로그인한 사용자 정보 조회
 */
data class UserInfoDetailDto(
    val email: String,
    val name: String,
    val phone: String?,
    val imageUrl: String?,
    val favoriteCafeIds: List<Long> = emptyList(),
    val sessions: List<SessionDto> = emptyList(),
    val timePassess: List<UserTimePass> = emptyList(),
    val role: ERole
)

/**
 * GET /users?studyCafeId={id} 응답
 * 시간권이 남아있는 사용자 정보
 */
data class UserTimePassInfo(
    val id: Long,
    val name: String,
    val cafeId: Long,
    val leftTime: Long,
    val totalTime: Long
)

/**
 * GET /users/{id} 응답 (관리자)
 * 사용자 정보 조회
 */
data class UserInfoSummaryDto(
    val email: String,
    val name: String,
    val phone: String?,
    val imageUrl: String?,
    val role: ERole
)

// =====================================================
// Session Responses
// =====================================================

/**
 * GET /sessions 응답
 * 세션 목록 조회
 */
data class SessionDto(
    val id: Long,
    val userId: Long,
    val studyCafeId: Long,
    val seatId: Long,
    val status: EStatus,
    val startTime: String // "2026-01-02T02:35:42Z” 형태의 UTC 타임 문자열
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
// Study Cafe Responses
// =====================================================

/**
 * 스터디카페 요약 정보
 * GET /study-cafes 또는 GET /study-cafes/admin 응답
 */
data class StudyCafeSummaryDto(
    val id: Long,
    val name: String,
    val address: String,
    val mainImageUrl: String?
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
    val phone: String?,
    val facilities: List<EFacility> = emptyList(),
    val openingHours: String? = null,
    val description: String? = null
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
// Seat Responses
// =====================================================

/**
 * GET /study-cafes/{id}/seats 응답
 * 카페의 좌석 목록
 */
data class SeatDto(
    val id: Long,
    val name: String,
    val status: ESeatStatus,
    val position: String
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