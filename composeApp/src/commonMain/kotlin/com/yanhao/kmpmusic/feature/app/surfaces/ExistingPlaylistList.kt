package com.yanhao.kmpmusic.feature.app.surfaces

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material3.Icon
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.yanhao.kmpmusic.core.theme.MusicColors
import com.yanhao.kmpmusic.domain.model.LocalPlaylist
import com.yanhao.kmpmusic.feature.app.AddToPlaylistFlowState
import com.yanhao.kmpmusic.feature.app.MusicAppController

/**
 * 已有歌单列表在弹窗内部滚动，避免超过规格要求的最大弹窗高度。
 */
@Composable
internal fun ExistingPlaylistList(
    flow: AddToPlaylistFlowState,
    controller: MusicAppController,
    modifier: Modifier = Modifier,
) {
    if (flow.availablePlaylists.isEmpty()) {
        Text(
            modifier = modifier.fillMaxWidth(),
            text = resolveAddToPlaylistEmptyStateText(flow = flow),
        )
        return
    }
    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(bottom = AddToPlaylistDialogDesignSpec.scrollBottomPadding),
        verticalArrangement = Arrangement.spacedBy(space = AddToPlaylistDialogDesignSpec.rowGap),
    ) {
        items(
            items = flow.availablePlaylists,
            key = { playlist: LocalPlaylist -> playlist.id },
            contentType = { "local-playlist-option" },
        ) { playlist: LocalPlaylist ->
            ExistingPlaylistRow(
                playlist = playlist,
                isSelected = playlist.id == flow.selectedPlaylistId,
                onSelect = { controller.selectAddToPlaylistTarget(playlistId = playlist.id) },
            )
        }
    }
}

/**
 * 空搜索和无歌单空态文案不同，帮助用户区分“还没创建”和“当前关键词无匹配”。
 */
internal fun resolveAddToPlaylistEmptyStateText(flow: AddToPlaylistFlowState): String {
    return if (!flow.hasAnyPlaylist || flow.playlistSearchQuery.trim().isEmpty()) {
        "暂无歌单"
    } else {
        "未找到相关歌单"
    }
}

/**
 * 单个已有歌单选项，使用熟悉的单选控件表达“一次只能选一个”。
 */
@Composable
private fun ExistingPlaylistRow(
    playlist: LocalPlaylist,
    isSelected: Boolean,
    onSelect: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(size = AddToPlaylistDialogDesignSpec.playlistRowRadius),
        color = MusicColors.Soft,
        onClick = onSelect,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = AddToPlaylistDialogDesignSpec.rowHorizontalPadding,
                    vertical = AddToPlaylistDialogDesignSpec.rowVerticalPadding,
                ),
            horizontalArrangement = Arrangement.spacedBy(space = AddToPlaylistDialogDesignSpec.rowGap),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ExistingPlaylistIcon()
            Text(
                modifier = Modifier.weight(weight = 1f),
                text = playlist.name,
            )
            RadioButton(
                selected = isSelected,
                onClick = onSelect,
            )
        }
    }
}

/**
 * 已有歌单列表项使用稳定占位封面，避免选中状态导致行高变化。
 */
@Composable
private fun ExistingPlaylistIcon() {
    Surface(
        modifier = Modifier.size(size = AddToPlaylistDialogDesignSpec.playlistCoverSize),
        shape = RoundedCornerShape(size = AddToPlaylistDialogDesignSpec.newPlaylistIconRadius),
        color = MusicColors.AccentSoft,
    ) {
        Icon(
            modifier = Modifier.padding(all = AddToPlaylistDialogDesignSpec.iconPadding),
            imageVector = Icons.Rounded.LibraryMusic,
            contentDescription = null,
            tint = MusicColors.AccentDeep,
        )
    }
}
