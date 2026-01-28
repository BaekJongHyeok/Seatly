package kr.jiyeok.seatly.ui.screen.admin

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import kr.jiyeok.seatly.data.remote.enums.EFacility
import kr.jiyeok.seatly.data.remote.request.CreateCafeRequest
import kr.jiyeok.seatly.data.remote.request.UpdateCafeRequest
import kr.jiyeok.seatly.presentation.viewmodel.CafeFormViewModel
import kr.jiyeok.seatly.ui.component.common.MaterialSymbol
import kr.jiyeok.seatly.ui.component.admin.RegisterCafeTopBar
import kr.jiyeok.seatly.ui.theme.*
import kr.jiyeok.seatly.ui.component.common.AppTextField
import kr.jiyeok.seatly.ui.component.common.PhoneTextField
import kr.jiyeok.seatly.util.formatKoreanPhoneFromDigits
import kr.jiyeok.seatly.util.toImageBitmap
import org.koin.compose.viewmodel.koinViewModel
import kotlin.math.min

// 요일 Enum
enum class DayOfWeek(val displayName: String, val shortName: String) {
    MONDAY("월요일", "월"),
    TUESDAY("화요일", "화"),
    WEDNESDAY("수요일", "수"),
    THURSDAY("목요일", "목"),
    FRIDAY("금요일", "금"),
    SATURDAY("토요일", "토"),
    SUNDAY("일요일", "일")
}

// 요일별 영업시간 데이터 클래스
data class DailyOperatingHours(
    val dayOfWeek: String,
    val isClosed: Boolean = false,
    val openTime: String = "0900", // HHMM 형식
    val closeTime: String = "2200"
)

