package kr.jiyeok.seatly.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kr.jiyeok.seatly.data.remote.request.ForgotPasswordRequest
import kr.jiyeok.seatly.data.remote.request.VerifyCodeRequest
import kr.jiyeok.seatly.data.repository.ApiResult
import kr.jiyeok.seatly.domain.usecase.ForgotPasswordUseCase
import kr.jiyeok.seatly.domain.usecase.VerifyCodeUseCase
import kr.jiyeok.seatly.di.IoDispatcher
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import javax.inject.Inject

/**
 * Password Recovery ViewModel
 * 
 * 역할:
 * - 비밀번호 재설정 흐름 관리
 * - 1단계: 보안 코드 요청 및 이메일 전송
 * - 2단계: 보안 코드 검증
 * - 3단계: 비밀번호 변경 (AuthViewModel의 changePassword 사용)
 * 
 * UI는 StateFlow를 통해 상태를 관찰하고,
 * 에러/이벤트는 [events] Channel을 통해 수신합니다
 */

/**
 * 비밀번호 재설정 UI 상태
 */
sealed interface PasswordRecoveryUiState {
    object Idle : PasswordRecoveryUiState
    object Loading : PasswordRecoveryUiState
    data class Success(val message: String = "") : PasswordRecoveryUiState
    data class Error(val message: String) : PasswordRecoveryUiState
}

