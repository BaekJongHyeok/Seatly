package kr.jiyeok.seatly.data.remote.enums

import kotlinx.serialization.Serializable

@Serializable
enum class EStatus {
    ACTIVE,
    FINISHED,
    EXPIRED,
    ASSIGNED,
    IN_USE
}
