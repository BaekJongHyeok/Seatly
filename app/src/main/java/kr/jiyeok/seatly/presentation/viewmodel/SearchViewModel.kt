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
 * Search Screen ViewModel
 * 
 * 역할:
 * - 스터디 카페 검색 및 필터링
 * - 카페 목록 페이징 관리
 * - 즐겨찾기 추가/제거
 * 
 * UI는 StateFlow를 통해 데이터를 관찰하고,
 * 에러는 [events] Channel을 통해 수신합니다
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

    /**
     * 전체 카페 목록 (필터링 전)
     */
    private val _allCafes = MutableStateFlow<List<StudyCafeSummaryDto>>(emptyList())

    /**
     * 필터링된 카페 목록 (검색 결과)
     */
    private val _filteredCafes = MutableStateFlow<List<StudyCafeSummaryDto>>(emptyList())
    val filteredCafes: StateFlow<List<StudyCafeSummaryDto>> = _filteredCafes.asStateFlow()

    /**
     * 페이지 정보
     */
    private val _pageInfo = MutableStateFlow<PageResponse<StudyCafeSummaryDto>?>(null)
    val pageInfo: StateFlow<PageResponse<StudyCafeSummaryDto>?> = _pageInfo.asStateFlow()

    /**
     * 검색 쿼리
     */
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

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
    // Initialization
    // =====================================================

    init {
        loadCafes()
    }

    // =====================================================
    // Public Methods
    // =====================================================

    /**
     * 검색 쿼리 업데이트 및 필터링
     */
    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
        filterCafes(query)
    }

    /**
     * 카페 목록 로드
     * 
     * 기본 페이지 크기: 100 (검색 화면용)
     */
    fun loadCafes(page: Int = 0, size: Int = 100) {
        viewModelScope.launch(ioDispatcher) {
            _isLoading.value = true
            _error.value = null
            try {
                when (val result = getStudyCafesUseCase(page, size)) {
                    is ApiResult.Success -> {
                        val pageResponse = result.data
                        _pageInfo.value = pageResponse

                        // 전체 카페 목록 저장
                        val cafeList = pageResponse?.content ?: emptyList()
                        _allCafes.value = cafeList

                        // 즐겨찾기 ID 추출
                        val favoriteIds = cafeList
                            .filter { it.id in _favoriteCafeIds.value }
                            .map { it.id }
                        _favoriteCafeIds.value = favoriteIds

                        // 현재 검색 쿼리 적용
                        filterCafes(_searchQuery.value)
                    }
                    is ApiResult.Failure -> {
                        _error.value = result.message ?: "카페 목록을 불러오지 못했습니다"
                        _events.send(result.message ?: "카페 목록 로드 실패")
                    }
                }
            } finally {
                _isLoading.value = false
            }
        }
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
     * 카페 목록 새로고침
     */
    fun refresh() {
        _searchQuery.value = ""
        loadCafes()
    }

    // =====================================================
    // Private Helper Methods
    // =====================================================

    /**
     * 검색 쿼리를 기반으로 카페 필터링
     * 
     * 카페 이름과 주소에서 검색
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
