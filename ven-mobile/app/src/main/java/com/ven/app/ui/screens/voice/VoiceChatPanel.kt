package com.ven.app.ui.screens.voice

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ven.app.R
import com.ven.app.ui.icons.VexIcon
import com.ven.app.ui.icons.VexIcons
import com.ven.app.ui.theme.VexRadius
import com.ven.app.ui.theme.VexSpace
import com.ven.app.ui.theme.VexTheme

/**
 * Kartu kecil di detail acara Schedule yang memberi tahu pengguna bahwa
 * ruangan privat ini mendukung voice chat saat bergabung.
 */
@Composable
fun VoiceChatPanel(
    status: VoiceSesi,
    modifier: Modifier = Modifier,
) {
    val colors = VexTheme.colors

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(VexRadius.md))
            .background(colors.primary.copy(alpha = 0.08f))
            .border(1.dp, colors.primary.copy(alpha = 0.25f), RoundedCornerShape(VexRadius.md))
            .padding(horizontal = VexSpace.s4, vertical = VexSpace.s3),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            VexIcon(
                id = if (status == VoiceSesi.Gagal) VexIcons.MicOff else VexIcons.Mic,
                contentDescription = null,
                tint = colors.primary,
                size = 18.dp,
            )

            Spacer(modifier = Modifier.width(VexSpace.s2))

            Text(
                text = stringResource(R.string.voice_panel_judul),
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                color = colors.text,
            )
        }

        Spacer(modifier = Modifier.height(VexSpace.s1))

        Text(
            text = when (status) {
                VoiceSesi.Menyiapkan -> stringResource(R.string.voice_status_menyiapkan)
                VoiceSesi.Tersambung -> stringResource(R.string.voice_status_tersambung)
                VoiceSesi.Gagal -> stringResource(R.string.voice_status_gagal)
                VoiceSesi.Jeda -> stringResource(R.string.voice_panel_deskripsi)
            },
            style = MaterialTheme.typography.bodySmall,
            color = colors.textMuted,
        )
    }
}

@Preview(name = "VoiceChatPanel - Dark Mode")
@Composable
private fun VoiceChatPanelPreview() {
    VexTheme(mode = com.ven.app.ui.theme.ThemeMode.Dark) {
        VoiceChatPanel(status = VoiceSesi.Jeda)
    }
}

@Preview(name = "VoiceChatPanel - Light Mode")
@Composable
private fun VoiceChatPanelLightPreview() {
    VexTheme(mode = com.ven.app.ui.theme.ThemeMode.Light) {
        VoiceChatPanel(status = VoiceSesi.Tersambung)
    }
}
