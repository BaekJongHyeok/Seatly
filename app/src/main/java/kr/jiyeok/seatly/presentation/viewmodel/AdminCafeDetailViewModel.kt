package kr.jiyeok.seatly.presentation.viewmodel

import androidx.compose.ui.util.fastCbrt
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.android.gms.common.api.Api
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kr.jiyeok.seatly.data.remote.enums.ESeatStatus
import kr.jiyeok.seatly.data.remote.request.SeatUpdate
import kr.jiyeok.seatly.data.remote.response.SeatDto
import kr.jiyeok.seatly.data.remote.response.StudyCafeDetailDto
import kr.jiyeok.seatly.data.remote.response.StudyCafeSummaryDto
import kr.jiyeok.seatly.data.remote.response.UsageDto
import kr.jiyeok.seatly.data.remote.response.UserInfoSummaryDto
import kr.jiyeok.seatly.data.remote.response.UserTimePassInfo
import kr.jiyeok.seatly.data.repository.ApiResult
import kr.jiyeok.seatly.di.IoDispatcher
import kr.jiyeok.seatly.domain.usecase.AddUserTimePassUseCase
import kr.jiyeok.seatly.domain.usecase.GetCafeDetailUseCase
import kr.jiyeok.seatly.domain.usecase.GetCafeSeatsUseCase
import kr.jiyeok.seatly.domain.usecase.GetCafeUsageUseCase
import kr.jiyeok.seatly.domain.usecase.GetImageUseCase
import kr.jiyeok.seatly.domain.usecase.GetUserInfoAdminUseCase
import kr.jiyeok.seatly.domain.usecase.GetUsersWithTimePassUseCase
import kr.jiyeok.seatly.domain.usecase.UpdateSeatsUseCase
import kr.jiyeok.seatly.ui.screen.admin.seat.Seat
import javax.inject.Inject

