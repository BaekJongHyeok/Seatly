package kr.jiyeok.seatly.presentation.viewmodel

import androidx.compose.ui.graphics.ImageBitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kr.jiyeok.seatly.data.remote.response.SeatDto
import kr.jiyeok.seatly.data.remote.response.SessionDto
import kr.jiyeok.seatly.data.remote.response.StudyCafeDetailDto
import kr.jiyeok.seatly.data.remote.response.UsageDto
import kr.jiyeok.seatly.data.repository.ApiResult
import kr.jiyeok.seatly.domain.usecase.*
import kr.jiyeok.seatly.util.toImageBitmap
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class CafeDetailViewModel(
    private val getCafeDetailUseCase: GetCafeDetailUseCase,
    private val getCafeUsageUseCase: GetCafeUsageUseCase,
    private val getCafeSeatsUseCase: GetCafeSeatsUseCase,
    private val getImageUseCase: GetImageUseCase,
    private val getSessionsUseCase: GetSessionsUseCase,
    private val assignSeatUseCase: AssignSeatUseCase,
    private val startSessionUseCase: StartSessionUseCase
) : ViewModel() {

    data class CafeDetailUiState(
        val cafeInfo: StudyCafeDetailDto? = null,
        val cafeUsage: UsageDto? = null,
        val seats: List<SeatDto> = emptyList(),
        val sessions: List<SessionDto> = emptyList(),
        val images: Map<String, ImageBitmap> = emptyMap(),
        val isLoadingInfo: Boolean = false,
        val isLoadingUsage: Boolean = false,
        val isLoadingSeats: Boolean = false,
        val isLoadingSessions: Boolean = false,
        val isLoadingAssignment: Boolean = false,
        val error: String? = null
    )

    private val _uiState = MutableStateFlow(CafeDetailUiState())
    val uiState: StateFlow<CafeDetailUiState> = _uiState.asStateFlow()

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

    private val _events = Channel<String>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    private val loadingImageIds = mutableSetOf<String>()
    private val imageLoadMutex = Mutex()

    fun loadCafeDetailInfos(cafeId: Long) {
        viewModelScope.launch {
            awaitAll(
                async { loadCafeInfo(cafeId) },
                async { loadCafeUsage(cafeId) },
                async { loadSeatInfo(cafeId) },
                async { loadSessions(cafeId) }
            )
        }
    }

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
                is ApiResult.Loading -> {}
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
                }
                is ApiResult.Loading -> {}
            }
        } catch (e: Exception) {
            _uiState.update { it.copy(isLoadingUsage = false) }
        }
    }

    fun loadImage(imageId: String) {
        if (_uiState.value.images.containsKey(imageId)) return

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
                                _uiState.update { state ->
                                    state.copy(images = state.images + (imageId to it))
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
    }

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
                is ApiResult.Loading -> {}
            }
        } catch (e: Exception) {
            _uiState.update { it.copy(isLoadingSeats = false) }
            _events.send(e.message ?: "알 수 없는 오류")
        }
    }

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
                }
                is ApiResult.Loading -> {}
            }
        } catch (e: Exception) {
            _uiState.update { it.copy(isLoadingSessions = false) }
        }
    }

    fun assignSeat(seatId: String, currentCafeId: Long) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingAssignment = true) }
            try {
                when (val assignResult = assignSeatUseCase(seatId)) {
                    is ApiResult.Success -> {
                        val session = assignResult.data
                        if (session != null) {
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
                                is ApiResult.Loading -> {}
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
                    is ApiResult.Loading -> {}
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoadingAssignment = false) }
                _events.send(e.message ?: "알 수 없는 오류")
            }
        }
    }
}
