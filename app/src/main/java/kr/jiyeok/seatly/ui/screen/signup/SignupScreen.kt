package kr.jiyeok.seatly.ui.screen.signup

import android.util.Patterns
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.regex.Pattern

/**
 * SignupScreen.kt
 *
 * Adjusted for the project's package (kr.jiyeok.seatly.ui.screen.signup) and fixed compile errors:
 * - Correct KeyboardOptions / KeyboardType imports from androidx.compose.ui.text.input
 * - Annotated composable with @OptIn(ExperimentalMaterial3Api::class) to silence material3 experimental warning
 * - Used TopAppBarDefaults.topAppBarColors(...) to avoid unresolved smallTopAppBarColors
 *
 * Paste this file into: app/src/main/java/kr/jiyeok/seatly/ui/screen/signup/SignupScreen.kt
 */

private val PrimaryColor = Color(0xFFE95321)
private val InputBg = Color(0xFFF8F8F8)
private val InputBorder = Color(0xFFE8E8E8)
private val TextPrimary = Color(0xFF1A1A1A)
private val TextHelper = Color(0xFF888888)
private val ErrorColor = Color(0xFFFF453A)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignupScreen(
    onBack: () -> Unit = {},
    onNext: (email: String, password: String) -> Unit = { _, _ -> }
) {
    val scrollState = rememberScrollState()

    // Form states
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPassword by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }

    // Agreements
    var agreeTerms by remember { mutableStateOf(false) }
    var agreePrivacy by remember { mutableStateOf(true) } // matches screenshot where privacy is checked

    // Derived validation
    val isEmailValid = remember(email) { Patterns.EMAIL_ADDRESS.matcher(email).matches() }
    val isPasswordValid = remember(password) { passwordStrengthScore(password) >= 2 } // simple threshold
    val doPasswordsMatch = remember(password, confirmPassword) { password.isNotBlank() && password == confirmPassword }
    val isNameValid = remember(name) { name.trim().length in 2..20 && isHangul(name.trim()) }
    val isPhoneValid = remember(phone) { PHONE_PATTERN.matcher(phone).matches() }

    // Overall enable for next button
    val canProceed =
        isEmailValid && isPasswordValid && doPasswordsMatch && isNameValid && isPhoneValid && agreePrivacy && agreeTerms

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Box(Modifier.fillMaxWidth()) {
                        Text(
                            "회원가입",
                            modifier = Modifier.align(Alignment.Center),
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.Default.ChevronLeft,
                            contentDescription = "뒤로",
                            tint = TextPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White.copy(alpha = 0.95f)
                )
            )
        },
        bottomBar = {
            Column(
                Modifier
                    .background(Color.White)
                    .padding(horizontal = 16.dp, vertical = 18.dp)
            ) {
                Button(
                    onClick = { if (canProceed) onNext(email, password) },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryColor),
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp)
                ) {
                    Text("다음", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    "뒤로가기",
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .clickable { onBack() },
                    color = TextHelper,
                    fontSize = 14.sp
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp)
                .fillMaxSize()
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            // Email
            LabelWithRequired(text = "이메일")
            Spacer(modifier = Modifier.height(8.dp))
            InputBox(
                value = email,
                onValueChange = { email = it },
                placeholder = "example@email.com",
                trailingIcon = {
                    if (email.isNotBlank() && isEmailValid) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier
                                .size(24.dp)
                                .background(PrimaryColor, shape = RoundedCornerShape(12.dp))
                                .padding(2.dp)
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = TextHelper,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            )
            Spacer(modifier = Modifier.height(20.dp))

            // Password
            LabelWithRequired(text = "비밀번호")
            Spacer(modifier = Modifier.height(8.dp))
            InputBox(
                value = password,
                onValueChange = { password = it },
                placeholder = "••••••••",
                trailingIcon = {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        if (passwordVisible) {
                            Icon(imageVector = Icons.Default.Visibility, contentDescription = "visible", tint = TextHelper)
                        } else {
                            Icon(imageVector = Icons.Default.VisibilityOff, contentDescription = "hidden", tint = TextHelper)
                        }
                    }
                },
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation()
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "8자 이상, 대소문자/숫자/특수문자 포함",
                color = TextHelper,
                fontSize = 11.sp,
                modifier = Modifier.padding(start = 4.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            // strength bar
            val strengthScore = passwordStrengthScore(password)
            StrengthBar(strengthScore = strengthScore)
            Spacer(modifier = Modifier.height(20.dp))

            // Confirm password
            LabelWithRequired(text = "비밀번호 확인")
            Spacer(modifier = Modifier.height(8.dp))
            InputBox(
                value = confirmPassword,
                onValueChange = { confirmPassword = it },
                placeholder = "••••••••",
                trailingIcon = {
                    if (confirmPassword.isNotBlank() && doPasswordsMatch) {
                        Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier
                            .size(24.dp)
                            .background(PrimaryColor, shape = RoundedCornerShape(12.dp))
                            .padding(2.dp))
                    } else {
                        Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = TextHelper, modifier = Modifier.size(20.dp))
                    }
                },
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation()
            )
            Spacer(modifier = Modifier.height(20.dp))

            // Name
            LabelWithRequired(text = "이름")
            Spacer(modifier = Modifier.height(8.dp))
            InputBox(
                value = name,
                onValueChange = { name = it },
                placeholder = "홍길동",
                trailingIcon = {
                    if (name.isNotBlank() && isNameValid) {
                        Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier
                            .size(24.dp)
                            .background(PrimaryColor, shape = RoundedCornerShape(12.dp))
                            .padding(2.dp))
                    } else {
                        Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = TextHelper, modifier = Modifier.size(20.dp))
                    }
                }
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text("2-20자 한글", color = TextHelper, fontSize = 11.sp)
            Spacer(modifier = Modifier.height(20.dp))

            // Phone
            LabelWithRequired(text = "휴대폰번호")
            Spacer(modifier = Modifier.height(8.dp))
            InputBox(
                value = phone,
                onValueChange = { phone = it },
                placeholder = "010-0000-0000",
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                trailingIcon = {
                    if (phone.isNotBlank() && isPhoneValid) {
                        Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier
                            .size(24.dp)
                            .background(PrimaryColor, shape = RoundedCornerShape(12.dp))
                            .padding(2.dp))
                    } else {
                        Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = TextHelper, modifier = Modifier.size(20.dp))
                    }
                }
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text("010-1234-5678 형식", color = TextHelper, fontSize = 11.sp)
            Spacer(modifier = Modifier.height(18.dp))

            // Agreements
            Text("약관 동의", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = TextPrimary)
            Spacer(modifier = Modifier.height(12.dp))

            // Terms row (unchecked by default in screenshot)
            AgreementRow(
                checked = agreeTerms,
                onCheckedChange = { agreeTerms = it },
                text = "이용약관에 동의합니다",
                required = true,
                onViewClick = { /* open terms */ }
            )
            Spacer(modifier = Modifier.height(8.dp))

            // Privacy row (checked)
            AgreementRow(
                checked = agreePrivacy,
                onCheckedChange = { agreePrivacy = it },
                text = "개인정보처리방침에 동의합니다",
                required = true,
                onViewClick = { /* open privacy */ }
            )

            Spacer(modifier = Modifier.height(60.dp)) // push content above bottom bar
        }
    }
}

