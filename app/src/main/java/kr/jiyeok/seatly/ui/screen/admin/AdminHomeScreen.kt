package kr.jiyeok.seatly.ui.screen.admin

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chair
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kr.jiyeok.seatly.R
import kr.jiyeok.seatly.ui.screen.admin.cafe.InMemoryCafeStore
import kr.jiyeok.seatly.ui.screen.admin.cafe.StudyCafeListItem

private val Primary = Color(0xFFe95321)
private val BackgroundBase = Color(0xFFFFFFFF)
private val SurfaceCard = Color(0xFFF9F9F9)
private val TextMain = Color(0xFF1A1A1A)
private val TextSub = Color(0xFF888888)
private val BorderColor = Color(0xFFE5E5E5)
private val Negative = Color(0xFFef4444)

/** Small models for the screen-local repository */
data class ReservationItem(
    val id: String,
    val name: String,
    val seatCode: String,
    val timeRange: String,
    val priceText: String,
    val agoText: String,
    val avatarBg: Color
)

data class OwnerMetrics(
    val totalSeats: Int,
    val usedSeats: Int,
    val todayVisitors: String,
    val cancelRate: String
)

/** Repository abstraction for OwnerHomeScreen data.
 *  Default mock implementation below reads cafes from InMemoryCafeStore (shared with registration flow)
 *  so that new mock-registered cafes appear here automatically.
 */
interface AdminHomeRepository {
    suspend fun getCafes(): List<StudyCafeListItem>
    suspend fun getReservations(): List<ReservationItem>
    suspend fun getMetrics(): OwnerMetrics
}

class MockAdminHomeRepository : AdminHomeRepository {
    override suspend fun getCafes(): List<StudyCafeListItem> {
        delay(200)
        return InMemoryCafeStore.getAll()
    }

    override suspend fun getReservations(): List<ReservationItem> {
        delay(150)
        return listOf(
            ReservationItem("1", "김준호", "A3", "14:00-18:00", "₩18,000", "5분 전", Primary),
            ReservationItem("2", "이서연", "B12", "19:00-22:00", "₩12,000", "12분 전", Color.White),
            ReservationItem("3", "Park Min", "S1", "09:00-12:00", "₩9,000", "24분 전", Color.White),
            ReservationItem("4", "최현수", "C4", "10:00-14:00", "₩15,000", "40분 전", Color.White)
        )
    }

    override suspend fun getMetrics(): OwnerMetrics {
        delay(120)
        // mock numbers used in the provided screen: total 60, used 24, available 36
        return OwnerMetrics(totalSeats = 60, usedSeats = 24, todayVisitors = "48명", cancelRate = "2.3%")
    }
}

/**
 * OwnerHomeScreen
 *
 * - Shows an "Operating Cafe" card + seat status card when there is at least one registered cafe.
 * - If single cafe -> show single card. If multiple -> show selectable chips to choose active cafe.
 * - Reads cafes from the injected repository (default: MockOwnerHomeRepository which reads from InMemoryCafeStore).
 * - Other sections (metrics, recent reservations, action pills, bottom nav) follow the design from the screenshot/code.html.
 */
