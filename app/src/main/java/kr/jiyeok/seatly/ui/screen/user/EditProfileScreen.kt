package kr.jiyeok.seatly.ui.screen.user

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import kr.jiyeok.seatly.presentation.viewmodel.EditProfileViewModel
import kr.jiyeok.seatly.ui.component.common.AppTopBar

// 색상 정의
private val ColorPrimaryOrange = Color(0xFFFFA500)
private val ColorTextBlack = Color(0xFF000000)
private val ColorTextGray = Color(0xFF888888)
private val ColorTextDarkGray = Color(0xFF999999)
private val ColorTextLightGray = Color(0xFFA8A8A8)
private val ColorBgBeige = Color(0xFFF8F6F3)
private val ColorBorderLight = Color(0xFFE0E0E0)
private val ColorInputBg = Color(0xFFF5F5F5)
private val ColorWhite = Color(0xFFFFFFFF)
private val ColorBrownBg = Color(0xFFD4C5B9)
private val ColorRedCancel = Color(0xFFFF6B6B)
private val ColorWarning = Color(0xFFFF6B6B)
private val ColorCheckCircle = Color(0xFF4CAF50)

/**
 * EditProfileScreen - 개인정보 수정 화면
 *
 * 기능:
 * - 프로필 사진 변경 (카메라/갤러리)
 * - 이름 수정
 * - 이메일 표시 (수정 불가)
 * - 휴대폰 번호 수정
 * - 비밀번호 변경
 * - 계정 탈퇴 (확인 다이얼로그)
 */
