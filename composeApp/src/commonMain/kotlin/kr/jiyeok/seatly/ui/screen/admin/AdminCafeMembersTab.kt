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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kr.jiyeok.seatly.data.remote.response.UserTimePassInfo
import kr.jiyeok.seatly.presentation.viewmodel.AdminCafeDetailViewModel
import kr.jiyeok.seatly.ui.theme.*

@Composable
fun AdminCafeMembersTab(
    viewModel: AdminCafeDetailViewModel,
    cafeId: Long
) {
    val uiState by viewModel.uiState.collectAsState()

    var searchQuery by remember { mutableStateOf("") }

    val filteredMembers = remember(uiState.members, searchQuery) {
        if (searchQuery.isEmpty()) {
            uiState.members
        } else {
            uiState.members.filter { member ->
                member.name.contains(searchQuery, ignoreCase = true)
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
                MemberSearchBar(
                    query = searchQuery,
                    onQueryChange = { searchQuery = it }
                )

                Spacer(modifier = Modifier.height(20.dp))

                MemberStatisticsCard(
                    totalCount = uiState.members.size,
                    activeCount = uiState.members.size
                )

                Spacer(modifier = Modifier.height(24.dp))

                MemberListHeader(
                    totalCount = filteredMembers.size,
                    isFiltered = searchQuery.isNotEmpty()
                )

                Spacer(modifier = Modifier.height(16.dp))

                when {
                    uiState.isLoadingMembers -> {
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
                        if (searchQuery.isEmpty()) {
                            EmptyMembersView()
                        } else {
                            EmptySearchResultView()
                        }
                    }
                    else -> {
                        MemberList(members = filteredMembers)
                    }
                }
            }

            FloatingActionButton(
                onClick = { /* TODO: 멤버 추가 다이얼로그 */ },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 20.dp, bottom = 20.dp),
                containerColor = ColorPrimaryOrange,
                contentColor = ColorWhite
            ) {
                Icon(
                    imageVector = Icons.Default.PersonAdd,
                    contentDescription = "멤버 추가"
                )
            }
        }
    }
}

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

@Composable
private fun MemberList(members: List<UserTimePassInfo>) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 80.dp)
    ) {
        items(
            items = members,
            key = { it.id }
        ) { member ->
            MemberItem(member = member)
        }
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
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(ColorPrimaryOrange.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = member.name.firstOrNull()?.toString() ?: "?",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = ColorPrimaryOrange
                    )
                }

                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
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

                        if (member.leftTime > 0) {
                            Box(
                                modifier = Modifier
                                    .background(
                                        ColorPrimaryOrange.copy(alpha = 0.1f),
                                        RoundedCornerShape(4.dp)
                                    )
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "${member.leftTime}분 남음",
                                    fontSize = 10.sp,
                                    color = ColorPrimaryOrange,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }

                    Text(
                        text = "ID: ${member.id}",
                        fontSize = 12.sp,
                        color = ColorTextGray
                    )
                }
            }

            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                val textColor = when {
                    member.leftTime > 60 -> ColorPrimaryOrange
                    member.leftTime > 0 -> Color(0xFFE57373) // Warning color
                    else -> ColorTextGray
                }
                
                Text(
                    text = "${member.leftTime}",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = textColor
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
