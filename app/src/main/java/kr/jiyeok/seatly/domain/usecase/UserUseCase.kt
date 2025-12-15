package kr.jiyeok.seatly.domain.usecase

import kr.jiyeok.seatly.data.remote.response.UserResponseDto
import kr.jiyeok.seatly.data.repository.UserRepository
import javax.inject.Inject

class UserUseCase @Inject constructor(
    private val userRepository: UserRepository
) {
    suspend fun getUserInfo(): Result<UserResponseDto> =
        userRepository.getUserInfo()
}
