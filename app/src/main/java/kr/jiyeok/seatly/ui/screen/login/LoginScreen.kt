package kr.jiyeok.seatly.ui.screen.login

import androidx.compose.foundation.Image
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import kr.jiyeok.seatly.R
import kr.jiyeok.seatly.ui.component.AuthButton
import kr.jiyeok.seatly.ui.component.EmailInputField
import kr.jiyeok.seatly.ui.component.PasswordInputField
import kr.jiyeok.seatly.ui.component.SocialLoginButton

@Composable
fun LoginScreen(navController: NavController) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isAutoLogin by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(horizontal = 24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(60.dp))

        // Logo and Title
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(
                    color = Color(0xFFFF6B4A),
                ),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.icon_seatly),
                contentDescription = "Seatly Logo",
                modifier = Modifier.size(58.dp)
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "Seatly",
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF2C2C2C)
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "당신의 집중을 위한 공간",
            fontSize = 14.sp,
            fontWeight = FontWeight.Normal,
            color = Color(0xFF999999)
        )

        Spacer(modifier = Modifier.height(40.dp))

        // Email Input
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "이메일",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF2C2C2C),
                modifier = Modifier.padding(bottom = 8.dp)
            )

            EmailInputField(
                value = email,
                onValueChange = { email = it },
                placeholder = "email@example.com"
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Password Input
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "비밀번호",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF2C2C2C),
                modifier = Modifier.padding(bottom = 8.dp)
            )

            PasswordInputField(
                value = password,
                onValueChange = { password = it },
                placeholder = "••••••••••"
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Auto Login Checkbox and Find Password
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                androidx.compose.material3.Checkbox(
                    checked = isAutoLogin,
                    onCheckedChange = { isAutoLogin = it },
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = "자동 로그인",
                    fontSize = 14.sp,
                    color = Color(0xFF2C2C2C),
                    modifier = Modifier.padding(start = 8.dp)
                )
            }

            Text(
                text = "비밀번호 찾기",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFFFF6B4A)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Login Button
        AuthButton(
            text = "로그인",
            onClick = {
                // Handle login
                navController.navigate("home") {
                    popUpTo("login") { inclusive = true }
                }
            }
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Divider with Text
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
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
                fontSize = 14.sp,
                color = Color(0xFF999999),
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(1.dp)
                    .background(Color(0xFFDDDDDD))
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Social Login Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SocialLoginButton(
                icon = painterResource(id = R.drawable.icon_google),
                contentDescription = "Google",
                modifier = Modifier.weight(1f),
                onClick = {
                    // Handle Google login
                }
            )

            SocialLoginButton(
                icon = painterResource(id = R.drawable.icon_kakao),
                contentDescription = "Kakao",
                modifier = Modifier.weight(1f),
                backgroundColor = Color(0xFFFFEB00),
                onClick = {
                    // Handle Kakao login
                }
            )

            SocialLoginButton(
                icon = painterResource(id = R.drawable.icon_naver),
                contentDescription = "Naver",
                modifier = Modifier.weight(1f),
                backgroundColor = Color(0xFF00C73C),
                onClick = {
                    // Handle Naver login
                }
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Sign Up Link
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "계정이 없으신가요? ",
                fontSize = 14.sp,
                color = Color(0xFF666666)
            )
            Text(
                text = "회원가입",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFFF6B4A)
            )
        }

        Spacer(modifier = Modifier.height(40.dp))
    }
}
