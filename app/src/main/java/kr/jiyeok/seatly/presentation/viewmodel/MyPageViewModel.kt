package kr.jiyeok.seatly.presentation.viewmodel

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kr.jiyeok.seatly.data.remote.enums.ERole
import kr.jiyeok.seatly.data.remote.response.StudyCafeSummaryDto
import kr.jiyeok.seatly.data.remote.response.UserInfoSummaryDto
import kr.jiyeok.seatly.data.repository.ApiResult
import kr.jiyeok.seatly.di.IoDispatcher
import kr.jiyeok.seatly.domain.usecase.GetAdminCafesUseCase
import kr.jiyeok.seatly.domain.usecase.GetFavoriteCafesUseCase
import kr.jiyeok.seatly.domain.usecase.GetImageUseCase
import kr.jiyeok.seatly.domain.usecase.GetStudyCafesUseCase
import kr.jiyeok.seatly.domain.usecase.GetUserInfoUseCase
import kr.jiyeok.seatly.domain.usecase.LogoutUseCase
import javax.inject.Inject

/**
 * 마이페이지 UI 상태
 */
data class MyPageUiState(
    val isLoading: Boolean = true,
    val userInfo: UserInfoSummaryDto? = null,
    val userProfileImage: Bitmap? = null,
    val favoriteCafeIds: List<Long> = emptyList(),
    val favoriteCafes: List<StudyCafeSummaryDto> = emptyList(),
    val registeredCafes: List<StudyCafeSummaryDto> = emptyList(),
    val cafeImages: Map<Long, Bitmap> = emptyMap(),
    val error: String? = null
)

/**
 * 로그아웃 결과 상태
 */
sealed class LogoutState {
    object Idle : LogoutState()
    object Loading : LogoutState()
    object Success : LogoutState()
    data class Error(val message: String) : LogoutState()
}

@HiltViewModel
class MyPageViewModel @Inject constructor(
    private val getUserInfoUseCase: GetUserInfoUseCase,
    private val getImageUseCase: GetImageUseCase,
    private val logoutUseCase: LogoutUseCase,
    private val getFavoriteCafeUseCase: GetFavoriteCafesUseCase,
    private val getAdminCafesUseCase: GetAdminCafesUseCase,
    private val getStudyCafesUseCase: GetStudyCafesUseCase,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : ViewModel() {

    private val _uiState = MutableStateFlow(MyPageUiState())
    val uiState: StateFlow<MyPageUiState> = _uiState.asStateFlow()

    private val _logoutState = MutableStateFlow<LogoutState>(LogoutState.Idle)
    val logoutState: StateFlow<LogoutState> = _logoutState.asStateFlow()

    private val _events = Channel<String>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    private val loadingImageIds = mutableSetOf<String>()

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
                            }
                        }
                        is ApiResult.Failure -> {
                            _events.send(favoriteIdsResult.message ?: "즐겨찾기 카페 조회 실패")
                        }
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
                    }
                }
            }
        } catch (e: Exception) {
            _events.send(e.message ?: "알 수 없는 오류")
        }
    }

    private suspend fun loadProfileImage(imageId: String) {
        synchronized(loadingImageIds) {
            if (loadingImageIds.contains(imageId)) return
            loadingImageIds.add(imageId)
        }

        try {
            when (val result = getImageUseCase(imageId)) {
                is ApiResult.Success -> {
                    result.data?.let { imageData ->
                        val bitmap = withContext(Dispatchers.Default) {
                            decodeSampledBitmap(imageData, 200, 200)
                        }
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
            }
        } catch (e: Exception) {
            _events.send(e.message ?: "프로필 이미지 로드 실패")
        } finally {
            synchronized(loadingImageIds) {
                loadingImageIds.remove(imageId)
            }
        }
    }

    private suspend fun loadCafeImage(cafeId: Long, imageId: String) {
        synchronized(loadingImageIds) {
            if (loadingImageIds.contains(imageId)) return
            loadingImageIds.add(imageId)
        }

        try {
            when (val result = getImageUseCase(imageId)) {
                is ApiResult.Success -> {
                    result.data?.let { imageData ->
                        val bitmap = withContext(Dispatchers.Default) {
                            decodeSampledBitmap(imageData, 800, 800)
                        }
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
            }
        } catch (e: Exception) {
            _events.send(e.message ?: "카페 이미지 로드 실패")
        } finally {
            synchronized(loadingImageIds) {
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

    private fun decodeSampledBitmap(data: ByteArray, reqWidth: Int, reqHeight: Int): Bitmap? {
        return try {
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
                BitmapFactory.decodeByteArray(data, 0, data.size, this)
                inSampleSize = calculateInSampleSize(this, reqWidth, reqHeight)
                inJustDecodeBounds = false
            }
            BitmapFactory.decodeByteArray(data, 0, data.size, options)
        } catch (e: Exception) {
            null
        }
    }

    private fun calculateInSampleSize(
        options: BitmapFactory.Options,
        reqWidth: Int,
        reqHeight: Int
    ): Int {
        val (height: Int, width: Int) = options.run { outHeight to outWidth }
        var inSampleSize = 1

        if (height > reqHeight || width > reqWidth) {
            val halfHeight = height / 2
            val halfWidth = width / 2

            while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }
}
