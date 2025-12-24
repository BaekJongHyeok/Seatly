package kr.jiyeok.seatly.ui.screen.manager

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import kr.jiyeok.seatly.ui.component.MaterialSymbol
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.layout.padding

/**
 * RegisterCafeScreen2.kt
 *
 * Precise spacing adjustments so that:
 * - gap between step pill and heading
 * - gap between heading and first label ("주소")
 * - horizontal inset (20.dp) and label/field alignment
 *
 * now exactly mirror RegisterCafeScreen1:
 *  - body top spacing = 18.dp (same)
 *  - step pill then Spacer(12.dp) then heading with Modifier.padding(bottom = 18.dp)
 *    (this mirrors Screen1 where heading had bottom padding)
 *  - label followed by Spacer(8.dp) then AppTextField (same as Screen1)
 */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterCafeScreen2(
    navController: NavController,
    modifier: Modifier = Modifier
) {
    // Colors + tokens matching design
    val Primary = Color(0xFFe95321)
    val BackgroundBase = Color(0xFFFFFFFF)
    val InputBg = Color(0xFFF8F8F8)
    val BorderColor = Color(0xFFE8E8E8)
    val TextMain = Color(0xFF1A1A1A)
    val TextSub = Color(0xFF888888)
    val StepBg = Color(0xFFfdede8)

    // Form state
    var postalCode by remember { mutableStateOf("") }
    var addressSelected by remember { mutableStateOf("서울시 강서구 화곡로 123") } // sample
    var detailAddress by remember { mutableStateOf("") }

    // facility list (label to icon name)
    val facilityList = listOf(
        "와이파이" to "wifi",
        "콘센트" to "power",
        "프린트" to "print",
        "음료" to "local_cafe",
        "미팅룸" to "meeting_room",
        "사물함" to "lock",
        "CCTV" to "videocam",
        "에어컨" to "ac_unit"
    )
    var selectedFacilities by remember { mutableStateOf(setOf("와이파이")) }

    // Reusable AppTextField used in this screen (compact, 52dp height)
    @Composable
    fun AppTextField(
        value: String,
        onValueChange: (String) -> Unit,
        placeholder: String = "",
        readOnly: Boolean = false,
        leading: (@Composable () -> Unit)? = null,
        trailing: (@Composable () -> Unit)? = null,
        keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
        highlight: Boolean = false
    ) {
        var focused by remember { mutableStateOf(false) }
        val borderCol = if (focused || highlight) Primary else BorderColor

        val base = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(InputBg)
            .border(BorderStroke(1.dp, borderCol), RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp)

        if (readOnly && trailing == null) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = base) {
                if (leading != null) {
                    androidx.compose.foundation.layout.Box(modifier = Modifier.padding(end = 8.dp)) { leading() }
                }
                Text(text = value.ifEmpty { placeholder }, color = if (value.isEmpty()) TextSub else TextMain, fontSize = 15.sp)
            }
        } else {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = base) {
                if (leading != null) {
                    androidx.compose.foundation.layout.Box(modifier = Modifier.padding(end = 8.dp)) { leading() }
                }
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    singleLine = true,
                    textStyle = TextStyle(color = TextMain, fontSize = 15.sp),
                    cursorBrush = SolidColor(Primary),
                    keyboardOptions = keyboardOptions,
                    modifier = Modifier
                        .weight(1f)
                        .onFocusChanged { focused = it.isFocused }
                ) { inner ->
                    if (value.isEmpty()) Text(text = placeholder, color = TextSub, fontSize = 15.sp)
                    inner()
                }
                if (trailing != null) {
                    androidx.compose.foundation.layout.Box(modifier = Modifier.padding(start = 8.dp)) { trailing() }
                }
            }
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = BackgroundBase,
        bottomBar = {
            Surface(tonalElevation = 4.dp, color = BackgroundBase) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = { navController.popBackStack() },
                        border = BorderStroke(1.dp, BorderColor),
                        colors = ButtonDefaults.outlinedButtonColors(containerColor = Color(0xFFF8F8F8), contentColor = TextMain),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(54.dp)
                    ) {
                        Text("이전", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                    }

                    Button(
                        onClick = { /* register complete */ },
                        colors = ButtonDefaults.buttonColors(containerColor = Primary, contentColor = Color.White),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(54.dp)
                    ) {
                        Text("등록 완료", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    ) { innerPadding ->
        // Place header wrapper outside LazyColumn so it can be full-width (no side padding),
        // and make the body (LazyColumn) use the same horizontal inset as Screen1 (20.dp).
        Column(modifier = Modifier.fillMaxSize()) {
            // Header wrapper identical to Screen1
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(BackgroundBase)
                    .padding(top = 20.dp, bottom = 6.dp)
            ) {
                // TopBar with same visual params as Screen1
                RegisterCafeTopBar(
                    navController = navController,
                    title = "카페 등록",
                    titleFontSize = 20.sp,
                    titleColor = TextMain,
                    onBack = { navController.popBackStack() },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                // thin divider
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(Color(0xFFF4F4F4))
                )
            }

            // Body - uses LazyColumn with horizontal padding = 20.dp to match Screen1 body inset
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 20.dp),
                contentPadding = PaddingValues(bottom = 20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    // Make top spacing identical to Screen1: body top padding = 18.dp
                    Spacer(modifier = Modifier.height(18.dp))

                    // Step pill
                    Box {
                        Text(
                            text = "Step 2/2",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Primary,
                            modifier = Modifier
                                .background(StepBg, RoundedCornerShape(12.dp))
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Heading: use the same modifier.padding(bottom = 18.dp) as in Screen1
                    Text(
                        text = "위치 및 시설 정보를 확인해주세요",
                        fontSize = 22.sp, // match Screen1's main heading size
                        fontWeight = FontWeight.Bold,
                        color = TextMain,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }

                item {
                    // Address label - style and spacing matched to Screen1's "카페명" label
                    Text("주소", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TextMain)
                    Spacer(modifier = Modifier.height(8.dp))

                    // Address row: read-only field + 검색 button
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.weight(1f)) {
                            AppTextField(
                                value = addressSelected,
                                onValueChange = { addressSelected = it },
                                placeholder = "도로명, 건물명 등을 입력하세요",
                                readOnly = true
                            )
                        }

                        Button(
                            onClick = { /* search action */ },
                            colors = ButtonDefaults.buttonColors(containerColor = Primary, contentColor = Color.White),
                            modifier = Modifier
                                .height(52.dp)
                                .widthIn(min = 92.dp)
                                .clip(RoundedCornerShape(12.dp))
                        ) {
                            MaterialSymbol(name = "search", size = 18.sp, tint = Color.White)
                            Spacer(modifier = Modifier.size(6.dp))
                            Text("검색", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Detail address input - matches AppTextField usage in Screen1
                    AppTextField(
                        value = detailAddress,
                        onValueChange = { detailAddress = it },
                        placeholder = "상세 주소를 입력해주세요 (예: 2층)"
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Map placeholder
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFEAEAEA)),
                        contentAlignment = Alignment.Center
                    ) {
                        MaterialSymbol(name = "map", size = 40.sp, tint = Color(0xFFBBBBBB))
                    }

                    Spacer(modifier = Modifier.height(20.dp))
                }

                item {
                    // Facilities header
                    Text("편의시설", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TextMain)
                }

                item {
                    // 4-column grid to match the design
                    val cols = 4
                    val itemHeight = 84.dp
                    val rowSpacing = 12.dp
                    val rowCount = (facilityList.size + cols - 1) / cols
                    val gridHeight = itemHeight * rowCount + rowSpacing * (if (rowCount > 0) (rowCount - 1) else 0)

                    LazyVerticalGrid(
                        columns = GridCells.Fixed(cols),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(gridHeight),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        content = {
                            gridItems(facilityList) { pair ->
                                val label = pair.first
                                val iconName = pair.second
                                val selected = selectedFacilities.contains(label)

                                Box(
                                    modifier = Modifier
                                        .height(itemHeight)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(if (selected) Color(0xFFFFF6F3) else Color.White)
                                        .border(BorderStroke(1.dp, if (selected) Primary else BorderColor), RoundedCornerShape(12.dp))
                                        .clickable { selectedFacilities = if (selected) selectedFacilities - label else selectedFacilities + label },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                                        MaterialSymbol(
                                            name = iconName,
                                            size = 24.sp,
                                            tint = if (selected) Primary else TextSub
                                        )
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = label,
                                            fontSize = 11.sp,
                                            fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal,
                                            color = if (selected) Primary else TextSub
                                        )
                                    }
                                }
                            }
                        }
                    )

                    Spacer(modifier = Modifier.height(36.dp))
                }
            }
        }
    }
}