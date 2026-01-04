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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Search
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

@Composable
fun SearchScreen(
    navController: NavController,
    viewModel: SearchViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current

    // ViewModel State
    val searchQuery by viewModel.searchQuery.collectAsState()
    val filteredCafes by viewModel.filteredCafes.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val favoriteCafeIds by viewModel.favoriteCafeIds.collectAsState()

    // Event Handling
    LaunchedEffect(Unit) {
        viewModel.events.collect { message ->
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }

    // UI
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(horizontal = 20.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { focusManager.clearFocus() } // 배경 터치 시 키보드 내림
    ) {
        Spacer(modifier = Modifier.height(20.dp))

        // 검색창
        SearchHeader(
            query = searchQuery,
            onQueryChange = viewModel::updateSearchQuery,
            onSearch = { focusManager.clearFocus() }
        )

        Spacer(modifier = Modifier.height(12.dp))

        // 검색 결과
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Color(0xFFFF6B4A))
            }
        } else {
            SearchResultList(
                cafes = filteredCafes,
                favoriteIds = favoriteCafeIds.toSet(), // Set으로 변환하여 전달
                onItemClick = { cafe ->
                    navController.navigate("cafe_detail/${cafe.id}") // ✅ 올바른 경로 사용
                },
                onToggleFavorite = { cafeId ->
                    viewModel.toggleFavoriteCafe(cafeId) // ✅ ViewModel 함수 연결
                }
            )
        }
    }
}

@Composable
fun SearchHeader(
    query: String,
    onQueryChange: (String) -> Unit,
    onSearch: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "검색",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Black
        )
        Spacer(modifier = Modifier.height(16.dp))

        // 검색 입력 필드
        BasicTextField(
            value = query,
            onValueChange = onQueryChange,
            singleLine = true,
            textStyle = TextStyle(fontSize = 15.sp, color = Color.Black),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { onSearch() }),
            decorationBox = { innerTextField ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFF5F5F5))
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = Color(0xFF9E9E9E),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Box(modifier = Modifier.weight(1f)) {
                        if (query.isEmpty()) {
                            Text(
                                text = "카페 이름이나 주소로 검색해보세요",
                                color = Color(0xFFBDBDBD),
                                fontSize = 15.sp
                            )
                        }
                        innerTextField()
                    }
                }
            }
        )
    }
}

@Composable
fun SearchResultList(
    cafes: List<StudyCafeSummaryDto>,
    favoriteIds: Set<Long>, // ViewModel에서 받은 즐겨찾기 ID 목록
    onItemClick: (StudyCafeSummaryDto) -> Unit,
    onToggleFavorite: (Long) -> Unit
) {
    if (cafes.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "검색 결과가 없습니다",
                color = Color(0xFFBDBDBD),
                fontSize = 16.sp
            )
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(bottom = 100.dp) // ✅ 바텀 네비게이션 가림 방지 (여유있게 100dp)
        ) {
            items(cafes, key = { it.id }) { cafe ->
                SearchCafeItem(
                    cafe = cafe,
                    isFavorite = cafe.id in favoriteIds, // ✅ 즐겨찾기 여부 확인
                    onClick = { onItemClick(cafe) },
                    onFavoriteClick = { onToggleFavorite(cafe.id) }
                )
            }
        }
    }
}

@Composable
fun SearchCafeItem(
    cafe: StudyCafeSummaryDto,
    isFavorite: Boolean,
    onClick: () -> Unit,
    onFavoriteClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White)
            .clickable { onClick() }
            .padding(end = 8.dp), // 우측 여백
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 카페 이미지
        AsyncImage(
            model = cafe.mainImageUrl,
            contentDescription = cafe.name,
            modifier = Modifier
                .size(100.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFFEEEEEE)),
            contentScale = ContentScale.Crop
        )

        Spacer(modifier = Modifier.width(16.dp))

        // 카페 정보
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = cafe.name ?: "이름 없음",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = cafe.address ?: "주소 정보 없음",
                fontSize = 13.sp,
                color = Color(0xFF757575),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(8.dp))

            // 상태 표시 (영업중 등) - 필요 시 추가 구현
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "영업중", // 실제 데이터 연동 필요 시 수정
                    fontSize = 12.sp,
                    color = Color(0xFFFF6B4A),
                    fontWeight = FontWeight.Medium
                )
            }
        }

        // 즐겨찾기 버튼
        Icon(
            imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
            contentDescription = "Favorite",
            tint = if (isFavorite) Color(0xFFFF6B4A) else Color(0xFFE0E0E0),
            modifier = Modifier
                .size(24.dp)
                .clickable { onFavoriteClick() } // ✅ 클릭 시 토글 이벤트 발생
        )
    }
}
