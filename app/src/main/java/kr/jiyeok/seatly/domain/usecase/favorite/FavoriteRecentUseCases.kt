package kr.jiyeok.seatly.domain.usecase.favorite

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import kr.jiyeok.seatly.data.remote.response.PageResponse
import kr.jiyeok.seatly.data.remote.response.StudyCafeSummaryDto
import kr.jiyeok.seatly.data.repository.ApiResult
import kr.jiyeok.seatly.data.repository.SeatlyRepository
import kr.jiyeok.seatly.di.IoDispatcher
import javax.inject.Inject

/**
 * Favorites (찜) and Recent cafes use-cases.
 */

/** Get paged favorite cafes. */
class GetFavoriteCafesUseCase @Inject constructor(
    private val repository: SeatlyRepository,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) {
    suspend operator fun invoke(page: Int = 0, size: Int = 20): ApiResult<PageResponse<StudyCafeSummaryDto>> =
        withContext(ioDispatcher) { repository.getFavoriteCafes(page, size) }
}

/** Add a cafe to favorites. */
class AddFavoriteCafeUseCase @Inject constructor(
    private val repository: SeatlyRepository,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) {
    suspend operator fun invoke(cafeId: Long): ApiResult<Unit> =
        withContext(ioDispatcher) { repository.addFavoriteCafe(cafeId) }
}

/** Remove a cafe from favorites. */
class RemoveFavoriteCafeUseCase @Inject constructor(
    private val repository: SeatlyRepository,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) {
    suspend operator fun invoke(cafeId: Long): ApiResult<Unit> =
        withContext(ioDispatcher) { repository.removeFavoriteCafe(cafeId) }
}

/** Get recent cafes visited by user. */
class GetRecentCafesUseCase @Inject constructor(
    private val repository: SeatlyRepository,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) {
    suspend operator fun invoke(limit: Int = 10): ApiResult<List<StudyCafeSummaryDto>> =
        withContext(ioDispatcher) { repository.getRecentCafes(limit) }
}