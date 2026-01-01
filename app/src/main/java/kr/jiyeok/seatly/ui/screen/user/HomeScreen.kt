package kr.jiyeok.seatly.ui.screen.user

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import kotlinx.coroutines.delay
import kr.jiyeok.seatly.R
import kr.jiyeok.seatly.data.remote.response.StudyCafeDetailDto
import kr.jiyeok.seatly.data.remote.response.StudyCafeSummaryDto
import kr.jiyeok.seatly.presentation.viewmodel.AuthViewModel
import kr.jiyeok.seatly.presentation.viewmodel.HomeViewModel

@Composable
fun HomeScreen(
    navController: NavController,
    viewModel: HomeViewModel = hiltViewModel(),
    authViewModel: AuthViewModel = hiltViewModel()
) {
    val notificationCount = 1

    // 사용자 데이터
    val userData by authViewModel.userData.collectAsState()

    // 카페 목록
    val cafesPage = viewModel.cafesPage.collectAsState().value

    // 즐겨찾기 ID 목록
    val favoriteCafeIds by viewModel.favoriteCafeIds.collectAsState()

    // 2. 초기 데이터 로드
    LaunchedEffect(Unit) {
        viewModel.loadHomeData()

        // 유저 정보 로드 로직
        if (userData == null) {
            authViewModel.getUserInfo()
        }
    }

    // 3. 시간 업데이트 트리거 (1분마다 갱신)
    var timeUpdateTrigger by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(60000L) // 1분
            timeUpdateTrigger++
        }
    }

    // DTO 변환 함수 (Summary -> Detail)
    fun mapSummaryToCafeInfo(dto: StudyCafeSummaryDto): StudyCafeDetailDto {
        return StudyCafeDetailDto(
            id = dto.id,
            name = dto.name ?: "",
            address = dto.address ?: "",
            imageUrls = listOfNotNull(dto.mainImageUrl),
            phoneNumber = "010-0000-0000",
            facilities = listOf("cctv", "와이파이"),
            openingHours = "연중 무휴",
            description = ""
        )
    }

    // 즐겨찾기 카페 목록 필터링
    // cafesPage(전체 목록)에서 favoriteCafeIds에 포함된 카페만 추출
    val allCafes = cafesPage?.content?.map { mapSummaryToCafeInfo(it) } ?: emptyList()
    val favoriteCafes = allCafes.filter { cafe ->
        favoriteCafeIds.contains(cafe.id)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .verticalScroll(rememberScrollState())
    ) {
        // 상단 바
        TopBar(notificationCount = notificationCount) {
            navController.navigate("notifications")
        }

        // 환영 메시지
        WelcomeSection(userName = userData?.name)

        // --- 현재 사용 중인 세션 표시 ---
        // userData의 sessions 리스트에서 첫 번째 세션을 활성 세션으로 간주
        val activeSession = userData?.sessions?.firstOrNull()

        if (activeSession != null) {
            // 세션 정보를 UI 표시용 DTO로 변환
            val currentCafeDto = StudyCafeDetailDto(
                id = activeSession.studyCafe.id,
                name = activeSession.studyCafe.name ?: "알 수 없는 카페",
                address = activeSession.studyCafe.address ?: "주소 정보 없음",
                imageUrls = emptyList(),
                phoneNumber = "",
                facilities = emptyList(),
                openingHours = "",
                description = ""
            )

            // 경과 시간 계산
            val elapsedTime = remember(activeSession.startTime, timeUpdateTrigger) {
                val now = System.currentTimeMillis()
                val diff = now - activeSession.startTime
                val minutes = (diff / (1000 * 60)).toInt()
                val hours = minutes / 60
                val remMin = minutes % 60
                if (hours > 0) "${hours}시간 ${remMin}분" else "${remMin}분"
            }

            // 진행률 계산
            val currentPass = userData?.timePassess?.find { it.studyCafeId == activeSession.studyCafe.id }
            val totalTime = currentPass?.totalTime ?: 0L
            val usedTimeSeconds = (System.currentTimeMillis() - activeSession.startTime) / 1000
            val progress = if (totalTime > 0) usedTimeSeconds.toFloat() / totalTime.toFloat() else 0f

            CurrentUsageSection(
                cafe = currentCafeDto,
                elapsedTime = elapsedTime,
                progressValue = progress.coerceIn(0f, 1f),
                onViewDetail = { navController.navigate("current_usage_detail") },
                onEndUsage = {
                    // ViewModel의 세션 종료 함수 호출
                    // HomeViewModel 구조상 endCurrentSession()이 private이거나 중첩 함수로 되어 있어 호출이 불가능할 수 있음.
                    // 해당 함수가 public 멤버 함수로 올바르게 정의되어 있다면 아래 주석 해제.
                    // viewModel.endCurrentSession()
                }
            )
        } else {
            // 세션 없음 가이드
            NoActiveSessionGuide(
                onFindCafe = { navController.navigate("search") }
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // 카페 찾기 버튼
        CafeFindSection(
            onSearch = { navController.navigate("search") }
        )

        // 찜한 카페 목록
        FavoritesCafeSection(
            cafes = favoriteCafes,
            onViewAll = { navController.navigate("favorites") },
            onItemClick = { cafe -> navController.navigate("cafe_detail/${cafe.id}") }
        )

        Spacer(modifier = Modifier.height(20.dp))
    }
}

// ------------------------------------------------------------------------
// 하위 컴포저블 (Sub-Composables)
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
fun CurrentUsageSection(
    cafe: StudyCafeDetailDto,
    elapsedTime: String,
    progressValue: Float,
    onViewDetail: () -> Unit,
    onEndUsage: () -> Unit
) {
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
                        Image(
                            painter = painterResource(id = R.drawable.icon_cafe_sample_1),
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
                            color = Color(0xFFFF6B4A)
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
                        onClick = onViewDetail,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF6B4A)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "연장하기",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                    }

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
}

@Composable
fun CafeFindSection(onSearch: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .padding(horizontal = 20.dp, vertical = 10.dp)
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
                    painter = painterResource(id = R.drawable.icon_search),
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

@Composable
fun FavoritesCafeSection(
    cafes: List<StudyCafeDetailDto>,
    onViewAll: () -> Unit,
    onItemClick: (StudyCafeDetailDto) -> Unit
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
                        painter = painterResource(id = R.drawable.icon_search),
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
                    CafeCardHorizontal(cafe = cafe, onClick = onItemClick)
                }
            }
        }
    }
}

@Composable
fun CafeCardHorizontal(
    cafe: StudyCafeDetailDto,
    onClick: (StudyCafeDetailDto) -> Unit
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
                Image(
                    painter = painterResource(id = R.drawable.icon_cafe_sample_1),
                    contentDescription = cafe.name,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )

                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(10.dp)
                        .size(32.dp)
                        .clip(RoundedCornerShape(50))
                        .background(Color.White),
                    contentAlignment = Alignment.Center
                ) {
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
                    painter = painterResource(id = R.drawable.icon_search),
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
