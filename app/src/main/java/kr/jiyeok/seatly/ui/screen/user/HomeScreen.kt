package kr.jiyeok.seatly.ui.screen.user

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EventSeat
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import kotlinx.coroutines.delay
import kr.jiyeok.seatly.data.remote.response.SessionDto
import kr.jiyeok.seatly.data.remote.response.StudyCafeSummaryDto
import kr.jiyeok.seatly.presentation.viewmodel.HomeViewModel
import kr.jiyeok.seatly.presentation.viewmodel.UserViewModel
import java.time.Instant

@Composable
fun HomeScreen(
    navController: NavController,
    viewModel: HomeViewModel = hiltViewModel(),
    userViewModel: UserViewModel = hiltViewModel()
) {
    // 1. 상태 구독
    val userData by userViewModel.userProfile.collectAsState()
    val allCafes by viewModel.cafesPage.collectAsState()

    // 시간 업데이트 트리거 (1분마다 갱신)
    var timeUpdateTrigger by remember { mutableIntStateOf(0) }

    // 2. 초기 데이터 로드
    LaunchedEffect(Unit) {
        userViewModel.loadUserProfile()
        viewModel.loadAllCafes()
    }

    // 3. 프로필 로드 완료 시 -> 세션 정보 확인 -> studyCafeId로 loadHomeData 호출
    LaunchedEffect(userData) {
        userData?.let { user ->
            user.favoriteCafeIds?.let { ids ->
                viewModel.updateFavoriteIds(ids)
            }
            val firstSessionCafeId = user.sessions?.firstOrNull()?.studyCafeId ?: 0L
            viewModel.loadHomeData(firstSessionCafeId)
        }
    }

    // 4. 시간 갱신 루프 (1분마다)
    LaunchedEffect(Unit) {
        while (true) {
            delay(60000L)
            timeUpdateTrigger++
        }
    }

    val favoriteCafeIds: Set<Long> = userData?.favoriteCafeIds?.toSet() ?: emptySet()
    val favoriteCafes: List<StudyCafeSummaryDto> = allCafes ?.filter { cafe -> cafe.id in favoriteCafeIds } ?: emptyList()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .verticalScroll(rememberScrollState())
    ) {
        // 상단 바
        TopBar(notificationCount = 0) {
            navController.navigate("notifications")
        }

        // 환영 메시지
        WelcomeSection(userName = userData?.name)

        // --- 모든 활성 세션 표시 ---
        val allSessions: List<SessionDto> = userData?.sessions ?: emptyList()

        if (allSessions.isNotEmpty()) {
            Text(
                text = "현재 사용 중",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
            )

            // ★ forEach로 모든 세션을 렌더링
            allSessions.forEach { sessionDto ->
                val elapsedTime = remember(sessionDto.startTime, timeUpdateTrigger) {
                    calculateElapsedTime(sessionDto.startTime)
                }
                val progress = remember(sessionDto.startTime, timeUpdateTrigger) {
                    calculateProgress(sessionDto.startTime, 4 * 60 * 60 * 1000L)
                }

                // 세션에 대응하는 카페 정보 찾기 (또는 임시 생성) - ★ 타입 명시
                val matchedCafe: StudyCafeSummaryDto? = allCafes?.find { cafe ->
                    cafe.id == sessionDto.studyCafeId
                }
                val displayCafe = matchedCafe ?: StudyCafeSummaryDto(
                    id = sessionDto.studyCafeId,
                    name = "카페 이름 로딩 중",
                    address = "위치 정보 로딩 중",
                    mainImageUrl = ""
                )

                SessionCard(
                    cafe = displayCafe,
                    elapsedTime = elapsedTime,
                    progressValue = progress,
                    onViewDetail = { navController.navigate("cafe_detail/${sessionDto.studyCafeId}") },
                    onEndUsage = { viewModel.endCurrentSession() },
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))
        } else {
            // 이용 중인 세션 없음
            NoActiveSessionGuide(
                onFindCafe = { navController.navigate("search") }
            )
            Spacer(modifier = Modifier.height(10.dp))
        }

        // 카페 찾기 버튼
        CafeFindSection(onSearch = { navController.navigate("search") })

        // 찜한 카페 목록
        FavoritesCafeSection(
            cafes = favoriteCafes,
            onViewAll = { navController.navigate("favorites") },
            onItemClick = { cafe -> navController.navigate("cafe_detail/${cafe.id}") },
            onToggleFavorite = { cafe ->
                viewModel.removeFavoriteCafe(cafe.id)
            }
        )

        Spacer(modifier = Modifier.height(100.dp))
    }
}

