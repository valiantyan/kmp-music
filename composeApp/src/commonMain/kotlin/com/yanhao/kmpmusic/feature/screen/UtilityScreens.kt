package com.yanhao.kmpmusic.feature.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.background
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Mail
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yanhao.kmpmusic.core.theme.MusicColors
import com.yanhao.kmpmusic.core.theme.MusicDimens
import com.yanhao.kmpmusic.core.theme.scaledDp
import com.yanhao.kmpmusic.core.theme.scaledSp
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
    Column(verticalArrangement = Arrangement.spacedBy(scaledDp(18.dp))) {
        AppHeader(title = "设置", subtitle = "播放、扫描与外观", onBack = onBack)
        SectionTitle(title = "外观")
        Row(horizontalArrangement = Arrangement.spacedBy(scaledDp(8.dp))) {
            ThemeMode.entries.forEach { item ->
                FilterChip(selected = themeMode == item, onClick = { onThemeMode(item) }, label = { Text(text = item.label()) })
            }
        }
        SectionTitle(title = "音乐库")
        SettingsGroup {
            SettingsListRow("重新扫描本地音乐", "上次扫描：今天 08:36", onScan)
            SettingsDivider()
            SettingsListRow("本地文件夹", "/Music/KMP Library", {})
            SettingsDivider()
            SettingsListRow("清理缓存", "可释放 428 MB", onClearCache)
        }
        SectionTitle(title = "播放")
        SettingsGroup {
            SettingsListRow("无损优先", "可用时优先播放 FLAC / ALAC", {})
            SettingsDivider()
            SettingsListRow("睡眠定时", "30 分钟后停止播放", {})
            SettingsDivider()
            SettingsListRow("设备同步", "手机、桌面端同步播放记录", {})
        }
        SectionTitle(title = "账号与安全")
        SettingsGroup {
            SettingsListRow("隐私保护", "本地音乐不会上传到云端", {})
        }
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
    Column(verticalArrangement = Arrangement.spacedBy(scaledDp(22.dp))) {
        AppHeader(title = "登录", subtitle = "使用邮箱接收魔法链接", onBack = onBack)
        LoginCard(
            email = email,
            isMailSent = isMailSent,
            onEmail = onEmail,
            onSend = onSend,
        )
    }
}

/**
 * 登录卡片，将信任说明、输入和操作拆成稳定的视觉层级。
 */
@Composable
private fun LoginCard(
    email: String,
    isMailSent: Boolean,
    onEmail: (String) -> Unit,
    onSend: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(scaledDp(MusicDimens.LoginCardRadius)),
        color = MusicColors.Soft.copy(alpha = 0.58f),
        tonalElevation = scaledDp(1.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = scaledDp(MusicDimens.LoginCardPaddingHorizontal),
                    vertical = scaledDp(MusicDimens.LoginCardPaddingVertical),
                ),
            verticalArrangement = Arrangement.spacedBy(scaledDp(MusicDimens.LoginCardHeaderGap)),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                imageVector = Icons.Rounded.Security,
                contentDescription = null,
                modifier = Modifier.size(scaledDp(MusicDimens.LoginIconSize)),
                tint = MusicColors.Accent,
            )
            LoginIntroCopy()
            LoginForm(
                email = email,
                isMailSent = isMailSent,
                onEmail = onEmail,
                onSend = onSend,
            )
        }
    }
}

/**
 * 登录卡片说明文案，控制行高和段落间距避免首屏挤压。
 */
@Composable
private fun LoginIntroCopy() {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(scaledDp(MusicDimens.LoginCardTextGap)),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "同步收藏和播放记录",
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = scaledSp(22.sp),
            lineHeight = scaledSp(27.sp),
            fontWeight = FontWeight.ExtraBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = "登录后可在 Android、iOS 和桌面端继续使用同一套资料。",
            modifier = Modifier.fillMaxWidth(),
            color = MusicColors.Muted,
            fontSize = scaledSp(16.sp),
            lineHeight = scaledSp(24.sp),
            fontWeight = FontWeight.Medium,
        )
    }
}

/**
 * 登录表单区域，让邮箱输入和提交按钮具有稳定高度与触控面积。
 */
@Composable
private fun LoginForm(
    email: String,
    isMailSent: Boolean,
    onEmail: (String) -> Unit,
    onSend: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(scaledDp(MusicDimens.LoginCardFormGap)),
    ) {
        LoginEmailField(
            email = email,
            onEmail = onEmail,
        )
        PrimaryPill(
            text = "发送登录邮件",
            onClick = onSend,
            modifier = Modifier.fillMaxWidth(),
        )
        if (isMailSent) {
            Text(
                text = "登录邮件已发送，请在邮箱中确认。",
                color = MusicColors.Accent,
                fontSize = scaledSp(14.sp),
                lineHeight = scaledSp(18.sp),
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

/**
 * 邮箱输入框容器，避免裸输入行造成低密度和不可点击区域不明确。
 */
@Composable
private fun LoginEmailField(
    email: String,
    onEmail: (String) -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(scaledDp(MusicDimens.LoginInputHeight)),
        shape = RoundedCornerShape(scaledDp(MusicDimens.LoginInputRadius)),
        color = MusicColors.Paper.copy(alpha = 0.78f),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = scaledDp(MusicDimens.LoginInputHorizontalPadding)),
            horizontalArrangement = Arrangement.spacedBy(scaledDp(12.dp)),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Rounded.Mail,
                contentDescription = null,
                tint = MusicColors.Muted,
            )
            BasicTextField(
                value = email,
                onValueChange = onEmail,
                modifier = Modifier.weight(weight = 1f),
                cursorBrush = SolidColor(MusicColors.Accent),
                textStyle = androidx.compose.ui.text.TextStyle(
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = scaledSp(16.sp),
                    lineHeight = scaledSp(20.sp),
                    fontWeight = FontWeight.Medium,
                ),
                decorationBox = { innerTextField ->
                    if (email.isEmpty()) {
                        Text(
                            text = "name@example.com",
                            color = MusicColors.Muted,
                            fontSize = scaledSp(16.sp),
                            lineHeight = scaledSp(20.sp),
                        )
                    }
                    innerTextField()
                },
            )
        }
    }
}

