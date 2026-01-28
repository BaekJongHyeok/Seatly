package kr.jiyeok.seatly.ui.screen.user

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import kotlinx.coroutines.delay
import kr.jiyeok.seatly.data.remote.response.SessionDto
import kr.jiyeok.seatly.data.remote.response.StudyCafeSummaryDto
import kr.jiyeok.seatly.presentation.viewmodel.HomeViewModel
import kr.jiyeok.seatly.ui.navigation.UserHomeNavigator
import kr.jiyeok.seatly.ui.theme.*
import kr.jiyeok.seatly.ui.component.common.ExitBackHandler
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import org.jetbrains.compose.resources.painterResource
import org.koin.compose.viewmodel.koinViewModel
import seatly.composeapp.generated.resources.Res
import seatly.composeapp.generated.resources.img_default_cafe

@Composable
fun UserHomeScreen(
    navController: NavController,
    viewModel: HomeViewModel = koinViewModel()
) {
    ExitBackHandler()

    val navigator = remember { UserHomeNavigator(navController) }

    val userData by viewModel.userData.collectAsState()
    val favoriteCafeIds by viewModel.favoriteCafeIds.collectAsState()
    val userSessions by viewModel.userSessions.collectAsState()
    val allCafes by viewModel.cafes.collectAsState()
    val imageBitmapCache by viewModel.imageBitmapCache.collectAsState()
    var timeUpdateTrigger by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        viewModel.loadHomeData()
    }

    LaunchedEffect(Unit) {
        while (true) {
            delay(60000L)
            timeUpdateTrigger++
        }
    }

    val favoriteCafes: List<StudyCafeSummaryDto> =
        allCafes.filter { cafe -> cafe.id in favoriteCafeIds }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ColorWhite)
            .verticalScroll(rememberScrollState())
    ) {
        TopBar(notificationCount = 1) {
            navigator.navigateToNotifications()
        }

        WelcomeSection(userName = userData?.name)

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
                val elapsedTime = remember(sessionDto.startTime, timeUpdateTrigger) {
                    calculateElapsedTime(sessionDto.startTime)
                }

                val progress = remember(sessionDto.startTime, timeUpdateTrigger) {
                    calculateProgress(sessionDto.startTime, 4 * 60 * 60 * 1000L)
                }

                val matchedCafe: StudyCafeSummaryDto? = allCafes.find { cafe ->
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

        Spacer(modifier = Modifier.height(100.dp))
    }
}

private fun calculateElapsedTime(startTimeStr: String): String {
    return try {
        val startInstant = Instant.parse(startTimeStr)
        val nowMillis = Clock.System.now().toEpochMilliseconds()
        val startMillis = startInstant.toEpochMilliseconds()
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

private fun calculateProgress(startTimeStr: String, totalDurationMillis: Long): Float {
    return try {
        val startInstant = Instant.parse(startTimeStr)
        val usedMillis = Clock.System.now().toEpochMilliseconds() - startInstant.toEpochMilliseconds()

        if (totalDurationMillis > 0) {
            (usedMillis.toFloat() / totalDurationMillis.toFloat()).coerceIn(0f, 1f)
        } else {
            0f
        }
    } catch (e: Exception) {
        0f
    }
}

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

            if (notificationCount > 0) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
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

@Composable
fun SessionCard(
    cafe: StudyCafeSummaryDto,
    imageBitmap: ImageBitmap?,
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
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.Top
            ) {
                Box(
                    modifier = Modifier
                        .size(90.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(ColorBrownBg),
                    contentAlignment = Alignment.Center
                ) {
                    if (imageBitmap != null) {
                        Image(
                            bitmap = imageBitmap,
                            contentDescription = cafe.name,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Image(
                            painter = painterResource(Res.drawable.img_default_cafe),
                            contentDescription = cafe.name,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                }

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
                                .clip(CircleShape)
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

@Composable
fun FavoritesCafeSection(
    cafes: List<StudyCafeSummaryDto>,
    imageBitmapCache: Map<String, ImageBitmap>,
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

@Composable
fun CafeCardHorizontal(
    cafe: StudyCafeSummaryDto,
    imageBitmap: ImageBitmap?,
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
                        bitmap = imageBitmap,
                        contentDescription = cafe.name,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Image(
                        painter = painterResource(Res.drawable.img_default_cafe),
                        contentDescription = cafe.name,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }

                Box(
                    modifier = Modifier
                        .padding(8.dp)
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(ColorWhite.copy(alpha = 0.8f))
                        .clickable { onFavoriteClick() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Favorite,
                        contentDescription = "찜",
                        tint = ColorPrimaryOrange,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Column(
                modifier = Modifier.padding(10.dp)
            ) {
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
