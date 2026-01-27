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
import kr.jiyeok.seatly.data.remote.response.StudyCafeSummaryDto
import kr.jiyeok.seatly.data.repository.ApiResult
import kr.jiyeok.seatly.di.IoDispatcher
import kr.jiyeok.seatly.domain.usecase.AddFavoriteCafeUseCase
import kr.jiyeok.seatly.domain.usecase.GetFavoriteCafesUseCase
import kr.jiyeok.seatly.domain.usecase.GetImageUseCase
import kr.jiyeok.seatly.domain.usecase.GetStudyCafesUseCase
import kr.jiyeok.seatly.domain.usecase.RemoveFavoriteCafeUseCase
import javax.inject.Inject

/**
 * Search Screen ViewModel
 * - 스터디카페 검색 및 필터링
 * - 즐겨찾기 관리
 * - 카페 이미지 로드
 */
@HiltViewModel
class SearchViewModel @Inject constructor(
    private val getStudyCafesUseCase: GetStudyCafesUseCase,
    private val addFavoriteCafeUseCase: AddFavoriteCafeUseCase,
    private val removeFavoriteCafeUseCase: RemoveFavoriteCafeUseCase,
    private val getFavoriteCafesUseCase: GetFavoriteCafesUseCase,
    private val getImageUseCase: GetImageUseCase,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : ViewModel() {

    // ===================================================================
    // State Management
    // ===================================================================

    // 전체 카페 목록 (필터링 전)
    private val _allCafes = MutableStateFlow<List<StudyCafeSummaryDto>>(emptyList())

    // UI에 표시될 필터링된 카페 목록
    private val _filteredCafes = MutableStateFlow<List<StudyCafeSummaryDto>>(emptyList())
    val filteredCafes: StateFlow<List<StudyCafeSummaryDto>> = _filteredCafes.asStateFlow()

    // 검색어
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    // 로딩 상태
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // 에러 메시지
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    // 즐겨찾기 카페 ID 목록
    private val _favoriteCafeIds = MutableStateFlow<List<Long>>(emptyList())
    val favoriteCafeIds: StateFlow<List<Long>> = _favoriteCafeIds.asStateFlow()

    // 카페 이미지 캐시 (cafeId -> Bitmap)
    private val _cafeImages = MutableStateFlow<Map<Long, Bitmap>>(emptyMap())
    val cafeImages: StateFlow<Map<Long, Bitmap>> = _cafeImages.asStateFlow()

    // One-shot Event (Toast 메시지 등)
    private val _events = Channel<String>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    // 이미지 로딩 중복 방지
    private val loadingImageIds = mutableSetOf<String>()

    // ===================================================================
    // Initialization
    // ===================================================================

    init {
        loadCafes()
    }

    // ===================================================================
    // Public Methods
    // ===================================================================

    /**
     * 검색어 업데이트 및 필터링
     */
    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
        filterCafes(query)
    }

    /**
     * 카페 목록 로드
     */
    fun loadCafes() {
        viewModelScope.launch(ioDispatcher) {
            _isLoading.value = true
            _error.value = null

            try {
                // 1. 전체 카페 목록 가져오기
                val cafesResult = getStudyCafesUseCase()
                when (cafesResult) {
                    is ApiResult.Success -> {
                        val cafeList = cafesResult.data ?: emptyList()
                        _allCafes.value = cafeList
                        filterCafes(_searchQuery.value)

                        // 2. 즐겨찾기 ID 목록 가져오기
                        val favoriteIdsResult = getFavoriteCafesUseCase()
                        when (favoriteIdsResult) {
                            is ApiResult.Success -> {
                                val favoriteIds = favoriteIdsResult.data ?: emptyList()
                                _favoriteCafeIds.value = favoriteIds
                            }
                            is ApiResult.Failure -> {
                                _events.send(favoriteIdsResult.message ?: "즐겨찾기 목록 조회 실패")
                            }
                        }

                        // 3. 카페 이미지 로드
                        loadCafeImages(cafeList)
                    }
                    is ApiResult.Failure -> {
                        val msg = cafesResult.message ?: "카페 목록을 불러올 수 없습니다"
                        _error.value = msg
                        _events.send(msg)
                    }
                }
            } catch (e: Exception) {
                val msg = e.message ?: "데이터 조회 실패"
                _error.value = msg
                _events.send(msg)
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * 즐겨찾기 토글
     * - Optimistic Update: UI 먼저 업데이트 후 API 호출
     */
    fun toggleFavoriteCafe(cafeId: Long) {
        val currentFavorites = _favoriteCafeIds.value.toMutableList()
        val isCurrentlyFavorite = currentFavorites.contains(cafeId)

        // 1. UI 먼저 업데이트 (낙관적 업데이트)
        if (isCurrentlyFavorite) {
            currentFavorites.remove(cafeId)
            _events.trySend("즐겨찾기에서 제거되었습니다")
        } else {
            currentFavorites.add(cafeId)
            _events.trySend("즐겨찾기에 추가되었습니다")
        }
        _favoriteCafeIds.value = currentFavorites

        // 2. API 호출
        viewModelScope.launch(ioDispatcher) {
            val result = if (isCurrentlyFavorite) {
                removeFavoriteCafeUseCase(cafeId)
            } else {
                addFavoriteCafeUseCase(cafeId)
            }

            // API 실패 시 - UI 롤백
            if (result is ApiResult.Failure) {
                _events.send(result.message ?: "즐겨찾기 처리 실패")
                rollbackFavoriteState(cafeId, !isCurrentlyFavorite)
            }
        }
    }

    // ===================================================================
    // Private Helper Methods
    // ===================================================================

    /**
     * 카페 목록 필터링 (검색어 기반)
     */
    private fun filterCafes(query: String) {
        if (query.isBlank()) {
            _filteredCafes.value = _allCafes.value
        } else {
            val lowerQuery = query.lowercase()
            _filteredCafes.value = _allCafes.value.filter { cafe ->
                val name = cafe.name?.lowercase() ?: ""
                val address = cafe.address?.lowercase() ?: ""
                name.contains(lowerQuery) || address.contains(lowerQuery)
            }
        }
    }

    /**
     * 카페 이미지 로드 (imageId -> ByteArray -> Bitmap)
     * - mainImageUrl은 실제로 imageId(문자열)
     * - GetImageUseCase를 사용하여 ByteArray로 가져온 후 Bitmap으로 변환
     */
    private suspend fun loadCafeImages(cafes: List<StudyCafeSummaryDto>) {
        cafes.forEach { cafe ->
            cafe.mainImageUrl?.let { imageId ->
                if (imageId.isNotEmpty()) {
                    loadCafeImage(cafe.id, imageId)
                }
            }
        }
    }

    /**
     * 개별 카페 이미지 로드
     */
    private suspend fun loadCafeImage(cafeId: Long, imageId: String) {
        // 중복 로딩 방지
        synchronized(loadingImageIds) {
            if (loadingImageIds.contains(imageId)) return
            loadingImageIds.add(imageId)
        }

        try {
            when (val result = getImageUseCase(imageId)) {
                is ApiResult.Success -> {
                    result.data?.let { imageData ->
                        // ByteArray -> Bitmap 변환 (Background Thread)
                        val bitmap = withContext(Dispatchers.Default) {
                            decodeSampledBitmap(imageData, 400, 400)
                        }

                        bitmap?.let {
                            _cafeImages.update { state ->
                                state + (cafeId to it)
                            }
                        }
                    }
                }
                is ApiResult.Failure -> {
                    // 이미지 로드 실패는 무시 (기본 이미지 표시)
                }
            }
        } catch (e: Exception) {
            // 이미지 로드 실패는 무시
        } finally {
            synchronized(loadingImageIds) {
                loadingImageIds.remove(imageId)
            }
        }
    }

    /**
     * 즐겨찾기 상태 롤백 (API 실패 시)
     */
    private fun rollbackFavoriteState(cafeId: Long, shouldBeInFavorites: Boolean) {
        val currentFavorites = _favoriteCafeIds.value.toMutableList()
        if (shouldBeInFavorites) {
            if (!currentFavorites.contains(cafeId)) {
                currentFavorites.add(cafeId)
            }
        } else {
            currentFavorites.remove(cafeId)
        }
        _favoriteCafeIds.value = currentFavorites
    }

    /**
     * ByteArray를 샘플링하여 Bitmap으로 디코딩 (메모리 효율적)
     */
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

    /**
     * 적절한 샘플링 비율 계산
     */
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
