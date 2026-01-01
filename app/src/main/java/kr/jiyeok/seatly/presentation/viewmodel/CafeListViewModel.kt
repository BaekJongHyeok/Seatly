package kr.jiyeok.seatly.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kr.jiyeok.seatly.data.remote.response.PageResponse
import kr.jiyeok.seatly.data.remote.response.StudyCafeSummaryDto
import kr.jiyeok.seatly.data.repository.ApiResult
import kr.jiyeok.seatly.domain.usecase.GetStudyCafesUseCase
import kr.jiyeok.seatly.domain.usecase.AddFavoriteCafeUseCase
import kr.jiyeok.seatly.domain.usecase.RemoveFavoriteCafeUseCase
import kr.jiyeok.seatly.di.IoDispatcher
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject

/**
 * Cafe Listing ViewModel
 * 
 * 역할:
 * - 카페 목록 조회 및 검색
 * - 카페 페이징 관리
 * - 즐겨찾기 추가/제거
 * 
 * UI는 StateFlow를 통해 상태를 관찰하고,
 * 에러/이벤트는 [events] Channel을 통해 수신합니다
 */

/**
 * 카페 목록 UI 상태
 */
sealed interface CafeListUiState {
    object Idle : CafeListUiState
    object Loading : CafeListUiState
    data class Success(val message: String = "") : CafeListUiState
    data class Error(val message: String) : CafeListUiState
}

@HiltViewModel
class CafeListViewModel @Inject constructor(
    private val getStudyCafesUseCase: GetStudyCafesUseCase,
    private val addFavoriteCafeUseCase: AddFavoriteCafeUseCase,
    private val removeFavoriteCafeUseCase: RemoveFavoriteCafeUseCase,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : ViewModel() {

    // =====================================================
    // State Management
    // =====================================================

    /**
     * 카페 목록 UI 상태
     * Idle → Loading → Success/Error
     */
    private val _uiState = MutableStateFlow<CafeListUiState>(CafeListUiState.Idle)
    val uiState: StateFlow<CafeListUiState> = _uiState.asStateFlow()

    /**
     * 카페 목록 페이지 정보
     */
    private val _cafeListPage = MutableStateFlow<PageResponse<StudyCafeSummaryDto>?>(null)
    val cafeListPage: StateFlow<PageResponse<StudyCafeSummaryDto>?> = _cafeListPage.asStateFlow()

    /**
     * 현재 검색 쿼리
     */
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

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
     * 즐겨찾기 카페 ID 목록
     */
    private val _favoriteCafeIds = MutableStateFlow<List<Long>>(emptyList())
    val favoriteCafeIds: StateFlow<List<Long>> = _favoriteCafeIds.asStateFlow()

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
     * 카페 목록 검색 및 로드
     */
    fun searchCafes(page: Int = 0, size: Int = 20, search: String? = null) {
        viewModelScope.launch(ioDispatcher) {
            _isLoading.value = true
            _uiState.value = CafeListUiState.Loading
            _error.value = null
            try {
                when (val result = getStudyCafesUseCase(page, size)) {
                    is ApiResult.Success -> {
                        val pageResponse = result.data
                        _cafeListPage.value = pageResponse
                        _currentPage.value = page

                        // 즐겨찾기 ID 추출
                        val favoriteIds = pageResponse?.content
                            ?.filter { it.id in _favoriteCafeIds.value }
                            ?.map { it.id }
                            ?: emptyList()
                        _favoriteCafeIds.value = favoriteIds

                        _uiState.value = CafeListUiState.Success("카페 목록 로드 완료")
                    }
                    is ApiResult.Failure -> {
                        _error.value = result.message ?: "카페 목록 조회 실패"
                        _uiState.value = CafeListUiState.Error(result.message ?: "카페 목록 조회 실패")
                        _events.send(result.message ?: "카페 목록 조회 실패")
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
        searchCafes(nextPage, size, _searchQuery.value)
    }

    /**
     * 이전 페이지 로드
     */
    fun loadPreviousPage(size: Int = 20) {
        val previousPage = maxOf(0, _currentPage.value - 1)
        searchCafes(previousPage, size, _searchQuery.value)
    }

    /**
     * 검색 쿼리 업데이트
     */
    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
        // 검색어 변경 시 첫 페이지부터 다시 로드
        searchCafes(0, 20, query)
    }

    /**
     * 카페를 즐겨찾기에 추가
     */
    fun addFavoriteCafe(cafeId: Long) {
        viewModelScope.launch(ioDispatcher) {
            when (val result = addFavoriteCafeUseCase(cafeId)) {
                is ApiResult.Success -> {
                    val newFavorites = _favoriteCafeIds.value.toMutableList()
                    if (!newFavorites.contains(cafeId)) {
                        newFavorites.add(cafeId)
                        _favoriteCafeIds.value = newFavorites
                    }
                    _events.send("즐겨찾기에 추가되었습니다")
                }
                is ApiResult.Failure -> {
                    _events.send(result.message ?: "즐겨찾기 추가 실패")
                }
            }
        }
    }

    /**
     * 카페를 즐겨찾기에서 제거
     */
    fun removeFavoriteCafe(cafeId: Long) {
        viewModelScope.launch(ioDispatcher) {
            when (val result = removeFavoriteCafeUseCase(cafeId)) {
                is ApiResult.Success -> {
                    val newFavorites = _favoriteCafeIds.value.toMutableList()
                    newFavorites.remove(cafeId)
                    _favoriteCafeIds.value = newFavorites
                    _events.send("즐겨찾기에서 제거되었습니다")
                }
                is ApiResult.Failure -> {
                    _events.send(result.message ?: "즐겨찾기 제거 실패")
                }
            }
        }
    }

    /**
     * 즐겨찾기 토글
     */
    fun toggleFavoriteCafe(cafeId: Long) {
        if (cafeId in _favoriteCafeIds.value) {
            removeFavoriteCafe(cafeId)
        } else {
            addFavoriteCafe(cafeId)
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
        _uiState.value = CafeListUiState.Idle
        _error.value = null
    }
}
