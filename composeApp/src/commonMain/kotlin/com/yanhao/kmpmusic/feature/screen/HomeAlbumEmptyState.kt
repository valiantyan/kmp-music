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
import com.yanhao.kmpmusic.domain.model.LocalMusicScanErrorType
import com.yanhao.kmpmusic.domain.model.LocalMusicScanState

// 专辑为空时仍复用扫描入口，保证未扫描设备不会出现空白页签。
@Composable
internal fun HomeEmptyAlbumsCard(
    scanState: LocalMusicScanState,
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
                text = "扫描后会按专辑自动聚合。",
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
                    text = albumScanActionLabel(scanState = scanState),
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

// 专辑空态按钮沿用歌曲空态的权限语义，避免权限永久拒绝时误导用户普通重试。
private fun albumScanActionLabel(scanState: LocalMusicScanState): String {
    return when (scanState) {
        LocalMusicScanState.Idle -> "扫描本地音乐"
        LocalMusicScanState.WaitingForPermission -> "继续授权"
        is LocalMusicScanState.Importing -> "导入中"
        is LocalMusicScanState.Scanning -> "扫描中"
        is LocalMusicScanState.Done -> "重新扫描"
        is LocalMusicScanState.Error -> albumScanErrorActionLabel(scanState = scanState)
    }
}

// 权限类错误需要区分普通重试和系统设置入口，避免重复触发无效弹窗。
private fun albumScanErrorActionLabel(scanState: LocalMusicScanState.Error): String {
    return when (scanState.error.type) {
        LocalMusicScanErrorType.PermissionDenied -> "继续授权"
        LocalMusicScanErrorType.PermissionPermanentlyDenied -> "打开权限设置"
        else -> "重试扫描"
    }
}
