package kr.jiyeok.seatly.presentation.viewmodel

import androidx.compose.ui.graphics.ImageBitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kr.jiyeok.seatly.data.remote.enums.ERole
import kr.jiyeok.seatly.data.remote.response.StudyCafeSummaryDto
import kr.jiyeok.seatly.data.remote.response.UserInfoSummaryDto
import kr.jiyeok.seatly.data.repository.ApiResult
import kr.jiyeok.seatly.domain.usecase.*
import kr.jiyeok.seatly.util.toImageBitmap
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * 마이페이지 UI 상태
 */
data class MyPageUiState(
    val isLoading: Boolean = true,
    val userInfo: UserInfoSummaryDto? = null,
    val userProfileImage: ImageBitmap? = null,
    val favoriteCafeIds: List<Long> = emptyList(),
    val favoriteCafes: List<StudyCafeSummaryDto> = emptyList(),
    val registeredCafes: List<StudyCafeSummaryDto> = emptyList(),
    val cafeImages: Map<Long, ImageBitmap> = emptyMap(),
    val error: String? = null
)

/**
 * 로그아웃 결과 상태
 */
sealed interface LogoutState {
    data object Idle : LogoutState
    data object Loading : LogoutState
    data object Success : LogoutState
    data class Error(val message: String) : LogoutState
}

