package kr.jiyeok.seatly.data

import android.util.Log
import kotlinx.coroutines.delay
import kotlin.random.Random

object FakeAuthRepository {
    // 등록된 이메일 시뮬레이션
    private val registeredEmails = mutableSetOf(
        "jonghyeok8956@gmail.com",
        "user1@example.com"
    )

    // email -> code
    private val codeStore = mutableMapOf<String, String>()

    // 단순 시뮬레이션: 이메일 등록 여부 확인
    suspend fun isEmailRegistered(email: String): Boolean {
        // simulate network delay
        delay(600)
        return registeredEmails.contains(email)
    }

    // 보안 코드 생성 및 "전송" (저장)
    suspend fun sendSecurityCode(email: String): Boolean {
        delay(800)
        if (!registeredEmails.contains(email)) return false
        val code = (100000..999999).random().toString()
        codeStore[email] = code
        Log.d("TAG", "sendSecurityCode: $code")
        // In real app you'd call email service here.
        // For debugging you can log or print the code.
        // println("DEBUG: sent code for $email: $code")
        return true
    }

    // 재전송 (동일 구현)
    suspend fun resendSecurityCode(email: String): Boolean {
        return sendSecurityCode(email)
    }

    // 코드 검증
    suspend fun verifySecurityCode(email: String, code: String): Boolean {
        delay(600)
        val stored = codeStore[email]
        return stored != null && stored == code
    }

    // 비밀번호 변경(모의)
    suspend fun resetPassword(email: String, newPassword: String): Boolean {
        delay(800)
        if (!registeredEmails.contains(email)) return false
        // In real app you'd persist hash; here we just succeed
        // Optionally clear stored code
        codeStore.remove(email)
        return true
    }
}