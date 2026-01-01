package kr.jiyeok.seatly.ui.screen.common.password

import android.util.Patterns
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.launch
import kr.jiyeok.seatly.presentation.viewmodel.PasswordRecoveryViewModel
import kr.jiyeok.seatly.ui.component.AuthButton
import kr.jiyeok.seatly.ui.component.EmailInputField
import kr.jiyeok.seatly.ui.component.common.AppTopBar

@Composable
fun PasswordScreen_1(
    viewModel: PasswordRecoveryViewModel = hiltViewModel(),
    onBack: () -> Unit,
    onNextNavigate: () -> Unit
) {
    // Observe ViewModel states
    val email by viewModel.email.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    val currentStep by viewModel.currentStep.collectAsState()
    
    // Local text state for input
    var localEmail by remember { mutableStateOf(email) }
    
    // Navigate when step changes to 2
    LaunchedEffect(currentStep) {
        if (currentStep == 2) {
            onNextNavigate()
        }
    }

    LaunchedEffect(localEmail) {
        viewModel.updateEmail(localEmail)
    }

    val isEmailValid = Patterns.EMAIL_ADDRESS.matcher(localEmail).matches()

    // Consistent typography sizes for this screen
    val topBarTitleSize = 24.sp
    val sectionTitleSize = 16.sp
    val helperTextSize = 12.sp
    val labelTextSize = 14.sp
    val errorTextSize = 13.sp

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        // Use AppTopBar with adjusted (smaller & consistent) title size so it doesn't look oversized
        AppTopBar(
            title = "비밀번호 찾기",
            leftContent = {
                Icon(imageVector = Icons.Filled.ArrowBack, contentDescription = "뒤로", tint = Color(0xFF1A1A1A))
            },
            onLeftClick = onBack,
            titleTextStyle = TextStyle(fontSize = topBarTitleSize, fontWeight = FontWeight.Bold, color = Color(0xFF1A1A1A)),
            backgroundColor = Color.White,
            verticalPadding = 18.dp,
            buttonContainerSize = 44.dp,
            minHeight = 64.dp
        )

        // bottom divider to separate the top bar from content
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(Color(0xFFEEEEEE))
        )

        // 추가: 구분선 아래 공백을 넣어 상단과 본문 간 연결을 자연스럽게 함
        Spacer(modifier = Modifier.height(12.dp))

        // Content
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .weight(1f)
                .padding(top = 8.dp),
            verticalArrangement = Arrangement.Top
        ) {
            Text(text = "이메일 확인", fontSize = sectionTitleSize, fontWeight = FontWeight.SemiBold, color = Color(0xFF1A1A1A))
            Spacer(modifier = Modifier.height(6.dp))
            Text(text = "가입한 이메일 주소를 입력해주세요", fontSize = helperTextSize, color = Color(0xFF888888))

            Spacer(modifier = Modifier.height(28.dp))

            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 8.dp)) {
                Text(text = "이메일", fontSize = labelTextSize, fontWeight = FontWeight.Medium, color = Color(0xFF1A1A1A))
                Spacer(modifier = Modifier.width(6.dp))
                Text(text = "*", fontSize = labelTextSize, color = Color(0xFFFF3B30))
            }

            // Email input with trailing check icon
            Box(modifier = Modifier.fillMaxWidth().height(56.dp), contentAlignment = Alignment.CenterStart) {
                EmailInputField(
                    value = localEmail,
                    onValueChange = { localEmail = it },
                    placeholder = "example@email.com",
                    modifier = Modifier.fillMaxWidth()
                )

                Box(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = 14.dp)
                        .size(36.dp)
                        .background(
                            color = if (isEmailValid) Color(0xFFEFEFEF) else Color.Transparent,
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (isEmailValid) {
                        Icon(imageVector = Icons.Filled.Check, contentDescription = "valid", tint = Color(0xFF9B9B9B), modifier = Modifier.size(20.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(text = "입력하신 이메일로 보안 코드를 전송할 예정입니다", fontSize = helperTextSize, color = Color(0xFF888888))

            Spacer(modifier = Modifier.height(28.dp))

            // Button area
            Box(modifier = Modifier.fillMaxWidth().padding(top = 6.dp), contentAlignment = Alignment.Center) {
                val coroutineScope = rememberCoroutineScope()
                Box(modifier = Modifier.fillMaxWidth().shadow(elevation = 16.dp, shape = RoundedCornerShape(18.dp), clip = false)) {
                    AuthButton(
                        text = if (isLoading) "전송 중..." else "보안 코드 전송",
                        onClick = {
                            coroutineScope.launch {
                                viewModel.requestSecurityCode()
                            }
                        },
                        enabled = isEmailValid && !isLoading,
                        backgroundColor = Color(0xFFFF6633),
                        modifier = Modifier.fillMaxWidth().height(56.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Error / success feedback
            error?.let { err ->
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = err, color = Color(0xFFFF3B30), fontSize = errorTextSize)
            }
        }
    }
}