package kr.jiyeok.seatly.ui.screen.user

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.Image
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EventSeat
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.asImageBitmap
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
import kr.jiyeok.seatly.ui.navigation.UserHomeNavigator
import kr.jiyeok.seatly.ui.theme.*
import java.time.Instant

@Composable
fun UserHomeScreen(
    navController: NavController,
    viewModel: HomeViewModel = hiltViewModel()
) {
    // 뒤로가기 종료 핸들러 추가
    kr.jiyeok.seatly.ui.component.common.ExitBackHandler()

    // 네비게이션 관리자 생성
    val navigator = remember { UserHomeNavigator(navController) }

    // HomeViewModel에서 모든 데이터 가져오기
    val userData by viewModel.userData.collectAsState()
    val favoriteCafeIds by viewModel.favoriteCafeIds.collectAsState()
    val userSessions by viewModel.userSessions.collectAsState()
    val allCafes by viewModel.cafes.collectAsState()
    val imageBitmapCache by viewModel.imageBitmapCache.collectAsState()
    var timeUpdateTrigger by remember { mutableIntStateOf(0) }

    // 처음 한 번만 홈 데이터 로드
    LaunchedEffect(Unit) {
        viewModel.loadHomeData()
    }

    // 1분마다 경과 시간 업데이트
    LaunchedEffect(Unit) {
        while (true) {
            delay(60000L)
            timeUpdateTrigger++
        }
    }

    // 즐겨찾기 카페 필터링
    val favoriteCafes: List<StudyCafeSummaryDto> =
        allCafes.filter { cafe -> cafe.id in favoriteCafeIds }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ColorWhite)
            .verticalScroll(rememberScrollState())
    ) {
        // 상단 바 (로고 + 알림)
        TopBar(notificationCount = 1) {
            navigator.navigateToNotifications()
        }

        // 환영 섹션
        WelcomeSection(userName = userData?.name)

        // 카페 찾기 섹션
//        CafeFindSection(onSearch = { navigator.navigateToSearch() })

        // 현재 사용 중인 세션 섹션
        val allSessions: List<SessionDto> = userSessions ?: emptyList()
        if (allSessions.isNotEmpty()) {
            Text(
                text = "현재 사용 중",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = ColorTextBlack,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
            )

            allSessions.forEach { sessionDto ->
                // 경과 시간 계산 (1분마다 업데이트)
                val elapsedTime = remember(sessionDto.startTime, timeUpdateTrigger) {
                    calculateElapsedTime(sessionDto.startTime)
                }

                // 진행률 계산 (4시간 기준, 1분마다 업데이트)
                val progress = remember(sessionDto.startTime, timeUpdateTrigger) {
                    calculateProgress(sessionDto.startTime, 4 * 60 * 60 * 1000L)
                }

                // 세션과 매칭되는 카페 찾기
                val matchedCafe: StudyCafeSummaryDto? = allCafes.find { cafe ->
                    cafe.id == sessionDto.studyCafeId
                }

                // 카페 정보가 없으면 기본값 사용
                val displayCafe = matchedCafe ?: StudyCafeSummaryDto(
                    id = sessionDto.studyCafeId,
                    name = "카페 이름 로딩 중",
                    address = "위치 정보 로딩 중",
                    mainImageUrl = ""
                )

                SessionCard(
                    cafe = displayCafe,
                    imageBitmap = displayCafe.mainImageUrl?.let { imageBitmapCache[it] },
                    elapsedTime = elapsedTime,
                    progressValue = progress,
                    onViewDetail = { navigator.navigateToCafeDetail(sessionDto.studyCafeId) },
                    onEndUsage = { viewModel.endCurrentSession() },
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))
            }
        } else {
            // 활성 세션이 없는 경우
            Text(
                text = "현재 사용 중",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = ColorTextBlack,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            NoActiveSessionCard(
                onFindCafe = { navigator.navigateToSearch() },
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))
        }

        // 즐겨찾기 카페 섹션
        FavoritesCafeSection(
            cafes = favoriteCafes,
            imageBitmapCache = imageBitmapCache,
            onViewAll = { navigator.navigateToFavorites() },
            onItemClick = { cafe -> navigator.navigateToCafeDetail(cafe.id) },
            onToggleFavorite = { cafe ->
                if (cafe.id in favoriteCafeIds) {
                    viewModel.removeFavoriteCafe(cafe.id)
                } else {
                    viewModel.addFavoriteCafe(cafe.id)
                }
            }
        )

        // 하단 여유 공간 (BottomNavigationBar와 겹치지 않기 위함)
        Spacer(modifier = Modifier.height(100.dp))
    }
}

