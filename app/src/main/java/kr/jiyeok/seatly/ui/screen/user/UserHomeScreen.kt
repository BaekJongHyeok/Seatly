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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.Image

import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.ui.res.stringResource
import kr.jiyeok.seatly.R

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
    val seatNames by viewModel.seatNames.collectAsState()
    val userTimePasses by viewModel.userTimePasses.collectAsState()
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

    // 이벤트 처리 (Toast 메시지)
    val context = androidx.compose.ui.platform.LocalContext.current
    LaunchedEffect(Unit) {
        viewModel.events.collect { message ->
            android.widget.Toast.makeText(context, message, android.widget.Toast.LENGTH_SHORT).show()
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
        // 상단: 환영 메시지와 알림 아이콘
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = if (userData?.name != null) stringResource(R.string.home_hello_user_format, userData?.name ?: "") else stringResource(R.string.home_hello_guest),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = ColorTextBlack
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.home_subtitle),
                    fontSize = 14.sp,
                    color = ColorTextGray
                )
            }

            // 알림 아이콘
            Box(contentAlignment = Alignment.TopEnd) {
                Icon(
                    imageVector = Icons.Default.Notifications,
                    contentDescription = stringResource(R.string.home_notification_desc),
                    tint = ColorTextBlack,
                    modifier = Modifier
                        .size(28.dp)
                        .clickable { navigator.navigateToNotifications() }
                )
                // 알림 배지 (하드코딩된 1 제거 또는 viewModel 연동)
                // 현재 TopBar에서 notificationCount=1로 하드코딩 되어 있었음.
                // 여기서는 일단 숨기거나 비워둠. 필요시 추가.
            }
        }

        // 카페 찾기 섹션 (주석 처리된거 유지)
//        CafeFindSection(onSearch = { navigator.navigateToSearch() })

        // 현재 사용 중인 세션 섹션
        val allSessions: List<SessionDto> = userSessions ?: emptyList()

        if (allSessions.isNotEmpty()) {


            val pagerState = rememberPagerState(pageCount = { allSessions.size })

            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxWidth(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 20.dp),
                pageSpacing = 16.dp,
                key = { index -> allSessions[index].id } // 세션 ID를 키로 사용하여 상태 불일치 방지
            ) { page ->
                val sessionDto = allSessions[page]
                
                // 진행률 계산 (남은 시간권 시간 기준, 1분마다 업데이트)
                val timePass = userTimePasses?.find { it.studyCafeId == sessionDto.studyCafeId }
                // leftTime이 0이거나 null이면 기본값 4시간 사용 (초 단위)
                val leftTimeSeconds = if (timePass != null && timePass.leftTime > 0) timePass.leftTime else (4 * 60 * 60L) // 4시간 (초)

                val progress = remember(sessionDto.startTime, timeUpdateTrigger, leftTimeSeconds) {
                    calculateRemainingProgress(sessionDto.startTime, leftTimeSeconds)
                }

                // 시작 시간 텍스트
                val startTimeDisplay = remember(sessionDto.startTime) {
                    formatStartTimeOnly(sessionDto.startTime)
                }
                
                // 경과 시간 텍스트
                val elapsedTimeDisplay = remember(sessionDto.startTime, timeUpdateTrigger) {
                    formatElapsedTimeOnly(sessionDto.startTime)
                }

                // 세션과 매칭되는 카페 찾기
                val matchedCafe: StudyCafeSummaryDto? = allCafes.find { cafe ->
                    cafe.id == sessionDto.studyCafeId
                }

                // 카페 정보가 없으면 기본값 사용
                val displayCafe = matchedCafe ?: StudyCafeSummaryDto(
                    id = sessionDto.studyCafeId,
                    name = stringResource(R.string.home_loading_cafe_name),
                    address = stringResource(R.string.home_loading_location),
                    mainImageUrl = ""
                )

                val seatName = seatNames[sessionDto.id] ?: stringResource(R.string.home_no_seat_info)
                val strSeat = stringResource(R.string.home_seat_suffix)
                val seatDisplay = if (seatName.endsWith(strSeat)) seatName else "$seatName $strSeat"

                SessionCard(
                    cafe = displayCafe,
                    seatName = seatDisplay,
                    imageBitmap = displayCafe.mainImageUrl?.let { imageBitmapCache[it] },
                    startTimeDisplay = startTimeDisplay,
                    elapsedTimeDisplay = elapsedTimeDisplay,
                    progressValue = progress,
                    onViewDetail = { navigator.navigateToCafeDetail(sessionDto.studyCafeId) },
                    onEndUsage = { viewModel.endCurrentSession(sessionDto.id) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
            
            // 인디케이터 (세션이 여러 개일 경우)
            if (allSessions.size > 1) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp, bottom = 12.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    repeat(pagerState.pageCount) { iteration ->
                        val color = if (pagerState.currentPage == iteration) ColorPrimaryOrange else Color(0xFFE0E0E0)
                        Box(
                            modifier = Modifier
                                .padding(3.dp)
                                .clip(RoundedCornerShape(50))
                                .background(color)
                                .size(6.dp)
                        )
                    }
                }
            } else {
                 Spacer(modifier = Modifier.height(12.dp))
            }

        } else {

            // 활성 세션이 없는 경우


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
 * 시작 시간 문자열 포맷팅
 * 예: "14:30 시작"
 */
/**
 * 시작 시간 문자열 포맷팅
 * 예: "14:30 시작"
 */
private fun formatStartTimeOnly(startTimeStr: String): String {
    return try {
        val startInstant = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Instant.parse(startTimeStr)
        } else {
            return "00:00"
        }
        val zoneId = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            java.time.ZoneId.of("Asia/Seoul")
        } else {
            java.time.ZoneId.systemDefault()
        }
        val localDateTime = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startInstant.atZone(zoneId).toLocalDateTime()
        } else {
            return "00:00"
        }
        val hour = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) localDateTime.hour else 0
        val minute = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) localDateTime.minute else 0
        
        String.format("%02d:%02d 시작", hour, minute)
    } catch (e: Exception) {
        "시간 오류"
    }
}

