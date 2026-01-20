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
import kr.jiyeok.seatly.data.remote.response.UsageDto
import kr.jiyeok.seatly.data.repository.ApiResult
import kr.jiyeok.seatly.di.IoDispatcher
import kr.jiyeok.seatly.domain.usecase.GetAdminCafesUseCase
import kr.jiyeok.seatly.domain.usecase.GetCafeUsageUseCase
import javax.inject.Inject

@HiltViewModel
class AdminHomeViewModel @Inject constructor(
    private val getAdminCafesUseCase: GetAdminCafesUseCase,
    private val getCafeUsageUseCase: GetCafeUsageUseCase,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : ViewModel() {

    // =====================================================
    // State Management - Cafes
    // =====================================================

    private val _cafes = MutableStateFlow<List<StudyCafeSummaryDto>>(emptyList())
    val cafes: StateFlow<List<StudyCafeSummaryDto>> = _cafes.asStateFlow()

    // 각 카페 ID별로 UsageDto를 저장하여 리스트에서 참조
    private val _cafeUsages = MutableStateFlow<Map<Long, UsageDto>>(emptyMap())
    val cafeUsages: StateFlow<Map<Long, UsageDto>> = _cafeUsages.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // =====================================================
    // State Management - Events
    // =====================================================

    private val _events = Channel<String>(Channel.Factory.BUFFERED)
    val events = _events.receiveAsFlow()

    // =====================================================
    // Public Methods
    // =====================================================

    fun loadRegisteredCafes() {
        viewModelScope.launch(ioDispatcher) {
            _isLoading.value = true
            try {
                when (val result = getAdminCafesUseCase()) {
                    is ApiResult.Success -> {
                        _cafes.value = result.data ?: emptyList()
                    }
                    is ApiResult.Failure -> {
                        _cafes.value = emptyList()
                        _events.send(result.message ?: "카페 목록 조회 실패")
                    }
                }
            } catch (e: Exception) {
                _cafes.value = emptyList()
                _events.send(e.message ?: "알 수 없는 오류")
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun loadCafeUsage(cafeId: Long) {
        viewModelScope.launch(ioDispatcher) {
            try {
                when (val result = getCafeUsageUseCase(cafeId)) {
                    is ApiResult.Success -> {
                        val usageData = result.data
                        if (usageData != null) {
                            _cafeUsages.value = _cafeUsages.value.toMutableMap().apply {
                                this[cafeId] = usageData
                            }
                        }
                    }
                    is ApiResult.Failure -> {
                        _events.send(result.message ?: "좌석 사용 현황 조회 실패")
                    }
                }
            } catch (e: Exception) {
                _events.send(e.message ?: "알 수 없는 오류")
            }
        }
    }

    fun retryLoadCafes() {
        loadRegisteredCafes()
    }
}