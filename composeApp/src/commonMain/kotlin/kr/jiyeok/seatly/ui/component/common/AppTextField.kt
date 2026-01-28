package kr.jiyeok.seatly.ui.component.common

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Reusable AppTextField used across project.
 */
@Composable
fun AppTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String = "",
    modifier: Modifier = Modifier,
    leading: (@Composable () -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    readOnly: Boolean = false,
    onClick: (() -> Unit)? = null,
    highlight: Boolean = false,
    isErrorBorder: Boolean = false,
    height: Dp = 52.dp,
    maxLines: Int = 1,
    backgroundColor: Color = Color(0xFFF8F8F8),
    borderColor: Color = Color(0xFFE8E8E8),
    activeBorderColor: Color = Color(0xFFe95321),
    textColor: Color = Color(0xFF1A1A1A),
    placeholderColor: Color = Color(0xFF888888)
) {
    var focused by rememberSaveable { mutableStateOf(false) }
    val currentOnClick by rememberUpdatedState(onClick)

    val actualBorderColor = when {
        isErrorBorder -> Color(0xFFFF453A)
        focused || highlight -> activeBorderColor
        else -> borderColor
    }

    val baseModifier = modifier
        .fillMaxWidth()
        .height(height)
        .clip(RoundedCornerShape(12.dp))
        .background(backgroundColor, RoundedCornerShape(12.dp))
        .border(BorderStroke(1.dp, actualBorderColor), RoundedCornerShape(12.dp))
        .padding(horizontal = 12.dp)

    if (readOnly && currentOnClick != null) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = baseModifier.clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) { currentOnClick?.invoke() }
        ) {
            if (leading != null) {
                Box(modifier = Modifier.padding(end = 8.dp)) { leading() }
            }
            Box(modifier = Modifier.weight(1f)) {
                if (value.isEmpty()) {
                    Text(text = placeholder, color = placeholderColor, fontSize = 15.sp)
                } else {
                    Text(text = value, color = textColor, fontSize = 15.sp)
                }
            }
            if (trailing != null) {
                Box(modifier = Modifier.padding(start = 8.dp)) { trailing() }
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
                    singleLine = maxLines == 1,
                    maxLines = maxLines,
                    textStyle = TextStyle(color = textColor, fontSize = 15.sp),
                    cursorBrush = SolidColor(activeBorderColor),
                    keyboardOptions = keyboardOptions,
                    modifier = Modifier
                        .fillMaxWidth()
                        .onFocusChanged { state -> focused = state.isFocused }
                ) { innerTextField ->
                    if (value.isEmpty()) {
                        Text(text = placeholder, color = placeholderColor, fontSize = 15.sp)
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
