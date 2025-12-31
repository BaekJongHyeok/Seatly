package kr.jiyeok.seatly.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kr.jiyeok.seatly.data.remote.response.CurrentCafeUsageDto
import kr.jiyeok.seatly.data.remote.response.PageResponse
import kr.jiyeok.seatly.data.remote.response.SessionResponseDto
import kr.jiyeok.seatly.data.remote.response.StudyCafeSummaryDto
import kr.jiyeok.seatly.data.repository.ApiResult
import kr.jiyeok.seatly.domain.usecase.favorite.GetFavoriteCafesUseCase
import kr.jiyeok.seatly.domain.usecase.favorite.GetRecentCafesUseCase
import kr.jiyeok.seatly.domain.usecase.session.EndSessionUseCase
import kr.jiyeok.seatly.domain.usecase.session.GetCurrentSessionsUseCase
import kr.jiyeok.seatly.domain.usecase.user.GetCurrentCafeUsageUseCase
import kr.jiyeok.seatly.di.IoDispatcher
import javax.inject.Inject

/**
 * Home screen ViewModel: orchestrates loading current usage, favorite cafes and recent cafes.
 *
 * Exposes simple StateFlows for the UI. Errors are emitted via [events] channel as simple strings.
 */
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val getCurrentCafeUsageUseCase: GetCurrentCafeUsageUseCase,
    private val getFavoriteCafesUseCase: GetFavoriteCafesUseCase,
    private val getRecentCafesUseCase: GetRecentCafesUseCase,
    private val getCurrentSessionsUseCase: GetCurrentSessionsUseCase,
    private val endSessionUseCase: EndSessionUseCase,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : ViewModel() {

    private val _currentUsage = MutableStateFlow<CurrentCafeUsageDto?>(null)
    val currentUsage: StateFlow<CurrentCafeUsageDto?> = _currentUsage.asStateFlow()

    private val _favoritePage = MutableStateFlow<PageResponse<StudyCafeSummaryDto>?>(null)
    val favoritePage: StateFlow<PageResponse<StudyCafeSummaryDto>?> = _favoritePage.asStateFlow()

    private val _recentCafes = MutableStateFlow<List<StudyCafeSummaryDto>>(emptyList())
    val recentCafes: StateFlow<List<StudyCafeSummaryDto>> = _recentCafes.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _events = Channel<String>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    /**
     * Load all data needed by the Home screen.
     */
    fun loadHomeData() {
        viewModelScope.launch(ioDispatcher) {
            _isLoading.value = true

            // current usage
            when (val r = getCurrentCafeUsageUseCase()) {
                is ApiResult.Success -> _currentUsage.value = r.data
                is ApiResult.Failure -> _events.send(r.message ?: "현재 이용 정보 조회 실패")
            }

            // favorites (small page)
            when (val r = getFavoriteCafesUseCase(0, 10)) {
                is ApiResult.Success -> _favoritePage.value = r.data
                is ApiResult.Failure -> _events.send(r.message ?: "찜한 카페 조회 실패")
            }

            // recent
            when (val r = getRecentCafesUseCase(10)) {
                is ApiResult.Success -> _recentCafes.value = r.data ?: emptyList()
                is ApiResult.Failure -> _events.send(r.message ?: "최근 이용 카페 조회 실패")
            }

            _isLoading.value = false
        }
    }

    /**
     * Try to end the current (active) session. This implementation:
     *  - fetches current sessions
     *  - finds the first ACTIVE session and ends it
     *  - refreshes current usage afterwards
     *
     * If backend provides direct session id in current-usage endpoint, you can simplify this.
     */
    fun endCurrentUsage() {
        viewModelScope.launch(ioDispatcher) {
            _isLoading.value = true
            try {
                when (val sessionsRes = getCurrentSessionsUseCase(null)) {
                    is ApiResult.Success -> {
                        val sessions = sessionsRes.data ?: emptyList()
                        val active = sessions.firstOrNull { it.status.equals("ACTIVE", ignoreCase = true) }
                            ?: sessions.firstOrNull() // fallback to first session if no explicit ACTIVE
                        if (active != null) {
                            when (val endRes = endSessionUseCase(active.id)) {
                                is ApiResult.Success -> {
                                    _events.send("이용이 종료되었습니다.")
                                    // refresh current usage
                                    when (val r = getCurrentCafeUsageUseCase()) {
                                        is ApiResult.Success -> _currentUsage.value = r.data
                                        is ApiResult.Failure -> _currentUsage.value = null
                                    }
                                }
                                is ApiResult.Failure -> _events.send(endRes.message ?: "이용 종료 실패")
                            }
                        } else {
                            _events.send("종료 가능한 세션이 없습니다.")
                        }
                    }
                    is ApiResult.Failure -> _events.send(sessionsRes.message ?: "세션 조회 실패")
                }
            } catch (t: Throwable) {
                _events.send(t.localizedMessage ?: "알 수 없는 오류")
            } finally {
                _isLoading.value = false
            }
        }
    }
}