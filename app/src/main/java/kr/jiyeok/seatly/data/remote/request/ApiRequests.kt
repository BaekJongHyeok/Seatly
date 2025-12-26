package kr.jiyeok.seatly.data.remote.request

data class LoginRequest(
    val email: String,
    val password: String
)

data class RegisterRequest(
    val email: String,
    val password: String,
    val passwordConfirm: String,
    val name: String,
    val phone: String?,
    val agreeTerms: Boolean
)

data class SocialRegisterRequest(
    val email: String,
    val name: String,
    val phone: String?,
    val provider: String,
    val agreeTerms: Boolean
)

data class ForgotPasswordRequest(
    val email: String
)

data class VerifyCodeRequest(
    val email: String,
    val code: String
)

data class ResetPasswordRequest(
    val email: String,
    val code: String,
    val newPassword: String,
    val newPasswordConfirm: String
)

data class UpdateUserRequest(
    val name: String?,
    val phone: String?,
    val imageUrl: String?
)

data class ChangePasswordRequest(
    val currentPassword: String,
    val newPassword: String,
    val newPasswordConfirm: String
)

data class AddSeatRequest(
    val seatNumber: String,
    val position: String,
    val type: String? = null,
    val meta: Map<String, String>? = null
)

data class EditSeatRequest(
    val seatNumber: String?,
    val position: String?,
    val type: String?,
    val meta: Map<String, String>? = null
)

data class ReservationRequest(
    val seatId: Long?,
    val startAt: String?,
    val durationMinutes: Int?,
    val note: String? = null
)

data class StartSessionRequest(
    val studyCafeId: Long,
    val seatId: Long?,
    val startAt: String? = null
)