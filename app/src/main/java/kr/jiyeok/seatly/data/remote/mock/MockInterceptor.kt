package kr.jiyeok.seatly.data.remote.mock

import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import com.google.gson.Gson
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

// MockInterceptor.kt 상단에 추가
import okio.Buffer

// 로그인 요청 JSON 모델
data class LoginRequestJson(
    val email: String,
    val password: String
)

class MockInterceptor : Interceptor {
    private val gson = Gson()

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val path = originalRequest.url.toUri().path ?: ""
        val method = originalRequest.method

        val mockResponse = when {
            // =========== 로그인 ===========
            path.endsWith("/auth/login") && method == "POST" -> {
                // 요청 본문 파싱
                val requestBody = chain.request().body?.let { body ->
                    val source = okio.Buffer()
                    body.writeTo(source)
                    source.readUtf8()
                } ?: ""

                // 요청 JSON 파싱
                val loginRequest = try {
                    gson.fromJson(requestBody, LoginRequestJson::class.java)
                } catch (e: Exception) {
                    null
                }

                // 올바른 자격증명 확인
                if (loginRequest?.email == "user@example.com" && loginRequest?.password == "password123") {
                    createMockResponse(200, mapOf(
                        "success" to true,
                        "message" to "로그인 성공",
                        "data" to mapOf(
                            "id" to 1,
                            "email" to "user@example.com",
                            "name" to "김테스트",
                            "role" to "USER",
                            "token" to "mock_jwt_token_12345",
                            "created_at" to getCurrentDateTime()
                        )
                    ))
                } else {
                    // 실패 응답
                    createMockResponse(400, mapOf(
                        "success" to false,
                        "message" to "이메일 또는 비밀번호가 올바르지 않습니다",
                        "data" to null
                    ))
                }
            }


            // =========== 사용자 정보 ===========
            path.endsWith("/user-info") && method == "GET" -> {
                createMockResponse(200, mapOf(
                    "success" to true,
                    "message" to "사용자 정보 조회 성공",
                    "data" to mapOf(
                        "id" to 1,
                        "email" to "user@example.com",
                        "name" to "김테스트",
                        "phone" to "010-1234-5678",
                        "credits" to 5000,
                        "membership_level" to "STANDARD",
                        "created_at" to getCurrentDateTime()
                    )
                ))
            }

            // =========== 스터디 카페 목록 ===========
            path.endsWith("/study-cafes") && method == "GET" -> {
                createMockResponse(200, mapOf(
                    "success" to true,
                    "message" to "스터디 카페 목록 조회 성공",
                    "data" to listOf(
                        mapOf(
                            "id" to 1,
                            "name" to "스터디 카페 강남",
                            "address" to "서울시 강남구 테헤란로 123",
                            "phone" to "02-1234-5678",
                            "total_seats" to 50,
                            "available_seats" to 15,
                            "hourly_rate" to 8000,
                            "facilities" to listOf("WiFi", "Locker", "Toilet"),
                            "latitude" to 37.4979,
                            "longitude" to 127.0276,
                            "image_url" to "https://example.com/cafe1.jpg"
                        ),
                        mapOf(
                            "id" to 2,
                            "name" to "스터디 카페 서초",
                            "address" to "서울시 서초구 사임로 123",
                            "phone" to "02-9876-5432",
                            "total_seats" to 40,
                            "available_seats" to 8,
                            "hourly_rate" to 7500,
                            "facilities" to listOf("WiFi", "Locker", "Toilet", "Cafe"),
                            "latitude" to 37.4829,
                            "longitude" to 127.0162,
                            "image_url" to "https://example.com/cafe2.jpg"
                        ),
                        mapOf(
                            "id" to 3,
                            "name" to "스터디 카페 종로",
                            "address" to "서울시 종로구 종로 123",
                            "phone" to "02-5555-5555",
                            "total_seats" to 60,
                            "available_seats" to 22,
                            "hourly_rate" to 9000,
                            "facilities" to listOf("WiFi", "Locker", "Toilet", "Cafe", "Printer"),
                            "latitude" to 37.5707,
                            "longitude" to 126.9819,
                            "image_url" to "https://example.com/cafe3.jpg"
                        )
                    )
                ))
            }

            // =========== 카페 좌석 목록 ===========
            path.contains("/study-cafes/") && path.endsWith("/seats") && method == "GET" && !path.contains("/auto-assign") && !path.contains("/reservation") -> {
                val cafeId = extractPathParam(path, "/study-cafes/", "/seats")
                createMockResponse(200, mapOf(
                    "success" to true,
                    "message" to "좌석 목록 조회 성공",
                    "data" to generateMockSeats(cafeId?.toLongOrNull() ?: 1)
                ))
            }

            // =========== 좌석 자동 배정 ===========
            path.contains("/auto-assign") && method == "POST" -> {
                createMockResponse(200, mapOf(
                    "success" to true,
                    "message" to "좌석 자동 배정 성공",
                    "data" to mapOf(
                        "id" to 1,
                        "seat_number" to 12,
                        "cafe_id" to 1,
                        "status" to "RESERVED",
                        "reserved_by" to 1,
                        "reserved_at" to getCurrentDateTime()
                    )
                ))
            }

            // =========== 좌석 선택 예약 ===========
            path.contains("/reservation") && method == "POST" -> {
                createMockResponse(200, mapOf(
                    "success" to true,
                    "message" to "좌석 예약 성공",
                    "data" to mapOf(
                        "id" to 1,
                        "seat_number" to 15,
                        "cafe_id" to 1,
                        "status" to "RESERVED",
                        "reserved_by" to 1,
                        "reserved_at" to getCurrentDateTime()
                    )
                ))
            }

            // =========== 현재 세션 조회 ===========
            path.endsWith("/sessions") && method == "GET" -> {
                createMockResponse(200, mapOf(
                    "success" to true,
                    "message" to "세션 목록 조회 성공",
                    "data" to listOf(
                        mapOf(
                            "id" to 1,
                            "user_id" to 1,
                            "seat_id" to 1,
                            "cafe_id" to 1,
                            "start_time" to getTimeString(9, 0),
                            "end_time" to null,
                            "status" to "ACTIVE",
                            "cost" to 0
                        ),
                        mapOf(
                            "id" to 2,
                            "user_id" to 2,
                            "seat_id" to 3,
                            "cafe_id" to 1,
                            "start_time" to getTimeString(10, 30),
                            "end_time" to null,
                            "status" to "ACTIVE",
                            "cost" to 0
                        )
                    )
                ))
            }

            // =========== 세션 시작 ===========
            path.endsWith("/sessions/start") && method == "POST" -> {
                createMockResponse(200, mapOf(
                    "success" to true,
                    "message" to "이용 시작 성공",
                    "data" to mapOf(
                        "id" to 1,
                        "user_id" to 1,
                        "seat_id" to 1,
                        "cafe_id" to 1,
                        "start_time" to getCurrentDateTime(),
                        "end_time" to null,
                        "status" to "ACTIVE",
                        "cost" to 0
                    )
                ))
            }

            // =========== 세션 종료 ===========
            path.contains("/sessions/") && path.endsWith("/end") && method == "POST" -> {
                createMockResponse(200, mapOf(
                    "success" to true,
                    "message" to "이용 종료 성공",
                    "data" to mapOf(
                        "id" to 1,
                        "user_id" to 1,
                        "seat_id" to 1,
                        "cafe_id" to 1,
                        "start_time" to getTimeString(9, 0),
                        "end_time" to getCurrentDateTime(),
                        "status" to "COMPLETED",
                        "cost" to 16000,
                        "duration_hours" to 2
                    )
                ))
            }

            // =========== 좌석 추가 (관리자) ===========
            path.contains("/study-cafes/") && path.endsWith("/seats") && method == "POST" && !path.contains("/auto-assign") && !path.contains("/reservation") -> {
                createMockResponse(200, mapOf(
                    "success" to true,
                    "message" to "좌석 추가 성공",
                    "data" to mapOf(
                        "id" to 51,
                        "seat_number" to 51,
                        "cafe_id" to 1,
                        "position" to "WINDOW",
                        "type" to "STANDARD",
                        "status" to "AVAILABLE",
                        "created_at" to getCurrentDateTime()
                    )
                ))
            }

            // =========== 좌석 삭제 (관리자) ===========
            path.contains("/study-cafes/") && path.contains("/seats/") && method == "DELETE" && !path.contains("/users/") -> {
                createMockResponse(200, mapOf(
                    "success" to true,
                    "message" to "좌석 삭제 성공",
                    "data" to null
                ))
            }

            // =========== 세션 강제 종료 (관리자) ===========
            path.contains("/sessions/") && method == "DELETE" && !path.contains("/study-cafes") -> {
                createMockResponse(200, mapOf(
                    "success" to true,
                    "message" to "세션 강제 종료 성공",
                    "data" to null
                ))
            }

            // =========== 회원 탈퇴 (관리자) ===========
            path.contains("/study-cafes/") && path.contains("/users/") && method == "DELETE" -> {
                createMockResponse(200, mapOf(
                    "success" to true,
                    "message" to "회원 탈퇴 성공",
                    "data" to null
                ))
            }

            // 매치되는 경로 없을 때
            else -> {
                createMockResponse(404, mapOf(
                    "success" to false,
                    "message" to "API 엔드포인트를 찾을 수 없습니다: $method $path",
                    "data" to null
                ))
            }
        }

