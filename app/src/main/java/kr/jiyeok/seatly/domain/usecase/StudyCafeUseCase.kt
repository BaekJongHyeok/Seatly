package kr.jiyeok.seatly.domain.usecase

import kr.jiyeok.seatly.data.remote.response.SeatResponseDto
import kr.jiyeok.seatly.data.remote.response.StudyCafeResponseDto
import kr.jiyeok.seatly.data.repository.StudyCafeRepository
import javax.inject.Inject

class StudyCafeUseCase @Inject constructor(
    private val studyCafeRepository: StudyCafeRepository
) {
    suspend fun getStudyCafes(): Result<List<StudyCafeResponseDto>> =
        studyCafeRepository.getStudyCafes()

    suspend fun getCafeSeats(cafeId: Long): Result<List<SeatResponseDto>> =
        studyCafeRepository.getCafeSeats(cafeId)
}
