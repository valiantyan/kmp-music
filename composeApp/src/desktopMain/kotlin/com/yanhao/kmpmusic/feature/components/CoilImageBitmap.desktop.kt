package com.yanhao.kmpmusic.feature.components

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import coil3.Image
import coil3.toBitmap
import org.jetbrains.skia.Image as SkiaImage

/**
 * Desktop 端把 Coil 的 Skia Bitmap 包装成 Compose 可读位图，继续复用共享调色算法。
 */
internal actual fun coilImageToImageBitmap(image: Image): ImageBitmap? {
    return runCatching {
        SkiaImage.makeFromBitmap(image.toBitmap()).toComposeImageBitmap()
    }.getOrNull()
}
