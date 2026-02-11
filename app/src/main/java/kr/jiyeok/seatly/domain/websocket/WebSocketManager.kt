package kr.jiyeok.seatly.domain.websocket

import kotlinx.coroutines.flow.Flow
import kr.jiyeok.seatly.data.remote.response.WebSocketMessage

/**
 * WebSocket 관리 인터페이스
 */
interface WebSocketManager {
    /**
     * WebSocket 연결
     */
    fun connect(token: String)
    
    /**
     * WebSocket 연결 해제
     */
    fun disconnect()
    
    /**
     * 스터디카페 전체 이벤트 구독
     */
    fun subscribeToStudyCafe(studyCafeId: Long): String
    
    /**
     * 사용자 개인 좌석 이벤트 구독
     */
    fun subscribeToUserSeatEvents(userId: Long): String
    
    /**
     * 시간권 요청 이벤트 구독 (관리자 또는 사용자)
     */
    fun subscribeToTimePassEvents(userId: Long): String
    
    /**
     * 특정 구독 해제
     */
    fun unsubscribe(subscriptionId: String)
    
    /**
     * WebSocket 메시지 스트림
     */
    val messages: Flow<WebSocketMessage>
    
    /**
     * 연결 상태
     */
    val isConnected: Flow<Boolean>
}