@HiltViewModel
class PasswordRecoveryViewModel @Inject constructor(
    private val forgotPasswordUseCase: ForgotPasswordUseCase,
    private val verifyCodeUseCase: VerifyCodeUseCase,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : ViewModel() {

    // =====================================================
    // State Management
    // =====================================================

    /**
     * 비밀번호 재설정 UI 상태
     * Idle → Loading → Success/Error
     */
    private val _uiState = MutableStateFlow<PasswordRecoveryUiState>(PasswordRecoveryUiState.Idle)
    val uiState: StateFlow<PasswordRecoveryUiState> = _uiState.asStateFlow()

    /**
     * 입력된 이메일
     */
    private val _email = MutableStateFlow("")
    val email: StateFlow<String> = _email.asStateFlow()

    /**
     * 검증된 코드 (최종 비밀번호 변경 시 사용)
     */
    private val _verifiedCode = MutableStateFlow("")
    val verifiedCode: StateFlow<String> = _verifiedCode.asStateFlow()

    /**
     * 코드 유효 시간 (초 단위 카운트다운)
     */
    private val _codeValiditySeconds = MutableStateFlow(0)
    val codeValiditySeconds: StateFlow<Int> = _codeValiditySeconds.asStateFlow()

    /**
     * 현재 단계 (1: 이메일, 2: 코드 검증, 3: 비밀번호 변경)
     */
    private val _currentStep = MutableStateFlow(1)
    val currentStep: StateFlow<Int> = _currentStep.asStateFlow()

    /**
     * 로딩 상태
     */
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    /**
     * 에러 메시지
     */
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    /**
     * 에러/이벤트 메시지 Channel
     * UI에서 토스트 메시지나 스낵바로 표시
     */
    private val _events = Channel<String>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    /**
     * 타이머 작업
     */
    private var timerJob: Job? = null

    // =====================================================
    // Public Methods
    // =====================================================

    /**
     * 이메일 업데이트
     */
    fun updateEmail(email: String) {
        _email.value = email
        _error.value = null
    }

    /**
     * 1단계: 보안 코드 요청 (이메일로 전송)
     */
    suspend fun requestSecurityCode() {
        val emailValue = _email.value
        
        if (emailValue.isBlank()) {
            _error.value = "이메일을 입력하세요"
            _events.runCatching { send("이메일을 입력하세요") }
            return
        }

        if (!isValidEmail(emailValue)) {
            _error.value = "올바른 이메일 형식이 아닙니다"
            _events.runCatching { send("올바른 이메일 형식이 아닙니다") }
            return
        }

        viewModelScope.launch(ioDispatcher) {
            _isLoading.value = true
            _uiState.value = PasswordRecoveryUiState.Loading
            _error.value = null
            try {
                val request = ForgotPasswordRequest(email = emailValue)
                when (val result = forgotPasswordUseCase(request)) {
                    is ApiResult.Success -> {
                        // 3분(180초) 코드 유효성 타이머 시작
                        startCodeValidityTimer(180)
                        _currentStep.value = 2
                        _uiState.value = PasswordRecoveryUiState.Success("보안 코드가 이메일로 발송되었습니다")
                        _events.send("보안 코드가 이메일로 발송되었습니다")
                    }
                    is ApiResult.Failure -> {
                        _error.value = result.message ?: "코드 요청 실패"
                        _uiState.value = PasswordRecoveryUiState.Error(result.message ?: "코드 요청 실패")
                        _events.send(result.message ?: "코드 요청 실패")
                    }
                }
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * 코드 재전송
     */
    suspend fun resendSecurityCode() {
        requestSecurityCode()
    }

    /**
     * 2단계: 보안 코드 검증
     */
    suspend fun verifyCode(code: String) {
        val emailValue = _email.value

        if (code.isBlank()) {
            _error.value = "보안 코드를 입력하세요"
            _events.runCatching { send("보안 코드를 입력하세요") }
            return
        }

        if (code.length < 4) {
            _error.value = "올바른 보안 코드를 입력하세요"
            _events.runCatching { send("올바른 보안 코드를 입력하세요") }
            return
        }

        viewModelScope.launch(ioDispatcher) {
            _isLoading.value = true
            _uiState.value = PasswordRecoveryUiState.Loading
            _error.value = null
            try {
                val request = VerifyCodeRequest(email = emailValue, code = code)
                when (val result = verifyCodeUseCase(request)) {
                    is ApiResult.Success -> {
                        _verifiedCode.value = code
                        _currentStep.value = 3
                        _uiState.value = PasswordRecoveryUiState.Success("코드 검증 완료")
                        _events.send("코드 검증 완료")
                    }
                    is ApiResult.Failure -> {
                        _error.value = result.message ?: "코드 검증 실패"
                        _uiState.value = PasswordRecoveryUiState.Error(result.message ?: "코드 검증 실패")
                        _events.send(result.message ?: "코드 검증 실패")
                    }
                }
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * 비밀번호 재설정 준비
     * 최종 비밀번호 변경은 AuthViewModel.changePassword를 호출하면 됨
     */
    fun getResetInfo(): Pair<String, String>? {
        val email = _email.value
        val code = _verifiedCode.value
        
        return if (email.isNotBlank() && code.isNotBlank()) {
            Pair(email, code)
        } else {
            null
        }
    }

    /**
     * 흐름 초기화 (처음부터 다시 시작)
     */
    fun reset() {
        _email.value = ""
        _verifiedCode.value = ""
        _currentStep.value = 1
        _error.value = null
        _uiState.value = PasswordRecoveryUiState.Idle
        timerJob?.cancel()
        _codeValiditySeconds.value = 0
    }

    /**
     * 에러 초기화
     */
    fun clearError() {
        _error.value = null
    }

    /**
     * 상태 초기화
     */
    fun resetState() {
        _uiState.value = PasswordRecoveryUiState.Idle
        _error.value = null
    }

    // =====================================================
    // Private Helper Methods
    // =====================================================

    /**
     * 이메일 유효성 검사 (간단한 정규식)
     */
    private fun isValidEmail(email: String): Boolean {
        return email.contains("@") && email.contains(".")
    }

    /**
     * 코드 유효 시간 카운트다운 시작 (초)
     */
    private fun startCodeValidityTimer(seconds: Int) {
        timerJob?.cancel()
        _codeValiditySeconds.value = seconds
        
        timerJob = viewModelScope.launch {
            while (_codeValiditySeconds.value > 0) {
                delay(1000L)
                _codeValiditySeconds.value = _codeValiditySeconds.value - 1
            }
        }
    }

    /**
     * ViewModel 해제 시 타이머 정리
     */
    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
    }
}
