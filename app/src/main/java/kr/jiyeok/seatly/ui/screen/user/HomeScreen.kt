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
import androidx.compose.runtime.remember
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
import kr.jiyeok.seatly.R
import kr.jiyeok.seatly.data.remote.response.StudyCafeSummaryDto
import kr.jiyeok.seatly.presentation.viewmodel.AuthViewModel
import kr.jiyeok.seatly.presentation.viewmodel.HomeViewModel

data class CafeInfo(
    val id: String,
    val name: String = "",
    val address: String = "",
    val imageRes: Int,
    val rating: Double = 4.8,
    val reviewCount: Int = 128,
    val isFavorite: Boolean = false,
    val usageTime: String = "",
    val price: Int = 0
)

@Composable
fun HomeScreen(
    navController: NavController,
    viewModel: HomeViewModel = hiltViewModel(),
    authViewModel: AuthViewModel = hiltViewModel()
) {
    val notificationCount = 1

    // Local UI states (kept to preserve original layout/interaction)
    val isUsingCafeState = remember { androidx.compose.runtime.mutableStateOf(false) }

    // Observe viewmodel data
    val currentUsage by viewModel.currentUsage.collectAsState()
    val favoritePage by viewModel.favoritePage.collectAsState()
    val recentCafesDto by viewModel.recentCafes.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    
    // Get user data from AuthViewModel
    val userData by authViewModel.userData.collectAsState()

    // Map DTO -> local CafeInfo for UI (use sample image if backend image absent)
    fun mapSummaryToCafeInfo(dto: StudyCafeSummaryDto?): CafeInfo? {
        if (dto == null) return null
        val image = if (!dto.mainImageUrl.isNullOrEmpty()) {
            // We only have URL from backend; keep UI identical by using sample drawable.
            // In real app you'd use Coil to load dto.mainImageUrl in Image composable directly.
            R.drawable.icon_cafe_sample_1
        } else {
            R.drawable.icon_cafe_sample_1
        }
        return CafeInfo(
            id = dto.id.toString(),
            name = dto.name ?: "",
            address = dto.address ?: "",
            imageRes = image,
            rating = dto.rating ?: 4.8,
            reviewCount = 128,
            isFavorite = dto.isFavorite,
            usageTime = "",
            price = 0
        )
    }

    // Convert lists
    val favoriteCafes = favoritePage?.content?.mapNotNull { mapSummaryToCafeInfo(it) } ?: emptyList()
    val recentCafes = recentCafesDto.map {
        // For recent, backend DTO may contain additional metadata; we map basic fields and place a usageTime placeholder
        val image = if (!it.mainImageUrl.isNullOrEmpty()) R.drawable.icon_cafe_sample_1 else R.drawable.icon_cafe_sample_1
        CafeInfo(
            id = it.id.toString(),
            name = it.name ?: "",
            address = it.address ?: "",
            imageRes = image,
            rating = it.rating ?: 4.8,
            reviewCount = 128,
            isFavorite = it.isFavorite,
            usageTime = "", // UI will show "최근 이용 내역" if empty
            price = 0
        )
    }.take(3)

    // Capture currentUsage into a local val to allow safe smart-cast after null check
    val usage = currentUsage

    // If currentUsage exists, show using state
    if (usage != null) {
        isUsingCafeState.value = true
    } else {
        // keep existing local toggle if backend returns no current usage
        // do not forcibly set false here because user may want to see the sample UI
    }

    // Load data on first composition
    LaunchedEffect(Unit) {
        viewModel.loadHomeData()
    }

    // Collect events to show (no direct UI binding here; apps typically show toasts/snackbar)
    LaunchedEffect(Unit) {
        viewModel.events.collect { /* You can show Toast/Snackbar here using scaffoldState or other mechanism */ }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .verticalScroll(rememberScrollState())
    ) {
        // TopBar: 사용자 이름 제거 (앱 타이틀 + 알림만 표시)
        TopBar(notificationCount) { navController.navigate("notifications") }

        // 기존 WelcomeSection 유지 - 사용자 이름 표시
        WelcomeSection(userName = userData?.name)

        // 현재 사용중 섹션 (원래 UI로 복원)
        if (isUsingCafeState.value && usage != null) {
            // Build a CafeInfo from currentUsage for UI
            val cafe = CafeInfo(
                id = usage.cafeId.toString(),
                name = usage.cafeName ?: "",
                address = usage.cafeAddress ?: "",
                imageRes = usage.cafeImageUrl?.let { _ ->
                    // backend gives url; we still use drawable for identical UI
                    R.drawable.icon_cafe_sample_1
                } ?: R.drawable.icon_cafe_sample_1,
                usageTime = usage.startedAt ?: "",
                rating = 4.8
            )
            val elapsedTime = run {
                // Display elapsed in friendly format (we have elapsedMillis)
                val minutes = (usage.elapsedMillis / 1000L / 60L).toInt()
                val hours = minutes / 60
                val remMin = minutes % 60
                if (hours > 0) "${hours}시간 ${remMin}분" else "${remMin}분"
            }
            val progressValue = 0.75f // backend does not provide progress; keep UI same
            CurrentUsageSection(
                cafe = cafe,
                elapsedTime = elapsedTime,
                progressValue = progressValue,
                onViewDetail = { navController.navigate("current_usage_detail") },
                onEndUsage = { viewModel.endCurrentUsage() }
            )
        } else {
            // Show guide when user has no active session
            NoActiveSessionGuide(
                onFindCafe = { navController.navigate("search") }
            )
        }

        CafeFindSection { navController.navigate("search") }

        FavoritesCafeSection(
            cafes = favoriteCafes,
            onViewAll = { navController.navigate("favorites") },
            onItemClick = { cafe -> navController.navigate("cafe_detail/${cafe.id}") }
        )

        RecentCafeSection(
            cafes = if (recentCafes.isNotEmpty()) recentCafes else listOf(
                CafeInfo(
                    "1",
                    "명지 스터디카페",
                    "서울시 강서구",
                    R.drawable.icon_cafe_sample_1,
                    usageTime = "12월 15일 10:00 AM·4시간30분",
                    price = 13500
                ),
                CafeInfo(
                    "2",
                    "강남 그린 램프",
                    "서울시 강남구",
                    R.drawable.icon_cafe_sample_1,
                    usageTime = "12월 12일 02:00 PM·2시간",
                    price = 8000
                )
            ).take(3),
            onViewAll = { navController.navigate("recent") },
            onItemClick = { cafe -> navController.navigate("cafe_detail/${cafe.id}") }
        )

        Spacer(modifier = Modifier.height(20.dp))
    }
}

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
                    contentDescription = "Notifications",
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

