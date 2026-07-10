package com.yanhao.kmpmusic.feature.app.surfaces

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.yanhao.kmpmusic.domain.model.LocalPlaylist
import com.yanhao.kmpmusic.feature.app.AddToPlaylistFlowState
import com.yanhao.kmpmusic.feature.app.MusicAppController

/**
 * 已有歌单列表在弹窗内部滚动，搜索功能已移除，因此这里始终展示完整目标集合。
 */
@Composable
internal fun ExistingPlaylistList(
    flow: AddToPlaylistFlowState,
    controller: MusicAppController,
    modifier: Modifier = Modifier,
) {
    LazyColumn(modifier = modifier) {
        item(contentType = "new-local-playlist-option") {
            NewPlaylistEntry(controller = controller)
            AddToPlaylistDivider()
        }
        itemsIndexed(
            items = flow.availablePlaylists,
            key = { _: Int, playlist: LocalPlaylist -> playlist.id },
            contentType = { _: Int, _: LocalPlaylist -> "local-playlist-option" },
        ) { index: Int, playlist: LocalPlaylist ->
            ExistingPlaylistRow(
                playlist = playlist,
                isSelected = playlist.id == flow.selectedPlaylistId,
                rowHeight = resolveExistingPlaylistRowHeight(index = index),
                onSelect = { controller.selectAddToPlaylistTarget(playlistId = playlist.id) },
            )
        }
    }
}

// 首个已有歌单行需要吃掉分隔线后的额外顶部节奏，对齐 Figma 中 68px 行高。
private fun resolveExistingPlaylistRowHeight(index: Int) = if (index == 0) {
    AddToPlaylistDialogDesignSpec.firstPlaylistRowHeight
} else {
    AddToPlaylistDialogDesignSpec.playlistRowHeight
}
