package com.yanhao.kmpmusic.feature.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// 搜索历史区域只负责渲染真实历史，是否显示由上层展示模型控制。
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun SearchHistorySection(
    history: List<String>,
    onHistorySelect: (String) -> Unit,
    onClearHistory: () -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = searchHorizontalPadding),
        verticalArrangement = Arrangement.spacedBy(space = 16.dp),
    ) {
        SearchSectionHeader(
            title = "搜索历史",
            actionContentDescription = "清空搜索历史",
            onAction = onClearHistory,
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(space = 8.dp),
            verticalArrangement = Arrangement.spacedBy(space = 8.dp),
        ) {
            visibleSearchHistoryChips(history = history).forEach { item: String ->
                SearchHistoryChip(
                    text = item,
                    onClick = { onHistorySelect(item) },
                )
            }
        }
    }
}

// 区块标题右侧操作按钮用于清空历史，尺寸跟随 Figma 的圆形图标按钮。
@Composable
private fun SearchSectionHeader(
    title: String,
    actionContentDescription: String,
    onAction: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            color = searchPrimaryTextColor,
            fontSize = 24.sp,
            lineHeight = 32.sp,
            fontWeight = FontWeight.Medium,
        )
        SearchRoundIconButton(
            contentDescription = actionContentDescription,
            onClick = onAction,
        ) {
            Icon(
                imageVector = Icons.Rounded.Delete,
                contentDescription = null,
                modifier = Modifier.size(width = 18.dp, height = 20.dp),
                tint = searchSecondaryTextColor,
            )
        }
    }
}

// 历史 chip 使用 Figma 胶囊样式，点击后直接回填并触发搜索历史置顶。
@Composable
private fun SearchHistoryChip(
    text: String,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier.height(height = searchHistoryChipHeight),
        shape = CircleShape,
        color = searchChipColor,
        onClick = onClick,
    ) {
        Box(
            modifier = Modifier.padding(horizontal = 16.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = text,
                color = searchSecondaryTextColor,
                fontSize = 14.sp,
                lineHeight = 16.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
