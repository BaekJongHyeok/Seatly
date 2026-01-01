package kr.jiyeok.seatly.ui.screen.user

import android.R
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
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

// 색상 정의
private object Colors {
    val PRIMARY = Color(0xFFFF6633)
    val BACKGROUND_LIGHT = Color(0xFFFFFFFF)
    val SURFACE_INPUT = Color(0xFFF8F8F8)
    val BORDER_LIGHT = Color(0xFFEFEFEF)
    val TEXT_PRIMARY = Color(0xFF1A1A1A)
    val TEXT_SECONDARY = Color(0xFF888888)
    val AMBER_500 = Color(0xFFFAA500)
    val GRAY_100 = Color(0xFFF3F3F3)
    val GRAY_200 = Color(0xFFE5E5E5)
    val GRAY_300 = Color(0xFFD1D1D1)
}

@Composable
fun SearchScreen(
    navController: NavHostController,
    viewModel: SearchViewModel = hiltViewModel(),
    authViewModel: kr.jiyeok.seatly.presentation.viewmodel.AuthViewModel = hiltViewModel()
) {
    val selectedFilter = remember { mutableStateOf("위치 기반") }
    val showFilterSheet = remember { mutableStateOf(false) }

    // 1. Correct Data Collection
    // Use 'filteredCafes' from ViewModel which returns List<StudyCafeSummaryDto>
    // collectAsState with initial empty list to handle loading state implicitly
    val cafesList by viewModel.filteredCafes.collectAsState()

    val isLoading by viewModel.isLoading.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val favoriteCafeIds by viewModel.favoriteCafeIds.collectAsState()

    val filters = listOf("위치 기반", "별점 높은순", "최저가", "최신오픈", "특가상품", "더보기")

    // Filter states
    val selectedLocationChip = remember { mutableIntStateOf(0) }
    val radiusKm = remember { mutableFloatStateOf(3f) }
    val priceValue = remember { mutableFloatStateOf(5000f) }
    val selectedPriceChip = remember { mutableIntStateOf(-1) }
    val facilitySelected = remember { mutableStateListOf<String>() }
    val parkingOnly = remember { mutableStateOf(true) }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Colors.BACKGROUND_LIGHT)
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 20.dp, start = 16.dp, end = 16.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(
                    onClick = { navController.popBackStack() },
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

            // Search Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .height(48.dp)
                    .background(Colors.SURFACE_INPUT, RoundedCornerShape(12.dp))
                    .border(1.dp, Colors.BORDER_LIGHT, RoundedCornerShape(12.dp))
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Filled.Star, "검색", tint = Colors.TEXT_SECONDARY, modifier = Modifier.size(20.dp))

                BasicTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.updateSearchQuery(it) },
                    modifier = Modifier.weight(1f).padding(start = 8.dp),
                    textStyle = TextStyle(fontSize = 14.sp, color = Colors.TEXT_PRIMARY),
                    singleLine = true,
                    decorationBox = { innerTextField ->
                        Box(contentAlignment = Alignment.CenterStart) {
                            if (searchQuery.isEmpty()) {
                                Text("카페명, 지역, 주소로 검색", fontSize = 14.sp, color = Colors.TEXT_SECONDARY)
                            }
                            innerTextField()
                        }
                    }
                )

                IconButton(onClick = { showFilterSheet.value = true }) {
                    Icon(Icons.Filled.Tune, "필터", tint = Colors.TEXT_SECONDARY)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            Divider(color = Colors.BORDER_LIGHT, thickness = 1.dp)

            // Filter Chips
            LazyRow(
                modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(horizontal = 16.dp)
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

            // Result Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "검색 결과: ${cafesList.size}개",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = Colors.TEXT_PRIMARY
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("별점 높은순", fontSize = 14.sp, color = Colors.TEXT_PRIMARY)
                    Icon(Icons.Filled.Tune, "정렬", tint = Colors.TEXT_SECONDARY, modifier = Modifier.size(16.dp))
                }
            }

            // List or Loading
            if (isLoading && cafesList.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Colors.PRIMARY)
                }
            } else if (cafesList.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("검색 결과가 없습니다.", color = Colors.TEXT_SECONDARY)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 20.dp)
                ) {
                    items(cafesList) { cafe ->
                        val isFavorite = favoriteCafeIds.contains(cafe.id)

                        CafeSearchItem(
                            cafe = cafe,
                            isFavorite = isFavorite,
                            onFavoriteClick = {
                                // ViewModel supports separate add/remove functions
                                if (isFavorite) viewModel.removeFavoriteCafe(cafe.id)
                                else viewModel.addFavoriteCafe(cafe.id)
                            },
                            onClick = { navController.navigate("cafe_detail/${cafe.id}") }
                        )
                        Divider(color = Colors.GRAY_100, thickness = 1.dp)
                    }
                }
            }
        }

        // Bottom Sheet Filter
        if (showFilterSheet.value) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f))
                    .clickable { showFilterSheet.value = false }
            )

            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .fillMaxHeight(0.85f)
                    .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                    .background(Colors.BACKGROUND_LIGHT)
                    .clickable(enabled = false) { }
            ) {
                // Sheet Header
                Box(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("검색 필터", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    IconButton(
                        onClick = { showFilterSheet.value = false },
                        modifier = Modifier.align(Alignment.CenterEnd).padding(end = 8.dp)
                    ) {
                        Icon(Icons.Filled.Close, "닫기")
                    }
                }
                Divider(color = Colors.BORDER_LIGHT)

                // Filter Content
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("위치", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("내 주변", "강서구", "마포구").forEachIndexed { index, label ->
                            FilterOptionChip(label, selectedLocationChip.intValue == index) {
                                selectedLocationChip.intValue = index
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                    // ... Other filters ...

                    Spacer(modifier = Modifier.weight(1f))

                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Button(
                            onClick = { /* Reset */ },
                            modifier = Modifier.weight(1f).height(52.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Colors.GRAY_100, contentColor = Colors.TEXT_PRIMARY),
                            shape = RoundedCornerShape(12.dp)
                        ) { Text("초기화") }

                        Button(
                            onClick = { showFilterSheet.value = false },
                            modifier = Modifier.weight(2f).height(52.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Colors.PRIMARY),
                            shape = RoundedCornerShape(12.dp)
                        ) { Text("필터 적용하기") }
                    }
                }
            }
        }
    }
}

