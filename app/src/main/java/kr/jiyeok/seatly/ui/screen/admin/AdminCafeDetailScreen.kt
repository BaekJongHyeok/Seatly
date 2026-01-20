package kr.jiyeok.seatly.ui.screen.admin

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import kr.jiyeok.seatly.presentation.viewmodel.AdminCafeDetailViewModel
import kr.jiyeok.seatly.ui.component.common.AppTopBar
import kr.jiyeok.seatly.ui.theme.*

@Composable
fun AdminCafeDetailScreen(
    cafeId: Long,
    navController: NavController,
    viewModel: AdminCafeDetailViewModel = hiltViewModel()
) {
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabs = listOf("상세 정보", "카페 유저", "좌석 현황")
    val cafe by viewModel.cafeInfo.collectAsState()

    // cafeId 관련 정보 전부 불러오기
    LaunchedEffect(Unit) {
        viewModel.loadCafeDetailInfos(cafeId)
    }

    Scaffold(
        topBar = {
            AppTopBar(
                title = cafe?.name ?: "카페 관리",
                leftContent = {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "뒤로가기",
                        modifier = Modifier.clickable { navController.popBackStack() }
                    )
                }
            )
        },
        containerColor = ColorWhite
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            // 상단 탭 레이아웃
            TabRow(
                selectedTabIndex = selectedTabIndex,
                containerColor = ColorWhite,
                contentColor = ColorPrimaryOrange,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                        color = ColorPrimaryOrange
                    )
                }
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTabIndex == index,
                        onClick = { selectedTabIndex = index },
                        text = { Text(text = title, fontSize = 14.sp) }
                    )
                }
            }

            // 컴포넌트 호출
            when (selectedTabIndex) {
                0 -> AdminCafeInfoTab(viewModel = viewModel, navController = navController)
                1 -> AdminCafeMembersTab(viewModel = viewModel, cafeId = cafeId)
                2 -> AdminCafeSeatInfoTab(viewModel = viewModel, cafeId = cafeId)
            }
        }
    }
}