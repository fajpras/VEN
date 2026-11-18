package com.ven.app.ui.components.ui

import androidx.annotation.DrawableRes
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ven.app.R
import com.ven.app.ui.theme.CategoryColorMap
import com.ven.app.ui.theme.ThemeMode
import com.ven.app.ui.theme.VexRadius
import com.ven.app.ui.theme.VexSize
import com.ven.app.ui.theme.VexSpace
import com.ven.app.ui.theme.VexTheme

private fun mixWith(color: Color, target: Color, amount: Float): Color = Color(
    red = color.red + (target.red - color.red) * amount,
    green = color.green + (target.green - color.green) * amount,
    blue = color.blue + (target.blue - color.blue) * amount,
    alpha = color.alpha,
)

/**
 * Chip pilihan kategori interaktif untuk form atau filter pencarian.
 */
@Composable
fun VexSelectableChip(
    text: String,
    selected: Boolean,
    onSelectedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val colors = VexTheme.colors

    val animatedBgColor by animateColorAsState(
        targetValue = if (selected) colors.primary.copy(alpha = 0.10f) else colors.surfaceInput.copy(alpha = 0.45f),
        animationSpec = tween(durationMillis = 180),
        label = "chipBgColor",
    )
    val animatedBorderColor by animateColorAsState(
        targetValue = if (selected) colors.primary else colors.border.copy(alpha = 0.5f),
        animationSpec = tween(durationMillis = 180),
        label = "chipBorderColor",
    )
    val animatedTextColor by animateColorAsState(
        targetValue = if (selected) colors.primary else colors.text,
        animationSpec = tween(durationMillis = 180),
        label = "chipTextColor",
    )

    val shape = RoundedCornerShape(VexRadius.full)
    val borderWidth = if (selected) 1.5.dp else 1.dp

    Row(
        modifier = modifier
            .height(VexSize.chip)
            .clip(shape)
            .border(
                width = borderWidth,
                color = animatedBorderColor,
                shape = shape,
            )
            .background(animatedBgColor)
            .clickable(
                enabled = enabled,
                role = Role.Checkbox,
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(color = animatedTextColor.copy(alpha = 0.18f)),
                onClick = { onSelectedChange(!selected) },
            )
            .padding(horizontal = VexSpace.s4, vertical = VexSpace.s2),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        if (selected) {
            Icon(
                painter = painterResource(id = R.drawable.ic_action_check),
                contentDescription = null,
                tint = animatedTextColor,
                modifier = Modifier
                    .size(14.dp)
                    .padding(end = 4.dp),
            )
        }
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                letterSpacing = 0.2.sp,
            ),
            color = animatedTextColor,
            textAlign = TextAlign.Center,
            maxLines = 1,
        )
    }
}

/**
 * Badge pill statis penampil kategori untuk kartu karya dan daftar portofolio.
 */
@Composable
fun VexTagChip(
    text: String,
    categoryColor: Color,
    modifier: Modifier = Modifier,
    @DrawableRes iconRes: Int? = R.drawable.ic_category_star,
) {
    VexSkillChip(
        text = text,
        categoryColor = categoryColor,
        modifier = modifier,
        iconRes = iconRes,
        onClick = null,
    )
}

/**
 * Chip pill keahlian atau kategori karya dengan ikon di sisi kiri.
 */
