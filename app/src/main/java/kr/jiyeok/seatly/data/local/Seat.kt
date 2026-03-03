package kr.jiyeok.seatly.data.local

import androidx.compose.runtime.MutableState
import androidx.compose.ui.geometry.Offset
import kr.jiyeok.seatly.data.remote.enums.ESeatStatus

data class Seat(
    val id: String,
    var label: String,
    val type: SeatType,
    val pos: MutableState<Offset>,
    val size: MutableState<Offset>,
    var rotation: Float = 0f,
    var availabilityStatus: ESeatStatus = ESeatStatus.AVAILABLE
)