package kr.jiyeok.seatly.ui.screen.admin

import android.graphics.BitmapFactory
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.Image
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
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kr.jiyeok.seatly.data.remote.enums.EFacility
import kr.jiyeok.seatly.data.remote.response.UsageDto
import kr.jiyeok.seatly.presentation.viewmodel.AdminCafeDetailViewModel
import kr.jiyeok.seatly.ui.theme.*
import java.text.NumberFormat

@Composable
fun AdminCafeInfoTab(
    viewModel: AdminCafeDetailViewModel,
    navController: NavController
) {
    val cafeInfo by viewModel.cafeInfo.collectAsState()
    val cafeUsage by viewModel.cafeUsage.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    var selectedTab by remember { mutableStateOf(0) }
    val scrollState = rememberScrollState()

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(bottom = 100.dp)
        ) {
            // 카페 이미지 헤더
            val gallery = if (cafeInfo?.imageUrls.isNullOrEmpty()) listOf("") else cafeInfo?.imageUrls!!
            HeaderSection(gallery, viewModel)

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .offset(y = (-40).dp)
                    .padding(horizontal = 20.dp)
            ) {
                InfoMainCard(
                    name = cafeInfo?.name ?: "카페 이름",
                    address = cafeInfo?.address ?: "카페 주소",
                    phone = cafeInfo?.phone ?: "카페 대표 번호",
                    selectedTab = selectedTab,
                    onTabSelected = { selectedTab = it }
                )

                Spacer(modifier = Modifier.height(24.dp))

                Crossfade(targetState = selectedTab, label = "") { tab ->
                    if (tab == 0) {
                        FeeSection()
                    } else {
                        SeatStatusSection(cafeUsage)
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))
                FacilitiesSection(cafeInfo?.facilities ?: emptyList())
                Spacer(modifier = Modifier.height(32.dp))
                LocationSection()
            }
        }

        FloatingActionButton(
            onClick = {
                cafeInfo?.id?.let { cafeId ->
                    navController.navigate("admin/cafe/update/$cafeId")
                }
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 20.dp, bottom = 20.dp),
            containerColor = ColorPrimaryOrange,
            contentColor = ColorWhite
        ) {
            Icon(Icons.Default.Edit, contentDescription = "카페 편집")
        }
    }
}

@Composable
private fun HeaderSection(
    images: List<String>,
    viewModel: AdminCafeDetailViewModel
) {
    val listState = rememberLazyListState()
    val currentIndex = remember { derivedStateOf { listState.firstVisibleItemIndex + 1 } }

    Box(modifier = Modifier.fillMaxWidth().height(240.dp)) {
        LazyRow(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(bottomStart = 40.dp, bottomEnd = 40.dp))
        ) {
            items(images) { imageId ->
                if (imageId.isNotEmpty()) {
                    ServerCafeImage(
                        imageId = imageId,
                        viewModel = viewModel,
                        modifier = Modifier
                            .width(LocalConfiguration.current.screenWidthDp.dp)
                            .fillMaxHeight()
                    )
                } else {
                    // 기본 이미지
                    Image(
                        painter = painterResource(kr.jiyeok.seatly.R.drawable.img_default_cafe),
                        contentDescription = null,
                        modifier = Modifier
                            .width(LocalConfiguration.current.screenWidthDp.dp)
                            .fillMaxHeight(),
                        contentScale = ContentScale.Crop
                    )
                }
            }
        }

        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 60.dp, end = 24.dp)
                .background(ColorWhite.copy(alpha = 0.9f), RoundedCornerShape(16.dp))
                .padding(horizontal = 10.dp, vertical = 4.dp)
        ) {
            Text(
                "${currentIndex.value} / ${images.size}",
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = ColorTextBlack
            )
        }
    }
}

// 서버 이미지 로드 Composable
@Composable
private fun ServerCafeImage(
    imageId: String,
    viewModel: AdminCafeDetailViewModel,
    modifier: Modifier = Modifier
) {
    val imageDataCache by viewModel.imageDataCache.collectAsState()
    var bitmap by remember { mutableStateOf<ImageBitmap?>(null) }

    LaunchedEffect(imageId) {
        viewModel.loadImage(imageId)
    }

    LaunchedEffect(imageDataCache) {
        val imageData = imageDataCache[imageId]
        if (imageData != null) {
            withContext(Dispatchers.IO) {
                try {
                    val bmp = BitmapFactory.decodeByteArray(imageData, 0, imageData.size)
                    bitmap = bmp?.asImageBitmap()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap!!,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            // 로딩 중
            CircularProgressIndicator(
                modifier = Modifier.size(40.dp),
                color = ColorPrimaryOrange,
                strokeWidth = 3.dp
            )
        }
    }
}

// 나머지 Composable 함수들은 동일...
@Composable
private fun InfoMainCard(
    name: String,
    address: String,
    phone: String,
    selectedTab: Int,
    onTabSelected: (Int) -> Unit
) {
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
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(ColorWhite, RoundedCornerShape(12.dp))
                .padding(8.dp)
        ) {
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
                Column(
                    modifier = Modifier.padding(20.dp).fillMaxSize(),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
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
private fun SeatStatusSection(cafeUsage: UsageDto?) {
    val total = cafeUsage?.totalCount ?: 0
    val used = cafeUsage?.useCount ?: 0
    val available = total - used
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
    if (facilities.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(ColorBgBeige, RoundedCornerShape(16.dp))
                .padding(vertical = 40.dp),
            contentAlignment = Alignment.Center
        ) {
            Text("등록된 편의시설이 없습니다", fontSize = 14.sp, color = ColorTextGray, fontWeight = FontWeight.Medium)
        }
    } else {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            for (i in facilities.indices step 2) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    FacilityItem(facilities[i], Modifier.weight(1f))
                    if (i + 1 < facilities.size) {
                        FacilityItem(facilities[i + 1], Modifier.weight(1f))
                    } else {
                        Spacer(Modifier.weight(1f))
                    }
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
        Image(
            painter = painterResource(kr.jiyeok.seatly.R.drawable.img_default_cafe),
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
