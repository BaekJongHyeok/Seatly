package kr.jiyeok.seatly.data.remote.response

import kotlinx.serialization.Serializable
import kr.jiyeok.seatly.data.remote.enums.EFacility
import kr.jiyeok.seatly.data.remote.enums.ERole
import kr.jiyeok.seatly.data.remote.enums.ESeatStatus
import kr.jiyeok.seatly.data.remote.enums.EStatus

@Serializable
data class ApiResponse<T>(
    val success: Boolean,
    val message: String?,
    val data: T?
)

@Serializable
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

@Serializable
data class UserTimePassInfo(
    val id: Long,
    val name: String,
    val cafeId: Long,
    val leftTime: Long,
    val totalTime: Long
)

@Serializable
data class UserInfoSummaryDto(
    val email: String,
    val name: String,
    val phone: String?,
    val imageUrl: String?,
    val role: ERole
)

@Serializable
data class SessionDto(
    val id: Long,
    val userId: Long,
    val studyCafeId: Long,
    val seatId: Long,
    val status: EStatus,
    val startTime: String
)

@Serializable
data class UserTimePass(
    val studyCafeId: Long,
    val leftTime: Long,
    val totalTime: Long
)

@Serializable
data class StudyCafeSummaryDto(
    val id: Long,
    val name: String,
    val address: String,
    val mainImageUrl: String?
)

@Serializable
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

@Serializable
data class UsageDto(
    val totalCount: Int,
    val useCount: Int
)

@Serializable
data class SeatDto(
    val id: Long,
    val name: String,
    val status: ESeatStatus,
    val position: String
)

@Serializable
data class ImageUploadResponse(
    val imageId: String,
    val imageUrl: String
)
