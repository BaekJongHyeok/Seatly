package kr.jiyeok.seatly.data.repository

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kr.jiyeok.seatly.data.remote.ApiService
import kr.jiyeok.seatly.data.remote.request.ChangePasswordRequest
import kr.jiyeok.seatly.data.remote.request.ForgotPasswordRequest
import kr.jiyeok.seatly.data.remote.request.LoginRequest
import kr.jiyeok.seatly.data.remote.request.RegisterRequest
import kr.jiyeok.seatly.data.remote.request.ReservationRequest
import kr.jiyeok.seatly.data.remote.request.ResetPasswordRequest
import kr.jiyeok.seatly.data.remote.request.SocialRegisterRequest
import kr.jiyeok.seatly.data.remote.request.StartSessionRequest
import kr.jiyeok.seatly.data.remote.request.UpdateUserRequest
import kr.jiyeok.seatly.data.remote.request.VerifyCodeRequest
import kr.jiyeok.seatly.di.IoDispatcher
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.HttpException
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SeatlyRepositoryImpl @Inject constructor(
    private val apiService: ApiService,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : SeatlyRepository {

    override suspend fun login(request: LoginRequest) = safeApiCall { apiService.login(request) }
    override suspend fun logout() = safeApiCall { apiService.logout() }
    override suspend fun register(request: RegisterRequest) = safeApiCall { apiService.register(request) }
    override suspend fun socialRegister(request: SocialRegisterRequest) = safeApiCall { apiService.socialRegister(request) }
    override suspend fun requestPasswordReset(request: ForgotPasswordRequest) = safeApiCall { apiService.requestPasswordReset(request) }
    override suspend fun verifyPasswordResetCode(request: VerifyCodeRequest) = safeApiCall { apiService.verifyPasswordResetCode(request) }
    override suspend fun resetPassword(request: ResetPasswordRequest) = safeApiCall { apiService.resetPassword(request) }

    override suspend fun getCurrentUser() = safeApiCall { apiService.getCurrentUser() }
    override suspend fun getUserInfo() = safeApiCall { apiService.getUserInfo() }
    override suspend fun updateUser(request: UpdateUserRequest) = safeApiCall { apiService.updateUser(request) }
    override suspend fun changePassword(request: ChangePasswordRequest) = safeApiCall { apiService.changePassword(request) }
    override suspend fun deleteAccount() = safeApiCall { apiService.deleteAccount() }
    override suspend fun getCurrentCafeUsage() = safeApiCall { apiService.getCurrentCafeUsage() }

    override suspend fun getFavoriteCafes(page: Int, size: Int) = safeApiCall { apiService.getFavoriteCafes(page, size) }
    override suspend fun addFavoriteCafe(cafeId: Long) = safeApiCall { apiService.addFavoriteCafe(cafeId) }
    override suspend fun removeFavoriteCafe(cafeId: Long) = safeApiCall { apiService.removeFavoriteCafe(cafeId) }
    override suspend fun getRecentCafes(limit: Int) = safeApiCall { apiService.getRecentCafes(limit) }

    override suspend fun getStudyCafes(page: Int, size: Int, search: String?, amenities: String?, openNow: Boolean?, sort: String?, lat: Double?, lng: Double?) =
        safeApiCall { apiService.getStudyCafes(page, size, search, amenities, openNow, sort, lat, lng) }

    override suspend fun getCafeSummary(cafeId: Long) = safeApiCall { apiService.getCafeSummary(cafeId) }
    override suspend fun getCafeDetail(cafeId: Long) = safeApiCall { apiService.getCafeDetail(cafeId) }

    override suspend fun getCafeSeats(cafeId: Long, status: String?) = safeApiCall { apiService.getCafeSeats(cafeId, status) }
    override suspend fun autoAssignSeat(cafeId: Long) = safeApiCall { apiService.autoAssignSeat(cafeId) }
    override suspend fun reserveSeat(cafeId: Long, request: ReservationRequest) = safeApiCall { apiService.reserveSeat(cafeId, request) }

    override suspend fun startSession(request: StartSessionRequest) = safeApiCall { apiService.startSession(request) }
    override suspend fun endSession(sessionId: Long) = safeApiCall { apiService.endSession(sessionId) }
    override suspend fun getCurrentSessions(studyCafeId: Long?) = safeApiCall { apiService.getCurrentSessions(studyCafeId) }

    override suspend fun getAdminCafes(page: Int, size: Int, search: String?) = safeApiCall { apiService.getAdminCafes(page, size, search) }
    override suspend fun getAdminCafeDetail(cafeId: Long) = safeApiCall { apiService.getAdminCafeDetail(cafeId) }

    override suspend fun createCafe(parts: Map<String, RequestBody>, images: List<MultipartBody.Part>) = safeApiCall { apiService.createCafe(parts, images) }
    override suspend fun updateCafe(cafeId: Long, parts: Map<String, RequestBody>, images: List<MultipartBody.Part>) = safeApiCall { apiService.updateCafe(cafeId, parts, images) }
    override suspend fun deleteCafe(cafeId: Long) = safeApiCall { apiService.deleteCafe(cafeId) }

    override suspend fun addSeat(cafeId: Long, request: kr.jiyeok.seatly.data.remote.request.AddSeatRequest) = safeApiCall { apiService.addSeat(cafeId, request) }
    override suspend fun editSeat(cafeId: Long, seatId: Long, request: kr.jiyeok.seatly.data.remote.request.EditSeatRequest) = safeApiCall { apiService.editSeat(cafeId, seatId, request) }
    override suspend fun deleteSeat(cafeId: Long, seatId: Long) = safeApiCall { apiService.deleteSeat(cafeId, seatId) }

    override suspend fun forceEndSession(sessionId: Long) = safeApiCall { apiService.forceEndSession(sessionId) }
    override suspend fun deleteUserFromCafe(cafeId: Long, userId: Long) = safeApiCall { apiService.deleteUserFromCafe(cafeId, userId) }

    private suspend fun <T> safeApiCall(call: suspend () -> kr.jiyeok.seatly.data.remote.response.ApiResponse<T>): ApiResult<T> {
        return withContext(ioDispatcher) {
            try {
                val response = call()
                if (response.success) {
                    ApiResult.Success(response.data)
                } else {
                    ApiResult.Failure(response.message ?: "Unknown API error")
                }
            } catch (t: Throwable) {
                val message = when (t) {
                    is IOException -> "Network error: ${t.localizedMessage ?: t.message}"
                    is HttpException -> "HTTP ${t.code()}: ${t.response()?.errorBody()?.string() ?: t.message()}"
                    else -> t.localizedMessage ?: "Unknown error"
                }
                ApiResult.Failure(message, t)
            }
        }
    }
}