package com.yanhao.kmpmusic.feature.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yanhao.kmpmusic.domain.model.LocalMusicScanState

// 标题行承接 Figma 的动态内容标签，右侧只表达当前歌曲总量。
@Composable
internal fun HomeSongSectionHeader(songCountText: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(40.dp)
            .padding(horizontal = 20.dp)
            .padding(top = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top,
    ) {
        Text(
            text = "本地音乐",
            color = Color(0xFF191C1D),
            fontSize = 24.sp,
            lineHeight = 32.sp,
            fontWeight = FontWeight.Medium,
        )
        Text(
            text = songCountText,
            color = homeMutedColor,
            fontSize = 12.sp,
            lineHeight = 16.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(top = 10.dp),
        )
    }
}

// 空曲库仍提供扫描入口，避免真实设备未授权或未扫描时首页白屏。
@Composable
internal fun HomeEmptySongsCard(
    scanState: LocalMusicScanState,
    discoveryPlatform: LocalMusicDiscoveryPlatform = LocalMusicDiscoveryPlatform.Android,
    onScan: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp)
            .padding(horizontal = 20.dp),
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        shadowElevation = 1.dp,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "扫描本地音乐后，歌曲会出现在这里。",
                color = homeMutedColor,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                modifier = Modifier.weight(weight = 1f),
            )
            Surface(
                shape = RoundedCornerShape(999.dp),
                color = homeAccentColor,
                onClick = onScan,
            ) {
                Text(
                    text = localMusicScanActionLabel(
                        scanState = scanState,
                        platform = discoveryPlatform,
                    ),
                    color = Color.White,
                    fontSize = 14.sp,
                    lineHeight = 16.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                )
            }
        }
    }
}
