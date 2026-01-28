package kr.jiyeok.seatly.ui.component.common

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun PhoneTextField(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    leading: (@Composable () -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    isErrorBorder: Boolean = false,
    activeColor: Color = Color(0xFFe95321),
    backgroundColor: Color = Color(0xFFF8F8F8),
    borderColor: Color = Color(0xFFE8E8E8),
    textColor: Color = Color(0xFF1A1A1A),
    placeholderColor: Color = Color(0xFF888888)
) {
    var focused by rememberSaveable { mutableStateOf(false) }

    val actualBorderColor = if (focused || isErrorBorder) activeColor else borderColor

    Row(
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 52.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(backgroundColor)
            .border(BorderStroke(1.dp, actualBorderColor), RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically
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
                singleLine = true,
                textStyle = TextStyle(color = textColor, fontSize = 15.sp),
                cursorBrush = SolidColor(activeColor),
                keyboardOptions = keyboardOptions,
                modifier = Modifier
                    .fillMaxWidth()
                    .onFocusChanged { state -> focused = state.isFocused }
            ) { innerTextField ->
                if (value.text.isEmpty()) {
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
