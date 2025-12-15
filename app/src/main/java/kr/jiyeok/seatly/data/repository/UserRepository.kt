package kr.jiyeok.seatly.data.repository

import kr.jiyeok.seatly.data.remote.ApiService
import kr.jiyeok.seatly.data.remote.response.UserResponseDto
import javax.inject.Inject

interface UserRepository {
    suspend fun getUserInfo(): Result<UserResponseDto>
}

class UserRepositoryImpl @Inject constructor(
    private val apiService: ApiService
) : UserRepository {
    override suspend fun getUserInfo(): Result<UserResponseDto> =
        try {
            val response = apiService.getUserInfo()
            if (response.success && response.data != null) {
                Result.success(response.data)
            } else {
                Result.failure(Exception(response.message))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
}
