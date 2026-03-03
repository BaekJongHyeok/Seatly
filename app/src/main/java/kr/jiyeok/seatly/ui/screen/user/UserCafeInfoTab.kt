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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.LocalCafe
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.MeetingRoom
import androidx.compose.material.icons.filled.Outlet
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
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
import kr.jiyeok.seatly.ui.theme.ColorCheckCircle
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
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
        uiState.images,
        uiState.isRefreshing // Refreshing 상태 추가
    ) {
        val hasCafeInfo = uiState.cafeInfo != null
        // Refreshing 중일 때는 로딩으로 간주하지 않음 (기존 데이터 표시)
        val notLoading = !uiState.isLoadingInfo && !uiState.isLoadingUsage

        val imageUrls = uiState.cafeInfo?.imageUrls ?: emptyList()
        val allImagesLoaded = if (imageUrls.isEmpty()) {
            true
        } else {
            imageUrls.all { imageId ->
                uiState.images.containsKey(imageId)
            }
        }

        // 데이터가 있으면 로딩 중이라도 보여줌 (Optimistic)
        // 데이터가 없으면 로딩 완료될 때까지 기다림
        if (hasCafeInfo) true
        else notLoading && allImagesLoaded
    }

    val refreshState = rememberPullToRefreshState()

    PullToRefreshBox(
        isRefreshing = uiState.isRefreshing,
        onRefresh = {
            uiState.cafeInfo?.id?.let {
                viewModel.refresh(it)
            }
        },
        state = refreshState,
        modifier = Modifier.fillMaxSize()
    ) {
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
                        navController = navController,
                        viewModel = viewModel
                    )
                }
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
    navController: NavController,
    viewModel: CafeDetailViewModel
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
            .background(ColorBgBeige) // Fix: Add background color to prevent white flash
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
                phone = uiState.cafeInfo?.phone?.let { formatKoreanPhoneFromDigits(it) } ?: "카페 대표 번호",
                operatingHours = uiState.cafeInfo?.openingHours,
                selectedTab = selectedTab,
                onTabSelected = onTabSelected
            )

            Spacer(modifier = Modifier.height(24.dp))

            if (selectedTab == 0) {
                uiState.cafeInfo?.let { cafeInfo ->
                    FeeSection(
                        viewModel = viewModel,
                        cafeId = cafeInfo.id
                    )
                }
            } else {
                SeatStatusSection(uiState.cafeUsage)
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

    // BottomActionBar 제거됨
}

@Composable
private fun HeaderSection(
    images: List<String>,
    imageBitmapCache: Map<String, android.graphics.Bitmap>
) {
    if (images.isEmpty()) return

    val pagerState = rememberPagerState(pageCount = { images.size })
    var zoomedImageId by remember { mutableStateOf<String?>(null) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(240.dp)
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(bottomStart = 40.dp, bottomEnd = 40.dp))
        ) { page ->
            val imageId = images[page]
            if (imageId.isNotEmpty()) {
                val bitmap = imageBitmapCache[imageId]
                if (bitmap != null) {
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = "카페 이미지",
                        modifier = Modifier
                            .fillMaxSize()
                            .clickable {
                                zoomedImageId = imageId
                            },
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Image(
                        painter = painterResource(R.drawable.img_default_cafe),
                        contentDescription = "기본 카페 이미지",
                        modifier = Modifier
                            .fillMaxSize()
                            .clickable {
                                // 기본 이미지도 확대? 일단은 클릭해도 아무 반응 없거나 토스트.
                                // 요청은 "카페 이미지를 클릭하면" 이므로 기본 이미지는 제외할 수도 있지만,
                                // 일관성을 위해 놔두거나... 여기서는 이미지가 없는 경우라 default 이미지 클릭은 안되게 함.
                            },
                        contentScale = ContentScale.Crop
                    )
                }
            } else {
                Image(
                    painter = painterResource(R.drawable.img_default_cafe),
                    contentDescription = "기본 카페 이미지",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
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
                text = "${pagerState.currentPage + 1} / ${images.size}",
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = ColorTextBlack
            )
        }
    }

    // 확대 다이얼로그 호출
    if (zoomedImageId != null) {
        val bitmap = imageBitmapCache[zoomedImageId]
        if (bitmap != null) {
            FullScreenImageDialog(
                bitmap = bitmap,
                onDismiss = { zoomedImageId = null }
            )
        }
    }
}

