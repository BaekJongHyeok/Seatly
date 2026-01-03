package kr.jiyeok.seatly.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kr.jiyeok.seatly.data.remote.request.*
import kr.jiyeok.seatly.data.repository.ApiResult
import kr.jiyeok.seatly.data.remote.enums.ERole
import kr.jiyeok.seatly.data.remote.response.UserInfoDetailDto
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
    private val logoutUseCase: LogoutUseCase
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
    private val _loginData = MutableStateFlow<UserInfoDetailDto?>(null)
    val loginData: StateFlow<UserInfoDetailDto?> = _loginData.asStateFlow()

    /**
     * 현재 로그인한 사용자 정보
     */
    private val _userData = MutableStateFlow<UserInfoDetailDto?>(null)
    val userData: StateFlow<UserInfoDetailDto?> = _userData.asStateFlow()

    /**
     * 사용자 역할
     * USER, ADMIN
     */
    private val _userRole = MutableStateFlow<ERole>(ERole.USER)
    val userRole: StateFlow<ERole> = _userRole.asStateFlow()

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
                        val userInfo = result.data

                        if (userInfo != null) {
                            _userData.value = userInfo
                            _userRole.value = userInfo.role
                            _authState.value = AuthUiState.Success(userInfo)
                            _events.send("로그인 성공")
                        } else {
                            _authState.value = AuthUiState.Error("사용자 정보가 없습니다")
                            _events.send("로그인 실패: 사용자 정보 없음")
                        }
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
                        _userRole.value = ERole.USER
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
}