/**
 * 本地文件夹页。
 */
@Composable
fun LocalFolderScreen(
    songs: List<Song>,
    currentSongId: String?,
    onBack: () -> Unit,
    onSongOpen: (Song) -> Unit,
    onSongPlay: (Song) -> Unit,
    onMore: (Song) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
        AppHeader(title = "本地文件夹", subtitle = "Music / KMP Library", onBack = onBack)
        Column(verticalArrangement = Arrangement.spacedBy(scaledDp(MusicDimens.FolderRowGap))) {
            listOf("似水流年", "Dream Stories", "华语人声", "森林歌单").forEachIndexed { index, folder ->
                FolderSummaryRow(
                    title = folder,
                    detail = "${24 - index * 3} 首歌曲 · 已加入音乐库",
                    onClick = {},
                )
            }
        }
        SectionTitle(title = "最近导入")
        songs.take(4).forEach { song ->
            SongRow(song, song.id == currentSongId, onSongOpen, onSongPlay, onMore, dense = true)
        }
    }
}

/**
 * 本地文件夹摘要行，为目录列表提供比设置项更舒展的阅读节奏。
 */
@Composable
private fun FolderSummaryRow(
    title: String,
    detail: String,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(scaledDp(MusicDimens.FolderRowRadius)),
        color = MusicColors.Soft,
        onClick = onClick,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = scaledDp(MusicDimens.FolderRowMinHeight))
                .padding(
                    horizontal = scaledDp(MusicDimens.FolderRowHorizontalPadding),
                    vertical = scaledDp(MusicDimens.FolderRowVerticalPadding),
                ),
            horizontalArrangement = Arrangement.spacedBy(scaledDp(MusicDimens.FolderRowContentGap)),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Rounded.Folder,
                contentDescription = null,
                modifier = Modifier.size(scaledDp(MusicDimens.FolderRowIconSize)),
                tint = MusicColors.Accent,
            )
            Column(
                modifier = Modifier.weight(weight = 1f),
                verticalArrangement = Arrangement.spacedBy(scaledDp(MusicDimens.FolderRowTextGap)),
            ) {
                Text(
                    text = title,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = scaledSp(22.sp),
                    lineHeight = scaledSp(26.sp),
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = detail,
                    color = MusicColors.Muted,
                    fontSize = scaledSp(16.sp),
                    lineHeight = scaledSp(20.sp),
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Text(
                text = "›",
                color = MusicColors.Muted,
                fontSize = scaledSp(28.sp),
                lineHeight = scaledSp(30.sp),
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

/**
 * 设置页分组容器，让同类设置形成一个稳定的信息块。
 */
@Composable
private fun SettingsGroup(
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(scaledDp(MusicDimens.SettingsGroupRadius)),
        color = MusicColors.Soft,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = scaledDp(MusicDimens.SettingsGroupPaddingVertical)),
            content = content,
        )
    }
}

/**
 * 设置页列表行，使用更舒展的行高和文字层级承载二级说明。
 */
@Composable
private fun SettingsListRow(
    title: String,
    detail: String,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color.Transparent,
        onClick = onClick,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = scaledDp(MusicDimens.SettingsRowMinHeight))
                .padding(
                    horizontal = scaledDp(MusicDimens.SettingsRowHorizontalPadding),
                    vertical = scaledDp(MusicDimens.SettingsRowVerticalPadding),
                ),
            horizontalArrangement = Arrangement.spacedBy(scaledDp(MusicDimens.SettingsRowContentGap)),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = if (title.contains("定时")) Icons.Rounded.Timer else Icons.Rounded.Folder,
                contentDescription = null,
                modifier = Modifier.size(scaledDp(MusicDimens.SettingsRowIconSize)),
                tint = MusicColors.Accent,
            )
            Column(
                modifier = Modifier.weight(weight = 1f),
                verticalArrangement = Arrangement.spacedBy(scaledDp(MusicDimens.SettingsRowTextGap)),
            ) {
                Text(
                    text = title,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = scaledSp(20.sp),
                    lineHeight = scaledSp(24.sp),
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = detail,
                    color = MusicColors.Muted,
                    fontSize = scaledSp(15.sp),
                    lineHeight = scaledSp(19.sp),
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Text(
                text = "›",
                color = MusicColors.Muted,
                fontSize = scaledSp(27.sp),
                lineHeight = scaledSp(30.sp),
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

/**
 * 设置页组内分隔线，只从文字区域开始，避免图标列被切开。
 */
@Composable
private fun SettingsDivider() {
    Row(modifier = Modifier.fillMaxWidth()) {
        Box(modifier = Modifier.width(scaledDp(MusicDimens.SettingsRowDividerStart)))
        Box(
            modifier = Modifier
                .weight(weight = 1f)
                .heightIn(min = scaledDp(1.dp))
                .background(MusicColors.Line.copy(alpha = 0.55f)),
        )
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
