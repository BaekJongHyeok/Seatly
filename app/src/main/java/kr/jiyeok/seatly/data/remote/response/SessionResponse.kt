package kr.jiyeok.seatly.data.remote.response

data class SessionResponseDto(
    val id: Long,
    val user_id: Long,
    val seat_id: Long,
    val status: String,
    val start_time: String?,
    val createdAt: String
)
