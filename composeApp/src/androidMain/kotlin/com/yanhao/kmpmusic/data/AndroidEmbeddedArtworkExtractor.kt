package com.yanhao.kmpmusic.data

import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.util.Log
import java.io.File
import java.io.IOException

/**
 * 从 Android 音频文件中提取 embedded artwork，并缓存成 UI 可直接解码的本地图片。
 */
internal class AndroidEmbeddedArtworkExtractor(
    private val context: Context,
    private val cacheDirectory: File,
) {
    // 复用 application context 的 [ContentResolver]，避免持有 Activity。
    private val contentResolver: ContentResolver = context.contentResolver

    /**
     * 提取单首歌曲封面。
     *
     * @param mediaUri MediaStore 音频 URI。
     * @param sourceId 来源内稳定歌曲标识，用于生成稳定缓存名。
     * @param albumId MediaStore 专辑 ID，用于 embedded picture 缺失时读取系统专辑封面。
     * @return 缓存图片 URI；无封面或读取失败时返回 null。
     */
    fun extractArtworkUri(
        mediaUri: Uri,
        sourceId: String,
        albumId: Long?,
    ): String? {
        return try {
            Log.d(TAG, "开始提取 Android 音频封面: sourceId=$sourceId")
            val artworkBytes: ByteArray = readEmbeddedPicture(mediaUri = mediaUri)
                ?: readAlbumArtwork(albumId = albumId)
                ?: return null
            val artworkFile: File = writeArtworkCache(
                sourceId = sourceId,
                artworkBytes = artworkBytes,
            )
            artworkFile.toURI().toString()
        } catch (ioException: IOException) {
            Log.e(TAG, "Android 音频封面缓存写入失败: sourceId=$sourceId", ioException)
            null
        } catch (securityException: SecurityException) {
            Log.e(TAG, "Android 音频封面读取缺少权限: sourceId=$sourceId", securityException)
            null
        } catch (runtimeException: RuntimeException) {
            Log.e(TAG, "Android 音频封面解析失败: sourceId=$sourceId", runtimeException)
            null
        }
    }

    // 优先通过 Context + Uri 读取 embedded picture，兼容更多 MediaStore content URI 实现。
    private fun readEmbeddedPicture(mediaUri: Uri): ByteArray? {
        val retriever: MediaMetadataRetriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(context, mediaUri)
            val embeddedPicture: ByteArray? = retriever.embeddedPicture
            if (embeddedPicture != null) {
                Log.d(TAG, "Android embedded picture 提取成功: uri=$mediaUri")
            }
            return embeddedPicture
        } finally {
            retriever.release()
        }
    }

    // 部分设备不会通过 retriever 暴露图片，但 MediaStore 会按 albumId 暴露专辑封面。
    private fun readAlbumArtwork(albumId: Long?): ByteArray? {
        if (albumId == null || albumId <= 0L) {
            return null
        }
        val albumArtworkUri: Uri = ContentUris.withAppendedId(ALBUM_ARTWORK_BASE_URI, albumId)
        return contentResolver.openInputStream(albumArtworkUri)?.use { inputStream ->
            val artworkBytes: ByteArray = inputStream.readBytes()
            if (artworkBytes.isNotEmpty()) {
                Log.d(TAG, "Android MediaStore 专辑封面提取成功: albumId=$albumId")
                artworkBytes
            } else {
                null
            }
        }
    }

    // 缓存图片使用来源 ID 派生命名，重新扫描同一媒体会覆盖旧封面。
    private fun writeArtworkCache(
        sourceId: String,
        artworkBytes: ByteArray,
    ): File {
        val artworkDirectory: File = File(cacheDirectory, ARTWORK_CACHE_DIRECTORY)
        if (!artworkDirectory.exists()) {
            artworkDirectory.mkdirs()
        }
        val artworkFile: File = File(artworkDirectory, "${sourceId.toCacheFileName()}.art")
        artworkFile.writeBytes(array = artworkBytes)
        return artworkFile
    }

    // 文件名只保留安全字符，避免来源 ID 中的路径分隔符影响缓存目录。
    private fun String.toCacheFileName(): String {
        return map { char: Char ->
            if (char.isLetterOrDigit()) {
                char
            } else {
                '_'
            }
        }.joinToString(separator = "")
    }
}

private const val TAG = "AndroidEmbeddedArtwork"
private const val ARTWORK_CACHE_DIRECTORY = "audio-artwork"
private val ALBUM_ARTWORK_BASE_URI: Uri = Uri.parse("content://media/external/audio/albumart")
