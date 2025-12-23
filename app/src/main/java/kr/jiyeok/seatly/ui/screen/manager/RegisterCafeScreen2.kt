package kr.jiyeok.seatly.ui.screen.manager

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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

/**
 * RegisterCafeScreen2.kt
 *
 * Reduced vertical whitespace further as requested:
 * - LazyColumn contentPadding bottom reduced to 48.dp
 * - verticalArrangement spacing reduced to 8.dp
 * - Several Spacer heights lowered to make the layout denser
 * - Final spacer after facilities reduced to 8.dp
 *
 * Keeps previous behavior: AppTextField styling, no parking section, address validation & check icon.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterCafeScreen2(
    navController: NavController,
    modifier: Modifier = Modifier
) {
    // Palette
    val Primary = Color(0xFFe95321)
    val BackgroundBase = Color(0xFFFFFFFF)
    val InputBg = Color(0xFFF8F8F8)
    val BorderColor = Color(0xFFE5E5E5)
    val TextMain = Color(0xFF1A1A1A)
    val TextSub = Color(0xFF888888)

    // Form state
    var postalCode by remember { mutableStateOf("") }
    var addressSelected by remember { mutableStateOf("서울시 강서구 마곡로 100, 101동 201호") }
    var detailAddress by remember { mutableStateOf("") }

    // facilities set
    val facilityList = listOf(
        "WiFi",
        "콘센트",
        "음식 반입",
        "주차",
        "음료 무한제공",
        "휴게실",
        "화장실",
        "24시간 운영"
    )
    var selectedFacilities by remember { mutableStateOf(setOf("24시간 운영")) }

    // Simple address validation: checks for common road/building markers and digits.
    fun isValidRoadAddress(addr: String): Boolean {
        if (addr.isBlank()) return false
        val hasRoadMarker = Regex("로|길|번지|동|길가").containsMatchIn(addr)
        val hasNumber = Regex("\\d+").containsMatchIn(addr)
        return hasRoadMarker && hasNumber
    }

    // Reusable text field with trailing composable and highlight flag
    @Composable
    fun AppTextField(
        value: String,
        onValueChange: (String) -> Unit,
        placeholder: String = "",
        leading: (@Composable () -> Unit)? = null,
        trailing: (@Composable () -> Unit)? = null,
        keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
        isErrorBorder: Boolean = false,
        highlight: Boolean = false,
        readOnly: Boolean = false,
        onClick: (() -> Unit)? = null
    ) {
        var focused by remember { mutableStateOf(false) }

        val baseModifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(InputBg, RoundedCornerShape(10.dp))
            .border(
                BorderStroke(1.dp, if (focused || isErrorBorder || highlight) Primary else BorderColor),
                RoundedCornerShape(10.dp)
            )
            .padding(horizontal = 8.dp)

        if (readOnly && onClick != null) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = baseModifier.clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { onClick() }
            ) {
                if (leading != null) {
                    Box(modifier = Modifier.padding(start = 6.dp, end = 6.dp)) { leading() }
                }
                Box(modifier = Modifier.weight(1f)) {
                    if (value.isEmpty()) {
                        Text(text = placeholder, color = TextSub, fontSize = 16.sp)
                    } else {
                        Text(text = value, color = TextMain, fontSize = 16.sp)
                    }
                }
                if (trailing != null) {
                    Box(modifier = Modifier.padding(start = 8.dp)) { trailing() }
                }
            }
        } else {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = baseModifier) {
                if (leading != null) {
                    Box(modifier = Modifier.padding(start = 6.dp, end = 6.dp)) { leading() }
                }

                Box(modifier = Modifier.weight(1f)) {
                    BasicTextField(
                        value = value,
                        onValueChange = onValueChange,
                        singleLine = true,
                        textStyle = TextStyle(color = TextMain, fontSize = 16.sp),
                        cursorBrush = SolidColor(Primary),
                        keyboardOptions = keyboardOptions,
                        modifier = Modifier
                            .fillMaxWidth()
                            .onFocusChanged { state -> focused = state.isFocused }
                    ) { innerTextField ->
                        if (value.isEmpty()) {
                            Text(text = placeholder, color = TextSub, fontSize = 16.sp)
                        }
                        innerTextField()
                    }
                }

                if (trailing != null) {
                    Box(modifier = Modifier.padding(start = 8.dp)) { trailing() }
                }
            }
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = BackgroundBase,
        bottomBar = {
            Surface(
                tonalElevation = 4.dp,
                color = BackgroundBase
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = { navController.popBackStack() },
                        border = BorderStroke(1.dp, Color(0xFFCCCCCC)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TextMain),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(0.4f)
                            .height(52.dp)
                    ) {
                        Text("이전", fontSize = 16.sp)
                    }

                    Button(
                        onClick = { /* TODO: submit registration */ },
                        colors = ButtonDefaults.buttonColors(containerColor = Primary, contentColor = Color.White),
                        modifier = Modifier
                            .weight(0.6f)
                            .height(52.dp),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("등록 완료", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    ) { innerPadding ->
        // LazyColumn root with denser spacing
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(bottom = 48.dp), // reduced bottom padding
            verticalArrangement = Arrangement.spacedBy(8.dp) // tighter spacing
        ) {
            item {
                Spacer(modifier = Modifier.height(12.dp)) // reduced

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { navController.popBackStack() }) {
                        MaterialSymbol(name = "arrow_back", size = 24.sp)
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    Text(text = "카페 등록", fontSize = 20.sp, fontWeight = FontWeight.SemiBold, color = TextMain)

                    Spacer(modifier = Modifier.weight(1f))
                    Box(modifier = Modifier.size(36.dp))
                }

                Spacer(modifier = Modifier.height(8.dp)) // reduced

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    Text("2/2", color = TextSub, fontSize = 12.sp)
                }

                Spacer(modifier = Modifier.height(6.dp)) // reduced

                Box(modifier = Modifier.fillMaxWidth()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFF0F0F0))
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Primary)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp)) // reduced

                Text("주소 및 위치 입력", fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = TextMain)
                Spacer(modifier = Modifier.height(4.dp))
                Text("카페의 정확한 위치를 설정해주세요", fontSize = 12.sp, color = TextSub)

                Spacer(modifier = Modifier.height(12.dp)) // reduced

                Text("도로명 주소", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = TextMain)
                Spacer(modifier = Modifier.height(6.dp)) // reduced
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // postal code small box
                    Box(modifier = Modifier.width(96.dp)) {
                        AppTextField(
                            value = postalCode,
                            onValueChange = { postalCode = it.filter { ch -> ch.isDigit() } },
                            placeholder = "00000",
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                            isErrorBorder = false
                        )
                    }

                    Button(
                        onClick = { /* open address search */ },
                        colors = ButtonDefaults.buttonColors(containerColor = Primary, contentColor = Color.White),
                        modifier = Modifier
                            .height(56.dp)
                            .weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        MaterialSymbol(name = "search", size = 18.sp, tint = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("주소 검색", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp)) // reduced

                val addressValid = remember(addressSelected) { isValidRoadAddress(addressSelected) }

                // Address field: editable AppTextField with trailing check icon only when valid
                AppTextField(
                    value = addressSelected,
                    onValueChange = { addressSelected = it },
                    placeholder = "도로명, 건물명 등을 입력하세요",
                    trailing = if (addressValid) {
                        {
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Primary),
                                contentAlignment = Alignment.Center
                            ) {
                                MaterialSymbol(name = "check", size = 14.sp, tint = Color.White)
                            }
                        }
                    } else null,
                    highlight = addressValid
                )

                Spacer(modifier = Modifier.height(8.dp)) // reduced

                Text("상세 주소 (동/호수 등)", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = TextMain)
                Spacer(modifier = Modifier.height(6.dp))
                AppTextField(
                    value = detailAddress,
                    onValueChange = { detailAddress = it },
                    placeholder = "예: 101동 201호",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Done),
                    isErrorBorder = false
                )

                Spacer(modifier = Modifier.height(12.dp))
            }

            // Parking section removed per request

            item {
                Spacer(modifier = Modifier.height(6.dp)) // reduced
                Text("편의시설", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TextMain)
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Facilities grid
            item {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(240.dp), // slightly reduced height for denser layout
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    content = {
                        gridItems(facilityList) { itemText ->
                            val selected = selectedFacilities.contains(itemText)
                            FacilityItem(
                                text = itemText,
                                selected = selected,
                                primary = Primary,
                                onClick = {
                                    selectedFacilities = if (selected) selectedFacilities - itemText else selectedFacilities + itemText
                                }
                            )
                        }
                    }
                )
            }

            // Smaller space under facilities
            item {
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun FacilityItem(
    text: String,
    selected: Boolean,
    primary: Color,
    onClick: () -> Unit
) {
    val border = if (selected) BorderStroke(1.dp, primary) else BorderStroke(1.dp, Color(0xFFEDEDED))
    val bg = if (selected) Color(0xFFFFFFFF) else Color(0xFFF8F8F8)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp) // slightly shorter for denser grid
            .clip(RoundedCornerShape(10.dp))
            .background(bg)
            .border(border, RoundedCornerShape(10.dp))
            .clickable { onClick() }
            .padding(horizontal = 10.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(18.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (selected) primary else Color.White)
                    .border(BorderStroke(1.dp, if (selected) primary else Color(0xFFDDDDDD)), RoundedCornerShape(6.dp)),
                contentAlignment = Alignment.Center
            ) {
                if (selected) {
                    MaterialSymbol(name = "check", size = 12.sp, tint = Color.White)
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = text, fontSize = 13.sp, color = Color(0xFF222222))
        }
    }
}