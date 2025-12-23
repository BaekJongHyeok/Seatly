package kr.jiyeok.seatly.ui.screen.owner

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.navigation.NavController
import kotlin.math.roundToInt
import kr.jiyeok.seatly.ui.component.MaterialSymbol
import java.time.format.DateTimeFormatter
import java.time.LocalTime

/**
 * CurrentSeatScreen — uses ActivitiesRepo.activities as the shared source for recent activities.
 *
 * Now accepts navController so it can navigate to the RecentActivitiesScreen when "더보기" is tapped.
 */

private val Primary = Color(0xFFFF6633)
private val SurfaceCard = Color(0xFFF8F8F8)
private val TextMain = Color(0xFF1A1A1A)
private val TextSecondary = Color(0xFF888888)
private val BorderSubtle = Color(0xFFE5E7EB)

enum class SeatState { AVAILABLE, OCCUPIED, RESERVED, MAINTENANCE, DISABLED }

// Observable seat model so state changes recompose UI
class CurrentSeat(
    val id: String,
    labelInit: String,
    stateInit: SeatState
) {
    var label by mutableStateOf(labelInit)
    var state by mutableStateOf(stateInit)
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CurrentSeatScreen(
    navController: NavController,
    modifier: Modifier = Modifier,
    onBack: () -> Unit = {}
) {
    val density = LocalDensity.current
    val cellDp = 64.dp
    val cellPx = with(density) { cellDp.toPx() }

    // Seats list (observable)
    val seats = remember {
        mutableStateListOf(
            CurrentSeat("A1", "A1", SeatState.OCCUPIED),
            CurrentSeat("A2", "A2", SeatState.AVAILABLE),
            CurrentSeat("A3", "A3", SeatState.RESERVED),
            CurrentSeat("A4", "A4", SeatState.AVAILABLE),
            CurrentSeat("A5", "A5", SeatState.MAINTENANCE),

            CurrentSeat("B1", "B1", SeatState.AVAILABLE),
            CurrentSeat("B2", "B2", SeatState.OCCUPIED),
            CurrentSeat("B3", "B3", SeatState.OCCUPIED),
            CurrentSeat("B4", "B4", SeatState.AVAILABLE),
            CurrentSeat("B5", "B5", SeatState.AVAILABLE),

            CurrentSeat("C1", "C1", SeatState.DISABLED),
            CurrentSeat("C2", "C2", SeatState.DISABLED),
            CurrentSeat("C3", "C3", SeatState.AVAILABLE),
            CurrentSeat("C4", "C4", SeatState.AVAILABLE),
            CurrentSeat("C5", "C5", SeatState.RESERVED),

            CurrentSeat("D1", "D1", SeatState.OCCUPIED),
            CurrentSeat("D2", "D2", SeatState.OCCUPIED),
            CurrentSeat("D3", "D3", SeatState.OCCUPIED),
            CurrentSeat("D4", "D4", SeatState.AVAILABLE),
            CurrentSeat("D5", "D5", SeatState.AVAILABLE),

            CurrentSeat("E1", "E1", SeatState.AVAILABLE),
            CurrentSeat("E2", "E2", SeatState.AVAILABLE),
            CurrentSeat("E3", "E3", SeatState.AVAILABLE),
            CurrentSeat("E4", "E4", SeatState.OCCUPIED),
            CurrentSeat("E5", "E5", SeatState.OCCUPIED)
        )
    }

    // Use shared ActivitiesRepo
    val activities = ActivitiesRepo.activities

    // UI state
    var selectedFilter by remember { mutableStateOf("all") } // all,reserved,occupied,maintenance
    var selectedSeatId by remember { mutableStateOf<String?>(null) }
    var showLabelEditor by remember { mutableStateOf(false) }
    var editingText by remember { mutableStateOf("") }
    var editingSeatRef by remember { mutableStateOf<CurrentSeat?>(null) }

    var showDetailsDialog by remember { mutableStateOf(false) }
    var detailsSeatRef by remember { mutableStateOf<CurrentSeat?>(null) }

    // zoom
    val scale = remember { mutableStateOf(1f) }
    val scaleAnimated by animateFloatAsState(scale.value)

    // derived counts (reactive)
    val totalCount by derivedStateOf { seats.size }
    val occupiedCount by derivedStateOf { seats.count { it.state == SeatState.OCCUPIED } }
    val availableCount by derivedStateOf { seats.count { it.state == SeatState.AVAILABLE } }
    val usageRate by derivedStateOf { if (totalCount == 0) 0 else ((occupiedCount.toFloat() / totalCount.toFloat()) * 100f).roundToInt() }

    val outerScroll = rememberScrollState()

    Surface(modifier = modifier.fillMaxSize(), color = Color.White) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.fillMaxSize().verticalScroll(outerScroll)) {
                // Top bar (no date)
                Box(modifier = Modifier.fillMaxWidth().height(72.dp).padding(horizontal = 16.dp), contentAlignment = Alignment.CenterStart) {
                    IconButton(onClick = onBack) { MaterialSymbol(name = "arrow_back", size = 24.sp, tint = TextMain) }
                    Text("좌석 사용 현황", fontSize = 20.sp, fontWeight = FontWeight.SemiBold, color = TextMain, modifier = Modifier.padding(start = 56.dp))
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Filter pills
                Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    FilterPill("전체", selectedFilter == "all") { selectedFilter = "all"; selectedSeatId = null }
                    FilterPill("예약", selectedFilter == "reserved") { selectedFilter = "reserved"; selectedSeatId = null }
                    FilterPill("사용중", selectedFilter == "occupied") { selectedFilter = "occupied"; selectedSeatId = null }
                    FilterPill("정비중", selectedFilter == "maintenance") { selectedFilter = "maintenance"; selectedSeatId = null }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Summary row (counts update automatically)
                Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    SummaryCard("전체", "${totalCount}석", emphasize = false)
                    SummaryCard("사용중", "${occupiedCount}석", emphasize = true)
                    SummaryCard("가능", "${availableCount}석", emphasize = false)
                    SummaryCard("사용률", "${usageRate}%", emphasize = true)
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Seat section (anchored zoom inside)
                val seatSectionHeight = 460.dp
                Box(modifier = Modifier.padding(horizontal = 16.dp).height(seatSectionHeight).fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Color.White).border(1.dp, BorderSubtle, RoundedCornerShape(18.dp)).padding(18.dp)) {
                    // grid background
                    Canvas(modifier = Modifier.matchParentSize()) {
                        val step = cellPx
                        val w = size.width
                        val h = size.height
                        val color = Color(0x0F000000)
                        for (x in 0..(w / step).toInt() + 1) {
                            drawLine(color = color, start = androidx.compose.ui.geometry.Offset(x * step, 0f), end = androidx.compose.ui.geometry.Offset(x * step, h), strokeWidth = 1f)
                        }
                        for (y in 0..(h / step).toInt() + 1) {
                            drawLine(color = color, start = androidx.compose.ui.geometry.Offset(0f, y * step), end = androidx.compose.ui.geometry.Offset(w, y * step), strokeWidth = 1f)
                        }
                    }

                    // seat grid scaled
                    Box(modifier = Modifier.align(Alignment.TopStart).graphicsLayer { scaleX = scaleAnimated; scaleY = scaleAnimated }) {
                        LazyVerticalGrid(columns = GridCells.Fixed(5), modifier = Modifier.wrapContentWidth().wrapContentHeight(), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            items(seats, key = { it.id }) { seat ->
                                val visible = when (selectedFilter) {
                                    "all" -> true
                                    "reserved" -> seat.state == SeatState.RESERVED
                                    "occupied" -> seat.state == SeatState.OCCUPIED
                                    "maintenance" -> seat.state == SeatState.MAINTENANCE
                                    else -> true
                                }
                                val alpha = if (visible) 1f else 0.18f

                                SeatTile(
                                    seat = seat,
                                    size = cellDp,
                                    alpha = alpha,
                                    isSelected = selectedSeatId == seat.id,
                                    enabled = visible,
                                    onTap = { if (visible) selectedSeatId = seat.id },
                                    onDoubleTap = {
                                        if (visible) {
                                            editingSeatRef = seat
                                            editingText = seat.label
                                            showLabelEditor = true
                                        }
                                    },
                                    onChangeState = { newState ->
                                        val prev = seat.state
                                        if (prev != newState) {
                                            seat.state = newState
                                            ActivitiesRepo.activities.add(0, ActivityItem(text = when (newState) {
                                                SeatState.OCCUPIED -> "${seat.label} 입실 완료"
                                                SeatState.AVAILABLE -> "${seat.label} 사용 가능"
                                                SeatState.RESERVED -> "${seat.label} 예약됨"
                                                SeatState.MAINTENANCE -> "${seat.label} 정비중"
                                                SeatState.DISABLED -> "${seat.label} 사용 불가"
                                            }, time = nowTime()))
                                        }
                                    }
                                )
                            }
                        }
                    }

                    // Quick actions overlay inside section (center bottom)
                    val selSeat = seats.find { it.id == selectedSeatId }
                    if (selSeat != null) {
                        Card(modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 12.dp).fillMaxWidth(0.80f), shape = RoundedCornerShape(12.dp), elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                                    Column {
                                        Text("${selSeat.label}", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = TextMain)
                                        Text("상태: ${selSeat.state.name}", fontSize = 12.sp, color = TextSecondary)
                                    }
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        TextButton(onClick = {
                                            detailsSeatRef = selSeat
                                            showDetailsDialog = true
                                        }) { Text("상세") }
                                        TextButton(onClick = { selectedSeatId = null }) { Text("닫기") }
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    ActionSmall("입실") {
                                        selSeat.state = SeatState.OCCUPIED
                                        ActivitiesRepo.activities.add(0, ActivityItem(text = "${selSeat.label} 입실 완료", time = nowTime()))
                                    }
                                    ActionSmall("해제") {
                                        selSeat.state = SeatState.AVAILABLE
                                        ActivitiesRepo.activities.add(0, ActivityItem(text = "${selSeat.label} 사용 가능", time = nowTime()))
                                    }
                                    ActionSmall("예약") {
                                        selSeat.state = SeatState.RESERVED
                                        ActivitiesRepo.activities.add(0, ActivityItem(text = "${selSeat.label} 예약됨", time = nowTime()))
                                    }
                                    ActionSmall("정비") {
                                        selSeat.state = SeatState.MAINTENANCE
                                        ActivitiesRepo.activities.add(0, ActivityItem(text = "${selSeat.label} 정비중", time = nowTime()))
                                    }
                                    ActionSmall("편집") {
                                        editingSeatRef = selSeat; editingText = selSeat.label; showLabelEditor = true
                                    }
                                    ActionSmall("메모") {}
                                    ActionSmall("차단") {
                                        selSeat.state = SeatState.DISABLED
                                        ActivitiesRepo.activities.add(0, ActivityItem(text = "${selSeat.label} 사용 불가", time = nowTime()))
                                    }
                                }
                            }
                        }
                    }

                    // Zoom controls anchored to seat section bottom-right
                    Column(modifier = Modifier.align(Alignment.BottomEnd).padding(end = 6.dp, bottom = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        FloatingActionButton(onClick = { scale.value = (scale.value + 0.12f).coerceAtMost(2.0f) }, containerColor = Color.White, modifier = Modifier.size(44.dp)) {
                            MaterialSymbol(name = "add", size = 18.sp, tint = TextMain)
                        }
                        FloatingActionButton(onClick = { scale.value = (scale.value - 0.12f).coerceAtLeast(0.6f) }, containerColor = Color.White, modifier = Modifier.size(44.dp)) {
                            MaterialSymbol(name = "remove", size = 18.sp, tint = TextMain)
                        }
                    }
                } // seat section

                Spacer(modifier = Modifier.height(16.dp))

                // Recent activity header + preview (top 3)
                Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("최근 활동", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TextMain)
                        TextButton(onClick = {
                            // navigate to full activity list screen
                            navController.navigate("recent_activities")
                        }) { Text("더보기", color = Primary) }
                    }
                    Spacer(modifier = Modifier.height(8.dp))

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        // Show up to 3 items from shared repo
                        ActivitiesRepo.activities.take(3).forEach { act ->
                            ActivityRow(text = act.text, time = act.time)
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Button(onClick = { /* navigate to editor */ }, modifier = Modifier.weight(1f).height(52.dp), colors = ButtonDefaults.buttonColors(containerColor = Primary), shape = RoundedCornerShape(16.dp)) {
                            Text("좌석 관리", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                        androidx.compose.material3.OutlinedButton(onClick = { /* reservations */ }, modifier = Modifier.weight(1f).height(52.dp), shape = RoundedCornerShape(16.dp)) {
                            Text("예약 현황", color = TextMain)
                        }
                    }

                    Spacer(modifier = Modifier.height(32.dp))
                }
            } // Column scrollable
        } // Box
    } // Surface

    // Details dialog
    if (showDetailsDialog && detailsSeatRef != null) {
        val seat = detailsSeatRef!!
        AlertDialog(
            onDismissRequest = { showDetailsDialog = false; detailsSeatRef = null },
            confirmButton = {
                TextButton(onClick = {
                    seat.state = if (seat.state == SeatState.OCCUPIED) SeatState.AVAILABLE else SeatState.OCCUPIED
                    ActivitiesRepo.activities.add(0, ActivityItem(text = when (seat.state) {
                        SeatState.OCCUPIED -> "${seat.label} 입실 완료"
                        SeatState.AVAILABLE -> "${seat.label} 사용 가능"
                        else -> "${seat.label} 상태 변경"
                    }, time = nowTime()))
                    showDetailsDialog = false
                    detailsSeatRef = null
                    selectedSeatId = seat.id
                }) { Text(if (seat.state == SeatState.OCCUPIED) "해제(사용 가능)" else "입실 처리") }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = {
                        editingSeatRef = seat
                        editingText = seat.label
                        showLabelEditor = true
                        showDetailsDialog = false
                        detailsSeatRef = null
                    }) { Text("편집") }
                    TextButton(onClick = { showDetailsDialog = false; detailsSeatRef = null }) { Text("닫기") }
                }
            },
            title = { Text("좌석 상세: ${seat.label}") },
            text = {
                Column {
                    Text("아이디: ${seat.id}", color = TextSecondary)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("상태: ${seat.state.name}", color = TextSecondary)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("추가 정보 또는 통계 등을 여기에 표시할 수 있습니다.", color = TextSecondary, fontSize = 12.sp)
                }
            }
        )
    }

    // Label editor dialog
    if (showLabelEditor && editingSeatRef != null) {
        val seat = editingSeatRef!!
        AlertDialog(
            onDismissRequest = { showLabelEditor = false; editingSeatRef = null },
            confirmButton = {
                TextButton(onClick = {
                    seat.label = editingText
                    ActivitiesRepo.activities.add(0, ActivityItem(text = "${seat.label} 라벨 변경", time = nowTime()))
                    showLabelEditor = false
                    editingSeatRef = null
                }) { Text("저장") }
            },
            dismissButton = { TextButton(onClick = { showLabelEditor = false; editingSeatRef = null }) { Text("취소") } },
            title = { Text("좌석 라벨 편집") },
            text = {
                Column {
                    OutlinedTextField(value = editingText, onValueChange = { editingText = it }, singleLine = true, label = { Text("라벨") }, modifier = Modifier.fillMaxWidth())
                }
            }
        )
    }
}

