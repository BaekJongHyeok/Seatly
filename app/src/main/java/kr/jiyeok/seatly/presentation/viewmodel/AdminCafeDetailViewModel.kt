package kr.jiyeok.seatly.presentation.viewmodel

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kr.jiyeok.seatly.data.local.Seat
import kr.jiyeok.seatly.data.remote.enums.ESeatStatus
import kr.jiyeok.seatly.data.remote.request.SeatCreate
import kr.jiyeok.seatly.data.remote.request.SeatUpdate
import kr.jiyeok.seatly.data.remote.response.SeatDto
import kr.jiyeok.seatly.data.remote.response.SessionDto
import kr.jiyeok.seatly.data.remote.response.StudyCafeDetailDto
import kr.jiyeok.seatly.data.remote.response.UsageDto
import kr.jiyeok.seatly.data.remote.response.UserTimePassInfo
import kr.jiyeok.seatly.data.repository.ApiResult
import kr.jiyeok.seatly.di.IoDispatcher
import kr.jiyeok.seatly.domain.usecase.AddUserTimePassUseCase
import kr.jiyeok.seatly.domain.usecase.CreateSeatsUseCase
import kr.jiyeok.seatly.domain.usecase.DeleteSeatUseCase
import kr.jiyeok.seatly.domain.usecase.GetCafeDetailUseCase
import kr.jiyeok.seatly.domain.usecase.GetCafeSeatsUseCase
import kr.jiyeok.seatly.domain.usecase.GetCafeUsageUseCase
import kr.jiyeok.seatly.domain.usecase.GetImageUseCase
import kr.jiyeok.seatly.domain.usecase.GetSessionsUseCase
import kr.jiyeok.seatly.domain.usecase.GetUsersWithTimePassUseCase
import kr.jiyeok.seatly.domain.usecase.UpdateSeatsUseCase
import javax.inject.Inject

