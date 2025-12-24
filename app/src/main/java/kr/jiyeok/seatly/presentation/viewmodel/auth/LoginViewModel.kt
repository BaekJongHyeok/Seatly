package kr.jiyeok.seatly.presentation.viewmodel.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kr.jiyeok.seatly.domain.usecase.LoginUseCase
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val loginUseCase: LoginUseCase
) : ViewModel() {

    // UI 상태
    private val _loginState = MutableStateFlow<LoginState>(LoginState.Idle)
    val loginState: StateFlow<LoginState> = _loginState

    fun login(email: String, password: String) {
        if (!validateInput(email, password)) {
            _loginState.value = LoginState.Error("이메일과 비밀번호를 입력하세요")
            return
        }

        viewModelScope.launch {
            _loginState.value = LoginState.Loading

            loginUseCase.login(email, password)
                .onSuccess { loginResponse ->
                    _loginState.value = LoginState.Success(loginResponse)
                }
                .onFailure { error ->
                    _loginState.value = LoginState.Error(
                        error.message ?: "로그인 실패"
                    )
                }
        }
    }

    private fun validateInput(email: String, password: String): Boolean {
        return email.isNotEmpty() && password.isNotEmpty()
    }
}

// 로그인 상태 sealed class
sealed class LoginState {
    object Idle : LoginState()
    object Loading : LoginState()
    data class Success(val data: Any) : LoginState()
    data class Error(val message: String) : LoginState()
}
