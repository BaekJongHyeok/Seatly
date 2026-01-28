package kr.jiyeok.seatly.data.remote

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.http.*
import kr.jiyeok.seatly.data.remote.request.*
import kr.jiyeok.seatly.data.remote.response.*

class ApiService(private val client: HttpClient) {

    private val baseUrl = "http://3.27.78.54:8080/api/"

    // =====================================================
    // AuthController
    // =====================================================

    suspend fun login(request: LoginRequest): UserInfoSummaryDto =
        client.post("${baseUrl}auth/login") { setBody(request) }.body()

    suspend fun logout(): Unit =
        client.post("${baseUrl}auth/logout").body()

    // =====================================================
    // UserController
    // =====================================================

    suspend fun getUserInfo(): UserInfoSummaryDto =
        client.get("${baseUrl}user").body()

    suspend fun getFavoriteCafes(): List<Long> =
        client.get("${baseUrl}user/study-cafes/favorite").body()

    suspend fun getMyTimePasses(): List<UserTimePass> =
        client.get("${baseUrl}user/time-passes").body()

    suspend fun getCurrentSessions(): List<SessionDto> =
        client.get("${baseUrl}user/sessions").body()

    suspend fun updateUserInfo(request: UpdateUserInfoRequest): Unit =
        client.patch("${baseUrl}user") { setBody(request) }.body()

    suspend fun deleteAccount(): Unit =
        client.delete("${baseUrl}user").body()

    suspend fun register(request: RegisterRequest): Unit =
        client.post("${baseUrl}user") { setBody(request) }.body()

    suspend fun changePassword(request: ChangePasswordRequest): Unit =
        client.put("${baseUrl}user/password") { setBody(request) }.body()

    // =====================================================
    // UsersController (Admin)
    // =====================================================

    suspend fun getUsersInfo(studyCafeId: Long): List<UserTimePassInfo> =
        client.get("${baseUrl}users") { parameter("studyCafeId", studyCafeId) }.body()

    suspend fun getUsersInfoById(userId: Long): UserInfoSummaryDto =
        client.get("${baseUrl}users/$userId").body()

    suspend fun addUserTimePass(userId: Long, studyCafeId: Long, time: Long): Unit =
        client.post("${baseUrl}users/$userId/time") {
            parameter("studyCafeId", studyCafeId)
            parameter("time", time)
        }.body()

    // =====================================================
    // SessionController
    // =====================================================

    suspend fun getSessions(studyCafeId: Long): List<SessionDto> =
        client.get("${baseUrl}sessions") { parameter("studyCafeId", studyCafeId) }.body()

    suspend fun startSession(sessionId: Long): SessionDto =
        client.patch("${baseUrl}sessions/$sessionId/start").body()

    suspend fun endSession(sessionId: Long): Unit =
        client.delete("${baseUrl}sessions/$sessionId").body()

    suspend fun assignSeat(seatId: String): SessionDto =
        client.post("${baseUrl}sessions/assign") { parameter("seatId", seatId) }.body()

    suspend fun autoAssignSeat(studyCafeId: Long): SessionDto =
        client.post("${baseUrl}sessions/auto-assign") { parameter("studyCafeId", studyCafeId) }.body()

    // =====================================================
    // StudyCafeController
    // =====================================================

    suspend fun getStudyCafes(): List<StudyCafeSummaryDto> =
        client.get("${baseUrl}study-cafes").body()

    suspend fun getCafeDetail(cafeId: Long): StudyCafeDetailDto =
        client.get("${baseUrl}study-cafes/$cafeId").body()

    suspend fun createCafe(request: CreateCafeRequest): Unit =
        client.post("${baseUrl}study-cafes") { setBody(request) }.body()

    suspend fun updateCafe(cafeId: Long, request: UpdateCafeRequest): Unit =
        client.patch("${baseUrl}study-cafes/$cafeId") { setBody(request) }.body()

    suspend fun deleteCafe(cafeId: Long): Unit =
        client.delete("${baseUrl}study-cafes/$cafeId").body()

    suspend fun addFavoriteCafe(cafeId: Long): Unit =
        client.post("${baseUrl}study-cafes/$cafeId/favorite").body()

    suspend fun removeFavoriteCafe(cafeId: Long): Unit =
        client.delete("${baseUrl}study-cafes/$cafeId/favorite").body()

    suspend fun getCafeUsage(cafeId: Long): UsageDto =
        client.get("${baseUrl}study-cafes/$cafeId/usage").body()

    suspend fun deleteUserTimePass(cafeId: Long, userId: Long): Unit =
        client.delete("${baseUrl}study-cafes/$cafeId/users/$userId/time").body()

    suspend fun getAdminCafes(): List<StudyCafeSummaryDto> =
        client.get("${baseUrl}study-cafes/admin").body()

    // =====================================================
    // SeatController
    // =====================================================

    suspend fun getCafeSeats(cafeId: Long): List<SeatDto> =
        client.get("${baseUrl}study-cafes/$cafeId/seats").body()

    suspend fun createSeats(cafeId: Long, request: List<SeatCreate>): Unit =
        client.post("${baseUrl}study-cafes/$cafeId/seats") { setBody(request) }.body()

    suspend fun updateSeats(cafeId: Long, request: List<SeatUpdate>): Unit =
        client.patch("${baseUrl}study-cafes/$cafeId/seats") { setBody(request) }.body()

    suspend fun deleteSeat(cafeId: Long, seatId: String): Unit =
        client.delete("${baseUrl}study-cafes/$cafeId/seats/$seatId").body()

    // =====================================================
    // ImageController
    // =====================================================

    suspend fun uploadImage(fileName: String, content: ByteArray): String =
        client.post("${baseUrl}images/upload") {
            setBody(MultiPartFormDataContent(
                formData {
                    append("file", content, Headers.build {
                        append(HttpHeaders.ContentType, "image/jpeg")
                        append(HttpHeaders.ContentDisposition, "filename=$fileName")
                    })
                }
            ))
        }.body()

    suspend fun getImage(imageId: String): ByteArray =
        client.get("${baseUrl}images/$imageId").body()

    suspend fun deleteImage(imageId: String): Unit =
        client.delete("${baseUrl}images/$imageId").body()
}
