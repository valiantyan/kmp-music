package com.yanhao.kmpmusic.feature.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Mail
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yanhao.kmpmusic.core.theme.MusicColors
import com.yanhao.kmpmusic.domain.model.Song
import com.yanhao.kmpmusic.domain.model.ThemeMode
import com.yanhao.kmpmusic.feature.components.AppHeader
import com.yanhao.kmpmusic.feature.components.PrimaryPill
import com.yanhao.kmpmusic.feature.components.SectionTitle
import com.yanhao.kmpmusic.feature.components.SongRow

/**
 * 设置页。
 */
@Composable
fun SettingsScreen(
    themeMode: ThemeMode,
    onThemeMode: (ThemeMode) -> Unit,
    onBack: () -> Unit,
    onScan: () -> Unit,
    onClearCache: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        AppHeader(title = "设置", subtitle = "播放、扫描与外观", onBack = onBack)
        SectionTitle(title = "外观")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ThemeMode.entries.forEach { item ->
                FilterChip(selected = themeMode == item, onClick = { onThemeMode(item) }, label = { Text(text = item.label()) })
            }
        }
        SectionTitle(title = "音乐库")
        SettingsRow("重新扫描本地音乐", "上次扫描：今天 08:36", onScan)
        SettingsRow("本地文件夹", "/Music/KMP Library", {})
        SettingsRow("清理缓存", "可释放 428 MB", onClearCache)
        SectionTitle(title = "播放")
        SettingsRow("无损优先", "可用时优先播放 FLAC / ALAC", {})
        SettingsRow("睡眠定时", "30 分钟后停止播放", {})
        SettingsRow("设备同步", "手机、桌面端同步播放记录", {})
        SectionTitle(title = "账号与安全")
        SettingsRow("隐私保护", "本地音乐不会上传到云端", {})
    }
}

/**
 * 登录页。
 */
@Composable
fun LoginScreen(
    email: String,
    isMailSent: Boolean,
    onEmail: (String) -> Unit,
    onSend: () -> Unit,
    onBack: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
        AppHeader(title = "登录", subtitle = "使用邮箱接收魔法链接", onBack = onBack)
        Surface(shape = androidx.compose.foundation.shape.RoundedCornerShape(20.dp), color = MusicColors.Paper, tonalElevation = 1.dp) {
            Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Rounded.Security, contentDescription = null, tint = MusicColors.Accent)
                Text(text = "同步收藏和播放记录", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
                Text(text = "登录后可在 Android、iOS 和桌面端继续使用同一套资料。", color = MusicColors.Muted)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Mail, contentDescription = null, tint = MusicColors.Muted)
                    BasicTextField(
                        value = email,
                        onValueChange = onEmail,
                        modifier = Modifier.weight(weight = 1f),
                        cursorBrush = SolidColor(MusicColors.Accent),
                        decorationBox = { innerTextField ->
                            if (email.isEmpty()) Text(text = "name@example.com", color = MusicColors.Muted)
                            innerTextField()
                        },
                    )
                }
                PrimaryPill(text = "发送登录邮件", onClick = onSend, modifier = Modifier.fillMaxWidth())
                if (isMailSent) Text(text = "登录邮件已发送，请在邮箱中确认。", color = MusicColors.Accent)
            }
        }
    }
}

/**
 * 本地文件夹页。
 */
@Composable
fun LocalFolderScreen(
    songs: List<Song>,
    currentSongId: String,
    onBack: () -> Unit,
    onSongOpen: (Song) -> Unit,
    onSongPlay: (Song) -> Unit,
    onMore: (Song) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
        AppHeader(title = "本地文件夹", subtitle = "Music / KMP Library", onBack = onBack)
        listOf("似水流年", "Dream Stories", "华语人声", "森林歌单").forEachIndexed { index, folder ->
            SettingsRow(folder, "${24 - index * 3} 首歌曲 · 已加入音乐库", {})
        }
        SectionTitle(title = "最近导入")
        songs.take(4).forEach { song ->
            SongRow(song, song.id == currentSongId, onSongOpen, onSongPlay, onMore, dense = true)
        }
    }
}

/**
 * 设置行和文件夹行的统一组件。
 */
@Composable
private fun SettingsRow(
    title: String,
    detail: String,
    onClick: () -> Unit,
) {
    Surface(shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp), color = MusicColors.Soft, onClick = onClick) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(if (title.contains("定时")) Icons.Rounded.Timer else Icons.Rounded.Folder, contentDescription = null, tint = MusicColors.Accent)
            Column(modifier = Modifier.weight(weight = 1f)) {
                Text(text = title, fontWeight = FontWeight.Bold)
                Text(text = detail, color = MusicColors.Muted, fontSize = 13.sp)
            }
            Text(text = "›", color = MusicColors.Muted, fontSize = 22.sp)
        }
    }
}

/**
 * 主题模式中文名。
 */
private fun ThemeMode.label(): String {
    return when (this) {
        ThemeMode.Light -> "浅色"
        ThemeMode.Dark -> "深色"
        ThemeMode.System -> "跟随系统"
    }
}
