package kr.jiyeok.seatly.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kr.jiyeok.seatly.data.remote.request.ChangePasswordRequest
import kr.jiyeok.seatly.data.remote.request.UpdateUserInfoRequest
import kr.jiyeok.seatly.data.remote.response.UserInfoSummaryDto
import kr.jiyeok.seatly.data.repository.ApiResult
import kr.jiyeok.seatly.domain.usecase.*

class EditProfileViewModel(
    private val getUserInfoUseCase: GetUserInfoUseCase,
    private val updateUserInfoUseCase: UpdateUserInfoUseCase,
    private val uploadImageUseCase: UploadImageUseCase,
    private val changePasswordUseCase: ChangePasswordUseCase,
    private val deleteAccountUseCase: DeleteAccountUseCase,
    private val ioDispatcher: CoroutineDispatcher
) : ViewModel() {

    // 사용자 정보
    private val _userData = MutableStateFlow<UserInfoSummaryDto?>(null)
    val userData: StateFlow<UserInfoSummaryDto?> = _userData.asStateFlow()

    // 업데이트 성공
    private val _updateSuccess = MutableStateFlow(false)
    val updateSuccess: StateFlow<Boolean> = _updateSuccess.asStateFlow()

    // 비밀번호 변경
    private val _changePasswordSuccess = MutableStateFlow(false)
    val changePasswordSuccess: StateFlow<Boolean> = _changePasswordSuccess

    // 계정 삭제
    private val _deleteAccountSuccess = MutableStateFlow(false)
    val deleteAccountSuccess: StateFlow<Boolean> = _deleteAccountSuccess.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _events = Channel<String>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    /**
     * 사용자 프로필 조회
     */
    fun loadUserProfile() {
        viewModelScope.launch(ioDispatcher) {
            _isLoading.value = true
            try {
                when (val result = getUserInfoUseCase()) {
                    is ApiResult.Success -> {
                        _userData.value = result.data
                    }
                    is ApiResult.Failure -> {
                        _userData.value = null
                        _events.send(result.message ?: "사용자 정보 조회 실패")
                    }
                    is ApiResult.Loading -> {}
                }
            } catch (e: Exception) {
                _events.send(e.message ?: "알 수 없는 오류")
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * 사용자 프로필 업데이트
     */
    fun updateUserProfile(
        name: String, 
        phoneNumber: String, 
        fileName: String? = null, 
        content: ByteArray? = null
    ) {
        viewModelScope.launch(ioDispatcher) {
            _isLoading.value = true
            try {
                var finalImageId: String? = userData.value?.imageUrl

                // 1. 이미지가 변경되었으면 업로드
                if (fileName != null && content != null) {
                    when (val result = uploadImageUseCase(fileName, content)) {
                        is ApiResult.Success -> {
                            finalImageId = result.data
                        }
                        is ApiResult.Failure -> {
                            _events.send(result.message ?: "이미지 업로드 실패")
                            _isLoading.value = false
                            return@launch
                        }
                        is ApiResult.Loading -> {}
                    }
                }

                // 2. 사용자 정보 업데이트
                val updateRequest = UpdateUserInfoRequest(name, phoneNumber, finalImageId)
                when (val result = updateUserInfoUseCase(updateRequest)) {
                    is ApiResult.Success -> {
                        _updateSuccess.value = true
                        _events.send("프로필이 업데이트되었습니다")
                        loadUserProfile()
                    }
                    is ApiResult.Failure -> {
                        _updateSuccess.value = false
                        _events.send(result.message ?: "사용자 정보 업데이트 실패")
                    }
                    is ApiResult.Loading -> {}
                }
            } catch (e: Exception) {
                _events.send(e.message ?: "알 수 없는 오류")
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * 비밀번호 변경
     */
    fun changePassword(currentPassword: String, newPassword: String) {
        viewModelScope.launch(ioDispatcher) {
            _isLoading.value = true
            try {
                val changeRequest = ChangePasswordRequest(currentPassword, newPassword)
                when (val result = changePasswordUseCase(changeRequest)) {
                    is ApiResult.Success -> {
                        _changePasswordSuccess.value = true
                        _events.send("비밀번호가 변경되었습니다")
                    }
                    is ApiResult.Failure -> {
                        _changePasswordSuccess.value = false
                        _events.send(result.message ?: "비밀번호 변경 실패")
                    }
                    is ApiResult.Loading -> {}
                }
            } catch (e: Exception) {
                _events.send(e.message ?: "알 수 없는 오류")
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * 계정 탈퇴 요청
     */
    fun deleteAccount() {
        viewModelScope.launch(ioDispatcher) {
            _isLoading.value = true
            try {
                when (val result = deleteAccountUseCase()) {
                    is ApiResult.Success -> {
                        _deleteAccountSuccess.value = true
                        _events.send("계정이 탈퇴되었습니다")
                    }
                    is ApiResult.Failure -> {
                        _deleteAccountSuccess.value = false
                        _events.send(result.message ?: "계정 탈퇴 실패")
                    }
                    is ApiResult.Loading -> {}
                }
            } catch (e: Exception) {
                _events.send(e.message ?: "알 수 없는 오류")
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun resetUpdateSuccess() {
        _updateSuccess.value = false
    }
}
