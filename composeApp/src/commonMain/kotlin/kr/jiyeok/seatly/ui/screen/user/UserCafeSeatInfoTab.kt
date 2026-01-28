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
import kr.jiyeok.seatly.data.local.Seat
import kr.jiyeok.seatly.data.local.SeatType
import kr.jiyeok.seatly.data.remote.enums.ESeatStatus
import kr.jiyeok.seatly.presentation.viewmodel.CafeDetailViewModel
import kr.jiyeok.seatly.ui.component.common.MaterialSymbol
import kr.jiyeok.seatly.ui.theme.*
import kotlin.math.roundToInt

private val GridCellDp = 40.dp
private val ZoomButtonSize = 44.dp

@Composable
fun UserCafeSeatInfoTab(
    viewModel: CafeDetailViewModel,
    cafeId: Long
) {
    val density = LocalDensity.current
    val uiState by viewModel.uiState.collectAsState()
    
    val seats = remember { mutableStateListOf<Seat>() }

    LaunchedEffect(uiState.seats) {
        seats.clear()
        if (uiState.seats.isNotEmpty()) {
            uiState.seats.forEach { dto ->
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
                seats.add(
                    Seat(
                        id = dto.id.toString(),
                        label = dto.name,
                        type = SeatType.NORMAL,
                        pos = mutableStateOf(pos),
                        size = mutableStateOf(size)
                    )
                )
            }
        }
    }

    val scaleState = remember { mutableStateOf(1f) }
    val translateX = remember { mutableStateOf(0f) }
    val translateY = remember { mutableStateOf(0f) }
    val scaleAnimated by animateFloatAsState(scaleState.value, label = "scale")
    val gridCellPx = with(density) { GridCellDp.toPx() }

    val selectedSeatId = remember { mutableStateOf<String?>(null) }
    val workspaceSize = remember { mutableStateOf(IntSize(0, 0)) }

    val isContentReady = remember(uiState.seats, uiState.isLoadingSeats) {
        !uiState.isLoadingSeats
    }

    Surface(
        color = ColorWhite,
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
                            onAssign = { seatId ->
                                viewModel.assignSeat(seatId, cafeId)
                                selectedSeatId.value = null
                            }
                        )
                    }
                }
            }
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
                text = "좌석 정보를 불러오는 중...",
                fontSize = 14.sp,
                color = ColorTextGray,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun BoxScope.SeatMapContent(
    seats: MutableList<Seat>,
    scaleState: MutableState<Float>,
    scaleAnimated: Float,
    translateX: MutableState<Float>,
    translateY: MutableState<Float>,
    workspaceSize: MutableState<IntSize>,
    gridCellPx: Float,
    selectedSeatId: MutableState<String?>,
    uiState: CafeDetailViewModel.CafeDetailUiState,
    onAssign: (String) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .onSizeChanged { workspaceSize.value = it }
            .pointerInput(Unit) {
                detectTransformGestures { centroid, pan, zoom, _ ->
                    val oldScale = scaleState.value
                    val newScale = (oldScale * zoom).coerceIn(0.5f, 3f)
                    translateX.value = (translateX.value - centroid.x) * (newScale / oldScale) + centroid.x + pan.x
                    translateY.value = (translateY.value - centroid.y) * (newScale / oldScale) + centroid.y + pan.y
                    scaleState.value = newScale
                }
            }
    ) {
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

            val isOccupied = uiState.sessions.any { session ->
                session.seatId.toString() == seat.id
            }
            val currentStatus = if (isOccupied) ESeatStatus.UNAVAILABLE else ESeatStatus.AVAILABLE

            SeatItem(
                seat = seat,
                scale = scaleAnimated,
                translate = Offset(translateX.value, translateY.value),
                isSelected = isSelected,
                status = currentStatus,
                zIndex = if (isSelected) seats.size.toFloat() else index.toFloat(),
                onSelect = { id ->
                    if(currentStatus == ESeatStatus.AVAILABLE) {
                        selectedSeatId.value = id
                    }
                }
            )
        }
    }

    val selectedSeat = seats.find { it.id == selectedSeatId.value }
    val isAssignmentLoading = uiState.isLoadingAssignment

    AnimatedVisibility(
        visible = selectedSeat != null,
        enter = slideInVertically(initialOffsetY = { it }),
        exit = slideOutVertically(targetOffsetY = { it }),
        modifier = Modifier.align(Alignment.BottomCenter)
    ) {
        selectedSeat?.let { seat ->
            SeatActionPanel(
                seat = seat,
                isLoading = isAssignmentLoading,
                onClose = { selectedSeatId.value = null },
                onStart = { onAssign(seat.id) }
            )
        }
    }

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

@Composable
private fun SeatActionPanel(
    seat: Seat,
    isLoading: Boolean,
    onClose: () -> Unit,
    onStart: () -> Unit
) {
    Card(
        modifier = Modifier
            .padding(16.dp)
            .fillMaxWidth()
            .shadow(16.dp, RoundedCornerShape(24.dp)),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(ColorWhite)
    ) {
        Column(
            modifier = Modifier.padding(24.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = seat.label,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = ColorTextBlack
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "사용 가능",
                        fontSize = 14.sp,
                        color = ColorCheckCircle,
                        fontWeight = FontWeight.Medium
                    )
                }

                IconButton(
                    onClick = onClose,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "닫기",
                        tint = ColorTextGray,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = onStart,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(ColorPrimaryOrange),
                enabled = !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = ColorWhite,
                        strokeWidth = 3.dp
                    )
                } else {
                    Text(
                        text = "이용 시작",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
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
    status: ESeatStatus,
    zIndex: Float,
    onSelect: (String) -> Unit
) {
    val density = LocalDensity.current
    val isWall = seat.label.startsWith("WALL")
    val displayText = if (isWall) "" else seat.label

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
                .pointerInput(Unit) {
                    detectTapGestures(
                        onTap = {
                            if (!isWall) onSelect(seat.id)
                        }
                    )
                }
        ) {
            if (isWall) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(ColorInputBg, RoundedCornerShape(4.dp))
                )
            } else {
                val backgroundColor = when (status) {
                    ESeatStatus.AVAILABLE -> if (isSelected) ColorPrimaryOrange else ColorWhite
                    ESeatStatus.UNAVAILABLE -> ColorTextLightGray
                    else -> ColorTextLightGray
                }
                
                val borderColor = if (isSelected) ColorPrimaryOrange else ColorBorderLight
                val contentColor = if (isSelected) ColorWhite else ColorTextBlack

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    shape = RoundedCornerShape(8.dp),
                    color = backgroundColor,
                    border = BorderStroke(1.dp, borderColor),
                    shadowElevation = if (isSelected) 8.dp else 2.dp
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = displayText,
                            fontSize = (14 * scale).coerceIn(10f, 24f).sp,
                            fontWeight = FontWeight.Bold,
                            color = contentColor
                        )
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
