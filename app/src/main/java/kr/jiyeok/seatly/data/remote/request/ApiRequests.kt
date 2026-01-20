package kr.jiyeok.seatly.data.remote.request

import kr.jiyeok.seatly.data.remote.enums.EFacility
import kr.jiyeok.seatly.data.remote.enums.ERole
import kr.jiyeok.seatly.data.remote.enums.ESeatStatus

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

// =====================================================
// User Requests
// =====================================================

/**
 * PATCH /user 요청
 * 사용자 정보 편집
 */
data class UpdateUserInfoRequest(
    val name: String?,
    val phone: String?,
    val imageUrl: String?
)

/**
 * POST /user 요청
 * 회원가입
 */
data class RegisterRequest(
    val email: String,
    val password: String,
    val name: String,
    val phone: String,
    val imageUrl: String,
    val role: ERole
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
// Study Cafe Requests
// =====================================================

/**
 * POST /study-cafes 요청
 * 카페 추가 (관리자)
 */
data class CreateCafeRequest(
    val name: String,
    val address: String,
    val imageUrls: List<String> = emptyList(),
    val phoneNumber: String?,
    val facilities: List<EFacility> = emptyList(),
    val openingHours: String? = null,
    val description: String? = null
)

/**
 * PATCH /study-cafes/{id} 요청
 * 카페 정보 수정 (관리자)
 */
data class UpdateCafeRequest(
    val name: String,
    val address: String,
    val imageUrls: List<String> = emptyList(),
    val phoneNumber: String?,
    val facilities: List<EFacility> = emptyList(),
    val openingHours: String? = null,
    val description: String? = null
)

// =====================================================
// Seat Requests
// =====================================================

/**
 * POST /study-cafes/{id}/seats 요청
 * 좌석 생성 정보
 */
data class SeatCreate(
    val name: String,
    val status: ESeatStatus,
    val position: String
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
    val id: Long,
    val name: String,
    val status: ESeatStatus,
    val position: String
)