package kr.jiyeok.seatly.domain.usecase

import kr.jiyeok.seatly.data.remote.AddSeatRequest
import kr.jiyeok.seatly.data.remote.response.SeatResponseDto
import kr.jiyeok.seatly.data.repository.AdminRepository
import javax.inject.Inject

class AdminUseCase @Inject constructor(
    private val adminRepository: AdminRepository
) {
    suspend fun addSeat(cafeId: Long, request: AddSeatRequest): Result<SeatResponseDto> =
        adminRepository.addSeat(cafeId, request)

    suspend fun deleteSeat(cafeId: Long, seatId: Long): Result<Unit> =
        adminRepository.deleteSeat(cafeId, seatId)

    suspend fun deleteUser(cafeId: Long, userId: Long): Result<Unit> =
        adminRepository.deleteUser(cafeId, userId)
}
