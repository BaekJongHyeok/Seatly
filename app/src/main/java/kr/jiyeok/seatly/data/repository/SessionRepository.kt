package kr.jiyeok.seatly.data.repository

import kr.jiyeok.seatly.data.remote.ApiService
import kr.jiyeok.seatly.data.remote.request.StartSessionRequest
import kr.jiyeok.seatly.data.remote.response.SessionResponseDto
import javax.inject.Inject

interface SessionRepository {
    suspend fun getCurrentSessions(studyCafeId: Long? = null): Result<List<SessionResponseDto>>
    suspend fun startSession(seatId: Long): Result<SessionResponseDto>
    suspend fun endSession(sessionId: Long): Result<SessionResponseDto>
    suspend fun forceEndSession(sessionId: Long): Result<Unit>
}

class SessionRepositoryImpl @Inject constructor(
    private val apiService: ApiService
) : SessionRepository {
    override suspend fun getCurrentSessions(studyCafeId: Long?): Result<List<SessionResponseDto>> =
        try {
            val response = apiService.getCurrentSessions(studyCafeId)
            if (response.success && response.data != null) {
                Result.success(response.data)
            } else {
                Result.failure(Exception(response.message))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }

    override suspend fun startSession(seatId: Long): Result<SessionResponseDto> =
        try {
            val response = apiService.startSession(StartSessionRequest(seatId))
            if (response.success && response.data != null) {
                Result.success(response.data)
            } else {
                Result.failure(Exception(response.message))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }

    override suspend fun endSession(sessionId: Long): Result<SessionResponseDto> =
        try {
            val response = apiService.endSession(sessionId)
            if (response.success && response.data != null) {
                Result.success(response.data)
            } else {
                Result.failure(Exception(response.message))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }

    override suspend fun forceEndSession(sessionId: Long): Result<Unit> =
        try {
            val response = apiService.forceEndSession(sessionId)
            if (response.success) {
                Result.success(Unit)
            } else {
                Result.failure(Exception(response.message))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
}
