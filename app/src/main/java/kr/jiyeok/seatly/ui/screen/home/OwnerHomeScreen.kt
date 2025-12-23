package kr.jiyeok.seatly.ui.screen.home

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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import kr.jiyeok.seatly.R

private val Primary = Color(0xFFe95321)
private val BackgroundBase = Color(0xFFFFFFFF)
private val SurfaceCard = Color(0xFFF9F9F9)
private val TextMain = Color(0xFF1A1A1A)
private val TextSub = Color(0xFF888888)
private val BorderColor = Color(0xFFE5E5E5)
private val Negative = Color(0xFFef4444)

/** Simple model representing a study cafe (for selection at the top) */
data class StudyCafe(val id: String, val name: String)

data class ReservationItem(
    val id: String,
    val name: String,
    val seatCode: String,
    val timeRange: String,
    val priceText: String,
    val agoText: String,
    val avatarBg: Color
)

/**
 * OwnerHomeScreen (no built-in dummy cafes)
 *
 * This version intentionally sets cafes = emptyList() so you can inspect the "no registered cafes" UI.
 * If you want to test with example cafes, replace cafes with a list of StudyCafe objects or
 * integrate a ViewModel/data source.
 */
@Composable
fun OwnerHomeScreen(navController: NavController) {
    // NO dummy cafes here — empty list to show registration prompt
    val cafes: List<StudyCafe> = emptyList()

    // Sample reservations are still provided here for preview/testing.
    // In real use replace with ViewModel-provided list.
    val reservations = listOf(
        ReservationItem("1", "김준호", "A3", "14:00-18:00", "₩18,000", "5분 전", Primary),
        ReservationItem("2", "이서연", "B12", "19:00-22:00", "₩12,000", "12분 전", Color.White),
        ReservationItem("3", "Park Min", "S1", "09:00-12:00", "₩9,000", "24분 전", Color.White),
        ReservationItem("4", "최현수", "C4", "10:00-14:00", "₩15,000", "40분 전", Color.White)
    )

    // selected cafe id (remembered state) - starts empty because there are no cafes
    var selectedCafeId by remember { mutableStateOf("") }

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
                Text("StudyHub", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = TextMain)
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

        // --- Study cafe selector section ---
        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
            // header matches 좌석 현황 styling
            Text("운영 카페", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextMain)
            Spacer(modifier = Modifier.height(8.dp))

            when {
                cafes.isEmpty() -> {
                    // No cafes: prompt to register
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
                            // registration button uses Primary (orange) to match other uses
                            Button(
                                onClick = { /* navigate to cafe registration screen */ navController.navigate("register_cafe_1") },
                                colors = ButtonDefaults.buttonColors(containerColor = Primary, contentColor = Color.White)
                            ) {
                                Text("등록하기")
                            }
                        }
                    }
                }
                cafes.size == 1 -> {
                    // Single cafe: show badge (left-aligned content similar to seat section)
                    val cafe = cafes.first()
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, BorderColor, RoundedCornerShape(12.dp))
                            .clickable { selectedCafeId = cafe.id }
                    ) {
                        Box(modifier = Modifier.height(56.dp).fillMaxWidth(), contentAlignment = Alignment.CenterStart) {
                            Box(modifier = Modifier.padding(start = 12.dp)) {
                                Column {
                                    // Show cafe name only (no prefix), larger and black as requested
                                    Text(cafe.name, fontSize = 15.sp, fontWeight = FontWeight.Medium, color = Color.Black)
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text("이 카페의 좌석 현황을 보고 있습니다.", fontSize = 12.sp, color = TextSub)
                                }
                            }
                        }
                    }
                }
                else -> {
                    // Multiple cafes: selectable chips in a LazyRow
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(cafes) { cafe ->
                            CafeChip(
                                name = cafe.name,
                                selected = (cafe.id == selectedCafeId),
                                onClick = { selectedCafeId = cafe.id }
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 좌석 현황 (uses selectedCafeId for context)
        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
            Text("좌석 현황", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextMain)
            Spacer(modifier = Modifier.height(12.dp))
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, BorderColor, RoundedCornerShape(18.dp))
            ) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(88.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(
                            progress = if (cafes.isEmpty()) 0.0f else 0.6f,
                            strokeWidth = 8.dp,
                            color = Primary,
                            modifier = Modifier.size(88.dp)
                        )
                        Text(if (cafes.isEmpty()) "-" else "60%", fontWeight = FontWeight.Bold, color = TextMain)
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            // When a cafe is selected, show its name only (bigger, black)
                            if (selectedCafeId.isNotBlank()) {
                                val cafeName = cafes.find { it.id == selectedCafeId }?.name ?: "선택 카페"
                                Text(cafeName, color = Color.Black, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                            } else if (cafes.isEmpty()) {
                                Text("선택된 카페 없음", color = TextSub, fontSize = 12.sp)
                            } else {
                                Text("전체 좌석", color = TextSub, fontSize = 12.sp)
                            }
                            Text(if (cafes.isEmpty()) "-" else "60석", color = TextMain, fontWeight = FontWeight.Medium)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(if (cafes.isEmpty()) BorderColor else Primary))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("사용 중", color = if (cafes.isEmpty()) TextSub else Primary, fontSize = 12.sp)
                            }
                            Text(if (cafes.isEmpty()) "-" else "24석", color = TextMain)
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(BorderColor))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("사용 가능", color = TextSub, fontSize = 12.sp)
                            }
                            Text(if (cafes.isEmpty()) "-" else "36석", color = TextMain, fontWeight = FontWeight.Medium)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        DividerRow(text = if (cafes.isEmpty()) "카페를 등록하면 좌석 현황이 여기에 표시됩니다." else "업데이트 2분 전")
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Simple metric row (kept minimal)
        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                MetricCard(title = "오늘 방문", value = if (cafes.isEmpty()) "-" else "48명", modifier = Modifier.weight(1f))
                Spacer(modifier = Modifier.width(12.dp))
                MetricCard(title = "취소율", value = if (cafes.isEmpty()) "-" else "2.3%", modifier = Modifier.weight(1f))
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // 최근 예약: show list if there are reservations, otherwise show a friendly empty state
        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text("최근 예약", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextMain)
            }
            Spacer(modifier = Modifier.height(12.dp))

            if (reservations.isEmpty()) {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, BorderColor, RoundedCornerShape(12.dp))
                ) {
                    Box(modifier = Modifier.padding(16.dp)) {
                        Text("최근 예약 정보가 없습니다.", color = TextSub)
                    }
                }
            } else {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                    modifier = Modifier.fillMaxWidth().border(1.dp, BorderColor, RoundedCornerShape(18.dp))
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
        }

        Spacer(modifier = Modifier.height(24.dp))

        // action buttons
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            ActionPill(icon = Icons.Default.Chair, label = "좌석 관리", badgeCount = 0, modifier = Modifier.weight(1f), onClick = { navController.navigate("current_seat") })
            ActionPill(icon = Icons.Default.CheckCircle, label = "예약 승인", badgeCount = 5, modifier = Modifier.weight(1f), onClick = { /* approval */ })
        }

        Spacer(modifier = androidx.compose.ui.Modifier.height(40.dp))
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
private fun DividerRow(text: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(text, fontSize = 10.sp, color = TextSub)
    }
}

