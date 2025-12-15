package kr.jiyeok.seatly.data.repository

import kr.jiyeok.seatly.data.remote.ApiService
import kr.jiyeok.seatly.data.remote.request.ReservationRequest
import kr.jiyeok.seatly.data.remote.response.SessionResponseDto
import javax.inject.Inject

interface ReservationRepository {
    suspend fun autoAssignSeat(cafeId: Long): Result<SessionResponseDto>
    suspend fun reserveSeat(cafeId: Long, seatId: Long): Result<SessionResponseDto>
}

class ReservationRepositoryImpl @Inject constructor(
    private val apiService: ApiService
) : ReservationRepository {
    override suspend fun autoAssignSeat(cafeId: Long): Result<SessionResponseDto> =
        try {
            val response = apiService.autoAssignSeat(cafeId)
            if (response.success && response.data != null) {
                Result.success(response.data)
            } else {
                Result.failure(Exception(response.message))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }

    override suspend fun reserveSeat(cafeId: Long, seatId: Long): Result<SessionResponseDto> =
        try {
            val response = apiService.reserveSeat(cafeId, ReservationRequest(seatId))
            if (response.success && response.data != null) {
                Result.success(response.data)
            } else {
                Result.failure(Exception(response.message))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
}
