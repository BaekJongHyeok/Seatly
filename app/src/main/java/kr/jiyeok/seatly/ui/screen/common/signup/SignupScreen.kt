package kr.jiyeok.seatly.ui.screen.common.signup

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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kr.jiyeok.seatly.data.remote.enums.ERole
import kr.jiyeok.seatly.data.remote.request.RegisterRequest
import kr.jiyeok.seatly.presentation.viewmodel.AuthViewModel
import kr.jiyeok.seatly.ui.component.common.AppTopBar
import kr.jiyeok.seatly.ui.screen.admin.formatKoreanPhoneFromDigits
import kr.jiyeok.seatly.ui.theme.ColorBorderLight
import kr.jiyeok.seatly.ui.theme.ColorInputBg
import kr.jiyeok.seatly.ui.theme.ColorPrimaryOrange
import kr.jiyeok.seatly.ui.theme.ColorTextBlack
import kr.jiyeok.seatly.ui.theme.ColorTextGray
import kr.jiyeok.seatly.ui.theme.ColorWarning
import kr.jiyeok.seatly.ui.theme.ColorWhite
import java.util.regex.Pattern

// ============ Colors & Constants (Seatly Theme 기반) ============

private val PrimaryColor = ColorPrimaryOrange
private val InputBg = ColorInputBg
private val InputBorder = ColorBorderLight
private val TextPrimary = ColorTextBlack
private val TextHelper = ColorTextGray
private val ErrorColor = ColorWarning

private const val EMAIL_HINT = "example@email.com"
private const val PASSWORD_HINT = "••••••••"
private const val NAME_HINT = "홍길동"
private const val PHONE_HINT = "010-0000-0000"

private const val PASSWORD_MIN_LENGTH = 8
private const val NAME_MIN_LENGTH = 2
private const val NAME_MAX_LENGTH = 20

private const val SPACING_XS = 6
private const val SPACING_SM = 8
private const val SPACING_MD = 12
private const val SPACING_LG = 16
private const val SPACING_XL = 18
private const val SPACING_2XL = 36

private const val INPUT_HEIGHT = 56
private const val INPUT_RADIUS = 18

private const val ICON_SIZE_SMALL = 20
private const val ICON_SIZE_MEDIUM = 22
private const val ICON_SIZE_LARGE = 24

private const val CHECKBOX_SIZE = 22
private const val CHECKBOX_RADIUS = 4

private val PHONE_PATTERN: Pattern =
    Pattern.compile("^01[0-9]-[0-9]{3,4}-[0-9]{4}$")

typealias SignupResult = Pair<Boolean, String?>

// ============ Data Classes ============

data class FormState(
    val email: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val name: String = "",
    val phone: String = "",
    val phoneDisplay: TextFieldValue = TextFieldValue("")
)

data class ErrorState(
    val email: String? = null,
    val password: String? = null,
    val confirmPassword: String? = null,
    val name: String? = null,
    val phone: String? = null,
    val agreements: String? = null,
    val server: String? = null
) {
    fun hasErrors(): Boolean =
        listOfNotNull(email, password, confirmPassword, name, phone, agreements, server).isNotEmpty()

    fun clearServerError(): ErrorState = copy(server = null)
}

data class AgreementState(
    val terms: Boolean = false,
    val privacy: Boolean = false
) {
    fun allAgreed(): Boolean = terms && privacy
}

