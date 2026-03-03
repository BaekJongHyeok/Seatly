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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
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
                        type = SeatType.NORMAL,
                        pos = mutableStateOf(pos),
                        size = mutableStateOf(size),
                        availabilityStatus = dto.status
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
    
    // 사용자 정보 다이얼로그 상태
    var showUserInfoDialog by remember { mutableStateOf(false) }
    // Triple: MemberWithUserInfo?, seatLabel, EStatus
    var selectedUserSession by remember { mutableStateOf<Triple<AdminCafeDetailViewModel.MemberWithUserInfo?, String, kr.jiyeok.seatly.data.remote.enums.EStatus?>?>(null) }
    val workspaceSize = remember { mutableStateOf(IntSize(0, 0)) }

    val isContentReady = remember(uiState.seats, uiState.isLoadingSeats) {
        !uiState.isLoadingSeats
    }

    // 편집 모드 진입 시 좌석 사용자 정보 패널 자동 좌기화
    LaunchedEffect(isEditMode) {
        if (isEditMode) {
            showUserInfoDialog = false
            selectedUserSession = null
        }
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
                // ★ WALL에 고유 번호 추가 ★
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
            id = "NEW_${System.currentTimeMillis()}",
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
                    id = "NEW_${System.currentTimeMillis()}",
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

    val deleteSeat: () -> Unit = {
        selectedSeatId.value?.let { id ->
            seats.removeIf { it.id == id }
            selectedSeatId.value = null
        }
    }

    Surface(
        color = ColorWhite,
        modifier = Modifier.fillMaxSize()
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTransformGestures { centroid, pan, zoom, _ ->
                        val oldScale = scaleState.value
                        val newScale = (oldScale * zoom).coerceIn(0.5f, 5f)
                        translateX.value =
                            (translateX.value - centroid.x) * (newScale / oldScale) + centroid.x + pan.x
                        translateY.value =
                            (translateY.value - centroid.y) * (newScale / oldScale) + centroid.y + pan.y
                        scaleState.value = newScale
                    }
                }
        ) {
            when {
                !isContentReady -> {
                    LoadingView()
                }
                seats.isEmpty() && !isEditMode -> {
                    // 좌석이 없을 때 안내 가이드
                    EmptySeatGuide(
                        onEditClick = { isEditMode = true },
                        modifier = Modifier.fillMaxSize()
                    )
                }
                else -> {
                    SeatMapContent(
                        seats = seats,
                        uiState = uiState,
                        isEditMode = isEditMode,
                        gridCellPx = gridCellPx,
                        scale = scaleAnimated,
                        translateX = translateX,
                        translateY = translateY,
                        selectedSeatId = selectedSeatId,
                        onSeatClick = { seat, session, member, isOccupied ->
                            if (!isEditMode && session != null) {
                                selectedUserSession = Triple(member, seat.label, session.status)
                                showUserInfoDialog = true
                            } else if (isEditMode && !isOccupied) {
                                selectedSeatId.value = seat.id
                            }
                        },
                        copySeat = copySeat,
                        onDelete = deleteSeat
                    )

                    // 사용자 정보 패널 (뷰 모드 - 상단 슬라이드)
                    AnimatedVisibility(
                        visible = showUserInfoDialog && selectedUserSession != null,
                        enter = slideInVertically(initialOffsetY = { -it }),
                        exit = slideOutVertically(targetOffsetY = { -it }),
                        modifier = Modifier.align(Alignment.TopCenter)
                    ) {
                        if (selectedUserSession != null) {
                            val (member, seatLabel, sessionStatus) = selectedUserSession!!
                            SeatOccupantInfoPanel(
                                seatLabel = seatLabel,
                                member = member,
                                sessionStatus = sessionStatus,
                                onClose = { showUserInfoDialog = false }
                            )
                        }
                    }

                    // 편집 모드 제어
                    if (isEditMode) {
                        EditControlBar(
                            selectedSeatId = selectedSeatId,
                            seats = seats,
                            isSaving = uiState.isLoadingSeats,
                            addItemAtCenter = addItemAtCenter,
                            onSave = {
                                viewModel.saveSeatConfig(cafeId, seats)
                                isEditMode = false
                            }
                        )
                    } else {
                        // 줌 컨트롤 (뷰 모드)
                        ZoomControls(
                            scaleState = scaleState,
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(end = 16.dp, bottom = 90.dp)
                        )
                        FloatingActionButton(
                            onClick = { isEditMode = true },
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
private fun EmptySeatGuide(
    onEditClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.background(ColorWhite),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            MaterialSymbol(
                name = "chair",
                size = 56.sp,
                tint = ColorBorderLight
            )

            Text(
                text = "등록된 좌석이 없습니다",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = ColorTextBlack
            )

            Text(
                text = "편집 버튼을 눌러 좌석을 추가해보세요",
                fontSize = 14.sp,
                color = ColorTextGray
            )

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = onEditClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = ColorPrimaryOrange,
                    contentColor = ColorWhite
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("좌석 편집 시작", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun BoxScope.SeatMapContent(
    seats: List<Seat>,
    uiState: AdminCafeDetailViewModel.CafeDetailUiState,
    isEditMode: Boolean,
    gridCellPx: Float,
    scale: Float,
    translateX: MutableState<Float>,
    translateY: MutableState<Float>,
    selectedSeatId: MutableState<String?>,
    onSeatClick: (Seat, kr.jiyeok.seatly.data.remote.response.SessionDto?, AdminCafeDetailViewModel.MemberWithUserInfo?, Boolean) -> Unit,
    copySeat: () -> Unit,
    onDelete: () -> Unit
) {
    val scaleAnimated by animateFloatAsState(targetValue = scale, label = "scale")

    Box(
        modifier = Modifier
            .fillMaxSize()
            .then(
                if (isEditMode) {
                    Modifier.pointerInput(isEditMode) {
                        detectTapGestures(onTap = {
                            // 캔버스 빈 영역 탭 → 선택 해제
                            selectedSeatId.value = null
                        })
                    }
                } else Modifier
            )
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

            // 세션 기반 이용중 여부 — 편집 모드에서도 항상 계산
            val matchingSession = uiState.sessions.find { session ->
                session.seatId.toString() == seat.id
            }
            val isOccupied = matchingSession != null

            val currentStatus = when {
                isOccupied -> ESeatStatus.UNAVAILABLE
                // 편집 모드: availabilityStatus 기반
                isEditMode -> seat.availabilityStatus
                // 뷰 모드: 서버 상태 기반
                else -> uiState.seats
                    .find { it.id.toString() == seat.id }?.status
                    ?: ESeatStatus.AVAILABLE
            }

            // 이용 중인 사용자 정보
            val occupantMember = matchingSession?.let { session ->
                uiState.members.find { it.basicInfo.userId == session.userId }
            }
            val occupantName = occupantMember?.let {
                it.detailInfo?.name ?: it.basicInfo.name
            }
            val occupantLeftTime = occupantMember?.basicInfo?.leftTime

            SeatItem(
                seat = seat,
                scale = scaleAnimated,
                translate = Offset(translateX.value, translateY.value),
                isSelected = isSelected && !isOccupied,
                isEditMode = isEditMode,
                isOccupied = isOccupied,
                sessionStatus = matchingSession?.status,
                status = currentStatus,
                userName = occupantName,
                leftTimeSeconds = occupantLeftTime,
                gridSizePx = gridCellPx,
                zIndex = if (isSelected) seats.size.toFloat() else index.toFloat(),
                onSelect = {
                    onSeatClick(seat, matchingSession, occupantMember, isOccupied)
                }
            )
        }
    }

    val selectedSeat = seats.find { it.id == selectedSeatId.value }

    // ── 선택된 좌석 정보 패널 (하단 슬라이드) ───────────────────
    AnimatedVisibility(
        visible = isEditMode && selectedSeat != null,
        enter = slideInVertically(initialOffsetY = { it }),
        exit = slideOutVertically(targetOffsetY = { it }),
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .zIndex(2f)
    ) {
        selectedSeat?.let { seat ->
            SeatInfoPanel(
                seat = seat,
                gridCellPx = gridCellPx,
                isOccupied = uiState.sessions.any { it.seatId.toString() == seat.id },
                onClose = { selectedSeatId.value = null },
                onDelete = onDelete,
                onCopy = copySeat
            )
        }
    }
}

@Composable
private fun SeatInfoPanel(
    seat: Seat,
    gridCellPx: Float,
    isOccupied: Boolean,
    onClose: () -> Unit,
    onDelete: () -> Unit,
    onCopy: () -> Unit
) {
    var availabilityStatus by remember(seat.id) { mutableStateOf(seat.availabilityStatus) }
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
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
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

                    // 삭제 버튼
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "삭제",
                            tint = Color(0xFFE53935),
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // X 버튼 — 패널 닫기만
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

            // 사용가능 / 사용불가 토글 (이용중 좌석은 수정 불가)
            if (!isOccupied) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = ColorCardBg,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (availabilityStatus == ESeatStatus.AVAILABLE)
                                ColorPrimaryOrange
                            else Color.Transparent,
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    availabilityStatus = ESeatStatus.AVAILABLE
                                    seat.availabilityStatus = ESeatStatus.AVAILABLE
                                }
                        ) {
                            Text(
                                text = "사용 가능",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (availabilityStatus == ESeatStatus.AVAILABLE)
                                    ColorWhite
                                else ColorTextGray,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (availabilityStatus == ESeatStatus.UNAVAILABLE)
                                Color(0xFFE53935)
                            else Color.Transparent,
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    availabilityStatus = ESeatStatus.UNAVAILABLE
                                    seat.availabilityStatus = ESeatStatus.UNAVAILABLE
                                }
                        ) {
                            Text(
                                text = "사용 불가",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (availabilityStatus == ESeatStatus.UNAVAILABLE)
                                    ColorWhite
                                else ColorTextGray,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }
                    }
                }
            } else {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = ColorPrimaryOrange.copy(alpha = 0.1f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "🔒 현재 이용 중인 좌석입니다",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = ColorPrimaryOrange,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Divider(color = ColorBorderLight)

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
private fun BoxScope.SeatItem(
    seat: Seat,
    scale: Float,
    translate: Offset,
    isSelected: Boolean,
    isEditMode: Boolean,
    isOccupied: Boolean = false,
    sessionStatus: kr.jiyeok.seatly.data.remote.enums.EStatus? = null,
    status: ESeatStatus,
    userName: String? = null,
    leftTimeSeconds: Long? = null,
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

    val isResizing = remember { mutableStateOf(false) }

    val textMeasurer = androidx.compose.ui.text.rememberTextMeasurer()
    val seatTextSize = (12 * scale).sp
    val seatTextLayout = if (displayText.isNotEmpty()) {
        textMeasurer.measure(
            text = displayText,
            style = TextStyle(fontSize = seatTextSize, fontWeight = FontWeight.SemiBold)
        )
    } else null
    val canShowSeatName = seatTextLayout == null || (widthPx >= seatTextLayout.size.width + (8 * scale) && heightPx >= seatTextLayout.size.height + (8 * scale))

    val userTextSize = (10 * scale).sp
    val userTextLayout = if (userName != null) {
        textMeasurer.measure(
            text = userName,
            style = TextStyle(fontSize = userTextSize, fontWeight = FontWeight.SemiBold)
        )
    } else null
    val canShowUserName = userTextLayout == null || (widthPx >= userTextLayout.size.width + (8 * scale) && heightPx >= (seatTextLayout?.size?.height?.toFloat() ?: 0f) + userTextLayout.size.height + (16 * scale))

    Box(
        modifier = Modifier
            .zIndex(zIndex)
            .offset { IntOffset(screenX.roundToInt(), screenY.roundToInt()) }
            .size(
                with(density) { widthPx.toDp() },
                with(density) { heightPx.toDp() }
            )
    ) {
        // 편집 모드에서 이용중인 좌석은 탭/드래그 모두 불가
        val canEdit = isEditMode && !isOccupied
        Box(
            modifier = Modifier
                .fillMaxSize()
                .then(
                    if (!isResizing.value) {
                        Modifier
                            .pointerInput(isSelected, isEditMode, isOccupied) {
                                detectTapGestures(
                                    onTap = {
                                        onSelect(seat.id)
                                    }
                                )
                            }
                            .then(
                                if (canEdit && isSelected) {
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
                sessionStatus == kr.jiyeok.seatly.data.remote.enums.EStatus.ASSIGNED -> Color(0xFFF0F0F0)
                sessionStatus == kr.jiyeok.seatly.data.remote.enums.EStatus.IN_USE -> Color(0xFFFFE0B2)
                status == ESeatStatus.UNAVAILABLE -> ColorTextLightGray.copy(alpha = 0.5f)
                else -> if (isSelected && isEditMode) {
                    ColorPrimaryOrange.copy(alpha = 0.2f)
                } else {
                    ColorWhite
                }
            }
            
            val borderColor = when {
                isWall -> if (isSelected && isEditMode) ColorPrimaryOrange else ColorBorderLight
                sessionStatus == kr.jiyeok.seatly.data.remote.enums.EStatus.ASSIGNED -> ColorBorderLight.copy(alpha = 0.5f)
                sessionStatus == kr.jiyeok.seatly.data.remote.enums.EStatus.IN_USE -> ColorPrimaryOrange
                isSelected -> ColorPrimaryOrange
                else -> ColorBorderLight
            }

            Card(
                shape = if (isWall) RoundedCornerShape(0.dp) else RoundedCornerShape(8.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .border(
                        width = if (isSelected && isEditMode) 2.dp else 1.dp,
                        color = borderColor,
                        shape = if (isWall) RoundedCornerShape(0.dp) else RoundedCornerShape(8.dp)
                    ),
                colors = CardDefaults.cardColors(backgroundColor),
                elevation = CardDefaults.cardElevation(defaultElevation = if (isOccupied) 1.dp else 2.dp)
            ) {
                Box(
                    Modifier.fillMaxSize(),
                    Alignment.Center
                ) {
                    if (!isWall) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            if (isOccupied && canShowUserName) {
                                if (sessionStatus == kr.jiyeok.seatly.data.remote.enums.EStatus.ASSIGNED) {
                                    Icon(
                                        imageVector = Icons.Default.Person,
                                        contentDescription = "점유중",
                                        tint = ColorTextGray,
                                        modifier = Modifier.size(with(density) { (12 * scale).dp })
                                    )
                                } else if (sessionStatus == kr.jiyeok.seatly.data.remote.enums.EStatus.IN_USE) {
                                    Icon(
                                        imageVector = Icons.Default.Lock,
                                        contentDescription = "이용중",
                                        tint = ColorPrimaryOrange,
                                        modifier = Modifier.size(with(density) { (12 * scale).dp })
                                    )
                                }
                                Spacer(modifier = Modifier.height(with(density) { (2 * scale).dp }))
                            }

                            if (canShowSeatName && displayText.isNotEmpty()) {
                                Text(
                                    text = displayText,
                                    fontSize = seatTextSize,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (isOccupied) ColorTextGray else if (status == ESeatStatus.UNAVAILABLE) Color(0xFF757575) else ColorTextBlack,
                                    textAlign = TextAlign.Center
                                )
                            }
                            
                            if (canShowUserName && userName != null && isOccupied) {
                                Text(
                                    text = userName,
                                    fontSize = userTextSize,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (sessionStatus == kr.jiyeok.seatly.data.remote.enums.EStatus.IN_USE) ColorPrimaryOrange else ColorTextGray,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
            }
        }

        // 편집 모드 이용중 좌석: 오렌지/회색 테두리 + 자물쇠 아이콘
        if (isEditMode && isOccupied) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .border(
                        width = 2.dp,
                        color = if (sessionStatus == kr.jiyeok.seatly.data.remote.enums.EStatus.IN_USE) ColorPrimaryOrange else ColorTextGray,
                        shape = if (isWall) RoundedCornerShape(0.dp) else RoundedCornerShape(8.dp)
                    )
                    .background(Color.Transparent),
                contentAlignment = Alignment.Center
            ) {
                kr.jiyeok.seatly.ui.component.common.MaterialSymbol(
                    name = "lock",
                    size = (with(density) { (minOf(widthPx, heightPx) * 0.35f).toDp().value }).coerceIn(10f, 24f).sp,
                    tint = if (sessionStatus == kr.jiyeok.seatly.data.remote.enums.EStatus.IN_USE) ColorPrimaryOrange else ColorTextGray
                )
            }
        }

        if (canEdit && isSelected) {
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
private fun SeatOccupantInfoPanel(
    seatLabel: String,
    member: AdminCafeDetailViewModel.MemberWithUserInfo?,
    sessionStatus: kr.jiyeok.seatly.data.remote.enums.EStatus?,
    onClose: () -> Unit
) {
    val userName = member?.detailInfo?.name ?: member?.basicInfo?.name ?: "알 수 없음"
    val leftTime = member?.basicInfo?.leftTime

    Surface(
        color = ColorWhite,
        shape = RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp),
        shadowElevation = 8.dp,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "좌석 정보",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = ColorTextBlack
                )
                IconButton(onClick = onClose) {
                    Icon(Icons.Default.Close, contentDescription = "닫기", tint = ColorTextGray)
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "좌석 번호",
                        fontSize = 12.sp,
                        color = ColorTextGray
                    )
                    Text(
                        text = seatLabel,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = ColorTextBlack
                    )
                }
                
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "사용자",
                        fontSize = 12.sp,
                        color = ColorTextGray
                    )
                    Text(
                        text = userName,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = ColorTextBlack
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "남은 시간",
                        fontSize = 12.sp,
                        color = ColorTextGray
                    )
                    val formattedTime = leftTime?.let { seconds ->
                        val hours = seconds / 3600
                        val minutes = (seconds % 3600) / 60
                        "${hours}시간 ${minutes}분"
                    } ?: "-"
                    Text(
                        text = formattedTime,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = ColorPrimaryOrange
                    )
                }
            }
        }
    }
}

@Composable
private fun ZoomControls(
    scaleState: MutableState<Float>,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FloatingActionButton(
            onClick = {
                scaleState.value = (scaleState.value + 0.2f).coerceAtMost(3f)
            },
            modifier = Modifier.size(40.dp),
            containerColor = ColorWhite,
            contentColor = ColorTextBlack,
            shape = CircleShape
        ) {
            Icon(Icons.Default.Add, contentDescription = "확대")
        }

        FloatingActionButton(
            onClick = {
                scaleState.value = (scaleState.value - 0.2f).coerceAtLeast(0.5f)
            },
            modifier = Modifier.size(40.dp),
            containerColor = ColorWhite,
            contentColor = ColorTextBlack,
            shape = CircleShape
        ) {
            Icon(Icons.Default.Remove, contentDescription = "축소")
        }
    }
}

@Composable
private fun BoxScope.EditControlBar(
    selectedSeatId: MutableState<String?>,
    seats: MutableList<Seat>,
    isSaving: Boolean,
    addItemAtCenter: (String) -> Unit,
    onSave: () -> Unit
) {
    val selectedSeat = seats.find { it.id == selectedSeatId.value }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .align(Alignment.BottomCenter)
            .zIndex(1f)
            .shadow(16.dp, RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)),
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        color = ColorWhite
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                ToolCircleWithLabel(
                    symbolName = "add_box", // Material Symbol needed, or Icon
                    label = "좌석 추가"
                ) { addItemAtCenter("SEAT") }

                ToolCircleWithLabel(
                    symbolName = "check_box_outline_blank",
                    label = "벽 추가"
                ) { addItemAtCenter("WALL") }


                if (selectedSeat != null) {
                    ToolCircleWithLabel(
                        symbolName = "delete",
                        label = "삭제"
                    ) {
                        seats.remove(selectedSeat)
                        selectedSeatId.value = null
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = onSave,
                enabled = !isSaving,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = ColorPrimaryOrange,
                    disabledContainerColor = ColorPrimaryOrange.copy(alpha = 0.5f)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                if (isSaving) {
                    CircularProgressIndicator(
                        color = ColorWhite,
                        modifier = Modifier.size(24.dp)
                    )
                } else {
                    Text(
                        text = "변경사항 저장",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = ColorWhite
                    )
                }
            }
        }
    }
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
