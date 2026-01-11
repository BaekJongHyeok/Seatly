package kr.jiyeok.seatly.ui.screen.common

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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import kr.jiyeok.seatly.R
import kr.jiyeok.seatly.data.remote.enums.ERole
import kr.jiyeok.seatly.presentation.viewmodel.AuthUiState
import kr.jiyeok.seatly.presentation.viewmodel.AuthViewModel
import kr.jiyeok.seatly.ui.component.AuthButton
import kr.jiyeok.seatly.ui.component.EmailInputField
import kr.jiyeok.seatly.ui.component.PasswordInputField
import kr.jiyeok.seatly.util.SharedPreferencesHelper

/**
 * 로그인 화면
 * 사용자 인증을 처리하고, 성공 시 사용자 역할에 따라 해당 홈 화면으로 이동
 *
 * 기능:
 * - 이메일/비밀번호 입력
 * - 자동 로그인 저장
 * - 비밀번호 찾기
 * - 회원가입 이동
 * - 소셜 로그인 (Google, Kakao, Naver)
 *
 * 네비게이션:
 * - 로그인 성공 (ADMIN) → admin/home
 * - 로그인 성공 (USER) → user/home
 * - 비밀번호 찾기 → auth/password/step1
 * - 회원가입 → auth/signup
 */
@Composable
fun LoginScreen(
    navController: NavController,
    viewModel: AuthViewModel
) {
    val context = LocalContext.current
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var isAutoLogin by rememberSaveable { mutableStateOf(false) }
    val authState by viewModel.authState.collectAsState()
    val userRole by viewModel.userRole.collectAsState()

    LaunchedEffect(Unit) {
        tryAutoLogin(context, viewModel) { savedEmail, savedPassword ->
            email = savedEmail
            password = savedPassword
            isAutoLogin = true
        }
    }

    LaunchedEffect(authState) {
        if (authState is AuthUiState.Success) {
            handleLoginSuccess(
                context = context,
                isAutoLogin = isAutoLogin,
                email = email,
                password = password,
                userRole = userRole,
                viewModel = viewModel,
                navController = navController
            )
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colorResource(id = R.color.login_background))
            .padding(horizontal = dimensionResource(id = R.dimen.login_horizontal_padding)),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.login_top_spacer)))

        LogoAndTitleSection()

        Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.login_section_spacing)))

        InputFieldsSection(
            email = email,
            password = password,
            onEmailChange = { email = it },
            onPasswordChange = { password = it }
        )

        Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.login_field_spacing)))

        AutoLoginAndPasswordRecoveryRow(
            isAutoLogin = isAutoLogin,
            onAutoLoginChange = { checked ->
                isAutoLogin = checked
                updateAutoLoginPreference(context, checked, email, password)
            },
            onForgotPasswordClick = { navController.navigate("auth/password/step1") }
        )

        Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.login_options_spacing)))

        if (authState is AuthUiState.Error) {
            ErrorMessage(message = (authState as AuthUiState.Error).message)
            Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.login_error_spacing)))
        }

        AuthButton(
            text = if (authState is AuthUiState.Loading) {
                stringResource(id = R.string.login_loading)
            } else {
                stringResource(id = R.string.login_button)
            },
            onClick = { viewModel.login(email, password) },
            enabled = authState !is AuthUiState.Loading
        )

        Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.login_button_spacing)))

        SocialLoginSection()

        Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.login_signup_spacing)))

        SignUpSection(onSignUpClick = { navController.navigate("auth/signup") })
    }
}

/**
 * 자동 로그인 시도
 * SharedPreferences에 저장된 자격증명이 있으면 로그인 수행
 */
private fun tryAutoLogin(
    context: android.content.Context,
    viewModel: AuthViewModel,
    onCredentialsLoaded: (String, String) -> Unit
) {
    try {
        if (SharedPreferencesHelper.isAutoLoginEnabled(context)) {
            val savedEmail = SharedPreferencesHelper.getSavedEmail(context)
            val savedPassword = SharedPreferencesHelper.getSavedPassword(context)

            if (savedEmail.isNotEmpty() && savedPassword.isNotEmpty()) {
                onCredentialsLoaded(savedEmail, savedPassword)
                viewModel.login(savedEmail, savedPassword)
            } else {
                SharedPreferencesHelper.clearAutoLoginCredentials(context)
            }
        }
    } catch (e: SecurityException) {
        try {
            SharedPreferencesHelper.clearAutoLoginCredentials(context)
        } catch (clearError: Exception) {
            // 자격증명 삭제 오류 무시
        }
    } catch (e: IllegalStateException) {
        try {
            SharedPreferencesHelper.clearAutoLoginCredentials(context)
        } catch (clearError: Exception) {
            // 자격증명 삭제 오류 무시
        }
    }
}

