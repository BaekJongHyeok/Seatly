package kr.jiyeok.seatly.ui.screen.admin.cafe

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kr.jiyeok.seatly.ui.component.MaterialSymbol
import kr.jiyeok.seatly.ui.component.admin.AdminBottomNavigationBar

/**
 * StudyCafeListScreen (updated)
 *
 * - Reads cafes from a repository abstraction (StudyCafeRepository).
 * - Default implementation uses InMemoryCafeStore-backed MockStudyCafeRepository so that
 *   cafes registered via RegisterCafeScreen2 (which writes to the same InMemoryCafeStore)
 *   will appear here immediately.
 * - When a real backend is available, provide a real StudyCafeRepository implementation to this screen.
 *
 * - Clicking a cafe item now navigates to the detail screen route "cafe_detail/{cafeId}".
 */

private val Primary = Color(0xFFe95321)
private val BackgroundBase = Color(0xFFFFFFFF)
private val InputBg = Color(0xFFF8F8F8)
private val BorderColor = Color(0xFFE5E5E5)
private val TextMain = Color(0xFF1A1A1A)
private val TextSub = Color(0xFF888888)
private val EmptyIconColor = Color(0xFFCFD6DB)
private val ErrorColor = Color(0xFFEF4444)
private val CardCorner = 12.dp

private enum class CafeStatus { OPEN, REVIEW, REJECT }

private data class Cafe(
    val id: String,
    val title: String,
    val address: String,
    val imageUrl: String,
    val status: CafeStatus
)

/** Repository abstraction to fetch list of study cafes */
interface StudyCafeRepository {
    suspend fun getCafes(): List<StudyCafeListItem>
}

/** Mock repository that reads from the shared InMemoryCafeStore */
class MockStudyCafeRepository : StudyCafeRepository {
    override suspend fun getCafes(): List<StudyCafeListItem> {
        // simulate network delay
        delay(300)
        return InMemoryCafeStore.getAll()
    }
}

/**
 * StudyCafeListScreen now accepts a StudyCafeRepository (default: mock).
 * If you want registrations to appear live, ensure the same InMemoryCafeStore is used by the
 * registration repository (RegisterCafeScreen2 uses MockCafeRepository which writes to the same store).
 */
@Composable
fun StudyCafeListScreen(
    navController: NavController,
    modifier: Modifier = Modifier,
    repository: StudyCafeRepository = MockStudyCafeRepository()
) {
    var cafes by remember { mutableStateOf<List<StudyCafeListItem>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        loading = true
        error = null
        try {
            cafes = repository.getCafes()
        } catch (t: Throwable) {
            error = "데이터를 불러올 수 없습니다."
        } finally {
            loading = false
        }
    }

    val listScrollState = rememberScrollState()

    Surface(modifier = modifier.fillMaxSize(), color = BackgroundBase) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(BackgroundBase)
                    .padding(top = 20.dp, bottom = 8.dp, start = 16.dp, end = 16.dp)
            ) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "등록 카페 목록",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextMain,
                        modifier = Modifier.align(Alignment.Center)
                    )

                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .clickable { navController.navigate("register_cafe_1") }
                            .padding(4.dp)
                    ) {
                        MaterialSymbol(name = "add", size = 20.sp, tint = Primary)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // thin divider under header
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(Color(0xFFF3F3F3))
                )
            }

            // Middle area - fills remaining space between header and bottom nav
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                when {
                    loading -> {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            // simple loading state
                            MaterialSymbol(name = "hourglass_top", size = 48.sp, tint = TextSub)
                        }
                    }
                    error != null -> {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(text = error ?: "", color = ErrorColor)
                                Spacer(modifier = Modifier.height(12.dp))
                                androidx.compose.material3.Button(onClick = {
                                    scope.launch {
                                        loading = true
                                        error = null
                                        try {
                                            cafes = repository.getCafes()
                                        } catch (t: Throwable) {
                                            error = "재시도 중 오류"
                                        } finally {
                                            loading = false
                                        }
                                    }
                                }) {
                                    Text("재시도")
                                }
                            }
                        }
                    }
                    cafes.isEmpty() -> {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                                modifier = Modifier.padding(horizontal = 24.dp)
                            ) {
                                MaterialSymbol(
                                    name = "storefront",
                                    size = 100.sp,
                                    tint = EmptyIconColor
                                )

                                Spacer(modifier = Modifier.height(28.dp))

                                Text(
                                    text = "등록된 스터디카페가 없습니다.",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = TextMain
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                Text(
                                    text = "새로운 카페를 등록해주세요",
                                    fontSize = 13.sp,
                                    color = TextSub
                                )
                            }
                        }
                    }
                    else -> {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(listScrollState)
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            cafes.forEach { cafe ->
                                Surface(
                                    shape = RoundedCornerShape(CardCorner),
                                    color = InputBg,
                                    border = BorderStroke(1.dp, BorderColor),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            // navigate to detail screen with cafe id
                                            navController.navigate("cafe_detail/${cafe.id}")
                                        }
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        // thumbnail
                                        Box(
                                            modifier = Modifier
                                                .size(72.dp)
                                                .clip(RoundedCornerShape(10.dp))
                                        ) {
                                            AsyncImage(
                                                model = cafe.imageUrl ?: "",
                                                contentDescription = "Cafe Thumbnail",
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(72.dp)
                                                    .clip(RoundedCornerShape(10.dp))
                                                    .background(Color(0xFFEEEEEE))
                                            )
                                        }

                                        Spacer(modifier = Modifier.width(16.dp))

                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = cafe.title,
                                                fontSize = 16.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = TextMain
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = cafe.address,
                                                fontSize = 12.sp,
                                                color = TextSub
                                            )

                                            Spacer(modifier = Modifier.height(6.dp))

                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                val (dotColor, statusText, statusColor) = when (cafe.status) {
                                                    "OPEN" -> Triple(Color(0xFF22C55E), "운영 중", Color(0xFF22C55E))
                                                    "REVIEW" -> Triple(Primary, "심사 중", Primary)
                                                    "REJECT" -> Triple(ErrorColor, "등록 거부", ErrorColor)
                                                    else -> Triple(TextSub, "알 수 없음", TextSub)
                                                }
                                                Box(
                                                    modifier = Modifier
                                                        .size(8.dp)
                                                        .clip(RoundedCornerShape(4.dp))
                                                        .background(dotColor)
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(text = statusText, fontSize = 12.sp, color = statusColor)
                                            }
                                        }

                                        if (cafe.status == "REJECT") {
                                            OutlinedButton(
                                                onClick = { /* 확인 action */ },
                                                border = BorderStroke(1.dp, Primary.copy(alpha = 0.2f)),
                                                colors = ButtonDefaults.outlinedButtonColors(
                                                    containerColor = Color.White,
                                                    contentColor = Primary
                                                ),
                                                shape = RoundedCornerShape(8.dp),
                                                modifier = Modifier
                                                    .widthIn(min = 88.dp)
                                                    .height(36.dp)
                                            ) {
                                                Text(text = "확인", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = Primary)
                                            }
                                        } else {
                                            Box(modifier = Modifier.padding(start = 6.dp)) {
                                                MaterialSymbol(name = "chevron_right", size = 20.sp, tint = TextMain)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Bottom navigation placed as the last child of Column so it's fixed at bottom
            AdminBottomNavigationBar(
                currentRoute = "cafe_list",
                onNavigate = { route -> navController.navigate(route) { launchSingleTop = true } }
            )
        }
    }
}