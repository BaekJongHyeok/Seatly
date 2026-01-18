package kr.jiyeok.seatly.data.repository

import android.se.omapi.Session
import kr.jiyeok.seatly.data.remote.request.*
import kr.jiyeok.seatly.data.remote.response.*

/**
 * Seatly 앱의 모든 API 호출을 관리하는 Repository 인터페이스
 */
interface SeatlyRepository {

    // =====================================================
    // Authentication
    // =====================================================

    suspend fun login(request: LoginRequest): ApiResult<UserInfoSummaryDto>
    suspend fun logout(): ApiResult<Unit>

    // =====================================================
    // User
    // =====================================================

    suspend fun getUserInfo(): ApiResult<UserInfoSummaryDto>
    suspend fun getFavoriteCafes(): ApiResult<List<Long>>
    suspend fun getMyTimePasses(): ApiResult<List<UserTimePass>>
    suspend fun getCurrentSessions(): ApiResult<List<SessionDto>>
    suspend fun updateUserInfo(request: UpdateUserInfoRequest): ApiResult<Unit>
    suspend fun deleteAccount(): ApiResult<Unit>
    suspend fun register(request: RegisterRequest): ApiResult<Unit>
    suspend fun changePassword(request: ChangePasswordRequest): ApiResult<Unit>

    // =====================================================
    // Users (Admin)
    // =====================================================

    suspend fun getUsersInfo(studyCafeId: Long): ApiResult<List<UserTimePassInfo>>
    suspend fun getUsersInfoById(userId: Long): ApiResult<UserInfoSummaryDto>
    suspend fun addUserTimePass(userId: Long): ApiResult<Unit>

    // =====================================================
    // Sessions
    // =====================================================

    suspend fun getSessions(studyCafeId: Long): ApiResult<List<SessionDto>>
    suspend fun startSession(sessionId: Long): ApiResult<SessionDto>
    suspend fun endSession(sessionId: Long): ApiResult<Unit>
    suspend fun assignSeat(seatId: String): ApiResult<SessionDto>
    suspend fun autoAssignSeat(studyCafeId: Long): ApiResult<SessionDto>

    // =====================================================
    // Study Cafes
    // =====================================================

    suspend fun getStudyCafes(): ApiResult<List<StudyCafeSummaryDto>>
    suspend fun getCafeDetail(cafeId: Long): ApiResult<StudyCafeDetailDto>
    suspend fun createCafe(request: CreateCafeRequest): ApiResult<Unit>
    suspend fun updateCafe(cafeId: Long, request: UpdateCafeRequest): ApiResult<Unit>
    suspend fun deleteCafe(cafeId: Long): ApiResult<Unit>
    suspend fun addFavoriteCafe(cafeId: Long): ApiResult<Unit>
    suspend fun removeFavoriteCafe(cafeId: Long): ApiResult<Unit>
    suspend fun getCafeUsage(cafeId: Long): ApiResult<UsageDto>
    suspend fun deleteUserTimePass(cafeId: Long, userId: Long): ApiResult<Unit>
    suspend fun getAdminCafes(): ApiResult<List<StudyCafeSummaryDto>>

    // =====================================================
    // Seats
    // =====================================================

    suspend fun getCafeSeats(cafeId: Long): ApiResult<List<SeatDto>>
    suspend fun createSeats(cafeId: Long, request: List<SeatCreate>): ApiResult<Unit>
    suspend fun updateSeats(cafeId: Long, request: List<SeatUpdate>): ApiResult<Unit>
    suspend fun deleteSeat(cafeId: Long, seatId: String): ApiResult<Unit>

    // =====================================================
    // Images
    // =====================================================

    suspend fun uploadImage(file: okhttp3.MultipartBody.Part): ApiResult<ImageUploadResponse>
    suspend fun getImage(imageId: String): ApiResult<ByteArray>
    suspend fun deleteImage(imageId: String): ApiResult<Unit>
}
