package kr.jiyeok.seatly.data.remote.response

data class UsageLogResponseDto(
    val id: Long,
    val session_id: Long,
    val used_minutes: Int,
    val createdAt: String
)