@Composable
fun FilterChip(label: String, isSelected: Boolean, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.clip(RoundedCornerShape(20.dp)).clickable { onClick() },
        color = if (isSelected) Colors.TEXT_PRIMARY else Colors.BACKGROUND_LIGHT,
        border = if (isSelected) null else androidx.compose.foundation.BorderStroke(1.dp, Colors.BORDER_LIGHT),
        shape = RoundedCornerShape(20.dp)
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            fontSize = 13.sp,
            color = if (isSelected) Color.White else Colors.TEXT_PRIMARY
        )
    }
}

@Composable
fun FilterOptionChip(label: String, isSelected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .height(36.dp)
            .border(1.dp, if (isSelected) Colors.PRIMARY else Colors.GRAY_300, RoundedCornerShape(18.dp))
            .background(if (isSelected) Colors.PRIMARY else Color.Transparent, RoundedCornerShape(18.dp))
            .clickable { onClick() }
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = 13.sp,
            color = if (isSelected) Color.White else Colors.TEXT_SECONDARY,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
        )
    }
}

@Composable
fun CafeSearchItem(
    cafe: StudyCafeSummaryDto,
    isFavorite: Boolean,
    onFavoriteClick: () -> Unit,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(16.dp)
    ) {
        // Image
        Box(
            modifier = Modifier
                .size(100.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Colors.GRAY_100)
        ) {
            AsyncImage(
                model = cafe.mainImageUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Content
        Column(modifier = Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = cafe.name ?: "이름 없음",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Colors.TEXT_PRIMARY,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                IconButton(onClick = onFavoriteClick, modifier = Modifier.size(24.dp)) {
                    Icon(
                        imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                        contentDescription = "찜하기",
                        tint = if (isFavorite) Colors.PRIMARY else Colors.TEXT_SECONDARY
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = cafe.address ?: "주소 정보 없음",
                fontSize = 13.sp,
                color = Colors.TEXT_SECONDARY,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
