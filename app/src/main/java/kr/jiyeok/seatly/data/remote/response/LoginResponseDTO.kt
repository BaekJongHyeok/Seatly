package kr.jiyeok.seatly.data.remote.response

data class LoginResponseDTO(
    val userId: Long,
    val token: String,
    val refreshToken: String,
    val userInfo: UserResponseDto
)
