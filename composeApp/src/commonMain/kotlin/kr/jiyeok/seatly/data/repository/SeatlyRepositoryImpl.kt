package kr.jiyeok.seatly.data.repository

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kr.jiyeok.seatly.data.remote.ApiService
import kr.jiyeok.seatly.data.remote.request.*
import kr.jiyeok.seatly.data.remote.response.*

/**
 * Seatly Repository 구현체
 * 모든 API 호출을 관리하고 에러 처리를 수행합니다
 */
class SeatlyRepositoryImpl(
    private val apiService: ApiService,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.Default // Default for multiplatform, IO is fine too in newer versions
) : SeatlyRepository {

    // =====================================================
    // Authentication
    // =====================================================

    override suspend fun login(request: LoginRequest): ApiResult<UserInfoSummaryDto> =
        safeApiCall { apiService.login(request) }

    override suspend fun logout() =
        safeApiCall { apiService.logout() }

    // =====================================================
    // User
    // =====================================================

    override suspend fun getUserInfo() =
        safeApiCall { apiService.getUserInfo() }

    override suspend fun getFavoriteCafes() =
        safeApiCall { apiService.getFavoriteCafes() }

    override suspend fun getMyTimePasses() =
        safeApiCall { apiService.getMyTimePasses() }

    override suspend fun getCurrentSessions() =
        safeApiCall { apiService.getCurrentSessions() }

    override suspend fun updateUserInfo(request: UpdateUserInfoRequest) =
        safeApiCall { apiService.updateUserInfo(request) }

    override suspend fun deleteAccount() =
        safeApiCall { apiService.deleteAccount() }

    override suspend fun register(request: RegisterRequest) =
        safeApiCall { apiService.register(request) }

    override suspend fun changePassword(request: ChangePasswordRequest) =
        safeApiCall { apiService.changePassword(request) }

    // =====================================================
    // Users (Admin)
    // =====================================================

    override suspend fun getUsersInfo(studyCafeId: Long) =
        safeApiCall { apiService.getUsersInfo(studyCafeId) }

    override suspend fun getUsersInfoById(userId: Long) =
        safeApiCall { apiService.getUsersInfoById(userId) }

    override suspend fun addUserTimePass(userId: Long, studyCafeId: Long, time: Long) =
        safeApiCall { apiService.addUserTimePass(userId, studyCafeId, time) }

    // =====================================================
    // Sessions
    // =====================================================

    override suspend fun getSessions(studyCafeId: Long) =
        safeApiCall { apiService.getSessions(studyCafeId) }

    override suspend fun startSession(sessionId: Long) =
        safeApiCall { apiService.startSession(sessionId) }

    override suspend fun endSession(sessionId: Long) =
        safeApiCall { apiService.endSession(sessionId) }

    override suspend fun assignSeat(seatId: String) =
        safeApiCall { apiService.assignSeat(seatId) }

    override suspend fun autoAssignSeat(studyCafeId: Long) =
        safeApiCall { apiService.autoAssignSeat(studyCafeId) }

    // =====================================================
    // Study Cafes
    // =====================================================

    override suspend fun getStudyCafes() =
        safeApiCall { apiService.getStudyCafes() }

    override suspend fun getCafeDetail(cafeId: Long) =
        safeApiCall { apiService.getCafeDetail(cafeId) }

    override suspend fun createCafe(request: CreateCafeRequest) =
        safeApiCall { apiService.createCafe(request) }

    override suspend fun updateCafe(cafeId: Long, request: UpdateCafeRequest) =
        safeApiCall { apiService.updateCafe(cafeId, request) }

    override suspend fun deleteCafe(cafeId: Long) =
        safeApiCall { apiService.deleteCafe(cafeId) }

    override suspend fun addFavoriteCafe(cafeId: Long) =
        safeApiCall { apiService.addFavoriteCafe(cafeId) }

    override suspend fun removeFavoriteCafe(cafeId: Long) =
        safeApiCall { apiService.removeFavoriteCafe(cafeId) }

    override suspend fun getCafeUsage(cafeId: Long) =
        safeApiCall { apiService.getCafeUsage(cafeId) }

    override suspend fun deleteUserTimePass(cafeId: Long, userId: Long) =
        safeApiCall { apiService.deleteUserTimePass(cafeId, userId) }

    override suspend fun getAdminCafes() =
        safeApiCall { apiService.getAdminCafes() }

    // =====================================================
    // Seats
    // =====================================================

    override suspend fun getCafeSeats(cafeId: Long) =
        safeApiCall { apiService.getCafeSeats(cafeId) }

    override suspend fun createSeats(cafeId: Long, request: List<SeatCreate>) =
        safeApiCall { apiService.createSeats(cafeId, request) }

    override suspend fun updateSeats(cafeId: Long, request: List<SeatUpdate>) =
        safeApiCall { apiService.updateSeats(cafeId, request) }

    override suspend fun deleteSeat(cafeId: Long, seatId: String) =
        safeApiCall { apiService.deleteSeat(cafeId, seatId) }

    // =====================================================
    // Images
    // =====================================================

    override suspend fun uploadImage(fileName: String, content: ByteArray) =
        safeApiCall { apiService.uploadImage(fileName, content) }

    override suspend fun getImage(imageId: String): ApiResult<ByteArray> =
        safeApiCall { apiService.getImage(imageId) }

    override suspend fun deleteImage(imageId: String) =
        safeApiCall { apiService.deleteImage(imageId) }

    // =====================================================
    // Helper Methods
    // =====================================================

    private suspend fun <T> safeApiCall(
        apiCall: suspend () -> T
    ): ApiResult<T> {
        return withContext(ioDispatcher) {
            try {
                val response = apiCall()
                ApiResult.Success(response)
            } catch (e: Exception) {
                // Simplified error handling for commonMain
                ApiResult.Failure(e.message ?: "알 수 없는 오류가 발생했습니다", e)
            }
        }
    }
}
