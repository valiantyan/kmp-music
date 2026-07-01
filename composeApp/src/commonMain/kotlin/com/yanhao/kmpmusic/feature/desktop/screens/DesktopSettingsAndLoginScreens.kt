package com.yanhao.kmpmusic.feature.desktop.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.yanhao.kmpmusic.domain.model.ThemeMode
import com.yanhao.kmpmusic.feature.desktop.DesktopMusicColors
import com.yanhao.kmpmusic.feature.desktop.DesktopMusicDimens
import com.yanhao.kmpmusic.feature.desktop.DesktopMusicType
import com.yanhao.kmpmusic.feature.desktop.components.DesktopContentRow
import com.yanhao.kmpmusic.feature.desktop.components.DesktopContentRowSyncIcon
import com.yanhao.kmpmusic.feature.desktop.components.DesktopPageHeader
import com.yanhao.kmpmusic.feature.desktop.components.DesktopPrimaryButton
import com.yanhao.kmpmusic.feature.desktop.components.DesktopSegmentedControl
import com.yanhao.kmpmusic.feature.desktop.components.DesktopTextInput

/**
 * 设置页只暴露当前桌面端已实现的偏好与维护动作。
 */
@Composable
internal fun DesktopSettingsScreen(
    themeMode: ThemeMode,
    onThemeMode: (ThemeMode) -> Unit,
    onBack: () -> Unit,
    onScan: () -> Unit,
    onLocalMusicSources: () -> Unit,
    onClearCache: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        DesktopPageHeader(
            title = "设置",
            eyebrow = "播放、扫描与显示偏好",
        ) {
            DesktopPrimaryButton(text = "返回", onClick = onBack)
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(28.dp),
        ) {
            Column(
                modifier = Modifier.width(DesktopMusicDimens.SettingNavWidth),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text(
                    text = "显示偏好",
                    color = DesktopMusicColors.AccentDeep,
                    fontSize = DesktopMusicType.Eyebrow,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "本地音乐",
                    color = DesktopMusicColors.MutedStrong,
                    fontSize = DesktopMusicType.Eyebrow,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = "缓存维护",
                    color = DesktopMusicColors.MutedStrong,
                    fontSize = DesktopMusicType.Eyebrow,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Text(
                    text = "主题模式",
                    color = DesktopMusicColors.Ink,
                    fontSize = DesktopMusicType.StatTitle,
                    fontWeight = FontWeight.Bold,
                )
                DesktopSegmentedControl(
                    labels = ThemeMode.entries.map { themeEntry: ThemeMode -> themeEntry.name },
                    selectedIndex = ThemeMode.entries.indexOf(themeMode),
                    onSelect = { index: Int -> onThemeMode(ThemeMode.entries[index]) },
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "本地音乐",
                    color = DesktopMusicColors.Ink,
                    fontSize = DesktopMusicType.StatTitle,
                    fontWeight = FontWeight.Bold,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    DesktopPrimaryButton(text = "管理本地文件夹", onClick = onLocalMusicSources)
                    DesktopPrimaryButton(text = "重新扫描", onClick = onScan)
                }
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "缓存维护",
                    color = DesktopMusicColors.Ink,
                    fontSize = DesktopMusicType.StatTitle,
                    fontWeight = FontWeight.Bold,
                )
                DesktopPrimaryButton(text = "清理缓存", onClick = onClearCache)
            }
        }
    }
}

/**
 * 登录页提供桌面原生邮箱输入，确保发送登录邮件前能完成最小必需表单。
 */
@Composable
internal fun DesktopLoginScreen(
    email: String,
    isMailSent: Boolean,
    onEmail: (String) -> Unit,
    onSend: () -> Unit,
    onBack: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        DesktopPageHeader(
            title = "登录音乐账号",
            eyebrow = if (isMailSent) "登录邮件已发送" else "使用邮箱接收魔法链接",
        ) {
            DesktopPrimaryButton(text = "返回", onClick = onBack)
            DesktopPrimaryButton(text = "发送登录邮件", onClick = onSend)
        }
        DesktopTextInput(
            value = email,
            onValueChange = onEmail,
            placeholder = "输入邮箱地址",
            modifier = Modifier.fillMaxWidth(),
            leadingIcon = Icons.Rounded.Person,
        )
        Spacer(modifier = Modifier.height(18.dp))
        DesktopContentRow(
            icon = DesktopContentRowSyncIcon,
            title = if (isMailSent) "请前往邮箱继续登录" else "邮箱魔法链接登录",
            subtitle = if (email.isBlank()) "输入邮箱后即可发送登录邮件。" else "当前邮箱：$email",
        )
    }
}
