package com.maxrave.simpmusic.ui.component

import android.graphics.Bitmap
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.FloatingToolbarDefaults
import androidx.compose.material3.HorizontalFloatingToolbar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.ConstraintSet
import androidx.constraintlayout.compose.Dimension
import androidx.constraintlayout.compose.Visibility
import androidx.core.graphics.scale
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.compose.currentBackStackEntryAsState
import com.maxrave.domain.data.player.GenericMediaItem
import com.maxrave.logger.Logger
import com.maxrave.simpmusic.expect.ui.PlatformBackdrop
import com.maxrave.simpmusic.expect.ui.drawBackdropCustomShape
import com.maxrave.simpmusic.extension.greyScale
import com.maxrave.simpmusic.ui.navigation.destination.home.HomeDestination
import com.maxrave.simpmusic.ui.navigation.destination.library.LibraryDestination
import com.maxrave.simpmusic.ui.navigation.destination.search.SearchDestination
import com.maxrave.simpmusic.ui.screen.MiniPlayer
import com.maxrave.simpmusic.ui.theme.bottomBarSeedDark
import com.maxrave.simpmusic.ui.theme.customDarkGray
import com.maxrave.simpmusic.ui.theme.customGray
import com.maxrave.simpmusic.ui.theme.transparent
import com.maxrave.simpmusic.ui.theme.typo
import com.maxrave.simpmusic.ui.theme.white
import com.maxrave.simpmusic.viewModel.SharedViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.stringResource
import java.nio.IntBuffer
import kotlin.math.roundToInt
import kotlin.reflect.KClass
import kotlin.time.Duration.Companion.seconds
import androidx.compose.ui.graphics.lerp as colorLerp

