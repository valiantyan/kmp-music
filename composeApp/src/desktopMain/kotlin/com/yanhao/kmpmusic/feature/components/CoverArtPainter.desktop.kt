package com.yanhao.kmpmusic.feature.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.toComposeImageBitmap
import com.yanhao.kmpmusic.domain.model.CoverArt
import java.io.IOException
import java.net.URI
import java.nio.file.Files
import java.nio.file.Paths
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.skia.Image

/**
 * Desktop 端从扫描缓存文件解码封面，失败时回退到应用内资源。
 */
@Composable
internal actual fun rememberPlatformCoverArtPainter(
    coverImageUri: String?,
    fallbackCoverArt: CoverArt,
): Painter {
    val imageBitmap: ImageBitmap? by produceState<ImageBitmap?>(
        initialValue = null,
        key1 = coverImageUri,
    ) {
        value = withContext(context = Dispatchers.IO) {
            decodeDesktopCoverImage(coverImageUri = coverImageUri)
        }
    }
    if (imageBitmap != null) {
        return BitmapPainter(image = imageBitmap!!)
    }
    return fallbackCoverArtPainter(coverArt = fallbackCoverArt)
}

// Desktop 扫描封面来自本地缓存文件，读取失败时由调用方显示兜底封面。
private fun decodeDesktopCoverImage(coverImageUri: String?): ImageBitmap? {
    if (coverImageUri.isNullOrBlank()) {
        return null
    }
    return try {
        val bytes: ByteArray = Files.readAllBytes(Paths.get(URI(coverImageUri)))
        Image.makeFromEncoded(bytes).toComposeImageBitmap()
    } catch (ioException: IOException) {
        null
    } catch (illegalArgumentException: IllegalArgumentException) {
        null
    }
}
