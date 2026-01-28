package kr.jiyeok.seatly.ui.screen.admin

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import kr.jiyeok.seatly.data.local.Seat
import kr.jiyeok.seatly.data.local.SeatType
import kotlinx.datetime.Clock
import kotlin.math.roundToInt
import kr.jiyeok.seatly.data.remote.enums.ESeatStatus
import kr.jiyeok.seatly.presentation.viewmodel.AdminCafeDetailViewModel
import kr.jiyeok.seatly.ui.component.common.MaterialSymbol
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

    val uiState by viewModel.uiState.collectAsState()

    var isEditMode by remember { mutableStateOf(false) }
    val seats = remember { mutableStateListOf<Seat>() }

    LaunchedEffect(uiState.seats) {
        if (uiState.seats.isNotEmpty()) {
            seats.clear()
            uiState.seats.forEach { dto ->
                val parts = dto.position.split(",")
                val pos = if (parts.size >= 2) {
                    Offset(
                        parts[0].toFloatOrNull() ?: 0f,
                        parts[1].toFloatOrNull() ?: 0f
                    )
                } else {
                    Offset(100f, 100f)
                }
                val size = if (parts.size >= 4) {
                    Offset(
                        parts[2].toFloatOrNull() ?: 40f,
                        parts[3].toFloatOrNull() ?: 40f
                    )
                } else {
                    Offset(40f, 40f)
                }
                seats.add(
                    Seat(
                        id = dto.id.toString(),
                        label = dto.name,
                        type = if (dto.name.startsWith("WALL")) SeatType.WALL else SeatType.NORMAL,
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

    val addItemAtCenter: (String) -> Unit = { prefix ->
        val screenCenter = Offset(
            workspaceSize.value.width / 2f,
            workspaceSize.value.height / 2f
        )
        val contentX = (screenCenter.x - translateX.value) / scaleState.value
        val contentY = (screenCenter.y - translateY.value) / scaleState.value
        val snappedX = (contentX / gridCellPx).roundToInt() * gridCellPx
        val snappedY = (contentY / gridCellPx).roundToInt() * gridCellPx

        val newLabel = when (prefix) {
            "WALL" -> {
                val wallCount = seats.count { it.label.startsWith("WALL") }
                "WALL_${wallCount + 1}"
            }
            "LABEL" -> "LABEL_텍스트"
            else -> {
                val existingNumbers = seats
                    .filter { !it.label.startsWith("WALL") && !it.label.startsWith("LABEL") }
                    .mapNotNull { it.label.replace("번", "").trim().toIntOrNull() }
                val nextNumber = (existingNumbers.maxOrNull() ?: 0) + 1
                "${nextNumber}번"
            }
        }

        val newItem = Seat(
            id = "NEW_${Clock.System.now().toEpochMilliseconds()}",
            label = newLabel,
            type = when {
                newLabel.startsWith("WALL") -> SeatType.WALL
                else -> SeatType.NORMAL
            },
            pos = mutableStateOf(Offset(snappedX, snappedY)),
            size = mutableStateOf(Offset(gridCellPx, gridCellPx))
        )

        seats.add(newItem)
        selectedSeatId.value = newItem.id
    }

    val copySeat: () -> Unit = {
        selectedSeatId.value?.let { id ->
            seats.find { it.id == id }?.let { originalSeat ->
                val offset = gridCellPx * 2

                val newLabel = when {
                    originalSeat.label.startsWith("WALL") -> {
                        val wallCount = seats.count { it.label.startsWith("WALL") }
                        "WALL_${wallCount + 1}"
                    }
                    originalSeat.label.startsWith("LABEL") -> "LABEL_텍스트"
                    else -> {
                        val existingNumbers = seats
                            .filter { !it.label.startsWith("WALL") && !it.label.startsWith("LABEL") }
                            .mapNotNull { it.label.replace("번", "").trim().toIntOrNull() }
                        val nextNumber = (existingNumbers.maxOrNull() ?: 0) + 1
                        "${nextNumber}번"
                    }
                }

                val newItem = Seat(
                    id = "NEW_${Clock.System.now().toEpochMilliseconds()}",
                    label = newLabel,
                    type = originalSeat.type,
                    pos = mutableStateOf(
                        Offset(
                            originalSeat.pos.value.x + offset,
                            originalSeat.pos.value.y + offset
                        )
                    ),
                    size = mutableStateOf(originalSeat.size.value)
                )

                seats.add(newItem)
                selectedSeatId.value = newItem.id
            }
        }
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
                    SeatMapContent(
                        seats = seats,
                        scaleState = scaleState,
                        scaleAnimated = scaleAnimated,
                        translateX = translateX,
                        translateY = translateY,
                        workspaceSize = workspaceSize,
                        gridCellPx = gridCellPx,
                        selectedSeatId = selectedSeatId,
                        isEditMode = isEditMode,
                        uiState = uiState,
                        onEditModeToggle = { isEditMode = !isEditMode },
                        onSave = {
                            viewModel.saveSeatConfig(cafeId, seats)
                            isEditMode = false
                        },
                        addItemAtCenter = addItemAtCenter,
                        copySeat = copySeat
                    )
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
    isEditMode: Boolean,
    uiState: AdminCafeDetailViewModel.CafeDetailUiState,
    onEditModeToggle: () -> Unit,
    onSave: () -> Unit,
    addItemAtCenter: (String) -> Unit,
    copySeat: () -> Unit
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
            val color = if (isEditMode) {
                ColorBorderLight
            } else {
                ColorBorderLight.copy(alpha = 0.1f)
            }

            for (x in generateSequence(startX) { it + gridPx }.takeWhile { it < size.width }) {
                drawLine(color, Offset(x, 0f), Offset(x, size.height))
            }
            for (y in generateSequence(startY) { it + gridPx }.takeWhile { it < size.height }) {
                drawLine(color, Offset(0f, y), Offset(size.width, y))
            }
        }

        seats.forEachIndexed { index, seat ->
            val isSelected = selectedSeatId.value == seat.id

            val currentStatus = if (!isEditMode) {
                val isOccupied = uiState.sessions.any { session ->
                    session.seatId.toString() == seat.id
                }
                if (isOccupied) ESeatStatus.UNAVAILABLE else ESeatStatus.AVAILABLE
            } else {
                uiState.seats
                    .find { it.id.toString() == seat.id }?.status
                    ?: ESeatStatus.AVAILABLE
            }

            SeatItem(
                seat = seat,
                scale = scaleAnimated,
                translate = Offset(translateX.value, translateY.value),
                isSelected = isSelected,
                isEditMode = isEditMode,
                status = currentStatus,
                gridSizePx = gridCellPx,
                zIndex = if (isSelected) seats.size.toFloat() else index.toFloat(),
                onSelect = { selectedSeatId.value = it }
            )
        }

    }

    val selectedSeat = seats.find { it.id == selectedSeatId.value }

    AnimatedVisibility(
        visible = isEditMode && selectedSeat != null,
        enter = slideInVertically(initialOffsetY = { -it }),
        exit = slideOutVertically(targetOffsetY = { -it }),
        modifier = Modifier.align(Alignment.TopCenter)
    ) {
        selectedSeat?.let { seat ->
            SeatInfoPanel(
                seat = seat,
                gridCellPx = gridCellPx,
                onClose = { selectedSeatId.value = null },
                onCopy = copySeat
            )
        }
    }

    Column(
        modifier = Modifier
            .align(Alignment.BottomEnd)
            .padding(
                end = 16.dp,
                bottom = if (isEditMode) 180.dp else 90.dp
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

    if (isEditMode) {
        EditControlBar(
            selectedSeatId = selectedSeatId,
            seats = seats,
            isSaving = uiState.isLoadingSeats,
            addItemAtCenter = addItemAtCenter,
            onSave = onSave
        )
    } else {
        FloatingActionButton(
            onClick = onEditModeToggle,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 20.dp, bottom = 20.dp),
            containerColor = ColorPrimaryOrange,
            contentColor = ColorWhite
        ) {
            Icon(
                imageVector = Icons.Default.Edit,
                contentDescription = "공간 편집"
            )
        }
    }
}

@Composable
private fun SeatInfoPanel(
    seat: Seat,
    gridCellPx: Float,
    onClose: () -> Unit,
    onCopy: () -> Unit
) {
    var editingName by remember { mutableStateOf(seat.label) }
    var editingWidth by remember { mutableStateOf((seat.size.value.x / gridCellPx).roundToInt().toString()) }
    var editingHeight by remember { mutableStateOf((seat.size.value.y / gridCellPx).roundToInt().toString()) }

    LaunchedEffect(seat.label) {
        editingName = seat.label
    }

    LaunchedEffect(seat.size.value) {
        editingWidth = (seat.size.value.x / gridCellPx).roundToInt().toString()
        editingHeight = (seat.size.value.y / gridCellPx).roundToInt().toString()
    }

    Card(
        modifier = Modifier
            .padding(16.dp)
            .fillMaxWidth()
            .shadow(8.dp, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(ColorWhite)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "선택된 객체 정보",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = ColorTextBlack
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onCopy,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "복사",
                            tint = ColorPrimaryOrange,
                            modifier = Modifier.size(18.dp)
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
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            HorizontalDivider(color = ColorBorderLight)

            Spacer(modifier = Modifier.height(16.dp))

            EditableInfoRow(
                label = "이름",
                value = editingName,
                onValueChange = {
                    editingName = it
                    seat.label = it
                }
            )

            Spacer(modifier = Modifier.height(12.dp))

            EditableSizeRow(
                label = "크기",
                widthValue = editingWidth,
                heightValue = editingHeight,
                onWidthChange = { newValue ->
                    editingWidth = newValue
                    val grids = newValue.toIntOrNull() ?: 1
                    seat.size.value = Offset(
                        (grids * gridCellPx).coerceAtLeast(gridCellPx),
                        seat.size.value.y
                    )
                },
                onHeightChange = { newValue ->
                    editingHeight = newValue
                    val grids = newValue.toIntOrNull() ?: 1
                    seat.size.value = Offset(
                        seat.size.value.x,
                        (grids * gridCellPx).coerceAtLeast(gridCellPx)
                    )
                }
            )
        }
    }
}

@Composable
private fun EditableInfoRow(
    label: String,
    value: String,
    onValueChange: (String) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 14.sp,
            color = ColorTextGray,
            fontWeight = FontWeight.Medium
        )

        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            textStyle = TextStyle(
                fontSize = 14.sp,
                color = ColorTextBlack,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.End
            ),
            cursorBrush = SolidColor(ColorPrimaryOrange),
            modifier = Modifier
                .weight(1f)
                .padding(start = 16.dp),
            singleLine = true,
            decorationBox = { innerTextField ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(ColorBgBeige, RoundedCornerShape(8.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    innerTextField()
                }
            }
        )
    }
}

@Composable
private fun EditableSizeRow(
    label: String,
    widthValue: String,
    heightValue: String,
    onWidthChange: (String) -> Unit,
    onHeightChange: (String) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 14.sp,
            color = ColorTextGray,
            fontWeight = FontWeight.Medium
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            BasicTextField(
                value = widthValue,
                onValueChange = { newValue ->
                    if (newValue.isEmpty() || newValue.all { it.isDigit() }) {
                        onWidthChange(newValue)
                    }
                },
                textStyle = TextStyle(
                    fontSize = 14.sp,
                    color = ColorTextBlack,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center
                ),
                cursorBrush = SolidColor(ColorPrimaryOrange),
                modifier = Modifier.width(50.dp),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                decorationBox = { innerTextField ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(ColorBgBeige, RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        innerTextField()
                    }
                }
            )

            Text(
                text = "칸",
                fontSize = 12.sp,
                color = ColorTextGray,
                fontWeight = FontWeight.Medium
            )

            Text(
                text = "X",
                fontSize = 14.sp,
                color = ColorTextGray,
                fontWeight = FontWeight.Bold
            )

            BasicTextField(
                value = heightValue,
                onValueChange = { newValue ->
                    if (newValue.isEmpty() || newValue.all { it.isDigit() }) {
                        onHeightChange(newValue)
                    }
                },
                textStyle = TextStyle(
                    fontSize = 14.sp,
                    color = ColorTextBlack,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center
                ),
                cursorBrush = SolidColor(ColorPrimaryOrange),
                modifier = Modifier.width(50.dp),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                decorationBox = { innerTextField ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(ColorBgBeige, RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        innerTextField()
                    }
                }
            )

            Text(
                text = "칸",
                fontSize = 12.sp,
                color = ColorTextGray,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun BoxScope.EditControlBar(
    selectedSeatId: MutableState<String?>,
    seats: MutableList<Seat>,
    addItemAtCenter: (String) -> Unit,
    onSave: () -> Unit,
    isSaving: Boolean = false
) {
    Box(
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .padding(16.dp)
            .fillMaxWidth()
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            elevation = CardDefaults.cardElevation(8.dp),
            colors = CardDefaults.cardColors(ColorWhite)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ToolCircleWithLabel("chair", "좌석") {
                        addItemAtCenter("")
                    }
                    ToolCircleWithLabel("rectangle", "벽") {
                        addItemAtCenter("WALL")
                    }
                    ToolCircleWithLabel("delete", "삭제") {
                        selectedSeatId.value?.let { id ->
                            seats.removeAll { it.id == id }
                            selectedSeatId.value = null
                        }
                    }
                }

                Button(
                    onClick = onSave,
                    enabled = !isSaving,
                    colors = ButtonDefaults.buttonColors(ColorPrimaryOrange)
                ) {
                    if (isSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = ColorWhite,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text("저장", fontWeight = FontWeight.Bold, color = ColorWhite)
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
    isEditMode: Boolean,
    status: ESeatStatus,
    gridSizePx: Float,
    zIndex: Float,
    onSelect: (String) -> Unit
) {
    val density = LocalDensity.current
    val isWall = seat.label.startsWith("WALL")
    val displayText = when {
        isWall -> ""
        else -> seat.label
    }

    val pos by seat.pos
    val size by seat.size
    val screenX = pos.x * scale + translate.x
    val screenY = pos.y * scale + translate.y
    val widthPx = size.x * scale
    val heightPx = size.y * scale

    val isResizing = remember { mutableStateOf(false) }

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
                    if (isEditMode && !isResizing.value) {
                        Modifier
                            .pointerInput(isSelected) {
                                detectTapGestures(
                                    onTap = {
                                        onSelect(seat.id)
                                    }
                                )
                            }
                            .then(
                                if (isSelected) {
                                    Modifier.pointerInput(Unit) {
                                        detectDragGestures(
                                            onDrag = { change, dragAmount ->
                                                change.consume()
                                                seat.pos.value += Offset(
                                                    dragAmount.x / scale,
                                                    dragAmount.y / scale
                                                )
                                            },
                                            onDragEnd = {
                                                seat.pos.value = Offset(
                                                    (seat.pos.value.x / gridSizePx).roundToInt() * gridSizePx,
                                                    (seat.pos.value.y / gridSizePx).roundToInt() * gridSizePx
                                                )
                                            }
                                        )
                                    }
                                } else {
                                    Modifier
                                }
                            )
                    } else {
                        Modifier
                    }
                )
        ) {
            val backgroundColor = when {
                isWall -> if (isSelected && isEditMode) Color.DarkGray else Color.LightGray
                !isEditMode && status == ESeatStatus.UNAVAILABLE -> Color(0xFFE57373) // Warning color
                else -> if (isSelected && isEditMode) {
                    ColorPrimaryOrange.copy(alpha = 0.2f)
                } else {
                    ColorWhite
                }
            }

            Card(
                shape = if (isWall) RoundedCornerShape(0.dp) else RoundedCornerShape(8.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .border(
                        width = if (isSelected && isEditMode) 3.dp else 1.dp,
                        color = if (isSelected && isEditMode) ColorPrimaryOrange else ColorBorderLight,
                        shape = if (isWall) RoundedCornerShape(0.dp) else RoundedCornerShape(8.dp)
                    ),
                colors = CardDefaults.cardColors(backgroundColor)
            ) {
                Box(
                    Modifier.fillMaxSize(),
                    Alignment.Center
                ) {
                    Text(
                        text = displayText,
                        fontSize = (12 * scale).sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        if (isEditMode && isSelected) {
            ResizeHandles(seat, scale, gridSizePx, isResizing)
        }
    }
}

@Composable
private fun BoxScope.ResizeHandles(
    seat: Seat,
    scale: Float,
    gridSizePx: Float,
    isResizing: MutableState<Boolean>
) {
    val handleSize = 20.dp
    val handleColor = ColorPrimaryOrange

    ResizeHandle(
        alignment = Alignment.BottomEnd,
        offset = Offset(10f, 10f),
        size = handleSize,
        color = handleColor,
        isResizing = isResizing
    ) { dragAmount ->
        val newW = (seat.size.value.x + dragAmount.x / scale).coerceAtLeast(gridSizePx)
        val newH = (seat.size.value.y + dragAmount.y / scale).coerceAtLeast(gridSizePx)
        seat.size.value = Offset(newW, newH)
    }

    ResizeHandle(
        alignment = Alignment.TopEnd,
        offset = Offset(10f, -10f),
        size = handleSize,
        color = handleColor,
        isResizing = isResizing
    ) { dragAmount ->
        val newW = (seat.size.value.x + dragAmount.x / scale).coerceAtLeast(gridSizePx)
        val newH = (seat.size.value.y - dragAmount.y / scale).coerceAtLeast(gridSizePx)
        val deltaY = seat.size.value.y - newH

        seat.pos.value = Offset(seat.pos.value.x, seat.pos.value.y + deltaY)
        seat.size.value = Offset(newW, newH)
    }

    ResizeHandle(
        alignment = Alignment.BottomStart,
        offset = Offset(-10f, 10f),
        size = handleSize,
        color = handleColor,
        isResizing = isResizing
    ) { dragAmount ->
        val newW = (seat.size.value.x - dragAmount.x / scale).coerceAtLeast(gridSizePx)
        val newH = (seat.size.value.y + dragAmount.y / scale).coerceAtLeast(gridSizePx)
        val deltaX = seat.size.value.x - newW

        seat.pos.value = Offset(seat.pos.value.x + deltaX, seat.pos.value.y)
        seat.size.value = Offset(newW, newH)
    }

    ResizeHandle(
        alignment = Alignment.TopStart,
        offset = Offset(-10f, -10f),
        size = handleSize,
        color = handleColor,
        isResizing = isResizing
    ) { dragAmount ->
        val newW = (seat.size.value.x - dragAmount.x / scale).coerceAtLeast(gridSizePx)
        val newH = (seat.size.value.y - dragAmount.y / scale).coerceAtLeast(gridSizePx)
        val deltaX = seat.size.value.x - newW
        val deltaY = seat.size.value.y - newH

        seat.pos.value = Offset(seat.pos.value.x + deltaX, seat.pos.value.y + deltaY)
        seat.size.value = Offset(newW, newH)
    }

    ResizeHandle(
        alignment = Alignment.CenterEnd,
        offset = Offset(10f, 0f),
        size = handleSize,
        color = handleColor,
        isResizing = isResizing
    ) { dragAmount ->
        val newW = (seat.size.value.x + dragAmount.x / scale).coerceAtLeast(gridSizePx)
        seat.size.value = Offset(newW, seat.size.value.y)
    }

    ResizeHandle(
        alignment = Alignment.CenterStart,
        offset = Offset(-10f, 0f),
        size = handleSize,
        color = handleColor,
        isResizing = isResizing
    ) { dragAmount ->
        val newW = (seat.size.value.x - dragAmount.x / scale).coerceAtLeast(gridSizePx)
        val deltaX = seat.size.value.x - newW

        seat.pos.value = Offset(seat.pos.value.x + deltaX, seat.pos.value.y)
        seat.size.value = Offset(newW, seat.size.value.y)
    }

    ResizeHandle(
        alignment = Alignment.BottomCenter,
        offset = Offset(0f, 10f),
        size = handleSize,
        color = handleColor,
        isResizing = isResizing
    ) { dragAmount ->
        val newH = (seat.size.value.y + dragAmount.y / scale).coerceAtLeast(gridSizePx)
        seat.size.value = Offset(seat.size.value.x, newH)
    }

    ResizeHandle(
        alignment = Alignment.TopCenter,
        offset = Offset(0f, -10f),
        size = handleSize,
        color = handleColor,
        isResizing = isResizing
    ) { dragAmount ->
        val newH = (seat.size.value.y - dragAmount.y / scale).coerceAtLeast(gridSizePx)
        val deltaY = seat.size.value.y - newH

        seat.pos.value = Offset(seat.pos.value.x, seat.pos.value.y + deltaY)
        seat.size.value = Offset(seat.size.value.x, newH)
    }
}

@Composable
private fun BoxScope.ResizeHandle(
    alignment: Alignment,
    offset: Offset,
    size: androidx.compose.ui.unit.Dp,
    color: Color,
    isResizing: MutableState<Boolean>,
    onDrag: (Offset) -> Unit
) {
    Box(
        modifier = Modifier
            .align(alignment)
            .offset(x = offset.x.dp, y = offset.y.dp)
            .size(size)
            .background(color, CircleShape)
            .border(2.dp, ColorWhite, CircleShape)
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = {
                        isResizing.value = true
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        onDrag(dragAmount)
                    },
                    onDragEnd = {
                        isResizing.value = false
                    },
                    onDragCancel = {
                        isResizing.value = false
                    }
                )
            }
    )
}

@Composable
private fun ToolCircleWithLabel(
    symbolName: String,
    label: String,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable { onClick() }
    ) {
        Box(
            Modifier
                .size(ToolCircleSize)
                .background(ColorCardBg, CircleShape),
            Alignment.Center
        ) {
            MaterialSymbol(
                name = symbolName,
                size = 20.sp,
                tint = ColorTextBlack
            )
        }
        Text(
            text = label,
            fontSize = 10.sp,
            color = ColorTextGray
        )
    }
}
