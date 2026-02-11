package kr.jiyeok.seatly.data.websocket

import android.util.Log
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kr.jiyeok.seatly.data.remote.response.WebSocketMessage
import kr.jiyeok.seatly.domain.websocket.WebSocketManager
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.min
import kotlin.math.pow

/**
 * WebSocket 관리자 구현체
 */
@Singleton
class WebSocketManagerImpl @Inject constructor(
    private val okHttpClient: OkHttpClient,
    private val gson: Gson
) : WebSocketManager {
    
    private companion object {
        const val TAG = "WebSocketManager"
        const val WS_URL = "ws://3.27.78.54:8080/ws"  // WebSocket 엔드포인트
        const val MAX_RECONNECT_ATTEMPTS = 5
        const val INITIAL_RECONNECT_DELAY = 1000L
    }
    
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    
    private var webSocket: WebSocket? = null
    private var currentToken: String? = null
    private var reconnectAttempts = 0
    
    // 구독 관리
    private val subscriptions = mutableMapOf<String, String>() // subscriptionId -> destination
    
    // 메시지 플로우
    private val _messages = MutableSharedFlow<WebSocketMessage>(
        replay = 0,
        extraBufferCapacity = 10,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    override val messages: Flow<WebSocketMessage> = _messages.asSharedFlow()
    
    // 연결 상태
    private val _isConnected = MutableStateFlow(false)
    override val isConnected: Flow<Boolean> = _isConnected.asStateFlow()
    
    private val listener = object : WebSocketListener() {
        override fun onOpen(webSocket: WebSocket, response: Response) {
            Log.d(TAG, "WebSocket connected")
            _isConnected.value = true
            reconnectAttempts = 0
            
            // STOMP CONNECT 전송
            currentToken?.let { token ->
                val connectFrame = StompProtocol.buildConnectFrame(token)
                webSocket.send(connectFrame)
                Log.d(TAG, "Sent CONNECT frame")
            }
        }
        
        override fun onMessage(webSocket: WebSocket, text: String) {
            Log.d(TAG, "Received message: $text")
            
            val stompMessage = StompProtocol.parseMessage(text)
            if (stompMessage == null) {
                Log.w(TAG, "Failed to parse STOMP message")
                return
            }
            
            when (stompMessage.command) {
                "CONNECTED" -> {
                    Log.d(TAG, "STOMP connection established")
                    // 재구독
                    resubscribeAll()
                }
                "MESSAGE" -> {
                    handleStompMessage(stompMessage)
                }
                "ERROR" -> {
                    Log.e(TAG, "STOMP error: ${stompMessage.body}")
                }
            }
        }
        
        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
            Log.e(TAG, "WebSocket failure", t)
            _isConnected.value = false
            attemptReconnect()
        }
        
        override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
            Log.d(TAG, "WebSocket closed: $code - $reason")
            _isConnected.value = false
        }
    }
    
    override fun connect(token: String) {
        currentToken = token
        
        if (webSocket != null) {
            Log.w(TAG, "WebSocket already connected")
            return
        }
        
        val request = Request.Builder()
            .url(WS_URL)
            .build()
        
        webSocket = okHttpClient.newWebSocket(request, listener)
        Log.d(TAG, "WebSocket connecting...")
    }
    
    override fun disconnect() {
        val ws = webSocket ?: return
        
        // STOMP DISCONNECT 전송
        val disconnectFrame = StompProtocol.buildDisconnectFrame()
        ws.send(disconnectFrame)
        
        // WebSocket 닫기
        ws.close(1000, "Client closed")
        webSocket = null
        currentToken = null
        subscriptions.clear()
        
        _isConnected.value = false
        Log.d(TAG, "WebSocket disconnected")
    }
    
    override fun subscribeToStudyCafe(studyCafeId: Long): String {
        val destination = "/topic/study-cafe/$studyCafeId"
        return subscribe(destination)
    }
    
    override fun subscribeToUserSeatEvents(userId: Long): String {
        val destination = "/topic/user/$userId/seat-events"
        return subscribe(destination)
    }
    
    override fun subscribeToTimePassEvents(userId: Long): String {
        val destination = "/topic/user/$userId/time-pass-request-events"
        return subscribe(destination)
    }
    
    override fun unsubscribe(subscriptionId: String) {
        val ws = webSocket ?: return
        
        val unsubscribeFrame = StompProtocol.buildUnsubscribeFrame(subscriptionId)
        ws.send(unsubscribeFrame)
        
        subscriptions.remove(subscriptionId)
        Log.d(TAG, "Unsubscribed: $subscriptionId")
    }
    
    private fun subscribe(destination: String): String {
        val ws = webSocket ?: throw IllegalStateException("WebSocket not connected")
        
        val subscriptionId = java.util.UUID.randomUUID().toString()
        val subscribeFrame = StompProtocol.buildSubscribeFrame(destination, subscriptionId)
        
        ws.send(subscribeFrame)
        subscriptions[subscriptionId] = destination
        
        Log.d(TAG, "Subscribed to $destination with id: $subscriptionId")
        return subscriptionId
    }
    
    private fun resubscribeAll() {
        val destinations = subscriptions.values.toList()
        subscriptions.clear()
        
        destinations.forEach { destination ->
            subscribe(destination)
        }
        
        Log.d(TAG, "Resubscribed to ${destinations.size} topics")
    }
    
    private fun attemptReconnect() {
        if (reconnectAttempts >= MAX_RECONNECT_ATTEMPTS) {
            Log.e(TAG, "Max reconnect attempts reached")
            return
        }
        
        val delay = INITIAL_RECONNECT_DELAY * 2.0.pow(reconnectAttempts).toLong()
        val cappedDelay = min(delay, 30000L) // 최대 30초
        
        reconnectAttempts++
        Log.d(TAG, "Reconnecting in ${cappedDelay}ms (attempt $reconnectAttempts)")
        
        scope.launch {
            delay(cappedDelay)
            currentToken?.let { connect(it) }
        }
    }
    
    private fun handleStompMessage(stompMessage: StompMessage) {
        val destination = stompMessage.headers["destination"] ?: return
        val body = stompMessage.body
        
        if (body.isBlank()) return
        
        try {
            val message = when {
                destination.contains("/study-cafe/") -> {
                    gson.fromJson(body, WebSocketMessage.StudyCafeUpdate::class.java)
                }
                destination.contains("/seat-events") -> {
                    gson.fromJson(body, WebSocketMessage.SeatEvent::class.java)
                }
                destination.contains("/time-pass-request-events") -> {
                    // 메시지 내용에 따라 TimePassRequest 또는 TimePassResponse
                    val jsonObject = gson.fromJson(body, Map::class.java) as Map<*, *>
                    if (jsonObject.containsKey("requestId") && jsonObject.containsKey("userName")) {
                        gson.fromJson(body, WebSocketMessage.TimePassRequest::class.java)
                    } else {
                        gson.fromJson(body, WebSocketMessage.TimePassResponse::class.java)
                    }
                }
                else -> {
                    Log.w(TAG, "Unknown destination: $destination")
                    return
                }
            }
            
            scope.launch {
                _messages.emit(message)
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse message: $body", e)
        }
    }
}
