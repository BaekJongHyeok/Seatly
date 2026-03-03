package kr.jiyeok.seatly.ui.screen.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import kr.jiyeok.seatly.data.remote.response.UserTimePassInfo
import kr.jiyeok.seatly.presentation.viewmodel.AdminCafeDetailViewModel
import kr.jiyeok.seatly.ui.theme.*

/**
 * 카페 멤버 관리 탭
 * - 멤버 검색, 통계, 목록 표시
 */
@Composable
fun AdminCafeMembersTab(
    viewModel: AdminCafeDetailViewModel,
    cafeId: Long,
    onNavigateToTimePassRequests: () -> Unit
) {
    // UI State 구독
    val uiState by viewModel.uiState.collectAsState()

    // 로컬 검색 상태
    var searchQuery by remember { mutableStateOf("") }

    // 필터링 최적화 (derivedStateOf 사용)
    val filteredMembers = remember(uiState.members, searchQuery) {
        if (searchQuery.isEmpty()) {
            uiState.members
        } else {
            uiState.members.filter { member ->
                val name = member.detailInfo?.name ?: member.basicInfo.name
                name?.contains(searchQuery, ignoreCase = true) == true
            }
        }
    }

    Surface(
        color = ColorWhite,
        modifier = Modifier.fillMaxSize()
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp, vertical = 16.dp)
            ) {
                // 검색 바
                MemberSearchBar(
                    query = searchQuery,
                    onQueryChange = { searchQuery = it }
                )

                Spacer(modifier = Modifier.height(20.dp))

                // 멤버 통계 카드
                MemberStatisticsCard(
                    totalCount = uiState.members.size,
                    activeCount = uiState.members.size
                )

                Spacer(modifier = Modifier.height(24.dp))

                // 시간권 요청 섹션
                TimePassRequestsSection(
                    requests = uiState.timePassRequests,
                    isLoading = uiState.isLoadingRequests,
                    onAccept = { requestId ->
                        viewModel.acceptTimePassRequest(requestId, cafeId)
                    },
                    onReject = { requestId ->
                        viewModel.rejectTimePassRequest(requestId, cafeId)
                    }
                )

                Spacer(modifier = Modifier.height(24.dp))

                // 섹션 헤더
                MemberListHeader(
                    totalCount = filteredMembers.size,
                    isFiltered = searchQuery.isNotEmpty()
                )

                Spacer(modifier = Modifier.height(16.dp))

                // 멤버 리스트
                when {
                    uiState.isLoadingMembers -> {
                        // 로딩 중
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
                    filteredMembers.isEmpty() -> {
                        // 빈 상태
                        if (searchQuery.isEmpty()) {
                            EmptyMembersView()
                        } else {
                            EmptySearchResultView()
                        }
                    }
                    else -> {
                        // 멤버 리스트
                        MemberList(
                            members = filteredMembers
                        )
                    }
                }
            }

            // 하단 플로팅 버튼 - 시간권 요청 확인
            FloatingActionButton(
                onClick = { onNavigateToTimePassRequests() },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 20.dp, bottom = 20.dp),
                containerColor = ColorPrimaryOrange,
                contentColor = ColorWhite
            ) {
                Box {
                    Icon(
                        imageVector = Icons.Default.Notifications,
                        contentDescription = "시간권 요청 확인"
                    )
                    
                    // 요청 개수 배지
                    if (uiState.timePassRequests.isNotEmpty()) {
                        Badge(
                            modifier = Modifier.align(Alignment.TopEnd)
                        ) {
                            Text(
                                text = uiState.timePassRequests.size.toString(),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

// =====================================================
// Time Pass Requests Section
// =====================================================

/**
 * 시간권 요청 섹션
 */
@Composable
private fun TimePassRequestsSection(
    requests: List<kr.jiyeok.seatly.presentation.viewmodel.AdminCafeDetailViewModel.TimePassRequestWithUser>,
    isLoading: Boolean,
    onAccept: (Long) -> Unit,
    onReject: (Long) -> Unit
) {
    val pendingRequests = remember(requests) {
        requests.filter { it.request.status == "PENDING" }
    }

    if (isLoading || pendingRequests.isNotEmpty()) {
        Column {
            // 헤더
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "시간권 요청",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = ColorTextBlack
                )

                if (!isLoading) {
                    Text(
                        text = "${pendingRequests.size}건",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = ColorPrimaryOrange
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 요청 목록
            when {
                isLoading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            color = ColorPrimaryOrange,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
                pendingRequests.isEmpty() -> {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(1.dp, RoundedCornerShape(16.dp)),
                        shape = RoundedCornerShape(16.dp),
                        color = ColorBgBeige
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "대기 중인 요청이 없습니다",
                                fontSize = 14.sp,
                                color = ColorTextGray
                            )
                        }
                    }
                }
                else -> {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        pendingRequests.forEach { item ->
                            TimePassRequestItem(
                                request = item.request,
                                userName = item.userName,
                                onAccept = { onAccept(item.request.id) },
                                onReject = { onReject(item.request.id) }
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * 시간권 요청 아이템
 */
@Composable
private fun TimePassRequestItem(
    request: kr.jiyeok.seatly.data.remote.response.TimePassRequestDto,
    userName: String,
    onAccept: () -> Unit,
    onReject: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(2.dp, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        color = ColorWhite
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // 요청 정보
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = userName,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = ColorTextBlack
                    )
                    
                    Text(
                        text = "요청 시간: ${request.time}분",
                        fontSize = 13.sp,
                        color = ColorTextGray
                    )
                    
                    Text(
                        text = "요청일: ${request.createdAt}",
                        fontSize = 12.sp,
                        color = ColorTextGray.copy(alpha = 0.7f)
                    )
                }

                // 시간권 표시
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = ColorPrimaryOrange.copy(alpha = 0.1f)
                ) {
                    Text(
                        text = "${request.time}분",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = ColorPrimaryOrange
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 수락/거절 버튼
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // 거절 버튼
                OutlinedButton(
                    onClick = onReject,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = ColorWarning
                    ),
                    border = androidx.compose.foundation.BorderStroke(1.dp, ColorWarning)
                ) {
                    Text(
                        text = "거절",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                // 수락 버튼
                Button(
                    onClick = onAccept,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ColorPrimaryOrange,
                        contentColor = ColorWhite
                    )
                ) {
                    Text(
                        text = "수락",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

// =====================================================
// Search Bar
// =====================================================

/**
 * 멤버 검색 바
 */
@Composable
private fun MemberSearchBar(
    query: String,
    onQueryChange: (String) -> Unit
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = Modifier
            .fillMaxWidth()
            .shadow(1.dp, RoundedCornerShape(16.dp)),
        placeholder = {
            Text(
                text = "이름으로 검색",
                color = ColorTextGray,
                fontSize = 14.sp
            )
        },
        leadingIcon = {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = null,
                tint = ColorTextGray,
                modifier = Modifier.size(20.dp)
            )
        },
        shape = RoundedCornerShape(16.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = ColorWhite,
            unfocusedContainerColor = ColorWhite,
            focusedBorderColor = ColorPrimaryOrange,
            unfocusedBorderColor = ColorBorderLight
        ),
        singleLine = true
    )
}

// =====================================================
// Statistics Card
// =====================================================

/**
 * 멤버 통계 카드
 */
@Composable
private fun MemberStatisticsCard(
    totalCount: Int,
    activeCount: Int
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(2.dp, RoundedCornerShape(24.dp)),
        shape = RoundedCornerShape(24.dp),
        color = ColorBgBeige
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            StatisticItem(
                label = "전체 멤버",
                value = totalCount.toString()
            )

            VerticalDivider(
                modifier = Modifier
                    .width(1.dp)
                    .height(50.dp),
                color = ColorBorderLight
            )

            StatisticItem(
                label = "활성 멤버",
                value = activeCount.toString()
            )
        }
    }
}

/**
 * 통계 아이템
 */
@Composable
private fun StatisticItem(
    label: String,
    value: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = label,
            fontSize = 13.sp,
            color = ColorTextGray,
            fontWeight = FontWeight.Medium
        )

        Text(
            text = value,
            fontSize = 24.sp,
            color = ColorPrimaryOrange,
            fontWeight = FontWeight.Bold
        )
    }
}

// =====================================================
// Member List Header
// =====================================================

/**
 * 멤버 리스트 헤더
 */
@Composable
private fun MemberListHeader(
    totalCount: Int,
    isFiltered: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = if (isFiltered) "검색 결과" else "전체 멤버",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = ColorTextBlack
        )

        Text(
            text = "${totalCount}명",
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = ColorTextGray
        )
    }
}

// =====================================================
// Member List
// =====================================================

/**
 * 멤버 리스트
 */
@Composable
private fun MemberList(
    members: List<kr.jiyeok.seatly.presentation.viewmodel.AdminCafeDetailViewModel.MemberWithUserInfo>
) {
    var selectedMember by remember {
        mutableStateOf<kr.jiyeok.seatly.presentation.viewmodel.AdminCafeDetailViewModel.MemberWithUserInfo?>(null)
    }

    // 멤버 상세 다이얼로그
    selectedMember?.let { member ->
        MemberDetailDialog(
            member = member,
            onDismiss = { selectedMember = null }
        )
    }

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 80.dp)
    ) {
        items(
            items = members,
            key = { it.basicInfo.userId }
        ) { member ->
            MemberItem(
                member = member,
                onClick = { selectedMember = member }
            )
        }
    }
}

/**
 * 멤버 아이템
 */
@Composable
private fun MemberItem(
    member: kr.jiyeok.seatly.presentation.viewmodel.AdminCafeDetailViewModel.MemberWithUserInfo,
    onClick: () -> Unit
) {
    val leftTimeSeconds = member.basicInfo.leftTime
    val hours = leftTimeSeconds / 3600
    val minutes = (leftTimeSeconds % 3600) / 60
    val formattedTime = when {
        hours > 0 && minutes > 0 -> "${hours}시간 ${minutes}분"
        hours > 0 -> "${hours}시간"
        minutes > 0 -> "${minutes}분"
        else -> "없음"
    }
    val hasTime = leftTimeSeconds > 0
    val timeColor = when {
        hours >= 1 -> ColorPrimaryOrange
        hasTime   -> ColorWarning
        else      -> ColorTextGray
    }

    val name = member.detailInfo?.name ?: member.basicInfo.name ?: "?"
    val initial = name.firstOrNull()?.toString() ?: "?"
    val phone = member.detailInfo?.phone

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(1.dp, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = ColorBgBeige
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // ── 아바타 ──────────────────────────────────────────
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(ColorPrimaryOrange),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = initial,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = ColorWhite
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            // ── 멤버 정보 ──────────────────────────────────────
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = name,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = ColorTextBlack
                )
                if (!phone.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = phone,
                        fontSize = 12.sp,
                        color = ColorTextGray
                    )
                }
            }

            // ── 잔여 시간 ──────────────────────────────────────
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = formattedTime,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = timeColor
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "잔여 시간",
                    fontSize = 11.sp,
                    color = ColorTextGray
                )
            }
        }
    }
}

