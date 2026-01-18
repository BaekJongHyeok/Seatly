package kr.jiyeok.seatly.ui.screen.user

import android.widget.Toast
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import kr.jiyeok.seatly.data.remote.enums.EFacility
import kr.jiyeok.seatly.data.remote.enums.ESeatStatus
import kr.jiyeok.seatly.presentation.viewmodel.CafeDetailViewModel
import kr.jiyeok.seatly.ui.theme.*
import java.text.NumberFormat

@Composable
fun AdminCafeDetailScreen(
    navController: NavController
    , cafeId: String?
) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    val viewModel: CafeDetailViewModel = hiltViewModel()

    val cafeDetail by viewModel.cafeDetail.collectAsState()
    val cafeUsage by viewModel.cafeUsage.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val seats by viewModel.seats.collectAsState()

    var selectedTab by remember { mutableStateOf(0) }
    val scrollState = rememberScrollState()

    LaunchedEffect(cafeId) {
        if (!cafeId.isNullOrEmpty()) {
            viewModel.loadCafeDetail(cafeId.toLong())
            viewModel.loadCafeUsage(cafeId.toLong())
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(ColorWhite)) {
        if (isLoading) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = ColorPrimaryOrange)
        } else if (cafeDetail != null) {
            val cafe = cafeDetail!!
            val gallery = if (cafe.imageUrls.isNullOrEmpty()) listOf("") else cafe.imageUrls!!

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(bottom = 80.dp) // 3. 하단 바 여백 증가에 따른 패딩 조정
            ) {
                HeaderSection(gallery, screenWidth) { navController?.popBackStack() }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .offset(y = (-40).dp)
                        .padding(horizontal = 20.dp)
                ) {
                    InfoMainCard(
                        name = cafe.name ?: "명지 스터디카페",
                        address = cafe.address ?: "서울시 강서구 마곡로 100",
                        phone = cafe.phone ?: "02-1234-5678",
                        selectedTab = selectedTab,
                        onTabSelected = { selectedTab = it }
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Crossfade(targetState = selectedTab, label = "") { tab ->
                        if (tab == 0) {
                            FeeSection()
                        } else {
                            SeatStatusSection(cafeUsage.useCount, cafeUsage.totalCount)
                        }
                    }

                    Spacer(modifier = Modifier.height(32.dp))
                    FacilitiesSection(cafe.facilities?.filterNotNull() ?: emptyList())
                    Spacer(modifier = Modifier.height(32.dp))
                    LocationSection()
                }
            }
        }

        BottomActionBar(
            modifier = Modifier.align(Alignment.BottomCenter),
            onEditContent = { navController.navigate("admin/cafe/update/{$cafeId}") },
            onEditSeats = { Toast.makeText(context, "좌석 편집", Toast.LENGTH_SHORT).show() }
        )
    }
}

@Composable
private fun HeaderSection(images: List<String>, screenWidth: Dp, onBack: () -> Unit) {
    val listState = rememberLazyListState()
    val currentIndex = remember { derivedStateOf { listState.firstVisibleItemIndex + 1 } }

    Box(modifier = Modifier.fillMaxWidth().height(240.dp)) {
        LazyRow(
            state = listState,
            modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(bottomStart = 40.dp, bottomEnd = 40.dp))
        ) {
            items(images) { url ->
                AsyncImage(
                    model = url,
                    contentDescription = null,
                    modifier = Modifier.width(screenWidth).fillMaxHeight(),
                    contentScale = ContentScale.Crop,
                    error = painterResource(kr.jiyeok.seatly.R.drawable.img_default_cafe)
                )
            }
        }

        IconButton(
            onClick = onBack,
            modifier = Modifier
                .padding(top = 24.dp, start = 20.dp)
                .size(44.dp)
                .shadow(1.dp, CircleShape)
                .background(ColorWhite, CircleShape)
        ) {
            Icon(Icons.Filled.ArrowBackIosNew, contentDescription = null, modifier = Modifier.size(18.dp), tint = ColorTextBlack)
        }

        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 60.dp, end = 24.dp)
                .background(ColorWhite.copy(alpha = 0.9f), RoundedCornerShape(16.dp))
                .padding(horizontal = 10.dp, vertical = 4.dp)
        ) {
            Text("${currentIndex.value} / ${images.size}", fontSize = 11.sp, fontWeight = FontWeight.Medium, color = ColorTextBlack)
        }
    }
}