@Composable
fun AdminCafeFormScreen(
    navController: NavController,
    cafeId: Long? = null,
    viewModel: CafeFormViewModel = koinViewModel(),
) {
    val isEditMode = (cafeId != null)
    val snackbarHostState = remember { SnackbarHostState() }

    // Form state
    var cafeName by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var phoneDisplay by remember { mutableStateOf(TextFieldValue("")) }
    var description by remember { mutableStateOf("") }
    var showAddressSearch by remember { mutableStateOf(false) }

    // 요일별 영업시간 상태
    val weeklyHours = remember {
        mutableStateListOf(
            DailyOperatingHours(DayOfWeek.MONDAY.name, false, "0900", "2200"),
            DailyOperatingHours(DayOfWeek.TUESDAY.name, false, "0900", "2200"),
            DailyOperatingHours(DayOfWeek.WEDNESDAY.name, false, "0900", "2200"),
            DailyOperatingHours(DayOfWeek.THURSDAY.name, false, "0900", "2200"),
            DailyOperatingHours(DayOfWeek.FRIDAY.name, false, "0900", "2200"),
            DailyOperatingHours(DayOfWeek.SATURDAY.name, false, "0900", "2200"),
            DailyOperatingHours(DayOfWeek.SUNDAY.name, false, "0900", "2200")
        )
    }

    var bulkEditMode by remember { mutableStateOf(true) }
    var bulkStartTime by remember { mutableStateOf("0900") }
    var bulkEndTime by remember { mutableStateOf("2200") }
    var showBulkStartTimePicker by remember { mutableStateOf(false) }
    var showBulkEndTimePicker by remember { mutableStateOf(false) }

    var showWeeklyTimePicker by remember { mutableStateOf(false) }
    var selectedDayForTimePicker by remember { mutableStateOf<DayOfWeek?>(null) }
    var isSelectingStartTime by remember { mutableStateOf(true) }

    val holidays = listOf("월", "화", "수", "목", "금", "토", "일", "명절", "연중무휴")
    var selectedHolidays by remember { mutableStateOf(setOf<String>("연중무휴")) }
    var showAllHolidays by remember { mutableStateOf(false) }

    val selectedFacilities = remember { mutableStateListOf<EFacility>() }
    val scrollState = rememberScrollState()

    // ViewModel states
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val cafeData by viewModel.cafeInfo.collectAsState()
    val updateCafeSuccess by viewModel.updateCafeSuccess.collectAsState()
    val createCafeSuccess by viewModel.createCafeSuccess.collectAsState()
    val serverImageUrls by viewModel.serverImageUrls.collectAsState()
    val uploadedImageUrls by viewModel.uploadedImageUrls.collectAsState()
    val imageUploadingCount by viewModel.imageUploadingCount.collectAsState()

    val totalImageCount = serverImageUrls.size + uploadedImageUrls.size

    // Error states
    var cafeNameError by remember { mutableStateOf<String?>(null) }
    var addressError by remember { mutableStateOf<String?>(null) }
    var phoneError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(cafeId) {
        if (isEditMode && cafeId != null) {
            viewModel.loadCafeDetailInfo(cafeId)
        }
    }

    LaunchedEffect(cafeData) {
        cafeData?.let { data ->
            cafeName = data.name
            address = data.address
            phone = data.phone.toString()
            val formattedPhone = formatKoreanPhoneFromDigits(data.phone.toString())
            phoneDisplay = TextFieldValue(formattedPhone, TextRange(formattedPhone.length))
            description = data.description ?: ""

            data.openingHours?.let { hoursStr ->
                try {
                    if (hoursStr.contains(",") && hoursStr.contains("=")) {
                        val dayHoursList = hoursStr.split(",")
                        weeklyHours.clear()
                        dayHoursList.forEach { dayHours ->
                            val parts = dayHours.split("=")
                            if (parts.size == 2) {
                                val dayOfWeek = parts[0]
                                val timeInfo = parts[1]
                                if (timeInfo == "Closed") {
                                    weeklyHours.add(DailyOperatingHours(dayOfWeek, true))
                                } else {
                                    val times = timeInfo.split("-")
                                    if (times.size == 2) {
                                        weeklyHours.add(DailyOperatingHours(dayOfWeek, false, times[0].replace(":", ""), times[1].replace(":", "")))
                                    }
                                }
                            }
                        }
                        bulkEditMode = false
                    } else if (hoursStr.contains("-")) {
                        val hours = hoursStr.split("-")
                        if (hours.size == 2) {
                            bulkStartTime = hours[0].trim().replace(":", "")
                            bulkEndTime = hours[1].trim().replace(":", "")
                            bulkEditMode = true
                        }
                    }
                } catch (e: Exception) {
                    bulkEditMode = true
                }
            }
            selectedFacilities.clear()
            selectedFacilities.addAll(data.facilities)
        }
    }

    LaunchedEffect(updateCafeSuccess, createCafeSuccess) {
        if (updateCafeSuccess || createCafeSuccess) {
            navController.popBackStack()
        }
    }

    LaunchedEffect(Unit) {
        viewModel.events.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = ColorWhite
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(ColorWhite)
                        .padding(top = 20.dp, bottom = 6.dp)
                ) {
                    RegisterCafeTopBar(
                        navController = navController,
                        title = if (isEditMode) "카페 정보 수정" else "카페 등록",
                        titleFontSize = 20.sp,
                        titleColor = ColorTextBlack,
                        onBack = { navController.popBackStack() },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    HorizontalDivider(color = ColorBorderLight)
                }

                // Body
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(scrollState)
                        .padding(horizontal = 20.dp, vertical = 18.dp)
                ) {
                    Text(
                        text = if (isEditMode) "정보 수정" else "기본 정보",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = ColorPrimaryOrange,
                        modifier = Modifier
                            .background(ColorBgBeige, RoundedCornerShape(12.dp))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    Text("카페 이름", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = ColorTextBlack)
                    Spacer(modifier = Modifier.height(8.dp))
                    AppTextField(
                        value = cafeName,
                        onValueChange = {
                            cafeName = it
                            cafeNameError = null
                        },
                        placeholder = "카페 이름을 입력해주세요",
                        leading = { MaterialSymbol(name = "domain", size = 18.sp, tint = ColorTextGray) },
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                        isErrorBorder = cafeNameError != null
                    )
                    cafeNameError?.let {
                        Text(text = it, color = Color(0xFFE57373), fontSize = 12.sp, modifier = Modifier.padding(top = 6.dp))
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text("주소", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = ColorTextBlack)
                    Spacer(modifier = Modifier.height(8.dp))
                    AppTextField(
                        value = address,
                        onValueChange = {
                            address = it
                            addressError = null
                        },
                        placeholder = "카페 주소를 입력해주세요",
                        leading = { MaterialSymbol(name = "location_on", size = 18.sp, tint = ColorTextGray) },
                        trailing = {
                            Box(
                                modifier = Modifier.clickable(
                                    indication = null,
                                    interactionSource = remember { MutableInteractionSource() }
                                ) { showAddressSearch = true }
                            ) {
                                MaterialSymbol(name = "search", size = 18.sp, tint = ColorTextGray)
                            }
                        },
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                        isErrorBorder = addressError != null
                    )
                    addressError?.let {
                        Text(text = it, color = Color(0xFFE57373), fontSize = 12.sp, modifier = Modifier.padding(top = 6.dp))
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text("전화번호", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = ColorTextBlack)
                    Spacer(modifier = Modifier.height(8.dp))
                    PhoneTextField(
                        value = phoneDisplay,
                        onValueChange = { newValue ->
                            val digitsOnly = newValue.text.filter { it.isDigit() }
                            if (digitsOnly.length <= 11) {
                                phone = digitsOnly
                                val formatted = formatKoreanPhoneFromDigits(digitsOnly)
                                phoneDisplay = TextFieldValue(
                                    text = formatted,
                                    selection = TextRange(formatted.length)
                                )
                                phoneError = null
                            }
                        },
                        placeholder = "010-1234-5678",
                        leading = { MaterialSymbol(name = "phone", size = 18.sp, tint = ColorTextGray) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone, imeAction = ImeAction.Next),
                        isErrorBorder = phoneError != null
                    )
                    phoneError?.let {
                        Text(text = it, color = Color(0xFFE57373), fontSize = 12.sp, modifier = Modifier.padding(top = 6.dp))
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    OperatingHoursSection(
                        weeklyHours = weeklyHours,
                        bulkEditMode = bulkEditMode,
                        onBulkEditModeChange = { bulkEditMode = it },
                        bulkStartTime = bulkStartTime,
                        bulkEndTime = bulkEndTime,
                        onBulkStartTimeChange = { showBulkStartTimePicker = true },
                        onBulkEndTimeChange = { showBulkEndTimePicker = true },
                        onShowTimePicker = { dayOfWeek, isStart ->
                            selectedDayForTimePicker = dayOfWeek
                            isSelectingStartTime = isStart
                            showWeeklyTimePicker = true
                        },
                        onApplyBulkTime = {
                            weeklyHours.forEachIndexed { index, _ ->
                                weeklyHours[index] = weeklyHours[index].copy(
                                    openTime = bulkStartTime,
                                    closeTime = bulkEndTime,
                                    isClosed = false
                                )
                            }
                        }
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("휴무일", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = ColorTextBlack)
                        IconButton(
                            onClick = { showAllHolidays = !showAllHolidays },
                            modifier = Modifier.rotate(if (showAllHolidays) 180f else 0f)
                        ) {
                            MaterialSymbol(name = "expand_more", size = 20.sp, tint = ColorPrimaryOrange)
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (selectedHolidays.isNotEmpty()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .wrapContentHeight(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                selectedHolidays.forEach { holiday ->
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(16.dp))
                                            .background(Color(0xFFFFF0EB))
                                            .border(
                                                BorderStroke(1.dp, ColorPrimaryOrange),
                                                RoundedCornerShape(16.dp)
                                            )
                                            .padding(horizontal = 10.dp, vertical = 6.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Text(
                                                text = holiday,
                                                color = ColorPrimaryOrange,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                            if (holiday != "연중무휴") {
                                                Box(
                                                    modifier = Modifier
                                                        .size(16.dp)
                                                        .clip(CircleShape)
                                                        .background(ColorPrimaryOrange)
                                                        .clickable(
                                                            indication = null,
                                                            interactionSource = remember { MutableInteractionSource() }
                                                        ) {
                                                            selectedHolidays = if (holiday == "연중무휴") {
                                                                setOf("연중무휴")
                                                            } else {
                                                                val mutable = selectedHolidays.toMutableSet()
                                                                mutable.remove(holiday)
                                                                if (mutable.isEmpty()) mutable.add("연중무휴")
                                                                mutable.toSet()
                                                            }
                                                        },
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    MaterialSymbol(name = "close", size = 10.sp, tint = ColorWhite)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        if (showAllHolidays) {
                            val unselectedHolidays = holidays.filter { !selectedHolidays.contains(it) }
                            unselectedHolidays.chunked(3).forEach { row ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    row.forEach { holiday ->
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(40.dp)
                                                .clip(RoundedCornerShape(20.dp))
                                                .background(ColorInputBg)
                                                .border(BorderStroke(1.dp, ColorBorderLight), RoundedCornerShape(20.dp))
                                                .clickable {
                                                    selectedHolidays = if (holiday == "연중무휴") {
                                                        setOf("연중무휴")
                                                    } else {
                                                        val mutable = selectedHolidays.toMutableSet()
                                                        if (mutable.contains("연중무휴")) mutable.remove("연중무휴")
                                                        if (mutable.contains(holiday)) mutable.remove(holiday)
                                                        else mutable.add(holiday)
                                                        if (mutable.isEmpty()) mutable.add("연중무휴")
                                                        mutable.toSet()
                                                    }
                                                },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(text = holiday, fontSize = 12.sp, color = ColorTextBlack)
                                        }
                                    }
                                    repeat(3 - row.size) { Spacer(modifier = Modifier.weight(1f)) }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Text("편의시설", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = ColorTextBlack)
                    Spacer(modifier = Modifier.height(12.dp))

                    EFacility.entries.chunked(4).forEach { row ->
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
                                        .background(if (isSelected) Color(0xFFFFF0EB) else ColorInputBg)
                                        .border(
                                            BorderStroke(
                                                1.5.dp,
                                                if (isSelected) ColorPrimaryOrange else ColorBorderLight
                                            ),
                                            RoundedCornerShape(12.dp)
                                        )
                                        .clickable {
                                            if (selectedFacilities.contains(facility)) selectedFacilities.remove(facility)
                                            else selectedFacilities.add(facility)
                                        }
                                        .padding(8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        MaterialSymbol(
                                            name = FacilityIcon(facility),
                                            size = 24.sp,
                                            tint = if (isSelected) ColorPrimaryOrange else ColorTextGray
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = FacilityName(facility),
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = if (isSelected) ColorPrimaryOrange else ColorTextBlack
                                        )
                                    }
                                }
                            }
                            repeat(4 - row.size) { Spacer(modifier = Modifier.weight(1f)) }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text("카페 소개", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = ColorTextBlack)
                    Spacer(modifier = Modifier.height(8.dp))
                    AppTextField(
                        value = description,
                        onValueChange = { description = it },
                        placeholder = "카페에 대한 간단한 소개를 입력해주세요",
                        maxLines = 4
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("카페 사진 (최대 5장)", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = ColorTextBlack)
                        Text("$totalImageCount/5", fontSize = 13.sp, color = ColorTextBlack)
                    }
                    Spacer(modifier = Modifier.height(12.dp))

                    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        item {
                            Box(
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .border(BorderStroke(1.dp, ColorBorderLight), RoundedCornerShape(12.dp))
                                    .background(ColorInputBg)
                                    .clickable {
                                        // TODO: Image Picking logic for Multiplatform
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                MaterialSymbol(name = "add", size = 24.sp, tint = ColorTextGray)
                            }
                        }

                        if (isEditMode) {
                            items(serverImageUrls) { imageUrl ->
                                ServerImageItem(
                                    imageUrl = imageUrl,
                                    viewModel = viewModel,
                                    onRemove = { viewModel.removeServerImage(imageUrl) }
                                )
                            }
                        }

                        // Display uploaded but not yet saved images
                        items(uploadedImageUrls) { imageUrl ->
                            ServerImageItem(
                                imageUrl = imageUrl,
                                viewModel = viewModel,
                                onRemove = { viewModel.removeUploadedImage(imageUrl) }
                            )
                        }
                    }

                    if (imageUploadingCount > 0) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = ColorPrimaryOrange
                            )
                            Text(
                                "이미지 업로드 중... ($imageUploadingCount)",
                                fontSize = 12.sp,
                                color = ColorTextGray
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(100.dp))
                }

                // Bottom Bar
                Surface(
                    tonalElevation = 4.dp,
                    shadowElevation = 8.dp,
                    color = ColorWhite,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = { navController.popBackStack() },
                            modifier = Modifier.weight(1f).height(52.dp),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, ColorBorderLight)
                        ) {
                            Text("취소", color = ColorTextBlack, fontSize = 16.sp)
                        }

                        Button(
                            onClick = {
                                var ok = true
                                if (cafeName.isBlank()) { cafeNameError = "카페 이름을 입력해주세요."; ok = false }
                                if (address.isBlank()) { addressError = "주소를 입력해주세요."; ok = false }
                                if (phone.isBlank()) { phoneError = "전화번호를 입력해주세요."; ok = false }
                                else if (phone.length < 9) { phoneError = "올바른 전화번호를 입력해주세요."; ok = false }

                                if (!ok) return@Button

                                val openingHoursStr = if (bulkEditMode) {
                                    "${formatTimeToHHMM(bulkStartTime)}-${formatTimeToHHMM(bulkEndTime)}"
                                } else {
                                    weeklyHours.joinToString(",") { hours ->
                                        if (hours.isClosed) "${hours.dayOfWeek}=Closed"
                                        else "${hours.dayOfWeek}=${formatTimeToHHMM(hours.openTime)}-${formatTimeToHHMM(hours.closeTime)}"
                                    }
                                }

                                if (isEditMode && cafeId != null) {
                                    viewModel.updateCafe(
                                        cafeId,
                                        UpdateCafeRequest(
                                            name = cafeName,
                                            address = address,
                                            phoneNumber = phone,
                                            openingHours = openingHoursStr,
                                            description = description.ifBlank { null },
                                            facilities = selectedFacilities.toList(),
                                            imageUrls = serverImageUrls + uploadedImageUrls
                                        )
                                    )
                                } else {
                                    viewModel.createCafe(
                                        CreateCafeRequest(
                                            name = cafeName,
                                            address = address,
                                            phoneNumber = phone,
                                            openingHours = openingHoursStr,
                                            description = description.ifBlank { null },
                                            facilities = selectedFacilities.toList(),
                                            imageUrls = uploadedImageUrls
                                        )
                                    )
                                }
                            },
                            modifier = Modifier.weight(1f).height(52.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = ColorPrimaryOrange),
                            enabled = !isLoading && imageUploadingCount == 0
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp), color = ColorWhite, strokeWidth = 2.dp)
                            } else {
                                Text(if (isEditMode) "수정 완료" else "등록하기", color = ColorWhite, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            if (showAddressSearch) {
                AddressSearchDialog(
                    onAddressSelected = {
                        address = it
                        showAddressSearch = false
                    },
                    onDismiss = { showAddressSearch = false }
                )
            }

            if (showBulkStartTimePicker) {
                ScrollableTimePickerDialog(bulkStartTime, onConfirm = { bulkStartTime = it; showBulkStartTimePicker = false }, onDismiss = { showBulkStartTimePicker = false })
            }
            if (showBulkEndTimePicker) {
                ScrollableTimePickerDialog(bulkEndTime, onConfirm = { bulkEndTime = it; showBulkEndTimePicker = false }, onDismiss = { showBulkEndTimePicker = false })
            }
            if (showWeeklyTimePicker && selectedDayForTimePicker != null) {
                val dayIndex = weeklyHours.indexOfFirst { it.dayOfWeek == selectedDayForTimePicker!!.name }
                if (dayIndex != -1) {
                    val currentTime = if (isSelectingStartTime) weeklyHours[dayIndex].openTime else weeklyHours[dayIndex].closeTime
                    ScrollableTimePickerDialog(currentTime, onConfirm = { time ->
                        weeklyHours[dayIndex] = if (isSelectingStartTime) weeklyHours[dayIndex].copy(openTime = time) else weeklyHours[dayIndex].copy(closeTime = time)
                        showWeeklyTimePicker = false
                    }, onDismiss = { showWeeklyTimePicker = false })
                }
            }
        }
    }
}

@Composable
fun OperatingHoursSection(
    weeklyHours: SnapshotStateList<DailyOperatingHours>,
    bulkEditMode: Boolean,
    onBulkEditModeChange: (Boolean) -> Unit,
    bulkStartTime: String,
    bulkEndTime: String,
    onBulkStartTimeChange: () -> Unit,
    onBulkEndTimeChange: () -> Unit,
    onShowTimePicker: (DayOfWeek, Boolean) -> Unit,
    onApplyBulkTime: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("운영 시간", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = ColorTextBlack)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(if (bulkEditMode) "일괄 적용" else "요일별 설정", fontSize = 12.sp, color = ColorTextGray)
                Switch(checked = !bulkEditMode, onCheckedChange = { onBulkEditModeChange(!it) }, colors = SwitchDefaults.colors(checkedTrackColor = ColorPrimaryOrange))
            }
        }

        if (bulkEditMode) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                AppTextField(value = formatTimeDisplay(bulkStartTime), onValueChange = {}, placeholder = "시작", leading = { MaterialSymbol("schedule", 18.sp, ColorTextGray) }, readOnly = true, onClick = onBulkStartTimeChange, modifier = Modifier.weight(1f))
                Text("~", fontWeight = FontWeight.Bold)
                AppTextField(value = formatTimeDisplay(bulkEndTime), onValueChange = {}, placeholder = "종료", readOnly = true, onClick = onBulkEndTimeChange, modifier = Modifier.weight(1f))
            }
            Button(onClick = onApplyBulkTime, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = ColorBgBeige, contentColor = ColorPrimaryOrange), shape = RoundedCornerShape(10.dp)) {
                Text("전체 요일에 적용", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                weeklyHours.forEachIndexed { index, hours ->
                    val dayOfWeek = DayOfWeek.valueOf(hours.dayOfWeek)
                    DailyHoursRow(hours, dayOfWeek.displayName, onClosedChange = { weeklyHours[index] = hours.copy(isClosed = it) }, onStartTimeClick = { onShowTimePicker(dayOfWeek, true) }, onEndTimeClick = { onShowTimePicker(dayOfWeek, false) })
                }
            }
        }
    }
}

@Composable
fun DailyHoursRow(dailyHours: DailyOperatingHours, dayDisplayName: String, onClosedChange: (Boolean) -> Unit, onStartTimeClick: () -> Unit, onEndTimeClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(if (dailyHours.isClosed) ColorInputBg else Color(0xFFFFF9F6)).border(BorderStroke(1.dp, ColorBorderLight), RoundedCornerShape(12.dp)).padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.width(100.dp)) {
            Checkbox(checked = dailyHours.isClosed, onCheckedChange = onClosedChange, colors = CheckboxDefaults.colors(checkedColor = ColorPrimaryOrange))
            Text(text = dayDisplayName, fontSize = 14.sp, color = if (dailyHours.isClosed) ColorTextGray else ColorTextBlack)
        }
        if (!dailyHours.isClosed) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Box(modifier = Modifier.weight(1f).clip(RoundedCornerShape(8.dp)).background(ColorWhite).border(BorderStroke(1.dp, ColorBorderLight), RoundedCornerShape(8.dp)).clickable { onStartTimeClick() }.padding(10.dp), contentAlignment = Alignment.Center) {
                    Text(formatTimeDisplay(dailyHours.openTime), fontSize = 13.sp)
                }
                Text("~", color = ColorTextGray)
                Box(modifier = Modifier.weight(1f).clip(RoundedCornerShape(8.dp)).background(ColorWhite).border(BorderStroke(1.dp, ColorBorderLight), RoundedCornerShape(8.dp)).clickable { onEndTimeClick() }.padding(10.dp), contentAlignment = Alignment.Center) {
                    Text(formatTimeDisplay(dailyHours.closeTime), fontSize = 13.sp)
                }
            }
        } else {
            Text("휴무", modifier = Modifier.weight(1f), textAlign = TextAlign.Center, color = ColorTextGray)
        }
    }
}

