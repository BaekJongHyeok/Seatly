package kr.jiyeok.seatly.di

import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody

/**
 * DebugMockInterceptor
 * - debug 빌드에서만 포함되는 인터셉터입니다 (src/debug/java/...).
 * - 서버가 없을 때 특정 경로를 가로채서 mock JSON을 반환합니다.
 * - 실제 엔드포인트 경로에 맞게 path.contains(...)를 수정하세요.
 * 
 * Mock user accounts:
 * - user@test.com / password -> USER role
 * - admin@test.com / password -> ADMIN role
 */
class DebugMockInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val req = chain.request()
        val path = req.url.encodedPath ?: ""
        val method = req.method.uppercase()

        // POST /auth/login 을 가로채서 성공 응답 반환 (프로젝트 엔드포인트에 맞게 변경)
        if (method == "POST" && path.contains("/auth/login")) {
            // Read request body to determine which user to return
            val requestBody = req.body?.let {
                val buffer = okio.Buffer()
                it.writeTo(buffer)
                buffer.readUtf8()
            } ?: ""
            
            // Determine role based on email in request
            val isAdmin = requestBody.contains("admin@test.com")
            
            val json = if (isAdmin) {
                """
                {
                  "success": true,
                  "message": null,
                  "data": {
                    "accessToken": "fake-admin-access-token",
                    "refreshToken": "fake-admin-refresh-token",
                    "expiresIn": 3600,
                    "user": {
                      "id": 2,
                      "email": "admin@test.com",
                      "name": "관리자",
                      "phone": "010-1234-5678",
                      "imageUrl": null,
                      "joinedAt": "2025-01-01T00:00:00Z",
                      "roles": ["ADMIN"],
                      "favoriteCafeIds": [2],
                      "favoritesCount": 1
                    }
                  }
                }
                """.trimIndent()
            } else {
                """
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
                      "joinedAt": "2025-01-01T00:00:00Z",
                      "roles": ["USER"],
                      "favoriteCafeIds": [1, 3],
                      "favoritesCount": 2
                    }
                  }
                }
                """.trimIndent()
            }

            return Response.Builder()
                .request(req)
                .protocol(Protocol.HTTP_1_1)
                .code(200)
                .message("OK")
                .body(json.toResponseBody("application/json; charset=utf-8".toMediaTypeOrNull()))
                .build()
        }

        // GET /users/me or /user/me
        if (method == "GET" && (path.contains("/users/me") || path.contains("/user/me") || path.contains("/auth/me"))) {
            // Determine role based on access token
            val authHeader = req.header("Authorization") ?: ""
            val isAdmin = authHeader.contains("fake-admin-access-token")
            
            val json = if (isAdmin) {
                """
                {
                  "success": true,
                  "message": null,
                  "data": {
                    "id": 2,
                    "email": "admin@test.com",
                    "name": "관리자",
                    "phone": "010-1234-5678",
                    "imageUrl": null,
                    "joinedAt": "2025-01-01T00:00:00Z",
                    "roles": ["ADMIN"],
                    "favoriteCafeIds": [2],
                    "favoritesCount": 1
                  }
                }
                """.trimIndent()
            } else {
                """
                {
                  "success": true,
                  "message": null,
                  "data": {
                    "id": 1,
                    "email": "user@test.com",
                    "name": "일반 사용자",
                    "phone": "010-9876-5432",
                    "imageUrl": null,
                    "joinedAt": "2025-01-01T00:00:00Z",
                    "roles": ["USER"],
                    "favoriteCafeIds": [1, 3],
                    "favoritesCount": 2
                  }
                }
                """.trimIndent()
            }

            return Response.Builder()
                .request(req)
                .protocol(Protocol.HTTP_1_1)
                .code(200)
                .message("OK")
                .body(json.toResponseBody("application/json; charset=utf-8".toMediaTypeOrNull()))
                .build()
        }

        // GET /users/me/current-cafe - Mock current cafe usage (empty for now to avoid null pointer)
        if (method == "GET" && path.contains("/users/me/current-cafe")) {
            val json = """
                {
                  "success": true,
                  "message": null,
                  "data": null
                }
            """.trimIndent()

            return Response.Builder()
                .request(req)
                .protocol(Protocol.HTTP_1_1)
                .code(200)
                .message("OK")
                .body(json.toResponseBody("application/json; charset=utf-8".toMediaTypeOrNull()))
                .build()
        }

        // GET /users/me/favorites - Mock favorite cafes
        if (method == "GET" && path.contains("/users/me/favorites")) {
            // Determine role based on access token
            val authHeader = req.header("Authorization") ?: ""
            val isAdmin = authHeader.contains("fake-admin-access-token")
            
            // Return favorites based on user
            // User has favorites: [1, 3]
            // Admin has favorites: [2]
            val json = if (isAdmin) {
                """
                {
                  "success": true,
                  "message": null,
                  "data": {
                    "content": [
                      {
                        "id": 2,
                        "name": "더존 프리미엄 독서실",
                        "mainImageUrl": "https://images.unsplash.com/photo-1521017432531-fbd92d768814",
                        "address": "서울시 강남구 테헤란로 123",
                        "rating": 4.5,
                        "isFavorite": true,
                        "isOpen": true,
                        "distanceMeters": 500
                      }
                    ],
                    "page": 0,
                    "size": 10,
                    "totalElements": 1,
                    "totalPages": 1
                  }
                }
                """.trimIndent()
            } else {
                """
                {
                  "success": true,
                  "message": null,
                  "data": {
                    "content": [
                      {
                        "id": 1,
                        "name": "명지 스터디카페",
                        "mainImageUrl": "https://images.unsplash.com/photo-1554118811-1e0d58224f24",
                        "address": "서울시 강서구 마곡로 100",
                        "rating": 4.8,
                        "isFavorite": true,
                        "isOpen": true,
                        "distanceMeters": 1200
                      },
                      {
                        "id": 3,
                        "name": "어반 라이브러리",
                        "mainImageUrl": "https://images.unsplash.com/photo-1497366754035-f200968a6e72",
                        "address": "서울시 마포구 양화로 45",
                        "rating": null,
                        "isFavorite": true,
                        "isOpen": false,
                        "distanceMeters": 3200
                      }
                    ],
                    "page": 0,
                    "size": 10,
                    "totalElements": 2,
                    "totalPages": 1
                  }
                }
                """.trimIndent()
            }

            return Response.Builder()
                .request(req)
                .protocol(Protocol.HTTP_1_1)
                .code(200)
                .message("OK")
                .body(json.toResponseBody("application/json; charset=utf-8".toMediaTypeOrNull()))
                .build()
        }

        // GET /users/me/recent-cafes - Mock recent cafes
        if (method == "GET" && path.contains("/users/me/recent-cafes")) {
            val json = """
                {
                  "success": true,
                  "message": null,
                  "data": []
                }
            """.trimIndent()

            return Response.Builder()
                .request(req)
                .protocol(Protocol.HTTP_1_1)
                .code(200)
                .message("OK")
                .body(json.toResponseBody("application/json; charset=utf-8".toMediaTypeOrNull()))
                .build()
        }

        // GET /study-cafes - Mock study cafes list
        if (method == "GET" && path.contains("/study-cafes") && !path.contains("/study-cafes/")) {
            val json = """
                {
                  "success": true,
                  "message": null,
                  "data": {
                    "content": [
                      {
                        "id": 1,
                        "name": "명지 스터디카페",
                        "mainImageUrl": "https://images.unsplash.com/photo-1554118811-1e0d58224f24",
                        "address": "서울시 강서구 마곡로 100",
                        "rating": 4.8,
                        "isFavorite": false,
                        "isOpen": true,
                        "distanceMeters": 1200
                      },
                      {
                        "id": 2,
                        "name": "더존 프리미엄 독서실",
                        "mainImageUrl": "https://images.unsplash.com/photo-1521017432531-fbd92d768814",
                        "address": "서울시 강남구 테헤란로 123",
                        "rating": 4.5,
                        "isFavorite": false,
                        "isOpen": true,
                        "distanceMeters": 500
                      },
                      {
                        "id": 3,
                        "name": "어반 라이브러리",
                        "mainImageUrl": "https://images.unsplash.com/photo-1497366754035-f200968a6e72",
                        "address": "서울시 마포구 양화로 45",
                        "rating": null,
                        "isFavorite": false,
                        "isOpen": false,
                        "distanceMeters": 3200
                      },
                      {
                        "id": 4,
                        "name": "포커스 온",
                        "mainImageUrl": "https://images.unsplash.com/photo-1517502884422-41eaead166d4",
                        "address": "경기도 성남시 분당구 판교로 67",
                        "rating": 4.9,
                        "isFavorite": true,
                        "isOpen": true,
                        "distanceMeters": 8500
                      },
                      {
                        "id": 5,
                        "name": "스타벅스 스터디룸",
                        "mainImageUrl": "https://images.unsplash.com/photo-1481277542470-605612bd2d61",
                        "address": "서울시 서초구 서초대로 89",
                        "rating": 4.6,
                        "isFavorite": false,
                        "isOpen": true,
                        "distanceMeters": 2100
                      }
                    ],
                    "page": 0,
                    "size": 100,
                    "totalElements": 5,
                    "totalPages": 1
                  }
                }
            """.trimIndent()

            return Response.Builder()
                .request(req)
                .protocol(Protocol.HTTP_1_1)
                .code(200)
                .message("OK")
                .body(json.toResponseBody("application/json; charset=utf-8".toMediaTypeOrNull()))
                .build()
        }

        // GET /sessions - Mock active sessions for logged-in user
        if (method == "GET" && path.contains("/sessions")) {
            // Check if user has an active session based on access token
            val authHeader = req.header("Authorization") ?: ""
            val isAdmin = authHeader.contains("fake-admin-access-token")
            
            // Return active session for demo purposes (can be toggled based on user)
            val json = """
                {
                  "success": true,
                  "message": null,
                  "data": [
                    {
                      "id": 1,
                      "userId": ${if (isAdmin) 2 else 1},
                      "studyCafeId": 1,
                      "seatId": 5,
                      "seatName": "A-5",
                      "startedAt": "2025-12-31T14:30:00Z",
                      "endedAt": null,
                      "status": "IN_USE"
                    }
                  ]
                }
            """.trimIndent()

            return Response.Builder()
                .request(req)
                .protocol(Protocol.HTTP_1_1)
                .code(200)
                .message("OK")
                .body(json.toResponseBody("application/json; charset=utf-8".toMediaTypeOrNull()))
                .build()
        }

        // POST /auth/refresh (토큰 리프레시 시뮬레이션)
        if (method == "POST" && path.contains("/auth/refresh")) {
            val json = """
                {
                  "accessToken": "fake-refreshed-access",
                  "refreshToken": "fake-refreshed-refresh",
                  "expiresIn": 3600
                }
            """.trimIndent()

            return Response.Builder()
                .request(req)
                .protocol(Protocol.HTTP_1_1)
                .code(200)
                .message("OK")
                .body(json.toResponseBody("application/json; charset=utf-8".toMediaTypeOrNull()))
                .build()
        }

        // 그 외는 실제 네트워크 호출
        return chain.proceed(req)
    }
}