package kr.jiyeok.seatly.ui.screen.user

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.compose.ui.res.stringResource
import kr.jiyeok.seatly.R
import kr.jiyeok.seatly.presentation.viewmodel.CafeDetailViewModel
import androidx.compose.ui.draw.shadow
import androidx.compose.foundation.shape.RoundedCornerShape
import kr.jiyeok.seatly.ui.theme.*
import kr.jiyeok.seatly.presentation.viewmodel.AuthViewModel
import kr.jiyeok.seatly.ui.component.common.AppTopBar

/**
 * 카페 상세 관리 화면
 * - 카페 정보, 멤버 관리, 좌석 관리 탭으로 구성
 */
@Composable
fun UserCafeDetailScreen(
    navController: NavController,
    cafeId: Long,
    viewModel: CafeDetailViewModel = hiltViewModel(),
    authViewModel: AuthViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    // State 구독
    val uiState by viewModel.uiState.collectAsState()
    val isAnyLoading by viewModel.isAnyLoading.collectAsState()

    // 초기 데이터 로드
    LaunchedEffect(cafeId) {
        viewModel.loadCafeDetailInfos(cafeId)
    }
    
    // WebSocket 구독 관리
    LaunchedEffect(cafeId) {
        viewModel.subscribeToCafeEvents(cafeId)
    }
    
    // Auth 정보 (더 이상 웹소켓 구독에 사용되지 않지만, 다른 곳에서 사용될 수 있음)
    val userData by authViewModel.userData.collectAsState()
    
    // 영업 시간 확인 로직
    val isOpen = remember(uiState.cafeInfo?.openingHours) {
        checkIsCafeOpen(uiState.cafeInfo?.openingHours)
    }

    // 이벤트 처리 (Toast 메시지)
    LaunchedEffect(Unit) {
        viewModel.events.collect { message ->
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }

    Scaffold(
        topBar = {
            AdminCafeDetailTopBar(
                title = uiState.cafeInfo?.name ?: stringResource(R.string.cafe_detail_title),
                onBackClick = { navController.popBackStack() }
            )
        },
        bottomBar = {
            // 하단 고정 버튼 (좌석 선택하기)
            Surface(
                modifier = Modifier.fillMaxWidth().height(80.dp),
                color = ColorBgBeige.copy(alpha = 0.98f),
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, ColorWhite),
                shadowElevation = 8.dp
            ) {
                Box(
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Button(
                        onClick = {
                            // 좌석 선택 화면으로 이동
                            navController.navigate("user/cafe/$cafeId/seats")
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight(),
                        shape = RoundedCornerShape(12.dp),

                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isOpen) ColorPrimaryOrange else ColorTextGray,
                            contentColor = ColorWhite, // Text color matches container for disabled in Material3 usually, but force white or allow default disabled
                            disabledContainerColor = ColorTextGray,
                            disabledContentColor = ColorWhite
                        ),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp),
                        enabled = isOpen
                    ) {
                        Text(
                            text = if (isOpen) stringResource(R.string.cafe_detail_select_seat) else stringResource(R.string.cafe_detail_closed),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = ColorWhite
                        )
                    }
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = ColorWhite
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // 전체 로딩 표시 (초기 로딩 시에만)
            if (isAnyLoading && uiState.cafeInfo == null) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = ColorPrimaryOrange,
                        modifier = Modifier.size(48.dp)
                    )
                }
            } else {
                // 카페 상세 정보 컨텐츠 (기존 UserCafeInfoTab 내용 통합)
                UserCafeInfoTab(
                    viewModel = viewModel,
                    navController = navController
                )
            }
        }
    }
}

// =====================================================
// Top Bar
// =====================================================

/**
 * 카페 상세 화면 상단 바
 */
@Composable
private fun AdminCafeDetailTopBar(
    title: String,
    onBackClick: () -> Unit
) {
    AppTopBar(
        title = title,
        leftContent = {
            Icon(
                imageVector = Icons.Default.ArrowBack,
                contentDescription = stringResource(R.string.cafe_detail_back_desc),
                tint = ColorTextBlack,
                modifier = Modifier
                    .size(24.dp)
                    .clickable { onBackClick() }
            )
        },
    )

}

/**
 * 영업 시간 파싱 및 현재 영업 여부 확인
 */
private fun checkIsCafeOpen(operatingHours: String?): Boolean {
    if (operatingHours.isNullOrBlank()) return false // 정보 없으면 닫힘 처리? or 열림? -> 일단 닫힘 or 기본값 정책. 여기선 false로.
    // However, if truly unknown, maybe default to true to not block?
    // Let's look at UserCafeInfoTab logic: 
    // "todayHours == null || isTodayClosed || todayHours == "Open" return false" 
    // Wait, if todayHours is null, it returns false.
    
    // Reuse logic style:
    try {
        val hoursMap = operatingHours.split(",").mapNotNull { entry ->
            val parts = entry.trim().split("=")
            if (parts.size == 2) parts[0].trim().uppercase() to parts[1].trim() else null
        }.toMap()
        
        val commonHours = hoursMap["ALL"]
        val todayKey = java.time.LocalDate.now().dayOfWeek.name
        val todayHours = hoursMap[todayKey] ?: commonHours
        
        // "Closed" or null -> Closed
        if (todayHours == null || todayHours.equals("Closed", ignoreCase = true)) return false
        if (todayHours == "Open") return true // "Open" implies 24h or always? Or just placeholder. Assuming Open.
        
        // Parse time range "09:00~22:00"
        val timePattern = Regex("(\\d{1,2}):(\\d{2})")
        val matches = timePattern.findAll(todayHours).toList()
        if (matches.size < 2) return false
        
        val openMinutes = matches[0].groupValues[1].toInt() * 60 + matches[0].groupValues[2].toInt()
        val closeMinutes = matches[1].groupValues[1].toInt() * 60 + matches[1].groupValues[2].toInt()
        val now = java.time.LocalTime.now()
        val nowMinutes = now.hour * 60 + now.minute
        
        // Handle overnight (e.g. 10:00 ~ 02:00) 
        // If close < open, it means next day.
        // For simplicity, assuming standard day hours for now as in UserCafeInfoTab.
        // UserCafeInfoTab uses: nowMinutes in openMinutes..closeMinutes
        
        return nowMinutes in openMinutes..closeMinutes
    } catch (e: Exception) {
        return false
    }
}