private const val TAG = "LiquidGlassAppBottomNavigationBar"

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
actual fun LiquidGlassAppBottomNavigationBar(
    startDestination: Any,
    navController: NavController,
    backdrop: PlatformBackdrop,
    viewModel: SharedViewModel,
    isScrolledToTop: Boolean,
    onOpenNowPlaying: () -> Unit,
    reloadDestinationIfNeeded: (KClass<*>) -> Unit
) {
    val density = LocalDensity.current
    val layer = rememberGraphicsLayer()
    val luminanceAnimation = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()

    val customGrayColor by animateColorAsState(
        targetValue = colorLerp(customGray, customDarkGray, luminanceAnimation.value * 1.25f),
        animationSpec = tween(1000),
        label = "CustomGrayColorAnimation",
    )

    LaunchedEffect(layer) {
        val buffer = IntBuffer.allocate(25)
        while (isActive) {
            try {
                withContext(Dispatchers.IO) {
                    val imageBitmap = layer.toImageBitmap()
                    val thumbnail =
                        imageBitmap
                            .asAndroidBitmap()
                            .scale(5, 5, false)
                            .copy(Bitmap.Config.ARGB_8888, false)
                    buffer.rewind()
                    thumbnail.copyPixelsToBuffer(buffer)
                }
            } catch (e: Exception) {
                Logger.e(TAG, "Error getting pixels from layer: ${e.localizedMessage}")
            }
            val averageLuminance =
                (0 until 25).sumOf { index ->
                    val color = buffer.get(index)
                    val r = (color shr 16 and 0xFF) / 255f
                    val g = (color shr 8 and 0xFF) / 255f
                    val b = (color and 0xFF) / 255f
                    0.2126 * r + 0.7152 * g + 0.0722 * b
                } / 25
            luminanceAnimation.animateTo(
                averageLuminance.coerceAtMost(0.8).toFloat(),
                tween(500),
            )
            delay(1.seconds)
        }
    }

    val nowPlayingData by viewModel.nowPlayingState.collectAsStateWithLifecycle()
    // MiniPlayer visibility logic
    var isShowMiniPlayer by rememberSaveable {
        mutableStateOf(true)
    }

    val currentBackStackEntry by navController.currentBackStackEntryAsState()
    // 只包含 Home 和 Library（Search 单独处理）
    val tabScreens = listOf(BottomNavScreen.Home, BottomNavScreen.Library)
    val bottomNavScreens =
        listOf(
            BottomNavScreen.Home,
            BottomNavScreen.Search,
            BottomNavScreen.Library,
        )
    var selectedIndex by rememberSaveable {
        mutableIntStateOf(
            when (startDestination) {
                is HomeDestination -> 0
                is LibraryDestination -> 1
                else -> 0
            },
        )
    }
    var previousSelectedIndex by rememberSaveable {
        mutableIntStateOf(selectedIndex)
    }
    var isExpanded by rememberSaveable {
        mutableStateOf(true)
    }

    var isInSearchDestination by remember {
        mutableStateOf(false)
    }
    
    // 拖动相关状态
    var isDragging by remember { mutableStateOf(false) }
    var dragOffset by remember { mutableFloatStateOf(0f) }
    val animatedDragOffset by animateFloatAsState(
        targetValue = if (isDragging) dragOffset else 0f,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = 400f),
        label = "DragOffset"
    )
    
    // 按压动画状态
    var pressedButtonIndex by remember { mutableIntStateOf(-1) }
    val buttonScales = tabScreens.mapIndexed { index, _ ->
        animateFloatAsState(
            targetValue = if (pressedButtonIndex == index) 0.92f else 1f,
            animationSpec = spring(dampingRatio = 0.6f, stiffness = 500f),
            label = "ButtonScale$index"
        )
    }

    LaunchedEffect(nowPlayingData) {
        isShowMiniPlayer = !(nowPlayingData?.mediaItem == null || nowPlayingData?.mediaItem == GenericMediaItem.EMPTY)
    }

    LaunchedEffect(currentBackStackEntry) {
        currentBackStackEntry?.destination?.let { current ->
            Logger.d(TAG, "LiquidGlassAppBottomNavigationBar: current route: ${current.route}")
            isInSearchDestination = current.hasRoute(SearchDestination::class)
            // 同步 selectedIndex
            when {
                current.hasRoute(HomeDestination::class) -> selectedIndex = 0
                current.hasRoute(LibraryDestination::class) -> selectedIndex = 1
            }
        }
    }

    LaunchedEffect(isInSearchDestination) {
        isExpanded = !isInSearchDestination
    }

    var updateConstraints by remember {
        mutableStateOf(true)
    }

    var constraintSet by remember {
        mutableStateOf(
            decoupledConstraints(isShowMiniPlayer, isExpanded),
        )
    }

    LaunchedEffect(isShowMiniPlayer, isExpanded) {
        constraintSet = decoupledConstraints(isShowMiniPlayer, isExpanded)
        updateConstraints = false
    }

    LaunchedEffect(updateConstraints) {
        if (updateConstraints) {
            constraintSet = decoupledConstraints(isShowMiniPlayer, isExpanded)
            updateConstraints = false
        }
    }

    LaunchedEffect(isScrolledToTop) {
        Logger.d(TAG, "isScrolledToTop: $isScrolledToTop")
        if (!isInSearchDestination) {
            isExpanded = isScrolledToTop
        }
    }

    ConstraintLayout(
        constraintSet = constraintSet,
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(
                    WindowInsets.navigationBars.asPaddingValues(),
                )
                .padding(
                    bottom = 8.dp,
                )
                .imePadding(),
        animateChangesSpec = spring(stiffness = 400f, dampingRatio = 0.85f),
    ) {
        /**
         * LTR: HOME -> LIBRARY | SEARCH
         */
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier =
                Modifier
                    .padding(start = 16.dp)
                    .wrapContentSize()
                    .layoutId("toolbar"),
        ) {
            HorizontalFloatingToolbar(
                modifier =
                    Modifier
                        .drawBackdropCustomShape(
                            backdrop,
                            layer,
                            luminanceAnimation.value,
                            CircleShape,
                        )
                        .then(
                            if (!isExpanded) {
                                Modifier.size(48.dp)
                            } else {
                                Modifier.wrapContentSize()
                            },
                        )
                        .graphicsLayer {
                            // 拖动时的微妙偏移效果
                            translationX = animatedDragOffset * 0.1f
                        }
                        .pointerInput(tabScreens.size) {
                            // 添加拖动手势来切换 Tab
                            detectHorizontalDragGestures(
                                onDragStart = { 
                                    isDragging = true
                                    dragOffset = 0f
                                },
                                onDragEnd = {
                                    isDragging = false
                                    // 根据拖动方向切换 Tab
                                    val threshold = 80f
                                    if (dragOffset < -threshold && selectedIndex < tabScreens.size - 1) {
                                        // 向左拖 -> 切换到下一个
                                        val newIndex = selectedIndex + 1
                                        previousSelectedIndex = selectedIndex
                                        selectedIndex = newIndex
                                        val screen = tabScreens[newIndex]
                                        navController.navigate(screen.destination) {
                                            popUpTo(navController.graph.startDestinationId) {
                                                saveState = true
                                            }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    } else if (dragOffset > threshold && selectedIndex > 0) {
                                        // 向右拖 -> 切换到上一个
                                        val newIndex = selectedIndex - 1
                                        previousSelectedIndex = selectedIndex
                                        selectedIndex = newIndex
                                        val screen = tabScreens[newIndex]
                                        navController.navigate(screen.destination) {
                                            popUpTo(navController.graph.startDestinationId) {
                                                saveState = true
                                            }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    }
                                    dragOffset = 0f
                                },
                                onDragCancel = {
                                    isDragging = false
                                    dragOffset = 0f
                                }
                            ) { _, dragAmount ->
                                dragOffset += dragAmount
                            }
                        }
                        .onGloballyPositioned {
                            updateConstraints = true
                        },
                contentPadding =
                    PaddingValues(
                        horizontal = if (isExpanded) 4.dp else 0.dp,
                    ),
                colors =
                    FloatingToolbarDefaults
                        .standardFloatingToolbarColors()
                        .copy(
                            toolbarContainerColor = transparent,
                        ),
                expanded = isExpanded,
                trailingContent = {
                    var buttonSize by remember { mutableStateOf(0.dp to 0.dp) }
                    tabScreens.forEachIndexed { index, screen ->
                        Box {
                            if (selectedIndex == index) {
                                Box(
                                    modifier =
                                        Modifier
                                            .size(buttonSize.first, buttonSize.second)
                                            .clip(CircleShape)
                                            .blur(8.dp),
                                )
                            }
                            Button(
                                modifier =
                                    Modifier
                                        .scale(buttonScales[index].value)
                                        .graphicsLayer {
                                            // 按压时的微妙下沉效果
                                            if (pressedButtonIndex == index) {
                                                shadowElevation = 2f
                                            }
                                        }
                                        .pointerInput(index) {
                                            detectTapGestures(
                                                onPress = {
                                                    pressedButtonIndex = index
                                                    tryAwaitRelease()
                                                    pressedButtonIndex = -1
                                                }
                                            )
                                        }
                                        .onGloballyPositioned {
                                            if (selectedIndex == index) {
                                                buttonSize = with(density) { it.size.width.toDp() to it.size.height.toDp() }
                                            }
                                        },
                                onClick = {
                                    if (selectedIndex == index) {
                                        if (currentBackStackEntry?.destination?.hierarchy?.any {
                                                it.hasRoute(screen.destination::class)
                                            } == true
                                        ) {
                                            reloadDestinationIfNeeded(
                                                screen.destination::class,
                                            )
                                        } else {
                                            navController.navigate(screen.destination)
                                        }
                                    } else {
                                        previousSelectedIndex = selectedIndex
                                        selectedIndex = index
                                        navController.navigate(screen.destination) {
                                            popUpTo(navController.graph.startDestinationId) {
                                                saveState = true
                                            }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    }
                                },
                                shape = CircleShape,
                                colors =
                                    ButtonDefaults
                                        .buttonColors()
                                        .copy(
                                            disabledContainerColor = transparent,
                                            containerColor =
                                                if (selectedIndex == index) {
                                                    customGrayColor
                                                } else {
                                                    transparent
                                                },
                                            contentColor =
                                                if (selectedIndex == index) {
                                                    bottomBarSeedDark
                                                } else {
                                                    white
                                                },
                                        ),
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                ) {
                                    Icon(
                                        when (screen) {
                                            BottomNavScreen.Home -> Icons.Rounded.Home
                                            BottomNavScreen.Search -> Icons.Rounded.Search
                                            BottomNavScreen.Library -> Icons.Rounded.LibraryMusic
                                        },
                                        "",
                                    )
                                    Text(
                                        stringResource(screen.title),
                                        style =
                                            if (selectedIndex == index) {
                                                typo().bodySmall.copy(
                                                    fontWeight = FontWeight.Bold,
                                                )
                                            } else {
                                                typo().bodySmall.greyScale()
                                            },
                                        color =
                                            if (selectedIndex == index) {
                                                bottomBarSeedDark
                                            } else {
                                                white
                                            },
                                    )
                                }
                            }
                        }
                    }
                },
            ) {
                if (!isExpanded) {
                    val screen = tabScreens.getOrElse(selectedIndex) { BottomNavScreen.Home }
                    val previousScreen = tabScreens.getOrElse(previousSelectedIndex) { BottomNavScreen.Home }
                    IconButton(
                        modifier =
                            Modifier.size(
                                FloatingToolbarDefaults.ContainerSize.value.dp,
                            ),
                        shape = CircleShape,
                        onClick = {
                            isExpanded = true
                        },
                        colors =
                            IconButtonDefaults.iconButtonColors().copy(
                                contentColor = bottomBarSeedDark,
                            ),
                    ) {
                        Icon(
                            when (screen) {
                                BottomNavScreen.Home -> Icons.Rounded.Home
                                BottomNavScreen.Library -> Icons.Rounded.LibraryMusic
                                else -> Icons.Rounded.Home
                            },
                            "",
                        )
                    }
                }
            }

            if (isExpanded) {
                Spacer(Modifier.size(12.dp))
            }

            val searchColor by animateColorAsState(
                targetValue = if (luminanceAnimation.value > 0.6f) Color.Black else Color.White,
                label = "MiniPlayerTextColor",
                animationSpec = tween(500),
            )

            // 按压动画状态 for search button
            var searchPressed by remember { mutableStateOf(false) }
            val searchScale by animateFloatAsState(
                targetValue = if (searchPressed) 0.92f else 1f,
                animationSpec = spring(dampingRatio = 0.6f, stiffness = 500f),
                label = "SearchScale"
            )

            AnimatedVisibility(
                visible = !isInSearchDestination && isExpanded,
                enter = slideInHorizontally(tween(100)) { it / 2 },
                exit = slideOutHorizontally(tween(100)) { -it / 2 },
            ) {
                FloatingActionButton(
                    modifier =
                        Modifier
                            .drawBackdropCustomShape(
                                backdrop,
                                layer,
                                luminanceAnimation.value,
                                CircleShape,
                            )
                            .scale(searchScale)
                            .pointerInput(Unit) {
                                detectTapGestures(
                                    onPress = {
                                        searchPressed = true
                                        tryAwaitRelease()
                                        searchPressed = false
                                    }
                                )
                            },
                    elevation =
                        FloatingActionButtonDefaults.elevation(
                            0.dp,
                            0.dp,
                            0.dp,
                            0.dp,
                        ),
                    onClick = {
                        navController.navigate(BottomNavScreen.Search.destination) {
                            popUpTo(navController.graph.startDestinationId) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    shape = CircleShape,
                    containerColor = transparent,
                    contentColor = transparent,
                ) {
                    Icon(
                        Icons.Outlined.Search,
                        "",
                        tint = searchColor,
                    )
                }
            }
        }

        // MiniPlayer
        MiniPlayer(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp)
                .height(56.dp)
                .layoutId("miniPlayer"),
            backdrop = backdrop,
            onClick = {
                onOpenNowPlaying()
            },
            onClose = {
                viewModel.stopPlayer()
                viewModel.isServiceRunning = false
            },
        )
    }
}

private fun decoupledConstraints(
    isMiniplayerShow: Boolean = true,
    isExpanded: Boolean,
): ConstraintSet =
    ConstraintSet {
        val toolbar = createRefFor("toolbar")
        constrain(toolbar) {
            bottom.linkTo(parent.bottom)
            width = Dimension.wrapContent
            height = Dimension.wrapContent
            if (!isExpanded) {
                start.linkTo(parent.start)
            } else {
                start.linkTo(parent.start)
                end.linkTo(parent.end)
            }
        }
        val miniPlayer = createRefFor("miniPlayer")
        constrain(miniPlayer) {
            if (!isExpanded) {
                start.linkTo(toolbar.end, margin = 8.dp)
                end.linkTo(parent.end)
                top.linkTo(toolbar.top)
                bottom.linkTo(toolbar.bottom)
                width = if (isMiniplayerShow) Dimension.fillToConstraints else Dimension.wrapContent
            } else {
                start.linkTo(parent.start)
                end.linkTo(parent.end)
                bottom.linkTo(toolbar.top, margin = 12.dp)
                width = if (isMiniplayerShow) Dimension.matchParent else Dimension.wrapContent
            }
            visibility =
                if (isMiniplayerShow) {
                    Visibility.Visible
                } else {
                    Visibility.Gone
                }
        }
    }