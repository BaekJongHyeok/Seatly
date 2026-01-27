package kr.jiyeok.seatly.ui.screen.common.password

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kr.jiyeok.seatly.ui.component.common.AuthButton
import kr.jiyeok.seatly.ui.component.common.AppTopBar

@Composable
fun PasswordScreen2(
    onBack: () -> Unit,
    onVerifiedNavigate: () -> Unit
) {
    // ★ Mock 상태 관리 (ViewModel 없이 로컬 상태 사용)
    var code by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var currentStep by remember { mutableStateOf(2) }
    var secondsLeft by remember { mutableStateOf(300) } // 5분 (300초)

    val isComplete = code.length == 6
    val focusManager = LocalFocusManager.current
    val scope = rememberCoroutineScope()

    // 타이머 로직 (1초마다 감소)
    LaunchedEffect(Unit) {
        while (secondsLeft > 0) {
            kotlinx.coroutines.delay(1000)
            secondsLeft--
        }
    }

    // 단계가 3으로 변경되면 다음 화면으로 이동
    LaunchedEffect(currentStep) {
        if (currentStep == 3) {
            onVerifiedNavigate()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
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

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(Color(0xFFEEEEEE))
        )

        Spacer(modifier = Modifier.height(12.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .weight(1f)
                .padding(top = 8.dp),
            verticalArrangement = Arrangement.Top
        ) {
            Text(
                text = "보안 코드 검증",
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF1A1A1A)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "이메일로 전송된 6자리 보안 코드를 입력해주세요",
                fontSize = 12.sp,
                color = Color(0xFF888888)
            )
            Spacer(modifier = Modifier.height(28.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 8.dp)
            ) {
                Text(
                    text = "보안 코드",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF1A1A1A)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "*",
                    fontSize = 14.sp,
                    color = Color(0xFFFF3B30)
                )
            }

            // 보안 코드 입력 필드
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .background(color = Color(0xFFF8F8F8), shape = RoundedCornerShape(18.dp))
                    .padding(horizontal = 18.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                BasicTextField(
                    value = code,
                    onValueChange = { input ->
                        val digits = input.filter { it.isDigit() }
                        code = if (digits.length <= 6) digits else digits.take(6)
                        errorMessage = null // 입력 시 에러 초기화
                    },
                    singleLine = true,
                    cursorBrush = SolidColor(Color(0xFFFF6633)),
                    textStyle = TextStyle(color = Color.Transparent),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    decorationBox = { innerTextField ->
                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            if (code.isEmpty()) {
                                Text(
                                    text = "000000",
                                    fontSize = 16.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = Color(0xFFBDBDBD),
                                    letterSpacing = 12.sp
                                )
                            } else {
                                val spaced = code.chunked(1).joinToString(" ")
                                Text(
                                    text = spaced,
                                    fontSize = 16.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = Color(0xFF2C2C2C),
                                    letterSpacing = 12.sp
                                )
                            }

                            // 입력 필드 (투명)
                            Box(modifier = Modifier.matchParentSize()) {
                                innerTextField()
                            }
                        }
                    }
                )

                // 체크 아이콘
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .size(36.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (isComplete) {
                        Icon(
                            imageVector = Icons.Filled.CheckCircle,
                            contentDescription = "완료",
                            tint = Color(0xFFFF6633),
                            modifier = Modifier.size(22.dp)
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Filled.Lock,
                            contentDescription = "lock",
                            tint = Color(0xFF9B9B9B),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 코드 재전송 및 타이머
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 2.dp)
            ) {
                Text(
                    text = "코드를 받지 못했나요?",
                    fontSize = 12.sp,
                    color = Color(0xFF888888)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "다시 전송",
                    fontSize = 12.sp,
                    color = Color(0xFFFF6633),
                    modifier = Modifier.clickable {
                        // ★ Mock: 코드 재전송 (실제로는 API 호출)
                        scope.launch {
                            isLoading = true
                            kotlinx.coroutines.delay(1000)
                            isLoading = false
                            secondsLeft = 300 // 타이머 리셋
                            // 실제 API: viewModel.resendSecurityCode()
                        }
                    }
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = "|", fontSize = 12.sp, color = Color(0xFFCCCCCC))
                Spacer(modifier = Modifier.width(8.dp))

                // 타이머 표시
                Text(
                    text = String.format("%02d:%02d", secondsLeft / 60, secondsLeft % 60),
                    fontSize = 12.sp,
                    color = if (secondsLeft < 60) Color(0xFFFF3B30) else Color(0xFF888888),
                    fontFamily = FontFamily.Monospace
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            // 검증 버튼
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
                            elevation = 18.dp,
                            shape = RoundedCornerShape(26.dp),
                            clip = false
                        )
                ) {
                    AuthButton(
                        text = if (isLoading) "검증 중..." else "검증",
                        onClick = {
                            if (isComplete && !isLoading) {
                                focusManager.clearFocus()
                                isLoading = true
                                scope.launch {
                                    // ★ Mock: 2초 후 성공 처리
                                    delay(2000)
                                    isLoading = false
                                    currentStep = 3 // 다음 단계로 이동
                                    // 실제 API: viewModel.verifyCode(code)
                                }
                            }
                        },
                        enabled = isComplete && !isLoading,
                        backgroundColor = Color(0xFFFF6633),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(60.dp)
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
                    fontSize = 13.sp
                )
            }
        }
    }
}
