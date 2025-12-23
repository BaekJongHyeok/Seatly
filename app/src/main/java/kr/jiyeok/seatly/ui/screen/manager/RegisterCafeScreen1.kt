package kr.jiyeok.seatly.ui.screen.manager

import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kr.jiyeok.seatly.ui.component.MaterialSymbol
import kr.jiyeok.seatly.ui.component.TimePicker

/**
 * RegisterCafeScreen1.kt
 *
 * Updated to call the single-column TimePicker component correctly.
 * When user clicks the start or end read-only field, we set which target is being picked
 * and open the TimePicker with that field's current value as the initial value.
 * On confirm, we assign the returned single time to the appropriate field.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterCafeScreen1(
    navController: NavController,
    modifier: Modifier = Modifier
) {
    // Theme colors
    val Primary = Color(0xFFe95321)
    val BackgroundBase = Color(0xFFFFFFFF)
    val InputBg = Color(0xFFF8F8F8)
    val BorderColor = Color(0xFFE5E5E5)
    val TextMain = Color(0xFF1A1A1A)
    val TextSub = Color(0xFF888888)

    // form state
    var cafeName by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }

    // time values (displayed)
    var startTime by remember { mutableStateOf("09:00") }
    var endTime by remember { mutableStateOf("24:00") }

    // whether to show the TimePicker sheet and which target we are picking ("start" or "end")
    var showTimePicker by remember { mutableStateOf(false) }
    var pickingTarget by remember { mutableStateOf("start") } // "start" or "end"

    // Move "없음" to the front and default-select it
    val weekdays = listOf("없음", "월요일", "화요일", "수요일", "목요일", "금요일", "토요일", "일요일")
    var selectedWeekdays by remember { mutableStateOf(setOf("없음")) }

    // images URIs (max 5)
    val images = remember { mutableStateListOf<Uri>() }

    val context = LocalContext.current
    val scrollState = rememberScrollState()

    // Gallery launcher for multiple selection
    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetMultipleContents()
    ) { uris: List<Uri> ->
        uris.forEach { uri ->
            if (images.size < 5) images.add(uri)
        }
    }

    // Phone formatting helper (Korean rules: '02' special, else 3-4-4)
    fun formatKoreanPhoneFromDigits(digits: String): String {
        if (digits.isEmpty()) return ""
        return if (digits.startsWith("02")) {
            when {
                digits.length <= 2 -> digits
                digits.length <= 5 -> digits.substring(0, 2) + "-" + digits.substring(2)
                digits.length <= 9 -> digits.substring(0, 2) + "-" + digits.substring(2, digits.length - 4) + "-" + digits.takeLast(4)
                else -> digits.substring(0, 2) + "-" + digits.substring(2, 6) + "-" + digits.substring(6, kotlin.math.min(10, digits.length))
            }
        } else {
            when {
                digits.length <= 3 -> digits
                digits.length <= 7 -> digits.substring(0, 3) + "-" + digits.substring(3)
                digits.length <= 11 -> digits.substring(0, 3) + "-" + digits.substring(3, 7) + "-" + digits.substring(7)
                else -> digits.substring(0, 3) + "-" + digits.substring(3, 7) + "-" + digits.substring(7, 11)
            }
        }
    }

    // Helper to decode Uri to ImageBitmap (safely on IO dispatcher)
    @Composable
    fun rememberBitmapForUri(uri: Uri): androidx.compose.ui.graphics.ImageBitmap? {
        var bitmap by remember(uri) { mutableStateOf<androidx.compose.ui.graphics.ImageBitmap?>(null) }
        LaunchedEffect(uri) {
            val bmp = try {
                withContext(Dispatchers.IO) {
                    context.contentResolver.openInputStream(uri)?.use { stream ->
                        BitmapFactory.decodeStream(stream)
                    }
                }
            } catch (e: Exception) {
                null
            }
            bitmap = bmp?.asImageBitmap()
        }
        return bitmap
    }

    // Reusable custom BasicTextField wrapper to control focus visuals exactly
    @Composable
    fun AppTextField(
        value: String,
        onValueChange: (String) -> Unit,
        placeholder: String,
        leading: (@Composable () -> Unit)? = null,
        keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
        isErrorBorder: Boolean = false,
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
                BorderStroke(1.dp, if (focused || isErrorBorder) Primary else BorderColor),
                RoundedCornerShape(10.dp)
            )
            .padding(horizontal = 8.dp)

        if (readOnly && onClick != null) {
            // Use a simple clickable layout for readOnly so clicks always reach it
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
            }
        } else {
            // Editable BasicTextField version
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
            }
        }
    }

    Surface(modifier = modifier.fillMaxSize(), color = BackgroundBase) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
            ) {
                // Header
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(BackgroundBase)
                        .padding(horizontal = 16.dp, vertical = 16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        IconButton(onClick = { navController.popBackStack() }, modifier = Modifier.size(36.dp)) {
                            MaterialSymbol(name = "arrow_back", size = 24.sp)
                        }

                        Text(
                            text = "카페 등록",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextMain
                        )

                        Spacer(modifier = Modifier.width(36.dp))
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Step text ABOVE progress bar
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        Text("1/2", color = TextSub, fontSize = 12.sp)
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // progress bar exactly 50%
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFF0F0F0))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(0.5f) // exactly 50%
                                .background(color = Primary, shape = RoundedCornerShape(12.dp))
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                    Text("기본 정보 입력", fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = TextMain)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("카페의 기본 정보를 입력해주세요", fontSize = 13.sp, color = TextSub)

                    Spacer(modifier = Modifier.height(20.dp))

                    // Cafe name
                    Text("카페명", fontSize = 13.sp, color = TextMain, fontWeight = FontWeight.Medium)
                    Spacer(modifier = Modifier.height(6.dp))
                    AppTextField(
                        value = cafeName,
                        onValueChange = { cafeName = it },
                        placeholder = "예: 명지 스터디카페",
                        leading = { MaterialSymbol(name = "domain", size = 18.sp, tint = TextMain) },
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Phone field
                    Text("대표 전화번호", fontSize = 13.sp, color = TextMain, fontWeight = FontWeight.Medium)
                    Spacer(modifier = Modifier.height(6.dp))
                    AppTextField(
                        value = phone,
                        onValueChange = { raw ->
                            val digits = raw.filter { it.isDigit() }
                            phone = formatKoreanPhoneFromDigits(digits)
                        },
                        placeholder = "010-1234-5678",
                        leading = { MaterialSymbol(name = "phone", size = 18.sp, tint = TextMain) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone, imeAction = ImeAction.Next),
                        isErrorBorder = false
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Business hours - open TimePicker when clicked
                    Text("영업 시간", fontSize = 13.sp, color = TextMain, fontWeight = FontWeight.Medium)
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        // Read-only field showing start time; clicking opens TimePicker for start
                        Box(modifier = Modifier.weight(1f)) {
                            AppTextField(
                                value = startTime,
                                onValueChange = {},
                                placeholder = startTime,
                                readOnly = true,
                                onClick = {
                                    pickingTarget = "start"
                                    showTimePicker = true
                                }
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Text("~", color = TextSub, fontSize = 16.sp)

                        Spacer(modifier = Modifier.width(12.dp))

                        // Read-only field showing end time; clicking opens TimePicker for end
                        Box(modifier = Modifier.weight(1f)) {
                            AppTextField(
                                value = endTime,
                                onValueChange = {},
                                placeholder = endTime,
                                readOnly = true,
                                onClick = {
                                    pickingTarget = "end"
                                    showTimePicker = true
                                }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // Weekday chips
                    Text("정기 휴무일", fontSize = 13.sp, color = TextMain, fontWeight = FontWeight.Medium)
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        weekdays.forEach { day ->
                            val selected = selectedWeekdays.contains(day)
                            Box(
                                modifier = Modifier
                                    .defaultMinSize(minHeight = 40.dp)
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(if (selected) Primary else InputBg, shape = RoundedCornerShape(20.dp))
                                    .border(BorderStroke(1.dp, if (selected) Primary else BorderColor), shape = RoundedCornerShape(20.dp))
                                    .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) {
                                        selectedWeekdays = if (day == "없음") {
                                            setOf("없음")
                                        } else {
                                            val mutable = selectedWeekdays.toMutableSet()
                                            if (mutable.contains("없음")) mutable.remove("없음")
                                            if (mutable.contains(day)) mutable.remove(day) else mutable.add(day)
                                            if (mutable.isEmpty()) mutable.add("없음")
                                            mutable.toSet()
                                        }
                                    }
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(text = day, color = if (selected) Color.White else TextMain, fontSize = 14.sp)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Photos section title + count
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("카페 사진 (최대 5장)", fontSize = 13.sp, color = TextMain, fontWeight = FontWeight.Medium)
                        Text("${images.size}/5", fontSize = 13.sp, color = TextSub)
                    }
                    Spacer(modifier = Modifier.height(12.dp))

                    // Thumbnails row - add tile opens gallery
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        item {
                            Box(
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .border(BorderStroke(1.dp, BorderColor), shape = RoundedCornerShape(10.dp))
                                    .background(InputBg)
                                    .clickable {
                                        galleryLauncher.launch("image/*")
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                MaterialSymbol(name = "add", size = 24.sp, tint = TextMain)
                            }
                        }

                        // show selected images as thumbnails with close icons
                        items(images) { uri ->
                            val bmp = rememberBitmapForUri(uri)
                            Box(
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color(0xFFF3E7DE)),
                                contentAlignment = Alignment.TopEnd
                            ) {
                                if (bmp != null) {
                                    Image(bitmap = bmp, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = androidx.compose.ui.layout.ContentScale.Crop)
                                }
                                IconButton(onClick = { images.remove(uri) }, modifier = Modifier.size(24.dp)) {
                                    MaterialSymbol(name = "close", size = 14.sp, tint = Color.White)
                                }
                            }
                        }
                    }

                    Spacer(modifier = androidx.compose.ui.Modifier.height(96.dp))
                }
            }

            // Bottom fixed bar
            Surface(
                tonalElevation = 4.dp,
                shadowElevation = 4.dp,
                color = BackgroundBase,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Make previous button wider and match corner radius of next button
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
                        onClick = { navController.navigate("register_cafe_2") },
                        colors = ButtonDefaults.buttonColors(containerColor = Primary, contentColor = Color.White),
                        modifier = Modifier
                            .weight(0.6f)
                            .height(52.dp),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("다음 (2/2)", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }

        // TimePicker sheet — open for whichever target was clicked
        TimePicker(
            visible = showTimePicker,
            initial = if (pickingTarget == "start") startTime else endTime,
            onDismiss = { showTimePicker = false },
            onConfirm = { selected ->
                if (pickingTarget == "start") startTime = selected else endTime = selected
                showTimePicker = false
            }
        )
    }
}