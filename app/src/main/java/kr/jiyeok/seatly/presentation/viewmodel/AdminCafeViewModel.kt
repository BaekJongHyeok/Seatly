package kr.jiyeok.seatly.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kr.jiyeok.seatly.data.remote.response.PageResponse
import kr.jiyeok.seatly.data.remote.response.StudyCafeSummaryDto
import kr.jiyeok.seatly.data.remote.response.StudyCafeDetailDto
import kr.jiyeok.seatly.data.repository.ApiResult
import kr.jiyeok.seatly.domain.usecase.GetStudyCafesUseCase
import kr.jiyeok.seatly.domain.usecase.GetCafeDetailUseCase
import kr.jiyeok.seatly.di.IoDispatcher
import kotlinx.coroutines.CoroutineDispatcher
import kr.jiyeok.seatly.data.remote.request.CreateSeatRequest
import kr.jiyeok.seatly.data.remote.request.UpdateSeatRequest
import okhttp3.RequestBody
import javax.inject.Inject

/**
 * Admin Cafe Management ViewModel
 * 
 * 역할:
 * - 관리자 카페 목록 관리
 * - 카페 CRUD 작업 (생성, 수정, 삭제)
 * - 카페 좌석 관리 (추가, 수정, 삭제)
 * - 카페 내 세션 강제 종료
 * - 카페 내 사용자 관리
 * 
 * UI는 StateFlow를 통해 상태를 관찰하고,
 * 에러/이벤트는 [events] Channel을 통해 수신합니다
 */

/**
 * 관리자 UI 상태
 */
sealed interface AdminUiState {
    object Idle : AdminUiState
    object Loading : AdminUiState
    data class Success(val message: String = "") : AdminUiState
    data class Error(val message: String) : AdminUiState
}

