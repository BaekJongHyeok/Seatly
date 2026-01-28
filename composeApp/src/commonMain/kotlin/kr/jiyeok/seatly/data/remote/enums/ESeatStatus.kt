package kr.jiyeok.seatly.data.remote.enums

import kotlinx.serialization.Serializable

@Serializable
enum class ESeatStatus {
    AVAILABLE,
    OCCUPIED,
    UNAVAILABLE
}
