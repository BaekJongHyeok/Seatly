package kr.jiyeok.seatly.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kr.jiyeok.seatly.data.remote.request.CreateCafeRequest
import kr.jiyeok.seatly.data.remote.request.UpdateCafeRequest
import kr.jiyeok.seatly.data.remote.response.StudyCafeDetailDto
import kr.jiyeok.seatly.data.repository.ApiResult
import kr.jiyeok.seatly.data.repository.SeatlyRepository
import kr.jiyeok.seatly.domain.usecase.CreateCafeUseCase
import kr.jiyeok.seatly.domain.usecase.GetCafeDetailUseCase
import kr.jiyeok.seatly.domain.usecase.UpdateCafeUseCase

class CafeFormViewModel(
    private val repository: SeatlyRepository,
    private val createCafeUseCase: CreateCafeUseCase,
    private val updateCafeUseCase: UpdateCafeUseCase,
    private val getCafeDetailUseCase: GetCafeDetailUseCase,
    private val ioDispatcher: CoroutineDispatcher
) : ViewModel() {

    private val _cafeInfo = MutableStateFlow<StudyCafeDetailDto?>(null)
    val cafeInfo: StateFlow<StudyCafeDetailDto?> = _cafeInfo.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _events = Channel<String>(Channel.Factory.BUFFERED)
    val events = _events.receiveAsFlow()

    private val _createCafeSuccess = MutableStateFlow(false)
    val createCafeSuccess: StateFlow<Boolean> = _createCafeSuccess

    private val _updateCafeSuccess = MutableStateFlow(false)
    val updateCafeSuccess: StateFlow<Boolean> = _updateCafeSuccess

    private val _serverImageUrls = MutableStateFlow<List<String>>(emptyList())
    val serverImageUrls: StateFlow<List<String>> = _serverImageUrls.asStateFlow()

    private val _uploadedImageUrls = MutableStateFlow<List<String>>(emptyList())
    val uploadedImageUrls: StateFlow<List<String>> = _uploadedImageUrls.asStateFlow()

    private val _imageUploadingCount = MutableStateFlow(0)
    val imageUploadingCount: StateFlow<Int> = _imageUploadingCount.asStateFlow()

    private val _imageDataCache = MutableStateFlow<Map<String, ByteArray>>(emptyMap())
    val imageDataCache: StateFlow<Map<String, ByteArray>> = _imageDataCache.asStateFlow()

    fun loadCafeDetailInfo(cafeId: Long) {
        viewModelScope.launch(ioDispatcher) {
            _isLoading.value = true
            try {
                when (val result = getCafeDetailUseCase(cafeId)) {
                    is ApiResult.Success -> {
                        _cafeInfo.value = result.data
                        _serverImageUrls.value = result.data?.imageUrls ?: emptyList()
                        _uploadedImageUrls.value = emptyList()
                    }
                    is ApiResult.Failure -> {
                        _cafeInfo.value = null
                        _events.send(result.message ?: "카페 정보를 불러올 수 없습니다")
                    }
                    is ApiResult.Loading -> {}
                }
            } catch (e: Exception) {
                _events.send(e.message ?: "알 수 없는 오류")
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun createCafe(request: CreateCafeRequest) {
        viewModelScope.launch(ioDispatcher) {
            _isLoading.value = true
            try {
                val finalRequest = request.copy(imageUrls = _uploadedImageUrls.value)

                when (val result = createCafeUseCase(finalRequest)) {
                    is ApiResult.Success -> {
                        _createCafeSuccess.value = true
                        _events.send("카페가 등록되었습니다")
                    }
                    is ApiResult.Failure -> {
                        _createCafeSuccess.value = false
                        _events.send(result.message ?: "카페 등록 실패")
                    }
                    is ApiResult.Loading -> {}
                }
            } catch (e: Exception) {
                _events.send(e.message ?: "알 수 없는 오류")
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun updateCafe(cafeId: Long, request: UpdateCafeRequest) {
        viewModelScope.launch(ioDispatcher) {
            _isLoading.value = true
            try {
                val allImageUrls = _serverImageUrls.value + _uploadedImageUrls.value
                val finalRequest = request.copy(imageUrls = allImageUrls)

                when (val result = updateCafeUseCase(cafeId, finalRequest)) {
                    is ApiResult.Success -> {
                        _updateCafeSuccess.value = true
                        _events.send("카페 정보가 수정되었습니다")
                    }
                    is ApiResult.Failure -> {
                        _updateCafeSuccess.value = false
                        _events.send(result.message ?: "카페 정보 수정 실패")
                    }
                    is ApiResult.Loading -> {}
                }
            } catch (e: Exception) {
                _events.send(e.message ?: "알 수 없는 오류")
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun uploadImage(fileName: String, content: ByteArray, onComplete: (() -> Unit)? = null) {
        viewModelScope.launch(ioDispatcher) {
            _imageUploadingCount.value += 1
            _errorMessage.value = null

            try {
                val result = repository.uploadImage(fileName, content)
                when (result) {
                    is ApiResult.Success -> {
                        val imageUrl = result.data?.trim()
                        if (!imageUrl.isNullOrEmpty()) {
                            val currentUrls = _uploadedImageUrls.value.toMutableList()
                            currentUrls.add(imageUrl)
                            _uploadedImageUrls.value = currentUrls
                        }
                    }
                    is ApiResult.Failure -> {
                        val errorMsg = when {
                            result.message?.contains("FileSizeLimitExceededException") == true
                                    || result.message?.contains("maximum permitted size") == true ->
                                "이미지 크기가 1MB를 초과합니다"
                            result.message?.contains("409") == true -> "이미지가 이미 존재합니다"
                            result.message?.contains("415") == true -> "지원하지 않는 이미지 형식입니다"
                            else -> result.message ?: "이미지 업로드 실패"
                        }
                        _errorMessage.value = errorMsg
                        _events.send(errorMsg)
                    }
                    is ApiResult.Loading -> {}
                }
            } catch (e: Exception) {
                val errorMsg = "이미지 업로드 중 오류: ${e.message}"
                _errorMessage.value = errorMsg
                _events.send(errorMsg)
            } finally {
                _imageUploadingCount.value -= 1
                onComplete?.invoke()
            }
        }
    }

    fun loadImage(imageId: String) {
        if (_imageDataCache.value.containsKey(imageId)) return

        viewModelScope.launch(ioDispatcher) {
            try {
                when (val result = repository.getImage(imageId)) {
                    is ApiResult.Success -> {
                        val imageData = result.data
                        if (imageData != null) {
                            val currentCache = _imageDataCache.value.toMutableMap()
                            currentCache[imageId] = imageData
                            _imageDataCache.value = currentCache
                        }
                    }
                    is ApiResult.Failure -> { }
                    is ApiResult.Loading -> {}
                }
            } catch (e: Exception) { }
        }
    }

    fun removeUploadedImage(imageUrl: String) {
        val currentUrls = _uploadedImageUrls.value.toMutableList()
        currentUrls.remove(imageUrl)
        _uploadedImageUrls.value = currentUrls
    }

    fun removeServerImage(imageUrl: String) {
        val currentUrls = _serverImageUrls.value.toMutableList()
        currentUrls.remove(imageUrl)
        _serverImageUrls.value = currentUrls
    }

    fun clearUploadedImages() {
        _uploadedImageUrls.value = emptyList()
    }

    fun resetCreateSuccess() {
        _createCafeSuccess.value = false
    }

    fun resetUpdateSuccess() {
        _updateCafeSuccess.value = false
    }

    fun clearError() {
        _errorMessage.value = null
    }
}
