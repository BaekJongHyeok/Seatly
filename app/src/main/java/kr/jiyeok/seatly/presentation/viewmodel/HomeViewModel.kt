package kr.jiyeok.seatly.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kr.jiyeok.seatly.data.remote.response.*
import kr.jiyeok.seatly.data.repository.ApiResult
import kr.jiyeok.seatly.di.IoDispatcher
import kr.jiyeok.seatly.domain.usecase.*
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val getSessionsUseCase: GetSessionsUseCase,
    private val endSessionUseCase: EndSessionUseCase,
    private val addFavoriteCafeUseCase: AddFavoriteCafeUseCase,
    private val removeFavoriteCafeUseCase: RemoveFavoriteCafeUseCase,
    private val getStudyCafesUseCase: GetStudyCafesUseCase,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : ViewModel() {

    // =====================================================
    // State Management
    // =====================================================

    private val _currentSession = MutableStateFlow<SessionDto?>(null)
    val currentSession: StateFlow<SessionDto?> = _currentSession.asStateFlow()

    private val _cafesPage = MutableStateFlow<PageResponse<StudyCafeSummaryDto>?>(null)
    val cafesPage: StateFlow<PageResponse<StudyCafeSummaryDto>?> = _cafesPage.asStateFlow()

    // 찜한 카페 ID 목록 (UI 상태 관리용)
    private val _favoriteCafeIds = MutableStateFlow<List<Long>>(emptyList())
    val favoriteCafeIds: StateFlow<List<Long>> = _favoriteCafeIds.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _events = Channel<String>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    // =====================================================
    // Public Methods
    // =====================================================

    fun loadHomeData() {
        viewModelScope.launch(ioDispatcher) {
            _isLoading.value = true
            try {
                loadCurrentSession()
                loadCafesForHome()
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * AuthViewModel의 userData로부터 찜 목록을 동기화하기 위한 함수
     */
    fun updateFavoriteIds(ids: List<Long>) {
        _favoriteCafeIds.value = ids
    }

    fun addFavoriteCafe(cafeId: Long) {
        viewModelScope.launch(ioDispatcher) {
            when (val result = addFavoriteCafeUseCase(cafeId)) {
                is ApiResult.Success -> {
                    val newFavorites = _favoriteCafeIds.value.toMutableList()
                    if (!newFavorites.contains(cafeId)) {
                        newFavorites.add(cafeId)
                        _favoriteCafeIds.value = newFavorites
                    }
                    _events.send("즐겨찾기에 추가되었습니다")
                }
                is ApiResult.Failure -> {
                    _events.send(result.message ?: "즐겨찾기 추가 실패")
                }
            }
        }
    }

    fun removeFavoriteCafe(cafeId: Long) {
        viewModelScope.launch(ioDispatcher) {
            when (val result = removeFavoriteCafeUseCase(cafeId)) {
                is ApiResult.Success -> {
                    val newFavorites = _favoriteCafeIds.value.toMutableList()
                    newFavorites.remove(cafeId)
                    _favoriteCafeIds.value = newFavorites
                    _events.send("즐겨찾기에서 제거되었습니다")
                }
                is ApiResult.Failure -> {
                    _events.send(result.message ?: "즐겨찾기 제거 실패")
                }
            }
        }
    }

    fun endCurrentSession() {
        viewModelScope.launch(ioDispatcher) {
            _isLoading.value = true
            try {
                val session = _currentSession.value
                if (session == null) {
                    _events.send("종료할 세션이 없습니다")
                    return@launch
                }

                when (val result = endSessionUseCase(session.id)) {
                    is ApiResult.Success -> {
                        _currentSession.value = null
                        _events.send("이용이 종료되었습니다")
                    }
                    is ApiResult.Failure -> {
                        _events.send(result.message ?: "세션 종료 실패")
                    }
                }
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun refreshCurrentSession() {
        viewModelScope.launch(ioDispatcher) {
            loadCurrentSession()
        }
    }

    // =====================================================
    // Private Helper Methods
    // =====================================================

    private suspend fun loadCurrentSession() {
        when (val result = getSessionsUseCase()) {
            is ApiResult.Success -> {
                val sessions = result.data ?: emptyList()
                val activeSession = sessions.firstOrNull { session ->
                    session.status.equals("IN_USE", ignoreCase = true) ||
                            session.status.equals("ASSIGNED", ignoreCase = true)
                }
                _currentSession.value = activeSession
            }
            is ApiResult.Failure -> {
                _currentSession.value = null
                // 세션 조회가 실패해도 홈 화면 로드는 계속 진행되어야 하므로 에러만 전송
                // _events.send(result.message ?: "세션 조회 실패")
            }
        }
    }

    private suspend fun loadCafesForHome() {
        // ★ 수정: 전체 카페 목록을 가져오기 위해 size를 100으로 설정 (Mock 데이터 전체 로드)
        when (val result = getStudyCafesUseCase(page = 0, size = 100)) {
            is ApiResult.Success -> {
                _cafesPage.value = result.data
                // ★ 수정: 여기서 _favoriteCafeIds를 초기화하는 로직 삭제함.
                // 찜 목록은 updateFavoriteIds()를 통해 외부에서 주입받거나 사용자 액션으로 변경됨.
            }
            is ApiResult.Failure -> {
                _cafesPage.value = null
                _events.send(result.message ?: "카페 목록 조회 실패")
            }
        }
    }
}
