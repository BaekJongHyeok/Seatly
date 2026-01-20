package kr.jiyeok.seatly.domain.usecase

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import kr.jiyeok.seatly.data.remote.request.*
import kr.jiyeok.seatly.data.remote.response.*
import kr.jiyeok.seatly.data.repository.ApiResult
import kr.jiyeok.seatly.data.repository.SeatlyRepository
import kr.jiyeok.seatly.di.IoDispatcher
import okhttp3.MultipartBody
import javax.inject.Inject

/**
 * Seatly UseCase 모음
 * 모든 비즈니스 로직을 담당하는 UseCase들을 하나의 파일에 정리했습니다
 * 각 UseCase는 operator invoke()를 통해 호출됩니다
 */

// =====================================================
// Authentication UseCases
// =====================================================

/**
 * POST /auth/login
 * 로그인
 */
class LoginUseCase @Inject constructor(
    private val repository: SeatlyRepository,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) {
    suspend operator fun invoke(request: LoginRequest): ApiResult<UserInfoSummaryDto> =
        withContext(ioDispatcher) { repository.login(request) }
}

/**
 * POST /auth/logout
 * 로그아웃
 */
class LogoutUseCase @Inject constructor(
    private val repository: SeatlyRepository,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) {
    suspend operator fun invoke(): ApiResult<Unit> =
        withContext(ioDispatcher) { repository.logout() }
}

// =====================================================
// User UseCases
// =====================================================

/**
 * GET /user
 * 사용자 정보 조회
 */
class GetUserInfoUseCase @Inject constructor(
    private val repository: SeatlyRepository,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) {
    suspend operator fun invoke(): ApiResult<UserInfoSummaryDto> =
        withContext(ioDispatcher) { repository.getUserInfo() }
}

/**
 * Get /user/study-cafes/favorite
 * 즐겨찾기 카페 아이디 조회
 */
class GetFavoriteCafesUseCase @Inject constructor(
    private val repository: SeatlyRepository,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) {
    suspend operator fun invoke(): ApiResult<List<Long>> =
        withContext(ioDispatcher) { repository.getFavoriteCafes() }
}

/**
 * GET /user/time-passes
 * 내 시간권 조회
 */
class GetMyTimePassesUseCase @Inject constructor(
    private val repository: SeatlyRepository,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) {
    suspend operator fun invoke(): ApiResult<List<UserTimePass>> =
        withContext(ioDispatcher) { repository.getMyTimePasses() }
}

/**
 * GET /user/sessions
 * 유저의 현재 세션 정보 조회
 */
class GetCurrentSessions @Inject constructor(
    private val repository: SeatlyRepository,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) {
    suspend operator fun invoke(): ApiResult<List<SessionDto>> =
        withContext(ioDispatcher) { repository.getCurrentSessions() }
}

/**
 * PATCH /user
 * 사용자 정보 수정
 */
class UpdateUserInfoUseCase @Inject constructor(
    private val repository: SeatlyRepository,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) {
    suspend operator fun invoke(request: UpdateUserInfoRequest): ApiResult<Unit> =
        withContext(ioDispatcher) { repository.updateUserInfo(request) }
}

/**
 * DELETE /user
 * 회원탈퇴
 */
class DeleteAccountUseCase @Inject constructor(
    private val repository: SeatlyRepository,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) {
    suspend operator fun invoke(): ApiResult<Unit> =
        withContext(ioDispatcher) { repository.deleteAccount() }
}

/**
 * POST /user
 * 회원가입
 */
class RegisterUseCase @Inject constructor(
    private val repository: SeatlyRepository,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) {
    suspend operator fun invoke(request: RegisterRequest): ApiResult<Unit> =
        withContext(ioDispatcher) { repository.register(request) }
}

/**
 * PUT /user/{id}/password
 * 비밀번호 변경
 */
class ChangePasswordUseCase @Inject constructor(
    private val repository: SeatlyRepository,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) {
    suspend operator fun invoke(request: ChangePasswordRequest): ApiResult<Unit> =
        withContext(ioDispatcher) { repository.changePassword(request) }
}

// =====================================================
// Users (Admin) UseCases
// =====================================================

/**
 * GET /users?studyCafeId={id}
 * 시간권이 남아있는 사용자 목록
 */
class GetUsersWithTimePassUseCase @Inject constructor(
    private val repository: SeatlyRepository,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) {
    suspend operator fun invoke(studyCafeId: Long): ApiResult<List<UserTimePassInfo>> =
        withContext(ioDispatcher) { repository.getUsersInfo(studyCafeId) }
}

/**
 * GET /users/{id}
 * 사용자 정보 조회 (관리자)
 */
