package com.yanhao.kmpmusic.feature.screen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yanhao.kmpmusic.domain.model.LibraryStats

/**
 * 我的页统计项，只把真实歌曲数接入业务状态，其余 Figma 静态占位保持不变。
 *
 * @property value 统计数值。
 * @property label 统计标签。
 */
private data class MeStatItem(
    val value: String,
    val label: String,
)

/**
 * 三列统计区按 Figma 使用白底和细分割线，不再使用旧版绿色卡片。
 */
@Composable
internal fun MeStatsSection(
    libraryStats: LibraryStats,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(size = meStatsRadius),
        color = meBackgroundColor,
        border = BorderStroke(width = 1.dp, color = meOutlineColor),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(all = meStatsPadding),
            horizontalArrangement = Arrangement.spacedBy(space = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val items: List<MeStatItem> = buildMeStatItems(libraryStats = libraryStats)
            MeStatColumn(item = items[0], modifier = Modifier.weight(weight = 1f))
            MeStatsDivider()
            MeStatColumn(item = items[1], modifier = Modifier.weight(weight = 1f))
            MeStatsDivider()
            MeStatColumn(item = items[2], modifier = Modifier.weight(weight = 1f))
        }
    }
}

// 构造统计项时只读取真实曲库歌曲数，避免把 Figma 静态数字误当真实歌单能力。
private fun buildMeStatItems(
    libraryStats: LibraryStats,
): List<MeStatItem> {
    return listOf(
        MeStatItem(value = libraryStats.songCount.toString(), label = "歌曲"),
        MeStatItem(value = "12", label = "歌单"),
        MeStatItem(value = "365", label = "听歌时长"),
    )
}

// 单个统计列使用居中层级贴近 Figma 的 Paragraph 结构。
@Composable
private fun MeStatColumn(
    item: MeStatItem,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = item.value,
            color = meAccentDarkColor,
            fontSize = 16.sp,
            lineHeight = 24.sp,
            fontWeight = FontWeight.Normal,
        )
        Text(
            text = item.label,
            color = meMetaColor,
            fontSize = 12.sp,
            lineHeight = 16.sp,
            fontWeight = FontWeight.Medium,
        )
    }
}

// 统计分隔线只保留 Figma 的细线节奏，不额外增加卡片阴影。
@Composable
private fun MeStatsDivider() {
    Surface(
        modifier = Modifier
            .width(width = 1.dp)
            .height(height = 32.dp),
        color = meDividerColor,
    ) {}
}
