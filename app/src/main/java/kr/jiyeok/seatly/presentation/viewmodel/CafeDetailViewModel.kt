package kr.jiyeok.seatly.presentation.viewmodel

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kr.jiyeok.seatly.data.remote.response.SeatDto
import kr.jiyeok.seatly.data.remote.response.SessionDto
import kr.jiyeok.seatly.data.remote.response.StudyCafeDetailDto
import kr.jiyeok.seatly.data.remote.response.UsageDto
import kr.jiyeok.seatly.data.repository.ApiResult
import kr.jiyeok.seatly.di.IoDispatcher
import kr.jiyeok.seatly.domain.usecase.AssignSeatUseCase
import kr.jiyeok.seatly.domain.usecase.GetCafeDetailUseCase
import kr.jiyeok.seatly.domain.usecase.GetCafeSeatsUseCase
import kr.jiyeok.seatly.domain.usecase.GetCafeUsageUseCase
import kr.jiyeok.seatly.domain.usecase.GetImageUseCase
import kr.jiyeok.seatly.domain.usecase.GetSessionsUseCase
import kr.jiyeok.seatly.domain.usecase.StartSessionUseCase
import javax.inject.Inject


@HiltViewModel
class CafeDetailViewModel @Inject constructor(
    private val getCafeDetailUseCase: GetCafeDetailUseCase,
    private val getCafeUsageUseCase: GetCafeUsageUseCase,
    private val getCafeSeatsUseCase: GetCafeSeatsUseCase,
    private val getImageUseCase: GetImageUseCase,
    private val getSessionsUseCase: GetSessionsUseCase,
    private val assignSeatUseCase: AssignSeatUseCase,
    private val startSessionUseCase: StartSessionUseCase,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : ViewModel() {

    // =====================================================
    // UI State
    // =====================================================

    /**
     * 카페 상세 화면의 전체 UI 상태를 관리하는 데이터 클래스
     */
    data class CafeDetailUiState(
        val cafeInfo: StudyCafeDetailDto? = null,
        val cafeUsage: UsageDto? = null,
        val seats: List<SeatDto> = emptyList(),
        val sessions: List<SessionDto> = emptyList(),
        val images: Map<String, Bitmap> = emptyMap(),
        val isLoadingInfo: Boolean = false,
        val isLoadingUsage: Boolean = false,
        val isLoadingSeats: Boolean = false,
        val isLoadingSessions: Boolean = false,
        val isLoadingAssignment: Boolean = false,
        val error: String? = null
    )

    private val _uiState = MutableStateFlow(CafeDetailUiState())
    val uiState: StateFlow<CafeDetailUiState> = _uiState.asStateFlow()

    // Derived State: 하나라도 로딩 중이면 true
    val isAnyLoading: StateFlow<Boolean> = _uiState
        .map { state ->
            state.isLoadingInfo ||
                    state.isLoadingUsage ||
                    state.isLoadingSeats ||
                    state.isLoadingSessions
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false
        )

    // 이벤트 채널 (Toast 메시지 등)
    private val _events = Channel<String>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    // 이미지 로딩 중복 방지
    private val loadingImageIds = mutableSetOf<String>()
    val isLoading: StateFlow<Boolean> = isAnyLoading


    // =====================================================
    // Public Methods
    // =====================================================

    /**
     * 카페 상세 정보를 병렬로 로드
     */
    fun loadCafeDetailInfos(cafeId: Long) {
        viewModelScope.launch(ioDispatcher) {
            awaitAll(
                async { loadCafeInfo(cafeId) },
                async { loadCafeUsage(cafeId) },
                async { loadSeatInfo(cafeId) },
                async { loadSessions(cafeId) }
            )
        }
    }


    // =====================================================
    // Private Methods
    // =====================================================

    /**
     * 카페 기본 정보 로드
     */
    private suspend fun loadCafeInfo(cafeId: Long) {
        _uiState.update { it.copy(isLoadingInfo = true) }

        try {
            when (val result = getCafeDetailUseCase(cafeId)) {
                is ApiResult.Success -> {
                    val cafeInfo = result.data
                    _uiState.update {
                        it.copy(
                            cafeInfo = cafeInfo,
                            isLoadingInfo = false,
                            error = null
                        )
                    }

                    // 이미지 자동 로드
                    cafeInfo?.imageUrls?.forEach { imageId ->
                        loadImage(imageId)
                    }
                }
                is ApiResult.Failure -> {
                    _uiState.update {
                        it.copy(
                            isLoadingInfo = false,
                            error = result.message
                        )
                    }
                    _events.send(result.message ?: "카페 정보 조회 실패")
                }
            }
        } catch (e: Exception) {
            _uiState.update {
                it.copy(
                    isLoadingInfo = false,
                    error = e.message
                )
            }
            _events.send(e.message ?: "알 수 없는 오류")
        }
    }

    /**
     * 카페 사용 현황 로드
     */
    private suspend fun loadCafeUsage(cafeId: Long) {
        _uiState.update { it.copy(isLoadingUsage = true) }

        try {
            when (val result = getCafeUsageUseCase(cafeId)) {
                is ApiResult.Success -> {
                    _uiState.update {
                        it.copy(
                            cafeUsage = result.data,
                            isLoadingUsage = false
                        )
                    }
                }
                is ApiResult.Failure -> {
                    _uiState.update { it.copy(isLoadingUsage = false) }
                    // Usage는 중요도가 낮으므로 실패해도 Toast 표시 안 함
                }
            }
        } catch (e: Exception) {
            _uiState.update { it.copy(isLoadingUsage = false) }
        }
    }


    /**
     * 이미지 로드 (공개 메서드, UI에서 직접 호출 가능)
     */
    fun loadImage(imageId: String) {
        // 이미 로드되었거나 로딩 중이면 스킵
        if (_uiState.value.images.containsKey(imageId)) return

        synchronized(loadingImageIds) {
            if (loadingImageIds.contains(imageId)) return
            loadingImageIds.add(imageId)
        }

        viewModelScope.launch(ioDispatcher) {
            try {
                when (val result = getImageUseCase(imageId)) {
                    is ApiResult.Success -> {
                        result.data?.let { imageData ->
                            val bitmap = withContext(Dispatchers.Default) {
                                decodeSampledBitmap(imageData, 800, 800)
                            }
                            bitmap?.let {
                                _uiState.update { state ->
                                    state.copy(images = state.images + (imageId to it))
                                }
                            }
                        }
                    }
                    is ApiResult.Failure -> {
                        // 이미지 로드 실패는 무시 (선택적)
                    }
                }
            } catch (e: Exception) {
                // 예외 발생 시 무시
            } finally {
                synchronized(loadingImageIds) {
                    loadingImageIds.remove(imageId)
                }
            }
        }
    }

    /**
     * 좌석 정보 로드
     */
    private suspend fun loadSeatInfo(cafeId: Long) {
        _uiState.update { it.copy(isLoadingSeats = true) }

        try {
            when (val result = getCafeSeatsUseCase(cafeId)) {
                is ApiResult.Success -> {
                    _uiState.update {
                        it.copy(
                            seats = result.data ?: emptyList(),
                            isLoadingSeats = false
                        )
                    }
                }
                is ApiResult.Failure -> {
                    _uiState.update { it.copy(isLoadingSeats = false) }
                    _events.send(result.message ?: "좌석 정보 조회 실패")
                }
            }
        } catch (e: Exception) {
            _uiState.update { it.copy(isLoadingSeats = false) }
            _events.send(e.message ?: "알 수 없는 오류")
        }
    }

    /**
     * 세션 정보 로드
     */
    private suspend fun loadSessions(cafeId: Long) {
        _uiState.update { it.copy(isLoadingSessions = true) }
        try {
            when (val result = getSessionsUseCase(cafeId)) {
                is ApiResult.Success -> {
                    _uiState.update {
                        it.copy(
                            sessions = result.data ?: emptyList(),
                            isLoadingSessions = false
                        )
                    }
                }
                is ApiResult.Failure -> {
                    _uiState.update { it.copy(isLoadingSessions = false) }
                    // 세션 정보는 중요도가 낮으므로 실패해도 Toast 표시 안 함
                }
            }
        } catch (e: Exception) {
            _uiState.update { it.copy(isLoadingSessions = false) }
        }
    }

    /**
     * 좌석 배정 및 이용 시작
     */
    fun assignSeat(seatId: String, currentCafeId: Long) {
        viewModelScope.launch(ioDispatcher) {
            _uiState.update { it.copy(isLoadingAssignment = true) }
            try {
                // 1. 좌석 배정 (Assign)
                when (val assignResult = assignSeatUseCase(seatId)) {
                    is ApiResult.Success -> {
                        val session = assignResult.data
                        if (session != null) {
                            // 2. 이용 시작 (Start)
                            when (val startResult = startSessionUseCase(session.id)) {
                                is ApiResult.Success -> {
                                    _uiState.update { it.copy(isLoadingAssignment = false) }
                                    _events.send("좌석 이용이 시작되었습니다.")
                                    loadSessions(currentCafeId)
                                }
                                is ApiResult.Failure -> {
                                    _uiState.update { it.copy(isLoadingAssignment = false) }
                                    _events.send(startResult.message ?: "이용 시작 실패")
                                }
                            }
                        } else {
                            _uiState.update { it.copy(isLoadingAssignment = false) }
                            _events.send("세션 정보를 가져올 수 없습니다.")
                        }
                    }
                    is ApiResult.Failure -> {
                        _uiState.update { it.copy(isLoadingAssignment = false) }
                        _events.send(assignResult.message ?: "좌석 배정 실패")
                    }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoadingAssignment = false) }
                _events.send(e.message ?: "알 수 없는 오류")
            }
        }
    }

    /**
     * 샘플링하여 Bitmap 디코딩 (메모리 최적화)
     */
    private fun decodeSampledBitmap(
        data: ByteArray,
        reqWidth: Int,
        reqHeight: Int
    ): Bitmap? {
        return try {
            val options = BitmapFactory.Options().apply {
                // 먼저 이미지 크기만 확인
                inJustDecodeBounds = true
                BitmapFactory.decodeByteArray(data, 0, data.size, this)

                // 샘플링 비율 계산
                inSampleSize = calculateInSampleSize(this, reqWidth, reqHeight)

                // 실제 디코딩
                inJustDecodeBounds = false
            }

            BitmapFactory.decodeByteArray(data, 0, data.size, options)
        } catch (e: Exception) {
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
}
