package com.yanhao.kmpmusic.feature.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yanhao.kmpmusic.core.theme.MusicColors
import com.yanhao.kmpmusic.core.theme.scaledDp
import com.yanhao.kmpmusic.core.theme.scaledSp
import com.yanhao.kmpmusic.domain.model.LocalMusicProblem
import com.yanhao.kmpmusic.domain.model.LocalMusicSourceSummary
import com.yanhao.kmpmusic.feature.components.SectionTitle

// 渲染来源和扫描问题，让部分失败不会阻塞成功歌曲展示。
@Composable
internal fun SourceSection(
    sources: List<LocalMusicSourceSummary>,
    problems: List<LocalMusicProblem>,
) {
    Column(verticalArrangement = Arrangement.spacedBy(scaledDp(12.dp))) {
        SectionTitle(title = "来源", meta = "${sources.size} 个")
        if (sources.isEmpty()) {
            EmptySourceState(text = "还没有可展示的扫描来源。")
        } else {
            sources.forEach { source ->
                SourceSummaryRow(source = source)
            }
        }
        SectionTitle(title = "扫描问题", meta = "${problems.size} 个")
        if (problems.isEmpty()) {
            Text(text = "没有扫描问题", color = MusicColors.Muted, fontSize = scaledSp(15.sp))
        } else {
            problems.forEach { problem ->
                ProblemSummaryRow(problem = problem)
            }
        }
    }
}

// 来源摘要用轻量行承载，避免来源页变成新的 mock 文件夹列表。
@Composable
private fun SourceSummaryRow(source: LocalMusicSourceSummary) {
    SummarySurface(
        title = source.displayName,
        detail = "${source.songCount} 首 · ${source.problemCount} 个问题",
    )
}

// 扫描问题摘要保留错误文案，方便后续真实 scanner 接入后定位。
@Composable
private fun ProblemSummaryRow(problem: LocalMusicProblem) {
    SummarySurface(
        title = problem.fileName,
        detail = problem.error.message,
    )
}

// 本地音乐页统一的说明行样式。
@Composable
private fun SummarySurface(
    title: String,
    detail: String,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MusicColors.Soft,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = scaledDp(16.dp), vertical = scaledDp(12.dp)),
            verticalArrangement = Arrangement.spacedBy(scaledDp(4.dp)),
        ) {
            Text(text = title, fontSize = scaledSp(16.sp), lineHeight = scaledSp(20.sp), fontWeight = FontWeight.Bold)
            Text(text = detail, color = MusicColors.Muted, fontSize = scaledSp(14.sp), lineHeight = scaledSp(18.sp))
        }
    }
}

// 来源页空态保持低调，避免把“尚未扫描”误读成失败。
@Composable
private fun EmptySourceState(text: String) {
    Text(text = text, color = MusicColors.Muted, fontSize = scaledSp(15.sp), fontWeight = FontWeight.SemiBold)
}
