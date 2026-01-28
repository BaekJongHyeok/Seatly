package kr.jiyeok.seatly.ui.screen.user

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import kr.jiyeok.seatly.presentation.viewmodel.CafeDetailViewModel
import kr.jiyeok.seatly.ui.component.common.AppTopBar
import kr.jiyeok.seatly.ui.theme.*
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun UserCafeDetailScreen(
    navController: NavController,
    cafeId: Long,
    viewModel: CafeDetailViewModel = koinViewModel()
) {
    val snackbarHostState = remember { SnackbarHostState() }

    val uiState by viewModel.uiState.collectAsState()
    val isAnyLoading by viewModel.isAnyLoading.collectAsState()

    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabs = listOf("상세 정보", "좌석 이용")

    LaunchedEffect(cafeId) {
        viewModel.loadCafeDetailInfos(cafeId)
    }

    LaunchedEffect(Unit) {
        viewModel.events.collect { message ->
            // Use Snackbar or other common notification
            snackbarHostState.showSnackbar(message)
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
            CafeDetailTabRow(
                tabs = tabs,
                selectedTabIndex = selectedTabIndex,
                onTabSelected = { selectedTabIndex = it }
            )

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