fun formatTimeDisplay(time: String): String = if (time.length == 4) "${time.substring(0, 2)}:${time.substring(2, 4)}" else time
fun formatTimeToHHMM(time: String): String = if (time.length == 4) "${time.substring(0, 2)}:${time.substring(2, 4)}" else time

@Composable
fun ServerImageItem(imageUrl: String, viewModel: CafeFormViewModel, onRemove: () -> Unit) {
    val imageDataCache by viewModel.imageDataCache.collectAsState()
    var bitmap by remember { mutableStateOf<ImageBitmap?>(null) }

    LaunchedEffect(imageUrl) { viewModel.loadImage(imageUrl) }
    LaunchedEffect(imageDataCache) {
        imageDataCache[imageUrl]?.let { bitmap = it.toImageBitmap() }
    }

    Box(modifier = Modifier.size(72.dp).clip(RoundedCornerShape(12.dp)).background(ColorBgBeige), contentAlignment = Alignment.TopEnd) {
        bitmap?.let { Image(bitmap = it, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop) }
            ?: Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(modifier = Modifier.size(20.dp), color = ColorPrimaryOrange) }
        IconButton(onClick = onRemove, modifier = Modifier.size(24.dp).padding(4.dp).background(Color(0x66000000), CircleShape)) {
            MaterialSymbol("close", 12.sp, ColorWhite)
        }
    }
}



