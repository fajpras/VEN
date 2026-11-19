package com.ven.app.ui.screens.ai.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp
import com.ven.app.R
import com.ven.app.ui.icons.VexIcon
import com.ven.app.ui.icons.VexIcons
import com.ven.app.ui.theme.ElmsSans
import com.ven.app.ui.theme.ThemeMode
import com.ven.app.ui.theme.VexSize
import com.ven.app.ui.theme.VexSpace
import com.ven.app.ui.theme.VexTheme

@Composable
fun ChatHistoryItem(
    title: String,
    onClick: () -> Unit,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier,
    isSelected: Boolean = false,
) {
    val colors = VexTheme.colors
    var isMenuOpen by remember { mutableStateOf(false) }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = VexSize.minTouch)
            .clickable(onClick = onClick)
            .padding(start = VexSpace.s6, end = VexSpace.s2),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            modifier = Modifier.weight(1f),
            style = TextStyle(
                fontFamily = ElmsSans,
                fontSize = 16.sp,
                color = if (isSelected) colors.primaryText else colors.text,
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )

        Box {
            IconButton(
                onClick = { isMenuOpen = true },
                modifier = Modifier.size(VexSize.minTouch),
            ) {
                VexIcon(
                    id = VexIcons.MoreVert,
                    contentDescription = stringResource(R.string.ai_chat_item_menu_desc),
                    tint = colors.textSecondary,
                    size = VexSize.iconSm,
                )
            }

            DropdownMenu(
                expanded = isMenuOpen,
                onDismissRequest = { isMenuOpen = false },
                containerColor = colors.surface,
            ) {
                DropdownMenuItem(
                    text = {
                        Text(
                            text = stringResource(R.string.ai_chat_delete_action),
                            style = TextStyle(
                                fontFamily = ElmsSans,
                                fontSize = 16.sp,
                                color = colors.danger,
                            ),
                        )
                    },
                    onClick = {
                        isMenuOpen = false
                        onDeleteClick()
                    },
                )
            }
        }
    }
}

@Preview(name = "ChatHistoryItem - Normal")
@Composable
private fun ChatHistoryItemNormalPreview() {
    VexTheme(mode = ThemeMode.Dark) {
        ChatHistoryItem(
            title = "Rekomendasi karya terkeren",
            onClick = {},
            onDeleteClick = {},
            isSelected = false,
        )
    }
}

@Preview(name = "ChatHistoryItem - Selected")
@Composable
private fun ChatHistoryItemSelectedPreview() {
    VexTheme(mode = ThemeMode.Dark) {
        ChatHistoryItem(
            title = "Pameran yang sesuai dengan kategori desain grafis",
            onClick = {},
            onDeleteClick = {},
            isSelected = true,
        )
    }
}
