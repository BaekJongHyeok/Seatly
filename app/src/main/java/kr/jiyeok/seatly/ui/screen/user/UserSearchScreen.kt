package kr.jiyeok.seatly.ui.screen.user

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import kr.jiyeok.seatly.data.remote.response.StudyCafeSummaryDto
import kr.jiyeok.seatly.presentation.viewmodel.SearchViewModel
import kr.jiyeok.seatly.ui.navigation.UserSearchNavigator
import kr.jiyeok.seatly.ui.theme.*
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocalCafe
import androidx.compose.material.icons.filled.LocalParking
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Power
import androidx.compose.material.icons.filled.SmokingRooms
import androidx.compose.material.icons.filled.Wc
import androidx.compose.material.icons.filled.Weekend
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.vector.ImageVector

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserSearchScreen(
    navController: NavController,
    viewModel: SearchViewModel = hiltViewModel()
) {
    // 네비게이션 관리자 생성
    val navigator = remember { UserSearchNavigator(navController) }

    // Context와 포커스 매니저
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current

    // ViewModel에서 상태 수집
    val searchQuery by viewModel.searchQuery.collectAsState()
    val filteredCafes by viewModel.filteredCafes.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val favoriteCafeIds by viewModel.favoriteCafeIds.collectAsState()

    // 이벤트 수집 (토스트 메시지)
    LaunchedEffect(Unit) {
        viewModel.events.collect { message ->
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }

    // 필터 BottomSheet 상태
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var isFilterSheetVisible by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ColorWhite)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { focusManager.clearFocus() }
    ) {
        // 상단 헤더 (뒤로가기 + 제목)
        SearchHeader(onBackClick = { navigator.goBack() })

        Spacer(modifier = Modifier.height(16.dp))

        // 검색 입력 필드
        SearchInputField(
            searchQuery = searchQuery,
            onQueryChange = { viewModel.updateSearchQuery(it) },
            onFocusClear = { focusManager.clearFocus() },
            onFilterClick = { isFilterSheetVisible = true }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 검색 결과 카운트 및 정렬 옵션
        if (searchQuery.isNotEmpty()) {
            SearchResultHeader(
                resultCount = filteredCafes.size
            )
            Spacer(modifier = Modifier.height(12.dp))
        }

        // 검색 결과 또는 로딩 상태
        if (isLoading) {
            LoadingIndicator()
        } else {
            SearchResultList(
                cafes = filteredCafes,
                favoriteIds = favoriteCafeIds.toSet(),
                onItemClick = { cafe ->
                    navigator.navigateToCafeDetail(cafe.id)
                },
                onToggleFavorite = { cafeId ->
                    viewModel.toggleFavoriteCafe(cafeId)
                }
            )
        }

        // 필터 BottomSheet
        if (isFilterSheetVisible) {
            ModalBottomSheet(
                onDismissRequest = { isFilterSheetVisible = false },
                sheetState = sheetState,
                containerColor = ColorWhite,
            ) {
                SearchFilterBottomSheet(
                    onClose = { isFilterSheetVisible = false },
                    onApply = {
                        // TODO: ViewModel에 실제 필터값 전달해서 검색 결과 갱신
                        isFilterSheetVisible = false
                    }
                )
            }
        }
    }
}

// =====================================================
// UI Components
// =====================================================

/**
 * 상단 헤더
 * 뒤로가기 버튼과 제목 표시
 */
@Composable
fun SearchHeader(onBackClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(ColorWhite)
            .padding(horizontal = 20.dp, vertical = 12.dp)
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = "뒤로가기",
            tint = ColorTextBlack,
            modifier = Modifier
                .size(24.dp)
                .clickable { onBackClick() }
                .align(Alignment.CenterStart)
        )

        Text(
            text = "스터디카페 검색",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = ColorTextBlack,
            modifier = Modifier.align(Alignment.Center)
        )
    }
}

/**
 * 검색 입력 필드
 * 검색어 입력과 필터 버튼
 */
