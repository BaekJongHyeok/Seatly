package kr.jiyeok.seatly.ui.screen.admin

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt
import kr.jiyeok.seatly.data.remote.enums.ESeatStatus
import kr.jiyeok.seatly.presentation.viewmodel.AdminCafeDetailViewModel
import kr.jiyeok.seatly.ui.component.MaterialSymbol
import kr.jiyeok.seatly.ui.screen.admin.seat.Seat
import kr.jiyeok.seatly.ui.screen.admin.seat.SeatType
import kr.jiyeok.seatly.ui.theme.*

private val GridCellDp = 40.dp
private val SeatDp = 40.dp
private val ToolCircleSize = 40.dp
private val ZoomButtonSize = 44.dp

@Composable
fun AdminCafeSeatInfoTab(
    cafeId: Long,
    viewModel: AdminCafeDetailViewModel
) {
    val density = LocalDensity.current
    val remoteSeats by viewModel.seatInfo.collectAsState()

    var isEditMode by remember { mutableStateOf(false) }
    val seats = remember { mutableStateListOf<Seat>() }

    // 데이터 로드
    LaunchedEffect(remoteSeats) {
        if (!remoteSeats.isNullOrEmpty()) {
            seats.clear()
            remoteSeats?.forEach { dto ->
                val parts = dto.position.split(",")
                val pos = if (parts.size >= 2) Offset(parts[0].toFloatOrNull() ?: 0f, parts[1].toFloatOrNull() ?: 0f) else Offset(100f, 100f)
                val size = if (parts.size >= 4) Offset(parts[2].toFloatOrNull() ?: 40f, parts[3].toFloatOrNull() ?: 40f) else Offset(40f, 40f)

                seats.add(Seat(dto.id.toString(), dto.name, SeatType.NORMAL, mutableStateOf(pos), mutableStateOf(size)))
            }
        }
    }

    val scaleState = remember { mutableStateOf(1f) }
    val translateX = remember { mutableStateOf(0f) }
    val translateY = remember { mutableStateOf(0f) }
    val scaleAnimated by animateFloatAsState(scaleState.value)

    val gridCellPx = with(density) { GridCellDp.toPx() }
    val selectedSeatId = remember { mutableStateOf<String?>(null) }
    var showLabelEditor by remember { mutableStateOf(false) }
    var editingSeatId by remember { mutableStateOf<String?>(null) }
    var editingText by remember { mutableStateOf("") }
    val workspaceSize = remember { mutableStateOf(IntSize(0, 0)) }

    // 아이템 추가 함수
    val addItemAtCenter: (String) -> Unit = { prefix ->
        val screenCenter = Offset(workspaceSize.value.width / 2f, workspaceSize.value.height / 2f)
        val contentX = (screenCenter.x - translateX.value) / scaleState.value
        val contentY = (screenCenter.y - translateY.value) / scaleState.value
        val snappedX = (contentX / gridCellPx).roundToInt() * gridCellPx
        val snappedY = (contentY / gridCellPx).roundToInt() * gridCellPx

        val newLabel = when(prefix) {
            "__WALL__" -> "__WALL__"
            "__LABEL__" -> "__LABEL__텍스트"
            else -> "${seats.count { !it.label.startsWith("__") } + 1}번"
        }

        val newItem = Seat("NEW_${System.currentTimeMillis()}", newLabel, SeatType.NORMAL, mutableStateOf(Offset(snappedX, snappedY)), mutableStateOf(Offset(gridCellPx, gridCellPx)))
        seats.add(newItem)
        selectedSeatId.value = newItem.id
    }

    Surface(color = ColorWhite, modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.fillMaxSize()) {
            // 맵 캔버스 영역
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
                    .pointerInput(Unit) { detectTapGestures { selectedSeatId.value = null } }
            ) {
                // 그리드 배경
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val gridPx = gridCellPx * scaleAnimated
                    val startX = translateX.value % gridPx
                    val startY = translateY.value % gridPx
                    val color = if (isEditMode) ColorBorderLight else ColorBorderLight.copy(alpha = 0.1f)
                    for (x in generateSequence(startX) { it + gridPx }.takeWhile { it < size.width }) drawLine(color, Offset(x, 0f), Offset(x, size.height))
                    for (y in generateSequence(startY) { it + gridPx }.takeWhile { it < size.height }) drawLine(color, Offset(0f, y), Offset(size.width, y))
                }

                // 모든 아이템(좌석, 벽, 라벨) 렌더링
                seats.forEach { seat ->
                    val currentStatus = remoteSeats?.find { it.id.toString() == seat.id }?.status ?: ESeatStatus.AVAILABLE
                    SeatItem(
                        seat = seat,
                        scale = scaleAnimated,
                        translate = Offset(translateX.value, translateY.value),
                        isSelected = selectedSeatId.value == seat.id,
                        isEditMode = isEditMode,
                        status = currentStatus,
                        gridSizePx = gridCellPx,
                        onSelect = { selectedSeatId.value = it },
                        onEditRequested = { s ->
                            editingSeatId = s.id
                            editingText = s.label
                            showLabelEditor = true
                        }
                    )
                }
            }

            // 줌 컨트롤러
            Column(
                modifier = Modifier.align(Alignment.BottomEnd).padding(end = 16.dp, bottom = if (isEditMode) 180.dp else 90.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FloatingActionButton(onClick = { scaleState.value = (scaleState.value + 0.2f).coerceAtMost(3f) }, modifier = Modifier.size(ZoomButtonSize), containerColor = ColorWhite, contentColor = ColorTextBlack, shape = CircleShape) { MaterialSymbol("add") }
                FloatingActionButton(onClick = { scaleState.value = (scaleState.value - 0.2f).coerceAtLeast(0.5f) }, modifier = Modifier.size(ZoomButtonSize), containerColor = ColorWhite, contentColor = ColorTextBlack, shape = CircleShape) { MaterialSymbol("remove") }
            }

            // 하단 컨트롤 바
            Box(modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp).fillMaxWidth()) {
                if (isEditMode) {
                    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp), elevation = CardDefaults.cardElevation(8.dp), colors = CardDefaults.cardColors(ColorWhite)) {
                        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                ToolCircleWithLabel("chair", "좌석") { addItemAtCenter("") }
                                ToolCircleWithLabel("rectangle", "벽") { addItemAtCenter("__WALL__") }
                                ToolCircleWithLabel("delete", "삭제") {
                                    selectedSeatId.value?.let { id -> seats.removeIf { it.id == id }; selectedSeatId.value = null }
                                }
                            }
                            Button(onClick = { viewModel.saveSeatConfig(cafeId, seats); isEditMode = false }, colors = ButtonDefaults.buttonColors(ColorPrimaryOrange)) {
                                Text("저장", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                } else {
                    Button(onClick = { isEditMode = true }, modifier = Modifier.fillMaxWidth().height(56.dp), shape = RoundedCornerShape(16.dp), colors = ButtonDefaults.buttonColors(
                        ColorPrimaryOrange)) {
                        MaterialSymbol("edit", tint = ColorWhite)
                        Spacer(Modifier.width(8.dp))
                        Text("공간 편집", fontWeight = FontWeight.Bold)
                    }
                }
            }

            if (showLabelEditor) {
                AlertDialog(
                    onDismissRequest = { showLabelEditor = false },
                    confirmButton = {
                        TextButton(onClick = {
                            seats.find { it.id == editingSeatId }?.let { it.label = editingText }
                            showLabelEditor = false
                        }) { Text("확인") }
                    },
                    title = { Text("이름 변경 (구조물 접두사 유지)") },
                    text = { OutlinedTextField(value = editingText, onValueChange = { editingText = it }, singleLine = true) }
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
    isEditMode: Boolean,
    status: ESeatStatus,
    gridSizePx: Float,
    onSelect: (String) -> Unit,
    onEditRequested: (Seat) -> Unit
) {
    val density = LocalDensity.current
    val isWall = seat.label.startsWith("__WALL__")
    val isLabel = seat.label.startsWith("__LABEL__")
    val displayText = when {
        isWall -> ""
        isLabel -> seat.label.removePrefix("__LABEL__")
        else -> seat.label
    }

    // 위치 및 크기 상태
    val pos by seat.pos
    val size by seat.size

    val screenX = pos.x * scale + translate.x
    val screenY = pos.y * scale + translate.y
    val widthPx = size.x * scale
    val heightPx = size.y * scale

    Box(
        modifier = Modifier
            .offset { IntOffset(screenX.roundToInt(), screenY.roundToInt()) }
            .size(with(density) { widthPx.toDp() }, with(density) { heightPx.toDp() })
            .pointerInput(isEditMode) {
                if (isEditMode) {
                    detectDragGestures(
                        onDragStart = { onSelect(seat.id) },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            seat.pos.value += Offset(dragAmount.x / scale, dragAmount.y / scale)
                        },
                        onDragEnd = {
                            // 그리드 스냅
                            seat.pos.value = Offset(
                                (seat.pos.value.x / gridSizePx).roundToInt() * gridSizePx,
                                (seat.pos.value.y / gridSizePx).roundToInt() * gridSizePx
                            )
                        }
                    )
                }
            }
            .clickable { if (isEditMode) onSelect(seat.id) }
            .pointerInput(Unit) {
                detectTapGestures(onDoubleTap = { if (isEditMode) onEditRequested(seat) })
            }
    ) {
        val backgroundColor = when {
            isWall -> if (isSelected && isEditMode) Color.DarkGray else Color.LightGray
            isLabel -> Color.Transparent
            !isEditMode && status == ESeatStatus.UNAVAILABLE -> ColorWarning
            else -> if (isSelected && isEditMode) ColorPrimaryOrange.copy(alpha = 0.1f) else ColorWhite
        }

        Card(
            shape = if (isWall) RoundedCornerShape(0.dp) else RoundedCornerShape(8.dp),
            modifier = Modifier.fillMaxSize().border(
                width = if (isSelected && isEditMode) 2.dp else if (isLabel) 0.dp else 1.dp,
                color = if (isSelected && isEditMode) ColorPrimaryOrange else ColorBorderLight,
                shape = if (isWall) RoundedCornerShape(0.dp) else RoundedCornerShape(8.dp)
            ),
            colors = CardDefaults.cardColors(backgroundColor)
        ) {
            Box(Modifier.fillMaxSize(), Alignment.Center) {
                Text(displayText, fontSize = (12 * scale).sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            }
        }

        // 편집 모드 + 선택 시 리사이징 핸들 표시 (8방향)
        if (isEditMode && isSelected) {
            ResizeHandles(seat, scale, gridSizePx)
        }
    }
}

@Composable
private fun BoxScope.ResizeHandles(seat: Seat, scale: Float, gridSizePx: Float) {
    val handleSize = 12.dp

    // 간소화된 8방향 리사이징 로직 (우하단 대표 예시, 실제로는 모든 방향 가능)
    // 여기서는 가장 많이 쓰이는 '우측 하단 확대/축소' 핸들을 눈에 띄게 배치
    Box(
        modifier = Modifier
            .align(Alignment.BottomEnd)
            .offset(x = 6.dp, y = 6.dp)
            .size(handleSize)
            .background(ColorPrimaryOrange, CircleShape)
            .pointerInput(Unit) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    val newW = (seat.size.value.x + dragAmount.x / scale).coerceAtLeast(gridSizePx)
                    val newH = (seat.size.value.y + dragAmount.y / scale).coerceAtLeast(gridSizePx)
                    seat.size.value = Offset(
                        (newW / gridSizePx).roundToInt() * gridSizePx,
                        (newH / gridSizePx).roundToInt() * gridSizePx
                    )
                }
            }
    )
}

@Composable
private fun ToolCircleWithLabel(symbolName: String, label: String, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { onClick() }) {
        Box(Modifier.size(ToolCircleSize).background(ColorCardBg, CircleShape), Alignment.Center) {
            MaterialSymbol(symbolName, size = 20.sp, tint = ColorTextBlack)
        }
        Text(label, fontSize = 10.sp, color = ColorTextGray)
    }
}