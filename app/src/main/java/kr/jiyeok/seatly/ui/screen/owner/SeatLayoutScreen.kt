package kr.jiyeok.seatly.ui.screen.owner

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.consumeAllChanges
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt
import kotlinx.coroutines.launch
import kr.jiyeok.seatly.ui.component.MaterialSymbol

// 공개 모델
enum class SeatType { NORMAL, WINDOW, PREMIUM }

data class Seat(
    val id: String,
    var label: String,
    val type: SeatType,
    val pos: MutableState<Offset>,      // content-space pixels (top-left)
    val size: MutableState<Offset>,     // width(x), height(y) in content-space pixels
    var rotation: Float = 0f
)

/* 색상/치수 */
private val Primary = Color(0xFFe95321)
private val SurfaceBorder = Color(0xFFE8E8E8)
private val GridLine = Color(0xFFF2F2F2)
private val TextMain = Color(0xFF1A1A1A)
private val TextSecondary = Color(0xFF888888)
private val PremiumBg = Color(0xFFFFF6F0)

private val GridCellDp = 40.dp
private val SeatDp = 40.dp
private val PremiumSeatDp = 52.dp

// 조정된 크기
private val ToolCircleSize = 40.dp
private val ZoomButtonSize = 40.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SeatLayoutScreen(
    onSave: (List<Seat>) -> Unit = {},
    onBack: () -> Unit = {}
) {
    val scope = rememberCoroutineScope()

    // transform state
    val scaleState = remember { mutableStateOf(1f) }
    val translateX = remember { mutableStateOf(0f) }
    val translateY = remember { mutableStateOf(0f) }
    val scaleAnimated by animateFloatAsState(scaleState.value)

    val density = LocalDensity.current
    val gridCellPx = with(density) { GridCellDp.toPx() }
    val normalSeatPx = with(density) { SeatDp.toPx() }
    val premiumSeatPx = with(density) { PremiumSeatDp.toPx() }

    // Initialize seats once (do NOT re-fill when user deletes all seats)
    val seats = remember {
        mutableStateListOf(
            Seat("A1","A1",SeatType.NORMAL, mutableStateOf(Offset(120f,140f)), mutableStateOf(Offset(normalSeatPx, normalSeatPx))),
            Seat("A2","A2",SeatType.NORMAL, mutableStateOf(Offset(180f,140f)), mutableStateOf(Offset(normalSeatPx, normalSeatPx))),
            Seat("A3","A3",SeatType.NORMAL, mutableStateOf(Offset(240f,140f)), mutableStateOf(Offset(normalSeatPx, normalSeatPx))),
            Seat("A4","A4",SeatType.NORMAL, mutableStateOf(Offset(420f,140f)), mutableStateOf(Offset(normalSeatPx, normalSeatPx))),
            Seat("A5","A5",SeatType.NORMAL, mutableStateOf(Offset(480f,140f)), mutableStateOf(Offset(normalSeatPx, normalSeatPx))),
            Seat("B1","B1",SeatType.NORMAL, mutableStateOf(Offset(120f,200f)), mutableStateOf(Offset(normalSeatPx, normalSeatPx))),
            Seat("B2","B2",SeatType.NORMAL, mutableStateOf(Offset(180f,200f)), mutableStateOf(Offset(normalSeatPx, normalSeatPx))),
            Seat("B3","B3",SeatType.NORMAL, mutableStateOf(Offset(240f,200f)), mutableStateOf(Offset(normalSeatPx, normalSeatPx))),
            Seat("B4","B4",SeatType.NORMAL, mutableStateOf(Offset(420f,200f)), mutableStateOf(Offset(normalSeatPx, normalSeatPx))),
            Seat("B5","B5",SeatType.NORMAL, mutableStateOf(Offset(480f,200f)), mutableStateOf(Offset(normalSeatPx, normalSeatPx))),
            Seat("C1","C1",SeatType.NORMAL, mutableStateOf(Offset(120f,260f)), mutableStateOf(Offset(normalSeatPx, normalSeatPx))),
            Seat("C2","C2",SeatType.NORMAL, mutableStateOf(Offset(180f,260f)), mutableStateOf(Offset(normalSeatPx, normalSeatPx))),
            Seat("C3","C3",SeatType.NORMAL, mutableStateOf(Offset(240f,260f)), mutableStateOf(Offset(normalSeatPx, normalSeatPx))),
            Seat("C4","C4",SeatType.NORMAL, mutableStateOf(Offset(420f,260f)), mutableStateOf(Offset(normalSeatPx, normalSeatPx))),
            Seat("C5","C5",SeatType.NORMAL, mutableStateOf(Offset(480f,260f)), mutableStateOf(Offset(normalSeatPx, normalSeatPx))),
            Seat("P1","P1",SeatType.PREMIUM, mutableStateOf(Offset(300f,460f)), mutableStateOf(Offset(premiumSeatPx, premiumSeatPx))),
            Seat("P2","P2",SeatType.PREMIUM, mutableStateOf(Offset(380f,460f)), mutableStateOf(Offset(premiumSeatPx, premiumSeatPx)))
        )
    }

    val selectedSeatId = remember { mutableStateOf<String?>(null) }
    val selectedTool = remember { mutableStateOf(SeatType.NORMAL) }

    // which seat is currently being resized (id) - 전역 상태로 둬서 클릭으로 중단 가능
    val resizingSeatId = remember { mutableStateOf<String?>(null) }

    // label editor state
    var showLabelEditor by remember { mutableStateOf(false) }
    var editingSeatId by remember { mutableStateOf<String?>(null) }
    var editingText by remember { mutableStateOf("") }

    // overlay sizes used for clamping
    val topBarHeight = 72.dp
    val summaryHeight = 56.dp
    val summarySpacing = 12.dp
    val totalTopOverlayDp = topBarHeight + summaryHeight + summarySpacing
    val topInsetPx = with(density) { totalTopOverlayDp.toPx() }

    // workspace size for placing new seats at viewport center
    val workspaceSize = remember { mutableStateOf(IntSize(0, 0)) }

    // helper to add seat at current viewport center (snapped to grid) and open label editor
    val addSeatAtCenter: (SeatType) -> Unit = { type ->
        val w = if (workspaceSize.value.width > 0) workspaceSize.value.width.toFloat() else 300f
        val h = if (workspaceSize.value.height > 0) workspaceSize.value.height.toFloat() else 300f
        val screenCenter = Offset(w / 2f, h / 2f)

        val contentX = (screenCenter.x - translateX.value) / scaleState.value
        val contentY = (screenCenter.y - translateY.value) / scaleState.value

        val g = gridCellPx / scaleState.value
        val snappedX = (contentX / g).roundToInt() * g
        val snappedY = (contentY / g).roundToInt() * g

        val countSameType = seats.count { it.type == type } + 1
        val prefix = when (type) {
            SeatType.NORMAL -> "N"
            SeatType.WINDOW -> "W"
            SeatType.PREMIUM -> "P"
        }
        val newId = "${prefix}${seats.size + 1}"
        val newLabel = when (type) {
            SeatType.NORMAL -> "일반${countSameType}"
            SeatType.WINDOW -> "창가${countSameType}"
            SeatType.PREMIUM -> "프리미엄${countSameType}"
        }

        val defaultSize = when (type) {
            SeatType.PREMIUM -> premiumSeatPx
            else -> normalSeatPx
        }

        val newSeat = Seat(newId, newLabel, type, mutableStateOf(Offset(snappedX, snappedY)), mutableStateOf(Offset(defaultSize, defaultSize)))
        seats.add(newSeat)
        selectedSeatId.value = newSeat.id
        selectedTool.value = type

        // open label editor
        editingSeatId = newSeat.id
        editingText = newLabel
        showLabelEditor = true
    }

    Surface(color = Color.White, modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.fillMaxSize()) {
            // -----------------------
            // Workspace (transformable). clamp translateY so content cannot move under overlay
            // -----------------------
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .onSizeChanged { workspaceSize.value = it }
                    // transform gestures
                    .pointerInput(Unit) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            val old = scaleState.value
                            val newScale = (old * zoom).coerceIn(0.6f, 3f)
                            scaleState.value = newScale

                            translateX.value += pan.x
                            translateY.value += pan.y

                            // clamp so content cannot be moved upwards into overlay area
                            if (translateY.value < -topInsetPx) translateY.value = -topInsetPx
                        }
                    }
                    // background tap: clear selection & stop resizing
                    .pointerInput(Unit) {
                        detectTapGestures { _ ->
                            selectedSeatId.value = null
                            resizingSeatId.value = null
                        }
                    }
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val gridPx = gridCellPx * scaleAnimated
                    val w = size.width
                    val h = size.height
                    val cols = (w / gridPx).toInt() + 2
                    val rows = (h / gridPx).toInt() + 2
                    for (i in 0..cols) {
                        val x = i * gridPx + (translateX.value % gridPx)
                        drawLine(color = GridLine, start = Offset(x, 0f), end = Offset(x, h), strokeWidth = 1f)
                    }
                    for (j in 0..rows) {
                        val y = j * gridPx + (translateY.value % gridPx)
                        drawLine(color = GridLine, start = Offset(0f, y), end = Offset(w, y), strokeWidth = 1f)
                    }
                }

                // seats rendering
                seats.forEach { seat ->
                    SeatItem(
                        seat = seat,
                        scale = scaleAnimated,
                        translate = Offset(translateX.value, translateY.value),
                        isSelected = selectedSeatId.value == seat.id,
                        onSelect = { id ->
                            selectedSeatId.value = id
                            // clicking a seat cancels any resizing in progress
                            resizingSeatId.value = null
                        },
                        onDuplicate = { src ->
                            val newId = "S${seats.size + 1}"
                            seats.add(Seat(newId, src.label + "_copy", src.type, mutableStateOf(src.pos.value + Offset(24f,24f)), mutableStateOf(Offset(src.size.value.x, src.size.value.y))))
                        },
                        onDelete = { tgt ->
                            val idx = seats.indexOfFirst { it.id == tgt.id }
                            if (idx >= 0) seats.removeAt(idx)
                            if (selectedSeatId.value == tgt.id) selectedSeatId.value = null
                        },
                        gridSizePxProvider = { gridCellPx },
                        onStartResize = { id -> resizingSeatId.value = id },
                        onEndResize = { resizingSeatId.value = null },
                        onEditRequested = { s ->
                            // open label editor on double-tap
                            selectedSeatId.value = s.id
                            resizingSeatId.value = null
                            editingSeatId = s.id
                            editingText = s.label
                            showLabelEditor = true
                        }
                    )
                }
            }

            // -----------------------
            // Overlay: TopBar + Summary — flush to the top (no extra top padding)
            // -----------------------
            Column(
                modifier = Modifier
                    .align(Alignment.TopCenter)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(topBarHeight)
                        .background(Color.White)
                ) {
                    Text(
                        text = "좌석 배치",
                        modifier = Modifier.align(Alignment.Center),
                        style = MaterialTheme.typography.titleLarge.copy(fontSize = 20.sp),
                        color = TextMain
                    )

                    IconButton(onClick = onBack, modifier = Modifier.align(Alignment.CenterStart).padding(start = 12.dp)) {
                        MaterialSymbol(name = "arrow_back_ios_new", size = 24.sp, tint = TextMain)
                    }

                    Button(
                        onClick = { onSave(seats.toList()) },
                        modifier = Modifier.align(Alignment.CenterEnd).padding(end = 12.dp).height(40.dp).width(84.dp).shadow(6.dp, RoundedCornerShape(10.dp)),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Primary)
                    ) {
                        Text("저장", color = Color.White)
                    }

                    Divider(color = SurfaceBorder, thickness = 1.dp, modifier = Modifier.align(Alignment.BottomStart).fillMaxWidth())
                }

                Spacer(modifier = Modifier.height(summarySpacing)) // additional gap between topbar and summary

                // dynamic counts computed from seats list
                val totalCount = seats.size
                val normalCount = seats.count { it.type == SeatType.NORMAL }
                val windowCount = seats.count { it.type == SeatType.WINDOW }
                val premiumCount = seats.count { it.type == SeatType.PREMIUM }

                Card(
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .fillMaxWidth()
                        .height(summaryHeight),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF9F9F9)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                ) {
                    Row(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("총 ${totalCount}석", fontWeight = FontWeight.Bold, color = TextMain)
                        // show only non-zero type counts, with dividers between visible items
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // build visible items
                            val items = listOfNotNull(
                                if (normalCount > 0) "일반석 $normalCount" else null,
                                if (windowCount > 0) "창가석 $windowCount" else null,
                                if (premiumCount > 0) "프리미엄 $premiumCount" else null
                            )

                            items.forEachIndexed { idx, text ->
                                Text(text, color = TextSecondary)
                                if (idx < items.lastIndex) {
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Divider(modifier = Modifier.height(18.dp).width(1.dp), color = SurfaceBorder)
                                    Spacer(modifier = Modifier.width(8.dp))
                                }
                            }
                        }
                    }
                }
            }

            // -----------------------
            // Zoom buttons (fixed overlay at bottom-right)
            // -----------------------
            Column(modifier = Modifier.align(Alignment.BottomEnd).padding(end = 12.dp, bottom = 220.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(modifier = Modifier.size(ZoomButtonSize).shadow(6.dp, CircleShape).clip(CircleShape).background(Color.White), contentAlignment = Alignment.Center) {
                    IconButton(onClick = {
                        scaleState.value = (scaleState.value + 0.18f).coerceAtMost(3f)
                        if (translateY.value < -topInsetPx) translateY.value = -topInsetPx
                    }) {
                        MaterialSymbol(name = "add", size = 18.sp, tint = TextMain)
                    }
                }
                Box(modifier = Modifier.size(ZoomButtonSize).shadow(6.dp, CircleShape).clip(CircleShape).background(Color.White), contentAlignment = Alignment.Center) {
                    IconButton(onClick = {
                        scaleState.value = (scaleState.value - 0.18f).coerceAtLeast(0.6f)
                        if (translateY.value < -topInsetPx) translateY.value = -topInsetPx
                    }) {
                        MaterialSymbol(name = "remove", size = 18.sp, tint = TextMain)
                    }
                }
            }

            // Bottom tool sheet
            Box(modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth()) {
                Card(
                    modifier = Modifier.fillMaxWidth().height(200.dp),
                    shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 10.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                ToolCircleWithLabel(symbolName = "event_seat", label = "일반석", selected = selectedTool.value == SeatType.NORMAL) {
                                    addSeatAtCenter(SeatType.NORMAL)
                                }
                                ToolCircleWithLabel(symbolName = "grid_view", label = "창가석", selected = selectedTool.value == SeatType.WINDOW) {
                                    addSeatAtCenter(SeatType.WINDOW)
                                }
                                ToolCircleWithLabel(symbolName = "diamond", label = "프리미엄", selected = selectedTool.value == SeatType.PREMIUM) {
                                    addSeatAtCenter(SeatType.PREMIUM)
                                }
                            }

                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Divider(modifier = Modifier.height(44.dp).width(1.dp), color = SurfaceBorder)
                                IconButton(onClick = { selectedSeatId.value?.let { id -> seats.find { it.id == id }?.let { it.rotation = (it.rotation + 90f) % 360f } } }) {
                                    MaterialSymbol(name = "rotate_right", size = 20.sp, tint = TextMain)
                                }
                                IconButton(onClick = {
                                    selectedSeatId.value?.let { id -> seats.find { it.id == id }?.let { src ->
                                        val newId = "S${seats.size + 1}"
                                        seats.add(Seat(newId, src.label + "_copy", src.type, mutableStateOf(src.pos.value + Offset(24f,24f)), mutableStateOf(Offset(src.size.value.x, src.size.value.y))))
                                    } }
                                }) {
                                    MaterialSymbol(name = "content_copy", size = 20.sp, tint = TextMain)
                                }
                                IconButton(onClick = {
                                    selectedSeatId.value?.let { id ->
                                        val idx = seats.indexOfFirst { s -> s.id == id }
                                        if (idx >= 0) seats.removeAt(idx)
                                        selectedSeatId.value = null
                                    }
                                }) {
                                    MaterialSymbol(name = "delete", size = 20.sp, tint = Color(0xFFEF4444))
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(onClick = { /* 완료 */ }, modifier = Modifier.fillMaxWidth().height(56.dp).shadow(12.dp, RoundedCornerShape(16.dp)), shape = RoundedCornerShape(16.dp), colors = ButtonDefaults.buttonColors(containerColor = Primary)) {
                            Text("완료", color = Color.White)
                        }
                    }
                }
            }

            // Label editor dialog
            if (showLabelEditor && editingSeatId != null) {
                AlertDialog(
                    onDismissRequest = { showLabelEditor = false },
                    confirmButton = {
                        TextButton(onClick = {
                            val id = editingSeatId
                            if (id != null) {
                                seats.find { it.id == id }?.let { it.label = editingText }
                            }
                            showLabelEditor = false
                            editingSeatId = null
                            editingText = ""
                        }) {
                            Text("저장")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = {
                            // 그냥 닫기 (기본 라벨 유지)
                            showLabelEditor = false
                            editingSeatId = null
                            editingText = ""
                        }) {
                            Text("취소")
                        }
                    },
                    title = { Text("좌석 라벨 편집") },
                    text = {
                        Column {
                            OutlinedTextField(
                                value = editingText,
                                onValueChange = { editingText = it },
                                singleLine = true,
                                label = { Text("라벨") },
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text("완료를 누르면 라벨이 저장됩니다.", color = TextSecondary, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                )
            }
        }
    }
}

/* SeatItem */
@Composable
private fun BoxScope.SeatItem(
    seat: Seat,
    scale: Float,
    translate: Offset,
    isSelected: Boolean,
    onSelect: (String?) -> Unit,
    onDuplicate: (Seat) -> Unit,
    onDelete: (Seat) -> Unit,
    gridSizePxProvider: () -> Float,
    onStartResize: (String) -> Unit,
    onEndResize: () -> Unit,
    onEditRequested: (Seat) -> Unit
) {
    val density = LocalDensity.current
    val gPx = gridSizePxProvider()

    // drag offset while moving (content-space)
    val dragOffset = remember { mutableStateOf(Offset.Zero) }

    val screenX = (seat.pos.value.x + dragOffset.value.x) * scale + translate.x
    val screenY = (seat.pos.value.y + dragOffset.value.y) * scale + translate.y

    // seat size in content-space px
    val widthPx = seat.size.value.x
    val heightPx = seat.size.value.y

    // convert to Dp for Modifier.size
    val widthDp = with(density) { (widthPx).toDp() }
    val heightDp = with(density) { (heightPx).toDp() }

    val minSizePx = gPx * 0.5f

    Box(
        modifier = Modifier
            .size(widthDp, heightDp)
            .offset { IntOffset(screenX.roundToInt(), screenY.roundToInt()) }
            .pointerInput(seat.id) {
                detectDragGestures(
                    onDragStart = { onSelect(seat.id) },
                    onDrag = { change, dragAmount ->
                        change.consumeAllChanges()
                        val dx = dragAmount.x / scale
                        val dy = dragAmount.y / scale
                        dragOffset.value = dragOffset.value + Offset(dx, dy)
                    },
                    onDragEnd = {
                        // apply final pos (snap to grid)
                        val newPos = Offset(seat.pos.value.x + dragOffset.value.x, seat.pos.value.y + dragOffset.value.y)
                        val g = gPx / scale
                        val snappedX = (newPos.x / g).roundToInt() * g
                        val snappedY = (newPos.y / g).roundToInt() * g
                        seat.pos.value = Offset(snappedX, snappedY)
                        dragOffset.value = Offset.Zero
                    },
                    onDragCancel = { dragOffset.value = Offset.Zero }
                )
            }
    ) {
        // Card: use pointerInput detectTapGestures to support single-tap and double-tap
        Card(
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxSize()
                .pointerInput("${seat.id}_tap") {
                    detectTapGestures(
                        onTap = {
                            onSelect(seat.id)
                            onEndResize() // cancel any resizing
                        },
                        onDoubleTap = {
                            onSelect(seat.id)
                            onEndResize() // stop resizing if any
                            onEditRequested(seat)
                        }
                    )
                }
                .shadow(if (isSelected) 8.dp else 2.dp, RoundedCornerShape(12.dp))
                .border(if (isSelected) 3.dp else 1.dp, if (isSelected) Primary else SurfaceBorder, RoundedCornerShape(12.dp)),
            colors = CardDefaults.cardColors(containerColor = if (seat.type == SeatType.PREMIUM) PremiumBg else Color.White)
        ) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                if (seat.type == SeatType.PREMIUM) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        MaterialSymbol(name = "chair", size = 14.sp, tint = Primary)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(seat.label, color = Primary, fontWeight = FontWeight.Medium)
                    }
                } else {
                    Text(seat.label, color = TextMain, fontWeight = FontWeight.Medium)
                }
            }
        }

        if (isSelected) {
            // selection dot
            Box(modifier = Modifier.align(Alignment.TopEnd).offset(x = 6.dp, y = (-6).dp).size(10.dp).clip(CircleShape).background(Primary))

            // resize handles size
            val handleDp = 12.dp
            val handleHalfDp = 6.dp

            // helper for updating size while dragging
            fun updateSizeAndPos(deltaX: Float, deltaY: Float, anchor: String) {
                // deltaX/deltaY are in content-space (already divided by scale by callers)
                var newW = seat.size.value.x
                var newH = seat.size.value.y
                var newPos = seat.pos.value

                when (anchor) {
                    "NW" -> {
                        newW = (seat.size.value.x - deltaX).coerceAtLeast(minSizePx)
                        newH = (seat.size.value.y - deltaY).coerceAtLeast(minSizePx)
                        newPos = Offset((seat.pos.value.x + deltaX).coerceAtLeast(0f), (seat.pos.value.y + deltaY).coerceAtLeast(0f))
                    }
                    "N" -> {
                        newH = (seat.size.value.y - deltaY).coerceAtLeast(minSizePx)
                        newPos = Offset(seat.pos.value.x, (seat.pos.value.y + deltaY).coerceAtLeast(0f))
                    }
                    "NE" -> {
                        newW = (seat.size.value.x + deltaX).coerceAtLeast(minSizePx)
                        newH = (seat.size.value.y - deltaY).coerceAtLeast(minSizePx)
                        newPos = Offset(seat.pos.value.x, (seat.pos.value.y + deltaY).coerceAtLeast(0f))
                    }
                    "W" -> {
                        newW = (seat.size.value.x - deltaX).coerceAtLeast(minSizePx)
                        newPos = Offset((seat.pos.value.x + deltaX).coerceAtLeast(0f), seat.pos.value.y)
                    }
                    "E" -> {
                        newW = (seat.size.value.x + deltaX).coerceAtLeast(minSizePx)
                    }
                    "SW" -> {
                        newW = (seat.size.value.x - deltaX).coerceAtLeast(minSizePx)
                        newH = (seat.size.value.y + deltaY).coerceAtLeast(minSizePx)
                        newPos = Offset((seat.pos.value.x + deltaX).coerceAtLeast(0f), seat.pos.value.y)
                    }
                    "S" -> {
                        newH = (seat.size.value.y + deltaY).coerceAtLeast(minSizePx)
                    }
                    "SE" -> {
                        newW = (seat.size.value.x + deltaX).coerceAtLeast(minSizePx)
                        newH = (seat.size.value.y + deltaY).coerceAtLeast(minSizePx)
                    }
                }

                seat.size.value = Offset(newW, newH)
                seat.pos.value = newPos
            }

            // create handle composable to avoid duplication
            @Composable
            fun ResizeHandle(anchor: Alignment, name: String, offsetX: Dp = 0.dp, offsetY: Dp = 0.dp) {
                Box(
                    modifier = Modifier
                        .size(handleDp)
                        .align(anchor)
                        .offset(x = offsetX, y = offsetY)
                        .pointerInput("${seat.id}_handle_$name") {
                            detectDragGestures(
                                onDragStart = {
                                    // start resizing
                                    onStartResize(seat.id)
                                },
                                onDrag = { change, dragAmount ->
                                    change.consumeAllChanges()
                                    val dx = dragAmount.x / scale
                                    val dy = dragAmount.y / scale
                                    // call updater with content-space deltas
                                    updateSizeAndPos(dx, dy, name)
                                },
                                onDragEnd = {
                                    // snap size and position to grid
                                    val g = gPx / scale
                                    val roundedW = (seat.size.value.x / g).roundToInt().coerceAtLeast(1) * g
                                    val roundedH = (seat.size.value.y / g).roundToInt().coerceAtLeast(1) * g
                                    // For position, snap top-left to grid
                                    val snappedX = (seat.pos.value.x / g).roundToInt() * g
                                    val snappedY = (seat.pos.value.y / g).roundToInt() * g
                                    seat.size.value = Offset(roundedW.coerceAtLeast(minSizePx), roundedH.coerceAtLeast(minSizePx))
                                    seat.pos.value = Offset(snappedX.coerceAtLeast(0f), snappedY.coerceAtLeast(0f))
                                    // end resizing
                                    onEndResize()
                                },
                                onDragCancel = {
                                    onEndResize()
                                }
                            )
                        }
                        .clip(CircleShape)
                        .background(Color.White)
                        .border(1.dp, Primary, CircleShape)
                )
            }

            // corners
            ResizeHandle(Alignment.TopStart, "NW", offsetX = (-handleHalfDp), offsetY = (-handleHalfDp))
            ResizeHandle(Alignment.TopEnd, "NE", offsetX = (handleHalfDp), offsetY = (-handleHalfDp))
            ResizeHandle(Alignment.BottomStart, "SW", offsetX = (-handleHalfDp), offsetY = (handleHalfDp))
            ResizeHandle(Alignment.BottomEnd, "SE", offsetX = (handleHalfDp), offsetY = (handleHalfDp))

            // edges
            ResizeHandle(Alignment.TopCenter, "N", offsetY = (-handleHalfDp))
            ResizeHandle(Alignment.CenterStart, "W", offsetX = (-handleHalfDp))
            ResizeHandle(Alignment.CenterEnd, "E", offsetX = (handleHalfDp))
            ResizeHandle(Alignment.BottomCenter, "S", offsetY = (handleHalfDp))
        }
    }
}

/* Tool circle: smaller + label below */
@Composable
private fun ToolCircleWithLabel(symbolName: String, label: String, selected: Boolean, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(64.dp)) {
        Card(
            shape = CircleShape,
            modifier = Modifier.size(ToolCircleSize).clickable { onClick() },
            elevation = CardDefaults.cardElevation(defaultElevation = if (selected) 6.dp else 2.dp)
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.background(if (selected) Primary.copy(alpha = 0.06f) else Color.White).fillMaxSize()) {
                MaterialSymbol(name = symbolName, size = 16.sp, tint = if (selected) Primary else TextMain)
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(label, color = TextSecondary, fontSize = 11.sp)
    }
}