@Composable
private fun FullScreenImageDialog(
    bitmap: android.graphics.Bitmap,
    onDismiss: () -> Unit
) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    val transformableState = rememberTransformableState { zoomChange, panChange, _ ->
        scale = (scale * zoomChange).coerceIn(1f, 5f)
        offset = if (scale > 1f) {
            Offset(
                x = offset.x + panChange.x,
                y = offset.y + panChange.y
            )
        } else {
            Offset.Zero
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.9f))
                .pointerInput(Unit) {
                    detectTapGestures(
                        onTap = { onDismiss() },
                        onDoubleTap = {
                            if (scale > 1f) {
                                scale = 1f
                                offset = Offset.Zero
                            } else {
                                scale = 2.5f
                            }
                        }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            Image(
                bitmap = bitmap.asImageBitmap(), // AsyncImage -> Image (using bitmap directly)
                contentDescription = "확대 이미지",
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        translationX = offset.x
                        translationY = offset.y
                    }
                    .transformable(state = transformableState),
                contentScale = ContentScale.Fit
            )

            // 닫기 버튼
            androidx.compose.material3.IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
                    .size(40.dp)
                    .background(Color.White.copy(alpha = 0.3f), CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.Close, // Close needs import or fully qualified. Icons.Default.Close is standard. 
                    // Wait, Icons.Default.Close is likely NOT imported.
                    // Checking imports: import androidx.compose.material.icons.filled.* covers it?
                    // Usually import androidx.compose.material.icons.filled.Close is needed or Icons.Filled.Close.
                    // But standard Icons.Default.Close is common. I'll use Icons.Filled.Close just to be safe if `Close` is not in star import?
                    // Actually, `Icons.Default` is alias for `Icons.Filled`.
                    // Let's use `Icons.Filled.Close`.
                    // But wait, `Icons.Filled.Close` is not imported explicitly.
                    // `import androidx.compose.material.icons.Icons` is there.
                    // `import androidx.compose.material.icons.filled.*` is NOT there. Individual imports are used.
                    // I need to add `import androidx.compose.material.icons.filled.Close` to imports or use fully qualified.
                    contentDescription = "닫기",
                    tint = Color.White
                )
            }
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

            OperatingHoursSection(operatingHours = operatingHours)

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

/**
 * 영업시간 파싱 및 표시
 * 포맷: "MONDAY=09:00~21:00,TUESDAY=09:00~23:00,..."
 * 기본: 오늘 영업 상태 + 시간 표시, 드롭다운으로 전체 요일 확장 가능
 */