class MyPageViewModel(
    private val getUserInfoUseCase: GetUserInfoUseCase,
    private val getImageUseCase: GetImageUseCase,
    private val logoutUseCase: LogoutUseCase,
    private val getFavoriteCafeUseCase: GetFavoriteCafesUseCase,
    private val getAdminCafesUseCase: GetAdminCafesUseCase,
    private val getStudyCafesUseCase: GetStudyCafesUseCase,
    private val ioDispatcher: CoroutineDispatcher
) : ViewModel() {

    private val _uiState = MutableStateFlow(MyPageUiState())
    val uiState: StateFlow<MyPageUiState> = _uiState.asStateFlow()

    private val _logoutState = MutableStateFlow<LogoutState>(LogoutState.Idle)
    val logoutState: StateFlow<LogoutState> = _logoutState.asStateFlow()

    private val _events = Channel<String>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    private val loadingImageIds = mutableSetOf<String>()
    private val imageLoadMutex = Mutex()

    fun loadData() {
        viewModelScope.launch(ioDispatcher) {
            _uiState.update { it.copy(isLoading = true, error = null) }

            try {
                when (val result = getUserInfoUseCase()) {
                    is ApiResult.Success -> {
                        val userData = result.data
                        if (userData != null) {
                            _uiState.update { it.copy(userInfo = userData) }

                            // 프로필 이미지 로드
                            userData.imageUrl?.let { imageId ->
                                if (imageId.isNotEmpty() && !imageId.startsWith("content://")) {
                                    loadProfileImage(imageId)
                                }
                            }

                            // 역할별 데이터 로드
                            loadRoleSpecificData(userData.role)
                        } else {
                            handleError("사용자 정보가 없습니다")
                        }
                    }
                    is ApiResult.Failure -> {
                        handleError(result.message ?: "사용자 정보 조회 실패")
                    }
                    is ApiResult.Loading -> {}
                }
            } catch (e: Exception) {
                handleError(e.message ?: "데이터 조회 실패")
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    private suspend fun loadRoleSpecificData(role: ERole) {
        try {
            when (role) {
                ERole.USER -> {
                    // 1. 즐겨찾기 ID 목록 가져오기
                    val favoriteIdsResult = getFavoriteCafeUseCase()

                    when (favoriteIdsResult) {
                        is ApiResult.Success -> {
                            val favoriteIds = favoriteIdsResult.data ?: emptyList()
                            _uiState.update { it.copy(favoriteCafeIds = favoriteIds) }

                            // 2. 전체 카페 목록 가져오기
                            val allCafesResult = getStudyCafesUseCase()

                            when (allCafesResult) {
                                is ApiResult.Success -> {
                                    val allCafes = allCafesResult.data ?: emptyList()

                                    // 3. 즐겨찾기 ID로 필터링
                                    val favoriteCafes = allCafes.filter { cafe ->
                                        cafe.id in favoriteIds
                                    }

                                    _uiState.update { it.copy(favoriteCafes = favoriteCafes) }

                                    // 4. 즐겨찾기 카페 이미지 로드
                                    favoriteCafes.forEach { cafe ->
                                        cafe.mainImageUrl?.let { imageId ->
                                            loadCafeImage(cafe.id, imageId)
                                        }
                                    }
                                }
                                is ApiResult.Failure -> {
                                    _events.send(allCafesResult.message ?: "카페 목록 조회 실패")
                                }
                                is ApiResult.Loading -> {}
                            }
                        }
                        is ApiResult.Failure -> {
                            _events.send(favoriteIdsResult.message ?: "즐겨찾기 카페 조회 실패")
                        }
                        is ApiResult.Loading -> {}
                    }
                }
                ERole.ADMIN -> {
                    when (val result = getAdminCafesUseCase()) {
                        is ApiResult.Success -> {
                            result.data?.let { cafes ->
                                _uiState.update { it.copy(registeredCafes = cafes) }

                                // 모든 카페 이미지 로드
                                cafes.forEach { cafe ->
                                    cafe.mainImageUrl?.let { imageId ->
                                        loadCafeImage(cafe.id, imageId)
                                    }
                                }
                            }
                        }
                        is ApiResult.Failure -> {
                            _events.send(result.message ?: "등록 카페 목록 조회 실패")
                        }
                        is ApiResult.Loading -> {}
                    }
                }
            }
        } catch (e: Exception) {
            _events.send(e.message ?: "알 수 없는 오류")
        }
    }

    private suspend fun loadProfileImage(imageId: String) {
        imageLoadMutex.withLock {
            if (loadingImageIds.contains(imageId)) return
            loadingImageIds.add(imageId)
        }

        try {
            when (val result = getImageUseCase(imageId)) {
                is ApiResult.Success -> {
                    result.data.let { imageData ->
                        val bitmap = imageData.toImageBitmap()
                        bitmap?.let {
                            _uiState.update { state ->
                                state.copy(userProfileImage = it)
                            }
                        }
                    }
                }
                is ApiResult.Failure -> {
                    _events.send(result.message ?: "프로필 이미지 로드 실패")
                }
                is ApiResult.Loading -> {}
            }
        } catch (e: Exception) {
            _events.send(e.message ?: "프로필 이미지 로드 실패")
        } finally {
            imageLoadMutex.withLock {
                loadingImageIds.remove(imageId)
            }
        }
    }

    private suspend fun loadCafeImage(cafeId: Long, imageId: String) {
        imageLoadMutex.withLock {
            if (loadingImageIds.contains(imageId)) return
            loadingImageIds.add(imageId)
        }

        try {
            when (val result = getImageUseCase(imageId)) {
                is ApiResult.Success -> {
                    result.data.let { imageData ->
                        val bitmap = imageData.toImageBitmap()
                        bitmap?.let {
                            _uiState.update { state ->
                                state.copy(
                                    cafeImages = state.cafeImages + (cafeId to it)
                                )
                            }
                        }
                    }
                }
                is ApiResult.Failure -> {
                    _events.send(result.message ?: "카페 이미지 로드 실패")
                }
                is ApiResult.Loading -> {}
            }
        } catch (e: Exception) {
            _events.send(e.message ?: "카페 이미지 로드 실패")
        } finally {
            imageLoadMutex.withLock {
                loadingImageIds.remove(imageId)
            }
        }
    }

    fun logout() {
        viewModelScope.launch(ioDispatcher) {
            _logoutState.value = LogoutState.Loading

            try {
                when (val result = logoutUseCase()) {
                    is ApiResult.Success -> {
                        _logoutState.value = LogoutState.Success
                    }
                    is ApiResult.Failure -> {
                        val errorMsg = result.message ?: "로그아웃 실패"
                        _logoutState.value = LogoutState.Error(errorMsg)
                        _events.send(errorMsg)
                    }
                    is ApiResult.Loading -> {}
                }
            } catch (e: Exception) {
                val errorMsg = e.message ?: "알 수 없는 오류"
                _logoutState.value = LogoutState.Error(errorMsg)
                _events.send(errorMsg)
            }
        }
    }

    fun resetLogoutState() {
        _logoutState.value = LogoutState.Idle
    }

    private suspend fun handleError(message: String) {
        _uiState.update { it.copy(error = message, isLoading = false) }
        _events.send(message)
    }
}
