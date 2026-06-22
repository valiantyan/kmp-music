package com.yanhao.kmpmusic.feature.app

import androidx.compose.runtime.Composable

/**
 * iOS 当前没有 Android 式系统返回键，这里保留平台占位。
 */
@Composable
actual fun PlatformBackHandler(
    enabled: Boolean,
    onBack: () -> Unit,
) = Unit
