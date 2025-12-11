package com.maxrave.simpmusic.ui.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Platform-specific Switch/Toggle component
 * On Android: Uses LiquidToggle with Liquid Glass effect when enabled
 * On Desktop: Uses standard Material3 Switch
 */
@Composable
expect fun PlatformSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    useLiquidGlass: Boolean = false,
)