// =====================================================
// Member Detail Dialog
// =====================================================

/**
 * 멤버 상세 정보 다이얼로그
 */
@Composable
private fun MemberDetailDialog(
    member: kr.jiyeok.seatly.presentation.viewmodel.AdminCafeDetailViewModel.MemberWithUserInfo,
    onDismiss: () -> Unit
) {
    val name    = member.detailInfo?.name  ?: member.basicInfo.name ?: "이름 없음"
    val email   = member.detailInfo?.email
    val phone   = member.detailInfo?.phone
    val initial = name.firstOrNull()?.toString() ?: "?"

    val leftSec   = member.basicInfo.leftTime
    val totalSec  = member.basicInfo.totalTime
    val leftHours = leftSec / 3600
    val leftMins  = (leftSec % 3600) / 60
    val formattedLeft = when {
        leftHours > 0 && leftMins > 0 -> "${leftHours}시간 ${leftMins}분"
        leftHours > 0                  -> "${leftHours}시간"
        leftMins  > 0                  -> "${leftMins}분"
        else                           -> "없음"
    }
    val totalHours = totalSec / 3600
    val totalMins  = (totalSec % 3600) / 60
    val formattedTotal = when {
        totalHours > 0 && totalMins > 0 -> "${totalHours}시간 ${totalMins}분"
        totalHours > 0                   -> "${totalHours}시간"
        totalMins  > 0                   -> "${totalMins}분"
        else                             -> "없음"
    }
    // 진행 비율 (0f ~ 1f)
    val progress = if (totalSec > 0) (leftSec.toFloat() / totalSec).coerceIn(0f, 1f) else 0f
    val timeColor = when {
        leftHours >= 1  -> ColorPrimaryOrange
        leftSec   > 0  -> ColorWarning
        else            -> ColorTextGray
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = ColorWhite,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // ── 프로필 아바타 ──────────────────────────────
                Box(
                    modifier = Modifier
                        .size(76.dp)
                        .clip(CircleShape)
                        .background(ColorPrimaryOrange),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = initial,
                        fontSize = 30.sp,
                        fontWeight = FontWeight.Bold,
                        color = ColorWhite
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // ── 이름 ──────────────────────────────────────
                Text(
                    text = name,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = ColorTextBlack,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(20.dp))

                // ── 연락처 정보 섹션 ──────────────────────────
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = ColorBgBeige,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        if (!phone.isNullOrBlank()) {
                            MemberInfoRow(
                                icon = Icons.Default.Phone,
                                label = "전화번호",
                                value = phone
                            )
                        }
                        if (!email.isNullOrBlank()) {
                            if (!phone.isNullOrBlank()) {
                                HorizontalDivider(color = ColorBorderLight)
                            }
                            MemberInfoRow(
                                icon = Icons.Default.Email,
                                label = "이메일",
                                value = email
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // ── 시간권 섹션 ───────────────────────────────
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = ColorBgBeige,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 18.dp, vertical = 16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "시간권",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = ColorTextGray
                            )
                            Text(
                                text = "총 $formattedTotal",
                                fontSize = 12.sp,
                                color = ColorTextGray
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // 잔여 시간 강조 표시
                        Text(
                            text = formattedLeft,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = timeColor
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // 프로그레스 바
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(ColorBorderLight)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(fraction = progress)
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(timeColor)
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = "잔여 ${(progress * 100).toInt()}%",
                            fontSize = 11.sp,
                            color = ColorTextGray,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.End
                        )
                    }
                }

                Spacer(modifier = Modifier.height(22.dp))

                // ── 닫기 버튼 ─────────────────────────────────
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ColorPrimaryOrange,
                        contentColor = ColorWhite
                    )
                ) {
                    Text(
                        text = "닫기",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
            }
        }
    }
}

