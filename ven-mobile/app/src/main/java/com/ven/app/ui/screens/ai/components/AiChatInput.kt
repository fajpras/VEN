package com.ven.app.ui.screens.ai.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ven.app.R
import com.ven.app.ui.icons.VexIcon
import com.ven.app.ui.icons.VexIcons
import com.ven.app.ui.theme.ElmsSans
import com.ven.app.ui.theme.ThemeMode
import com.ven.app.ui.theme.VexTheme

@Composable
fun AiChatInput(
    value: String,
    onValueChange: (String) -> Unit,
    onSend: () -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = stringResource(R.string.ai_chat_placeholder),
    enabled: Boolean = true,
) {
    val colors = VexTheme.colors
    val hasText = value.isNotBlank() && enabled
    val shape = RoundedCornerShape(26.dp)

    val borderColor by animateColorAsState(
        targetValue = if (hasText) colors.primary.copy(alpha = 0.55f) else colors.border.copy(alpha = 0.7f),
        animationSpec = tween(180),
        label = "inputBorderColor",
    )

    val sendBgColor by animateColorAsState(
        targetValue = if (hasText) colors.primary else colors.border.copy(alpha = 0.45f),
        animationSpec = tween(180),
        label = "sendBgColor",
    )

    val sendTintColor by animateColorAsState(
        targetValue = if (hasText) Color.White else colors.textMuted,
        animationSpec = tween(180),
        label = "sendTintColor",
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 52.dp)
            .clip(shape)
            .background(colors.surfaceInput.copy(alpha = 0.65f))
            .border(width = 1.dp, color = borderColor, shape = shape)
            .padding(start = 18.dp, end = 8.dp, top = 7.dp, bottom = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.weight(1f),
            enabled = enabled,
            maxLines = 4,
            textStyle = TextStyle(
                fontFamily = ElmsSans,
                fontSize = 15.sp,
                lineHeight = 21.sp,
                color = colors.text,
            ),
            cursorBrush = SolidColor(colors.primary),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
            keyboardActions = KeyboardActions(
                onSend = {
                    if (hasText) onSend()
                },
            ),
            decorationBox = { innerTextField ->
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    if (value.isEmpty()) {
                        Text(
                            text = placeholder,
                            style = TextStyle(
                                fontFamily = ElmsSans,
                                fontSize = 15.sp,
                                color = colors.textMuted,
                            ),
                        )
                    }
                    innerTextField()
                }
            },
        )

        Spacer(modifier = Modifier.width(8.dp))

        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(sendBgColor)
                .clickable(enabled = hasText, onClick = onSend),
            contentAlignment = Alignment.Center,
        ) {
            VexIcon(
                id = VexIcons.Send,
                contentDescription = stringResource(R.string.ai_chat_send_desc),
                tint = sendTintColor,
                size = 16.dp,
            )
        }
    }
}

@Preview(name = "AiChatInput - Empty - Light")
@Composable
private fun AiChatInputEmptyLightPreview() {
    VexTheme(mode = ThemeMode.Light) {
        AiChatInput(
            value = "",
            onValueChange = {},
            onSend = {},
            modifier = Modifier.padding(16.dp),
        )
    }
}

@Preview(name = "AiChatInput - Filled - Dark")
@Composable
private fun AiChatInputFilledDarkPreview() {
    VexTheme(mode = ThemeMode.Dark) {
        AiChatInput(
            value = "Rekomendasikan karya 3D...",
            onValueChange = {},
            onSend = {},
            modifier = Modifier.padding(16.dp),
        )
    }
}
