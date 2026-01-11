package kr.jiyeok.seatly.presentation.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kr.jiyeok.seatly.data.remote.request.CreateCafeRequest
import kr.jiyeok.seatly.data.repository.ApiResult
import kr.jiyeok.seatly.data.repository.SeatlyRepository
import okhttp3.MultipartBody
import javax.inject.Inject

/**
 * CreateCafeViewModel - 카페 등록 화면 데이터 관리
 *
 * 기능:
 * - 새로운 스터디 카페 등록
 */
@HiltViewModel
class CreateCafeViewModel @Inject constructor(
    private val repository: SeatlyRepository
) : ViewModel() {

    // =====================================================
    // Loading State
    // =====================================================
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // =====================================================
    // Error State
    // =====================================================
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    // =====================================================
    // Create Cafe
    // =====================================================
    private val _createCafeSuccess = MutableStateFlow(false)
    val createCafeSuccess: StateFlow<Boolean> = _createCafeSuccess

    // =====================================================
    // Upload Image
    // =====================================================
    private val _uploadedImageUrls = MutableStateFlow<List<String>>(emptyList())
    val uploadedImageUrls: StateFlow<List<String>> = _uploadedImageUrls.asStateFlow()

    init {

    }

    // =====================================================
    // Public Methods - Create Cafe
    // =====================================================

    /**
     * 카페 등록 요청
     *
     * 기능:
     * - API를 통해 카페 등록 요청
     * - 성공 시 등록된 카페 리스트 갱신 필요
     * - 실패 시 에러 메시지 표시
     */
    fun createCafe(request: CreateCafeRequest) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null

            try {
                // 카페 등록 API 호출
                val result = repository.createCafe(request)

                when (result) {
                    is ApiResult.Success -> {
                        _createCafeSuccess.value = true
                        _errorMessage.value = null
                    }
                    is ApiResult.Failure -> {
                        _errorMessage.value = result.message
                        _createCafeSuccess.value = false
                    }
                }
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "Cafe creation failed"
                _createCafeSuccess.value = false
            } finally {
                _isLoading.value = false
            }
        }
    }

    // =====================================================
    // Public Methods - Upload Image
    // =====================================================
    /**
     * 이미지 업로드
     *
     * 기능:
     * - API를 통해 이미지 업로드 요청
     * - 성공 시
     * - 실패 시 에러 메시지 표시
     */
    fun uploadImage(file: MultipartBody.Part) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            try {
                val result = repository.uploadImage(file)
                when (result) {
                    is ApiResult.Success -> {
                        val imageUrl = result.data?.imageUrl
                        if (imageUrl != null) {
                            val currentUrls = _uploadedImageUrls.value.toMutableList()
                            currentUrls.add(imageUrl)
                            _uploadedImageUrls.value = currentUrls
                        }
                    }

                    is ApiResult.Failure -> {
                        _errorMessage.value = result.message ?: "이미지 업로드 실패"
                    }
                }
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "이미지 업로드 중 오류 발생"
            } finally {
                _isLoading.value = false
            }
        }
    }


    fun resetCreateSuccess() {

    }

    fun removeUploadedImage (uri: String) {

    }

    // =====================================================
    // Error Handling
    // =====================================================

    /**
     * 에러 메시지 초기화
     */
    fun clearError() {
        _errorMessage.value = null
    }
}