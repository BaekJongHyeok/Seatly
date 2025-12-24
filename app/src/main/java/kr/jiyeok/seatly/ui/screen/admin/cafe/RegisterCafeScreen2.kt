package kr.jiyeok.seatly.ui.screen.admin.cafe

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kr.jiyeok.seatly.ui.component.MaterialSymbol
import kr.jiyeok.seatly.ui.component.common.AppTextField
import kr.jiyeok.seatly.ui.screen.manager.RegisterCafeTopBar
import androidx.compose.foundation.lazy.grid.items as gridItems

/**
 * RegisterCafeScreen2 (updated to persist mock-registered cafes in a shared in-memory store)
 *
 * - Uses a shared InMemoryCafeStore so that the mock registration performed here is visible
 *   to other screens (e.g., StudyCafeListScreen) that read from the same store.
 * - When a real backend is available you can replace the repository implementation to call the real API.
 */

/** Shared in-memory store for mock data; replace with persistent storage or network-backed repo later. */
object InMemoryCafeStore {
    private val _cafes = mutableStateListOf<StudyCafeListItem>()

    fun getAll(): List<StudyCafeListItem> = _cafes.toList()

    fun addCafe(item: StudyCafeListItem) {
        // prepend to list so newest appears top
        _cafes.add(0, item)
    }

    fun clear() = _cafes.clear()

    // helper to seed sample data for dev if needed
    fun seed(sample: List<StudyCafeListItem>) {
        _cafes.clear()
        _cafes.addAll(sample)
    }
}

/** Lightweight model used for listing */
data class StudyCafeListItem(
    val id: String,
    val title: String,
    val address: String,
    val imageUrl: String?,
    val status: String // "OPEN", "REVIEW", "REJECT"
)

/** Request model for cafe registration */
data class CafeRegistrationRequest(
    val name: String,
    val phone: String,
    val startTime: String,
    val endTime: String,
    val weekdays: List<String>,
    val imageUris: List<String>,
    val address: String,
    val detailAddress: String,
    val facilities: List<String>
)

typealias RepoResult = Pair<Boolean, String?>

interface CafeRepository {
    suspend fun registerCafe(req: CafeRegistrationRequest): RepoResult
}

