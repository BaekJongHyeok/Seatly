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
 */
class DebugMockInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val req = chain.request()
        val path = req.url.encodedPath ?: ""
        val method = req.method.uppercase()

        // POST /auth/login 을 가로채서 성공 응답 반환 (프로젝트 엔드포인트에 맞게 변경)
        if (method == "POST" && path.contains("/auth/login")) {
            val json = """
                {
                  "success": true,
                  "message": null,
                  "data": {
                    "accessToken": "fake-access-token",
                    "refreshToken": "fake-refresh-token",
                    "expiresIn": 3600,
                    "user": {
                      "id": 1,
                      "email": "test@seatly.com",
                      "name": "테스트 사용자",
                      "phone": null,
                      "imageUrl": null,
                      "joinedAt": "2025-01-01T00:00:00Z",
                      "roles": ["USER"],
                      "favoritesCount": 0
                    }
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

        // GET /users/me or /user/me
        if (method == "GET" && (path.contains("/users/me") || path.contains("/user/me") || path.contains("/auth/me"))) {
            val json = """
                {
                  "success": true,
                  "message": null,
                  "data": {
                    "id": 1,
                    "email": "test@seatly.com",
                    "name": "테스트 사용자",
                    "phone": null,
                    "imageUrl": null,
                    "joinedAt": "2025-01-01T00:00:00Z",
                    "roles": ["USER"],
                    "favoritesCount": 0
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