package kr.jiyeok.seatly.ui.screen.common

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import kr.jiyeok.seatly.ui.component.common.AppTopBar
import kr.jiyeok.seatly.ui.theme.*

/**
 * 알림 화면 진입 시 사용할 메인 Composable
 *
 * - 상단 바: 뒤로가기 + "알림" 타이틀
 * - 오늘 / 이전 알림 섹션
 * - 알림이 없을 때 Empty 상태
 *
 * 실제 데이터 연동 시에는 notificationsToday / notificationsPast 를
 * ViewModel에서 가져오도록 교체하면 됨.
 */
@Composable
fun NotificationsScreen(
    navController: NavController
) {
    // TODO: ViewModel 연동 시 이 부분을 교체
    val (notificationsToday, notificationsPast) = remember {
        mockNotifications()
    }

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

        if (notificationsToday.isEmpty() && notificationsPast.isEmpty()) {
            EmptyNotificationState()
        } else {
            if (notificationsToday.isNotEmpty()) {
                NotificationSection(
                    title = "오늘",
                    notifications = notificationsToday
                )
            }

            if (notificationsPast.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                NotificationSection(
                    title = "지난 알림",
                    notifications = notificationsPast
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
    notifications: List<NotificationItem>
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
                NotificationCard(item = item)
            }
        }
    }
}

/**
 * 개별 알림 카드
 *
 * - 읽지 않은 알림: 배경 살짝 진하게 / 오른쪽 상단 빨간 뱃지
 * - 아이콘 + 제목 + 내용 + 시간
 */
@Composable
private fun NotificationCard(
    item: NotificationItem,
    onClick: () -> Unit = {}
) {
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
                    imageVector = Icons.Filled.Notifications,
                    contentDescription = "알림",
                    tint = ColorPrimaryOrange,
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
                        overflow = TextOverflow.Ellipsis
                    )

                    Text(
                        text = item.timeText,
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
                text = "중요한 소식이 있으면 여기에서 알려드릴게요",
                fontSize = 11.sp,
                color = ColorTextDarkGray
            )
        }
    }
}

/**
 * UI에서 사용할 알림 아이템 모델
 */
data class NotificationItem(
    val id: String,
    val title: String,
    val message: String,
    val timeText: String,   // "5분 전", "어제", "3일 전" 등
    val isRead: Boolean
)

/**
 * UI 확인용 Mock 데이터
 */
private fun mockNotifications(): Pair<List<NotificationItem>, List<NotificationItem>> {
    val today = listOf(
        NotificationItem(
            id = "1",
            title = "예약 시간이 곧 시작돼요",
            message = "홍대 스터디카페 A 지점 예약 시작 10분 전입니다.",
            timeText = "5분 전",
            isRead = false
        ),
        NotificationItem(
            id = "2",
            title = "이용 종료 알림",
            message = "강남 스터디카페 B 지점 이용이 곧 종료됩니다.",
            timeText = "30분 전",
            isRead = false
        )
    )

    val past = listOf(
        NotificationItem(
            id = "3",
            title = "이용이 정상 종료되었어요",
            message = "어제 이용하신 스터디카페 C 지점 이용이 정상적으로 종료되었습니다.",
            timeText = "어제",
            isRead = true
        ),
        NotificationItem(
            id = "4",
            title = "예약이 완료되었어요",
            message = "내일 오후 2시에 이용하실 강남 스터디카페 B 지점 예약이 완료되었습니다.",
            timeText = "2일 전",
            isRead = true
        )
    )

    return today to past
}
