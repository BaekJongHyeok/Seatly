package kr.jiyeok.seatly.domain.usecase.seat

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import kr.jiyeok.seatly.data.remote.request.ReservationRequest
import kr.jiyeok.seatly.data.remote.request.StartSessionRequest
import kr.jiyeok.seatly.data.remote.response.SessionResponseDto
import kr.jiyeok.seatly.data.remote.response.SeatResponseDto
import kr.jiyeok.seatly.data.repository.ApiResult
import kr.jiyeok.seatly.data.repository.SeatlyRepository
import kr.jiyeok.seatly.di.IoDispatcher
import javax.inject.Inject

/**
 * Seat and reservation related use-cases.
 */

/** Get seats for a cafe with optional status filter. */
class GetCafeSeatsUseCase @Inject constructor(
    private val repository: SeatlyRepository,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) {
    suspend operator fun invoke(cafeId: Long, status: String? = null): ApiResult<List<SeatResponseDto>> =
        withContext(ioDispatcher) { repository.getCafeSeats(cafeId, status) }
}

/** Auto assign a seat for the user. */
class AutoAssignSeatUseCase @Inject constructor(
    private val repository: SeatlyRepository,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) {
    suspend operator fun invoke(cafeId: Long): ApiResult<SessionResponseDto> =
        withContext(ioDispatcher) { repository.autoAssignSeat(cafeId) }
}

/** Reserve a specific seat or make a pre-reservation. */
class ReserveSeatUseCase @Inject constructor(
    private val repository: SeatlyRepository,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) {
    suspend operator fun invoke(cafeId: Long, request: ReservationRequest): ApiResult<SessionResponseDto> =
        withContext(ioDispatcher) { repository.reserveSeat(cafeId, request) }
}