@HiltViewModel
class AdminCafeDetailViewModel @Inject constructor(
    private val getCafeDetailUseCase: GetCafeDetailUseCase,
    private val getCafeUsageUseCase: GetCafeUsageUseCase,
    private val getCafeMembersUseCase: GetUsersWithTimePassUseCase,
    private val addUserTimePassUseCase: AddUserTimePassUseCase,
    private val getCafeSeatsUseCase: GetCafeSeatsUseCase,
    private val createSeatsUseCase: CreateSeatsUseCase,
    private val updateSeatsUseCase: UpdateSeatsUseCase,
    private val deleteSeatUseCase: DeleteSeatUseCase,
    private val getImageUseCase: GetImageUseCase,
    private val getSessionsUseCase: GetSessionsUseCase,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : ViewModel() {

    // =====================================================
    // UI State
    // =====================================================

    /**
     * 카페 상세 화면의 전체 UI 상태를 관리하는 데이터 클래스
     */
    data class CafeDetailUiState(
        val cafeInfo: StudyCafeDetailDto? = null,
        val cafeUsage: UsageDto? = null,
        val members: List<UserTimePassInfo> = emptyList(),
        val seats: List<SeatDto> = emptyList(),
        val sessions: List<SessionDto> = emptyList(),
        val images: Map<String, Bitmap> = emptyMap(),
        val isLoadingInfo: Boolean = false,
        val isLoadingUsage: Boolean = false,
        val isLoadingMembers: Boolean = false,
        val isLoadingSeats: Boolean = false,
        val isLoadingSessions: Boolean = false,
        val error: String? = null
    )

    private val _uiState = MutableStateFlow(CafeDetailUiState())
    val uiState: StateFlow<CafeDetailUiState> = _uiState.asStateFlow()

    // Derived State: 하나라도 로딩 중이면 true
    val isAnyLoading: StateFlow<Boolean> = _uiState
        .map { state ->
            state.isLoadingInfo ||
                    state.isLoadingUsage ||
                    state.isLoadingMembers ||
                    state.isLoadingSeats ||
                    state.isLoadingSessions
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false
        )

    // 이벤트 채널 (Toast 메시지 등)
    private val _events = Channel<String>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    // 이미지 로딩 중복 방지
    private val loadingImageIds = mutableSetOf<String>()

    // =====================================================
    // Public Methods
    // =====================================================

    /**
     * 카페 상세 정보를 병렬로 로드
     */
    fun loadCafeDetailInfos(cafeId: Long) {
        viewModelScope.launch(ioDispatcher) {
            awaitAll(
                async { loadCafeInfo(cafeId) },
                async { loadCafeUsage(cafeId) },
                async { loadCafeMembers(cafeId) },
                async { loadSeatInfo(cafeId) },
                async { loadSessions(cafeId) }
            )
        }
    }

    /**
     * 시간권 추가
     */
    fun addUserTimePass(userId: Long, studyCafeId: Long, time: Long) {
        viewModelScope.launch(ioDispatcher) {
            when (val result = addUserTimePassUseCase(userId, studyCafeId, time)) {
                is ApiResult.Success -> {
                    _events.send("시간권이 추가되었습니다.")
                    // 멤버 목록 다시 로드
                    loadCafeMembers(studyCafeId)
                }
                is ApiResult.Failure -> {
                    _events.send(result.message ?: "시간권 추가 실패")
                }
            }
        }
    }

    /**
     * 이미지 로드 (공개 메서드, UI에서 직접 호출 가능)
     */
    fun loadImage(imageId: String) {
        // 이미 로드되었거나 로딩 중이면 스킵
        if (_uiState.value.images.containsKey(imageId)) return

        synchronized(loadingImageIds) {
            if (loadingImageIds.contains(imageId)) return
            loadingImageIds.add(imageId)
        }

        viewModelScope.launch(ioDispatcher) {
            try {
                when (val result = getImageUseCase(imageId)) {
                    is ApiResult.Success -> {
                        result.data?.let { imageData ->
                            val bitmap = withContext(Dispatchers.Default) {
                                decodeSampledBitmap(imageData, 800, 800)
                            }
                            bitmap?.let {
                                _uiState.update { state ->
                                    state.copy(images = state.images + (imageId to it))
                                }
                            }
                        }
                    }
                    is ApiResult.Failure -> {
                        // 이미지 로드 실패는 무시 (선택적)
                    }
                }
            } catch (e: Exception) {
                // 예외 발생 시 무시
            } finally {
                synchronized(loadingImageIds) {
                    loadingImageIds.remove(imageId)
                }
            }
        }
    }

    /**
     * 세션 정보 로드
     */
    private suspend fun loadSessions(cafeId: Long) {
        _uiState.update { it.copy(isLoadingSessions = true) }
        try {
            when (val result = getSessionsUseCase(cafeId)) {
                is ApiResult.Success -> {
                    _uiState.update {
                        it.copy(
                            sessions = result.data ?: emptyList(),
                            isLoadingSessions = false
                        )
                    }
                }
                is ApiResult.Failure -> {
                    _uiState.update { it.copy(isLoadingSessions = false) }
                    // 세션 정보는 중요도가 낮으므로 실패해도 Toast 표시 안 함
                }
            }
        } catch (e: Exception) {
            _uiState.update { it.copy(isLoadingSessions = false) }
        }
    }

    // =====================================================
    // Private Methods
    // =====================================================

    /**
     * 카페 기본 정보 로드
     */
    private suspend fun loadCafeInfo(cafeId: Long) {
        _uiState.update { it.copy(isLoadingInfo = true) }

        try {
            when (val result = getCafeDetailUseCase(cafeId)) {
                is ApiResult.Success -> {
                    val cafeInfo = result.data
                    _uiState.update {
                        it.copy(
                            cafeInfo = cafeInfo,
                            isLoadingInfo = false,
                            error = null
                        )
                    }

                    // 이미지 자동 로드
                    cafeInfo?.imageUrls?.forEach { imageId ->
                        loadImage(imageId)
                    }
                }
                is ApiResult.Failure -> {
                    _uiState.update {
                        it.copy(
                            isLoadingInfo = false,
                            error = result.message
                        )
                    }
                    _events.send(result.message ?: "카페 정보 조회 실패")
                }
            }
        } catch (e: Exception) {
            _uiState.update {
                it.copy(
                    isLoadingInfo = false,
                    error = e.message
                )
            }
            _events.send(e.message ?: "알 수 없는 오류")
        }
    }

    /**
     * 카페 사용 현황 로드
     */
    private suspend fun loadCafeUsage(cafeId: Long) {
        _uiState.update { it.copy(isLoadingUsage = true) }

        try {
            when (val result = getCafeUsageUseCase(cafeId)) {
                is ApiResult.Success -> {
                    _uiState.update {
                        it.copy(
                            cafeUsage = result.data,
                            isLoadingUsage = false
                        )
                    }
                }
                is ApiResult.Failure -> {
                    _uiState.update { it.copy(isLoadingUsage = false) }
                    // Usage는 중요도가 낮으므로 실패해도 Toast 표시 안 함
                }
            }
        } catch (e: Exception) {
            _uiState.update { it.copy(isLoadingUsage = false) }
        }
    }

    /**
     * 카페 멤버 목록 로드
     */
    fun loadCafeMembers(cafeId: Long) {
        viewModelScope.launch(ioDispatcher) {
            _uiState.update { it.copy(isLoadingMembers = true) }

            try {
                when (val result = getCafeMembersUseCase(cafeId)) {
                    is ApiResult.Success -> {
                        _uiState.update {
                            it.copy(
                                members = result.data ?: emptyList(),
                                isLoadingMembers = false
                            )
                        }
                    }
                    is ApiResult.Failure -> {
                        _uiState.update { it.copy(isLoadingMembers = false) }
                        _events.send(result.message ?: "카페 멤버 조회 실패")
                    }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoadingMembers = false) }
                _events.send(e.message ?: "알 수 없는 오류")
            }
        }
    }

    /**
     * 좌석 정보 로드
     */
    private suspend fun loadSeatInfo(cafeId: Long) {
        _uiState.update { it.copy(isLoadingSeats = true) }

        try {
            when (val result = getCafeSeatsUseCase(cafeId)) {
                is ApiResult.Success -> {
                    _uiState.update {
                        it.copy(
                            seats = result.data ?: emptyList(),
                            isLoadingSeats = false
                        )
                    }
                }
                is ApiResult.Failure -> {
                    _uiState.update { it.copy(isLoadingSeats = false) }
                    _events.send(result.message ?: "좌석 정보 조회 실패")
                }
            }
        } catch (e: Exception) {
            _uiState.update { it.copy(isLoadingSeats = false) }
            _events.send(e.message ?: "알 수 없는 오류")
        }
    }

    /**
     * 좌석 설정 저장 (CREATE, UPDATE, DELETE 모두 처리)
     */
    fun saveSeatConfig(cafeId: Long, currentSeats: List<Seat>) {
        viewModelScope.launch(ioDispatcher) {
            _uiState.update { it.copy(isLoadingSeats = true) }
            try {
                val serverSeats = uiState.value.seats
                val serverSeatIds = serverSeats.map { it.id.toString() }.toSet()

                android.util.Log.d("SeatConfig", "=== Save Seat Config ===")
                android.util.Log.d("SeatConfig", "Server seat IDs: $serverSeatIds")
                currentSeats.forEach { seat ->
                    android.util.Log.d("SeatConfig", "Current seat: id=${seat.id}, label=${seat.label}, isNew=${seat.id.startsWith("NEW_")}")
                }

                // 1. 새로 생성할 좌석 필터링
                val newSeats = currentSeats.filter { it.id.startsWith("NEW_") }

                // 2. 수정할 좌석 필터링
                val updateSeats = currentSeats.filter { seat ->
                    !seat.id.startsWith("NEW_") && serverSeatIds.contains(seat.id)
                }

                // 3. 삭제할 좌석 필터링
                val deleteIds = serverSeats
                    .map { it.id.toString() }
                    .filter { serverId ->
                        currentSeats.none { it.id == serverId }
                    }

                android.util.Log.d("SeatConfig", "New seats (CREATE): ${newSeats.size} -> ${newSeats.map { it.label }}")
                android.util.Log.d("SeatConfig", "Update seats (PATCH): ${updateSeats.size} -> ${updateSeats.map { it.label }}")
                android.util.Log.d("SeatConfig", "Delete seat IDs: ${deleteIds.size} -> $deleteIds")

                // 4. 좌석 삭제 (DELETE) - 먼저 실행
                var deleteFailed = false
                for (deleteId in deleteIds) {
                    when (val result = deleteSeatUseCase(cafeId, deleteId)) {
                        is ApiResult.Failure -> {
                            _events.send("좌석 삭제 실패: ${result.message}")
                            deleteFailed = true
                        }
                        else -> {
                            android.util.Log.d("SeatConfig", "Deleted seat id=$deleteId")
                        }
                    }
                }

                // ★ 추가: DELETE 완료 후 서버 트랜잭션 커밋 대기 ★
                if (deleteIds.isNotEmpty()) {
                    kotlinx.coroutines.delay(300) // 300ms 대기
                    android.util.Log.d("SeatConfig", "Waited 300ms for DELETE transaction commit")
                }

                // 5. 기존 좌석 수정 (PATCH) - 두 번째로 실행
                if (updateSeats.isNotEmpty()) {
                    val updateRequest = updateSeats.map { seat ->
                        SeatUpdate(
                            id = seat.id.toLong(),
                            name = seat.label,
                            status = ESeatStatus.AVAILABLE,
                            position = buildPositionString(seat)
                        )
                    }
                    when (val result = updateSeatsUseCase(cafeId, updateRequest)) {
                        is ApiResult.Failure -> {
                            _uiState.update { it.copy(isLoadingSeats = false) }
                            _events.send(result.message ?: "좌석 수정 실패")
                            return@launch
                        }
                        else -> {
                            android.util.Log.d("SeatConfig", "Updated ${updateSeats.size} seats")
                        }
                    }
                }

                // 6. 새 좌석 생성 (POST) - 마지막에 실행
                if (newSeats.isNotEmpty()) {
                    val createRequest = newSeats.map { seat ->
                        SeatCreate(
                            name = seat.label,
                            status = ESeatStatus.AVAILABLE,
                            position = buildPositionString(seat)
                        )
                    }
                    when (val result = createSeatsUseCase(cafeId, createRequest)) {
                        is ApiResult.Failure -> {
                            _uiState.update { it.copy(isLoadingSeats = false) }
                            _events.send(result.message ?: "좌석 생성 실패")
                            return@launch
                        }
                        else -> {
                            android.util.Log.d("SeatConfig", "Created ${newSeats.size} seats")
                        }
                    }
                }

                // 7. 성공 메시지 및 데이터 리로드
                val message = when {
                    newSeats.isEmpty() && updateSeats.isEmpty() && deleteIds.isEmpty() ->
                        "변경 사항이 없습니다."
                    deleteFailed ->
                        "좌석 정보가 부분적으로 저장되었습니다."
                    else ->
                        "좌석 정보가 저장되었습니다."
                }
                _events.send(message)

                // 서버에서 최신 데이터 다시 로드
                loadSeatInfo(cafeId)

            } catch (e: Exception) {
                _uiState.update { it.copy(isLoadingSeats = false) }
                _events.send(e.message ?: "저장 중 오류 발생")
            }
        }
    }

    private fun buildPositionString(seat: Seat): String {
        return "${seat.pos.value.x},${seat.pos.value.y},${seat.size.value.x},${seat.size.value.y}"
    }

    /**
     * 샘플링하여 Bitmap 디코딩 (메모리 최적화)
     */
    private fun decodeSampledBitmap(
        data: ByteArray,
        reqWidth: Int,
        reqHeight: Int
    ): Bitmap? {
        return try {
            val options = BitmapFactory.Options().apply {
                // 먼저 이미지 크기만 확인
                inJustDecodeBounds = true
                BitmapFactory.decodeByteArray(data, 0, data.size, this)

                // 샘플링 비율 계산
                inSampleSize = calculateInSampleSize(this, reqWidth, reqHeight)

                // 실제 디코딩
                inJustDecodeBounds = false
            }

            BitmapFactory.decodeByteArray(data, 0, data.size, options)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * 샘플링 비율 계산
     */
    private fun calculateInSampleSize(
        options: BitmapFactory.Options,
        reqWidth: Int,
        reqHeight: Int
    ): Int {
        val (height: Int, width: Int) = options.run { outHeight to outWidth }
        var inSampleSize = 1

        if (height > reqHeight || width > reqWidth) {
            val halfHeight: Int = height / 2
            val halfWidth: Int = width / 2

            while (halfHeight / inSampleSize >= reqHeight &&
                halfWidth / inSampleSize >= reqWidth) {
                inSampleSize *= 2
            }
        }

        return inSampleSize
    }

    // =====================================================
    // Lifecycle
    // =====================================================

    override fun onCleared() {
        super.onCleared()
        // Bitmap 메모리 해제
        _uiState.value.images.values.forEach { bitmap ->
            if (!bitmap.isRecycled) {
                bitmap.recycle()
            }
        }
    }
}
