package com.yanhao.kmpmusic.feature.app.surfaces

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.PlaylistAdd
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yanhao.kmpmusic.core.theme.MusicColors
import com.yanhao.kmpmusic.domain.model.PlaybackSpeed
import com.yanhao.kmpmusic.domain.model.Song
import com.yanhao.kmpmusic.feature.app.MusicAppController
import com.yanhao.kmpmusic.feature.app.MusicAppUiState
import com.yanhao.kmpmusic.feature.app.SongMoreSourceContext
import com.yanhao.kmpmusic.feature.components.SongRow

/**
 * 跨端复用的全局底部面板。
 */
@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun AppPanels(
    state: MusicAppUiState,
    controller: MusicAppController,
) {
    if (state.isQueueOpen) {
        ModalBottomSheet(onDismissRequest = controller::closeQueue) {
            val queueSongs: List<Song> = state.queueSongs
            LazyColumn(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding(),
                contentPadding = PaddingValues(all = 21.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                item(key = "queue-title") {
                    Text(text = "播放队列", fontSize = 21.sp, fontWeight = FontWeight.ExtraBold)
                }
                items(
                    items = queueSongs,
                    key = { song: Song -> song.id },
                    contentType = { "queue-song" },
                ) { song: Song ->
                    SongRow(
                        song = song,
                        isCurrentSong = song.id == state.currentSongId,
                        currentPlaybackStatus = state.playbackStatus,
                        onPlay = { selectedSong: Song ->
                            controller.playSong(
                                song = selectedSong,
                                queueSongs = queueSongs,
                            )
                        },
                        onCurrentSongToggle = controller::togglePlayback,
                        onMore = controller::openMore,
                        dense = true,
                    )
                }
            }
        }
    }
    if (state.isPlaybackSpeedPanelOpen) {
        PlaybackSpeedPanel(
            selectedSpeed = state.playbackSpeed,
            onSelect = { playbackSpeed: PlaybackSpeed ->
                controller.setPlaybackSpeed(playbackSpeed = playbackSpeed)
                controller.closePlaybackSpeedPanel()
            },
            onDismiss = controller::closePlaybackSpeedPanel,
        )
    }
    state.moreSongId?.let { songId ->
        val song: Song? =
            resolveMorePanelSong(
                state = state,
                songId = songId,
            )
        if (song != null) {
            SongMorePanel(
                song = song,
                state = state,
                controller = controller,
            )
        }
    }
}

/**
 * 移动端播放倍速底部面板，选择项只来自 [PlaybackSpeed] 支持集合。
 */
@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun PlaybackSpeedPanel(
    selectedSpeed: PlaybackSpeed,
    onSelect: (PlaybackSpeed) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape =
            RoundedCornerShape(
                topStart = SongMorePanelDesignSpec.cornerRadius,
                topEnd = SongMorePanelDesignSpec.cornerRadius,
            ),
        containerColor = Color.White,
        scrimColor = MusicColors.DialogText.copy(alpha = 0.4f),
        dragHandle = null,
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding(),
        ) {
            SongMoreDragHandle()
            SongMoreHeader(songTitle = "播放倍速")
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(
                            horizontal = SongMorePanelDesignSpec.contentHorizontalPadding,
                            vertical = SongMorePanelDesignSpec.contentVerticalPadding,
                        ),
                verticalArrangement = Arrangement.spacedBy(space = 10.dp),
            ) {
                PlaybackSpeed.entries.forEach { playbackSpeed: PlaybackSpeed ->
                    PlaybackSpeedRow(
                        playbackSpeed = playbackSpeed,
                        isSelected = playbackSpeed == selectedSpeed,
                        onSelect = { onSelect(playbackSpeed) },
                    )
                }
            }
        }
    }
}

// 倍速选项保持固定高度，避免选中勾出现时挤压面板布局。
@Composable
private fun PlaybackSpeedRow(
    playbackSpeed: PlaybackSpeed,
    isSelected: Boolean,
    onSelect: () -> Unit,
) {
    Surface(
        modifier =
            Modifier
                .fillMaxWidth()
                .heightIn(min = 54.dp),
        shape = RoundedCornerShape(size = 14.dp),
        color =
            if (isSelected) {
                MusicColors.AccentSoft
            } else {
                MusicColors.DialogDivider.copy(alpha = 0.5f)
            },
        onClick = onSelect,
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(space = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = playbackSpeed.label,
                color = if (isSelected) MusicColors.AccentDeep else MusicColors.DialogText,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                lineHeight = 22.sp,
            )
            Spacer(modifier = Modifier.weight(weight = 1f))
            if (isSelected) {
                Icon(
                    imageVector = Icons.Rounded.Check,
                    contentDescription = "已选中",
                    tint = MusicColors.AccentDeep,
                    modifier = Modifier.size(size = 20.dp),
                )
            }
        }
    }
}