/** Small labeled text with red asterisk when required */
@Composable
private fun LabelWithRequired(text: String, required: Boolean = true) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(text = text, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = TextPrimary)
        if (required) {
            Spacer(modifier = Modifier.width(4.dp))
            Text(text = "*", color = ErrorColor, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

/** Generic input box that imitates the rounded light background with border and trailing icons */
@Composable
private fun InputBox(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String = "",
    modifier: Modifier = Modifier.fillMaxWidth(),
    trailingIcon: @Composable (() -> Unit)? = null,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default
) {
    Box(modifier = modifier) {
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = InputBg,
            shadowElevation = 0.dp,
            border = BorderStroke(1.dp, InputBorder),
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .padding(start = 18.dp, end = 18.dp)
                    .fillMaxSize()
            ) {
                // Basic text field without inner box styling so that background & border use our Surface
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    singleLine = true,
                    textStyle = TextStyle(color = TextPrimary, fontSize = 16.sp),
                    cursorBrush = SolidColor(PrimaryColor),
                    visualTransformation = visualTransformation,
                    keyboardOptions = keyboardOptions,
                    decorationBox = { innerTextField ->
                        if (value.isEmpty()) {
                            Text(
                                text = placeholder,
                                color = TextHelper,
                                fontSize = 16.sp
                            )
                        }
                        Box(modifier = Modifier.fillMaxWidth()) {
                            innerTextField()
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                )

                if (trailingIcon != null) {
                    Box(
                        modifier = Modifier.padding(start = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        trailingIcon()
                    }
                }
            }
        }
    }
}

/** Password strength bar - horizontal with primary color fill depending on score */
@Composable
private fun StrengthBar(strengthScore: Int) {
    // map score (0..4) to fraction width
    val fraction = (strengthScore.coerceIn(0, 4)) / 4f
    val barColor = when (strengthScore) {
        0, 1 -> PrimaryColor.copy(alpha = 0.9f)
        2 -> PrimaryColor.copy(alpha = 0.85f)
        3, 4 -> PrimaryColor
        else -> PrimaryColor
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(6.dp)
            .clip(RoundedCornerShape(2.dp))
            .background(InputBorder)
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .width(with(LocalDensity.current) { (fraction * 300).dp }) // width scaled visually
                .align(Alignment.CenterStart)
                .clip(RoundedCornerShape(2.dp))
                .background(barColor)
        )
    }
}

/** Agreement row with a custom checkbox look that matches the screenshot */
@Composable
private fun AgreementRow(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    text: String,
    required: Boolean = false,
    onViewClick: () -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .clickable { onCheckedChange(!checked) },
        verticalAlignment = Alignment.CenterVertically
    ) {
        // custom checkbox
        Box(
            modifier = Modifier
                .size(22.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(if (checked) PrimaryColor else Color.Transparent)
                .border(
                    width = 1.dp,
                    color = if (checked) Color.Transparent else Color(0xFFCCCCCC),
                    shape = RoundedCornerShape(4.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            if (checked) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(14.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = text, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = TextPrimary)
                if (required) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("*", color = ErrorColor, fontSize = 13.sp)
                }
            }
        }

        Text(
            text = "보기",
            color = PrimaryColor,
            fontSize = 12.sp,
            modifier = Modifier
                .padding(start = 8.dp)
                .clickable { onViewClick() }
        )
    }
}

/** Basic password strength scoring:
 * 0 - empty
 * 1 - length < 8
 * 2 - length >= 8 and has letters and digits
 * 3 - includes uppercase, lower, digits, special
 * 4 - includes more variety (bonus)
 */
private fun passwordStrengthScore(pw: String): Int {
    if (pw.isEmpty()) return 0
    var score = 0
    if (pw.length >= 8) score++
    if (pw.any { it.isDigit() } && pw.any { it.isLetter() }) score++
    val hasUpper = pw.any { it.isUpperCase() }
    val hasLower = pw.any { it.isLowerCase() }
    val hasSpecial = pw.any { !it.isLetterOrDigit() }
    if (hasUpper && hasLower && hasSpecial) score++
    if (pw.length >= 12 && score >= 3) score++ // bonus for longer complex passwords
    return score.coerceIn(0, 4)
}

private fun isHangul(s: String): Boolean {
    // Very simple check: allow Hangul syllables range
    return s.all { ch ->
        val code = ch.code
        (code in 0xAC00..0xD7A3) || ch.isWhitespace()
    }
}

private val PHONE_PATTERN: Pattern = Pattern.compile("^01[0-9]-[0-9]{3,4}-[0-9]{4}$")