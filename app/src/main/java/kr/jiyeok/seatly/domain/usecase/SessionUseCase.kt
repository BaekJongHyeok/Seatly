package kr.jiyeok.seatly.domain.usecase

import kr.jiyeok.seatly.data.remote.response.SessionResponseDto
import kr.jiyeok.seatly.data.repository.SessionRepository
import javax.inject.Inject

class SessionUseCase @Inject constructor(
    private val sessionRepository: SessionRepository
) {
    suspend fun getCurrentSessions(studyCafeId: Long? = null): Result<List<SessionResponseDto>> =
        sessionRepository.getCurrentSessions(studyCafeId)

    suspend fun startSession(seatId: Long): Result<SessionResponseDto> =
        sessionRepository.startSession(seatId)

    suspend fun endSession(sessionId: Long): Result<SessionResponseDto> =
        sessionRepository.endSession(sessionId)

    suspend fun forceEndSession(sessionId: Long): Result<Unit> =
        sessionRepository.forceEndSession(sessionId)
}