class GetUserInfoAdminUseCase @Inject constructor(
    private val repository: SeatlyRepository,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) {
    suspend operator fun invoke(userId: Long): ApiResult<UserInfoSummaryDto> =
        withContext(ioDispatcher) { repository.getUsersInfoById(userId) }
}

/**
 * POST /users/{id}/time
 * 관리지가 사용자에게 시간권 추가
 */
class AddUserTimePassUseCase @Inject constructor(
    private val repository: SeatlyRepository,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) {
    suspend operator fun invoke(userId: Long, studyCafeId: Long, time: Long): ApiResult<Unit> =
        withContext(ioDispatcher) { repository.addUserTimePass(userId, studyCafeId, time) }
}


// =====================================================
// Session UseCases
// =====================================================

/**
 * GET /sessions
 * 세션 목록 조회
 */
class GetSessionsUseCase @Inject constructor(
    private val repository: SeatlyRepository,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) {
    suspend operator fun invoke(studyCafeId: Long): ApiResult<List<SessionDto>> =
        withContext(ioDispatcher) { repository.getSessions(studyCafeId) }
}

/**
 * PATCH /sessions/{id}/start
 * 좌석 이용 시작
 */
class StartSessionUseCase @Inject constructor(
    private val repository: SeatlyRepository,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) {
    suspend operator fun invoke(sessionId: Long): ApiResult<SessionDto> =
        withContext(ioDispatcher) { repository.startSession(sessionId) }
}

/**
 * DELETE /sessions/{id}
 * 좌석 이용 종료
 */
class EndSessionUseCase @Inject constructor(
    private val repository: SeatlyRepository,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) {
    suspend operator fun invoke(sessionId: Long): ApiResult<Unit> =
        withContext(ioDispatcher) { repository.endSession(sessionId) }
}

/**
 * POST /sessions/assign?seatId={seatId}
 * 좌석 선택 (수동 할당)
 */
class AssignSeatUseCase @Inject constructor(
    private val repository: SeatlyRepository,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) {
    suspend operator fun invoke(seatId: String): ApiResult<SessionDto> =
        withContext(ioDispatcher) { repository.assignSeat(seatId) }
}

/**
 * POST /sessions/auto-assign?seatId={seatId}
 * 좌석 자동 할당
 */
class AutoAssignSeatUseCase @Inject constructor(
    private val repository: SeatlyRepository,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) {
    suspend operator fun invoke(studyCafeId: Long): ApiResult<SessionDto> =
        withContext(ioDispatcher) { repository.autoAssignSeat(studyCafeId) }
}

// =====================================================
// Study Cafe UseCases
// =====================================================

/**
 * GET /study-cafes
 * 전체 카페 목록 조회
 */
class GetStudyCafesUseCase @Inject constructor(
    private val repository: SeatlyRepository,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) {
    suspend operator fun invoke(): ApiResult<List<StudyCafeSummaryDto>> =
        withContext(ioDispatcher) { repository.getStudyCafes() }
}

/**
 * GET /study-cafes/{id}
 * 카페 상세 정보 조회
 */
class GetCafeDetailUseCase @Inject constructor(
    private val repository: SeatlyRepository,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) {
    suspend operator fun invoke(cafeId: Long): ApiResult<StudyCafeDetailDto> =
        withContext(ioDispatcher) { repository.getCafeDetail(cafeId) }
}

/**
 * POST /study-cafes
 * 카페 추가 (관리자)
 */
class CreateCafeUseCase @Inject constructor(
    private val repository: SeatlyRepository,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) {
    suspend operator fun invoke(request: CreateCafeRequest): ApiResult<Unit> =
        withContext(ioDispatcher) { repository.createCafe(request) }
}

/**
 * PATCH /study-cafes/{id}
 * 카페 정보 수정 (관리자)
 */
class UpdateCafeUseCase @Inject constructor(
    private val repository: SeatlyRepository,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) {
    suspend operator fun invoke(
        cafeId: Long,
        request: UpdateCafeRequest
    ): ApiResult<Unit> =
        withContext(ioDispatcher) { repository.updateCafe(cafeId, request) }
}

/**
 * DELETE /study-cafes/{id}
 * 카페 삭제 (관리자)
 */
class DeleteCafeUseCase @Inject constructor(
    private val repository: SeatlyRepository,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) {
    suspend operator fun invoke(cafeId: Long): ApiResult<Unit> =
        withContext(ioDispatcher) { repository.deleteCafe(cafeId) }
}

/**
 * POST /study-cafes/{id}/favorite
 * 카페 즐겨찾기 추가
 */
