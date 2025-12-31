package kr.jiyeok.seatly.domain.manager

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages favorite cafe state across the app.
 * This is a singleton that both AuthViewModel and SearchViewModel can access.
 */
@Singleton
class FavoriteManager @Inject constructor() {
    
    private val _favoriteCafeIds = MutableStateFlow<List<Long>>(emptyList())
    val favoriteCafeIds: StateFlow<List<Long>> = _favoriteCafeIds.asStateFlow()
    
    /**
     * Set the initial favorite cafe IDs from user data.
     */
    fun setFavorites(cafeIds: List<Long>) {
        _favoriteCafeIds.value = cafeIds
    }
    
    /**
     * Toggle favorite status for a cafe.
     * Adds to favorites if not present, removes if present.
     */
    fun toggleFavorite(cafeId: Long) {
        val currentFavorites = _favoriteCafeIds.value.toMutableList()
        
        if (cafeId in currentFavorites) {
            currentFavorites.remove(cafeId)
        } else {
            currentFavorites.add(cafeId)
        }
        
        _favoriteCafeIds.value = currentFavorites
    }
    
    /**
     * Check if a cafe is favorited.
     */
    fun isFavorite(cafeId: Long): Boolean {
        return cafeId in _favoriteCafeIds.value
    }
    
    /**
     * Clear all favorites (e.g., on logout).
     */
    fun clearFavorites() {
        _favoriteCafeIds.value = emptyList()
    }
}
