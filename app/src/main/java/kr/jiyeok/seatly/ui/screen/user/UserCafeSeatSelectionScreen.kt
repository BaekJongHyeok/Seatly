package kr.jiyeok.seatly.ui.screen.user

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.navigation.NavController
import kr.jiyeok.seatly.data.local.Seat
import kr.jiyeok.seatly.data.local.SeatType
import kr.jiyeok.seatly.data.remote.enums.ESeatStatus
import kr.jiyeok.seatly.presentation.viewmodel.CafeDetailViewModel
import kr.jiyeok.seatly.ui.component.common.MaterialSymbol
import kr.jiyeok.seatly.ui.theme.*
import androidx.compose.ui.res.stringResource
import kr.jiyeok.seatly.R
import kotlin.math.roundToInt
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.CheckCircleOutline
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Info
import androidx.compose.ui.graphics.Brush
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext
import kr.jiyeok.seatly.data.remote.response.UserTimePass
import androidx.activity.compose.BackHandler

private val GridCellDp = 40.dp
private val ZoomButtonSize = 44.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserCafeSeatSelectionScreen(
    navController: NavController,
    viewModel: CafeDetailViewModel,
    cafeId: Long
) {
    val density = LocalDensity.current
    val uiState by viewModel.uiState.collectAsState()
    
    val seats = remember(uiState.seats) {
        if (uiState.seats.isNotEmpty()) {
            uiState.seats.map { dto ->
                val parts = dto.position.split(",")
                val pos = if (parts.size >= 2) {
                    Offset(
                        parts[0].trim().toFloatOrNull() ?: 0f,
                        parts[1].trim().toFloatOrNull() ?: 0f
                    )
                } else {
                    Offset(100f, 100f)
                }
                val size = if (parts.size >= 4) {
                    Offset(
                        parts[2].trim().toFloatOrNull() ?: 40f,
                        parts[3].trim().toFloatOrNull() ?: 40f
                    )
                } else {
                    Offset(40f, 40f)
                }
                Seat(
                    id = dto.id.toString(),
                    label = dto.name,
                    type = SeatType.NORMAL,
                    pos = mutableStateOf(pos),
                    size = mutableStateOf(size)
                )
            }.toMutableList()
        } else {
            mutableListOf()
        }
    }

    // 현재 사용자의 활성 세션에서 seatId 목록 추출
    val mySeatIds = remember(uiState.mySessions) {
        uiState.mySessions.map { it.seatId.toString() }.toSet()
    }

    // 사용자가 이용중인 좌석이 있는지 여부
    val hasActiveSession = mySeatIds.isNotEmpty()

    val scaleState = remember { mutableStateOf(1f) }
    val translateX = remember { mutableStateOf(0f) }
    val translateY = remember { mutableStateOf(0f) }
    val scaleAnimated by animateFloatAsState(scaleState.value, label = "scale")
    val gridCellPx = with(density) { GridCellDp.toPx() }

    val selectedSeatId = remember { mutableStateOf<String?>(null) }
    val workspaceSize = remember { mutableStateOf(IntSize(0, 0)) }
    
    // holdingSessionId의 변화(점유 성공/취소)를 감지하여 패널 닫기
    val holdingSessionId = uiState.holdingSessionId
    LaunchedEffect(holdingSessionId) {
        if (holdingSessionId == null) {
            // 점유 취소되었으단 없다면 선택 해제
            val mySeatHolding = uiState.mySessions.any {
                it.seatId.toString() == selectedSeatId.value
            }
            if (!mySeatHolding) {
                selectedSeatId.value = null
            }
        }
    }

    // Better Transition Logic:
    // Watch `mySeatIds`. If the currently selected seat's ownership status changes, dismiss.
    val previousMySeatIds = remember { mutableStateOf(mySeatIds) }
    LaunchedEffect(mySeatIds) {
        val currentSelected = selectedSeatId.value
        if (currentSelected != null) {
            val wasMine = previousMySeatIds.value.contains(currentSelected)
            val isMine = mySeatIds.contains(currentSelected)
            
            if (wasMine != isMine) {
                // Ownership changed! (Started or Ended)
                selectedSeatId.value = null
            }
        }
        previousMySeatIds.value = mySeatIds
    }



    // 리프레시 상태
    val isContentReady = remember(uiState.seats, uiState.isLoadingSeats, uiState.isRefreshing) {
         if (uiState.seats.isNotEmpty()) true
         else !uiState.isLoadingSeats
    }

    val refreshState = rememberPullToRefreshState()

    // 이벤트 처리 (Toast 메시지)
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        viewModel.events.collect { message ->
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }

    // 점유 상태에서 뒤로가기(시스템 백) 가로채기 → 점유 해제 후 이동
    val isHoldActive = uiState.isHolding || uiState.holdingSessionId != null
    
    // 뒤로가기 공통 로직
    val handleBack: () -> Unit = {
        if (isHoldActive) {
            viewModel.cancelHold(cafeId)
        }
        navController.popBackStack()
    }

    BackHandler(enabled = true) {
        handleBack()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.seat_selection_title)) },
                navigationIcon = {
                    IconButton(onClick = handleBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.cafe_detail_back_desc)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = ColorWhite,
                    titleContentColor = ColorTextBlack,
                    navigationIconContentColor = ColorTextBlack
                )
            )
        },
        containerColor = ColorWhite
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            PullToRefreshBox(
                isRefreshing = uiState.isRefreshing && !uiState.isHolding && uiState.holdingSessionId == null,
                onRefresh = {
                    // 점유 중에는 새로고침 불가
                    val locked = uiState.isHolding || uiState.holdingSessionId != null
                    if (!locked) viewModel.refresh(cafeId)
                },
                state = refreshState,
                modifier = Modifier.fillMaxSize()
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    when {
                        !isContentReady -> {
                            LoadingView()
                        }
                        else -> {
                            if (seats.isEmpty()) {
                                EmptySeatView()
                            } else {
                                SeatMapContent(
                                    seats = seats,
                                    scaleState = scaleState,
                                    scaleAnimated = scaleAnimated,
                                    translateX = translateX,
                                    translateY = translateY,
                                    workspaceSize = workspaceSize,
                                    gridCellPx = gridCellPx,
                                    selectedSeatId = selectedSeatId,
                                    uiState = uiState,
                                    mySeatIds = mySeatIds,
                                    hasActiveSession = hasActiveSession,
                                    onHold = { seatId ->
                                        selectedSeatId.value = seatId
                                        viewModel.holdSeat(seatId, cafeId)
                                    },
                                    onStart = {
                                        viewModel.startHeldSession(cafeId)
                                    },
                                    onCancelHold = {
                                        viewModel.cancelHold(cafeId)
                                        selectedSeatId.value = null
                                    },
                                    onEndSession = { sessionId ->
                                        viewModel.endSession(sessionId, cafeId)
                                        // selectedSeatId.value = null // <-- Removed: Let LaunchedEffect handle dismissal
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // 트렌디한 남은 시간 표시 오버레이
            TimePassOverlay(
                userTimePass = uiState.userTimePass,
                hasActiveSession = hasActiveSession,
                mySessions = uiState.mySessions,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 16.dp)
            )
        }
    }
}

@Composable
private fun LoadingView() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ColorWhite),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(64.dp),
                color = ColorPrimaryOrange,
                strokeWidth = 4.dp
            )

            Text(
                text = stringResource(R.string.seat_loading_info),
                fontSize = 14.sp,
                color = ColorTextGray,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun BoxScope.SeatMapContent(
    seats: List<Seat>,
    scaleState: MutableState<Float>,
    scaleAnimated: Float,
    translateX: MutableState<Float>,
    translateY: MutableState<Float>,
    workspaceSize: MutableState<IntSize>,
    gridCellPx: Float,
    selectedSeatId: MutableState<String?>,
    uiState: CafeDetailViewModel.CafeDetailUiState,
    mySeatIds: Set<String>,
    hasActiveSession: Boolean,
    onHold: (String) -> Unit,
    onStart: () -> Unit,
    onCancelHold: () -> Unit,
    onEndSession: (Long) -> Unit
) {
    // 점유 진행 중이거나 점유 완료(대기 중)일 때는 맵 인터랙션 잠금
    val isLocked = uiState.isHolding || uiState.holdingSessionId != null
    Box(
        modifier = Modifier
            .fillMaxSize()
            .onSizeChanged { workspaceSize.value = it }
            .pointerInput(isLocked) {
                detectTapGestures(
                    onTap = {
                        // 잠금 상태에서는 배경 탭으로 패널 닫기 불가
                        if (!isLocked) selectedSeatId.value = null
                    }
                )
            }
            .pointerInput(isLocked) {
                if (!isLocked) {
                    detectTransformGestures { centroid, pan, zoom, _ ->
                        val oldScale = scaleState.value
                        val newScale = (oldScale * zoom).coerceIn(0.5f, 3f)
                        translateX.value = (translateX.value - centroid.x) * (newScale / oldScale) + centroid.x + pan.x
                        translateY.value = (translateY.value - centroid.y) * (newScale / oldScale) + centroid.y + pan.y
                        scaleState.value = newScale
                    }
                }
            }
    ) {
        // 배경 격자 (View Only)
        Canvas(modifier = Modifier.fillMaxSize()) {
            val gridPx = gridCellPx * scaleAnimated
            val startX = translateX.value % gridPx
            val startY = translateY.value % gridPx
            val color = ColorBorderLight.copy(alpha = 0.1f)

            for (x in generateSequence(startX) { it + gridPx }.takeWhile { it < size.width }) {
                drawLine(color, Offset(x, 0f), Offset(x, size.height))
            }
            for (y in generateSequence(startY) { it + gridPx }.takeWhile { it < size.height }) {
                drawLine(color, Offset(0f, y), Offset(size.width, y))
            }
        }

        seats.forEachIndexed { index, seat ->
            val isSelected = selectedSeatId.value == seat.id
            val isMySeat = mySeatIds.contains(seat.id)
            // 내가 현재 점유(hold) 중인 좌석인지 확인
            val isHoldingThisSeat = uiState.holdingSessionId != null &&
                    selectedSeatId.value == seat.id && !isMySeat

            // 실시간 상태 계산: 세션 정보 기반
            val matchingSession = uiState.sessions.find { session ->
                session.seatId.toString() == seat.id
            }
            val isOccupied = matchingSession != null
            val currentStatus = if (isOccupied) ESeatStatus.UNAVAILABLE else ESeatStatus.AVAILABLE

            val occupantName = matchingSession?.userName
            
            // Calculate leftTimeSeconds based on endTime
            val leftTimeSeconds = matchingSession?.endTime?.let { endTimeStr ->
                try {
                    val instant = java.time.Instant.parse(endTimeStr)
                    val now = java.time.Instant.now()
                    val seconds = java.time.Duration.between(now, instant).seconds
                    if (seconds > 0) seconds else 0L
                } catch (e: Exception) {
                    null
                }
            }

            SeatItem(
                seat = seat,
                scale = scaleAnimated,
                translate = Offset(translateX.value, translateY.value),
                isSelected = isSelected,
                isMySeat = isMySeat,
                isHoldingThisSeat = isHoldingThisSeat,
                isOccupied = isOccupied,
                userName = occupantName,
                leftTimeSeconds = leftTimeSeconds,
                status = currentStatus,

                zIndex = if (isSelected) 20000f else if (seat.label.startsWith("WALL")) 0f else 10000f + index.toFloat(),
                enabled = !isLocked,
                onSelect = { id ->
                    // 잠금 상태에서는 다른 좌석 클릭 불가 (이중 방어)
                    if (!isLocked) onHold(id)
                }
            )
        }
    }

    val selectedSeat = seats.find { it.id == selectedSeatId.value }
    val isAssignmentLoading = uiState.isLoadingAssignment
    val isHolding = uiState.isHolding
    val holdingSessionId = uiState.holdingSessionId
    val selectedSeatIsMine = selectedSeat?.let { mySeatIds.contains(it.id) } ?: false

    // 내 좌석의 세션 ID 찾기
    val mySessionForSelectedSeat = if (selectedSeatIsMine && selectedSeat != null) {
        uiState.mySessions.find { it.seatId.toString() == selectedSeat.id }
    } else null

    // 하단 정보 패널
    AnimatedVisibility(
        visible = selectedSeat != null || isHolding,
        enter = slideInVertically(initialOffsetY = { it }),
        exit = slideOutVertically(targetOffsetY = { it }),
        modifier = Modifier.align(Alignment.BottomCenter)
    ) {
        when {
            isHolding -> {
                // 점유 API 호출 중 — 로딩 패널 표시
                SeatHoldingPanel()
            }
            selectedSeat != null && selectedSeatIsMine && mySessionForSelectedSeat != null -> {
                // 내 좌석 선택 → 이용 종료 패널
                SeatEndPanel(
                    seat = selectedSeat,
                    isLoading = isAssignmentLoading,
                    onClose = { selectedSeatId.value = null },
                    onEnd = { 
                        onEndSession(mySessionForSelectedSeat.id)
                        selectedSeatId.value = null // Clear immediately for better UX
                    }
                )
            }
            selectedSeat != null && hasActiveSession -> {
                // 다른 좌석 선택 but 이미 이용중인 좌석이 있음 → 경고 패널
                SeatWarningPanel(
                    seat = selectedSeat,
                    onClose = {
                        onCancelHold()
                        selectedSeatId.value = null
                    }
                )
            }
            selectedSeat != null -> {
                // 이용중인 좌석 없음 → 이용 시작 패널
                SeatActionPanel(
                    seat = selectedSeat,
                    isOccupied = holdingSessionId == null && !selectedSeatIsMine && uiState.sessions.any { it.seatId.toString() == selectedSeat.id },
                    isLoading = isAssignmentLoading,
                    holdingSessionId = holdingSessionId,
                    onClose = { onCancelHold() },
                    onStart = { onStart() }
                )
            }
        }
    }

    // 줌 컨트롤 버튼 (점유 중에는 숨김)
    if (!isLocked) {
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(
                    end = 16.dp,
                    bottom = if (selectedSeat != null) 220.dp else 20.dp
                ),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FloatingActionButton(
                onClick = {
                    scaleState.value = (scaleState.value + 0.2f).coerceAtMost(3f)
                },
                modifier = Modifier.size(ZoomButtonSize),
                containerColor = ColorWhite,
                contentColor = ColorTextBlack,
                shape = CircleShape
            ) {
                MaterialSymbol("add")
            }

            FloatingActionButton(
                onClick = {
                    scaleState.value = (scaleState.value - 0.2f).coerceAtLeast(0.5f)
                },
                modifier = Modifier.size(ZoomButtonSize),
                containerColor = ColorWhite,
                contentColor = ColorTextBlack,
                shape = CircleShape
            ) {
                MaterialSymbol("remove")
            }
        }
    }
}