@Composable
fun AdminHomeScreen(
    navController: NavController,
    repository: AdminHomeRepository = MockAdminHomeRepository()
) {
    var cafes by remember { mutableStateOf<List<StudyCafeListItem>>(emptyList()) }
    var reservations by remember { mutableStateOf<List<ReservationItem>>(emptyList()) }
    var metrics by remember { mutableStateOf<OwnerMetrics?>(null) }
    var loading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var selectedCafeId by remember { mutableStateOf<String?>(null) }

    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        loading = true
        errorMessage = null
        try {
            val cafesDeferred = scope.launch { cafes = repository.getCafes() }
            val resDeferred = scope.launch { reservations = repository.getReservations() }
            val metricsDeferred = scope.launch { metrics = repository.getMetrics() }
            cafesDeferred.join(); resDeferred.join(); metricsDeferred.join()

            // If a single cafe exists, pre-select it
            if (cafes.size == 1) selectedCafeId = cafes.first().id
            // if none selected and multiple cafes exist, leave null so UI shows chips
        } catch (t: Throwable) {
            errorMessage = "데이터를 불러오는 중 오류가 발생했습니다."
        } finally {
            loading = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundBase)
            .verticalScroll(rememberScrollState())
            .padding(bottom = 40.dp)
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(BackgroundBase)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Seatly", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = TextMain)
                Text(".", color = Primary, fontWeight = FontWeight.Bold, fontSize = 20.sp)
            }

            Box(modifier = Modifier.size(36.dp), contentAlignment = Alignment.Center) {
                IconButton(onClick = { navController.navigate("notifications") }) {
                    Icon(painter = painterResource(id = R.drawable.icon_search), contentDescription = "알림", tint = TextMain)
                }
                Box(
                    modifier = Modifier
                        .size(14.dp)
                        .align(Alignment.TopEnd)
                        .offset(x = (-2).dp, y = 2.dp)
                        .clip(CircleShape)
                        .background(Negative),
                    contentAlignment = Alignment.Center
                ) {
                    Text("3", fontSize = 9.sp, color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // --- Operating cafe section ---
        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
            Text("운영 카페", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextMain)
            Spacer(modifier = Modifier.height(8.dp))

            when {
                loading -> {
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, BorderColor, RoundedCornerShape(12.dp))
                            .heightIn(min = 72.dp)
                    ) {
                        Box(modifier = Modifier.fillMaxWidth().padding(12.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    }
                }
                errorMessage != null -> {
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, BorderColor, RoundedCornerShape(12.dp))
                    ) {
                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("데이터를 불러오는 중 오류가 발생했습니다.", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = TextMain)
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(errorMessage ?: "", fontSize = 12.sp, color = TextSub)
                            }
                            Button(onClick = {
                                scope.launch {
                                    loading = true
                                    errorMessage = null
                                    try {
                                        cafes = repository.getCafes()
                                        reservations = repository.getReservations()
                                        metrics = repository.getMetrics()
                                    } catch (t: Throwable) {
                                        errorMessage = "재시도 중 오류"
                                    } finally {
                                        loading = false
                                    }
                                }
                            }, colors = ButtonDefaults.buttonColors(containerColor = Primary, contentColor = Color.White)) {
                                Text("재시도")
                            }
                        }
                    }
                }
                cafes.isEmpty() -> {
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, BorderColor, RoundedCornerShape(12.dp))
                    ) {
                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("등록된 스터디 카페가 없습니다.", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = TextMain)
                                Spacer(modifier = Modifier.height(6.dp))
                                Text("먼저 스터디 카페를 등록해 주세요.", fontSize = 12.sp, color = TextSub)
                            }
                            Button(
                                onClick = { navController.navigate("register_cafe_1") },
                                colors = ButtonDefaults.buttonColors(containerColor = Primary, contentColor = Color.White)
                            ) {
                                Text("등록하기")
                            }
                        }
                    }
                }
                else -> {
                    // If there's at least one cafe, show the top "operating cafe" card similar to the provided design.
                    val activeCafe = cafes.find { it.id == selectedCafeId } ?: cafes.first()

                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, BorderColor, RoundedCornerShape(12.dp))
                    ) {
                        Row(modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 18.dp, horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("OPERATING CAFE", fontSize = 11.sp, color = TextSub)
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(activeCafe.title, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = TextMain)
                            }

                            // status pill
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(Color(0xFFFFF4F0))
                                    .border(BorderStroke(1.dp, Color(0x30E95321)), RoundedCornerShape(20.dp))
                                    .padding(horizontal = 12.dp, vertical = 8.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Primary))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("운영 중", color = Primary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // If multiple cafes, show chips to allow selection
                    if (cafes.size > 1) {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            items(cafes) { cafe ->
                                CafeChip(
                                    name = cafe.title,
                                    selected = cafe.id == activeCafe.id,
                                    onClick = { selectedCafeId = cafe.id }
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    // Seat status card (matches design)
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, BorderColor, RoundedCornerShape(12.dp))
                    ) {
                        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            // Circular progress + percent
                            val total = metrics?.totalSeats ?: 0
                            val used = metrics?.usedSeats ?: 0
                            val percent = if (total > 0) (used.toFloat() / total.toFloat()) else 0f
                            Box(modifier = Modifier.size(88.dp), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(
                                    progress = percent,
                                    strokeWidth = 8.dp,
                                    color = Primary,
                                    modifier = Modifier.size(88.dp)
                                )
                                Text("${(percent * 100).toInt()}%", fontWeight = FontWeight.Bold, color = TextMain)
                            }

                            Spacer(modifier = Modifier.width(16.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text(activeCafe.title, color = TextSub, fontSize = 13.sp)
                                    Text(if (total == 0) "-" else "${total}석", color = TextMain, fontWeight = FontWeight.Medium)
                                }
                                Spacer(modifier = Modifier.height(10.dp))

                                // used / available rows
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Primary))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("사용 중", color = Primary, fontSize = 13.sp)
                                    }
                                    Text(if (total == 0) "-" else "${used}석", color = Primary, fontWeight = FontWeight.Medium)
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Color(0xFFEFEFEF)))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("사용 가능", color = TextSub, fontSize = 13.sp)
                                    }
                                    val avail = if (total - used >= 0) total - used else 0
                                    Text(if (total == 0) "-" else "${avail}석", color = TextMain, fontWeight = FontWeight.Medium)
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                Box(modifier = Modifier.height(1.dp).fillMaxWidth().background(Color(0xFFF0F0F0)))
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("업데이트 2분 전", fontSize = 12.sp, color = TextSub)
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // metrics row (two cards)
        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                MetricCard(title = "오늘 방문", value = metrics?.todayVisitors ?: "-", modifier = Modifier.weight(1f))
                Spacer(modifier = Modifier.width(12.dp))
                MetricCard(title = "취소율", value = metrics?.cancelRate ?: "-", modifier = Modifier.weight(1f))
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // recent reservations
        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
            Text("최근 예약", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextMain)
            Spacer(modifier = Modifier.height(12.dp))

            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                modifier = Modifier.fillMaxWidth().border(1.dp, BorderColor, RoundedCornerShape(12.dp))
            ) {
                Column {
                    reservations.forEachIndexed { index, r ->
                        ReservationRow(res = r, onClick = { /* navigate */ })
                        if (index != reservations.lastIndex) {
                            DividerSimple()
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // action pills
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            ActionPill(icon = Icons.Default.Chair, label = "좌석 관리", badgeCount = 0, modifier = Modifier.weight(1f), onClick = { navController.navigate("current_seat") })
            ActionPill(icon = Icons.Default.CheckCircle, label = "예약 승인", badgeCount = reservations.size.coerceAtMost(99), modifier = Modifier.weight(1f), onClick = { /* approval */ })
        }

        Spacer(modifier = Modifier.height(40.dp))
    }
}

/* ---------- Helpers (same as earlier) ---------- */

@Composable
private fun CafeChip(name: String, selected: Boolean, onClick: () -> Unit) {
    val bg = if (selected) Primary else SurfaceCard
    val textColor = if (selected) Color.White else TextMain
    Card(
        shape = RoundedCornerShape(999.dp),
        colors = CardDefaults.cardColors(containerColor = bg),
        modifier = Modifier
            .border(1.dp, BorderColor, RoundedCornerShape(999.dp))
            .clickable { onClick() }
    ) {
        Box(modifier = Modifier.fillMaxSize().heightIn(min = 40.dp), contentAlignment = Alignment.Center) {
            Text(name, color = textColor, fontSize = 13.sp, fontWeight = FontWeight.Medium, modifier = Modifier.padding(horizontal = 14.dp))
        }
    }
}

@Composable
private fun MetricCard(title: String, value: String, modifier: Modifier = Modifier) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceCard),
        modifier = modifier
            .height(80.dp)
            .border(1.dp, BorderColor, RoundedCornerShape(12.dp))
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.Center) {
            Text(title, fontSize = 11.sp, color = TextSub)
            Spacer(modifier = Modifier.height(6.dp))
            Text(value, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextMain)
        }
    }
}

