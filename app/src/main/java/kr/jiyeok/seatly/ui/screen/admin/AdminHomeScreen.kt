package kr.jiyeok.seatly.ui.screen.admin

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import kr.jiyeok.seatly.R
import kr.jiyeok.seatly.data.remote.response.StudyCafeSummaryDto
import kr.jiyeok.seatly.data.remote.response.UsageDto
import kr.jiyeok.seatly.presentation.viewmodel.AdminHomeViewModel
import kr.jiyeok.seatly.ui.component.common.AppTopBar
import kr.jiyeok.seatly.ui.theme.ColorBgBeige
import kr.jiyeok.seatly.ui.theme.ColorBorderLight
import kr.jiyeok.seatly.ui.theme.ColorPrimaryOrange
import kr.jiyeok.seatly.ui.theme.ColorTextBlack
import kr.jiyeok.seatly.ui.theme.ColorTextGray
import kr.jiyeok.seatly.ui.theme.ColorWhite

/**
 * 관리자 홈 화면
 * 관리자가 등록한 카페 목록과 각 카페의 실시간 좌석 사용 현황을 표시합니다.
 */
@Composable
fun AdminHomeScreen(
    navController: NavController,
    viewModel: AdminHomeViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current

    // State 구독
    val cafes by viewModel.cafes.collectAsState()
    val cafeUsages by viewModel.cafeUsages.collectAsState()
    val imageBitmapCache by viewModel.imageBitmapCache.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    // 초기 데이터 로드 및 이벤트 처리
    LaunchedEffect(Unit) {
        viewModel.loadRegisteredCafes()

        // Toast 메시지 표시
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
            ) { focusManager.clearFocus() }
    ) {
        // 상단 헤더
        AdminHomeTopBar(
            onAddCafeClick = { navController.navigate("admin/cafe/create") }
        )

        Spacer(modifier = Modifier.height(8.dp))

        // 메인 컨텐츠
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

// =====================================================
// Top Bar
// =====================================================

/**
 * 관리자 홈 화면 상단 바
 */
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

// =====================================================
// State UI Components
// =====================================================

/**
 * 로딩 상태 UI
 */
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

/**
 * 빈 상태 UI - 등록된 카페가 없을 때
 */
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

// =====================================================
// Content
// =====================================================

/**
 * 카페 목록 컨텐츠
 */
@Composable
private fun CafeListContent(
    cafes: List<StudyCafeSummaryDto>,
    usages: Map<Long, UsageDto>,
    imageBitmapCache: Map<String, Bitmap>,
    onCafeClick: (Long) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp)
    ) {
        // 카페 개수 표시
        Text(
            text = "총 ${cafes.size}개 카페 등록됨",
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = ColorTextGray,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        // 카페 목록
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

// =====================================================
// Cafe Item Components
// =====================================================

/**
 * 카페 아이템 카드
 */
@Composable
private fun AdminCafeItem(
    cafe: StudyCafeSummaryDto,
    usage: UsageDto?,
    bitmap: Bitmap?,
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
            // 카페 기본 정보 (이미지 + 이름/주소)
            CafeBasicInfo(
                cafe = cafe,
                bitmap = bitmap
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 실시간 좌석 현황
            CafeSeatUsageSection(usage = usage)
        }
    }
}

/**
 * 카페 기본 정보 (이미지 + 이름/주소)
 */
@Composable
private fun CafeBasicInfo(
    cafe: StudyCafeSummaryDto,
    bitmap: Bitmap?
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        // 카페 이미지
        CafeImage(
            bitmap = bitmap,
            mainImageUrl = cafe.mainImageUrl,
            cafeName = cafe.name
        )

        // 카페 정보 (이름, 주소)
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(top = 4.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // 카페 이름
            Text(
                text = cafe.name,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = ColorTextBlack,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            // 카페 주소
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

/**
 * 카페 이미지
 * 3가지 상태: 로딩 중, 이미지 있음, 기본 이미지
 */
@Composable
private fun CafeImage(
    bitmap: Bitmap?,  // ByteArray 대신 Bitmap
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
            // ⚡ Bitmap이 있으면 바로 표시 (디코딩 불필요)
            bitmap != null -> {
                Image(
                    bitmap = bitmap.asImageBitmap(),
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


/**
 * 기본 카페 이미지
 */
@Composable
private fun DefaultCafeImage(cafeName: String) {
    Image(
        painter = painterResource(id = R.drawable.img_default_cafe),
        contentDescription = cafeName,
        modifier = Modifier.fillMaxSize(),
        contentScale = ContentScale.Crop
    )
}

/**
 * 카페 좌석 사용 현황 섹션
 */
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
            // 데이터 로딩 전
            usage == null -> {
                Text(
                    text = "현황 데이터를 불러오는 중...",
                    fontSize = 12.sp,
                    color = ColorTextGray
                )
            }

            // 좌석이 등록되지 않은 경우
            total == 0 -> {
                Text(
                    text = "등록된 좌석이 없습니다. 좌석을 등록해주세요.",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = ColorTextGray
                )
            }

            // 정상 데이터
            else -> {
                SeatProgressRow(
                    value = "$used / $total",
                    ratio = usedRatio
                )
            }
        }
    }
}

/**
 * 좌석 사용률 프로그레스 바
 */
@Composable
private fun SeatProgressRow(
    value: String,
    ratio: Float
) {
    Column {
        // 레이블과 값
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

        // 프로그레스 바
        LinearProgressIndicator(
            progress = ratio,
            modifier = Modifier
                .fillMaxWidth()
                .height(12.dp)
                .clip(CircleShape),
            color = ColorPrimaryOrange,
            trackColor = ColorWhite
        )
    }
}

// =====================================================
// Preview (Optional)
// =====================================================

/**
 * 카페 태그 (현재 미사용, 필요시 활성화)
 */
@Composable
private fun CafeTag(text: String) {
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
