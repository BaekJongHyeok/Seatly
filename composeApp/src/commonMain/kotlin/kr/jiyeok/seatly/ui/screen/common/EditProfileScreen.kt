package kr.jiyeok.seatly.ui.screen.common

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.navigation.NavController
import kr.jiyeok.seatly.presentation.viewmodel.EditProfileViewModel
import kr.jiyeok.seatly.ui.component.common.AppTopBar
import kr.jiyeok.seatly.util.formatKoreanPhoneFromDigits
import kr.jiyeok.seatly.ui.theme.*
import org.koin.compose.viewmodel.koinViewModel
import org.jetbrains.compose.resources.painterResource
import seatly.composeapp.generated.resources.Res
import seatly.composeapp.generated.resources.img_default_cafe
import androidx.compose.foundation.Image
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.graphics.ImageBitmap
import kr.jiyeok.seatly.ui.component.common.PhoneTextField
import kr.jiyeok.seatly.ui.component.common.AppTextField

@Composable
fun EditProfileScreen(
    navController: NavController,
    viewModel: EditProfileViewModel = koinViewModel()
) {
    val snackbarHostState = remember { SnackbarHostState() }

    // 사용자 정보
    val userData by viewModel.userData.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val isAccountDeleted by viewModel.deleteAccountSuccess.collectAsState()
    val isPwChanged by viewModel.changePasswordSuccess.collectAsState()
    val updateSuccess by viewModel.updateSuccess.collectAsState()

    // 로컬 상태
    var name by remember { mutableStateOf("") }
    var phoneNumber by remember { mutableStateOf("") }
    var phoneDisplay by remember { mutableStateOf(TextFieldValue("")) }
    // Multiplatform image handling: instead of Uri, we use name and content
    var selectedFileName by remember { mutableStateOf<String?>(null) }
    var selectedImageContent by remember { mutableStateOf<ByteArray?>(null) }
    
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showChangePasswordDialog by remember { mutableStateOf(false) }

    // 원본 데이터 저장 (변경 감지용)
    var originalName by remember { mutableStateOf("") }
    var originalPhone by remember { mutableStateOf("") }
    var originalImageUrl by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        viewModel.loadUserProfile()
    }

    // 초기값 설정
    LaunchedEffect(userData) {
        userData?.let {
            name = it.name ?: ""
            phoneNumber = it.phone ?: ""
            val formatted = formatKoreanPhoneFromDigits(it.phone ?: "")
            phoneDisplay = TextFieldValue(formatted, TextRange(formatted.length))
            originalName = it.name ?: ""
            originalPhone = it.phone ?: ""
            originalImageUrl = it.imageUrl
        }
    }

    // 업데이트 성공 시 뒤로가기
    LaunchedEffect(updateSuccess) {
        if (updateSuccess) {
            viewModel.resetUpdateSuccess()
            navController.popBackStack()
        }
    }

    // 계정 삭제 성공 시 로그인 화면으로
    LaunchedEffect(isAccountDeleted) {
        if (isAccountDeleted) {
            // 로그인 화면으로 이동 + 백스택 클리어
            navController.navigate("auth/login") {
                popUpTo(0) { inclusive = true }
            }
        }
    }

    // 이벤트 수집 (스낵바)
    LaunchedEffect(Unit) {
        viewModel.events.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    // 변경 감지
    val hasChanges = remember(name, phoneDisplay, selectedImageContent) {
        name != originalName ||
                phoneDisplay.text.filter { it.isDigit() } != originalPhone ||
                selectedImageContent != null
    }

    // 비밀번호 변경 다이얼로그
    if (showChangePasswordDialog) {
        ChangePasswordDialog(
            onConfirm = { currentPassword, newPassword ->
                showChangePasswordDialog = false
                viewModel.changePassword(currentPassword, newPassword)
            },
            onDismiss = {
                showChangePasswordDialog = false
            }
        )
    }

    // 계정 탈퇴 확인 다이얼로그
    if (showDeleteDialog) {
        DeleteAccountDialog(
            onConfirm = {
                showDeleteDialog = false
                viewModel.deleteAccount()
            },
            onDismiss = {
                showDeleteDialog = false
            }
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = ColorWhite
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
            ) {
                AppTopBar(
                    title = "개인정보 수정",
                    leftContent = {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "뒤로가기",
                            tint = ColorTextBlack,
                            modifier = Modifier
                                .size(24.dp)
                                .clickable { navController.popBackStack() }
                        )
                    },
                    rightContent = {
                        if (hasChanges) {
                            Text(
                                text = "저장",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = ColorPrimaryOrange,
                                modifier = Modifier.clickable {
                                    if (!isLoading) {
                                        viewModel.updateUserProfile(
                                            name = name,
                                            phoneNumber = phoneNumber,
                                            fileName = selectedFileName,
                                            content = selectedImageContent
                                        )
                                    }
                                }
                            )
                        }
                    }
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Profile Image Section
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .shadow(
                            elevation = 8.dp,
                            shape = RoundedCornerShape(16.dp),
                            ambientColor = ColorTextBlack.copy(alpha = 0.15f),
                            spotColor = ColorTextBlack.copy(alpha = 0.08f)
                        )
                        .clip(RoundedCornerShape(16.dp))
                        .background(ColorBgBeige)
                        .padding(vertical = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(120.dp)
                                .clip(CircleShape)
                                .background(ColorBrownBg),
                            contentAlignment = Alignment.Center
                        ) {
                            // TODO: Add image display for commonMain (cached/bitmap)
                            Image(
                                painter = painterResource(Res.drawable.img_default_cafe),
                                contentDescription = "프로필 사진",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )

                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .offset(x = (-6).dp, y = (-6).dp)
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(ColorWhite)
                                    .border(width = 2.dp, color = ColorBorderLight, shape = CircleShape)
                                    .clickable { 
                                        // TODO: Image Picking logic for Multiplatform
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CameraAlt,
                                    contentDescription = "사진 변경",
                                    tint = ColorPrimaryOrange,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        Text(
                            text = "프로필 사진 변경",
                            fontSize = 12.sp,
                            color = ColorTextLightGray
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Name Input
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                ) {
                    Text(
                        text = "이름 *",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = ColorTextBlack,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    AppTextField(
                        value = name,
                        onValueChange = { name = it },
                        placeholder = "이름을 입력하세요"
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Email Display (Read-Only)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                ) {
                    Text(
                        text = "이메일 *",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = ColorTextBlack,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .background(ColorInputBg, RoundedCornerShape(8.dp))
                            .padding(horizontal = 12.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Text(
                            text = userData?.email ?: "email@example.com",
                            fontSize = 14.sp,
                            color = ColorTextDarkGray
                        )
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "잠금",
                            tint = ColorTextLightGray,
                            modifier = Modifier
                                .size(18.dp)
                                .align(Alignment.CenterEnd)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Phone Number Input
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                ) {
                    Text(
                        text = "휴대폰 번호 *",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = ColorTextBlack,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    PhoneTextField(
                        value = phoneDisplay,
                        onValueChange = { newValue ->
                            val digitsOnly = newValue.text.filter { it.isDigit() }
                            if (digitsOnly.length <= 11) {
                                phoneNumber = digitsOnly
                                val formatted = formatKoreanPhoneFromDigits(digitsOnly)
                                phoneDisplay = TextFieldValue(
                                    text = formatted,
                                    selection = TextRange(formatted.length)
                                )
                            }
                        },
                        placeholder = "010-1234-5678",
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Password Change Section
                SettingsRow(
                    icon = Icons.Default.Lock,
                    label = "비밀번호 변경",
                    onClick = { showChangePasswordDialog = true }
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Delete Account Section
                SettingsRow(
                    label = "계정 탈퇴",
                    labelColor = ColorWarning,
                    onClick = { showDeleteDialog = true }
                )

                Spacer(modifier = Modifier.height(100.dp))
            }

            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.3f)),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = ColorPrimaryOrange)
                }
            }
        }
    }
}

@Composable
private fun SettingsRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    label: String,
    labelColor: Color = ColorTextBlack,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .height(48.dp)
            .shadow(elevation = 8.dp, shape = RoundedCornerShape(8.dp))
            .clip(RoundedCornerShape(8.dp))
            .background(ColorBgBeige)
            .clickable { onClick() }
            .padding(horizontal = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (icon != null) {
                    Icon(imageVector = icon, contentDescription = label, tint = ColorPrimaryOrange, modifier = Modifier.size(20.dp))
                }
                Text(text = label, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = labelColor)
            }
            Icon(imageVector = Icons.Default.ChevronRight, contentDescription = "이동", tint = ColorTextLightGray, modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
private fun ChangePasswordDialog(
    onConfirm: (String, String) -> Unit,
    onDismiss: () -> Unit
) {
    var currentPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var showCurrentPassword by remember { mutableStateOf(false) }
    var showNewPassword by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .background(ColorWhite, RoundedCornerShape(20.dp))
                .padding(24.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("비밀번호 변경", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                
                AppTextField(
                    value = currentPassword,
                    onValueChange = { currentPassword = it },
                    placeholder = "현재 비밀번호",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
                )

                AppTextField(
                    value = newPassword,
                    onValueChange = { newPassword = it },
                    placeholder = "새 비밀번호",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
                )

                AppTextField(
                    value = confirmPassword,
                    onValueChange = { confirmPassword = it },
                    placeholder = "비밀번호 확인",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
                )

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) { Text("취소") }
                    Button(
                        onClick = { onConfirm(currentPassword, newPassword) },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = ColorPrimaryOrange)
                    ) { Text("확인", color = ColorWhite) }
                }
            }
        }
    }
}

@Composable
private fun DeleteAccountDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .background(ColorWhite, RoundedCornerShape(20.dp))
                .padding(24.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("계정 탈퇴", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Text("정말 탈퇴하시겠습니까?\n모든 데이터가 삭제되며 복구할 수 없습니다.", textAlign = TextAlign.Center, fontSize = 14.sp)
                
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) { Text("취소") }
                    Button(
                        onClick = onConfirm,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = ColorWarning)
                    ) { Text("탈퇴", color = ColorWhite) }
                }
            }
        }
    }
}
