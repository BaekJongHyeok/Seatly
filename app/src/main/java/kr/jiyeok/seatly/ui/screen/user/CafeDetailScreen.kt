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
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EventSeat
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.LocalCafe
import androidx.compose.material.icons.filled.LocalParking
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.abs

// Theme/colors
private object ThemeColors {
    val Primary = Color(0xFFFF6633)
    val Background = Color(0xFFFFFFFF)
    val SurfaceCard = Color(0xFFF8F8F8)
    val TextPrimary = Color(0xFF1A1A1A)
    val TextSecondary = Color(0xFF888888)
    val Border = Color(0xFFEFEFEF)
    val Gray100 = Color(0xFFF6F6F8)
    val Gray200 = Color(0xFFEDEDED)
}

// Unified spacing constants
private val DividerGap = 20.dp   // space above and below every Divider (unified)
private val SectionGap = 24.dp   // space between major sections

@Composable
fun CafeDetailScreen(navController: NavHostController?, cafeId: String?) {
    val context = LocalContext.current
    val numberFormatter = NumberFormat.getNumberInstance(Locale.KOREA)
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp

    // sample data (replace with real)
    val gallery = listOf(
        "https://lh3.googleusercontent.com/aida-public/AB6AXuAYf3FYoVTWbP4naNk-5bVX3PQG44xBGSAFgHuLHjGsK_sDxxIm_2MQhGnL6wnFmSwV1hDT4wNxoNtfxjqOduIVTC3zuVXS5rsSuSBPhoVj3Te8b_WVuOYMT8cPgObriBOcXAPPDqGlEP6wMJF-YLbVilcb3o_n6wX_pdQVMgn2Qj5pPHpV7Oo7kLzhlMgS8GD9gEjvd98KBMcO1SFVEvopEbDY6zpsmckDMS8DdipjO8tL4B371NUU8uNoYPrnv5bR0TnpIr9BYqU",
        "https://lh3.googleusercontent.com/aida-public/AB6AXuAfJQjvyf2Fy2Q2GDG0rkdI5MPKBltZYsBJV_yiGtHiVULcmI27zpR2MUn_TtzWkJQgLZH8lyPobHWIlG3G24nAcFOJTW3P90Y0sHJfa7-phAmMKMz_dqK7aYvRPTaHkWRBsuTAkNtG7X7MB2CPTc_7zJHqKDiJrXiB2I_pK0TXE2-sYkU7IDGS6x4xA00BQOi3IZ1c3xAeAYYJZN5pwwtU6NeblSpmQAodAmpR43VCfHALUNE4-WC9IxAveCB6srxxYZELUoXXOyk",
        "https://lh3.googleusercontent.com/aida-public/AB6AXuAXFyexJQ9snhmWLAWe3E-K4yy0uG0WnemrrjKKdjpvFe2h9P8-_rVa7p2zbOgt5Vo3mrgYeMyunuD1maA7WN_8WIdmUjr66IEj7akVFfBXDhZIJoqS7NQ_FsMT14QW0SeTkBPOwZ65S-wJ_0exmF5ALmvSDi1nd5XsJix6c2dFlmK2FoXtvg2hXzyoEyZPkWD5_sVZzWTxzQNkE5Ov48jgAuK3vK6gKGfif2OSlCdual2d4LN0RMpF44Arqap79Topuuplt69zTCg"
    )
    val mapImageUrl =
        "https://maps.googleapis.com/maps/api/staticmap?center=37.5665,126.9780&zoom=13&size=600x300&markers=color:orange%7Clabel:%7C37.5665,126.9780"

    // sample details
    val cafeName = "명지 스터디카페"
    val rating = 4.8f
    val reviewCount = 156
    val address = "서울시 강서구 마곡로 100\n마곡 빌딩 3층"
    val phone = "02-1234-5678"
    val openStatus = "영업중"
    val openInfo = "24시간 운영"
    val priceOptions = listOf(
        Triple("1시간", 4500, 10),
        Triple("4시간", 17000, 0),
        Triple("8시간", 32000, 0)
    )
    val facilities = listOf("wifi", "electrical_services", "local_cafe", "local_parking", "local_printshop", "wc")
    // Example used/total — you can replace with real values
    val usedSeats = 14
    val totalSeats = 56

    val listState = rememberLazyListState()
    val currentPage by remember { derivedStateOf { listState.firstVisibleItemIndex.coerceIn(0, maxOf(0, gallery.size - 1)) } }
    val coroutineScope = rememberCoroutineScope()

    // interactive states
    var isFavorite by remember { mutableStateOf(false) }
    var showHoursDialog by remember { mutableStateOf(false) }

    // scroll state for whole column (we need this to programmatically scroll)
    val scrollState = rememberScrollState()

    // positions for computing scroll offset to "운영 정보"
    var containerTop by remember { mutableStateOf(0f) }
    var infoSectionTop by remember { mutableStateOf(0f) }

    // helpers: intents
    fun openMapForAddress(addr: String) {
        try {
            val uri = Uri.parse("geo:0,0?q=${Uri.encode(addr)}")
            val intent = Intent(Intent.ACTION_VIEW, uri)
            intent.setPackage("com.google.android.apps.maps")
            if (intent.resolveActivity(context.packageManager) == null) {
                // fallback to web maps
                val web = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/maps/search/?api=1&query=${Uri.encode(addr)}"))
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
            val chooser = Intent.createChooser(sendIntent, "공유하기")
            context.startActivity(chooser)
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

    // snap to nearest image when scrolling stops (keeps one image per page behavior)
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .onGloballyPositioned { coords ->
                    // record top of the scrollable container in window coordinates
                    containerTop = coords.positionInWindow().y
                }
                .padding(bottom = 96.dp)
        ) {
            val imageHeight = 240.dp

            // Gallery — each item given exact screen width so one image shows per page.
            Box(modifier = Modifier.fillMaxWidth().height(imageHeight)) {
                LazyRow(
                    state = listState,
                    contentPadding = PaddingValues(0.dp),
                    horizontalArrangement = Arrangement.spacedBy(0.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(gallery) { url ->
                        Box(modifier = Modifier.width(screenWidth).height(imageHeight)) {
                            AsyncImage(
                                model = url,
                                contentDescription = null,
                                modifier = Modifier.fillMaxWidth().height(imageHeight),
                                contentScale = ContentScale.Crop,
                                placeholder = painterResource(id = R.drawable.ic_menu_report_image),
                                error = painterResource(id = R.drawable.ic_menu_report_image)
                            )

                            // top gradient for icon visibility
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(84.dp)
                                    .align(Alignment.TopCenter)
                                    .background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.28f), Color.Transparent)))
                            )
                        }
                    }
                }

                // Top controls (close left, badge+share right)
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

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .height(28.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(ThemeColors.Primary),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = "특가 할인", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 10.dp))
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        IconButton(onClick = { shareCafe(cafeName, address, phone) }) {
                            Icon(imageVector = Icons.Filled.Share, contentDescription = "share", tint = Color.White)
                        }
                    }
                }

                // page indicator bottom-left
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(start = 16.dp, bottom = 36.dp)
                        .background(Color.White.copy(alpha = 0.95f), androidx.compose.foundation.shape.RoundedCornerShape(12.dp))
                        .border(1.dp, ThemeColors.Border, androidx.compose.foundation.shape.RoundedCornerShape(12.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Filled.PhotoCamera, contentDescription = "photo", tint = ThemeColors.TextPrimary, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = "${currentPage + 1}/${gallery.size}", color = ThemeColors.TextPrimary, fontSize = 12.sp)
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
                Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 16.dp)) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = cafeName, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = ThemeColors.TextPrimary)
                    Spacer(modifier = Modifier.height(12.dp))

                    // Rating
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Filled.Star, contentDescription = null, tint = ThemeColors.Primary, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = String.format("%.1f", rating), fontWeight = FontWeight.Bold, fontSize = 19.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "(${reviewCount}개 리뷰)", color = ThemeColors.TextSecondary, fontSize = 13.sp)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(text = "평가하기", color = ThemeColors.Primary, fontSize = 13.sp, modifier = Modifier.clickable {
                            Toast.makeText(context, "평가하기 (미구현)", Toast.LENGTH_SHORT).show()
                        })
                    }

                    Spacer(modifier = Modifier.height(SectionGap))

                    // Address + buttons
                    val actionButtonWidth = 104.dp
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column {
                            Row(verticalAlignment = Alignment.Top) {
                                Icon(imageVector = Icons.Filled.LocationOn, contentDescription = null, tint = ThemeColors.TextSecondary, modifier = Modifier.size(18.dp).offset(y = 6.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(text = address, color = ThemeColors.TextSecondary, fontSize = 13.sp)
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Row(verticalAlignment = Alignment.Top) {
                                Icon(imageVector = Icons.Filled.Call, contentDescription = null, tint = ThemeColors.TextSecondary, modifier = Modifier.size(18.dp).offset(y = 4.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(text = phone, color = ThemeColors.TextSecondary, fontSize = 13.sp)
                            }
                        }

                        Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            HtmlOutlinedButtonColored(text = "위치보기", width = actionButtonWidth) { openMapForAddress(address) }
                            HtmlOutlinedButtonColored(text = "전화하기", width = actionButtonWidth) { dialPhone(phone) }
                        }
                    }

                    Spacer(modifier = Modifier.height(SectionGap))

                    // Open status + Hours button
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .height(22.dp)
                                    .clip(androidx.compose.foundation.shape.RoundedCornerShape(6.dp))
                                    .background(ThemeColors.Primary),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 6.dp)) {
                                    Icon(imageVector = Icons.Filled.Schedule, contentDescription = null, tint = Color.White, modifier = Modifier.size(12.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(text = openStatus, color = Color.White, fontSize = 11.sp)
                                }
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            Text(text = openInfo, fontWeight = FontWeight.Medium, fontSize = 13.sp)
                        }

                        // When pressed: scroll to 운영 정보 section (and optionally show dialog)
                        HtmlOutlinedButtonColored(text = "영업시간", width = actionButtonWidth) {
                            // bring the "운영 정보" area into view by computing positions and animating scroll
                            coroutineScope.launch {
                                // compute offset relative to scroll container
                                val offset = (infoSectionTop - containerTop).coerceAtLeast(0f).toInt()
                                scrollState.animateScrollTo(offset)
                            }
                        }
                    }

                    // hours dialog (still available via other UI if needed)
                    if (showHoursDialog) {
                        AlertDialog(
                            onDismissRequest = { showHoursDialog = false },
                            confirmButton = {
                                Button(onClick = { showHoursDialog = false }) {
                                    Text("확인")
                                }
                            },
                            title = { Text("영업시간") },
                            text = { Text("월-금: 06:00 - 02:00\n토-일: 08:00 - 02:00") }
                        )
                    }

                    // unified roomy divider spacing everywhere
                    Spacer(modifier = Modifier.height(DividerGap))
                    Divider(color = ThemeColors.Border)
                    Spacer(modifier = Modifier.height(DividerGap))

                    // Prices
                    Text(text = "요금", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = ThemeColors.TextPrimary)
                    Spacer(modifier = Modifier.height(8.dp))

                    val TARGET_PRICE_CARD_HEIGHT = 96.dp
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
                                cardHeight = TARGET_PRICE_CARD_HEIGHT
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(DividerGap))
                    Divider(color = ThemeColors.Border)
                    Spacer(modifier = Modifier.height(DividerGap))

                    // =========================
                    // 좌석 현황 섹션 — 링과 레전드를 카드 중앙으로 더 가깝게 정렬
                    // =========================

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text(text = "좌석 현황", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = ThemeColors.TextPrimary)
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

                    // Amenities (편의시설)
                    Text(text = "편의시설", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = ThemeColors.TextPrimary)
                    Spacer(modifier = Modifier.height(8.dp))

                    FacilitiesGridCompact(facilities = facilities)

                    Spacer(modifier = Modifier.height(DividerGap))
                    Divider(color = ThemeColors.Border)
                    Spacer(modifier = Modifier.height(DividerGap))

                    // Map preview (button opens external maps)
                    Text(text = "위치", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = ThemeColors.TextPrimary)
                    Spacer(modifier = Modifier.height(8.dp))
                    MapPreviewStyled(mapImageUrl = mapImageUrl) { openMapForAddress(address) }

                    Spacer(modifier = Modifier.height(DividerGap))
                    Divider(color = ThemeColors.Border)
                    Spacer(modifier = Modifier.height(DividerGap))

                    // 운영 정보 — attach onGloballyPositioned so we can scroll to it
                    Text(
                        text = "운영 정보",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.onGloballyPositioned { coords ->
                            // record the top of this section in window coordinates
                            infoSectionTop = coords.positionInWindow().y
                        }
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(text = "월-금: 06:00 - 02:00\n토-일: 08:00 - 02:00", color = ThemeColors.TextSecondary, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }

        // Bottom fixed action bar (favorite + reserve)
        Surface(
            modifier = Modifier.fillMaxWidth().height(96.dp).align(Alignment.BottomCenter),
            color = ThemeColors.Background
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = {
                        isFavorite = !isFavorite
                        Toast.makeText(context, if (isFavorite) "찜 등록됨" else "찜 취소됨", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier
                        .size(56.dp)
                        .border(1.dp, ThemeColors.Border, androidx.compose.foundation.shape.RoundedCornerShape(12.dp))
                ) {
                    if (isFavorite) {
                        Icon(imageVector = Icons.Filled.Favorite, contentDescription = "fav", tint = ThemeColors.Primary)
                    } else {
                        Icon(imageVector = Icons.Outlined.FavoriteBorder, contentDescription = "fav", tint = ThemeColors.TextSecondary)
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Button(
                    onClick = { navigateToReservation() },
                    modifier = Modifier.weight(1f).height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ThemeColors.Primary)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                        Text(text = "예약하기", color = Color.White, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(imageVector = Icons.Filled.ArrowForward, contentDescription = "go", tint = Color.White)
                    }
                }
            }
        }
    }
}