@Composable
fun SearchInputField(
    searchQuery: String,
    onQueryChange: (String) -> Unit,
    onFocusClear: () -> Unit,
    onFilterClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // 검색 입력 박스
        Box(
            modifier = Modifier
                .weight(1f)
                .height(52.dp)
                .shadow(
                    elevation = 3.dp,
                    shape = RoundedCornerShape(12.dp),
                    ambientColor = ColorTextBlack.copy(alpha = 0.1f),
                    spotColor = ColorTextBlack.copy(alpha = 0.05f)
                )
                .clip(RoundedCornerShape(12.dp))
                .background(ColorInputBg)
        ) {
            BasicTextField(
                value = searchQuery,
                onValueChange = onQueryChange,
                singleLine = true,
                textStyle = TextStyle(fontSize = 13.sp, color = ColorTextBlack),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { onFocusClear() }),
                decorationBox = { innerTextField ->
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "검색",
                            tint = ColorPrimaryOrange,
                            modifier = Modifier.size(20.dp)
                        )

                        Box(modifier = Modifier.weight(1f)) {
                            if (searchQuery.isEmpty()) {
                                Text(
                                    text = "카페명, 지역, 주소로 검색",
                                    color = ColorTextLightGray,
                                    fontSize = 13.sp
                                )
                            }
                            innerTextField()
                        }
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        }

        // 필터 버튼
        Box(
            modifier = Modifier
                .size(52.dp)
                .shadow(
                    elevation = 3.dp,
                    shape = RoundedCornerShape(12.dp),
                    ambientColor = ColorTextBlack.copy(alpha = 0.1f),
                    spotColor = ColorTextBlack.copy(alpha = 0.05f)
                )
                .clip(RoundedCornerShape(12.dp))
                .background(ColorInputBg)
                .clickable { onFilterClick() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Tune,
                contentDescription = "필터",
                tint = ColorTextBlack,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

/**
 * 검색 결과 헤더
 * 검색 결과 개수와 정렬 옵션 표시
 */
@Composable
fun SearchResultHeader(resultCount: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = "검색 결과: ${resultCount}개",
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = ColorTextBlack
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = "거리순",
                fontSize = 12.sp,
                color = ColorTextGray
            )
        }
    }
}

/**
 * 로딩 인디케이터
 * 검색 중일 때 로딩 상태 표시
 */
@Composable
fun LoadingIndicator() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(
            color = ColorPrimaryOrange,
            modifier = Modifier.size(48.dp)
        )
    }
}

/**
 * 검색 결과 리스트
 * 검색된 카페 목록 표시
 */
@Composable
fun SearchResultList(
    cafes: List<StudyCafeSummaryDto>,
    favoriteIds: Set<Long>,
    onItemClick: (StudyCafeSummaryDto) -> Unit,
    onToggleFavorite: (Long) -> Unit
) {
    if (cafes.isEmpty()) {
        // 검색 결과가 없을 때
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "검색 결과가 없습니다",
                color = ColorTextLightGray,
                fontSize = 14.sp
            )
        }
    } else {
        // 검색 결과 리스트
        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(
                start = 20.dp,
                end = 20.dp,
                top = 0.dp,
                bottom = 120.dp
            )
        ) {
            items(
                items = cafes,
                key = { it.id }
            ) { cafe ->
                SearchCafeItem(
                    cafe = cafe,
                    isFavorite = cafe.id in favoriteIds,
                    onClick = { onItemClick(cafe) },
                    onFavoriteClick = { onToggleFavorite(cafe.id) }
                )
            }
        }
    }
}

/**
 * 검색 결과 카페 아이템
 * 카페 이미지, 이름, 주소, 태그, 즐겨찾기 버튼 표시
 */
