package kr.jiyeok.seatly.data.repository

import android.util.Log
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kr.jiyeok.seatly.data.remote.ApiService
import kr.jiyeok.seatly.data.remote.request.*
import kr.jiyeok.seatly.data.remote.response.*
import kr.jiyeok.seatly.di.IoDispatcher
import kr.jiyeok.seatly.ui.screen.admin.seat.Seat
import retrofit2.HttpException
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Seatly Repository 구현체
 * 모든 API 호출을 관리하고 에러 처리를 수행합니다
 */
@Singleton
class SeatlyRepositoryImpl @Inject constructor(
    private val apiService: ApiService,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : SeatlyRepository {

    // =====================================================
    // Authentication
    // =====================================================

    override suspend fun login(request: LoginRequest): ApiResult<UserInfoSummaryDto> =
        safeApiCall {
            val userInfo = apiService.login(request)
            // 수동으로 ApiResponse 구조로 변환
            ApiResponse(
                success = true,
                message = null,
                data = userInfo
            )
        }



    override suspend fun logout() =
        safeApiCall { apiService.logout() }

    // =====================================================
    // User
    // =====================================================

    override suspend fun getUserInfo() =
        safeApiCall { apiService.getUserInfo() }

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

    override suspend fun addUserTimePass(userId: Long) =
        safeApiCall { apiService.addUserTimePass(userId) }

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

    override suspend fun uploadImage(file: okhttp3.MultipartBody.Part) =
        safeApiCall { apiService.uploadImage(file) }

    override suspend fun getImage(imageId: String) =
        safeApiCall { apiService.getImage(imageId) }

    override suspend fun deleteImage(imageId: String) =
        safeApiCall { apiService.deleteImage(imageId) }

    // =====================================================
    // Helper Methods
    // =====================================================

    /**
     * 안전한 API 호출을 수행하고 ApiResult로 변환
     * 네트워크 에러, HTTP 예외 등을 처리합니다
     */
    private suspend inline fun <reified T> safeApiCall(
        crossinline call: suspend () -> ApiResponse<T>
    ): ApiResult<T> {
        return withContext(ioDispatcher) {
            try {
                val response = call()
                if (response.success && response.data != null) {
                    ApiResult.Success(response.data)
                } else {
                    ApiResult.Failure(
                        response.message ?: "Unknown API error"
                    )
                }
            } catch (t: Throwable) {
                val message = when (t) {
                    is IOException -> "Network error: ${t.localizedMessage ?: t.message}"
                    is HttpException -> "HTTP ${t.code()}: ${t.message()}"
                    else -> t.localizedMessage ?: "Unknown error"
                }
                ApiResult.Failure(message, t)
            }
        }
    }
}
