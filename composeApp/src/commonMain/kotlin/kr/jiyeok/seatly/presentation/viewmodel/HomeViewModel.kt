package kr.jiyeok.seatly.presentation.viewmodel

import androidx.compose.ui.graphics.ImageBitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kr.jiyeok.seatly.data.remote.enums.EStatus
import kr.jiyeok.seatly.data.remote.response.*
import kr.jiyeok.seatly.data.repository.ApiResult
import kr.jiyeok.seatly.domain.usecase.*
import kr.jiyeok.seatly.util.toImageBitmap
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * HomeScreen UI 상태
 */
sealed interface HomeUiState {
    data object Idle : HomeUiState
    data object Loading : HomeUiState
    data class Success(val message: String = "") : HomeUiState
    data class Error(val message: String) : HomeUiState
}

class HomeViewModel(
    private val getUserInfoUseCase: GetUserInfoUseCase,
    private val getFavoriteCafesUseCase: GetFavoriteCafesUseCase,
    private val getCurrentSessions: GetCurrentSessions,
    private val getMyTimePassesUseCase: GetMyTimePassesUseCase,
    private val getImageUseCase: GetImageUseCase,
    private val getStudyCafesUseCase: GetStudyCafesUseCase,
    private val getAdminCafesUseCase: GetAdminCafesUseCase,
    private val addFavoriteCafeUseCase: AddFavoriteCafeUseCase,
    private val removeFavoriteCafeUseCase: RemoveFavoriteCafeUseCase,
    private val getSessionsUseCase: GetSessionsUseCase,
    private val endSessionUseCase: EndSessionUseCase
) : ViewModel() {

    private val _userData = MutableStateFlow<UserInfoSummaryDto?>(null)
    val userData: StateFlow<UserInfoSummaryDto?> = _userData.asStateFlow()

    private val _userState = MutableStateFlow<HomeUiState>(HomeUiState.Idle)
    val userState: StateFlow<HomeUiState> = _userState.asStateFlow()

    private val _userSessions = MutableStateFlow<List<SessionDto>?>(null)
    val userSessions: StateFlow<List<SessionDto>?> = _userSessions.asStateFlow()

    private val _userTimePasses = MutableStateFlow<List<UserTimePass>?>(null)
    val userTimePasses: StateFlow<List<UserTimePass>?> = _userTimePasses.asStateFlow()

    private val _cafes = MutableStateFlow<List<StudyCafeSummaryDto>>(emptyList())
    val cafes: StateFlow<List<StudyCafeSummaryDto>> = _cafes.asStateFlow()

    private val _adminCafes = MutableStateFlow<List<StudyCafeSummaryDto>>(emptyList())
    val adminCafes: StateFlow<List<StudyCafeSummaryDto>> = _adminCafes.asStateFlow()

    private val _favoriteCafeIds = MutableStateFlow<List<Long>>(emptyList())
    val favoriteCafeIds: StateFlow<List<Long>> = _favoriteCafeIds.asStateFlow()

    private val _currentSession = MutableStateFlow<SessionDto?>(null)
    val currentSession: StateFlow<SessionDto?> = _currentSession.asStateFlow()

    private val _imageBitmapCache = MutableStateFlow<Map<String, ImageBitmap>>(emptyMap())
    val imageBitmapCache: StateFlow<Map<String, ImageBitmap>> = _imageBitmapCache.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _events = Channel<String>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    private val loadingImageIds = mutableSetOf<String>()
    private val imageLoadMutex = Mutex()

    fun loadHomeData(studyCafeId: Long? = null) {
        viewModelScope.launch {
            _isLoading.value = true
            _userState.value = HomeUiState.Loading

            try {
                loadUserInfo()
                loadFavoriteCafes()
                loadCurrentSessionsInfo()
                loadMyTimePasses()
                loadCafes()
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
        viewModelScope.launch {
            try {
                when (val result = getUserInfoUseCase()) {
                    is ApiResult.Success -> {
                        _userData.value = result.data
                    }
                    is ApiResult.Failure -> {
                        _events.send(result.message ?: "사용자 정보 조회 실패")
                    }
                    is ApiResult.Loading -> {}
                }
            } catch (e: Exception) {
                _events.send(e.message ?: "알 수 없는 오류")
            }
        }
    }

    private fun loadFavoriteCafes() {
        viewModelScope.launch {
            try {
                when (val result = getFavoriteCafesUseCase()) {
                    is ApiResult.Success -> {
                        _favoriteCafeIds.value = result.data ?: emptyList()
                    }
                    is ApiResult.Failure -> {
                        _events.send(result.message ?: "즐겨찾기 카페 조회 실패")
                    }
                    is ApiResult.Loading -> {}
                }
            } catch (e: Exception) {
                _events.send(e.message ?: "알 수 없는 오류")
            }
        }
    }

    private fun loadCurrentSessionsInfo() {
        viewModelScope.launch {
            try {
                when (val result = getCurrentSessions()) {
                    is ApiResult.Success -> {
                        _userSessions.value = result.data
                    }
                    is ApiResult.Failure -> {
                        _events.send(result.message ?: "현재 세션 정보 조회 실패")
                    }
                    is ApiResult.Loading -> {}
                }
            } catch (e: Exception) {
                _events.send(e.message ?: "알 수 없는 오류")
            }
        }
    }

    private fun loadMyTimePasses() {
        viewModelScope.launch {
            try {
                when (val result = getMyTimePassesUseCase()) {
                    is ApiResult.Success -> {
                        _userTimePasses.value = result.data
                    }
                    is ApiResult.Failure -> {
                        _events.send(result.message ?: "현재 세션 정보 조회 실패")
                    }
                    is ApiResult.Loading -> {}
                }
            } catch (e: Exception) {
                _events.send(e.message ?: "알 수 없는 오류")
            }
        }
    }

    fun loadCafes() {
        viewModelScope.launch {
            try {
                when (val result = getStudyCafesUseCase()) {
                    is ApiResult.Success -> {
                        val cafeList = result.data ?: emptyList()
                        _cafes.value = cafeList
                        cafeList.forEach { cafe ->
                            cafe.mainImageUrl?.let { url -> loadImage(url) }
                        }
                    }
                    is ApiResult.Failure -> {
                        _cafes.value = emptyList()
                        _events.send(result.message ?: "카페 목록 조회 실패")
                    }
                    is ApiResult.Loading -> {}
                }
            } catch (e: Exception) {
                _cafes.value = emptyList()
                _events.send(e.message ?: "알 수 없는 오류")
            }
        }
    }

    fun loadAdminCafes() {
        viewModelScope.launch {
            try {
                when (val result = getAdminCafesUseCase()) {
                    is ApiResult.Success -> {
                        val cafeList = result.data ?: emptyList()
                        _adminCafes.value = cafeList
                        cafeList.forEach { cafe ->
                            cafe.mainImageUrl?.let { url -> loadImage(url) }
                        }
                    }
                    is ApiResult.Failure -> {
                        _adminCafes.value = emptyList()
                        _events.send(result.message ?: "카페 목록 조회 실패")
                    }
                    is ApiResult.Loading -> {}
                }
            } catch (e: Exception) {
                _adminCafes.value = emptyList()
                _events.send(e.message ?: "알 수 없는 오류")
            }
        }
    }

    fun loadCurrentSession(studyCafeId: Long) {
        viewModelScope.launch {
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
                    is ApiResult.Loading -> {}
                }
            } catch (e: Exception) {
                _currentSession.value = null
            }
        }
    }

    fun endCurrentSession() {
        viewModelScope.launch {
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
                    is ApiResult.Loading -> {}
                }
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun addFavoriteCafe(cafeId: Long) {
        viewModelScope.launch {
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
                    is ApiResult.Loading -> {}
                }
            } catch (e: Exception) {
                _events.send(e.message ?: "알 수 없는 오류")
            }
        }
    }

    fun removeFavoriteCafe(cafeId: Long) {
        viewModelScope.launch {
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
                    is ApiResult.Loading -> {}
                }
            } catch (e: Exception) {
                _events.send(e.message ?: "알 수 없는 오류")
            }
        }
    }

    private fun loadImage(imageId: String) {
        if (_imageBitmapCache.value.containsKey(imageId)) return

        viewModelScope.launch {
            imageLoadMutex.withLock {
                if (loadingImageIds.contains(imageId)) return@withLock
                loadingImageIds.add(imageId)
            }
            try {
                when (val result = getImageUseCase(imageId)) {
                    is ApiResult.Success -> {
                        result.data.let { imageData ->
                            val bitmap = imageData.toImageBitmap()
                            bitmap?.let {
                                _imageBitmapCache.update { cache -> cache + (imageId to it) }
                            }
                        }
                    }
                    is ApiResult.Failure -> {}
                    is ApiResult.Loading -> {}
                }
            } catch (e: Exception) {
            } finally {
                imageLoadMutex.withLock {
                    loadingImageIds.remove(imageId)
                }
            }
        }
    }
}
