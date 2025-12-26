package kr.jiyeok.seatly.domain.usecase.session

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import kr.jiyeok.seatly.data.remote.request.StartSessionRequest
import kr.jiyeok.seatly.data.remote.response.SessionResponseDto
import kr.jiyeok.seatly.data.repository.ApiResult
import kr.jiyeok.seatly.data.repository.SeatlyRepository
import kr.jiyeok.seatly.di.IoDispatcher
import javax.inject.Inject

/**
 * Session lifecycle related use-cases.
 */

/** Start a session (begin using a seat). */
class StartSessionUseCase @Inject constructor(
    private val repository: SeatlyRepository,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) {
    suspend operator fun invoke(request: StartSessionRequest): ApiResult<SessionResponseDto> =
        withContext(ioDispatcher) { repository.startSession(request) }
}

/** End a session (user ends usage). */
class EndSessionUseCase @Inject constructor(
    private val repository: SeatlyRepository,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) {
    suspend operator fun invoke(sessionId: Long): ApiResult<SessionResponseDto> =
        withContext(ioDispatcher) { repository.endSession(sessionId) }
}

/** Get current sessions, optionally filtered by cafe id. */
class GetCurrentSessionsUseCase @Inject constructor(
    private val repository: SeatlyRepository,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) {
    suspend operator fun invoke(studyCafeId: Long? = null): ApiResult<List<SessionResponseDto>> =
        withContext(ioDispatcher) { repository.getCurrentSessions(studyCafeId) }
}