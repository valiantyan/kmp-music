package com.yanhao.kmpmusic.feature.components

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import coil3.Image
import coil3.toBitmap

/**
 * Android 端直接复用 Bitmap 桥接到 Compose 位图，避免重复解码。
 */
internal actual fun coilImageToImageBitmap(image: Image): ImageBitmap? {
    return runCatching {
        image.toBitmap().asImageBitmap()
    }.getOrNull()
}
