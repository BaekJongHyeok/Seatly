package kr.jiyeok.seatly.ui.screen.common.password

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
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
import kr.jiyeok.seatly.ui.component.common.PasswordInputField
import kr.jiyeok.seatly.ui.component.common.AppTopBar

// Helper functions (Defined outside Composable)
private fun hasUpper(pw: String) = pw.any { it.isUpperCase() }
private fun hasLower(pw: String) = pw.any { it.isLowerCase() }
private fun hasDigit(pw: String) = pw.any { it.isDigit() }
private fun hasSpecial(pw: String) = pw.any { "!@#$%^&*()_+-=[]{}|;':\",./<>?/`~".contains(it) }

@Composable
fun PasswordScreen3(
    emailArg: String? = null,
    onBack: () -> Unit,
    onCompleteNavigate: () -> Unit
) {
    // ★ Mock 상태 관리 (ViewModel 없이 로컬 상태 사용)
    var password by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var validationError by remember { mutableStateOf<String?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val scope = rememberCoroutineScope()

    val strength = remember(password) {
        when {
            password.length >= 12 -> 1.0f
            password.length >= 10 -> 0.8f
            password.length >= 8 -> 0.6f
            password.length >= 6 -> 0.3f
            else -> 0.0f
        }
    }

    val isMatch = password.isNotEmpty() && password == confirm
    val isPasswordValid = password.length >= 8 &&
            hasLower(password) &&
            hasUpper(password) &&
            hasDigit(password) &&
            hasSpecial(password)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        // AppTopBar for consistent header
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

        // divider + spacing to separate topbar from content
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
                text = "새 비밀번호 설정",
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF1A1A1A)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "새로운 비밀번호를 설정해주세요",
                fontSize = 12.sp,
                color = Color(0xFF888888)
            )
            Spacer(modifier = Modifier.height(20.dp))

            // 계정 이메일 표시
            Text(
                text = "계정 이메일",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF1A1A1A)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .background(color = Color(0xFFF5F5F5), shape = RoundedCornerShape(12.dp))
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Text(
                    text = emailArg ?: "example@email.com",
                    fontSize = 16.sp,
                    color = Color(0xFF1A1A1A)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 새 비밀번호
            Text(
                text = "새 비밀번호 *",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF1A1A1A)
            )
            Spacer(modifier = Modifier.height(8.dp))
            PasswordInputField(
                value = password,
                onValueChange = {
                    password = it
                    validationError = null
                    errorMessage = null
                },
                placeholder = "••••••••••",
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "8자 이상, 대소문자/숫자/특수문자 포함",
                fontSize = 11.sp,
                color = Color(0xFF888888)
            )

            // 강도 표시기
            Spacer(modifier = Modifier.height(12.dp))
            Box(modifier = Modifier.fillMaxWidth()) {
                // 배경 바
                Box(
                    modifier = Modifier
                        .height(4.dp)
                        .fillMaxWidth()
                        .background(color = Color(0xFFECECEC), shape = RoundedCornerShape(4.dp))
                )
                // 진행률 바
                Box(
                    modifier = Modifier
                        .height(4.dp)
                        .fillMaxWidth(strength)
                        .background(
                            color = when {
                                strength >= 0.8f -> Color(0xFF34C759)
                                strength >= 0.6f -> Color(0xFFFF9500)
                                else -> Color(0xFFFF3B30)
                            },
                            shape = RoundedCornerShape(4.dp)
                        )
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 비밀번호 확인
            Text(
                text = "새 비밀번호 확인 *",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF1A1A1A)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Box(modifier = Modifier.fillMaxWidth()) {
                PasswordInputField(
                    value = confirm,
                    onValueChange = {
                        confirm = it
                        validationError = null
                        errorMessage = null
                    },
                    placeholder = "••••••••••",
                    modifier = Modifier.fillMaxWidth()
                )

                // 일치 아이콘
                if (isMatch && confirm.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .padding(end = 14.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.CheckCircle,
                            contentDescription = "match",
                            tint = Color(0xFFFF6633)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 검증 항목 표시
            if (password.isNotEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFF5F5F5), RoundedCornerShape(12.dp))
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    ValidationItem("8자 이상", password.length >= 8)
                    ValidationItem("소문자 포함", hasLower(password))
                    ValidationItem("대문자 포함", hasUpper(password))
                    ValidationItem("숫자 포함", hasDigit(password))
                    ValidationItem("특수문자 포함", hasSpecial(password))
                    ValidationItem("비밀번호 일치", isMatch)
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            // 클라이언트 검증 에러
            validationError?.let { err ->
                Text(
                    text = err,
                    color = Color(0xFFFF3B30),
                    fontSize = 13.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            // 서버 에러
            errorMessage?.let { err ->
                Text(
                    text = err,
                    color = Color(0xFFFF3B30),
                    fontSize = 13.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
        }

        // 변경 완료 버튼
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(elevation = 8.dp, shape = RoundedCornerShape(16.dp))
            ) {
                AuthButton(
                    text = if (isLoading) "변경 중..." else "변경 완료",
                    onClick = {
                        validationError = null
                        errorMessage = null

                        // 클라이언트 검증
                        val missing = mutableListOf<String>()
                        if (password.length < 8) missing += "8자 이상"
                        if (!hasLower(password)) missing += "소문자"
                        if (!hasUpper(password)) missing += "대문자"
                        if (!hasDigit(password)) missing += "숫자"
                        if (!hasSpecial(password)) missing += "특수문자"
                        if (password != confirm) missing += "비밀번호 일치"

                        if (missing.isNotEmpty()) {
                            validationError = "다음 항목을 확인하세요: ${missing.joinToString(", ")}"
                            return@AuthButton
                        }

                        // Mock: 2초 후 성공 처리
                        isLoading = true
                        scope.launch {
                            delay(2000)
                            isLoading = false
                            onCompleteNavigate() // 로그인 화면으로 이동
                            // 실제 API: viewModel.resetPassword(password)
                        }
                    },
                    enabled = isPasswordValid && isMatch && !isLoading,
                    backgroundColor = Color(0xFFFF6633),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp)
                )
            }
        }
    }
}

// 검증 항목 컴포넌트
@Composable
fun ValidationItem(label: String, isValid: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = Icons.Filled.CheckCircle,
            contentDescription = "check",
            tint = if (isValid) Color(0xFF34C759) else Color(0xFFCCCCCC),
            modifier = Modifier.size(18.dp)
        )
        Text(
            text = label,
            fontSize = 12.sp,
            color = if (isValid) Color(0xFF34C759) else Color(0xFF888888)
        )
    }
}
