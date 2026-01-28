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
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kr.jiyeok.seatly.data.local.Seat
import kr.jiyeok.seatly.data.remote.enums.ESeatStatus
import kr.jiyeok.seatly.data.remote.request.SeatCreate
import kr.jiyeok.seatly.data.remote.request.SeatUpdate
import kr.jiyeok.seatly.data.remote.response.SeatDto
import kr.jiyeok.seatly.data.remote.response.SessionDto
import kr.jiyeok.seatly.data.remote.response.StudyCafeDetailDto
import kr.jiyeok.seatly.data.remote.response.UsageDto
import kr.jiyeok.seatly.data.remote.response.UserTimePassInfo
import kr.jiyeok.seatly.data.repository.ApiResult
import kr.jiyeok.seatly.domain.usecase.AddUserTimePassUseCase
import kr.jiyeok.seatly.domain.usecase.CreateSeatsUseCase
import kr.jiyeok.seatly.domain.usecase.DeleteSeatUseCase
import kr.jiyeok.seatly.domain.usecase.GetCafeDetailUseCase
import kr.jiyeok.seatly.domain.usecase.GetCafeSeatsUseCase
import kr.jiyeok.seatly.domain.usecase.GetCafeUsageUseCase
import kr.jiyeok.seatly.domain.usecase.GetImageUseCase
import kr.jiyeok.seatly.domain.usecase.GetSessionsUseCase
import kr.jiyeok.seatly.domain.usecase.GetUsersWithTimePassUseCase
import kr.jiyeok.seatly.domain.usecase.UpdateSeatsUseCase
import kr.jiyeok.seatly.util.toImageBitmap

