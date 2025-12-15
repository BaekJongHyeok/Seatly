package kr.jiyeok.seatly.domain.usecase

import kr.jiyeok.seatly.data.remote.response.LoginResponseDTO
import kr.jiyeok.seatly.data.repository.LoginRepository
import javax.inject.Inject

class LoginUseCase @Inject constructor(
    private val loginRepository: LoginRepository
) {
    suspend fun login(email: String, password: String): Result<LoginResponseDTO> =
        loginRepository.login(email, password)
}
