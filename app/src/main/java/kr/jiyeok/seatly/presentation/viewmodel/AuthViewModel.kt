package kr.jiyeok.seatly.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kr.jiyeok.seatly.data.remote.request.*
import kr.jiyeok.seatly.data.remote.response.LoginResponse
import kr.jiyeok.seatly.data.remote.response.UserResponseDto
import kr.jiyeok.seatly.data.repository.ApiResult
import kr.jiyeok.seatly.domain.usecase.*
import javax.inject.Inject

/**
 * Authentication ViewModel
 * 
 * 역할:
 * - 로그인/로그아웃 처리
 * - 회원가입 (일반/소셜)
 * - 비밀번호 초기화 플로우
 * - 사용자 정보 관리
 * 
 * UI는 StateFlow를 통해 상태를 관찰하고,
 * 에러/이벤트는 [events] Channel을 통해 수신합니다
 */

/**
 * 인증 UI 상태
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
    private val refreshTokenUseCase: RefreshTokenUseCase,
    private val socialRegisterUseCase: SocialRegisterUseCase,
    private val forgotPasswordUseCase: ForgotPasswordUseCase,
    private val verifyCodeUseCase: VerifyCodeUseCase,
    private val getUserInfoUseCase: GetUserInfoUseCase,
    private val updateUserInfoUseCase: UpdateUserInfoUseCase,
    private val changePasswordUseCase: ChangePasswordUseCase
) : ViewModel() {

    // =====================================================
    // State Management
    // =====================================================

    /**
     * 인증 상태
     * Idle → Loading → Success/Error
     */
    private val _authState = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val authState: StateFlow<AuthUiState> = _authState.asStateFlow()

    /**
     * 로그인 응답 데이터
     * accessToken, refreshToken, user 정보 포함
     */
    private val _loginData = MutableStateFlow<LoginResponse?>(null)
    val loginData: StateFlow<LoginResponse?> = _loginData.asStateFlow()

    /**
     * 현재 로그인한 사용자 정보
     */
    private val _userData = MutableStateFlow<UserResponseDto?>(null)
    val userData: StateFlow<UserResponseDto?> = _userData.asStateFlow()

    /**
     * 사용자 역할
     * USER, ADMIN
     */
    private val _userRole = MutableStateFlow<String>("USER")
    val userRole: StateFlow<String> = _userRole.asStateFlow()

    /**
     * 로딩 상태
     */
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    /**
     * 에러/이벤트 메시지 Channel
     * UI에서 토스트 메시지나 스낵바로 표시
     */
    private val _events = Channel<String>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    // =====================================================
    // Public Methods - Authentication
    // =====================================================

    /**
     * 로그인
     */
    fun login(email: String, password: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _authState.value = AuthUiState.Loading
            try {
                val request = LoginRequest(email, password)
                when (val result = loginUseCase(request)) {
                    is ApiResult.Success -> {
                        val loginResponse = result.data
                        _loginData.value = loginResponse
                        _userData.value = loginResponse?.user
                        _userRole.value = loginResponse?.user?.role ?: "USER"
                        _authState.value = AuthUiState.Success(loginResponse?.user)
                        _events.send("로그인 성공")
                    }
                    is ApiResult.Failure -> {
                        _authState.value = AuthUiState.Error(result.message ?: "로그인 실패")
                        _events.send(result.message ?: "로그인 실패")
                    }
                }
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * 로그아웃
     */
    fun logout() {
        viewModelScope.launch {
            _isLoading.value = true
            _authState.value = AuthUiState.Loading
            try {
                when (val result = logoutUseCase()) {
                    is ApiResult.Success -> {
                        _loginData.value = null
                        _userData.value = null
                        _userRole.value = "USER"
                        _authState.value = AuthUiState.Success(Unit)
                        _events.send("로그아웃 되었습니다")
                    }
                    is ApiResult.Failure -> {
                        _authState.value = AuthUiState.Error(result.message ?: "로그아웃 실패")
                        _events.send(result.message ?: "로그아웃 실패")
                    }
                }
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * 회원가입
     */
    fun register(email: String, password: String, name: String, phone: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _authState.value = AuthUiState.Loading
            try {
                val request = RegisterRequest(email, password, name, phone)
                when (val result = registerUseCase(request)) {
                    is ApiResult.Success -> {
                        _authState.value = AuthUiState.Success(Unit)
                        _events.send("회원가입 완료")
                    }
                    is ApiResult.Failure -> {
                        _authState.value = AuthUiState.Error(result.message ?: "회원가입 실패")
                        _events.send(result.message ?: "회원가입 실패")
                    }
                }
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * 토큰 리프레시
     */
    fun refreshToken(refreshToken: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val request = RefreshTokenRequest(refreshToken)
                when (val result = refreshTokenUseCase(request)) {
                    is ApiResult.Success -> {
                        _authState.value = AuthUiState.Success(result.data)
                        _events.send("토큰 갱신 완료")
                    }
                    is ApiResult.Failure -> {
                        _authState.value = AuthUiState.Error(result.message ?: "토큰 갱신 실패")
                        _events.send(result.message ?: "토큰 갱신 실패")
                    }
                }
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * 소셜 회원가입 (보류)
     */
    fun socialRegister(email: String, name: String, phone: String?, provider: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _authState.value = AuthUiState.Loading
            try {
                val request = SocialRegisterRequest(email, name, phone, provider)
                when (val result = socialRegisterUseCase(request)) {
                    is ApiResult.Success -> {
                        _authState.value = AuthUiState.Success(Unit)
                        _events.send("소셜 회원가입 완료")
                    }
                    is ApiResult.Failure -> {
                        _authState.value = AuthUiState.Error(result.message ?: "소셜 회원가입 실패")
                        _events.send(result.message ?: "소셜 회원가입 실패")
                    }
                }
            } finally {
                _isLoading.value = false
            }
        }
    }

    // =====================================================
    // Public Methods - Password Reset (보류)
    // =====================================================

    /**
     * 비밀번호 초기화 요청 (보안 코드 전송)
     */
    fun forgotPassword(email: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _authState.value = AuthUiState.Loading
            try {
                val request = ForgotPasswordRequest(email)
                when (val result = forgotPasswordUseCase(request)) {
                    is ApiResult.Success -> {
                        _authState.value = AuthUiState.Success(Unit)
                        _events.send("보안 코드가 이메일로 발송되었습니다")
                    }
                    is ApiResult.Failure -> {
                        _authState.value = AuthUiState.Error(result.message ?: "이메일 전송 실패")
                        _events.send(result.message ?: "이메일 전송 실패")
                    }
                }
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * 비밀번호 초기화 코드 검증
     */
    fun verifyPasswordResetCode(email: String, code: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _authState.value = AuthUiState.Loading
            try {
                val request = VerifyCodeRequest(email, code)
                when (val result = verifyCodeUseCase(request)) {
                    is ApiResult.Success -> {
                        _authState.value = AuthUiState.Success(Unit)
                        _events.send("보안 코드 검증 완료")
                    }
                    is ApiResult.Failure -> {
                        _authState.value = AuthUiState.Error(result.message ?: "코드 검증 실패")
                        _events.send(result.message ?: "코드 검증 실패")
                    }
                }
            } finally {
                _isLoading.value = false
            }
        }
    }

    // =====================================================
    // Public Methods - User Profile
    // =====================================================

    /**
     * 사용자 정보 조회
     */
    fun getUserInfo() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                when (val result = getUserInfoUseCase()) {
                    is ApiResult.Success -> {
                        _userData.value = result.data
                        _userRole.value = result.data?.role ?: "USER"
                        _authState.value = AuthUiState.Success(result.data)
                    }
                    is ApiResult.Failure -> {
                        _authState.value = AuthUiState.Error(result.message ?: "사용자 정보 조회 실패")
                        _events.send(result.message ?: "사용자 정보 조회 실패")
                    }
                }
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * 사용자 정보 수정
     */
    fun updateUserInfo(name: String?, phone: String?, imageUrl: String?) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val request = UpdateUserRequest(name, phone, imageUrl)
                when (val result = updateUserInfoUseCase(request)) {
                    is ApiResult.Success -> {
                        _userData.value = result.data
                        _authState.value = AuthUiState.Success(result.data)
                        _events.send("사용자 정보가 업데이트되었습니다")
                    }
                    is ApiResult.Failure -> {
                        _authState.value = AuthUiState.Error(result.message ?: "정보 수정 실패")
                        _events.send(result.message ?: "정보 수정 실패")
                    }
                }
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * 비밀번호 변경
     */
    fun changePassword(userId: Long, currentPassword: String, newPassword: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _authState.value = AuthUiState.Loading
            try {
                val request = ChangePasswordRequest(currentPassword, newPassword)
                when (val result = changePasswordUseCase(userId, request)) {
                    is ApiResult.Success -> {
                        _authState.value = AuthUiState.Success(Unit)
                        _events.send("비밀번호가 변경되었습니다")
                    }
                    is ApiResult.Failure -> {
                        _authState.value = AuthUiState.Error(result.message ?: "비밀번호 변경 실패")
                        _events.send(result.message ?: "비밀번호 변경 실패")
                    }
                }
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * 계정 삭제
     * TODO: 추가 구현 필요
     */
    suspend fun deleteAccount() {
        _events.runCatching { send("계정 삭제 기능은 아직 구현되지 않았습니다") }
    }
}
