package kr.jiyeok.seatly.ui.screen.user

import android.R
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EventSeat
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocalCafe
import androidx.compose.material.icons.filled.LocalParking
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.MeetingRoom
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
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
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import kr.jiyeok.seatly.data.remote.enums.EFacility
import kr.jiyeok.seatly.data.remote.enums.ESeatStatus
import kr.jiyeok.seatly.presentation.viewmodel.CafeDetailViewModel
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.abs

// Theme/colors
private object ThemeColors {
    val Primary = Color(0xFFFF6633)
    val Background = Color(0xFFFFFFFF)
    val TextPrimary = Color(0xFF1A1A1A)
    val TextSecondary = Color(0xFF888888)
    val Border = Color(0xFFEFEFEF)
    val Gray100 = Color(0xFFF6F6F8)
    val Gray200 = Color(0xFFEDEDED)
}

// Unified spacing constants
private val DividerGap = 20.dp
private val SectionGap = 24.dp

@Composable
fun CafeDetailScreen(navController: NavHostController?, cafeId: String?) {
    val context = LocalContext.current
    val numberFormatter = NumberFormat.getNumberInstance(Locale.KOREA)
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp

    // ViewModel
    val viewModel: CafeDetailViewModel = hiltViewModel()
    val cafeDetail by viewModel.cafeDetail.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    val seats by viewModel.seats.collectAsState()

    LaunchedEffect(cafeId) {
        if (!cafeId.isNullOrEmpty()) {
            try {
                val id = cafeId.toLong()
                viewModel.loadCafeDetail(id)
                viewModel.loadCafeSeats(id)
            } catch (e: NumberFormatException) {
                Toast.makeText(context, "유효하지 않은 카페 ID입니다", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // ====== DTO 기반 바인딩 (샘플 데이터 제거) ======
    val cafeName = cafeDetail?.name ?: "카페 정보 로딩 중"
    val address = cafeDetail?.address ?: "주소 정보 로딩 중"
    val phone = cafeDetail?.phone ?: "전화번호 정보 로딩 중"
    val description = cafeDetail?.description ?: "소개 정보가 없습니다." // ✅ 소개 텍스트

    val gallery: List<String> = cafeDetail?.imageUrls ?: emptyList()
    val galleryForUi: List<String> = if (gallery.isNotEmpty()) gallery else listOf("")

    // ✅ 편의시설: NPE 방지 (filterNotNull 추가)
    val facilities: List<EFacility> =
        (cafeDetail?.facilities ?: emptyList())
            .filterNotNull() // NPE 원인 제거
            .filterNot { it == EFacility.OPEN_24H }

    // 좌석 상태 (Mock 기준 INUSE가 사용중)
    val usedSeats = seats.count { it.status == ESeatStatus.AVAILABLE }
    val totalSeats = seats.size

    // 요금은 DTO에 없으므로(현재 스키마 기준) 임시 유지
    val priceOptions = listOf(
        Triple("1시간", 4500, 10),
        Triple("4시간", 17000, 0),
        Triple("8시간", 32000, 0)
    )

    // map 미리보기
    val mapImageUrl =
        "https://maps.googleapis.com/maps/api/staticmap?center=37.5665,126.9780&zoom=13&size=600x300&markers=color:orange%7Clabel:%7C37.5665,126.9780"

    // gallery paging
    val listState = rememberLazyListState()
    val currentPage by remember {
        derivedStateOf {
            listState.firstVisibleItemIndex.coerceIn(0, maxOf(0, galleryForUi.size - 1))
        }
    }
    val coroutineScope = rememberCoroutineScope()

    var isFavorite by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()

    fun openMapForAddress(addr: String) {
        try {
            val uri = Uri.parse("geo:0,0?q=${Uri.encode(addr)}")
            val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                setPackage("com.google.android.apps.maps")
            }

            if (intent.resolveActivity(context.packageManager) == null) {
                val web = Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("https://www.google.com/maps/search/?api=1&query=${Uri.encode(addr)}")
                )
                context.startActivity(web)
            } else {
                context.startActivity(intent)
            }
        } catch (e: Exception) {
            Toast.makeText(context, "지도를 열 수 없습니다.", Toast.LENGTH_SHORT).show()
        }
    }

    fun dialPhone(phoneNumber: String) {
        try {
            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phoneNumber"))
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "전화 앱을 열 수 없습니다.", Toast.LENGTH_SHORT).show()
        }
    }

    fun shareCafe(cafeName: String, addr: String, phone: String) {
        try {
            val sendIntent = Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_TEXT, "$cafeName\n$addr\n전화: $phone")
                type = "text/plain"
            }
            context.startActivity(Intent.createChooser(sendIntent, "공유하기"))
        } catch (e: Exception) {
            Toast.makeText(context, "공유할 수 없습니다.", Toast.LENGTH_SHORT).show()
        }
    }

    fun navigateToReservation() {
        if (navController != null) {
            navController.navigate("reservation")
        } else {
            Toast.makeText(context, "예약 화면으로 이동할 수 없습니다.", Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(listState) {
        snapshotFlow { listState.isScrollInProgress }
            .distinctUntilChanged()
            .filter { inProgress -> !inProgress }
            .collect {
                val info = listState.layoutInfo
                if (info.visibleItemsInfo.isNotEmpty()) {
                    val viewportCenter = info.viewportEndOffset / 2
                    val nearest = info.visibleItemsInfo.minByOrNull { item ->
                        val center = item.offset + item.size / 2
                        abs(center - viewportCenter)
                    }?.index ?: listState.firstVisibleItemIndex

                    coroutineScope.launch {
                        listState.animateScrollToItem(nearest)
                    }
                }
            }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (isLoading) {
            Column(
                modifier = Modifier
                    .align(Alignment.Center)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                CircularProgressIndicator(color = ThemeColors.Primary, modifier = Modifier.size(48.dp))
                Spacer(modifier = Modifier.height(16.dp))
                Text("카페 정보를 불러오는 중입니다...", fontSize = 14.sp, color = ThemeColors.TextSecondary)
            }
        }

        if (error != null && !isLoading) {
            Column(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(16.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "정보를 불러올 수 없습니다",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = ThemeColors.TextPrimary
                )
                Text(
                    text = error ?: "알 수 없는 오류",
                    fontSize = 14.sp,
                    color = ThemeColors.TextSecondary
                )
                Button(
                    onClick = {
                        if (!cafeId.isNullOrEmpty()) {
                            val id = cafeId.toLong()
                            viewModel.loadCafeDetail(id)
                            viewModel.loadCafeSeats(id)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ThemeColors.Primary)
                ) {
                    Text("다시 시도", color = Color.White)
                }
            }
        }

        if (cafeDetail != null && !isLoading && error == null) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(bottom = 96.dp)
            ) {
                val imageHeight = 240.dp

                // Gallery
                Box(modifier = Modifier.fillMaxWidth().height(imageHeight)) {
                    LazyRow(
                        state = listState,
                        contentPadding = PaddingValues(0.dp),
                        horizontalArrangement = Arrangement.spacedBy(0.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(galleryForUi) { url ->
                            Box(modifier = Modifier.width(screenWidth).height(imageHeight)) {
                                AsyncImage(
                                    model = url,
                                    contentDescription = null,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(imageHeight),
                                    contentScale = ContentScale.Crop,
                                    placeholder = painterResource(id = R.drawable.ic_menu_report_image),
                                    error = painterResource(id = R.drawable.ic_menu_report_image)
                                )
                            }
                        }
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(84.dp)
                            .align(Alignment.TopCenter)
                            .background(
                                Brush.verticalGradient(
                                    listOf(Color.Black.copy(alpha = 0.28f), Color.Transparent)
                                )
                            )
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        IconButton(onClick = { navController?.popBackStack() }) {
                            Icon(imageVector = Icons.Filled.Close, contentDescription = "close", tint = Color.White)
                        }
                        IconButton(onClick = { shareCafe(cafeName, address, phone) }) {
                            Icon(imageVector = Icons.Filled.Share, contentDescription = "share", tint = Color.White)
                        }
                    }

                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(start = 16.dp, bottom = 36.dp)
                            .background(Color.White.copy(alpha = 0.95f), RoundedCornerShape(12.dp))
                            .border(1.dp, ThemeColors.Border, RoundedCornerShape(12.dp))
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.PhotoCamera,
                                contentDescription = "photo",
                                tint = ThemeColors.TextPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "${currentPage + 1}/${galleryForUi.size}",
                                color = ThemeColors.TextPrimary,
                                fontSize = 12.sp
                            )
                        }
                    }
                }

                // Content card
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .offset(y = (-20).dp)
                        .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)),
                    color = ThemeColors.Background
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 16.dp)
                    ) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = cafeName,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = ThemeColors.TextPrimary
                        )
                        Spacer(modifier = Modifier.height(SectionGap))

                        // Address + phone + buttons
                        val actionButtonWidth = 104.dp
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Row(verticalAlignment = Alignment.Top) {
                                    Icon(
                                        imageVector = Icons.Filled.LocationOn,
                                        contentDescription = null,
                                        tint = ThemeColors.TextSecondary,
                                        modifier = Modifier
                                            .size(18.dp)
                                            .offset(y = 6.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(text = address, color = ThemeColors.TextSecondary, fontSize = 13.sp)
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                Row(verticalAlignment = Alignment.Top) {
                                    Icon(
                                        imageVector = Icons.Filled.Call,
                                        contentDescription = null,
                                        tint = ThemeColors.TextSecondary,
                                        modifier = Modifier
                                            .size(18.dp)
                                            .offset(y = 4.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(text = phone, color = ThemeColors.TextSecondary, fontSize = 13.sp)
                                }
                            }

                            Column(
                                horizontalAlignment = Alignment.End,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                HtmlOutlinedButtonColored(text = "위치보기", width = actionButtonWidth) {
                                    openMapForAddress(address)
                                }
                                HtmlOutlinedButtonColored(text = "전화하기", width = actionButtonWidth) {
                                    dialPhone(phone)
                                }
                            }
                        }

                        // ✅ 영업시간 (단순 텍스트)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "영업시간: 09:00 ~ 22:00 (연중무휴)",
                            color = ThemeColors.TextPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )

                        Spacer(modifier = Modifier.height(DividerGap))
                        Divider(color = ThemeColors.Border)
                        Spacer(modifier = Modifier.height(DividerGap))

                        // Prices
                        Text(
                            text = "요금",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = ThemeColors.TextPrimary
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        val targetPriceCardHeight = 96.dp
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            contentPadding = PaddingValues(start = 4.dp, end = 16.dp)
                        ) {
                            items(priceOptions) { option ->
                                PriceCardFixedHeight(
                                    label = option.first,
                                    price = option.second,
                                    discountPercent = option.third,
                                    selected = option == priceOptions.first(),
                                    numberFormatter = numberFormatter,
                                    cardHeight = targetPriceCardHeight
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(DividerGap))
                        Divider(color = ThemeColors.Border)
                        Spacer(modifier = Modifier.height(DividerGap))

                        // Seats
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "좌석 현황",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = ThemeColors.TextPrimary
                            )
                            Text(
                                text = "좌석 관리 >",
                                fontSize = 13.sp,
                                color = ThemeColors.Primary,
                                modifier = Modifier.clickable {
                                    Toast.makeText(context, "좌석 관리 (미구현)", Toast.LENGTH_SHORT).show()
                                }
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        SeatStatusCardCentered(used = usedSeats, total = totalSeats)

                        Spacer(modifier = Modifier.height(DividerGap))
                        Divider(color = ThemeColors.Border)
                        Spacer(modifier = Modifier.height(DividerGap))

                        // Facilities
                        Text(
                            text = "편의시설",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = ThemeColors.TextPrimary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        FacilitiesGridCompact(facilities = facilities)

                        Spacer(modifier = Modifier.height(DividerGap))
                        Divider(color = ThemeColors.Border)
                        Spacer(modifier = Modifier.height(DividerGap))

                        // Map preview
                        Text(
                            text = "위치",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = ThemeColors.TextPrimary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        MapPreviewStyled(mapImageUrl = mapImageUrl) { openMapForAddress(address) }

                        Spacer(modifier = Modifier.height(DividerGap))
                        Divider(color = ThemeColors.Border)
                        Spacer(modifier = Modifier.height(DividerGap))

                        // ✅ 소개 섹션 추가
                        Text(
                            text = "소개",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = ThemeColors.TextPrimary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = description,
                            fontSize = 14.sp,
                            color = ThemeColors.TextSecondary,
                            lineHeight = 22.sp
                        )

                        Spacer(modifier = Modifier.height(24.dp))
                    }
                }
            }

            // Bottom fixed action bar
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(96.dp),
                color = ThemeColors.Background
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            isFavorite = !isFavorite
                            Toast.makeText(
                                context,
                                if (isFavorite) "찜 등록됨" else "찜 취소됨",
                                Toast.LENGTH_SHORT
                            ).show()
                        },
                        modifier = Modifier
                            .size(56.dp)
                            .border(1.dp, ThemeColors.Border, RoundedCornerShape(12.dp))
                    ) {
                        if (isFavorite) {
                            Icon(
                                imageVector = Icons.Filled.Favorite,
                                contentDescription = "fav",
                                tint = ThemeColors.Primary
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Outlined.FavoriteBorder,
                                contentDescription = "fav",
                                tint = ThemeColors.TextSecondary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Button(
                        onClick = { navigateToReservation() },
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = ThemeColors.Primary)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(text = "예약하기", color = Color.White, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(
                                imageVector = Icons.Filled.ArrowForward,
                                contentDescription = "go",
                                tint = Color.White
                            )
                        }
                    }
                }
            }
        }
    }
}

