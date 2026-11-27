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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ven.app.R
import com.ven.app.data.api.ApiHttpException
import com.ven.app.data.api.ApiUndangan
import com.ven.app.ui.components.layout.VexTopBar
import com.ven.app.ui.components.ui.VexButton
import com.ven.app.ui.components.ui.VexButtonVariant
import com.ven.app.ui.screens.voice.VoiceAcara
import com.ven.app.ui.screens.voice.VoiceChatService
import com.ven.app.ui.screens.voice.VoiceChatViewModel
import com.ven.app.ui.theme.VexSpace
import com.ven.app.ui.theme.VexTheme
import kotlinx.coroutines.delay

private sealed interface WaitingRoomState {
    data object Checking : WaitingRoomState
    data class Knocking(val title: String?) : WaitingRoomState
    data object Expired : WaitingRoomState
    data object Rejected : WaitingRoomState
    data class Error(val message: String) : WaitingRoomState
}

@Composable
fun PrivateRoomWaitingScreen(
    token: String,
    onBackClick: () -> Unit,
    onEnterPrivateRoom: (exhibitId: String, roomId: Int) -> Unit,
    modifier: Modifier = Modifier,
    voiceViewModel: VoiceChatViewModel = viewModel(),
) {
    val context = LocalContext.current
    val colors = VexTheme.colors
    var state by remember { mutableStateOf<WaitingRoomState>(WaitingRoomState.Checking) }
    var pendingEntry by remember { mutableStateOf<Pair<Int, String>?>(null) }
    var hasLaunched by remember { mutableStateOf(false) }

    val micPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { result ->
        val target = pendingEntry
        pendingEntry = null
        if (target != null && result[Manifest.permission.RECORD_AUDIO] == true) {
            voiceViewModel.mulaiSesi(target.first, target.second)
        } else if (target != null) {
            Toast.makeText(context, context.getString(R.string.voice_izin_ditolak), Toast.LENGTH_SHORT).show()
            onEnterPrivateRoom(target.second, target.first)
        }
    }

    LaunchedEffect(Unit) {
        voiceViewModel.acara.collect { event ->
            when (event) {
                is VoiceAcara.SiapGabung -> {
                    VoiceChatService.mulai(context, event.token.url, event.token.token)
                    onEnterPrivateRoom(event.exhibitId?.ifBlank { "4" } ?: "4", event.idRuangan)
                }

                is VoiceAcara.Gagal -> {
                    Toast.makeText(context, event.pesan, Toast.LENGTH_SHORT).show()
                    val fallback = pendingEntry
                    if (fallback != null) {
                        onEnterPrivateRoom(fallback.second, fallback.first)
                    }
                }
            }
        }
    }

    LaunchedEffect(token) {
        if (token.isBlank()) {
            state = WaitingRoomState.Expired
            return@LaunchedEffect
        }

        while (!hasLaunched) {
            val res = ApiUndangan.aksesTautan(token)
            if (res.isSuccess) {
                val data = res.getOrNull()
                if (data != null && data.statusAkses == "langsung") {
                    hasLaunched = true
                    val exhibitId = data.idPameran?.toString() ?: "4"
                    val roomId = data.idRuangan
                    val granted = ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.RECORD_AUDIO,
                    ) == PackageManager.PERMISSION_GRANTED
                    if (granted) {
                        pendingEntry = roomId to exhibitId
                        voiceViewModel.mulaiSesi(roomId, exhibitId)
                    } else {
                        pendingEntry = roomId to exhibitId
                        micPermissionLauncher.launch(
                            arrayOf(
                                Manifest.permission.RECORD_AUDIO,
                                Manifest.permission.POST_NOTIFICATIONS,
                            ),
                        )
                    }
                    break
                } else {
                    state = WaitingRoomState.Knocking(data?.judulPameran)
                    delay(3000L)
                }
            } else {
                val err = res.exceptionOrNull()
                when ((err as? ApiHttpException)?.statusCode) {
                    410 -> {
                        state = WaitingRoomState.Expired
                        break
                    }
                    403 -> {
                        state = WaitingRoomState.Rejected
                        break
                    }
                    else -> {
                        state = WaitingRoomState.Error(err?.message ?: context.getString(R.string.waiting_room_expired_title))
                        break
                    }
                }
            }
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = colors.bg,
        topBar = {
            VexTopBar.Back(
                title = stringResource(R.string.waiting_room_title),
                onBackClick = onBackClick,
            )
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(colors.bg)
                .padding(innerPadding)
                .padding(horizontal = VexSpace.s6),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth(),
            ) {
                when (val current = state) {
                    WaitingRoomState.Checking -> {
                        CircularProgressIndicator(
                            color = colors.primary,
                            strokeWidth = 2.5.dp,
                            modifier = Modifier.size(32.dp),
                        )
                        Spacer(modifier = Modifier.height(VexSpace.s4))
                        Text(
                            text = stringResource(R.string.waiting_room_checking),
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.textMuted,
                            textAlign = TextAlign.Center,
                        )
                    }

                    is WaitingRoomState.Knocking -> {
                        CircularProgressIndicator(
                            color = colors.primary,
                            strokeWidth = 2.5.dp,
                            modifier = Modifier.size(32.dp),
                        )
                        Spacer(modifier = Modifier.height(VexSpace.s5))
                        if (!current.title.isNullOrBlank()) {
                            Text(
                                text = current.title,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = colors.text,
                                textAlign = TextAlign.Center,
                            )
                            Spacer(modifier = Modifier.height(VexSpace.s2))
                        }
                        Text(
                            text = stringResource(R.string.waiting_room_knocking_title),
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = colors.text,
                            textAlign = TextAlign.Center,
                        )
                        Spacer(modifier = Modifier.height(VexSpace.s2))
                        Text(
                            text = stringResource(R.string.waiting_room_knocking_subtitle),
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.textMuted,
                            textAlign = TextAlign.Center,
                        )
                        Spacer(modifier = Modifier.height(VexSpace.s6))
                        VexButton(
                            text = stringResource(R.string.waiting_room_back),
                            onClick = onBackClick,
                            variant = VexButtonVariant.Outline,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }

                    WaitingRoomState.Expired -> {
                        Text(
                            text = stringResource(R.string.waiting_room_expired_title),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = colors.text,
                            textAlign = TextAlign.Center,
                        )
                        Spacer(modifier = Modifier.height(VexSpace.s2))
                        Text(
                            text = stringResource(R.string.waiting_room_expired_subtitle),
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.textMuted,
                            textAlign = TextAlign.Center,
                        )
                        Spacer(modifier = Modifier.height(VexSpace.s6))
                        VexButton(
                            text = stringResource(R.string.waiting_room_back),
                            onClick = onBackClick,
                            variant = VexButtonVariant.Primary,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }

                    WaitingRoomState.Rejected -> {
                        Text(
                            text = stringResource(R.string.waiting_room_rejected_title),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = colors.text,
                            textAlign = TextAlign.Center,
                        )
                        Spacer(modifier = Modifier.height(VexSpace.s2))
                        Text(
                            text = stringResource(R.string.waiting_room_rejected_subtitle),
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.textMuted,
                            textAlign = TextAlign.Center,
                        )
                        Spacer(modifier = Modifier.height(VexSpace.s6))
                        VexButton(
                            text = stringResource(R.string.waiting_room_back),
                            onClick = onBackClick,
                            variant = VexButtonVariant.Primary,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }

                    is WaitingRoomState.Error -> {
                        Text(
                            text = current.message,
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.danger,
                            textAlign = TextAlign.Center,
                        )
                        Spacer(modifier = Modifier.height(VexSpace.s6))
                        VexButton(
                            text = stringResource(R.string.waiting_room_back),
                            onClick = onBackClick,
                            variant = VexButtonVariant.Outline,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }
        }
    }
}
