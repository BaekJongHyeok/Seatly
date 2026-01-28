package kr.jiyeok.seatly.domain.usecase

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import kr.jiyeok.seatly.data.remote.request.*
import kr.jiyeok.seatly.data.remote.response.*
import kr.jiyeok.seatly.data.repository.ApiResult
import kr.jiyeok.seatly.data.repository.SeatlyRepository

/**
 * Seatly UseCase 모음
 */

// =====================================================
// Authentication UseCases
// =====================================================

class LoginUseCase(
    private val repository: SeatlyRepository,
    private val ioDispatcher: CoroutineDispatcher
) {
    suspend operator fun invoke(request: LoginRequest): ApiResult<UserInfoSummaryDto> =
        withContext(ioDispatcher) { repository.login(request) }
}

class LogoutUseCase(
    private val repository: SeatlyRepository,
    private val ioDispatcher: CoroutineDispatcher
) {
    suspend operator fun invoke(): ApiResult<Unit> =
        withContext(ioDispatcher) { repository.logout() }
}

// =====================================================
// User UseCases
// =====================================================

class GetUserInfoUseCase(
    private val repository: SeatlyRepository,
    private val ioDispatcher: CoroutineDispatcher
) {
    suspend operator fun invoke(): ApiResult<UserInfoSummaryDto> =
        withContext(ioDispatcher) { repository.getUserInfo() }
}

class GetFavoriteCafesUseCase(
    private val repository: SeatlyRepository,
    private val ioDispatcher: CoroutineDispatcher
) {
    suspend operator fun invoke(): ApiResult<List<Long>> =
        withContext(ioDispatcher) { repository.getFavoriteCafes() }
}

class GetMyTimePassesUseCase(
    private val repository: SeatlyRepository,
    private val ioDispatcher: CoroutineDispatcher
) {
    suspend operator fun invoke(): ApiResult<List<UserTimePass>> =
        withContext(ioDispatcher) { repository.getMyTimePasses() }
}

class GetCurrentSessions(
    private val repository: SeatlyRepository,
    private val ioDispatcher: CoroutineDispatcher
) {
    suspend operator fun invoke(): ApiResult<List<SessionDto>> =
        withContext(ioDispatcher) { repository.getCurrentSessions() }
}

class UpdateUserInfoUseCase(
    private val repository: SeatlyRepository,
    private val ioDispatcher: CoroutineDispatcher
) {
    suspend operator fun invoke(request: UpdateUserInfoRequest): ApiResult<Unit> =
        withContext(ioDispatcher) { repository.updateUserInfo(request) }
}

class DeleteAccountUseCase(
    private val repository: SeatlyRepository,
    private val ioDispatcher: CoroutineDispatcher
) {
    suspend operator fun invoke(): ApiResult<Unit> =
        withContext(ioDispatcher) { repository.deleteAccount() }
}

class RegisterUseCase(
    private val repository: SeatlyRepository,
    private val ioDispatcher: CoroutineDispatcher
) {
    suspend operator fun invoke(request: RegisterRequest): ApiResult<Unit> =
        withContext(ioDispatcher) { repository.register(request) }
}

class ChangePasswordUseCase(
    private val repository: SeatlyRepository,
    private val ioDispatcher: CoroutineDispatcher
) {
    suspend operator fun invoke(request: ChangePasswordRequest): ApiResult<Unit> =
        withContext(ioDispatcher) { repository.changePassword(request) }
}

// =====================================================
// Users (Admin) UseCases
// =====================================================

class GetUsersWithTimePassUseCase(
    private val repository: SeatlyRepository,
    private val ioDispatcher: CoroutineDispatcher
) {
    suspend operator fun invoke(studyCafeId: Long): ApiResult<List<UserTimePassInfo>> =
        withContext(ioDispatcher) { repository.getUsersInfo(studyCafeId) }
}

