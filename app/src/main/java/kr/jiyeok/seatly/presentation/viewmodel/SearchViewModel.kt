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
import kr.jiyeok.seatly.data.remote.response.StudyCafeSummaryDto
import kr.jiyeok.seatly.data.repository.ApiResult
import kr.jiyeok.seatly.di.IoDispatcher
import kr.jiyeok.seatly.domain.usecase.AddFavoriteCafeUseCase
import kr.jiyeok.seatly.domain.usecase.GetStudyCafesUseCase
import kr.jiyeok.seatly.domain.usecase.RemoveFavoriteCafeUseCase
import javax.inject.Inject

/**
 * Search Screen ViewModel
 *
 * 역할:
 * - 스터디 카페 검색 및 필터링
 * - 카페 목록 페이징 관리
 * - 즐겨찾기 추가/제거
 */
@HiltViewModel
class SearchViewModel @Inject constructor(
    private val getStudyCafesUseCase: GetStudyCafesUseCase,
    private val addFavoriteCafeUseCase: AddFavoriteCafeUseCase,
    private val removeFavoriteCafeUseCase: RemoveFavoriteCafeUseCase,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : ViewModel() {

    // =====================================================
    // State Management
    // =====================================================

    // 전체 카페 목록 (서버 원본)
    private val _allCafes = MutableStateFlow<List<StudyCafeSummaryDto>>(emptyList())

    // 필터링된 카페 목록 (UI 표시용)
    private val _filteredCafes = MutableStateFlow<List<StudyCafeSummaryDto>>(emptyList())
    val filteredCafes: StateFlow<List<StudyCafeSummaryDto>> = _filteredCafes.asStateFlow()

    // 검색어
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    // 로딩 상태
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // 에러 상태
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    // 즐겨찾기 카페 ID 목록
    private val _favoriteCafeIds = MutableStateFlow<List<Long>>(emptyList())
    val favoriteCafeIds: StateFlow<List<Long>> = _favoriteCafeIds.asStateFlow()

    // One-shot Event (Toast 등)
    private val _events = Channel<String>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    // =====================================================
    // Initialization
    // =====================================================

    init {
        loadCafes()
    }

    // =====================================================
    // Public Methods
    // =====================================================

    /**
     * 검색어 입력 시 호출
     */
    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
        filterCafes(query)
    }

    /**
     * 카페 목록 데이터 로드
     */
    fun loadCafes() {
        viewModelScope.launch(ioDispatcher) {
            _isLoading.value = true
            _error.value = null

            when (val result = getStudyCafesUseCase()) {
                is ApiResult.Success -> {
                    val cafeList = result.data ?: emptyList()
                    _allCafes.value = cafeList

                    // 현재 검색어 기준으로 필터링 적용
                    filterCafes(_searchQuery.value)
                }
                is ApiResult.Failure -> {
                    val msg = result.message ?: "카페 목록 로드 실패"
                    _error.value = msg
                    _events.send(msg)
                }
            }
            _isLoading.value = false
        }
    }

    /**
     * 즐겨찾기 토글 (추가/삭제)
     * - 낙관적 업데이트(Optimistic Update) 적용: UI를 먼저 갱신하고 API 호출
     */
    fun toggleFavoriteCafe(cafeId: Long) {
        val currentFavorites = _favoriteCafeIds.value.toMutableList()
        val isCurrentlyFavorite = currentFavorites.contains(cafeId)

        // 1. UI 즉시 업데이트 (반응성 향상)
        if (isCurrentlyFavorite) {
            currentFavorites.remove(cafeId)
            _events.trySend("즐겨찾기에서 제거되었습니다") // Optional: 제거 메시지
        } else {
            currentFavorites.add(cafeId)
            _events.trySend("즐겨찾기에 추가되었습니다") // Optional: 추가 메시지
        }
        _favoriteCafeIds.value = currentFavorites

        // 2. 백그라운드 API 호출
        viewModelScope.launch(ioDispatcher) {
            val result = if (isCurrentlyFavorite) {
                removeFavoriteCafeUseCase(cafeId)
            } else {
                addFavoriteCafeUseCase(cafeId)
            }

            // API 실패 시 롤백 (선택 사항 - 여기선 에러 메시지만 표시)
            if (result is ApiResult.Failure) {
                _events.send(result.message ?: "요청 처리에 실패했습니다")

                // 필요 시 여기서 UI 상태를 원래대로 되돌리는 로직 추가 가능
                // rollbackFavoriteState(cafeId, !isCurrentlyFavorite)
            }
        }
    }

    // =====================================================
    // Private Helper Methods
    // =====================================================

    /**
     * 로컬 필터링 로직
     */
    private fun filterCafes(query: String) {
        if (query.isBlank()) {
            _filteredCafes.value = _allCafes.value
        } else {
            val lowerQuery = query.lowercase()
            _filteredCafes.value = _allCafes.value.filter { cafe ->
                val name = cafe.name?.lowercase() ?: ""
                val address = cafe.address?.lowercase() ?: ""
                name.contains(lowerQuery) || address.contains(lowerQuery)
            }
        }
    }
}