/* ---------- Helpers (unchanged except using ActivitiesRepo where appropriate) ---------- */

@Composable
private fun FilterPill(text: String, active: Boolean, onClick: () -> Unit) {
    val bg = if (active) Primary else SurfaceCard
    val contentColor = if (active) Color.White else TextMain
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(bg)
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = contentColor, fontSize = 13.sp, fontWeight = if (active) FontWeight.Bold else FontWeight.Medium)
    }
}

@Composable
private fun SummaryCard(title: String, value: String, emphasize: Boolean) {
    val borderColor = if (emphasize) Primary.copy(alpha = 0.14f) else BorderSubtle
    Column(
        modifier = Modifier
            .height(72.dp)
            .widthIn(min = 110.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFFF9F9F9))
            .border(1.dp, borderColor, RoundedCornerShape(14.dp))
            .padding(horizontal = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        val textColor = if (emphasize) Primary else TextMain
        Text(value, fontWeight = FontWeight.Bold, fontSize = 20.sp, color = textColor)
        Spacer(modifier = Modifier.height(4.dp))
        Text(title, fontSize = 12.sp, color = TextSecondary)
    }
}

@Composable
private fun SeatTile(
    seat: CurrentSeat,
    size: Dp,
    alpha: Float = 1f,
    isSelected: Boolean,
    enabled: Boolean,
    onTap: () -> Unit,
    onDoubleTap: () -> Unit,
    onChangeState: (SeatState) -> Unit
) {
    val bg = when (seat.state) {
        SeatState.AVAILABLE -> Color.White
        SeatState.OCCUPIED -> Primary.copy(alpha = 0.06f)
        SeatState.RESERVED -> Color.White
        SeatState.MAINTENANCE -> Color(0xFFF7F7F7)
        SeatState.DISABLED -> Color(0xFFF3F4F6)
    }
    val borderColor = when (seat.state) {
        SeatState.OCCUPIED -> Primary.copy(alpha = 0.14f)
        SeatState.MAINTENANCE -> BorderSubtle
        SeatState.DISABLED -> Color(0xFFF3F4F6)
        else -> BorderSubtle
    }
    val selectionBorder = if (isSelected) Primary else borderColor

    var mod = Modifier
        .size(size)
        .graphicsLayer { this.alpha = alpha }
        .clip(RoundedCornerShape(10.dp))
        .background(bg)
        .border(1.dp, selectionBorder, RoundedCornerShape(10.dp))

    if (enabled) {
        mod = mod.pointerInput(seat.id) {
            detectTapGestures(onTap = { onTap() }, onDoubleTap = { onDoubleTap() })
        }
    }

    Box(modifier = mod, contentAlignment = Alignment.Center) {
        Box(modifier = Modifier.align(Alignment.TopStart).padding(start = 6.dp, top = 6.dp)) {
            Text(seat.label, fontSize = 11.sp, color = if (seat.state == SeatState.DISABLED) Color(0xFF9CA3AF) else TextMain)
        }

        when (seat.state) {
            SeatState.OCCUPIED -> MaterialSymbol(name = "person", size = 18.sp, tint = Primary)
            SeatState.RESERVED -> MaterialSymbol(name = "calendar_today", size = 16.sp, tint = TextMain)
            SeatState.MAINTENANCE -> MaterialSymbol(name = "build", size = 16.sp, tint = Color(0xFF9CA3AF))
            else -> { /* empty */ }
        }
    }
}

