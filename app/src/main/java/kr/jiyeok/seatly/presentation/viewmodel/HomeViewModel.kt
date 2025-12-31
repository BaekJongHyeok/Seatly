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
     * Fetches current sessions to determine if user has an active cafe usage.
     */
    fun loadHomeData() {
        viewModelScope.launch(ioDispatcher) {
            _isLoading.value = true

            // Fetch current sessions to check if user has an active session
            when (val sessionsResult = getCurrentSessionsUseCase()) {
                is ApiResult.Success -> {
                    val sessions = sessionsResult.data ?: emptyList()
                    // Find the first active session (IN_USE or ASSIGNED status)
                    val activeSession = sessions.firstOrNull { 
                        it.status.equals("IN_USE", ignoreCase = true) || 
                        it.status.equals("ASSIGNED", ignoreCase = true)
                    }
                    
                    if (activeSession != null) {
                        // Convert session to CurrentCafeUsageDto
                        val elapsedMillis = try {
                            val startTime = java.time.Instant.parse(activeSession.startedAt)
                            val now = java.time.Instant.now()
                            java.time.Duration.between(startTime, now).toMillis()
                        } catch (e: Exception) {
                            0L // Default to 0 if parsing fails
                        }
                        
                        _currentUsage.value = CurrentCafeUsageDto(
                            cafeId = activeSession.studyCafeId,
                            cafeName = "스터디카페", // Will be enriched with actual cafe data in real implementation
                            cafeImageUrl = null,
                            cafeAddress = "서울시",
                            seatName = activeSession.seatName,
                            startedAt = activeSession.startedAt,
                            elapsedMillis = elapsedMillis
                        )
                    } else {
                        _currentUsage.value = null
                    }
                }
                is ApiResult.Failure -> {
                    _currentUsage.value = null
                    _events.send(sessionsResult.message ?: "세션 정보 조회 실패")
                }
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
     * Load favorite cafes based on the list of favorite cafe IDs.
     * This is called when favoriteCafeIds changes to sync favorites in real-time.
     */
    fun loadFavoriteCafesFromIds(favoriteIds: List<Long>) {
        viewModelScope.launch(ioDispatcher) {
            if (favoriteIds.isEmpty()) {
                _favoritePage.value = null
                return@launch
            }
            
            // Load favorites from API
            // For now, use the existing getFavoriteCafesUseCase
            // In a real app, you might want a specific endpoint to fetch cafes by IDs
            when (val r = getFavoriteCafesUseCase(0, favoriteIds.size)) {
                is ApiResult.Success -> {
                    // Filter to only include cafes that are in favoriteIds
                    val filteredCafes = r.data?.content?.filter { cafe ->
                        cafe.id in favoriteIds
                    } ?: emptyList()
                    
                    _favoritePage.value = r.data?.copy(content = filteredCafes)
                }
                is ApiResult.Failure -> {
                    // On failure, just keep existing data
                    _events.send(r.message ?: "찜한 카페 조회 실패")
                }
            }
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