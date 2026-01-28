package kr.jiyeok.seatly.ui.screen.user

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import kr.jiyeok.seatly.data.remote.response.StudyCafeSummaryDto
import kr.jiyeok.seatly.presentation.viewmodel.AuthViewModel
import kr.jiyeok.seatly.presentation.viewmodel.LogoutState
import kr.jiyeok.seatly.presentation.viewmodel.MyPageViewModel
import kr.jiyeok.seatly.ui.component.common.AppTopBar
import kr.jiyeok.seatly.ui.navigation.UserMyPageNavigator
import kr.jiyeok.seatly.ui.theme.*
import kr.jiyeok.seatly.util.AppSettings
import kr.jiyeok.seatly.ui.component.common.ExitBackHandler
import org.jetbrains.compose.resources.painterResource
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import seatly.composeapp.generated.resources.Res
import seatly.composeapp.generated.resources.img_default_cafe

@Composable
fun UserMyPageScreen(
    navController: NavController,
    viewModel: MyPageViewModel = koinViewModel(),
    authViewModel: AuthViewModel = koinViewModel()
) {
    val appSettings: AppSettings = koinInject()
    
    ExitBackHandler()

    val uiState by viewModel.uiState.collectAsState()
    val logoutState by viewModel.logoutState.collectAsState()

    val navigator = remember { UserMyPageNavigator(navController) }
    var notiEnabled by remember { mutableStateOf(appSettings.getNotificationEnabled()) }

    LaunchedEffect(Unit) {
        viewModel.loadData()
    }

    LaunchedEffect(logoutState) {
        when (logoutState) {
            is LogoutState.Success -> {
                authViewModel.logout()
                appSettings.clearAutoLoginCredentials()
                navigator.navigateToLoginAndClearStack()
                viewModel.resetLogoutState()
            }
            is LogoutState.Error -> {
                viewModel.resetLogoutState()
            }
            else -> { /* Idle, Loading */ }
        }
    }

    LaunchedEffect(Unit) {
        viewModel.events.collect { message ->
            println("Event: $message")
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(ColorWhite)
                .verticalScroll(rememberScrollState())
        ) {
            AppTopBar(title = "마이페이지")

            when {
                uiState.isLoading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(400.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            color = ColorPrimaryOrange,
                            strokeWidth = 3.dp
                        )
                    }
                }
                uiState.error != null -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(400.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = uiState.error ?: "알 수 없는 오류",
                            fontSize = 14.sp,
                            color = ColorTextDarkGray
                        )
                    }
                }
                else -> {
                    ProfileCardSection(
                        userName = uiState.userInfo?.name ?: "사용자",
                        userEmail = uiState.userInfo?.email ?: "email@example.com",
                        profileBitmap = uiState.userProfileImage,
                        onEditClick = { navigator.navigateToEditProfile() }
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    FavoritesSection(
                        onViewAll = { navigator.navigateToFavorites() },
                        favoriteCafes = uiState.favoriteCafes,
                        cafeImages = uiState.cafeImages,
                        navigator = navigator
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    ManagementSection(
                        notificationEnabled = notiEnabled,
                        onNotificationToggle = { enabled ->
                            notiEnabled = enabled
                            appSettings.saveNotificationEnabled(enabled)
                        },
                        onNotificationClick = { navigator.navigateToNotifications() },
                        onSettingsClick = { navigator.navigateToAppSettings() }
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    LogoutButton(
                        onLogoutClick = { viewModel.logout() },
                        isLoading = logoutState is LogoutState.Loading,
                        modifier = Modifier.padding(horizontal = 20.dp)
                    )

                    Spacer(modifier = Modifier.height(100.dp))
                }
            }
        }
    }
}