/**
 * 로그인 성공 처리
 * 사용자 역할을 AuthViewModel에 업데이트하고 해당 홈 화면으로 이동
 *
 * 핵심:
 * 1. userRole 값을 AuthViewModel에 설정 (RootNavigation에서 감지)
 * 2. 자동 로그인 여부에 따라 자격증명 저장
 * 3. 역할에 따라 적절한 홈 화면으로 네비게이션
 */
private fun handleLoginSuccess(
    context: android.content.Context,
    isAutoLogin: Boolean,
    email: String,
    password: String,
    userRole: ERole,
    viewModel: AuthViewModel,
    navController: NavController
) {
    try {
        if (isAutoLogin) {
            SharedPreferencesHelper.saveAutoLoginCredentials(context, email, password)
        } else {
            SharedPreferencesHelper.clearAutoLoginCredentials(context)
        }
    } catch (e: SecurityException) {
        // SharedPreferences 보안 오류 발생 시에도 네비게이션 계속 진행
    }

    viewModel.setUserRole(userRole)

    val destination = if (userRole == ERole.ADMIN) {
        "admin/home"
    } else {
        "user/home"
    }

    navController.navigate(destination) {
        popUpTo("auth/login") { inclusive = true }
    }
}

/**
 * 자동 로그인 설정 업데이트
 * SharedPreferences에 자동 로그인 상태와 자격증명 저장
 */
private fun updateAutoLoginPreference(
    context: android.content.Context,
    isChecked: Boolean,
    email: String,
    password: String
) {
    try {
        if (isChecked) {
            if (email.isNotEmpty() && password.isNotEmpty()) {
                SharedPreferencesHelper.saveAutoLoginCredentials(context, email, password)
            } else {
                SharedPreferencesHelper.enableAutoLogin(context)
            }
        } else {
            SharedPreferencesHelper.clearAutoLoginCredentials(context)
        }
    } catch (e: SecurityException) {
        // SharedPreferences 보안 오류 무시
    } catch (e: IllegalStateException) {
        // SharedPreferences 상태 오류 무시
    }
}

/**
 * 로고 및 제목 섹션
 */
@Composable
private fun LogoAndTitleSection() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .size(dimensionResource(id = R.dimen.login_logo_size))
                .clip(RoundedCornerShape(dimensionResource(id = R.dimen.login_logo_corner_radius)))
                .background(color = colorResource(id = R.color.login_primary)),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.icon_seatly),
                contentDescription = stringResource(id = R.string.login_logo_content_desc),
                modifier = Modifier.size(dimensionResource(id = R.dimen.login_logo_icon_size))
            )
        }

        Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.login_logo_title_spacing)))

        Text(
            text = stringResource(id = R.string.login_app_name),
            fontSize = dimensionResource(id = R.dimen.login_title_text_size).value.sp,
            fontWeight = FontWeight.Bold,
            color = colorResource(id = R.color.login_text_primary)
        )

        Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.login_title_subtitle_spacing)))

        Text(
            text = stringResource(id = R.string.login_tagline),
            fontSize = dimensionResource(id = R.dimen.login_subtitle_text_size).value.sp,
            fontWeight = FontWeight.Normal,
            color = colorResource(id = R.color.login_text_secondary)
        )
    }
}

/**
 * 입력 필드 섹션 (이메일, 비밀번호)
 */
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
            .padding(vertical = dimensionResource(id = R.dimen.login_vertical_padding)),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = stringResource(id = R.string.login_email_label),
                fontSize = dimensionResource(id = R.dimen.login_label_text_size).value.sp,
                fontWeight = FontWeight.SemiBold,
                color = colorResource(id = R.color.login_text_primary),
                modifier = Modifier.padding(bottom = dimensionResource(id = R.dimen.login_label_spacing))
            )

            EmailInputField(
                value = email,
                onValueChange = onEmailChange,
                placeholder = stringResource(id = R.string.login_email_placeholder)
            )
        }

        Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.login_field_spacing)))

        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = stringResource(id = R.string.login_password_label),
                fontSize = dimensionResource(id = R.dimen.login_label_text_size).value.sp,
                fontWeight = FontWeight.SemiBold,
                color = colorResource(id = R.color.login_text_primary),
                modifier = Modifier.padding(bottom = dimensionResource(id = R.dimen.login_label_spacing))
            )

            PasswordInputField(
                value = password,
                onValueChange = onPasswordChange,
                placeholder = stringResource(id = R.string.login_password_placeholder)
            )
        }
    }
}

