package kr.jiyeok.seatly.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kr.jiyeok.seatly.data.remote.enums.EStatus
import kr.jiyeok.seatly.data.remote.request.UpdateUserInfoRequest
import kr.jiyeok.seatly.data.remote.response.*
import kr.jiyeok.seatly.data.repository.ApiResult
import kr.jiyeok.seatly.di.IoDispatcher
import kr.jiyeok.seatly.domain.usecase.*
import javax.inject.Inject

/**
 * HomeScreen Fat ViewModel (하이브리드 방식)
 *
 * 역할:
 * - 사용자 정보 관리 (UserViewModel 기능 통합)
 * - 카페 목록 관리
 * - 세션 관리
 * - 즐겨찾기 관리
 *
 * 💡 특징:
 * - Repository 캐싱으로 중복 API 요청 방지
 * - 각 화면에 필요한 모든 기능을 포함
 * - UI는 이 ViewModel만 사용
 */

sealed interface HomeUiState {
    object Idle : HomeUiState
    object Loading : HomeUiState
    data class Success(val message: String = "") : HomeUiState
    data class Error(val message: String) : HomeUiState
}

@HiltViewModel
class HomeViewModel @Inject constructor(
    // User 관련 UseCase
    private val getUserInfoUseCase: GetUserInfoUseCase,
    private val getFavoriteCafesUseCase: GetFavoriteCafesUseCase,
    private val getCurrentSessions: GetCurrentSessions,
    private val getMyTimePassesUseCase: GetMyTimePassesUseCase,
    private val updateUserInfoUseCase: UpdateUserInfoUseCase,

    // Cafe 관련 UseCase
    private val getStudyCafesUseCase: GetStudyCafesUseCase,
    private val getAdminCafesUseCase: GetAdminCafesUseCase,
    private val addFavoriteCafeUseCase: AddFavoriteCafeUseCase,
    private val removeFavoriteCafeUseCase: RemoveFavoriteCafeUseCase,

    // Session 관련 UseCase
    private val getSessionsUseCase: GetSessionsUseCase,
    private val endSessionUseCase: EndSessionUseCase,

    // 로그아웃
    private val logoutUseCase: LogoutUseCase,

    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : ViewModel() {

    // =====================================================
    // State Management - User
    // =====================================================

    private val _userData = MutableStateFlow<UserInfoSummaryDto?>(null)
    val userData: StateFlow<UserInfoSummaryDto?> = _userData.asStateFlow()

    private val _userState = MutableStateFlow<HomeUiState>(HomeUiState.Idle)
    val userState: StateFlow<HomeUiState> = _userState.asStateFlow()

    private val _userSessions = MutableStateFlow<List<SessionDto>?>(null)
    val userSessions: StateFlow<List<SessionDto>?> = _userSessions.asStateFlow()

    private val _userTimePasses = MutableStateFlow<List<UserTimePass>?>(null)
    val userTimePasses: StateFlow<List<UserTimePass>?> = _userTimePasses.asStateFlow()

    // =====================================================
    // State Management - Cafes
    // =====================================================

    private val _cafes = MutableStateFlow<List<StudyCafeSummaryDto>>(emptyList())
    val cafes: StateFlow<List<StudyCafeSummaryDto>> = _cafes.asStateFlow()

    private val _adminCafes = MutableStateFlow<List<StudyCafeSummaryDto>>(emptyList())
    val adminCafes: StateFlow<List<StudyCafeSummaryDto>> = _adminCafes.asStateFlow()

    private val _favoriteCafeIds = MutableStateFlow<List<Long>>(emptyList())
    val favoriteCafeIds: StateFlow<List<Long>> = _favoriteCafeIds.asStateFlow()

    // =====================================================
    // State Management - Sessions
    // =====================================================

    private val _currentSession = MutableStateFlow<SessionDto?>(null)
    val currentSession: StateFlow<SessionDto?> = _currentSession.asStateFlow()

    // =====================================================
    // State Management - General
    // =====================================================

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _events = Channel<String>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    // =====================================================
    // Public Methods - 초기 로드
    // =====================================================

    fun loadHomeData(studyCafeId: Long? = null) {
        viewModelScope.launch(ioDispatcher) {
            _isLoading.value = true
            _userState.value = HomeUiState.Loading
            _error.value = null

            try {
                // 유저 기본 정보 조회
                loadUserInfo()

                // 유저 즐겨찾기 카페 정보 조회
                loadFavoriteCafes()

                // 유저 현재 세션 정보 조회
                loadCurrentSessionsInfo()

                // 유저 현재 시간권 정보 조회
                loadMyTimePasses()

                // 전체 카페 목록 조회
                loadCafes()

                // 관리자 카페 정보 조회
                loadAdminCafes()
                if (studyCafeId != null) {
                    loadCurrentSession(studyCafeId)
                }
                _userState.value = HomeUiState.Success("홈 데이터 로드 완료")
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun loadUserInfo() {
        viewModelScope.launch(ioDispatcher) {
            try {
                when (val result = getUserInfoUseCase()) {
                    is ApiResult.Success -> {
                        val userInfo = result.data
                        if (userInfo != null) {
                            _userData.value = userInfo
                        }
                    }
                    is ApiResult.Failure -> {
                        _error.value = result.message ?: "사용자 정보 조회 실패"
                        _events.send(result.message ?: "사용자 정보 조회 실패")
                    }
                }
            } catch (e: Exception) {
                _error.value = e.message ?: "알 수 없는 오류"
                _events.send(e.message ?: "알 수 없는 오류")
            }
        }
    }

    private fun loadFavoriteCafes() {
        viewModelScope.launch(ioDispatcher) {
            try {
                when (val result = getFavoriteCafesUseCase()) {
                    is ApiResult.Success -> {
                        val cafes = result.data
                        if (cafes != null) {
                            _favoriteCafeIds.value = cafes
                        }
                    }
                    is ApiResult.Failure -> {
                        _error.value = result.message ?: "즐겨찾기 카페 조회 실패"
                        _events.send(result.message ?: "즐겨찾기 카페 조회 실패")
                    }
                }
            } catch (e: Exception) {
                _error.value = e.message ?: "알 수 없는 오류"
                _events.send(e.message ?: "알 수 없는 오류")
            }
        }
    }

    private fun loadCurrentSessionsInfo() {
        viewModelScope.launch(ioDispatcher) {
            try {
                when (val result = getCurrentSessions()) {
                    is ApiResult.Success -> {
                        val session = result.data
                        if (session != null) {
                            _userSessions.value = session
                        }
                    }
                    is ApiResult.Failure -> {
                        _error.value = result.message ?: "현재 세션 정보 조회 실패"
                        _events.send(result.message ?: "현재 세션 정보 조회 실패")
                    }
                }
            } catch (e: Exception) {
                _error.value = e.message ?: "알 수 없는 오류"
                _events.send(e.message ?: "알 수 없는 오류")
            }
        }
    }

    private fun loadMyTimePasses() {
        viewModelScope.launch(ioDispatcher) {
            try {
                when (val result = getMyTimePassesUseCase()) {
                    is ApiResult.Success -> {
                        val timePasses = result.data
                        if (timePasses != null) {
                            _userTimePasses.value = timePasses
                        }
                    }
                    is ApiResult.Failure -> {
                        _error.value = result.message ?: "현재 세션 정보 조회 실패"
                        _events.send(result.message ?: "현재 세션 정보 조회 실패")
                    }
                }
            } catch (e: Exception) {
                _error.value = e.message ?: "알 수 없는 오류"
                _events.send(e.message ?: "알 수 없는 오류")
            }
        }
    }

    fun loadCafes() {
        viewModelScope.launch(ioDispatcher) {
            try {
                when (val result = getStudyCafesUseCase()) {
                    is ApiResult.Success -> {
                        _cafes.value = result.data ?: emptyList()
                    }
                    is ApiResult.Failure -> {
                        _cafes.value = emptyList()
                        _events.send(result.message ?: "카페 목록 조회 실패")
                    }
                }
            } catch (e: Exception) {
                _cafes.value = emptyList()
                _events.send(e.message ?: "알 수 없는 오류")
            }
        }
    }

    fun loadAdminCafes() {
        viewModelScope.launch(ioDispatcher) {
            try {
                when (val result = getAdminCafesUseCase()) {
                    is ApiResult.Success -> {
                        _adminCafes.value = result.data ?: emptyList()
                    }
                    is ApiResult.Failure -> {
                        _adminCafes.value = emptyList()
                        _events.send(result.message ?: "카페 목록 조회 실패")
                    }
                }
            } catch (e: Exception) {
                _adminCafes.value = emptyList()
                _events.send(e.message ?: "알 수 없는 오류")
            }
        }
    }

    // =====================================================
    // Public Methods - 세션 관리
    // =====================================================

    fun loadCurrentSession(studyCafeId: Long) {
        viewModelScope.launch(ioDispatcher) {
            try {
                when (val result = getSessionsUseCase(studyCafeId)) {
                    is ApiResult.Success -> {
                        val sessions = result.data ?: emptyList()
                        val activeSession = sessions.firstOrNull { session ->
                            session.status == EStatus.IN_USE ||
                                    session.status == EStatus.ASSIGNED
                        }
                        _currentSession.value = activeSession
                    }
                    is ApiResult.Failure -> {
                        _currentSession.value = null
                    }
                }
            } catch (e: Exception) {
                _currentSession.value = null
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

    fun refreshCurrentSession(studyCafeId: Long) {
        viewModelScope.launch(ioDispatcher) {
            loadCurrentSession(studyCafeId)
        }
    }

    // =====================================================
    // Public Methods - 즐겨찾기
    // =====================================================

    fun addFavoriteCafe(cafeId: Long) {
        viewModelScope.launch(ioDispatcher) {
            try {
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
            } catch (e: Exception) {
                _events.send(e.message ?: "알 수 없는 오류")
            }
        }
    }

    fun removeFavoriteCafe(cafeId: Long) {
        viewModelScope.launch(ioDispatcher) {
            try {
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
            } catch (e: Exception) {
                _events.send(e.message ?: "알 수 없는 오류")
            }
        }
    }

    // =====================================================
    // Public Methods - Logout
    // =====================================================
    fun logout() {
        viewModelScope.launch(ioDispatcher) {
            try {
                when (val result = logoutUseCase()) {
                    is ApiResult.Success -> {
                        _events.send("로그아웃 되었습니다")
                    }
                    is ApiResult.Failure -> {
                        _events.send(result.message ?: "즐겨찾기 제거 실패")
                    }
                }
            } catch (e: Exception) {
                _events.send(e.message ?: "알 수 없는 오류")
            }
        }
    }

    // =====================================================
    // Public Methods - 유틸리티
    // =====================================================

    fun clearError() {
        _error.value = null
    }

    fun resetState() {
        _userState.value = HomeUiState.Idle
        _error.value = null
    }
}
