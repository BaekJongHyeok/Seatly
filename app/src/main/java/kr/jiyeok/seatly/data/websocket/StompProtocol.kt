package kr.jiyeok.seatly.data.websocket

import java.util.UUID

/**
 * STOMP 프로토콜 헬퍼
 * WebSocket 메시지를 STOMP 형식으로 변환
 */
object StompProtocol {
    private const val NULL_CHAR = '\u0000'
    
    /**
     * CONNECT 프레임 생성
     */
    fun buildConnectFrame(token: String): String {
        return buildString {
            append("CONNECT\n")
            append("accept-version:1.1,1.2\n")
            append("heart-beat:10000,10000\n")
            append("Authorization:Bearer $token\n")
            append("\n")
            append(NULL_CHAR)
        }
    }
    
    /**
     * SUBSCRIBE 프레임 생성
     */
    fun buildSubscribeFrame(destination: String, subscriptionId: String = UUID.randomUUID().toString()): String {
        return buildString {
            append("SUBSCRIBE\n")
            append("id:$subscriptionId\n")
            append("destination:$destination\n")
            append("\n")
            append(NULL_CHAR)
        }
    }
    
    /**
     * UNSUBSCRIBE 프레임 생성
     */
    fun buildUnsubscribeFrame(subscriptionId: String): String {
        return buildString {
            append("UNSUBSCRIBE\n")
            append("id:$subscriptionId\n")
            append("\n")
            append(NULL_CHAR)
        }
    }
    
    /**
     * DISCONNECT 프레임 생성
     */
    fun buildDisconnectFrame(): String {
        return buildString {
            append("DISCONNECT\n")
            append("\n")
            append(NULL_CHAR)
        }
    }
    
    /**
     * STOMP 메시지 파싱
     */
    fun parseMessage(text: String): StompMessage? {
        if (text.isBlank()) return null
        
        val lines = text.split("\n")
        if (lines.isEmpty()) return null
        
        val command = lines[0]
        val headers = mutableMapOf<String, String>()
        var bodyStartIndex = 1
        
        // Parse headers
        for (i in 1 until lines.size) {
            val line = lines[i]
            if (line.isBlank()) {
                bodyStartIndex = i + 1
                break
            }
            val parts = line.split(":", limit = 2)
            if (parts.size == 2) {
                headers[parts[0].trim()] = parts[1].trim()
            }
        }
        
        // Parse body
        val body = if (bodyStartIndex < lines.size) {
            lines.subList(bodyStartIndex, lines.size)
                .joinToString("\n")
                .trimEnd(NULL_CHAR)
        } else {
            ""
        }
        
        return StompMessage(command, headers, body)
    }
}

/**
 * 파싱된 STOMP 메시지
 */
data class StompMessage(
    val command: String,
    val headers: Map<String, String>,
    val body: String
)
