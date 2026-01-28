package kr.jiyeok.seatly.data.repository

sealed class ApiResult<out T> {
    data class Success<out T>(val data: T) : ApiResult<T>()
    data class Failure(val message: String, val exception: Throwable? = null) : ApiResult<Nothing>()
    data object Loading : ApiResult<Nothing>()
}