/**
 * 경과 시간 문자열 포맷팅
 * 예: "(30분 사용중)"
 */
private fun formatElapsedTimeOnly(startTimeStr: String): String {
    return try {
        val startInstant = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Instant.parse(startTimeStr)
        } else {
            return ""
        }
        
        val nowMillis = System.currentTimeMillis()
        val startMillis = startInstant.toEpochMilli()
        val diffMillis = nowMillis - startMillis
        
        val elapsedMinutes = (diffMillis / (1000 * 60)).toInt()
        val elapsedString = if (elapsedMinutes >= 60) {
             val h = elapsedMinutes / 60
             val m = elapsedMinutes % 60
             if (m > 0) "${h}시간 ${m}분 사용중" else "${h}시간 사용중"
        } else {
            "${elapsedMinutes}분 사용중"
        }
        
        "($elapsedString)"
    } catch (e: Exception) {
        ""
    }
}

/**
 * 잔여 시간 진행률 계산
 * (남은 시간권 시간 - 사용 시간) / 남은 시간권 시간 = 진행률 (1f -> 0f)
 * 사용자가 "90%가 채워져 있어야지" 라고 했으므로, 남은 시간 비율을 의미함.
 * @param totalLeftSeconds 남은 시간권 시간 (초 단위)
 */
private fun calculateRemainingProgress(startTimeStr: String, totalLeftSeconds: Long): Float {
    return try {
        val startInstant = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Instant.parse(startTimeStr)
        } else {
            return 0f
        }

        val usedMillis = System.currentTimeMillis() - startInstant.toEpochMilli()
        val totalLeftMillis = totalLeftSeconds * 1000L // 초 -> 밀리초 변환

        if (totalLeftMillis > 0) {
            val remainingMillis = totalLeftMillis - usedMillis
            val progress = (remainingMillis.toFloat() / totalLeftMillis.toFloat())
            // 0.0 ~ 1.0 사이로 제한
            progress.coerceIn(0f, 1f)
        } else {
            0f
        }
    } catch (e: Exception) {
        0f
    }
}

/**
 * 진행률 계산
 * 사용 시간 / 남은 시간권 시간 = 진행률 (0f ~ 1f)
 */


// =====================================================
// UI Components
// =====================================================



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