class GetUserInfoAdminUseCase(
    private val repository: SeatlyRepository,
    private val ioDispatcher: CoroutineDispatcher
) {
    suspend operator fun invoke(userId: Long): ApiResult<UserInfoSummaryDto> =
        withContext(ioDispatcher) { repository.getUsersInfoById(userId) }
}

class AddUserTimePassUseCase(
    private val repository: SeatlyRepository,
    private val ioDispatcher: CoroutineDispatcher
) {
    suspend operator fun invoke(userId: Long, studyCafeId: Long, time: Long): ApiResult<Unit> =
        withContext(ioDispatcher) { repository.addUserTimePass(userId, studyCafeId, time) }
}

// =====================================================
// Session UseCases
// =====================================================

class GetSessionsUseCase(
    private val repository: SeatlyRepository,
    private val ioDispatcher: CoroutineDispatcher
) {
    suspend operator fun invoke(studyCafeId: Long): ApiResult<List<SessionDto>> =
        withContext(ioDispatcher) { repository.getSessions(studyCafeId) }
}

class StartSessionUseCase(
    private val repository: SeatlyRepository,
    private val ioDispatcher: CoroutineDispatcher
) {
    suspend operator fun invoke(sessionId: Long): ApiResult<SessionDto> =
        withContext(ioDispatcher) { repository.startSession(sessionId) }
}

class EndSessionUseCase(
    private val repository: SeatlyRepository,
    private val ioDispatcher: CoroutineDispatcher
) {
    suspend operator fun invoke(sessionId: Long): ApiResult<Unit> =
        withContext(ioDispatcher) { repository.endSession(sessionId) }
}

class AssignSeatUseCase(
    private val repository: SeatlyRepository,
    private val ioDispatcher: CoroutineDispatcher
) {
    suspend operator fun invoke(seatId: String): ApiResult<SessionDto> =
        withContext(ioDispatcher) { repository.assignSeat(seatId) }
}

class AutoAssignSeatUseCase(
    private val repository: SeatlyRepository,
    private val ioDispatcher: CoroutineDispatcher
) {
    suspend operator fun invoke(studyCafeId: Long): ApiResult<SessionDto> =
        withContext(ioDispatcher) { repository.autoAssignSeat(studyCafeId) }
}

// =====================================================
// Study Cafe UseCases
// =====================================================

class GetStudyCafesUseCase(
    private val repository: SeatlyRepository,
    private val ioDispatcher: CoroutineDispatcher
) {
    suspend operator fun invoke(): ApiResult<List<StudyCafeSummaryDto>> =
        withContext(ioDispatcher) { repository.getStudyCafes() }
}

class GetCafeDetailUseCase(
    private val repository: SeatlyRepository,
    private val ioDispatcher: CoroutineDispatcher
) {
    suspend operator fun invoke(cafeId: Long): ApiResult<StudyCafeDetailDto> =
        withContext(ioDispatcher) { repository.getCafeDetail(cafeId) }
}

class CreateCafeUseCase(
    private val repository: SeatlyRepository,
    private val ioDispatcher: CoroutineDispatcher
) {
    suspend operator fun invoke(request: CreateCafeRequest): ApiResult<Unit> =
        withContext(ioDispatcher) { repository.createCafe(request) }
}

class UpdateCafeUseCase(
    private val repository: SeatlyRepository,
    private val ioDispatcher: CoroutineDispatcher
) {
    suspend operator fun invoke(
        cafeId: Long,
        request: UpdateCafeRequest
    ): ApiResult<Unit> =
        withContext(ioDispatcher) { repository.updateCafe(cafeId, request) }
}

class DeleteCafeUseCase(
    private val repository: SeatlyRepository,
    private val ioDispatcher: CoroutineDispatcher
) {
    suspend operator fun invoke(cafeId: Long): ApiResult<Unit> =
        withContext(ioDispatcher) { repository.deleteCafe(cafeId) }
}

