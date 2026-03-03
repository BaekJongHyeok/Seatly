package kr.jiyeok.seatly.ui.screen.user

import android.graphics.Bitmap
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
import androidx.compose.material.icons.filled.ConfirmationNumber
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.compose.ui.res.stringResource
import kr.jiyeok.seatly.R
import kr.jiyeok.seatly.data.remote.response.StudyCafeSummaryDto
import kr.jiyeok.seatly.presentation.viewmodel.AuthViewModel
import kr.jiyeok.seatly.presentation.viewmodel.LogoutState
import kr.jiyeok.seatly.presentation.viewmodel.MyPageViewModel
import kr.jiyeok.seatly.ui.component.common.AppTopBar
import kr.jiyeok.seatly.ui.navigation.UserMyPageNavigator
import kr.jiyeok.seatly.ui.theme.*
import kr.jiyeok.seatly.util.SharedPreferencesHelper

@Composable
fun UserMyPageScreen(
    navController: NavController,
    viewModel: MyPageViewModel = hiltViewModel(),
    authViewModel: AuthViewModel
) {
    val context = LocalContext.current

    // 뒤로가기 종료 핸들러 추가
    kr.jiyeok.seatly.ui.component.common.ExitBackHandler()

    // UI State 수집 - lifecycle aware
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val logoutState by viewModel.logoutState.collectAsStateWithLifecycle()

    val navigator = remember { UserMyPageNavigator(navController) }
    var notiEnabled by remember { mutableStateOf(SharedPreferencesHelper.getNotificationEnabled(context)) }

    // 처음 한 번만 로드
    LaunchedEffect(Unit) {
        viewModel.loadData()
    }

    // 로그아웃 상태 처리
    LaunchedEffect(logoutState) {
        when (logoutState) {
            is LogoutState.Success -> {
                // 1. 서버/로컬 로그아웃
                authViewModel.logout()
                // 2. 자동 로그인 정보 완전히 제거
                SharedPreferencesHelper.clearAutoLoginCredentials(context)
                // 3. 로그인 화면으로 이동 + 백스텝 정리
                navigator.navigateToLoginAndClearStack()
                // 4. 상태 초기화
                viewModel.resetLogoutState()
            }
            is LogoutState.Error -> {
                // 에러 처리는 events로 처리됨
                viewModel.resetLogoutState()
            }
            else -> { /* Idle, Loading */ }
        }
    }

    // 이벤트 수집 (토스트 메시지 등)
    LaunchedEffect(Unit) {
        viewModel.events.collect { message ->
            // TODO: 토스트나 스낵바로 표시
            // Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(ColorWhite)
                .verticalScroll(rememberScrollState())
        ) {
            // 상단 헤더 (제목)
            AppTopBar(title = stringResource(R.string.mypage_title))

            when {
                uiState.isLoading -> {
                    // 로딩 상태
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
                    // 에러 상태
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(400.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = uiState.error ?: stringResource(R.string.mypage_unknown_error),
                            fontSize = 14.sp,
                            color = ColorTextDarkGray
                        )
                    }
                }
                else -> {
                    // 정상 상태 - 데이터 표시
                    // 프로필 섹션
                    ProfileCardSection(
                        userName = uiState.userInfo?.name ?: "사용자",
                        userEmail = uiState.userInfo?.email ?: "email@example.com",
                        profileBitmap = uiState.userProfileImage,
                        onEditClick = { navigator.navigateToEditProfile() }
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // 시간권 섹션
                    TimePassesSection(
                        timePasses = uiState.myTimePasses,
                        allCafes = uiState.allCafes,
                        navigator = navigator
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // 관리 및 설정 섹션
                    ManagementSection(
                        notificationEnabled = notiEnabled,
                        onNotificationToggle = { enabled ->
                            notiEnabled = enabled
                            SharedPreferencesHelper.saveNotificationEnabled(context, enabled)
                        },
                        onNotificationClick = { navigator.navigateToNotifications() },
                        onSettingsClick = { navigator.navigateToAppSettings() }
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // 로그아웃 버튼
                    LogoutButton(
                        onLogoutClick = { viewModel.logout() },
                        isLoading = logoutState is LogoutState.Loading,
                        modifier = Modifier.padding(horizontal = 20.dp)
                    )

                    // 하단 여유 공간
                    Spacer(modifier = Modifier.height(100.dp))
                }
            }
        }
    }
}

// =====================================================
// UI Components
// =====================================================

/**
 * 프로필 카드 섹션
 * Bitmap으로 이미지 표시
 */
@Composable
fun ProfileCardSection(
    userName: String,
    userEmail: String,
    profileBitmap: Bitmap?,
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
            // 사용자 프로필 이미지
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(ColorBrownBg)
                    .border(width = 1.dp, color = ColorBorderLight, shape = CircleShape)
            ) {
                if (profileBitmap != null) {
                    Image(
                        bitmap = profileBitmap.asImageBitmap(),
                        contentDescription = userName,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Image(
                        painter = painterResource(R.drawable.img_default_cafe),
                        contentDescription = userName,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
            }

            // 사용자 정보 (이름, 이메일)
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

            // 프로필 수정 버튼
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
                    contentDescription = stringResource(R.string.mypage_edit_profile_desc),
                    tint = ColorTextBlack,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

/**
 * 내 시간권 헤더 섹션
 */
@Composable
fun TimePassesSection(
    timePasses: List<kr.jiyeok.seatly.data.remote.response.UserTimePass>,
    allCafes: List<kr.jiyeok.seatly.data.remote.response.StudyCafeSummaryDto>,
    navigator: UserMyPageNavigator
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
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
                text = stringResource(R.string.mypage_time_passes),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = ColorTextBlack
            )

            if (timePasses.size > 2) {
                Text(
                    text = stringResource(R.string.mypage_see_more),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = ColorTextDarkGray,
                    modifier = Modifier.clickable { navigator.navigateToTimePasses() }
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (timePasses.isEmpty()) {
            EmptyTimePassCard(
                modifier = Modifier.padding(horizontal = 20.dp)
            )
        } else {
            // 시간권 목록 (최대 2개)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                timePasses.take(2).forEach { pass ->
                    val cafe = allCafes.find { it.id == pass.studyCafeId }
                    if (cafe != null) {
                        TimePassCard(
                            cafe = cafe,
                            timePass = pass,
                            onClick = { navigator.navigateToCafeDetail(cafe.id) }
                        )
                    }
                }
            }
        }
    }
}

/**
 * 빈 시간권 카드
 */
@Composable
fun EmptyTimePassCard(modifier: Modifier = Modifier) {
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
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Edit, // Or a more appropriate icon like Receipt or Schedule
                contentDescription = stringResource(R.string.mypage_no_time_pass_desc),
                tint = ColorTextVeryLightGray,
                modifier = Modifier.size(32.dp)
            )
            Text(
                text = stringResource(R.string.mypage_no_time_pass_message),
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = ColorTextDarkGray
            )
        }
    }
}

