package kr.jiyeok.seatly.di

import android.util.Log
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody

class DebugMockInterceptor : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val req = chain.request()
        val path = req.url.encodedPath ?: ""
        val method = req.method.uppercase()

        Log.d("MockInterceptor", "--> Request: $method $path")

        // 1. 로그인 (POST /auth/login)
        // ApiResponse<UserInfoDetailDto>
        if (method == "POST" && path.contains("/auth/login")) {
            Log.d("MockInterceptor", "<-- Mocking Login Response")
            val json = """
                {
                    "email": "user@test.com",
                    "name": "일반 사용자",
                    "phone": "010-9876-5432",
                    "imageUrl": null,
                    "role": "ADMIN"
                }
            """.trimIndent()
            return makeResponse(req, json)
        }

        // 2. 로그아웃 (POST /auth/logout)
        if (method == "POST" && path.contains("/auth/logout")) {
            Log.d("MockInterceptor", "<-- Mocking Logout Response")
            val json = """
                {
                    "success": true,
                    "message": "로그아웃 성공",
                    "data": null
                }
            """.trimIndent()
            return makeResponse(req, json)
        }

        // 3-1. 즐겨찾기 카페 아이디 조회
        if (method == "GET" && path.contains("/user/study-cafes/favorite") && !path.contains("/users")) {
            Log.d("MockInterceptor", "<-- Mocking User Favorite Cafes Response")
            val json = """
                {
                  "success": true,
                  "message": "즐겨찾기 카페 조회 성공",
                  "data": [1, 3, 4]
                }
            """.trimIndent()
            return makeResponse(req, json)
        }


        // 3-2. 내 시간권 조회
        if (method == "GET" && path.contains("/user/time-passes") && !path.contains("/users")) {
            Log.d("MockInterceptor", "<-- Mocking User Time Passes Response")
            val json = """
                {
                    "success": true,
                    "message": "시간권 조회 성공",
                    "data":  [
                        {
                            "studyCafeId": 1,
                            "leftTime": 14400000,
                            "totalTime": 28800000
                        }
                    ]
                }
            """.trimIndent()
            return makeResponse(req, json)
        }


        // 3-3. 내 현재 세션 조회
        if (method == "GET" && path.contains("/user/sessions") && !path.contains("/users")) {
            Log.d("MockInterceptor", "<-- Mocking User Current Sessions Response")
            val json = """
                {
                    "success": true,
                    "message": "로그아웃 성공",
                    "data": [
                        {
                            "id": 101,
                            "userId": 1,
                            "studyCafeId": 1,
                            "seatId": 1,
                            "status": "IN_USE",
                            "startTime": "${getISOTimeString(System.currentTimeMillis() - 3600000)}"
                        }
                    ]   
                }
            """.trimIndent()
            return makeResponse(req, json)
        }

        // 3. 내 정보 조회 (GET /user)
        if (method == "GET" && path.contains("/user") && !path.contains("/users")) {
            Log.d("MockInterceptor", "<-- Mocking User Info Response")
            val json = """
                {
                    "success": true,
                    "message": null,
                    "success": true,
                    "message": null,
                    "data": {
                        "email": "user@test.com",
                        "name": "일반 사용자",
                        "phone": "010-9876-5432",
                        "imageUrl": null,
                        "role": "USER"
                    }
                }
            """.trimIndent()
            return makeResponse(req, json)
        }

        // 4. 사용자 정보 수정 (PATCH /user)
        if (method == "PATCH" && path.contains("/user") && !path.contains("/password")) {
            Log.d("MockInterceptor", "<-- Mocking Update User Info Response")
            val json = """
                {
                    "success": true,
                    "message": "사용자 정보 수정 완료",
                    "data": {
                        "email": "user@test.com",
                        "name": "수정된 사용자",
                        "phone": "010-1111-2222",
                        "imageUrl": null,
                        "role": "USER",
                        "favoriteCafeIds": [1, 3],
                        "sessions": [],
                        "timePassess": []
                    }
                }
            """.trimIndent()
            return makeResponse(req, json)
        }

        // 5. 비밀번호 변경 (PUT /user/password)
        if (method == "PUT" && path.contains("/user/password")) {
            Log.d("MockInterceptor", "<-- Mocking Change Password Response")
            val json = """
                {
                    "success": true,
                    "message": "비밀번호 변경 완료",
                    "data": null
                }
            """.trimIndent()
            return makeResponse(req, json)
        }

        // 6. 회원 탈퇴 (DELETE /user)
        if (method == "DELETE" && path.contains("/user") && !path.contains("/users")) {
            Log.d("MockInterceptor", "<-- Mocking Delete Account Response")
            val json = """
                {
                    "success": true,
                    "message": "회원탈퇴 완료",
                    "data": null
                }
            """.trimIndent()
            return makeResponse(req, json)
        }

        // 7. 회원가입 (POST /user)
        if (method == "POST" && path.contains("/user") && !path.contains("/users")) {
            Log.d("MockInterceptor", "<-- Mocking Register Response")
            val json = """
                {
                    "success": true,
                    "message": "회원가입 완료",
                    "data": {
                        "email": "newuser@test.com",
                        "name": "새로운 사용자",
                        "phone": "010-1234-5678",
                        "imageUrl": null,
                        "role": "USER",
                        "favoriteCafeIds": [],
                        "sessions": [],
                        "timePassess": []
                    }
                }
            """.trimIndent()
            return makeResponse(req, json)
        }

        // 8. 사용자 목록 조회 (GET /users?studyCafeId=...)
        if (method == "GET" && path.contains("/users") && path.contains("studyCafeId")) {
            Log.d("MockInterceptor", "<-- Mocking Users Info Response")
            val json = """
                {
                    "success": true,
                    "message": null,
                    "data": [
                        {
                            "id": 1,
                            "name": "일반 사용자",
                            "cafeId": 1,
                            "leftTime": 14400000,
                            "totalTime": 28800000
                        },
                        {
                            "id": 2,
                            "name": "다른 사용자",
                            "cafeId": 1,
                            "leftTime": 7200000,
                            "totalTime": 14400000
                        }
                    ]
                }
            """.trimIndent()
            return makeResponse(req, json)
        }

        // 9. 특정 사용자 조회 (GET /users/{id})
        if (method == "GET" && path.matches(Regex(".*users/\\d+$"))) {
            Log.d("MockInterceptor", "<-- Mocking User Detail Response")
            val userId = path.substringAfterLast("/").toLongOrNull() ?: 1L
            val json = """
                {
                    "success": true,
                    "message": null,
                    "data": {
                        "email": "user$userId@test.com",
                        "name": "사용자 $userId",
                        "phone": "010-1111-$userId",
                        "imageUrl": null,
                        "role": "USER",
                        "favoriteCafeIds": [],
                        "sessions": [],
                        "timePassess": []
                    }
                }
            """.trimIndent()
            return makeResponse(req, json)
        }

        // 10. 카페 목록 조회 (GET /study-cafes)
        if (method == "GET" && path.contains("study-cafes") && !path.matches(Regex(".*study-cafes/\\d+.*")) && !path.contains("/admin")) {
            Log.d("MockInterceptor", "<-- Mocking Cafe List Response")
            val json = """
                {
                    "success": true,
                    "message": null,
                    "data": [
                        {
                            "id": 1,
                            "name": "수원 스터디카페",
                            "address": "경기도 수원시 팔달구 마곡로 100",
                            "mainImageUrl": "https://images.unsplash.com/photo-1554118811-1e0d58224f24"
                        },
                        {
                            "id": 2,
                            "name": "더존 프리미엄 독서실",
                            "address": "서울시 강남구 테헤란로 123",
                            "mainImageUrl": "https://images.unsplash.com/photo-1521017432531-fbd92d768814"
                        },
                        {
                            "id": 3,
                            "name": "어반 라이브러리",
                            "address": "서울시 마포구 양화로 45",
                            "mainImageUrl": "https://images.unsplash.com/photo-1497366754035-f200968a6e72"
                        },
                        {
                            "id": 4,
                            "name": "포커스 온",
                            "address": "경기도 성남시 분당구 판교로 67",
                            "mainImageUrl": "https://images.unsplash.com/photo-1517502884422-41eaead166d4"
                        },
                        {
                            "id": 5,
                            "name": "스타벅스 스터디룸",
                            "address": "서울시 서초구 서초대로 89",
                            "mainImageUrl": "https://images.unsplash.com/photo-1481277542470-605612bd2d61"
                        }
                    ]
                }
            """.trimIndent()
            return makeResponse(req, json)
        }

        // 11. 관리자 카페 목록 조회 (GET /study-cafes/admin)
        if (method == "GET" && path.contains("/study-cafes/admin")) {
            Log.d("MockInterceptor", "<-- Mocking Admin Cafe List Response")
            val json = """
                {
                    "success": true,
                    "message": null,
                    "data": [
                        {
                            "id": 1,
                            "name": "명지 스터디카페",
                            "address": "서울시 강서구 마곡로 100",
                            "mainImageUrl": "https://images.unsplash.com/photo-1554118811-1e0d58224f24"
                        },
                        {
                            "id": 2,
                            "name": "더존 프리미엄 독서실",
                            "address": "서울시 강남구 테헤란로 123",
                            "mainImageUrl": "https://images.unsplash.com/photo-1521017432531-fbd92d768814"
                        }
                    ]
                }
            """.trimIndent()
            return makeResponse(req, json)
        }

        // 12. 카페 상세 조회 (GET /study-cafes/{id})
        if (method == "GET" && path.matches(Regex(".*study-cafes/\\d+$"))) {
            Log.d("MockInterceptor", "<-- Mocking Cafe Detail Response")
            val id = path.substringAfterLast("/").toLongOrNull() ?: 1L
            val json = """
                {
                    "success": true,
                    "message": null,
                    "data": {
                        "id": $id,
                        "name": "명지 스터디카페 (상세)",
                        "address": "서울시 강서구 마곡로 100",
                        "imageUrls": [
                            "https://images.unsplash.com/photo-1554118811-1e0d58224f24"
                        ],
                        "phone": "010-1234-5678",
                        "facilities": ["WIFI", "AIRCONDITION", "CAFE"],
                        "openingHours": "24시간",
                        "description": "학습하기 좋은 공간입니다."
                    }
                }
            """.trimIndent()
            return makeResponse(req, json)
        }

        // 13. 카페 생성 (POST /study-cafes)
        if (method == "POST" && path.contains("study-cafes") && !path.contains("/")) {
            Log.d("MockInterceptor", "<-- Mocking Create Cafe Response")
            val json = """
                {
                    "success": true,
                    "message": "카페 생성 완료",
                    "data": {
                        "id": 6,
                        "name": "새로운 스터디카페",
                        "address": "서울시 강남구 새로운로 1",
                        "imageUrls": [],
                        "phone": "010-5555-5555",
                        "facilities": [],
                        "openingHours": "09:00 ~ 22:00",
                        "description": "새로 오픈한 카페입니다."
                    }
                }
            """.trimIndent()
            return makeResponse(req, json)
        }

        // 14. 카페 수정 (PATCH /study-cafes/{id})
        if (method == "PATCH" && path.matches(Regex(".*study-cafes/\\d+$"))) {
            Log.d("MockInterceptor", "<-- Mocking Update Cafe Response")
            val id = path.substringAfterLast("/").toLongOrNull() ?: 1L
            val json = """
                {
                    "success": true,
                    "message": "카페 정보 수정 완료",
                    "data": {
                        "id": $id,
                        "name": "수정된 스터디카페",
                        "address": "서울시 강서구 수정로 100",
                        "imageUrls": [],
                        "phone": "010-9999-9999",
                        "facilities": ["WIFI", "AIRCONDITIONING"],
                        "openingHours": "06:00 ~ 23:00",
                        "description": "수정된 설명입니다."
                    }
                }
            """.trimIndent()
            return makeResponse(req, json)
        }

        // 15. 카페 삭제 (DELETE /study-cafes/{id})
        if (method == "DELETE" && path.matches(Regex(".*study-cafes/\\d+$"))) {
            Log.d("MockInterceptor", "<-- Mocking Delete Cafe Response")
            val json = """
                {
                    "success": true,
                    "message": "카페 삭제 완료",
                    "data": null
                }
            """.trimIndent()
            return makeResponse(req, json)
        }

        // 16. 카페 즐겨찾기 추가 (POST /study-cafes/{id}/favorite)
        if (method == "POST" && path.contains("favorite")) {
            Log.d("MockInterceptor", "<-- Mocking Add Favorite Response")
            val json = """
                {
                    "success": true,
                    "message": "즐겨찾기 추가 완료",
                    "data": null
                }
            """.trimIndent()
            return makeResponse(req, json)
        }

        // 17. 카페 즐겨찾기 제거 (DELETE /study-cafes/{id}/favorite)
        if (method == "DELETE" && path.contains("favorite")) {
            Log.d("MockInterceptor", "<-- Mocking Remove Favorite Response")
            val json = """
                {
                    "success": true,
                    "message": "즐겨찾기 제거 완료",
                    "data": null
                }
            """.trimIndent()
            return makeResponse(req, json)
        }

        // 18. 카페 실시간 혼잡도 조회 (GET /study-cafes/{id}/usage)
        if (method == "GET" && path.contains("usage")) {
            Log.d("MockInterceptor", "<-- Mocking Cafe Usage Response")
            val json = """
                {
                    "success": true,
                    "message": null,
                    "data": {
                        "totalCount": 50,
                        "useCount": 35
                    }
                }
            """.trimIndent()
            return makeResponse(req, json)
        }

        // 19. 좌석 목록 조회 (GET /study-cafes/{id}/seats)
        if (method == "GET" && path.contains("/seats") && !path.contains("/{seatId}")) {
            Log.d("MockInterceptor", "<-- Mocking Cafe Seats Response")
            val json = """
                {
                    "success": true,
                    "message": null,
                    "data": [
                        {
                            "id": 1,
                            "name": "A열 1번",
                            "status": "AVAILABLE",
                            "position": "창가"
                        },
                        {
                            "id": 2,
                            "name": "A열 2번",
                            "status": "AVAILABLE",
                            "position": "창가"
                        },
                        {
                            "id": 3,
                            "name": "A열 3번",
                            "status": "IN_USE",
                            "position": "창가"
                        },
                        {
                            "id": 4,
                            "name": "B열 1번",
                            "status": "AVAILABLE",
                            "position": "복도"
                        },
                        {
                            "id": 5,
                            "name": "B열 2번",
                            "status": "BROKEN",
                            "position": "복도"
                        }
                    ]
                }
            """.trimIndent()
            return makeResponse(req, json)
        }

        // 20. 좌석 추가 (POST /study-cafes/{id}/seats)
        if (method == "POST" && path.contains("/seats")) {
            Log.d("MockInterceptor", "<-- Mocking Create Seats Response")
            val json = """
                {
                    "success": true,
                    "message": "좌석 생성 완료",
                    "data": [
                        {
                            "id": 6,
                            "name": "C열 1번",
                            "status": "AVAILABLE",
                            "position": "중앙"
                        },
                        {
                            "id": 7,
                            "name": "C열 2번",
                            "status": "AVAILABLE",
                            "position": "중앙"
                        }
                    ]
                }
            """.trimIndent()
            return makeResponse(req, json)
        }

        // 21. 좌석 수정 (PATCH /study-cafes/{id}/seats)
        if (method == "PATCH" && path.contains("/seats")) {
            Log.d("MockInterceptor", "<-- Mocking Update Seats Response")
            val json = """
                {
                    "success": true,
                    "message": "좌석 수정 완료",
                    "data": [
                        {
                            "id": 1,
                            "name": "A열 1번 (수정)",
                            "status": "AVAILABLE",
                            "position": "창가"
                        }
                    ]
                }
            """.trimIndent()
            return makeResponse(req, json)
        }

        // 22. 좌석 삭제 (DELETE /study-cafes/{id}/seats/{seatId})
        if (method == "DELETE" && path.contains("/seats/")) {
            Log.d("MockInterceptor", "<-- Mocking Delete Seat Response")
            val json = """
                {
                    "success": true,
                    "message": "좌석 삭제 완료",
                    "data": null
                }
            """.trimIndent()
            return makeResponse(req, json)
        }

        // 23. 세션 목록 조회 (GET /sessions?studyCafeId=...)
        if (method == "GET" && path.contains("/sessions") && !path.contains("{id}")) {
            Log.d("MockInterceptor", "<-- Mocking Sessions Response")
            val json = """
                {
                    "success": true,
                    "message": null,
                    "data": [
                        {
                            "id": 1,
                            "userId": 1,
                            "studyCafeId": 1,
                            "seatId": 1,
                            "status": "IN_USE",
                            "startTime": "${getISOTimeString(System.currentTimeMillis() - 3600000)}"
                        },
                        {
                            "id": 2,
                            "userId": 2,
                            "studyCafeId": 1,
                            "seatId": 3,
                            "status": "IN_USE",
                            "startTime": "${getISOTimeString(System.currentTimeMillis() - 1800000)}"
                        }
                    ]
                }
            """.trimIndent()
            return makeResponse(req, json)
        }

        // 24. 세션 시작 (PATCH /sessions/{id}/start)
        if (method == "PATCH" && path.contains("/sessions/") && path.contains("/start")) {
            Log.d("MockInterceptor", "<-- Mocking Start Session Response")
            val json = """
                {
                    "success": true,
                    "message": "세션 시작 완료",
                    "data": {
                        "id": 101,
                        "userId": 1,
                        "studyCafeId": 1,
                        "seatId": 1,
                        "status": "IN_USE",
                        "startTime": "${getISOTimeString(System.currentTimeMillis())}"
                    }
                }
            """.trimIndent()
            return makeResponse(req, json)
        }

        // 25. 세션 종료 (DELETE /sessions/{id})
        if (method == "DELETE" && path.matches(Regex(".*sessions/\\d+$"))) {
            Log.d("MockInterceptor", "<-- Mocking End Session Response")
            val json = """
                {
                    "success": true,
                    "message": "세션 종료 완료",
                    "data": null
                }
            """.trimIndent()
            return makeResponse(req, json)
        }

        // 26. 좌석 할당 (POST /sessions/assign?seatId=...)
        if (method == "POST" && path.contains("/sessions/assign")) {
            Log.d("MockInterceptor", "<-- Mocking Assign Seat Response")
            val json = """
                {
                    "success": true,
                    "message": "좌석 할당 완료",
                    "data": {
                        "id": 102,
                        "userId": 1,
                        "studyCafeId": 1,
                        "seatId": 4,
                        "status": "IN_USE",
                        "startTime": "${getISOTimeString(System.currentTimeMillis())}"
                    }
                }
            """.trimIndent()
            return makeResponse(req, json)
        }

        // 27. 자동 좌석 할당 (POST /sessions/auto-assign?studyCafeId=...)
        if (method == "POST" && path.contains("/sessions/auto-assign")) {
            Log.d("MockInterceptor", "<-- Mocking Auto Assign Seat Response")
            val json = """
                {
                    "success": true,
                    "message": "자동 좌석 할당 완료",
                    "data": {
                        "id": 103,
                        "userId": 1,
                        "studyCafeId": 1,
                        "seatId": 2,
                        "status": "IN_USE",
                        "startTime": "${getISOTimeString(System.currentTimeMillis())}"
                    }
                }
            """.trimIndent()
            return makeResponse(req, json)
        }

        // 28. 사용자 시간권 삭제 (DELETE /study-cafes/{id}/users/{userId}/time)
        if (method == "DELETE" && path.contains("/users/") && path.contains("/time")) {
            Log.d("MockInterceptor", "<-- Mocking Delete User Time Pass Response")
            val json = """
                {
                    "success": true,
                    "message": "시간권 삭제 완료",
                    "data": null
                }
            """.trimIndent()
            return makeResponse(req, json)
        }

        // 29. 이미지 업로드 (POST /images/upload)
        if (method == "POST" && path.contains("/images/upload")) {
            Log.d("MockInterceptor", "<-- Mocking Image Upload Response")
            val json = """
                {
                    "success": true,
                    "message": "이미지 업로드 완료",
                    "data": {
                        "imageId": "img-${System.currentTimeMillis()}",
                        "imageUrl": "https://example.com/images/img-${System.currentTimeMillis()}"
                    }
                }
            """.trimIndent()
            return makeResponse(req, json)
        }

        // 30. 이미지 조회 (GET /images/{imageId})
        if (method == "GET" && path.contains("/images/")) {
            Log.d("MockInterceptor", "<-- Mocking Get Image Response")
            val imageId = path.substringAfterLast("/")
            val json = """
                {
                    "success": true,
                    "message": null,
                    "data": {
                        "imageId": "$imageId",
                        "imageUrl": "https://example.com/images/$imageId"
                    }
                }
            """.trimIndent()
            return makeResponse(req, json)
        }

        // 31. 이미지 삭제 (DELETE /images/{imageId})
        if (method == "DELETE" && path.contains("/images/")) {
            Log.d("MockInterceptor", "<-- Mocking Delete Image Response")
            val json = """
                {
                    "success": true,
                    "message": "이미지 삭제 완료",
                    "data": null
                }
            """.trimIndent()
            return makeResponse(req, json)
        }

        // 그 외 요청은 통과
        return chain.proceed(req)
    }

    private fun makeResponse(req: okhttp3.Request, json: String): Response {
        return Response.Builder()
            .request(req)
            .protocol(Protocol.HTTP_1_1)
            .code(200)
            .message("OK")
            .body(json.toResponseBody("application/json; charset=utf-8".toMediaTypeOrNull()))
            .build()
    }

    /**
     * 현재 시간을 ISO 8601 형식 UTC 문자열로 변환
     * 예: "2026-01-03T03:05:32Z"
     */
    private fun getISOTimeString(timeMillis: Long): String {
        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", java.util.Locale.getDefault())
        sdf.timeZone = java.util.TimeZone.getTimeZone("UTC")
        return sdf.format(java.util.Date(timeMillis))
    }
}
