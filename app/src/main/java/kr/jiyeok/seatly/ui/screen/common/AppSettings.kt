package kr.jiyeok.seatly.ui.screen.common

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Language
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavController
import kr.jiyeok.seatly.R

private val ColorPrimaryOrange = Color(0xFFFFA500)
private val ColorTextBlack = Color(0xFF000000)
private val ColorTextGray = Color(0xFF888888)
private val ColorTextLightGray = Color(0xFFA8A8A8)
private val ColorBgBeige = Color(0xFFF8F6F3)
private val ColorBorderLight = Color(0xFFE0E0E0)
private val ColorInputBg = Color(0xFFF5F5F5)
private val ColorWhite = Color(0xFFFFFFFF)
private val ColorText = Color(0xFF666666)
/**
 * AppSettings - 앱 설정 화면
 *
 * 기능:
 * - 테마 설정 (라이트/다크 모드)
 * - 언어 설정 (한국어/영어/일본어 등)
 * - 앱 버전 표시
 */
@Composable
fun AppSettings(
    navController: NavController
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var isDarkMode by remember { mutableStateOf(false) }
    
    // Load saved language
    val savedLanguageCode = remember { 
        kr.jiyeok.seatly.util.LocaleHelper.getLanguage(context) 
    }
    
    // Map code to display name
    val codeToNameMap = mapOf(
        "ko" to "한국어",
        "en" to "English",
        "ja" to "日本語",
        "zh" to "中文"
    )
    val nameToCodeMap = codeToNameMap.entries.associate { (k, v) -> v to k }
    
    var selectedLanguage by remember { mutableStateOf(codeToNameMap[savedLanguageCode] ?: "한국어") }
    var showLanguageDialog by remember { mutableStateOf(false) }
    val languageOptions = codeToNameMap.values.toList()

    if (showLanguageDialog) {
        LanguageSelectionDialog(
            selectedLanguage = selectedLanguage,
            languages = languageOptions,
            onLanguageSelected = { displayName ->
                selectedLanguage = displayName
                showLanguageDialog = false
                
                // Save and Restart
                val code = nameToCodeMap[displayName] ?: "ko"
                kr.jiyeok.seatly.util.LocaleHelper.setLocale(context, code)
                (context as? android.app.Activity)?.recreate()
            },
            onDismiss = { showLanguageDialog = false }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ColorWhite)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(ColorWhite)
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "뒤로가기",
                tint = ColorTextBlack,
                modifier = Modifier
                    .size(24.dp)
                    .clickable { navController.popBackStack() }
                    .align(Alignment.CenterStart)
            )
            Text(
                text = stringResource(R.string.settings_title),
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = ColorTextBlack,
                modifier = Modifier.align(Alignment.Center)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(horizontal = 20.dp)
                .shadow(
                    elevation = 4.dp,
                    shape = RoundedCornerShape(12.dp),
                    ambientColor = ColorTextBlack.copy(alpha = 0.1f),
                    spotColor = ColorTextBlack.copy(alpha = 0.05f)
                )
                .clip(RoundedCornerShape(12.dp))
                .background(ColorBgBeige)
                .padding(horizontal = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.DarkMode,
                        contentDescription = "테마 설정",
                        tint = ColorPrimaryOrange,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = stringResource(R.string.settings_theme),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = ColorTextBlack
                    )
                }

                Switch(
                    checked = isDarkMode,
                    onCheckedChange = { isDarkMode = it },
                    modifier = Modifier.scale(0.8f),
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = ColorPrimaryOrange,
                        checkedTrackColor = ColorPrimaryOrange.copy(alpha = 0.3f),
                        uncheckedThumbColor = ColorBorderLight,
                        uncheckedTrackColor = ColorBorderLight.copy(alpha = 0.5f)
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(horizontal = 20.dp)
                .shadow(
                    elevation = 4.dp,
                    shape = RoundedCornerShape(12.dp),
                    ambientColor = ColorTextBlack.copy(alpha = 0.1f),
                    spotColor = ColorTextBlack.copy(alpha = 0.05f)
                )
                .clip(RoundedCornerShape(12.dp))
                .background(ColorBgBeige)
                .clickable { showLanguageDialog = true }
                .padding(horizontal = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Language,
                        contentDescription = "언어 설정",
                        tint = ColorPrimaryOrange,
                        modifier = Modifier.size(24.dp)
                    )

                    Text(
                        text = stringResource(R.string.settings_language),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = ColorTextBlack
                    )
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = selectedLanguage,
                        fontSize = 13.sp,
                        color = ColorText
                    )

                    Icon(
                        imageVector = Icons.Filled.ChevronRight,
                        contentDescription = "이동",
                        tint = ColorTextLightGray,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
        Spacer(modifier = Modifier.weight(1f))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = stringResource(R.string.settings_version),
                    fontSize = 12.sp,
                    color = ColorTextLightGray
                )

                Text(
                    text = "1.0.0",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = ColorTextBlack
                )
            }
        }
    }
}

/**
 * LanguageSelectionDialog - 언어 선택 다이얼로그
 */
@Composable
private fun LanguageSelectionDialog(
    selectedLanguage: String,
    languages: List<String>,
    onLanguageSelected: (String) -> Unit,
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
                .background(ColorWhite, RoundedCornerShape(20.dp))
                .padding(24.dp),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = stringResource(R.string.settings_language_select_title),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = ColorTextBlack
                )

                Spacer(modifier = Modifier.height(8.dp))

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    languages.forEach { language ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (selectedLanguage == language) ColorPrimaryOrange.copy(alpha = 0.15f)
                                    else ColorInputBg
                                )
                                .clickable {
                                    onLanguageSelected(language)
                                    onDismiss()
                                }
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = language,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = ColorTextBlack
                                )

                                if (selectedLanguage == language) {
                                    Box(
                                        modifier = Modifier
                                            .size(20.dp)
                                            .background(
                                                ColorPrimaryOrange,
                                                RoundedCornerShape(4.dp)
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "✓",
                                            fontSize = 12.sp,
                                            color = ColorWhite,
                                            fontWeight = FontWeight.Bold
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
                        .height(48.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ColorPrimaryOrange
                    ),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(
                        text = stringResource(R.string.settings_close),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = ColorWhite
                    )
                }
            }
        }
    }
}