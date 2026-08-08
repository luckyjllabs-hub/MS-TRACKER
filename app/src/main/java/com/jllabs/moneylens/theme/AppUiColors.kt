package com.jllabs.moneylens.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color

@Immutable
data class AppUiColors(
    val page: Color,
    val card: Color,
    val ink: Color,
    val muted: Color,
    val divider: Color,
    val chip: Color,
    val accent: Color = Color(0xFF3B7A57)
)

@Composable
fun rememberAppUiColors(isDark: Boolean): AppUiColors = remember(isDark) {
    if (isDark) {
        AppUiColors(
            page = Color(0xFF121612),
            card = Color(0xFF1E241C),
            ink = Color(0xFFE8EDE8),
            muted = Color(0xFFA8B2A8),
            divider = Color(0xFF2E382E),
            chip = Color(0xFF2A322A)
        )
    } else {
        AppUiColors(
            page = Color(0xFFF4F3EF),
            card = Color.White,
            ink = Color(0xFF2D332A),
            muted = Color(0xFF5A6258),
            divider = Color(0xFFE0E4DC),
            chip = Color(0xFFE4E8E3)
        )
    }
}

@Composable
fun appUiColors(): AppUiColors {
    val scheme = MaterialTheme.colorScheme
    return AppUiColors(
        page = scheme.background,
        card = scheme.surface,
        ink = scheme.onSurface,
        muted = scheme.onSurfaceVariant,
        divider = scheme.surfaceVariant,
        chip = scheme.surfaceVariant
    )
}
