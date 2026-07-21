package com.yanhao.kmpmusic.feature.desktop.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.yanhao.kmpmusic.feature.desktop.DesktopMusicColors

// 只有主动开启清除能力且存在输入内容时，桌面输入框才展示清除按钮。
internal fun shouldShowDesktopTextInputClearAction(
    value: String,
    isClearEnabled: Boolean,
): Boolean {
    return isClearEnabled && value.isNotEmpty()
}

// 空输入仍保留按钮槽位，避免搜索框文字区域在输入和清空时横向跳动。
@Composable
internal fun DesktopTextInputClearAction(
    isClearEnabled: Boolean,
    shouldShowClearAction: Boolean,
    onClear: () -> Unit,
) {
    if (!isClearEnabled) {
        return
    }
    if (!shouldShowClearAction) {
        Spacer(modifier = Modifier.size(22.dp))
        return
    }
    Box(
        modifier = Modifier
            .size(22.dp)
            .clip(shape = CircleShape)
            .background(color = DesktopMusicColors.Line)
            .semantics { this.contentDescription = "清除输入内容" }
            .clickable(
                role = Role.Button,
                onClick = onClear,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Rounded.Close,
            contentDescription = null,
            modifier = Modifier.size(14.dp),
            tint = DesktopMusicColors.MutedStrong,
        )
    }
}
