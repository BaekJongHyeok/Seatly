package kr.jiyeok.seatly.ui.screen.admin

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
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

@Composable
fun AdminHomeScreen(
    navController: NavController,
    viewModel: AdminHomeViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current

    // AdminHomeViewModel 에서 모든 데이터 가져오기
    val registeredCafes by viewModel.cafes.collectAsState()
    val cafeUsages by viewModel.cafeUsages.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()


    LaunchedEffect(Unit) {
        viewModel.events.collect { message ->
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }

    // 처음 한 번만 홈 데이터 로드
    LaunchedEffect(Unit) {
        viewModel.loadRegisteredCafes()
    }

    LaunchedEffect(registeredCafes) {
        registeredCafes.forEach { cafe ->
            viewModel.loadCafeUsage(cafe.id)
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
        AppTopBar(
            title = "등록 카페 목록",
            rightContent = {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "카페 추가",
                    tint = ColorPrimaryOrange,
                    modifier = Modifier.size(28.dp).clickable { navController.navigate("admin/cafe/create") }
                )
            }
        )

        Spacer(modifier = Modifier.height(8.dp))

        // 메인 컨텐츠
        Box(modifier = Modifier.fillMaxSize()) {
            when {
                isLoading -> {
                    LoadingState()
                }
                registeredCafes.isEmpty() -> {
                    EmptyState()
                }
                else -> {
                    CafeListContent(
                        cafes = registeredCafes,
                        usages = cafeUsages,
                        onCafeClick = { cafeId ->
                            navController.navigate("admin/cafe/$cafeId")
                        }
                    )
                }
            }
        }
    }
}

/**
 * 로딩 상태
 */
@Composable
fun LoadingState() {
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
 * 빈 상태
 */
@Composable
fun EmptyState() {
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

/**
 * 카페 리스트 컨텐츠
 */
@Composable
fun CafeListContent(
    cafes: List<StudyCafeSummaryDto>,
    usages: Map<Long, UsageDto>,
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
                    usage = usages[cafe.id], // 해당 카페의 사용 현황 전달
                    onClick = { onCafeClick(cafe.id) }
                )
            }
        }
    }
}

/**
 * 검색 결과 카페 아이템
 * 카페 이미지, 이름, 주소, 태그 표시
 */
@Composable
fun AdminCafeItem(
    cafe: StudyCafeSummaryDto,
    usage: UsageDto?,
    onClick: () -> Unit
) {
    val total = usage?.totalCount ?: 0
    val used = usage?.useCount ?: 0
    val usedRatio = if (total > 0) used.toFloat() / total else 0f

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

                    // 카페 태그 (24시간, 주차가능 등)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CafeTag(text = "24시간")
                        CafeTag(text = "주차가능")
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 2. 실시간 현황 섹션
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(ColorWhite.copy(alpha = 0.5f))
                    .padding(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                when {
                    // 데이터 로딩 전 (ViewModel에서 아직 Map에 데이터가 안 들어왔을 때)
                    usage == null -> {
                        Text(
                            text = "현황 데이터를 불러오는 중...",
                            fontSize = 12.sp,
                            color = ColorTextGray
                        )
                    }
                    // 좌석이 아직 등록되지 않은 경우 (total이 0)
                    total == 0 -> {
                        Text(
                            text = "등록된 좌석이 없습니다. 좌석을 등록해주세요.",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = ColorTextGray
                        )
                    }
                    // 정상 데이터가 있는 경우
                    else -> {
                        SeatProgressRow("$used / $total", usedRatio)
                    }
                }
            }
        }
    }
}

@Composable
private fun SeatProgressRow(value: String, ratio: Float) {
    Column {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("실시간 점유율", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = ColorTextBlack)
            Text(value, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = ColorTextBlack)
        }
        Spacer(modifier = Modifier.height(8.dp))
        LinearProgressIndicator(
            progress = ratio,
            modifier = Modifier.fillMaxWidth().height(12.dp).clip(CircleShape),
            color = ColorPrimaryOrange,
            trackColor = ColorWhite
        )
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

