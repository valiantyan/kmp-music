package com.yanhao.kmpmusic.feature.app

import androidx.compose.runtime.Composable

/**
 * 平台返回键桥接，Android 会接入系统返回，其他平台保持无操作。
 */
@Composable
expect fun PlatformBackHandler(
    enabled: Boolean,
    onBack: () -> Unit,
)