class AddFavoriteCafeUseCase @Inject constructor(
    private val repository: SeatlyRepository,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) {
    suspend operator fun invoke(cafeId: Long): ApiResult<Unit> =
        withContext(ioDispatcher) { repository.addFavoriteCafe(cafeId) }
}

/**
 * DELETE /study-cafes/{id}/favorite
 * 카페 즐겨찾기 제거
 */
class RemoveFavoriteCafeUseCase @Inject constructor(
    private val repository: SeatlyRepository,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) {
    suspend operator fun invoke(cafeId: Long): ApiResult<Unit> =
        withContext(ioDispatcher) { repository.removeFavoriteCafe(cafeId) }
}

/**
 * GET /study-cafes/{id}/usage
 * 카페 실시간 혼잡도 조회
 */
class GetCafeUsageUseCase @Inject constructor(
    private val repository: SeatlyRepository,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) {
    suspend operator fun invoke(cafeId: Long): ApiResult<UsageDto> =
        withContext(ioDispatcher) { repository.getCafeUsage(cafeId) }
}

/**
 * DELETE /study-cafes/{id}/users/{userId}/time
 * 회원의 남은 시간권 삭제 (관리자)
 */
class DeleteUserTimePassUseCase @Inject constructor(
    private val repository: SeatlyRepository,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) {
    suspend operator fun invoke(cafeId: Long, userId: Long): ApiResult<Unit> =
        withContext(ioDispatcher) { repository.deleteUserTimePass(cafeId, userId) }
}

/**
 * GET /study-cafes/admin
 * 관리자가 관리하는 카페 목록
 */
class GetAdminCafesUseCase @Inject constructor(
    private val repository: SeatlyRepository,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) {
    suspend operator fun invoke(): ApiResult<List<StudyCafeSummaryDto>> =
        withContext(ioDispatcher) { repository.getAdminCafes() }
}

// =====================================================
// Seat UseCases
// =====================================================

/**
 * GET /study-cafes/{id}/seats
 * 좌석 정보 조회
 */
class GetCafeSeatsUseCase @Inject constructor(
    private val repository: SeatlyRepository,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) {
    suspend operator fun invoke(cafeId: Long): ApiResult<List<SeatDto>> =
        withContext(ioDispatcher) { repository.getCafeSeats(cafeId) }
}

/**
 * POST /study-cafes/{id}/seats
 * 좌석 추가 (관리자, 리스트로 여러 개 한번에)
 */
class CreateSeatsUseCase @Inject constructor(
    private val repository: SeatlyRepository,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) {
    suspend operator fun invoke(
        cafeId: Long,
        request: List<SeatCreate>
    ): ApiResult<Unit> =
        withContext(ioDispatcher) { repository.createSeats(cafeId, request) }
}

/**
 * PATCH /study-cafes/{id}/seats
 * 좌석 정보 수정 (관리자, 리스트로 여러 개 한번에)
 */
class UpdateSeatsUseCase @Inject constructor(
    private val repository: SeatlyRepository,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) {
    suspend operator fun invoke(
        cafeId: Long,
        request: List<SeatUpdate>
    ): ApiResult<Unit> =
        withContext(ioDispatcher) { repository.updateSeats(cafeId, request) }
}

/**
 * DELETE /study-cafes/{id}/seats/{seatId}
 * 좌석 삭제 (관리자)
 */
class DeleteSeatUseCase @Inject constructor(
    private val repository: SeatlyRepository,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) {
    suspend operator fun invoke(
        cafeId: Long,
        seatId: String
    ): ApiResult<Unit> =
        withContext(ioDispatcher) { repository.deleteSeat(cafeId, seatId) }
}

// =====================================================
// Image UseCases
// =====================================================

/**
 * POST /images/upload
 * 이미지 업로드
 */
class UploadImageUseCase @Inject constructor(
    private val repository: SeatlyRepository,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) {
    suspend operator fun invoke(file: MultipartBody.Part): ApiResult<String> =
        withContext(ioDispatcher) { repository.uploadImage(file) }
}

/**
 * GET /images/{imageId}
 * 이미지 조회 (다운로드)
 */
class GetImageUseCase @Inject constructor(
    private val repository: SeatlyRepository,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) {
    suspend operator fun invoke(imageId: String): ApiResult<ByteArray> =
        withContext(ioDispatcher) { repository.getImage(imageId) }
}

/**
 * DELETE /images/{imageId}
 * 이미지 삭제
 */
class DeleteImageUseCase @Inject constructor(
    private val repository: SeatlyRepository,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) {
    suspend operator fun invoke(imageId: String): ApiResult<Unit> =
        withContext(ioDispatcher) { repository.deleteImage(imageId) }
}