@Composable
fun EditProfileScreen(
    navController: NavController,
    viewModel: EditProfileViewModel = hiltViewModel()
) {
    // ViewModel에서 데이터 가져오기
    val userData by viewModel.userData.collectAsState()

    // 로컬 상태
    var name by remember { mutableStateOf("") }
    var phoneNumber by remember { mutableStateOf("") }
    var profileImageUri by remember { mutableStateOf<Uri?>(null) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showChangePasswordDialog by remember { mutableStateOf(false) }

    // 초기값 설정
    LaunchedEffect(userData) {
        userData?.let {
            name = it.name ?: ""
            phoneNumber = it.phone ?: ""
        }
    }

    // 갤러리 런처
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { profileImageUri = it }
    }

    // 비밀번호 변경 다이얼로그
    if (showChangePasswordDialog) {
        ChangePasswordDialog(
            onConfirm = { currentPassword, newPassword, _ ->
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ColorWhite)
            .verticalScroll(rememberScrollState())
    ) {
        // =====================================================
        // Top Bar - 뒤로가기 + 제목 + 완료 체크 아이콘
        // =====================================================
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
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "완료",
                    tint = ColorPrimaryOrange,
                    modifier = Modifier
                        .size(24.dp)
                        .clickable {
                            viewModel.updateUserProfile(
                                name = name,
                                phoneNumber = phoneNumber,
                                imageUri = profileImageUri
                            )
                            navController.popBackStack()
                        }
                )
            }
        )

        Spacer(modifier = Modifier.height(12.dp))

        // =====================================================
        // Profile Image Section
        // =====================================================
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
                    if (profileImageUri != null) {
                        AsyncImage(
                            model = profileImageUri,
                            contentDescription = "프로필 사진",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else if (!userData?.imageUrl.isNullOrEmpty()) {
                        AsyncImage(
                            model = userData?.imageUrl,
                            contentDescription = "프로필 사진",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }

                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .offset(x = (-6).dp, y = (-6).dp)
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(ColorWhite)
                            .border(width = 2.dp, color = ColorBorderLight, shape = CircleShape)
                            .clickable { galleryLauncher.launch("image/*") },
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

        // =====================================================
        // Name Input
        // =====================================================
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

            TextField(
                value = name,
                onValueChange = { name = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                placeholder = {
                    Text(
                        text = "이름을 입력하세요",
                        color = ColorTextLightGray,
                        fontSize = 14.sp
                    )
                },
                singleLine = true,
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = ColorInputBg,
                    unfocusedContainerColor = ColorInputBg,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    focusedTextColor = ColorTextBlack,
                    unfocusedTextColor = ColorTextBlack
                ),
                shape = RoundedCornerShape(8.dp),
                textStyle = androidx.compose.ui.text.TextStyle(
                    fontSize = 14.sp,
                    color = ColorTextBlack
                )
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // =====================================================
        // Email Display (Read-Only)
        // =====================================================
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "이메일 *",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = ColorTextBlack
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .background(ColorInputBg, RoundedCornerShape(8.dp))
                    .border(1.dp, Color.Transparent, RoundedCornerShape(8.dp))
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
                        .padding(end = 12.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // =====================================================
        // Phone Number Input
        // =====================================================
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "휴대폰 번호 *",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = ColorTextBlack
                )
            }

            TextField(
                value = phoneNumber,
                onValueChange = { phoneNumber = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                placeholder = {
                    Text(
                        text = "010-1234-5678",
                        color = ColorTextLightGray,
                        fontSize = 14.sp
                    )
                },
                singleLine = true,
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = ColorInputBg,
                    unfocusedContainerColor = ColorInputBg,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    focusedTextColor = ColorTextBlack,
                    unfocusedTextColor = ColorTextBlack
                ),
                shape = RoundedCornerShape(8.dp),
                textStyle = androidx.compose.ui.text.TextStyle(
                    fontSize = 14.sp,
                    color = ColorTextBlack
                )
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // =====================================================
        // Password Change Section
        // =====================================================
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .height(48.dp)
                .shadow(
                    elevation = 8.dp,
                    shape = RoundedCornerShape(8.dp),
                    ambientColor = ColorTextBlack.copy(alpha = 0.15f),
                    spotColor = ColorTextBlack.copy(alpha = 0.08f)
                )
                .clip(RoundedCornerShape(8.dp))
                .background(ColorBgBeige)
                .clickable { showChangePasswordDialog = true }
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
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "비밀번호",
                        tint = ColorPrimaryOrange,
                        modifier = Modifier.size(20.dp)
                    )

                    Text(
                        text = "비밀번호 변경",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = ColorTextBlack
                    )
                }

                Icon(
                    imageVector = Icons.Filled.ChevronRight,
                    contentDescription = "이동",
                    tint = ColorTextLightGray,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // =====================================================
        // Delete Account Section
        // =====================================================
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .height(48.dp)
                .shadow(
                    elevation = 8.dp,
                    shape = RoundedCornerShape(8.dp),
                    ambientColor = ColorTextBlack.copy(alpha = 0.15f),
                    spotColor = ColorTextBlack.copy(alpha = 0.08f)
                )
                .clip(RoundedCornerShape(8.dp))
                .background(ColorBgBeige)
                .clickable { showDeleteDialog = true }
                .padding(horizontal = 14.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "계정 탈퇴",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = ColorWarning
                )

                Icon(
                    imageVector = Icons.Filled.ChevronRight,
                    contentDescription = "이동",
                    tint = ColorTextLightGray,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(100.dp))
    }
}

/**
 * ChangePasswordDialog - 비밀번호 변경 다이얼로그
 */
@Composable
private fun ChangePasswordDialog(
    onConfirm: (String, String, String) -> Unit,
    onDismiss: () -> Unit
) {
    var currentPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var showCurrentPassword by remember { mutableStateOf(false) }
    var showNewPassword by remember { mutableStateOf(false) }
    var showConfirmPassword by remember { mutableStateOf(false) }
    var passwordStrength by remember { mutableStateOf(0f) }
    var passwordError by remember { mutableStateOf("") }

    LaunchedEffect(newPassword) {
        if (newPassword.isEmpty()) {
            passwordStrength = 0f
            passwordError = ""
        } else {
            passwordStrength = when {
                newPassword.length < 6 -> 0.3f
                newPassword.length < 8 -> 0.6f
                newPassword.any { it.isUpperCase() } && newPassword.any { it.isDigit() } -> 1f
                else -> 0.8f
            }
            passwordError = when {
                newPassword.length < 6 -> "6자리 이상이고 문자와 숫자 조합만 혼합되면 됩니다."
                confirmPassword.isNotEmpty() && newPassword != confirmPassword -> "비밀번호가 일치하지 않습니다."
                else -> ""
            }
        }
    }
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .background(ColorWhite, RoundedCornerShape(20.dp))
                .padding(24.dp),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "뒤로",
                        tint = ColorTextBlack,
                        modifier = Modifier
                            .size(24.dp)
                            .clickable { onDismiss() }
                    )
                    Text(
                        text = "비밀번호 변경",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = ColorTextBlack,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "현재 비밀번호 *",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = ColorTextBlack
                    )
                    TextField(
                        value = currentPassword,
                        onValueChange = { currentPassword = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        placeholder = {
                            Text(
                                text = "••••••••",
                                color = ColorTextLightGray,
                                fontSize = 14.sp
                            )
                        },
                        visualTransformation = if (showCurrentPassword) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            Icon(
                                imageVector = if (showCurrentPassword) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = "비밀번호 표시",
                                tint = ColorPrimaryOrange,
                                modifier = Modifier
                                    .size(20.dp)
                                    .clickable { showCurrentPassword = !showCurrentPassword }
                            )
                        },
                        singleLine = true,
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = ColorInputBg,
                            unfocusedContainerColor = ColorInputBg,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            focusedTextColor = ColorTextBlack,
                            unfocusedTextColor = ColorTextBlack
                        ),
                        shape = RoundedCornerShape(8.dp),
                        textStyle = androidx.compose.ui.text.TextStyle(
                            fontSize = 14.sp,
                            color = ColorTextBlack
                        )
                    )
                }
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "새 비밀번호 *",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = ColorTextBlack
                    )
                    TextField(
                        value = newPassword,
                        onValueChange = { newPassword = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        placeholder = {
                            Text(
                                text = "••••••••",
                                color = ColorTextLightGray,
                                fontSize = 14.sp
                            )
                        },
                        visualTransformation = if (showNewPassword) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            Icon(
                                imageVector = if (showNewPassword) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = "비밀번호 표시",
                                tint = ColorPrimaryOrange,
                                modifier = Modifier
                                    .size(20.dp)
                                    .clickable { showNewPassword = !showNewPassword }
                            )
                        },
                        singleLine = true,
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = ColorInputBg,
                            unfocusedContainerColor = ColorInputBg,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            focusedTextColor = ColorTextBlack,
                            unfocusedTextColor = ColorTextBlack
                        ),
                        shape = RoundedCornerShape(8.dp),
                        textStyle = androidx.compose.ui.text.TextStyle(
                            fontSize = 14.sp,
                            color = ColorTextBlack
                        )
                    )
                    if (newPassword.isNotEmpty()) {
                        LinearProgressIndicator(
                            progress = passwordStrength,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(4.dp)
                                .clip(RoundedCornerShape(2.dp)),
                            color = ColorPrimaryOrange,
                            trackColor = ColorBorderLight
                        )
                    }
                }
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "새 비밀번호 확인 *",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = ColorTextBlack
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .background(ColorInputBg, RoundedCornerShape(8.dp))
                            .border(
                                width = 2.dp,
                                color = if (passwordError.isNotEmpty() && confirmPassword.isNotEmpty()) ColorWarning else Color.Transparent,
                                shape = RoundedCornerShape(8.dp)
                            )
                            .padding(horizontal = 12.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        TextField(
                            value = confirmPassword,
                            onValueChange = { confirmPassword = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            placeholder = {
                                Text(
                                    text = "••••••••",
                                    color = ColorTextLightGray,
                                    fontSize = 14.sp
                                )
                            },
                            visualTransformation = if (showConfirmPassword) VisualTransformation.None else PasswordVisualTransformation(),
                            trailingIcon = {
                                if (confirmPassword.isNotEmpty()) {
                                    if (newPassword == confirmPassword) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = "일치",
                                            tint = ColorCheckCircle,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    } else {
                                        Icon(
                                            imageVector = Icons.Default.Clear,
                                            contentDescription = "불일치",
                                            tint = ColorWarning,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                } else {
                                    Icon(
                                        imageVector = if (showConfirmPassword) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                        contentDescription = "비밀번호 표시",
                                        tint = ColorPrimaryOrange,
                                        modifier = Modifier
                                            .size(20.dp)
                                            .clickable { showConfirmPassword = !showConfirmPassword }
                                    )
                                }
                            },
                            singleLine = true,
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent,
                                focusedTextColor = ColorTextBlack,
                                unfocusedTextColor = ColorTextBlack
                            ),
                            shape = RoundedCornerShape(0.dp),
                            textStyle = androidx.compose.ui.text.TextStyle(
                                fontSize = 14.sp,
                                color = ColorTextBlack
                            )
                        )
                    }
                    if (passwordError.isNotEmpty()) {
                        Text(
                            text = passwordError,
                            fontSize = 12.sp,
                            color = ColorWarning
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = {
                        if (currentPassword.isNotEmpty() && newPassword.isNotEmpty() && confirmPassword.isNotEmpty() && newPassword == confirmPassword) {
                            onConfirm(currentPassword, newPassword, confirmPassword)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ColorPrimaryOrange
                    ),
                    shape = RoundedCornerShape(12.dp),
                    enabled = currentPassword.isNotEmpty() && newPassword.isNotEmpty() && confirmPassword.isNotEmpty() && newPassword == confirmPassword && passwordError.isEmpty()
                ) {
                    Text(
                        text = "변경 완료",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = ColorWhite
                    )
                }
            }
        }
    }
}

