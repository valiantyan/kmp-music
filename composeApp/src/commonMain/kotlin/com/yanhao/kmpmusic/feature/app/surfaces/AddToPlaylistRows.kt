package com.yanhao.kmpmusic.feature.app.surfaces

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.sp
import com.yanhao.kmpmusic.domain.model.CoverArt
import com.yanhao.kmpmusic.domain.model.LocalPlaylist
import com.yanhao.kmpmusic.feature.app.MusicAppController
import com.yanhao.kmpmusic.feature.components.CoverArtImage

/**
 * 新建入口复用 Figma 列表项结构，右侧空心圆只作为设计稿中的视觉占位。
 */
@Composable
internal fun NewPlaylistEntry(controller: MusicAppController) {
    AddToPlaylistOptionRow(
        modifier = Modifier.clickable { controller.openCreatePlaylistDialog() },
        title = "新建歌单",
        rowHeight = AddToPlaylistDialogDesignSpec.newPlaylistRowHeight,
        leadingContent = { NewPlaylistIcon() },
        isSelected = false,
    )
}

/**
 * 单个已有歌单选项，使用自绘单选环保证尺寸和颜色贴近 Figma。
 */
@Composable
internal fun ExistingPlaylistRow(
    playlist: LocalPlaylist,
    isSelected: Boolean,
    rowHeight: Dp,
    onSelect: () -> Unit,
) {
    AddToPlaylistOptionRow(
        modifier = Modifier.clickable(onClick = onSelect),
        title = playlist.name,
        rowHeight = rowHeight,
        leadingContent = { ExistingPlaylistCover() },
        isSelected = isSelected,
    )
}

/**
 * 新建入口与已有歌单之间的单像素分隔线。
 */
@Composable
internal fun AddToPlaylistDivider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = AddToPlaylistDialogDesignSpec.horizontalPadding)
            .height(height = AddToPlaylistDialogDesignSpec.dividerHeight)
            .background(color = AddToPlaylistDialogDesignSpec.dividerColor),
    )
}

/**
 * 统一列表项横向结构：左侧 40px 图块、中间标题、右侧 20px 单选控件。
 */
@Composable
private fun AddToPlaylistOptionRow(
    modifier: Modifier,
    title: String,
    rowHeight: Dp,
    leadingContent: @Composable () -> Unit,
    isSelected: Boolean,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(height = rowHeight)
            .padding(horizontal = AddToPlaylistDialogDesignSpec.horizontalPadding),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier.width(width = AddToPlaylistDialogDesignSpec.leadingSlotWidth),
            contentAlignment = Alignment.CenterStart,
        ) {
            leadingContent()
        }
        Text(
            modifier = Modifier.weight(weight = 1f),
            text = title,
            color = AddToPlaylistDialogDesignSpec.primaryTextColor,
            fontSize = 16.sp,
            lineHeight = 24.sp,
            fontWeight = FontWeight.Medium,
        )
        AddToPlaylistRadio(isSelected = isSelected)
    }
}

/**
 * 新建入口图标块使用 40px 浅灰圆角底和青绿色加号。
 */
@Composable
private fun NewPlaylistIcon() {
    Surface(
        modifier = Modifier.size(size = AddToPlaylistDialogDesignSpec.playlistCoverSize),
        shape = RoundedCornerShape(size = AddToPlaylistDialogDesignSpec.playlistCoverRadius),
        color = AddToPlaylistDialogDesignSpec.softContainerColor,
    ) {
        Icon(
            modifier = Modifier.padding(all = AddToPlaylistDialogDesignSpec.newPlaylistIconPadding),
            imageVector = Icons.Rounded.Add,
            contentDescription = null,
            tint = AddToPlaylistDialogDesignSpec.actionColor,
        )
    }
}

/**
 * 歌单弹窗没有详情封面数据，使用项目内真实封面资源避免泄漏 Figma 占位文本。
 */
@Composable
private fun ExistingPlaylistCover() {
    CoverArtImage(
        coverArt = CoverArt.CoverSeaDream,
        contentDescription = null,
        modifier = Modifier
            .size(size = AddToPlaylistDialogDesignSpec.playlistCoverSize)
            .clip(shape = RoundedCornerShape(size = AddToPlaylistDialogDesignSpec.playlistCoverRadius)),
    )
}

/**
 * 自绘单选控件避免 [androidx.compose.material3.RadioButton] 默认内边距破坏 20px 尺寸。
 */
@Composable
private fun AddToPlaylistRadio(isSelected: Boolean) {
    Box(
        modifier = Modifier
            .size(size = AddToPlaylistDialogDesignSpec.radioSize)
            .border(
                border = BorderStroke(
                    width = AddToPlaylistDialogDesignSpec.radioBorderWidth,
                    color = if (isSelected) {
                        AddToPlaylistDialogDesignSpec.actionColor
                    } else {
                        AddToPlaylistDialogDesignSpec.radioBorderColor
                    },
                ),
                shape = CircleShape,
            ),
        contentAlignment = Alignment.Center,
    ) {
        if (isSelected) {
            Box(
                modifier = Modifier
                    .size(size = AddToPlaylistDialogDesignSpec.radioDotSize)
                    .clip(shape = CircleShape)
                    .background(color = AddToPlaylistDialogDesignSpec.actionColor),
            )
        }
    }
}
