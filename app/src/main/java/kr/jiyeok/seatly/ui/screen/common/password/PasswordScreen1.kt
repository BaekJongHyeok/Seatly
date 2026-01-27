package kr.jiyeok.seatly.ui.screen.common.password

import android.util.Patterns
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kr.jiyeok.seatly.ui.component.common.AuthButton
import kr.jiyeok.seatly.ui.component.common.EmailInputField
import kr.jiyeok.seatly.ui.component.common.AppTopBar

@Composable
fun PasswordScreen1(
    onBack: () -> Unit,
    onNextNavigate: () -> Unit
) {
    // ★ Mock 상태 관리 (ViewModel 없이 로컬 상태 사용)
    var localEmail by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var currentStep by remember { mutableStateOf(1) }

    val scope = rememberCoroutineScope()

    // 단계가 2로 변경되면 다음 화면으로 이동
    LaunchedEffect(currentStep) {
        if (currentStep == 2) {
            onNextNavigate()
        }
    }

    val isEmailValid = Patterns.EMAIL_ADDRESS.matcher(localEmail).matches()

    // 텍스트 스타일 정의
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
        // 상단 바
        AppTopBar(
            title = "비밀번호 찾기",
            leftContent = {
                Icon(
                    imageVector = Icons.Filled.ArrowBack,
                    contentDescription = "뒤로",
                    tint = Color(0xFF1A1A1A)
                )
            }
        )

        // 구분선
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(Color(0xFFEEEEEE))
        )

        Spacer(modifier = Modifier.height(12.dp))

        // 메인 콘텐츠
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .weight(1f)
                .padding(top = 8.dp),
            verticalArrangement = Arrangement.Top
        ) {
            Text(
                text = "이메일 확인",
                fontSize = sectionTitleSize,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF1A1A1A)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "가입한 이메일 주소를 입력해주세요",
                fontSize = helperTextSize,
                color = Color(0xFF888888)
            )
            Spacer(modifier = Modifier.height(28.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 8.dp)
            ) {
                Text(
                    text = "이메일",
                    fontSize = labelTextSize,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF1A1A1A)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "*",
                    fontSize = labelTextSize,
                    color = Color(0xFFFF3B30)
                )
            }

            // 이메일 입력 필드
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                EmailInputField(
                    value = localEmail,
                    onValueChange = { newValue ->
                        localEmail = newValue
                        errorMessage = null // 입력 시 에러 초기화
                    },
                    placeholder = "example@email.com",
                    modifier = Modifier.fillMaxWidth()
                )

                // 유효성 체크 아이콘
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
                        Icon(
                            imageVector = Icons.Filled.Check,
                            contentDescription = "유효함",
                            tint = Color(0xFF9B9B9B),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "입력하신 이메일로 보안 코드를 전송할 예정입니다",
                fontSize = helperTextSize,
                color = Color(0xFF888888)
            )
            Spacer(modifier = Modifier.height(28.dp))

            // 버튼 영역
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(
                            elevation = 16.dp,
                            shape = RoundedCornerShape(18.dp),
                            clip = false
                        )
                ) {
                    AuthButton(
                        text = if (isLoading) "전송 중..." else "보안 코드 전송",
                        onClick = {
                            if (isEmailValid && !isLoading) {
                                isLoading = true
                                scope.launch {
                                    // ★ Mock: 2초 후 성공 처리
                                    delay(2000)
                                    isLoading = false
                                    currentStep = 2 // 다음 단계로 이동
                                    // 실제 API 호출은 ViewModel에서 처리하면 됨:
                                    // viewModel.requestSecurityCode()
                                }
                            }
                        },
                        enabled = isEmailValid && !isLoading,
                        backgroundColor = Color(0xFFFF6633),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 에러 메시지 표시
            errorMessage?.let { err ->
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = err,
                    color = Color(0xFFFF3B30),
                    fontSize = errorTextSize
                )
            }
        }
    }
}
