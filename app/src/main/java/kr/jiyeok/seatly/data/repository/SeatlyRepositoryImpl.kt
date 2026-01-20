package kr.jiyeok.seatly.data.repository

import android.util.Log
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kr.jiyeok.seatly.data.remote.ApiService
import kr.jiyeok.seatly.data.remote.request.*
import kr.jiyeok.seatly.data.remote.response.*
import kr.jiyeok.seatly.di.IoDispatcher
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

    override suspend fun uploadImage(file: okhttp3.MultipartBody.Part) =
        safeApiCall { apiService.uploadImage(file) }

    override suspend fun getImage(imageId: String): ApiResult<ByteArray> =
        withContext(ioDispatcher) {
            try {
                val responseBody = apiService.getImage(imageId)
                val byteArray = responseBody.bytes()
                ApiResult.Success(byteArray)
            } catch (e: Exception) {
                Log.e("SeatlyRepository", "Image download failed", e)
                val message = when (e) {
                    is IOException -> "네트워크 오류: ${e.localizedMessage ?: "알 수 없는 오류"}"
                    is HttpException -> {
                        val code = e.code()
                        when (code) {
                            401 -> "인증이 필요합니다"
                            403 -> "접근 권한이 없습니다"
                            404 -> "이미지를 찾을 수 없습니다"
                            500 -> "서버 오류가 발생했습니다"
                            else -> "HTTP 오류 ($code): ${e.message}"
                        }
                    }
                    else -> "알 수 없는 오류: ${e.localizedMessage ?: "오류 발생"}"
                }
                ApiResult.Failure(message, e)
            }
        }

    override suspend fun deleteImage(imageId: String) =
        safeApiCall { apiService.deleteImage(imageId) }

    // =====================================================
    // Helper Methods
    // =====================================================

    /**
     * 안전한 API 호출을 수행하고 ApiResult로 변환
     * 네트워크 에러, HTTP 예외 등을 처리합니다
     * 서버가 데이터를 직접 반환하는 구조에 대응
     */
    private suspend fun <T> safeApiCall(
        apiCall: suspend () -> T
    ): ApiResult<T> {
        return withContext(ioDispatcher) {
            try {
                val response = apiCall()
                ApiResult.Success(response)
            } catch (e: Exception) {
                Log.e("SeatlyRepository", "API call failed", e)
                val message = when (e) {
                    is IOException -> "네트워크 오류: ${e.localizedMessage ?: "연결할 수 없습니다"}"
                    is HttpException -> {
                        val code = e.code()
                        when (code) {
                            401 -> "인증이 필요합니다"
                            403 -> "권한이 없습니다"
                            404 -> "요청한 리소스를 찾을 수 없습니다"
                            500 -> "서버 오류가 발생했습니다"
                            else -> "HTTP $code: ${e.message()}"
                        }
                    }
                    else -> e.localizedMessage ?: "알 수 없는 오류가 발생했습니다"
                }
                ApiResult.Failure(message, e)
            }
        }
    }
}
