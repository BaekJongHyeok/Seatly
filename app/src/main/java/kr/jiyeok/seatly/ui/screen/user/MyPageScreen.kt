package kr.jiyeok.seatly.ui.screen.user

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Coffee
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Help
import androidx.compose.material.icons.filled.LocalLibrary
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import kr.jiyeok.seatly.R
import kr.jiyeok.seatly.data.remote.response.StudyCafeSummaryDto
import kr.jiyeok.seatly.data.remote.response.UserInfoDetailDto
import kr.jiyeok.seatly.presentation.viewmodel.MyPageViewModel
import kr.jiyeok.seatly.presentation.viewmodel.UserViewModel
import kotlin.collections.forEach

// Colors from the design
private val BackgroundColor = Color.White
private val SurfaceColor = Color(0xFFFBFAF8) // Card background
private val PrimaryColor = Color(0xFFFF6B4A) // Orange
private val TextPrimary = Color(0xFF1A1A1A)
private val TextSecondary = Color(0xFF888888)
private val BorderColor = Color(0xFFEFEFEF)
private val IconBgOrange = Color(0xFFFFF4E6) // Light orange for icon bg
private val IconBgBlue = Color(0xFFEDF6FF)   // Light blue for icon bg
private val IconBgGreen = Color(0xFFE8F5E9)  // Light green for icon bg

@Composable
fun MyPageScreen(
    navController: NavController,
    viewModel: MyPageViewModel = hiltViewModel(),
    userViewModel: UserViewModel = hiltViewModel()
) {
    // 1. 상태 구독
    val userData by userViewModel.userProfile.collectAsState()
    val allCafes by viewModel.cafesPage.collectAsState()

    // 2. 초기 데이터 로드
    LaunchedEffect(Unit) {
        userViewModel.loadUserProfile()
        viewModel.loadAllCafes()
    }

    val favoriteCafeIds: Set<Long> = userData?.favoriteCafeIds?.toSet() ?: emptySet()
    val favoriteCafes: List<StudyCafeSummaryDto> = allCafes ?.filter { cafe -> cafe.id in favoriteCafeIds } ?: emptyList()

    Scaffold(
        topBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .background(BackgroundColor)
                    .padding(horizontal = 20.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "마이페이지",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                IconButton(
                    onClick = { /* Settings action */ },
                    modifier = Modifier.align(Alignment.CenterEnd)
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "설정",
                        tint = TextPrimary
                    )
                }
            }
        },
        containerColor = BackgroundColor
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(bottom = 24.dp)
        ) {
            // 1. Profile Section
            ProfileCardSection(userData)

            Spacer(modifier = Modifier.height(32.dp))

            // 2. Favorites Section
            FavoritesCafeSection2(
                cafes = favoriteCafes,
                onViewAll = { navController.navigate("favorites") },
                onItemClick = { cafe -> navController.navigate("cafe_detail/${cafe.id}") },
                onToggleFavorite = { cafe ->
                    viewModel.removeFavoriteCafe(cafe.id)
                }
            )

            Spacer(modifier = Modifier.height(32.dp))

            // 3. Menu List Section
            MenuListSection()

            Spacer(modifier = Modifier.height(40.dp))

            // 4. Logout Button
            LogoutButton()

            Spacer(modifier = Modifier.height(100.dp))
        }
    }
}

@Composable
fun ProfileCardSection(userData: UserInfoDetailDto?) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .shadow(
                elevation = 8.dp,
                shape = RoundedCornerShape(24.dp),
                spotColor = Color.Black.copy(alpha = 0.05f)
            )
            .clip(RoundedCornerShape(24.dp))
            .background(SurfaceColor)
            .padding(24.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            // Profile Image
            val imageUrl = userData?.imageUrl

            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(imageUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = "Profile",
                contentScale = ContentScale.Crop,
                placeholder = painterResource(R.drawable.ic_launcher_foreground),
                error = painterResource(R.drawable.ic_launcher_foreground),
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(Color.LightGray)
                    .border(1.dp, Color(0xFFEEEEEE), CircleShape)
            )

            Spacer(modifier = Modifier.width(20.dp))

            // Profile Info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${userData?.name}",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${userData?.email}",
                    fontSize = 14.sp,
                    color = TextSecondary
                )
            }

            // Arrow Button
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color.White)
                    .clickable { /* Navigate to edit profile */ },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = "Edit Profile",
                    tint = TextPrimary
                )
            }
        }
    }
}


@Composable
fun FavoritesCafeSection2(
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
                    CafeCardHorizontal2(
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
fun CafeCardHorizontal2(
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
fun MenuListSection() {
    Column(modifier = Modifier.padding(horizontal = 20.dp)) {
        // Notification Toggle
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(SurfaceColor),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Notifications,
                        contentDescription = null,
                        tint = TextPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = "알림 설정",
                    fontSize = 16.sp,
                    color = TextPrimary
                )
            }

            var checked by remember { mutableStateOf(true) }
            Switch(
                checked = checked,
                onCheckedChange = { checked = it },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = PrimaryColor,
                    uncheckedThumbColor = Color.White,
                    uncheckedTrackColor = Color(0xFFE0E0E0),
                    uncheckedBorderColor = Color.Transparent
                )
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Support
        MenuItem(icon = Icons.Default.Help, text = "고객 지원")

        Spacer(modifier = Modifier.height(12.dp))

        // Terms
        MenuItem(icon = Icons.Default.Description, text = "약관 및 정책")
    }
}

@Composable
fun MenuItem(icon: ImageVector, text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { /* Navigate */ }
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(SurfaceColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = TextPrimary,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = text,
                fontSize = 16.sp,
                color = TextPrimary
            )
        }
        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = Color(0xFFCCCCCC)
        )
    }
}

@Composable
fun LogoutButton() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
    ) {
        Button(
            onClick = { /* Logout */ },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .border(1.dp, PrimaryColor.copy(alpha = 0.5f), RoundedCornerShape(16.dp)),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.White
            ),
            elevation = ButtonDefaults.buttonElevation(0.dp)
        ) {
            Text(
                text = "로그아웃",
                color = PrimaryColor,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

// Data Models & Mock Data
data class FavoriteItem(
    val name: String,
    val rating: Double,
    val icon: ImageVector,
    val iconTint: Color,
    val iconBgColor: Color
)

fun getDummyFavorites(): List<FavoriteItem> {
    return listOf(
        FavoriteItem("스터디허브 A", 4.8, Icons.Default.Storefront, Color(0xFFFFB74D), IconBgOrange),
        FavoriteItem("강남 라이브러리", 4.9, Icons.Default.LocalLibrary, Color(0xFF64B5F6), IconBgBlue),
        FavoriteItem("숲속 스터디", 4.5, Icons.Default.Coffee, Color(0xFF81C784), IconBgGreen)
    )
}
