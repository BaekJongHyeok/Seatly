package kr.jiyeok.seatly.domain.usecase.cafe

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import kr.jiyeok.seatly.data.remote.response.PageResponse
import kr.jiyeok.seatly.data.remote.response.StudyCafeDetailDto
import kr.jiyeok.seatly.data.remote.response.StudyCafeSummaryDto
import kr.jiyeok.seatly.data.repository.ApiResult
import kr.jiyeok.seatly.data.repository.SeatlyRepository
import kr.jiyeok.seatly.di.IoDispatcher
import javax.inject.Inject

/**
 * Study cafe listing & search related use-cases.
 */

/** Get paged list of study cafes with optional filters. */
class GetStudyCafesUseCase @Inject constructor(
    private val repository: SeatlyRepository,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) {
    suspend operator fun invoke(
        page: Int = 0,
        size: Int = 20,
        search: String? = null,
        amenities: String? = null,
        openNow: Boolean? = null,
        sort: String? = null,
        lat: Double? = null,
        lng: Double? = null
    ): ApiResult<PageResponse<StudyCafeSummaryDto>> =
        withContext(ioDispatcher) { repository.getStudyCafes(page, size, search, amenities, openNow, sort, lat, lng) }
}

/** Get summary info for a cafe (card). */
class GetCafeSummaryUseCase @Inject constructor(
    private val repository: SeatlyRepository,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) {
    suspend operator fun invoke(cafeId: Long): ApiResult<StudyCafeSummaryDto> =
        withContext(ioDispatcher) { repository.getCafeSummary(cafeId) }
}

/** Get full cafe detail. */
class GetCafeDetailUseCase @Inject constructor(
    private val repository: SeatlyRepository,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) {
    suspend operator fun invoke(cafeId: Long): ApiResult<StudyCafeDetailDto> =
        withContext(ioDispatcher) { repository.getCafeDetail(cafeId) }
}