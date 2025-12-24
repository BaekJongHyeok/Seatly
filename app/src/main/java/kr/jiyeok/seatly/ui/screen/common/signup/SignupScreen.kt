package kr.jiyeok.seatly.ui.screen.common.signup

import android.util.Patterns
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.runtime.*
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kr.jiyeok.seatly.ui.component.common.AppTopBar
import java.util.regex.Pattern

// All backgrounds set to white as requested
private val PrimaryColor = Color(0xFFE95321)
private val InputBg = Color.White
private val InputBorder = Color(0xFFE8E8E8)
private val TextPrimary = Color(0xFF1A1A1A)
private val TextHelper = Color(0xFF888888)
private val ErrorColor = Color(0xFFFF453A)

/**
 * performSignup: suspend function used to execute signup.
 * - Default implementation is a local mock (simulates network delay + response).
 * - You can pass a real implementation when calling SignupScreen to switch to real server easily.
 *
 * Example to provide real implementation:
 * SignupScreen(onNext = { ... }, performSignup = { email, password, name, phone ->
 *    // call repository / retrofit here and return Pair(success:Boolean, message:String?)
 * })
 */
typealias SignupResult = Pair<Boolean, String?>

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignupScreen(
    onBack: () -> Unit = {},
    // called when signup succeeded (default navigates back to login in caller)
    onNext: (email: String, password: String) -> Unit = { _, _ -> },
    // injectable signup action (default: mock)
    performSignup: suspend (email: String, password: String, name: String, phone: String) -> SignupResult = { email, password, name, phone ->
        // Mock behavior:
        // - simulate network delay
        // - if email == "exists@example.com" -> return failure message
        // - otherwise success
        delay(800)
        if (email.equals("exists@example.com", ignoreCase = true)) {
            Pair(false, "이미 존재하는 이메일입니다.")
        } else {
            Pair(true, "회원가입 성공")
        }
    }
) {
    val scrollState = rememberScrollState()
    val coroutineScope = rememberCoroutineScope()

    // Form states
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPassword by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }

    // Agreements: default unchecked as requested
    var agreeTerms by remember { mutableStateOf(false) }
    var agreePrivacy by remember { mutableStateOf(false) }

    // Error states (null = no error, otherwise show text)
    var emailError by remember { mutableStateOf<String?>(null) }
    var passwordError by remember { mutableStateOf<String?>(null) }
    var confirmPasswordError by remember { mutableStateOf<String?>(null) }
    var nameError by remember { mutableStateOf<String?>(null) }
    var phoneError by remember { mutableStateOf<String?>(null) }
    var agreementsError by remember { mutableStateOf<String?>(null) }
    var serverError by remember { mutableStateOf<String?>(null) }

    var isSubmitting by remember { mutableStateOf(false) }

    // Derived quick checks
    val isEmailValid by derivedStateOf { Patterns.EMAIL_ADDRESS.matcher(email).matches() }
    val strengthScore by derivedStateOf { passwordStrengthScore(password) }
    val doPasswordsMatch by derivedStateOf { password.isNotBlank() && password == confirmPassword }
    val isNameValid by derivedStateOf { name.trim().length in 2..20 && isHangul(name.trim()) }
    val isPhoneValid by derivedStateOf { PHONE_PATTERN.matcher(phone).matches() }

    fun validateAll(): Boolean {
        var ok = true

        // clear server error on new attempt
        serverError = null

        // Email
        if (email.isBlank()) {
            emailError = "이메일을 입력해주세요."
            ok = false
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailError = "올바른 이메일 형식을 입력해주세요."
            ok = false
        } else {
            emailError = null
        }

        // Password
        if (password.isBlank()) {
            passwordError = "비밀번호를 입력해주세요."
            ok = false
        } else {
            if (password.length < 8) {
                passwordError = "비밀번호는 8자 이상이어야 합니다."
                ok = false
            } else if (!(password.any { it.isDigit() } && password.any { it.isLetter() })) {
                passwordError = "비밀번호는 문자와 숫자를 모두 포함해야 합니다."
                ok = false
            } else {
                passwordError = null
            }
        }

        // Confirm password
        if (confirmPassword.isBlank()) {
            confirmPasswordError = "비밀번호 확인을 입력해주세요."
            ok = false
        } else if (password != confirmPassword) {
            confirmPasswordError = "비밀번호가 일치하지 않습니다."
            ok = false
        } else {
            confirmPasswordError = null
        }

        // Name
        val trimmedName = name.trim()
        if (trimmedName.isBlank()) {
            nameError = "이름을 입력해주세요."
            ok = false
        } else if (trimmedName.length !in 2..20 || !isHangul(trimmedName)) {
            nameError = "2-20자 한글로 입력해주세요."
            ok = false
        } else {
            nameError = null
        }

        // Phone
        if (phone.isBlank()) {
            phoneError = "휴대폰 번호를 입력해주세요."
            ok = false
        } else if (!PHONE_PATTERN.matcher(phone).matches()) {
            phoneError = "010-1234-5678 형식으로 입력해주세요."
            ok = false
        } else {
            phoneError = null
        }

        // Agreements (both required)
        if (!agreeTerms || !agreePrivacy) {
            agreementsError = "필수 약관에 모두 동의해주세요."
            ok = false
        } else {
            agreementsError = null
        }

        return ok
    }

    Scaffold(
        topBar = {
            // Reduce topbar vertical size while keeping the original title style.
            AppTopBar(
                title = "회원가입",
                titleTextStyle = TextStyle(fontSize = 28.sp, fontWeight = FontWeight.Bold, color = TextPrimary),
                leftContent = {
                    Icon(
                        imageVector = Icons.Default.ChevronLeft,
                        contentDescription = "뒤로",
                        tint = TextPrimary
                    )
                },
                onLeftClick = onBack,
                backgroundColor = Color.White,
                buttonContainerSize = 44.dp,
                verticalPadding = 10.dp,
                minHeight = 64.dp
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .background(Color.White) // ensure whole content area background is white
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp)
                .fillMaxSize()
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Email
            LabelWithRequired(text = "이메일")
            Spacer(modifier = Modifier.height(8.dp))
            InputBox(
                value = email,
                onValueChange = {
                    email = it
                    emailError = null
                    serverError = null
                },
                placeholder = "example@email.com",
                trailingIcon = {
                    if (email.isNotBlank() && isEmailValid) {
                        androidx.compose.material3.Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier
                                .size(24.dp)
                                .background(PrimaryColor, shape = RoundedCornerShape(12.dp))
                                .padding(2.dp)
                        )
                    } else {
                        androidx.compose.material3.Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = TextHelper,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            )
            emailError?.let { Text(text = it, color = ErrorColor, fontSize = 12.sp, modifier = Modifier.padding(top = 6.dp)) }
            Spacer(modifier = Modifier.height(16.dp))

            // Password
            LabelWithRequired(text = "비밀번호")
            Spacer(modifier = Modifier.height(8.dp))
            InputBox(
                value = password,
                onValueChange = {
                    password = it
                    passwordError = null
                    serverError = null
                },
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
            passwordError?.let { Text(text = it, color = ErrorColor, fontSize = 12.sp, modifier = Modifier.padding(top = 6.dp)) }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "8자 이상, 문자와 숫자를 포함",
                color = TextHelper,
                fontSize = 11.sp,
                modifier = Modifier.padding(start = 4.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))

            // strength bar
            StrengthBar(strengthScore = strengthScore)
            Spacer(modifier = Modifier.height(16.dp))

            // Confirm password
            LabelWithRequired(text = "비밀번호 확인")
            Spacer(modifier = Modifier.height(8.dp))
            InputBox(
                value = confirmPassword,
                onValueChange = {
                    confirmPassword = it
                    confirmPasswordError = null
                    serverError = null
                },
                placeholder = "••••••••",
                trailingIcon = {
                    if (confirmPassword.isNotBlank() && doPasswordsMatch) {
                        androidx.compose.material3.Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier
                            .size(24.dp)
                            .background(PrimaryColor, shape = RoundedCornerShape(12.dp))
                            .padding(2.dp))
                    } else {
                        androidx.compose.material3.Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = TextHelper, modifier = Modifier.size(20.dp))
                    }
                },
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation()
            )
            confirmPasswordError?.let { Text(text = it, color = ErrorColor, fontSize = 12.sp, modifier = Modifier.padding(top = 6.dp)) }
            Spacer(modifier = Modifier.height(16.dp))

            // Name
            LabelWithRequired(text = "이름")
            Spacer(modifier = Modifier.height(8.dp))
            InputBox(
                value = name,
                onValueChange = {
                    name = it
                    nameError = null
                    serverError = null
                },
                placeholder = "홍길동",
                trailingIcon = {
                    if (name.isNotBlank() && isNameValid) {
                        androidx.compose.material3.Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier
                            .size(24.dp)
                            .background(PrimaryColor, shape = RoundedCornerShape(12.dp))
                            .padding(2.dp))
                    } else {
                        androidx.compose.material3.Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = TextHelper, modifier = Modifier.size(20.dp))
                    }
                }
            )
            nameError?.let { Text(text = it, color = ErrorColor, fontSize = 12.sp, modifier = Modifier.padding(top = 6.dp)) }
            Spacer(modifier = Modifier.height(6.dp))
            Text("2-20자 한글", color = TextHelper, fontSize = 11.sp)
            Spacer(modifier = Modifier.height(16.dp))

            // Phone
            LabelWithRequired(text = "휴대폰번호")
            Spacer(modifier = Modifier.height(8.dp))
            InputBox(
                value = phone,
                onValueChange = {
                    phone = it
                    phoneError = null
                    serverError = null
                },
                placeholder = "010-0000-0000",
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                trailingIcon = {
                    if (phone.isNotBlank() && isPhoneValid) {
                        androidx.compose.material3.Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier
                            .size(24.dp)
                            .background(PrimaryColor, shape = RoundedCornerShape(12.dp))
                            .padding(2.dp))
                    } else {
                        androidx.compose.material3.Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = TextHelper, modifier = Modifier.size(20.dp))
                    }
                }
            )
            phoneError?.let { Text(text = it, color = ErrorColor, fontSize = 12.sp, modifier = Modifier.padding(top = 6.dp)) }
            Spacer(modifier = Modifier.height(6.dp))
            Text("010-1234-5678 형식", color = TextHelper, fontSize = 11.sp)
            Spacer(modifier = Modifier.height(18.dp))

            // Agreements
            Text("약관 동의", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = TextPrimary)
            Spacer(modifier = Modifier.height(12.dp))

            AgreementRow(
                checked = agreeTerms,
                onCheckedChange = {
                    agreeTerms = it
                    agreementsError = null
                },
                text = "이용약관에 동의합니다",
                required = true,
                onViewClick = { /* open terms */ }
            )
            Spacer(modifier = Modifier.height(8.dp))

            AgreementRow(
                checked = agreePrivacy,
                onCheckedChange = {
                    agreePrivacy = it
                    agreementsError = null
                },
                text = "개인정보처리방침에 동의합니다",
                required = true,
                onViewClick = { /* open privacy */ }
            )
            agreementsError?.let { Text(text = it, color = ErrorColor, fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp)) }

            Spacer(modifier = Modifier.height(18.dp))

            // Signup button moved here and text changed to "가입하기"
            Button(
                onClick = {
                    // Run validation first
                    val ok = validateAll()
                    if (!ok) return@Button

                    // If valid, call performSignup (mock or real depending on injected lambda)
                    coroutineScope.launch {
                        isSubmitting = true
                        serverError = null
                        val (success, message) = try {
                            performSignup(email.trim(), password, name.trim(), phone.trim())
                        } catch (e: Exception) {
                            Pair(false, e.message ?: "서버 오류가 발생했습니다.")
                        }
                        isSubmitting = false

                        if (success) {
                            // onNext is called only after successful signup
                            onNext(email.trim(), password)
                        } else {
                            // show server-provided message (or generic)
                            serverError = message ?: "회원가입에 실패했습니다."
                        }
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryColor),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                enabled = !isSubmitting
            ) {
                Text(if (isSubmitting) "가입 중..." else "가입하기", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            }

            serverError?.let {
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = it, color = ErrorColor, fontSize = 13.sp)
            }

            Spacer(modifier = Modifier.height(36.dp)) // breathing room before end of scroll
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

/** Generic input box that imitates the rounded light background with border and trailing icons
 *  Note: InputBg is set to white per request.
 */
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

/** Password strength bar */
@Composable
private fun StrengthBar(strengthScore: Int) {
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
                .width(with(LocalDensity.current) { (fraction * 300).dp })
                .align(Alignment.CenterStart)
                .clip(RoundedCornerShape(2.dp))
                .background(barColor)
        )
    }
}

/** Agreement row with a custom checkbox look */
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
                androidx.compose.material3.Icon(
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

/** Basic password strength scoring */
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
    return s.all { ch ->
        val code = ch.code
        (code in 0xAC00..0xD7A3) || ch.isWhitespace()
    }
}

private val PHONE_PATTERN: Pattern = Pattern.compile("^01[0-9]-[0-9]{3,4}-[0-9]{4}$")