@Composable
private fun InfoMainCard(name: String, address: String, phone: String, selectedTab: Int, onTabSelected: (Int) -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().shadow(2.dp, RoundedCornerShape(24.dp)),
        shape = RoundedCornerShape(24.dp),
        color = ColorBgBeige
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Text(name, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = ColorTextBlack)

            Spacer(modifier = Modifier.height(20.dp))

            InfoRow(Icons.Filled.LocationOn, address)
            Divider(modifier = Modifier.padding(vertical = 12.dp), color = ColorBorderLight)
            InfoRow(Icons.Filled.Call, phone)
            Divider(modifier = Modifier.padding(vertical = 12.dp), color = ColorBorderLight)
            InfoRow(Icons.Filled.Schedule, "24시간 운영\n월-금: 06:00 - 02:00\n토-일: 08:00 - 02:00")

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .background(Color(0xFFF0F0F0), RoundedCornerShape(20.dp))
                    .padding(4.dp)
            ) {
                TabButton("요금 정보", isSelected = selectedTab == 0, modifier = Modifier.weight(1f)) { onTabSelected(0) }
                TabButton("좌석 현황", isSelected = selectedTab == 1, modifier = Modifier.weight(1f)) { onTabSelected(1) }
            }
        }
    }
}

@Composable
private fun InfoRow(icon: ImageVector, text: String) {
    // 2. 수직 중앙 정렬 적용 (Alignment.Top -> CenterVertically)
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(36.dp).background(ColorWhite, RoundedCornerShape(12.dp)).padding(8.dp)) {
            Icon(icon, contentDescription = null, tint = ColorPrimaryOrange, modifier = Modifier.size(20.dp))
        }
        Spacer(modifier = Modifier.width(16.dp))
        Text(text, fontSize = 14.sp, color = ColorTextGray, lineHeight = 20.sp)
    }
}

@Composable
private fun TabButton(text: String, isSelected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Box(
        modifier = modifier
            .fillMaxHeight()
            .clip(RoundedCornerShape(16.dp))
            .background(if (isSelected) ColorPrimaryOrange else Color.Transparent)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = if (isSelected) ColorWhite else ColorTextBlack,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun FeeSection() {
    val fees = listOf("1시간" to 4500, "4시간" to 17000, "8시간" to 32000, "회원권" to 80000)
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 10.dp)
    ) {
        items(fees) { (label, price) ->
            Surface(
                modifier = Modifier.size(160.dp, 130.dp).shadow(1.dp, RoundedCornerShape(20.dp)),
                shape = RoundedCornerShape(20.dp),
                color = ColorBgBeige
            ) {
                Column(modifier = Modifier.padding(20.dp).fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
                    Text(label, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = ColorTextBlack)
                    Column {
                        Text("₩${NumberFormat.getInstance().format(price)}", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = ColorPrimaryOrange)
                        Text("₩${NumberFormat.getInstance().format(price + 1000)}", fontSize = 13.sp, color = ColorTextGray, textDecoration = TextDecoration.LineThrough)
                    }
                }
            }
        }
    }
}

@Composable
private fun SeatStatusSection(available: Int, total: Int) {
    val used = total - available
    val usedRatio = if (total > 0) used.toFloat() / total else 0f

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(ColorBgBeige, RoundedCornerShape(24.dp))
            .padding(24.dp)
    ) {
        SeatProgressRow("사용 중", "$used / $total", usedRatio, ColorPrimaryOrange)
        Spacer(modifier = Modifier.height(20.dp))
        SeatProgressRow("사용 가능", "$available 석", 1f - usedRatio, Color(0xFFE0E0E0))
    }
}

