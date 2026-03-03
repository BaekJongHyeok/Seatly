package kr.jiyeok.seatly.data.repository

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 알림 엔티티
 */
data class NotificationEntity(
    val id: String = java.util.UUID.randomUUID().toString(),
    val title: String,
    val message: String,
    val type: NotificationType,
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val isRead: Boolean = false
)

enum class NotificationType {
    TIME_PASS_APPROVED,
    TIME_PASS_REJECTED,
    SEAT_EVENT,
    GENERAL
}

/**
 * 인메모리 알림 저장소 (Singleton)
 * 나중에 Room DB로 교체 가능한 구조
 */
@Singleton
class NotificationRepository @Inject constructor() {

    private val _notifications = MutableStateFlow<List<NotificationEntity>>(emptyList())
    val notifications: StateFlow<List<NotificationEntity>> = _notifications.asStateFlow()

    /**
     * 알림 추가
     */
    fun addNotification(notification: NotificationEntity) {
        _notifications.update { current ->
            listOf(notification) + current // 최신이 맨 위
        }
    }

    /**
     * 시간권 응답 알림 추가 (편의 메서드)
     */
    fun addTimePassNotification(approved: Boolean, message: String) {
        val notification = NotificationEntity(
            title = if (approved) "시간권 요청이 승인되었습니다" else "시간권 요청이 거절되었습니다",
            message = message,
            type = if (approved) NotificationType.TIME_PASS_APPROVED else NotificationType.TIME_PASS_REJECTED
        )
        addNotification(notification)
    }

    /**
     * 읽음 처리
     */
    fun markAsRead(notificationId: String) {
        _notifications.update { current ->
            current.map {
                if (it.id == notificationId) it.copy(isRead = true) else it
            }
        }
    }

    /**
     * 전체 읽음 처리
     */
    fun markAllAsRead() {
        _notifications.update { current ->
            current.map { it.copy(isRead = true) }
        }
    }

    /**
     * 읽지 않은 알림 수
     */
    fun getUnreadCount(): Int = _notifications.value.count { !it.isRead }

    /**
     * 오늘 알림
     */
    fun getTodayNotifications(): List<NotificationEntity> {
        val today = LocalDate.now()
        return _notifications.value.filter { it.createdAt.toLocalDate() == today }
    }

    /**
     * 이전 알림
     */
    fun getPastNotifications(): List<NotificationEntity> {
        val today = LocalDate.now()
        return _notifications.value.filter { it.createdAt.toLocalDate() != today }
    }
}
