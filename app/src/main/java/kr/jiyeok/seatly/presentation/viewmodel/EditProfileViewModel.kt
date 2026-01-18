package kr.jiyeok.seatly.presentation.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kr.jiyeok.seatly.data.remote.request.ChangePasswordRequest
import kr.jiyeok.seatly.data.remote.request.UpdateUserInfoRequest
import kr.jiyeok.seatly.data.remote.response.UserInfoDetailDto
import kr.jiyeok.seatly.data.remote.response.UserInfoSummaryDto
import kr.jiyeok.seatly.data.repository.ApiResult
import kr.jiyeok.seatly.data.repository.SeatlyRepository
import javax.inject.Inject

/**
 * EditProfileViewModel - 개인정보 수정 화면 데이터 관리
 *
 * 기능:
 * - User 정보 로드
 * - 프로필 사진 변경
 * - 이름, 전화번호 수정
 * - 계정 탈퇴
 * - 로딩, 에러 상태 관리
 */
@HiltViewModel
class EditProfileViewModel @Inject constructor(
    private val repository: SeatlyRepository
) : ViewModel() {

    // =====================================================
    // User Data
    // =====================================================
    private val _userData = MutableStateFlow<UserInfoSummaryDto?>(null)
    val userData: StateFlow<UserInfoSummaryDto?> = _userData.asStateFlow()

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
    // Change Password State
    // =====================================================
    private val _changePasswordSuccess = MutableStateFlow(false)
    val changePasswordSuccess: StateFlow<Boolean> = _changePasswordSuccess

    // =====================================================
    // Delete Account State
    // =====================================================
    private val _deleteAccountSuccess = MutableStateFlow(false)
    val deleteAccountSuccess: StateFlow<Boolean> = _deleteAccountSuccess.asStateFlow()

    // =====================================================
    // Initialization
    // =====================================================

    init {
        loadUserProfile()
    }

    // =====================================================
    // Public Methods - Data Loading
    // =====================================================

    /**
     * 사용자 프로필 정보 로드
     */
    fun loadUserProfile() {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null

            try {
                val result = repository.getUserInfo()
                when (result) {
                    is ApiResult.Success -> {
                        _userData.value = result.data
                    }
                    is ApiResult.Failure -> {
                        _errorMessage.value = result.message
                    }
                }
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "Unknown error"
            } finally {
                _isLoading.value = false
            }
        }
    }

    // =====================================================
    // Public Methods - Profile Update
    // =====================================================

    /**
     * 사용자 프로필 업데이트
     * - 이름
     * - 전화번호
     * - 프로필 사진
     */
    fun updateUserProfile(
        name: String,
        phoneNumber: String,
        imageUri: Uri? = null
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null

            try {
                // 이미지 업로드 (필요시)
                val imagePath = if (imageUri != null) {
                    imageUri.toString()  // Uri를 String 경로로 변환
                } else {
                    userData.value?.imageUrl  // 기존 이미지 URL 유지
                }

                // 사용자 정보 업데이트
                val result = repository.updateUserInfo(UpdateUserInfoRequest(name, phoneNumber, imagePath))

                when (result) {
                    is ApiResult.Success -> {
                        loadUserProfile()
                        _errorMessage.value = null
                    }
                    is ApiResult.Failure -> {
                        _errorMessage.value = result.message
                    }
                }
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "Unknown error"
            } finally {
                _isLoading.value = false
            }
        }
    }

    // =====================================================
    // Public Methods - Change Password
    // =====================================================
    /**
     * 비밀번호 변경 요청
     *
     * 기능:
     * - API를 통해 비밀번호 변경 요청
     * - 성공 시 로그아웃 처리 필요
     * - 실패 시 에러 메시지 표시
     */
    fun changePassword(currentPassword: String, newPassword: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null

            try {
                // 비밀번호 변경 API 호출
                val result = repository.changePassword(ChangePasswordRequest(currentPassword, newPassword))

                when (result) {
                    is ApiResult.Success -> {
                        _changePasswordSuccess.value = true
                        _errorMessage.value = null
                    }
                    is ApiResult.Failure -> {
                        _errorMessage.value = result.message
                        _deleteAccountSuccess.value = false
                    }
                }
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "Change Password failed"
                _deleteAccountSuccess.value = false
            } finally {
                _isLoading.value = false
            }
        }
    }

    // =====================================================
    // Public Methods - Delete Account
    // =====================================================

    /**
     * 계정 탈퇴 요청
     *
     * 기능:
     * - API를 통해 계정 삭제 요청
     * - 성공 시 로그아웃 처리 필요
     * - 실패 시 에러 메시지 표시
     */
    fun deleteAccount() {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null

            try {
                // 계정 탈퇴 API 호출
                val result = repository.deleteAccount()

                when (result) {
                    is ApiResult.Success -> {
                        _deleteAccountSuccess.value = true
                        _errorMessage.value = null
                    }
                    is ApiResult.Failure -> {
                        _errorMessage.value = result.message
                        _deleteAccountSuccess.value = false
                    }
                }
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "Account deletion failed"
                _deleteAccountSuccess.value = false
            } finally {
                _isLoading.value = false
            }
        }
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