@Composable
private fun MetricCard(title: String, value: String, trailingIcon: Boolean = false, modifier: Modifier = Modifier) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceCard),
        modifier = modifier
            .height(80.dp)
            .border(1.dp, BorderColor, RoundedCornerShape(16.dp))
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.Center) {
            Text(title, fontSize = 11.sp, color = TextSub)
            Spacer(modifier = androidx.compose.ui.Modifier.height(6.dp))
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

        Spacer(modifier = androidx.compose.ui.Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(res.name, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = TextMain)
            Text("${res.seatCode} | ${res.timeRange}", fontSize = 11.sp, color = TextSub)
        }

        Column(horizontalAlignment = Alignment.End) {
            Text(res.priceText, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = if (res.avatarBg == Primary) Primary else TextMain)
            Text(res.agoText, fontSize = 11.sp, color = TextSub)
        }
    }
}

@Composable
private fun DividerSimple() {
    Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(BorderColor))
}

@Composable
private fun ActionPill(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, badgeCount: Int = 0, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = SurfaceCard), modifier = modifier.height(84.dp).border(1.dp, BorderColor, RoundedCornerShape(16.dp)).clickable { onClick() }) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center, modifier = Modifier.fillMaxSize()) {
            Box {
                Icon(icon, contentDescription = label, tint = TextMain, modifier = Modifier.size(28.dp))
                if (badgeCount > 0) {
                    Box(modifier = Modifier.size(18.dp).align(Alignment.TopEnd).offset(x = 10.dp, y = (-10).dp).clip(CircleShape).background(Primary), contentAlignment = Alignment.Center) {
                        Text("$badgeCount", fontSize = 10.sp, color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
            Spacer(modifier = androidx.compose.ui.Modifier.height(8.dp))
            Text(label, fontSize = 12.sp, color = TextMain)
        }
    }
}