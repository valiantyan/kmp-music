package com.yanhao.kmpmusic.feature.desktop.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.yanhao.kmpmusic.domain.model.Artist
import com.yanhao.kmpmusic.feature.desktop.components.DesktopAutoHideLazyScrollbar
import com.yanhao.kmpmusic.feature.desktop.components.DesktopLazyScrollbarStyle
import com.yanhao.kmpmusic.feature.desktop.components.DesktopPageTitleToolbar
import com.yanhao.kmpmusic.feature.desktop.components.DesktopSectionEmptyMessage

/**
 * 桌面歌手页按 Figma `1085:709` 渲染单列列表。
 */
@Composable
internal fun DesktopLocalArtistPage(
    artists: List<Artist>,
    onArtistOpen: (Artist) -> Unit,
    modifier: Modifier = Modifier,
) {
    val visualSpec: DesktopLocalArtistListVisualSpec = resolveDesktopLocalArtistListVisualSpec()
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .background(color = visualSpec.pageBackgroundColor)
                .padding(
                    start = visualSpec.pageHorizontalPadding,
                    top = visualSpec.pageTopPadding,
                    end = visualSpec.pageHorizontalPadding,
                    bottom = visualSpec.pageHorizontalPadding,
                ),
    ) {
        DesktopPageTitleToolbar(title = "歌手")
        if (artists.isEmpty()) {
            DesktopSectionEmptyMessage(message = "扫描后会按歌手自动聚合。")
            return@Column
        }
        DesktopLocalArtistResultsList(
            artists = artists,
            onArtistOpen = onArtistOpen,
            modifier = Modifier.weight(weight = 1f),
        )
    }
}

/** 搜索结果复用歌手一级页的列表容器和行样式，但不重复页面标题与外层内边距。 */
@Composable
internal fun DesktopLocalArtistResultsList(
    artists: List<Artist>,
    onArtistOpen: (Artist) -> Unit,
    modifier: Modifier = Modifier,
) {
    val visualSpec: DesktopLocalArtistListVisualSpec = resolveDesktopLocalArtistListVisualSpec()
    val listState: LazyListState = rememberLazyListState()
    DesktopLocalArtistList(
        artists = artists,
        visualSpec = visualSpec,
        listState = listState,
        onArtistOpen = onArtistOpen,
        modifier = modifier,
    )
}

// 列表容器保留 Figma 的浅蓝底和圆角，滚动只发生在列表内部。
@Composable
private fun DesktopLocalArtistList(
    artists: List<Artist>,
    visualSpec: DesktopLocalArtistListVisualSpec,
    listState: LazyListState,
    onArtistOpen: (Artist) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(size = visualSpec.listRadius),
        color = visualSpec.listColor,
        border = BorderStroke(width = 1.dp, color = visualSpec.listBorderColor),
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                state = listState,
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(all = visualSpec.listBorderPadding),
            ) {
                itemsIndexed(
                    items = artists,
                    key = { _: Int, artist: Artist -> artist.id },
                    contentType = { _: Int, _: Artist -> "desktop-local-artist-row" },
                ) { index: Int, artist: Artist ->
                    val isLastRow: Boolean = index == artists.lastIndex
                    DesktopLocalArtistRow(
                        artist = artist,
                        index = index,
                        isLastRow = isLastRow,
                        visualSpec = visualSpec,
                        onArtistOpen = onArtistOpen,
                    )
                }
            }
            DesktopAutoHideLazyScrollbar(
                listState = listState,
                modifier = Modifier.align(alignment = Alignment.CenterEnd),
                style = DesktopLazyScrollbarStyle.HighContrast,
            )
        }
    }
}
