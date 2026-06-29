package com.yanhao.kmpmusic.feature.components

import androidx.compose.ui.graphics.ImageBitmap
import coil3.Image

/**
 * 将 Coil 跨平台 [Image] 转为 Compose [ImageBitmap]，供现有取色算法读取像素。
 */
internal expect fun coilImageToImageBitmap(image: Image): ImageBitmap?
