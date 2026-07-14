package com.yanhao.kmpmusic.feature.app.layout

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import com.yanhao.kmpmusic.core.theme.MusicDimens
import com.yanhao.kmpmusic.core.theme.scaledDp
import com.yanhao.kmpmusic.feature.app.ContentBottomSpace
import com.yanhao.kmpmusic.feature.app.MobileFixedBarMode
import com.yanhao.kmpmusic.feature.app.MusicAppController
import com.yanhao.kmpmusic.feature.app.MusicAppUiState
import com.yanhao.kmpmusic.feature.app.SecondaryScreen
import com.yanhao.kmpmusic.feature.app.routes.MobileRootScreenRoute
import com.yanhao.kmpmusic.feature.app.routes.MobileSecondaryScreenRoute
import com.yanhao.kmpmusic.feature.screen.LocalMusicDiscoveryPlatform

/**
 * 播放页覆盖层转场时长(320ms)，只作用于播放页自身，不驱动底层 chrome。
 */
private const val MOBILE_PLAYER_OVERLAY_TRANSITION_MILLIS = 320

/**
 * 根据导航状态渲染手机端页面内容，页面自行拥有滚动容器并共享底部避让空间。
 */
@Composable
fun MobileContentLayout(
    state: MusicAppUiState,
    controller: MusicAppController,
    fixedBarMode: MobileFixedBarMode,
    discoveryPlatform: LocalMusicDiscoveryPlatform = LocalMusicDiscoveryPlatform.Android,
    onScanLocalMusic: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val bottomPadding: Dp = getContentBottomPadding(contentBottomSpace = fixedBarMode.contentBottomSpace)
    val statusBarTopPadding: Dp = with(receiver = LocalDensity.current) {
        WindowInsets.statusBars.getTop(density = this).toDp()
    }
    val pagePadding: PaddingValues = PaddingValues(
        start = scaledDp(MusicDimens.PagePaddingHorizontal),
        top = scaledDp(MusicDimens.PagePaddingTop),
        end = scaledDp(MusicDimens.PagePaddingHorizontal),
        bottom = bottomPadding,
    )
    val saveableStateHolder = rememberSaveableStateHolder()
    saveableStateHolder.SaveableStateProvider(key = state.navigationState.chromeUnderlayScrollStateKey) {
        val secondaryScreen: SecondaryScreen? = state.navigationState.chromeUnderlaySecondaryScreen
        if (secondaryScreen != null) {
            MobileSecondaryScreenRoute(
                secondaryScreen = secondaryScreen,
                state = state,
                controller = controller,
                discoveryPlatform = discoveryPlatform,
                onScanLocalMusic = onScanLocalMusic,
                modifier = modifier.fillMaxSize(),
                contentPadding = pagePadding,
            )
        } else {
            MobileRootScreenRoute(
                state = state,
                controller = controller,
                discoveryPlatform = discoveryPlatform,
                modifier = modifier
                    .fillMaxSize()
                    .navigationBarsPadding(),
                contentPadding = PaddingValues(
                    top = statusBarTopPadding,
                    bottom = bottomPadding,
                ),
            )
        }
    }
}

/**
 * 渲染压在底层 chrome 上方的移动端覆盖页，防止底栏被无 chrome 目标页驱动隐藏动画。
 */
