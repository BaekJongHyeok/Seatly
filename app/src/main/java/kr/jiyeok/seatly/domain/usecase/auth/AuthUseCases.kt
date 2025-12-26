package kr.jiyeok.seatly.domain.usecase.auth

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import kr.jiyeok.seatly.data.remote.request.*
import kr.jiyeok.seatly.data.remote.response.LoginResponseDTO
import kr.jiyeok.seatly.data.repository.ApiResult
import kr.jiyeok.seatly.data.repository.SeatlyRepository
import kr.jiyeok.seatly.di.IoDispatcher
import javax.inject.Inject

/**
 * Authentication related use-cases.
 *
 * Each use-case delegates to [SeatlyRepository] and executes on the injected [IoDispatcher].
 * Use operator `invoke` for concise calling from ViewModel / UseCase orchestrator.
 */

/** Login with email/password. */
class LoginUseCase @Inject constructor(
    private val repository: SeatlyRepository,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) {
    suspend operator fun invoke(request: LoginRequest): ApiResult<LoginResponseDTO> =
        withContext(ioDispatcher) { repository.login(request) }
}

/** Logout current user. */
class LogoutUseCase @Inject constructor(
    private val repository: SeatlyRepository,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) {
    suspend operator fun invoke(): ApiResult<Unit> =
        withContext(ioDispatcher) { repository.logout() }
}

/** Standard email/password registration. */
class RegisterUseCase @Inject constructor(
    private val repository: SeatlyRepository,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) {
    suspend operator fun invoke(request: RegisterRequest) =
        withContext(ioDispatcher) { repository.register(request) }
}

/** Social registration. */
class SocialRegisterUseCase @Inject constructor(
    private val repository: SeatlyRepository,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) {
    suspend operator fun invoke(request: SocialRegisterRequest) =
        withContext(ioDispatcher) { repository.socialRegister(request) }
}

/** Start password reset (send code). */
class RequestPasswordResetUseCase @Inject constructor(
    private val repository: SeatlyRepository,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) {
    suspend operator fun invoke(request: ForgotPasswordRequest) =
        withContext(ioDispatcher) { repository.requestPasswordReset(request) }
}

/** Verify password reset code. */
class VerifyPasswordResetCodeUseCase @Inject constructor(
    private val repository: SeatlyRepository,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) {
    suspend operator fun invoke(request: VerifyCodeRequest) =
        withContext(ioDispatcher) { repository.verifyPasswordResetCode(request) }
}

/** Reset password after verification. */
class ResetPasswordUseCase @Inject constructor(
    private val repository: SeatlyRepository,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) {
    suspend operator fun invoke(request: ResetPasswordRequest) =
        withContext(ioDispatcher) { repository.resetPassword(request) }
}