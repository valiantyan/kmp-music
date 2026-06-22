package com.yanhao.kmpmusic.feature.app

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable

/**
 * Android 系统返回键接入共享导航状态，避免二级页直接退到桌面。
 */
@Composable
actual fun PlatformBackHandler(
    enabled: Boolean,
    onBack: () -> Unit,
) {
    BackHandler(
        enabled = enabled,
        onBack = onBack,
    )
}
