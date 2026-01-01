package kr.jiyeok.seatly.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kr.jiyeok.seatly.data.remote.request.ChangePasswordRequest
import kr.jiyeok.seatly.data.remote.request.UpdateUserRequest
import kr.jiyeok.seatly.data.remote.response.UserResponseDto
import kr.jiyeok.seatly.data.repository.ApiResult
import kr.jiyeok.seatly.domain.usecase.*
import kr.jiyeok.seatly.di.IoDispatcher
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject

/**
 * User Profile ViewModel
 * 
 * 역할:
 * - 사용자 프로필 정보 관리
 * - 사용자 정보 수정
 * - 비밀번호 변경
 * - 계정 삭제
 * 
 * UI는 StateFlow를 통해 상태를 관찰하고,
 * 에러/이벤트는 [events] Channel을 통해 수신합니다
 */

/**
 * 사용자 UI 상태
 */
sealed interface UserUiState {
    object Idle : UserUiState
    object Loading : UserUiState
    data class Success(val message: String = "") : UserUiState
    data class Error(val message: String) : UserUiState
}

@HiltViewModel
class UserViewModel @Inject constructor(
    private val getUserInfoUseCase: GetUserInfoUseCase,
    private val updateUserInfoUseCase: UpdateUserInfoUseCase,
    private val changePasswordUseCase: ChangePasswordUseCase,
    private val deleteAccountUseCase: DeleteAccountUseCase,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : ViewModel() {

    // =====================================================
    // State Management
    // =====================================================

    /**
     * 사용자 UI 상태
     * Idle → Loading → Success/Error
     */
    private val _userState = MutableStateFlow<UserUiState>(UserUiState.Idle)
    val userState: StateFlow<UserUiState> = _userState.asStateFlow()

    /**
     * 사용자 프로필 정보
     */
    private val _userProfile = MutableStateFlow<UserResponseDto?>(null)
    val userProfile: StateFlow<UserResponseDto?> = _userProfile.asStateFlow()

    /**
     * 로딩 상태
     */
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    /**
     * 에러 메시지
     */
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    /**
     * 에러/이벤트 메시지 Channel
     * UI에서 토스트 메시지나 스낵바로 표시
     */
    private val _events = Channel<String>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    // =====================================================
    // Public Methods
    // =====================================================

    /**
     * 사용자 정보 로드
     */
    fun loadUserProfile() {
        viewModelScope.launch(ioDispatcher) {
            _isLoading.value = true
            _userState.value = UserUiState.Loading
            _error.value = null
            try {
                when (val result = getUserInfoUseCase()) {
                    is ApiResult.Success -> {
                        _userProfile.value = result.data
                        _userState.value = UserUiState.Success("프로필 로드 완료")
                    }
                    is ApiResult.Failure -> {
                        _error.value = result.message ?: "사용자 정보 조회 실패"
                        _userState.value = UserUiState.Error(result.message ?: "사용자 정보 조회 실패")
                        _events.send(result.message ?: "사용자 정보 조회 실패")
                    }
                }
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * 사용자 정보 수정
     */
    fun updateUserProfile(name: String?, phone: String?, imageUrl: String?) {
        viewModelScope.launch(ioDispatcher) {
            _isLoading.value = true
            _userState.value = UserUiState.Loading
            _error.value = null
            try {
                val request = UpdateUserRequest(name, phone, imageUrl)
                when (val result = updateUserInfoUseCase(request)) {
                    is ApiResult.Success -> {
                        _userProfile.value = result.data
                        _userState.value = UserUiState.Success("프로필이 업데이트되었습니다")
                        _events.send("프로필이 업데이트되었습니다")
                    }
                    is ApiResult.Failure -> {
                        _error.value = result.message ?: "프로필 업데이트 실패"
                        _userState.value = UserUiState.Error(result.message ?: "프로필 업데이트 실패")
                        _events.send(result.message ?: "프로필 업데이트 실패")
                    }
                }
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * 비밀번호 변경
     */
    fun changePassword(userId: Long, currentPassword: String, newPassword: String) {
        viewModelScope.launch(ioDispatcher) {
            _isLoading.value = true
            _userState.value = UserUiState.Loading
            _error.value = null
            try {
                val request = ChangePasswordRequest(currentPassword, newPassword)
                when (val result = changePasswordUseCase(userId, request)) {
                    is ApiResult.Success -> {
                        _userState.value = UserUiState.Success("비밀번호가 변경되었습니다")
                        _events.send("비밀번호가 변경되었습니다")
                    }
                    is ApiResult.Failure -> {
                        _error.value = result.message ?: "비밀번호 변경 실패"
                        _userState.value = UserUiState.Error(result.message ?: "비밀번호 변경 실패")
                        _events.send(result.message ?: "비밀번호 변경 실패")
                    }
                }
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * 계정 삭제
     */
    fun deleteAccount() {
        viewModelScope.launch(ioDispatcher) {
            _isLoading.value = true
            _userState.value = UserUiState.Loading
            _error.value = null
            try {
                when (val result = deleteAccountUseCase()) {
                    is ApiResult.Success -> {
                        _userProfile.value = null
                        _userState.value = UserUiState.Success("회원탈퇴 완료")
                        _events.send("회원탈퇴 완료되었습니다")
                    }
                    is ApiResult.Failure -> {
                        _error.value = result.message ?: "회원탈퇴 실패"
                        _userState.value = UserUiState.Error(result.message ?: "회원탈퇴 실패")
                        _events.send(result.message ?: "회원탈퇴 실패")
                    }
                }
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * 에러 초기화
     */
    fun clearError() {
        _error.value = null
    }

    /**
     * 상태 초기화
     */
    fun resetState() {
        _userState.value = UserUiState.Idle
        _error.value = null
    }
}
