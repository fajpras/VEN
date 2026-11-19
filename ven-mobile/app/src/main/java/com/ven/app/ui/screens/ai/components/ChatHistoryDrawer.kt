package com.ven.app.ui.screens.ai.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ven.app.R
import com.ven.app.ui.icons.VexIcon
import com.ven.app.ui.icons.VexIcons
import com.ven.app.ui.screens.ai.ChatConversation
import com.ven.app.ui.theme.ElmsSans
import com.ven.app.ui.theme.ThemeMode
import com.ven.app.ui.theme.VexRadius
import com.ven.app.ui.theme.VexSize
import com.ven.app.ui.theme.VexSpace
import com.ven.app.ui.theme.VexTheme

/**
 * Konten Drawer Laci Riwayat Percakapan AI (DESIGN.md §8.12.5).
 * Sesuai wireframe `Wireframe - AI Chat-2.png`:
 * - Lebar laci: 300dp (VexSize.drawerWidth).
 * - Latar Belakang: Warna dasar #1F1F1F (colors.bg).
 * - Baris "New chat": Ikon pensil kotak (VexIcons.NewChat) + teks "New chat".
 * - Label Seksi: "Recents" (colors.textMuted).
 * - Daftar percakapan terdahulu (ChatHistoryItem).
 */
@Composable
fun ChatHistoryDrawer(
    chatHistory: List<ChatConversation>,
    selectedChatId: String?,
    onNewChatClick: () -> Unit,
    onSelectChat: (ChatConversation) -> Unit,
    onDeleteChat: (ChatConversation) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = VexTheme.colors
    var pendingDelete by remember { mutableStateOf<ChatConversation?>(null) }

    Box(
        modifier = modifier
            .width(VexSize.drawerWidth)
            .fillMaxHeight()
            .background(colors.bg)
            .statusBarsPadding(),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
        ) {
            Spacer(modifier = Modifier.height(VexSpace.s4))

            // 1. Aksi Sesi Baru ("New chat")
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = VexSize.minTouch)
                    .clickable(onClick = onNewChatClick)
                    .padding(horizontal = VexSpace.s6),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                VexIcon(
                    id = VexIcons.NewChat,
                    contentDescription = stringResource(R.string.ai_chat_new_chat),
                    tint = colors.text,
                    size = VexSize.icon,
                )

                Spacer(modifier = Modifier.width(VexSpace.s4))

                Text(
                    text = stringResource(R.string.ai_chat_new_chat),
                    style = TextStyle(
                        fontFamily = ElmsSans,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium,
                        color = colors.text,
                    ),
                )
            }

            Spacer(modifier = Modifier.height(VexSpace.s4))

            // 2. Label Seksi "Recents"
            Text(
                text = stringResource(R.string.ai_chat_recents),
                style = TextStyle(
                    fontFamily = ElmsSans,
                    fontSize = 14.sp,
                    color = colors.textMuted,
                ),
                modifier = Modifier.padding(horizontal = VexSpace.s6, vertical = VexSpace.s2),
            )

            // 3. Daftar Riwayat Percakapan
            if (chatHistory.isEmpty()) {
                Text(
                    text = stringResource(R.string.ai_chat_empty_history),
                    style = TextStyle(
                        fontFamily = ElmsSans,
                        fontSize = 14.sp,
                        color = colors.textDisabled,
                    ),
                    modifier = Modifier.padding(horizontal = VexSpace.s6, vertical = VexSpace.s3),
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    items(
                        items = chatHistory,
                        key = { it.id },
                    ) { conversation ->
                        ChatHistoryItem(
                            title = conversation.title,
                            isSelected = conversation.id == selectedChatId,
                            onClick = { onSelectChat(conversation) },
                            onDeleteClick = { pendingDelete = conversation },
                        )
                    }
                }
            }
        }

        pendingDelete?.let { conversation ->
            DeleteConversationDialog(
                onConfirm = {
                    onDeleteChat(conversation)
                    pendingDelete = null
                },
                onDismiss = { pendingDelete = null },
            )
        }
    }
}

@Composable
private fun DeleteConversationDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = VexTheme.colors
    val dialogShape = RoundedCornerShape(VexRadius.xl)

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.border(
            width = 1.dp,
            color = colors.border,
            shape = dialogShape,
        ),
        shape = dialogShape,
        containerColor = colors.surface,
        tonalElevation = 0.dp,
        titleContentColor = colors.text,
        textContentColor = colors.textSecondary,
        title = {
            Text(
                text = stringResource(R.string.ai_chat_delete_dialog_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
        },
        text = {
            Text(
                text = stringResource(R.string.ai_chat_delete_dialog_msg),
                style = MaterialTheme.typography.bodyMedium,
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(
                    text = stringResource(R.string.ai_chat_delete_confirm),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.danger,
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = stringResource(R.string.ai_chat_delete_cancel),
                    style = MaterialTheme.typography.labelLarge,
                    color = colors.text,
                )
            }
        },
    )
}

@Preview(name = "ChatHistoryDrawer - Dark")
@Composable
private fun ChatHistoryDrawerDarkPreview() {
    VexTheme(mode = ThemeMode.Dark) {
        ChatHistoryDrawer(
            chatHistory = emptyList(),
            selectedChatId = null,
            onNewChatClick = {},
            onSelectChat = {},
            onDeleteChat = {},
        )
    }
}

@Preview(name = "ChatHistoryDrawer - Light")
@Composable
private fun ChatHistoryDrawerLightPreview() {
    VexTheme(mode = ThemeMode.Light) {
        ChatHistoryDrawer(
            chatHistory = emptyList(),
            selectedChatId = null,
            onNewChatClick = {},
            onSelectChat = {},
            onDeleteChat = {},
        )
    }
}