@Composable
private fun SeatProgressRow(label: String, value: String, ratio: Float, color: Color) {
    Column {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = ColorTextBlack)
            Text(value, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = ColorTextBlack)
        }
        Spacer(modifier = Modifier.height(8.dp))
        LinearProgressIndicator(
            progress = ratio,
            modifier = Modifier.fillMaxWidth().height(12.dp).clip(CircleShape),
            color = color,
            trackColor = ColorWhite
        )
    }
}

@Composable
private fun FacilitiesSection(facilities: List<EFacility>) {
    Text("편의시설", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = ColorTextBlack, modifier = Modifier.padding(start = 4.dp))
    Spacer(modifier = Modifier.height(16.dp))

    val items = facilities.ifEmpty { listOf(EFacility.WIFI, EFacility.CAFE, EFacility.PRINTER, EFacility.LOCKER) }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        for (i in items.indices step 2) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                FacilityItem(items[i], Modifier.weight(1f))
                if (i + 1 < items.size) {
                    FacilityItem(items[i + 1], Modifier.weight(1f))
                } else {
                    Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun FacilityItem(facility: EFacility, modifier: Modifier) {
    val (icon, label) = when (facility) {
        EFacility.WIFI -> Icons.Default.Wifi to "WiFi 무료"
        EFacility.CAFE -> Icons.Default.LocalCafe to "음료 무한"
        EFacility.PRINTER -> Icons.Default.Print to "프린트 가능"
        else -> Icons.Default.SettingsPower to "콘센트 완비"
    }

    Surface(
        modifier = modifier.shadow(1.dp, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        color = ColorWhite
    ) {
        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = ColorPrimaryOrange, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(10.dp))
            Text(label, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = ColorTextBlack)
        }
    }
}

@Composable
private fun LocationSection() {
    Text("위치", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = ColorTextBlack, modifier = Modifier.padding(start = 4.dp))
    Spacer(modifier = Modifier.height(16.dp))

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .shadow(1.dp, RoundedCornerShape(24.dp))
            .clip(RoundedCornerShape(24.dp))
            .background(ColorWhite)
    ) {
        AsyncImage(
            model = "https://maps.googleapis.com/maps/api/staticmap?center=37.5665,126.9780&zoom=15&size=600x400",
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        Icon(
            Icons.Filled.LocationOn,
            contentDescription = null,
            tint = ColorPrimaryOrange,
            modifier = Modifier.size(40.dp).align(Alignment.Center).offset(y = (-16).dp)
        )

        Button(
            onClick = { /* 지도 앱 연결 */ },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(12.dp)
                .fillMaxWidth(0.85f)
                .height(44.dp),
            colors = ButtonDefaults.buttonColors(containerColor = ColorPrimaryOrange),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Default.Map, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("지도에서 보기", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = ColorWhite)
        }
    }
}

@Composable
private fun BottomActionBar(modifier: Modifier = Modifier, onEditContent: () -> Unit, onEditSeats: () -> Unit) {
    // 3. 버튼 바 높이 및 위아래 공백 증가 (52.dp -> 80.dp / vertical padding 6.dp -> 16.dp)
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(80.dp),
        color = ColorBgBeige.copy(alpha = 0.98f),
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, ColorWhite)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Button(
                onClick = onEditContent,
                modifier = Modifier.weight(1f).fillMaxHeight(),
                colors = ButtonDefaults.buttonColors(containerColor = ColorWhite),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, ColorBorderLight)
            ) {
                Text("내용 수정", color = ColorTextBlack, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }

            Button(
                onClick = onEditSeats,
                modifier = Modifier.weight(1f).fillMaxHeight(),
                colors = ButtonDefaults.buttonColors(containerColor = ColorPrimaryOrange),
                shape = RoundedCornerShape(12.dp),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
            ) {
                Text("좌석 편집", color = ColorWhite, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
        }
    }
}