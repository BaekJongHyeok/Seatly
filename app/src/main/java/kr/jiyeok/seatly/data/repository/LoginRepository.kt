package kr.jiyeok.seatly.data.repository

import kr.jiyeok.seatly.data.remote.ApiService
import kr.jiyeok.seatly.data.remote.LoginRequest
import kr.jiyeok.seatly.data.remote.response.LoginResponseDTO
import javax.inject.Inject

interface LoginRepository {
    suspend fun login(email: String, password: String): Result<LoginResponseDTO>
}

class LoginRepositoryImpl @Inject constructor(
    private val apiService: ApiService
) : LoginRepository {
    override suspend fun login(email: String, password: String): Result<LoginResponseDTO> =
        try {
            val response = apiService.login(LoginRequest(email, password))
            if (response.success && response.data != null) {
                Result.success(response.data)
            } else {
                Result.failure(Exception(response.message))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
}
