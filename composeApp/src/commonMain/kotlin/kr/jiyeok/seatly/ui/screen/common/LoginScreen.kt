package kr.jiyeok.seatly.ui.screen.common

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import kr.jiyeok.seatly.data.remote.enums.ERole
import kr.jiyeok.seatly.presentation.viewmodel.AuthUiState
import kr.jiyeok.seatly.presentation.viewmodel.AuthViewModel
import kr.jiyeok.seatly.ui.component.common.EmailInputField
import kr.jiyeok.seatly.ui.component.common.PasswordInputField
import kr.jiyeok.seatly.ui.theme.*
import kr.jiyeok.seatly.util.AppSettings
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import seatly.composeapp.generated.resources.*

/**
 * 로그인 화면
 */
@Composable
fun LoginScreen(
    navController: NavController,
    viewModel: AuthViewModel
) {
    val appSettings: AppSettings = koinInject()

    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var isAutoLogin by rememberSaveable { mutableStateOf(false) }

    val authState by viewModel.authState.collectAsState()
    val userRole by viewModel.userRole.collectAsState()

    LaunchedEffect(Unit) {
        if (appSettings.isAutoLoginEnabled()) {
            val savedEmail = appSettings.getSavedEmail()
            val savedPassword = appSettings.getSavedPassword()
            if (savedEmail.isNotEmpty() && savedPassword.isNotEmpty()) {
                email = savedEmail
                password = savedPassword
                isAutoLogin = true
                viewModel.login(savedEmail, savedPassword)
            } else {
                appSettings.clearAutoLoginCredentials()
            }
        }
    }

    LaunchedEffect(authState) {
        if (authState is AuthUiState.Success) {
            if (isAutoLogin) {
                appSettings.saveAutoLoginCredentials(email, password)
            } else {
                appSettings.clearAutoLoginCredentials()
            }
            
            viewModel.setUserRole(userRole)
            val destination = if (userRole == ERole.ADMIN) "admin/home" else "user/home"
            navController.navigate(destination) {
                popUpTo("auth/login") { inclusive = true }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ColorWhite)
            .padding(horizontal = LoginDimens.horizontalPadding),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        Spacer(modifier = Modifier.height(LoginDimens.topSpacer))

        LogoAndTitleSection()

        Spacer(modifier = Modifier.height(LoginDimens.sectionSpacing))

        InputFieldsSection(
            email = email,
            password = password,
            onEmailChange = { email = it },
            onPasswordChange = { password = it }
        )

        Spacer(modifier = Modifier.height(LoginDimens.fieldSpacing))

        AutoLoginAndPasswordRecoveryRow(
            isAutoLogin = isAutoLogin,
            onAutoLoginChange = { checked ->
                isAutoLogin = checked
                if (checked && email.isNotEmpty() && password.isNotEmpty()) {
                    appSettings.saveAutoLoginCredentials(email, password)
                } else if (!checked) {
                    appSettings.clearAutoLoginCredentials()
                } else {
                    appSettings.enableAutoLogin()
                }
            },
            onForgotPasswordClick = { navController.navigate("auth/password/step1") }
        )

        Spacer(modifier = Modifier.height(LoginDimens.optionsSpacing))

        if (authState is AuthUiState.Error) {
            ErrorMessage(message = (authState as AuthUiState.Error).message)
            Spacer(modifier = Modifier.height(LoginDimens.errorSpacing))
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .background(
                    color = ColorPrimaryOrange,
                    shape = RoundedCornerShape(12.dp)
                )
                .clickable(enabled = authState !is AuthUiState.Loading) { viewModel.login(email, password) },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = if (authState is AuthUiState.Loading) {
                    stringResource(Res.string.login_loading)
                } else {
                    stringResource(Res.string.login_button)
                },
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White
            )
        }

        Spacer(modifier = Modifier.height(LoginDimens.buttonSpacing))

        SocialLoginSection()

        Spacer(modifier = Modifier.height(LoginDimens.signupSpacing))

        SignUpSection(onSignUpClick = { navController.navigate("auth/signup") })
    }
}

@Composable
private fun LogoAndTitleSection() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .size(LoginDimens.logoSize)
                .clip(RoundedCornerShape(LoginDimens.logoCornerRadius))
                .background(color = ColorPrimaryOrange),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(Res.drawable.icon_seatly),
                contentDescription = stringResource(Res.string.login_logo_content_desc),
                modifier = Modifier.size(LoginDimens.logoIconSize)
            )
        }

        Spacer(modifier = Modifier.height(LoginDimens.logoTitleSpacing))

        Text(
            text = stringResource(Res.string.login_app_name),
            fontSize = LoginDimens.titleTextSize,
            fontWeight = FontWeight.Bold,
            color = ColorTextBlack
        )

        Spacer(modifier = Modifier.height(LoginDimens.titleSubtitleSpacing))

        Text(
            text = stringResource(Res.string.login_tagline),
            fontSize = LoginDimens.subtitleTextSize,
            fontWeight = FontWeight.Normal,
            color = ColorTextGray
        )
    }
}

