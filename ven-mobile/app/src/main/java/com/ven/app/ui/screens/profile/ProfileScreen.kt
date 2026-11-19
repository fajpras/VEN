package com.ven.app.ui.screens.profile

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ven.app.R
import com.ven.app.ui.components.feature.MediaGridItem
import com.ven.app.ui.components.feature.MediaGridTile
import com.ven.app.ui.components.layout.AppScaffold
import com.ven.app.ui.components.layout.VexTopBar
import com.ven.app.ui.components.ui.Avatar
import com.ven.app.ui.components.ui.AvatarSize
import com.ven.app.ui.components.ui.VexBottomSheet
import com.ven.app.ui.components.ui.VexCounterChip
import com.ven.app.ui.components.ui.VexIconTabs
import com.ven.app.ui.components.ui.VexSkillChip
import com.ven.app.ui.icons.VexIcon
import com.ven.app.ui.icons.VexIcons
import com.ven.app.ui.navigation.BottomNavTab
import com.ven.app.ui.theme.ThemeMode
import com.ven.app.ui.theme.VexAspect
import com.ven.app.ui.theme.VexSpace
import com.ven.app.ui.theme.VexTheme

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    uiState: ProfileUiState,
    onTabSelected: (BottomNavTab) -> Unit,
    onCreateClick: () -> Unit,
    onMenuClick: () -> Unit,
    onProfileTabChange: (ProfileTab) -> Unit,
    onSkillSheetToggle: (Boolean) -> Unit,
    onMediaClick: (MediaGridItem) -> Unit,
    modifier: Modifier = Modifier,
    onEditProfileClick: () -> Unit = {},
    onRetryClick: () -> Unit = {},
) {
    val colors = VexTheme.colors
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val profile = uiState.profile

    AppScaffold(
        currentTab = BottomNavTab.Profile,
        onTabSelected = onTabSelected,
        modifier = modifier,
        topBar = {
            VexTopBar.Profile(
                username = profile?.username.orEmpty(),
                onCreateClick = onCreateClick,
                onMenuClick = onMenuClick,
            )
        },
    ) { innerPadding ->
        when {
            uiState.isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(color = colors.primary)
                }
            }
            uiState.errorMessage != null -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(horizontal = VexSpace.s6),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(
                        text = stringResource(R.string.profile_error),
                        style = MaterialTheme.typography.titleMedium,
                        color = colors.text,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(modifier = Modifier.height(VexSpace.s4))
                    Button(
                        onClick = onRetryClick,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colors.primary,
                            contentColor = colors.onPrimary,
                        ),
                    ) {
                        Text(text = stringResource(R.string.profile_retry))
                    }
                }
            }
            profile != null -> {
                val currentMediaItems = when (uiState.selectedTab) {
                    ProfileTab.Posts -> uiState.posts
                    ProfileTab.Exhibits -> uiState.exhibits
                }

                PullToRefreshBox(
                    isRefreshing = uiState.isLoading,
                    onRefresh = onRetryClick,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                ) {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                        contentPadding = PaddingValues(bottom = VexSpace.s6),
                    ) {
                    // -----------------------------------------------------
                    // Item Span Penuh: Header Profil, Bio, Kontak & Tabs
                    // -----------------------------------------------------
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(colors.bg)
                                .padding(horizontal = VexSpace.s6, vertical = VexSpace.s4),
                        ) {
                            // Baris 1: Avatar 96dp + Nama Lengkap & Stats
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Avatar(
                                    initials = profile.username,
                                    size = AvatarSize.Lg,
                                    imageRes = profile.avatarRes,
                                    imageBitmap = uiState.avatarBitmap,
                                    contentDescription = profile.fullName,
                                )

                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(start = VexSpace.s5),
                                ) {
                                    Text(
                                        text = profile.fullName,
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.SemiBold,
                                        ),
                                        color = colors.text,
                                        maxLines = 2,
                                    )

                                    Spacer(modifier = Modifier.height(VexSpace.s3))

                                    // Stat Block: posts & exhibits
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceEvenly,
                                    ) {
                                        Column {
                                            Text(
                                                text = "${profile.postCount}",
                                                style = MaterialTheme.typography.titleMedium.copy(
                                                    fontWeight = FontWeight.Bold,
                                                ),
                                                color = colors.text,
                                            )
                                            Text(
                                                text = stringResource(R.string.profile_stat_posts),
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = colors.textMuted,
                                            )
                                        }

                                        Column {
                                            Text(
                                                text = "${profile.exhibitCount}",
                                                style = MaterialTheme.typography.titleMedium.copy(
                                                    fontWeight = FontWeight.Bold,
                                                ),
                                                color = colors.text,
                                            )
                                            Text(
                                                text = stringResource(R.string.profile_stat_exhibits),
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = colors.textMuted,
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(VexSpace.s5))

                            VexIconTabs(
                                tabs = listOf(ProfileTab.Posts, ProfileTab.Exhibits),
                                selectedTab = uiState.selectedTab,
                                onTabSelected = onProfileTabChange,
                                iconFor = { tab, isSelected ->
                                    when (tab) {
                                        ProfileTab.Posts -> if (isSelected) VexIcons.Tab.Posts.filled else VexIcons.Tab.Posts.outline
                                        ProfileTab.Exhibits -> if (isSelected) VexIcons.Tab.Exhibits.filled else VexIcons.Tab.Exhibits.outline
                                    }
                                },
                                contentDescriptionFor = { tab ->
                                    when (tab) {
                                        ProfileTab.Posts -> stringResource(R.string.profile_tab_posts)
                                        ProfileTab.Exhibits -> stringResource(R.string.profile_tab_exhibits)
                                    }
                                },
                            )

                            Spacer(modifier = Modifier.height(VexSpace.s5))

                            // Baris 2: Skill Chips — warna seragam, turun baris bila penuh
                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(VexSpace.s2),
                                verticalArrangement = Arrangement.spacedBy(VexSpace.s2),
                            ) {
                                uiState.visibleSkills.forEach { skill ->
                                    VexSkillChip(
                                        text = skill.label,
                                        categoryColor = colors.primary,
                                        iconRes = null,
                                        onClick = { onSkillSheetToggle(true) },
                                    )
                                }

                                if (uiState.extraSkillCount > 0) {
                                    VexCounterChip(
                                        count = uiState.extraSkillCount,
                                        onClick = { onSkillSheetToggle(true) },
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(VexSpace.s4))

                            // Baris 3: Bio Pengguna
                            if (profile.bio.isNotBlank()) {
                                Text(
                                    text = profile.bio,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = colors.text,
                                )
                                Spacer(modifier = Modifier.height(VexSpace.s4))
                            }

                            HorizontalDivider(
                                modifier = Modifier.padding(vertical = VexSpace.s2),
                                color = colors.border.copy(alpha = 0.6f),
                            )

                            // Baris 4: Email — diklik untuk membuka aplikasi email
                            if (profile.email.isNotBlank()) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            runCatching { uriHandler.openUri("mailto:${profile.email}") }
                                        }
                                        .padding(vertical = VexSpace.s2),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(VexSpace.s3),
                                ) {
                                    VexIcon(
                                        id = VexIcons.SettingsEmail,
                                        contentDescription = stringResource(R.string.profile_email_label),
                                        size = 18.dp,
                                        tint = colors.primaryText,
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = stringResource(R.string.profile_email_label),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = colors.textMuted,
                                        )
                                        Text(
                                            text = profile.email,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = colors.primaryText,
                                        )
                                    }
                                }
                            }

                            // Baris 5: Phone — diklik untuk membuka dialer
                            if (profile.phone.isNotBlank()) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            runCatching { uriHandler.openUri("tel:${profile.phone}") }
                                        }
                                        .padding(vertical = VexSpace.s2),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(VexSpace.s3),
                                ) {
                                    VexIcon(
                                        id = VexIcons.ContactPhone,
                                        contentDescription = stringResource(R.string.profile_phone_label),
                                        size = 18.dp,
                                        tint = colors.primaryText,
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = stringResource(R.string.profile_phone_label),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = colors.textMuted,
                                        )
                                        Text(
                                            text = profile.phone,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = colors.primaryText,
                                        )
                                    }
                                }
                            }

                            HorizontalDivider(
                                modifier = Modifier.padding(vertical = VexSpace.s2),
                                color = colors.border.copy(alpha = 0.6f),
                            )

                            // Baris 6: Aksi utama — Edit Profile & Share Profile
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = VexSpace.s1),
                                horizontalArrangement = Arrangement.spacedBy(VexSpace.s3),
                            ) {
                                Button(
                                    onClick = onEditProfileClick,
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = colors.primary,
                                        contentColor = colors.onPrimary,
                                    ),
                                ) {
                                    Text(
                                        text = stringResource(R.string.profile_edit_profile),
                                        style = MaterialTheme.typography.titleSmall.copy(
                                            fontWeight = FontWeight.SemiBold,
                                        ),
                                    )
                                }
                                OutlinedButton(
                                    onClick = {
                                        val intent = Intent(Intent.ACTION_SEND).apply {
                                            type = "text/plain"
                                            putExtra(Intent.EXTRA_TEXT, "https://ven.app/@${profile.username}")
                                        }
                                        runCatching {
                                            context.startActivity(Intent.createChooser(intent, "Share Profile"))
                                        }
                                    },
                                    modifier = Modifier.weight(1f),
                                ) {
                                    Text(
                                        text = stringResource(R.string.profile_share_profile),
                                        style = MaterialTheme.typography.titleSmall.copy(
                                            fontWeight = FontWeight.SemiBold,
                                        ),
                                    )
                                }
                            }
                        }
                    }

                    // -----------------------------------------------------
                    // Media Items (Poster Grid 3 Kolom untuk Posts, Full Banner 16:9 untuk Exhibits)
                    // -----------------------------------------------------
                    if (currentMediaItems.isNotEmpty()) {
                        val isExhibits = uiState.selectedTab == ProfileTab.Exhibits
                        items(
                            items = currentMediaItems,
                            key = { it.id },
                            span = {
                                if (isExhibits) GridItemSpan(maxLineSpan) else GridItemSpan(1)
                            },
                        ) { item ->
                            MediaGridTile(
                                item = item,
                                onClick = { onMediaClick(item) },
                                aspectRatio = if (isExhibits) (16f / 9f) else VexAspect.poster,
                                modifier = if (isExhibits) {
                                    Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 4.dp, vertical = 4.dp)
                                } else {
                                    Modifier
                                },
                            )
                        }
                    } else {
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(180.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                val emptyText = when (uiState.selectedTab) {
                                    ProfileTab.Posts -> stringResource(R.string.profile_empty_posts)
                                    ProfileTab.Exhibits -> stringResource(R.string.profile_empty_exhibits)
                                }
                                Text(
                                    text = emptyText,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = colors.textMuted,
                                )
                            }
                        }
                    }
                }
            }
        }
    }

        if (uiState.isSkillSheetOpen) {
            VexBottomSheet(
                onDismissRequest = { onSkillSheetToggle(false) },
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = VexSpace.s6)
                        .padding(bottom = VexSpace.s8)
                        .navigationBarsPadding(),
                ) {
                    Text(
                        text = stringResource(R.string.profile_skills_title),
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.SemiBold,
                        ),
                        color = colors.text,
                        modifier = Modifier.padding(bottom = VexSpace.s4),
                    )

                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(VexSpace.s2),
                        verticalArrangement = Arrangement.spacedBy(VexSpace.s2),
                    ) {
                        uiState.allSkills.forEach { skill ->
                            VexSkillChip(
                                text = skill.label,
                                categoryColor = colors.primary,
                                iconRes = null,
                            )
                        }
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------
// Previews
// ---------------------------------------------------------

@Preview(name = "Profile Screen - Dark Mode", showBackground = true, backgroundColor = 0xFF1F1F1F)
@Composable
private fun ProfileScreenDarkPreview() {
    var state by remember { mutableStateOf(dummyProfileUiState) }

    VexTheme(mode = ThemeMode.Dark) {
        ProfileScreen(
            uiState = state,
            onTabSelected = {},
            onCreateClick = {},
            onMenuClick = {},
            onProfileTabChange = { state = state.copy(selectedTab = it) },
            onSkillSheetToggle = { state = state.copy(isSkillSheetOpen = it) },
            onMediaClick = {},
        )
    }
}

@Preview(name = "Profile Screen - Light Mode", showBackground = true, backgroundColor = 0xFFFFFFFF)
@Composable
private fun ProfileScreenLightPreview() {
    var state by remember { mutableStateOf(dummyProfileUiState) }

    VexTheme(mode = ThemeMode.Light) {
        ProfileScreen(
            uiState = state,
            onTabSelected = {},
            onCreateClick = {},
            onMenuClick = {},
            onProfileTabChange = { state = state.copy(selectedTab = it) },
            onSkillSheetToggle = { state = state.copy(isSkillSheetOpen = it) },
            onMediaClick = {},
        )
    }
}
