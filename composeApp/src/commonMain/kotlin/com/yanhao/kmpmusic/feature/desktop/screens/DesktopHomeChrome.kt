package com.yanhao.kmpmusic.feature.desktop.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yanhao.kmpmusic.domain.model.LocalMusicScanState
import com.yanhao.kmpmusic.feature.desktop.components.DesktopScanIcon
import com.yanhao.kmpmusic.feature.desktop.components.DesktopSecondaryButton
import com.yanhao.kmpmusic.feature.screen.LocalMusicDiscoveryPlatform
import com.yanhao.kmpmusic.feature.screen.localMusicScanActionLabel

// 搜索栏固定在首页第一行，点击继续进入既有独立搜索页。
@Composable
internal fun DesktopHomeSearchBar(onSearch: () -> Unit) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(64.dp)
                .background(Color(0x1AF9F9FF))
                .padding(horizontal = 24.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Surface(
            modifier =
                Modifier
                    .width(576.dp)
                    .height(34.dp),
            shape = CircleShape,
            color = Color(0xFFF0F3FF),
            onClick = onSearch,
        ) {
            Row(
                modifier = Modifier.padding(start = 12.dp, end = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Rounded.Search,
                    contentDescription = null,
                    tint = Color(0x803C4A46),
                    modifier = Modifier.size(15.dp),
                )
                Text(
                    text = "搜索音乐、专辑或歌手",
                    color = Color(0x803C4A46),
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

// 标题和歌曲数量固定在列表上方，滚动时不随歌曲卡片移动。
@Composable
internal fun DesktopHomeTitle(songCount: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom,
    ) {
        Text(
            text = "本地音乐",
            color = Color(0xFF111C2D),
            fontSize = 32.sp,
            lineHeight = 48.sp,
            maxLines = 1,
        )
        Surface(
            shape = CircleShape,
            color = Color(0xFFE7EEFF),
        ) {
            Text(
                text = "$songCount 首歌曲",
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                color = Color(0xFF3C4A46),
                fontSize = 13.sp,
                lineHeight = 18.sp,
                maxLines = 1,
            )
        }
    }
}

// 空曲库只在列表区域展示，不挤走搜索栏和标题。
@Composable
internal fun DesktopHomeEmptyState(
    scanState: LocalMusicScanState,
    onScan: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(
                text = "暂无本地音乐",
                color = Color(0xFF111C2D),
                fontSize = 18.sp,
                lineHeight = 26.sp,
                fontWeight = FontWeight.SemiBold,
            )
            DesktopSecondaryButton(
                text =
                    localMusicScanActionLabel(
                        scanState = scanState,
                        platform = LocalMusicDiscoveryPlatform.Desktop,
                    ),
                icon = DesktopScanIcon,
                onClick = onScan,
            )
        }
    }
}
