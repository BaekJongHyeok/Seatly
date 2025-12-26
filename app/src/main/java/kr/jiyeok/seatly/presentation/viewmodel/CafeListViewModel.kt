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
import kr.jiyeok.seatly.domain.usecase.cafe.GetCafeSummaryUseCase
import kr.jiyeok.seatly.domain.usecase.cafe.GetStudyCafesUseCase
import kr.jiyeok.seatly.domain.usecase.favorite.*
import kr.jiyeok.seatly.di.IoDispatcher
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject

sealed interface CafeListUiState {
    object Idle : CafeListUiState
    object Loading : CafeListUiState
    data class Success(val data: PageResponse<StudyCafeSummaryDto>) : CafeListUiState
    data class Error(val message: String) : CafeListUiState
}

@HiltViewModel
class CafeListViewModel @Inject constructor(
    private val getStudyCafesUseCase: GetStudyCafesUseCase,
    private val getFavoriteCafesUseCase: GetFavoriteCafesUseCase,
    private val addFavoriteCafeUseCase: AddFavoriteCafeUseCase,
    private val removeFavoriteCafeUseCase: RemoveFavoriteCafeUseCase,
    private val getCafeSummaryUseCase: GetCafeSummaryUseCase,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : ViewModel() {

    private val _state = MutableStateFlow<CafeListUiState>(CafeListUiState.Idle)
    val state: StateFlow<CafeListUiState> = _state.asStateFlow()

    private val _favorites = MutableStateFlow<PageResponse<StudyCafeSummaryDto>?>(null)
    val favorites: StateFlow<PageResponse<StudyCafeSummaryDto>?> = _favorites.asStateFlow()

    private val _events = Channel<String>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    fun searchCafes(
        page: Int = 0,
        size: Int = 20,
        search: String? = null,
        amenities: String? = null,
        openNow: Boolean? = null,
        sort: String? = null,
        lat: Double? = null,
        lng: Double? = null
    ) {
        viewModelScope.launch(ioDispatcher) {
            _state.value = CafeListUiState.Loading
            when (val res = getStudyCafesUseCase(page, size, search, amenities, openNow, sort, lat, lng)) {
                is ApiResult.Success -> {
                    val data = res.data ?: PageResponse(emptyList(), page, size, 0, 0)
                    _state.value = CafeListUiState.Success(data)
                }
                is ApiResult.Failure -> {
                    _state.value = CafeListUiState.Error(res.message ?: "카페 조회 실패")
                    _events.send(res.message ?: "카페 조회 실패")
                }
            }
        }
    }

    fun loadFavorites(page: Int = 0, size: Int = 20) {
        viewModelScope.launch(ioDispatcher) {
            when (val res = getFavoriteCafesUseCase(page, size)) {
                is ApiResult.Success -> {
                    _favorites.value = res.data
                }
                is ApiResult.Failure -> {
                    _events.send(res.message ?: "찜한 카페 로드 실패")
                }
            }
        }
    }

    fun addFavorite(cafeId: Long) {
        viewModelScope.launch(ioDispatcher) {
            when (val res = addFavoriteCafeUseCase(cafeId)) {
                is ApiResult.Success -> _events.send("찜 추가 완료")
                is ApiResult.Failure -> _events.send(res.message ?: "찜 추가 실패")
            }
        }
    }

    fun removeFavorite(cafeId: Long) {
        viewModelScope.launch(ioDispatcher) {
            when (val res = removeFavoriteCafeUseCase(cafeId)) {
                is ApiResult.Success -> _events.send("찜 삭제 완료")
                is ApiResult.Failure -> _events.send(res.message ?: "찜 삭제 실패")
            }
        }
    }

    fun getCafeSummary(cafeId: Long, onResult: (StudyCafeSummaryDto?) -> Unit) {
        viewModelScope.launch(ioDispatcher) {
            when (val res = getCafeSummaryUseCase(cafeId)) {
                is ApiResult.Success -> onResult(res.data)
                is ApiResult.Failure -> {
                    _events.send(res.message ?: "카페 요약 조회 실패")
                    onResult(null)
                }
            }
        }
    }
}