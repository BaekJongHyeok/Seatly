package kr.jiyeok.seatly.ui.screen.admin

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kr.jiyeok.seatly.data.remote.response.TimePassRequestDto
import kr.jiyeok.seatly.presentation.viewmodel.AdminCafeDetailViewModel
import kr.jiyeok.seatly.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

/**
 * 시간권 요청 목록 화면
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimePassRequestsScreen(
    viewModel: AdminCafeDetailViewModel,
    cafeId: Long,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val requests = uiState.timePassRequests
    
    // Event 처리 (Toast 메시지)
    LaunchedEffect(Unit) {
        viewModel.events.collect { message ->
            // TODO: Show Toast
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("시간권 요청", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "뒤로가기")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = ColorWhite,
                    titleContentColor = ColorTextBlack
                )
            )
        },
        containerColor = ColorBgBeige
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (uiState.isLoadingRequests) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = ColorPrimaryOrange
                )
            } else if (requests.isEmpty()) {
                // 요청이 없을 때
                EmptyRequestsMessage()
            } else {
                // 요청 목록
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(requests) { item ->
                        TimePassRequestCard(
                            request = item.request,
                            userName = item.userName,
                            cafeId = cafeId,
                            onAccept = { viewModel.acceptTimePassRequest(it, cafeId) },
                            onReject = { viewModel.rejectTimePassRequest(it, cafeId) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyRequestsMessage() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "📭",
                fontSize = 48.sp
            )
            Text(
                text = "대기 중인 요청이 없습니다",
                fontSize = 16.sp,
                color = ColorTextGray,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun TimePassRequestCard(
    request: TimePassRequestDto,
    userName: String,
    cafeId: Long,
    onAccept: (Long) -> Unit,
    onReject: (Long) -> Unit
) {
    var isProcessing by remember { mutableStateOf(false) }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(2.dp, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = ColorWhite)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 사용자 정보
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = userName,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = ColorTextBlack
                    )
                    Text(
                        text = "요청 시간: ${request.createdAt?.let { formatDateTime(it) } ?: "정보 없음"}",
                        fontSize = 12.sp,
                        color = ColorTextGray
                    )
                }
                
                // 시간 표시
                val timeInMinutes = request.time / 60
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = ColorPrimaryOrange.copy(alpha = 0.1f)
                ) {
                    Text(
                        text = "${timeInMinutes}분",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = ColorPrimaryOrange
                    )
                }
            }
            
            Divider(color = ColorBgBeige)
            
            // 승인/거절 버튼
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // 거절 버튼
                OutlinedButton(
                    onClick = {
                        isProcessing = true
                        onReject(request.id)
                    },
                    modifier = Modifier.weight(1f),
                    enabled = !isProcessing,
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color.Red
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Cancel,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("거절")
                }
                
                // 승인 버튼
                Button(
                    onClick = {
                        isProcessing = true
                        onAccept(request.id)
                    },
                    modifier = Modifier.weight(1f),
                    enabled = !isProcessing,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ColorPrimaryOrange
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("승인")
                }
            }
        }
    }
}

private fun formatDateTime(dateTimeString: String): String {
    return try {
        val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
        val outputFormat = SimpleDateFormat("MM/dd HH:mm", Locale.getDefault())
        val date = inputFormat.parse(dateTimeString)
        date?.let { outputFormat.format(it) } ?: dateTimeString
    } catch (e: Exception) {
        dateTimeString
    }
}
