package kr.jiyeok.seatly.ui.screen.owner

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import java.util.UUID

data class ActivityItem(
    val id: String = UUID.randomUUID().toString(),
    val text: String,
    val time: String
)

/**
 * Shared repository for recent activities so different screens can read/write a single source of truth.
 * Using a SnapshotStateList ensures Compose recomposes consumers automatically.
 */
object ActivitiesRepo {
    val activities: SnapshotStateList<ActivityItem> = mutableStateListOf()
}