/**
 * 패널 공통 컨테이너 (Bottom Sheet 스타일)
 */
@Composable
private fun BottomSheetContainer(
    onClose: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 32.dp,
                shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
            ),
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        color = ColorWhite
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp)
        ) {
            // Handle Bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Spacer(
                    modifier = Modifier
                        .size(40.dp, 4.dp)
                        .background(ColorBorderLight, RoundedCornerShape(2.dp))
                )
            }

            content()
        }
    }
}

/**
 * 이용 시작 패널 (기존)
 */
@Composable

private fun SeatActionPanel(
    seat: Seat,
    isOccupied: Boolean,
    isLoading: Boolean,
    holdingSessionId: Long?,      // null: 점유 대기 중, non-null: 점유 완료
    onClose: () -> Unit,
    onStart: () -> Unit
) {
    BottomSheetContainer(onClose = onClose) {
        Column(
            modifier = Modifier.padding(horizontal = 24.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    modifier = Modifier.size(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = if (isOccupied) ColorTextLightGray.copy(alpha = 0.2f) else ColorPrimaryOrange.copy(alpha = 0.1f)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (isOccupied) Icons.Default.ErrorOutline else Icons.Default.CheckCircleOutline,
                            contentDescription = null,
                            tint = if (isOccupied) ColorTextGray else ColorPrimaryOrange,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.seat_label_format, seat.label),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = ColorTextBlack
                    )
                    Text(
                        text = when {
                            isOccupied -> stringResource(R.string.seat_occupied_message)
                            holdingSessionId == null -> "좌석 점유 중..."
                            else -> stringResource(R.string.seat_available_message)
                        },
                        fontSize = 14.sp,
                        color = if (isOccupied) ColorTextGray else ColorTextGray
                    )
                }

                IconButton(onClick = onClose) {
                    Icon(Icons.Default.Close, stringResource(R.string.seat_close_desc), tint = ColorTextGray)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 점유 완료 전(holdingSessionId == null)에는 뺄리는 동안 뺄리기 스피너 표시
            Button(
                onClick = onStart,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isOccupied) ColorTextLightGray else Color.Transparent,
                    disabledContainerColor = ColorTextLightGray
                ),
                contentPadding = PaddingValues(0.dp),
                enabled = !isLoading && !isOccupied && holdingSessionId != null
            ) {
                Box(
                    modifier = if (!isOccupied) Modifier
                        .fillMaxSize()
                        .background(
                            brush = Brush.horizontalGradient(
                                colors = listOf(Color(0xFFFF8A65), ColorPrimaryOrange)
                            ),
                            shape = RoundedCornerShape(16.dp)
                        ) else Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    if (isLoading || holdingSessionId == null) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = if (holdingSessionId == null) ColorPrimaryOrange else ColorWhite,
                            strokeWidth = 3.dp
                        )
                    } else {
                        Text(
                            text = if (isOccupied) stringResource(R.string.seat_unavailable_button) else stringResource(R.string.seat_start_button),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = ColorWhite
                        )
                    }
                }
            }
        }
    }
}