/* ================= Helper composables ================= */

@Composable
private fun HtmlOutlinedButtonColored(text: String, width: Dp, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .width(width)
            .height(36.dp)
            .clip(RoundedCornerShape(10.dp))
            .border(1.dp, ThemeColors.Primary.copy(alpha = 0.12f), RoundedCornerShape(10.dp))
            .clickable { onClick() },
        color = ThemeColors.Primary.copy(alpha = 0.08f)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(text = text, color = ThemeColors.Primary, fontSize = 13.sp)
        }
    }
}

@Composable
private fun PriceCardFixedHeight(
    label: String,
    price: Int,
    discountPercent: Int = 0,
    selected: Boolean = false,
    numberFormatter: NumberFormat,
    cardHeight: Dp
) {
    val borderColor = if (selected) ThemeColors.Primary else ThemeColors.Border

    Card(
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
            .width(140.dp)
            .height(cardHeight)
            .shadow(if (selected) 6.dp else 0.dp, RoundedCornerShape(14.dp))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxSize()
                .background(Color.White)
                .border(
                    width = if (selected) 2.dp else 1.dp,
                    color = borderColor,
                    shape = RoundedCornerShape(14.dp)
                )
                .padding(12.dp)
        ) {
            if (discountPercent > 0) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .background(ThemeColors.Primary, RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "-$discountPercent%",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .align(Alignment.CenterStart),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = label, fontSize = 12.sp, color = ThemeColors.TextSecondary)

                Text(
                    text = "₩${numberFormatter.format(price)}",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = ThemeColors.TextPrimary
                )

                if (discountPercent > 0) {
                    val original = (price * 100) / (100 - discountPercent)
                    Text(
                        text = "₩${numberFormatter.format(original)}",
                        fontSize = 12.sp,
                        color = ThemeColors.TextSecondary,
                        textDecoration = TextDecoration.LineThrough
                    )
                } else {
                    Spacer(modifier = Modifier.height(0.dp))
                }
            }
        }
    }
}