@HiltViewModel
class AdminCafeViewModel @Inject constructor(
    private val getStudyCafesUseCase: GetStudyCafesUseCase,
    private val getCafeDetailUseCase: GetCafeDetailUseCase,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : ViewModel() {

    // =====================================================
    // State Management
    // =====================================================

    /**
     * 관리자 UI 상태
     * Idle → Loading → Success/Error
     */
    private val _uiState = MutableStateFlow<AdminUiState>(AdminUiState.Idle)
    val uiState: StateFlow<AdminUiState> = _uiState.asStateFlow()

    /**
     * 관리 중인 카페 목록
     */
    private val _cafeListPage = MutableStateFlow<PageResponse<StudyCafeSummaryDto>?>(null)
    val cafeListPage: StateFlow<PageResponse<StudyCafeSummaryDto>?> = _cafeListPage.asStateFlow()

    /**
     * 선택된 카페 상세 정보
     */
    private val _selectedCafeDetail = MutableStateFlow<StudyCafeDetailDto?>(null)
    val selectedCafeDetail: StateFlow<StudyCafeDetailDto?> = _selectedCafeDetail.asStateFlow()

    /**
     * 현재 페이지 번호
     */
    private val _currentPage = MutableStateFlow(0)
    val currentPage: StateFlow<Int> = _currentPage.asStateFlow()

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
    // Public Methods - Cafe Management
    // =====================================================

    /**
     * 관리자용 카페 목록 로드
     */
    fun loadMyCafes(page: Int = 0, size: Int = 20, search: String? = null) {
        viewModelScope.launch(ioDispatcher) {
            _isLoading.value = true
            _uiState.value = AdminUiState.Loading
            _error.value = null
            try {
                when (val result = getStudyCafesUseCase(page, size)) {
                    is ApiResult.Success -> {
                        _cafeListPage.value = result.data
                        _currentPage.value = page
                        _uiState.value = AdminUiState.Success("카페 목록 로드 완료")
                    }
                    is ApiResult.Failure -> {
                        _error.value = result.message ?: "카페 목록 조회 실패"
                        _uiState.value = AdminUiState.Error(result.message ?: "카페 목록 조회 실패")
                        _events.send(result.message ?: "카페 목록 조회 실패")
                    }
                }
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * 카페 상세 정보 로드
     */
    fun loadCafeDetail(cafeId: Long) {
        viewModelScope.launch(ioDispatcher) {
            _isLoading.value = true
            _uiState.value = AdminUiState.Loading
            _error.value = null
            try {
                when (val result = getCafeDetailUseCase(cafeId)) {
                    is ApiResult.Success -> {
                        _selectedCafeDetail.value = result.data
                        _uiState.value = AdminUiState.Success("카페 상세 정보 로드 완료")
                    }
                    is ApiResult.Failure -> {
                        _error.value = result.message ?: "카페 상세 조회 실패"
                        _uiState.value = AdminUiState.Error(result.message ?: "카페 상세 조회 실패")
                        _events.send(result.message ?: "카페 상세 조회 실패")
                    }
                }
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * 다음 페이지 로드
     */
    fun loadNextPage(size: Int = 20) {
        val nextPage = _currentPage.value + 1
        loadMyCafes(nextPage, size)
    }

    /**
     * 이전 페이지 로드
     */
    fun loadPreviousPage(size: Int = 20) {
        val previousPage = maxOf(0, _currentPage.value - 1)
        loadMyCafes(previousPage, size)
    }

    /**
     * 카페 생성 (다른 레이어의 API 호출)
     * 실제 구현은 Repository에서 처리
     * 
     * 참고: 이미지 업로드는 multipart/form-data로 처리되어야 함
     */
    fun createCafe(
        name: String,
        address: String,
        description: String? = null,
        imageUrl: String? = null
    ) {
        viewModelScope.launch(ioDispatcher) {
            _isLoading.value = true
            _uiState.value = AdminUiState.Loading
            _error.value = null
            try {
                // TODO: createCafeUseCase 호출 시 실제 인자 전달
                _uiState.value = AdminUiState.Success("카페가 생성되었습니다")
                _events.send("카페가 생성되었습니다")
            } catch (e: Exception) {
                _error.value = e.message ?: "카페 생성 실패"
                _uiState.value = AdminUiState.Error(e.message ?: "카페 생성 실패")
                _events.send(e.message ?: "카페 생성 실패")
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * 카페 정보 수정
     */
    fun updateCafe(cafeId: Long, name: String?, address: String?, description: String?) {
        viewModelScope.launch(ioDispatcher) {
            _isLoading.value = true
            _uiState.value = AdminUiState.Loading
            _error.value = null
            try {
                // TODO: updateCafeUseCase 호출
                _uiState.value = AdminUiState.Success("카페가 수정되었습니다")
                _events.send("카페가 수정되었습니다")
            } catch (e: Exception) {
                _error.value = e.message ?: "카페 수정 실패"
                _uiState.value = AdminUiState.Error(e.message ?: "카페 수정 실패")
                _events.send(e.message ?: "카페 수정 실패")
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * 카페 삭제
     */
    fun deleteCafe(cafeId: Long) {
        viewModelScope.launch(ioDispatcher) {
            _isLoading.value = true
            _uiState.value = AdminUiState.Loading
            _error.value = null
            try {
                // TODO: deleteCafeUseCase 호출
                _uiState.value = AdminUiState.Success("카페가 삭제되었습니다")
                _events.send("카페가 삭제되었습니다")
            } catch (e: Exception) {
                _error.value = e.message ?: "카페 삭제 실패"
                _uiState.value = AdminUiState.Error(e.message ?: "카페 삭제 실패")
                _events.send(e.message ?: "카페 삭제 실패")
            } finally {
                _isLoading.value = false
            }
        }
    }

    // =====================================================
    // Public Methods - Seat Management
    // =====================================================

    /**
     * 좌석 추가
     */
    fun addSeat(cafeId: Long, request: CreateSeatRequest) {
        viewModelScope.launch(ioDispatcher) {
            try {
                // TODO: addSeatUseCase 호출
                _events.send("좌석이 추가되었습니다")
            } catch (e: Exception) {
                _events.send(e.message ?: "좌석 추가 실패")
            }
        }
    }

    /**
     * 좌석 수정
     */
    fun editSeat(cafeId: Long, seatId: Long, request: UpdateSeatRequest) {
        viewModelScope.launch(ioDispatcher) {
            try {
                // TODO: editSeatUseCase 호출
                _events.send("좌석이 수정되었습니다")
            } catch (e: Exception) {
                _events.send(e.message ?: "좌석 수정 실패")
            }
        }
    }

    /**
     * 좌석 삭제
     */
    fun deleteSeat(cafeId: Long, seatId: Long) {
        viewModelScope.launch(ioDispatcher) {
            try {
                // TODO: deleteSeatUseCase 호출
                _events.send("좌석이 삭제되었습니다")
            } catch (e: Exception) {
                _events.send(e.message ?: "좌석 삭제 실패")
            }
        }
    }

    // =====================================================
    // Public Methods - Session & User Management
    // =====================================================

    /**
     * 세션 강제 종료
     */
    fun forceEndSession(sessionId: Long) {
        viewModelScope.launch(ioDispatcher) {
            try {
                // TODO: forceEndSessionUseCase 호출
                _events.send("세션이 강제 종료되었습니다")
            } catch (e: Exception) {
                _events.send(e.message ?: "세션 강제 종료 실패")
            }
        }
    }

    /**
     * 카페 내 사용자 삭제/탈퇴 처리
     */
    fun deleteUserFromCafe(cafeId: Long, userId: Long) {
        viewModelScope.launch(ioDispatcher) {
            try {
                // TODO: deleteUserFromCafeUseCase 호출
                _events.send("사용자가 탈퇴 처리되었습니다")
            } catch (e: Exception) {
                _events.send(e.message ?: "사용자 삭제 실패")
            }
        }
    }

    // =====================================================
    // Helper Methods
    // =====================================================

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
        _uiState.value = AdminUiState.Idle
        _error.value = null
    }

    /**
     * 선택된 카페 초기화
     */
    fun deselectCafe() {
        _selectedCafeDetail.value = null
    }
}
