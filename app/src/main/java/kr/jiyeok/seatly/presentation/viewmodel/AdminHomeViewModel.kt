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
import kr.jiyeok.seatly.domain.usecase.GetAdminCafesUseCase
import javax.inject.Inject

@HiltViewModel
class AdminHomeViewModel @Inject constructor(
    private val getAdminCafesUseCase: GetAdminCafesUseCase,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : ViewModel() {

    // =====================================================
    // State Management - Cafes
    // =====================================================

    private val _cafes = MutableStateFlow<List<StudyCafeSummaryDto>>(emptyList())
    val cafes: StateFlow<List<StudyCafeSummaryDto>> = _cafes.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // =====================================================
    // State Management - Events
    // =====================================================

    private val _events = Channel<String>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    // =====================================================
    // Public Methods
    // =====================================================

    fun loadCafes() {
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

    fun retryLoadCafes() {
        loadCafes()
    }
}