// ------------------------------------------------------------------------
// 헬퍼 함수
// ------------------------------------------------------------------------

fun calculateElapsedTime(startTimeStr: String): String {
    return try {
        val startInstant = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Instant.parse(startTimeStr)
        } else {
            return "시간 계산 불가"
        }
        val nowMillis = System.currentTimeMillis()
        val startMillis = startInstant.toEpochMilli()

        val diffMillis = nowMillis - startMillis
        if (diffMillis < 0) return "0분"

        val minutes = (diffMillis / (1000 * 60)).toInt()
        val hours = minutes / 60
        val remMin = minutes % 60

        if (hours > 0) "${hours}시간 ${remMin}분" else "${remMin}분"
    } catch (e: Exception) {
        "시간 오류"
    }
}

fun calculateProgress(startTimeStr: String, totalDurationMillis: Long): Float {
    return try {
        val startInstant = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Instant.parse(startTimeStr)
        } else {
            return 0f
        }
        val usedMillis = System.currentTimeMillis() - startInstant.toEpochMilli()
        if (totalDurationMillis > 0) {
            (usedMillis.toFloat() / totalDurationMillis.toFloat()).coerceIn(0f, 1f)
        } else 0f
    } catch (e: Exception) {
        0f
    }
}

// ------------------------------------------------------------------------
// UI 컴포넌트
// ------------------------------------------------------------------------

@Composable
fun TopBar(notificationCount: Int, onNotificationClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Seatly",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Black
        )
        Box(contentAlignment = Alignment.TopEnd) {
            IconButton(onClick = onNotificationClick) {
                Icon(
                    imageVector = Icons.Default.Notifications,
                    contentDescription = "알림",
                    tint = Color.Black,
                    modifier = Modifier.size(24.dp)
                )
            }
            if (notificationCount > 0) {
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .clip(RoundedCornerShape(50))
                        .background(Color(0xFFFF6B4A))
                        .align(Alignment.TopEnd),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = notificationCount.toString(),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    }
}

@Composable
fun WelcomeSection(userName: String? = null) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .padding(horizontal = 20.dp, vertical = 8.dp)
    ) {
        Text(
            text = if (userName != null) "안녕하세요! ${userName}님" else "안녕하세요!",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Black
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "오늘도 열정적으로 공부하세요!",
            fontSize = 14.sp,
            color = Color(0xFFAAAAAA)
        )
    }
}

@Composable
fun SessionCard(
    cafe: StudyCafeSummaryDto,
    elapsedTime: String,
    progressValue: Float,
    onViewDetail: () -> Unit,
    onEndUsage: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Color(0xFFFBFAF8))
            .border(1.dp, Color(0xFFE8E6E1), RoundedCornerShape(18.dp))
            .clickable { onViewDetail() }
            .padding(14.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.Top
            ) {
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFD4C5B9)),
                    contentAlignment = Alignment.Center
                ) {
                    AsyncImage(
                        model = cafe.mainImageUrl,
                        contentDescription = cafe.name,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = cafe.name,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.Black,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(1.dp))
                    Text(
                        text = cafe.address,
                        fontSize = 12.sp,
                        color = Color(0xFFA0A0A0),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "사용 중 $elapsedTime",
                        fontSize = 12.sp,
                        color = Color(0xFFFF6B4A),
                        fontWeight = FontWeight.Medium
                    )
                }

                Box(modifier = Modifier.size(80.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(
                        progress = { progressValue },
                        modifier = Modifier.size(70.dp),
                        color = Color(0xFFFF6B4A),
                        trackColor = Color(0xFFF0F0F0),
                        strokeWidth = 4.dp
                    )
                    Text(
                        text = "${(progressValue * 100).toInt()}%",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = onEndUsage,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .border(1.dp, Color(0xFFE8E6E1), RoundedCornerShape(12.dp)),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "이용 종료",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.Black
                    )
                }
            }
        }
    }
}

