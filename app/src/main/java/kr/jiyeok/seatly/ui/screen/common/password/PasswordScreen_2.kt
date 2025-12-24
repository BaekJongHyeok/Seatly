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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import kr.jiyeok.seatly.presentation.viewmodel.password.PasswordRecoveryViewModel
import kr.jiyeok.seatly.ui.component.AuthButton
import kr.jiyeok.seatly.ui.component.common.AppTopBar

@Composable
fun PasswordScreen_2(
    viewModel: PasswordRecoveryViewModel = viewModel(),
    onBack: () -> Unit,
    onVerifiedNavigate: () -> Unit
) {
    var code by remember { mutableStateOf("") }
    val localSeconds by remember { derivedStateOf { viewModel.secondsLeft } }
    val isComplete = code.length == 6
    val focusManager = LocalFocusManager.current

    Column(modifier = Modifier.fillMaxSize().background(Color.White)) {
        // AppTopBar 사용 (일관된 상단 바)
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
            Text(text = "보안 코드 검증", fontSize = 18.sp, color = Color(0xFF1A1A1A))
            Spacer(modifier = Modifier.height(6.dp))
            Text(text = "이메일로 전송된 6자리 보안 코드를 입력해주세요", fontSize = 12.sp, color = Color(0xFF888888))

            Spacer(modifier = Modifier.height(28.dp))

            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 8.dp)) {
                Text(text = "보안 코드", fontSize = 14.sp, color = Color(0xFF1A1A1A))
                Spacer(modifier = Modifier.width(6.dp))
                Text(text = "*", fontSize = 14.sp, color = Color(0xFFFF3B30))
            }

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
                    },
                    singleLine = true,
                    cursorBrush = SolidColor(Color(0xFFFF6633)),
                    textStyle = TextStyle(color = Color.Transparent),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                ) { innerTextField ->
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterStart) {
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

                        Box(modifier = Modifier.matchParentSize()) {
                            innerTextField()
                        }
                    }
                }

                Box(modifier = Modifier.align(Alignment.CenterEnd).size(36.dp), contentAlignment = Alignment.Center) {
                    if (isComplete) {
                        Icon(imageVector = Icons.Filled.CheckCircle, contentDescription = "완료", tint = Color(0xFFFF6633), modifier = Modifier.size(22.dp))
                    } else {
                        Icon(imageVector = Icons.Filled.Lock, contentDescription = "lock", tint = Color(0xFF9B9B9B), modifier = Modifier.size(22.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(text = "이메일로 전송된 6자리 코드를 입력하세요", fontSize = 12.sp, color = Color(0xFF888888))

            Spacer(modifier = Modifier.height(6.dp))

            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 2.dp)) {
                Text(text = "코드를 받지 못했나요?", fontSize = 12.sp, color = Color(0xFF888888))
                Spacer(modifier = Modifier.width(6.dp))
                Text(text = "다시 전송", fontSize = 12.sp, color = Color(0xFFFF6633), modifier = Modifier.clickable {
                    viewModel.resendSecurityCode()
                })
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = "|", fontSize = 12.sp, color = Color(0xFFCCCCCC))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = String.format("%02d:%02d", localSeconds / 60, localSeconds % 60),
                    fontSize = 12.sp,
                    color = Color(0xFF888888),
                    fontFamily = FontFamily.Monospace
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            Box(modifier = Modifier.fillMaxWidth().padding(top = 6.dp), contentAlignment = Alignment.Center) {
                Box(modifier = Modifier.fillMaxWidth().shadow(elevation = 18.dp, shape = RoundedCornerShape(26.dp), clip = false)) {
                    AuthButton(
                        text = if (viewModel.isLoading) "검증 중..." else "검증",
                        onClick = {
                            focusManager.clearFocus()
                            viewModel.verifyCode(code, onSuccess = {
                                onVerifiedNavigate()
                            })
                        },
                        enabled = isComplete && !viewModel.isLoading,
                        backgroundColor = Color(0xFFFF6633),
                        modifier = Modifier.fillMaxWidth().height(60.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            viewModel.errorMessage?.let { err ->
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = err, color = Color(0xFFFF3B30), fontSize = 13.sp)
            }
        }
    }
}