// 원래의 CurrentUsageSection 레이아웃으로 복원
@Composable
fun CurrentUsageSection(
    cafe: CafeInfo,
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
                .clickable {}
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
                            painter = painterResource(id = cafe.imageRes),
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
                            color = Color.Black
                        )
                        Spacer(modifier = Modifier.height(1.dp))
                        Text(
                            text = cafe.address,
                            fontSize = 12.sp,
                            color = Color(0xFFA0A0A0)
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
                            progress = progressValue,
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
                    contentDescription = "Search",
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
fun FavoritesCafeSection(cafes: List<CafeInfo>, onViewAll: () -> Unit, onItemClick: (CafeInfo) -> Unit) {
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
            // Show guide when no favorites
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
                        contentDescription = "No favorites",
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
fun CafeCardHorizontal(cafe: CafeInfo, onClick: (CafeInfo) -> Unit) {
    Box(
        modifier = Modifier
            .width(155.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(Color(0xFFFBFAF8))
            .border(1.dp, Color(0xFFE8E6E1), RoundedCornerShape(18.dp))
            .clickable { onClick(cafe) } // 클릭 시 전달된 람다 호출
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
                    painter = painterResource(id = cafe.imageRes),
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
                    Text(
                        text = "❤️",
                        fontSize = 16.sp
                    )
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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "★",
                        fontSize = 12.sp,
                        color = Color(0xFFFF6B4A)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = "${cafe.rating}",
                        fontSize = 12.sp,
                        color = Color.Black,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    Text(
                        text = "(${cafe.reviewCount})",
                        fontSize = 11.sp,
                        color = Color(0xFF999999)
                    )
                }
            }
        }
    }
}

@Composable
fun RecentCafeSection(cafes: List<CafeInfo>, onViewAll: () -> Unit, onItemClick: (CafeInfo) -> Unit) {
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
                text = "최근 이용",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )
            Text(
                text = "더보기",
                fontSize = 14.sp,
                color = Color(0xFF999999),
                fontWeight = FontWeight.Medium,
                modifier = Modifier.clickable { onViewAll() }
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            cafes.forEach { cafe ->
                CafeCardVertical(cafe = cafe, onClick = onItemClick)
            }
        }
    }
}

@Composable
fun CafeCardVertical(cafe: CafeInfo, onClick: (CafeInfo) -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Color(0xFFFBFAF8))
            .border(1.dp, Color(0xFFE8E6E1), RoundedCornerShape(18.dp))
            .clickable { onClick(cafe) } // 클릭 시 전달된 람다 호출
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFD4C5B9)),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = cafe.imageRes),
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
                    color = Color.Black
                )
                Spacer(modifier = Modifier.height(1.dp))
                Text(
                    text = cafe.address,
                    fontSize = 12.sp,
                    color = Color(0xFFA0A0A0)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = cafe.usageTime.ifEmpty { "최근 이용 내역" },
                    fontSize = 12.sp,
                    color = Color(0xFF666666)
                )
            }
            Text(
                text = "${cafe.price}원",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.Black
            )
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
                    contentDescription = "No active session",
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