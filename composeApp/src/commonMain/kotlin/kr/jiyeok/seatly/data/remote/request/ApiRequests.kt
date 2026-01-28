package kr.jiyeok.seatly.data.remote.request

import kotlinx.serialization.Serializable
import kr.jiyeok.seatly.data.remote.enums.EFacility
import kr.jiyeok.seatly.data.remote.enums.ERole
import kr.jiyeok.seatly.data.remote.enums.ESeatStatus

@Serializable
data class LoginRequest(
    val email: String,
    val password: String
)

@Serializable
data class UpdateUserInfoRequest(
    val name: String?,
    val phone: String?,
    val imageUrl: String?
)

@Serializable
data class RegisterRequest(
    val email: String,
    val password: String,
    val name: String,
    val phone: String,
    val imageUrl: String,
    val role: ERole
)

@Serializable
data class ChangePasswordRequest(
    val currentPassword: String,
    val newPassword: String
)

@Serializable
data class CreateCafeRequest(
    val name: String,
    val address: String,
    val imageUrls: List<String> = emptyList(),
    val phoneNumber: String?,
    val facilities: List<EFacility> = emptyList(),
    val openingHours: String? = null,
    val description: String? = null
)

@Serializable
data class UpdateCafeRequest(
    val name: String,
    val address: String,
    val imageUrls: List<String> = emptyList(),
    val phoneNumber: String?,
    val facilities: List<EFacility> = emptyList(),
    val openingHours: String? = null,
    val description: String? = null
)

@Serializable
data class SeatCreate(
    val name: String,
    val status: ESeatStatus,
    val position: String
)

@Serializable
data class UpdateSeatRequest(
    val seats: List<SeatUpdate>
)

@Serializable
data class SeatUpdate(
    val id: Long,
    val name: String,
    val status: ESeatStatus,
    val position: String
)