@Composable
fun SearchCafeItem(
    cafe: StudyCafeSummaryDto,
    isFavorite: Boolean,
    onClick: () -> Unit,
    onFavoriteClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 3.dp,
                shape = RoundedCornerShape(16.dp),
                ambientColor = ColorTextBlack.copy(alpha = 0.1f),
                spotColor = ColorTextBlack.copy(alpha = 0.05f)
            )
            .clip(RoundedCornerShape(16.dp))
            .background(ColorBgBeige)
            .clickable { onClick() }
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            // 카페 이미지
            AsyncImage(
                model = cafe.mainImageUrl,
                contentDescription = cafe.name,
                modifier = Modifier
                    .size(100.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(ColorBorderLight),
                contentScale = ContentScale.Crop
            )

            // 카페 정보
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(top = 4.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // 카페 이름
                Text(
                    text = cafe.name ?: "이름 없음",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = ColorTextBlack,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                // 카페 주소
                Text(
                    text = cafe.address ?: "주소 정보 없음",
                    fontSize = 12.sp,
                    color = ColorTextGray,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                // 카페 태그 (24시간, 주차가능 등)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CafeTag(text = "24시간")
                    CafeTag(text = "주차가능")
                }
            }

            // 즐겨찾기 버튼
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(50.dp))
                    .background(
                        if (isFavorite) ColorPrimaryOrange else Color.Transparent
                    )
                    .clickable { onFavoriteClick() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isFavorite) {
                        Icons.Filled.Favorite
                    } else {
                        Icons.Outlined.FavoriteBorder
                    },
                    contentDescription = "즐겨찾기",
                    tint = if (isFavorite) ColorWhite else ColorTextLightGray,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

/**
 * 카페 태그
 * 카페의 특징(24시간, 주차가능 등)을 나타내는 태그
 */
@Composable
fun CafeTag(text: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(ColorWhite)
            .padding(horizontal = 6.dp, vertical = 3.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = 10.sp,
            color = ColorTextBlack,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun SearchFilterBottomSheet(
    onClose: () -> Unit,
    onApply: () -> Unit,
) {
    // 편의시설
    val amenityOptions = listOf(
        "Wi-Fi", "콘센트", "음료", "주차",
        "24시간", "개인 룸", "화장실",
        "휴게 공간", "흡연실"
    )
    var selectedAmenities by remember { mutableStateOf(setOf("Wi-Fi", "24시간")) }

    // 운영 시간
    val operationOptions = listOf("현재 영업 중", "24시간", "평일", "주말")
    var selectedOperation by remember { mutableStateOf("현재 영업 중") }

    fun resetAll() {
        selectedAmenities = setOf("Wi-Fi", "24시간")
        selectedOperation = "현재 영업 중"
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 24.dp)
    ) {
        // 상단 탭 + 닫기 버튼 (변경 없음)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(24.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "정렬",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = ColorTextGray
                )
                Text(
                    text = "필터",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = ColorTextBlack
                )
            }

            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "닫기",
                tint = ColorTextBlack,
                modifier = Modifier
                    .size(20.dp)
                    .clickable { onClose() }
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
        ) {
            // ✔ 편의시설
            Text(
                text = "편의시설",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = ColorTextBlack
            )
            Spacer(modifier = Modifier.height(10.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                amenityOptions.forEach { amenity ->
                    val isSelected = amenity in selectedAmenities
                    FilterChip(
                        text = amenity,
                        selected = isSelected,
                        onClick = {
                            selectedAmenities =
                                if (isSelected) selectedAmenities - amenity
                                else selectedAmenities + amenity
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = amenityIconFor(amenity),
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = if (isSelected) ColorWhite else ColorTextBlack
                            )
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 운영 시간 (기존과 동일, 아이콘 없음)
            Text(
                text = "운영 시간",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = ColorTextBlack
            )
            Spacer(modifier = Modifier.height(10.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                operationOptions.forEach { option ->
                    FilterChip(
                        text = option,
                        selected = selectedOperation == option,
                        onClick = { selectedOperation = option }
                        // 운영 시간 칩은 아이콘 없이 사용
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // 하단 버튼 영역 (초기화 / 적용하기)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 초기화
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(54.dp)
                    .shadow(
                        elevation = 4.dp,
                        shape = RoundedCornerShape(27.dp),
                        ambientColor = ColorTextBlack.copy(alpha = 0.1f),
                        spotColor = ColorTextBlack.copy(alpha = 0.05f)
                    )
                    .clip(RoundedCornerShape(27.dp))
                    .background(ColorWhite)
                    .clickable { resetAll() },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "초기화",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = ColorTextBlack
                )
            }

            // 적용하기
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(54.dp)
                    .shadow(
                        elevation = 4.dp,
                        shape = RoundedCornerShape(27.dp),
                        ambientColor = ColorTextBlack.copy(alpha = 0.1f),
                        spotColor = ColorTextBlack.copy(alpha = 0.05f)
                    )
                    .clip(RoundedCornerShape(27.dp))
                    .background(ColorPrimaryOrange)
                    .clickable {
                        // 여기서 실제 필터 값들을 ViewModel에 넘기면 됨
                        onApply()
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "적용하기",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = ColorWhite
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
    }
}

/**
 * 필터용 Chip 컴포넌트
 * (선택 시 주황색, 미선택 시 흰색 톤)
 */
@Composable
fun FilterChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    leadingIcon: (@Composable (() -> Unit))? = null
) {
    Box(
        modifier = Modifier
            .shadow(
                elevation = if (selected) 4.dp else 2.dp,
                shape = RoundedCornerShape(24.dp),
                ambientColor = ColorTextBlack.copy(alpha = 0.08f),
                spotColor = ColorTextBlack.copy(alpha = 0.05f)
            )
            .clip(RoundedCornerShape(24.dp))
            .background(
                if (selected) ColorPrimaryOrange else ColorInputBg
            )
            .clickable { onClick() }
            .padding(horizontal = 18.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (leadingIcon != null) {
                leadingIcon()
            }
            Text(
                text = text,
                fontSize = 13.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                color = if (selected) ColorWhite else ColorTextBlack
            )
        }
    }
}

@Composable
private fun amenityIconFor(amenity: String): ImageVector {
    return when (amenity) {
        "Wi-Fi" -> Icons.Filled.Wifi
        "콘센트" -> Icons.Filled.Power
        "음료" -> Icons.Filled.LocalCafe
        "주차" -> Icons.Filled.LocalParking
        "24시간" -> Icons.Filled.AccessTime
        "개인 룸" -> Icons.Filled.Person
        "화장실" -> Icons.Filled.Wc
        "휴게 공간" -> Icons.Filled.Weekend
        "흡연실" -> Icons.Filled.SmokingRooms
        else -> Icons.Filled.Wifi
    }
}


