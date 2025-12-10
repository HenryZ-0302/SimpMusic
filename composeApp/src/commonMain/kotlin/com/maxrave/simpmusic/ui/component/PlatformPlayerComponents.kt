package com.maxrave.simpmusic.ui.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.maxrave.domain.mediaservice.handler.ControlState
import com.maxrave.simpmusic.viewModel.UIEvent

/**
 * Platform-specific progress slider for the player
 * On Android: Uses LiquidSlider with Liquid Glass effect when enabled
 * On Desktop: Uses standard Material3 Slider
 */
@Composable
expect fun PlatformProgressSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    onValueChangeFinished: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    useLiquidGlass: Boolean = false,
)

/**
 * Platform-specific player control layout
 * On Android: Uses LiquidPlayerControlLayout with Liquid Glass effect when enabled
 * On Desktop: Uses standard PlayerControlLayout
 */
@Composable
expect fun PlatformPlayerControlLayout(
    controllerState: ControlState,
    onUIEvent: (UIEvent) -> Unit,
    useLiquidGlass: Boolean = false,
)
