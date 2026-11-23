package com.ven.app.ui.screens.ai.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ven.app.R
import com.ven.app.ui.icons.VexIcon
import com.ven.app.ui.icons.VexIcons
import com.ven.app.ui.icons.VexLogo
import com.ven.app.ui.theme.ThemeMode
import com.ven.app.ui.theme.VexSize
import com.ven.app.ui.theme.VexSpace
import com.ven.app.ui.theme.VexTheme

@Composable
fun AiChatTopBar(
    onMenuClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = VexTheme.colors

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(colors.bg)
            .statusBarsPadding(),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(VexSize.topBar),
            contentAlignment = Alignment.Center,
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = VexSpace.s4),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(
                    onClick = onMenuClick,
                    modifier = Modifier.size(VexSize.minTouch),
                ) {
                    VexIcon(
                        id = VexIcons.Menu,
                        contentDescription = stringResource(R.string.ai_chat_menu_desc),
                        tint = colors.text,
                        size = 22.dp,
                    )
                }

                VexLogo(
                    modifier = Modifier
                        .padding(end = VexSpace.s2)
                        .height(22.dp),
                )
            }
        }

        HorizontalDivider(
            thickness = 0.5.dp,
            color = colors.border.copy(alpha = 0.55f),
        )
    }
}

@Preview(name = "AiChatTopBar - Dark Mode")
@Composable
private fun AiChatTopBarDarkPreview() {
    VexTheme(mode = ThemeMode.Dark) {
        AiChatTopBar(onMenuClick = {})
    }
}

@Preview(name = "AiChatTopBar - Light Mode")
@Composable
private fun AiChatTopBarLightPreview() {
    VexTheme(mode = ThemeMode.Light) {
        AiChatTopBar(onMenuClick = {})
    }
}
