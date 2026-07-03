package com.yanhao.kmpmusic.feature.screen

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// 展开头图占视口高度的比例，保证首屏视觉接近半屏歌手图。
private const val ARTIST_DETAIL_EXPANDED_HEADER_VIEWPORT_FRACTION = 0.50f

// 展开态内容组在头图中的锚点比例，让歌手名落在图片中下部。
private const val ARTIST_DETAIL_CONTENT_GROUP_HEADER_FRACTION = 0.58f

// Toolbar 内容高度，不包含状态栏安全区。
internal val artistDetailToolbarContentHeight: Dp = 56.dp

// 顶部下拉放大最大高度，避免头图无限拉伸。
internal val artistDetailMaxPullStretchHeight: Dp = 96.dp

// 展开态歌手名的一行标题估算高度，用于把列表 item 位置转成连续滚动距离。
internal val artistDetailExpandedTitleScrollHeight: Dp = 64.dp

// 旧头图播放入口移除后保留的锚点高度，避免正文组突然上跳。
internal val artistDetailPlayAllScrollHeight: Dp = 48.dp

// 播放全部标题行固定视觉高度，参与歌手图跟随滚动的距离换算。
internal val artistDetailSectionTitleScrollHeight: Dp = 80.dp

// 歌曲行主体高度来自文字列与垂直 padding，用于更深列表滚动时连续推进头图。
internal val artistDetailSongRowScrollHeight: Dp = 76.dp

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
 * @property collapseProgress Toolbar 渐显进度，固定限制在 0..1。
 * @property toolbarAlpha Toolbar 背景遮罩透明度。
 * @property toolbarTitleAlpha Toolbar 标题透明度。
 * @property expandedHeaderHeight 当前展开头图高度，包含下拉放大高度。
 * @property collapsedToolbarHeight 折叠 Toolbar 高度，包含状态栏安全区。
 * @property pullStretchHeight 当前下拉放大高度。
 * @property contentTopBarrier 正文可绘制区域顶部屏障，保证文字不会进入状态栏。
 * @property headerVisibleHeight 当前可见头图高度。
 * @property heroImageHeight 歌手图实际绘制高度，上滑时不裁切，只由 Toolbar 渐显表达折叠反馈。
 * @property heroImageOffset 歌手图跟随列表滚动的纵向偏移，避免背景层停住不动。
 * @property listHeaderSpacerHeight 完整头图折叠使用的列表顶部高度。
 * @property contentGroupSpacerHeight 展开态内容组锚点高度，允许标题和按钮叠在头图中下部。
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
    val heroImageHeight: Dp,
    val heroImageOffset: Dp,
    val listHeaderSpacerHeight: Dp,
    val contentGroupSpacerHeight: Dp,
    val expandedContentAlpha: Float,
)

/**
 * 创建歌手详情页折叠滚动配置，状态栏高度由当前平台窗口安全区提供。
 */