fun FacilityIcon(facility: EFacility) = when (facility) {
    EFacility.WIFI -> "wifi"
    EFacility.PRINTER -> "print"
    EFacility.OUTLET -> "outlet"
    EFacility.OPEN_24H -> "schedule"
    EFacility.CAFE -> "local_cafe"
    EFacility.MEETING_ROOM -> "meeting_room"
    EFacility.LOCKER -> "inventory_2"
    EFacility.AIR_CONDITIONING -> "ac_unit"
    EFacility.PARKING -> "local_parking"
    EFacility.STUDY_ROOM -> "meeting_room"
    EFacility.SMOKING_ROOM -> "smoking_rooms"
    EFacility.LOUNGE -> "weekend"
}

fun FacilityName(facility: EFacility) = when (facility) {
    EFacility.WIFI -> "Wi-Fi"
    EFacility.PRINTER -> "프린터"
    EFacility.OUTLET -> "콘센트"
    EFacility.OPEN_24H -> "24시간"
    EFacility.CAFE -> "카페"
    EFacility.MEETING_ROOM -> "미팅룸"
    EFacility.LOCKER -> "사물함"
    EFacility.AIR_CONDITIONING -> "에어컨"
    EFacility.PARKING -> "주차"
    EFacility.STUDY_ROOM -> "스터디룸"
    EFacility.SMOKING_ROOM -> "흡연실"
    EFacility.LOUNGE -> "라운지"
}

