package com.yanhao.kmpmusic.feature.desktop.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.yanhao.kmpmusic.domain.model.Song
import com.yanhao.kmpmusic.feature.desktop.DesktopMusicColors
import com.yanhao.kmpmusic.feature.desktop.DesktopMusicType
import com.yanhao.kmpmusic.feature.desktop.components.DesktopBackTitleToolbar

/**
 * 桌面最近播放页复用最近播放专用播放入口，保证点击任意行都使用完整最近播放队列。
 */
@Composable
internal fun DesktopRecentPlayedScreen(
    songs: List<Song>,
    currentSongId: String?,
    isPlaying: Boolean,
    onBack: () -> Unit,
    onSongPlay: (Song) -> Unit,
    onCurrentSongToggle: () -> Unit,
    onSongMore: (Song) -> Unit,
    onSongLike: (String) -> Unit,
) {
    val displayModel: DesktopRecentPlayedPageDisplayModel =
        buildDesktopRecentPlayedPageDisplayModel(
            songs = songs,
            currentSongId = currentSongId,
        )
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .background(Color(0xFFF9F9FF)),
    ) {
        DesktopBackTitleToolbar(
            title = displayModel.title,
            onBack = onBack,
        )
        if (displayModel.rows.isEmpty()) {
            DesktopRecentPlayedEmptyState(
                displayModel = displayModel,
                modifier = Modifier.weight(1f).padding(horizontal = 24.dp),
            )
        } else {
            DesktopHomeSongList(
                songs = songs,
                currentSongId = currentSongId,
                isPlaying = isPlaying,
                onSongPlay = { song: Song, _: List<Song> -> onSongPlay(song) },
                onCurrentSongToggle = onCurrentSongToggle,
                onMore = onSongMore,
                onLike = onSongLike,
                modifier = Modifier.weight(1f).padding(horizontal = 24.dp),
            )
        }
    }
}

/**
 * 空态明确告诉用户最近播放来自播放行为，避免误解为加载失败或隐藏管理页。
 */
@Composable
private fun DesktopRecentPlayedEmptyState(
    displayModel: DesktopRecentPlayedPageDisplayModel,
    modifier: Modifier,
) {
    Surface(
        modifier =
            modifier
                .fillMaxWidth()
                .height(180.dp),
        shape = RoundedCornerShape(18.dp),
        color = Color.White.copy(alpha = 0.72f),
        border =
            BorderStroke(
                width = 1.dp,
                color = DesktopMusicColors.Line,
            ),
    ) {
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp),
            contentAlignment = Alignment.CenterStart,
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = displayModel.emptyTitle,
                    color = DesktopMusicColors.Ink,
                    fontSize = DesktopMusicType.StatTitle,
                    fontWeight = FontWeight.ExtraBold,
                )
                Text(
                    text = displayModel.emptyDetail,
                    color = DesktopMusicColors.MutedStrong,
                    fontSize = DesktopMusicType.Body,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}
