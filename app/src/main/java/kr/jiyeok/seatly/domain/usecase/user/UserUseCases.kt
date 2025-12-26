package kr.jiyeok.seatly.domain.usecase.user

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import kr.jiyeok.seatly.data.remote.request.ChangePasswordRequest
import kr.jiyeok.seatly.data.remote.request.UpdateUserRequest
import kr.jiyeok.seatly.data.remote.response.CurrentCafeUsageDto
import kr.jiyeok.seatly.data.remote.response.UserResponseDto
import kr.jiyeok.seatly.data.repository.ApiResult
import kr.jiyeok.seatly.data.repository.SeatlyRepository
import kr.jiyeok.seatly.di.IoDispatcher
import javax.inject.Inject

/**
 * User related use-cases.
 */

/** Get current authenticated user's profile. */
class GetCurrentUserUseCase @Inject constructor(
    private val repository: SeatlyRepository,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) {
    suspend operator fun invoke(): ApiResult<UserResponseDto> =
        withContext(ioDispatcher) { repository.getCurrentUser() }
}

/** Get user info (compat endpoint). */
class GetUserInfoUseCase @Inject constructor(
    private val repository: SeatlyRepository,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) {
    suspend operator fun invoke(): ApiResult<UserResponseDto> =
        withContext(ioDispatcher) { repository.getUserInfo() }
}

/** Update user profile (name/phone/avatar). */
class UpdateUserUseCase @Inject constructor(
    private val repository: SeatlyRepository,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) {
    suspend operator fun invoke(request: UpdateUserRequest): ApiResult<UserResponseDto> =
        withContext(ioDispatcher) { repository.updateUser(request) }
}

/** Change password with current password verification. */
class ChangePasswordUseCase @Inject constructor(
    private val repository: SeatlyRepository,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) {
    suspend operator fun invoke(request: ChangePasswordRequest): ApiResult<Unit> =
        withContext(ioDispatcher) { repository.changePassword(request) }
}

/** Withdraw / delete current user account. */
class DeleteAccountUseCase @Inject constructor(
    private val repository: SeatlyRepository,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) {
    suspend operator fun invoke(): ApiResult<Unit> =
        withContext(ioDispatcher) { repository.deleteAccount() }
}

/** Get currently used cafe usage by the user. */
class GetCurrentCafeUsageUseCase @Inject constructor(
    private val repository: SeatlyRepository,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) {
    suspend operator fun invoke(): ApiResult<CurrentCafeUsageDto> =
        withContext(ioDispatcher) { repository.getCurrentCafeUsage() }
}