package com.maxrave.simpmusic.ui.component.liquidglass

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.input.pointer.util.VelocityTrackerInput
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastCoerceIn
import androidx.compose.ui.util.fastRoundToInt
import androidx.compose.ui.util.lerp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberBackdrop
import com.kyant.backdrop.backdrops.rememberCombinedBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.shadow.InnerShadow
import com.kyant.backdrop.shadow.Shadow
import kotlinx.coroutines.flow.collectLatest

// 使用胶囊形圆角作为替代
private val CapsuleShape = RoundedCornerShape(50)

/**
 * Liquid Glass styled slider for music progress
 */
@Composable
fun LiquidSlider(
    value: () -> Float,
    onValueChange: (Float) -> Unit,
    onValueChangeFinished: (() -> Unit)? = null,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    backdrop: Backdrop,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    accentColor: Color = Color.Unspecified,
    trackColor: Color = Color.Unspecified,
) {
    val isLightTheme = !isSystemInDarkTheme()
    val resolvedAccentColor = if (accentColor == Color.Unspecified) {
        if (isLightTheme) Color(0xFF0088FF) else Color(0xFF0091FF)
    } else accentColor
    val resolvedTrackColor = if (trackColor == Color.Unspecified) {
        if (isLightTheme) Color(0xFF787878).copy(0.2f) else Color(0xFF787880).copy(0.36f)
    } else trackColor

    val trackBackdrop = rememberLayerBackdrop()
    val visibilityThreshold = 0.001f

    BoxWithConstraints(
        modifier.fillMaxWidth(),
        contentAlignment = Alignment.CenterStart
    ) {
        val trackWidth = constraints.maxWidth

        val isLtr = LocalLayoutDirection.current == LayoutDirection.Ltr
        val animationScope = rememberCoroutineScope()
        
        val currentQ by rememberUpdatedState(value) // 使用 State 包装函数类型
        val currentOnValueChange by rememberUpdatedState(onValueChange)
        val currentOnValueChangeFinished by rememberUpdatedState(onValueChangeFinished)

        val dampedDragAnimation = remember(animationScope, trackWidth, valueRange, isLtr) {
            DampedDragAnimation(
                animationScope = animationScope,
                initialValue = value(),
                valueRange = valueRange,
                visibilityThreshold = visibilityThreshold,
                initialScale = 1f,
                pressedScale = 1.5f,
                onDragStarted = {},
                onDragStopped = {
                    currentOnValueChangeFinished?.invoke()
                },
                onDrag = { _, dragAmount ->
                    if (enabled && dragAmount.x != 0f) {
                        // 使用最新的 value() 作为基准
                        val currentBase = currentQ()
                        val delta = (valueRange.endInclusive - valueRange.start) * (dragAmount.x / trackWidth)
                        val newTarget = if (isLtr) (currentBase + delta).coerceIn(valueRange)
                                        else (currentBase - delta).coerceIn(valueRange)
                        
                        // 1. 通知外部更新
                        currentOnValueChange(newTarget)
                        
                        // 2. 立即更新视觉位置，防止等待回流导致卡顿
                        dragTo(newTarget)
                    }
                }
            )
        }
        
        LaunchedEffect(dampedDragAnimation) {
            snapshotFlow { value() }
                .collectLatest { newValue ->
                    if (dampedDragAnimation.targetValue != newValue) {
                        dampedDragAnimation.updateValue(newValue)
                    }
                }
        }

        // Track background
        Box(Modifier.layerBackdrop(trackBackdrop)) {
            Box(
                Modifier
                    .clip(CapsuleShape)
                    .background(resolvedTrackColor)
                    .pointerInput(animationScope, enabled, trackWidth, valueRange, isLtr) {
                        if (enabled) {
                            awaitEachGesture {
                                val down = awaitFirstDown(requireUnconsumed = false)
                                down.consume()
                                
                                // 1. 按下即跳转 (Jump)
                                val position = down.position.x
                                val delta = (valueRange.endInclusive - valueRange.start) * (position / trackWidth)
                                val target = if (isLtr) valueRange.start + delta else valueRange.endInclusive - delta
                                val clamped = target.coerceIn(valueRange)
                                
                                // 直接调用 latest callback
                                currentOnValueChange(clamped)
                                dampedDragAnimation.dragTo(clamped)
                                
                                // 2. 进入连续拖动模式
                                var dragging = true
                                while (dragging) {
                                    val event = awaitPointerEvent()
                                    val change = event.changes.firstOrNull { it.id == down.id } ?: break // 手指抬起或丢失
                                    
                                    if (!change.pressed) {
                                        dragging = false
                                        currentOnValueChangeFinished?.invoke()
                                        break
                                    }
                                    
                                    val dragAmount = change.positionChange()
                                    // 只要位置改变，或者手指还在按着，就持续更新
                                    // 注意：dragTo 是绝对位置定位，这里我们重新计算绝对位置
                                    if (change.positionChange() != Offset.Zero) {
                                        change.consume()
                                        val currPos = change.position.x
                                        val currDelta = (valueRange.endInclusive - valueRange.start) * (currPos / trackWidth)
                                        val currTarget = if (isLtr) valueRange.start + currDelta else valueRange.endInclusive - currDelta
                                        val currClamped = currTarget.coerceIn(valueRange)
                                        
                                        currentOnValueChange(currClamped)
                                        dampedDragAnimation.dragTo(currClamped)
                                    }
                                }
                            }
                        }
                    }
                    .height(6f.dp)
                    .fillMaxWidth()
            )

            // Active track
            Box(
                Modifier
                    .clip(CapsuleShape)
                    .background(resolvedAccentColor)
                    .height(6f.dp)
                    .layout { measurable, constraints ->
                        val placeable = measurable.measure(constraints)
                        val width = (constraints.maxWidth * dampedDragAnimation.progress).fastRoundToInt()
                        layout(width, placeable.height) {
                            placeable.place(0, 0)
                        }
                    }
            )
        }

        // Thumb with liquid glass effect
        if (enabled) {
            Box(
                Modifier
                    .graphicsLayer {
                        translationX =
                            (-size.width / 2f + trackWidth * dampedDragAnimation.progress)
                                .fastCoerceIn(-size.width / 4f, trackWidth - size.width * 3f / 4f) * 
                                if (isLtr) 1f else -1f
                    }
                    .size(40f.dp, 24f.dp) // 基准尺寸用于定位计算
                ,
                contentAlignment = Alignment.Center // 确保中心对齐
            ) {
                // 触摸热区 - 显著增大 (80x60)
                Box(
                    Modifier
                        .size(80.dp, 60.dp)
                        .then(dampedDragAnimation.modifier)
                )

                // 视觉 Thumb - 保持原样 (40x24)
                Box(
                    Modifier
                        .drawBackdrop(
                            backdrop = rememberCombinedBackdrop(
                                backdrop,
                                rememberBackdrop(trackBackdrop) { drawBackdrop ->
                                    val progress = dampedDragAnimation.pressProgress
                                    val scaleX = lerp(2f / 3f, 1f, progress)
                                    val scaleY = lerp(0f, 1f, progress)
                                    scale(scaleX, scaleY) {
                                        drawBackdrop()
                                    }
                                }
                            ),
                            shape = { CapsuleShape },
                            effects = {
                                val progress = dampedDragAnimation.pressProgress
                                blur(8f.dp.toPx() * (1f - progress))
                                lens(
                                    10f.dp.toPx() * progress,
                                    14f.dp.toPx() * progress,
                                    chromaticAberration = true
                                )
                            },
                            highlight = {
                                val progress = dampedDragAnimation.pressProgress
                                Highlight.Ambient.copy(
                                    width = Highlight.Ambient.width / 1.5f,
                                    blurRadius = Highlight.Ambient.blurRadius / 1.5f,
                                    alpha = progress
                                )
                            },
                            shadow = {
                                Shadow(
                                    radius = 4f.dp,
                                    color = Color.Black.copy(alpha = 0.05f)
                                )
                            },
                            innerShadow = {
                                val progress = dampedDragAnimation.pressProgress
                                InnerShadow(
                                    radius = 4f.dp * progress,
                                    alpha = progress
                                )
                            },
                            layerBlock = {
                                scaleX = dampedDragAnimation.scaleX
                                scaleY = dampedDragAnimation.scaleY
                                val velocity = dampedDragAnimation.velocity / 10f
                                scaleX /= 1f - (velocity * 0.75f).fastCoerceIn(-0.2f, 0.2f)
                                scaleY *= 1f - (velocity * 0.25f).fastCoerceIn(-0.2f, 0.2f)
                            },
                            onDrawSurface = {
                                val progress = dampedDragAnimation.pressProgress
                                drawRect(Color.White.copy(alpha = 1f - progress))
                            }
                        )
                        .size(40f.dp, 24f.dp)
                )
            }
        }
    }
}