@Composable
fun CafeFindSection(onSearch: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .padding(horizontal = 20.dp, vertical = 10.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(Color(0xFFFBFAF8))
                .border(1.dp, Color(0xFFE8E6E1), RoundedCornerShape(18.dp))
                .clickable { onSearch() }
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "검색",
                        tint = Color(0xFFFF6B4A),
                        modifier = Modifier.size(24.dp)
                    )
                    Column {
                        Text(
                            text = "스터디카페 찾기",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.Black
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "새로운 카페를 찾아보세요",
                            fontSize = 12.sp,
                            color = Color(0xFFA0A0A0)
                        )
                    }
                }
                Text(
                    text = "›",
                    fontSize = 28.sp,
                    color = Color(0xFFCCCCCC)
                )
            }
        }
    }
}

@Composable
fun FavoritesCafeSection(
    cafes: List<StudyCafeSummaryDto>,
    onViewAll: () -> Unit,
    onItemClick: (StudyCafeSummaryDto) -> Unit,
    onToggleFavorite: (StudyCafeSummaryDto) -> Unit // 파라미터 추가
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .padding(vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "찜한 카페",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )
            if (cafes.isNotEmpty()) {
                Text(
                    text = "더보기",
                    fontSize = 14.sp,
                    color = Color(0xFF999999),
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.clickable { onViewAll() }
                )
            }
        }
        Spacer(modifier = Modifier.height(12.dp))

        if (cafes.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color(0xFFFBFAF8))
                    .border(1.dp, Color(0xFFE8E6E1), RoundedCornerShape(18.dp))
                    .padding(vertical = 32.dp, horizontal = 24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "즐겨찾기 없음",
                        tint = Color(0xFFCCCCCC),
                        modifier = Modifier.size(40.dp)
                    )
                    Text(
                        text = "찜한 카페가 없습니다",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF666666),
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "마음에 드는 카페를 즐겨찾기 해보세요",
                        fontSize = 12.sp,
                        color = Color(0xFF999999),
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                cafes.forEach { cafe ->
                    CafeCardHorizontal(
                        cafe = cafe,
                        onClick = onItemClick,
                        onFavoriteClick = { onToggleFavorite(cafe) } // 이벤트 전달
                    )
                }
            }
        }
    }
}

@Composable
fun CafeCardHorizontal(
    cafe: StudyCafeSummaryDto,
    onClick: (StudyCafeSummaryDto) -> Unit,
    onFavoriteClick: () -> Unit // 파라미터 추가
) {
    Box(
        modifier = Modifier
            .width(155.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(Color(0xFFFBFAF8))
            .border(1.dp, Color(0xFFE8E6E1), RoundedCornerShape(18.dp))
            .clickable { onClick(cafe) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(Color(0xFFFBFAF8))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp)
                    .background(Color(0xFFD4C5B9))
            ) {
                AsyncImage(
                    model = cafe.mainImageUrl,
                    contentDescription = cafe.name,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )

                // 하트 버튼 영역 (수정됨)
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(10.dp)
                        .size(32.dp)
                        .clip(RoundedCornerShape(50))
                        .background(Color.White)
                        .clickable { onFavoriteClick() }, // 클릭 이벤트 추가
                    contentAlignment = Alignment.Center
                ) {
                    // UI 완벽 유지를 위해 기존 Text 이모지 사용
                    Text(text = "❤️", fontSize = 16.sp)
                }
            }
            Column(modifier = Modifier.padding(10.dp)) {
                Text(
                    text = cafe.name,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.Black,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(1.dp))
                Text(
                    text = cafe.address,
                    fontSize = 12.sp,
                    color = Color(0xFFA0A0A0),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(6.dp))
            }
        }
    }
}


@Composable
fun NoActiveSessionGuide(onFindCafe: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .padding(horizontal = 20.dp, vertical = 10.dp)
    ) {
        Text(
            text = "현재 사용 중",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Black
        )
        Spacer(modifier = Modifier.height(12.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(Color(0xFFFBFAF8))
                .border(1.dp, Color(0xFFE8E6E1), RoundedCornerShape(18.dp))
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.EventSeat,
                    contentDescription = "세션 없음",
                    tint = Color(0xFFCCCCCC),
                    modifier = Modifier.size(48.dp)
                )
                Text(
                    text = "현재 이용 중인 좌석이 없습니다",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF666666),
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "새로운 스터디카페를 찾아보세요",
                    fontSize = 12.sp,
                    color = Color(0xFF999999),
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(4.dp))
                Button(
                    onClick = onFindCafe,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF6B4A)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "카페 찾기",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                }
            }
        }
    }
}
