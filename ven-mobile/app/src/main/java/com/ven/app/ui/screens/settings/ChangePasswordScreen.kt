package com.ven.app.ui.screens.settings

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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import com.ven.app.R
import com.ven.app.ui.components.layout.VexBottomActionBar
import com.ven.app.ui.components.layout.VexTopBar
import com.ven.app.ui.components.ui.VexButton
import com.ven.app.ui.components.ui.VexButtonVariant
import com.ven.app.ui.components.ui.VexPasswordField
import com.ven.app.ui.components.ui.VexTextFieldVariant
import com.ven.app.ui.screens.auth.PasswordValidator
import com.ven.app.ui.theme.ThemeMode
import com.ven.app.ui.theme.VexSpace
import com.ven.app.ui.theme.VexTheme

/**
 * Halaman Change Password.
 * Sesuai wireframe `Wireframe - Setting Password.png` dan DESIGN.md §8.10:
 * - Field: Current password, New password, Confirm password.
 * - Validasi real-time kekuatan kata sandi (5 aturan), beda dari sandi lama, & konfirmasi cocok.
 * - Sticky bottom CTA: [VexBottomActionBar].
 */
@Composable
fun ChangePasswordScreen(
    state: ChangePasswordUiState,
    onCurrentPasswordChange: (String) -> Unit,
    onNewPasswordChange: (String) -> Unit,
    onConfirmPasswordChange: (String) -> Unit,
    onBackClick: () -> Unit,
    onSubmitClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = VexTheme.colors

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = colors.bg,
        topBar = {
            VexTopBar.Back(
                title = stringResource(R.string.settings_item_change_password),
                onBackClick = onBackClick,
            )
        },
        bottomBar = {
            VexBottomActionBar {
                VexButton(
                    text = stringResource(R.string.settings_save_changes),
                    onClick = onSubmitClick,
                    variant = VexButtonVariant.Primary,
                    enabled = state.isFormValid,
                    loading = state.isLoading,
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
                .padding(horizontal = VexSpace.s6, vertical = VexSpace.s5)
                .padding(bottom = VexSpace.s8),
        ) {
            val hasFormError = state.errorMessage != null
            val newPasswordWeak = state.kataSandi.isNotEmpty() && !state.isPasswordStrong
            val confirmMismatch = state.konfirmasiKataSandi.isNotEmpty() && !state.isPasswordMatch

            VexPasswordField(
                value = state.kataSandiSaatIni,
                onValueChange = onCurrentPasswordChange,
                variant = VexTextFieldVariant.Boxed,
                placeholder = "",
                label = stringResource(R.string.settings_current_password_label),
                isError = hasFormError,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Next,
                ),
            )

            Spacer(modifier = Modifier.height(VexSpace.s4))

            VexPasswordField(
                value = state.kataSandi,
                onValueChange = onNewPasswordChange,
                variant = VexTextFieldVariant.Boxed,
                placeholder = "",
                label = stringResource(R.string.settings_new_password_label),
                isError = state.isSameAsCurrent || newPasswordWeak || hasFormError,
                errorMessage = if (state.isSameAsCurrent) stringResource(R.string.settings_change_password_same_as_current) else "",
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Next,
                ),
            )

            Spacer(modifier = Modifier.height(VexSpace.s2))

            ChangePasswordRequirements(
                password = state.kataSandi,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = VexSpace.s2),
            )

            Spacer(modifier = Modifier.height(VexSpace.s4))

            VexPasswordField(
                value = state.konfirmasiKataSandi,
                onValueChange = onConfirmPasswordChange,
                variant = VexTextFieldVariant.Boxed,
                placeholder = "",
                label = stringResource(R.string.settings_confirm_password_label),
                isError = confirmMismatch || hasFormError,
                errorMessage = if (confirmMismatch) stringResource(R.string.register_error_confirm) else "",
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done,
                ),
            )

            state.errorMessage?.let {
                Spacer(modifier = Modifier.height(VexSpace.s3))
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.danger,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun ChangePasswordRequirements(password: String, modifier: Modifier = Modifier) {
    val colors = VexTheme.colors
    val rules = PasswordValidator.rules(password)

    Column(modifier = modifier) {
        rules.forEach { rule ->
            val color = if (rule.isMet) colors.success else colors.textMuted
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(vertical = VexSpace.s1 / 2),
            ) {
                Text(
                    text = if (rule.isMet) "✓" else "•",
                    color = color,
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.width(VexSpace.s5),
                )
                Text(
                    text = rule.label,
                    color = color,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}

// ---------------------------------------------------------
// Previews
// ---------------------------------------------------------

@Preview(name = "ChangePassword - Dark Mode", showBackground = true, backgroundColor = 0xFF1F1F1F)
@Composable
private fun ChangePasswordDarkPreview() {
    VexTheme(mode = ThemeMode.Dark) {
        ChangePasswordScreen(
            state = ChangePasswordUiState(
                kataSandiSaatIni = "OldPass.123",
                kataSandi = "NewPass.456",
                konfirmasiKataSandi = "NewPass.456",
            ),
            onCurrentPasswordChange = {},
            onNewPasswordChange = {},
            onConfirmPasswordChange = {},
            onBackClick = {},
            onSubmitClick = {},
        )
    }
}

@Preview(name = "ChangePassword - Light Mode", showBackground = true, backgroundColor = 0xFFFFFFFF)
@Composable
private fun ChangePasswordLightPreview() {
    VexTheme(mode = ThemeMode.Light) {
        ChangePasswordScreen(
            state = ChangePasswordUiState(),
            onCurrentPasswordChange = {},
            onNewPasswordChange = {},
            onConfirmPasswordChange = {},
            onBackClick = {},
            onSubmitClick = {},
        )
    }
}