// =====================================================
// Helper Functions
// =====================================================

/**
 * 세션 시작 시간 문자열에서 경과 시간을 계산
 * 형식: "1시간 30분" 또는 "45분"
 */
private fun calculateElapsedTime(startTimeStr: String): String {
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

/**
 * 진행률 계산
 * 사용 시간 / 총 사용 시간 = 진행률 (0f ~ 1f)
 */
private fun calculateProgress(startTimeStr: String, totalDurationMillis: Long): Float {
    return try {
        val startInstant = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Instant.parse(startTimeStr)
        } else {
            return 0f
        }

        val usedMillis = System.currentTimeMillis() - startInstant.toEpochMilli()

        if (totalDurationMillis > 0) {
            (usedMillis.toFloat() / totalDurationMillis.toFloat()).coerceIn(0f, 1f)
        } else {
            0f
        }
    } catch (e: Exception) {
        0f
    }
}

// =====================================================
// UI Components
// =====================================================

/**
 * 상단 바 (로고 + 알림 버튼)
 */
@Composable
fun TopBar(notificationCount: Int, onNotificationClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(ColorWhite)
            .padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Seatly",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = ColorTextBlack
        )

        Box(contentAlignment = Alignment.TopEnd) {
            Icon(
                imageVector = Icons.Default.Notifications,
                contentDescription = "알림",
                tint = ColorPrimaryOrange,
                modifier = Modifier
                    .size(24.dp)
                    .clickable { onNotificationClick() }
            )

            // 알림 배지
            if (notificationCount > 0) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(RoundedCornerShape(50))
                        .background(ColorRedBadge)
                        .align(Alignment.TopEnd)
                        .offset(x = 6.dp, y = (-6.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = notificationCount.toString(),
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        color = ColorWhite
                    )
                }
            }
        }
    }
}

/**
 * 환영 섹션
 * 사용자 이름과 함께 인사 메시지 표시
 */
@Composable
fun WelcomeSection(userName: String? = null) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp)
            .shadow(
                elevation = 8.dp,
                shape = RoundedCornerShape(16.dp),
                ambientColor = ColorTextBlack.copy(alpha = 0.15f),
                spotColor = ColorTextBlack.copy(alpha = 0.08f)
            )
            .clip(RoundedCornerShape(16.dp))
            .background(ColorBgBeige)
            .padding(horizontal = 16.dp, vertical = 16.dp)
    ) {
        Column {
            Text(
                text = if (userName != null) "안녕하세요, ${userName}님!" else "안녕하세요!",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = ColorTextBlack
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = "오늘도 열정적으로 공부하세요!",
                fontSize = 12.sp,
                color = ColorTextGray
            )
        }
    }
}

/**
 * 카페 찾기 섹션
 * 검색 버튼을 통해 카페 검색 화면으로 이동
 */
