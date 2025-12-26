package kr.jiyeok.seatly.ui.screen.common

import android.content.Context
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Checkbox
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import kr.jiyeok.seatly.R
import kr.jiyeok.seatly.data.remote.request.LoginRequest
import kr.jiyeok.seatly.presentation.viewmodel.AuthUiState
import kr.jiyeok.seatly.presentation.viewmodel.AuthViewModel
import kr.jiyeok.seatly.ui.component.AuthButton
import kr.jiyeok.seatly.ui.component.EmailInputField
import kr.jiyeok.seatly.ui.component.PasswordInputField

@Composable
fun LoginScreen(
    navController: NavController,
    viewModel: AuthViewModel = hiltViewModel()
) {
    val context = LocalContext.current

    // SharedPreferences (simple demo implementation)
    val prefsName = "seatly_prefs"
    val prefs = context.getSharedPreferences(prefsName, Context.MODE_PRIVATE)

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isAutoLogin by remember { mutableStateOf(false) }

    val authState by viewModel.authState.collectAsState()
    // Keep a copy of latest login data if needed
    val loginData by viewModel.loginData.collectAsState()

    // Try auto-login if enabled
    LaunchedEffect(Unit) {
        val auto = prefs.getBoolean("auto_login", false)
        if (auto) {
            val savedEmail = prefs.getString("saved_email", "") ?: ""
            val savedPassword = prefs.getString("saved_password", "") ?: ""
            if (savedEmail.isNotEmpty() && savedPassword.isNotEmpty()) {
                email = savedEmail
                password = savedPassword
                isAutoLogin = true
                viewModel.login(LoginRequest(savedEmail, savedPassword))
            } else {
                prefs.edit().putBoolean("auto_login", false).apply()
            }
        }
    }

    // On success, persist/clear auto-login and navigate
    LaunchedEffect(authState) {
        if (authState is AuthUiState.Success) {
            if (isAutoLogin) {
                prefs.edit()
                    .putBoolean("auto_login", true)
                    .putString("saved_email", email)
                    .putString("saved_password", password)
                    .apply()
            } else {
                prefs.edit()
                    .putBoolean("auto_login", false)
                    .remove("saved_email")
                    .remove("saved_password")
                    .apply()
            }

            navController.navigate("home") {
                popUpTo("login") { inclusive = true }
            }
        }
    }

    // Use explicit top arrangement so we can precisely control spacing (no scrolling)
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        // 추가: 로고 위 공백
        Spacer(modifier = Modifier.height(60.dp))

        // Top section (logo + title) - kept compact
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(color = Color(0xFFFF6B4A)),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.icon_seatly),
                    contentDescription = "Seatly Logo",
                    modifier = Modifier.size(60.dp)
                )
            }

            // 로고와 이메일 섹션 사이 공백을 줄이기 위해 내부 Spacer 축소
            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Seatly",
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF2C2C2C)
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = "당신의 집중을 위한 공간",
                fontSize = 12.sp,
                fontWeight = FontWeight.Normal,
                color = Color(0xFF999999)
            )
        }

        // 중간 섹션 (입력 필드 등) - 간격을 좀 더 압축
        Spacer(modifier = Modifier.height(24.dp)) // 전체적으로 로고-이메일 간격을 더 줄이기 위한 추가/조정

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Email
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "이메일",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF2C2C2C),
                    modifier = Modifier.padding(bottom = 6.dp)
                )
                EmailInputField(
                    value = email,
                    onValueChange = { email = it },
                    placeholder = "email@example.com"
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Password
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "비밀번호",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF2C2C2C),
                    modifier = Modifier.padding(bottom = 6.dp)
                )
                PasswordInputField(
                    value = password,
                    onValueChange = { password = it },
                    placeholder = "••••••••"
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Auto login + find password row (compact)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = isAutoLogin,
                        onCheckedChange = { checked ->
                            isAutoLogin = checked
                            if (checked) {
                                if (email.isNotEmpty() && password.isNotEmpty()) {
                                    prefs.edit()
                                        .putBoolean("auto_login", true)
                                        .putString("saved_email", email)
                                        .putString("saved_password", password)
                                        .apply()
                                } else {
                                    prefs.edit().putBoolean("auto_login", true).apply()
                                }
                            } else {
                                prefs.edit()
                                    .putBoolean("auto_login", false)
                                    .remove("saved_email")
                                    .remove("saved_password")
                                    .apply()
                            }
                        },
                        modifier = Modifier.size(18.dp)
                    )

                    Text(
                        text = "자동 로그인",
                        fontSize = 13.sp,
                        color = Color(0xFF2C2C2C),
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }

                Text(
                    text = "비밀번호 찾기",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFFFF6B4A),
                    modifier = Modifier
                        .clickable { navController.navigate("password_step1") }
                        .padding(4.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Error message (no background, orange text)
            if (authState is AuthUiState.Error) {
                Text(
                    text = (authState as AuthUiState.Error).message,
                    color = Color(0xFFFF6B4A),
                    fontSize = 13.sp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))
            }

            // Login button (compact)
            AuthButton(
                text = if (authState is AuthUiState.Loading) "로그인 중..." else "로그인",
                onClick = { viewModel.login(LoginRequest(email, password)) },
                enabled = authState !is AuthUiState.Loading,
            )

            // 로그인 버튼과 "-또는-" 사이 공백을 20.dp 줄이기 위해 Spacer를 작게 유지
            Spacer(modifier = Modifier.height(20.dp))
        }

        // Bottom section (social buttons + signup) — 간격 조정: 로그인 버튼과 Divider 사이 공백 축소
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Divider with "또는" (tight)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(1.dp)
                        .background(Color(0xFFDDDDDD))
                )

                Text(
                    text = "또는",
                    fontSize = 12.sp,
                    color = Color(0xFF999999),
                    modifier = Modifier.padding(horizontal = 12.dp)
                )

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(1.dp)
                        .background(Color(0xFFDDDDDD))
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Social buttons all circular and slightly smaller to fit one screen
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Google
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(Color.White)
                            .border(1.dp, Color(0xFFDDDDDD), CircleShape)
                            .clickable { /* Google login */ },
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.icon_google),
                            contentDescription = "Google",
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                // Kakao
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFFFEB00))
                            .clickable { /* Kakao login */ },
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.icon_kakao),
                            contentDescription = "Kakao",
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                // Naver (bigger "N" but still fitting)
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF00C73C))
                            .clickable { /* Naver login */ },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "N",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 24.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Sign up row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "계정이 없으신가요? ",
                    fontSize = 13.sp,
                    color = Color(0xFF666666)
                )

                Text(
                    text = "회원가입",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFFF6B4A),
                    modifier = Modifier.clickable { navController.navigate("signup") }
                )
            }
        }
    }
}