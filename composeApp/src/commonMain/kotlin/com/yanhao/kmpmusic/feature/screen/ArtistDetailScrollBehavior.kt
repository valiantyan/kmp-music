package com.yanhao.kmpmusic.feature.screen

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// 展开头图高度，足够容纳沉浸背景和歌手名。
internal val artistDetailExpandedHeaderHeight: Dp = 420.dp

// Toolbar 内容高度，不包含状态栏安全区。
internal val artistDetailToolbarContentHeight: Dp = 56.dp

// 顶部下拉放大最大高度，避免头图无限拉伸。
internal val artistDetailMaxPullStretchHeight: Dp = 96.dp

/**
 * 歌手详情页折叠滚动尺寸配置。
 *
 * @property expandedHeaderHeight 展开时头图和歌手信息区域高度。
 * @property toolbarContentHeight 不含状态栏的 Toolbar 内容高度。
 * @property statusBarInset 当前平台状态栏安全区高度。
 * @property maxPullStretchHeight 顶部下拉放大的最大高度。
 */
internal data class ArtistDetailScrollSpec(
    val expandedHeaderHeight: Dp,
    val toolbarContentHeight: Dp,
    val statusBarInset: Dp,
    val maxPullStretchHeight: Dp,
)

/**
 * 歌手详情页滚动派生状态，供 Compose 层渲染 Collapsing Toolbar。
 *
 * @property collapseProgress 头图折叠进度，固定限制在 0..1。
 * @property toolbarAlpha Toolbar 背景遮罩透明度。
 * @property toolbarTitleAlpha Toolbar 标题透明度。
 * @property expandedHeaderHeight 当前展开头图高度，包含下拉放大高度。
 * @property collapsedToolbarHeight 折叠 Toolbar 高度，包含状态栏安全区。
 * @property pullStretchHeight 当前下拉放大高度。
 * @property contentTopBarrier 正文可绘制区域顶部屏障，保证文字不会进入状态栏。
 * @property headerVisibleHeight 当前可见头图高度。
 * @property listContentTopPadding 列表首个内容相对 Toolbar 下方的顶部留白。
 * @property expandedContentAlpha 展开态歌手信息透明度。
 */
internal data class ArtistDetailScrollState(
    val collapseProgress: Float,
    val toolbarAlpha: Float,
    val toolbarTitleAlpha: Float,
    val expandedHeaderHeight: Dp,
    val collapsedToolbarHeight: Dp,
    val pullStretchHeight: Dp,
    val contentTopBarrier: Dp,
    val headerVisibleHeight: Dp,
    val listContentTopPadding: Dp,
    val expandedContentAlpha: Float,
)

/**
 * 创建歌手详情页折叠滚动配置，状态栏高度由当前平台窗口安全区提供。
 */
internal fun createArtistDetailScrollSpec(statusBarInset: Dp): ArtistDetailScrollSpec {
    return ArtistDetailScrollSpec(
        expandedHeaderHeight = artistDetailExpandedHeaderHeight,
        toolbarContentHeight = artistDetailToolbarContentHeight,
        statusBarInset = statusBarInset,
        maxPullStretchHeight = artistDetailMaxPullStretchHeight,
    )
}

/**
 * 根据列表滚动和下拉距离计算歌手详情页折叠状态。
 */
internal fun calculateArtistDetailScrollState(
    spec: ArtistDetailScrollSpec,
    scrollOffset: Dp,
    pullOffset: Dp,
): ArtistDetailScrollState {
    val collapsedToolbarHeight: Dp = spec.statusBarInset + spec.toolbarContentHeight
    val collapseDistance: Dp = calculateCollapseDistance(
        expandedHeaderHeight = spec.expandedHeaderHeight,
        collapsedToolbarHeight = collapsedToolbarHeight,
    )
    val safeScrollOffset: Dp = scrollOffset.coerceAtLeast(minimumValue = 0.dp)
    val collapseProgress: Float = (safeScrollOffset.value / collapseDistance.value).coerceIn(
        minimumValue = 0f,
        maximumValue = 1f,
    )
    val pullStretchHeight: Dp = pullOffset.coerceIn(
        minimumValue = 0.dp,
        maximumValue = spec.maxPullStretchHeight,
    )
    val expandedHeaderHeight: Dp = spec.expandedHeaderHeight + pullStretchHeight
    val headerVisibleHeight: Dp = calculateHeaderVisibleHeight(
        expandedHeaderHeight = spec.expandedHeaderHeight,
        collapsedToolbarHeight = collapsedToolbarHeight,
        collapseDistance = collapseDistance,
        scrollOffset = safeScrollOffset,
        pullStretchHeight = pullStretchHeight,
    )
    return ArtistDetailScrollState(
        collapseProgress = collapseProgress,
        toolbarAlpha = calculateToolbarAlpha(collapseProgress = collapseProgress),
        toolbarTitleAlpha = calculateToolbarTitleAlpha(collapseProgress = collapseProgress),
        expandedHeaderHeight = expandedHeaderHeight,
        collapsedToolbarHeight = collapsedToolbarHeight,
        pullStretchHeight = pullStretchHeight,
        contentTopBarrier = collapsedToolbarHeight,
        headerVisibleHeight = headerVisibleHeight,
        listContentTopPadding = (expandedHeaderHeight - collapsedToolbarHeight).coerceAtLeast(
            minimumValue = 0.dp,
        ),
        expandedContentAlpha = (1f - collapseProgress * 1.35f).coerceIn(
            minimumValue = 0f,
            maximumValue = 1f,
        ),
    )
}

// 折叠距离至少保留 1dp，避免极端小屏或异常 token 导致除零。
private fun calculateCollapseDistance(
    expandedHeaderHeight: Dp,
    collapsedToolbarHeight: Dp,
): Dp {
    return (expandedHeaderHeight - collapsedToolbarHeight).coerceAtLeast(minimumValue = 1.dp)
}

// Toolbar 背景不完全不透明，保留一点沉浸背景氛围。
private fun calculateToolbarAlpha(collapseProgress: Float): Float {
    return (collapseProgress * 0.96f).coerceIn(
        minimumValue = 0f,
        maximumValue = 0.96f,
    )
}

// 标题在接近折叠完成时渐显，避免展开态和头图上的歌手名抢焦点。
private fun calculateToolbarTitleAlpha(collapseProgress: Float): Float {
    return ((collapseProgress - 0.68f) / 0.32f).coerceIn(
        minimumValue = 0f,
        maximumValue = 1f,
    )
}

// 头图向上折叠到 Toolbar 高度后停止，正文由 [contentTopBarrier] 负责裁切。
private fun calculateHeaderVisibleHeight(
    expandedHeaderHeight: Dp,
    collapsedToolbarHeight: Dp,
    collapseDistance: Dp,
    scrollOffset: Dp,
    pullStretchHeight: Dp,
): Dp {
    val consumedHeight: Dp = scrollOffset.coerceIn(
        minimumValue = 0.dp,
        maximumValue = collapseDistance,
    )
    return (expandedHeaderHeight - consumedHeight + pullStretchHeight).coerceAtLeast(
        minimumValue = collapsedToolbarHeight,
    )
}
