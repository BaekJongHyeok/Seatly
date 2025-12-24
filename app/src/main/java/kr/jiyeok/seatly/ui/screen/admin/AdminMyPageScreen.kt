package kr.jiyeok.seatly.ui.screen.admin

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.ReportProblem
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import coil.compose.AsyncImage
import kr.jiyeok.seatly.ui.component.admin.AdminBottomNavigationBar

/**
 * Admin "My Page" screen.
 *
 * - Added stroke (border) to each card view.
 * - The account management card was removed; only a standalone logout button remains.
 * - Logout button uses red background and white text.
 * - Icons used in "관리 및 설정" are colored with gray (UiColors.TextSecondary).
 * - Card elevation remains 0 (no shadow).
 */

private object UiColors {
    val Primary = Color(0xFFE95220)
    val BackgroundLight = Color(0xFFFFFFFF)
    val SurfaceLight = Color(0xFFF8F8F8)
    val TextMain = Color(0xFF1A1A1A)
    val TextSecondary = Color(0xFF888888)
    val BorderSubtle = Color(0xFFEBEBEB)
    val GreenDot = Color(0xFF10B981)
}

@Composable
fun AdminMyPageScreen(
    navController: NavHostController?,
    onAddCafe: () -> Unit = { navController?.navigate("add_cafe") },
    onLogout: () -> Unit = { /* implement as needed */ },
    onLeaveAccount: () -> Unit = { /* implement as needed */ }
) {
    val context = LocalContext.current
    val scroll = rememberScrollState()
    val notifyEnabled = remember { mutableStateOf(true) }

    // determine current route (safe when navController == null)
    val currentRoute = if (navController != null) {
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        navBackStackEntry?.destination?.route
    } else {
        null
    }

    Surface(
        color = UiColors.BackgroundLight,
        modifier = Modifier.fillMaxSize()
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Content (scrollable)
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scroll)
                    .padding(bottom = 90.dp) // leave room for bottom bar
            ) {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(UiColors.BackgroundLight)
                        .padding(horizontal = 8.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Start
                ) {
                    IconButton(onClick = { navController?.popBackStack() }) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "back", tint = UiColors.TextMain)
                    }

                    // center title
                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        Text(text = "계정 정보", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = UiColors.TextMain)
                    }

                    IconButton(onClick = { navController?.navigate("edit_profile"); Toast.makeText(context, "프로필 편집 (미구현)", Toast.LENGTH_SHORT).show() }) {
                        Icon(imageVector = Icons.Default.Edit, contentDescription = "edit", tint = UiColors.TextMain)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Profile card (no shadow) + border (stroke)
                Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .wrapContentHeight()
                            .border(1.dp, UiColors.BorderSubtle, RoundedCornerShape(12.dp)),
                        colors = CardDefaults.cardColors(containerColor = UiColors.SurfaceLight),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp) // no shadow
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(60.dp)
                                    .clip(CircleShape)
                                    .background(UiColors.Primary),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(text = "이", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(text = "이준영", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = UiColors.TextMain)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Box(
                                        modifier = Modifier
                                            .background(UiColors.Primary.copy(alpha = 0.12f), RoundedCornerShape(8.dp))
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text(text = "OWNER", fontSize = 11.sp, fontWeight = FontWeight.Medium, color = UiColors.Primary)
                                    }
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(text = "카페 사장님", fontSize = 12.sp, color = UiColors.TextSecondary)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(text = "사업자 번호: 123-45-67890", fontSize = 11.sp, color = UiColors.TextSecondary)
                            }

                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = "open",
                                tint = UiColors.TextMain.copy(alpha = 0.4f)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Cafes card (no shadow) + border
                Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, UiColors.BorderSubtle, RoundedCornerShape(12.dp)),
                        colors = CardDefaults.cardColors(containerColor = UiColors.SurfaceLight),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp) // no shadow
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(text = "운영 중인 카페", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = UiColors.TextMain)
                            Spacer(modifier = Modifier.height(12.dp))

                            CafeListItem(
                                imageUrl = "https://lh3.googleusercontent.com/aida-public/AB6AXuDRF3yBo11294crsqk_39h7H7r_EwyZIsVZKHjLjwjbuKI3CTDsE1o8lb1OnFc4z7FAeqCw_cYbhx0N8ZOoetJhkEb0BTVZ9A5VGL7sTua032QZqNYjz6reWQFxe_xXWL6Rje0WcmauxDi2lUHb-kjkLUdXcTJu_j7nupRuwnkEsTiy3Fy8-Zjkxc3O87p7ZWS78ayqB6RyFvFLiB4aWQnmHGDuhDBiHub-pXnrNff_p9zqvRJT_FTDXc-LFGgQQGs3PLk7YTIlgUE",
                                title = "명지 스터디카페",
                                statusText = "운영 중",
                                onClick = { navController?.navigate("cafe/myeongji") }
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            CafeListItem(
                                imageUrl = "https://lh3.googleusercontent.com/aida-public/AB6AXuBYTDjEeYpaEkjdBWzcrURXnFEhWQHf8SSkDnfHaf2GPwrEn6mAJyNF6_-3xnLF4iN_w2nzXn5w5Vk-38mp_lk7prgjqXSgJl4vkvkLVF8e6VK-WqKuBGig5uslPe72RAZ72EGEhbPJG47wO308LfmBPnMUzN5_pfqtsK5XXGzFmCvjtS8M7G_PEh9BpObYqhqaJNujM_ExHtOsRMEAQYfRMOIwRzs50DlDg_j5Zp40fp3KBxWTvWeqOnSoir3vZQow_qAMz47phP0",
                                title = "강남 2호점",
                                statusText = "운영 중",
                                onClick = { navController?.navigate("cafe/gangnam2") }
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                                Text(
                                    text = "+ 카페 추가하기",
                                    color = UiColors.Primary,
                                    fontSize = 13.sp,
                                    modifier = Modifier
                                        .clickable {
                                            onAddCafe()
                                            Toast.makeText(context, "카페 추가 (미구현)", Toast.LENGTH_SHORT).show()
                                        }
                                        .padding(vertical = 6.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(22.dp))

                // Settings
                Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                    Text(text = "관리 및 설정", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = UiColors.TextMain)
                    Spacer(modifier = Modifier.height(12.dp))

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(UiColors.BackgroundLight)
                    ) {
                        SettingRow(icon = Icons.Default.Person, title = "가입 정보 수정", onClick = { navController?.navigate("edit_profile") })
                        Divider(color = UiColors.BorderSubtle, thickness = 1.dp)
                        SettingRow(icon = Icons.Default.Lock, title = "비밀번호 변경", onClick = { navController?.navigate("change_password") })
                        Divider(color = UiColors.BorderSubtle, thickness = 1.dp)

                        // notification row with switch (icon colored gray)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                                .background(Color.White)
                                .padding(horizontal = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Icon(imageVector = Icons.Default.Notifications, contentDescription = "notify", tint = UiColors.TextSecondary)
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(text = "알림 설정", fontSize = 14.sp, color = UiColors.TextMain)
                            }

                            Switch(
                                checked = notifyEnabled.value,
                                onCheckedChange = { notifyEnabled.value = it },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = UiColors.Primary,
                                    checkedTrackColor = UiColors.Primary.copy(alpha = 0.2f)
                                )
                            )
                        }

                        Divider(color = UiColors.BorderSubtle, thickness = 1.dp)
                        SettingRow(icon = Icons.Default.ReportProblem, title = "고객 지원", onClick = { navController?.navigate("support") })
                        Divider(color = UiColors.BorderSubtle, thickness = 1.dp)
                        SettingRow(icon = Icons.Default.PrivacyTip, title = "약관 및 정책", onClick = { navController?.navigate("policies") })
                    }
                }

                Spacer(modifier = Modifier.height(22.dp))

                // Only a standalone Logout button (no surrounding card)
                Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Button(
                        onClick = {
                            onLogout()
                            Toast.makeText(context, "로그아웃 (미구현)", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = UiColors.Primary, contentColor = Color.White)
                    ) {
                        Text(text = "로그아웃", fontSize = 16.sp, fontWeight = FontWeight.Medium)
                    }
                }

                Spacer(modifier = Modifier.height(120.dp))
            }

            // Bottom navigation anchored to bottom center
            Box(modifier = Modifier.align(Alignment.BottomCenter)) {
                AdminBottomNavigationBar(
                    currentRoute = currentRoute,
                    onNavigate = { route ->
                        // perform navigation using the provided NavHostController
                        navController?.let { nc ->
                            nc.navigate(route) {
                                launchSingleTop = true
                                restoreState = true
                                popUpTo(nc.graph.startDestinationId) { saveState = true }
                            }
                        }
                    }
                )
            }
        }
    }
}

