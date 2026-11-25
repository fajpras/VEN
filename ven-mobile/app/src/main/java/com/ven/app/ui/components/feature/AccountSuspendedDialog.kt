package com.ven.app.ui.components.feature

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.ven.app.R
import com.ven.app.data.api.AccountSuspensionInfo
import com.ven.app.ui.components.ui.VexButton
import com.ven.app.ui.components.ui.VexButtonShape
import com.ven.app.ui.components.ui.VexButtonVariant
import com.ven.app.ui.theme.ThemeMode
import com.ven.app.ui.theme.VexTheme

/**
 * Dialog penangguhan akun bergaya Instagram:
 * menampilkan ikon perisai peringatan, judul tegas, kartu rincian alasan & durasi,
 * penjelasan dampak pembatasan, dan tombol konfirmasi.
 */
@Composable
fun AccountSuspendedDialog(
    info: AccountSuspensionInfo,
    onDismiss: () -> Unit,
) {
    val colors = VexTheme.colors
    val dangerAccent = colors.danger

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            shape = RoundedCornerShape(24.dp),
            color = colors.bg,
            border = BorderStroke(1.dp, colors.border),
            shadowElevation = 12.dp,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                // Ikon perisai peringatan dalam lingkaran halus
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(dangerAccent.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(dangerAccent.copy(alpha = 0.16f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        SuspensionShieldIcon(
                            tint = dangerAccent,
                            modifier = Modifier.size(28.dp),
                        )
                    }
                }

                Spacer(Modifier.height(18.dp))

                Text(
                    text = stringResource(
                        if (info.isPermanent) R.string.suspended_title_permanent else R.string.suspended_title_temp,
                    ),
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-0.3).sp,
                    ),
                    color = colors.text,
                    textAlign = TextAlign.Center,
                )

                Spacer(Modifier.height(6.dp))

                Text(
                    text = stringResource(R.string.suspended_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.textSecondary,
                    textAlign = TextAlign.Center,
                )

                Spacer(Modifier.height(20.dp))

                // Kartu terstruktur: Alasan & Durasi
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = colors.surfaceInput,
                    border = BorderStroke(1.dp, colors.border),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = stringResource(R.string.suspended_label_reason).uppercase(),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    letterSpacing = 0.7.sp,
                                ),
                                color = colors.textMuted,
                            )
                            Text(
                                text = info.reason,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.SemiBold,
                                ),
                                color = colors.text,
                            )
                        }

                        HorizontalDivider(color = colors.border, thickness = 1.dp)

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(
                                text = stringResource(
                                    if (info.isPermanent) R.string.suspended_label_status else R.string.suspended_label_duration,
                                ).uppercase(),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    letterSpacing = 0.7.sp,
                                ),
                                color = colors.textMuted,
                            )

                            if (info.isPermanent || info.untilText.isNullOrBlank()) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(999.dp))
                                        .background(dangerAccent.copy(alpha = 0.14f))
                                        .padding(horizontal = 10.dp, vertical = 4.dp),
                                ) {
                                    Text(
                                        text = stringResource(R.string.suspended_permanent_badge),
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                        ),
                                        color = dangerAccent,
                                    )
                                }
                            } else {
                                Text(
                                    text = info.untilText,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontWeight = FontWeight.SemiBold,
                                    ),
                                    color = dangerAccent,
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(18.dp))

                // Bagian "What this means"
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = stringResource(R.string.suspended_what_means_title),
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                        ),
                        color = colors.text,
                    )

                    SuspensionBulletRow(
                        text = stringResource(R.string.suspended_bullet_hidden),
                        dotColor = colors.textMuted,
                        textColor = colors.textSecondary,
                    )
                    SuspensionBulletRow(
                        text = stringResource(
                            if (info.isPermanent) {
                                R.string.suspended_bullet_restore_permanent
                            } else {
                                R.string.suspended_bullet_restore_temp
                            },
                        ),
                        dotColor = colors.textMuted,
                        textColor = colors.textSecondary,
                    )
                }

                Spacer(Modifier.height(24.dp))

                VexButton(
                    text = stringResource(R.string.suspended_understood_button),
                    onClick = onDismiss,
                    variant = VexButtonVariant.Primary,
                    shape = VexButtonShape.Boxed,
                )
            }
        }
    }
}

@Composable
private fun SuspensionBulletRow(
    text: String,
    dotColor: Color,
    textColor: Color,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
    ) {
        Box(
            modifier = Modifier
                .padding(top = 7.dp)
                .size(5.dp)
                .clip(CircleShape)
                .background(dotColor),
        )
        Spacer(Modifier.width(10.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall.copy(lineHeight = 18.sp),
            color = textColor,
        )
    }
}

@Composable
private fun SuspensionShieldIcon(
    tint: Color,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val stroke = Stroke(width = w * 0.09f, cap = StrokeCap.Round, join = StrokeJoin.Round)

        val shield = Path().apply {
            moveTo(w * 0.50f, h * 0.08f)
            lineTo(w * 0.86f, h * 0.22f)
            lineTo(w * 0.86f, h * 0.52f)
            cubicTo(w * 0.86f, h * 0.74f, w * 0.68f, h * 0.87f, w * 0.50f, h * 0.94f)
            cubicTo(w * 0.32f, h * 0.87f, w * 0.14f, h * 0.74f, w * 0.14f, h * 0.52f)
            lineTo(w * 0.14f, h * 0.22f)
            close()
        }
        drawPath(path = shield, color = tint, style = stroke)

        // Tanda seru di dalam perisai
        drawLine(
            color = tint,
            start = Offset(w * 0.50f, h * 0.34f),
            end = Offset(w * 0.50f, h * 0.56f),
            strokeWidth = w * 0.095f,
            cap = StrokeCap.Round,
        )
        drawCircle(
            color = tint,
            radius = w * 0.055f,
            center = Offset(w * 0.50f, h * 0.69f),
        )
    }
}

@Preview(name = "AccountSuspendedDialog - Light", showBackground = true)
@Composable
private fun AccountSuspendedDialogLightPreview() {
    VexTheme(mode = ThemeMode.Light) {
        AccountSuspendedDialog(
            info = AccountSuspensionInfo(
                reason = "Automated bot activity detected",
                untilText = "26 Nov 2026 12:24",
                isPermanent = false,
            ),
            onDismiss = {},
        )
    }
}