// ============ Main Screen ============

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignupScreen(
    navController: NavController,
    viewModel: AuthViewModel,
    onBack: () -> Unit = {},
    onNext: (email: String, password: String) -> Unit = { _, _ -> },
    performSignup: suspend (
        email: String,
        password: String,
        name: String,
        phone: String
    ) -> SignupResult = { email, password, name, phone ->
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

    // Form State
    var formState by remember { mutableStateOf(FormState()) }
    var agreementState by remember { mutableStateOf(AgreementState()) }
    var errorState by remember { mutableStateOf(ErrorState()) }
    var passwordVisible by remember { mutableStateOf(false) }
    var isSubmitting by remember { mutableStateOf(false) }
    var selectedRole by remember { mutableStateOf(ERole.USER) }

    // Derived states
    val isEmailValid by derivedStateOf { isValidEmail(formState.email) }
    val passwordStrength by derivedStateOf { calculatePasswordStrength(formState.password) }
    val isPasswordsMatching by derivedStateOf {
        formState.password.isNotBlank() && formState.password == formState.confirmPassword
    }
    val isNameValid by derivedStateOf { isValidName(formState.name) }
    val isPhoneValid by derivedStateOf { isValidPhone(formState.phone) }

    fun clearFieldError(field: String) {
        errorState = when (field) {
            "email" -> errorState.copy(email = null, server = null)
            "password" -> errorState.copy(password = null, server = null)
            "confirmPassword" -> errorState.copy(confirmPassword = null, server = null)
            "name" -> errorState.copy(name = null, server = null)
            "phone" -> errorState.copy(phone = null, server = null)
            "agreements" -> errorState.copy(agreements = null, server = null)
            else -> errorState
        }
    }

    fun validateAll(): Boolean {
        var newErrorState = errorState.copy(
            email = null,
            password = null,
            confirmPassword = null,
            name = null,
            phone = null,
            agreements = null,
            server = null
        )
        var isValid = true

        // Email
        if (formState.email.isBlank()) {
            newErrorState = newErrorState.copy(email = "이메일을 입력해주세요.")
            isValid = false
        } else if (!isValidEmail(formState.email)) {
            newErrorState = newErrorState.copy(email = "올바른 이메일 형식을 입력해주세요.")
            isValid = false
        }

        // Password
        if (formState.password.isBlank()) {
            newErrorState = newErrorState.copy(password = "비밀번호를 입력해주세요.")
            isValid = false
        } else {
            val pwError = validatePassword(formState.password)
            if (pwError != null) {
                newErrorState = newErrorState.copy(password = pwError)
                isValid = false
            }
        }

        // Confirm password
        if (formState.confirmPassword.isBlank()) {
            newErrorState = newErrorState.copy(confirmPassword = "비밀번호 확인을 입력해주세요.")
            isValid = false
        } else if (formState.password != formState.confirmPassword) {
            newErrorState =
                newErrorState.copy(confirmPassword = "비밀번호가 일치하지 않습니다.")
            isValid = false
        }

        // Name
        val trimmedName = formState.name.trim()
        if (trimmedName.isBlank()) {
            newErrorState = newErrorState.copy(name = "이름을 입력해주세요.")
            isValid = false
        } else {
            val nameError = validateName(trimmedName)
            if (nameError != null) {
                newErrorState = newErrorState.copy(name = nameError)
                isValid = false
            }
        }

        // Phone
        if (formState.phone.isBlank()) {
            newErrorState = newErrorState.copy(phone = "휴대폰 번호를 입력해주세요.")
            isValid = false
        } else if (!isValidPhone(formState.phone)) {
            newErrorState =
                newErrorState.copy(phone = "010-1234-5678 형식으로 입력해주세요.")
            isValid = false
        }

        // Agreements
        if (!agreementState.allAgreed()) {
            newErrorState =
                newErrorState.copy(agreements = "필수 약관에 모두 동의해주세요.")
            isValid = false
        }

        errorState = newErrorState
        return isValid
    }

    fun onSubmit() {
        if (!validateAll()) return

        val request = RegisterRequest(
            email = formState.email.trim(),
            password = formState.password,
            name = formState.name.trim(),
            phone = formState.phone.trim(),
            imageUrl = "",
            role = selectedRole
        )

        viewModel.signUp(request)

        coroutineScope.launch {
            isSubmitting = true
            errorState = errorState.clearServerError()

            val (success, message) = try {
                performSignup(
                    formState.email.trim(),
                    formState.password,
                    formState.name.trim(),
                    formState.phone.trim()
                )
            } catch (e: Exception) {
                Pair(false, e.message ?: "서버 오류가 발생했습니다.")
            }

            isSubmitting = false

            if (success) {
                onNext(formState.email.trim(), formState.password)
            } else {
                errorState =
                    errorState.copy(server = message ?: "회원가입에 실패했습니다.")
            }
        }
    }

    Scaffold(
        topBar = {
            AppTopBar(
                title = "회원가입",
                leftContent = {
                    Icon(
                        imageVector = Icons.Default.ChevronLeft,
                        contentDescription = "뒤로",
                        tint = TextPrimary,
                        modifier = Modifier.clickable { onBack() }
                    )
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .background(ColorWhite)
                .verticalScroll(scrollState)
                .padding(horizontal = SPACING_LG.dp)
                .fillMaxSize()
        ) {
            Spacer(modifier = Modifier.height(SPACING_LG.dp))

            // 이메일
            FormField(
                label = "이메일",
                value = formState.email,
                onValueChange = {
                    formState = formState.copy(email = it)
                    clearFieldError("email")
                },
                placeholder = EMAIL_HINT,
                error = errorState.email,
                trailingIcon = if (formState.email.isNotBlank() && isEmailValid) {
                    { ValidCheckIcon() }
                } else {
                    { InactiveCheckIcon() }
                }
            )

            Spacer(modifier = Modifier.height(SPACING_LG.dp))

            // 이름
            FormField(
                label = "이름",
                value = formState.name,
                onValueChange = {
                    formState = formState.copy(name = it)
                    clearFieldError("name")
                },
                placeholder = NAME_HINT,
                error = errorState.name,
                helperText = "$NAME_MIN_LENGTH-$NAME_MAX_LENGTH 자 한글",
                trailingIcon = if (formState.name.isNotBlank() && isNameValid) {
                    { ValidCheckIcon() }
                } else {
                    { InactiveCheckIcon() }
                }
            )

            Spacer(modifier = Modifier.height(SPACING_LG.dp))

            // 비밀번호
            FormField(
                label = "비밀번호",
                value = formState.password,
                onValueChange = {
                    formState = formState.copy(password = it)
                    clearFieldError("password")
                },
                placeholder = PASSWORD_HINT,
                error = errorState.password,
                helperText = "8자 이상, 문자와 숫자를 포함",
                visualTransformation = if (passwordVisible) VisualTransformation.None
                else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(
                            imageVector = if (passwordVisible) Icons.Default.Visibility
                            else Icons.Default.VisibilityOff,
                            contentDescription = if (passwordVisible) "숨기기" else "보기",
                            tint = TextHelper
                        )
                    }
                }
            )

            Spacer(modifier = Modifier.height(SPACING_SM.dp))

            PasswordStrengthBar(strengthScore = passwordStrength)

            Spacer(modifier = Modifier.height(SPACING_LG.dp))

            // 비밀번호 확인
            FormField(
                label = "비밀번호 확인",
                value = formState.confirmPassword,
                onValueChange = {
                    formState = formState.copy(confirmPassword = it)
                    clearFieldError("confirmPassword")
                },
                placeholder = PASSWORD_HINT,
                error = errorState.confirmPassword,
                visualTransformation = if (passwordVisible) VisualTransformation.None
                else PasswordVisualTransformation(),
                trailingIcon = if (formState.confirmPassword.isNotBlank() && isPasswordsMatching) {
                    { ValidCheckIcon() }
                } else {
                    { InactiveCheckIcon() }
                }
            )

            Spacer(modifier = Modifier.height(SPACING_LG.dp))

            // 휴대폰 번호
            PhoneFormField(
                label = "휴대폰번호",
                value = formState.phoneDisplay,
                onValueChange = { newValue ->
                    val digitsOnly = newValue.text.filter { it.isDigit() }
                    if (digitsOnly.length <= 11) {
                        val formatted = formatKoreanPhoneFromDigits(digitsOnly)
                        formState = formState.copy(
                            phone = digitsOnly,
                            phoneDisplay = TextFieldValue(
                                text = formatted,
                                selection = TextRange(formatted.length)
                            )
                        )
                        clearFieldError("phone")
                    }
                },
                placeholder = PHONE_HINT,
                error = errorState.phone,
                helperText = "010-1234-5678 형식",
                trailingIcon = if (formState.phone.isNotBlank() && isPhoneValid) {
                    { ValidCheckIcon() }
                } else {
                    { InactiveCheckIcon() }
                }
            )

            Spacer(modifier = Modifier.height(SPACING_XL.dp))

            // 약관 동의
            AgreementsSection(
                agreeTerms = agreementState.terms,
                onAgreeTermsChange = {
                    agreementState = agreementState.copy(terms = it)
                    clearFieldError("agreements")
                },
                agreePrivacy = agreementState.privacy,
                onAgreePrivacyChange = {
                    agreementState = agreementState.copy(privacy = it)
                    clearFieldError("agreements")
                },
                error = errorState.agreements
            )

            Spacer(modifier = Modifier.height(SPACING_XL.dp))

            // 회원 유형 선택
            RoleSelectionSection(
                selectedRole = selectedRole,
                onRoleSelected = { selectedRole = it }
            )

            Spacer(modifier = Modifier.height(SPACING_XL.dp))

            // 가입하기 버튼
            Button(
                onClick = { onSubmit() },
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryColor),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(INPUT_HEIGHT.dp),
                enabled = !isSubmitting
            ) {
                Text(
                    text = if (isSubmitting) "가입 중..." else "가입하기",
                    color = ColorWhite,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            // 서버 에러 메시지
            errorState.server?.let {
                Spacer(modifier = Modifier.height(SPACING_SM.dp))
                Text(text = it, color = ErrorColor, fontSize = 13.sp)
            }

            Spacer(modifier = Modifier.height(SPACING_2XL.dp))
        }
    }
}

// ============ Reusable Components ============

@Composable
private fun FormField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    error: String? = null,
    helperText: String? = null,
    modifier: Modifier = Modifier.fillMaxWidth(),
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    trailingIcon: @Composable (() -> Unit)? = null
) {
    Column {
        LabelWithRequired(text = label)
        Spacer(modifier = Modifier.height(SPACING_SM.dp))
        InputBox(
            value = value,
            onValueChange = onValueChange,
            placeholder = placeholder,
            modifier = modifier,
            trailingIcon = trailingIcon,
            visualTransformation = visualTransformation,
            keyboardOptions = keyboardOptions
        )

        error?.let {
            Spacer(modifier = Modifier.height(SPACING_SM.dp))
            Text(text = it, color = ErrorColor, fontSize = 12.sp)
        }

        helperText?.let {
            Spacer(modifier = Modifier.height(SPACING_SM.dp))
            Text(
                text = it,
                color = TextHelper,
                fontSize = 11.sp,
                modifier = Modifier.padding(start = 4.dp)
            )
        }
    }
}

@Composable
private fun PhoneFormField(
    label: String,
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    placeholder: String,
    error: String? = null,
    helperText: String? = null,
    modifier: Modifier = Modifier.fillMaxWidth(),
    trailingIcon: @Composable (() -> Unit)? = null
) {
    Column {
        LabelWithRequired(text = label)
        Spacer(modifier = Modifier.height(SPACING_SM.dp))
        PhoneInputBox(
            value = value,
            onValueChange = onValueChange,
            placeholder = placeholder,
            modifier = modifier,
            trailingIcon = trailingIcon
        )

        error?.let {
            Spacer(modifier = Modifier.height(SPACING_SM.dp))
            Text(text = it, color = ErrorColor, fontSize = 12.sp)
        }

        helperText?.let {
            Spacer(modifier = Modifier.height(SPACING_SM.dp))
            Text(
                text = it,
                color = TextHelper,
                fontSize = 11.sp,
                modifier = Modifier.padding(start = 4.dp)
            )
        }
    }
}

@Composable
private fun LabelWithRequired(text: String, required: Boolean = true) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = text,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = TextPrimary
        )
        if (required) {
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "*",
                color = ErrorColor,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

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
            shape = RoundedCornerShape(INPUT_RADIUS.dp),
            color = InputBg,
            shadowElevation = 0.dp,
            border = BorderStroke(1.dp, InputBorder),
            modifier = Modifier
                .fillMaxWidth()
                .height(INPUT_HEIGHT.dp)
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
                    modifier = Modifier.weight(1f)
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

@Composable
private fun PhoneInputBox(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    placeholder: String = "",
    modifier: Modifier = Modifier.fillMaxWidth(),
    trailingIcon: @Composable (() -> Unit)? = null
) {
    Box(modifier = modifier) {
        Surface(
            shape = RoundedCornerShape(INPUT_RADIUS.dp),
            color = InputBg,
            shadowElevation = 0.dp,
            border = BorderStroke(1.dp, InputBorder),
            modifier = Modifier
                .fillMaxWidth()
                .height(INPUT_HEIGHT.dp)
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
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    decorationBox = { innerTextField ->
                        if (value.text.isEmpty()) {
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
                    modifier = Modifier.weight(1f)
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

@Composable
private fun ValidCheckIcon() {
    Icon(
        imageVector = Icons.Default.Check,
        contentDescription = null,
        tint = ColorWhite,
        modifier = Modifier
            .size(ICON_SIZE_LARGE.dp)
            .background(PrimaryColor, shape = RoundedCornerShape(12.dp))
            .padding(2.dp)
    )
}

@Composable
private fun InactiveCheckIcon() {
    Icon(
        imageVector = Icons.Default.Check,
        contentDescription = null,
        tint = TextHelper,
        modifier = Modifier.size(ICON_SIZE_SMALL.dp)
    )
}

@Composable
private fun PasswordStrengthBar(strengthScore: Int) {
    val fraction = (strengthScore.coerceIn(0, 4)) / 4f
    val barColor = when (strengthScore) {
        0, 1 -> PrimaryColor.copy(alpha = 0.9f)
        2 -> PrimaryColor.copy(alpha = 0.85f)
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

@Composable
private fun AgreementsSection(
    agreeTerms: Boolean,
    onAgreeTermsChange: (Boolean) -> Unit,
    agreePrivacy: Boolean,
    onAgreePrivacyChange: (Boolean) -> Unit,
    error: String? = null
) {
    Column {
        Text(
            text = "약관 동의",
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = TextPrimary
        )
        Spacer(modifier = Modifier.height(SPACING_MD.dp))

        AgreementRow(
            checked = agreeTerms,
            onCheckedChange = onAgreeTermsChange,
            text = "이용약관에 동의합니다",
            required = true,
            onViewClick = { /* open terms */ }
        )

        Spacer(modifier = Modifier.height(SPACING_SM.dp))

        AgreementRow(
            checked = agreePrivacy,
            onCheckedChange = onAgreePrivacyChange,
            text = "개인정보처리방침에 동의합니다",
            required = true,
            onViewClick = { /* open privacy */ }
        )

        error?.let {
            Spacer(modifier = Modifier.height(SPACING_SM.dp))
            Text(text = it, color = ErrorColor, fontSize = 12.sp)
        }
    }
}

@Composable
private fun AgreementRow(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    text: String,
    required: Boolean = false,
    onViewClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .clickable { onCheckedChange(!checked) },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(CHECKBOX_SIZE.dp)
                .clip(RoundedCornerShape(CHECKBOX_RADIUS.dp))
                .background(if (checked) PrimaryColor else ColorWhite)
                .border(
                    width = 1.dp,
                    color = if (checked) PrimaryColor else ColorBorderLight,
                    shape = RoundedCornerShape(CHECKBOX_RADIUS.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            if (checked) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = ColorWhite,
                    modifier = Modifier.size(14.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = text,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextPrimary
                )
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

@Composable
private fun RoleSelectionSection(
    selectedRole: ERole,
    onRoleSelected: (ERole) -> Unit
) {
    Column {
        Text(
            text = "회원 유형 선택",
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = TextPrimary
        )
        Spacer(modifier = Modifier.height(SPACING_MD.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min),
            horizontalArrangement = Arrangement.spacedBy(SPACING_SM.dp)
        ) {
            RoleButton(
                text = "유저",
                isSelected = selectedRole == ERole.USER,
                onClick = { onRoleSelected(ERole.USER) },
                modifier = Modifier.weight(1f)
            )
            RoleButton(
                text = "관리자",
                isSelected = selectedRole == ERole.ADMIN,
                onClick = { onRoleSelected(ERole.ADMIN) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun RoleButton(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isSelected) PrimaryColor else ColorWhite
        ),
        border = BorderStroke(
            width = 1.dp,
            color = if (isSelected) PrimaryColor else InputBorder
        ),
        shape = RoundedCornerShape(12.dp),
        modifier = modifier.height(48.dp)
    ) {
        Text(
            text = text,
            color = if (isSelected) ColorWhite else TextPrimary,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

// ============ Validation Functions ============

private fun isValidEmail(email: String): Boolean {
    return Patterns.EMAIL_ADDRESS.matcher(email).matches()
}

private fun validatePassword(password: String): String? {
    return when {
        password.length < PASSWORD_MIN_LENGTH ->
            "비밀번호는 ${PASSWORD_MIN_LENGTH}자 이상이어야 합니다."
        !password.any { it.isDigit() } || !password.any { it.isLetter() } ->
            "비밀번호는 문자와 숫자를 모두 포함해야 합니다."
        else -> null
    }
}

private fun isValidName(name: String): Boolean {
    val trimmed = name.trim()
    return trimmed.length in NAME_MIN_LENGTH..NAME_MAX_LENGTH && isHangulOnly(trimmed)
}

private fun validateName(name: String): String? {
    val trimmed = name.trim()
    return when {
        trimmed.length !in NAME_MIN_LENGTH..NAME_MAX_LENGTH || !isHangulOnly(trimmed) ->
            "$NAME_MIN_LENGTH-$NAME_MAX_LENGTH 자 한글로 입력해주세요."
        else -> null
    }
}

private fun isValidPhone(phone: String): Boolean {
    return PHONE_PATTERN.matcher(phone).matches()
}

private fun calculatePasswordStrength(password: String): Int {
    if (password.isEmpty()) return 0
    var score = 0
    if (password.length >= PASSWORD_MIN_LENGTH) score++
    if (password.any { it.isDigit() } && password.any { it.isLetter() }) score++

    val hasUpper = password.any { it.isUpperCase() }
    val hasLower = password.any { it.isLowerCase() }
    val hasSpecial = password.any { !it.isLetterOrDigit() }
    if (hasUpper && hasLower && hasSpecial) score++

    if (password.length >= 12 && score >= 3) score++

    return score.coerceIn(0, 4)
}

private fun isHangulOnly(s: String): Boolean {
    return s.all { ch ->
        val code = ch.code
        (code in 0xAC00..0xD7A3) || ch.isWhitespace()
    }
}
