package kr.jiyeok.seatly.ui.screen.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
                        MemberList(members = filteredMembers)
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
                        text = "$userName (${request.userId})",
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
private fun MemberList(members: List<kr.jiyeok.seatly.presentation.viewmodel.AdminCafeDetailViewModel.MemberWithUserInfo>) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 80.dp) // FAB 공간 확보
    ) {
        items(
            items = members,
            key = { it.basicInfo.id }
        ) { member ->
            MemberItem(member = member)
        }
    }
}

/**
 * 멤버 아이템
 */
@Composable
private fun MemberItem(member: kr.jiyeok.seatly.presentation.viewmodel.AdminCafeDetailViewModel.MemberWithUserInfo) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(1.dp, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        color = ColorBgBeige
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                // 프로필 이미지
                val imageUrl = member.detailInfo?.imageUrl
                val name = member.detailInfo?.name ?: member.basicInfo.name ?: "?"
                
                if (!imageUrl.isNullOrEmpty()) {
                     coil.compose.AsyncImage(
                        model = imageUrl,
                        contentDescription = "Profile Image",
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(ColorPrimaryOrange.copy(alpha = 0.1f)),
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(ColorPrimaryOrange.copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = name.firstOrNull()?.toString() ?: "?",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = ColorPrimaryOrange
                        )
                    }
                }

                // 멤버 정보
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = member.detailInfo?.name ?: member.basicInfo.name ?: "이름 없음",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = ColorTextBlack
                        )
                        
                        // 전화번호 표시
                        member.detailInfo?.phone?.let { phone ->
                            Text(
                                text = phone,
                                fontSize = 12.sp,
                                color = ColorTextGray
                            )
                        }

                        // 잔여 시간 표시
                        if (member.basicInfo.leftTime > 0) {
                            Box(
                                modifier = Modifier
                                    .background(
                                        ColorPrimaryOrange.copy(alpha = 0.1f),
                                        RoundedCornerShape(4.dp)
                                    )
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "${member.basicInfo.leftTime}분 남음",
                                    fontSize = 10.sp,
                                    color = ColorPrimaryOrange,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }

                    // 추가 정보 (이메일 등)
                    Text(
                        text = "ID: ${member.basicInfo.id}",
                        fontSize = 12.sp,
                        color = ColorTextGray
                    )
                }
            }

            // 잔여 시간 큰 숫자로 표시
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = "${member.basicInfo.leftTime}",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (member.basicInfo.leftTime > 60) {
                        ColorPrimaryOrange
                    } else if (member.basicInfo.leftTime > 0) {
                        ColorWarning
                    } else {
                        ColorTextGray
                    }
                )
                Text(
                    text = "분",
                    fontSize = 12.sp,
                    color = ColorTextGray
                )
            }
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
