package kr.jiyeok.seatly.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kr.jiyeok.seatly.data.remote.request.*
import kr.jiyeok.seatly.data.repository.ApiResult
import kr.jiyeok.seatly.data.remote.enums.ERole
import kr.jiyeok.seatly.data.remote.response.UserInfoSummaryDto
import kr.jiyeok.seatly.domain.usecase.*

/**
 * 인증 UI 상태
 */
sealed interface AuthUiState {
    data object Idle : AuthUiState
    data object Loading : AuthUiState
    data class Success(val data: Any? = null) : AuthUiState
    data class Error(val message: String) : AuthUiState
}

class AuthViewModel(
    private val loginUseCase: LoginUseCase,
    private val logoutUseCase: LogoutUseCase,
    private val signUpUseCase: RegisterUseCase
) : ViewModel() {

    // =====================================================
    // State Management
    // =====================================================

    private val _authState = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val authState: StateFlow<AuthUiState> = _authState.asStateFlow()

    private val _userData = MutableStateFlow<UserInfoSummaryDto?>(null)
    val userData: StateFlow<UserInfoSummaryDto?> = _userData.asStateFlow()

    private val _userRole = MutableStateFlow<ERole>(ERole.USER)
    val userRole: StateFlow<ERole> = _userRole.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _events = Channel<String>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    // =====================================================
    // Public Methods - Authentication
    // =====================================================

    fun login(email: String, password: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _authState.value = AuthUiState.Loading

            try {
                val request = LoginRequest(email, password)
                when (val result = loginUseCase(request)) {
                    is ApiResult.Success -> {
                        val userInfo = result.data
                        _userData.value = userInfo
                        _userRole.value = userInfo.role

                        _authState.value = AuthUiState.Success(userInfo)
                        _events.send("로그인 성공")
                    }
                    is ApiResult.Failure -> {
                        _authState.value = AuthUiState.Error(result.message ?: "로그인 실패")
                        _events.send(result.message ?: "로그인 실패")
                    }
                    is ApiResult.Loading -> {
                        _authState.value = AuthUiState.Loading
                    }
                }
            } finally {
                _isLoading.value = false
            }
        }
    }


    fun logout() {
        viewModelScope.launch {
            _isLoading.value = true
            _authState.value = AuthUiState.Loading
            try {
                when (val result = logoutUseCase()) {
                    is ApiResult.Success -> {
                        _userData.value = null
                        _userRole.value = ERole.USER

                        _authState.value = AuthUiState.Idle
                        _events.send("로그아웃 되었습니다")
                    }
                    is ApiResult.Failure -> {
                        _authState.value = AuthUiState.Error(result.message ?: "로그아웃 실패")
                        _events.send(result.message ?: "로그아웃 실패")
                    }
                    is ApiResult.Loading -> {
                        _authState.value = AuthUiState.Loading
                    }
                }
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun setUserRole(role: ERole) {
        _userRole.value = role
    }

    fun signUp(request: RegisterRequest) {
        viewModelScope.launch {
            _isLoading.value = true
            _authState.value = AuthUiState.Loading
            try {
                when (val result = signUpUseCase(request)) {
                    is ApiResult.Success -> {
                        _authState.value = AuthUiState.Success(Unit)
                        _events.send("회원가입 되었습니다")
                    }
                    is ApiResult.Failure -> {
                        _events.send(result.message ?: "회원가입 실패")
                    }
                    is ApiResult.Loading -> {
                        _authState.value = AuthUiState.Loading
                    }
                }
            } finally {
                _isLoading.value = false
            }
        }
    }
}