@Composable
private fun FacilitiesGridCompact(facilities: List<EFacility>) {
    val itemsPerRow = 2
    Column {
        val rows = (facilities.size + itemsPerRow - 1) / itemsPerRow
        for (r in 0 until rows) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(bottom = 8.dp)
            ) {
                val start = r * itemsPerRow
                for (c in 0 until itemsPerRow) {
                    val index = start + c
                    if (index < facilities.size) {
                        val key = facilities[index]
                        FacilityChipCompact(key = key, modifier = Modifier.weight(1f))
                    } else {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun FacilityChipCompact(key: EFacility?, modifier: Modifier = Modifier) {
    if (key == null) return // ✅ 안전장치: key가 null이면 렌더링 스킵

    Card(
        modifier = modifier.wrapContentHeight(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = ThemeColors.Gray100)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start
        ) {
            val iconTint = ThemeColors.Primary

            // ✅ 안전하게 key 참조 (NPE 해결)
            val (icon, label) = when (key) {
                EFacility.WIFI -> Icons.Filled.Wifi to "WiFi 무료"
                EFacility.CAFE -> Icons.Filled.LocalCafe to "카페/음료"
                EFacility.PRINTER -> Icons.Filled.Print to "프린트 가능"
                EFacility.MEETINGROOM -> Icons.Filled.MeetingRoom to "회의실"
                EFacility.LOCKER -> Icons.Filled.Lock to "사물함"
                EFacility.OPEN_24H -> Icons.Filled.AccessTime to "24시"
                EFacility.AIRCONDITION -> Icons.Filled.AcUnit to "에어컨"
                else -> Icons.Filled.Info to (key.name ?: "기타") // ✅ null safety
            }

            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = iconTint,
                modifier = Modifier.size(22.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Text(
                text = label,
                color = ThemeColors.TextPrimary,
                fontSize = 14.sp
            )
        }
    }
}

@Composable
private fun SeatStatusCardCentered(used: Int, total: Int) {
    val avail = (total - used).coerceAtLeast(0)
    val usedFraction =
        if (total > 0) (used.toFloat() / total.toFloat()).coerceIn(0f, 1f) else 0f

    Card(
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            val circleSize = 84.dp
            Box(modifier = Modifier.size(circleSize), contentAlignment = Alignment.Center) {
                Canvas(modifier = Modifier.matchParentSize()) {
                    val stroke = 8.dp.toPx()

                    drawArc(
                        color = ThemeColors.Gray200,
                        startAngle = 0f,
                        sweepAngle = 360f,
                        useCenter = false,
                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = stroke, cap = StrokeCap.Round)
                    )

                    drawArc(
                        color = ThemeColors.Primary,
                        startAngle = -90f,
                        sweepAngle = 360f * usedFraction,
                        useCenter = false,
                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = stroke, cap = StrokeCap.Round)
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "$avail",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = ThemeColors.TextPrimary
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(text = "잔여석", fontSize = 12.sp, color = ThemeColors.TextSecondary)
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.Start,
                modifier = Modifier.wrapContentHeight()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(ThemeColors.Primary)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "사용 중: ${used}석", fontSize = 14.sp, color = ThemeColors.TextPrimary)
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(ThemeColors.Gray200)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "사용 가능: ${avail}석", fontSize = 14.sp, color = ThemeColors.TextSecondary)
                }
            }
        }
    }
}

@Composable
private fun MapPreviewStyled(mapImageUrl: String, onOpenMap: () -> Unit) {
    Card(shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
        Box {
            AsyncImage(
                model = mapImageUrl,
                contentDescription = null,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
                    .clip(RoundedCornerShape(12.dp)),
                contentScale = ContentScale.Crop,
                placeholder = painterResource(id = R.drawable.ic_menu_report_image),
                error = painterResource(id = R.drawable.ic_menu_report_image)
            )

            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.95f))
                    .shadow(8.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.LocationOn,
                    contentDescription = null,
                    tint = ThemeColors.Primary,
                    modifier = Modifier.size(32.dp)
                )
            }

            Button(
                onClick = { onOpenMap() },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = ThemeColors.Primary)
            ) {
                Icon(imageVector = Icons.Filled.Map, contentDescription = null, tint = Color.White)
                Spacer(modifier = Modifier.width(6.dp))
                Text(text = "지도에서 보기", color = Color.White)
            }
        }
    }
}
