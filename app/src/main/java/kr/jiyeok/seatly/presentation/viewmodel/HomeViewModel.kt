package kr.jiyeok.seatly.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kr.jiyeok.seatly.data.remote.response.*
import kr.jiyeok.seatly.data.repository.ApiResult
import kr.jiyeok.seatly.domain.usecase.*
import kr.jiyeok.seatly.di.IoDispatcher
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject

/**
 * Home Screen ViewModel
 * 
 * 역할:
 * - 현재 사용 중인 카페 세션 정보 관리
 * - 즐겨찾기 카페 목록 로드
 * - 세션 종료 처리
 * 
 * UI는 StateFlow를 통해 데이터를 관찰하고,
 * 에러는 [events] Channel을 통해 수신합니다
 */
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

    /**
     * 현재 활성 세션 정보
     * 사용자가 현재 이용 중인 카페/좌석 정보
     */
    private val _currentSession = MutableStateFlow<SessionDto?>(null)
    val currentSession: StateFlow<SessionDto?> = _currentSession.asStateFlow()

    /**
     * 카페 목록 (홈 화면용 최소 페이지)
     */
    private val _cafesPage = MutableStateFlow<PageResponse<StudyCafeSummaryDto>?>(null)
    val cafesPage: StateFlow<PageResponse<StudyCafeSummaryDto>?> = _cafesPage.asStateFlow()

    /**
     * 즐겨찾기 카페 ID 목록
     */
    private val _favoriteCafeIds = MutableStateFlow<List<Long>>(emptyList())
    val favoriteCafeIds: StateFlow<List<Long>> = _favoriteCafeIds.asStateFlow()

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
    // Public Methods
    // =====================================================

    /**
     * 홈 화면 초기 데이터 로드
     * 
     * 수행:
     * 1. 현재 활성 세션 조회
     * 2. 카페 목록 조회
     */
    fun loadHomeData() {
        viewModelScope.launch(ioDispatcher) {
            _isLoading.value = true
            try {
                // 1. 현재 세션 조회
                loadCurrentSession()

                // 2. 카페 목록 조회 (홈 화면용)
                loadCafesForHome()

            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * 특정 카페를 즐겨찾기에 추가
     */
    fun addFavoriteCafe(cafeId: Long) {
        viewModelScope.launch(ioDispatcher) {
            when (val result = addFavoriteCafeUseCase(cafeId)) {
                is ApiResult.Success -> {
                    // 즐겨찾기 ID 리스트 업데이트
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

    /**
     * 특정 카페를 즐겨찾기에서 제거
     */
    fun removeFavoriteCafe(cafeId: Long) {
        viewModelScope.launch(ioDispatcher) {
            when (val result = removeFavoriteCafeUseCase(cafeId)) {
                is ApiResult.Success -> {
                    // 즐겨찾기 ID 리스트 업데이트
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

    /**
     * 현재 세션 종료
     * 
     * 수행:
     * 1. 활성 세션 조회
     * 2. 세션 종료 API 호출
     * 3. 현재 세션 정보 초기화
     */
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

    /**
     * 현재 세션 새로고침
     * 유저가 수동으로 새로고침할 때 호출
     */
    fun refreshCurrentSession() {
        viewModelScope.launch(ioDispatcher) {
            loadCurrentSession()
        }
    }

    // =====================================================
    // Private Helper Methods
    // =====================================================

    /**
     * 현재 활성 세션 조회
     * 
     * IN_USE 또는 ASSIGNED 상태의 세션을 찾아 _currentSession에 저장
     */
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
                _events.send(result.message ?: "세션 조회 실패")
            }
        }
    }

    /**
     * 홈 화면용 카페 목록 로드
     * 
     * 처음 페이지만 로드 (0, 20)
     */
    private suspend fun loadCafesForHome() {
        when (val result = getStudyCafesUseCase(page = 0, size = 20)) {
            is ApiResult.Success -> {
                _cafesPage.value = result.data

                // 현재 로그인한 사용자의 즐겨찾기 카페 ID 추출
                // (실제로는 서버에서 유저 정보 조회 시 받아오거나
                //  별도의 엔드포인트에서 조회)
                val favoriteIds = result.data?.content
                    ?.filter { it.isFavorite }
                    ?.map { it.id }
                    ?: emptyList()
                _favoriteCafeIds.value = favoriteIds
            }
            is ApiResult.Failure -> {
                _cafesPage.value = null
                _events.send(result.message ?: "카페 목록 조회 실패")
            }
        }
    }
}

/**
 * Extension: StudyCafeSummaryDto에 isFavorite 속성 추가
 * 실제 구현 시 서버에서 받아오거나 로컬 데이터베이스에서 관리
 */
val StudyCafeSummaryDto.isFavorite: Boolean
    get() = false  // TODO: 실제 구현 시 UserRepository에서 관리
