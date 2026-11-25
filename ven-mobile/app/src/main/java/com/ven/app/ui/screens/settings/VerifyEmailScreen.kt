package com.ven.app.ui.screens.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ven.app.R
import com.ven.app.ui.components.layout.VexBottomActionBar
import com.ven.app.ui.components.layout.VexTopBar
import com.ven.app.ui.components.ui.OtpInput
import com.ven.app.ui.components.ui.VexButton
import com.ven.app.ui.components.ui.VexButtonVariant
import com.ven.app.ui.theme.ThemeMode
import com.ven.app.ui.theme.VexRadius
import com.ven.app.ui.theme.VexSpace
import com.ven.app.ui.theme.VexTheme
import kotlinx.coroutines.delay

/**
 * Halaman Verifikasi Email melalui OTP.
 * Sesuai DESIGN.md §8.9:
 * - Subjudul alamat email tujuan + label OTP.
 * - 6 kotak [OtpInput].
 * - Banner feedback tunggal (error atau info, tidak bertumpuk) dengan warna semantik tema.
 * - Resend dengan hitung mundur cooldown + sticky [VexBottomActionBar].
 */
@Composable
fun VerifyEmailScreen(
    email: String,
    state: ChangeEmailUiState,
    onOtpChange: (String) -> Unit,
    onVerifyClick: () -> Unit,
    onResendClick: () -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = VexTheme.colors
    val targetEmail = email.ifBlank { state.emailBaru }

    var cooldown by remember { mutableIntStateOf(60) }

    LaunchedEffect(cooldown) {
        if (cooldown > 0) {
            delay(1000)
            cooldown -= 1
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = colors.bg,
        topBar = {
            VexTopBar.Back(
                title = stringResource(R.string.settings_verify_email_title),
                onBackClick = onBackClick,
            )
        },
        bottomBar = {
            VexBottomActionBar {
                VexButton(
                    text = stringResource(R.string.verify_otp_button),
                    onClick = onVerifyClick,
                    variant = VexButtonVariant.Primary,
                    loading = state.isLoading,
                    enabled = state.otp.trim().length >= 6 && !state.isLoading,
                )
            }
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(colors.bg)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = VexSpace.s6, vertical = VexSpace.s6),
        ) {
            Text(
                text = stringResource(R.string.verify_otp_label),
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.SemiBold,
                ),
                color = colors.text,
            )

            if (targetEmail.isNotBlank()) {
                Spacer(modifier = Modifier.height(VexSpace.s2))
                Text(
                    text = stringResource(R.string.settings_verify_email_subtitle, targetEmail),
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.textSecondary,
                )
            }

            Spacer(modifier = Modifier.height(VexSpace.s6))

            // 6 Digit OtpInput
            OtpInput(
                value = state.otp,
                onValueChange = onOtpChange,
                digitCount = 6,
                isError = state.errorMessage != null,
                onOtpComplete = { onVerifyClick() },
            )

            // Banner status tunggal (error diprioritaskan; tidak pernah tampil dobel dengan info)
            val feedbackMessage = state.errorMessage ?: state.infoMessage
            val isFeedbackError = state.errorMessage != null
            if (!feedbackMessage.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(VexSpace.s4))
                FeedbackBanner(
                    message = feedbackMessage,
                    accentColor = if (isFeedbackError) colors.danger else colors.success,
                    symbol = if (isFeedbackError) "!" else "✓",
                )
            }

            Spacer(modifier = Modifier.height(VexSpace.s6))

            // Info & Link Resend
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = stringResource(R.string.settings_didnt_get_mail),
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.textSecondary,
                    textAlign = TextAlign.Center,
                )

                Spacer(modifier = Modifier.height(VexSpace.s1))

                if (cooldown > 0) {
                    Text(
                        text = stringResource(R.string.verify_otp_resend_cooldown, cooldown),
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontWeight = FontWeight.SemiBold,
                        ),
                        color = colors.textMuted,
                    )
                } else {
                    VexButton(
                        text = stringResource(R.string.settings_resend),
                        onClick = {
                            cooldown = 60
                            onResendClick()
                        },
                        variant = VexButtonVariant.TextLink,
                        enabled = !state.isLoading,
                    )
                }
            }
        }
    }
}

@Composable
private fun FeedbackBanner(
    message: String,
    accentColor: Color,
    symbol: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(VexRadius.md),
        color = accentColor.copy(alpha = 0.10f),
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.30f)),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = VexSpace.s4, vertical = VexSpace.s3),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = symbol,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                color = accentColor,
            )
            Spacer(modifier = Modifier.width(VexSpace.s3))
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = accentColor,
            )
        }
    }
}

// ---------------------------------------------------------
// Previews
// ---------------------------------------------------------

@Preview(name = "VerifyEmail - Dark Mode", showBackground = true, backgroundColor = 0xFF1F1F1F)
@Composable
private fun VerifyEmailDarkPreview() {
    VexTheme(mode = ThemeMode.Dark) {
        VerifyEmailScreen(
            email = "user@example.com",
            state = ChangeEmailUiState(
                emailBaru = "user@example.com",
                otp = "125059",
                errorMessage = "Too many requests. Please try again in a moment.",
            ),
            onOtpChange = {},
            onVerifyClick = {},
            onResendClick = {},
            onBackClick = {},
        )
    }
}

@Preview(name = "VerifyEmail - Light Mode", showBackground = true, backgroundColor = 0xFFFFFFFF)
@Composable
private fun VerifyEmailLightPreview() {
    VexTheme(mode = ThemeMode.Light) {
        VerifyEmailScreen(
            email = "user@example.com",
            state = ChangeEmailUiState(
                emailBaru = "user@example.com",
                infoMessage = "Verification code has been sent to your new email.",
            ),
            onOtpChange = {},
            onVerifyClick = {},
            onResendClick = {},
            onBackClick = {},
        )
    }
}
