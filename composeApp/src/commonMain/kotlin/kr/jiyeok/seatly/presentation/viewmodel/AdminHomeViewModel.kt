package kr.jiyeok.seatly.presentation.viewmodel

import androidx.compose.ui.graphics.ImageBitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kr.jiyeok.seatly.data.remote.response.StudyCafeSummaryDto
import kr.jiyeok.seatly.data.remote.response.UsageDto
import kr.jiyeok.seatly.data.repository.ApiResult
import kr.jiyeok.seatly.domain.usecase.GetAdminCafesUseCase
import kr.jiyeok.seatly.domain.usecase.GetCafeUsageUseCase
import kr.jiyeok.seatly.domain.usecase.GetImageUseCase
import kr.jiyeok.seatly.util.toImageBitmap
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class AdminHomeViewModel(
    private val getAdminCafesUseCase: GetAdminCafesUseCase,
    private val getCafeUsageUseCase: GetCafeUsageUseCase,
    private val getImageUseCase: GetImageUseCase,
    private val ioDispatcher: CoroutineDispatcher
) : ViewModel() {

    private val _cafes = MutableStateFlow<List<StudyCafeSummaryDto>>(emptyList())
    val cafes: StateFlow<List<StudyCafeSummaryDto>> = _cafes.asStateFlow()

    private val _cafeUsages = MutableStateFlow<Map<Long, UsageDto>>(emptyMap())
    val cafeUsages: StateFlow<Map<Long, UsageDto>> = _cafeUsages.asStateFlow()

    private val _imageBitmapCache = MutableStateFlow<Map<String, ImageBitmap>>(emptyMap())
    val imageBitmapCache: StateFlow<Map<String, ImageBitmap>> = _imageBitmapCache.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _events = Channel<String>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    private val loadingImageIds = mutableSetOf<String>()
    private val imageLoadMutex = Mutex()

    fun loadRegisteredCafes() {
        viewModelScope.launch(ioDispatcher) {
            _isLoading.update { true }

            try {
                when (val result = getAdminCafesUseCase()) {
                    is ApiResult.Success -> {
                        val cafes = result.data ?: emptyList()
                        _cafes.update { cafes }
                        loadCafeRelatedData(cafes)
                    }
                    is ApiResult.Failure -> {
                        _cafes.update { emptyList() }
                        sendEvent(result.message ?: ERROR_LOAD_CAFES)
                    }
                    is ApiResult.Loading -> {}
                }
            } catch (e: Exception) {
                _cafes.update { emptyList() }
                sendEvent(ERROR_UNKNOWN)
            } finally {
                _isLoading.update { false }
            }
        }
    }

    private fun loadCafeRelatedData(cafes: List<StudyCafeSummaryDto>) {
        viewModelScope.launch(ioDispatcher) {
            cafes.forEach { cafe ->
                launch { loadCafeUsage(cafe.id) }
            }

            val imageIds = cafes.mapNotNull { it.mainImageUrl }
            imageIds.chunked(3).forEach { batch ->
                batch.map { imageId ->
                    async { loadImage(imageId) }
                }.awaitAll()
            }
        }
    }

    private suspend fun loadCafeUsage(cafeId: Long) {
        try {
            when (val result = getCafeUsageUseCase(cafeId)) {
                is ApiResult.Success -> {
                    result.data?.let { usage ->
                        _cafeUsages.update { it + (cafeId to usage) }
                    }
                }
                is ApiResult.Failure -> {}
                is ApiResult.Loading -> {}
            }
        } catch (e: Exception) {
        }
    }

    private suspend fun loadImage(imageId: String) {
        if (_imageBitmapCache.value.containsKey(imageId)) return

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

    private suspend fun sendEvent(message: String) {
        _events.send(message)
    }

    companion object {
        private const val ERROR_LOAD_CAFES = "카페 목록 조회 실패"
        private const val ERROR_UNKNOWN = "알 수 없는 오류"
    }
}
