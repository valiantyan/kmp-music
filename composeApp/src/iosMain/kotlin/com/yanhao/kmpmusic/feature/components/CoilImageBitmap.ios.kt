package com.yanhao.kmpmusic.feature.components

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import coil3.Image
import coil3.toBitmap
import org.jetbrains.skia.Image as SkiaImage

/**
 * iOS 端沿用非 Android 的 Skia 桥接路径，确保共享取色逻辑只依赖 [ImageBitmap]。
 */
internal actual fun coilImageToImageBitmap(image: Image): ImageBitmap? {
    return runCatching {
        SkiaImage.makeFromBitmap(image.toBitmap()).toComposeImageBitmap()
    }.getOrNull()
}
