package kr.jiyeok.seatly.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kr.jiyeok.seatly.data.remote.request.ReservationRequest
import kr.jiyeok.seatly.data.remote.request.StartSessionRequest
import kr.jiyeok.seatly.data.remote.response.SeatResponseDto
import kr.jiyeok.seatly.data.remote.response.StudyCafeDetailDto
import kr.jiyeok.seatly.data.remote.response.SessionResponseDto
import kr.jiyeok.seatly.data.repository.ApiResult
import kr.jiyeok.seatly.domain.usecase.cafe.GetCafeDetailUseCase
import kr.jiyeok.seatly.domain.usecase.seat.AutoAssignSeatUseCase
import kr.jiyeok.seatly.domain.usecase.seat.GetCafeSeatsUseCase
import kr.jiyeok.seatly.domain.usecase.seat.ReserveSeatUseCase
import kr.jiyeok.seatly.domain.usecase.session.StartSessionUseCase
import kr.jiyeok.seatly.di.IoDispatcher
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject

sealed interface CafeDetailUiState {
    object Idle : CafeDetailUiState
    object Loading : CafeDetailUiState
    data class Success(val cafe: StudyCafeDetailDto) : CafeDetailUiState
    data class Error(val message: String) : CafeDetailUiState
}

@HiltViewModel
class CafeDetailViewModel @Inject constructor(
    private val getCafeDetailUseCase: GetCafeDetailUseCase,
    private val getCafeSeatsUseCase: GetCafeSeatsUseCase,
    private val autoAssignSeatUseCase: AutoAssignSeatUseCase,
    private val reserveSeatUseCase: ReserveSeatUseCase,
    private val startSessionUseCase: StartSessionUseCase,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : ViewModel() {

    private val _state = MutableStateFlow<CafeDetailUiState>(CafeDetailUiState.Idle)
    val state: StateFlow<CafeDetailUiState> = _state.asStateFlow()

    private val _seats = MutableStateFlow<List<SeatResponseDto>>(emptyList())
    val seats: StateFlow<List<SeatResponseDto>> = _seats.asStateFlow()

    private val _session = MutableStateFlow<SessionResponseDto?>(null)
    val session: StateFlow<SessionResponseDto?> = _session.asStateFlow()

    private val _events = Channel<String>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    fun loadCafeDetail(cafeId: Long) {
        viewModelScope.launch(ioDispatcher) {
            _state.value = CafeDetailUiState.Loading
            when (val res = getCafeDetailUseCase(cafeId)) {
                is ApiResult.Success -> {
                    val cafe = res.data
                    if (cafe != null) {
                        _state.value = CafeDetailUiState.Success(cafe)
                    } else {
                        _state.value = CafeDetailUiState.Error("카페 정보가 없습니다.")
                        _events.send("카페 정보가 없습니다.")
                    }
                }
                is ApiResult.Failure -> {
                    _state.value = CafeDetailUiState.Error(res.message ?: "카페 상세 조회 실패")
                    _events.send(res.message ?: "카페 상세 조회 실패")
                }
            }
        }
    }

    fun loadSeats(cafeId: Long, status: String? = null) {
        viewModelScope.launch(ioDispatcher) {
            when (val res = getCafeSeatsUseCase(cafeId, status)) {
                is ApiResult.Success -> _seats.value = res.data ?: emptyList()
                is ApiResult.Failure -> _events.send(res.message ?: "좌석 조회 실패")
            }
        }
    }

    fun autoAssignSeat(cafeId: Long) {
        viewModelScope.launch(ioDispatcher) {
            when (val res = autoAssignSeatUseCase(cafeId)) {
                is ApiResult.Success -> {
                    _session.value = res.data
                    _events.send("좌석이 자동 배정되었습니다.")
                }
                is ApiResult.Failure -> _events.send(res.message ?: "자동 배정 실패")
            }
        }
    }

    fun reserveSeat(cafeId: Long, request: ReservationRequest) {
        viewModelScope.launch(ioDispatcher) {
            when (val res = reserveSeatUseCase(cafeId, request)) {
                is ApiResult.Success -> {
                    _session.value = res.data
                    _events.send("좌석 예약이 완료되었습니다.")
                }
                is ApiResult.Failure -> _events.send(res.message ?: "예약 실패")
            }
        }
    }

    fun startSession(request: StartSessionRequest) {
        viewModelScope.launch(ioDispatcher) {
            when (val res = startSessionUseCase(request)) {
                is ApiResult.Success -> {
                    _session.value = res.data
                    _events.send("이용이 시작되었습니다.")
                }
                is ApiResult.Failure -> _events.send(res.message ?: "이용 시작 실패")
            }
        }
    }
}