        return mockResponse
    }

    private fun createMockResponse(code: Int, body: Any): Response {
        val jsonBody = gson.toJson(body)
        return Response.Builder()
            .code(code)
            .protocol(Protocol.HTTP_1_1)
            .message("Mock Response")
            .body(jsonBody.toResponseBody("application/json".toMediaTypeOrNull()))
            .addHeader("content-type", "application/json")
            .request(Request.Builder().url("http://mock/").build())
            .build()
    }

    private fun generateMockSeats(cafeId: Long): List<Map<String, Any?>> {
        return (1..20).map { seatNum ->
            mapOf(
                "id" to (cafeId * 100 + seatNum),
                "seat_number" to seatNum,
                "cafe_id" to cafeId,
                "position" to if (seatNum % 3 == 0) "WINDOW" else "INNER",
                "type" to if (seatNum % 5 == 0) "PREMIUM" else "STANDARD",
                "status" to if (seatNum % 4 == 0) "RESERVED" else "AVAILABLE",
                "reserved_by" to if (seatNum % 4 == 0) 1 else null,
                "reserved_at" to if (seatNum % 4 == 0) getCurrentDateTime() else null
            )
        }
    }

    private fun extractPathParam(path: String, prefix: String, suffix: String): String? {
        val startIndex = path.indexOf(prefix) + prefix.length
        val endIndex = path.indexOf(suffix, startIndex)
        return if (startIndex > prefix.length - 1 && endIndex > startIndex) {
            path.substring(startIndex, endIndex)
        } else null
    }

    private fun getCurrentDateTime(): String {
        return LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
    }

    private fun getTimeString(hour: Int, minute: Int): String {
        val now = LocalDateTime.now()
        return now.withHour(hour).withMinute(minute).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
    }
}