@Composable
fun ProfileCardSection(
    userName: String,
    userEmail: String,
    profileBitmap: ImageBitmap?,
    onEditClick: () -> Unit
) {
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
            .background(ColorCardBg)
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(ColorBrownBg)
                    .border(width = 1.dp, color = ColorBorderLight, shape = CircleShape)
            ) {
                if (profileBitmap != null) {
                    Image(
                        bitmap = profileBitmap,
                        contentDescription = userName,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Image(
                        painter = painterResource(Res.drawable.img_default_cafe),
                        contentDescription = userName,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = userName,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = ColorTextBlack,
                    modifier = Modifier.padding(bottom = 2.dp)
                )
                Text(
                    text = userEmail,
                    fontSize = 11.sp,
                    color = ColorTextLightGray
                )
            }

            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(ColorWhite)
                    .border(width = 1.dp, color = ColorBorderLight, shape = CircleShape)
                    .clickable { onEditClick() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "프로필 수정",
                    tint = ColorTextBlack,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
fun FavoritesSection(
    onViewAll: () -> Unit,
    favoriteCafes: List<StudyCafeSummaryDto>,
    cafeImages: Map<Long, ImageBitmap>,
    navigator: UserMyPageNavigator
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "찜한 목록",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = ColorTextBlack
        )
        Text(
            text = "더보기",
            fontSize = 12.sp,
            color = ColorPrimaryOrange,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.clickable { onViewAll() }
        )
    }

    if (favoriteCafes.isEmpty()) {
        EmptyFavoritesCard(
            modifier = Modifier.padding(horizontal = 20.dp)
        )
    } else {
        FavoriteCafesListSection(
            cafes = favoriteCafes,
            cafeImages = cafeImages,
            onCafeClick = { cafe ->
                navigator.navigateToCafeDetail(cafe.id)
            }
        )
    }
}

@Composable
fun EmptyFavoritesCard(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(160.dp)
            .shadow(
                elevation = 8.dp,
                shape = RoundedCornerShape(16.dp),
                ambientColor = ColorTextBlack.copy(alpha = 0.15f),
                spotColor = ColorTextBlack.copy(alpha = 0.08f)
            )
            .clip(RoundedCornerShape(16.dp))
            .background(ColorCardBg),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "아직 찜한 카페가 없어요",
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = ColorTextDarkGray
        )
    }
}

@Composable
fun FavoriteCafesListSection(
    cafes: List<StudyCafeSummaryDto>,
    cafeImages: Map<Long, ImageBitmap>,
    onCafeClick: (StudyCafeSummaryDto) -> Unit
) {
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(
            items = cafes,
            key = { cafe -> cafe.id }
        ) { cafe ->
            FavoritesCafeCard(
                cafe = cafe,
                cafeBitmap = cafeImages[cafe.id],
                onClick = { onCafeClick(cafe) }
            )
        }
    }
}

@Composable
fun FavoritesCafeCard(
    cafe: StudyCafeSummaryDto,
    cafeBitmap: ImageBitmap?,
    onClick: () -> Unit
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
            .background(ColorCardBg)
            .clickable { onClick() }
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
                    .background(ColorBrownBg)
            ) {
                if (cafeBitmap != null) {
                    Image(
                        bitmap = cafeBitmap,
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
                        .align(Alignment.TopEnd)
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
                        contentDescription = "찜함",
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

@Composable
fun ManagementSection(
    notificationEnabled: Boolean,
    onNotificationToggle: (Boolean) -> Unit,
    onNotificationClick: () -> Unit,
    onSettingsClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(ColorWhite)
            .padding(vertical = 12.dp)
    ) {
        Text(
            text = "관리 및 설정",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = ColorTextBlack,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 4.dp)
                .shadow(
                    elevation = 8.dp,
                    shape = RoundedCornerShape(16.dp),
                    ambientColor = ColorTextBlack.copy(alpha = 0.15f),
                    spotColor = ColorTextBlack.copy(alpha = 0.08f)
                )
                .clip(RoundedCornerShape(16.dp))
                .background(ColorCardBg)
                .padding(14.dp)
                .clickable { onNotificationClick() },
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(ColorWhite),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Notifications,
                        contentDescription = "알림",
                        tint = ColorPrimaryOrange,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Text(
                    text = "알림 설정",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = ColorTextBlack
                )
            }
            Switch(
                checked = notificationEnabled,
                onCheckedChange = onNotificationToggle,
                modifier = Modifier.scale(0.8f),
                colors = SwitchDefaults.colors(
                    checkedThumbColor = ColorWhite,
                    checkedTrackColor = ColorPrimaryOrange,
                    uncheckedThumbColor = ColorWhite,
                    uncheckedTrackColor = ColorToggleInactive
                )
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 4.dp)
                .shadow(
                    elevation = 8.dp,
                    shape = RoundedCornerShape(16.dp),
                    ambientColor = ColorTextBlack.copy(alpha = 0.15f),
                    spotColor = ColorTextBlack.copy(alpha = 0.08f)
                )
                .clip(RoundedCornerShape(16.dp))
                .background(ColorCardBg)
                .clickable { onSettingsClick() }
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(ColorWhite),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "설정",
                        tint = ColorPrimaryOrange,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Text(
                    text = "앱 설정",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = ColorTextBlack
                )
            }
        }
    }
}

@Composable
fun LogoutButton(
    onLogoutClick: () -> Unit,
    isLoading: Boolean = false,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onLogoutClick,
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp),
        enabled = !isLoading,
        colors = ButtonDefaults.buttonColors(containerColor = ColorWhite),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(width = 2.dp, color = ColorPrimaryOrange)
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                color = ColorPrimaryOrange,
                strokeWidth = 2.dp
            )
        } else {
            Text(
                text = "로그아웃",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = ColorPrimaryOrange
            )
        }
    }
}
