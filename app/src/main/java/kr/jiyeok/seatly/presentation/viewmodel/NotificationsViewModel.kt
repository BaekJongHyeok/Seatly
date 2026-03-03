package kr.jiyeok.seatly.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kr.jiyeok.seatly.data.repository.NotificationEntity
import kr.jiyeok.seatly.data.repository.NotificationRepository
import java.time.LocalDate
import javax.inject.Inject

data class NotificationsUiState(
    val todayNotifications: List<NotificationEntity> = emptyList(),
    val pastNotifications: List<NotificationEntity> = emptyList(),
    val unreadCount: Int = 0
)

@HiltViewModel
class NotificationsViewModel @Inject constructor(
    private val notificationRepository: NotificationRepository
) : ViewModel() {

    val uiState: StateFlow<NotificationsUiState> = notificationRepository.notifications
        .map { notifications ->
            val today = LocalDate.now()
            NotificationsUiState(
                todayNotifications = notifications.filter { it.createdAt.toLocalDate() == today },
                pastNotifications = notifications.filter { it.createdAt.toLocalDate() != today },
                unreadCount = notifications.count { !it.isRead }
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = NotificationsUiState()
        )

    fun markAsRead(notificationId: String) {
        notificationRepository.markAsRead(notificationId)
    }

    fun markAllAsRead() {
        notificationRepository.markAllAsRead()
    }
}
