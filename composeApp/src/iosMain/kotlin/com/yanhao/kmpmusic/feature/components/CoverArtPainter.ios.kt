package com.yanhao.kmpmusic.feature.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.painter.Painter
import com.yanhao.kmpmusic.domain.model.CoverArt

/**
 * iOS 扫描封面尚未接入时保持应用内资源兜底，避免 common UI 分叉。
 */
@Composable
internal actual fun rememberPlatformCoverArtPainter(
    coverImageUri: String?,
    fallbackCoverArt: CoverArt,
): Painter {
    return fallbackCoverArtPainter(coverArt = fallbackCoverArt)
}
