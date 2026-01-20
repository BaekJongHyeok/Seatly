package kr.jiyeok.seatly.ui.screen.admin.cafe

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
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
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import androidx.exifinterface.media.ExifInterface
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kr.jiyeok.seatly.data.remote.enums.EFacility
import kr.jiyeok.seatly.data.remote.request.CreateCafeRequest
import kr.jiyeok.seatly.data.remote.request.UpdateCafeRequest
import kr.jiyeok.seatly.presentation.viewmodel.CafeFormViewModel
import kr.jiyeok.seatly.ui.component.MaterialSymbol
import kr.jiyeok.seatly.ui.screen.manager.RegisterCafeTopBar
import kr.jiyeok.seatly.ui.theme.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.ByteArrayOutputStream
import java.io.File
import kotlin.math.min
import kotlin.math.sqrt

fun rotateBitmap(bitmap: Bitmap, degrees: Float): Bitmap {
    val matrix = Matrix()
    matrix.postRotate(degrees)
    return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
}

/**
 * 카페 생성/수정 통합 화면
 * @param cafeId null이면 생성 모드, 값이 있으면 수정 모드
 */
@Composable
fun CafeFormScreen(
    navController: NavController,
    cafeId: Long? = null,
    viewModel: CafeFormViewModel = hiltViewModel(),
) {
    val isEditMode = (cafeId != null)

    // Form state
    var cafeName by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var phoneDisplay by remember { mutableStateOf("") }
    var startTime by remember { mutableStateOf("0900") }
    var endTime by remember { mutableStateOf("2400") }
    var description by remember { mutableStateOf("") }
    var showStartTimePicker by remember { mutableStateOf(false) }
    var showEndTimePicker by remember { mutableStateOf(false) }
    var showAddressSearch by remember { mutableStateOf(false) }
    val holidays = listOf("월", "화", "수", "목", "금", "토", "일", "명절", "연중무휴")
    var selectedHolidays by remember { mutableStateOf(setOf("연중무휴")) }
    var showAllHolidays by remember { mutableStateOf(false) }
    val selectedFacilities = remember { mutableStateListOf<EFacility>() }
    val images = remember { mutableStateListOf<Uri>() }
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    val coroutineScope = rememberCoroutineScope()

    // ViewModel states
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val cafeData by viewModel.cafeInfo.collectAsState()
    val updateCafeSuccess by viewModel.updateCafeSuccess.collectAsState()
    val serverImageUrls by viewModel.serverImageUrls.collectAsState()
    val uploadedImageUrls by viewModel.uploadedImageUrls.collectAsState()
    val imageUploadingCount by viewModel.imageUploadingCount.collectAsState()

    // 전체 이미지 개수 계산
    val totalImageCount = serverImageUrls.size + images.size

    // 이미지 압축 함수
    fun compressImage(uri: Uri): File? {
        try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return null
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeStream(inputStream, null, options)
            inputStream.close()

            val maxSize = 1920
            var scale = 1
            while (options.outWidth / scale > maxSize || options.outHeight / scale > maxSize) {
                scale *= 2
            }

            val actualInputStream = context.contentResolver.openInputStream(uri) ?: return null
            val decodingOptions = BitmapFactory.Options().apply {
                inSampleSize = scale
            }
            var bitmap = BitmapFactory.decodeStream(actualInputStream, null, decodingOptions)
            actualInputStream.close()
            if (bitmap == null) return null

            // EXIF 회전 처리
            val exifInputStream = context.contentResolver.openInputStream(uri)
            val exif = exifInputStream?.let { ExifInterface(it) }
            exifInputStream?.close()
            val orientation = exif?.getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL
            ) ?: ExifInterface.ORIENTATION_NORMAL
            bitmap = when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> rotateBitmap(bitmap, 90f)
                ExifInterface.ORIENTATION_ROTATE_180 -> rotateBitmap(bitmap, 180f)
                ExifInterface.ORIENTATION_ROTATE_270 -> rotateBitmap(bitmap, 270f)
                else -> bitmap
            }

            // 압축
            var quality = 90
            var outputStream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)

            while (outputStream.size() > 1048576 && quality > 10) {
                outputStream.reset()
                quality -= 10
                bitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
            }

            if (outputStream.size() > 1048576) {
                val ratio = sqrt(1048576.0 / outputStream.size())
                val newWidth = (bitmap.width * ratio).toInt()
                val newHeight = (bitmap.height * ratio).toInt()
                bitmap = Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
                outputStream.reset()
                bitmap.compress(Bitmap.CompressFormat.JPEG, 85, outputStream)
            }

            val timeStamp = System.currentTimeMillis()
            val uniqueId = java.util.UUID.randomUUID().toString().substring(0, 8)
            val file = File(context.cacheDir, "cafe_${timeStamp}_${uniqueId}.jpg")
            file.outputStream().use { fileOut ->
                fileOut.write(outputStream.toByteArray())
            }

            bitmap.recycle()
            return file
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    // Gallery launcher
    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        uris.forEach { uri ->
            if (totalImageCount < 5) {
                images.add(uri)
                coroutineScope.launch(Dispatchers.IO) {
                    var compressedFile: File? = null
                    try {
                        compressedFile = compressImage(uri)
                        if (compressedFile != null) {
                            val requestFile = compressedFile.asRequestBody("image/jpeg".toMediaTypeOrNull())
                            val body = MultipartBody.Part.createFormData("file", compressedFile.name, requestFile)
                            val fileToDelete = compressedFile
                            viewModel.uploadImage(body) {
                                try {
                                    fileToDelete.delete()
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            }
                        } else {
                            withContext(Dispatchers.Main) {
                                Toast.makeText(context, "이미지 처리 실패", Toast.LENGTH_SHORT).show()
                                images.remove(uri)
                            }
                        }
                    } catch (e: Exception) {
                        compressedFile?.delete()
                        withContext(Dispatchers.Main) {
                            Toast.makeText(context, "이미지 업로드 실패: ${e.message}", Toast.LENGTH_SHORT).show()
                            images.remove(uri)
                        }
                    }
                }
            } else {
                Toast.makeText(context, "최대 5장까지 등록 가능합니다", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Error states
    var cafeNameError by remember { mutableStateOf<String?>(null) }
    var addressError by remember { mutableStateOf<String?>(null) }
    var phoneError by remember { mutableStateOf<String?>(null) }

    // 수정 모드일 때 기존 데이터 로드
    LaunchedEffect(cafeId) {
        if (isEditMode && cafeId != null) {
            viewModel.loadCafeDetailInfo(cafeId)
        }
    }

    // 기존 데이터로 폼 초기화
    LaunchedEffect(cafeData) {
        cafeData?.let { data ->
            cafeName = data.name
            address = data.address
            phone = data.phone.toString()
            phoneDisplay = data.phone.toString()
            description = data.description ?: ""
            val hours = data.openingHours?.split("-")
            if (hours?.size == 2) {
                startTime = hours[0].trim()
                endTime = hours[1].trim()
            }
            selectedFacilities.clear()
            selectedFacilities.addAll(data.facilities)
        }
    }

    LaunchedEffect(updateCafeSuccess) {
        if (updateCafeSuccess) {
            Toast.makeText(context, "카페 정보가 수정되었습니다", Toast.LENGTH_SHORT).show()
            navController.popBackStack()
        }
    }

    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
        }
    }

    fun formatKoreanPhoneFromDigits(digits: String): String {
        if (digits.isEmpty()) return ""
        return if (digits.startsWith("02")) {
            when (digits.length) {
                in 0..2 -> digits
                in 3..5 -> "${digits.substring(0, 2)}-${digits.substring(2)}"
                in 6..9 -> "${digits.substring(0, 2)}-${digits.substring(2, digits.length - 4)}-${digits.takeLast(4)}"
                else -> "${digits.substring(0, 2)}-${digits.substring(2, 6)}-${digits.substring(6, min(10, digits.length))}"
            }
        } else {
            when (digits.length) {
                in 0..3 -> digits
                in 4..7 -> "${digits.substring(0, 3)}-${digits.substring(3)}"
                in 8..11 -> "${digits.substring(0, 3)}-${digits.substring(3, 7)}-${digits.substring(7)}"
                else -> "${digits.substring(0, 3)}-${digits.substring(3, 7)}-${digits.substring(7, 11)}"
            }
        }
    }

    Surface(modifier = Modifier.fillMaxSize(), color = ColorWhite) {
        Box(modifier = Modifier.fillMaxSize()) {
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
                }

                Spacer(modifier = Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(ColorBorderLight)
                )

                // Body - scrollable
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(scrollState)
                        .padding(start = 20.dp, end = 20.dp, top = 18.dp, bottom = 20.dp)
                ) {
                    // Step indicator
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

                    // 1. Cafe Name
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
                        Text(text = it, color = ColorWarning, fontSize = 12.sp, modifier = Modifier.padding(top = 6.dp))
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // 2. Address
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
                        Text(text = it, color = ColorWarning, fontSize = 12.sp, modifier = Modifier.padding(top = 6.dp))
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // 3. Phone
                    Text("전화번호", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = ColorTextBlack)
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
                        leading = { MaterialSymbol(name = "phone", size = 18.sp, tint = ColorTextGray) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone, imeAction = ImeAction.Next),
                        isErrorBorder = phoneError != null
                    )
                    phoneError?.let {
                        Text(text = it, color = ColorWarning, fontSize = 12.sp, modifier = Modifier.padding(top = 6.dp))
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // 4. Opening Hours
                    Text("운영 시간", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = ColorTextBlack)
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
                            leading = { MaterialSymbol(name = "schedule", size = 18.sp, tint = ColorTextGray) },
                            readOnly = true,
                            onClick = { showStartTimePicker = true },
                            modifier = Modifier.weight(1f)
                        )
                        Text("~", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = ColorTextBlack)
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
                        Text("휴무일", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = ColorTextBlack)
                        Button(
                            onClick = { showAllHolidays = !showAllHolidays },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.Transparent,
                                contentColor = ColorPrimaryOrange
                            ),
                            modifier = Modifier
                                .defaultMinSize(minWidth = 0.dp, minHeight = 0.dp)
                                .rotate(if (showAllHolidays) 180f else 0f),
                            contentPadding = PaddingValues(0.dp)
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
                                                        .clip(RoundedCornerShape(8.dp))
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
                            val holidayRows: List<List<String>> = unselectedHolidays.chunked(3)

                            holidayRows.forEach { row ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    row.forEach { holiday ->
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .defaultMinSize(minHeight = 40.dp)
                                                .clip(RoundedCornerShape(20.dp))
                                                .background(ColorInputBg)
                                                .border(
                                                    BorderStroke(1.dp, ColorBorderLight),
                                                    RoundedCornerShape(20.dp)
                                                )
                                                .clickable(
                                                    indication = null,
                                                    interactionSource = remember { MutableInteractionSource() }
                                                ) {
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
                                                }
                                                .padding(horizontal = 12.dp, vertical = 8.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = holiday,
                                                color = ColorTextBlack,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Normal
                                            )
                                        }
                                    }
                                    repeat(3 - row.size) {
                                        Spacer(modifier = Modifier.weight(1f))
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // 6. Facilities
                    Text("편의시설", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = ColorTextBlack)
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
                                    Column(
                                        modifier = Modifier.fillMaxSize(),
                                        verticalArrangement = Arrangement.Center,
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
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
                            repeat(4 - row.size) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // 7. Description
                    Text("카페 소개", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = ColorTextBlack)
                    Spacer(modifier = Modifier.height(8.dp))
                    AppTextField(
                        value = description,
                        onValueChange = { description = it },
                        placeholder = "카페에 대한 간단한 소개를 입력해주세요",
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Default),
                        maxLines = 4
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    // 8. Photos
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
                        // 사진 추가 버튼
                        item {
                            Box(
                                modifier = Modifier
                                    .width(72.dp)
                                    .height(72.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .border(BorderStroke(1.dp, ColorBorderLight), RoundedCornerShape(12.dp))
                                    .background(ColorInputBg)
                                    .clickable {
                                        if (totalImageCount < 5) {
                                            galleryLauncher.launch("image/*")
                                        } else {
                                            Toast
                                                .makeText(context, "최대 5장까지 등록 가능합니다", Toast.LENGTH_SHORT)
                                                .show()
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                MaterialSymbol(name = "add", size = 24.sp, tint = ColorTextGray)
                            }
                        }

                        // 서버에서 로드한 기존 이미지들 (수정 모드일 때만)
                        if (isEditMode) {
                            items(serverImageUrls) { imageUrl ->
                                ServerImageItem(
                                    imageUrl = imageUrl,
                                    viewModel = viewModel,
                                    onRemove = {
                                        viewModel.removeServerImage(imageUrl)
                                    }
                                )
                            }
                        }

                        // 새로 선택한 이미지들
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
                                    Image(
                                        bitmap = bmp,
                                        contentDescription = null,
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
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

                    // 이미지 업로드 상태 표시
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
                                "이미지 압축 및 업로드 중... ($imageUploadingCount)",
                                fontSize = 12.sp,
                                color = ColorTextGray
                            )
                        }
                    }
                }

                // Bottom fixed bar
                Surface(
                    tonalElevation = 4.dp,
                    shadowElevation = 4.dp,
                    color = ColorWhite,
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
                            border = BorderStroke(1.dp, ColorBorderLight),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = ColorTextBlack,
                                containerColor = ColorWhite
                            ),
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
                                    cafeNameError = "카페 이름을 입력해주세요."
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
                                } else if (phone.length < 9) {
                                    phoneError = "올바른 전화번호를 입력해주세요."
                                    ok = false
                                } else {
                                    phoneError = null
                                }

                                if (!ok) return@Button

                                val openingHoursStr = "$startTime-$endTime"

                                if (isEditMode && cafeId != null) {
                                    val request = UpdateCafeRequest(
                                        name = cafeName,
                                        address = address,
                                        phoneNumber = phoneDisplay,
                                        openingHours = openingHoursStr,
                                        description = if (description.isBlank()) null else description,
                                        facilities = selectedFacilities.toList(),
                                        imageUrls = emptyList() // ViewModel에서 자동으로 처리
                                    )
                                    viewModel.updateCafe(cafeId, request)
                                } else {
                                    val request = CreateCafeRequest(
                                        name = cafeName,
                                        address = address,
                                        phoneNumber = phoneDisplay,
                                        openingHours = openingHoursStr,
                                        description = if (description.isBlank()) null else description,
                                        facilities = selectedFacilities.toList(),
                                        imageUrls = emptyList() // ViewModel에서 자동으로 처리
                                    )
                                    viewModel.createCafe(request)
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = ColorPrimaryOrange,
                                contentColor = ColorWhite
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .height(52.dp),
                            shape = RoundedCornerShape(12.dp),
                            enabled = !isLoading && imageUploadingCount == 0
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = ColorWhite,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Text(
                                    if (isEditMode) "수정 완료" else "등록하기",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            // Dialogs
            if (showStartTimePicker) {
                ScrollableTimePickerDialog(
                    initialTime = startTime,
                    onConfirm = { time ->
                        startTime = time
                        showStartTimePicker = false
                    },
                    onDismiss = { showStartTimePicker = false }
                )
            }

            if (showEndTimePicker) {
                ScrollableTimePickerDialog(
                    initialTime = endTime,
                    onConfirm = { time ->
                        endTime = time
                        showEndTimePicker = false
                    },
                    onDismiss = { showEndTimePicker = false }
                )
            }

            if (showAddressSearch) {
                AddressSearchDialog(
                    onAddressSelected = { selectedAddress ->
                        address = selectedAddress
                        showAddressSearch = false
                    },
                    onDismiss = { showAddressSearch = false }
                )
            }
        }
    }
}

// 서버 이미지를 로드하는 Composable
@Composable
fun ServerImageItem(
    imageUrl: String,
    viewModel: CafeFormViewModel,
    onRemove: () -> Unit
) {
    val imageDataCache by viewModel.imageDataCache.collectAsState()
    var bitmap by remember { mutableStateOf<ImageBitmap?>(null) }

    // 이미지 로드 트리거
    LaunchedEffect(imageUrl) {
        viewModel.loadImage(imageUrl)
    }

    // ByteArray를 Bitmap으로 변환
    LaunchedEffect(imageDataCache) {
        val imageData = imageDataCache[imageUrl]
        if (imageData != null) {
            withContext(Dispatchers.IO) {
                try {
                    val bmp = BitmapFactory.decodeByteArray(imageData, 0, imageData.size)
                    bitmap = bmp?.asImageBitmap()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .width(72.dp)
            .height(72.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(ColorBgBeige),
        contentAlignment = Alignment.TopEnd
    ) {
        // Bitmap 표시
        if (bitmap != null) {
            Image(
                bitmap = bitmap!!,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            // 로딩 중 표시
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                    color = ColorPrimaryOrange
                )
            }
        }

        Box(
            modifier = Modifier
                .size(24.dp)
                .padding(4.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0x66000000))
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) { onRemove() },
            contentAlignment = Alignment.Center
        ) {
            MaterialSymbol(name = "close", size = 12.sp, tint = ColorWhite)
        }
    }
}

@Composable
fun rememberBitmapForUri(uri: Uri): ImageBitmap? {
    var bitmap by remember(uri) { mutableStateOf<ImageBitmap?>(null) }
    val context = LocalContext.current
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
        .background(ColorInputBg, RoundedCornerShape(12.dp))
        .border(
            BorderStroke(
                1.dp,
                if (focused || isErrorBorder) ColorPrimaryOrange else ColorBorderLight
            ),
            RoundedCornerShape(12.dp)
        )

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
                if (value.isEmpty()) {
                    Text(text = placeholder, color = ColorTextGray, fontSize = 15.sp)
                } else {
                    Text(text = value, color = ColorTextBlack, fontSize = 15.sp)
                }
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

            Box(
                modifier = Modifier
                    .weight(1f)
                    .defaultMinSize(minHeight = 52.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    singleLine = maxLines == 1,
                    textStyle = TextStyle(color = ColorTextBlack, fontSize = 15.sp),
                    cursorBrush = SolidColor(ColorPrimaryOrange),
                    keyboardOptions = keyboardOptions,
                    modifier = Modifier
                        .fillMaxWidth()
                        .onFocusChanged { state -> focused = state.isFocused },
                    decorationBox = { innerTextField ->
                        if (value.isEmpty()) {
                            Text(text = placeholder, color = ColorTextGray, fontSize = 15.sp)
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
        EFacility.MEETING_ROOM -> "미팅룸"
        EFacility.LOCKER -> "사물함"
        EFacility.AIR_CONDITIONING -> "에어컨"
    }
}

@Composable
private fun AddressSearchDialog(
    onAddressSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var searchResults by remember { mutableStateOf(listOf<String>()) }

    val sampleAddresses = listOf(
        "서울특별시 강남구 테헤란로 123",
        "서울특별시 서초구 서초대로 201",
        "서울특별시 송파구 올림픽로 200",
        "경기도 성남시 분당구 판교역로 60",
        "경기도 수원시 영통구 광교로 535",
        "인천광역시 연수구 송도과학로 166",
        "부산광역시 해운대구 해운대해변로 125",
        "대전광역시 유성구 대학로 78"
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
                ) { },
            color = ColorWhite
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
                    color = ColorTextBlack,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 48.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(ColorInputBg, RoundedCornerShape(10.dp))
                        .border(BorderStroke(1.dp, ColorBorderLight), RoundedCornerShape(10.dp))
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    MaterialSymbol(name = "search", size = 18.sp, tint = ColorTextGray)
                    Spacer(modifier = Modifier.width(8.dp))
                    BasicTextField(
                        value = searchQuery,
                        onValueChange = { query ->
                            searchQuery = query
                            searchResults = sampleAddresses.filter { it.contains(query, ignoreCase = true) }
                        },
                        singleLine = true,
                        textStyle = TextStyle(color = ColorTextBlack, fontSize = 14.sp),
                        cursorBrush = SolidColor(ColorPrimaryOrange),
                        modifier = Modifier
                            .weight(1f)
                            .defaultMinSize(minHeight = 24.dp),
                        decorationBox = { innerTextField ->
                            if (searchQuery.isEmpty()) {
                                Text("주소를 입력하세요", color = ColorTextGray, fontSize = 14.sp)
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
                                    .background(ColorBgBeige)
                                    .border(BorderStroke(1.dp, ColorBorderLight), RoundedCornerShape(8.dp))
                                    .clickable { onAddressSelected(address) }
                                    .padding(12.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    MaterialSymbol(name = "location_on", size = 18.sp, tint = ColorPrimaryOrange)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = address,
                                        color = ColorTextBlack,
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
                                    Text(text = "검색 결과가 없습니다", color = ColorTextGray, fontSize = 13.sp)
                                }
                            }
                        } else {
                            items(searchResults) { address ->
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(ColorBgBeige)
                                        .border(BorderStroke(1.dp, ColorBorderLight), RoundedCornerShape(8.dp))
                                        .clickable { onAddressSelected(address) }
                                        .padding(12.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        MaterialSymbol(name = "location_on", size = 18.sp, tint = ColorPrimaryOrange)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = address,
                                            color = ColorTextBlack,
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
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ColorBorderLight,
                        contentColor = ColorTextBlack
                    ),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("취소", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun ScrollableTimePickerDialog(
    initialTime: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
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
                ) { },
            color = ColorWhite
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
                    color = ColorTextBlack
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Hour picker
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
                                    color = if (hour == selectedHour) ColorPrimaryOrange else ColorTextGray,
                                    modifier = Modifier
                                        .padding(8.dp)
                                        .clickable { selectedHour = hour }
                                )
                            }
                            Spacer(modifier = Modifier.height(40.dp))
                        }
                    }

                    Text(":", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = ColorTextBlack)

                    // Minute picker
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
                                    color = if (minute == selectedMinute) ColorPrimaryOrange else ColorTextGray,
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
                        border = BorderStroke(1.dp, ColorBorderLight)
                    ) {
                        Text("취소", color = ColorTextBlack)
                    }

                    Button(
                        onClick = {
                            val timeStr = "${selectedHour.toString().padStart(2, '0')}${selectedMinute.toString().padStart(2, '0')}"
                            onConfirm(timeStr)
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = ColorPrimaryOrange)
                    ) {
                        Text("확인", color = ColorWhite, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