/** Mock repository that writes to InMemoryCafeStore on success */
class MockCafeRepository : CafeRepository {
    override suspend fun registerCafe(req: CafeRegistrationRequest): RepoResult {
        // simulate latency
        delay(700)
        return if (req.name.contains("fail", ignoreCase = true)) {
            Pair(false, "서버: 등록에 실패했습니다. (mock)")
        } else {
            // create an item and add to in-memory store
            val id = "cafe_${System.currentTimeMillis()}"
            val imageUrl = req.imageUris.firstOrNull()
            val status = "REVIEW" // new registrations under review in mock
            val item = StudyCafeListItem(
                id = id,
                title = req.name,
                address = "${req.address} ${req.detailAddress}".trim(),
                imageUrl = imageUrl,
                status = status
            )
            InMemoryCafeStore.addCafe(item)
            Pair(true, "등록 성공 (mock)")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterCafeScreen2(
    navController: NavController,
    modifier: Modifier = Modifier,
    repository: CafeRepository = MockCafeRepository() // replace with real impl later
) {
    val Primary = Color(0xFFe95321)
    val BackgroundBase = Color(0xFFFFFFFF)
    val InputBg = Color(0xFFF8F8F8)
    val BorderColor = Color(0xFFE8E8E8)
    val TextMain = Color(0xFF1A1A1A)
    val TextSub = Color(0xFF888888)
    val StepBg = Color(0xFFfdede8)

    var postalCode by remember { mutableStateOf("") }
    var addressSelected by remember { mutableStateOf("") }
    var detailAddress by remember { mutableStateOf("") }

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

    var isSubmitting by remember { mutableStateOf(false) }
    var serverError by remember { mutableStateOf<String?>(null) }

    var addressError by remember { mutableStateOf<String?>(null) }

    val scope = rememberCoroutineScope()

    // If the previous screen saved an address in savedStateHandle under reg_address, pick it up
    LaunchedEffect(Unit) {
        val prevHandle = navController.previousBackStackEntry?.savedStateHandle
        prevHandle?.get<String>("reg_address")?.let {
            addressSelected = it
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
                        onClick = {
                            // Build full request from savedStateHandle (first try previous entry where Screen1 saved data)
                            val prevHandle = navController.previousBackStackEntry?.savedStateHandle
                            val currHandle = navController.currentBackStackEntry?.savedStateHandle

                            val name = prevHandle?.get<String>("reg_cafe_name") ?: currHandle?.get<String>("reg_cafe_name") ?: ""
                            val phone = prevHandle?.get<String>("reg_phone") ?: currHandle?.get<String>("reg_phone") ?: ""
                            val start = prevHandle?.get<String>("reg_start") ?: currHandle?.get<String>("reg_start") ?: "09:00"
                            val end = prevHandle?.get<String>("reg_end") ?: currHandle?.get<String>("reg_end") ?: "24:00"
                            val weekdays = prevHandle?.get<List<String>>("reg_weekdays") ?: currHandle?.get<List<String>>("reg_weekdays") ?: listOf("연중 무휴")
                            val images = prevHandle?.get<List<String>>("reg_images") ?: currHandle?.get<List<String>>("reg_images") ?: emptyList()

                            if (name.isBlank()) {
                                serverError = "카페명이 비어있습니다."
                                return@Button
                            }
                            if (addressSelected.isBlank()) {
                                addressError = "주소를 선택해주세요."
                                return@Button
                            } else {
                                addressError = null
                            }

                            val req = CafeRegistrationRequest(
                                name = name,
                                phone = phone,
                                startTime = start,
                                endTime = end,
                                weekdays = weekdays,
                                imageUris = images,
                                address = addressSelected,
                                detailAddress = detailAddress,
                                facilities = selectedFacilities.toList()
                            )

                            scope.launch {
                                isSubmitting = true
                                serverError = null
                                try {
                                    val (success, message) = repository.registerCafe(req)
                                    isSubmitting = false
                                    if (success) {
                                        // after successful registration navigate to cafe list
                                        navController.navigate("cafe_list") {
                                            popUpTo("admin_home") { inclusive = false }
                                        }
                                    } else {
                                        serverError = message ?: "카페 등록에 실패했습니다."
                                    }
                                } catch (e: Exception) {
                                    isSubmitting = false
                                    serverError = e.message ?: "등록 중 오류가 발생했습니다."
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Primary, contentColor = Color.White),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(54.dp),
                        enabled = !isSubmitting
                    ) {
                        Text(if (isSubmitting) "등록 중..." else "등록 완료", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(modifier = Modifier.fillMaxSize()) {
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
                        .background(Color(0xFFF4F4F4))
                )
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 20.dp),
                contentPadding = PaddingValues(bottom = 20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Spacer(modifier = Modifier.height(18.dp))

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

                    Text(
                        text = "위치 및 시설 정보를 확인해주세요",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextMain,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }

                item {
                    Text("주소", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TextMain)
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.weight(1f)) {
                            AppTextField(
                                value = addressSelected,
                                onValueChange = {
                                    addressSelected = it
                                    addressError = null
                                },
                                placeholder = "도로명, 건물명 등을 입력하세요",
                                readOnly = true
                            )
                        }

                        Button(
                            onClick = {
                                // mock address selection
                                addressSelected = "서울시 강남구 테헤란로 123"
                                addressError = null
                            },
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

                    addressError?.let { Text(text = it, color = Color(0xFFFF453A), modifier = Modifier.padding(top = 6.dp), fontSize = 12.sp) }

                    Spacer(modifier = Modifier.height(12.dp))

                    AppTextField(
                        value = detailAddress,
                        onValueChange = { detailAddress = it },
                        placeholder = "상세 주소를 입력해주세요 (예: 2층)"
                    )

                    Spacer(modifier = Modifier.height(12.dp))

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
                    Text("편의시설", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TextMain)
                }

                item {
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
                                        .clickable {
                                            selectedFacilities = if (selected) selectedFacilities - label else selectedFacilities + label
                                        },
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

                item {
                    serverError?.let {
                        Text(text = it, color = Color(0xFFFF453A), modifier = Modifier.padding(vertical = 6.dp))
                    }
                }
            }
        }
    }
}