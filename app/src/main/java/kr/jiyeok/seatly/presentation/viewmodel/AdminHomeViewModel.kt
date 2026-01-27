package kr.jiyeok.seatly.presentation.viewmodel

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kr.jiyeok.seatly.data.remote.response.StudyCafeSummaryDto
import kr.jiyeok.seatly.data.remote.response.UsageDto
import kr.jiyeok.seatly.data.repository.ApiResult
import kr.jiyeok.seatly.di.IoDispatcher
import kr.jiyeok.seatly.domain.usecase.GetAdminCafesUseCase
import kr.jiyeok.seatly.domain.usecase.GetCafeUsageUseCase
import kr.jiyeok.seatly.domain.usecase.GetImageUseCase
import javax.inject.Inject

@HiltViewModel
class AdminHomeViewModel @Inject constructor(
    private val getAdminCafesUseCase: GetAdminCafesUseCase,
    private val getCafeUsageUseCase: GetCafeUsageUseCase,
    private val getImageUseCase: GetImageUseCase,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : ViewModel() {

    // =====================================================
    // State
    // =====================================================

    private val _cafes = MutableStateFlow<List<StudyCafeSummaryDto>>(emptyList())
    val cafes: StateFlow<List<StudyCafeSummaryDto>> = _cafes.asStateFlow()

    private val _cafeUsages = MutableStateFlow<Map<Long, UsageDto>>(emptyMap())
    val cafeUsages: StateFlow<Map<Long, UsageDto>> = _cafeUsages.asStateFlow()

    private val _imageBitmapCache = MutableStateFlow<Map<String, Bitmap>>(emptyMap())
    val imageBitmapCache: StateFlow<Map<String, Bitmap>> = _imageBitmapCache.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _events = Channel<String>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    private val loadingImageIds = mutableSetOf<String>()

    // =====================================================
    // Public Methods
    // =====================================================

    /**
     * 관리자가 등록한 카페 목록을 로드합니다.
     * 카페 로드 후 각 카페의 사용 현황과 이미지를 자동으로 로드합니다.
     */
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
                        logError("카페 목록 조회 실패", result.throwable)
                    }
                }
            } catch (e: Exception) {
                _cafes.update { emptyList() }
                sendEvent(ERROR_UNKNOWN)
                logError("카페 목록 조회 중 예외 발생", e)
            } finally {
                _isLoading.update { false }
            }
        }
    }

    /**
     * 카페 목록 로드 재시도
     */
    fun retryLoadCafes() {
        loadRegisteredCafes()
    }

    // =====================================================
    // Private Methods
    // =====================================================

    /**
     * 카페 관련 데이터(사용 현황, 이미지)를 병렬로 로드합니다.
     */
    private fun loadCafeRelatedData(cafes: List<StudyCafeSummaryDto>) {
        viewModelScope.launch(ioDispatcher) {
            // Usage는 가볍기 때문에 전체 병렬 로드
            cafes.forEach { cafe ->
                launch { loadCafeUsage(cafe.id) }
            }

            // 이미지는 배치 처리 (동시에 최대 3개씩만 로드)
            val imageIds = cafes.mapNotNull { it.mainImageUrl }
            imageIds.chunked(3).forEach { batch ->
                batch.map { imageId ->
                    async { loadImage(imageId) }
                }.awaitAll()
            }
        }
    }

    /**
     * 특정 카페의 좌석 사용 현황을 로드합니다.
     */
    private suspend fun loadCafeUsage(cafeId: Long) {
        try {
            when (val result = getCafeUsageUseCase(cafeId)) {
                is ApiResult.Success -> {
                    result.data?.let { usage ->
                        _cafeUsages.update { it + (cafeId to usage) }
                    }
                }
                is ApiResult.Failure -> {
                    logWarning("좌석 사용 현황 조회 실패: ${result.message} (cafeId=$cafeId)")
                }
            }
        } catch (e: Exception) {
            logError("좌석 사용 현황 조회 중 예외 발생 (cafeId=$cafeId)", e)
        }
    }

    /**
     * 이미지를 로드하고 캐시에 저장합니다.
     * 이미 캐시된 이미지는 다시 로드하지 않습니다.
     */
    private suspend fun loadImage(imageId: String) {
        if (_imageBitmapCache.value.containsKey(imageId)) return

        synchronized(loadingImageIds) {
            if (loadingImageIds.contains(imageId)) return
            loadingImageIds.add(imageId)
        }

        try {
            when (val result = getImageUseCase(imageId)) {
                is ApiResult.Success -> {
                    result.data?.let { imageData ->
                        val bitmap = decodeSampledBitmap(imageData, 200, 200)
                        bitmap?.let {
                            _imageBitmapCache.update { cache -> cache + (imageId to it) }
                            logDebug("이미지 로드 성공: $imageId")
                        }
                    }
                }
                is ApiResult.Failure -> {
                    logWarning("이미지 로드 실패: ${result.message} (imageId=$imageId)")
                }
            }
        } catch (e: Exception) {
            logError("이미지 로드 중 예외 발생 (imageId=$imageId)", e)
        } finally {
            synchronized(loadingImageIds) {
                loadingImageIds.remove(imageId)
            }
        }
    }

    /**
     * 샘플링하여 Bitmap 디코딩
     */
    private fun decodeSampledBitmap(
        data: ByteArray,
        reqWidth: Int,
        reqHeight: Int
    ): Bitmap? {
        return try {
            // 먼저 이미지 크기만 확인
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
                BitmapFactory.decodeByteArray(data, 0, data.size, this)

                // 샘플링 비율 계산
                inSampleSize = calculateInSampleSize(this, reqWidth, reqHeight)
                inJustDecodeBounds = false
            }

            // 실제 디코딩 (이 부분이 문제였습니다)
            BitmapFactory.decodeByteArray(data, 0, data.size, options)
        } catch (e: Exception) {
            logError("Bitmap 디코딩 실패", e)
            null
        }
    }

    /**
     * 샘플링 비율 계산
     */
    private fun calculateInSampleSize(
        options: BitmapFactory.Options,
        reqWidth: Int,
        reqHeight: Int
    ): Int {
        val (height: Int, width: Int) = options.run { outHeight to outWidth }
        var inSampleSize = 1

        if (height > reqHeight || width > reqWidth) {
            val halfHeight: Int = height / 2
            val halfWidth: Int = width / 2

            while (halfHeight / inSampleSize >= reqHeight &&
                halfWidth / inSampleSize >= reqWidth) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }

    /**
     * 사용자에게 표시할 이벤트를 전송합니다.
     */
    private suspend fun sendEvent(message: String) {
        _events.send(message)
    }

    // =====================================================
    // Logging Helpers
    // =====================================================

    private fun logDebug(message: String) {
        Log.d(TAG, message)
    }

    private fun logWarning(message: String) {
        Log.w(TAG, message)
    }

    private fun logError(message: String, throwable: Throwable?) {
        Log.e(TAG, message, throwable)
    }

    // =====================================================
    // Constants
    // =====================================================

    override fun onCleared() {
        super.onCleared()
        // Bitmap 메모리 해제
        _imageBitmapCache.value.values.forEach { it.recycle() }
    }

    companion object {
        private const val TAG = "AdminHomeViewModel"

        // Error Messages
        private const val ERROR_LOAD_CAFES = "카페 목록 조회 실패"
        private const val ERROR_UNKNOWN = "알 수 없는 오류"
    }
}
