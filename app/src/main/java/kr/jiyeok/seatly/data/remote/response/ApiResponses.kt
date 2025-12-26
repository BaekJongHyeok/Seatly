package kr.jiyeok.seatly.data.remote.response

data class ApiResponse<T>(
    val success: Boolean,
    val message: String? = null,
    val data: T? = null
)

data class PageResponse<T>(
    val content: List<T>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int
)

data class LoginResponseDTO(
    val accessToken: String,
    val refreshToken: String,
    val expiresIn: Long,
    val user: UserResponseDto
)

data class UserResponseDto(
    val id: Long,
    val email: String,
    val name: String,
    val phone: String?,
    val imageUrl: String?,
    val joinedAt: String,
    val roles: List<String> = emptyList(),
    val favoritesCount: Int = 0
)

data class CurrentCafeUsageDto(
    val cafeId: Long,
    val cafeName: String,
    val cafeImageUrl: String?,
    val cafeAddress: String,
    val seatName: String,
    val startedAt: String,
    val elapsedMillis: Long
)

data class StudyCafeSummaryDto(
    val id: Long,
    val name: String,
    val mainImageUrl: String?,
    val address: String,
    val rating: Double? = null,
    val isFavorite: Boolean = false,
    val isOpen: Boolean = false,
    val distanceMeters: Int? = null
)

data class StudyCafeDetailDto(
    val id: Long,
    val name: String,
    val address: String,
    val images: List<String> = emptyList(),
    val phoneNumber: String?,
    val facilities: List<String> = emptyList(),
    val openingHours: List<OpeningHourDto> = emptyList(),
    val isOpen: Boolean = false,
    val isFavorite: Boolean = false,
    val seatsSummary: SeatsSummaryDto,
    val description: String? = null,
    val createdAt: String? = null,
    val updatedAt: String? = null
)

data class OpeningHourDto(
    val dayOfWeek: String,
    val openTime: String,
    val closeTime: String,
    val closed: Boolean = false
)

data class SeatsSummaryDto(
    val total: Int,
    val occupied: Int,
    val available: Int,
    val reserved: Int = 0
)

data class SeatResponseDto(
    val id: Long,
    val seatNumber: String,
    val position: String?,
    val type: String?,
    val status: String,
    val extra: Map<String, Any>? = null
)

data class SessionResponseDto(
    val id: Long,
    val userId: Long,
    val studyCafeId: Long,
    val seatId: Long?,
    val seatName: String?,
    val startedAt: String,
    val endedAt: String?,
    val status: String
)