package kr.jiyeok.seatly.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kr.jiyeok.seatly.data.remote.request.StartSessionRequest
import kr.jiyeok.seatly.data.remote.response.SessionResponseDto
import kr.jiyeok.seatly.data.repository.ApiResult
import kr.jiyeok.seatly.domain.usecase.session.*
import javax.inject.Inject

sealed interface SessionUiState {
    object Idle : SessionUiState
    object Loading : SessionUiState
    data class ActiveSessions(val sessions: List<SessionResponseDto>) : SessionUiState
    data class SessionResult(val session: SessionResponseDto?) : SessionUiState
    data class Error(val message: String) : SessionUiState
}

@HiltViewModel
class SessionViewModel @Inject constructor(
    private val startSessionUseCase: StartSessionUseCase,
    private val endSessionUseCase: EndSessionUseCase,
    private val getCurrentSessionsUseCase: GetCurrentSessionsUseCase
) : ViewModel() {

    private val _state = MutableStateFlow<SessionUiState>(SessionUiState.Idle)
    val state: StateFlow<SessionUiState> = _state.asStateFlow()

    private val _events = Channel<String>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    fun startSession(request: StartSessionRequest) {
        viewModelScope.launch {
            _state.value = SessionUiState.Loading
            when (val res = startSessionUseCase(request)) {
                is ApiResult.Success -> {
                    _state.value = SessionUiState.SessionResult(res.data)
                    _events.send("이용이 시작되었습니다.")
                }
                is ApiResult.Failure -> {
                    _state.value = SessionUiState.Error(res.message ?: "이용 시작 실패")
                    _events.send(res.message ?: "이용 시작 실패")
                }
            }
        }
    }

    fun endSession(sessionId: Long) {
        viewModelScope.launch {
            _state.value = SessionUiState.Loading
            when (val res = endSessionUseCase(sessionId)) {
                is ApiResult.Success -> {
                    _state.value = SessionUiState.SessionResult(res.data)
                    _events.send("이용이 종료되었습니다.")
                }
                is ApiResult.Failure -> {
                    _state.value = SessionUiState.Error(res.message ?: "이용 종료 실패")
                    _events.send(res.message ?: "이용 종료 실패")
                }
            }
        }
    }

    fun loadCurrentSessions(studyCafeId: Long? = null) {
        viewModelScope.launch {
            _state.value = SessionUiState.Loading
            when (val res = getCurrentSessionsUseCase(studyCafeId)) {
                is ApiResult.Success -> {
                    _state.value = SessionUiState.ActiveSessions(res.data ?: emptyList())
                }
                is ApiResult.Failure -> {
                    _state.value = SessionUiState.Error(res.message ?: "세션 목록 조회 실패")
                    _events.send(res.message ?: "세션 목록 조회 실패")
                }
            }
        }
    }
}