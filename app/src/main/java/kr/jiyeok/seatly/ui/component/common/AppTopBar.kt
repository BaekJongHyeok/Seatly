package kr.jiyeok.seatly.ui.component.common

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * AppTopBar
 *
 * Common top app bar used across the app.
 *
 * Behavior:
 * - Always reserves the left area for an optional left icon button.
 * - Always reserves the right area for an optional right icon button.
 * - Title text is placed exactly at the horizontal center of the bar (true center).
 * - Keeps vertical padding (top/bottom) so spacing matches typical top app bar.
 */
@Composable
fun AppTopBar(
    title: String,
    modifier: Modifier = Modifier,
    titleFontSize: androidx.compose.ui.unit.TextUnit = 18.sp,
    titleColor: Color = Color.Unspecified,
    titleTextStyle: TextStyle? = null,
    // Optional left button content and click handler
    leftContent: (@Composable (() -> Unit))? = null,
    onLeftClick: (() -> Unit)? = null,
    // Optional right button content and click handler
    rightContent: (@Composable (() -> Unit))? = null,
    onRightClick: (() -> Unit)? = null,
    // sizes for icon buttons (button container size)
    buttonContainerSize: Dp = 40.dp,
    // vertical padding above and below the content (preserves topbar spacing)
    verticalPadding: Dp = 12.dp,
    // minimum height of the bar (keeps consistent visual height)
    minHeight: Dp = 56.dp,
    backgroundColor: Color = MaterialTheme.colorScheme.background
) {
    Surface(color = backgroundColor, tonalElevation = 0.dp) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(minHeight)
                .padding(vertical = verticalPadding)
        ) {
            val finalStyle = titleTextStyle ?: TextStyle(
                color = if (titleColor == Color.Unspecified) MaterialTheme.colorScheme.onBackground else titleColor,
                fontSize = titleFontSize,
            )

            // Centered title: placed at true center regardless of left/right content
            Text(
                text = title,
                style = finalStyle,
                modifier = Modifier.align(Alignment.Center)
            )

            // Left IconButton (if provided)
            if (leftContent != null) {
                IconButton(
                    onClick = { onLeftClick?.invoke() },
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .padding(start = 4.dp)
                        .height(buttonContainerSize)
                        .then(Modifier)
                ) {
                    leftContent()
                }
            }

            // Right IconButton (if provided)
            if (rightContent != null) {
                IconButton(
                    onClick = { onRightClick?.invoke() },
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = 4.dp)
                        .height(buttonContainerSize)
                        .then(Modifier)
                ) {
                    rightContent()
                }
            }
        }
    }
}