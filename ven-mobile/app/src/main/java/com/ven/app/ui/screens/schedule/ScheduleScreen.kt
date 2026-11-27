package com.ven.app.ui.screens.schedule

import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ven.app.R
import com.ven.app.ui.components.feature.EventListItem
import com.ven.app.ui.components.feature.RequestListItem
import com.ven.app.ui.components.layout.AppScaffold
import com.ven.app.ui.components.ui.SearchBar
import com.ven.app.ui.components.ui.VexTextTabs
import com.ven.app.ui.navigation.BottomNavTab
import com.ven.app.ui.screens.schedule.components.EventDetailSheet
import com.ven.app.ui.screens.schedule.components.RequestDetailSheet
import com.ven.app.ui.screens.voice.VoiceAcara
import com.ven.app.ui.screens.voice.VoiceChatService
import com.ven.app.ui.screens.voice.VoiceChatViewModel
import com.ven.app.ui.screens.voice.VoiceSesi
import com.ven.app.ui.theme.ThemeMode
import com.ven.app.ui.theme.VexSpace
import com.ven.app.ui.theme.VexTheme
import com.ven.app.util.UnityLauncher
import kotlinx.coroutines.delay

private data class RuanganIzinMenunggu(
    val roomId: Int,
    val exhibitId: String?,
    val idUndangan: Int?,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleScreen(
    onTabSelected: (BottomNavTab) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ScheduleViewModel = viewModel(),
    voiceViewModel: VoiceChatViewModel = viewModel(),
    onJoinRoom: ((exhibitId: String?, roomId: Int?) -> Unit)? = null,
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val voiceState by voiceViewModel.uiState.collectAsStateWithLifecycle()

    // Ruangan privat yang menunggu persetujuan izin mikrofon sebelum token diambil.
    var ruanganMenungguIzin by remember { mutableStateOf<RuanganIzinMenunggu?>(null) }

    val izinMicLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { hasil ->
        val target = ruanganMenungguIzin
        ruanganMenungguIzin = null

        if (hasil[Manifest.permission.RECORD_AUDIO] == true && target != null) {
            voiceViewModel.mulaiSesi(target.roomId, target.exhibitId, target.idUndangan)
        } else if (hasil[Manifest.permission.RECORD_AUDIO] == false) {
            Toast.makeText(context, context.getString(R.string.voice_izin_ditolak), Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(uiState.userFeedbackMessage) {
        uiState.userFeedbackMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.clearFeedbackMessage()
        }
    }

    // Auto-refresh jadwal setiap 60 detik & tick waktu setiap 30 detik untuk memperbarui status join
    LaunchedEffect(Unit) {
        while (true) {
            delay(60_000L)
            viewModel.refreshSilently()
        }
    }

    LaunchedEffect(Unit) {
        while (true) {
            delay(30_000L)
            viewModel.tick()
        }
    }

    // Peristiwa voice: nyalakan service (koneksi bertahan selama di Unity) lalu buka ruangan 3D privat.
    LaunchedEffect(Unit) {
        voiceViewModel.acara.collect { peristiwa ->
            when (peristiwa) {
                is VoiceAcara.SiapGabung -> {
                    VoiceChatService.mulai(context, peristiwa.token.url, peristiwa.token.token)
                    if (onJoinRoom != null) {
                        onJoinRoom(peristiwa.exhibitId, peristiwa.idRuangan)
                    } else {
                        UnityLauncher.launchViewer(context, peristiwa.exhibitId?.ifBlank { "4" } ?: "4", peristiwa.idRuangan)
                    }
                }

                is VoiceAcara.Gagal ->
                    Toast.makeText(context, peristiwa.pesan, Toast.LENGTH_SHORT).show()
            }
        }
    }

    AppScaffold(
        currentTab = BottomNavTab.Schedule,
        onTabSelected = onTabSelected,
        modifier = modifier,
    ) { innerPadding ->
        ScheduleContent(
            uiState = uiState,
            isRefreshing = uiState.isRefreshing,
            onRefresh = viewModel::refresh,
            onLoadMore = viewModel::loadMore,
            onTabSelected = viewModel::selectTab,
            onSearchQueryChange = viewModel::setSearchQuery,
            onEventClick = viewModel::selectEventDetail,
            onRequestClick = viewModel::selectRequestDetail,
            onDismissDetail = viewModel::dismissDetail,
            voiceStatus = voiceState.sesi,
            onJoinRoom = { event ->
                val roomId = event.roomId
                val idUndangan = event.id.toIntOrNull()

                if (roomId != null) {
                    // Validasi gating waktu: hanya izinkan dalam jendela [-15 menit, +15 menit]
                    if (!event.isJoinable(uiState.nowMillis)) {
                        Toast.makeText(
                            context,
                            context.getString(R.string.schedule_join_window_warning),
                            Toast.LENGTH_SHORT,
                        ).show()
                        return@ScheduleContent
                    }

                    val sudahDiizinkan = ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.RECORD_AUDIO,
                    ) == PackageManager.PERMISSION_GRANTED

                    if (sudahDiizinkan) {
                        voiceViewModel.mulaiSesi(roomId, event.exhibitId, idUndangan)
                    } else {
                        ruanganMenungguIzin = RuanganIzinMenunggu(roomId, event.exhibitId, idUndangan)
                        izinMicLauncher.launch(
                            arrayOf(
                                Manifest.permission.RECORD_AUDIO,
                                Manifest.permission.POST_NOTIFICATIONS,
                            ),
                        )
                    }
                } else if (onJoinRoom != null) {
                    onJoinRoom(event.exhibitId, null)
                } else {
                    val targetId = event.exhibitId?.ifBlank { "4" } ?: "4"
                    UnityLauncher.launchViewer(context, targetId)
                }
                viewModel.dismissDetail()
            },
            onApproveRequest = viewModel::approveRequest,
            onRejectRequest = viewModel::rejectRequest,
            modifier = Modifier.padding(bottom = innerPadding.calculateBottomPadding()),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleContent(
    uiState: ScheduleUiState,
    onTabSelected: (ScheduleTab) -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onEventClick: (ScheduleEventItem) -> Unit,
    onRequestClick: (ScheduleRequestItem) -> Unit,
    onDismissDetail: () -> Unit,
    onJoinRoom: (ScheduleEventItem) -> Unit,
    onApproveRequest: (ScheduleRequestItem) -> Unit,
    onRejectRequest: (ScheduleRequestItem) -> Unit,
    modifier: Modifier = Modifier,
    isRefreshing: Boolean = false,
    onRefresh: () -> Unit = {},
    onLoadMore: () -> Unit = {},
    voiceStatus: VoiceSesi = VoiceSesi.Jeda,
) {
    val colors = VexTheme.colors

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.bg)
            .statusBarsPadding(),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = VexSpace.s5)
                .padding(top = VexSpace.s4, bottom = VexSpace.s4),
        ) {
            SearchBar(
                query = uiState.searchQuery,
                onQueryChange = onSearchQueryChange,
            )
        }

        VexTextTabs(
            tabs = ScheduleTab.entries,
            selectedTab = uiState.selectedTab,
            onTabSelected = onTabSelected,
            labelFor = { tab ->
                when (tab) {
                    ScheduleTab.Approved -> stringResource(R.string.schedule_tab_approved)
                    ScheduleTab.Waiting -> stringResource(R.string.schedule_tab_waiting)
                    ScheduleTab.Request -> stringResource(R.string.schedule_tab_request)
                    ScheduleTab.History -> stringResource(R.string.schedule_tab_history)
                }
            },
        )

        HorizontalDivider(
            thickness = 1.dp,
            color = colors.border,
        )

        // Banner Kesalahan bila pemanggilan API gagal
        if (!uiState.errorMessage.isNullOrBlank()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(colors.danger.copy(alpha = 0.15f))
                    .padding(horizontal = VexSpace.s5, vertical = VexSpace.s2),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = uiState.errorMessage,
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.danger,
                    modifier = Modifier.weight(1f),
                )
                TextButton(onClick = onRefresh) {
                    Text(
                        text = stringResource(R.string.schedule_retry),
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = colors.danger,
                    )
                }
            }
        }

        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = onRefresh,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = VexSpace.s2),
            ) {
                when (uiState.selectedTab) {
                    ScheduleTab.Approved -> {
                        val items = uiState.filteredApprovedItems
                        if (items.isEmpty()) {
                            item {
                                EmptyScheduleView(
                                    message = stringResource(R.string.schedule_empty_approved),
                                )
                            }
                        } else {
                            items(items, key = { it.id }) { item ->
                                EventListItem(
                                    entry = item,
                                    onClick = { onEventClick(item) },
                                )
                            }
                        }
                    }

                    ScheduleTab.Waiting -> {
                        val items = uiState.filteredWaitingItems
                        if (items.isEmpty()) {
                            item {
                                EmptyScheduleView(
                                    message = stringResource(R.string.schedule_empty_waiting),
                                )
                            }
                        } else {
                            items(items, key = { it.id }) { item ->
                                EventListItem(
                                    entry = item,
                                    onClick = { onEventClick(item) },
                                )
                            }
                        }
                    }

                    ScheduleTab.Request -> {
                        val items = uiState.filteredRequestItems
                        if (items.isEmpty()) {
                            item {
                                EmptyScheduleView(
                                    message = stringResource(R.string.schedule_empty_request),
                                )
                            }
                        } else {
                            items(items, key = { it.id }) { item ->
                                RequestListItem(
                                    entry = item,
                                    onClick = { onRequestClick(item) },
                                )
                            }
                        }
                    }

                    ScheduleTab.History -> {
                        val items = uiState.filteredHistoryItems
                        if (items.isEmpty()) {
                            item {
                                EmptyScheduleView(
                                    message = stringResource(R.string.schedule_empty_history),
                                )
                            }
                        } else {
                            items(items, key = { it.id }) { item ->
                                EventListItem(
                                    entry = item,
                                    onClick = { onEventClick(item) },
                                )
                            }
                        }
                    }
                }

                // Indikator "Muat Lebih Banyak" jika masih ada halaman berikutnya
                if (uiState.hasMore) {
                    item(key = "load_more_footer") {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = VexSpace.s4),
                            contentAlignment = Alignment.Center,
                        ) {
                            if (uiState.isLoadingMore) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    color = colors.primary,
                                    strokeWidth = 2.dp,
                                )
                            } else {
                                TextButton(onClick = onLoadMore) {
                                    Text(
                                        text = stringResource(R.string.schedule_load_more),
                                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                                        color = colors.primary,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    uiState.activeEventDetail?.let { event ->
        EventDetailSheet(
            event = event,
            onDismissRequest = onDismissDetail,
            onJoinRoom = { onJoinRoom(event) },
            nowMillis = uiState.nowMillis,
            voiceStatus = voiceStatus,
        )
    }

    uiState.activeRequestDetail?.let { request ->
        RequestDetailSheet(
            request = request,
            onDismissRequest = onDismissDetail,
            onApprove = { onApproveRequest(request) },
            onReject = { onRejectRequest(request) },
        )
    }
}

@Composable
private fun EmptyScheduleView(
    message: String,
    modifier: Modifier = Modifier,
) {
    val colors = VexTheme.colors

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 48.dp, horizontal = 24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = colors.textMuted,
        )
    }
}