/**
 * 자동 로그인 체크박스 및 비밀번호 찾기 행
 */
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
                modifier = Modifier.size(dimensionResource(id = R.dimen.login_checkbox_size))
            )

            Text(
                text = stringResource(id = R.string.login_auto_login),
                fontSize = dimensionResource(id = R.dimen.login_label_text_size).value.sp,
                color = colorResource(id = R.color.login_text_primary),
                modifier = Modifier.padding(start = dimensionResource(id = R.dimen.login_checkbox_text_padding))
            )
        }

        Text(
            text = stringResource(id = R.string.login_forgot_password),
            fontSize = dimensionResource(id = R.dimen.login_label_text_size).value.sp,
            fontWeight = FontWeight.SemiBold,
            color = colorResource(id = R.color.login_primary),
            modifier = Modifier
                .clickable { onForgotPasswordClick() }
                .padding(dimensionResource(id = R.dimen.login_forgot_password_padding))
        )
    }
}

/**
 * 오류 메시지 표시
 */
@Composable
private fun ErrorMessage(message: String) {
    Text(
        text = message,
        color = colorResource(id = R.color.login_primary),
        fontSize = dimensionResource(id = R.dimen.login_label_text_size).value.sp,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = dimensionResource(id = R.dimen.login_error_horizontal_padding))
    )
}

/**
 * 소셜 로그인 섹션 (Google, Kakao, Naver)
 */
@Composable
private fun SocialLoginSection() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = dimensionResource(id = R.dimen.login_bottom_padding)),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(dimensionResource(id = R.dimen.login_divider_height)),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(dimensionResource(id = R.dimen.login_divider_line_height))
                    .background(colorResource(id = R.color.login_divider))
            )

            Text(
                text = stringResource(id = R.string.login_divider_or),
                fontSize = dimensionResource(id = R.dimen.login_divider_text_size).value.sp,
                color = colorResource(id = R.color.login_text_secondary),
                modifier = Modifier.padding(horizontal = dimensionResource(id = R.dimen.login_divider_padding))
            )

            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(dimensionResource(id = R.dimen.login_divider_line_height))
                    .background(colorResource(id = R.color.login_divider))
            )
        }

        Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.login_social_button_spacing)))

        SocialLoginButtons()
    }
}

/**
 * 소셜 로그인 버튼들
 */
@Composable
private fun SocialLoginButtons() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(dimensionResource(id = R.dimen.login_social_button_spacing))
    ) {
        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
            Box(
                modifier = Modifier
                    .size(dimensionResource(id = R.dimen.login_social_button_size))
                    .clip(CircleShape)
                    .background(colorResource(id = R.color.login_background))
                    .border(
                        dimensionResource(id = R.dimen.social_button_border_width),
                        colorResource(id = R.color.login_social_border),
                        CircleShape
                    )
                    .clickable { },
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.icon_google),
                    contentDescription = stringResource(id = R.string.login_google_content_desc),
                    modifier = Modifier.size(dimensionResource(id = R.dimen.login_social_icon_size_google))
                )
            }
        }

        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
            Box(
                modifier = Modifier
                    .size(dimensionResource(id = R.dimen.login_social_button_size))
                    .clip(CircleShape)
                    .background(colorResource(id = R.color.login_kakao_bg))
                    .clickable { },
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.icon_kakao),
                    contentDescription = stringResource(id = R.string.login_kakao_content_desc),
                    modifier = Modifier.size(dimensionResource(id = R.dimen.login_social_icon_size_kakao))
                )
            }
        }

        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
            Box(
                modifier = Modifier
                    .size(dimensionResource(id = R.dimen.login_social_button_size))
                    .clip(CircleShape)
                    .background(colorResource(id = R.color.login_naver_bg))
                    .clickable { },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(id = R.string.login_naver_text),
                    color = colorResource(id = R.color.login_background),
                    fontWeight = FontWeight.Bold,
                    fontSize = dimensionResource(id = R.dimen.login_social_naver_text_size).value.sp
                )
            }
        }
    }
}

/**
 * 회원가입 섹션
 */
@Composable
private fun SignUpSection(onSignUpClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = stringResource(id = R.string.login_no_account),
            fontSize = dimensionResource(id = R.dimen.login_signup_text_size).value.sp,
            color = colorResource(id = R.color.login_text_tertiary)
        )

        Text(
            text = stringResource(id = R.string.login_signup),
            fontSize = dimensionResource(id = R.dimen.login_signup_text_size).value.sp,
            fontWeight = FontWeight.Bold,
            color = colorResource(id = R.color.login_primary),
            modifier = Modifier.clickable { onSignUpClick() }
        )
    }
}
