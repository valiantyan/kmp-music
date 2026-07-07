package com.yanhao.kmpmusic.feature.desktop.screens

import com.yanhao.kmpmusic.domain.model.Song
import com.yanhao.kmpmusic.feature.app.MusicAppUiState

/**
 * 桌面最近播放页使用工作区表格宽度，不复用移动端窄列表布局。
 */
internal enum class DesktopRecentPlayedLayoutPolicy {
    WorkspaceTable,
}

/**
 * 桌面最近播放页展示模型，只消费统一过滤后的最近播放歌曲列表。
 *
 * @property title 页面标题。
 * @property eyebrow 页面副标题。
 * @property emptyTitle 空态标题。
 * @property emptyDetail 空态说明。
 * @property layoutPolicy 桌面布局策略。
 * @property rows 完整最近播放歌曲行。
 * @property hasManagementActions 是否暴露清空、编辑、筛选、排序或审计能力。
 */
internal data class DesktopRecentPlayedPageDisplayModel(
    val title: String,
    val eyebrow: String,
    val emptyTitle: String,
    val emptyDetail: String,
    val layoutPolicy: DesktopRecentPlayedLayoutPolicy,
    val rows: List<DesktopRecentPlayedSongDisplayModel>,
    val hasManagementActions: Boolean,
)

/**
 * 桌面最近播放页歌曲行展示模型，不携带播放或更多菜单动作语义。
 *
 * @property song 统一过滤后的歌曲实体。
 * @property indexLabel 桌面表格序号。
 * @property title 歌曲标题。
 * @property artist 歌手名。
 * @property album 专辑名。
 * @property duration 时长文案。
 * @property hasPlaybackAction 当前切片是否允许播放动作。
 * @property hasMoreAction 当前切片是否允许更多菜单动作。
 */
internal data class DesktopRecentPlayedSongDisplayModel(
    val song: Song,
    val indexLabel: String,
    val title: String,
    val artist: String,
    val album: String,
    val duration: String,
    val hasPlaybackAction: Boolean,
    val hasMoreAction: Boolean,
)

/**
 * 构造桌面最近播放页模型；调用方必须传入 [MusicAppUiState.recentSongs] 的过滤结果。
 */
internal fun buildDesktopRecentPlayedPageDisplayModel(
    songs: List<Song>,
): DesktopRecentPlayedPageDisplayModel {
    val rows: List<DesktopRecentPlayedSongDisplayModel> = songs.mapIndexed { index: Int, song: Song ->
        DesktopRecentPlayedSongDisplayModel(
            song = song,
            indexLabel = (index + 1).toString(),
            title = song.title,
            artist = song.artist,
            album = song.album,
            duration = song.duration,
            hasPlaybackAction = false,
            hasMoreAction = false,
        )
    }
    val eyebrow: String = if (rows.isEmpty()) {
        "播放歌曲后会生成最近播放歌曲列表"
    } else {
        "完整最近播放歌曲列表 · ${rows.size} 首"
    }
    return DesktopRecentPlayedPageDisplayModel(
        title = "最近播放",
        eyebrow = eyebrow,
        emptyTitle = "暂无最近播放",
        emptyDetail = "播放歌曲后会在这里显示最近听过的音乐。",
        layoutPolicy = DesktopRecentPlayedLayoutPolicy.WorkspaceTable,
        rows = rows,
        hasManagementActions = false,
    )
}