@Composable
fun VexSkillChip(
    text: String,
    categoryColor: Color,
    modifier: Modifier = Modifier,
    @DrawableRes iconRes: Int? = R.drawable.ic_category_star,
    onClick: (() -> Unit)? = null,
) {
    val isDark = VexTheme.colors.bg.luminance() < 0.5f
    val categoryLuminance = categoryColor.luminance()
    val accent = when {
        isDark && categoryLuminance < 0.42f -> mixWith(categoryColor, Color.White, 0.45f)
        !isDark && categoryLuminance > 0.45f -> mixWith(categoryColor, Color.Black, 0.38f)
        else -> categoryColor
    }
    val clickModifier = if (onClick != null) {
        Modifier.clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = ripple(color = accent.copy(alpha = 0.16f)),
            role = Role.Button,
            onClick = onClick,
        )
    } else {
        Modifier
    }

    val shape = RoundedCornerShape(VexRadius.full)

    Row(
        modifier = modifier
            .height(26.dp)
            .clip(shape)
            .background(accent.copy(alpha = if (isDark) 0.15f else 0.11f))
            .border(
                width = 1.dp,
                color = accent.copy(alpha = if (isDark) 0.34f else 0.30f),
                shape = shape,
            )
            .then(clickModifier)
            .padding(horizontal = 10.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        if (iconRes != null && iconRes != 0) {
            Box(
                modifier = Modifier
                    .size(15.dp)
                    .clip(RoundedCornerShape(VexRadius.full))
                    .background(accent.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(id = iconRes),
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(9.dp),
                )
            }
        }
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall.copy(
                fontWeight = FontWeight.SemiBold,
                fontSize = 11.5.sp,
                letterSpacing = 0.2.sp,
            ),
            color = accent,
            textAlign = TextAlign.Center,
            maxLines = 1,
        )
    }
}

/**
 * Chip penghitung jumlah tambahan item tersembunyi.
 */
@Composable
fun VexCounterChip(
    count: Int,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    val colors = VexTheme.colors
    val clickModifier = if (onClick != null) {
        Modifier.clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = ripple(color = colors.text.copy(alpha = 0.2f)),
            role = Role.Button,
            onClick = onClick,
        )
    } else {
        Modifier
    }

    val shape = RoundedCornerShape(VexRadius.full)

    Box(
        modifier = modifier
            .height(24.dp)
            .clip(shape)
            .border(
                width = 1.dp,
                color = colors.border.copy(alpha = 0.4f),
                shape = shape,
            )
            .background(colors.surfaceInput)
            .then(clickModifier)
            .padding(horizontal = VexSpace.s2, vertical = VexSpace.s1),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "+$count",
            style = MaterialTheme.typography.bodySmall.copy(
                fontWeight = FontWeight.SemiBold,
                fontSize = 11.5.sp,
            ),
            color = colors.textSecondary,
            textAlign = TextAlign.Center,
        )
    }
}

@Preview(name = "Chips Preview - Dark", showBackground = true, backgroundColor = 0xFF191919)
@Composable
private fun VexChipDarkPreview() {
    VexTheme(mode = ThemeMode.Dark) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                VexSelectableChip(text = "Prototyping", selected = true, onSelectedChange = {})
                VexSelectableChip(text = "Figma", selected = true, onSelectedChange = {})
                VexSelectableChip(text = "Sketch", selected = false, onSelectedChange = {})
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                VexSkillChip(text = "3D", categoryColor = CategoryColorMap["3D"] ?: Color(0xFFFF9A00))
                VexSkillChip(text = "Cybersecurity", categoryColor = CategoryColorMap["Cybersecurity"] ?: Color(0xFF0E7C66))
                VexSkillChip(text = "UI/UX", categoryColor = CategoryColorMap["UI/UX"] ?: Color(0xFFD81B8C))
                VexSkillChip(text = "Software", categoryColor = CategoryColorMap["Software"] ?: Color(0xFF1565C0))
            }
        }
    }
}

@Preview(name = "Chips Preview - Light", showBackground = true, backgroundColor = 0xFFFFFFFF)
@Composable
private fun VexChipLightPreview() {
    VexTheme(mode = ThemeMode.Light) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                VexSelectableChip(text = "Prototyping", selected = true, onSelectedChange = {})
                VexSelectableChip(text = "Figma", selected = true, onSelectedChange = {})
                VexSelectableChip(text = "Sketch", selected = false, onSelectedChange = {})
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                VexSkillChip(text = "3D", categoryColor = CategoryColorMap["3D"] ?: Color(0xFFFF9A00))
                VexSkillChip(text = "Cybersecurity", categoryColor = CategoryColorMap["Cybersecurity"] ?: Color(0xFF0E7C66))
                VexSkillChip(text = "UI/UX", categoryColor = CategoryColorMap["UI/UX"] ?: Color(0xFFD81B8C))
                VexSkillChip(text = "Software", categoryColor = CategoryColorMap["Software"] ?: Color(0xFF1565C0))
            }
        }
    }
}
