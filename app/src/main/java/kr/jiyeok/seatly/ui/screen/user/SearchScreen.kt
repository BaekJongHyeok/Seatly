package kr.jiyeok.seatly.ui.screen.user

import android.R
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.ElectricalServices
import androidx.compose.material.icons.filled.LocalCafe
import androidx.compose.material.icons.filled.LocalParking
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.EventSeat
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import kr.jiyeok.seatly.data.remote.response.StudyCafeSummaryDto
import kr.jiyeok.seatly.presentation.viewmodel.SearchViewModel
import java.text.NumberFormat
import java.util.Locale

// 색상 정의 (디자인에 맞춘 값)
private object Colors {
    val PRIMARY = Color(0xFFFF6633)
    val PRIMARY_DARK = Color(0xFFE95321)
    val BACKGROUND_LIGHT = Color(0xFFFFFFFF)
    val SURFACE_CARD = Color(0xFFFFFFFF)
    val SURFACE_INPUT = Color(0xFFF8F8F8)
    val SURFACE_CHIP = Color(0xFFE8E8E8)
    val BORDER_LIGHT = Color(0xFFEFEFEF)
    val TEXT_PRIMARY = Color(0xFF1A1A1A)
    val TEXT_SECONDARY = Color(0xFF888888)
    val AMBER_500 = Color(0xFFFAA500)
    val GRAY_100 = Color(0xFFF3F3F3)
    val GRAY_200 = Color(0xFFE5E5E5)
    val GRAY_300 = Color(0xFFD1D1D1)
    val GRAY_400 = Color(0xFFA3A3A3)
}

