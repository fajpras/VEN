package com.ven.app.ui.screens.ai.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ven.app.R
import com.ven.app.ui.theme.ElmsSans
import com.ven.app.ui.theme.ThemeMode
import com.ven.app.ui.theme.VexTheme

@Composable
private fun AiAvatarBadge(modifier: Modifier = Modifier) {
    val colors = VexTheme.colors
    Box(
        modifier = modifier
            .size(28.dp)
            .clip(CircleShape)
            .background(colors.primary.copy(alpha = 0.12f)),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = painterResource(R.drawable.ic_nav_ai_filled),
            contentDescription = null,
            modifier = Modifier.size(15.dp),
            colorFilter = ColorFilter.tint(colors.primary),
        )
    }
}

@Composable
fun ChatMessageAi(
    text: String,
    modifier: Modifier = Modifier,
    onItemClick: ((String) -> Unit)? = null,
) {
    val colors = VexTheme.colors

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 18.dp, end = 24.dp),
        verticalAlignment = Alignment.Top,
    ) {
        AiAvatarBadge(modifier = Modifier.padding(top = 2.dp))

        Spacer(modifier = Modifier.width(12.dp))

        Text(
            text = text,
            style = TextStyle(
                fontFamily = ElmsSans,
                fontWeight = FontWeight.Normal,
                fontSize = 15.sp,
                lineHeight = 23.sp,
            ),
            color = colors.text,
            textAlign = TextAlign.Start,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
fun AiTypingIndicator(
    modifier: Modifier = Modifier,
) {
    val colors = VexTheme.colors
    val transition = rememberInfiniteTransition(label = "AiTypingAnimation")

    val dot1Alpha by transition.animateFloat(
        initialValue = 0.25f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(550, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "dot1Alpha",
    )

    val dot2Alpha by transition.animateFloat(
        initialValue = 0.25f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(550, delayMillis = 180, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "dot2Alpha",
    )

    val dot3Alpha by transition.animateFloat(
        initialValue = 0.25f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(550, delayMillis = 360, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "dot3Alpha",
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 18.dp, end = 24.dp, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AiAvatarBadge()

        Spacer(modifier = Modifier.width(12.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(5.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            listOf(dot1Alpha, dot2Alpha, dot3Alpha).forEach { alpha ->
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .alpha(alpha)
                        .clip(CircleShape)
                        .background(colors.primary),
                )
            }
        }
    }
}

@Preview(name = "ChatMessageAi - Dark")
@Composable
private fun ChatMessageAiDarkPreview() {
    VexTheme(mode = ThemeMode.Dark) {
        ChatMessageAi(
            text = "Halo! Saya asisten AI resmi VEN. Ada yang bisa saya bantu seputar karya atau pameran di aplikasi ini?",
        )
    }
}

@Preview(name = "ChatMessageAi - Light")
@Composable
private fun ChatMessageAiLightPreview() {
    VexTheme(mode = ThemeMode.Light) {
        ChatMessageAi(
            text = "Halo! Saya asisten AI resmi VEN. Ada yang bisa saya bantu seputar karya atau pameran di aplikasi ini?",
        )
    }
}

@Preview(name = "AiTypingIndicator - Light")
@Composable
private fun AiTypingIndicatorLightPreview() {
    VexTheme(mode = ThemeMode.Light) {
        AiTypingIndicator()
    }
}
