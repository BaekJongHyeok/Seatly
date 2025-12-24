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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.regex.Pattern

/**
 * SocialSignupScreen.kt
 *
 * Social signup variant of the signup screen that matches the provided HTML + screenshots.
 * Place this file at: app/src/main/java/kr/jiyeok/seatly/ui/screen/signup/SocialSignupScreen.kt
 *
 * Visual notes:
 * - No password fields (social signup) — only email, name, phone, agreements.
 * - Trailing orange check icon inside circular orange background when the field is valid / not empty.
 * - Big orange "다음" button at bottom, shadowed and rounded.
 * - TopAppBar with centered title and back chevron at left.
 *
 * Usage:
 * SocialSignupScreen(onBack = { /* pop nav */ }, onNext = { email, name, phone -> ... })
 */

private val PrimaryColor = Color(0xFFFF6633) // matches code.html color variable
private val InputBg = Color(0xFFF8F8F8)
private val InputBorder = Color(0xFFE8E8E8)
private val TextPrimary = Color(0xFF1A1A1A)
private val TextHelper = Color(0xFF888888)
private val ErrorColor = Color(0xFFFF6B6B)

private val CHECK_ICON_BG = PrimaryColor
private val CHECK_ICON_TINT = Color.White

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SocialSignupScreen(
    onBack: () -> Unit = {},
    onNext: (email: String, name: String, phone: String) -> Unit = { _, _, _ -> }
) {
    val scroll = rememberScrollState()

    var email by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }

    var agreeTerms by remember { mutableStateOf(false) } // unchecked by default
    var agreePrivacy by remember { mutableStateOf(true) } // privacy checked in screenshot

    val isEmailValid = remember(email) { email.isNotBlank() && Patterns.EMAIL_ADDRESS.matcher(email).matches() }
    val isNameValid = remember(name) { name.trim().length in 2..20 && isHangul(name.trim()) }
    val isPhoneValid = remember(phone) { PHONE_PATTERN.matcher(phone).matches() }

    val canProceed = isEmailValid && isNameValid && isPhoneValid && agreeTerms && agreePrivacy

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
                        Icon(imageVector = Icons.Default.ChevronLeft, contentDescription = "뒤로", tint = TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White.copy(alpha = 0.95f))
            )
        },
        bottomBar = {
            Column(
                Modifier
                    .background(Color.White)
                    .padding(horizontal = 20.dp, vertical = 18.dp)
            ) {
                Button(
                    onClick = { if (canProceed) onNext(email.trim(), name.trim(), phone.trim()) },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryColor),
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp)
                ) {
                    Text("다음", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                }

                Spacer(modifier = Modifier.height(14.dp))

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
    ) { inner ->
        Column(
            modifier = Modifier
                .padding(inner)
                .verticalScroll(scroll)
                .padding(horizontal = 20.dp)
                .fillMaxSize()
        ) {
            Spacer(modifier = Modifier.height(28.dp))

            // Email
            LabelWithRequired(text = "이메일")
            Spacer(modifier = Modifier.height(8.dp))
            InputBox(
                value = email,
                onValueChange = { email = it },
                placeholder = "example@email.com",
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                trailingChecked = isEmailValid
            )

            Spacer(modifier = Modifier.height(22.dp))

            // Name
            LabelWithRequired(text = "이름")
            Spacer(modifier = Modifier.height(8.dp))
            InputBox(
                value = name,
                onValueChange = { name = it },
                placeholder = "홍길동",
                trailingChecked = isNameValid
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text("2-20자 한글", color = TextHelper, fontSize = 11.sp)

            Spacer(modifier = Modifier.height(22.dp))

            // Phone
            LabelWithRequired(text = "휴대폰번호")
            Spacer(modifier = Modifier.height(8.dp))
            InputBox(
                value = phone,
                onValueChange = { phone = it },
                placeholder = "010-0000-0000",
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                trailingChecked = isPhoneValid
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text("010-1234-5678 형식", color = TextHelper, fontSize = 11.sp)

            Spacer(modifier = Modifier.height(20.dp))

            // Agreements header
            Text("약관 동의", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = TextPrimary)
            Spacer(modifier = Modifier.height(12.dp))

            AgreementRow(
                checked = agreeTerms,
                onCheckedChange = { agreeTerms = it },
                text = "이용약관에 동의합니다",
                required = true,
                onViewClick = { /* open terms */ }
            )

            Spacer(modifier = Modifier.height(10.dp))

            AgreementRow(
                checked = agreePrivacy,
                onCheckedChange = { agreePrivacy = it },
                text = "개인정보처리방침에 동의합니다",
                required = true,
                onViewClick = { /* open privacy */ }
            )

            Spacer(modifier = Modifier.height(160.dp)) // keep content above bottom bar
        }
    }
}

/** Small labeled text with red asterisk when required */
@Composable
private fun LabelWithRequired(text: String, required: Boolean = true) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(text = text, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = TextPrimary)
        if (required) {
            Spacer(modifier = Modifier.width(6.dp))
            Text(text = "*", color = ErrorColor, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

/**
 * InputBox: rounded rectangle with light background and border, inner padding,
 * placeholder, and optional trailing check icon in orange circular background when `trailingChecked` is true.
 */
@Composable
private fun InputBox(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String = "",
    modifier: Modifier = Modifier.fillMaxWidth(),
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    trailingChecked: Boolean = false
) {
    Box(modifier = modifier) {
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = InputBg,
            shadowElevation = 0.dp,
            border = BorderStroke(1.dp, InputBorder),
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
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
                    keyboardOptions = keyboardOptions,
                    decorationBox = { inner ->
                        if (value.isEmpty()) {
                            Text(text = placeholder, color = TextHelper, fontSize = 16.sp)
                        }
                        Box(modifier = Modifier.fillMaxWidth()) {
                            inner()
                        }
                    },
                    modifier = Modifier.weight(1f)
                )

                // Trailing check icon circle
                Box(modifier = Modifier.padding(start = 8.dp), contentAlignment = Alignment.Center) {
                    if (trailingChecked) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(RoundedCornerShape(18.dp))
                                .background(CHECK_ICON_BG),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(imageVector = Icons.Default.Check, contentDescription = "ok", tint = CHECK_ICON_TINT)
                        }
                    } else {
                        // faint check glyph to match screenshot (gray circle with check not visible)
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = TextHelper,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
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
                .clip(RoundedCornerShape(6.dp))
                .background(if (checked) PrimaryColor else Color.Transparent)
                .border(
                    width = 1.dp,
                    color = if (checked) Color.Transparent else Color(0xFFCCCCCC),
                    shape = RoundedCornerShape(6.dp)
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

private fun isHangul(s: String): Boolean {
    return s.all { ch ->
        val code = ch.code
        (code in 0xAC00..0xD7A3) || ch.isWhitespace()
    }
}

private val PHONE_PATTERN: Pattern = Pattern.compile("^01[0-9]-[0-9]{3,4}-[0-9]{4}$")