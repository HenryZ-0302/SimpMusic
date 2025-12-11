package com.maxrave.simpmusic.ui.component

import androidx.compose.material3.Switch
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.maxrave.simpmusic.ui.component.liquidglass.LiquidToggle

@Composable
actual fun PlatformSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier,
    enabled: Boolean,
    useLiquidGlass: Boolean,
) {
    if (useLiquidGlass) {
        val backdrop = rememberLayerBackdrop {
            drawContent()
        }
        LiquidToggle(
            selected = { checked },
            onSelect = onCheckedChange,
            backdrop = backdrop,
            modifier = modifier,
        )
    } else {
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = modifier,
            enabled = enabled,
        )
    }
}
