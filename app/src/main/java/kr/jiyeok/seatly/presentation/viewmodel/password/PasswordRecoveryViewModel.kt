package kr.jiyeok.seatly.presentation.viewmodel.password

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kr.jiyeok.seatly.data.FakeAuthRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class PasswordRecoveryViewModel : ViewModel() {

    // 내부 이메일 상태와 외부 읽기 전용 노출
    private var _email by mutableStateOf("")
    val email: String
        get() = _email

    var isLoading by mutableStateOf(false)
        private set

    // errorMessage: 내부에서만 설정 가능하도록 하고, 외부에는 읽기 전용으로 노출
    private var _errorMessage by mutableStateOf<String?>(null)
    val errorMessage: String?
        get() = _errorMessage

    var codeSent by mutableStateOf(false)
        private set

    var secondsLeft by mutableStateOf(300) // 5:00
        private set

    private var timerJob: Job? = null
    var isTimerRunning by mutableStateOf(false)
        private set

    var verified by mutableStateOf(false)
        private set

    // 이메일 업데이트 메서드
    fun updateEmail(value: String) {
        _email = value
        clearError()
    }

    // 에러 초기화 (UI에서 호출)
    fun clearError() {
        _errorMessage = null
    }

    private fun setError(message: String?) {
        _errorMessage = message
    }

    fun startTimer() {
        timerJob?.cancel()
        secondsLeft = 300
        isTimerRunning = true
        timerJob = viewModelScope.launch {
            while (isTimerRunning && secondsLeft > 0) {
                delay(1000L)
                secondsLeft -= 1
            }
            if (secondsLeft <= 0) isTimerRunning = false
        }
    }

    fun stopTimer() {
        timerJob?.cancel()
        isTimerRunning = false
    }

    fun requestSecurityCode(onSuccess: () -> Unit = {}, onFailure: (() -> Unit)? = null) {
        if (email.isBlank()) {
            setError("이메일을 입력해주세요")
            onFailure?.invoke()
            return
        }
        viewModelScope.launch {
            isLoading = true
            setError(null)
            val registered = try {
                FakeAuthRepository.isEmailRegistered(email)
            } catch (e: Exception) {
                false
            }
            if (!registered) {
                isLoading = false
                setError("등록된 이메일이 아닙니다")
                onFailure?.invoke()
                return@launch
            }
            val sent = try {
                FakeAuthRepository.sendSecurityCode(email)
            } catch (e: Exception) {
                false
            }
            isLoading = false
            if (sent) {
                codeSent = true
                startTimer()
                onSuccess()
            } else {
                setError("보안 코드 전송에 실패했습니다")
                onFailure?.invoke()
            }
        }
    }

    fun resendSecurityCode(onComplete: (() -> Unit)? = null) {
        if (email.isBlank()) {
            setError("이메일이 없습니다")
            return
        }
        viewModelScope.launch {
            isLoading = true
            val ok = try {
                FakeAuthRepository.resendSecurityCode(email)
            } catch (e: Exception) {
                false
            }
            isLoading = false
            if (ok) {
                codeSent = true
                startTimer()
                onComplete?.invoke()
            } else {
                setError("재전송 실패")
            }
        }
    }

    fun verifyCode(code: String, onSuccess: () -> Unit = {}, onFailure: (() -> Unit)? = null) {
        if (email.isBlank()) {
            setError("이메일이 없습니다")
            onFailure?.invoke()
            return
        }
        if (code.length != 6) {
            setError("6자리 코드를 입력해주세요")
            onFailure?.invoke()
            return
        }
        viewModelScope.launch {
            isLoading = true
            val ok = try {
                FakeAuthRepository.verifySecurityCode(email, code)
            } catch (e: Exception) {
                false
            }
            isLoading = false
            if (ok) {
                verified = true
                stopTimer()
                onSuccess()
            } else {
                setError("코드가 올바르지 않습니다")
                onFailure?.invoke()
            }
        }
    }

    fun resetPassword(newPassword: String, onSuccess: () -> Unit = {}, onFailure: (() -> Unit)? = null) {
        if (email.isBlank()) {
            setError("이메일이 없습니다")
            onFailure?.invoke()
            return
        }
        viewModelScope.launch {
            isLoading = true
            val ok = try {
                FakeAuthRepository.resetPassword(email, newPassword)
            } catch (e: Exception) {
                false
            }
            isLoading = false
            if (ok) {
                onSuccess()
            } else {
                setError("비밀번호 변경 실패")
                onFailure?.invoke()
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
    }
}