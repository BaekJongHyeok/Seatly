package kr.jiyeok.seatly.presentation.viewmodel.home

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor() : ViewModel() {
    fun onEvent(event: HomeEvent) {
    }
}

sealed class HomeEvent {
}

data class HomeUiState(
    val isLoading: Boolean = false,
    val error: String? = null
)
