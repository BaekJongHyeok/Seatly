package kr.jiyeok.seatly.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Divider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kr.jiyeok.seatly.ui.component.MaterialSymbol

private val Primary = Color(0xFFe95321)
private val Unselected = Color(0xFFA0A0A0)
private val Background = Color(0xFFFFFFFF)

/**
 * Owner bottom navigation reworked for balanced horizontal spacing and no clipped labels.
 *
 * - Uses three equally weighted items (대시보드 / 카페 관리 / 마이페이지)
 * - Row height increased to avoid label clipping and provide comfortable touch targets
 * - Each item uses Modifier.weight(1f) so left/right spacing is symmetric and natural
 * - Top divider retained to match the design
 */
@Composable
fun OwnerBottomNavigationBar(
    currentRoute: String?,
    onNavigate: (String) -> Unit
) {
    Surface(color = Background, modifier = Modifier.fillMaxWidth()) {
        Divider(color = Color(0xFFF0F0F0), thickness = 1.dp)

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(72.dp) // increased to avoid text clipping and to match design spacing
                .background(Background),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Dashboard
            val dashSelected = currentRoute == "dashboard" || currentRoute == "home"
            val dashTint = if (dashSelected) Primary else Unselected
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onNavigate("dashboard") }
                    .padding(top = 6.dp, bottom = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                MaterialSymbol(name = "home", size = 24.sp, tint = dashTint)
                Text("대시보드", fontSize = 11.sp, color = dashTint, modifier = Modifier.padding(top = 4.dp))
            }

            // Cafe management (center)
            val cafeSelected = currentRoute == "cafe_list" || currentRoute == "cafe_management"
            val cafeTint = if (cafeSelected) Primary else Unselected
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onNavigate("cafe_list") }
                    .padding(top = 6.dp, bottom = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                MaterialSymbol(name = "storefront", size = 24.sp, tint = cafeTint)
                Text("카페 관리", fontSize = 11.sp, color = cafeTint, modifier = Modifier.padding(top = 4.dp))
            }

            // My page
            val mySelected = currentRoute == "mypage" || currentRoute == "profile"
            val myTint = if (mySelected) Primary else Unselected
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onNavigate("mypage") }
                    .padding(top = 6.dp, bottom = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                MaterialSymbol(name = "person", size = 24.sp, tint = myTint)
                Text("마이페이지", fontSize = 11.sp, color = myTint, modifier = Modifier.padding(top = 4.dp))
            }
        }
    }
}