@Composable
private fun AddressSearchDialog(onAddressSelected: (String) -> Unit, onDismiss: () -> Unit) {
    var searchQuery by remember { mutableStateOf("") }
    val samples = listOf("서울특별시 강남구 테헤란로 123", "서울특별시 서초구 서초대로 201", "서울특별시 송파구 올림픽로 200", "경기도 성남시 분당구 판교역로 60")
    Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(0.5f)).clickable { onDismiss() }, contentAlignment = Alignment.Center) {
        Surface(modifier = Modifier.width(320.dp).height(500.dp).clip(RoundedCornerShape(16.dp)).clickable(enabled = false) {}, color = ColorWhite) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("주소 검색", fontWeight = FontWeight.Bold, fontSize = 18.sp); Spacer(Modifier.height(12.dp))
                Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(ColorInputBg).border(BorderStroke(1.dp, ColorBorderLight), RoundedCornerShape(10.dp)).padding(10.dp)) {
                    MaterialSymbol("search", 18.sp, ColorTextGray); BasicTextField(searchQuery, { searchQuery = it }, modifier = Modifier.padding(start = 8.dp))
                }
                LazyColumn(modifier = Modifier.weight(1f).padding(top = 12.dp)) {
                    items(samples.filter { it.contains(searchQuery) }) { addr ->
                        Text(addr, modifier = Modifier.fillMaxWidth().clickable { onAddressSelected(addr) }.padding(12.dp), fontSize = 13.sp)
                    }
                }
                Button(onDismiss, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = ColorBorderLight, contentColor = ColorTextBlack)) { Text("취소") }
            }
        }
    }
}

