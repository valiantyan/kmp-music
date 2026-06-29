package com.yanhao.kmpmusic.data

import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.security.MessageDigest
import org.jaudiotagger.audio.AudioFile
import org.jaudiotagger.audio.AudioFileIO
import org.jaudiotagger.audio.exceptions.CannotReadException
import org.jaudiotagger.audio.exceptions.InvalidAudioFrameException
import org.jaudiotagger.audio.exceptions.ReadOnlyFileException
import org.jaudiotagger.tag.TagException
import org.jaudiotagger.tag.datatype.Artwork

/**
 * Desktop 端从本地音频文件提取 embedded artwork，并缓存为 UI 可解码的图片文件。
 */
internal class DesktopEmbeddedArtworkExtractor(
    private val cacheDirectory: Path = defaultArtworkCacheDirectory(),
) {
    /**
     * 提取单个文件的封面缓存 URI。
     *
     * @param audioPath 音频文件路径。
     * @param sourceId 来源内稳定歌曲标识，用于生成稳定缓存名。
     * @return 缓存图片 URI；无封面或解析失败时返回 null。
     */
    fun extractArtworkUri(
        audioPath: Path,
        sourceId: String,
    ): String? {
        return try {
            val audioFile: AudioFile = AudioFileIO.read(audioPath.toFile())
            val artworkBytes: ByteArray = audioFile.readEmbeddedArtworkBytes() ?: return null
            val mimeType: String? = audioFile.tag?.firstArtwork?.mimeType
            writeArtworkCache(
                sourceId = sourceId,
                artworkBytes = artworkBytes,
                mimeType = mimeType,
            ).toUri().toString()
        } catch (cannotReadException: CannotReadException) {
            null
        } catch (tagException: TagException) {
            null
        } catch (readOnlyFileException: ReadOnlyFileException) {
            null
        } catch (invalidAudioFrameException: InvalidAudioFrameException) {
            null
        } catch (ioException: IOException) {
            null
        } catch (runtimeException: RuntimeException) {
            null
        }
    }

    // 通过 jaudiotagger 读取各格式内嵌封面，避免只支持 MP3 ID3 APIC。
    internal fun AudioFile.readEmbeddedArtworkBytes(): ByteArray? {
        val artwork: Artwork = tag?.firstArtwork ?: return null
        val artworkBytes: ByteArray = artwork.binaryData ?: return null
        if (artworkBytes.isEmpty()) {
            return null
        }
        return artworkBytes
    }

    // 缓存图片使用来源 ID 派生命名，重新扫描同一媒体会覆盖旧封面。
    private fun writeArtworkCache(
        sourceId: String,
        artworkBytes: ByteArray,
        mimeType: String?,
    ): Path {
        Files.createDirectories(cacheDirectory)
        val artworkPath: Path = cacheDirectory.resolve("${sourceId.toSha256()}${mimeType.toImageExtension()}")
        Files.write(artworkPath, artworkBytes)
        return artworkPath
    }
}

// 常见 MIME 类型转成真实图片扩展名，帮助桌面图片解码器识别缓存文件。
private fun String?.toImageExtension(): String {
    return when (this?.trim()?.lowercase()) {
        "image/jpeg",
        "image/jpg",
        -> ".jpg"
        "image/png" -> ".png"
        "image/gif" -> ".gif"
        "image/webp" -> ".webp"
        else -> ".art"
    }
}

// 使用 SHA-256 避免本地绝对路径直接出现在缓存文件名中。
private fun String.toSha256(): String {
    val digest: ByteArray = MessageDigest.getInstance("SHA-256").digest(toByteArray())
    return digest.joinToString(separator = "") { byte: Byte ->
        "%02x".format(byte)
    }
}

// 默认缓存目录放在用户主目录下，跨启动保留已提取封面。
private fun defaultArtworkCacheDirectory(): Path {
    val userHome: String = System.getProperty("user.home").orEmpty()
    return Path.of(userHome, ".kmp-music", "artwork-cache")
}
