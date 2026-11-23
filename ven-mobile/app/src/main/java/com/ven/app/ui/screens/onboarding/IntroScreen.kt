package com.ven.app.ui.screens.onboarding

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ven.app.R
import com.ven.app.ui.components.ui.VexButton
import com.ven.app.ui.theme.ThemeMode
import com.ven.app.ui.theme.VexRadius
import com.ven.app.ui.theme.VexSize
import com.ven.app.ui.theme.VexSpace
import com.ven.app.ui.theme.VexTheme
import kotlinx.coroutines.flow.collect

/**
 * Pengenalan aplikasi sebelum login (DESIGN.md §8.0).
 * Murni stateless: perpindahan halaman dan penanda selesai dikelola [IntroViewModel].
 */
@Composable
fun IntroScreen(
    state: IntroUiState,
    onPageChange: (Int) -> Unit,
    onNextClick: () -> Unit,
    onSkipClick: () -> Unit,
    onStartClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = VexTheme.colors
    val pagerState = rememberPagerState(initialPage = state.pageIndex) { state.totalPages }

    LaunchedEffect(state.pageIndex) {
        if (pagerState.currentPage != state.pageIndex) {
            pagerState.animateScrollToPage(state.pageIndex)
        }
    }

    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.settledPage }.collect(onPageChange)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(colors.bg)
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        if (!state.isLastPage) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(VexSpace.s2)
                    .sizeIn(minWidth = VexSize.minTouch, minHeight = VexSize.minTouch)
                    .clip(RoundedCornerShape(VexRadius.full))
                    .clickable(role = Role.Button, onClick = onSkipClick),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(R.string.intro_skip),
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.textSecondary,
                )
            }
        }

        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
            beyondViewportPageCount = 1,
        ) { pageIndex ->
            IntroPage(slide = IntroSlide.entries[pageIndex])
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = VexSpace.s6, vertical = VexSpace.s8),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            PageIndicator(
                totalPages = state.totalPages,
                currentPage = pagerState.currentPage,
            )

            Spacer(modifier = Modifier.height(VexSpace.s6))

            VexButton(
                text = stringResource(
                    if (state.isLastPage) R.string.intro_start else R.string.intro_next,
                ),
                onClick = if (state.isLastPage) onStartClick else onNextClick,
                modifier = Modifier.widthIn(max = VexSize.authContentWidth),
            )
        }
    }
}

@Composable
private fun IntroPage(
    slide: IntroSlide,
    modifier: Modifier = Modifier,
) {
    val colors = VexTheme.colors
    val imageRes = slide.imageRes

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(
                start = VexSpace.s6,
                end = VexSpace.s6,
                top = VexSize.topBar,
                bottom = 160.dp,
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        if (imageRes != null) {
            Image(
                painter = painterResource(id = imageRes),
                contentDescription = stringResource(slide.imageContentDescriptionRes),
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxWidth(0.82f)
                    .widthIn(max = 320.dp)
                    .aspectRatio(1f),
            )
        } else {
            Image(
                painter = painterResource(id = R.drawable.logo_ven),
                contentDescription = stringResource(R.string.splash_logo_content_desc),
                modifier = Modifier.size(width = 160.dp, height = 64.dp),
            )
        }

        Spacer(modifier = Modifier.height(VexSpace.s8))

        Text(
            text = stringResource(slide.titleRes),
            style = MaterialTheme.typography.displayMedium,
            color = colors.text,
            textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(VexSpace.s3))

        Text(
            text = stringResource(slide.descriptionRes),
            style = MaterialTheme.typography.bodyMedium,
            color = colors.textSecondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(max = 320.dp),
        )
    }
}

@Composable
private fun PageIndicator(
    totalPages: Int,
    currentPage: Int,
) {
    val colors = VexTheme.colors

    Row(
        horizontalArrangement = Arrangement.spacedBy(VexSpace.s2),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(totalPages) { index ->
            val selected = index == currentPage
            val indicatorWidth by animateDpAsState(
                targetValue = if (selected) 20.dp else 8.dp,
                label = "IntroIndicatorWidth",
            )

            Box(
                modifier = Modifier
                    .width(indicatorWidth)
                    .height(8.dp)
                    .clip(RoundedCornerShape(VexRadius.full))
                    .background(if (selected) colors.primary else colors.border),
            )
        }
    }
}

@Preview(name = "IntroScreen - Dark", showBackground = true, backgroundColor = 0xFF1F1F1F)
@Composable
private fun IntroScreenDarkPreview() {
    VexTheme(mode = ThemeMode.Dark) {
        IntroScreen(
            state = IntroUiState(),
            onPageChange = {},
            onNextClick = {},
            onSkipClick = {},
            onStartClick = {},
        )
    }
}

@Preview(name = "IntroScreen - Light", showBackground = true, backgroundColor = 0xFFFFFFFF)
@Composable
private fun IntroScreenLightPreview() {
    VexTheme(mode = ThemeMode.Light) {
        IntroScreen(
            state = IntroUiState(pageIndex = IntroSlide.entries.lastIndex),
            onPageChange = {},
            onNextClick = {},
            onSkipClick = {},
            onStartClick = {},
        )
    }
}
