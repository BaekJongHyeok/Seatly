package kr.jiyeok.seatly.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kr.jiyeok.seatly.data.remote.response.SeatDto
import kr.jiyeok.seatly.data.remote.response.StudyCafeDetailDto
import kr.jiyeok.seatly.data.repository.ApiResult
import kr.jiyeok.seatly.domain.usecase.GetCafeDetailUseCase
import kr.jiyeok.seatly.domain.usecase.GetCafeSeatsUseCase
import kr.jiyeok.seatly.di.IoDispatcher
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject

/**
 * Cafe Detail ViewModel
 * 
 * 역할:
 * - 카페 상세 정보 조회
 * - 카페 좌석 관리
 * - 좌석 예약/자동 배정 처리
 * - 세션 시작 처리
 * 
 * UI는 StateFlow를 통해 상태를 관찰하고,
 * 에러/이벤트는 [events] Channel을 통해 수신합니다
 */

/**
 * 카페 상세 UI 상태
 */
sealed interface CafeDetailUiState {
    object Idle : CafeDetailUiState
    object Loading : CafeDetailUiState
    data class Success(val message: String = "") : CafeDetailUiState
    data class Error(val message: String) : CafeDetailUiState
}

@HiltViewModel
class CafeDetailViewModel @Inject constructor(
    private val getCafeDetailUseCase: GetCafeDetailUseCase,
    private val getCafeSeatsUseCase: GetCafeSeatsUseCase,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : ViewModel() {

    // =====================================================
    // State Management
    // =====================================================

    /**
     * 카페 상세 UI 상태
     * Idle → Loading → Success/Error
     */
    private val _uiState = MutableStateFlow<CafeDetailUiState>(CafeDetailUiState.Idle)
    val uiState: StateFlow<CafeDetailUiState> = _uiState.asStateFlow()

    /**
     * 카페 상세 정보
     */
    private val _cafeDetail = MutableStateFlow<StudyCafeDetailDto?>(null)
    val cafeDetail: StateFlow<StudyCafeDetailDto?> = _cafeDetail.asStateFlow()

    /**
     * 카페 좌석 목록
     */
    private val _seats = MutableStateFlow<List<SeatDto>>(emptyList())
    val seats: StateFlow<List<SeatDto>> = _seats.asStateFlow()

    /**
     * 선택된 좌석
     */
    private val _selectedSeat = MutableStateFlow<SeatDto?>(null)
    val selectedSeat: StateFlow<SeatDto?> = _selectedSeat.asStateFlow()

    /**
     * 로딩 상태
     */
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    /**
     * 에러 메시지
     */
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    /**
     * 에러/이벤트 메시지 Channel
     * UI에서 토스트 메시지나 스낵바로 표시
     */
    private val _events = Channel<String>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    // =====================================================
    // Public Methods
    // =====================================================

    /**
     * 카페 상세 정보 로드
     */
    fun loadCafeDetail(cafeId: Long) {
        viewModelScope.launch(ioDispatcher) {
            _isLoading.value = true
            _uiState.value = CafeDetailUiState.Loading
            _error.value = null
            try {
                when (val result = getCafeDetailUseCase(cafeId)) {
                    is ApiResult.Success -> {
                        _cafeDetail.value = result.data
                        _uiState.value = CafeDetailUiState.Success("카페 정보 로드 완료")
                    }
                    is ApiResult.Failure -> {
                        _error.value = result.message ?: "카페 상세 조회 실패"
                        _uiState.value = CafeDetailUiState.Error(result.message ?: "카페 상세 조회 실패")
                        _events.send(result.message ?: "카페 상세 조회 실패")
                    }
                }
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * 카페 좌석 목록 로드
     */
    fun loadCafeSeats(cafeId: Long) {
        viewModelScope.launch(ioDispatcher) {
            _isLoading.value = true
            _error.value = null
            try {
                when (val result = getCafeSeatsUseCase(cafeId)) {
                    is ApiResult.Success -> {
                        _seats.value = result.data ?: emptyList()
                        _uiState.value = CafeDetailUiState.Success("좌석 정보 로드 완료")
                    }
                    is ApiResult.Failure -> {
                        _error.value = result.message ?: "좌석 조회 실패"
                        _uiState.value = CafeDetailUiState.Error(result.message ?: "좌석 조회 실패")
                        _events.send(result.message ?: "좌석 조회 실패")
                    }
                }
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * 좌석 선택
     */
    suspend fun selectSeat(seat: SeatDto) {
        // 좌석 가용성 확인
        if (seat.status?.equals("AVAILABLE", ignoreCase = true) == true) {
            _selectedSeat.value = seat
            _events.send("좌석 ${seat.name}이 선택되었습니다")
        } else {
            _error.value = "사용 불가능한 좌석입니다"
            _events.send("사용 불가능한 좌석입니다")
        }
    }

    /**
     * 선택된 좌석 해제
     */
    fun deselectSeat() {
        _selectedSeat.value = null
    }

    /**
     * 사용 가능한 좌석 필터링
     */
    fun getAvailableSeats(): List<SeatDto> {
        return _seats.value.filter { 
            it.status?.equals("AVAILABLE", ignoreCase = true) == true 
        }
    }

    /**
     * 에러 초기화
     */
    fun clearError() {
        _error.value = null
    }

    /**
     * 상태 초기화
     */
    fun resetState() {
        _uiState.value = CafeDetailUiState.Idle
        _error.value = null
        _selectedSeat.value = null
    }
}