@Composable
private fun ReservationRow(res: ReservationItem, onClick: () -> Unit) {
    Row(modifier = Modifier
        .fillMaxWidth()
        .clickable { onClick() }
        .padding(horizontal = 12.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(res.avatarBg)
                .border(1.dp, BorderColor, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            val initial = res.name.firstOrNull()?.toString() ?: "?"
            Text(initial, color = if (res.avatarBg == Color.White) TextMain else Color.White, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(res.name, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = TextMain)
            Text("${res.seatCode} | ${res.timeRange}", fontSize = 11.sp, color = TextSub)
        }

        Column(horizontalAlignment = Alignment.End) {
            Text(res.priceText, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Primary)
            Text(res.agoText, fontSize = 11.sp, color = TextSub)
        }
    }
}

@Composable
private fun DividerSimple() {
    Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(BorderColor))
}

@Composable
private fun ActionPill(icon: ImageVector, label: String, badgeCount: Int = 0, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Card(shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = SurfaceCard), modifier = modifier.height(84.dp).border(1.dp, BorderColor, RoundedCornerShape(12.dp)).clickable { onClick() }) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center, modifier = Modifier.fillMaxSize().padding(8.dp)) {
            Box {
                Icon(icon, contentDescription = label, tint = TextMain, modifier = Modifier.size(28.dp))
                if (badgeCount > 0) {
                    Box(modifier = Modifier.size(18.dp).align(Alignment.TopEnd).offset(x = 10.dp, y = (-10).dp).clip(CircleShape).background(Primary), contentAlignment = Alignment.Center) {
                        Text("$badgeCount", fontSize = 10.sp, color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(label, fontSize = 12.sp, color = TextMain)
        }
    }
}