package kr.jiyeok.seatly.ui.screen.admin.cafe

import android.graphics.BitmapFactory
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
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
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kr.jiyeok.seatly.data.remote.enums.EFacility
import kr.jiyeok.seatly.data.remote.request.CreateCafeRequest
import kr.jiyeok.seatly.presentation.viewmodel.CreateCafeViewModel
import kr.jiyeok.seatly.ui.component.MaterialSymbol
import kr.jiyeok.seatly.ui.screen.manager.RegisterCafeTopBar
import kr.jiyeok.seatly.ui.theme.ColorPrimaryOrange
import kr.jiyeok.seatly.ui.theme.ColorTextBlack
import kr.jiyeok.seatly.ui.theme.ColorTextGray
import kr.jiyeok.seatly.ui.theme.ColorBorderLight
import kr.jiyeok.seatly.ui.theme.ColorInputBg
import kr.jiyeok.seatly.ui.theme.ColorBgBeige
import kr.jiyeok.seatly.ui.theme.ColorWhite
import kr.jiyeok.seatly.ui.theme.ColorRedBadge
import kotlin.math.min

@Composable
fun CreateCafeScreen(
    navController: NavController,
    viewModel: CreateCafeViewModel = hiltViewModel(),
    modifier: Modifier = Modifier
) {
    // Theme colors from Color.kt
    val Primary = ColorPrimaryOrange
    val BackgroundBase = ColorWhite
    val InputBg = ColorInputBg
    val BorderColor = ColorBorderLight
    val TextMain = ColorTextBlack
    val TextSub = ColorTextGray
    val TextError = ColorRedBadge
    val StepBg = ColorBgBeige
    val ChipSelectedBg = Color(0xFFFFF0EB)
    val ChipUnselectedBg = InputBg

    // Form state
    var cafeName by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var phoneDisplay by remember { mutableStateOf("") }
    var startTime by remember { mutableStateOf("09:00") }
    var endTime by remember { mutableStateOf("24:00") }
    var description by remember { mutableStateOf("") }

    var showStartTimePicker by remember { mutableStateOf(false) }
    var showEndTimePicker by remember { mutableStateOf(false) }
    var showAddressSearch by remember { mutableStateOf(false) }

    val holidays = listOf(
        "연중 무휴",
        "월요일", "화요일", "수요일", "목요일", "금요일",
        "토요일", "일요일",
        "공휴일"
    )
    var selectedHolidays by remember { mutableStateOf(setOf("연중 무휴")) }
    var showAllHolidays by remember { mutableStateOf(false) }

    val selectedFacilities = remember { mutableStateListOf<EFacility>() }
    val images = remember { mutableStateListOf<Uri>() }

    val context = LocalContext.current
    val scrollState = rememberScrollState()

    // ViewModel states
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()

    // Gallery launcher
    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
        uris.forEach { uri ->
            if (images.size < 5) images.add(uri)
        }
    }

    // Error states
    var cafeNameError by remember { mutableStateOf<String?>(null) }
    var addressError by remember { mutableStateOf<String?>(null) }
    var phoneError by remember { mutableStateOf<String?>(null) }

    // Collect error messages from ViewModel
    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
        }
    }

    fun formatKoreanPhoneFromDigits(digits: String): String {
        if (digits.isEmpty()) return ""
        return if (digits.startsWith("02")) {
            when {
                digits.length <= 2 -> digits
                digits.length <= 5 -> digits.substring(0, 2) + "-" + digits.substring(2)
                digits.length <= 9 -> digits.substring(0, 2) + "-" + digits.substring(2, digits.length - 4) + "-" + digits.takeLast(4)
                else -> digits.substring(0, 2) + "-" + digits.substring(2, 6) + "-" + digits.substring(6, min(10, digits.length))
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

    @Composable
    fun AppTextField(
        value: String,
        onValueChange: (String) -> Unit,
        placeholder: String,
        leading: (@Composable () -> Unit)? = null,
        trailing: (@Composable () -> Unit)? = null,
        keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
        isErrorBorder: Boolean = false,
        readOnly: Boolean = false,
        onClick: (() -> Unit)? = null,
        maxLines: Int = 1,
        modifier: Modifier = Modifier
    ) {
        var focused by remember { mutableStateOf(false) }
        val baseModifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 52.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(InputBg, RoundedCornerShape(12.dp))
            .border(BorderStroke(1.dp, if (focused || isErrorBorder) Primary else BorderColor), RoundedCornerShape(12.dp))

        if (readOnly && onClick != null) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = baseModifier
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { onClick() }
                    .padding(start = 12.dp, end = 12.dp, top = 12.dp, bottom = 12.dp)
            ) {
                if (leading != null) {
                    Box(modifier = Modifier.padding(end = 8.dp)) { leading() }
                }
                Box(modifier = Modifier.weight(1f)) {
                    if (value.isEmpty()) Text(text = placeholder, color = TextSub, fontSize = 15.sp)
                    else Text(text = value, color = TextMain, fontSize = 15.sp)
                }
                if (trailing != null) {
                    Box(modifier = Modifier.padding(start = 8.dp)) { trailing() }
                }
            }
        } else {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = baseModifier.padding(horizontal = 12.dp, vertical = 0.dp)
            ) {
                if (leading != null) {
                    Box(modifier = Modifier.padding(end = 8.dp)) { leading() }
                }
                Box(modifier = Modifier
                    .weight(1f)
                    .defaultMinSize(minHeight = 52.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    BasicTextField(
                        value = value,
                        onValueChange = onValueChange,
                        singleLine = maxLines == 1,
                        textStyle = TextStyle(color = TextMain, fontSize = 15.sp),
                        cursorBrush = SolidColor(Primary),
                        keyboardOptions = keyboardOptions,
                        modifier = Modifier
                            .fillMaxWidth()
                            .onFocusChanged { state -> focused = state.isFocused },
                        decorationBox = { innerTextField ->
                            if (value.isEmpty()) {
                                Text(text = placeholder, color = TextSub, fontSize = 15.sp)
                            }
                            innerTextField()
                        }
                    )
                }
                if (trailing != null) {
                    Box(modifier = Modifier.padding(start = 8.dp)) { trailing() }
                }
            }
        }
    }

    @Composable
    fun FacilityIcon(facility: EFacility): String {
        return when (facility) {
            EFacility.WIFI -> "wifi"
            EFacility.PRINTER -> "print"
            EFacility.OUTLET -> "outlet"
            EFacility.OPEN_24H -> "schedule"
            EFacility.CAFE -> "local_cafe"
            EFacility.MEETING_ROOM -> "meeting_room"
            EFacility.LOCKER -> "inventory_2"
            EFacility.AIR_CONDITIONING -> "ac_unit"
        }
    }

    @Composable
    fun FacilityName(facility: EFacility): String {
        return when (facility) {
            EFacility.WIFI -> "Wi-Fi"
            EFacility.PRINTER -> "프린터"
            EFacility.OUTLET -> "콘센트"
            EFacility.OPEN_24H -> "24시간"
            EFacility.CAFE -> "카페"
            EFacility.MEETING_ROOM -> "회의실"
            EFacility.LOCKER -> "락커"
            EFacility.AIR_CONDITIONING -> "에어컨"
        }
    }

    Surface(modifier = modifier.fillMaxSize(), color = BackgroundBase) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header
                Column(
                    modifier = Modifier
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
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(ColorBorderLight)
                    )
                }

                // Body - scrollable
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(scrollState)
                        .padding(start = 20.dp, end = 20.dp, top = 18.dp, bottom = 20.dp)
                ) {
                    // Step indicator
                    Text(
                        text = "기본정보 입력",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Primary,
                        modifier = Modifier
                            .background(StepBg, RoundedCornerShape(12.dp))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                    Spacer(modifier = Modifier.height(20.dp))

                    // 1. Cafe Name (필수)
                    Text("카페명 *", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TextMain)
                    Spacer(modifier = Modifier.height(8.dp))
                    AppTextField(
                        value = cafeName,
                        onValueChange = {
                            cafeName = it
                            cafeNameError = null
                        },
                        placeholder = "예: 명지 스터디카페",
                        leading = { MaterialSymbol(name = "domain", size = 18.sp, tint = TextSub) },
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                        isErrorBorder = cafeNameError != null
                    )
                    cafeNameError?.let {
                        Text(text = it, color = TextError, fontSize = 12.sp, modifier = Modifier.padding(top = 6.dp))
                    }
                    Spacer(modifier = Modifier.height(16.dp))

                    // 2. Address (필수) - 주소 검색 기능
                    Text("주소 *", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TextMain)
                    Spacer(modifier = Modifier.height(8.dp))
                    AppTextField(
                        value = address,
                        onValueChange = {
                            address = it
                            addressError = null
                        },
                        placeholder = "주소 검색...",
                        leading = { MaterialSymbol(name = "location_on", size = 18.sp, tint = TextSub) },
                        trailing = {
                            Box(
                                modifier = Modifier.clickable(
                                    indication = null,
                                    interactionSource = remember { MutableInteractionSource() }
                                ) { showAddressSearch = true }
                            ) {
                                MaterialSymbol(
                                    name = "search",
                                    size = 18.sp,
                                    tint = TextSub
                                )
                            }
                        },
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                        isErrorBorder = addressError != null
                    )
                    addressError?.let {
                        Text(text = it, color = TextError, fontSize = 12.sp, modifier = Modifier.padding(top = 6.dp))
                    }
                    Spacer(modifier = Modifier.height(16.dp))

                    // 3. Phone (필수) - 자동 포맷팅
                    Text("대표 전화번호 *", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TextMain)
                    Spacer(modifier = Modifier.height(8.dp))
                    AppTextField(
                        value = phoneDisplay,
                        onValueChange = { input ->
                            val digitsOnly = input.filter { it.isDigit() }
                            if (digitsOnly.length <= 11) {
                                phone = digitsOnly
                                phoneDisplay = formatKoreanPhoneFromDigits(digitsOnly)
                                phoneError = null
                            }
                        },
                        placeholder = "010-1234-5678",
                        leading = { MaterialSymbol(name = "phone", size = 18.sp, tint = TextSub) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone, imeAction = ImeAction.Next),
                        isErrorBorder = phoneError != null
                    )
                    phoneError?.let {
                        Text(text = it, color = TextError, fontSize = 12.sp, modifier = Modifier.padding(top = 6.dp))
                    }
                    Spacer(modifier = Modifier.height(16.dp))

                    // 4. Opening Hours (선택)
                    Text("영업 시간", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TextMain)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AppTextField(
                            value = startTime,
                            onValueChange = {},
                            placeholder = "시작",
                            leading = { MaterialSymbol(name = "schedule", size = 18.sp, tint = TextSub) },
                            readOnly = true,
                            onClick = { showStartTimePicker = true },
                            modifier = Modifier.weight(1f)
                        )

                        Text("~", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextMain)

                        AppTextField(
                            value = endTime,
                            onValueChange = {},
                            placeholder = "종료",
                            readOnly = true,
                            onClick = { showEndTimePicker = true },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))

                    // 5. Holiday Selection
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("정기 휴무일", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TextMain)
                        Button(
                            onClick = { showAllHolidays = !showAllHolidays },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent, contentColor = Primary),
                            modifier = Modifier
                                .defaultMinSize(minWidth = 0.dp, minHeight = 0.dp)
                                .rotate(if (showAllHolidays) 180f else 0f),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
                        ) {
                            MaterialSymbol(
                                name = "expand_more",
                                size = 20.sp,
                                tint = Primary
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        // 선택된 항목을 칩 형태로 한줄에 표시
                        if (selectedHolidays.isNotEmpty()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .wrapContentHeight(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                selectedHolidays.forEach { holiday: String ->
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(16.dp))
                                            .background(ChipSelectedBg)
                                            .border(BorderStroke(1.dp, Primary), RoundedCornerShape(16.dp))
                                            .padding(horizontal = 10.dp, vertical = 6.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Text(
                                                text = holiday,
                                                color = Primary,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                            if (holiday != "연중 무휴") {
                                                Box(
                                                    modifier = Modifier
                                                        .size(16.dp)
                                                        .clip(RoundedCornerShape(8.dp))
                                                        .background(Primary)
                                                        .clickable(
                                                            indication = null,
                                                            interactionSource = remember { MutableInteractionSource() }
                                                        ) {
                                                            selectedHolidays = if (holiday == "연중 무휴") {
                                                                setOf("연중 무휴")
                                                            } else {
                                                                val mutable = selectedHolidays.toMutableSet()
                                                                mutable.remove(holiday)
                                                                if (mutable.isEmpty()) mutable.add("연중 무휴")
                                                                mutable.toSet()
                                                            }
                                                        },
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    MaterialSymbol(
                                                        name = "close",
                                                        size = 10.sp,
                                                        tint = ColorWhite
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // 모든 옵션 표시
                        if (showAllHolidays) {
                            val unselectedHolidays = holidays.filter { !selectedHolidays.contains(it) }
                            val holidayRows: List<List<String>> = unselectedHolidays.chunked(3)
                            holidayRows.forEach { row: List<String> ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    row.forEach { holiday: String ->
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .defaultMinSize(minHeight = 40.dp)
                                                .clip(RoundedCornerShape(20.dp))
                                                .background(ChipUnselectedBg)
                                                .border(BorderStroke(1.dp, BorderColor), RoundedCornerShape(20.dp))
                                                .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) {
                                                    selectedHolidays = if (holiday == "연중 무휴") {
                                                        setOf("연중 무휴")
                                                    } else {
                                                        val mutable = selectedHolidays.toMutableSet()
                                                        if (mutable.contains("연중 무휴")) mutable.remove("연중 무휴")
                                                        if (mutable.contains(holiday)) mutable.remove(holiday) else mutable.add(holiday)
                                                        if (mutable.isEmpty()) mutable.add("연중 무휴")
                                                        mutable.toSet()
                                                    }
                                                }
                                                .padding(horizontal = 12.dp, vertical = 8.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = holiday,
                                                color = TextMain,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Normal
                                            )
                                        }
                                    }
                                    repeat((3 - row.size)) {
                                        Spacer(modifier = Modifier.weight(1f))
                                    }
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(20.dp))

                    // 6. Facilities (선택)
                    Text("시설 선택", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TextMain)
                    Spacer(modifier = Modifier.height(12.dp))

                    val facilitiesList = EFacility.values().toList()
                    val facilitiesRows = facilitiesList.chunked(4)
                    facilitiesRows.forEach { row ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            row.forEach { facility ->
                                val isSelected = selectedFacilities.contains(facility)
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(80.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(if (isSelected) ChipSelectedBg else InputBg)
                                        .border(
                                            BorderStroke(1.5.dp, if (isSelected) Primary else BorderColor),
                                            RoundedCornerShape(12.dp)
                                        )
                                        .clickable {
                                            if (selectedFacilities.contains(facility)) {
                                                selectedFacilities.remove(facility)
                                            } else {
                                                selectedFacilities.add(facility)
                                            }
                                        }
                                        .padding(8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(
                                        modifier = Modifier.fillMaxSize(),
                                        verticalArrangement = Arrangement.Center,
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        MaterialSymbol(
                                            name = FacilityIcon(facility),
                                            size = 24.sp,
                                            tint = if (isSelected) Primary else TextSub
                                        )

                                        Spacer(modifier = Modifier.height(4.dp))

                                        Text(
                                            text = FacilityName(facility),
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = if (isSelected) Primary else TextMain
                                        )
                                    }
                                }
                            }
                            repeat((4 - row.size)) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // 7. Description
                    Text("설명", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TextMain)
                    Spacer(modifier = Modifier.height(8.dp))
                    AppTextField(
                        value = description,
                        onValueChange = { description = it },
                        placeholder = "카페에 대한 설명을 입력하세요",
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Default),
                        maxLines = 4
                    )
                    Spacer(modifier = Modifier.height(20.dp))

                    // 8. Photos
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Text("카페 사진 (최대 5장)", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TextMain)
                        Text("${images.size}/5", fontSize = 13.sp, color = TextMain)
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        item {
                            Box(
                                modifier = Modifier
                                    .width(72.dp)
                                    .height(72.dp)
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
                                    .width(72.dp)
                                    .height(72.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(ColorBgBeige),
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
                                    MaterialSymbol(name = "close", size = 12.sp, tint = ColorWhite)
                                }
                            }
                        }
                    }
                }

                // Bottom fixed bar
                Surface(
                    tonalElevation = 4.dp,
                    shadowElevation = 4.dp,
                    color = BackgroundBase,
                    modifier = Modifier.fillMaxWidth()
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
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = TextMain, containerColor = ColorWhite),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(52.dp)
                        ) {
                            Text("취소", fontSize = 16.sp)
                        }

                        Button(
                            onClick = {
                                var ok = true
                                if (cafeName.isBlank()) {
                                    cafeNameError = "카페명을 입력해주세요."
                                    ok = false
                                } else {
                                    cafeNameError = null
                                }

                                if (address.isBlank()) {
                                    addressError = "주소를 입력해주세요."
                                    ok = false
                                } else {
                                    addressError = null
                                }

                                if (phone.isBlank()) {
                                    phoneError = "전화번호를 입력해주세요."
                                    ok = false
                                } else {
                                    if (phone.length < 9) {
                                        phoneError = "유효한 전화번호를 입력해주세요."
                                        ok = false
                                    } else {
                                        phoneError = null
                                    }
                                }

                                if (!ok) return@Button

                                val openingHoursStr = "$startTime - $endTime"
                                val request = CreateCafeRequest(
                                    name = cafeName,
                                    address = address,
                                    phoneNumber = phoneDisplay,
                                    openingHours = openingHoursStr,
                                    description = if (description.isBlank()) null else description,
                                    facilities = selectedFacilities.toList(),
                                    imageUrls = emptyList()
                                )

                                viewModel.createCafe(request)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Primary, contentColor = ColorWhite),
                            modifier = Modifier
                                .weight(1f)
                                .height(52.dp),
                            shape = RoundedCornerShape(12.dp),
                            enabled = !isLoading
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = ColorWhite,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Text("등록하기", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            if (showStartTimePicker) {
                ScrollableTimePickerDialog(
                    initialTime = startTime,
                    onConfirm = { time ->
                        startTime = time
                        showStartTimePicker = false
                    },
                    onDismiss = { showStartTimePicker = false },
                    primary = Primary,
                    textMain = TextMain,
                    backgroundBase = BackgroundBase,
                    borderColor = BorderColor
                )
            }

            if (showEndTimePicker) {
                ScrollableTimePickerDialog(
                    initialTime = endTime,
                    onConfirm = { time ->
                        endTime = time
                        showEndTimePicker = false
                    },
                    onDismiss = { showEndTimePicker = false },
                    primary = Primary,
                    textMain = TextMain,
                    backgroundBase = BackgroundBase,
                    borderColor = BorderColor
                )
            }

            if (showAddressSearch) {
                AddressSearchDialog(
                    onAddressSelected = { selectedAddress ->
                        address = selectedAddress
                        showAddressSearch = false
                    },
                    onDismiss = { showAddressSearch = false },
                    primary = Primary,
                    textMain = TextMain,
                    backgroundBase = BackgroundBase,
                    borderColor = BorderColor,
                    inputBg = InputBg,
                    textSub = TextSub
                )
            }
        }
    }
}

@Composable
private fun AddressSearchDialog(
    onAddressSelected: (String) -> Unit,
    onDismiss: () -> Unit,
    primary: Color,
    textMain: Color,
    backgroundBase: Color,
    borderColor: Color,
    inputBg: Color,
    textSub: Color
) {
    var searchQuery by remember { mutableStateOf("") }
    var searchResults by remember { mutableStateOf(listOf<String>()) }

    val sampleAddresses = listOf(
        "서울시 강남구 테헤란로 123",
        "서울시 서초구 반포대로 201",
        "서울시 마포구 월드컵북로 200",
        "서울시 영등포구 여의나루로 60",
        "서울시 광진구 능동로 어린이대공원",
        "경기도 수원시 팔달구 권광로 535",
        "경기도 성남시 분당구 판교역로 166",
        "인천시 연수구 송도동 인천대공원",
        "대전시 유성구 과학로 125",
        "부산시 해운대구 센텀중앙로 78"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.5f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onDismiss() },
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier
                .width(320.dp)
                .height(500.dp)
                .clip(RoundedCornerShape(16.dp))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {},
            color = backgroundBase
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                Text(
                    text = "주소 검색",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = textMain,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 48.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(inputBg, RoundedCornerShape(10.dp))
                        .border(BorderStroke(1.dp, borderColor), RoundedCornerShape(10.dp))
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    MaterialSymbol(name = "search", size = 18.sp, tint = textSub)
                    Spacer(modifier = Modifier.width(8.dp))
                    BasicTextField(
                        value = searchQuery,
                        onValueChange = { query ->
                            searchQuery = query
                            searchResults = sampleAddresses.filter { it.contains(query, ignoreCase = true) }
                        },
                        singleLine = true,
                        textStyle = TextStyle(color = textMain, fontSize = 14.sp),
                        cursorBrush = SolidColor(primary),
                        modifier = Modifier
                            .weight(1f)
                            .defaultMinSize(minHeight = 24.dp),
                        decorationBox = { innerTextField ->
                            if (searchQuery.isEmpty()) {
                                Text("주소를 입력하세요", color = textSub, fontSize = 14.sp)
                            }
                            innerTextField()
                        }
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (searchQuery.isEmpty()) {
                        items(sampleAddresses.take(5)) { address ->
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFFFAFAFA))
                                    .border(BorderStroke(1.dp, Color(0xFFE8E8E8)), RoundedCornerShape(8.dp))
                                    .clickable { onAddressSelected(address) }
                                    .padding(12.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    MaterialSymbol(name = "location_on", size = 18.sp, tint = primary)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = address,
                                        color = textMain,
                                        fontSize = 13.sp,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    } else {
                        if (searchResults.isEmpty()) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(24.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "검색 결과가 없습니다",
                                        color = textSub,
                                        fontSize = 13.sp
                                    )
                                }
                            }
                        } else {
                            items(searchResults) { address ->
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(0xFFFAFAFA))
                                        .border(BorderStroke(1.dp, Color(0xFFE8E8E8)), RoundedCornerShape(8.dp))
                                        .clickable { onAddressSelected(address) }
                                        .padding(12.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        MaterialSymbol(name = "location_on", size = 18.sp, tint = primary)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = address,
                                            color = textMain,
                                            fontSize = 13.sp,
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ColorBorderLight, contentColor = textMain),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("닫기", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun ScrollableTimePickerDialog(
    initialTime: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
    primary: Color,
    textMain: Color,
    backgroundBase: Color,
    borderColor: Color
) {
    val textSub = ColorTextGray

    val parts = initialTime.split(":")
    var selectedHour by remember { mutableStateOf(parts.getOrNull(0)?.toIntOrNull() ?: 9) }
    var selectedMinute by remember { mutableStateOf(parts.getOrNull(1)?.toIntOrNull() ?: 0) }

    val hours = (0..23).toList()
    val minutes = (0..59).filter { it % 5 == 0 }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.5f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onDismiss() },
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier
                .width(300.dp)
                .clip(RoundedCornerShape(16.dp))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {},
            color = backgroundBase
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "시간 선택",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = textMain
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxSize()
                    ) {
                        val hourScrollState = rememberScrollState()
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .verticalScroll(hourScrollState),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Spacer(modifier = Modifier.height(40.dp))
                            hours.forEach { hour ->
                                Text(
                                    text = hour.toString().padStart(2, '0'),
                                    fontSize = if (hour == selectedHour) 28.sp else 16.sp,
                                    fontWeight = if (hour == selectedHour) FontWeight.Bold else FontWeight.Normal,
                                    color = if (hour == selectedHour) primary else textSub,
                                    modifier = Modifier
                                        .padding(8.dp)
                                        .clickable { selectedHour = hour }
                                )
                            }
                            Spacer(modifier = Modifier.height(40.dp))
                        }
                    }

                    Text(":", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = textMain)

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxSize()
                    ) {
                        val minuteScrollState = rememberScrollState()
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .verticalScroll(minuteScrollState),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Spacer(modifier = Modifier.height(40.dp))
                            minutes.forEach { minute ->
                                Text(
                                    text = minute.toString().padStart(2, '0'),
                                    fontSize = if (minute == selectedMinute) 28.sp else 16.sp,
                                    fontWeight = if (minute == selectedMinute) FontWeight.Bold else FontWeight.Normal,
                                    color = if (minute == selectedMinute) primary else textSub,
                                    modifier = Modifier
                                        .padding(8.dp)
                                        .clickable { selectedMinute = minute }
                                )
                            }
                            Spacer(modifier = Modifier.height(40.dp))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        border = BorderStroke(1.dp, borderColor)
                    ) {
                        Text("취소", color = textMain)
                    }
                    Button(
                        onClick = {
                            val timeStr = "${selectedHour.toString().padStart(2, '0')}:${selectedMinute.toString().padStart(2, '0')}"
                            onConfirm(timeStr)
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = primary)
                    ) {
                        Text("확인", color = ColorWhite, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