/**
 * Figma 节点 990:1150 的单曲更多面板视觉规格。
 */
internal object SongMorePanelDesignSpec {
    val initialHeightFraction: Float = 0.5f
    val maxHeightTopMargin: Dp = 56.dp
    val minMaxHeight: Dp = 256.dp
    val cornerRadius: Dp = 32.dp
    val handleWidth: Dp = 48.dp
    val handleHeight: Dp = 4.dp
    val handleTopPadding: Dp = 12.dp
    val handleBottomPadding: Dp = 4.dp
    val headerHorizontalPadding: Dp = 24.dp
    val headerTopPadding: Dp = 8.dp
    val headerBottomPadding: Dp = 17.dp
    val titleVerticalPadding: Dp = 8.dp
    val titleFontSize = 19.sp
    val titleLineHeight = 27.sp
    val contentHorizontalPadding: Dp = 24.dp
    val contentVerticalPadding: Dp = 16.dp
    val itemTopPadding: Dp = 16.dp
    val itemHeight: Dp = 64.dp
    val itemRadius: Dp = 16.dp
    val itemHorizontalPadding: Dp = 16.dp
    val itemVerticalPadding: Dp = 17.dp
    val itemGap: Dp = 16.dp
    val itemIconSize: Dp = 24.dp
    val itemContentGap: Dp = 16.dp
    val itemFontSize = 15.sp
    val itemLineHeight = 25.sp

    /**
     * Android 底部弹层默认先停在半屏锚点，避免简单操作面板一打开就顶到高位。
     */
    fun resolveInitialHeight(viewportHeight: Dp): Dp = viewportHeight * initialHeightFraction

    /**
     * Android 展开态保留顶部余量，不让底部弹层覆盖整屏和状态栏区域。
     */
    fun resolveMaxHeight(viewportHeight: Dp): Dp {
        val preferredMaxHeight: Dp =
            maxOf(
                a = viewportHeight - maxHeightTopMargin,
                b = minMaxHeight,
            )
        return minOf(
            a = viewportHeight,
            b = preferredMaxHeight,
        )
    }
}

/**
 * 单曲更多面板中收藏入口的视觉状态。
 */
internal enum class SongMoreFavoriteVisualState {
    Liked,
    Unliked,
}

/**
 * 只有非歌单详情来源能看到添加入口，避免本次切片扩大成歌单内管理流程。
 */
internal fun canShowAddToPlaylistAction(state: MusicAppUiState): Boolean = state.moreSongSourceContext != SongMoreSourceContext.LocalPlaylistDetail

/**
 * 解析更多面板收藏入口状态，兼容全局收藏集合和歌曲对象自身的同步字段。
 */
internal fun resolveSongMoreFavoriteVisualState(
    state: MusicAppUiState,
    song: Song,
): SongMoreFavoriteVisualState =
    if (state.likedSongIds.contains(element = song.id) || song.isLiked) {
        SongMoreFavoriteVisualState.Liked
    } else {
        SongMoreFavoriteVisualState.Unliked
    }

/**
 * 根据全局 [MusicAppUiState.moreSongId] 找到现有单曲更多面板要展示的歌曲，
 * 最近播放只复用面板不复制操作模型。
 */
internal fun resolveMorePanelSong(
    state: MusicAppUiState,
    songId: String,
): Song? = state.findKnownSong(songId = songId)

/**
 * 按 Figma 节点 990:1150 渲染单曲更多面板。
 */
@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun SongMorePanel(
    song: Song,
    state: MusicAppUiState,
    controller: MusicAppController,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
    ModalBottomSheet(
        onDismissRequest = controller::closeMore,
        sheetState = sheetState,
        shape =
            RoundedCornerShape(
                topStart = SongMorePanelDesignSpec.cornerRadius,
                topEnd = SongMorePanelDesignSpec.cornerRadius,
            ),
        containerColor = Color.White,
        scrimColor = MusicColors.DialogText.copy(alpha = 0.4f),
        dragHandle = null,
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val sheetMaxHeight: Dp = SongMorePanelDesignSpec.resolveMaxHeight(viewportHeight = maxHeight)
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(height = sheetMaxHeight)
                        .navigationBarsPadding(),
            ) {
                SongMoreDragHandle()
                SongMoreHeader(songTitle = song.title)
                SongMoreActionList(
                    song = song,
                    state = state,
                    controller = controller,
                )
            }
        }
    }
}

// 绘制设计稿中的短横拖拽柄，避免使用 Material 默认尺寸。
@Composable
private fun SongMoreDragHandle() {
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .background(color = Color.White)
                .padding(
                    top = SongMorePanelDesignSpec.handleTopPadding,
                    bottom = SongMorePanelDesignSpec.handleBottomPadding,
                ),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier =
                Modifier
                    .width(width = SongMorePanelDesignSpec.handleWidth)
                    .height(height = SongMorePanelDesignSpec.handleHeight)
                    .background(
                        color = MusicColors.DialogText.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(percent = 50),
                    ),
        )
    }
}

