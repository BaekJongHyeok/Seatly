package kr.jiyeok.seatly.data.repository

import kr.jiyeok.seatly.data.remote.AddSeatRequest
import kr.jiyeok.seatly.data.remote.ApiService
import kr.jiyeok.seatly.data.remote.response.SeatResponseDto
import javax.inject.Inject

interface AdminRepository {
    suspend fun addSeat(cafeId: Long, request: AddSeatRequest): Result<SeatResponseDto>
    suspend fun deleteSeat(cafeId: Long, seatId: Long): Result<Unit>
    suspend fun deleteUser(cafeId: Long, userId: Long): Result<Unit>
}

class AdminRepositoryImpl @Inject constructor(
    private val apiService: ApiService
) : AdminRepository {
    override suspend fun addSeat(cafeId: Long, request: AddSeatRequest): Result<SeatResponseDto> =
        try {
            val response = apiService.addSeat(cafeId, request)
            if (response.success && response.data != null) {
                Result.success(response.data)
            } else {
                Result.failure(Exception(response.message))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }

    override suspend fun deleteSeat(cafeId: Long, seatId: Long): Result<Unit> =
        try {
            val response = apiService.deleteSeat(cafeId, seatId)
            if (response.success) {
                Result.success(Unit)
            } else {
                Result.failure(Exception(response.message))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }

    override suspend fun deleteUser(cafeId: Long, userId: Long): Result<Unit> =
        try {
            val response = apiService.deleteUser(cafeId, userId)
            if (response.success) {
                Result.success(Unit)
            } else {
                Result.failure(Exception(response.message))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
}
