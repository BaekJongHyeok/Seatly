package kr.jiyeok.seatly.ui.screen.user

import android.graphics.Bitmap
import android.widget.Toast
import androidx.compose.foundation.Image
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import kr.jiyeok.seatly.R
import kr.jiyeok.seatly.data.remote.response.StudyCafeSummaryDto
import kr.jiyeok.seatly.presentation.viewmodel.SearchViewModel
import kr.jiyeok.seatly.ui.navigation.UserSearchNavigator
import kr.jiyeok.seatly.ui.theme.ColorBgBeige
import kr.jiyeok.seatly.ui.theme.ColorBorderLight
import kr.jiyeok.seatly.ui.theme.ColorInputBg
import kr.jiyeok.seatly.ui.theme.ColorPrimaryOrange
import kr.jiyeok.seatly.ui.theme.ColorTextBlack
import kr.jiyeok.seatly.ui.theme.ColorTextGray
import kr.jiyeok.seatly.ui.theme.ColorTextLightGray
import kr.jiyeok.seatly.ui.theme.ColorWhite

@Composable
fun UserSearchScreen(
    navController: NavController,
    viewModel: SearchViewModel = hiltViewModel()
) {
    val navigator = remember { UserSearchNavigator(navController) }
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current

    // ViewModel State
    val searchQuery by viewModel.searchQuery.collectAsState()
    val filteredCafes by viewModel.filteredCafes.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val favoriteCafeIds by viewModel.favoriteCafeIds.collectAsState()
    val cafeImages by viewModel.cafeImages.collectAsState()

    // Toast 이벤트 수집
    LaunchedEffect(Unit) {
        viewModel.events.collect { message ->
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ColorWhite)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                focusManager.clearFocus()
            }
    ) {
        // 검색 헤더
        SearchHeader(
            onBackClick = { navigator.goBack() }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 검색 입력 필드
        SearchInputField(
            searchQuery = searchQuery,
            onQueryChange = { viewModel.updateSearchQuery(it) },
            onFocusClear = { focusManager.clearFocus() }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 검색 결과 헤더
        if (searchQuery.isNotEmpty()) {
            SearchResultHeader(resultCount = filteredCafes.size)
            Spacer(modifier = Modifier.height(12.dp))
        }

        // 검색 결과 목록
        if (isLoading) {
            LoadingIndicator()
        } else {
            SearchResultList(
                cafes = filteredCafes,
                favoriteIds = favoriteCafeIds.toSet(),
                cafeImages = cafeImages,
                onItemClick = { cafe -> navigator.navigateToCafeDetail(cafe.id) },
                onToggleFavorite = { cafeId -> viewModel.toggleFavoriteCafe(cafeId) }
            )
        }
    }
}

// =====================================================
// UI Components
// =====================================================

/**
 * 검색 헤더
 */
@Composable
fun SearchHeader(
    onBackClick: () -> Unit
) {
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
            text = "검색",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = ColorTextBlack,
            modifier = Modifier.align(Alignment.Center)
        )
    }
}

/**
 * 검색 입력 필드
 */
@Composable
fun SearchInputField(
    searchQuery: String,
    onQueryChange: (String) -> Unit,
    onFocusClear: () -> Unit
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
                                    text = "카페 이름, 주소를 검색하세요",
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
    }
}

/**
 * 검색 결과 헤더
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
            text = "검색 결과 $resultCount",
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = ColorTextBlack
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = "정확도순",
                fontSize = 12.sp,
                color = ColorTextGray
            )
        }
    }
}

/**
 * 로딩 인디케이터
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
 * 검색 결과 목록
 */
@Composable
fun SearchResultList(
    cafes: List<StudyCafeSummaryDto>,
    favoriteIds: Set<Long>,
    cafeImages: Map<Long, Bitmap>,
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
                color = ColorTextLightGray,
                fontSize = 14.sp
            )
        }
    } else {
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
                    cafeBitmap = cafeImages[cafe.id],
                    isFavorite = cafe.id in favoriteIds,
                    onClick = { onItemClick(cafe) },
                    onFavoriteClick = { onToggleFavorite(cafe.id) }
                )
            }
        }
    }
}

/**
 * 검색 카페 아이템
 */
@Composable
fun SearchCafeItem(
    cafe: StudyCafeSummaryDto,
    cafeBitmap: Bitmap?,
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
            .clickable(onClick = onClick)
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            // 카페 이미지
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(ColorBorderLight)
            ) {
                if (cafeBitmap != null) {
                    Image(
                        bitmap = cafeBitmap.asImageBitmap(),
                        contentDescription = cafe.name,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    // 기본 이미지
                    Image(
                        painter = painterResource(R.drawable.img_default_cafe),
                        contentDescription = cafe.name,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
            }

            // 카페 정보
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(top = 4.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = cafe.name ?: "알 수 없는 카페",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = ColorTextBlack,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = cafe.address ?: "주소 없음",
                    fontSize = 12.sp,
                    color = ColorTextGray,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // 즐겨찾기 버튼
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(RoundedCornerShape(50.dp))
                    .background(if (isFavorite) ColorPrimaryOrange else Color.Transparent)
                    .clickable(onClick = onFavoriteClick),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                    contentDescription = "즐겨찾기",
                    tint = if (isFavorite) ColorWhite else ColorTextLightGray,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}
