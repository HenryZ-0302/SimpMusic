package com.maxrave.simpmusic.ui.component.liquidglass

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.RepeatOne
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.Backdrop
import com.maxrave.domain.mediaservice.handler.ControlState
import com.maxrave.domain.mediaservice.handler.RepeatState
import com.maxrave.simpmusic.ui.theme.seed
import com.maxrave.simpmusic.viewModel.UIEvent

/**
 * Liquid Glass styled player control layout
 */
@Composable
fun LiquidPlayerControlLayout(
    controllerState: ControlState,
    backdrop: Backdrop,
    onUIEvent: (UIEvent) -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceEvenly,
        modifier = Modifier
            .fillMaxWidth()
            .height(96.dp)
            .padding(horizontal = 20.dp),
    ) {
        // Shuffle button
        Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
            LiquidCircleButtonSmall(
                onClick = { onUIEvent(UIEvent.Shuffle) },
                backdrop = backdrop,
            ) {
                Crossfade(targetState = controllerState.isShuffle, label = "Shuffle") { isShuffle ->
                    Icon(
                        imageVector = Icons.Rounded.Shuffle,
                        tint = if (isShuffle) seed else Color.White,
                        contentDescription = "Shuffle",
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
        }

        // Previous button
        Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
            LiquidCircleButton(
                onClick = {
                    if (controllerState.isPreviousAvailable) {
                        onUIEvent(UIEvent.Previous)
                    }
                },
                backdrop = backdrop,
                size = 48.dp,
            ) {
                Icon(
                    imageVector = Icons.Rounded.SkipPrevious,
                    tint = if (controllerState.isPreviousAvailable) Color.White else Color.Gray,
                    contentDescription = "Previous",
                    modifier = Modifier.size(28.dp),
                )
            }
        }

        // Play/Pause button (large)
        Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
            LiquidCircleButtonLarge(
                onClick = { onUIEvent(UIEvent.PlayPause) },
                backdrop = backdrop,
            ) {
                Crossfade(targetState = controllerState.isPlaying, label = "PlayPause") { isPlaying ->
                    Icon(
                        imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        tint = Color.White,
                        contentDescription = if (isPlaying) "Pause" else "Play",
                        modifier = Modifier.size(40.dp),
                    )
                }
            }
        }

        // Next button
        Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
            LiquidCircleButton(
                onClick = {
                    if (controllerState.isNextAvailable) {
                        onUIEvent(UIEvent.Next)
                    }
                },
                backdrop = backdrop,
                size = 48.dp,
            ) {
                Icon(
                    imageVector = Icons.Rounded.SkipNext,
                    tint = if (controllerState.isNextAvailable) Color.White else Color.Gray,
                    contentDescription = "Next",
                    modifier = Modifier.size(28.dp),
                )
            }
        }

        // Repeat button
        Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
            LiquidCircleButtonSmall(
                onClick = { onUIEvent(UIEvent.Repeat) },
                backdrop = backdrop,
            ) {
                Crossfade(targetState = controllerState.repeatState, label = "Repeat") { rs ->
                    when (rs) {
                        is RepeatState.None -> {
                            Icon(
                                imageVector = Icons.Rounded.Repeat,
                                tint = Color.White,
                                contentDescription = "Repeat Off",
                                modifier = Modifier.size(20.dp),
                            )
                        }
                        RepeatState.All -> {
                            Icon(
                                imageVector = Icons.Rounded.Repeat,
                                tint = seed,
                                contentDescription = "Repeat All",
                                modifier = Modifier.size(20.dp),
                            )
                        }
                        RepeatState.One -> {
                            Icon(
                                imageVector = Icons.Rounded.RepeatOne,
                                tint = seed,
                                contentDescription = "Repeat One",
                                modifier = Modifier.size(20.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}
