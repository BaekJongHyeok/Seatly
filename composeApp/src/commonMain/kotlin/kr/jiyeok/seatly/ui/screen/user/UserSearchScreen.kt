package kr.jiyeok.seatly.ui.screen.user

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import kr.jiyeok.seatly.data.remote.response.StudyCafeSummaryDto
import kr.jiyeok.seatly.presentation.viewmodel.SearchViewModel
import kr.jiyeok.seatly.ui.component.common.AppTopBar
import kr.jiyeok.seatly.ui.navigation.UserSearchNavigator
import kr.jiyeok.seatly.ui.theme.*
import kr.jiyeok.seatly.ui.component.common.ExitBackHandler
import org.jetbrains.compose.resources.painterResource
import org.koin.compose.viewmodel.koinViewModel
import seatly.composeapp.generated.resources.Res
import seatly.composeapp.generated.resources.img_default_cafe

@Composable
fun UserSearchScreen(
    navController: NavController,
    viewModel: SearchViewModel = koinViewModel()
) {
    val navigator = remember { UserSearchNavigator(navController) }
    val focusManager = LocalFocusManager.current
    
    ExitBackHandler()

    val searchQuery by viewModel.searchQuery.collectAsState()
    val filteredCafes by viewModel.filteredCafes.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val favoriteCafeIds by viewModel.favoriteCafeIds.collectAsState()
    val cafeImages by viewModel.cafeImages.collectAsState()

    // Events (Toast replacement/logging)
    LaunchedEffect(Unit) {
        viewModel.events.collect { message ->
            // In a real app, we would use a SnackbarHost. For now, we'll just log.
            println("Event: $message")
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
        AppTopBar(title = "검색")

        Spacer(modifier = Modifier.height(16.dp))

        SearchInputField(
            searchQuery = searchQuery,
            onQueryChange = { viewModel.updateSearchQuery(it) },
            onFocusClear = { focusManager.clearFocus() }
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (searchQuery.isNotEmpty()) {
            SearchResultHeader(resultCount = filteredCafes.size)
            Spacer(modifier = Modifier.height(12.dp))
        }

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

@Composable
fun SearchResultList(
    cafes: List<StudyCafeSummaryDto>,
    favoriteIds: Set<Long>,
    cafeImages: Map<Long, ImageBitmap>,
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

@Composable
fun SearchCafeItem(
    cafe: StudyCafeSummaryDto,
    cafeBitmap: ImageBitmap?,
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
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(ColorBorderLight)
            ) {
                if (cafeBitmap != null) {
                    Image(
                        bitmap = cafeBitmap,
                        contentDescription = cafe.name,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Image(
                        painter = painterResource(Res.drawable.img_default_cafe),
                        contentDescription = cafe.name,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
            }

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
