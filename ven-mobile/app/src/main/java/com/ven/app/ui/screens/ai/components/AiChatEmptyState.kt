package com.ven.app.ui.screens.ai.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
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

private val defaultSuggestions = listOf(
    "Rekomendasi pameran populer",
    "Cari karya seni 3D terbaik",
    "Cara membuat pameran virtual",
)

@Composable
fun AiChatEmptyState(
    inputText: String,
    onInputChange: (String) -> Unit,
    onSend: () -> Unit,
    modifier: Modifier = Modifier,
    onSuggestionClick: (String) -> Unit = { prompt ->
        onInputChange(prompt)
        onSend()
    },
) {
    val colors = VexTheme.colors

    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Konten utama di tengah vertikal
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            // Emblem AI minimalis dengan aksen ungu halus
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                colors.primary.copy(alpha = 0.16f),
                                colors.primary.copy(alpha = 0.06f),
                            ),
                        ),
                    )
                    .border(
                        width = 1.dp,
                        color = colors.primary.copy(alpha = 0.22f),
                        shape = CircleShape,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Image(
                    painter = painterResource(R.drawable.ic_nav_ai_filled),
                    contentDescription = stringResource(R.string.ai_chat_sparkle_desc),
                    modifier = Modifier.size(30.dp),
                    colorFilter = ColorFilter.tint(colors.primary),
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            Text(
                text = stringResource(R.string.ai_chat_title),
                style = TextStyle(
                    fontFamily = ElmsSans,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 22.sp,
                    color = colors.text,
                    textAlign = TextAlign.Center,
                ),
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Tanyakan seputar karya, pameran, atau ruang virtual 3D di VEN",
                style = TextStyle(
                    fontFamily = ElmsSans,
                    fontWeight = FontWeight.Normal,
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    color = colors.textMuted,
                    textAlign = TextAlign.Center,
                ),
            )

            Spacer(modifier = Modifier.height(28.dp))

            // Quick prompt chips minimalis
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                defaultSuggestions.forEach { suggestion ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(colors.surfaceInput.copy(alpha = 0.45f))
                            .border(
                                width = 1.dp,
                                color = colors.border.copy(alpha = 0.6f),
                                shape = RoundedCornerShape(14.dp),
                            )
                            .clickable { onSuggestionClick(suggestion) }
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                    ) {
                        Text(
                            text = suggestion,
                            style = TextStyle(
                                fontFamily = ElmsSans,
                                fontWeight = FontWeight.Normal,
                                fontSize = 14.sp,
                                color = colors.text,
                            ),
                        )
                    }
                }
            }
        }

        // Input bar tetap di bawah (konsisten dengan state percakapan)
        AiChatInput(
            value = inputText,
            onValueChange = onInputChange,
            onSend = onSend,
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .padding(bottom = 12.dp),
        )
    }
}

@Preview(name = "AiChatEmptyState - Dark")
@Composable
private fun AiChatEmptyStateDarkPreview() {
    VexTheme(mode = ThemeMode.Dark) {
        AiChatEmptyState(
            inputText = "",
            onInputChange = {},
            onSend = {},
        )
    }
}

@Preview(name = "AiChatEmptyState - Light")
@Composable
private fun AiChatEmptyStateLightPreview() {
    VexTheme(mode = ThemeMode.Light) {
        AiChatEmptyState(
            inputText = "",
            onInputChange = {},
            onSend = {},
        )
    }
}