// 标题区保持居中，并用极浅分割线承接滚动内容。
@Composable
private fun SongMoreHeader(songTitle: String) {
    Column(modifier = Modifier.background(color = Color.White)) {
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(
                        start = SongMorePanelDesignSpec.headerHorizontalPadding,
                        top = SongMorePanelDesignSpec.headerTopPadding,
                        end = SongMorePanelDesignSpec.headerHorizontalPadding,
                        bottom = SongMorePanelDesignSpec.headerBottomPadding,
                    ),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                modifier = Modifier.padding(vertical = SongMorePanelDesignSpec.titleVerticalPadding),
                text = songTitle,
                color = MusicColors.DialogText,
                fontSize = SongMorePanelDesignSpec.titleFontSize,
                fontWeight = FontWeight.Medium,
                lineHeight = SongMorePanelDesignSpec.titleLineHeight,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        HorizontalDivider(color = MusicColors.Line.copy(alpha = 0.3f))
    }
}

// 更多操作列表的留白和行距按设计稿固定，底部空白由弹层高度自然承载。
@Composable
private fun SongMoreActionList(
    song: Song,
    state: MusicAppUiState,
    controller: MusicAppController,
) {
    val favoriteVisualState: SongMoreFavoriteVisualState =
        resolveSongMoreFavoriteVisualState(
            state = state,
            song = song,
        )
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = SongMorePanelDesignSpec.contentHorizontalPadding,
                    vertical = SongMorePanelDesignSpec.contentVerticalPadding,
                ),
    ) {
        Spacer(modifier = Modifier.height(height = SongMorePanelDesignSpec.itemTopPadding))
        Column(verticalArrangement = Arrangement.spacedBy(space = SongMorePanelDesignSpec.itemGap)) {
            BottomSheetAction(
                label = favoriteVisualState.resolveFavoriteActionLabel(),
                icon = favoriteVisualState.resolveFavoriteActionIcon(),
                onClick = { controller.toggleFavoriteFromMorePanel(songId = song.id) },
            )
            if (canShowAddToPlaylistAction(state = state)) {
                BottomSheetAction(
                    label = "添加到歌单",
                    icon = Icons.AutoMirrored.Rounded.PlaylistAdd,
                    onClick = { controller.openAddToPlaylistFlow(song = song) },
                )
            }
            BottomSheetAction(
                label = "查看专辑",
                icon = Icons.Rounded.Album,
                onClick = { controller.openAlbumFromSong(song = song) },
            )
            BottomSheetAction(
                label = "查看歌手",
                icon = Icons.Rounded.Person,
                onClick = { controller.openArtistFromSong(song = song) },
            )
        }
    }
}

// 将收藏状态映射为面板文案，避免在组合函数中散落分支。
private fun SongMoreFavoriteVisualState.resolveFavoriteActionLabel(): String =
    when (this) {
        SongMoreFavoriteVisualState.Liked -> "已收藏"
        SongMoreFavoriteVisualState.Unliked -> "加入收藏"
    }

// 将收藏状态映射为面板图标，已收藏使用填充心形，未收藏使用描边心形。
private fun SongMoreFavoriteVisualState.resolveFavoriteActionIcon(): ImageVector =
    when (this) {
        SongMoreFavoriteVisualState.Liked -> Icons.Rounded.Favorite
        SongMoreFavoriteVisualState.Unliked -> Icons.Rounded.FavoriteBorder
    }

/**
 * 更多操作面板中的单行动作，视觉对齐 Figma 节点 990:1150 的浅灰行项。
 */
@Composable
private fun BottomSheetAction(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit,
) {
    Surface(
        modifier =
            Modifier
                .fillMaxWidth()
                .heightIn(min = SongMorePanelDesignSpec.itemHeight),
        shape = RoundedCornerShape(size = SongMorePanelDesignSpec.itemRadius),
        color = MusicColors.DialogDivider.copy(alpha = 0.5f),
        onClick = onClick,
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = SongMorePanelDesignSpec.itemHorizontalPadding,
                        vertical = SongMorePanelDesignSpec.itemVerticalPadding,
                    ),
            horizontalArrangement = Arrangement.spacedBy(space = SongMorePanelDesignSpec.itemContentGap),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                modifier = Modifier.size(size = SongMorePanelDesignSpec.itemIconSize),
                imageVector = icon,
                contentDescription = null,
                tint = MusicColors.DialogText,
            )
            Text(
                text = label,
                color = MusicColors.DialogText,
                fontSize = SongMorePanelDesignSpec.itemFontSize,
                fontWeight = FontWeight.Medium,
                lineHeight = SongMorePanelDesignSpec.itemLineHeight,
            )
        }
    }
}
