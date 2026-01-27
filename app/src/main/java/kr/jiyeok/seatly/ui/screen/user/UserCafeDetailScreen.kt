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
import kr.jiyeok.seatly.presentation.viewmodel.CafeDetailViewModel
import kr.jiyeok.seatly.ui.component.common.AppTopBar
import kr.jiyeok.seatly.ui.theme.*

/**
 * 카페 상세 관리 화면
 * - 카페 정보, 멤버 관리, 좌석 관리 탭으로 구성
 */
@Composable
fun UserCafeDetailScreen(
    navController: NavController,
    cafeId: Long,
    viewModel: CafeDetailViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    // State 구독
    val uiState by viewModel.uiState.collectAsState()
    val isAnyLoading by viewModel.isAnyLoading.collectAsState()

    // 탭 상태
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabs = listOf("상세 정보", "좌석 이용")

    // 초기 데이터 로드
    LaunchedEffect(cafeId) {
        viewModel.loadCafeDetailInfos(cafeId)
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
                title = uiState.cafeInfo?.name ?: "카페 관리",
                onBackClick = { navController.popBackStack() }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = ColorWhite
    ) { paddingValues ->
        Column(Modifier.fillMaxSize().padding(paddingValues)) {
            // 상단 탭 레이아웃
            CafeDetailTabRow(
                tabs = tabs,
                selectedTabIndex = selectedTabIndex,
                onTabSelected = { selectedTabIndex = it }
            )

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
                // 탭 컨텐츠
                CafeDetailTabContent(
                    selectedTabIndex = selectedTabIndex,
                    viewModel = viewModel,
                    navController = navController,
                    cafeId = cafeId
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
                contentDescription = "뒤로가기",
                tint = ColorTextBlack,
                modifier = Modifier
                    .size(24.dp)
                    .clickable { onBackClick() }
            )
        },
    )
}

// =====================================================
// Tab Components
// =====================================================

/**
 * 탭 레이아웃
 */
@Composable
private fun CafeDetailTabRow(
    tabs: List<String>,
    selectedTabIndex: Int,
    onTabSelected: (Int) -> Unit
) {
    TabRow(
        selectedTabIndex = selectedTabIndex,
        containerColor = ColorWhite,
        contentColor = ColorPrimaryOrange,
        indicator = { tabPositions ->
            if (selectedTabIndex < tabPositions.size) {
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                    color = ColorPrimaryOrange,
                    height = 3.dp
                )
            }
        },
        divider = {
            HorizontalDivider(
                thickness = 1.dp,
                color = ColorBorderLight
            )
        }
    ) {
        tabs.forEachIndexed { index, title ->
            Tab(
                selected = selectedTabIndex == index,
                onClick = { onTabSelected(index) },
                text = {
                    Text(
                        text = title,
                        fontSize = 14.sp,
                        fontWeight = if (selectedTabIndex == index) {
                            FontWeight.Bold
                        } else {
                            FontWeight.Normal
                        }
                    )
                },
                selectedContentColor = ColorPrimaryOrange,
                unselectedContentColor = ColorTextGray
            )
        }
    }
}

/**
 * 탭 컨텐츠
 */
@Composable
private fun CafeDetailTabContent(
    selectedTabIndex: Int,
    viewModel: CafeDetailViewModel,
    navController: NavController,
    cafeId: Long
) {
    when (selectedTabIndex) {
        0 -> UserCafeInfoTab(
            viewModel = viewModel,
            navController = navController
        )
        1 -> UserCafeSeatInfoTab(
            viewModel = viewModel,
            cafeId = cafeId
        )
    }
}