/**
 * 시간권 카드
 */
@Composable
fun TimePassCard(
    cafe: kr.jiyeok.seatly.data.remote.response.StudyCafeSummaryDto,
    timePass: kr.jiyeok.seatly.data.remote.response.UserTimePass,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 4.dp,
                shape = RoundedCornerShape(16.dp),
                ambientColor = ColorTextBlack.copy(alpha = 0.1f),
                spotColor = ColorTextBlack.copy(alpha = 0.05f)
            )
            .clip(RoundedCornerShape(16.dp))
            .background(ColorCardBg)
            .clickable { onClick() }
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 카페 이름 및 아이콘
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(ColorWhite)
                        .border(1.dp, ColorBorderLight, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.ConfirmationNumber,
                        contentDescription = null,
                        tint = ColorPrimaryOrange,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Text(
                    text = cafe.name,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = ColorTextBlack,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // 남은 시간 표시
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                // 남은 시간 계산 (초 단위)
                val hours = timePass.leftTime / (60 * 60)
                val minutes = (timePass.leftTime / 60) % 60
                
                Row(verticalAlignment = Alignment.Bottom) {
                    if (hours > 0) {
                        Text(
                            text = "${hours}",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = ColorPrimaryOrange
                        )
                        Text(
                            text = stringResource(R.string.mypage_suffix_hours),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = ColorTextGray,
                            modifier = Modifier.padding(bottom = 2.dp)
                        )
                    }
                    Text(
                        text = "${minutes}",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = ColorPrimaryOrange
                    )
                    Text(
                        text = stringResource(R.string.mypage_suffix_minutes),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = ColorTextGray,
                        modifier = Modifier.padding(bottom = 2.dp)
                    )
                }
                

            }
        }
    }
}

/**
 * 관리 및 설정 섹션
 * 알림 설정 토글과 앱 설정 버튼
 */
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
        // 섹션 제목
        Text(
            text = stringResource(R.string.mypage_management_title),
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = ColorTextBlack,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
        )

        // 알림 설정
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
                // 알림 아이콘
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
                // 알림 설정 텍스트
                Text(
                    text = "알림 설정",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = ColorTextBlack
                )
            }
            // 토글 스위치
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

        // 앱 설정
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
                // 설정 아이콘
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
                // 앱 설정 텍스트
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

/**
 * 로그아웃 버튼
 * 로딩 상태 표시 추가
 */
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
                text = stringResource(R.string.mypage_logout),
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = ColorPrimaryOrange
            )
        }
    }
}
