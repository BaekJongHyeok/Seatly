package kr.jiyeok.seatly.data.remote.response

data class UserResponseDto(
    val id: Long,
    val name: String,
    val createdAt: String,
    val role: Int,
    val lastLoginType: String?
)
