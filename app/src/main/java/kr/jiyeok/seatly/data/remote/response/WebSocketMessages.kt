package kr.jiyeok.seatly.data.remote.response

import com.google.gson.annotations.SerializedName

/**
 * WebSocket 메시지 타입
 */
sealed class WebSocketMessage {
    /**
     * 스터디카페 전체 업데이트 (좌석 현황 등)
     */
    data class StudyCafeUpdate(
        val studyCafeId: Long,
        val totalSeats: Int,
        val availableSeats: Int,
        val message: String?
    ) : WebSocketMessage()

    /**
     * 사용자 개인 좌석 이벤트
     */
    data class SeatEvent(
        val userId: Long,
        val seatId: String,
        val eventType: SeatEventType,
        val message: String
    ) : WebSocketMessage()

    /**
     * 시간권 요청 이벤트 (관리자용)
     */
    data class TimePassRequest(
        val requestId: Long,
        val studyCafeId: Long,
        val userId: Long,
        val userName: String,
        val requestedTime: Long,
        val requestedAt: String
    ) : WebSocketMessage()

    /**
     * 시간권 응답 이벤트 (사용자용)
     */
    data class TimePassResponse(
        val requestId: Long,
        val studyCafeId: Long,
        val status: TimePassStatus,
        val message: String
    ) : WebSocketMessage()
}

/**
 * 좌석 이벤트 타입
 */
enum class SeatEventType {
    @SerializedName("ASSIGNED")
    ASSIGNED,
    
    @SerializedName("RELEASED")
    RELEASED,
    
    @SerializedName("EXTENDED")
    EXTENDED
}

/**
 * 시간권 상태
 */
enum class TimePassStatus {
    @SerializedName("APPROVED")
    APPROVED,
    
    @SerializedName("REJECTED")
    REJECTED
}
