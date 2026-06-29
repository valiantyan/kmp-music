package com.yanhao.kmpmusic.feature.components

import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalContext
import com.yanhao.kmpmusic.domain.model.CoverArt
import java.io.IOException
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Android 端从本地 URI 解码扫描封面，失败时回退到应用内资源。
 */
@Composable
internal actual fun rememberPlatformCoverArtPainter(
    coverImageUri: String?,
    fallbackCoverArt: CoverArt,
): Painter {
    val context = LocalContext.current
    val imageBitmap: ImageBitmap? by produceState<ImageBitmap?>(
        initialValue = null,
        key1 = coverImageUri,
    ) {
        value = withContext(context = Dispatchers.IO) {
            decodeAndroidCoverImage(
                contentResolver = context.contentResolver,
                coverImageUri = coverImageUri,
            )
        }
    }
    if (imageBitmap != null) {
        return BitmapPainter(image = imageBitmap!!)
    }
    return fallbackCoverArtPainter(coverArt = fallbackCoverArt)
}

// Android 列表渲染不能抛出平台 I/O 异常，失败时只记录并回退默认封面。
private fun decodeAndroidCoverImage(
    contentResolver: android.content.ContentResolver,
    coverImageUri: String?,
): ImageBitmap? {
    if (coverImageUri.isNullOrBlank()) {
        return null
    }
    return try {
        Log.d(TAG, "开始解码 Android 扫描封面: $coverImageUri")
        val uri: Uri = Uri.parse(coverImageUri)
        val inputStream = if (uri.scheme == "file") {
            File(uri.path.orEmpty()).inputStream()
        } else {
            contentResolver.openInputStream(uri)
        }
        inputStream?.use { inputStream ->
            BitmapFactory.decodeStream(inputStream)?.asImageBitmap()
        }
    } catch (ioException: IOException) {
        Log.e(TAG, "Android 扫描封面读取失败: $coverImageUri", ioException)
        null
    } catch (securityException: SecurityException) {
        Log.e(TAG, "Android 扫描封面缺少读取权限: $coverImageUri", securityException)
        null
    } catch (illegalArgumentException: IllegalArgumentException) {
        Log.e(TAG, "Android 扫描封面 URI 无效: $coverImageUri", illegalArgumentException)
        null
    }
}

private const val TAG = "CoverArtPainter"
