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
        if (method == "POST" && path.contains("/auth/login")) {
            Log.d("MockInterceptor", "<-- Mocking Login Response")
            val json = """
                {
                    "success": true,
                    "message": null,
                    "data": {
                        "accessToken": "fake-user-access-token",
                        "refreshToken": "fake-user-refresh-token",
                        "expiresIn": 3600,
                        "user": {
                            "id": 1,
                            "email": "user@test.com",
                            "name": "일반 사용자",
                            "phone": "010-9876-5432",
                            "imageUrl": null,
                            "role": "USER",
                            "favoriteCafeIds": [1, 3, 4],
                            "sessions": [
                                {
                                    "id": 101,
                                    "userId": 1,
                                    "seat": { 
                                        "id": "A-12",
                                        "name": "창가 1인석",
                                        "totalTime": 14400000 
                                    },
                                    "studyCafe": {
                                        "id": 1,
                                        "name": "명지 스터디카페",
                                        "address": "서울시 강서구 마곡로 100",
                                        "mainImageUrl": "https://images.unsplash.com/photo-1554118811-1e0d58224f24"
                                    },
                                    "startTime": ${System.currentTimeMillis() - 3600000},
                                    "status": "IN_USE"
                                }
                            ]
                        }
                    }
                }
            """.trimIndent()
            return makeResponse(req, json)
        }

        // 2. 내 정보 조회 (GET /users/me)
        if (method == "GET" && (path.contains("/users/me") || path.contains("/auth/me"))) {
            Log.d("MockInterceptor", "<-- Mocking User Info Response")
            val json = """
            {
                "success": true,
                "message": null,
                "data": {
                    "id": 1,
                    "email": "user@test.com",
                    "name": "일반 사용자",
                    "phone": "010-9876-5432",
                    "imageUrl": null,
                    "favoriteCafeIds": [1, 3],
                    "sessions": [],
                    "role": "USER"
                }
            }
            """.trimIndent()
            return makeResponse(req, json)
        }

        // 3. 카페 목록 조회 (GET /study-cafes)
        if (method == "GET" && path.contains("study-cafes") && !path.matches(Regex(".*study-cafes/\\d+$"))) {
            Log.d("MockInterceptor", "<-- Mocking Cafe List (Clean Version)")

            val json = """
            {
                "success": true,
                "message": null,
                "data": {
                    "content": [
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
                    ],
                    "page": 0,
                    "size": 100,
                    "totalElements": 5,
                    "totalPages": 1
                }
            }
            """.trimIndent()
            return makeResponse(req, json)
        }

        // 4. 카페 상세 조회 (GET /study-cafes/{id})
        // 상세 화면에서는 상세 정보가 필요할 수 있으나, 요청하신 바에 따라 최소한의 구조를 유지합니다.
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
                    "mainImageUrl": "https://images.unsplash.com/photo-1554118811-1e0d58224f24",
                    "imageUrls": [
                        "https://images.unsplash.com/photo-1554118811-1e0d58224f24"
                    ],
                    "phoneNumber": "010-1234-5678",
                    "facilities": ["WIFI", "AC"],
                    "openingHours": "24시간",
                    "description": "학습하기 좋은 공간입니다."
                }
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
}
