package kr.jiyeok.seatly.util

import kotlin.math.min

/**
 * 숫자로만 된 문자열을 한국 휴대폰 번호 형식(010-XXXX-XXXX)으로 변환합니다.
 */
fun formatKoreanPhoneFromDigits(digits: String): String {
    return if (digits.startsWith("02")) {
        when (digits.length) {
            in 0..2 -> digits
            in 3..5 -> "${digits.substring(0, 2)}-${digits.substring(2)}"
            in 6..9 -> "${digits.substring(0, 2)}-${digits.substring(2, digits.length - 4)}-${digits.takeLast(4)}"
            else -> "${digits.substring(0, 2)}-${digits.substring(2, 6)}-${digits.substring(6, min(10, digits.length))}"
        }
    } else {
        when (digits.length) {
            in 0..3 -> digits
            in 4..6 -> "${digits.substring(0, 3)}-${digits.substring(3)}"
            in 7..10 -> "${digits.substring(0, 3)}-${digits.substring(3, 6)}-${digits.substring(6)}"
            else -> "${digits.substring(0, 3)}-${digits.substring(3, 7)}-${digits.substring(7, 11)}"
        }
    }
}
