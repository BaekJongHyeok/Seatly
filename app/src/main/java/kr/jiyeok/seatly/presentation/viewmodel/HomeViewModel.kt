package kr.jiyeok.seatly.presentation.viewmodel

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kr.jiyeok.seatly.data.remote.enums.EStatus
import kr.jiyeok.seatly.data.remote.request.UpdateUserInfoRequest
import kr.jiyeok.seatly.data.remote.response.*
import kr.jiyeok.seatly.data.repository.ApiResult
import kr.jiyeok.seatly.data.repository.NotificationRepository
import kr.jiyeok.seatly.di.IoDispatcher
import kr.jiyeok.seatly.di.TokenProvider
import kr.jiyeok.seatly.domain.usecase.*
import kr.jiyeok.seatly.domain.websocket.WebSocketManager
import kr.jiyeok.seatly.util.NotificationHelper
import kr.jiyeok.seatly.data.remote.enums.ERole
import javax.inject.Inject

/**
 * HomeScreen ViewModel
 */

sealed interface HomeUiState {
    object Idle : HomeUiState
    object Loading : HomeUiState
    data class Success(val message: String = "") : HomeUiState
    data class Error(val message: String) : HomeUiState
}

@HiltViewModel
class HomeViewModel @Inject constructor(
    // User 관련 UseCase
    private val getUserInfoUseCase: GetUserInfoUseCase,
    private val getFavoriteCafesUseCase: GetFavoriteCafesUseCase,
    private val getCurrentSessions: GetCurrentSessions,
    private val getMyTimePassesUseCase: GetMyTimePassesUseCase,
    private val getImageUseCase: GetImageUseCase,

    // Cafe 관련 UseCase
    private val getStudyCafesUseCase: GetStudyCafesUseCase,
    private val addFavoriteCafeUseCase: AddFavoriteCafeUseCase,
    private val removeFavoriteCafeUseCase: RemoveFavoriteCafeUseCase,

    // Seat 관련 UseCase
    private val getCafeSeatsUseCase: GetCafeSeatsUseCase,
    private val getSessionsUseCase: GetSessionsUseCase,
    private val endSessionUseCase: EndSessionUseCase,

    // WebSocket & 알림
    private val webSocketManager: WebSocketManager,
    private val notificationRepository: NotificationRepository,
    private val tokenProvider: TokenProvider,
    private val notificationHelper: NotificationHelper,

    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : ViewModel() {

    companion object {
        private const val TAG = "HomeViewModel"
    }

    // =====================================================
    // State
    // =====================================================

    private val _userData = MutableStateFlow<UserInfoSummaryDto?>(null)
    val userData: StateFlow<UserInfoSummaryDto?> = _userData.asStateFlow()

    private val _userState = MutableStateFlow<HomeUiState>(HomeUiState.Idle)
    val userState: StateFlow<HomeUiState> = _userState.asStateFlow()

    private val _userSessions = MutableStateFlow<List<SessionDto>?>(null)
    val userSessions: StateFlow<List<SessionDto>?> = _userSessions.asStateFlow()

    private val _userTimePasses = MutableStateFlow<List<UserTimePass>?>(null)
    val userTimePasses: StateFlow<List<UserTimePass>?> = _userTimePasses.asStateFlow()

    private val _seatNames = MutableStateFlow<Map<Long, String>>(emptyMap())
    val seatNames: StateFlow<Map<Long, String>> = _seatNames.asStateFlow()

    // =====================================================
    // State Management - Cafes
    // =====================================================

    private val _cafes = MutableStateFlow<List<StudyCafeSummaryDto>>(emptyList())
    val cafes: StateFlow<List<StudyCafeSummaryDto>> = _cafes.asStateFlow()



    private val _favoriteCafeIds = MutableStateFlow<List<Long>>(emptyList())
    val favoriteCafeIds: StateFlow<List<Long>> = _favoriteCafeIds.asStateFlow()

    // =====================================================
    // State Management - Sessions
    // =====================================================

    private val _currentSession = MutableStateFlow<SessionDto?>(null)
    val currentSession: StateFlow<SessionDto?> = _currentSession.asStateFlow()

    private val _imageBitmapCache = MutableStateFlow<Map<String, Bitmap>>(emptyMap())
    val imageBitmapCache: StateFlow<Map<String, Bitmap>> = _imageBitmapCache.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _events = MutableSharedFlow<String>(
        replay = 0,
        extraBufferCapacity = 1,
        onBufferOverflow = kotlinx.coroutines.channels.BufferOverflow.DROP_OLDEST
    )
    val events: kotlinx.coroutines.flow.SharedFlow<String> = _events.asSharedFlow()

    private val loadingImageIds = mutableSetOf<String>()

    // WebSocket 구독 ID
    private var timePassSubscriptionId: String? = null
    private var isWebSocketConnected = false

    init {
        // WebSocket 메시지 리스닝
        viewModelScope.launch {
            webSocketManager.messages.collect { message ->
                when (message) {
                    is WebSocketMessage.TimePassEvent -> {
                        when (message.type) {
                            TimePassEventType.TIMEPASS_REQUEST_ACCEPTED,
                            TimePassEventType.TIMEPASS_REQUEST_REJECTED -> {
                                val approved = message.type == TimePassEventType.TIMEPASS_REQUEST_ACCEPTED
                                val msg = if (approved) "시간권 요청이 수락되었습니다." else "시간권 요청이 거절되었습니다."
                                notificationRepository.addTimePassNotification(
                                    approved = approved,
                                    message = msg
                                )
                                _events.tryEmit(msg)
                                Log.d(TAG, "TimePassResponse received: ${message.type}")
                            }
                            TimePassEventType.TIMEPASS_REQUEST -> {
                                // 관리자에게 온 시간권 요청 알림
                                if (userData.value?.role == ERole.ADMIN) {
                                    val hours = message.request.time / 3600
                                    val minutes = (message.request.time % 3600) / 60
                                    val timeString = if (minutes > 0) "${hours}시간 ${minutes}분" else "${hours}시간"
                                    
                                    notificationHelper.showTimePassRequestNotification(
                                        title = "시간권 요청",
                                        message = "새로운 유저가 ${timeString} 이용권을 요청했습니다."
                                    )
                                }
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

    // =====================================================
    // Public Methods
    // =====================================================

    fun loadHomeData(studyCafeId: Long? = null) {
        viewModelScope.launch(ioDispatcher) {
            _isLoading.value = true
            _userState.value = HomeUiState.Loading

            try {
                // 유저 기본 정보 조회
                loadUserInfo()

                // 유저 즐겨찾기 카페 정보 조회
                loadFavoriteCafes()

                // 유저 현재 세션 정보 조회
                loadCurrentSessionsInfo()

                // 유저 현재 시간권 정보 조회
                loadMyTimePasses()

                // 전체 카페 목록 조회
                loadCafes()

                if (studyCafeId != null) {
                    loadCurrentSession(studyCafeId)
                }
                _userState.value = HomeUiState.Success("홈 데이터 로드 완료")
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun loadUserInfo() {
        viewModelScope.launch(ioDispatcher) {
            try {
                when (val result = getUserInfoUseCase()) {
                    is ApiResult.Success -> {
                        val userData = result.data
                        if (userData != null) {
                            _userData.value = userData
                            // 유저 정보 로드 성공 후 WebSocket 연결
                            connectWebSocket(userData.id)
                        }
                    }
                    is ApiResult.Failure -> {
                        _events.tryEmit(result.message ?: "사용자 정보 조회 실패")
                    }
                }
            } catch (e: Exception) {
                _events.tryEmit(e.message ?: "알 수 없는 오류")
            }
        }
    }

    /**
     * WebSocket 연결 및 시간권 이벤트 구독
     */
    private fun connectWebSocket(userId: Long) {
        if (isWebSocketConnected) return

        val token = tokenProvider.getAccessToken()
        if (token.isNullOrEmpty()) {
            Log.w(TAG, "No access token available for WebSocket connection")
            return
        }

        try {
            webSocketManager.connect(token)
            timePassSubscriptionId = webSocketManager.subscribeToTimePassEvents(userId)
            
            // 사용자 좌석 이벤트 구독 추가
            webSocketManager.subscribeToUserSeatEvents(userId)
            
            isWebSocketConnected = true
            Log.d(TAG, "WebSocket connected and subscribed to events for userId=$userId")
        } catch (e: Exception) {
            Log.e(TAG, "WebSocket connection failed", e)
        }
    }

    private fun loadFavoriteCafes() {
        viewModelScope.launch(ioDispatcher) {
            try {
                when (val result = getFavoriteCafesUseCase()) {
                    is ApiResult.Success -> {
                        val cafes = result.data
                        if (cafes != null) {
                            _favoriteCafeIds.value = cafes
                        }
                    }
                    is ApiResult.Failure -> {
                        _events.tryEmit(result.message ?: "즐겨찾기 카페 조회 실패")
                    }
                }
            } catch (e: Exception) {
                _events.tryEmit(e.message ?: "알 수 없는 오류")
            }
        }
    }

    private fun loadCurrentSessionsInfo() {
        viewModelScope.launch(ioDispatcher) {
            try {
                when (val result = getCurrentSessions()) {
                    is ApiResult.Success -> {
                        val session = result.data
                        if (session != null) {
                            _userSessions.value = session
                            // 세션에 해당하는 좌석 이름 로드
                            session.forEach { sessionDto ->
                                loadSeatName(sessionDto.studyCafeId, sessionDto.id, sessionDto.seatId)
                            }
                        }
                    }
                    is ApiResult.Failure -> {
                        _events.tryEmit(result.message ?: "현재 세션 정보 조회 실패")
                    }
                }
            } catch (e: Exception) {
                _events.tryEmit(e.message ?: "알 수 없는 오류")
            }
        }
    }

    private fun loadSeatName(studyCafeId: Long, sessionId: Long, seatId: Long) {
        viewModelScope.launch(ioDispatcher) {
            try {
                // 해당 카페의 모든 좌석 정보를 가져옴
                when (val result = getCafeSeatsUseCase(studyCafeId)) {
                    is ApiResult.Success -> {
                        val seats = result.data ?: emptyList()
                        val seat = seats.find { it.id == seatId }
                        if (seat != null) {
                            _seatNames.update { it + (sessionId to seat.name) }
                        }
                    }
                    is ApiResult.Failure -> {
                        // 좌석 정보 로드 실패 시 무시하거나 로깅
                    }
                }
            } catch (e: Exception) {
                // 예외 발생 시 무시
            }
        }
    }

    private fun loadMyTimePasses() {
        viewModelScope.launch(ioDispatcher) {
            try {
                when (val result = getMyTimePassesUseCase()) {
                    is ApiResult.Success -> {
                        val timePasses = result.data
                        if (timePasses != null) {
                            _userTimePasses.value = timePasses
                        }
                    }
                    is ApiResult.Failure -> {
                        _events.tryEmit(result.message ?: "현재 세션 정보 조회 실패")
                    }
                }
            } catch (e: Exception) {
                _events.tryEmit(e.message ?: "알 수 없는 오류")
            }
        }
    }

    fun loadCafes() {
        viewModelScope.launch(ioDispatcher) {
            try {
                when (val result = getStudyCafesUseCase()) {
                    is ApiResult.Success -> {
                        val cafeList = result.data ?: emptyList()
                        _cafes.value = cafeList
                        // 이미지 병렬 배치 로드 (3개씩 동시 로드)
                        val imageIds = cafeList.mapNotNull { it.mainImageUrl }
                        imageIds.chunked(3).forEach { batch ->
                            batch.map { imageId ->
                                async { loadImage(imageId) }
                            }.awaitAll()
                        }
                    }
                    is ApiResult.Failure -> {
                        _cafes.value = emptyList()
                        _events.tryEmit(result.message ?: "카페 목록 조회 실패")
                    }
                }
            } catch (e: Exception) {
                _cafes.value = emptyList()
                _events.tryEmit(e.message ?: "알 수 없는 오류")
            }
        }
    }



    // =====================================================
    // Public Methods - 세션 관리
    // =====================================================

    fun loadCurrentSession(studyCafeId: Long) {
        viewModelScope.launch(ioDispatcher) {
            try {
                when (val result = getSessionsUseCase(studyCafeId)) {
                    is ApiResult.Success -> {
                        val sessions = result.data ?: emptyList()
                        val activeSession = sessions.firstOrNull { session ->
                            session.status == EStatus.IN_USE ||
                                    session.status == EStatus.ASSIGNED
                        }
                        _currentSession.value = activeSession
                    }
                    is ApiResult.Failure -> {
                        _currentSession.value = null
                    }
                }
            } catch (e: Exception) {
                _currentSession.value = null
            }
        }
    }

    fun endCurrentSession(sessionId: Long) {
        viewModelScope.launch(ioDispatcher) {
            _isLoading.value = true
            try {
                when (val result = endSessionUseCase(sessionId)) {
                    is ApiResult.Success -> {
                        _currentSession.value = null
                        _events.tryEmit("이용이 종료되었습니다")
                        // 세션 목록 갱신
                        loadCurrentSessionsInfo()
                        // 시간권 목록도 갱신 (시간 차감 반영)
                        loadMyTimePasses()
                    }
                    is ApiResult.Failure -> {
                        _events.tryEmit(result.message ?: "세션 종료 실패")
                    }
                }
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun refreshCurrentSession(studyCafeId: Long) {
        viewModelScope.launch(ioDispatcher) {
            loadCurrentSession(studyCafeId)
        }
    }

    // =====================================================
    // Public Methods - 즐겨찾기
    // =====================================================

    fun addFavoriteCafe(cafeId: Long) {
        viewModelScope.launch(ioDispatcher) {
            try {
                when (val result = addFavoriteCafeUseCase(cafeId)) {
                    is ApiResult.Success -> {
                        val newFavorites = _favoriteCafeIds.value.toMutableList()
                        if (!newFavorites.contains(cafeId)) {
                            newFavorites.add(cafeId)
                            _favoriteCafeIds.value = newFavorites
                        }
                        _events.tryEmit("즐겨찾기에 추가되었습니다")
                    }
                    is ApiResult.Failure -> {
                        _events.tryEmit(result.message ?: "즐겨찾기 추가 실패")
                    }
                }
            } catch (e: Exception) {
                _events.tryEmit(e.message ?: "알 수 없는 오류")
            }
        }
    }

    fun removeFavoriteCafe(cafeId: Long) {
        viewModelScope.launch(ioDispatcher) {
            try {
                when (val result = removeFavoriteCafeUseCase(cafeId)) {
                    is ApiResult.Success -> {
                        val newFavorites = _favoriteCafeIds.value.toMutableList()
                        newFavorites.remove(cafeId)
                        _favoriteCafeIds.value = newFavorites
                        _events.tryEmit("즐겨찾기에서 제거되었습니다")
                    }
                    is ApiResult.Failure -> {
                        _events.tryEmit(result.message ?: "즐겨찾기 제거 실패")
                    }
                }
            } catch (e: Exception) {
                _events.tryEmit(e.message ?: "알 수 없는 오류")
            }
        }
    }


    /**
     * 서버 이미지 로드 (ByteArray)
     */
    private suspend fun loadImage(imageId: String) {
        if (_imageBitmapCache.value.containsKey(imageId)) return

        synchronized(loadingImageIds) {
            if (loadingImageIds.contains(imageId)) return
            loadingImageIds.add(imageId)
        }

        try {
            when (val result = getImageUseCase(imageId)) {
                is ApiResult.Success -> {
                    result.data?.let { imageData ->
                        val bitmap = decodeSampledBitmap(imageData, 200, 200)
                        bitmap?.let {
                            _imageBitmapCache.update { cache -> cache + (imageId to it) }
                        }
                    }
                }
                is ApiResult.Failure -> {

                }
            }
        } catch (e: Exception) {

        } finally {
            synchronized(loadingImageIds) {
                loadingImageIds.remove(imageId)
            }
        }
    }

    /**
     * 샘플링하여 Bitmap 디코딩
     */
    private fun decodeSampledBitmap(
        data: ByteArray,
        reqWidth: Int,
        reqHeight: Int
    ): Bitmap? {
        return try {
            // 먼저 이미지 크기만 확인
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
                BitmapFactory.decodeByteArray(data, 0, data.size, this)

                // 샘플링 비율 계산
                inSampleSize = calculateInSampleSize(this, reqWidth, reqHeight)
                inJustDecodeBounds = false
            }

            // 실제 디코딩 (이 부분이 문제였습니다)
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
        // 웹소켓 연결은 전역(로그아웃 시)으로 관리되므로, 여기서는 끊지 않고 유지합니다.
        // 다만 해당 화면에 특화된 구독이 있다면 해제(unsubscribe)만 수행합니다.
        // timePassSubscriptionId는 전역 알림을 위해 유지합니다.
    }
}
