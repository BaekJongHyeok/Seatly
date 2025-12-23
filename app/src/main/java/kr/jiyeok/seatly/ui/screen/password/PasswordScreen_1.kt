package kr.jiyeok.seatly.ui.screen.password

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.style.TextAlign
import androidx.lifecycle.viewmodel.compose.viewModel
import kr.jiyeok.seatly.ui.component.AuthButton
import kr.jiyeok.seatly.ui.component.EmailInputField
import kr.jiyeok.seatly.presentation.viewmodel.password.PasswordRecoveryViewModel

@Composable
fun PasswordScreen_1(
    viewModel: PasswordRecoveryViewModel = viewModel(),
    onBack: () -> Unit,
    onNextNavigate: () -> Unit
) {
    // Bind viewModel email to local text state for two-way binding convenience
    var localEmail by remember { mutableStateOf(viewModel.email) }

    LaunchedEffect(localEmail) {
        if (localEmail != viewModel.email) {
            // 이전에 setEmail(...)를 썼던 부분을 updateEmail(...)로 변경
            viewModel.updateEmail(localEmail)
        }
    }

    val isEmailValid = android.util.Patterns.EMAIL_ADDRESS.matcher(localEmail).matches()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 60.dp, bottom = 8.dp, start = 8.dp, end = 8.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clickable { onBack() }
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = Icons.Filled.ArrowBack, contentDescription = "뒤로", tint = Color(0xFF1A1A1A))
            }

            Box(modifier = Modifier.weight(1f).padding(top = 4.dp), contentAlignment = Alignment.Center) {
                Text(text = "비밀번호 찾기", fontSize = 32.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1A1A1A), textAlign = TextAlign.Center)
            }

            Spacer(modifier = Modifier.size(44.dp))
        }

        // Content
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).weight(1f).padding(top = 24.dp), verticalArrangement = Arrangement.Top) {
            Text(text = "이메일 확인", fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF1A1A1A))
            Spacer(modifier = Modifier.height(6.dp))
            Text(text = "가입한 이메일 주소를 입력해주세요", fontSize = 12.sp, color = Color(0xFF888888))

            Spacer(modifier = Modifier.height(28.dp))

            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 8.dp)) {
                Text(text = "이메일", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = Color(0xFF1A1A1A))
                Spacer(modifier = Modifier.width(6.dp))
                Text(text = "*", fontSize = 14.sp, color = Color(0xFFFF3B30))
            }

            // Email input with trailing check icon
            Box(modifier = Modifier.fillMaxWidth().height(56.dp), contentAlignment = Alignment.CenterStart) {
                EmailInputField(value = localEmail, onValueChange = { localEmail = it }, placeholder = "example@email.com", modifier = Modifier.fillMaxWidth())

                Box(modifier = Modifier.align(Alignment.CenterEnd).padding(end = 14.dp).size(36.dp).background(color = if (isEmailValid) Color(0xFFEFEFEF) else Color.Transparent, shape = CircleShape), contentAlignment = Alignment.Center) {
                    if (isEmailValid) {
                        Icon(imageVector = Icons.Filled.Check, contentDescription = "valid", tint = Color(0xFF9B9B9B), modifier = Modifier.size(20.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(text = "입력하신 이메일로 보안 코드를 전송할 예정입니다", fontSize = 12.sp, color = Color(0xFF888888))

            Spacer(modifier = Modifier.height(28.dp))

            // Button area
            Box(modifier = Modifier.fillMaxWidth().padding(top = 6.dp), contentAlignment = Alignment.Center) {
                Box(modifier = Modifier.fillMaxWidth().shadow(elevation = 16.dp, shape = RoundedCornerShape(18.dp), clip = false)) {
                    AuthButton(
                        text = if (viewModel.isLoading) "전송 중..." else "보안 코드 전송",
                        onClick = {
                            // set email in viewModel and request
                            viewModel.updateEmail(localEmail)
                            viewModel.requestSecurityCode(
                                onSuccess = {
                                    // navigate to step2
                                    onNextNavigate()
                                }
                            )
                        },
                        enabled = isEmailValid && !viewModel.isLoading,
                        backgroundColor = Color(0xFFFF6633),
                        modifier = Modifier.fillMaxWidth().height(60.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Error / success feedback
            viewModel.errorMessage?.let { err ->
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = err, color = Color(0xFFFF3B30), fontSize = 13.sp)
            }
        }
    }
}