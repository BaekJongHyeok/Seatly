package kr.jiyeok.seatly.presentation.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kr.jiyeok.seatly.data.remote.request.ForgotPasswordRequest
import kr.jiyeok.seatly.data.remote.request.ResetPasswordRequest
import kr.jiyeok.seatly.data.remote.request.VerifyCodeRequest
import kr.jiyeok.seatly.data.repository.ApiResult
import kr.jiyeok.seatly.domain.usecase.auth.RequestPasswordResetUseCase
import kr.jiyeok.seatly.domain.usecase.auth.ResetPasswordUseCase
import kr.jiyeok.seatly.domain.usecase.auth.VerifyPasswordResetCodeUseCase
import kr.jiyeok.seatly.di.IoDispatcher
import javax.inject.Inject
import dagger.hilt.android.lifecycle.HiltViewModel

/**
 * ViewModel for password recovery flow (step1: request code, step2: verify code, step3: reset password).
 *
 * Exposes simple Compose-backed state properties so existing composables can read them directly
 * (keeps the UI identical while wiring real network calls).
 */
@HiltViewModel
class PasswordRecoveryViewModel @Inject constructor(
    private val requestPasswordResetUseCase: RequestPasswordResetUseCase,
    private val verifyPasswordResetCodeUseCase: VerifyPasswordResetCodeUseCase,
    private val resetPasswordUseCase: ResetPasswordUseCase,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : ViewModel() {

    // bound to UI
    var email by mutableStateOf("")
        private set

    // server-side / flow state
    var isLoading by mutableStateOf(false)
        private set

    var errorMessage by mutableStateOf<String?>(null)
        private set

    // countdown for code validity / resend UI (seconds)
    var secondsLeft by mutableStateOf(0)
        private set

    // last verified code (kept to use in reset step)
    private var verifiedCode: String? = null

    private var timerJob: Job? = null

    fun updateEmail(value: String) {
        email = value
        // clear previous server errors when user edits
        errorMessage = null
    }

    fun clearError() {
        errorMessage = null
    }

    /**
     * Request security code to be sent to [email].
     * onSuccess is invoked when backend accepted the request.
     */
    fun requestSecurityCode(onSuccess: () -> Unit = {}, onFailure: ((String) -> Unit)? = null) {
        if (email.isBlank()) {
            val msg = "이메일을 입력하세요."
            errorMessage = msg
            onFailure?.invoke(msg)
            return
        }

        viewModelScope.launch(ioDispatcher) {
            isLoading = true
            errorMessage = null
            try {
                val req = ForgotPasswordRequest(email = email)
                when (val res = requestPasswordResetUseCase(req)) {
                    is ApiResult.Success -> {
                        // start countdown (common default 3 minutes)
                        startCountdown(180)
                        onSuccess()
                    }
                    is ApiResult.Failure -> {
                        val msg = res.message ?: "보안 코드 전송 실패"
                        errorMessage = msg
                        onFailure?.invoke(msg)
                    }
                }
            } catch (t: Throwable) {
                val msg = t.localizedMessage ?: "알 수 없는 오류"
                errorMessage = msg
                onFailure?.invoke(msg)
            } finally {
                isLoading = false
            }
        }
    }

    fun resendSecurityCode() {
        // Allow resend even if secondsLeft > 0 — backend may throttle; UI keeps responsibility
        requestSecurityCode()
    }

    /**
     * Verify the security code that user entered.
     * On success we store the verified code to be used in the final reset step.
     */
    fun verifyCode(code: String, onSuccess: () -> Unit = {}, onFailure: ((String) -> Unit)? = null) {
        if (email.isBlank()) {
            val msg = "이메일이 없습니다."
            errorMessage = msg
            onFailure?.invoke(msg)
            return
        }
        if (code.length < 4) {
            val msg = "올바른 보안 코드를 입력하세요."
            errorMessage = msg
            onFailure?.invoke(msg)
            return
        }

        viewModelScope.launch(ioDispatcher) {
            isLoading = true
            errorMessage = null
            try {
                val req = VerifyCodeRequest(email = email, code = code)
                when (val res = verifyPasswordResetCodeUseCase(req)) {
                    is ApiResult.Success -> {
                        verifiedCode = code
                        onSuccess()
                    }
                    is ApiResult.Failure -> {
                        val msg = res.message ?: "코드 검증 실패"
                        errorMessage = msg
                        onFailure?.invoke(msg)
                    }
                }
            } catch (t: Throwable) {
                val msg = t.localizedMessage ?: "알 수 없는 오류"
                errorMessage = msg
                onFailure?.invoke(msg)
            } finally {
                isLoading = false
            }
        }
    }

    /**
     * Reset the password using previously verified code.
     * If the code was not verified in this session, backend may still accept it (depending on backend design).
     */
    fun resetPassword(
        newPassword: String,
        onSuccess: () -> Unit = {},
        onFailure: ((String) -> Unit)? = null
    ) {
        val codeToUse = verifiedCode
        if (email.isBlank() || codeToUse.isNullOrBlank()) {
            val msg = "이메일 또는 보안 코드가 없습니다."
            errorMessage = msg
            onFailure?.invoke(msg)
            return
        }

        viewModelScope.launch(ioDispatcher) {
            isLoading = true
            errorMessage = null
            try {
                val req = ResetPasswordRequest(
                    email = email,
                    code = codeToUse,
                    newPassword = newPassword,
                    newPasswordConfirm = newPassword
                )
                when (val res = resetPasswordUseCase(req)) {
                    is ApiResult.Success -> {
                        // clear sensitive stored code after success
                        verifiedCode = null
                        onSuccess()
                    }
                    is ApiResult.Failure -> {
                        val msg = res.message ?: "비밀번호 변경 실패"
                        errorMessage = msg
                        onFailure?.invoke(msg)
                    }
                }
            } catch (t: Throwable) {
                val msg = t.localizedMessage ?: "알 수 없는 오류"
                errorMessage = msg
                onFailure?.invoke(msg)
            } finally {
                isLoading = false
            }
        }
    }

    private fun startCountdown(seconds: Int) {
        timerJob?.cancel()
        secondsLeft = seconds
        timerJob = viewModelScope.launch {
            while (secondsLeft > 0) {
                delay(1000L)
                secondsLeft = secondsLeft - 1
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
    }
}