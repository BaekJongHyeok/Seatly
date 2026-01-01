package kr.jiyeok.seatly.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kr.jiyeok.seatly.data.remote.response.SessionDto
import kr.jiyeok.seatly.data.repository.ApiResult
import kr.jiyeok.seatly.domain.usecase.GetSessionsUseCase
import kr.jiyeok.seatly.domain.usecase.EndSessionUseCase
import kr.jiyeok.seatly.di.IoDispatcher
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject

/**
 * Session Management ViewModel
 * 
 * 역할:
 * - 사용자 세션 목록 관리
 * - 현재 활성 세션 조회
 * - 세션 종료 처리
 * 
 * UI는 StateFlow를 통해 상태를 관찰하고,
 * 에러/이벤트는 [events] Channel을 통해 수신합니다
 */

/**
 * 세션 UI 상태
 */
sealed interface SessionUiState {
    object Idle : SessionUiState
    object Loading : SessionUiState
    data class Success(val message: String = "") : SessionUiState
    data class Error(val message: String) : SessionUiState
}

@HiltViewModel
class SessionViewModel @Inject constructor(
    private val getSessionsUseCase: GetSessionsUseCase,
    private val endSessionUseCase: EndSessionUseCase,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : ViewModel() {

    // =====================================================
    // State Management
    // =====================================================

    /**
     * 세션 UI 상태
     * Idle → Loading → Success/Error
     */
    private val _sessionState = MutableStateFlow<SessionUiState>(SessionUiState.Idle)
    val sessionState: StateFlow<SessionUiState> = _sessionState.asStateFlow()

    /**
     * 사용자의 모든 세션 목록
     */
    private val _sessions = MutableStateFlow<List<SessionDto>>(emptyList())
    val sessions: StateFlow<List<SessionDto>> = _sessions.asStateFlow()

    /**
     * 현재 활성 세션 (IN_USE 또는 ASSIGNED 상태)
     */
    private val _currentSession = MutableStateFlow<SessionDto?>(null)
    val currentSession: StateFlow<SessionDto?> = _currentSession.asStateFlow()

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

    // =====================================================
    // Public Methods
    // =====================================================

    /**
     * 사용자의 모든 세션 로드
     */
    fun loadSessions() {
        viewModelScope.launch(ioDispatcher) {
            _isLoading.value = true
            _sessionState.value = SessionUiState.Loading
            _error.value = null
            try {
                when (val result = getSessionsUseCase()) {
                    is ApiResult.Success -> {
                        val sessionList = result.data ?: emptyList()
                        _sessions.value = sessionList

                        // 활성 세션 추출 (IN_USE 또는 ASSIGNED 상태)
                        val activeSession = sessionList.firstOrNull { session ->
                            session.status.equals("IN_USE", ignoreCase = true) ||
                            session.status.equals("ASSIGNED", ignoreCase = true)
                        }
                        _currentSession.value = activeSession

                        _sessionState.value = SessionUiState.Success("세션 로드 완료")
                    }
                    is ApiResult.Failure -> {
                        _error.value = result.message ?: "세션 조회 실패"
                        _sessionState.value = SessionUiState.Error(result.message ?: "세션 조회 실패")
                        _events.send(result.message ?: "세션 조회 실패")
                    }
                }
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * 현재 활성 세션 새로고침
     */
    fun refreshCurrentSession() {
        viewModelScope.launch(ioDispatcher) {
            when (val result = getSessionsUseCase()) {
                is ApiResult.Success -> {
                    val sessionList = result.data ?: emptyList()
                    _sessions.value = sessionList

                    // 활성 세션 업데이트
                    val activeSession = sessionList.firstOrNull { session ->
                        session.status.equals("IN_USE", ignoreCase = true) ||
                        session.status.equals("ASSIGNED", ignoreCase = true)
                    }
                    _currentSession.value = activeSession
                }
                is ApiResult.Failure -> {
                    _error.value = result.message ?: "세션 새로고침 실패"
                    _events.send(result.message ?: "세션 새로고침 실패")
                }
            }
        }
    }

    /**
     * 특정 세션 종료
     */
    fun endSession(sessionId: Long) {
        viewModelScope.launch(ioDispatcher) {
            _isLoading.value = true
            _sessionState.value = SessionUiState.Loading
            _error.value = null
            try {
                when (val result = endSessionUseCase(sessionId)) {
                    is ApiResult.Success -> {
                        // 세션 종료 성공 시 현재 세션 초기화
                        if (_currentSession.value?.id == sessionId) {
                            _currentSession.value = null
                        }

                        // 세션 목록에서 제거
                        val updatedSessions = _sessions.value.filter { it.id != sessionId }
                        _sessions.value = updatedSessions

                        _sessionState.value = SessionUiState.Success("이용이 종료되었습니다")
                        _events.send("이용이 종료되었습니다")
                    }
                    is ApiResult.Failure -> {
                        _error.value = result.message ?: "세션 종료 실패"
                        _sessionState.value = SessionUiState.Error(result.message ?: "세션 종료 실패")
                        _events.send(result.message ?: "세션 종료 실패")
                    }
                }
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * 현재 활성 세션 종료
     */
    fun endCurrentSession() {
        val sessionId = _currentSession.value?.id
        if (sessionId != null) {
            endSession(sessionId)
        } else {
            viewModelScope.launch {
                _error.value = "활성 세션이 없습니다"
                _events.send("활성 세션이 없습니다")
            }
        }
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
        _sessionState.value = SessionUiState.Idle
        _error.value = null
    }
}