@Composable
fun MobileChromeOverlay(
    state: MusicAppUiState,
    controller: MusicAppController,
    discoveryPlatform: LocalMusicDiscoveryPlatform = LocalMusicDiscoveryPlatform.Android,
    onScanLocalMusic: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val pagePadding: PaddingValues = PaddingValues(
        start = scaledDp(MusicDimens.PagePaddingHorizontal),
        top = scaledDp(MusicDimens.PagePaddingTop),
        end = scaledDp(MusicDimens.PagePaddingHorizontal),
        bottom = getContentBottomPadding(contentBottomSpace = ContentBottomSpace.Fullscreen),
    )
    val saveableStateHolder = rememberSaveableStateHolder()
    var wasPlayerDismissedByDrag: Boolean by remember { mutableStateOf(value = false) }
    var retainedOverlayScrollStateKey: String? by remember { mutableStateOf(value = null) }
    val targetOverlayScreen: SecondaryScreen? = state.navigationState.chromeOverlayScreen
    val targetOverlayScrollStateKey: String? = targetOverlayScreen?.let { state.navigationState.scrollStateKey }
    val navigationBarBottomPx: Int = WindowInsets.navigationBars.getBottom(density = LocalDensity.current)
    LaunchedEffect(state.navigationState.chromeOverlayScreen) {
        if (state.navigationState.chromeOverlayScreen == SecondaryScreen.Player) {
            wasPlayerDismissedByDrag = false
        }
    }
    LaunchedEffect(targetOverlayScrollStateKey) {
        if (targetOverlayScrollStateKey != null) {
            retainedOverlayScrollStateKey = targetOverlayScrollStateKey
        }
    }
    AnimatedContent(
        targetState = targetOverlayScreen,
        modifier = modifier,
        transitionSpec = {
            buildChromeOverlayTransition(
                wasPlayerDismissedByDrag = wasPlayerDismissedByDrag,
                navigationBarBottomPx = navigationBarBottomPx,
            )
        },
        label = "MobileChromeOverlay",
    ) { overlayScreen: SecondaryScreen? ->
        if (overlayScreen == null) {
            Box(modifier = Modifier.fillMaxSize())
            return@AnimatedContent
        }
        if (shouldHidePlayerOverlayContentAfterDrag(
                overlayScreen = overlayScreen,
                targetOverlayScreen = targetOverlayScreen,
                wasDismissedByDrag = wasPlayerDismissedByDrag,
            )
        ) {
            Box(modifier = Modifier.fillMaxSize())
            return@AnimatedContent
        }
        val overlaySaveableStateKey: String = resolveOverlaySaveableStateKey(
            overlayScreen = overlayScreen,
            targetOverlayScreen = targetOverlayScreen,
            targetScrollStateKey = state.navigationState.scrollStateKey,
            retainedOverlayScrollStateKey = retainedOverlayScrollStateKey,
        )
        saveableStateHolder.SaveableStateProvider(key = overlaySaveableStateKey) {
            MobileOverlayScreenRoute(
                overlayScreen = overlayScreen,
                state = state,
                controller = controller,
                discoveryPlatform = discoveryPlatform,
                onScanLocalMusic = onScanLocalMusic,
                contentPadding = pagePadding,
                modifier = Modifier.fillMaxSize(),
                onPlayerDismissedByDrag = {
                    wasPlayerDismissedByDrag = true
                    controller.navigateBack()
                },
            )
        }
    }
}

/**
 * 选择覆盖页动画：播放页自底部展开，普通无 chrome 页直接覆盖。
 */
private fun AnimatedContentTransitionScope<SecondaryScreen?>.buildChromeOverlayTransition(
    wasPlayerDismissedByDrag: Boolean,
    navigationBarBottomPx: Int,
): ContentTransform {
    val enterTransition: EnterTransition = if (targetState == SecondaryScreen.Player) {
        slideInVertically(
            animationSpec = tween(durationMillis = MOBILE_PLAYER_OVERLAY_TRANSITION_MILLIS),
            initialOffsetY = { fullHeight: Int ->
                calculatePlayerOverlayExitOffsetY(
                    fullHeight = fullHeight,
                    navigationBarBottomPx = navigationBarBottomPx,
                )
            },
        )
    } else {
        fadeIn(animationSpec = tween(durationMillis = 0))
    }
    val exitTransition: ExitTransition = if (initialState == SecondaryScreen.Player) {
        if (shouldSkipPlayerExitTransitionAfterDrag(
                initialScreen = initialState,
                wasDismissedByDrag = wasPlayerDismissedByDrag,
            )
        ) {
            ExitTransition.None
        } else {
            slideOutVertically(
                animationSpec = tween(durationMillis = MOBILE_PLAYER_OVERLAY_TRANSITION_MILLIS),
                targetOffsetY = { fullHeight: Int ->
                    calculatePlayerOverlayExitOffsetY(
                        fullHeight = fullHeight,
                        navigationBarBottomPx = navigationBarBottomPx,
                    )
                },
            )
        }
    } else {
        fadeOut(animationSpec = tween(durationMillis = 0))
    }
    return (enterTransition togetherWith exitTransition).using(SizeTransform(clip = false))
}

/**
 * 播放页进出场目标必须越过系统导航栏区域，否则视觉上会从导航栏上方被截走。
 */
internal fun calculatePlayerOverlayExitOffsetY(
    fullHeight: Int,
    navigationBarBottomPx: Int,
): Int {
    return fullHeight + navigationBarBottomPx.coerceAtLeast(minimumValue = 0)
}

/**
 * outgoing 覆盖页仍在转场时继续使用进入时的 key，避免播放页重组后 palette 回到白色默认值。
 */
