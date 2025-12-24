package kr.jiyeok.seatly.ui.screen.manager

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
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
import kr.jiyeok.seatly.ui.component.MaterialSymbol
import kr.jiyeok.seatly.ui.components.OwnerBottomNavigationBar

/**
 * StudyCafeListScreen.kt
 *
 * Fixed compilation error by removing usage of Modifier.align outside appropriate scope.
 * Layout:
 *  - Column: header, middle (weight 1f), bottom navigation (OwnerBottomNavigationBar)
 *  - Middle uses Box with contentAlignment = Alignment.Center to vertically center empty-state.
 *
 * Empty-state: when cafes.isEmpty() the icon + messages are centered vertically between header and bottom nav.
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

@Composable
fun StudyCafeListScreen(
    navController: NavController,
    modifier: Modifier = Modifier
) {
    // Replace with real data from ViewModel; empty list demonstrates empty-state UI
    val cafes = remember { emptyList<Cafe>() }

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
                if (cafes.isEmpty()) {
                    // Center the empty-state vertically and horizontally within this Box
                    Box(
                        modifier = Modifier
                            .fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier.padding(horizontal = 24.dp)
                        ) {
                            // Large light storefront icon
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
                } else {
                    // Non-empty list
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(listScrollState)
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        cafes.forEach { cafe ->
                            androidx.compose.material3.Surface(
                                shape = RoundedCornerShape(CardCorner),
                                color = InputBg,
                                border = BorderStroke(1.dp, BorderColor),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { /* open cafe detail */ }
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
                                            model = cafe.imageUrl,
                                            contentDescription = "Cafe Thumbnail",
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(72.dp)
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(Color(0xFFEEEEEE))
                                        )
                                    }

                                    // horizontal gap between image and info
                                    Spacer(modifier = Modifier.size(16.dp))

                                    // Title / Address / Status
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

                                        // Status row
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            val (dotColor, statusText, statusColor) = when (cafe.status) {
                                                CafeStatus.OPEN -> Triple(Color(0xFF22C55E), "운영 중", Color(0xFF22C55E))
                                                CafeStatus.REVIEW -> Triple(Primary, "심사 중", Primary)
                                                CafeStatus.REJECT -> Triple(ErrorColor, "등록 거부", ErrorColor)
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

                                    // Right action: chevron or "확인" button
                                    if (cafe.status == CafeStatus.REJECT) {
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

            // Bottom navigation placed as the last child of Column so it's fixed at bottom
            OwnerBottomNavigationBar(
                currentRoute = "cafe_list",
                onNavigate = { route ->
                    navController.navigate(route) {
                        launchSingleTop = true
                    }
                }
            )
        }
    }
}