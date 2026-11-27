package com.ven.app.ui.screens.unity

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ven.app.R
import com.ven.app.ui.theme.VexSpace
import com.ven.app.ui.theme.VexTheme
import com.ven.app.util.UnityLauncher

/**
 * Host Compose minimalis yang langsung meluncurkan [VenUnityActivity] tanpa jeda,
 * sehingga tidak ada transisi loading ganda dan tidak ada blank screen.
 */
@Composable
fun UnityLoadingScreen(
    targetId: String,
    title: String = "",
    description: String = "",
    bannerUrl: String = "",
    tipe: String = "kecil",
    isEditor: Boolean = true,
    roomId: Int? = null,
    editRoomId: String = "",
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val colors = VexTheme.colors
    var hasLaunched by rememberSaveable { mutableStateOf(false) }

    val unityLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
    ) {
        onBackClick()
    }

    LaunchedEffect(targetId) {
        if (!hasLaunched) {
            hasLaunched = true
            val intent = if (isEditor) {
                UnityLauncher.createEditorIntent(
                    context = context,
                    templateId = targetId,
                    title = title,
                    description = description,
                    bannerUrl = bannerUrl,
                    tipe = tipe,
                    editRoomId = editRoomId.ifBlank { roomId?.toString().orEmpty() },
                )
            } else {
                UnityLauncher.createViewerIntent(context, targetId, roomId)
            }
            unityLauncher.launch(intent)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(colors.bg),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = VexSpace.s8),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = "VEN",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 2.sp,
                ),
                color = colors.primary,
            )

            Spacer(modifier = Modifier.height(VexSpace.s4))

            Text(
                text = if (isEditor) {
                    stringResource(R.string.unity_loading_editor_title)
                } else {
                    stringResource(R.string.unity_loading_viewer_title)
                },
                style = MaterialTheme.typography.bodyMedium,
                color = colors.textMuted,
                textAlign = TextAlign.Center,
            )

            Spacer(modifier = Modifier.height(VexSpace.s5))

            LinearProgressIndicator(
                modifier = Modifier
                    .fillMaxWidth(0.55f)
                    .height(3.dp)
                    .clip(RoundedCornerShape(2.dp)),
                color = colors.primary,
                trackColor = colors.border.copy(alpha = 0.45f),
                strokeCap = StrokeCap.Round,
            )
        }
    }
}
