package kr.jiyeok.seatly.ui.screen.admin

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import kr.jiyeok.seatly.data.remote.response.StudyCafeSummaryDto
import kr.jiyeok.seatly.data.remote.response.UsageDto
import kr.jiyeok.seatly.presentation.viewmodel.AdminHomeViewModel
import kr.jiyeok.seatly.ui.component.common.AppTopBar
import kr.jiyeok.seatly.ui.theme.*
import kr.jiyeok.seatly.ui.component.common.ExitBackHandler
import org.jetbrains.compose.resources.painterResource
import org.koin.compose.viewmodel.koinViewModel
import seatly.composeapp.generated.resources.Res
import seatly.composeapp.generated.resources.img_default_cafe

@Composable
fun AdminHomeScreen(
    navController: NavController,
    viewModel: AdminHomeViewModel = koinViewModel()
) {
    val focusManager = LocalFocusManager.current
    
    ExitBackHandler()

    val cafes by viewModel.cafes.collectAsState()
    val cafeUsages by viewModel.cafeUsages.collectAsState()
    val imageBitmapCache by viewModel.imageBitmapCache.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadRegisteredCafes()
    }

    LaunchedEffect(Unit) {
        viewModel.events.collect { message ->
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
            ) { focusManager.clearFocus() }
    ) {
        AdminHomeTopBar(
            onAddCafeClick = { navController.navigate("admin/cafe/create") }
        )

        Spacer(modifier = Modifier.height(8.dp))

        Box(modifier = Modifier.fillMaxSize()) {
            when {
                isLoading -> LoadingState()
                cafes.isEmpty() -> EmptyState()
                else -> CafeListContent(
                    cafes = cafes,
                    usages = cafeUsages,
                    imageBitmapCache = imageBitmapCache,
                    onCafeClick = { cafeId ->
                        navController.navigate("admin/cafe/$cafeId")
                    }
                )
            }
        }
    }
}

@Composable
private fun AdminHomeTopBar(
    onAddCafeClick: () -> Unit
) {
    AppTopBar(
        title = "등록 카페 목록",
        rightContent = {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "카페 추가",
                tint = ColorPrimaryOrange,
                modifier = Modifier
                    .size(28.dp)
                    .clickable { onAddCafeClick() }
            )
        }
    )
}

@Composable
private fun LoadingState() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(
            color = ColorPrimaryOrange,
            modifier = Modifier.size(48.dp)
        )
    }
}

@Composable
private fun EmptyState() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "등록된 카페가 없습니다",
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = ColorTextGray
            )
        }
    }
}

@Composable
private fun CafeListContent(
    cafes: List<StudyCafeSummaryDto>,
    usages: Map<Long, UsageDto>,
    imageBitmapCache: Map<String, ImageBitmap>,
    onCafeClick: (Long) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp)
    ) {
        Text(
            text = "총 ${cafes.size}개 카페 등록됨",
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = ColorTextGray,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            items(items = cafes, key = { it.id }) { cafe ->
                AdminCafeItem(
                    cafe = cafe,
                    usage = usages[cafe.id],
                    bitmap = cafe.mainImageUrl?.let { imageBitmapCache[it] },
                    onClick = { onCafeClick(cafe.id) }
                )
            }
        }
    }
}

@Composable
private fun AdminCafeItem(
    cafe: StudyCafeSummaryDto,
    usage: UsageDto?,
    bitmap: ImageBitmap?,
    onClick: () -> Unit
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
        Column {
            CafeBasicInfo(
                cafe = cafe,
                bitmap = bitmap
            )

            Spacer(modifier = Modifier.height(16.dp))

            CafeSeatUsageSection(usage = usage)
        }
    }
}

@Composable
private fun CafeBasicInfo(
    cafe: StudyCafeSummaryDto,
    bitmap: ImageBitmap?
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        CafeImage(
            bitmap = bitmap,
            mainImageUrl = cafe.mainImageUrl,
            cafeName = cafe.name
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(top = 4.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = cafe.name,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = ColorTextBlack,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = cafe.address,
                fontSize = 12.sp,
                color = ColorTextGray,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun CafeImage(
    bitmap: ImageBitmap?,
    mainImageUrl: String?,
    cafeName: String
) {
    Box(
        modifier = Modifier
            .size(64.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(ColorBorderLight),
        contentAlignment = Alignment.Center
    ) {
        when {
            bitmap != null -> {
                Image(
                    bitmap = bitmap,
                    contentDescription = cafeName,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }

            mainImageUrl == null -> {
                DefaultCafeImage(cafeName)
            }

            else -> {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = ColorPrimaryOrange,
                    strokeWidth = 2.dp
                )
            }
        }
    }
}

@Composable
private fun DefaultCafeImage(cafeName: String) {
    Image(
        painter = painterResource(Res.drawable.img_default_cafe),
        contentDescription = cafeName,
        modifier = Modifier.fillMaxSize(),
        contentScale = ContentScale.Crop
    )
}

@Composable
private fun CafeSeatUsageSection(usage: UsageDto?) {
    val total = usage?.totalCount ?: 0
    val used = usage?.useCount ?: 0
    val usedRatio = if (total > 0) used.toFloat() / total else 0f

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(ColorWhite.copy(alpha = 0.5f))
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        when {
            usage == null -> {
                Text(
                    text = "현황 데이터를 불러오는 중...",
                    fontSize = 12.sp,
                    color = ColorTextGray
                )
            }

            total == 0 -> {
                Text(
                    text = "등록된 좌석이 없습니다. 좌석을 등록해주세요.",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = ColorTextGray
                )
            }

            else -> {
                SeatProgressRow(
                    value = "$used / $total",
                    ratio = usedRatio
                )
            }
        }
    }
}

@Composable
private fun SeatProgressRow(
    value: String,
    ratio: Float
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "실시간 점유율",
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
            color = ColorPrimaryOrange,
            trackColor = ColorWhite
        )
    }
}