@HiltViewModel
class AdminCafeDetailViewModel @Inject constructor(
    private val getCafeDetailUseCase: GetCafeDetailUseCase,
    private val getCafeUsageUseCase: GetCafeUsageUseCase,
    private val getCafeMembersUseCase: GetUsersWithTimePassUseCase,
    private val addUserTimePassUseCase: AddUserTimePassUseCase,
    private val getCafeSeatsUseCase: GetCafeSeatsUseCase,
    private val updateSeatsUseCase: UpdateSeatsUseCase,
    private val getImageUseCase: GetImageUseCase,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : ViewModel() {

    private val _cafeInfo = MutableStateFlow<StudyCafeDetailDto?>(null)
    val cafeInfo: StateFlow<StudyCafeDetailDto?> = _cafeInfo.asStateFlow()

    private val _cafeUsage = MutableStateFlow<UsageDto?>(null)
    val cafeUsage: StateFlow<UsageDto?> = _cafeUsage.asStateFlow()

    private val _cafeMembers = MutableStateFlow<List<UserTimePassInfo>?>(null)
    val cafeMembers: StateFlow<List<UserTimePassInfo>?> = _cafeMembers.asStateFlow()

    private val _seatInfo = MutableStateFlow<List<SeatDto>?>(emptyList())
    val seatInfo: StateFlow<List<SeatDto>?> = _seatInfo.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _events = Channel<String>(Channel.Factory.BUFFERED)
    val events = _events.receiveAsFlow()

    private val _imageDataCache = MutableStateFlow<Map<String, ByteArray>>(emptyMap())
    val imageDataCache: StateFlow<Map<String, ByteArray>> = _imageDataCache.asStateFlow()


    fun loadCafeDetailInfos(cafeId: Long) {
        loadCafeInfo(cafeId)
        loadCafeUsage(cafeId)
        loadCafeMembers(cafeId)
        loadSeatInfo(cafeId)
    }


    // Public Methods
    fun loadCafeInfo(cafeId: Long) {
        viewModelScope.launch(ioDispatcher) {
            _isLoading.value = true
            try {
                when (val result = getCafeDetailUseCase(cafeId)) {
                    is ApiResult.Success -> {
                        _cafeInfo.value = result.data
                    }
                    is ApiResult.Failure -> {
                        _cafeInfo.value = null
                        _events.send(result.message ?: "카페 정보 조회 실패")
                    }
                }
            } catch (e: Exception) {
                _events.send(e.message ?: "알 수 없는 오류")
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun loadCafeUsage(cafeId: Long) {
        viewModelScope.launch(ioDispatcher) {
            _isLoading.value = true
            try {
                when (val result = getCafeUsageUseCase(cafeId)) {
                    is ApiResult.Success -> {
                        _cafeUsage.value = result.data
                    }
                    is ApiResult.Failure -> {
                        _cafeUsage.value = null
                        _events.send(result.message ?: "카페 실시간 사용 현황 조회 실패")
                    }
                }
            } catch (e: Exception) {
                _events.send(e.message ?: "알 수 없는 오류")
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun loadCafeMembers(cafeId: Long) {
        viewModelScope.launch(ioDispatcher) {
            _isLoading.value = true
            try {
                when (val result = getCafeMembersUseCase(cafeId)) {
                    is ApiResult.Success -> {
                        _cafeMembers.value = result.data
                    }
                    is ApiResult.Failure -> {
                        _cafeMembers.value = null
                        _events.send(result.message ?: "카페 멤버 조회 실패")
                    }
                }
            } catch (e: Exception) {
                _events.send(e.message ?: "알 수 없는 오류")
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun loadSeatInfo(cafeId: Long) {
        viewModelScope.launch(ioDispatcher) {
            _isLoading.value = true
            try {
                when (val result = getCafeSeatsUseCase(cafeId)) {
                    is ApiResult.Success -> {
                        _seatInfo.value = result.data
                    }
                    is ApiResult.Failure -> {
                        _seatInfo.value = emptyList()
                        _events.send(result.message ?: "좌석 정보 조회 실패")
                    }
                }
            } catch (e: Exception) {
                _events.send(e.message ?: "알 수 없는 오류")
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun addUserTimePass(userId: Long, studyCafeId: Long, time: Long) {
        viewModelScope.launch(ioDispatcher) {
            _isLoading.value = true
            when (val result = addUserTimePassUseCase(userId, studyCafeId, time)) {
                is ApiResult.Success -> {
                    _events.send("시간권 추가 되었습니다.")
                }
                is ApiResult.Failure -> {
                    _events.send("시간권 추가 실패")
                }
            }
            _isLoading.value = false
        }
    }

    fun saveSeatConfig(cafeId: Long, seatList: List<Seat>) {
        viewModelScope.launch(ioDispatcher) {
            _isLoading.value = true
            // Seat 객체를 API 스펙인 SeatUpdate(또는 SeatCreate)로 변환
            val request = seatList.map { seat ->
                SeatUpdate(
                    id = seat.id.toLong(),
                    name = seat.label,
                    status = ESeatStatus.AVAILABLE, // 기본값 설정
                    position = "${seat.pos.value.x},${seat.pos.value.y},${seat.size.value.x},${seat.size.value.y}"
                )
            }

            when (val result = updateSeatsUseCase(cafeId, request)) {
                is ApiResult.Success -> _events.send("좌석 정보가 저장되었습니다.")
                is ApiResult.Failure -> _events.send(result.message ?: "저장 실패")
            }
            _isLoading.value = false
        }
    }

    // 이미지 로드
    fun loadImage(imageId: String) {
        if (_imageDataCache.value.containsKey(imageId)) return

        viewModelScope.launch(ioDispatcher) {
            try {
                when (val result = getImageUseCase(imageId)) {
                    is ApiResult.Success -> {
                        val imageData = result.data
                        if (imageData != null) {
                            val currentCache = _imageDataCache.value.toMutableMap()
                            currentCache[imageId] = imageData
                            _imageDataCache.value = currentCache
                        }
                    }

                    is ApiResult.Failure -> {

                    }
                }
            } catch (e: Exception) {

            }
        }
    }
}