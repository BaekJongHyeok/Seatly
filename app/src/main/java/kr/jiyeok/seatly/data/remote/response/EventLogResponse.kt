package kr.jiyeok.seatly.data.remote.response

data class EventLogResponseDto(
    val id: Long,
    val session_id: Long?,
    val event_type: String,
    val detail: String,
    val createdAt: String
)
