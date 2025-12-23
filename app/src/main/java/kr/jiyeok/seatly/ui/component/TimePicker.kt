package kr.jiyeok.seatly.ui.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListItemInfo
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * TimePicker.kt
 *
 * Fix for off-by-one selection (visual center showed "10" but selection became "11"):
 * - More robust centered-index calculation:
 *   1) Prefer visibleItemsInfo method (computes each item center and picks the closest).
 *   2) Fallback uses precise firstVisibleItemIndex + firstVisibleItemScrollOffset-based formula:
 *      indexOffset = round((firstVisibleItemScrollOffset + viewportCenter) / itemHeight)
 *      centerIndex = firstVisibleIndex + indexOffset
 *   This formula avoids the previous half-item shift that produced off-by-one.
 *
 * - programmaticScroll guard remains (prevents races).
 * - After any programmatic snap we recompute the centered index via the robust method and set selected
 *   so the red band and selection are always identical.
 *
 * Usage: same as before.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimePicker(
    visible: Boolean,
    initial: String = "09:00",
    sheetHeight: Dp = 380.dp,
    title: String = "영업 시작 시간",
    onDismiss: () -> Unit,
    onConfirm: (selected: String) -> Unit
) {
    if (!visible) return

    val Primary = Color(0xFFe95321)
    val Surface = Color(0xFFFFFFFF)
    val centerBandColor = Color(0xFFF9E6E0)
    val TextMain = Color(0xFF1A1A1A)
    val TextSub = Color(0xFFBDBDBD)

    val coroutineScope = rememberCoroutineScope()
    val density = LocalDensity.current

    // Data
    val hours = remember { (0..23).map { String.format("%02d", it) } }
    val minutes = remember { (0..55 step 5).map { String.format("%02d", it) } }

    // parse initial "HH:MM"
    val (initH, initM) = remember(initial) {
        try {
            val parts = initial.split(":")
            val h = parts.getOrNull(0)?.toIntOrNull() ?: 9
            val m = parts.getOrNull(1)?.toIntOrNull() ?: 0
            val hStr = h.coerceIn(0, 23).let { String.format("%02d", it) }
            val nearestM = minutes.minByOrNull { abs(it.toInt() - m) } ?: "00"
            Pair(hStr, nearestM)
        } catch (_: Exception) {
            Pair("09", "00")
        }
    }

    var selectedHour by remember { mutableStateOf(initH) }
    var selectedMinute by remember { mutableStateOf(initM) }

    // Layout constants
    val listAreaHeight: Dp = 220.dp
    val itemHeight: Dp = 56.dp
    val verticalPadding = (listAreaHeight - itemHeight) / 2f

    // px conversions
    val listAreaHeightPx by remember { derivedStateOf { with(density) { listAreaHeight.toPx() } } }
    val itemHeightPx by remember { derivedStateOf { with(density) { itemHeight.toPx() } } }
    val viewportCenterPx by remember { derivedStateOf { listAreaHeightPx / 2f } }

    // LazyListStates
    val hourState = rememberLazyListState(hours.indexOf(selectedHour).coerceAtLeast(0))
    val minuteState = rememberLazyListState(minutes.indexOf(selectedMinute).coerceAtLeast(0))

    // programmatic scroll guard
    var programmaticScroll by remember { mutableStateOf(false) }

    // Compute closest-to-center using visibleItemsInfo when available
    fun computeCenteredIndexFromVisibleInfo(visible: List<LazyListItemInfo>, viewportCenter: Float): Int? {
        if (visible.isEmpty()) return null
        var bestIdx: Int? = null
        var bestDist = Float.MAX_VALUE
        for (info in visible) {
            val itemCenter = info.offset + info.size / 2f
            val dist = kotlin.math.abs(itemCenter - viewportCenter)
            if (dist < bestDist) {
                bestDist = dist
                bestIdx = info.index
            }
        }
        return bestIdx
    }

    // NEW: Fallback computation from firstVisible index + offset (precise, avoids half-item shift)
    fun computeCenteredIndexFromScroll(listState: LazyListState, viewportCenter: Float, itemPx: Float): Int {
        val firstIndex = listState.firstVisibleItemIndex
        val firstOffset = listState.firstVisibleItemScrollOffset.toFloat()
        // center position relative to first visible item's top:
        // centerRelativeToFirst = firstOffset + viewportCenter
        // indexOffsetFraction = centerRelativeToFirst / itemHeight
        // nearest index offset = round(indexOffsetFraction)
        val centerRelativeToFirst = firstOffset + viewportCenter
        val indexOffset = (centerRelativeToFirst / itemPx).roundToInt()
        val tentative = firstIndex + indexOffset
        val total = listState.layoutInfo.totalItemsCount
        return if (total > 0) tentative.coerceIn(0, total - 1) else tentative.coerceAtLeast(0)
    }

    // Robust getter: prefer visibleItemsInfo, fallback to offset-based
    fun getCenteredIndexRobust(listState: LazyListState): Int {
        val visible = listState.layoutInfo.visibleItemsInfo
        val byVisible = computeCenteredIndexFromVisibleInfo(visible, viewportCenterPx)
        return byVisible ?: computeCenteredIndexFromScroll(listState, viewportCenterPx, itemHeightPx)
    }

    // safe animate to index (no scrollOffset) with guard
    suspend fun LazyListState.safeAnimateToIndexWithGuard(index: Int) {
        val total = this.layoutInfo.totalItemsCount
        val safeIndex = if (total > 0) index.coerceIn(0, total - 1) else index.coerceAtLeast(0)
        programmaticScroll = true
        try {
            this.animateScrollToItem(safeIndex)
            // allow layout to settle
            delay(80)
        } catch (_: Throwable) {
            try {
                this.scrollToItem(safeIndex)
                delay(80)
            } catch (_: Throwable) {
                // ignore
            }
        } finally {
            // small buffer then re-enable handlers
            delay(40)
            programmaticScroll = false
        }
    }

    // Initial centering when sheet opens: guarded snap and then force selected to actual centered item
    LaunchedEffect(visible) {
        if (visible) {
            delay(30) // wait layout
            coroutineScope.launch {
                val hIndex = hours.indexOf(initH).coerceAtLeast(0)
                hourState.safeAnimateToIndexWithGuard(hIndex)
                // recompute robust centered index and set selected
                val centeredH = getCenteredIndexRobust(hourState)
                hours.getOrNull(centeredH)?.let { selectedHour = it }
            }
            coroutineScope.launch {
                val mIndex = minutes.indexOf(initM).coerceAtLeast(0)
                minuteState.safeAnimateToIndexWithGuard(mIndex)
                val centeredM = getCenteredIndexRobust(minuteState)
                minutes.getOrNull(centeredM)?.let { selectedMinute = it }
            }
        }
    }

    // Snap when user stops scrolling AND no programmatic scroll in progress
    LaunchedEffect(hourState) {
        snapshotFlow { hourState.isScrollInProgress }.collectLatest { inProgress ->
            if (!inProgress && !programmaticScroll) {
                val idx = getCenteredIndexRobust(hourState)
                coroutineScope.launch {
                    if (hourState.layoutInfo.totalItemsCount > 0) {
                        hourState.safeAnimateToIndexWithGuard(idx)
                        hours.getOrNull(idx)?.let { selectedHour = it }
                    }
                }
            }
        }
    }

    LaunchedEffect(minuteState) {
        snapshotFlow { minuteState.isScrollInProgress }.collectLatest { inProgress ->
            if (!inProgress && !programmaticScroll) {
                val idx = getCenteredIndexRobust(minuteState)
                coroutineScope.launch {
                    if (minuteState.layoutInfo.totalItemsCount > 0) {
                        minuteState.safeAnimateToIndexWithGuard(idx)
                        minutes.getOrNull(idx)?.let { selectedMinute = it }
                    }
                }
            }
        }
    }

    // UI
    ModalBottomSheet(
        onDismissRequest = { onDismiss() },
        containerColor = Surface,
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
        tonalElevation = 8.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .height(sheetHeight)
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Box(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = title,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 8.dp),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextMain
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Box(modifier = Modifier.fillMaxWidth()) {
                // center band behind
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(72.dp)
                        .align(Alignment.Center)
                        .clip(RoundedCornerShape(4.dp))
                        .background(centerBandColor)
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(listAreaHeight),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Hours wheel
                    LazyColumn(
                        state = hourState,
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                        contentPadding = PaddingValues(vertical = verticalPadding)
                    ) {
                        items(hours) { h ->
                            val isSelected = h == selectedHour
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(itemHeight)
                                    .clickable {
                                        // immediate highlight for UI responsiveness
                                        selectedHour = h
                                        // guard and animate to requested index, then recompute centered and reapply selected
                                        coroutineScope.launch {
                                            hourState.safeAnimateToIndexWithGuard(hours.indexOf(h))
                                            val centered = getCenteredIndexRobust(hourState)
                                            hours.getOrNull(centered)?.let { selectedHour = it }
                                        }
                                    }
                                    .padding(horizontal = 12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = h,
                                    fontSize = if (isSelected) 32.sp else 16.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) TextMain else TextSub,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }

                    // colon
                    Box(modifier = Modifier.width(28.dp), contentAlignment = Alignment.Center) {
                        Text(text = ":", fontSize = 24.sp, color = TextMain)
                    }

                    // Minutes wheel
                    LazyColumn(
                        state = minuteState,
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                        contentPadding = PaddingValues(vertical = verticalPadding)
                    ) {
                        items(minutes) { m ->
                            val isSelected = m == selectedMinute
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(itemHeight)
                                    .clickable {
                                        selectedMinute = m
                                        coroutineScope.launch {
                                            minuteState.safeAnimateToIndexWithGuard(minutes.indexOf(m))
                                            val centered = getCenteredIndexRobust(minuteState)
                                            minutes.getOrNull(centered)?.let { selectedMinute = it }
                                        }
                                    }
                                    .padding(horizontal = 12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = m,
                                    fontSize = if (isSelected) 32.sp else 16.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) TextMain else TextSub,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                OutlinedButton(
                    onClick = { onDismiss() },
                    border = BorderStroke(1.dp, Color(0xFFCCCCCC)),
                    modifier = Modifier.height(44.dp).widthIn(min = 100.dp)
                ) {
                    Text("취소", color = Color(0xFF333333))
                }

                Spacer(modifier = Modifier.width(12.dp))

                Button(
                    onClick = { onConfirm("${selectedHour}:${selectedMinute}") },
                    colors = ButtonDefaults.buttonColors(containerColor = Primary, contentColor = Color.White),
                    modifier = Modifier.height(44.dp).widthIn(min = 160.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(text = "선택 완료", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}