@Composable
fun SessionCard(
    cafe: StudyCafeSummaryDto,
    seatName: String,
    imageBitmap: android.graphics.Bitmap?,
    startTimeDisplay: String,
    elapsedTimeDisplay: String,
    progressValue: Float,
    onViewDetail: () -> Unit,
    onEndUsage: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(420.dp) // 카드 높이 약간 증가
            .shadow(
                elevation = 8.dp,
                shape = RoundedCornerShape(24.dp),
                ambientColor = ColorTextBlack.copy(alpha = 0.2f),
                spotColor = ColorTextBlack.copy(alpha = 0.1f)
            )
            .clip(RoundedCornerShape(24.dp))
            .clickable { onViewDetail() }
    ) {
        // 1. 배경 이미지 (Blur 처리)
        // "검정색 블러가 아니라 투명한 블러" -> 어두운 오버레이 제거 또는 투명도 조절
        // 가독성을 위해 텍스트에 그림자를 주거나, 이미지를 밝게 처리
        if (imageBitmap != null) {
            Image(
                bitmap = imageBitmap.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .blur(radius = 20.dp), // 블러 강도 증가
                contentScale = ContentScale.Crop
            )
        } else {
            AsyncImage(
                model = cafe.mainImageUrl,
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .blur(radius = 20.dp),
                contentScale = ContentScale.Crop
            )
        }
        
        // 투명한 블러 느낌을 위해 밝은 반투명 오버레이를 살짝 추가하거나, 어두운 오버레이를 제거함.
        // 여기서는 약간의 화이트 노이즈나 밝은 그라데이션이 없으면 텍스트가 안보일 수 있음.
        // 사용자 요청 "투명한 블러"에 맞춰 오버레이를 거의 없앰 (아주 약한 화이트/그레이 틴트)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White.copy(alpha = 0.1f)) // 아주 약한 밝은 틴트
        )

        // 3. 컨텐츠 (중앙 정렬)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // "Active Session" 텍스트 추가
            Text(
                text = stringResource(R.string.home_active_seat),
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = ColorWhite.copy(alpha = 0.9f),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            // 상단: 카페명 - 좌석이름
            Text(
                text = "${cafe.name} - $seatName",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = ColorWhite,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = androidx.compose.ui.text.TextStyle(
                    shadow = androidx.compose.ui.graphics.Shadow(
                        color = Color.Black.copy(alpha = 0.5f),
                        blurRadius = 8f
                    )
                )
            )

            Spacer(modifier = Modifier.height(32.dp))

            // 중앙: 대형 원형 프로그레스바
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(160.dp)
            ) {
                CircularProgressIndicator(
                    progress = { progressValue },
                    modifier = Modifier.fillMaxSize(),
                    color = ColorPrimaryOrange,
                    trackColor = ColorWhite.copy(alpha = 0.4f),
                    strokeWidth = 14.dp,
                    strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
                )
                
                // 가독성을 위해 텍스트 그림자 추가
                Text(
                    text = "${(progressValue * 100).toInt()}%",
                    fontSize = 38.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = ColorWhite,
                    style = androidx.compose.ui.text.TextStyle(
                        shadow = androidx.compose.ui.graphics.Shadow(
                            color = Color.Black.copy(alpha = 0.5f),
                            blurRadius = 8f
                        )
                    )
                )
            }

            // 간격 줄임 (32.dp -> 12.dp)
            Spacer(modifier = Modifier.height(16.dp))

            // 하단: 시작 시간 (사용 시간)
            Text(
                text = "$startTimeDisplay $elapsedTimeDisplay",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = ColorWhite,
                textAlign = TextAlign.Center,
                style = androidx.compose.ui.text.TextStyle(
                    shadow = androidx.compose.ui.graphics.Shadow(
                        color = Color.Black.copy(alpha = 0.5f),
                        blurRadius = 4f
                    )
                )
            )
            
            Spacer(modifier = Modifier.weight(1f))

            // 최하단: 이용 종료 버튼 (그라데이션)
            Button(
                onClick = onEndUsage,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Transparent, // 그라데이션을 위해 투명 처리
                    contentColor = ColorWhite
                ),
                contentPadding = PaddingValues(0.dp), // 내부 패딩 제거
                shape = RoundedCornerShape(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            brush = Brush.horizontalGradient(
                                colors = listOf(
                                    ColorPrimaryOrange,
                                    Color(0xFFFF9800), // 조금 더 밝은 오렌지 
                                    Color(0xFFFFB74D)  // 더 밝은 오렌지
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.home_end_usage),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
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
                contentDescription = stringResource(R.string.home_no_session_desc),
                tint = ColorTextVeryLightGray,
                modifier = Modifier.size(40.dp)
            )

            Text(
                text = stringResource(R.string.home_no_active_session),
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = ColorDarkGray,
                textAlign = TextAlign.Center
            )

            Text(
                text = stringResource(R.string.home_search_cafe_hint),
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
                text = stringResource(R.string.home_favorite_cafes),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = ColorTextBlack
            )

            if (cafes.isNotEmpty()) {
                Text(
                    text = stringResource(R.string.home_see_all),
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
                        contentDescription = stringResource(R.string.home_no_favorites_desc),
                        tint = ColorTextVeryLightGray,
                        modifier = Modifier.size(40.dp)
                    )

                    Text(
                        text = stringResource(R.string.home_no_favorites_message),
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
