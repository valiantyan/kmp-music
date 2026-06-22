package com.yanhao.kmpmusic.feature.app

import androidx.compose.runtime.Composable

/**
 * 桌面端暂不接管系统返回键，避免改变窗口默认行为。
 */
@Composable
actual fun PlatformBackHandler(
    enabled: Boolean,
    onBack: () -> Unit,
) = Unit
