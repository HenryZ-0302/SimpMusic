package com.maxrave.simpmusic.ui.component

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.maxrave.domain.mediaservice.handler.ControlState
import com.maxrave.simpmusic.viewModel.UIEvent

@Composable
actual fun PlatformProgressSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    onValueChangeFinished: () -> Unit,
    modifier: Modifier,
    enabled: Boolean,
    useLiquidGlass: Boolean,
) {
    // Desktop: Use standard Material3 Slider (Liquid Glass not supported)
    CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides Dp.Unspecified) {
        Slider(
            value = value,
            onValueChange = onValueChange,
            onValueChangeFinished = onValueChangeFinished,
            valueRange = 0f..100f,
            modifier = modifier.fillMaxWidth(),
            enabled = enabled,
            track = { sliderState ->
                SliderDefaults.Track(
                    modifier = Modifier.height(5.dp),
                    enabled = enabled,
                    sliderState = sliderState,
                    colors = SliderDefaults.colors().copy(
                        thumbColor = Color.White,
                        activeTrackColor = Color.White,
                        inactiveTrackColor = Color.Transparent,
                    ),
                    thumbTrackGapSize = 0.dp,
                    drawTick = { _, _ -> },
                    drawStopIndicator = null,
                )
            },
            thumb = {
                SliderDefaults.Thumb(
                    modifier = Modifier
                        .height(18.dp)
                        .width(8.dp)
                        .padding(vertical = 4.dp),
                    thumbSize = DpSize(8.dp, 8.dp),
                    interactionSource = remember { MutableInteractionSource() },
                    colors = SliderDefaults.colors().copy(
                        thumbColor = Color.White,
                        activeTrackColor = Color.White,
                        inactiveTrackColor = Color.Transparent,
                    ),
                    enabled = enabled,
                )
            },
        )
    }
}

@Composable
actual fun PlatformPlayerControlLayout(
    controllerState: ControlState,
    onUIEvent: (UIEvent) -> Unit,
    useLiquidGlass: Boolean,
) {
    // Desktop: Use standard PlayerControlLayout (Liquid Glass not supported)
    PlayerControlLayout(
        controllerState = controllerState,
        onUIEvent = onUIEvent,
    )
}