/* ----------------- Helper composables ----------------- */

@Composable
private fun HtmlOutlinedButtonColored(text: String, width: Dp, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .width(width)
            .height(36.dp)
            .clip(androidx.compose.foundation.shape.RoundedCornerShape(10.dp))
            .border(1.dp, ThemeColors.Primary.copy(alpha = 0.12f), androidx.compose.foundation.shape.RoundedCornerShape(10.dp))
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
        shape = androidx.compose.foundation.shape.RoundedCornerShape(14.dp),
        modifier = Modifier
            .width(140.dp)
            .height(cardHeight)
            .shadow(if (selected) 6.dp else 0.dp, androidx.compose.foundation.shape.RoundedCornerShape(14.dp))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight()
                .background(Color.White)
                .border(width = if (selected) 2.dp else 1.dp, color = borderColor, shape = androidx.compose.foundation.shape.RoundedCornerShape(14.dp))
                .padding(12.dp)
        ) {
            if (discountPercent > 0) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .background(ThemeColors.Primary, androidx.compose.foundation.shape.RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(text = "-${discountPercent}%", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxHeight()
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
private fun FacilitiesGridCompact(facilities: List<String>) {
    val itemsPerRow = 2
    Column {
        val rows = (facilities.size + itemsPerRow - 1) / itemsPerRow
        for (r in 0 until rows) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.padding(bottom = 8.dp)) {
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
private fun FacilityChipCompact(key: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.wrapContentHeight(),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
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
            when (key) {
                "wifi" -> Icon(imageVector = Icons.Filled.Wifi, contentDescription = key, tint = iconTint, modifier = Modifier.size(22.dp))
                "electrical_services" -> Icon(imageVector = Icons.Filled.LocalParking, contentDescription = key, tint = iconTint, modifier = Modifier.size(22.dp))
                "local_cafe" -> Icon(imageVector = Icons.Filled.LocalCafe, contentDescription = key, tint = iconTint, modifier = Modifier.size(22.dp))
                "local_parking" -> Icon(imageVector = Icons.Filled.LocalParking, contentDescription = key, tint = iconTint, modifier = Modifier.size(22.dp))
                "local_printshop", "printer" -> Icon(imageVector = Icons.Filled.Map, contentDescription = key, tint = iconTint, modifier = Modifier.size(22.dp))
                "wc" -> Icon(imageVector = Icons.Filled.EventSeat, contentDescription = key, tint = iconTint, modifier = Modifier.size(22.dp))
                else -> Icon(imageVector = Icons.Filled.Map, contentDescription = key, tint = iconTint, modifier = Modifier.size(22.dp))
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = when (key) {
                    "wifi" -> "WiFi 무료"
                    "electrical_services" -> "콘센트 완비"
                    "local_cafe" -> "음료 무한제공"
                    "local_parking" -> "주차"
                    "local_printshop", "printer" -> "프린트 가능"
                    "wc" -> "화장실 구분"
                    else -> key
                },
                color = ThemeColors.TextPrimary,
                fontSize = 14.sp
            )
        }
    }
}

/**
 * 중앙 정렬된 좌석 상태 카드: 링 + 레전드를 카드 중앙에 가깝게 배치
 */
@Composable
private fun SeatStatusCardCentered(used: Int, total: Int) {
    val avail = (total - used).coerceAtLeast(0)
    val usedFraction = if (total > 0) (used.toFloat() / total.toFloat()).coerceIn(0f, 1f) else 0f

    Card(
        shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        // 전체 Row를 중앙 정렬로 바꿔 링과 레전드를 카드 중앙으로 이동
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            // Left: circular progress + center text (좀 더 작게 해서 중앙에 가깝게)
            val circleSize = 84.dp
            Box(modifier = Modifier.size(circleSize), contentAlignment = Alignment.Center) {
                Canvas(modifier = Modifier.matchParentSize()) {
                    val stroke = 8.dp.toPx()
                    // background ring
                    drawArc(
                        color = ThemeColors.Gray200,
                        startAngle = 0f,
                        sweepAngle = 360f,
                        useCenter = false,
                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = stroke, cap = StrokeCap.Round)
                    )
                    // used arc (orange)
                    drawArc(
                        color = ThemeColors.Primary,
                        startAngle = -90f,
                        sweepAngle = 360f * usedFraction,
                        useCenter = false,
                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = stroke, cap = StrokeCap.Round)
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = "$avail", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = ThemeColors.TextPrimary)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(text = "잔여석", fontSize = 12.sp, color = ThemeColors.TextSecondary)
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            // Right: legend (wrap content, 카드 중앙에 가깝게 위치)
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
private fun SeatProgressWithAvailable(used: Int, total: Int) {
    // 기존 구현은 보존 (다른 곳에서 참조될 수 있으므로), 하지만 이제 메인 화면에서는 SeatStatusCardCentered를 사용합니다.
    val usedPerc = (used.toFloat() / total.toFloat()).coerceIn(0f, 1f)
    val avail = total - used
    val availPerc = (avail.toFloat() / total.toFloat()).coerceIn(0f, 1f)
    Column {
        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            Text(text = "사용 중 (${used}석)", color = ThemeColors.TextSecondary, fontSize = 12.sp)
            Text(text = "전체 ${total}석", color = ThemeColors.TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
        }
        Spacer(modifier = Modifier.height(8.dp))
        Box(modifier = Modifier.fillMaxWidth().height(8.dp).clip(androidx.compose.foundation.shape.RoundedCornerShape(6.dp)).background(ThemeColors.Gray200)) {
            Box(modifier = Modifier.fillMaxWidth(fraction = usedPerc).height(8.dp).background(ThemeColors.Primary))
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            Text(text = "사용 가능", color = ThemeColors.TextSecondary, fontSize = 12.sp)
            Text(text = "${avail}석", color = ThemeColors.Primary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
        }
        Spacer(modifier = Modifier.height(6.dp))
        Box(modifier = Modifier.fillMaxWidth().height(6.dp).clip(androidx.compose.foundation.shape.RoundedCornerShape(6.dp)).background(ThemeColors.Gray100)) {
            Box(modifier = Modifier.fillMaxWidth(fraction = availPerc).height(6.dp).background(ThemeColors.Gray200))
        }
    }
}

@Composable
private fun MapPreviewStyled(mapImageUrl: String, onOpenMap: () -> Unit) {
    Card(shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
        Box {
            AsyncImage(
                model = mapImageUrl,
                contentDescription = null,
                modifier = Modifier.fillMaxWidth().height(160.dp).clip(androidx.compose.foundation.shape.RoundedCornerShape(12.dp)),
                contentScale = ContentScale.Crop,
                placeholder = painterResource(id = R.drawable.ic_menu_report_image),
                error = painterResource(id = R.drawable.ic_menu_report_image)
            )

            Box(modifier = Modifier.align(Alignment.Center).size(56.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.95f)).shadow(8.dp), contentAlignment = Alignment.Center) {
                Icon(imageVector = Icons.Filled.LocationOn, contentDescription = null, tint = ThemeColors.Primary, modifier = Modifier.size(32.dp))
            }

            Button(onClick = { onOpenMap() }, modifier = Modifier.align(Alignment.BottomCenter).padding(12.dp), colors = ButtonDefaults.buttonColors(containerColor = ThemeColors.Primary)) {
                Icon(imageVector = Icons.Filled.Map, contentDescription = null, tint = Color.White)
                Spacer(modifier = Modifier.width(6.dp))
                Text(text = "지도에서 보기", color = Color.White)
            }
        }
    }
}