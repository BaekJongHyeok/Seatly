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
import kr.jiyeok.seatly.data.remote.request.LoginRequest
import kr.jiyeok.seatly.domain.model.ERole
import kr.jiyeok.seatly.presentation.viewmodel.AuthUiState
import kr.jiyeok.seatly.presentation.viewmodel.AuthViewModel
import kr.jiyeok.seatly.ui.component.AuthButton
import kr.jiyeok.seatly.ui.component.EmailInputField
import kr.jiyeok.seatly.ui.component.PasswordInputField
import kr.jiyeok.seatly.util.SharedPreferencesHelper

@Composable
fun LoginScreen(
    navController: NavController,
    viewModel: AuthViewModel = hiltViewModel()
) {
    val context = LocalContext.current

    // Use rememberSaveable for state persistence during configuration changes
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var isAutoLogin by rememberSaveable { mutableStateOf(false) }

    val authState by viewModel.authState.collectAsState()
    val loginData by viewModel.loginData.collectAsState()
    val userRole by viewModel.userRole.collectAsState()

    // Try auto-login if enabled with enhanced error handling
    LaunchedEffect(Unit) {
        try {
            if (SharedPreferencesHelper.isAutoLoginEnabled(context)) {
                val savedEmail = SharedPreferencesHelper.getSavedEmail(context)
                val savedPassword = SharedPreferencesHelper.getSavedPassword(context)
                
                if (savedEmail.isNotEmpty() && savedPassword.isNotEmpty()) {
                    email = savedEmail
                    password = savedPassword
                    isAutoLogin = true
                    viewModel.login(LoginRequest(savedEmail, savedPassword))
                } else {
                    // Clear invalid auto-login state
                    try {
                        SharedPreferencesHelper.clearAutoLoginCredentials(context)
                    } catch (e: Exception) {
                        // Ignore errors when clearing credentials
                    }
                }
            }
        } catch (e: SecurityException) {
            // Handle security-related errors when accessing SharedPreferences
            try {
                SharedPreferencesHelper.clearAutoLoginCredentials(context)
            } catch (clearError: Exception) {
                // Ignore errors when clearing credentials
            }
        } catch (e: IllegalStateException) {
            // Handle state-related errors
            try {
                SharedPreferencesHelper.clearAutoLoginCredentials(context)
            } catch (clearError: Exception) {
                // Ignore errors when clearing credentials
            }
        }
    }

    // On success, persist/clear auto-login and navigate based on user role
    LaunchedEffect(authState) {
        if (authState is AuthUiState.Success) {
            try {
                if (isAutoLogin) {
                    SharedPreferencesHelper.saveAutoLoginCredentials(context, email, password)
                } else {
                    SharedPreferencesHelper.clearAutoLoginCredentials(context)
                }
                
                // Navigate based on user role
                val destination = if (userRole == ERole.ADMIN) {
                    "admin_home"
                } else {
                    "home"
                }
                
                navController.navigate(destination) {
                    popUpTo("login") { inclusive = true }
                }
            } catch (e: SecurityException) {
                // Handle error but continue with navigation
                val destination = if (userRole == ERole.ADMIN) {
                    "admin_home"
                } else {
                    "home"
                }
                
                navController.navigate(destination) {
                    popUpTo("login") { inclusive = true }
                }
            }
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

        // Logo and Title Section
        LogoAndTitleSection()

        Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.login_section_spacing)))

        // Input Fields Section
        InputFieldsSection(
            email = email,
            password = password,
            onEmailChange = { email = it },
            onPasswordChange = { password = it }
        )

        Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.login_field_spacing)))

        // Auto-login and Password Recovery
        AutoLoginAndPasswordRecoveryRow(
            isAutoLogin = isAutoLogin,
            email = email,
            password = password,
            onAutoLoginChange = { checked ->
                isAutoLogin = checked
                try {
                    if (checked) {
                        if (email.isNotEmpty() && password.isNotEmpty()) {
                            SharedPreferencesHelper.saveAutoLoginCredentials(context, email, password)
                        } else {
                            SharedPreferencesHelper.enableAutoLogin(context)
                        }
                    } else {
                        SharedPreferencesHelper.clearAutoLoginCredentials(context)
                    }
                } catch (e: SecurityException) {
                    // Handle SharedPreferences security error - reset checkbox state
                    isAutoLogin = !checked
                } catch (e: IllegalStateException) {
                    // Handle state error - reset checkbox state
                    isAutoLogin = !checked
                }
            },
            onForgotPasswordClick = { navController.navigate("password_step1") }
        )

        Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.login_options_spacing)))

        // Error Message
        if (authState is AuthUiState.Error) {
            ErrorMessage(message = (authState as AuthUiState.Error).message)
            Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.login_error_spacing)))
        }

        // Login Button
        AuthButton(
            text = if (authState is AuthUiState.Loading) {
                stringResource(id = R.string.login_loading)
            } else {
                stringResource(id = R.string.login_button)
            },
            onClick = { viewModel.login(LoginRequest(email, password)) },
            enabled = authState !is AuthUiState.Loading,
        )

        Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.login_button_spacing)))

        // Social Login Section
        SocialLoginSection()

        Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.login_signup_spacing)))

        // Sign Up Section
        SignUpSection(onSignUpClick = { navController.navigate("signup") })
    }
}

/**
 * Logo and Title Section Composable
 * Displays the app logo and tagline
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
 * Input Fields Section Composable
 * Contains email and password input fields
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
        // Email Field
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

        // Password Field
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
 * Auto-login Checkbox and Password Recovery Row Composable
 */
@Composable
private fun AutoLoginAndPasswordRecoveryRow(
    isAutoLogin: Boolean,
    email: String,
    password: String,
    onAutoLoginChange: (Boolean) -> Unit,
    onForgotPasswordClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Auto-login checkbox
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

        // Forgot password
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
 * Error Message Composable
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
 * Social Login Section Composable
 * Contains divider and social login buttons
 */
@Composable
private fun SocialLoginSection() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = dimensionResource(id = R.dimen.login_bottom_padding)),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Divider with "또는"
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

        // Social buttons
        SocialLoginButtons()
    }
}

/**
 * Social Login Buttons Composable
 */
@Composable
private fun SocialLoginButtons() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(dimensionResource(id = R.dimen.login_social_button_spacing))
    ) {
        // Google
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
                    .clickable { /* Google login */ },
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.icon_google),
                    contentDescription = stringResource(id = R.string.login_google_content_desc),
                    modifier = Modifier.size(dimensionResource(id = R.dimen.login_social_icon_size_google))
                )
            }
        }

        // Kakao
        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
            Box(
                modifier = Modifier
                    .size(dimensionResource(id = R.dimen.login_social_button_size))
                    .clip(CircleShape)
                    .background(colorResource(id = R.color.login_kakao_bg))
                    .clickable { /* Kakao login */ },
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.icon_kakao),
                    contentDescription = stringResource(id = R.string.login_kakao_content_desc),
                    modifier = Modifier.size(dimensionResource(id = R.dimen.login_social_icon_size_kakao))
                )
            }
        }

        // Naver
        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
            Box(
                modifier = Modifier
                    .size(dimensionResource(id = R.dimen.login_social_button_size))
                    .clip(CircleShape)
                    .background(colorResource(id = R.color.login_naver_bg))
                    .clickable { /* Naver login */ },
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
 * Sign Up Section Composable
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