@Composable
private fun InputFieldsSection(
    email: String,
    password: String,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = LoginDimens.verticalPadding),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = stringResource(Res.string.login_email_label),
                fontSize = LoginDimens.labelTextSize,
                fontWeight = FontWeight.SemiBold,
                color = ColorTextBlack,
                modifier = Modifier.padding(bottom = LoginDimens.labelSpacing)
            )

            EmailInputField(
                value = email,
                onValueChange = onEmailChange,
                placeholder = stringResource(Res.string.login_email_placeholder)
            )
        }

        Spacer(modifier = Modifier.height(LoginDimens.fieldSpacing))

        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = stringResource(Res.string.login_password_label),
                fontSize = LoginDimens.labelTextSize,
                fontWeight = FontWeight.SemiBold,
                color = ColorTextBlack,
                modifier = Modifier.padding(bottom = LoginDimens.labelSpacing)
            )

            PasswordInputField(
                value = password,
                onValueChange = onPasswordChange,
                placeholder = stringResource(Res.string.login_password_placeholder)
            )
        }
    }
}

@Composable
private fun AutoLoginAndPasswordRecoveryRow(
    isAutoLogin: Boolean,
    onAutoLoginChange: (Boolean) -> Unit,
    onForgotPasswordClick: () -> Unit
) {
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
                onCheckedChange = onAutoLoginChange,
                modifier = Modifier.size(LoginDimens.checkboxSize),
                colors = CheckboxDefaults.colors(
                    checkedColor = ColorPrimaryOrange,
                    uncheckedColor = ColorBorderLight,
                    checkmarkColor = ColorWhite
                )
            )

            Text(
                text = stringResource(Res.string.login_auto_login),
                fontSize = LoginDimens.labelTextSize,
                color = ColorTextBlack,
                modifier = Modifier.padding(start = LoginDimens.checkboxTextPadding)
            )
        }

        Text(
            text = stringResource(Res.string.login_forgot_password),
            fontSize = LoginDimens.labelTextSize,
            fontWeight = FontWeight.SemiBold,
            color = ColorPrimaryOrange,
            modifier = Modifier
                .clickable { onForgotPasswordClick() }
                .padding(LoginDimens.forgotPasswordPadding)
        )
    }
}

@Composable
private fun ErrorMessage(message: String) {
    Text(
        text = message,
        color = ColorWarning,
        fontSize = LoginDimens.labelTextSize,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = LoginDimens.errorHorizontalPadding)
    )
}

@Composable
private fun SocialLoginSection() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = LoginDimens.bottomPadding),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(LoginDimens.dividerHeight),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(LoginDimens.dividerLineHeight)
                    .background(ColorBorderLight)
            )

            Text(
                text = stringResource(Res.string.login_divider_or),
                fontSize = LoginDimens.dividerTextSize,
                color = ColorTextGray,
                modifier = Modifier.padding(horizontal = LoginDimens.dividerPadding)
            )

            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(LoginDimens.dividerLineHeight)
                    .background(ColorBorderLight)
            )
        }

        Spacer(modifier = Modifier.height(LoginDimens.socialButtonSpacing))

        SocialLoginButtons()
    }
}

@Composable
private fun SocialLoginButtons() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(LoginDimens.socialButtonSpacing)
    ) {
        // Google 로그인
        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
            Box(
                modifier = Modifier
                    .size(LoginDimens.socialButtonSize)
                    .clip(CircleShape)
                    .background(ColorWhite)
                    .border(
                        LoginDimens.socialButtonBorderWidth,
                        ColorBorderLight,
                        CircleShape
                    )
                    .clickable { },
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(Res.drawable.icon_google),
                    contentDescription = stringResource(Res.string.login_google_content_desc),
                    modifier = Modifier.size(LoginDimens.socialIconSizeGoogle)
                )
            }
        }

        // Kakao 로그인
        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
            Box(
                modifier = Modifier
                    .size(LoginDimens.socialButtonSize)
                    .clip(CircleShape)
                    .background(AppColors.loginKakaoBg)
                    .clickable { },
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(Res.drawable.icon_kakao),
                    contentDescription = stringResource(Res.string.login_kakao_content_desc),
                    modifier = Modifier.size(LoginDimens.socialIconSizeKakao)
                )
            }
        }

        // Naver 로그인
        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
            Box(
                modifier = Modifier
                    .size(LoginDimens.socialButtonSize)
                    .clip(CircleShape)
                    .background(AppColors.loginNaverBg)
                    .clickable { },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(Res.string.login_naver_text),
                    color = ColorWhite,
                    fontWeight = FontWeight.Bold,
                    fontSize = LoginDimens.socialNaverTextSize
                )
            }
        }
    }
}

@Composable
private fun SignUpSection(onSignUpClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = stringResource(Res.string.login_no_account),
            fontSize = LoginDimens.signupTextSize,
            color = ColorTextGray
        )

        Spacer(Modifier.width(4.dp))

        Text(
            text = stringResource(Res.string.login_signup),
            fontSize = LoginDimens.signupTextSize,
            fontWeight = FontWeight.Bold,
            color = ColorPrimaryOrange,
            modifier = Modifier.clickable { onSignUpClick() }
        )
    }
}
