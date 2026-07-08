package com.yanhao.kmpmusic.feature.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yanhao.kmpmusic.domain.model.SearchScope

// 结果标题和 tab 组合成 Figma 的“搜索结果”区块。
@Composable
internal fun SearchResultsHeader(
    selectedTab: SearchResultTab,
    onScope: (SearchScope) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = searchHorizontalPadding),
        verticalArrangement = Arrangement.spacedBy(space = 24.dp),
    ) {
        Text(
            text = "搜索结果",
            color = searchPrimaryTextColor,
            fontSize = 24.sp,
            lineHeight = 32.sp,
            fontWeight = FontWeight.Medium,
        )
        SearchResultTabs(
            selectedTab = selectedTab,
            onScope = onScope,
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(height = 1.dp)
                .background(color = searchDividerColor),
        )
    }
}

// 结果 tabs 横向排列，保持 Figma 的 32dp 间距和底部指示线。
@Composable
private fun SearchResultTabs(
    selectedTab: SearchResultTab,
    onScope: (SearchScope) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(height = 33.dp)
            .background(color = Color.Transparent),
        horizontalArrangement = Arrangement.spacedBy(space = 32.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        SearchResultTab.entries.forEach { tab: SearchResultTab ->
            SearchResultTabButton(
                tab = tab,
                isSelected = tab == selectedTab,
                onScope = onScope,
            )
        }
    }
}

// 单个结果 tab，歌单 tab 没有领域 scope 时只承担视觉占位。
@Composable
private fun SearchResultTabButton(
    tab: SearchResultTab,
    isSelected: Boolean,
    onScope: (SearchScope) -> Unit,
) {
    val tabColor: Color = if (isSelected) searchAccentColor else searchSecondaryTextColor
    Column(
        modifier = Modifier
            .height(height = 33.dp)
            .clickable(enabled = tab.scope != null) {
                tab.scope?.let { scope: SearchScope -> onScope(scope) }
            },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Bottom,
    ) {
        Text(
            text = tab.label,
            color = tabColor,
            fontSize = 14.sp,
            lineHeight = 16.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(height = 8.dp))
        Box(
            modifier = Modifier
                .height(height = 3.dp)
                .width(width = 28.dp)
                .clip(shape = CircleShape)
                .background(color = if (isSelected) searchAccentColor else Color.Transparent),
        )
    }
}
