package kr.jiyeok.seatly.data.repository

import kr.jiyeok.seatly.data.remote.ApiService
import kr.jiyeok.seatly.data.remote.response.SeatResponseDto
import kr.jiyeok.seatly.data.remote.response.StudyCafeResponseDto
import javax.inject.Inject

interface StudyCafeRepository {
    suspend fun getStudyCafes(): Result<List<StudyCafeResponseDto>>
    suspend fun getCafeSeats(cafeId: Long): Result<List<SeatResponseDto>>
}

class StudyCafeRepositoryImpl @Inject constructor(
    private val apiService: ApiService
) : StudyCafeRepository {
    override suspend fun getStudyCafes(): Result<List<StudyCafeResponseDto>> =
        try {
            val response = apiService.getStudyCafes()
            if (response.success && response.data != null) {
                Result.success(response.data)
            } else {
                Result.failure(Exception(response.message))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }

    override suspend fun getCafeSeats(cafeId: Long): Result<List<SeatResponseDto>> =
        try {
            val response = apiService.getCafeSeats(cafeId)
            if (response.success && response.data != null) {
                Result.success(response.data)
            } else {
                Result.failure(Exception(response.message))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
}