/* ----------------- Small reusable pieces ----------------- */

@Composable
private fun CafeListItem(
    imageUrl: String,
    title: String,
    statusText: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .background(Color.White, shape = RoundedCornerShape(10.dp))
            .border(width = 1.dp, color = Color.Transparent, shape = RoundedCornerShape(10.dp))
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = imageUrl,
            contentDescription = title,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(8.dp))
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = UiColors.TextMain)
            Spacer(modifier = Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                androidx.compose.foundation.layout.Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(UiColors.GreenDot)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = statusText, fontSize = 12.sp, color = UiColors.TextSecondary)
            }
        }

        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = "open",
            tint = UiColors.TextMain.copy(alpha = 0.4f)
        )
    }
}

@Composable
private fun SettingRow(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .background(Color.White)
            .clickable { onClick() }
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // icon color changed to gray (TextSecondary)
        Icon(imageVector = icon, contentDescription = title, tint = UiColors.TextSecondary)
        Spacer(modifier = Modifier.width(12.dp))
        Text(text = title, fontSize = 14.sp, color = UiColors.TextMain)
        Spacer(modifier = Modifier.weight(1f))
        Icon(imageVector = Icons.Default.ChevronRight, contentDescription = "go", tint = UiColors.TextMain.copy(alpha = 0.4f))
    }
}