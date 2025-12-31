package kr.jiyeok.seatly.presentation.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kr.jiyeok.seatly.SeatlyApplication
import kr.jiyeok.seatly.data.remote.response.StudyCafeSummaryDto
import kr.jiyeok.seatly.data.repository.ApiResult
import kr.jiyeok.seatly.domain.usecase.cafe.GetStudyCafesUseCase
import kr.jiyeok.seatly.domain.usecase.user.GetCurrentUserUseCase
import kr.jiyeok.seatly.di.IoDispatcher
import javax.inject.Inject

/**
 * Entry point to access AuthViewModel from SearchViewModel.
 * This is the proper way to access a ViewModel from another ViewModel in Hilt.
 */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface AuthViewModelEntryPoint {
    fun authViewModel(): AuthViewModel
}

/**
 * ViewModel for SearchScreen that fetches and manages study cafe list.
 */
@HiltViewModel
class SearchViewModel @Inject constructor(
    private val getStudyCafesUseCase: GetStudyCafesUseCase,
    private val getCurrentUserUseCase: GetCurrentUserUseCase,
    @ApplicationContext private val context: Context,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : ViewModel() {

    // Access AuthViewModel through Hilt entry point (lazy to avoid initialization issues)
    private val authViewModel: AuthViewModel by lazy {
        val appContext = context.applicationContext as SeatlyApplication
        val entryPoint = EntryPointAccessors.fromApplication(
            appContext,
            AuthViewModelEntryPoint::class.java
        )
        entryPoint.authViewModel()
    }

    private val _allCafes = MutableStateFlow<List<StudyCafeSummaryDto>>(emptyList())
    
    private val _cafes = MutableStateFlow<List<StudyCafeSummaryDto>>(emptyList())
    val cafes: StateFlow<List<StudyCafeSummaryDto>> = _cafes.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    // User's favorite cafe IDs - expose from AuthViewModel for consistency
    val favoriteCafeIds: StateFlow<List<Long>> by lazy {
        authViewModel.favoriteCafeIds
    }

    init {
        loadCafes()
        loadUserFavorites()
    }
    
    /**
     * Update search query and filter cafes.
     */
    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
        filterCafes(query)
    }
    
    /**
     * Filter cafes based on search query.
     * Searches in cafe name and address.
     */
    private fun filterCafes(query: String) {
        if (query.isBlank()) {
            _cafes.value = _allCafes.value
        } else {
            val lowerQuery = query.lowercase()
            _cafes.value = _allCafes.value.filter { cafe ->
                val name = cafe.name?.lowercase() ?: ""
                val address = cafe.address?.lowercase() ?: ""
                name.contains(lowerQuery) || address.contains(lowerQuery)
            }
        }
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
                    val cafeList = result.data?.content ?: emptyList()
                    _allCafes.value = cafeList
                    // Apply current search filter
                    filterCafes(_searchQuery.value)
                }
                is ApiResult.Failure -> {
                    _error.value = result.message ?: "카페 목록을 불러오지 못했습니다"
                }
            }

            _isLoading.value = false
        }
    }

    /**
     * Load user's favorite cafes.
     * This initializes the favorites in AuthViewModel if needed.
     */
    private fun loadUserFavorites() {
        viewModelScope.launch(ioDispatcher) {
            when (val result = getCurrentUserUseCase()) {
                is ApiResult.Success -> {
                    // Favorites are automatically available through AuthViewModel
                    // No need to do anything here
                }
                is ApiResult.Failure -> {
                    // Silently fail - user might not be logged in
                }
            }
        }
    }

    /**
     * Toggle favorite status for a cafe.
     * This updates the state in AuthViewModel so it's shared across the app.
     */
    fun toggleFavorite(cafeId: Long) {
        authViewModel.toggleFavorite(cafeId)
    }

    /**
     * Refresh cafe list and user favorites.
     */
    fun refresh() {
        loadCafes()
        loadUserFavorites()
    }
}
