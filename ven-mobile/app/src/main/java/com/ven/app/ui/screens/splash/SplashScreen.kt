package com.ven.app.ui.screens.splash

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ven.app.R
import com.ven.app.ui.theme.VexSize
import com.ven.app.ui.theme.VexSpace
import com.ven.app.ui.theme.VexTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Splash brand Compose yang melanjutkan splash native Android 12+
 * (DESIGN.md §8.0). Menampilkan logo, tagline, dan indikator memuat.
 */
@Composable
fun SplashScreen(
    onSplashFinished: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = VexTheme.colors
    val scale = remember { Animatable(0.85f) }
    val alpha = remember { Animatable(0f) }
    val loadingDescription = stringResource(R.string.splash_loading)

    LaunchedEffect(Unit) {
        launch {
            scale.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing),
            )
        }
        alpha.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing),
        )
        delay(250)
        onSplashFinished()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(colors.bg),
    ) {
        Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Image(
                painter = painterResource(id = R.drawable.logo_ven),
                contentDescription = stringResource(R.string.splash_logo_content_desc),
                modifier = Modifier
                    .size(width = 160.dp, height = 64.dp)
                    .scale(scale.value)
                    .alpha(alpha.value),
            )

            Spacer(modifier = Modifier.height(VexSpace.s4))

            Text(
                text = stringResource(R.string.splash_tagline),
                style = MaterialTheme.typography.bodySmall.copy(letterSpacing = 1.sp),
                color = colors.textSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.alpha(alpha.value),
            )
        }

        CircularProgressIndicator(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = VexSpace.s16)
                .size(VexSize.iconLg)
                .alpha(alpha.value)
                .semantics { contentDescription = loadingDescription },
            color = colors.primary,
            strokeWidth = 2.dp,
        )
    }
}