@Composable
private fun OperatingHoursSection(operatingHours: String?) {
    val dayOrder = listOf("MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY", "SUNDAY")
    val dayKorean = mapOf(
        "MONDAY" to "월", "TUESDAY" to "화", "WEDNESDAY" to "수",
        "THURSDAY" to "목", "FRIDAY" to "금", "SATURDAY" to "토", "SUNDAY" to "일"
    )

    val todayKey = remember {
        java.time.LocalDate.now().dayOfWeek.name
    }

    // 파싱
    val hoursMap = remember(operatingHours) {
        if (operatingHours.isNullOrBlank()) {
            emptyMap()
        } else {
            operatingHours.split(",").mapNotNull { entry ->
                val parts = entry.trim().split("=")
                if (parts.size == 2) parts[0].trim().uppercase() to parts[1].trim() else null
            }.toMap()
        }
    }

    val commonHours = hoursMap["ALL"]

    // 오늘 영업시간
    // 전체 요일 적용(ALL) 모드일 경우 각 요일별 데이터가 없으면 ALL 시간 사용
    val todayHours = hoursMap[todayKey] ?: commonHours
    val isTodayClosed = todayHours.equals("Closed", ignoreCase = true) || todayHours == null

    // 현재 영업 상태 판단
    val isOpen = remember(todayHours) {
        if (todayHours == null || isTodayClosed || todayHours == "Open") return@remember false
        try {
            val timePattern = Regex("(\\d{1,2}):(\\d{2})")
            val matches = timePattern.findAll(todayHours).toList()
            if (matches.size < 2) return@remember false

            val openMinutes = matches[0].groupValues[1].toInt() * 60 + matches[0].groupValues[2].toInt()
            val closeMinutes = matches[1].groupValues[1].toInt() * 60 + matches[1].groupValues[2].toInt()
            val now = java.time.LocalTime.now()
            val nowMinutes = now.hour * 60 + now.minute

            nowMinutes in openMinutes..closeMinutes
        } catch (_: Exception) {
            false
        }
    }

    // 드롭다운 상태
    var expanded by remember { mutableStateOf(false) }

    Row(verticalAlignment = Alignment.CenterVertically) {
        // 시계 아이콘
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(ColorWhite, RoundedCornerShape(12.dp))
                .padding(8.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.Schedule,
                contentDescription = null,
                tint = ColorPrimaryOrange,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        if (operatingHours.isNullOrBlank()) {
            Text(
                text = "운영시간 정보 없음",
                fontSize = 14.sp,
                color = ColorTextGray,
                lineHeight = 20.sp
            )
        } else {
            Column(modifier = Modifier.fillMaxWidth()) {
                // 요약 행: 영업 상태 + 오늘 시간 + 드롭다운 버튼
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(
                            indication = null,
                            interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
                        ) { expanded = !expanded },
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // 영업 상태
                        Text(
                            text = if (isTodayClosed) "휴무" else if (isOpen) "영업중" else "영업 종료",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isOpen) ColorCheckCircle else Color(0xFFE53935)
                        )

                        if (todayHours != null && !isTodayClosed) {
                            Text(
                                text = "  |  ",
                                fontSize = 14.sp,
                                color = ColorTextGray
                            )
                            Text(
                                text = todayHours,
                                fontSize = 14.sp,
                                color = ColorTextGray,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    // 드롭다운 아이콘
                    Icon(
                        imageVector = if (expanded)
                            Icons.Filled.KeyboardArrowUp
                        else
                            Icons.Filled.KeyboardArrowDown,
                        contentDescription = if (expanded) "접기" else "펼치기",
                        tint = ColorTextGray,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // 확장 영역: 전체 요일 표시
                androidx.compose.animation.AnimatedVisibility(visible = expanded) {
                    Column(
                        modifier = Modifier.padding(top = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        dayOrder.forEach { dayKey ->
                            val koreanDay = dayKorean[dayKey] ?: dayKey
                            // 요일 데이터 없으면 전체(ALL) 데이터 사용
                            val hours = hoursMap[dayKey] ?: commonHours
                            val isToday = dayKey == todayKey

                            // 데이터가 없는 요일은 표시하지 않음
                            if (hours == null) return@forEach

                            val isClosed = hours.equals("Closed", ignoreCase = true)

                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = koreanDay,
                                    fontSize = 13.sp,
                                    fontWeight = if (isToday) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isToday) ColorPrimaryOrange else ColorTextBlack
                                )

                                Spacer(modifier = Modifier.width(12.dp))

                                Text(
                                    text = if (isClosed) "휴무" else hours,
                                    fontSize = 13.sp,
                                    fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
                                    color = when {
                                        isClosed -> Color(0xFFE53935)
                                        isToday -> ColorPrimaryOrange
                                        else -> ColorTextGray
                                    }
                                )
                            }
                        }

                        // 공휴일 정보 추가
                        val holidayHours = hoursMap["HOLIDAY"]
                        if (holidayHours != null) {
                             val isHolidayClosed = holidayHours.equals("Closed", ignoreCase = true)
                             val displayHours = if (isHolidayClosed) "휴무" else if (commonHours != null) commonHours else "영업"
                             
                             Spacer(modifier = Modifier.height(8.dp))
                             Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "공휴일",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = ColorTextBlack
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = displayHours,
                                    fontSize = 13.sp,
                                    color = if (isHolidayClosed) Color(0xFFE53935) else ColorTextGray
                                )
                             }
                        }
                    }
                }
            }
        }
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
private fun FeeSection(
    viewModel: CafeDetailViewModel,
    cafeId: Long
) {
    // 시간권 데이터: 라벨과 분 단위 시간
    val fees = listOf(
        "1시간" to 60,
        "4시간" to 240,
        "8시간" to 480,
        "12시간" to 720
    )

    // 확인 다이얼로그 상태
    var showDialog by remember { mutableStateOf(false) }
    var selectedLabel by remember { mutableStateOf("") }
    var selectedTimeMinutes by remember { mutableIntStateOf(0) }

    // 확인 다이얼로그
    if (showDialog) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showDialog = false },
            title = {
                Text(
                    text = "시간권 요청",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "${selectedLabel} (${selectedTimeMinutes}분) 시간권을 요청하시겠습니까?",
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDialog = false
                        viewModel.requestTimePass(cafeId, selectedTimeMinutes.toLong() * 60)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ColorPrimaryOrange
                    )
                ) {
                    Text("요청")
                }
            },
            dismissButton = {
                Button(
                    onClick = { showDialog = false },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Transparent,
                        contentColor = ColorTextGray
                    )
                ) {
                    Text("취소")
                }
            }
        )
    }

    // 2열 그리드 레이아웃
    val chunkedFees = fees.chunked(2)

    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        chunkedFees.forEach { rowItems ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                rowItems.forEach { (label, timeInMinutes) ->
                    // 시간권 카드
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .height(100.dp)
                            .shadow(2.dp, RoundedCornerShape(16.dp))
                            .clickable {
                                selectedLabel = label
                                selectedTimeMinutes = timeInMinutes
                                showDialog = true
                            },
                        shape = RoundedCornerShape(16.dp),
                        color = ColorWhite
                    ) {
                        Column(
                            modifier = Modifier
                                .padding(16.dp)
                                .fillMaxSize(),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            // 상단: 아이콘 + 라벨
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = label, // 예: "1시간"
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = ColorTextBlack
                                )

                                Icon(
                                    imageVector = Icons.Filled.Schedule,
                                    contentDescription = null,
                                    tint = ColorPrimaryOrange,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            // 하단: 설명 및 화살표
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "이용권 요청",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = ColorTextGray
                                )

                                Icon(
                                    imageVector = Icons.Filled.KeyboardArrowRight,
                                    contentDescription = "요청",
                                    tint = ColorTextGray,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
                // 홀수 개수일 경우 빈 공간 채우기
                if (rowItems.size < 2) {
                    Spacer(modifier = Modifier.weight(1f))
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
    val availableRatio = if (total > 0) available.toFloat() / total else 0f

    // 통합 대시보드 카드 (단일 카드 디자인)
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(2.dp, RoundedCornerShape(24.dp)),
        shape = RoundedCornerShape(24.dp),
        color = ColorWhite
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // 헤더: 타이틀
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Inventory2,
                    contentDescription = null,
                    tint = ColorPrimaryOrange,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = "실시간 좌석 현황",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = ColorTextBlack
                )
            }

            // 메인 콘텐츠: 도넛 차트 + 요약 정보 (가로 배치)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                // 도넛 차트
                Box(contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(
                        progress = { 1f },
                        modifier = Modifier.size(120.dp),
                        color = ColorBgBeige,
                        strokeWidth = 12.dp,
                    )
                    CircularProgressIndicator(
                        progress = { availableRatio },
                        modifier = Modifier.size(120.dp),
                        color = ColorPrimaryOrange,
                        strokeWidth = 12.dp,
                        trackColor = ColorBgBeige,
                    )
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "잔여",
                            fontSize = 12.sp,
                            color = ColorTextGray,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "$available",
                            fontSize = 32.sp, // 강조
                            fontWeight = FontWeight.ExtraBold,
                            color = ColorPrimaryOrange
                        )
                    }
                }

                // 우측 요약 스탯
                Column(
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    StatRow(label = "전체 좌석", value = "$total", icon = Icons.Filled.LocalCafe)
                    StatRow(label = "사용 중", value = "$used", icon = Icons.Filled.Person)
                }
            }
        }
    }
}

