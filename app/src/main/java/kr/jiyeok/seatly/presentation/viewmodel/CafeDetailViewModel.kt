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
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kr.jiyeok.seatly.data.remote.response.SeatDto
import kr.jiyeok.seatly.data.remote.response.SessionDto
import kr.jiyeok.seatly.data.remote.response.StudyCafeDetailDto
import kr.jiyeok.seatly.data.remote.response.UsageDto
import kr.jiyeok.seatly.data.repository.ApiResult
import kr.jiyeok.seatly.di.IoDispatcher
import kr.jiyeok.seatly.domain.usecase.AssignSeatUseCase
import kr.jiyeok.seatly.domain.usecase.GetCafeDetailUseCase
import kr.jiyeok.seatly.domain.usecase.GetCafeSeatsUseCase
import kr.jiyeok.seatly.domain.usecase.GetCafeUsageUseCase
import kr.jiyeok.seatly.domain.usecase.GetImageUseCase
import kr.jiyeok.seatly.domain.usecase.GetSessionsUseCase
import kr.jiyeok.seatly.domain.usecase.StartSessionUseCase
import kr.jiyeok.seatly.domain.usecase.EndSessionUseCase
import kr.jiyeok.seatly.domain.usecase.GetCurrentSessions
import kr.jiyeok.seatly.domain.usecase.RequestTimePassUseCase
import kr.jiyeok.seatly.domain.websocket.WebSocketManager
import kr.jiyeok.seatly.data.remote.response.WebSocketMessage
import javax.inject.Inject


import kr.jiyeok.seatly.domain.usecase.GetMyTimePassesUseCase
import kr.jiyeok.seatly.data.remote.response.UserTimePass

