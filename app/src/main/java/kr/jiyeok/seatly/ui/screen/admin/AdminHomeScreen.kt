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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import kr.jiyeok.seatly.presentation.viewmodel.AdminHomeViewModel
import kr.jiyeok.seatly.ui.theme.ColorBgBeige
import kr.jiyeok.seatly.ui.theme.ColorBorderLight
import kr.jiyeok.seatly.ui.theme.ColorPrimaryOrange
import kr.jiyeok.seatly.ui.theme.ColorTextBlack
import kr.jiyeok.seatly.ui.theme.ColorTextGray
import kr.jiyeok.seatly.ui.theme.ColorTextLightGray
import kr.jiyeok.seatly.ui.theme.ColorWhite

@Composable
fun AdminHomeScreen(
    navController: NavController,
    viewModel: AdminHomeViewModel = hiltViewModel()
) {
    // Context와 포커스 매니저
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current

    // ViewModel에서 상태 수집
    val cafes by viewModel.cafes.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    // 이벤트 수집 (토스트 메시지)
    LaunchedEffect(Unit) {
        viewModel.events.collect { message ->
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }

    // 초기 로드
    LaunchedEffect(Unit) {
        viewModel.loadCafes()
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
        AdminHeader(onBackClick = { navController.popBackStack() })

        Spacer(modifier = Modifier.height(16.dp))

        // 메인 컨텐츠
        Box(
            modifier = Modifier
                .fillMaxSize()
        ) {
            when {
                isLoading -> {
                    LoadingState()
                }
                cafes.isEmpty() -> {
                    EmptyState(onRegisterClick = { navController.navigate("admin/cafe/create") })
                }
                else -> {
                    CafeListContent(
                        cafes = cafes,
                        onCafeClick = { cafeId ->
                            navController.navigate("admin/cafe/$cafeId")
                        },
                        onRegisterClick = { navController.navigate("admin/cafe/create") }
                    )
                }
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
fun AdminHeader(onBackClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(ColorWhite)
            .padding(horizontal = 16.dp, vertical = 12.dp)
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
            text = "관리자 홈",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = ColorTextBlack,
            modifier = Modifier.align(Alignment.Center)
        )
    }
}

/**
 * 로딩 상태
 * 데이터 로드 중일 때 표시
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
 * 등록된 카페가 없을 때 표시
 */
@Composable
fun EmptyState(onRegisterClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "등록된 카페가 없습니다",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = ColorTextBlack
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "카페를 등록하여 관리를 시작하세요",
            fontSize = 14.sp,
            color = ColorTextGray
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onRegisterClick,
            colors = ButtonDefaults.buttonColors(containerColor = ColorPrimaryOrange),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
        ) {
            Text(
                text = "카페 등록하기",
                color = ColorWhite,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
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
    onCafeClick: (Long) -> Unit,
    onRegisterClick: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(
                start = 20.dp,
                end = 20.dp,
                top = 0.dp,
                bottom = 80.dp
            )
        ) {
            items(
                items = cafes,
                key = { it.id }
            ) { cafe ->
                AdminCafeItem(
                    cafe = cafe,
                    onClick = { onCafeClick(cafe.id) }
                )
            }
        }

        // 하단 등록 버튼 (고정)
        Button(
            onClick = onRegisterClick,
            colors = ButtonDefaults.buttonColors(containerColor = ColorPrimaryOrange),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .padding(horizontal = 20.dp, vertical = 16.dp)
                .align(Alignment.BottomCenter)
        ) {
            Text(
                text = "+ 새 카페 등록",
                color = ColorWhite,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

/**
 * 관리자용 카페 아이템
 * 카페 이미지, 이름, 주소 표시
 * UserSearchScreen의 SearchCafeItem을 참고하여 구현
 */
@Composable
fun AdminCafeItem(
    cafe: StudyCafeSummaryDto,
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
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                // 카페 ID (참고용)
                Text(
                    text = "ID: ${cafe.id}",
                    fontSize = 11.sp,
                    color = ColorTextLightGray,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // 우측 화살표 표시
            Box(
                modifier = Modifier
                    .align(Alignment.CenterVertically)
                    .size(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = ">",
                    fontSize = 18.sp,
                    color = ColorPrimaryOrange,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