@Composable
fun CafeFindSection(onSearch: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(ColorWhite)
            .padding(horizontal = 20.dp, vertical = 12.dp)
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
                .background(ColorBgBeige)
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
                    // 검색 아이콘
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(ColorWhite)
                            .shadow(
                                elevation = 4.dp,
                                shape = RoundedCornerShape(12.dp),
                                ambientColor = ColorTextBlack.copy(alpha = 0.1f),
                                spotColor = ColorTextBlack.copy(alpha = 0.05f)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "검색",
                            tint = ColorPrimaryOrange,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    // 검색 텍스트
                    Column {
                        Text(
                            text = "스터디카페 찾기",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = ColorTextBlack
                        )

                        Spacer(modifier = Modifier.height(2.dp))

                        Text(
                            text = "새로운 카페를 찾아보세요",
                            fontSize = 11.sp,
                            color = ColorTextLightGray
                        )
                    }
                }

                // 화살표
                Text(
                    text = "→",
                    fontSize = 18.sp,
                    color = ColorTextVeryLightGray2
                )
            }
        }
    }
}

/**
 * 세션 카드
 * 현재 사용 중인 카페 정보와 경과 시간, 진행률 표시
 */
@Composable
fun SessionCard(
    cafe: StudyCafeSummaryDto,
    imageBitmap: android.graphics.Bitmap?,
    elapsedTime: String,
    progressValue: Float,
    onViewDetail: () -> Unit,
    onEndUsage: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = 8.dp,
                shape = RoundedCornerShape(16.dp),
                ambientColor = ColorTextBlack.copy(alpha = 0.15f),
                spotColor = ColorTextBlack.copy(alpha = 0.08f)
            )
            .clip(RoundedCornerShape(16.dp))
            .background(ColorBgBeige)
            .clickable { onViewDetail() }
            .padding(14.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // 카페 정보 + 진행 상황
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.Top
            ) {
                // 카페 이미지
                Box(
                    modifier = Modifier
                        .size(90.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(ColorBrownBg),
                    contentAlignment = Alignment.Center
                ) {
                    if (imageBitmap != null) {
                        Image(
                            bitmap = imageBitmap.asImageBitmap(),
                            contentDescription = cafe.name,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        AsyncImage(
                            model = cafe.mainImageUrl,
                            contentDescription = cafe.name,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                }

                // 카페 정보 (이름, 주소, 경과 시간)
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = cafe.name,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = ColorTextBlack,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = cafe.address,
                        fontSize = 11.sp,
                        color = ColorTextDarkGray,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(RoundedCornerShape(50))
                                .background(ColorPrimaryOrange)
                        )

                        Text(
                            text = elapsedTime,
                            fontSize = 11.sp,
                            color = ColorPrimaryOrange,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                // 진행률 표시
                Box(
                    modifier = Modifier.size(90.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        progress = { progressValue },
                        modifier = Modifier.size(75.dp),
                        color = ColorPrimaryOrange,
                        trackColor = ColorProgressTrack,
                        strokeWidth = 4.dp
                    )

                    Text(
                        text = "${(progressValue * 100).toInt()}%",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = ColorTextBlack
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 이용 종료 버튼
            Button(
                onClick = onEndUsage,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                colors = ButtonDefaults.buttonColors(containerColor = ColorPrimaryOrange),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = "이용 종료",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = ColorWhite
                )
            }
        }
    }
}

/**
 * 활성 세션이 없을 때 표시하는 카드
 */
@Composable
fun NoActiveSessionCard(
    onFindCafe: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
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
                imageVector = Icons.Default.EventSeat,
                contentDescription = "세션 없음",
                tint = ColorTextVeryLightGray,
                modifier = Modifier.size(40.dp)
            )

            Text(
                text = "아직 활성 세션이 없습니다",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = ColorDarkGray,
                textAlign = TextAlign.Center
            )

            Text(
                text = "카페를 검색하여 시작하세요",
                fontSize = 11.sp,
                color = ColorTextDarkGray,
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * 즐겨찾기 카페 섹션
 * 즐겨찾기한 카페들을 가로 스크롤로 표시
 */
@Composable
fun FavoritesCafeSection(
    cafes: List<StudyCafeSummaryDto>,
    imageBitmapCache: Map<String, android.graphics.Bitmap>,
    onViewAll: () -> Unit,
    onItemClick: (StudyCafeSummaryDto) -> Unit,
    onToggleFavorite: (StudyCafeSummaryDto) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(ColorWhite)
            .padding(vertical = 12.dp)
    ) {
        // 섹션 헤더
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "찜한 카페",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = ColorTextBlack
            )

            if (cafes.isNotEmpty()) {
                Text(
                    text = "더 보기",
                    fontSize = 12.sp,
                    color = ColorPrimaryOrange,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.clickable { onViewAll() }
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 즐겨찾기가 없는 경우
        if (cafes.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .shadow(
                        elevation = 8.dp,
                        shape = RoundedCornerShape(12.dp),
                        ambientColor = ColorTextBlack.copy(alpha = 0.15f),
                        spotColor = ColorTextBlack.copy(alpha = 0.08f)
                    )
                    .clip(RoundedCornerShape(12.dp))
                    .background(ColorBgBeige)
                    .padding(vertical = 32.dp, horizontal = 20.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "즐겨찾기 없음",
                        tint = ColorTextVeryLightGray,
                        modifier = Modifier.size(40.dp)
                    )

                    Text(
                        text = "아직 찜한 카페가 없어요",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = ColorDarkGray,
                        textAlign = TextAlign.Center
                    )

                    Text(
                        text = "마음에 드는 카페를 찜해보세요",
                        fontSize = 11.sp,
                        color = ColorTextDarkGray,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            // 즐겨찾기 카페 목록 (가로 스크롤)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                cafes.forEach { cafe ->
                    CafeCardHorizontal(
                        cafe = cafe,
                        imageBitmap = cafe.mainImageUrl?.let { imageBitmapCache[it] },
                        onClick = onItemClick,
                        onFavoriteClick = { onToggleFavorite(cafe) }
                    )
                }
            }
        }
    }
}

/**
 * 가로 스크롤 카페 카드
 * 이미지, 이름, 주소, 찜 버튼 표시
 */
@Composable
fun CafeCardHorizontal(
    cafe: StudyCafeSummaryDto,
    imageBitmap: android.graphics.Bitmap?,
    onClick: (StudyCafeSummaryDto) -> Unit,
    onFavoriteClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .width(150.dp)
            .shadow(
                elevation = 8.dp,
                shape = RoundedCornerShape(12.dp),
                ambientColor = ColorTextBlack.copy(alpha = 0.15f),
                spotColor = ColorTextBlack.copy(alpha = 0.08f)
            )
            .clip(RoundedCornerShape(12.dp))
            .background(ColorBgBeige)
            .clickable { onClick(cafe) }
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            // 카페 이미지 + 찜 버튼
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp)
                    .padding(top = 12.dp)
                    .height(110.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(ColorBrownBg),
                contentAlignment = Alignment.TopEnd
            ) {
                if (imageBitmap != null) {
                    Image(
                        bitmap = imageBitmap.asImageBitmap(),
                        contentDescription = cafe.name,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    AsyncImage(
                        model = cafe.mainImageUrl,
                        contentDescription = cafe.name,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }

                // 찜 버튼
                Box(
                    modifier = Modifier
                        .padding(6.dp)
                        .size(28.dp)
                        .shadow(
                            elevation = 4.dp,
                            shape = RoundedCornerShape(50),
                            ambientColor = ColorTextBlack.copy(alpha = 0.15f),
                            spotColor = ColorTextBlack.copy(alpha = 0.08f)
                        )
                        .clip(RoundedCornerShape(50))
                        .background(ColorWhite),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Favorite,
                        contentDescription = "찜",
                        tint = ColorPrimaryOrange,
                        modifier = Modifier
                            .size(16.dp)
                            .clickable { onFavoriteClick() }
                    )
                }
            }

            // 카페 정보
            Column(modifier = Modifier.padding(10.dp)) {
                Text(
                    text = cafe.name,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = ColorTextBlack,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = cafe.address,
                    fontSize = 10.sp,
                    color = ColorTextDarkGray,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