@Composable
private fun StatRow(label: String, value: String, icon: ImageVector) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .background(ColorBgBeige, CircleShape)
                .padding(6.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = ColorTextGray,
                modifier = Modifier.fillMaxSize()
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(
                text = label,
                fontSize = 11.sp,
                color = ColorTextGray
            )
            Text(
                text = value,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = ColorTextBlack
            )
        }
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

// BottomActionBar 제거됨

private fun formatKoreanPhoneFromDigits(digits: String): String {
    if (digits.isEmpty()) return ""
    return if (digits.startsWith("02")) {
        // 서울 지역번호 (02)
        when (digits.length) {
            in 0..2 -> digits
            in 3..5 -> "${digits.substring(0, 2)}-${digits.substring(2)}"
            in 6..9 -> "${digits.substring(0, 2)}-${digits.substring(2, digits.length - 4)}-${digits.takeLast(4)}"
            else -> "${digits.substring(0, 2)}-${digits.substring(2, 6)}-${digits.substring(6, kotlin.math.min(10, digits.length))}"
        }
    } else {
        // 휴대폰 및 기타 지역번호 (010, 031 등)
        when (digits.length) {
            in 0..3 -> digits
            in 4..6 -> "${digits.substring(0, 3)}-${digits.substring(3)}"
            in 7..10 -> "${digits.substring(0, 3)}-${digits.substring(3, 6)}-${digits.substring(6)}"
            else -> "${digits.substring(0, 3)}-${digits.substring(3, 7)}-${digits.substring(7, 11)}" // Fixed to allow up to 11 digits properly
        }
    }
}