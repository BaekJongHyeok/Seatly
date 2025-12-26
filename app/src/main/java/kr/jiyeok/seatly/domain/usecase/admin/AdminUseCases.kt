package kr.jiyeok.seatly.domain.usecase.admin

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import kr.jiyeok.seatly.data.remote.request.AddSeatRequest
import kr.jiyeok.seatly.data.remote.request.EditSeatRequest
import kr.jiyeok.seatly.data.remote.response.PageResponse
import kr.jiyeok.seatly.data.remote.response.SeatResponseDto
import kr.jiyeok.seatly.data.remote.response.StudyCafeDetailDto
import kr.jiyeok.seatly.data.remote.response.StudyCafeSummaryDto
import kr.jiyeok.seatly.data.repository.ApiResult
import kr.jiyeok.seatly.data.repository.SeatlyRepository
import kr.jiyeok.seatly.di.IoDispatcher
import okhttp3.MultipartBody
import okhttp3.RequestBody
import javax.inject.Inject

/**
 * Admin related use-cases for cafe & seat management.
 */

/** Get paged admin cafes list. */
class GetAdminCafesUseCase @Inject constructor(
    private val repository: SeatlyRepository,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) {
    suspend operator fun invoke(page: Int = 0, size: Int = 20, search: String? = null): ApiResult<PageResponse<StudyCafeSummaryDto>> =
        withContext(ioDispatcher) { repository.getAdminCafes(page, size, search) }
}

/** Get admin view of a specific cafe detail. */
class GetAdminCafeDetailUseCase @Inject constructor(
    private val repository: SeatlyRepository,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) {
    suspend operator fun invoke(cafeId: Long): ApiResult<StudyCafeDetailDto> =
        withContext(ioDispatcher) { repository.getAdminCafeDetail(cafeId) }
}

/** Create a cafe (multipart parts + images). */
class CreateCafeUseCase @Inject constructor(
    private val repository: SeatlyRepository,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) {
    suspend operator fun invoke(parts: Map<String, RequestBody>, images: List<MultipartBody.Part> = emptyList()): ApiResult<StudyCafeDetailDto> =
        withContext(ioDispatcher) { repository.createCafe(parts, images) }
}

/** Update a cafe. */
class UpdateCafeUseCase @Inject constructor(
    private val repository: SeatlyRepository,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) {
    suspend operator fun invoke(cafeId: Long, parts: Map<String, RequestBody>, images: List<MultipartBody.Part> = emptyList()): ApiResult<StudyCafeDetailDto> =
        withContext(ioDispatcher) { repository.updateCafe(cafeId, parts, images) }
}

/** Delete a cafe. */
class DeleteCafeUseCase @Inject constructor(
    private val repository: SeatlyRepository,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) {
    suspend operator fun invoke(cafeId: Long): ApiResult<Unit> =
        withContext(ioDispatcher) { repository.deleteCafe(cafeId) }
}

/** Add seat to a cafe. */
class AddSeatUseCase @Inject constructor(
    private val repository: SeatlyRepository,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) {
    suspend operator fun invoke(cafeId: Long, request: AddSeatRequest): ApiResult<SeatResponseDto> =
        withContext(ioDispatcher) { repository.addSeat(cafeId, request) }
}

/** Edit seat. */
class EditSeatUseCase @Inject constructor(
    private val repository: SeatlyRepository,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) {
    suspend operator fun invoke(cafeId: Long, seatId: Long, request: EditSeatRequest): ApiResult<SeatResponseDto> =
        withContext(ioDispatcher) { repository.editSeat(cafeId, seatId, request) }
}

/** Delete seat. */
class DeleteSeatUseCase @Inject constructor(
    private val repository: SeatlyRepository,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) {
    suspend operator fun invoke(cafeId: Long, seatId: Long): ApiResult<Unit> =
        withContext(ioDispatcher) { repository.deleteSeat(cafeId, seatId) }
}

/** Force end a session (admin). */
class ForceEndSessionUseCase @Inject constructor(
    private val repository: SeatlyRepository,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) {
    suspend operator fun invoke(sessionId: Long): ApiResult<Unit> =
        withContext(ioDispatcher) { repository.forceEndSession(sessionId) }
}

/** Remove a user from a cafe (admin). */
class DeleteUserFromCafeUseCase @Inject constructor(
    private val repository: SeatlyRepository,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) {
    suspend operator fun invoke(cafeId: Long, userId: Long): ApiResult<Unit> =
        withContext(ioDispatcher) { repository.deleteUserFromCafe(cafeId, userId) }
}