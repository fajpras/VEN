package com.ven.app.ui.screens.ai.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ven.app.ui.theme.ElmsSans
import com.ven.app.ui.theme.ThemeMode
import com.ven.app.ui.theme.VexTheme

@Composable
fun ChatBubbleUser(
    text: String,
    modifier: Modifier = Modifier,
) {
    val colors = VexTheme.colors
    val bubbleShape = RoundedCornerShape(
        topStart = 20.dp,
        bottomStart = 20.dp,
        bottomEnd = 20.dp,
        topEnd = 6.dp,
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 56.dp, end = 18.dp),
        contentAlignment = Alignment.CenterEnd,
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = 310.dp)
                .clip(bubbleShape)
                .background(colors.primary)
                .padding(horizontal = 16.dp, vertical = 11.dp),
        ) {
            Text(
                text = text,
                style = TextStyle(
                    fontFamily = ElmsSans,
                    fontWeight = FontWeight.Normal,
                    fontSize = 15.sp,
                    lineHeight = 22.sp,
                ),
                color = Color.White,
                textAlign = TextAlign.Start,
            )
        }
    }
}

@Preview(name = "ChatBubbleUser - Dark")
@Composable
private fun ChatBubbleUserDarkPreview() {
    VexTheme(mode = ThemeMode.Dark) {
        ChatBubbleUser(
            text = "Rekomendasikan pameran virtual 3D yang menarik minggu ini",
        )
    }
}

@Preview(name = "ChatBubbleUser - Light")
@Composable
private fun ChatBubbleUserLightPreview() {
    VexTheme(mode = ThemeMode.Light) {
        ChatBubbleUser(
            text = "Rekomendasikan pameran virtual 3D yang menarik minggu ini",
        )
    }
}
