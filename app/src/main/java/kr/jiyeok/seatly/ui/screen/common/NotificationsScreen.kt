package kr.jiyeok.seatly.ui.screen.common

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import kr.jiyeok.seatly.data.repository.NotificationEntity
import kr.jiyeok.seatly.data.repository.NotificationType
import kr.jiyeok.seatly.presentation.viewmodel.NotificationsViewModel
import kr.jiyeok.seatly.ui.component.common.AppTopBar
import kr.jiyeok.seatly.ui.theme.ColorBgBeige
import kr.jiyeok.seatly.ui.theme.ColorCheckCircle
import kr.jiyeok.seatly.ui.theme.ColorDarkGray
import kr.jiyeok.seatly.ui.theme.ColorPrimaryOrange
import kr.jiyeok.seatly.ui.theme.ColorRedBadge
import kr.jiyeok.seatly.ui.theme.ColorTextBlack
import kr.jiyeok.seatly.ui.theme.ColorTextDarkGray
import kr.jiyeok.seatly.ui.theme.ColorTextGray
import kr.jiyeok.seatly.ui.theme.ColorTextVeryLightGray
import kr.jiyeok.seatly.ui.theme.ColorWhite
import java.time.Duration
import java.time.LocalDateTime

/**
 * 알림 화면
 *
 * - 상단 바: 뒤로가기 + "알림" 타이틀
 * - 오늘 / 이전 알림 섹션
 * - 알림이 없을 때 Empty 상태
 */
@Composable
fun NotificationsScreen(
    navController: NavController,
    viewModel: NotificationsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ColorWhite)
            .verticalScroll(rememberScrollState())
    ) {
        AppTopBar(
            title = "알림",
            leftContent = {
                Icon(
                    imageVector = Icons.Filled.ArrowBack,
                    contentDescription = "뒤로가기",
                    tint = ColorTextBlack,
                    modifier = Modifier.size(22.dp).clickable { navController.popBackStack() },
                )
            }
        )

        Spacer(modifier = Modifier.height(4.dp))

        if (uiState.todayNotifications.isEmpty() && uiState.pastNotifications.isEmpty()) {
            EmptyNotificationState()
        } else {
            if (uiState.todayNotifications.isNotEmpty()) {
                NotificationSection(
                    title = "오늘",
                    notifications = uiState.todayNotifications,
                    onNotificationClick = { viewModel.markAsRead(it.id) }
                )
            }

            if (uiState.pastNotifications.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                NotificationSection(
                    title = "지난 알림",
                    notifications = uiState.pastNotifications,
                    onNotificationClick = { viewModel.markAsRead(it.id) }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }

        // 하단 여유 공간 (BottomNavigationBar와 겹치지 않도록)
        Spacer(modifier = Modifier.height(100.dp))
    }
}

/**
 * 알림 섹션 (예: "오늘", "지난 알림")
 */
@Composable
private fun NotificationSection(
    title: String,
    notifications: List<NotificationEntity>,
    onNotificationClick: (NotificationEntity) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp)
    ) {
        Text(
            text = title,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = ColorTextBlack,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            notifications.forEach { item ->
                NotificationCard(
                    item = item,
                    onClick = { onNotificationClick(item) }
                )
            }
        }
    }
}

/**
 * 개별 알림 카드
 *
 * - 읽지 않은 알림: 오른쪽 상단 빨간 뱃지
 * - 타입별 아이콘 + 제목 + 내용 + 시간
 */
@Composable
private fun NotificationCard(
    item: NotificationEntity,
    onClick: () -> Unit = {}
) {
    val icon = when (item.type) {
        NotificationType.TIME_PASS_APPROVED -> Icons.Filled.CheckCircle
        NotificationType.TIME_PASS_REJECTED -> Icons.Filled.Cancel
        else -> Icons.Filled.Notifications
    }
    val iconTint = when (item.type) {
        NotificationType.TIME_PASS_APPROVED -> ColorCheckCircle
        NotificationType.TIME_PASS_REJECTED -> Color(0xFFE53935)
        else -> ColorPrimaryOrange
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 8.dp,
                shape = RoundedCornerShape(16.dp),
                ambientColor = ColorTextBlack.copy(alpha = 0.15f),
                spotColor = ColorTextBlack.copy(alpha = 0.08f)
            )
            .clip(RoundedCornerShape(16.dp))
            .background(
                if (item.isRead) ColorBgBeige
                else ColorBgBeige.copy(alpha = 0.95f)
            )
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 아이콘
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(ColorWhite),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = "알림",
                    tint = iconTint,
                    modifier = Modifier.size(22.dp)
                )
            }

            Column(
                modifier = Modifier
                    .weight(1f)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = item.title,
                        fontSize = 13.sp,
                        fontWeight = if (item.isRead) FontWeight.SemiBold else FontWeight.Bold,
                        color = ColorTextBlack,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )

                    Text(
                        text = formatTimeAgo(item.createdAt),
                        fontSize = 11.sp,
                        color = ColorTextDarkGray
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = item.message,
                    fontSize = 12.sp,
                    color = ColorTextGray,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        // 읽지 않은 알림 뱃지
        if (!item.isRead) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(10.dp)
                    .clip(RoundedCornerShape(50))
                    .background(ColorRedBadge)
            )
        }
    }
}

/**
 * 알림이 하나도 없을 때 표시되는 Empty 상태
 */
@Composable
private fun EmptyNotificationState() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 24.dp)
            .shadow(
                elevation = 8.dp,
                shape = RoundedCornerShape(16.dp),
                ambientColor = ColorTextBlack.copy(alpha = 0.15f),
                spotColor = ColorTextBlack.copy(alpha = 0.08f)
            )
            .clip(RoundedCornerShape(16.dp))
            .background(ColorBgBeige)
            .padding(vertical = 32.dp, horizontal = 20.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.Notifications,
                contentDescription = "알림 없음",
                tint = ColorTextVeryLightGray,
                modifier = Modifier.size(40.dp)
            )

            Text(
                text = "아직 도착한 알림이 없어요",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = ColorDarkGray
            )

            Text(
                text = "시간권을 요청하면 관리자의 응답을 여기서 확인할 수 있어요",
                fontSize = 11.sp,
                color = ColorTextDarkGray
            )
        }
    }
}

/**
 * 시간 포맷 (상대 시간)
 */
private fun formatTimeAgo(dateTime: LocalDateTime): String {
    val now = LocalDateTime.now()
    val duration = Duration.between(dateTime, now)

    return when {
        duration.toMinutes() < 1 -> "방금 전"
        duration.toMinutes() < 60 -> "${duration.toMinutes()}분 전"
        duration.toHours() < 24 -> "${duration.toHours()}시간 전"
        duration.toDays() == 1L -> "어제"
        duration.toDays() < 7 -> "${duration.toDays()}일 전"
        else -> "${dateTime.monthValue}/${dateTime.dayOfMonth}"
    }
}
