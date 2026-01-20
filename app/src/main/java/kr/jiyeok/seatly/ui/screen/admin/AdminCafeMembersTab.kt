package kr.jiyeok.seatly.ui.screen.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import kr.jiyeok.seatly.data.remote.response.UserTimePassInfo
import kr.jiyeok.seatly.presentation.viewmodel.AdminCafeDetailViewModel
import kr.jiyeok.seatly.ui.theme.*

@Composable
fun AdminCafeMembersTab(
    viewModel: AdminCafeDetailViewModel,
    cafeId: Long
) {
    val cafeMembers by viewModel.cafeMembers.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    var searchQuery by remember { mutableStateOf("") }

    // 데이터 로드
    LaunchedEffect(cafeId) {
        viewModel.loadCafeMembers(cafeId)
    }

    Surface(color = ColorWhite, modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp, vertical = 16.dp)
            ) {
                // 검색 바
                SearchBar(
                    query = searchQuery,
                    onQueryChange = { searchQuery = it }
                )

                Spacer(modifier = Modifier.height(20.dp))

                // 멤버 통계 카드
                MemberStatisticsCard(
                    totalCount = cafeMembers?.size ?: 0,
                    activeCount = cafeMembers?.size  ?: 0
                )

                Spacer(modifier = Modifier.height(24.dp))

                // 섹션 헤더
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "전체 멤버",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = ColorTextBlack
                    )
                    Text(
                        "${cafeMembers?.size ?: 0}명",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = ColorTextGray
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 멤버 리스트
                if (isLoading) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = ColorPrimaryOrange)
                    }
                } else {
                    val filteredMembers = cafeMembers?.filter {
                        it.name.contains(searchQuery, ignoreCase = true)
                    } ?: emptyList()

                    if (filteredMembers.isEmpty()) {
                        EmptyMembersView()
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(filteredMembers) { member ->
                                MemberItem(member)
                            }
                        }
                    }
                }
            }

            // 하단 플로팅 버튼
            FloatingActionButton(
                onClick = { /* TODO: 멤버 추가 */ },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 20.dp, bottom = 20.dp),
                containerColor = ColorPrimaryOrange,
                contentColor = ColorWhite
            ) {
                Icon(Icons.Default.PersonAdd, contentDescription = "멤버 추가")
            }
        }
    }
}

@Composable
private fun SearchBar(query: String, onQueryChange: (String) -> Unit) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = Modifier
            .fillMaxWidth()
            .shadow(1.dp, RoundedCornerShape(16.dp)),
        placeholder = { Text("이름으로 검색", color = ColorTextGray, fontSize = 14.sp) },
        leadingIcon = {
            Icon(
                Icons.Default.Search,
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

@Composable
private fun MemberStatisticsCard(totalCount: Int, activeCount: Int) {
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
            StatisticItem("전체 멤버", totalCount.toString())
            Divider(
                modifier = Modifier
                    .width(1.dp)
                    .height(50.dp),
                color = ColorBorderLight
            )
            StatisticItem("활성 멤버", activeCount.toString())
        }
    }
}

@Composable
private fun StatisticItem(label: String, value: String) {
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

@Composable
private fun MemberItem(member: UserTimePassInfo) {
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
//                // 프로필 이미지
//                AsyncImage(
//                    model = member.profileImageUrl,
//                    contentDescription = null,
//                    modifier = Modifier
//                        .size(48.dp)
//                        .clip(CircleShape)
//                        .background(ColorWhite),
//                    contentScale = ContentScale.Crop
//                )

                // 멤버 정보
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = member.name,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = ColorTextBlack
                        )

//                        // 활성 상태 뱃지
//                        if (member.isActive) {
//                            Box(
//                                modifier = Modifier
//                                    .background(
//                                        ColorPrimaryOrange.copy(alpha = 0.1f),
//                                        RoundedCornerShape(4.dp)
//                                    )
//                                    .padding(horizontal = 6.dp, vertical = 2.dp)
//                            ) {
//                                Text(
//                                    "활성",
//                                    fontSize = 10.sp,
//                                    color = ColorPrimaryOrange,
//                                    fontWeight = FontWeight.Medium
//                                )
//                            }
//                        }
                    }
                }
            }
        }
    }
}

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
                Icons.Default.PersonAdd,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = ColorTextGray.copy(alpha = 0.5f)
            )
            Text(
                "등록된 멤버가 없습니다",
                fontSize = 16.sp,
                color = ColorTextGray,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
