package kr.jiyeok.seatly.ui.screen.common.password

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import kr.jiyeok.seatly.presentation.viewmodel.PasswordRecoveryViewModel
import kr.jiyeok.seatly.ui.component.AuthButton
import kr.jiyeok.seatly.ui.component.PasswordInputField
import kr.jiyeok.seatly.ui.component.common.AppTopBar

@Composable
fun PasswordScreen_3(
    viewModel: PasswordRecoveryViewModel = hiltViewModel(),
    emailArg: String? = null,
    onBack: () -> Unit,
    onCompleteNavigate: () -> Unit
) {
    // Observe ViewModel states
    val email by viewModel.email.collectAsState()
    val error by viewModel.error.collectAsState()
    
    var password by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }

    // If emailArg given, set it in viewModel (keeps shared state)
    LaunchedEffect(emailArg) {
        emailArg?.let { viewModel.updateEmail(it) }
    }

    // Local validation error message (client-side)
    var validationError by remember { mutableStateOf<String?>(null) }

    val strength = remember(password) {
        when {
            password.length >= 12 -> 1.0f
            password.length >= 10 -> 0.8f
            password.length >= 8 -> 0.6f
            password.length >= 6 -> 0.3f
            else -> 0.0f
        }
    }

    // Helper checks
    fun hasUpper(pw: String) = pw.any { it.isUpperCase() }
    fun hasLower(pw: String) = pw.any { it.isLowerCase() }
    fun hasDigit(pw: String) = pw.any { it.isDigit() }
    fun hasSpecial(pw: String) = pw.any { "!@#\$%^&*()_+-=[]{}|;':\",.<>?/`~".contains(it) }

    val isValidForm =
        password.length >= 8 &&
                hasLower(password) &&
                hasUpper(password) &&
                hasDigit(password) &&
                hasSpecial(password) &&
                password == confirm

    val isMatch = password.isNotEmpty() && password == confirm

    Column(modifier = Modifier.fillMaxSize().background(Color.White)) {
        // AppTopBar for consistent header
        AppTopBar(
            title = "비밀번호 찾기",
            leftContent = {
                Icon(imageVector = Icons.Filled.ArrowBack, contentDescription = "뒤로", tint = Color(0xFF1A1A1A))
            },
            onLeftClick = onBack,
            titleTextStyle = TextStyle(fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1A1A1A)),
            backgroundColor = Color.White,
            verticalPadding = 18.dp,
            buttonContainerSize = 44.dp,
            minHeight = 64.dp
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
            Text(text = "새 비밀번호 설정", fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF1A1A1A))
            Spacer(modifier = Modifier.height(6.dp))
            Text(text = "새로운 비밀번호를 설정해주세요", fontSize = 12.sp, color = Color(0xFF888888))

            Spacer(modifier = Modifier.height(20.dp))

            Text(text = "계정 이메일", fontSize = 14.sp, color = Color(0xFF1A1A1A))
            Spacer(modifier = Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .background(color = Color(0xFFF5F5F5), shape = RoundedCornerShape(12.dp))
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Text(text = email.ifBlank { emailArg ?: "example@email.com" }, fontSize = 16.sp, color = Color(0xFF1A1A1A))
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(text = "새 비밀번호 *", fontSize = 14.sp, color = Color(0xFF1A1A1A))
            Spacer(modifier = Modifier.height(8.dp))

            PasswordInputField(value = password, onValueChange = {
                password = it
                validationError = null
                viewModel.clearError()
            }, placeholder = "••••••••••", modifier = Modifier.fillMaxWidth())

            Spacer(modifier = Modifier.height(8.dp))

            Text(text = "8자 이상, 대소문자/숫자/특수문자 포함", fontSize = 11.sp, color = Color(0xFF888888))

            Spacer(modifier = Modifier.height(12.dp))

            Box(modifier = Modifier.fillMaxWidth()) {
                Box(modifier = Modifier.height(4.dp).fillMaxWidth().background(color = Color(0xFFECECEC), shape = RoundedCornerShape(4.dp)))
                Box(modifier = Modifier.height(4.dp).fillMaxWidth(strength).background(color = Color(0xFFFF6633), shape = RoundedCornerShape(4.dp)))
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(text = "새 비밀번호 확인 *", fontSize = 14.sp, color = Color(0xFF1A1A1A))
            Spacer(modifier = Modifier.height(8.dp))

            Box(modifier = Modifier.fillMaxWidth()) {
                PasswordInputField(value = confirm, onValueChange = {
                    confirm = it
                    validationError = null
                    viewModel.clearError()
                }, placeholder = "••••••••••", modifier = Modifier.fillMaxWidth())

                if (isMatch) {
                    Box(modifier = Modifier.align(Alignment.CenterEnd).padding(end = 14.dp)) {
                        Icon(imageVector = Icons.Filled.CheckCircle, contentDescription = "match", tint = Color(0xFFFF6633))
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Client-side validation error messages
            validationError?.let { err ->
                Text(text = err, color = Color(0xFFFF3B30), fontSize = 13.sp)
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Server-side error from ViewModel (read-only)
            error?.let { err ->
                Text(text = err, color = Color(0xFFFF3B30), fontSize = 13.sp)
                Spacer(modifier = Modifier.height(8.dp))
            }
        }

        Box(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Box(modifier = Modifier.fillMaxWidth().shadow(elevation = 8.dp, shape = RoundedCornerShape(16.dp))) {
                AuthButton(
                    text = "변경 완료",
                    onClick = {
                        validationError = null
                        viewModel.clearError()

                        val missing = mutableListOf<String>()
                        if (password.length < 8) missing += "8자 이상이어야 합니다"
                        if (!hasLower(password)) missing += "소문자 포함"
                        if (!hasUpper(password)) missing += "대문자 포함"
                        if (!hasDigit(password)) missing += "숫자 포함"
                        if (!hasSpecial(password)) missing += "특수문자 포함"
                        if (password != confirm) missing += "비밀번호가 일치하지 않습니다"

                        if (missing.isNotEmpty()) {
                            validationError = "다음 항목을 확인하세요: ${missing.joinToString(", ")}"
                            return@AuthButton
                        }

                        // TODO: Password reset needs to be implemented with AuthViewModel.changePassword
                        // For now, just navigate to completion
                        onCompleteNavigate()
                    },
                    enabled = true,
                    backgroundColor = Color(0xFFFF6633),
                    modifier = Modifier.fillMaxWidth().height(60.dp)
                )
            }
        }
    }
}

// helper fns used inside composable
private fun hasUpper(pw: String) = pw.any { it.isUpperCase() }
private fun hasLower(pw: String) = pw.any { it.isLowerCase() }
private fun hasDigit(pw: String) = pw.any { it.isDigit() }
private fun hasSpecial(pw: String) = pw.any { "!@#\$%^&*()_+-=[]{}|;':\",.<>?/`~".contains(it) }