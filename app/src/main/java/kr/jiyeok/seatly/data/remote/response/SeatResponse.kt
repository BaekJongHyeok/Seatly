package kr.jiyeok.seatly.data.remote.response

data class SeatResponseDto(
    val id: Long,
    val study_cafe_id: Long,
    val seat_number: Int,
    val position: String,
    val status: String,
    val updatedAt: String
)
