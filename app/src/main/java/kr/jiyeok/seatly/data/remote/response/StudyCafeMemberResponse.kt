package kr.jiyeok.seatly.data.remote.response

data class StudyCafeMemberResponseDto(
    val id: Long,
    val study_cafe_id: Long,
    val user_id: Long,
    val createdAt: String
)