@HiltViewModel
class CafeDetailViewModel @Inject constructor(
    private val getCafeDetailUseCase: GetCafeDetailUseCase,
    private val getCafeUsageUseCase: GetCafeUsageUseCase,
    private val getCafeSeatsUseCase: GetCafeSeatsUseCase,
    private val getImageUseCase: GetImageUseCase,
    private val getSessionsUseCase: GetSessionsUseCase,
    private val assignSeatUseCase: AssignSeatUseCase,
    private val startSessionUseCase: StartSessionUseCase,
    private val endSessionUseCase: EndSessionUseCase,
    private val getCurrentSessions: GetCurrentSessions,
    private val requestTimePassUseCase: RequestTimePassUseCase,
    private val getMyTimePassesUseCase: GetMyTimePassesUseCase,
    private val webSocketManager: WebSocketManager,
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
        val seats: List<SeatDto> = emptyList(),
        val sessions: List<SessionDto> = emptyList(),
        val mySessions: List<SessionDto> = emptyList(),
        val userTimePass: UserTimePass? = null,
        val images: Map<String, Bitmap> = emptyMap(),
        val isLoadingInfo: Boolean = false,
        val isLoadingUsage: Boolean = false,
        val isLoadingSeats: Boolean = false,
        val isLoadingSessions: Boolean = false,
        val isLoadingAssignment: Boolean = false,
        val isHolding: Boolean = false,          // 좌석 클릭 시 assign 중 로딩
        val holdingSessionId: Long? = null,      // assign 완료 후 보관하는 sessionId
        val isRefreshing: Boolean = false,
        val error: String? = null
    )

    private val _uiState = MutableStateFlow(CafeDetailUiState())
    val uiState: StateFlow<CafeDetailUiState> = _uiState.asStateFlow()

    // Derived State: 하나라도 로딩 중이면 true (초기 로딩용)
    // 리프레시 중일 때는 false를 반환하여 전체 로딩 화면을 가리지 않도록 함
    val isAnyLoading: StateFlow<Boolean> = _uiState
        .map { state ->
            !state.isRefreshing && (state.isLoadingInfo ||
                    state.isLoadingUsage ||
                    state.isLoadingSeats ||
                    state.isLoadingSessions)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false
        )

    // 이벤트 채널 (Toast 메시지 등)
    private val _events = MutableSharedFlow<String>(
        replay = 0,
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val events: kotlinx.coroutines.flow.SharedFlow<String> = _events.asSharedFlow()

    // 이미지 로딩 중복 방지
    private val loadingImageIds = mutableSetOf<String>()
    val isLoading: StateFlow<Boolean> = isAnyLoading
    
    // WebSocket 구독 IDs
    private var studyCafeSubscriptionId: String? = null
    
    init {
        // WebSocket 메시지 리스닝
        viewModelScope.launch {
            webSocketManager.messages.collect { message ->
                when (message) {
                    is WebSocketMessage.TimePassEvent -> {
                        // 시간권 요청 및 응답 알림
                        val msg = when (message.type) {
                            kr.jiyeok.seatly.data.remote.response.TimePassEventType.TIMEPASS_REQUEST_ACCEPTED -> "시간권 요청이 수락되었습니다."
                            kr.jiyeok.seatly.data.remote.response.TimePassEventType.TIMEPASS_REQUEST_REJECTED -> "시간권 요청이 거절되었습니다."
                            kr.jiyeok.seatly.data.remote.response.TimePassEventType.TIMEPASS_REQUEST -> "새로운 시간권 요청이 있습니다."
                        }
                        _events.tryEmit(msg)
                    }
                    is WebSocketMessage.SeatEvent -> {
                        // 좌석 이벤트 (ASSIGNED, USAGE_STARTED, USAGE_FINISHED, HOLD_RELEASED)
                        // 좌석 리스트 로컬 업데이트 (서버 재호출 방지)
                        _uiState.update { state ->
                            val updatedSeats = state.seats.map { seat ->
                                if (seat.id == message.seatId) {
                                    val newStatus = when (message.type) {
                                        kr.jiyeok.seatly.data.remote.response.SeatEventType.ASSIGNED -> kr.jiyeok.seatly.data.remote.enums.ESeatStatus.UNAVAILABLE
                                        kr.jiyeok.seatly.data.remote.response.SeatEventType.USAGE_STARTED -> kr.jiyeok.seatly.data.remote.enums.ESeatStatus.UNAVAILABLE
                                        kr.jiyeok.seatly.data.remote.response.SeatEventType.USAGE_FINISHED -> kr.jiyeok.seatly.data.remote.enums.ESeatStatus.AVAILABLE
                                        kr.jiyeok.seatly.data.remote.response.SeatEventType.HOLD_RELEASED -> kr.jiyeok.seatly.data.remote.enums.ESeatStatus.AVAILABLE
                                    }
                                    seat.copy(status = newStatus)
                                } else {
                                    seat
                                }
                            }
                            state.copy(seats = updatedSeats)
                        }

                        // Usage 데이터는 가벼우므로 갱신 (선택적) 또는 위에서 직접 계산할 수 있음.
                        // 서버와의 Sync를 위해 세션 목록과 사용량만 가볍게 갱신
                        // 딜레이를 주어 서버 DB 반영 시간을 기다립니다.
                        val currentCafeId = uiState.value.cafeInfo?.id
                        if (currentCafeId != null) {
                            viewModelScope.launch {
                                kotlinx.coroutines.delay(500L)
                                loadSessions(currentCafeId, isRefresh = false)
                                loadCafeUsage(currentCafeId, isRefresh = false)
                            }
                        }
                    }
                    else -> {
                        // 다른 메시지 타입은 무시
                    }
                }
            }
        }
    }
    
    /**
     * 특정 카페의 좌석 이벤트를 구독합니다.
     */
    fun subscribeToCafeEvents(cafeId: Long) {
        android.util.Log.d("WebSocketManager", "subscribeToCafeEvents called: cafeId=$cafeId, currentId=$studyCafeSubscriptionId")
        if (studyCafeSubscriptionId == null) {
            studyCafeSubscriptionId = webSocketManager.subscribeToStudyCafe(cafeId)
            android.util.Log.d("WebSocketManager", "Assigned studyCafeSubscriptionId=$studyCafeSubscriptionId")
        }
    }
    
    /**
     * 해당 카페 구독을 해제합니다.
     */
    fun unsubscribeCafeEvents() {
        studyCafeSubscriptionId?.let { 
            webSocketManager.unsubscribe(it) 
            studyCafeSubscriptionId = null
        }
    }


    // =====================================================
    // Public Methods
    // =====================================================

    /**
     * 카페 상세 정보를 병렬로 로드
     * @param cafeId 카페 ID
     * @param isRefresh 강제 새로고침 여부
     */
    fun loadCafeDetailInfos(cafeId: Long, isRefresh: Boolean = false) {
        // 이미 데이터가 있고 리프레시가 아니면 로드하지 않음 (캐싱)
        if (!isRefresh && _uiState.value.cafeInfo != null) {
            return
        }

        viewModelScope.launch(ioDispatcher) {
            if (isRefresh) {
                _uiState.update { it.copy(isRefreshing = true) }
            }

            val deferreds = listOf(
                async { loadCafeInfo(cafeId, isRefresh) },
                async { loadCafeUsage(cafeId, isRefresh) },
                async { loadSeatInfo(cafeId, isRefresh) },
                async { loadSessions(cafeId, isRefresh) },
                async { loadMySessions() },
                async { loadMyTimePass(cafeId) }
            )
            deferreds.awaitAll()

            if (isRefresh) {
                _uiState.update { it.copy(isRefreshing = false) }
            }
        }
    }

    /**
     * 당겨서 새로고침
     */
    fun refresh(cafeId: Long) {
        loadCafeDetailInfos(cafeId, isRefresh = true)
    }


    // =====================================================
    // Private Methods
    // =====================================================

    /**
     * 카페 기본 정보 로드
     */
    private suspend fun loadCafeInfo(cafeId: Long, isRefresh: Boolean) {
        if (!isRefresh) _uiState.update { it.copy(isLoadingInfo = true) }

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
                    _events.tryEmit(result.message ?: "카페 정보 조회 실패")
                }
            }
        } catch (e: Exception) {
            _uiState.update {
                it.copy(
                    isLoadingInfo = false,
                    error = e.message
                )
            }
            _events.tryEmit(e.message ?: "알 수 없는 오류")
        }
    }

    /**
     * 카페 사용 현황 로드
     */
    private suspend fun loadCafeUsage(cafeId: Long, isRefresh: Boolean = false) {
        if (!isRefresh) _uiState.update { it.copy(isLoadingUsage = true) }

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
     * 좌석 정보 로드
     */
    private suspend fun loadSeatInfo(cafeId: Long, isRefresh: Boolean) {
        if (!isRefresh) _uiState.update { it.copy(isLoadingSeats = true) }

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
                    _events.tryEmit(result.message ?: "좌석 정보 조회 실패")
                }
            }
        } catch (e: Exception) {
            _uiState.update { it.copy(isLoadingSeats = false) }
            _events.tryEmit(e.message ?: "알 수 없는 오류")
        }
    }

    /**
     * 세션 정보 로드
     */
    private suspend fun loadSessions(cafeId: Long, isRefresh: Boolean) {
        if (!isRefresh) _uiState.update { it.copy(isLoadingSessions = true) }
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

    /**
     * [Step 1] 좌석 점유 (클릭 시 호출)
     * 서버에 assign을 요청하여 다른 사용자가 동시에 접근하지 못하도록 잠금.
     * 성공 시 holdingSessionId에 세션 ID를 저장함.
     */
    fun holdSeat(seatId: String, currentCafeId: Long) {
        viewModelScope.launch(ioDispatcher) {
            _uiState.update { it.copy(isHolding = true, holdingSessionId = null) }
            try {
                when (val assignResult = assignSeatUseCase(seatId)) {
                    is ApiResult.Success -> {
                        val session = assignResult.data
                        if (session != null) {
                            _uiState.update {
                                it.copy(
                                    isHolding = false,
                                    holdingSessionId = session.id
                                )
                            }
                            // 세션 목록 갱신 (다른 사용자에게도 점유 상태 반영)
                            loadSessions(currentCafeId, isRefresh = true)
                        } else {
                            _uiState.update { it.copy(isHolding = false) }
                            _events.tryEmit("세션 정보를 가져올 수 없습니다.")
                        }
                    }
                    is ApiResult.Failure -> {
                        _uiState.update { it.copy(isHolding = false) }
                        val errorMessage = assignResult.message ?: "좌석 점유 실패. 다른 사용자가 이미 선택했을 수 있습니다."
                        
                        // 이미 이용 중인 세션 오류 메시지는 무시 (토스트 안 띄움)
                        if (errorMessage != "이미 이용 중인 세션이 존재합니다.") {
                            _events.tryEmit(errorMessage)
                        }
                    }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isHolding = false) }
                _events.tryEmit(e.message ?: "알 수 없는 오류")
            }
        }
    }

    /**
     * [Step 2] 이용 시작 ("이용 시작하기" 버튼 클릭 시 호출)
     * holdSeat()에서 보관한 holdingSessionId로 이용을 시작함.
     */
    fun startHeldSession(currentCafeId: Long) {
        val sessionId = _uiState.value.holdingSessionId ?: return
        viewModelScope.launch(ioDispatcher) {
            _uiState.update { it.copy(isLoadingAssignment = true) }
            try {
                when (val startResult = startSessionUseCase(sessionId)) {
                    is ApiResult.Success -> {
                        _uiState.update { it.copy(isLoadingAssignment = false, holdingSessionId = null) }
                        _events.tryEmit("좌석 이용이 시작되었습니다.")
                        loadSessions(currentCafeId, isRefresh = true)
                        loadMySessions()
                        loadMyTimePass(currentCafeId)
                    }
                    is ApiResult.Failure -> {
                        _uiState.update { it.copy(isLoadingAssignment = false) }
                        _events.tryEmit(startResult.message ?: "이용 시작 실패")
                    }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoadingAssignment = false) }
                _events.tryEmit(e.message ?: "알 수 없는 오류")
            }
        }
    }

    /**
     * 점유 취소 (패널 닫기 시 호출)
     * holdingSessionId가 있으면 해당 세션을 삭제하여 점유를 해제함.
     */
    fun cancelHold(currentCafeId: Long) {
        val sessionId = _uiState.value.holdingSessionId ?: return
        _uiState.update { it.copy(holdingSessionId = null) }
        viewModelScope.launch(ioDispatcher) {
            try {
                // assign된 세션을 삭제하여 점유 해제
                endSessionUseCase(sessionId)
                loadSessions(currentCafeId, isRefresh = true)
            } catch (_: Exception) {
                // 취소 실패는 무시 (로컬 상태는 이미 초기화됨)
            }
        }
    }

    /**
     * 좌석 이용 종료
     */
    fun endSession(sessionId: Long, currentCafeId: Long) {
        viewModelScope.launch(ioDispatcher) {
            _uiState.update { it.copy(isLoadingAssignment = true) }
            try {
                when (val result = endSessionUseCase(sessionId)) {
                    is ApiResult.Success -> {
                        _uiState.update { 
                            it.copy(
                                isLoadingAssignment = false,
                                mySessions = it.mySessions.filter { session -> session.id != sessionId }
                            )
                        }
                        _events.tryEmit("좌석 이용이 종료되었습니다.")
                        loadSessions(currentCafeId, isRefresh = true)
                        loadMySessions()
                        loadMyTimePass(currentCafeId)
                    }
                    is ApiResult.Failure -> {
                        _uiState.update { it.copy(isLoadingAssignment = false) }
                        _events.tryEmit(result.message ?: "이용 종료 실패")
                    }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoadingAssignment = false) }
                _events.tryEmit(e.message ?: "알 수 없는 오류")
            }
        }
    }

    /**
     * 현재 사용자의 활성 세션 목록 로드
     */
    private suspend fun loadMySessions() {
        try {
            when (val result = getCurrentSessions()) {
                is ApiResult.Success -> {
                    _uiState.update {
                        it.copy(mySessions = result.data ?: emptyList())
                    }
                }
                is ApiResult.Failure -> {
                    // 실패해도 무시
                }
            }
        } catch (_: Exception) {
            // 예외 무시
        }
    }

    /**
     * 내 시간권 조회 (해당 카페용)
     */
    private suspend fun loadMyTimePass(cafeId: Long) {
        try {
            when (val result = getMyTimePassesUseCase()) {
                is ApiResult.Success -> {
                    val timePass = result.data?.find { it.studyCafeId == cafeId }
                    _uiState.update {
                        it.copy(userTimePass = timePass)
                    }
                }
                is ApiResult.Failure -> {
                    // 실패해도 무시
                }
            }
        } catch (_: Exception) {
            // 예외 무시
        }
    }

    /**
     * 시간권 요청
     */
    fun requestTimePass(studyCafeId: Long, time: Long) {
        viewModelScope.launch(ioDispatcher) {
            try {
                when (val result = requestTimePassUseCase(studyCafeId, time)) {
                    is ApiResult.Success -> {
                        _events.tryEmit("시간권 요청이 완료되었습니다. 관리자 승인 후 사용 가능합니다.")
                    }
                    is ApiResult.Failure -> {
                        _events.tryEmit(result.message ?: "시간권 요청 실패")
                    }
                }
            } catch (e: Exception) {
                _events.tryEmit(e.message ?: "알 수 없는 오류")
            }
        }
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

    override fun onCleared() {
        super.onCleared()
        unsubscribeCafeEvents()
    }
}