class AddFavoriteCafeUseCase(
    private val repository: SeatlyRepository,
    private val ioDispatcher: CoroutineDispatcher
) {
    suspend operator fun invoke(cafeId: Long): ApiResult<Unit> =
        withContext(ioDispatcher) { repository.addFavoriteCafe(cafeId) }
}

class RemoveFavoriteCafeUseCase(
    private val repository: SeatlyRepository,
    private val ioDispatcher: CoroutineDispatcher
) {
    suspend operator fun invoke(cafeId: Long): ApiResult<Unit> =
        withContext(ioDispatcher) { repository.removeFavoriteCafe(cafeId) }
}

class GetCafeUsageUseCase(
    private val repository: SeatlyRepository,
    private val ioDispatcher: CoroutineDispatcher
) {
    suspend operator fun invoke(cafeId: Long): ApiResult<UsageDto> =
        withContext(ioDispatcher) { repository.getCafeUsage(cafeId) }
}

class DeleteUserTimePassUseCase(
    private val repository: SeatlyRepository,
    private val ioDispatcher: CoroutineDispatcher
) {
    suspend operator fun invoke(cafeId: Long, userId: Long): ApiResult<Unit> =
        withContext(ioDispatcher) { repository.deleteUserTimePass(cafeId, userId) }
}

class GetAdminCafesUseCase(
    private val repository: SeatlyRepository,
    private val ioDispatcher: CoroutineDispatcher
) {
    suspend operator fun invoke(): ApiResult<List<StudyCafeSummaryDto>> =
        withContext(ioDispatcher) { repository.getAdminCafes() }
}

// =====================================================
// Seat UseCases
// =====================================================

class GetCafeSeatsUseCase(
    private val repository: SeatlyRepository,
    private val ioDispatcher: CoroutineDispatcher
) {
    suspend operator fun invoke(cafeId: Long): ApiResult<List<SeatDto>> =
        withContext(ioDispatcher) { repository.getCafeSeats(cafeId) }
}

class CreateSeatsUseCase(
    private val repository: SeatlyRepository,
    private val ioDispatcher: CoroutineDispatcher
) {
    suspend operator fun invoke(
        cafeId: Long,
        request: List<SeatCreate>
    ): ApiResult<Unit> =
        withContext(ioDispatcher) { repository.createSeats(cafeId, request) }
}

class UpdateSeatsUseCase(
    private val repository: SeatlyRepository,
    private val ioDispatcher: CoroutineDispatcher
) {
    suspend operator fun invoke(
        cafeId: Long,
        request: List<SeatUpdate>
    ): ApiResult<Unit> =
        withContext(ioDispatcher) { repository.updateSeats(cafeId, request) }
}

class DeleteSeatUseCase(
    private val repository: SeatlyRepository,
    private val ioDispatcher: CoroutineDispatcher
) {
    suspend operator fun invoke(
        cafeId: Long,
        seatId: String
    ): ApiResult<Unit> =
        withContext(ioDispatcher) { repository.deleteSeat(cafeId, seatId) }
}

// =====================================================
// Image UseCases
// =====================================================

class UploadImageUseCase(
    private val repository: SeatlyRepository,
    private val ioDispatcher: CoroutineDispatcher
) {
    suspend operator fun invoke(fileName: String, content: ByteArray): ApiResult<String> =
        withContext(ioDispatcher) { repository.uploadImage(fileName, content) }
}

class GetImageUseCase(
    private val repository: SeatlyRepository,
    private val ioDispatcher: CoroutineDispatcher
) {
    suspend operator fun invoke(imageId: String): ApiResult<ByteArray> =
        withContext(ioDispatcher) { repository.getImage(imageId) }
}

class DeleteImageUseCase(
    private val repository: SeatlyRepository,
    private val ioDispatcher: CoroutineDispatcher
) {
    suspend operator fun invoke(imageId: String): ApiResult<Unit> =
        withContext(ioDispatcher) { repository.deleteImage(imageId) }
}
