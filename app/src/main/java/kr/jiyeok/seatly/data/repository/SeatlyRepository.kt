package kr.jiyeok.seatly.data.repository

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
import kr.jiyeok.seatly.data.remote.response.CurrentCafeUsageDto
import kr.jiyeok.seatly.data.remote.response.LoginResponseDTO
import kr.jiyeok.seatly.data.remote.response.PageResponse
import kr.jiyeok.seatly.data.remote.response.SeatResponseDto
import kr.jiyeok.seatly.data.remote.response.SessionResponseDto
import kr.jiyeok.seatly.data.remote.response.StudyCafeDetailDto
import kr.jiyeok.seatly.data.remote.response.StudyCafeSummaryDto
import kr.jiyeok.seatly.data.remote.response.UserResponseDto

interface SeatlyRepository {

    // Authentication
    suspend fun login(request: LoginRequest): ApiResult<LoginResponseDTO>
    suspend fun logout(): ApiResult<Unit>
    suspend fun register(request: RegisterRequest): ApiResult<UserResponseDto>
    suspend fun socialRegister(request: SocialRegisterRequest): ApiResult<UserResponseDto>
    suspend fun requestPasswordReset(request: ForgotPasswordRequest): ApiResult<Unit>
    suspend fun verifyPasswordResetCode(request: VerifyCodeRequest): ApiResult<Unit>
    suspend fun resetPassword(request: ResetPasswordRequest): ApiResult<Unit>

    // User
    suspend fun getCurrentUser(): ApiResult<UserResponseDto>
    suspend fun getUserInfo(): ApiResult<UserResponseDto>
    suspend fun updateUser(request: UpdateUserRequest): ApiResult<UserResponseDto>
    suspend fun changePassword(request: ChangePasswordRequest): ApiResult<Unit>
    suspend fun deleteAccount(): ApiResult<Unit>
    suspend fun getCurrentCafeUsage(): ApiResult<CurrentCafeUsageDto>

    // Favorites & Recent
    suspend fun getFavoriteCafes(page: Int = 0, size: Int = 20): ApiResult<PageResponse<StudyCafeSummaryDto>>
    suspend fun addFavoriteCafe(cafeId: Long): ApiResult<Unit>
    suspend fun removeFavoriteCafe(cafeId: Long): ApiResult<Unit>
    suspend fun getRecentCafes(limit: Int = 10): ApiResult<List<StudyCafeSummaryDto>>

    // Study Cafes
    suspend fun getStudyCafes(
        page: Int = 0,
        size: Int = 20,
        search: String? = null,
        amenities: String? = null,
        openNow: Boolean? = null,
        sort: String? = null,
        lat: Double? = null,
        lng: Double? = null
    ): ApiResult<PageResponse<StudyCafeSummaryDto>>

    suspend fun getCafeSummary(cafeId: Long): ApiResult<StudyCafeSummaryDto>
    suspend fun getCafeDetail(cafeId: Long): ApiResult<StudyCafeDetailDto>

    // Seats
    suspend fun getCafeSeats(cafeId: Long, status: String? = null): ApiResult<List<SeatResponseDto>>
    suspend fun autoAssignSeat(cafeId: Long): ApiResult<SessionResponseDto>
    suspend fun reserveSeat(cafeId: Long, request: ReservationRequest): ApiResult<SessionResponseDto>

    // Sessions
    suspend fun startSession(request: StartSessionRequest): ApiResult<SessionResponseDto>
    suspend fun endSession(sessionId: Long): ApiResult<SessionResponseDto>
    suspend fun getCurrentSessions(studyCafeId: Long? = null): ApiResult<List<SessionResponseDto>>

    // Admin
    suspend fun getAdminCafes(page: Int = 0, size: Int = 20, search: String? = null): ApiResult<PageResponse<StudyCafeSummaryDto>>
    suspend fun getAdminCafeDetail(cafeId: Long): ApiResult<StudyCafeDetailDto>

    suspend fun createCafe(parts: Map<String, @JvmSuppressWildcards okhttp3.RequestBody>, images: List<okhttp3.MultipartBody.Part> = emptyList()): ApiResult<StudyCafeDetailDto>
    suspend fun updateCafe(cafeId: Long, parts: Map<String, @JvmSuppressWildcards okhttp3.RequestBody>, images: List<okhttp3.MultipartBody.Part> = emptyList()): ApiResult<StudyCafeDetailDto>
    suspend fun deleteCafe(cafeId: Long): ApiResult<Unit>

    suspend fun addSeat(cafeId: Long, request: kr.jiyeok.seatly.data.remote.request.AddSeatRequest): ApiResult<SeatResponseDto>
    suspend fun editSeat(cafeId: Long, seatId: Long, request: kr.jiyeok.seatly.data.remote.request.EditSeatRequest): ApiResult<SeatResponseDto>
    suspend fun deleteSeat(cafeId: Long, seatId: Long): ApiResult<Unit>

    suspend fun forceEndSession(sessionId: Long): ApiResult<Unit>
    suspend fun deleteUserFromCafe(cafeId: Long, userId: Long): ApiResult<Unit>
}