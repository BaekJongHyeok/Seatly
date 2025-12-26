package kr.jiyeok.seatly.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kr.jiyeok.seatly.data.remote.request.ChangePasswordRequest
import kr.jiyeok.seatly.data.remote.request.UpdateUserRequest
import kr.jiyeok.seatly.data.remote.response.CurrentCafeUsageDto
import kr.jiyeok.seatly.data.remote.response.UserResponseDto
import kr.jiyeok.seatly.data.repository.ApiResult
import kr.jiyeok.seatly.domain.usecase.user.*
import javax.inject.Inject

sealed interface UserUiState {
    object Idle : UserUiState
    object Loading : UserUiState
    data class Success<T>(val data: T) : UserUiState
    data class Error(val message: String) : UserUiState
}

@HiltViewModel
class UserViewModel @Inject constructor(
    private val getCurrentUserUseCase: GetCurrentUserUseCase,
    private val getUserInfoUseCase: GetUserInfoUseCase,
    private val updateUserUseCase: UpdateUserUseCase,
    private val changePasswordUseCase: ChangePasswordUseCase,
    private val deleteAccountUseCase: DeleteAccountUseCase,
    private val getCurrentCafeUsageUseCase: GetCurrentCafeUsageUseCase
) : ViewModel() {

    private val _userState = MutableStateFlow<UserUiState>(UserUiState.Idle)
    val userState: StateFlow<UserUiState> = _userState.asStateFlow()

    private val _profile = MutableStateFlow<UserResponseDto?>(null)
    val profile: StateFlow<UserResponseDto?> = _profile.asStateFlow()

    private val _currentCafeUsage = MutableStateFlow<CurrentCafeUsageDto?>(null)
    val currentCafeUsage: StateFlow<CurrentCafeUsageDto?> = _currentCafeUsage.asStateFlow()

    private val _events = Channel<String>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    fun loadProfile() {
        viewModelScope.launch {
            _userState.value = UserUiState.Loading
            when (val res = getCurrentUserUseCase()) {
                is ApiResult.Success -> {
                    res.data?.let { _profile.value = it }
                    _userState.value = res.data?.let { UserUiState.Success(it) } ?: UserUiState.Error("프로필이 없습니다.")
                }
                is ApiResult.Failure -> {
                    _userState.value = UserUiState.Error(res.message ?: "사용자 조회 실패")
                    _events.send(res.message ?: "사용자 조회 실패")
                }
            }
        }
    }

    fun loadUserInfo() {
        viewModelScope.launch {
            _userState.value = UserUiState.Loading
            when (val res = getUserInfoUseCase()) {
                is ApiResult.Success -> {
                    res.data?.let { _profile.value = it }
                    _userState.value = res.data?.let { UserUiState.Success(it) } ?: UserUiState.Error("정보 없음")
                }
                is ApiResult.Failure -> {
                    _userState.value = UserUiState.Error(res.message ?: "정보 조회 실패")
                    _events.send(res.message ?: "정보 조회 실패")
                }
            }
        }
    }

    fun updateProfile(request: UpdateUserRequest) {
        viewModelScope.launch {
            _userState.value = UserUiState.Loading
            when (val res = updateUserUseCase(request)) {
                is ApiResult.Success -> {
                    res.data?.let { _profile.value = it }
                    _userState.value = UserUiState.Success(res.data ?: Unit)
                    _events.send("프로필이 업데이트되었습니다.")
                }
                is ApiResult.Failure -> {
                    _userState.value = UserUiState.Error(res.message ?: "프로필 업데이트 실패")
                    _events.send(res.message ?: "프로필 업데이트 실패")
                }
            }
        }
    }

    fun changePassword(request: ChangePasswordRequest) {
        viewModelScope.launch {
            _userState.value = UserUiState.Loading
            when (val res = changePasswordUseCase(request)) {
                is ApiResult.Success -> {
                    _userState.value = UserUiState.Success(Unit)
                    _events.send("비밀번호가 변경되었습니다.")
                }
                is ApiResult.Failure -> {
                    _userState.value = UserUiState.Error(res.message ?: "비밀번호 변경 실패")
                    _events.send(res.message ?: "비밀번호 변경 실패")
                }
            }
        }
    }

    fun deleteAccount() {
        viewModelScope.launch {
            _userState.value = UserUiState.Loading
            when (val res = deleteAccountUseCase()) {
                is ApiResult.Success -> {
                    _userState.value = UserUiState.Success(Unit)
                    _events.send("회원탈퇴 완료")
                }
                is ApiResult.Failure -> {
                    _userState.value = UserUiState.Error(res.message ?: "회원탈퇴 실패")
                    _events.send(res.message ?: "회원탈퇴 실패")
                }
            }
        }
    }

    fun loadCurrentCafeUsage() {
        viewModelScope.launch {
            _userState.value = UserUiState.Loading
            when (val res = getCurrentCafeUsageUseCase()) {
                is ApiResult.Success -> {
                    _currentCafeUsage.value = res.data
                    _userState.value = res.data?.let { UserUiState.Success(it) } ?: UserUiState.Error("사용중인 카페가 없습니다.")
                }
                is ApiResult.Failure -> {
                    _userState.value = UserUiState.Error(res.message ?: "현재 이용 정보 조회 실패")
                    _events.send(res.message ?: "현재 이용 정보 조회 실패")
                }
            }
        }
    }
}