class AdminCafeDetailViewModel(
    private val getCafeDetailUseCase: GetCafeDetailUseCase,
    private val getCafeUsageUseCase: GetCafeUsageUseCase,
    private val getCafeMembersUseCase: GetUsersWithTimePassUseCase,
    private val addUserTimePassUseCase: AddUserTimePassUseCase,
    private val getCafeSeatsUseCase: GetCafeSeatsUseCase,
    private val createSeatsUseCase: CreateSeatsUseCase,
    private val updateSeatsUseCase: UpdateSeatsUseCase,
    private val deleteSeatUseCase: DeleteSeatUseCase,
    private val getImageUseCase: GetImageUseCase,
    private val getSessionsUseCase: GetSessionsUseCase,
    private val ioDispatcher: CoroutineDispatcher
) : ViewModel() {

    data class CafeDetailUiState(
        val cafeInfo: StudyCafeDetailDto? = null,
        val cafeUsage: UsageDto? = null,
        val members: List<UserTimePassInfo> = emptyList(),
        val seats: List<SeatDto> = emptyList(),
        val sessions: List<SessionDto> = emptyList(),
        val images: Map<String, ImageBitmap> = emptyMap(),
        val isLoadingInfo: Boolean = false,
        val isLoadingUsage: Boolean = false,
        val isLoadingMembers: Boolean = false,
        val isLoadingSeats: Boolean = false,
        val isLoadingSessions: Boolean = false,
        val error: String? = null
    )

    private val _uiState = MutableStateFlow(CafeDetailUiState())
    val uiState: StateFlow<CafeDetailUiState> = _uiState.asStateFlow()

    val isAnyLoading: StateFlow<Boolean> = _uiState
        .map { state ->
            state.isLoadingInfo ||
                    state.isLoadingUsage ||
                    state.isLoadingMembers ||
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
    private val mutex = Mutex()

    fun loadCafeDetailInfos(cafeId: Long) {
        viewModelScope.launch(ioDispatcher) {
            awaitAll(
                async { loadCafeInfo(cafeId) },
                async { loadCafeUsage(cafeId) },
                async { loadCafeMembers(cafeId) },
                async { loadSeatInfo(cafeId) },
                async { loadSessions(cafeId) }
            )
        }
    }

    fun addUserTimePass(userId: Long, studyCafeId: Long, time: Long) {
        viewModelScope.launch(ioDispatcher) {
            when (val result = addUserTimePassUseCase(userId, studyCafeId, time)) {
                is ApiResult.Success -> {
                    _events.send("시간권이 추가되었습니다.")
                    loadCafeMembers(studyCafeId)
                }
                is ApiResult.Failure -> {
                    _events.send(result.message ?: "시간권 추가 실패")
                }
                is ApiResult.Loading -> {}
            }
        }
    }

    fun loadImage(imageId: String) {
        if (_uiState.value.images.containsKey(imageId)) return

        viewModelScope.launch(ioDispatcher) {
            val shouldLoad = mutex.withLock {
                if (loadingImageIds.contains(imageId)) {
                    false
                } else {
                    loadingImageIds.add(imageId)
                    true
                }
            }
            if (!shouldLoad) return@launch

            try {
                when (val result = getImageUseCase(imageId)) {
                    is ApiResult.Success -> {
                        result.data?.let { imageData ->
                            val bitmap = imageData.toImageBitmap()
                            if (bitmap != null) {
                                _uiState.update { state ->
                                    state.copy(images = state.images + (imageId to bitmap))
                                }
                            }
                        }
                    }
                    is ApiResult.Failure -> {}
                    is ApiResult.Loading -> {}
                }
            } catch (e: Exception) {
            } finally {
                mutex.withLock {
                    loadingImageIds.remove(imageId)
                }
            }
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

    fun loadCafeMembers(cafeId: Long) {
        viewModelScope.launch(ioDispatcher) {
            _uiState.update { it.copy(isLoadingMembers = true) }
            try {
                when (val result = getCafeMembersUseCase(cafeId)) {
                    is ApiResult.Success -> {
                        _uiState.update {
                            it.copy(
                                members = result.data ?: emptyList(),
                                isLoadingMembers = false
                            )
                        }
                    }
                    is ApiResult.Failure -> {
                        _uiState.update { it.copy(isLoadingMembers = false) }
                        _events.send(result.message ?: "카페 멤버 조회 실패")
                    }
                    is ApiResult.Loading -> {}
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoadingMembers = false) }
                _events.send(e.message ?: "알 수 없는 오류")
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

    fun saveSeatConfig(cafeId: Long, currentSeats: List<Seat>) {
        viewModelScope.launch(ioDispatcher) {
            _uiState.update { it.copy(isLoadingSeats = true) }
            try {
                val serverSeats = uiState.value.seats
                val serverSeatIds = serverSeats.map { it.id.toString() }.toSet()

                val newSeats = currentSeats.filter { it.id.startsWith("NEW_") }
                val updateSeats = currentSeats.filter { seat ->
                    !seat.id.startsWith("NEW_") && serverSeatIds.contains(seat.id)
                }
                val deleteIds = serverSeats
                    .map { it.id.toString() }
                    .filter { serverId ->
                        currentSeats.none { it.id == serverId }
                    }

                var deleteFailed = false
                for (deleteId in deleteIds) {
                    when (val result = deleteSeatUseCase(cafeId, deleteId)) {
                        is ApiResult.Failure -> {
                            _events.send("좌석 삭제 실패: ${result.message}")
                            deleteFailed = true
                        }
                        is ApiResult.Success -> {}
                        is ApiResult.Loading -> {}
                    }
                }

                if (deleteIds.isNotEmpty()) {
                    kotlinx.coroutines.delay(300)
                }

                if (updateSeats.isNotEmpty()) {
                    val updateRequest = updateSeats.map { seat ->
                        SeatUpdate(
                            id = seat.id.toLong(),
                            name = seat.label,
                            status = ESeatStatus.AVAILABLE,
                            position = buildPositionString(seat)
                        )
                    }
                    when (val result = updateSeatsUseCase(cafeId, updateRequest)) {
                        is ApiResult.Failure -> {
                            _uiState.update { it.copy(isLoadingSeats = false) }
                            _events.send(result.message ?: "좌석 수정 실패")
                            return@launch
                        }
                        is ApiResult.Success -> {}
                        is ApiResult.Loading -> {}
                    }
                }

                if (newSeats.isNotEmpty()) {
                    val createRequest = newSeats.map { seat ->
                        SeatCreate(
                            name = seat.label,
                            status = ESeatStatus.AVAILABLE,
                            position = buildPositionString(seat)
                        )
                    }
                    when (val result = createSeatsUseCase(cafeId, createRequest)) {
                        is ApiResult.Failure -> {
                            _uiState.update { it.copy(isLoadingSeats = false) }
                            _events.send(result.message ?: "좌석 생성 실패")
                            return@launch
                        }
                        is ApiResult.Success -> {}
                        is ApiResult.Loading -> {}
                    }
                }

                val message = when {
                    newSeats.isEmpty() && updateSeats.isEmpty() && deleteIds.isEmpty() ->
                        "변경 사항이 없습니다."
                    deleteFailed ->
                        "좌석 정보가 부분적으로 저장되었습니다."
                    else ->
                        "좌석 정보가 저장되었습니다."
                }
                _events.send(message)
                loadSeatInfo(cafeId)

            } catch (e: Exception) {
                _uiState.update { it.copy(isLoadingSeats = false) }
                _events.send(e.message ?: "저장 중 오류 발생")
            }
        }
    }

    private fun buildPositionString(seat: Seat): String {
        return "${seat.pos.value.x},${seat.pos.value.y},${seat.size.value.x},${seat.size.value.y}"
    }
}
