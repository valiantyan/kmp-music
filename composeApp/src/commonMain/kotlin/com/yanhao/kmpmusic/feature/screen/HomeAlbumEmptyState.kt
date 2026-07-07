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

// 专辑为空时仍复用扫描入口，保证未扫描设备不会出现空白页签。
@Composable
internal fun HomeEmptyAlbumsCard(
    scanState: LocalMusicScanState,
    discoveryPlatform: LocalMusicDiscoveryPlatform = LocalMusicDiscoveryPlatform.Android,
    onScan: () -> Unit,
) {
    HomeEmptyLibraryAggregationCard(
        message = "扫描后会按专辑自动聚合。",
        scanState = scanState,
        discoveryPlatform = discoveryPlatform,
        onScan = onScan,
    )
}

// 歌手为空时仍复用扫描入口，保证未扫描设备不会出现空白页签。
@Composable
internal fun HomeEmptyArtistsCard(
    scanState: LocalMusicScanState,
    discoveryPlatform: LocalMusicDiscoveryPlatform = LocalMusicDiscoveryPlatform.Android,
    onScan: () -> Unit,
) {
    HomeEmptyLibraryAggregationCard(
        message = "扫描后会按歌手自动聚合。",
        scanState = scanState,
        discoveryPlatform = discoveryPlatform,
        onScan = onScan,
    )
}

// 首页聚合型页签共用空态卡片，避免专辑和歌手权限文案分叉。
@Composable
private fun HomeEmptyLibraryAggregationCard(
    message: String,
    scanState: LocalMusicScanState,
    discoveryPlatform: LocalMusicDiscoveryPlatform,
    onScan: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp)
            .padding(horizontal = 20.dp),
        shape = RoundedCornerShape(size = 16.dp),
        color = Color.White,
        shadowElevation = 1.dp,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = message,
                color = homeMutedColor,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                modifier = Modifier.weight(weight = 1f),
            )
            Surface(
                shape = RoundedCornerShape(size = 999.dp),
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
                    modifier = Modifier.padding(
                        horizontal = 14.dp,
                        vertical = 8.dp,
                    ),
                )
            }
        }
    }
}
