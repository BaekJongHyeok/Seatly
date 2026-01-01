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
    // ViewModel 상태 수집 (collectAsState 사용)
    val viewModelEmail by viewModel.email.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.error.collectAsState() // ViewModel의 error StateFlow 이름 확인 (error 또는 errorMessage)
    val currentStep by viewModel.currentStep.collectAsState() // 단계 변경 감지용

    // 로컬 입력 상태 관리
    var localEmail by remember { mutableStateOf(viewModelEmail) }

    // 코루틴 스코프 (suspend 함수 호출용)
    val scope = rememberCoroutineScope()

    // 단계가 2로 변경되면 다음 화면으로 이동 (성공 시 ViewModel이 2로 변경함)
    LaunchedEffect(currentStep) {
        if (currentStep == 2) {
            onNextNavigate()
        }
    }

    // 로컬 이메일 변경 시 ViewModel 업데이트
    LaunchedEffect(localEmail) {
        if (localEmail != viewModelEmail) {
            viewModel.updateEmail(localEmail)
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
                Icon(imageVector = Icons.Filled.ArrowBack, contentDescription = "뒤로", tint = Color(0xFF1A1A1A))
            },
            onLeftClick = onBack,
            titleTextStyle = TextStyle(fontSize = topBarTitleSize, fontWeight = FontWeight.Bold, color = Color(0xFF1A1A1A)),
            backgroundColor = Color.White,
            verticalPadding = 18.dp,
            buttonContainerSize = 44.dp,
            minHeight = 64.dp
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
            Text(text = "이메일 확인", fontSize = sectionTitleSize, fontWeight = FontWeight.SemiBold, color = Color(0xFF1A1A1A))
            Spacer(modifier = Modifier.height(6.dp))
            Text(text = "가입한 이메일 주소를 입력해주세요", fontSize = helperTextSize, color = Color(0xFF888888))
            Spacer(modifier = Modifier.height(28.dp))

            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 8.dp)) {
                Text(text = "이메일", fontSize = labelTextSize, fontWeight = FontWeight.Medium, color = Color(0xFF1A1A1A))
                Spacer(modifier = Modifier.width(6.dp))
                Text(text = "*", fontSize = labelTextSize, color = Color(0xFFFF3B30))
            }

            // 이메일 입력 필드
            Box(modifier = Modifier.fillMaxWidth().height(56.dp), contentAlignment = Alignment.CenterStart) {
                EmailInputField(
                    value = localEmail,
                    onValueChange = { localEmail = it },
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
                        Icon(imageVector = Icons.Filled.Check, contentDescription = "유효함", tint = Color(0xFF9B9B9B), modifier = Modifier.size(20.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            Text(text = "입력하신 이메일로 보안 코드를 전송할 예정입니다", fontSize = helperTextSize, color = Color(0xFF888888))
            Spacer(modifier = Modifier.height(28.dp))

            // 버튼 영역
            Box(modifier = Modifier.fillMaxWidth().padding(top = 6.dp), contentAlignment = Alignment.Center) {
                Box(modifier = Modifier.fillMaxWidth().shadow(elevation = 16.dp, shape = RoundedCornerShape(18.dp), clip = false)) {
                    AuthButton(
                        text = if (isLoading) "전송 중..." else "보안 코드 전송",
                        onClick = {
                            viewModel.updateEmail(localEmail)
                            // suspend 함수이므로 scope.launch 사용
                            scope.launch {
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

            // 에러 메시지 표시
            errorMessage?.let { err ->
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = err, color = Color(0xFFFF3B30), fontSize = errorTextSize)
            }
        }
    }
}
