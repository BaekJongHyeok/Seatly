package kr.jiyeok.seatly.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kr.jiyeok.seatly.data.remote.request.AddSeatRequest
import kr.jiyeok.seatly.data.remote.request.EditSeatRequest
import kr.jiyeok.seatly.data.remote.response.PageResponse
import kr.jiyeok.seatly.data.remote.response.SeatResponseDto
import kr.jiyeok.seatly.data.remote.response.StudyCafeDetailDto
import kr.jiyeok.seatly.data.remote.response.StudyCafeSummaryDto
import kr.jiyeok.seatly.data.repository.ApiResult
import kr.jiyeok.seatly.domain.usecase.admin.*
import okhttp3.MultipartBody
import okhttp3.RequestBody
import javax.inject.Inject

sealed interface AdminUiState {
    object Idle : AdminUiState
    object Loading : AdminUiState
    data class CafesList(val page: PageResponse<StudyCafeSummaryDto>) : AdminUiState
    data class CafeDetail(val detail: StudyCafeDetailDto) : AdminUiState
    data class Success(val message: String) : AdminUiState
    data class Error(val message: String) : AdminUiState
}

@HiltViewModel
class AdminCafeViewModel @Inject constructor(
    private val getAdminCafesUseCase: GetAdminCafesUseCase,
    private val getAdminCafeDetailUseCase: GetAdminCafeDetailUseCase,
    private val createCafeUseCase: CreateCafeUseCase,
    private val updateCafeUseCase: UpdateCafeUseCase,
    private val deleteCafeUseCase: DeleteCafeUseCase,
    private val addSeatUseCase: AddSeatUseCase,
    private val editSeatUseCase: EditSeatUseCase,
    private val deleteSeatUseCase: DeleteSeatUseCase,
    private val forceEndSessionUseCase: ForceEndSessionUseCase,
    private val deleteUserFromCafeUseCase: DeleteUserFromCafeUseCase
) : ViewModel() {

    private val _state = MutableStateFlow<AdminUiState>(AdminUiState.Idle)
    val state: StateFlow<AdminUiState> = _state.asStateFlow()

    private val _events = Channel<String>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    fun loadCafes(page: Int = 0, size: Int = 20, search: String? = null) {
        viewModelScope.launch {
            _state.value = AdminUiState.Loading
            when (val res = getAdminCafesUseCase(page, size, search)) {
                is ApiResult.Success -> {
                    val pageResp = res.data ?: PageResponse(emptyList(), page, size, 0, 0)
                    _state.value = AdminUiState.CafesList(pageResp)
                }
                is ApiResult.Failure -> {
                    _state.value = AdminUiState.Error(res.message ?: "카페 목록 조회 실패")
                    _events.send(res.message ?: "카페 목록 조회 실패")
                }
            }
        }
    }

    fun loadCafeDetail(cafeId: Long) {
        viewModelScope.launch {
            _state.value = AdminUiState.Loading
            when (val res = getAdminCafeDetailUseCase(cafeId)) {
                is ApiResult.Success -> {
                    val detail = res.data
                    if (detail != null) {
                        _state.value = AdminUiState.CafeDetail(detail)
                    } else {
                        _state.value = AdminUiState.Error("카페 상세 없음")
                        _events.send("카페 상세 없음")
                    }
                }
                is ApiResult.Failure -> {
                    _state.value = AdminUiState.Error(res.message ?: "카페 상세 조회 실패")
                    _events.send(res.message ?: "카페 상세 조회 실패")
                }
            }
        }
    }

    fun createCafe(parts: Map<String, RequestBody>, images: List<MultipartBody.Part> = emptyList()) {
        viewModelScope.launch {
            _state.value = AdminUiState.Loading
            when (val res = createCafeUseCase(parts, images)) {
                is ApiResult.Success -> {
                    _state.value = AdminUiState.Success("카페가 생성되었습니다.")
                    _events.send("카페가 생성되었습니다.")
                }
                is ApiResult.Failure -> {
                    _state.value = AdminUiState.Error(res.message ?: "카페 생성 실패")
                    _events.send(res.message ?: "카페 생성 실패")
                }
            }
        }
    }

    fun updateCafe(cafeId: Long, parts: Map<String, RequestBody>, images: List<MultipartBody.Part> = emptyList()) {
        viewModelScope.launch {
            _state.value = AdminUiState.Loading
            when (val res = updateCafeUseCase(cafeId, parts, images)) {
                is ApiResult.Success -> {
                    _state.value = AdminUiState.Success("카페가 수정되었습니다.")
                    _events.send("카페가 수정되었습니다.")
                }
                is ApiResult.Failure -> {
                    _state.value = AdminUiState.Error(res.message ?: "카페 수정 실패")
                    _events.send(res.message ?: "카페 수정 실패")
                }
            }
        }
    }

    fun deleteCafe(cafeId: Long) {
        viewModelScope.launch {
            _state.value = AdminUiState.Loading
            when (val res = deleteCafeUseCase(cafeId)) {
                is ApiResult.Success -> {
                    _state.value = AdminUiState.Success("카페가 삭제되었습니다.")
                    _events.send("카페가 삭제되었습니다.")
                }
                is ApiResult.Failure -> {
                    _state.value = AdminUiState.Error(res.message ?: "카페 삭제 실패")
                    _events.send(res.message ?: "카페 삭제 실패")
                }
            }
        }
    }

    fun addSeat(cafeId: Long, request: AddSeatRequest) {
        viewModelScope.launch {
            when (val res = addSeatUseCase(cafeId, request)) {
                is ApiResult.Success -> _events.send("좌석이 추가되었습니다.")
                is ApiResult.Failure -> _events.send(res.message ?: "좌석 추가 실패")
            }
        }
    }

    fun editSeat(cafeId: Long, seatId: Long, request: EditSeatRequest) {
        viewModelScope.launch {
            when (val res = editSeatUseCase(cafeId, seatId, request)) {
                is ApiResult.Success -> _events.send("좌석이 수정되었습니다.")
                is ApiResult.Failure -> _events.send(res.message ?: "좌석 수정 실패")
            }
        }
    }

    fun deleteSeat(cafeId: Long, seatId: Long) {
        viewModelScope.launch {
            when (val res = deleteSeatUseCase(cafeId, seatId)) {
                is ApiResult.Success -> _events.send("좌석이 삭제되었습니다.")
                is ApiResult.Failure -> _events.send(res.message ?: "좌석 삭제 실패")
            }
        }
    }

    fun forceEndSession(sessionId: Long) {
        viewModelScope.launch {
            when (val res = forceEndSessionUseCase(sessionId)) {
                is ApiResult.Success -> _events.send("세션이 강제 종료되었습니다.")
                is ApiResult.Failure -> _events.send(res.message ?: "세션 강제 종료 실패")
            }
        }
    }

    fun deleteUserFromCafe(cafeId: Long, userId: Long) {
        viewModelScope.launch {
            when (val res = deleteUserFromCafeUseCase(cafeId, userId)) {
                is ApiResult.Success -> _events.send("사용자가 탈퇴 처리되었습니다.")
                is ApiResult.Failure -> _events.send(res.message ?: "사용자 삭제 실패")
            }
        }
    }
}