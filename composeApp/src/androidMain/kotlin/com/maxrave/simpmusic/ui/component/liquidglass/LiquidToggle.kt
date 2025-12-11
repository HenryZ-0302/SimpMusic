package com.maxrave.simpmusic.ui.component.liquidglass

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastCoerceIn
import androidx.compose.ui.util.lerp
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.shadow.InnerShadow
import com.kyant.backdrop.shadow.Shadow
import kotlinx.coroutines.flow.collectLatest

// Capsule shape for the toggle (fully rounded)
private val CapsuleShape = RoundedCornerShape(50)

/**
 * Liquid Glass Toggle switch component
 * Ported from Kyant0/AndroidLiquidGlass demo
 */
@Composable
fun LiquidToggle(
    selected: () -> Boolean,
    onValueChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    initialProgress: Float = if (selected()) 1f else 0f
) {
    val backdrop = rememberLayerBackdrop {
        drawContent()
    }
    val dark = isSystemInDarkTheme()
    var trackColor by remember(dark) { mutableStateOf(if (dark) Color.Gray else Color.LightGray) }
    // 创建 track 的 LayerBackdrop
    val trackBackdrop = rememberLayerBackdrop {
        drawRect(trackColor)
        drawContent()
    }
    val isLtr = LocalLayoutDirection.current == LayoutDirection.Ltr
    val animationScope = rememberCoroutineScope()
    val density = LocalDensity.current
    val dragDivisorPx = remember(density) { with(density) { 14.dp.toPx() } }
    var targetValue by remember { mutableFloatStateOf(initialProgress) }
    val dampedDragAnimation = remember {
        DampedDragAnimation(
            animationScope = animationScope,
            initialValue = initialProgress,
            visibilityThreshold = 0.001f,
            valueRange = 0f..1f,
            initialScale = 1f,
            pressedScale = 1.5f,
            onDragStarted = {
                onValueChange(!selected())
            },
            onDragStopped = {
                onValueChange(progress >= 0.5f)
                targetValue = if (progress >= 0.5f) 1f else 0f
            },
            onDrag = { _, dragAmount ->
                targetValue = (value + dragAmount.x / dragDivisorPx * (if (isLtr) 1f else -1f)).fastCoerceIn(0f, 1f)
            }
        )
    }

    LaunchedEffect(Unit) {
        snapshotFlow { targetValue }
            .collectLatest {
                dampedDragAnimation.updateValue(it)
            }
    }

    // 同步外部 selected 状态变化
    LaunchedEffect(selected()) {
        val newTarget = if (selected()) 1f else 0f
        if (targetValue != newTarget) {
            targetValue = newTarget
        }
    }

    Box(
        modifier = modifier
            .size(52.dp, 32.dp) // 加宽一点确保 thumb 有足够空间移动
            .layerBackdrop(trackBackdrop)
            .clip(CapsuleShape)
            .drawBehind {
                drawRect(lerp(trackColor, Color(0xFF67C26A), dampedDragAnimation.progress))
            }
            .semantics { role = Role.Switch }
    ) {
        val density = LocalDensity.current
        // Thumb 圆形尺寸
        val thumbSize = 26.dp
        val thumbSizePx = remember(density) { with(density) { thumbSize.toPx() } }
        val trackWidthPx = remember(density) { with(density) { 52.dp.toPx() } }
        // 计算 thumb 在 track 内的移动范围
        val padding = (trackWidthPx - thumbSizePx) / 2f
        Box(
            Modifier
                .graphicsLayer {
                    // translationX: 从 -padding 到 +padding
                    translationX = lerp(-padding, padding, dampedDragAnimation.progress)
                }
                .then(dampedDragAnimation.modifier)
                .drawBackdrop(
                    backdrop = backdrop,
                    shape = { CapsuleShape },
                    effects = {
                        val progress = dampedDragAnimation.pressProgress
                        blur(5f.dp.toPx() * (1f - progress))
                        lens(
                            6f.dp.toPx() * progress,
                            8f.dp.toPx() * progress,
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
                            radius = 2.dp,
                            color = Color.Black.copy(alpha = 0.05f)
                        )
                    },
                    innerShadow = {
                        val progress = dampedDragAnimation.pressProgress
                        InnerShadow(
                            radius = 2.dp * progress,
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
                .size(thumbSize) // 使用圆形 thumb
                .align(Alignment.Center)
        )
    }
}