@Composable
private fun ScrollableTimePickerDialog(initialTime: String, onConfirm: (String) -> Unit, onDismiss: () -> Unit) {
    var h by remember { mutableStateOf(initialTime.take(2).toIntOrNull() ?: 9) }
    var m by remember { mutableStateOf(initialTime.drop(2).toIntOrNull() ?: 0) }
    Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(0.5f)).clickable { onDismiss() }, contentAlignment = Alignment.Center) {
        Surface(modifier = Modifier.width(300.dp).clip(RoundedCornerShape(16.dp)).clickable(enabled = false) {}, color = ColorWhite) {
            Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("시간 선택", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 20.dp)) {
                    Text(h.toString().padStart(2, '0'), fontSize = 32.sp, color = ColorPrimaryOrange, modifier = Modifier.clickable { h = (h + 1) % 24 })
                    Text(" : ", fontSize = 32.sp)
                    Text(m.toString().padStart(2, '0'), fontSize = 32.sp, color = ColorPrimaryOrange, modifier = Modifier.clickable { m = (m + 5) % 60 })
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton({ onDismiss() }, modifier = Modifier.weight(1f)) { Text("취소", color = ColorTextBlack) }
                    Button({ onConfirm(h.toString().padStart(2, '0') + m.toString().padStart(2, '0')) }, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = ColorPrimaryOrange)) { Text("확인", color = ColorWhite) }
                }
            }
        }
    }
}
