package com.yanhao.kmpmusic.feature.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.yanhao.kmpmusic.domain.model.LibraryStats
import com.yanhao.kmpmusic.domain.model.Song

/**
 * 我的页，按 Figma 节点 919:439 还原个人中心首屏，同时保留真实扫描和最近播放入口。
 */
@Composable
fun MeScreen(
    recentSongs: List<Song>,
    currentSongId: String?,
    libraryStats: LibraryStats,
    localPlaylistCount: Int,
    onScanMusic: () -> Unit,
    onSongsStatClick: () -> Unit,
    onPlaylistsStatClick: () -> Unit,
    onRecentPlayedViewAll: () -> Unit,
    onRecentSongPlay: (Song) -> Unit,
    onRecentSongMore: (Song) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
) {
    LazyColumn(
        modifier =
            modifier
                .fillMaxSize()
                .background(color = meBackgroundColor),
        contentPadding =
            PaddingValues(
                top = contentPadding.calculateTopPadding() + meContentTopPadding,
                bottom = contentPadding.calculateBottomPadding() + 40.dp,
            ),
    ) {
        item(key = "me-figma-content", contentType = "me-figma-content") {
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = meHorizontalPadding),
                verticalArrangement = Arrangement.spacedBy(space = meSectionGap),
            ) {
                MeProfileSection()
                MeStatsSection(
                    libraryStats = libraryStats,
                    localPlaylistCount = localPlaylistCount,
                    onSongsClick = onSongsStatClick,
                    onPlaylistsClick = onPlaylistsStatClick,
                )
                MeQuickActionsSection(onScanMusic = onScanMusic)
                MeRecentPlayedSummarySection(
                    recentSongs = recentSongs,
                    currentSongId = currentSongId,
                    onViewAll = onRecentPlayedViewAll,
                    onSongPlay = onRecentSongPlay,
                    onSongMore = onRecentSongMore,
                )
                MeSettingsMenuSection()
            }
        }
    }
}