internal fun resolveOverlaySaveableStateKey(
    overlayScreen: SecondaryScreen,
    targetOverlayScreen: SecondaryScreen?,
    targetScrollStateKey: String,
    retainedOverlayScrollStateKey: String?,
): String {
    if (overlayScreen == targetOverlayScreen) {
        return targetScrollStateKey
    }
    return retainedOverlayScrollStateKey ?: targetScrollStateKey
}

/**
 * 手势关闭已经把播放页移出屏幕后，父级不能再从全屏顶部播放一次退出动画。
 */
internal fun shouldSkipPlayerExitTransitionAfterDrag(
    initialScreen: SecondaryScreen?,
    wasDismissedByDrag: Boolean,
): Boolean {
    return wasDismissedByDrag && initialScreen == SecondaryScreen.Player
}

/**
 * 拖拽关闭后 [AnimatedContent] 会继续保留 outgoing content；此时必须阻止旧播放页重组回全屏。
 */
internal fun shouldHidePlayerOverlayContentAfterDrag(
    overlayScreen: SecondaryScreen?,
    targetOverlayScreen: SecondaryScreen?,
    wasDismissedByDrag: Boolean,
): Boolean {
    return wasDismissedByDrag &&
        overlayScreen == SecondaryScreen.Player &&
        targetOverlayScreen != SecondaryScreen.Player
}

/**
 * 覆盖层只承载无 chrome 页面；播放页全屏，普通页使用常规二级页内边距。
 */
@Composable
private fun MobileOverlayScreenRoute(
    overlayScreen: SecondaryScreen,
    state: MusicAppUiState,
    controller: MusicAppController,
    discoveryPlatform: LocalMusicDiscoveryPlatform = LocalMusicDiscoveryPlatform.Android,
    onScanLocalMusic: () -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
    onPlayerDismissedByDrag: () -> Unit,
) {
    if (overlayScreen == SecondaryScreen.Player) {
        MobilePlayerOverlayGesture(
            onDismiss = onPlayerDismissedByDrag,
            modifier = modifier.fillMaxSize(),
        ) { playerModifier: Modifier ->
            MobileSecondaryScreenRoute(
                secondaryScreen = overlayScreen,
                state = state,
                controller = controller,
                discoveryPlatform = discoveryPlatform,
                onScanLocalMusic = onScanLocalMusic,
                modifier = playerModifier,
                contentPadding = contentPadding,
            )
        }
        return
    }
    val overlayInteractionSource: MutableInteractionSource = remember { MutableInteractionSource() }
    val blockingModifier: Modifier = modifier
        .fillMaxSize()
        .clickable(
            interactionSource = overlayInteractionSource,
            indication = null,
            onClick = {},
        )
    if (shouldRenderOverlayScreenDirectly(overlayScreen = overlayScreen)) {
        Box(
            modifier = blockingModifier
                .background(MaterialTheme.colorScheme.background),
        ) {
            MobileSecondaryScreenRoute(
                secondaryScreen = overlayScreen,
                state = state,
                controller = controller,
                discoveryPlatform = discoveryPlatform,
                onScanLocalMusic = onScanLocalMusic,
                modifier = Modifier.fillMaxSize(),
                contentPadding = contentPadding,
            )
        }
        return
    }
    Box(
        modifier = blockingModifier
            .background(MaterialTheme.colorScheme.background),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(contentPadding),
        ) {
            MobileSecondaryScreenRoute(
                secondaryScreen = overlayScreen,
                state = state,
                controller = controller,
                discoveryPlatform = discoveryPlatform,
                onScanLocalMusic = onScanLocalMusic,
                contentPadding = contentPadding,
            )
        }
    }
}

/**
 * 已自行管理 Toolbar 和正文滚动的覆盖页不能再由外层重复包裹滚动容器。
 */
internal fun shouldRenderOverlayScreenDirectly(overlayScreen: SecondaryScreen): Boolean {
    return overlayScreen == SecondaryScreen.About ||
        overlayScreen == SecondaryScreen.AudioScan ||
        overlayScreen == SecondaryScreen.LocalPlaylistManagement
}

/**
 * 根据固定底栏策略计算页面底部避让空间，避免隐藏播放器后留下空白。
 */
@Composable
private fun getContentBottomPadding(contentBottomSpace: ContentBottomSpace): Dp {
    return when (contentBottomSpace) {
        ContentBottomSpace.TopLevel -> scaledDp(MusicDimens.TopLevelContentBottom)
        ContentBottomSpace.SecondaryWithMiniPlayer -> scaledDp(MusicDimens.SecondaryContentBottom)
        ContentBottomSpace.Fullscreen -> scaledDp(MusicDimens.FullscreenContentBottom)
    }
}
