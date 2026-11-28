package com.ven.app.ui.screens.explore

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ven.app.R
import com.ven.app.ui.components.feature.PostCard
import com.ven.app.ui.components.layout.VexTopBar
import com.ven.app.ui.components.ui.VexBottomSheet
import com.ven.app.ui.icons.VexIcon
import com.ven.app.ui.icons.VexIcons
import com.ven.app.ui.theme.ThemeMode
import com.ven.app.ui.theme.VexRadius
import com.ven.app.ui.theme.VexSpace
import com.ven.app.ui.theme.VexTheme

/**
 * Model satu item feed pada [PostsFeedScreen].
 */
data class PostsFeedItem(
    val id: String,
    val username: String,
    val userId: String? = null,
    val userAvatarUrl: String? = null,
    val title: String = "",
    val category: String? = null,
    val timestampText: String? = null,
    val description: String = "",
    val imageUrl: String? = null,
    val isVideo: Boolean = false,
    val pameranId: String? = null,
    val aspectRatio: Float = 4f / 5f,
)

/**
 * Layar detail feed post (scrollable) untuk VEN (Virtual Exhibition).
 *
 * Sesuai DESIGN.md §8.5.5 & wireframe `Wireframe - Post.png`:
 * - Top bar "Posts" yang terpaku (pinned), tidak ikut scroll.
 * - Body: `LazyColumn` vertikal penuh berisi kartu [PostCard] yang dapat di-scroll.
 * - Bila [initialPostId] diberikan, feed otomatis scroll ke item tersebut.
 * - Bila [onEditPostClick] atau [onDeletePostClick] disediakan (milik sendiri),
 *   menampilkan tombol opsi di header kartu & bottom sheet aksi.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostsFeedScreen(
    posts: List<PostsFeedItem>,
    onBackClick: () -> Unit,
    onRequestMeetingClick: (postId: String) -> Unit,
    onView3dClick: (postId: String) -> Unit,
    modifier: Modifier = Modifier,
    initialPostId: String? = null,
    isRefreshing: Boolean = false,
    onRefresh: () -> Unit = {},
    onUserClick: ((userId: String) -> Unit)? = null,
    onEditPostClick: ((postId: String) -> Unit)? = null,
    onDeletePostClick: ((postId: String) -> Unit)? = null,
) {
    val colors = VexTheme.colors
    val listState = rememberLazyListState()
    var actionTargetPost by remember { mutableStateOf<PostsFeedItem?>(null) }
    var deleteConfirmPost by remember { mutableStateOf<PostsFeedItem?>(null) }

    val activeVideoPostId by remember(listState, posts) {
        derivedStateOf {
            val videoIds = posts.asSequence().filter { it.isVideo }.map { it.id }.toSet()
            if (videoIds.isEmpty()) return@derivedStateOf null

            val layoutInfo = listState.layoutInfo
            val viewportCenter = (layoutInfo.viewportStartOffset + layoutInfo.viewportEndOffset) / 2
            layoutInfo.visibleItemsInfo
                .asSequence()
                .filter { it.key is String && (it.key as String) in videoIds }
                .minByOrNull { item ->
                    val itemCenter = item.offset + item.size / 2
                    kotlin.math.abs(itemCenter - viewportCenter)
                }
                ?.key as? String
        }
    }

    // Auto-scroll ke post yang diketuk dari grid
    LaunchedEffect(initialPostId, posts) {
        if (!initialPostId.isNullOrBlank()) {
            val index = posts.indexOfFirst { it.id == initialPostId }
            if (index >= 0) {
                listState.scrollToItem(index)
            }
        }
    }

    val hasOwnerActions = onEditPostClick != null || onDeletePostClick != null

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = colors.bg,
        topBar = {
            VexTopBar.Back(
                title = stringResource(R.string.explore_posts_title),
                onBackClick = onBackClick,
            )
        },
    ) { innerPadding ->
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = onRefresh,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = VexSpace.s6),
            ) {
                itemsIndexed(
                    items = posts,
                    key = { _, post -> post.id },
                ) { _, post ->
                    PostCard(
                        username = post.username,
                        userAvatarUrl = post.userAvatarUrl,
                        title = post.title,
                        category = post.category,
                        timestampText = post.timestampText,
                        description = post.description,
                        imageUrl = post.imageUrl,
                        isVideo = post.isVideo,
                        isVideoPlaying = post.isVideo && post.id == activeVideoPostId,
                        aspectRatio = post.aspectRatio,
                        onRequestMeetingClick = { onRequestMeetingClick(post.id) },
                        onView3dClick = { onView3dClick(post.pameranId ?: post.id) },
                        onUsernameClick = {
                            post.userId?.let { onUserClick?.invoke(it) }
                        },
                        onMoreClick = if (hasOwnerActions) {
                            { actionTargetPost = post }
                        } else {
                            null
                        },
                    )
                }
            }
        }
    }

    actionTargetPost?.let { post ->
        VexBottomSheet(
            onDismissRequest = { actionTargetPost = null },
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = VexSpace.s6)
                    .navigationBarsPadding(),
            ) {
                Text(
                    text = post.title.ifBlank { stringResource(R.string.post_more_options) },
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = colors.text,
                    modifier = Modifier.padding(horizontal = VexSpace.s6, vertical = VexSpace.s3),
                )

                if (onEditPostClick != null) {
                    PostActionSheetRow(
                        iconRes = VexIcons.Edit,
                        iconTint = colors.primary,
                        iconBgColor = colors.primary.copy(alpha = 0.10f),
                        label = stringResource(R.string.post_action_edit),
                        labelColor = colors.text,
                        onClick = {
                            actionTargetPost = null
                            onEditPostClick(post.id)
                        },
                    )
                }

                if (onDeletePostClick != null) {
                    PostActionSheetRow(
                        iconRes = VexIcons.Close,
                        iconTint = colors.danger,
                        iconBgColor = colors.danger.copy(alpha = 0.12f),
                        label = stringResource(R.string.post_action_delete),
                        labelColor = colors.danger,
                        onClick = {
                            actionTargetPost = null
                            deleteConfirmPost = post
                        },
                    )
                }
            }
        }
    }

    deleteConfirmPost?.let { post ->
        AlertDialog(
            onDismissRequest = { deleteConfirmPost = null },
            containerColor = colors.surface,
            title = {
                Text(
                    text = stringResource(R.string.post_delete_confirm_title),
                    color = colors.text,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                )
            },
            text = {
                Text(
                    text = stringResource(R.string.post_delete_confirm_body),
                    color = colors.textSecondary,
                    style = MaterialTheme.typography.bodyMedium,
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        deleteConfirmPost = null
                        onDeletePostClick?.invoke(post.id)
                    },
                ) {
                    Text(
                        text = stringResource(R.string.post_action_delete),
                        color = colors.danger,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteConfirmPost = null }) {
                    Text(
                        text = stringResource(R.string.common_cancel),
                        color = colors.textMuted,
                    )
                }
            },
        )
    }
}

@Composable
private fun PostActionSheetRow(
    iconRes: Int,
    iconTint: Color,
    iconBgColor: Color,
    label: String,
    labelColor: Color,
    onClick: () -> Unit,
) {
    val colors = VexTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(color = colors.text.copy(alpha = 0.08f)),
                role = Role.Button,
                onClick = onClick,
            )
            .padding(horizontal = VexSpace.s6, vertical = VexSpace.s3),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(VexRadius.md))
                .background(iconBgColor),
            contentAlignment = Alignment.Center,
        ) {
            VexIcon(
                id = iconRes,
                contentDescription = null,
                tint = iconTint,
                size = 20.dp,
            )
        }
        Spacer(modifier = Modifier.width(VexSpace.s4))
        Text(
            text = label,
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
            color = labelColor,
            modifier = Modifier.weight(1f),
        )
    }
}

// ---------------------------------------------------------
// Previews
// ---------------------------------------------------------

internal val dummyPostsFeed = listOf(
    PostsFeedItem(
        id = "feed_1",
        username = "Graaph",
        title = "Cyberpunk Low Poly Mech Robot 3D",
        category = "3D",
        timestampText = "1 day ago",
        description = "Peletakan kalimat ini akan diisi dengan deskripsi yang akan menjelaskan dengan lebih rinci lagi dari apa yang ingin disampaikan oleh kreator.",
    ),
    PostsFeedItem(
        id = "feed_2",
        username = "Shaf.One",
        title = "Fintech Neo-Glass Mobile Banking",
        category = "Software",
        timestampText = "2 days ago",
        description = "Eksplorasi desain antarmuka aplikasi mobile dengan pendekatan minimalis dan tipografi tebal. Kunjungi https://vex.art/exhibit untuk detail lengkap.",
    ),
)

@Preview(name = "PostsFeedScreen - Dark Mode", showBackground = true, backgroundColor = 0xFF1F1F1F)
@Composable
private fun PostsFeedScreenDarkPreview() {
    VexTheme(mode = ThemeMode.Dark) {
        PostsFeedScreen(
            posts = dummyPostsFeed,
            onBackClick = {},
            onRequestMeetingClick = {},
            onView3dClick = {},
        )
    }
}

@Preview(name = "PostsFeedScreen - Light Mode", showBackground = true, backgroundColor = 0xFFFFFFFF)
@Composable
private fun PostsFeedScreenLightPreview() {
    VexTheme(mode = ThemeMode.Light) {
        PostsFeedScreen(
            posts = dummyPostsFeed,
            onBackClick = {},
            onRequestMeetingClick = {},
            onView3dClick = {},
        )
    }
}
