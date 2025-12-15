package kr.jiyeok.seatly.domain.usecase

import kr.jiyeok.seatly.data.remote.response.SessionResponseDto
import kr.jiyeok.seatly.data.repository.ReservationRepository
import javax.inject.Inject

class ReservationUseCase @Inject constructor(
    private val reservationRepository: ReservationRepository
) {
    suspend fun autoAssignSeat(cafeId: Long): Result<SessionResponseDto> =
        reservationRepository.autoAssignSeat(cafeId)

    suspend fun reserveSeat(cafeId: Long, seatId: Long): Result<SessionResponseDto> =
        reservationRepository.reserveSeat(cafeId, seatId)
}
