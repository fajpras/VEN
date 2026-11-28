package com.ven.app

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.os.LocaleListCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ven.app.data.api.ApiClient
import com.ven.app.data.api.SessionManager
import com.ven.app.data.preference.AppLanguage
import com.ven.app.data.preference.ThemePreferences
import com.ven.app.ui.navigation.AppNavHost
import com.ven.app.ui.theme.VexTheme

class MainActivity : AppCompatActivity() {

    private var pendingDeepLinkToken by mutableStateOf<String?>(null)
    private var sessionExpiredTick by mutableIntStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)

        if (AppCompatDelegate.getApplicationLocales().isEmpty) {
            AppCompatDelegate.setApplicationLocales(
                LocaleListCompat.forLanguageTags(AppLanguage.English.tag),
            )
        }

        val sessionManager = SessionManager.getInstance(applicationContext)
        val themePreferences = ThemePreferences.getInstance(applicationContext)
        ApiClient.init(
            tokenCallback = { sessionManager.getToken() },
            onUnauthorized = {
                if (!sessionManager.getToken().isNullOrBlank()) {
                    sessionManager.clearSession()
                    runOnUiThread {
                        Toast.makeText(
                            this,
                            getString(R.string.session_kicked_other_device),
                            Toast.LENGTH_LONG,
                        ).show()
                        sessionExpiredTick += 1
                    }
                }
            },
        )

        pendingDeepLinkToken = extractPrivateRoomToken(intent)

        enableEdgeToEdge()
        setContent {
            val themeMode by themePreferences.themeMode.collectAsStateWithLifecycle()
            VexTheme(mode = themeMode) {
                AppNavHost(
                    showExplore = BuildConfig.DEBUG &&
                        intent.getBooleanExtra("ven.showExplore", false),
                    deepLinkToken = pendingDeepLinkToken,
                    onDeepLinkConsumed = { pendingDeepLinkToken = null },
                    sessionExpiredTick = sessionExpiredTick,
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        extractPrivateRoomToken(intent)?.let { token ->
            pendingDeepLinkToken = token
        }
    }

    private fun extractPrivateRoomToken(targetIntent: Intent?): String? {
        val uri = targetIntent?.data ?: return null
        if (uri.scheme.equals("ven", ignoreCase = true) && uri.host.equals("diskusi", ignoreCase = true)) {
            return uri.pathSegments?.firstOrNull()?.takeIf { it.isNotBlank() }
        }
        return null
    }
}