/**
 * 좌석 점유 중 로딩 패널 (좌석 클릭 후 assign API 호출 중)
 */
@Composable
private fun SeatHoldingPanel() {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 32.dp,
                shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
            ),
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        color = ColorWhite
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Spacer(
                    modifier = Modifier
                        .size(40.dp, 4.dp)
                        .background(ColorBorderLight, RoundedCornerShape(2.dp))
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            CircularProgressIndicator(
                modifier = Modifier.size(40.dp),
                color = ColorPrimaryOrange,
                strokeWidth = 3.dp
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "좌석 점유 중...",
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = ColorTextGray
            )
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

/**
 * 이용 종료 패널 (내 좌석 선택 시)
 */
@Composable
private fun SeatEndPanel(
    seat: Seat,
    isLoading: Boolean,
    onClose: () -> Unit,
    onEnd: () -> Unit
) {
    BottomSheetContainer(onClose = onClose) {
        Column(
            modifier = Modifier.padding(horizontal = 24.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    modifier = Modifier.size(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = ColorPrimaryOrange.copy(alpha = 0.1f)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = ColorPrimaryOrange,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.seat_label_format, seat.label),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = ColorTextBlack
                    )
                    Text(
                        text = stringResource(R.string.seat_my_seat_message),
                        fontSize = 14.sp,
                        color = ColorPrimaryOrange,
                        fontWeight = FontWeight.Bold
                    )
                }

                IconButton(onClick = onClose) {
                    Icon(Icons.Default.Close, stringResource(R.string.seat_close_desc), tint = ColorTextGray)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = onEnd,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(Color(0xFFE53935).copy(alpha = 0.9f)),
                enabled = !isLoading
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.PowerSettingsNew, null, tint = ColorWhite, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = ColorWhite,
                            strokeWidth = 3.dp
                        )
                    } else {
                        Text(
                            text = stringResource(R.string.seat_end_button),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

/**
 * 경고 패널 (이용중인 좌석이 있는데 다른 좌석을 선택한 경우)
 */
@Composable
private fun SeatWarningPanel(
    seat: Seat,
    onClose: () -> Unit
) {
    BottomSheetContainer(onClose = onClose) {
        Column(
            modifier = Modifier.padding(horizontal = 24.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    modifier = Modifier.size(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFFFCC80).copy(alpha = 0.2f)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.ErrorOutline,
                            contentDescription = null,
                            tint = Color(0xFFE65100),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.seat_label_format, seat.label),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = ColorTextBlack
                    )
                    Text(
                        text = stringResource(R.string.seat_warning_occupied_title),
                        fontSize = 14.sp,
                        color = Color(0xFFE65100),
                        fontWeight = FontWeight.Bold
                    )
                }

                IconButton(onClick = onClose) {
                    Icon(Icons.Default.Close, stringResource(R.string.seat_close_desc), tint = ColorTextGray)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFFFFF3E0),
                border = BorderStroke(1.dp, Color(0xFFFFB74D).copy(alpha = 0.3f))
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = Color(0xFFE65100),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = stringResource(R.string.seat_warning_occupied_message),
                        fontSize = 14.sp,
                        color = Color(0xFFE65100),
                        lineHeight = 20.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = onClose,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(ColorTextGray.copy(alpha = 0.1f))
            ) {
                Text(
                    text = stringResource(R.string.seat_confirm_button),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = ColorTextBlack
                )
            }
        }
    }
}

@Composable
private fun BoxScope.SeatItem(
    seat: Seat,
    scale: Float,
    translate: Offset,
    isSelected: Boolean,
    isMySeat: Boolean,
    isHoldingThisSeat: Boolean = false,
    isOccupied: Boolean = false,
    userName: String? = null,
    leftTimeSeconds: Long? = null,
    status: ESeatStatus,
    zIndex: Float,
    enabled: Boolean = true,     // false이면 탭 제스처 비활성화
    onSelect: (String) -> Unit
) {
    val density = LocalDensity.current
    val isWall = seat.label.startsWith("WALL")
    val displayText = if (isWall) "" else seat.label

    // 남은 시간 포맷팅 (초 → 시간/분)
    val formattedLeftTime = leftTimeSeconds?.let { seconds ->
        val hours = seconds / 3600
        val minutes = (seconds % 3600) / 60
        when {
            hours > 0 && minutes > 0 -> "${hours}h ${minutes}m"
            hours > 0 -> "${hours}h"
            minutes > 0 -> "${minutes}m"
            else -> "0m"
        }
    }

    val pos by seat.pos
    val size by seat.size
    val screenX = pos.x * scale + translate.x
    val screenY = pos.y * scale + translate.y
    val widthPx = size.x * scale
    val heightPx = size.y * scale

    Box(
        modifier = Modifier
            .zIndex(zIndex)
            .offset { IntOffset(screenX.roundToInt(), screenY.roundToInt()) }
            .size(
                with(density) { widthPx.toDp() },
                with(density) { heightPx.toDp() }
            )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .then(
                    if (!isWall && enabled) {
                        Modifier.pointerInput(Unit) {
                            detectTapGestures(
                                onTap = {
                                    onSelect(seat.id)
                                }
                            )
                        }
                    } else Modifier
                )
        ) {
            // 좌석 본체
            if (isWall) {
                // 벽: 단순 회색 박스
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(ColorInputBg, RoundedCornerShape(4.dp))
                )
            } else {
                // 실제 좌석 UI
                val backgroundColor = when {
                    isMySeat -> ColorPrimaryOrange // Solid Orange for My Seat
                    isHoldingThisSeat -> Color(0xFFFFE0B2) // 내가 점유 중: 연한 오렌지
                    isOccupied -> Color(0xFFF0F0F0) // Subtle gray background for occupied
                    status == ESeatStatus.UNAVAILABLE -> ColorTextLightGray.copy(alpha = 0.5f)
                    else -> ColorWhite
                }
                
                val borderColor = when {
                    isMySeat -> ColorPrimaryOrange
                    isHoldingThisSeat -> ColorPrimaryOrange
                    isSelected -> ColorPrimaryOrange
                    isOccupied -> ColorBorderLight.copy(alpha = 0.5f)
                    else -> ColorBorderLight
                }

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    shape = RoundedCornerShape(10.dp),
                    color = backgroundColor,
                    border = BorderStroke(
                        width = if (isSelected || isMySeat) 2.dp else 1.dp,
                        color = borderColor
                    ),
                    shadowElevation = when {
                        isMySeat -> 8.dp
                        isSelected -> 6.dp
                        isOccupied -> 1.dp
                        else -> 2.dp
                    }
                ) {
                    Box(
                        modifier = Modifier,
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            if (isMySeat) {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = "내 좌석",
                                    tint = ColorWhite,
                                    modifier = Modifier.size(with(density) { (14 * scale).dp })
                                )
                            } else if (isOccupied && userName != null) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = "사용중",
                                    tint = ColorTextGray,
                                    modifier = Modifier.size(with(density) { (10 * scale).dp })
                                )
                                Spacer(modifier = Modifier.height(with(density) { (2 * scale).dp }))
                            }
                            
                            if (displayText.isNotEmpty() && !isWall) {
                                Text(
                                    text = if (isOccupied && userName != null) userName else displayText,
                                    fontSize = with(density) { (if (isMySeat || isOccupied) 8 * scale else 10 * scale).sp },
                                    color = when {
                                        isMySeat -> ColorWhite
                                        isOccupied -> ColorTextGray
                                        else -> ColorTextBlack
                                    },
                                    fontWeight = if (isMySeat || isSelected || isOccupied) FontWeight.Bold else FontWeight.Medium
                                )
                            }

                            if (isOccupied && formattedLeftTime != null) {
                                Spacer(modifier = Modifier.height(with(density) { (2 * scale).dp }))
                                Text(
                                    text = formattedLeftTime,
                                    fontSize = with(density) { (8 * scale).sp },
                                    color = ColorPrimaryOrange,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptySeatView() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ColorWhite),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Close, 
                contentDescription = "No Seats",
                tint = ColorTextGray,
                modifier = Modifier.size(48.dp)
            )
            Text(
                text = "등록된 좌석 정보가 없습니다.",
                fontSize = 16.sp,
                color = ColorTextGray,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun TimePassOverlay(
    userTimePass: UserTimePass?,
    hasActiveSession: Boolean,
    mySessions: List<kr.jiyeok.seatly.data.remote.response.SessionDto> = emptyList(),
    modifier: Modifier = Modifier
) {
    userTimePass?.let { pass ->
        // 이용 중일 때: startTime 기반 경과 시간 실시간 계산
        // 이용 중 아닐 때: 남은 시간권 표시
        var elapsedSeconds by remember { mutableStateOf(0L) }
        var timeLeft by remember(pass.leftTime) { mutableStateOf(pass.leftTime) }

        // 경과 시간 계산용 startTime (가장 이른 세션 기준)
        val startInstant = remember(mySessions) {
            mySessions.mapNotNull { session ->
                try { java.time.Instant.parse(session.startTime) } catch (e: Exception) { null }
            }.minOrNull()
        }

        LaunchedEffect(hasActiveSession, startInstant, pass.leftTime) {
            if (hasActiveSession && startInstant != null) {
                // 경과 시간 실시간 업데이트
                while (true) {
                    val now = java.time.Instant.now()
                    elapsedSeconds = java.time.Duration.between(startInstant, now).seconds.coerceAtLeast(0L)
                    kotlinx.coroutines.delay(1000L)
                }
            } else {
                elapsedSeconds = 0L
                timeLeft = pass.leftTime
            }
        }

        val displayHours: Long
        val displayMinutes: Long
        val displaySeconds: Long
        val displayText: String

        if (hasActiveSession) {
            displayHours = elapsedSeconds / 3600
            displayMinutes = (elapsedSeconds % 3600) / 60
            displaySeconds = elapsedSeconds % 60
            displayText = "사용 중 : %02d:%02d:%02d".format(displayHours, displayMinutes, displaySeconds)
        } else {
            displayHours = timeLeft / 3600
            displayMinutes = (timeLeft % 3600) / 60
            displayText = "남은 시간 : ${displayHours}시간 ${displayMinutes}분"
        }

        Surface(
            modifier = modifier
                .shadow(
                    elevation = 8.dp,
                    shape = RoundedCornerShape(50),
                    ambientColor = ColorPrimaryOrange.copy(alpha = 0.3f),
                    spotColor = ColorPrimaryOrange.copy(alpha = 0.3f)
                ),
            shape = RoundedCornerShape(50),
            color = Color.Transparent // Gradient for Box
        ) {
            Box(
                modifier = Modifier
                    .background(
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                Color(0xFFFF8A65), // Light Orange
                                Color(0xFFFFB74D)  // Gold/Orange
                            )
                        )
                    )
                    .padding(horizontal = 20.dp, vertical = 10.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Schedule,
                        contentDescription = "시간권",
                        tint = ColorWhite,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = displayText,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = ColorWhite
                    )
                }
            }
        }
    }
}