// ---------------------------------------------------------
// Previews
// ---------------------------------------------------------

@Preview(name = "ScheduleScreen Approved - Dark Mode", showBackground = true, backgroundColor = 0xFF1F1F1F)
@Composable
private fun ScheduleScreenApprovedDarkPreview() {
    VexTheme(mode = ThemeMode.Dark) {
        ScheduleContent(
            uiState = dummyScheduleUiState,
            onTabSelected = {},
            onSearchQueryChange = {},
            onEventClick = {},
            onRequestClick = {},
            onDismissDetail = {},
            onJoinRoom = {},
            onApproveRequest = {},
            onRejectRequest = {},
        )
    }
}

@Preview(name = "ScheduleScreen Request - Dark Mode", showBackground = true, backgroundColor = 0xFF1F1F1F)
@Composable
private fun ScheduleScreenRequestDarkPreview() {
    VexTheme(mode = ThemeMode.Dark) {
        ScheduleContent(
            uiState = dummyScheduleUiState.copy(selectedTab = ScheduleTab.Request),
            onTabSelected = {},
            onSearchQueryChange = {},
            onEventClick = {},
            onRequestClick = {},
            onDismissDetail = {},
            onJoinRoom = {},
            onApproveRequest = {},
            onRejectRequest = {},
        )
    }
}
