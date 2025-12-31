package kr.jiyeok.seatly.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kr.jiyeok.seatly.data.remote.response.StudyCafeSummaryDto
import kr.jiyeok.seatly.data.repository.ApiResult
import kr.jiyeok.seatly.domain.usecase.cafe.GetStudyCafesUseCase
import kr.jiyeok.seatly.di.IoDispatcher
import javax.inject.Inject

/**
 * ViewModel for SearchScreen that fetches and manages study cafe list.
 */
@HiltViewModel
class SearchViewModel @Inject constructor(
    private val getStudyCafesUseCase: GetStudyCafesUseCase,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : ViewModel() {

    private val _cafes = MutableStateFlow<List<StudyCafeSummaryDto>>(emptyList())
    val cafes: StateFlow<List<StudyCafeSummaryDto>> = _cafes.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    init {
        loadCafes()
    }

    /**
     * Load all cafes from the API.
     */
    fun loadCafes(
        page: Int = 0,
        size: Int = 100,  // Get many cafes for search screen
        search: String? = null,
        amenities: String? = null,
        openNow: Boolean? = null,
        sort: String? = null,
        lat: Double? = null,
        lng: Double? = null
    ) {
        viewModelScope.launch(ioDispatcher) {
            _isLoading.value = true
            _error.value = null

            when (val result = getStudyCafesUseCase(page, size, search, amenities, openNow, sort, lat, lng)) {
                is ApiResult.Success -> {
                    _cafes.value = result.data?.content ?: emptyList()
                }
                is ApiResult.Failure -> {
                    _error.value = result.message ?: "카페 목록을 불러오지 못했습니다"
                }
            }

            _isLoading.value = false
        }
    }

    /**
     * Refresh cafe list.
     */
    fun refresh() {
        loadCafes()
    }
}