/**
 * 다이얼로그 내 정보 행 (아이콘 + 레이블 + 값)
 */
@Composable
private fun MemberInfoRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(ColorPrimaryOrange.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = ColorPrimaryOrange,
                modifier = Modifier.size(18.dp)
            )
        }
        Column {
            Text(
                text = label,
                fontSize = 11.sp,
                color = ColorTextGray
            )
            Text(
                text = value,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = ColorTextBlack
            )
        }
    }
}

// =====================================================
// Empty States
// =====================================================

/**
 * 멤버가 없을 때
 */
@Composable
private fun EmptyMembersView() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = Icons.Default.PersonAdd,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = ColorTextGray.copy(alpha = 0.5f)
            )

            Text(
                text = "등록된 멤버가 없습니다",
                fontSize = 16.sp,
                color = ColorTextGray,
                fontWeight = FontWeight.Medium
            )

            Text(
                text = "우측 하단 버튼을 눌러 멤버를 추가하세요",
                fontSize = 13.sp,
                color = ColorTextGray.copy(alpha = 0.7f)
            )
        }
    }
}

/**
 * 검색 결과가 없을 때
 */
@Composable
private fun EmptySearchResultView() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = ColorTextGray.copy(alpha = 0.5f)
            )

            Text(
                text = "검색 결과가 없습니다",
                fontSize = 16.sp,
                color = ColorTextGray,
                fontWeight = FontWeight.Medium
            )

            Text(
                text = "다른 이름으로 검색해보세요",
                fontSize = 13.sp,
                color = ColorTextGray.copy(alpha = 0.7f)
            )
        }
    }
}