@Composable
fun SearchScreen(
    navController: NavHostController,
    viewModel: SearchViewModel = hiltViewModel(),
    authViewModel: kr.jiyeok.seatly.presentation.viewmodel.AuthViewModel = hiltViewModel()
) {
    val selectedFilter = remember { mutableStateOf("위치 기반") }
    val showFilterSheet = remember { mutableStateOf(false) }

    // Observe cafe list, search query, and favorite cafe IDs from ViewModel
    val cafes by viewModel.cafes.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val favoriteCafeIds by viewModel.favoriteCafeIds.collectAsState()

    val filters = listOf("위치 기반", "별점 높은순", "최저가", "최신오픈", "특가상품", "더보기")
    val formatter = NumberFormat.getNumberInstance(Locale.KOREA)

    // Bottom sheet filter states
    val selectedLocationChip = remember { mutableStateOf(0) } // 0=current location, 1=gangseo, 2=mapo
    val radiusKm = remember { mutableStateOf(3f) } // 1..10
    val priceValue = remember { mutableStateOf(5000f) } // 0..10000
    val selectedPriceChip = remember { mutableStateOf(-1) } // -1 none, 0,1,2
    val facilitySelected = remember { mutableStateListOf<String>() } // "wifi", "electrical_services", "local_cafe", "local_parking" ...
    val parkingOnly = remember { mutableStateOf(true) }

    // Root box so we can overlay a custom bottom sheet without experimental APIs
    Box(modifier = Modifier.fillMaxSize()) {
        // Main content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Colors.BACKGROUND_LIGHT)
        ) {
            // 상단: X 버튼 instead of back arrow
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 20.dp, start = 16.dp, end = 16.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(
                    onClick = { /* usually close screen */ navController.popBackStack() },
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "닫기",
                        tint = Colors.TEXT_PRIMARY,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Text(
                    text = "스터디카페 검색",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Colors.TEXT_PRIMARY
                )

                Box(modifier = Modifier.size(40.dp))
            }

            // 검색창
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .height(48.dp)
                    .background(
                        color = Colors.SURFACE_INPUT,
                        shape = RoundedCornerShape(12.dp)
                    )
                    .border(
                        width = 1.dp,
                        color = Colors.BORDER_LIGHT,
                        shape = RoundedCornerShape(12.dp)
                    )
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Filled.Star, // placeholder; replace with search icon resource if you have one
                    contentDescription = "검색",
                    tint = Colors.TEXT_SECONDARY,
                    modifier = Modifier.size(20.dp)
                )

                BasicTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.updateSearchQuery(it) },
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .padding(start = 8.dp),
                    textStyle = TextStyle(
                        fontSize = 14.sp,
                        color = Colors.TEXT_PRIMARY
                    ),
                    singleLine = true,
                    decorationBox = { innerTextField ->
                        Box(
                            modifier = Modifier.fillMaxHeight(),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            if (searchQuery.isEmpty()) {
                                Text(
                                    text = "카페명, 지역, 주소로 검색",
                                    fontSize = 14.sp,
                                    color = Colors.TEXT_SECONDARY
                                )
                            }
                            innerTextField()
                        }
                    }
                )

                IconButton(onClick = {
                    // open bottom sheet
                    showFilterSheet.value = true
                }) {
                    Icon(
                        imageVector = Icons.Filled.Tune,
                        contentDescription = "필터",
                        tint = Colors.TEXT_SECONDARY,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Divider(color = Colors.BORDER_LIGHT, thickness = 1.dp)

            // 필터 칩 스크롤
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Colors.BACKGROUND_LIGHT)
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                items(filters) { filter ->
                    FilterChip(
                        label = filter,
                        isSelected = selectedFilter.value == filter,
                        onClick = { selectedFilter.value = filter }
                    )
                }
            }

            Divider(color = Colors.BORDER_LIGHT, thickness = 1.dp)

            // 검색 결과 & list header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "검색 결과: ${cafes.size}개",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = Colors.TEXT_PRIMARY
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { }
                ) {
                    Text(
                        text = "별점 높은순",
                        fontSize = 14.sp,
                        color = Colors.TEXT_PRIMARY,
                        fontWeight = FontWeight.Normal
                    )
                    Icon(
                        imageVector = Icons.Filled.Tune,
                        contentDescription = "정렬",
                        tint = Colors.TEXT_SECONDARY,
                        modifier = Modifier
                            .size(18.dp)
                            .padding(start = 4.dp)
                    )
                }
            }

            // 카페 리스트
            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Colors.PRIMARY)
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Colors.BACKGROUND_LIGHT),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(cafes) { cafe ->
                        CafeCardItem(
                            cafe = cafe,
                            isFavorite = cafe.id in favoriteCafeIds,
                            onFavoriteClick = {
                                authViewModel.toggleFavorite(cafe.id)
                            },
                            onCardClick = {
                                navController.navigate("cafe_detail/${cafe.id}")
                            },
                            formatter = formatter
                        )
                    }
                }
            }
        } // end main Column

        // Custom non-experimental bottom sheet overlay
        if (showFilterSheet.value) {
            // Semi-transparent scrim
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.35f))
                    .clickable { showFilterSheet.value = false } // dismiss when tapping scrim
            )

            // Sheet content
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 260.dp, max = 720.dp)
                    .align(Alignment.BottomCenter)
                    .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                    .shadow(elevation = 8.dp, shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)),
                color = Colors.BACKGROUND_LIGHT
            ) {
                val scrollState = rememberScrollState()
                Column(modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(scrollState)
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    // Header with title and close (X) button
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "필터 설정",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Colors.TEXT_PRIMARY,
                            modifier = Modifier.weight(1f)
                        )

                        IconButton(onClick = { showFilterSheet.value = false }) {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = "닫기",
                                tint = Colors.TEXT_PRIMARY,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    Divider(color = Colors.BORDER_LIGHT, thickness = 1.dp)

                    // 위치 섹션 (LazyRow so user can swipe)
                    Column(modifier = Modifier.padding(vertical = 12.dp)) {
                        Text(text = "위치", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Colors.TEXT_PRIMARY)
                        Spacer(modifier = Modifier.height(12.dp))

                        // horizontally swipable chips
                        val locList = listOf("현재 위치", "강서구", "마포구")
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(horizontal = 0.dp)
                        ) {
                            itemsIndexed(locList) { idx, label ->
                                val selected = selectedLocationChip.value == idx
                                Box(
                                    modifier = Modifier
                                        .height(36.dp)
                                        .clip(RoundedCornerShape(20.dp))
                                        .background(if (selected) Colors.PRIMARY else Colors.SURFACE_CHIP)
                                        .border(1.dp, if (selected) Color.Transparent else Colors.BORDER_LIGHT, RoundedCornerShape(20.dp))
                                        .clickable { selectedLocationChip.value = idx }
                                        .padding(horizontal = 14.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(text = label, color = if (selected) Color.White else Colors.TEXT_PRIMARY, fontSize = 13.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // radius labels + slider (styled)
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(text = "반경 1km", color = Colors.TEXT_SECONDARY, fontSize = 12.sp)
                            Text(text = "${radiusKm.value.toInt()}km", color = Colors.PRIMARY, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            Text(text = "10km", color = Colors.TEXT_SECONDARY, fontSize = 12.sp)
                        }

                        Slider(
                            value = (radiusKm.value - 1f) / 9f, // normalize 0..1
                            onValueChange = { v ->
                                radiusKm.value = (1 + v * 9f).coerceIn(1f, 10f)
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = SliderDefaults.colors(
                                thumbColor = Colors.PRIMARY,
                                activeTrackColor = Colors.PRIMARY,
                                inactiveTrackColor = Colors.GRAY_300
                            )
                        )
                    }

                    Divider(color = Colors.BORDER_LIGHT, thickness = 1.dp)

                    // 가격대 섹션 (LazyRow for price chips)
                    Column(modifier = Modifier.padding(vertical = 12.dp)) {
                        Text(text = "가격대 (시간당)", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Colors.TEXT_PRIMARY)
                        Spacer(modifier = Modifier.height(10.dp))

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(text = "0원", fontSize = 12.sp, color = Colors.TEXT_SECONDARY)
                            Text(text = "₩${formatter.format(priceValue.value.toInt())}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Colors.TEXT_PRIMARY)
                            Text(text = "10,000원+", fontSize = 12.sp, color = Colors.TEXT_SECONDARY)
                        }

                        Slider(
                            value = priceValue.value / 10000f,
                            onValueChange = { v ->
                                priceValue.value = (v * 10000f).coerceIn(0f, 10000f)
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = SliderDefaults.colors(
                                thumbColor = Colors.PRIMARY,
                                activeTrackColor = Colors.PRIMARY,
                                inactiveTrackColor = Colors.GRAY_300
                            )
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        val priceLabels = listOf("5천원 이하", "5-10천원", "10천원 이상")
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(horizontal = 0.dp)
                        ) {
                            itemsIndexed(priceLabels) { idx, label ->
                                val selected = selectedPriceChip.value == idx
                                Box(
                                    modifier = Modifier
                                        .height(44.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(if (selected) Colors.PRIMARY else Colors.SURFACE_CHIP)
                                        .border(1.dp, Colors.BORDER_LIGHT, RoundedCornerShape(12.dp))
                                        .clickable {
                                            selectedPriceChip.value = if (selected) -1 else idx
                                        }
                                        .padding(horizontal = 16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = label,
                                        color = if (selected) Color.White else Colors.TEXT_PRIMARY,
                                        fontSize = 13.sp,
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }

                    Divider(color = Colors.BORDER_LIGHT, thickness = 1.dp)

                    // 편의시설
                    Column(modifier = Modifier.padding(vertical = 12.dp)) {
                        Text(text = "편의시설", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Colors.TEXT_PRIMARY)
                        Spacer(modifier = Modifier.height(12.dp))

                        Row(horizontalArrangement = Arrangement.spacedBy(20.dp), verticalAlignment = Alignment.CenterVertically) {
                            // WiFi
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                val selected = facilitySelected.contains("wifi")
                                Box(
                                    modifier = Modifier
                                        .size(56.dp)
                                        .clip(CircleShape)
                                        .background(if (selected) Colors.PRIMARY else Colors.SURFACE_CHIP)
                                        .border(1.dp, Colors.BORDER_LIGHT, CircleShape)
                                        .clickable {
                                            if (selected) facilitySelected.remove("wifi") else facilitySelected.add("wifi")
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(imageVector = Icons.Filled.Wifi, contentDescription = "wifi", tint = if (selected) Color.White else Colors.TEXT_SECONDARY, modifier = Modifier.size(20.dp))
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(text = "WiFi", fontSize = 12.sp, color = if (selected) Colors.TEXT_PRIMARY else Colors.TEXT_SECONDARY)
                            }

                            // 콘센트
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                val selected = facilitySelected.contains("electrical_services")
                                Box(
                                    modifier = Modifier
                                        .size(56.dp)
                                        .clip(CircleShape)
                                        .background(if (selected) Colors.PRIMARY else Colors.SURFACE_CHIP)
                                        .border(1.dp, Colors.BORDER_LIGHT, CircleShape)
                                        .clickable {
                                            if (selected) facilitySelected.remove("electrical_services") else facilitySelected.add("electrical_services")
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(imageVector = Icons.Filled.ElectricalServices, contentDescription = "concent", tint = if (selected) Color.White else Colors.TEXT_SECONDARY, modifier = Modifier.size(20.dp))
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(text = "콘센트", fontSize = 12.sp, color = if (selected) Colors.TEXT_PRIMARY else Colors.TEXT_SECONDARY)
                            }

                            // 음료
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                val selected = facilitySelected.contains("local_cafe")
                                Box(
                                    modifier = Modifier
                                        .size(56.dp)
                                        .clip(CircleShape)
                                        .background(if (selected) Colors.PRIMARY else Colors.SURFACE_CHIP)
                                        .border(1.dp, Colors.BORDER_LIGHT, CircleShape)
                                        .clickable {
                                            if (selected) facilitySelected.remove("local_cafe") else facilitySelected.add("local_cafe")
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(imageVector = Icons.Filled.LocalCafe, contentDescription = "coffee", tint = if (selected) Color.White else Colors.TEXT_SECONDARY, modifier = Modifier.size(20.dp))
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(text = "음료", fontSize = 12.sp, color = if (selected) Colors.TEXT_PRIMARY else Colors.TEXT_SECONDARY)
                            }

                            // 주차
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                val selected = facilitySelected.contains("local_parking")
                                Box(
                                    modifier = Modifier
                                        .size(56.dp)
                                        .clip(CircleShape)
                                        .background(if (selected) Colors.PRIMARY else Colors.SURFACE_CHIP)
                                        .border(1.dp, Colors.BORDER_LIGHT, CircleShape)
                                        .clickable {
                                            if (selected) facilitySelected.remove("local_parking") else facilitySelected.add("local_parking")
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(imageVector = Icons.Filled.LocalParking, contentDescription = "parking", tint = if (selected) Color.White else Colors.TEXT_SECONDARY, modifier = Modifier.size(20.dp))
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(text = "주차", fontSize = 12.sp, color = if (selected) Colors.TEXT_PRIMARY else Colors.TEXT_SECONDARY)
                            }
                        }
                    }

                    Divider(color = Colors.BORDER_LIGHT, thickness = 1.dp)

                    // 주차 가능 여부 (Switch color changed to PRIMARY)
                    Column(modifier = Modifier.padding(vertical = 12.dp)) {
                        Text(text = "주차 가능 여부", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Colors.TEXT_PRIMARY)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                            Text(text = "주차 가능한 장소만 보기", fontSize = 13.sp, color = Colors.TEXT_PRIMARY)
                            Switch(
                                checked = parkingOnly.value,
                                onCheckedChange = { parkingOnly.value = it },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Colors.PRIMARY,
                                    checkedTrackColor = Colors.PRIMARY.copy(alpha = 0.28f)
                                )
                            )
                        }
                    }

                    Divider(color = Colors.BORDER_LIGHT, thickness = 1.dp)

                    // bottom action buttons (reset / apply)
                    Row(modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = {
                                // reset filter
                                selectedLocationChip.value = 0
                                radiusKm.value = 3f
                                priceValue.value = 5000f
                                selectedPriceChip.value = -1
                                facilitySelected.clear()
                                parkingOnly.value = true
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Colors.GRAY_100)
                        ) {
                            Text(text = "필터 초기화", color = Colors.TEXT_SECONDARY)
                        }

                        Button(
                            onClick = {
                                // apply filters (you can hook into viewmodel)
                                showFilterSheet.value = false
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Colors.PRIMARY)
                        ) {
                            Text(text = "적용하기", color = Color.White)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                } // sheet Column
            } // Surface
        } // if showFilterSheet
    } // Box root
}

@Composable
fun FilterChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .height(36.dp)
            .clickable { onClick() }
            .background(
                color = if (isSelected) Colors.PRIMARY else Colors.SURFACE_CHIP,
                shape = RoundedCornerShape(20.dp)
            )
            .border(
                width = if (isSelected) 0.dp else 1.dp,
                color = if (isSelected) Color.Transparent else Colors.BORDER_LIGHT,
                shape = RoundedCornerShape(20.dp)
            )
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = if (isSelected) Color.White else Colors.TEXT_PRIMARY
        )
    }
}

@Composable
fun CafeCardItem(
    cafe: StudyCafeSummaryDto,
    isFavorite: Boolean,
    onFavoriteClick: () -> Unit,
    onCardClick: () -> Unit,
    formatter: NumberFormat
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(140.dp)
            // 카드 전체 클릭은 상세 이동; 내부 버튼은 자체적으로 클릭 처리
            .clickable { onCardClick() }
            .shadow(elevation = 6.dp, shape = RoundedCornerShape(12.dp)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .background(Colors.SURFACE_CARD)
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // image area
            Box(
                modifier = Modifier
                    .width(100.dp)
                    .height(116.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Colors.GRAY_100)
            ) {
                AsyncImage(
                    model = cafe.mainImageUrl,
                    contentDescription = "Cafe Thumbnail",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(116.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop,
                    // use a safe built-in placeholder so build won't fail; replace later if desired
                    placeholder = painterResource(id = R.drawable.ic_menu_report_image),
                    error = painterResource(id = R.drawable.ic_menu_report_image)
                )
                
                // Show status badge if cafe is not open
                if (!cafe.isOpen) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(6.dp)
                            .background(
                                color = Colors.AMBER_500,
                                shape = RoundedCornerShape(6.dp)
                            )
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "준비중",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }

            // info area
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // Cafe name
                    Text(
                        text = cafe.name ?: "이름 없음",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Colors.TEXT_PRIMARY,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    // Address (removed distance)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = cafe.address ?: "주소 정보 없음",
                            fontSize = 13.sp,
                            color = Colors.TEXT_SECONDARY,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // bottom row: open status + right-side favorite + reserve buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // left: open status
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = if (cafe.isOpen) "영업중" else "준비중",
                            fontSize = 12.sp,
                            color = if (cafe.isOpen) Colors.TEXT_PRIMARY else Colors.AMBER_500,
                            fontWeight = FontWeight.Medium
                        )
                        if (cafe.isOpen) {
                            Text(text = "  |  24시", fontSize = 12.sp, color = Colors.TEXT_SECONDARY)
                        }
                    }

                    // right: favorite + reservation buttons
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { onFavoriteClick() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                                contentDescription = "즐겨찾기",
                                tint = if (isFavorite) Colors.PRIMARY else Colors.TEXT_SECONDARY,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        val isDisabled = !cafe.isOpen
                        val reserveShape = RoundedCornerShape(8.dp)
                        Box(
                            modifier = Modifier
                                .width(56.dp)
                                .height(36.dp)
                                .then(if (!isDisabled) Modifier.shadow(6.dp, reserveShape) else Modifier)
                                .clip(reserveShape)
                                .background(if (isDisabled) Colors.GRAY_200 else Colors.PRIMARY)
                                .border(
                                    width = if (isDisabled) 1.dp else 0.dp,
                                    color = if (isDisabled) Colors.GRAY_300 else Color.Transparent,
                                    shape = reserveShape
                                )
                                .clickable(enabled = !isDisabled) { /* 예약 동작 */ },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (isDisabled) "불가" else "예약",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isDisabled) Colors.GRAY_400 else Color.White
                            )
                        }
                    }
                }
            }
        }
    }
}