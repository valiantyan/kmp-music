package com.yanhao.kmpmusic.feature.desktop.navigation

import com.yanhao.kmpmusic.domain.model.SearchContext
import com.yanhao.kmpmusic.feature.app.LocalMusicSection
import com.yanhao.kmpmusic.feature.app.MusicAppUiState
import com.yanhao.kmpmusic.feature.app.NavigationState
import com.yanhao.kmpmusic.feature.app.RootTab
import com.yanhao.kmpmusic.feature.app.SecondaryScreen
import com.yanhao.kmpmusic.feature.app.SecondaryStackEntry

enum class DesktopRailDestination {
    Music,
    Albums,
    Artists,
    Playlists,
    Favorites,
    Me,
}

/** 桌面左侧导航按当前路由和来源栈解析唯一选中态。 */
fun MusicAppUiState.desktopRailDestination(): DesktopRailDestination = resolveDesktopRailDestination(navigationState = navigationState)

/** 纯函数拆出给测试使用，避免 UI 截图才能发现来源高亮回退。 */
internal fun resolveDesktopRailDestination(navigationState: NavigationState): DesktopRailDestination {
    val currentScreen: SecondaryScreen? = navigationState.secondaryScreen
    return when (currentScreen) {
        is SecondaryScreen.LocalMusic -> currentScreen.initialSection.toDesktopRailDestination()

        SecondaryScreen.LocalPlaylists,
        SecondaryScreen.LocalPlaylistManagement,
        SecondaryScreen.LocalPlaylistDetail,
        -> DesktopRailDestination.Playlists

        is SecondaryScreen.Search -> currentScreen.toDesktopRailDestination(navigationState = navigationState)

        SecondaryScreen.AlbumDetail,
        SecondaryScreen.ArtistDetail,
        -> navigationState.resolveDetailDestination()

        else -> navigationState.rootTab.toDesktopRailDestination()
    }
}

/** 一级页面直接映射到左栏入口，二级页不命中时才使用这个回退。 */
private fun RootTab.toDesktopRailDestination(): DesktopRailDestination =
    when (this) {
        RootTab.Home -> DesktopRailDestination.Music
        RootTab.Favorites -> DesktopRailDestination.Favorites
        RootTab.Me -> DesktopRailDestination.Me
    }

/** 本地音乐分段和左栏入口保持一一映射，来源页高亮依赖这里。 */
private fun LocalMusicSection.toDesktopRailDestination(): DesktopRailDestination =
    when (this) {
        LocalMusicSection.Albums -> DesktopRailDestination.Albums

        LocalMusicSection.Artists -> DesktopRailDestination.Artists

        LocalMusicSection.Songs,
        LocalMusicSection.Sources,
        -> DesktopRailDestination.Music
    }

/** 搜索页按上下文和二级来源栈恢复左栏高亮。 */
private fun SecondaryScreen.Search.toDesktopRailDestination(
    navigationState: NavigationState,
): DesktopRailDestination {
    if (context == SearchContext.Favorites) {
        return DesktopRailDestination.Favorites
    }
    return navigationState.resolveLocalMusicSourceDestination() ?: DesktopRailDestination.Music
}

/** 详情页优先沿用本地音乐二级来源，否则按当前根页面归属回退。 */
private fun NavigationState.resolveDetailDestination(): DesktopRailDestination {
    val sourceDestination: DesktopRailDestination? = resolveLocalMusicSourceDestination()
    if (sourceDestination != null) {
        return sourceDestination
    }
    return rootTab.toDesktopRailDestination()
}

/** 从当前页和回退栈中找到最近的本地音乐来源分段。 */
private fun NavigationState.resolveLocalMusicSourceDestination(): DesktopRailDestination? {
    val currentLocalMusic: SecondaryScreen.LocalMusic? = secondaryScreen as? SecondaryScreen.LocalMusic
    if (currentLocalMusic != null) {
        return currentLocalMusic.initialSection.toDesktopRailDestination()
    }
    return secondaryBackStack
        .asReversed()
        .mapNotNull { entry: SecondaryStackEntry -> entry.screen as? SecondaryScreen.LocalMusic }
        .firstOrNull()
        ?.initialSection
        ?.toDesktopRailDestination()
}
