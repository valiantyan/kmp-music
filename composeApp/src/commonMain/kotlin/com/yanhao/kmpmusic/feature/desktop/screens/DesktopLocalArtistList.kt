package com.yanhao.kmpmusic.feature.desktop.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.yanhao.kmpmusic.domain.model.Artist
import com.yanhao.kmpmusic.feature.desktop.components.DesktopSectionEmptyMessage
import kotlinx.coroutines.flow.collect

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
    val listState: LazyListState = rememberLazyListState()
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
        DesktopLocalArtistToolbar(visualSpec = visualSpec)
        Spacer(modifier = Modifier.height(height = visualSpec.titleBottomSpacing))
        if (artists.isEmpty()) {
            DesktopSectionEmptyMessage(message = "扫描后会按歌手自动聚合。")
            return@Column
        }
        DesktopLocalArtistList(
            artists = artists,
            visualSpec = visualSpec,
            listState = listState,
            onArtistOpen = onArtistOpen,
            modifier = Modifier.weight(weight = 1f),
        )
    }
}

// 顶部标题区独立于列表滚动容器，滑动列表时位置保持固定。
@Composable
private fun DesktopLocalArtistToolbar(
    visualSpec: DesktopLocalArtistListVisualSpec,
) {
    Text(
        text = "歌手",
        color = Color(0xFF111C2D),
        fontSize = visualSpec.titleFontSize,
        lineHeight = visualSpec.titleLineHeight,
        fontWeight = FontWeight.Medium,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
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
    var scrollbarVisibilitySignal: Int by remember { mutableStateOf(value = 0) }
    LaunchedEffect(listState) {
        var previousScrollPosition: Pair<Int, Int> =
            listState.firstVisibleItemIndex to listState.firstVisibleItemScrollOffset
        snapshotFlow {
            listState.firstVisibleItemIndex to listState.firstVisibleItemScrollOffset
        }.collect { scrollPosition: Pair<Int, Int> ->
            if (scrollPosition != previousScrollPosition) {
                scrollbarVisibilitySignal += 1
                previousScrollPosition = scrollPosition
            }
        }
    }
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
            DesktopLocalArtistScrollbar(
                listState = listState,
                visibilitySignal = scrollbarVisibilitySignal,
                modifier = Modifier.align(alignment = Alignment.CenterEnd),
            )
        }
    }
}
