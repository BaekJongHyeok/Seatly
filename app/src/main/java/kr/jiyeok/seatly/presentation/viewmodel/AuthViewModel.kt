package kr.jiyeok.seatly.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kr.jiyeok.seatly.data.remote.request.*
import kr.jiyeok.seatly.data.remote.response.LoginResponseDTO
import kr.jiyeok.seatly.data.remote.response.UserResponseDto
import kr.jiyeok.seatly.data.repository.ApiResult
import kr.jiyeok.seatly.domain.model.ERole
import kr.jiyeok.seatly.domain.usecase.auth.*
import kr.jiyeok.seatly.domain.usecase.user.GetCurrentUserUseCase
import javax.inject.Inject

/**
 * ViewModel for authentication flows:
 * - login / logout
 * - register (normal / social)
 * - password reset flow (request code / verify / reset)
 *
 * Exposes StateFlows for UI consumption and a one-shot event channel for transient messages.
 */

sealed interface AuthUiState {
    object Idle : AuthUiState
    object Loading : AuthUiState
    data class Success(val data: Any? = null) : AuthUiState
    data class Error(val message: String) : AuthUiState
}

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val loginUseCase: LoginUseCase,
    private val logoutUseCase: LogoutUseCase,
    private val registerUseCase: RegisterUseCase,
    private val socialRegisterUseCase: SocialRegisterUseCase,
    private val requestPasswordResetUseCase: RequestPasswordResetUseCase,
    private val verifyPasswordResetCodeUseCase: VerifyPasswordResetCodeUseCase,
    private val resetPasswordUseCase: ResetPasswordUseCase,
    private val getCurrentUserUseCase: GetCurrentUserUseCase
) : ViewModel() {

    private val _authState = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val authState: StateFlow<AuthUiState> = _authState.asStateFlow()

    private val _loginData = MutableStateFlow<LoginResponseDTO?>(null)
    val loginData: StateFlow<LoginResponseDTO?> = _loginData.asStateFlow()

    private val _userRole = MutableStateFlow<ERole>(ERole.USER)
    val userRole: StateFlow<ERole> = _userRole.asStateFlow()

    private val _userData = MutableStateFlow<UserResponseDto?>(null)
    val userData: StateFlow<UserResponseDto?> = _userData.asStateFlow()

    // One-shot events (toasts, navigation commands)
    private val _events = Channel<String>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    fun login(request: LoginRequest) {
        viewModelScope.launch {
            _authState.value = AuthUiState.Loading
            when (val result = loginUseCase(request)) {
                is ApiResult.Success -> {
                    _loginData.value = result.data
                    // Extract user data and role from login response
                    val user = result.data?.user
                    _userData.value = user
                    // Determine role from user data in login response
                    val isAdmin = user?.roles?.let { ERole.isAdmin(it) } ?: false
                    _userRole.value = if (isAdmin) ERole.ADMIN else ERole.USER
                    _authState.value = AuthUiState.Success(user)
                    _events.send("로그인 성공")
                }
                is ApiResult.Failure -> {
                    _authState.value = AuthUiState.Error(result.message ?: "로그인 실패")
                    _events.send(result.message ?: "로그인 실패")
                }
            }
        }
    }

    private suspend fun fetchUserData() {
        when (val userResult = getCurrentUserUseCase()) {
            is ApiResult.Success -> {
                val user = userResult.data
                _userData.value = user
                // Determine role from user data
                val isAdmin = user?.roles?.let { ERole.isAdmin(it) } ?: false
                _userRole.value = if (isAdmin) ERole.ADMIN else ERole.USER
                _authState.value = AuthUiState.Success(user)
                _events.send("로그인 성공")
            }
            is ApiResult.Failure -> {
                // If user data fetch fails, default to USER role but still mark as success
                _userRole.value = ERole.USER
                _authState.value = AuthUiState.Success(_loginData.value)
                _events.send("로그인 성공")
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            _authState.value = AuthUiState.Loading
            when (val result = logoutUseCase()) {
                is ApiResult.Success -> {
                    _loginData.value = null
                    _userData.value = null
                    _userRole.value = ERole.USER
                    _authState.value = AuthUiState.Success(Unit)
                    _events.send("로그아웃 되었습니다.")
                }
                is ApiResult.Failure -> {
                    _authState.value = AuthUiState.Error(result.message ?: "로그아웃 실패")
                    _events.send(result.message ?: "로그아웃 실패")
                }
            }
        }
    }

    fun register(request: RegisterRequest) {
        viewModelScope.launch {
            _authState.value = AuthUiState.Loading
            when (val result = registerUseCase(request)) {
                is ApiResult.Success -> {
                    _authState.value = AuthUiState.Success(result.data)
                    _events.send("회원가입 완료")
                }
                is ApiResult.Failure -> {
                    _authState.value = AuthUiState.Error(result.message ?: "회원가입 실패")
                    _events.send(result.message ?: "회원가입 실패")
                }
            }
        }
    }

    fun socialRegister(request: SocialRegisterRequest) {
        viewModelScope.launch {
            _authState.value = AuthUiState.Loading
            when (val result = socialRegisterUseCase(request)) {
                is ApiResult.Success -> {
                    _authState.value = AuthUiState.Success(result.data)
                    _events.send("소셜 회원가입 완료")
                }
                is ApiResult.Failure -> {
                    _authState.value = AuthUiState.Error(result.message ?: "소셜 회원가입 실패")
                    _events.send(result.message ?: "소셜 회원가입 실패")
                }
            }
        }
    }

    fun requestPasswordReset(request: ForgotPasswordRequest) {
        viewModelScope.launch {
            _authState.value = AuthUiState.Loading
            when (val result = requestPasswordResetUseCase(request)) {
                is ApiResult.Success -> {
                    _authState.value = AuthUiState.Success(Unit)
                    _events.send("보안 코드가 발송됐습니다.")
                }
                is ApiResult.Failure -> {
                    _authState.value = AuthUiState.Error(result.message ?: "이메일 전송 실패")
                    _events.send(result.message ?: "이메일 전송 실패")
                }
            }
        }
    }

    fun verifyPasswordResetCode(request: VerifyCodeRequest) {
        viewModelScope.launch {
            _authState.value = AuthUiState.Loading
            when (val result = verifyPasswordResetCodeUseCase(request)) {
                is ApiResult.Success -> {
                    _authState.value = AuthUiState.Success(Unit)
                    _events.send("보안 코드 확인 완료")
                }
                is ApiResult.Failure -> {
                    _authState.value = AuthUiState.Error(result.message ?: "코드 검증 실패")
                    _events.send(result.message ?: "코드 검증 실패")
                }
            }
        }
    }

    fun resetPassword(request: ResetPasswordRequest) {
        viewModelScope.launch {
            _authState.value = AuthUiState.Loading
            when (val result = resetPasswordUseCase(request)) {
                is ApiResult.Success -> {
                    _authState.value = AuthUiState.Success(Unit)
                    _events.send("비밀번호가 변경되었습니다.")
                }
                is ApiResult.Failure -> {
                    _authState.value = AuthUiState.Error(result.message ?: "비밀번호 변경 실패")
                    _events.send(result.message ?: "비밀번호 변경 실패")
                }
            }
        }
    }
}