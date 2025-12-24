package kr.jiyeok.seatly.ui.screen.admin.cafe

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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
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
import kr.jiyeok.seatly.ui.screen.manager.RegisterCafeTopBar
import kotlin.math.min

/**
 * RegisterCafeScreen1
 *
 * UI restored to the original layout with the following added behavior:
 * - Field-level validation errors are shown inline under each relevant section (카페명, 전화번호).
 * - Errors clear when user edits the corresponding field.
 * - On "다음 (2/2)" click, validations run and only when passing the data is saved to savedStateHandle and navigation occurs.
 */

@Composable
fun RegisterCafeScreen1(
    navController: NavController,
    modifier: Modifier = Modifier
) {
    // Theme colors matching design
    val Primary = Color(0xFFe95321)
    val BackgroundBase = Color(0xFFFFFFFF)
    val InputBg = Color(0xFFF8F8F8)
    val BorderColor = Color(0xFFE8E8E8)
    val TextMain = Color(0xFF1A1A1A)
    val TextSub = Color(0xFF888888)
    val StepBg = Color(0xFFfdede8)
    val ChipSelectedBg = Color(0xFFFFF0EB)
    val ChipUnselectedBg = InputBg

    // Form state
    var cafeName by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var startTime by remember { mutableStateOf("09:00") }
    var endTime by remember { mutableStateOf("24:00") }

    var showTimePicker by remember { mutableStateOf(false) }
    var pickingTarget by remember { mutableStateOf("start") }

    val weekdays = listOf("연중 무휴", "월요일", "화요일", "수요일", "목요일", "금요일", "토요일", "일요일")
    var selectedWeekdays by remember { mutableStateOf(setOf("연중 무휴")) }

    val images = remember { mutableStateListOf<Uri>() }
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    // Gallery launcher
    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
        uris.forEach { uri ->
            if (images.size < 5) images.add(uri)
        }
    }

    // Field-level error states (null = no error)
    var cafeNameError by remember { mutableStateOf<String?>(null) }
    var phoneError by remember { mutableStateOf<String?>(null) }

    fun formatKoreanPhoneFromDigits(digits: String): String {
        if (digits.isEmpty()) return ""
        return if (digits.startsWith("02")) {
            when {
                digits.length <= 2 -> digits
                digits.length <= 5 -> digits.substring(0, 2) + "-" + digits.substring(2)
                digits.length <= 9 -> digits.substring(0, 2) + "-" + digits.substring(2, digits.length - 4) + "-" + digits.takeLast(4)
                else -> digits.substring(0, 2) + "-" + digits.substring(2, 6) + "-" + digits.substring(6,
                    min(10, digits.length)
                )
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

    @Composable
    fun rememberBitmapForUri(uri: Uri): ImageBitmap? {
        var bitmap by remember(uri) { mutableStateOf<ImageBitmap?>(null) }
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

    // Local AppTextField (kept as original)
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
            .height(52.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(InputBg, RoundedCornerShape(12.dp))
            .border(BorderStroke(1.dp, if (focused || isErrorBorder) Primary else BorderColor), RoundedCornerShape(12.dp))
            .padding(start = 12.dp, end = 12.dp)

        if (readOnly && onClick != null) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = baseModifier.clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { onClick() }
            ) {
                if (leading != null) {
                    Box(modifier = Modifier.padding(end = 8.dp)) { leading() }
                }
                Box(modifier = Modifier.weight(1f)) {
                    if (value.isEmpty()) Text(text = placeholder, color = TextSub, fontSize = 15.sp)
                    else Text(text = value, color = TextMain, fontSize = 15.sp)
                }
            }
        } else {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = baseModifier) {
                if (leading != null) {
                    Box(modifier = Modifier.padding(end = 8.dp)) { leading() }
                }
                Box(modifier = Modifier.weight(1f)) {
                    BasicTextField(
                        value = value,
                        onValueChange = onValueChange,
                        singleLine = true,
                        textStyle = TextStyle(color = TextMain, fontSize = 15.sp),
                        cursorBrush = SolidColor(Primary),
                        keyboardOptions = keyboardOptions,
                        modifier = Modifier
                            .fillMaxWidth()
                            .onFocusChanged { state -> focused = state.isFocused }
                    ) { innerTextField ->
                        if (value.isEmpty()) Text(text = placeholder, color = TextSub, fontSize = 15.sp)
                        innerTextField()
                    }
                }
            }
        }
    }

    Surface(modifier = modifier.fillMaxSize(), color = BackgroundBase) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header
                Column(modifier = Modifier
                    .fillMaxWidth()
                    .background(BackgroundBase)
                    .padding(top = 20.dp, bottom = 6.dp)
                ) {
                    RegisterCafeTopBar(
                        navController = navController,
                        title = "카페 등록",
                        titleFontSize = 20.sp,
                        titleColor = TextMain,
                        onBack = { navController.popBackStack() },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Box(modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(Color(0xFFF4F4F4))
                    )
                }

                // Body - scrollable
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(scrollState)
                        .padding(start = 20.dp, end = 20.dp, top = 18.dp, bottom = 120.dp)
                ) {
                    // Step pill + heading
                    Box {
                        Text(
                            text = "Step 1/2",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Primary,
                            modifier = Modifier
                                .background(StepBg, RoundedCornerShape(12.dp))
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "카페의 기본 정보를 입력해주세요",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextMain,
                        modifier = Modifier.padding(bottom = 18.dp)
                    )

                    // Cafe name
                    Text("카페명", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TextMain)
                    Spacer(modifier = Modifier.height(8.dp))
                    AppTextField(
                        value = cafeName,
                        onValueChange = {
                            cafeName = it
                            cafeNameError = null // clear error on edit
                        },
                        placeholder = "예: 명지 스터디카페",
                        leading = {
                            MaterialSymbol(name = "domain", size = 18.sp, tint = TextSub)
                        },
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                        isErrorBorder = cafeNameError != null
                    )
                    // inline error for cafe name
                    cafeNameError?.let { Text(text = it, color = Color(0xFFFF453A), fontSize = 12.sp, modifier = Modifier.padding(top = 6.dp)) }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Phone
                    Text("대표 전화번호", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TextMain)
                    Spacer(modifier = Modifier.height(8.dp))
                    AppTextField(
                        value = phone,
                        onValueChange = { raw ->
                            val digits = raw.filter { it.isDigit() }
                            phone = formatKoreanPhoneFromDigits(digits)
                            phoneError = null // clear error on edit
                        },
                        placeholder = "010-1234-5678",
                        leading = {
                            MaterialSymbol(name = "phone", size = 18.sp, tint = TextSub)
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone, imeAction = ImeAction.Next),
                        isErrorBorder = phoneError != null
                    )
                    // inline error for phone
                    phoneError?.let { Text(text = it, color = Color(0xFFFF453A), fontSize = 12.sp, modifier = Modifier.padding(top = 6.dp)) }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Operating hours
                    Text("영업 시간", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TextMain)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
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

                        Text("~", color = TextSub, fontSize = 16.sp)

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

                    // Holiday chips
                    Text("정기 휴무일", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TextMain)
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        weekdays.forEach { day ->
                            val selected = selectedWeekdays.contains(day)
                            val bg = if (selected) ChipSelectedBg else ChipUnselectedBg
                            val borderCol = if (selected) Primary else BorderColor
                            val textCol = if (selected) Primary else TextMain
                            Box(
                                modifier = Modifier
                                    .defaultMinSize(minHeight = 44.dp)
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(bg)
                                    .border(BorderStroke(1.dp, borderCol), RoundedCornerShape(20.dp))
                                    .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) {
                                        selectedWeekdays = if (day == "연중 무휴") {
                                            setOf("연중 무휴")
                                        } else {
                                            val mutable = selectedWeekdays.toMutableSet()
                                            if (mutable.contains("연중 무휴")) mutable.remove("연중 무휴")
                                            if (mutable.contains(day)) mutable.remove(day) else mutable.add(day)
                                            if (mutable.isEmpty()) mutable.add("연중 무휴")
                                            mutable.toSet()
                                        }
                                    }
                                    .padding(horizontal = 16.dp, vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = day,
                                    color = textCol,
                                    fontSize = 14.sp,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Photos
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Text("카페 사진 (최대 5장)", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TextMain)
                        Text("${images.size}/5", fontSize = 13.sp, color = TextMain)
                    }
                    Spacer(modifier = Modifier.height(12.dp))

                    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        item {
                            Box(
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .border(BorderStroke(1.dp, BorderColor), RoundedCornerShape(12.dp))
                                    .background(InputBg)
                                    .clickable { galleryLauncher.launch("image/*") },
                                contentAlignment = Alignment.Center
                            ) {
                                MaterialSymbol(name = "add", size = 24.sp, tint = TextSub)
                            }
                        }

                        items(images) { uri ->
                            val bmp = rememberBitmapForUri(uri)
                            Box(
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFFF3E7DE)),
                                contentAlignment = Alignment.TopEnd
                            ) {
                                if (bmp != null) {
                                    Image(bitmap = bmp, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                                }
                                IconButton(
                                    onClick = { images.remove(uri) },
                                    modifier = Modifier
                                        .size(24.dp)
                                        .padding(4.dp)
                                        .background(Color(0x66000000), RoundedCornerShape(12.dp))
                                ) {
                                    MaterialSymbol(name = "close", size = 12.sp, tint = Color.White)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // keep UI original: no in-body "다음" here; bottom fixed bar will contain Previous/Next
                }
            }

            // Bottom fixed bar (original UI) with Previous / Next
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
                    OutlinedButton(
                        onClick = { navController.popBackStack() },
                        border = BorderStroke(1.dp, BorderColor),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TextMain, containerColor = Color.Transparent),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                    ) {
                        Text("이전", fontSize = 16.sp)
                    }

                    Button(
                        onClick = {
                            // Validate fields and set inline errors if any
                            var ok = true
                            if (cafeName.isBlank()) {
                                cafeNameError = "카페명을 입력해주세요."
                                ok = false
                            } else {
                                cafeNameError = null
                            }

                            if (phone.isBlank()) {
                                phoneError = "전화번호를 입력해주세요."
                                ok = false
                            } else {
                                // basic pattern check: at least 9 digits overall
                                val digits = phone.filter { it.isDigit() }
                                if (digits.length < 9) {
                                    phoneError = "유효한 전화번호를 입력해주세요."
                                    ok = false
                                } else {
                                    phoneError = null
                                }
                            }

                            if (!ok) return@Button

                            // save to savedStateHandle for screen2
                            val handle = navController.currentBackStackEntry?.savedStateHandle
                            handle?.set("reg_cafe_name", cafeName)
                            handle?.set("reg_phone", phone)
                            handle?.set("reg_start", startTime)
                            handle?.set("reg_end", endTime)
                            handle?.set("reg_weekdays", selectedWeekdays.toList())
                            handle?.set("reg_images", images.map { it.toString() })
                            // navigate
                            navController.navigate("register_cafe_2")
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Primary, contentColor = Color.White),
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("다음 (2/2)", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // TimePicker sheet
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
}