/**
 * DeleteAccountDialog - 계정 탈퇴 확인 다이얼로그
 */
@Composable
private fun DeleteAccountDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
            usePlatformDefaultWidth = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .background(ColorWhite, RoundedCornerShape(24.dp))
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 경고 아이콘
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .background(
                            Color(0xFFFFEEEE),
                            RoundedCornerShape(50.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.WarningAmber,
                        contentDescription = "경고",
                        tint = ColorWarning,
                        modifier = Modifier.size(28.dp)
                    )
                }

                // 제목
                Text(
                    text = "계정을 탈퇴하시겠습니까?",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = ColorTextBlack,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )

                // 설명 텍스트 (빨간색)
                Text(
                    text = "탈퇴 시 모든 정보가 영구적으로 삭제되며\n복구할 수 없습니다.",
                    fontSize = 13.sp,
                    color = ColorWarning,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    lineHeight = 18.sp
                )

                // 재확인 질문
                Text(
                    text = "정말 탈퇴하시겠습니까?",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = ColorTextBlack,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )

                Spacer(modifier = Modifier.height(24.dp))

                // 버튼 영역
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 취소 버튼
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ColorWhite
                        ),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(
                            width = 2.dp,
                            color = ColorWarning
                        )
                    ) {
                        Text(
                            text = "취소",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = ColorWarning
                        )
                    }

                    // 탈퇴 버튼
                    Button(
                        onClick = onConfirm,
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ColorWarning
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "탈퇴",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = ColorWhite
                        )
                    }
                }
            }
        }
    }
}