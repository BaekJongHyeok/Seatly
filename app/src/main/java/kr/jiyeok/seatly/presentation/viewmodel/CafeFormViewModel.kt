package kr.jiyeok.seatly.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
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
import kr.jiyeok.seatly.di.IoDispatcher
import kr.jiyeok.seatly.domain.usecase.CreateCafeUseCase
import kr.jiyeok.seatly.domain.usecase.GetCafeDetailUseCase
import kr.jiyeok.seatly.domain.usecase.UpdateCafeUseCase
import okhttp3.MultipartBody
import javax.inject.Inject

@HiltViewModel
class CafeFormViewModel @Inject constructor(
    private val repository: SeatlyRepository,
    private val createCafeUseCase: CreateCafeUseCase,
    private val updateCafeUseCase: UpdateCafeUseCase,
    private val getCafeDetailUseCase: GetCafeDetailUseCase,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : ViewModel() {

    // 카페 정보
    private val _cafeInfo = MutableStateFlow<StudyCafeDetailDto?>(null)
    val cafeInfo: StateFlow<StudyCafeDetailDto?> = _cafeInfo.asStateFlow()

    // =====================================================
    // Loading State
    // =====================================================
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // =====================================================
    // Error State
    // =====================================================
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _events = Channel<String>(Channel.Factory.BUFFERED)
    val events = _events.receiveAsFlow()

    // 카페 등록 성공 플래그
    private val _createCafeSuccess = MutableStateFlow(false)
    val createCafeSuccess: StateFlow<Boolean> = _createCafeSuccess

    // 카페 수정 성공 플래그
    private val _updateCafeSuccess = MutableStateFlow(false)
    val updateCafeSuccess: StateFlow<Boolean> = _updateCafeSuccess

    // =====================================================
    // Image Upload States
    // =====================================================

    // 서버에서 로드한 기존 이미지 UUID 리스트 (수정 모드용)
    private val _serverImageUrls = MutableStateFlow<List<String>>(emptyList())
    val serverImageUrls: StateFlow<List<String>> = _serverImageUrls.asStateFlow()

    // 새로 업로드한 이미지 UUID 리스트
    private val _uploadedImageUrls = MutableStateFlow<List<String>>(emptyList())
    val uploadedImageUrls: StateFlow<List<String>> = _uploadedImageUrls.asStateFlow()

    // 이미지 업로드 진행 중 개수
    private val _imageUploadingCount = MutableStateFlow(0)
    val imageUploadingCount: StateFlow<Int> = _imageUploadingCount.asStateFlow()

    // 이미지 바이트 데이터 캐시 (imageId -> ByteArray)
    private val _imageDataCache = MutableStateFlow<Map<String, ByteArray>>(emptyMap())
    val imageDataCache: StateFlow<Map<String, ByteArray>> = _imageDataCache.asStateFlow()

    init {
        // 초기화 로직이 필요하면 여기에 작성
    }

    // =====================================================
    // Public Methods - Cafe CRUD
    // =====================================================

    /**
     * 카페 상세 정보 조회 (수정 모드)
     */
    fun loadCafeDetailInfo(cafeId: Long) {
        viewModelScope.launch(ioDispatcher) {
            _isLoading.value = true
            try {
                when (val result = getCafeDetailUseCase(cafeId)) {
                    is ApiResult.Success -> {
                        _cafeInfo.value = result.data
                        // 서버에서 받은 이미지 URL들을 별도로 저장
                        _serverImageUrls.value = result.data?.imageUrls ?: emptyList()
                        // uploadedImageUrls는 빈 리스트로 시작 (새로 업로드하는 이미지용)
                        _uploadedImageUrls.value = emptyList()
                    }
                    is ApiResult.Failure -> {
                        _cafeInfo.value = null
                        _events.send(result.message ?: "카페 정보를 불러올 수 없습니다")
                    }
                }
            } catch (e: Exception) {
                _events.send(e.message ?: "알 수 없는 오류")
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * 카페 등록
     */
    fun createCafe(request: CreateCafeRequest) {
        viewModelScope.launch(ioDispatcher) {
            _isLoading.value = true
            try {
                // imageUrls에 업로드된 URL 사용
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
                }
            } catch (e: Exception) {
                _events.send(e.message ?: "알 수 없는 오류")
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * 카페 정보 수정
     */
    fun updateCafe(cafeId: Long, request: UpdateCafeRequest) {
        viewModelScope.launch(ioDispatcher) {
            _isLoading.value = true
            try {
                // 서버에 이미 있는 이미지 + 새로 업로드한 이미지 합치기
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
                }
            } catch (e: Exception) {
                _events.send(e.message ?: "알 수 없는 오류")
            } finally {
                _isLoading.value = false
            }
        }
    }

    // =====================================================
    // Public Methods - Image Upload
    // =====================================================

    /**
     * 이미지 업로드
     * - API를 통해 이미지 업로드 요청
     * - 성공 시 uploadedImageUrls에 추가
     * - 실패 시 에러 메시지 표시
     */
    fun uploadImage(file: MultipartBody.Part, onComplete: (() -> Unit)? = null) {
        viewModelScope.launch(ioDispatcher) {
            _imageUploadingCount.value += 1
            _errorMessage.value = null

            try {
                val result = repository.uploadImage(file)
                when (result) {
                    is ApiResult.Success -> {
                        // String(UUID)을 받아서 처리
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
                }
            } catch (e: Exception) {
                val errorMsg = "이미지 업로드 중 오류: ${e.localizedMessage}"
                _errorMessage.value = errorMsg
                _events.send(errorMsg)
            } finally {
                _imageUploadingCount.value -= 1
                onComplete?.invoke()
            }
        }
    }

    /**
     * 서버 이미지 로드 (ByteArray)
     */
    fun loadImage(imageId: String) {
        // 이미 캐시에 있으면 다시 로드하지 않음
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
                    is ApiResult.Failure -> {
                        // 이미지 로드 실패는 조용히 처리 (UI에 영향 없음)
                    }
                }
            } catch (e: Exception) {
                // 에러 무시
            }
        }
    }

    /**
     * 새로 업로드한 이미지 제거
     */
    fun removeUploadedImage(imageUrl: String) {
        val currentUrls = _uploadedImageUrls.value.toMutableList()
        currentUrls.remove(imageUrl)
        _uploadedImageUrls.value = currentUrls
    }

    /**
     * 서버 이미지 삭제 (수정 모드에서 기존 이미지 제거)
     */
    fun removeServerImage(imageUrl: String) {
        val currentUrls = _serverImageUrls.value.toMutableList()
        currentUrls.remove(imageUrl)
        _serverImageUrls.value = currentUrls
    }

    /**
     * 모든 업로드된 이미지 URL 초기화
     */
    fun clearUploadedImages() {
        _uploadedImageUrls.value = emptyList()
    }

    /**
     * 성공 플래그 초기화
     */
    fun resetCreateSuccess() {
        _createCafeSuccess.value = false
    }

    fun resetUpdateSuccess() {
        _updateCafeSuccess.value = false
    }

    // =====================================================
    // Error Handling
    // =====================================================

    /**
     * 에러 메시지 초기화
     */
    fun clearError() {
        _errorMessage.value = null
    }
}
