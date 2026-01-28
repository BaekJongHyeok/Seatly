package kr.jiyeok.seatly.presentation.viewmodel

import androidx.compose.ui.graphics.ImageBitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
import kr.jiyeok.seatly.domain.usecase.AddFavoriteCafeUseCase
import kr.jiyeok.seatly.domain.usecase.GetFavoriteCafesUseCase
import kr.jiyeok.seatly.domain.usecase.GetImageUseCase
import kr.jiyeok.seatly.domain.usecase.GetStudyCafesUseCase
import kr.jiyeok.seatly.domain.usecase.RemoveFavoriteCafeUseCase
import kr.jiyeok.seatly.util.toImageBitmap
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Search Screen ViewModel
 */
class SearchViewModel(
    private val getStudyCafesUseCase: GetStudyCafesUseCase,
    private val addFavoriteCafeUseCase: AddFavoriteCafeUseCase,
    private val removeFavoriteCafeUseCase: RemoveFavoriteCafeUseCase,
    private val getFavoriteCafesUseCase: GetFavoriteCafesUseCase,
    private val getImageUseCase: GetImageUseCase
) : ViewModel() {

    private val _allCafes = MutableStateFlow<List<StudyCafeSummaryDto>>(emptyList())

    private val _filteredCafes = MutableStateFlow<List<StudyCafeSummaryDto>>(emptyList())
    val filteredCafes: StateFlow<List<StudyCafeSummaryDto>> = _filteredCafes.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _favoriteCafeIds = MutableStateFlow<List<Long>>(emptyList())
    val favoriteCafeIds: StateFlow<List<Long>> = _favoriteCafeIds.asStateFlow()

    private val _cafeImages = MutableStateFlow<Map<Long, ImageBitmap>>(emptyMap())
    val cafeImages: StateFlow<Map<Long, ImageBitmap>> = _cafeImages.asStateFlow()

    private val _events = Channel<String>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    private val loadingImageIds = mutableSetOf<String>()
    private val imageLoadMutex = Mutex()

    init {
        loadCafes()
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
        filterCafes(query)
    }

    fun loadCafes() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            try {
                when (val cafesResult = getStudyCafesUseCase()) {
                    is ApiResult.Success -> {
                        val cafeList = cafesResult.data ?: emptyList()
                        _allCafes.value = cafeList
                        filterCafes(_searchQuery.value)

                        when (val favoriteIdsResult = getFavoriteCafesUseCase()) {
                            is ApiResult.Success -> {
                                _favoriteCafeIds.value = favoriteIdsResult.data ?: emptyList()
                            }
                            is ApiResult.Failure -> {
                                _events.send(favoriteIdsResult.message ?: "즐겨찾기 목록 조회 실패")
                            }
                            is ApiResult.Loading -> {}
                        }

                        loadCafeImages(cafeList)
                    }
                    is ApiResult.Failure -> {
                        val msg = cafesResult.message ?: "카페 목록을 불러올 수 없습니다"
                        _error.value = msg
                        _events.send(msg)
                    }
                    is ApiResult.Loading -> {}
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

    fun toggleFavoriteCafe(cafeId: Long) {
        val currentFavorites = _favoriteCafeIds.value.toMutableList()
        val isCurrentlyFavorite = currentFavorites.contains(cafeId)

        if (isCurrentlyFavorite) {
            currentFavorites.remove(cafeId)
            _events.trySend("즐겨찾기에서 제거되었습니다")
        } else {
            currentFavorites.add(cafeId)
            _events.trySend("즐겨찾기에 추가되었습니다")
        }
        _favoriteCafeIds.value = currentFavorites

        viewModelScope.launch {
            val result = if (isCurrentlyFavorite) {
                removeFavoriteCafeUseCase(cafeId)
            } else {
                addFavoriteCafeUseCase(cafeId)
            }

            if (result is ApiResult.Failure) {
                _events.send(result.message ?: "즐겨찾기 처리 실패")
                rollbackFavoriteState(cafeId, !isCurrentlyFavorite)
            }
        }
    }

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

    private suspend fun loadCafeImages(cafes: List<StudyCafeSummaryDto>) {
        cafes.forEach { cafe ->
            cafe.mainImageUrl?.let { imageId ->
                if (imageId.isNotEmpty()) {
                    loadCafeImage(cafe.id, imageId)
                }
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
                            _cafeImages.update { state ->
                                state + (cafeId to it)
                            }
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
}
