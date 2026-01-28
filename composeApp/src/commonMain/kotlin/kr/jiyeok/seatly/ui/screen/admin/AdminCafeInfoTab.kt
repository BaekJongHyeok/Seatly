package kr.jiyeok.seatly.ui.screen.admin

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.Image
import androidx.compose.foundation.ScrollState
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import kr.jiyeok.seatly.data.remote.enums.EFacility
import kr.jiyeok.seatly.data.remote.response.UsageDto
import kr.jiyeok.seatly.presentation.viewmodel.AdminCafeDetailViewModel
import kr.jiyeok.seatly.ui.theme.*
import kr.jiyeok.seatly.ui.component.common.MapView
import org.jetbrains.compose.resources.painterResource
import seatly.composeapp.generated.resources.Res
import seatly.composeapp.generated.resources.img_default_cafe

@Composable
fun AdminCafeInfoTab(
    viewModel: AdminCafeDetailViewModel,
    navController: NavController
) {
    val uiState by viewModel.uiState.collectAsState()

    var selectedTab by remember { mutableIntStateOf(0) }
    val scrollState = rememberScrollState()

    val isContentReady = remember(
        uiState.cafeInfo,
        uiState.cafeUsage,
        uiState.isLoadingInfo,
        uiState.isLoadingUsage,
        uiState.images
    ) {
        val hasCafeInfo = uiState.cafeInfo != null
        val notLoadingInfo = !uiState.isLoadingInfo
        val notLoadingUsage = !uiState.isLoadingUsage

        val imageUrls = uiState.cafeInfo?.imageUrls ?: emptyList()
        val allImagesLoaded = if (imageUrls.isEmpty()) {
            true
        } else {
            imageUrls.all { imageId ->
                uiState.images.containsKey(imageId)
            }
        }

        hasCafeInfo && notLoadingInfo && notLoadingUsage && allImagesLoaded
    }

    Box(modifier = Modifier.fillMaxSize()) {
        when {
            !isContentReady -> {
                LoadingView()
            }
            else -> {
                CafeInfoContent(
                    uiState = uiState,
                    selectedTab = selectedTab,
                    onTabSelected = { selectedTab = it },
                    scrollState = scrollState,
                    navController = navController
                )
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
                text = "카페 정보 불러오는 중...",
                fontSize = 14.sp,
                color = ColorTextGray,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun BoxScope.CafeInfoContent(
    uiState: AdminCafeDetailViewModel.CafeDetailUiState,
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    scrollState: ScrollState,
    navController: NavController
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(bottom = 100.dp)
    ) {
        val gallery = remember(uiState.cafeInfo?.imageUrls) {
            if (uiState.cafeInfo?.imageUrls.isNullOrEmpty()) {
                listOf("")
            } else {
                uiState.cafeInfo?.imageUrls!!
            }
        }

        HeaderSection(
            images = gallery,
            imageBitmapCache = uiState.images
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .offset(y = (-40).dp)
                .padding(horizontal = 20.dp)
        ) {
            InfoMainCard(
                name = uiState.cafeInfo?.name ?: "카페 이름",
                address = uiState.cafeInfo?.address ?: "카페 주소",
                phone = uiState.cafeInfo?.phone ?: "카페 대표 번호",
                operatingHours = uiState.cafeInfo?.openingHours,
                selectedTab = selectedTab,
                onTabSelected = onTabSelected
            )

            Spacer(modifier = Modifier.height(24.dp))

            Crossfade(
                targetState = selectedTab,
                label = "tab_content_animation"
            ) { tab ->
                if (tab == 0) {
                    FeeSection()
                } else {
                    SeatStatusSection(uiState.cafeUsage)
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            FacilitiesSection(uiState.cafeInfo?.facilities ?: emptyList())

            Spacer(modifier = Modifier.height(32.dp))

            LocationSection(
                address = uiState.cafeInfo?.address ?: "",
                cafeName = uiState.cafeInfo?.name ?: "카페"
            )
        }
    }

    FloatingActionButton(
        onClick = {
            uiState.cafeInfo?.id?.let { cafeId ->
                navController.navigate("admin/cafe/update/$cafeId")
            }
        },
        modifier = Modifier
            .align(Alignment.BottomEnd)
            .padding(end = 20.dp, bottom = 20.dp),
        containerColor = ColorPrimaryOrange,
        contentColor = ColorWhite
    ) {
        Icon(
            imageVector = Icons.Default.Edit,
            contentDescription = "카페 편집"
        )
    }
}

@Composable
private fun HeaderSection(
    images: List<String>,
    imageBitmapCache: Map<String, ImageBitmap>
) {
    val listState = rememberLazyListState()
    val currentIndex = remember {
        derivedStateOf { listState.firstVisibleItemIndex + 1 }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(240.dp)
    ) {
        LazyRow(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(bottomStart = 40.dp, bottomEnd = 40.dp))
        ) {
            items(images) { imageId ->
                if (imageId.isNotEmpty()) {
                    val bitmap = imageBitmapCache[imageId]
                    if (bitmap != null) {
                        Image(
                            bitmap = bitmap,
                            contentDescription = "카페 이미지",
                            modifier = Modifier
                                .fillParentMaxWidth()
                                .fillMaxHeight(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Image(
                            painter = painterResource(Res.drawable.img_default_cafe),
                            contentDescription = "기본 카페 이미지",
                            modifier = Modifier
                                .fillParentMaxWidth()
                                .fillMaxHeight(),
                            contentScale = ContentScale.Crop
                        )
                    }
                } else {
                    Image(
                        painter = painterResource(Res.drawable.img_default_cafe),
                        contentDescription = "기본 카페 이미지",
                        modifier = Modifier
                            .fillParentMaxWidth()
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
                text = "${currentIndex.value} / ${images.size}",
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = ColorTextBlack
            )
        }
    }
}

@Composable
private fun InfoMainCard(
    name: String,
    address: String,
    phone: String,
    operatingHours: String?,
    selectedTab: Int,
    onTabSelected: (Int) -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(2.dp, RoundedCornerShape(24.dp)),
        shape = RoundedCornerShape(24.dp),
        color = ColorBgBeige
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Text(
                text = name,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = ColorTextBlack
            )

            Spacer(modifier = Modifier.height(20.dp))

            InfoRow(Icons.Filled.LocationOn, address)
            HorizontalDivider(
                modifier = Modifier.padding(vertical = 12.dp),
                color = ColorBorderLight
            )

            InfoRow(Icons.Filled.Call, phone)
            HorizontalDivider(
                modifier = Modifier.padding(vertical = 12.dp),
                color = ColorBorderLight
            )

            InfoRow(
                icon = Icons.Filled.Schedule,
                text = operatingHours ?: "운영시간 정보 없음"
            )

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .background(Color(0xFFF0F0F0), RoundedCornerShape(20.dp))
                    .padding(4.dp)
            ) {
                TabButton(
                    text = "요금 정보",
                    isSelected = selectedTab == 0,
                    modifier = Modifier.weight(1f)
                ) {
                    onTabSelected(0)
                }

                TabButton(
                    text = "좌석 현황",
                    isSelected = selectedTab == 1,
                    modifier = Modifier.weight(1f)
                ) {
                    onTabSelected(1)
                }
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
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = ColorPrimaryOrange,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        Text(
            text = text,
            fontSize = 14.sp,
            color = ColorTextGray,
            lineHeight = 20.sp
        )
    }
}

@Composable
private fun TabButton(
    text: String,
    isSelected: Boolean,
    modifier: Modifier,
    onClick: () -> Unit
) {
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
    val fees = listOf(
        "1시간" to 4500,
        "4시간" to 17000,
        "8시간" to 32000,
        "회원권" to 80000
    )

    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 10.dp)
    ) {
        items(fees) { (label, price) ->
            val formattedPrice = formatPrice(price)
            val formattedStruckPrice = formatPrice(price + 1000)
            
            Surface(
                modifier = Modifier
                    .size(160.dp, 130.dp)
                    .shadow(1.dp, RoundedCornerShape(20.dp)),
                shape = RoundedCornerShape(20.dp),
                color = ColorBgBeige
            ) {
                Column(
                    modifier = Modifier
                        .padding(20.dp)
                        .fillMaxSize(),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = label,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = ColorTextBlack
                    )

                    Column {
                        Text(
                            text = "₩$formattedPrice",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = ColorPrimaryOrange
                        )

                        Text(
                            text = "₩$formattedStruckPrice",
                            fontSize = 13.sp,
                            color = ColorTextGray,
                            textDecoration = TextDecoration.LineThrough
                        )
                    }
                }
            }
        }
    }
}

private fun formatPrice(price: Int): String {
    val s = price.toString()
    if (s.length <= 3) return s
    val res = StringBuilder()
    var count = 0
    for (i in s.length - 1 downTo 0) {
        res.append(s[i])
        count++
        if (count == 3 && i != 0) {
            res.append(',')
            count = 0
        }
    }
    return res.reverse().toString()
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
        SeatProgressRow(
            label = "사용 중",
            value = "$used / $total",
            ratio = usedRatio,
            color = ColorPrimaryOrange
        )

        Spacer(modifier = Modifier.height(20.dp))

        SeatProgressRow(
            label = "사용 가능",
            value = "$available 석",
            ratio = 1f - usedRatio,
            color = Color(0xFFE0E0E0)
        )
    }
}

@Composable
private fun SeatProgressRow(
    label: String,
    value: String,
    ratio: Float,
    color: Color
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = label,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = ColorTextBlack
            )

            Text(
                text = value,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = ColorTextBlack
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        LinearProgressIndicator(
            progress = { ratio },
            modifier = Modifier
                .fillMaxWidth()
                .height(12.dp)
                .clip(CircleShape),
            color = color,
            trackColor = ColorWhite
        )
    }
}

@Composable
private fun FacilitiesSection(facilities: List<EFacility>) {
    Text(
        text = "편의시설",
        fontSize = 16.sp,
        fontWeight = FontWeight.Bold,
        color = ColorTextBlack,
        modifier = Modifier.padding(start = 4.dp)
    )

    Spacer(modifier = Modifier.height(16.dp))

    if (facilities.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(ColorBgBeige, RoundedCornerShape(16.dp))
                .padding(vertical = 40.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "등록된 편의시설이 없습니다",
                fontSize = 14.sp,
                color = ColorTextGray,
                fontWeight = FontWeight.Medium
            )
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
        EFacility.WIFI -> Icons.Default.Wifi to "WiFi"
        EFacility.CAFE -> Icons.Default.LocalCafe to "카페"
        EFacility.PRINTER -> Icons.Default.Print to "프린트"
        EFacility.OPEN_24H -> Icons.Default.Schedule to "24시간"
        EFacility.OUTLET -> Icons.Default.Outlet to "콘센트"
        EFacility.MEETING_ROOM -> Icons.Default.MeetingRoom to "미팅룸"
        EFacility.LOCKER -> Icons.Default.Inventory2 to "사물함"
        EFacility.AIR_CONDITIONING -> Icons.Default.AcUnit to "에어컨"
        EFacility.PARKING -> Icons.Default.LocalParking to "주차"
        EFacility.STUDY_ROOM -> Icons.Default.MeetingRoom to "스터디룸"
        EFacility.SMOKING_ROOM -> Icons.Default.SmokingRooms to "흡연실"
        EFacility.LOUNGE -> Icons.Default.Weekend to "라운지"
    }

    Surface(
        modifier = modifier.shadow(1.dp, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        color = ColorWhite
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = ColorPrimaryOrange,
                modifier = Modifier.size(20.dp)
            )

            Spacer(modifier = Modifier.width(10.dp))

            Text(
                text = label,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = ColorTextBlack
            )
        }
    }
}

@Composable
private fun LocationSection(
    address: String,
    cafeName: String
) {
    Text(
        text = "위치",
        fontSize = 16.sp,
        fontWeight = FontWeight.Bold,
        color = ColorTextBlack,
        modifier = Modifier.padding(start = 4.dp)
    )

    Spacer(modifier = Modifier.height(16.dp))

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .shadow(1.dp, RoundedCornerShape(24.dp))
            .clip(RoundedCornerShape(24.dp))
    ) {
        MapView(
            modifier = Modifier.fillMaxSize(),
            address = address,
            cafeName = cafeName
        )
    }
}