internal fun createArtistDetailScrollSpec(
    statusBarInset: Dp,
    viewportHeight: Dp,
): ArtistDetailScrollSpec {
    return ArtistDetailScrollSpec(
        expandedHeaderHeight = calculateExpandedHeaderHeight(viewportHeight = viewportHeight),
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
    val headerCollapseDistance: Dp = calculateCollapseDistance(
        expandedHeaderHeight = spec.expandedHeaderHeight,
        collapsedToolbarHeight = collapsedToolbarHeight,
    )
    val toolbarRevealDistance: Dp = calculateArtistDetailToolbarRevealDistance(
        expandedHeaderHeight = spec.expandedHeaderHeight,
        collapsedToolbarHeight = collapsedToolbarHeight,
    )
    val safeScrollOffset: Dp = scrollOffset.coerceAtLeast(minimumValue = 0.dp)
    val collapseProgress: Float = (safeScrollOffset.value / toolbarRevealDistance.value).coerceIn(
        minimumValue = 0f,
        maximumValue = 1f,
    )
    val pullStretchHeight: Dp = pullOffset.coerceIn(
        minimumValue = 0.dp,
        maximumValue = spec.maxPullStretchHeight,
    )
    val expandedHeaderHeight: Dp = spec.expandedHeaderHeight + pullStretchHeight
    val heroImageOffset: Dp = calculateHeroImageOffset(
        scrollOffset = safeScrollOffset,
        maxOffset = spec.expandedHeaderHeight,
    )
    val headerVisibleHeight: Dp = calculateHeaderVisibleHeight(
        expandedHeaderHeight = spec.expandedHeaderHeight,
        collapsedToolbarHeight = collapsedToolbarHeight,
        collapseDistance = headerCollapseDistance,
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
        heroImageHeight = expandedHeaderHeight,
        heroImageOffset = heroImageOffset,
        listHeaderSpacerHeight = (expandedHeaderHeight - collapsedToolbarHeight).coerceAtLeast(
            minimumValue = 0.dp,
        ),
        contentGroupSpacerHeight = calculateContentGroupSpacerHeight(
            expandedHeaderHeight = expandedHeaderHeight,
            collapsedToolbarHeight = collapsedToolbarHeight,
        ),
        expandedContentAlpha = (1f - collapseProgress * 1.35f).coerceIn(
            minimumValue = 0f,
            maximumValue = 1f,
        ),
    )
}

// 头图初始高度来自视口，而不是单个 Image 的固定高度。
private fun calculateExpandedHeaderHeight(viewportHeight: Dp): Dp {
    return viewportHeight * ARTIST_DETAIL_EXPANDED_HEADER_VIEWPORT_FRACTION
}

// 内容组锚点落在展开头图 55%-65% 区间，扣除 Toolbar 后成为 LazyColumn 的真实 spacer。
private fun calculateContentGroupSpacerHeight(
    expandedHeaderHeight: Dp,
    collapsedToolbarHeight: Dp,
): Dp {
    val contentTop: Dp = expandedHeaderHeight * ARTIST_DETAIL_CONTENT_GROUP_HEADER_FRACTION
    return (contentTop - collapsedToolbarHeight).coerceAtLeast(minimumValue = 0.dp)
}

// Toolbar 渐显跟随内容组锚点，而不是把歌手图高度裁掉。
internal fun calculateArtistDetailToolbarRevealDistance(
    expandedHeaderHeight: Dp,
    collapsedToolbarHeight: Dp,
): Dp {
    return calculateContentGroupSpacerHeight(
        expandedHeaderHeight = expandedHeaderHeight,
        collapsedToolbarHeight = collapsedToolbarHeight,
    ).coerceAtLeast(minimumValue = 1.dp)
}

// 把 [LazyColumn] 的首个可见 item 位置换算成连续滚动距离，避免 item 边界造成头图跳变。
internal fun calculateArtistDetailScrollOffsetFromListPosition(
    firstVisibleItemIndex: Int,
    firstVisibleItemScrollOffset: Dp,
    scrollSpec: ArtistDetailScrollSpec,
): Dp {
    val collapsedToolbarHeight: Dp = scrollSpec.statusBarInset + scrollSpec.toolbarContentHeight
    val contentSpacerHeight: Dp = calculateArtistDetailToolbarRevealDistance(
        expandedHeaderHeight = scrollSpec.expandedHeaderHeight,
        collapsedToolbarHeight = collapsedToolbarHeight,
    )
    val baseOffset: Dp = when (firstVisibleItemIndex) {
        0 -> 0.dp
        1 -> contentSpacerHeight
        2 -> contentSpacerHeight + artistDetailExpandedTitleScrollHeight
        3 -> contentSpacerHeight + artistDetailExpandedTitleScrollHeight + artistDetailPlayAllScrollHeight
        else -> calculateArtistDetailSongListBaseOffset(
            firstVisibleItemIndex = firstVisibleItemIndex,
            contentSpacerHeight = contentSpacerHeight,
        )
    }
    return (baseOffset + firstVisibleItemScrollOffset).coerceIn(
        minimumValue = 0.dp,
        maximumValue = scrollSpec.expandedHeaderHeight,
    )
}

// 歌曲列表后的 item 使用估算行高推进，保证深度滚动时头图自然离场。
private fun calculateArtistDetailSongListBaseOffset(
    firstVisibleItemIndex: Int,
    contentSpacerHeight: Dp,
): Dp {
    val songRowCount: Int = (firstVisibleItemIndex - 4).coerceAtLeast(minimumValue = 0)
    return contentSpacerHeight +
        artistDetailExpandedTitleScrollHeight +
        artistDetailPlayAllScrollHeight +
        artistDetailSectionTitleScrollHeight +
        artistDetailSongRowScrollHeight * songRowCount
}

// 歌手图跟随内容自然上移，但最多移出自身高度，避免无意义的超大负偏移。
private fun calculateHeroImageOffset(
    scrollOffset: Dp,
    maxOffset: Dp,
): Dp {
    return -scrollOffset.coerceIn(
        minimumValue = 0.dp,
        maximumValue = maxOffset,
    )
}

// 折叠距离至少保留 1dp，避免极端小屏或异常 token 导致除零。
private fun calculateCollapseDistance(
    expandedHeaderHeight: Dp,
    collapsedToolbarHeight: Dp,
): Dp {
    return (expandedHeaderHeight - collapsedToolbarHeight).coerceAtLeast(minimumValue = 1.dp)
}

// 完全折叠时使用不透明浅色 Toolbar，避免头图透出后显得发黑。
private fun calculateToolbarAlpha(collapseProgress: Float): Float {
    return collapseProgress.coerceIn(
        minimumValue = 0f,
        maximumValue = 1f,
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
