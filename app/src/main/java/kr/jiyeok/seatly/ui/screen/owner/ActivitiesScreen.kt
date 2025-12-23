package kr.jiyeok.seatly.ui.screen.owner

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import kr.jiyeok.seatly.ui.component.MaterialSymbol

/**
 * Full recent activities screen — reads from ActivitiesRepo.activities (shared state).
 * Displays all activities and provides a back button.
 */
@Composable
fun ActivitiesScreen(navController: NavController) {
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
                IconButton(onClick = { navController.popBackStack() }) {
                    MaterialSymbol(name = "arrow_back", size = 24.sp)
                }
                Text("최근 활동", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(start = 8.dp))
            }

            Spacer(modifier = Modifier.height(8.dp))

            val activities = ActivitiesRepo.activities

            if (activities.isEmpty()) {
                Text("활동 내역이 없습니다.", modifier = Modifier.padding(16.dp))
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxSize()) {
                    items(activities) { item ->
                        ActivityRow(text = item.text, time = item.time)
                    }
                }
            }
        }
    }
}