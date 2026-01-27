package kr.jiyeok.seatly.ui.screen.user

import android.content.Intent
import android.location.Geocoder
import android.net.Uri
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.Image
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.LocalCafe
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.MeetingRoom
import androidx.compose.material.icons.filled.Outlet
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kr.jiyeok.seatly.R
import kr.jiyeok.seatly.data.remote.enums.EFacility
import kr.jiyeok.seatly.data.remote.response.UsageDto
import kr.jiyeok.seatly.presentation.viewmodel.CafeDetailViewModel
import kr.jiyeok.seatly.ui.theme.ColorBgBeige
import kr.jiyeok.seatly.ui.theme.ColorBorderLight
import kr.jiyeok.seatly.ui.theme.ColorPrimaryOrange
import kr.jiyeok.seatly.ui.theme.ColorTextBlack
import kr.jiyeok.seatly.ui.theme.ColorTextGray
import kr.jiyeok.seatly.ui.theme.ColorWhite
import java.text.NumberFormat
import java.util.Locale

@Composable
fun UserCafeInfoTab(
    viewModel: CafeDetailViewModel,
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
    uiState: CafeDetailViewModel.CafeDetailUiState,
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    scrollState: ScrollState,
    navController: NavController
) {
    val gallery = remember(uiState.cafeInfo?.imageUrls) {
        if (uiState.cafeInfo?.imageUrls.isNullOrEmpty()) {
            listOf("")
        } else {
            uiState.cafeInfo?.imageUrls!!
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(bottom = 100.dp)
    ) {
        HeaderSection(
            images = gallery,
            imageBitmapCache = uiState.images
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
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

    BottomActionBar(
        modifier = Modifier.align(Alignment.BottomCenter),
        onBack = { navController.popBackStack() },
        onSelectSeats = { navController.navigate("") }
    )
}

@Composable
private fun HeaderSection(
    images: List<String>,
    imageBitmapCache: Map<String, android.graphics.Bitmap>
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
                            bitmap = bitmap.asImageBitmap(),
                            contentDescription = "카페 이미지",
                            modifier = Modifier
                                .width(LocalConfiguration.current.screenWidthDp.dp)
                                .fillMaxHeight(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Image(
                            painter = painterResource(R.drawable.img_default_cafe),
                            contentDescription = "기본 카페 이미지",
                            modifier = Modifier
                                .width(LocalConfiguration.current.screenWidthDp.dp)
                                .fillMaxHeight(),
                            contentScale = ContentScale.Crop
                        )
                    }
                } else {
                    Image(
                        painter = painterResource(R.drawable.img_default_cafe),
                        contentDescription = "기본 카페 이미지",
                        modifier = Modifier
                            .width(LocalConfiguration.current.screenWidthDp.dp)
                            .fillMaxHeight(),
                        contentScale = ContentScale.Crop
                    )
                }
            }
        }

        // 이미지 개수 뱃지
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
                            text = "₩${NumberFormat.getInstance().format(price)}",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = ColorPrimaryOrange
                        )

                        Text(
                            text = "₩${NumberFormat.getInstance().format(price + 1000)}",
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
                    FacilityItem(
                        facilities[i],
                        Modifier.weight(1f)
                    )
                    if (i + 1 < facilities.size) {
                        FacilityItem(
                            facilities[i + 1],
                            Modifier.weight(1f)
                        )
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
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var location by remember { mutableStateOf<LatLng?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(address) {
        if (address.isBlank()) {
            errorMessage = "주소 정보가 없습니다"
            location = LatLng(37.5665, 126.9780)
            return@LaunchedEffect
        }

        scope.launch {
            location = withContext(Dispatchers.IO) {
                try {
                    android.util.Log.d("LocationSection", "검색할 주소: $address")
                    val geocoder = Geocoder(context, Locale.KOREA)
                    val addresses = geocoder.getFromLocationName(address, 1)

                    if (!addresses.isNullOrEmpty()) {
                        val result = LatLng(addresses[0].latitude, addresses[0].longitude)
                        android.util.Log.d("LocationSection", "좌표 찾음: $result")
                        errorMessage = null
                        result
                    } else {
                        android.util.Log.w("LocationSection", "주소를 찾을 수 없음: $address")
                        errorMessage = "주소를 찾을 수 없습니다"
                        LatLng(37.5665, 126.9780)
                    }
                } catch (e: Exception) {
                    android.util.Log.e("LocationSection", "Geocoding 오류: ${e.message}", e)
                    errorMessage = "주소 변환 실패: ${e.message}"
                    LatLng(37.5665, 126.9780)
                }
            }
        }
    }

    Text(
        text = "위치",
        fontSize = 16.sp,
        fontWeight = FontWeight.Bold,
        color = ColorTextBlack,
        modifier = Modifier.padding(start = 4.dp)
    )

    Spacer(modifier = Modifier.height(16.dp))

    // 에러 메시지 표시
    if (errorMessage != null) {
        Text(
            text = errorMessage!!,
            fontSize = 12.sp,
            color = Color.Red,
            modifier = Modifier.padding(bottom = 8.dp)
        )
    }

    if (location != null) {
        val cameraPositionState = rememberCameraPositionState {
            position = CameraPosition.fromLatLngZoom(location!!, 15f)
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .shadow(1.dp, RoundedCornerShape(24.dp))
                .clip(RoundedCornerShape(24.dp))
        ) {
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState
            ) {
                Marker(
                    state = MarkerState(position = location!!),
                    title = cafeName,
                    snippet = address
                )
            }

            Button(
                onClick = {
                    val uri = Uri.parse("geo:${location!!.latitude},${location!!.longitude}?q=${location!!.latitude},${location!!.longitude}($cafeName)")
                    val intent = Intent(Intent.ACTION_VIEW, uri)
                    intent.setPackage("com.google.android.apps.maps")

                    if (intent.resolveActivity(context.packageManager) != null) {
                        context.startActivity(intent)
                    } else {
                        val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://maps.google.com/?q=${location!!.latitude},${location!!.longitude}"))
                        context.startActivity(webIntent)
                    }
                },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(12.dp)
                    .fillMaxWidth(0.85f)
                    .height(44.dp),
                colors = ButtonDefaults.buttonColors(containerColor = ColorPrimaryOrange),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Map,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = "지도에서 보기",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = ColorWhite
                )
            }
        }
    } else {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .shadow(1.dp, RoundedCornerShape(24.dp))
                .clip(RoundedCornerShape(24.dp))
                .background(ColorBgBeige),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(
                color = ColorPrimaryOrange,
                modifier = Modifier.size(40.dp)
            )
        }
    }
}

@Composable
private fun BottomActionBar(modifier: Modifier = Modifier, onBack: () -> Unit, onSelectSeats: () -> Unit) {
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
                onClick = onBack,
                modifier = Modifier.weight(1f).fillMaxHeight(),
                colors = ButtonDefaults.buttonColors(containerColor = ColorWhite),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, ColorBorderLight)
            ) {
                Text("뒤로 가기", color = ColorTextBlack, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }

            Button(
                onClick = onSelectSeats,
                modifier = Modifier.weight(1f).fillMaxHeight(),
                colors = ButtonDefaults.buttonColors(containerColor = ColorPrimaryOrange),
                shape = RoundedCornerShape(12.dp),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
            ) {
                Text("좌석 선택", color = ColorWhite, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
        }
    }
}