@Composable
private fun ActionSmall(text: String, onClick: () -> Unit) {
    Button(onClick = onClick, modifier = Modifier.height(40.dp), colors = ButtonDefaults.buttonColors(containerColor = SurfaceCard), shape = RoundedCornerShape(10.dp)) {
        Text(text, color = TextMain)
    }
}

@Composable
internal fun ActivityRow(text: String, time: String) {
    Card(shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = Color.White), modifier = Modifier.fillMaxWidth().border(1.dp, BorderSubtle, RoundedCornerShape(12.dp))) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(40.dp).clip(RoundedCornerShape(999.dp)).background(Primary.copy(alpha = 0.08f)), contentAlignment = Alignment.Center) {
                MaterialSymbol(name = "check_circle", size = 16.sp, tint = Primary)
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text, fontWeight = FontWeight.Medium, fontSize = 14.sp, color = TextMain)
                Spacer(modifier = Modifier.height(6.dp))
                Text(time, fontSize = 12.sp, color = TextSecondary)
            }
        }
    }
}

/* utilities */
private fun nowTime(offsetMinutes: Long = 0): String {
    val t = LocalTime.now().plusMinutes(offsetMinutes)
    val fmt = DateTimeFormatter.ofPattern("HH:mm")
    return t.format(fmt)
}