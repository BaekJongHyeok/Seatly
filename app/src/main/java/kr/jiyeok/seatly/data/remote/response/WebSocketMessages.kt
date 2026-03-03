package kr.jiyeok.seatly.data.remote.response

import com.google.gson.annotations.SerializedName

/**
 * WebSocket 메시지 타입
 */
sealed class WebSocketMessage {
    /**
     * 좌석 이벤트 (ASSIGNED, USAGE_STARTED, USAGE_FINISHED, HOLD_RELEASED)
     */
    data class SeatEvent(
        val type: SeatEventType,
        val seatId: Long
    ) : WebSocketMessage()

    /**
     * 시간권 이벤트 (TIMEPASS_REQUEST, TIMEPASS_REQUEST_ACCEPTED, TIMEPASS_REQUEST_REJECTED)
     */
    data class TimePassEvent(
        val type: TimePassEventType,
        val request: TimePassRequestDto
    ) : WebSocketMessage()
}

/**
 * 좌석 이벤트 타입
 */
enum class SeatEventType {
    @SerializedName("SEAT_ASSIGNED")
    ASSIGNED,
    
    @SerializedName("SEAT_USAGE_STARTED")
    USAGE_STARTED,
    
    @SerializedName("SEAT_USAGE_FINISHED")
    USAGE_FINISHED,
    
    @SerializedName("SEAT_HOLD_RELEASED")
    HOLD_RELEASED
}

/**
 * 시간권 이벤트 타입
 */
enum class TimePassEventType {
    @SerializedName("TIMEPASS_REQUEST")
    TIMEPASS_REQUEST,
    
    @SerializedName("TIMEPASS_REQUEST_ACCEPTED")
    TIMEPASS_REQUEST_ACCEPTED,
    
    @SerializedName("TIMEPASS_REQUEST_REJECTED")
    TIMEPASS_REQUEST_REJECTED
}
