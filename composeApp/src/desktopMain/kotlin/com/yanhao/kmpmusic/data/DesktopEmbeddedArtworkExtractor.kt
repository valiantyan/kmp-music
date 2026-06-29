package com.yanhao.kmpmusic.data

import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.security.MessageDigest

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
            val artworkBytes: ByteArray = readId3v2Artwork(audioPath = audioPath) ?: return null
            Files.createDirectories(cacheDirectory)
            val artworkPath: Path = cacheDirectory.resolve("${sourceId.toSha256()}.art")
            Files.write(artworkPath, artworkBytes)
            artworkPath.toUri().toString()
        } catch (ioException: IOException) {
            null
        } catch (illegalArgumentException: IllegalArgumentException) {
            null
        }
    }

    // 当前先覆盖 MP3 ID3v2 APIC，这是桌面本地库最常见的 embedded artwork 来源。
    private fun readId3v2Artwork(audioPath: Path): ByteArray? {
        Files.newInputStream(audioPath).use { inputStream ->
            val header: ByteArray = inputStream.readNBytes(ID3_HEADER_SIZE)
            if (header.size < ID3_HEADER_SIZE || header.decodeAscii(start = 0, length = 3) != "ID3") {
                return null
            }
            val version: Int = header[3].toInt() and BYTE_MASK
            val tagSize: Int = header.readSyncSafeInt(offset = 6)
            val tagPayload: ByteArray = inputStream.readNBytes(tagSize)
            val bytes: ByteArray = header + tagPayload
            val tagEnd: Int = bytes.size
            return when (version) {
                2 -> bytes.readId3v22Artwork(tagEnd = tagEnd)
                3,
                4,
                -> bytes.readId3v23Or24Artwork(version = version, tagEnd = tagEnd)
                else -> null
            }
        }
    }

    // ID3v2.3/2.4 的 APIC 帧承载图片类型、描述和原始图片字节。
    private fun ByteArray.readId3v23Or24Artwork(
        version: Int,
        tagEnd: Int,
    ): ByteArray? {
        var offset: Int = ID3_HEADER_SIZE
        while (offset + ID3_FRAME_HEADER_SIZE <= tagEnd) {
            val frameId: String = decodeAscii(start = offset, length = 4)
            val frameSize: Int = if (version == 4) {
                readSyncSafeInt(offset = offset + 4)
            } else {
                readInt(offset = offset + 4)
            }
            if (frameId.isBlank() || frameSize <= 0) {
                return null
            }
            val contentStart: Int = offset + ID3_FRAME_HEADER_SIZE
            val contentEnd: Int = contentStart + frameSize
            if (contentEnd > tagEnd) {
                return null
            }
            if (frameId == "APIC") {
                return readApicPayload(start = contentStart, end = contentEnd)
            }
            offset = contentEnd
        }
        return null
    }

    // ID3v2.2 使用 PIC 帧，结构与 APIC 相近但 MIME 字段固定 3 字节。
    private fun ByteArray.readId3v22Artwork(tagEnd: Int): ByteArray? {
        var offset: Int = ID3_HEADER_SIZE
        while (offset + ID3_V22_FRAME_HEADER_SIZE <= tagEnd) {
            val frameId: String = decodeAscii(start = offset, length = 3)
            val frameSize: Int = read24BitInt(offset = offset + 3)
            if (frameId.isBlank() || frameSize <= 0) {
                return null
            }
            val contentStart: Int = offset + ID3_V22_FRAME_HEADER_SIZE
            val contentEnd: Int = contentStart + frameSize
            if (contentEnd > tagEnd) {
                return null
            }
            if (frameId == "PIC") {
                return readPicPayload(start = contentStart, end = contentEnd)
            }
            offset = contentEnd
        }
        return null
    }

    // 解析 APIC 文本头并返回图片原始字节，文本编码只影响描述分隔符长度。
    private fun ByteArray.readApicPayload(
        start: Int,
        end: Int,
    ): ByteArray? {
        if (start + 4 >= end) {
            return null
        }
        val encoding: Int = this[start].toInt() and BYTE_MASK
        val mimeEnd: Int = indexOfZeroByte(start = start + 1, end = end) ?: return null
        val descriptionStart: Int = mimeEnd + 2
        val imageStart: Int = findDescriptionEnd(
            start = descriptionStart,
            end = end,
            encoding = encoding,
        ) ?: return null
        if (imageStart >= end) {
            return null
        }
        return copyOfRange(fromIndex = imageStart, toIndex = end)
    }

    // 解析 ID3v2.2 PIC 帧，跳过 encoding、image format、picture type 和描述。
    private fun ByteArray.readPicPayload(
        start: Int,
        end: Int,
    ): ByteArray? {
        if (start + 5 >= end) {
            return null
        }
        val encoding: Int = this[start].toInt() and BYTE_MASK
        val descriptionStart: Int = start + 5
        val imageStart: Int = findDescriptionEnd(
            start = descriptionStart,
            end = end,
            encoding = encoding,
        ) ?: return null
        if (imageStart >= end) {
            return null
        }
        return copyOfRange(fromIndex = imageStart, toIndex = end)
    }
}

// 文本描述以 0 结尾；UTF-16 编码使用两个 0 字节结束。
private fun ByteArray.findDescriptionEnd(
    start: Int,
    end: Int,
    encoding: Int,
): Int? {
    if (encoding == UTF_16_ENCODING || encoding == UTF_16BE_ENCODING) {
        var offset: Int = start
        while (offset + 1 < end) {
            if (this[offset].toInt() == 0 && this[offset + 1].toInt() == 0) {
                return offset + 2
            }
            offset += 2
        }
        return null
    }
    val descriptionEnd: Int = indexOfZeroByte(start = start, end = end) ?: return null
    return descriptionEnd + 1
}

// 读取 ID3 syncsafe 整数，避免把最高位当作数据位。
private fun ByteArray.readSyncSafeInt(offset: Int): Int {
    return ((this[offset].toInt() and 0x7F) shl 21) or
        ((this[offset + 1].toInt() and 0x7F) shl 14) or
        ((this[offset + 2].toInt() and 0x7F) shl 7) or
        (this[offset + 3].toInt() and 0x7F)
}

// 读取 ID3v2.3 帧长度的普通大端整数。
private fun ByteArray.readInt(offset: Int): Int {
    return ((this[offset].toInt() and BYTE_MASK) shl 24) or
        ((this[offset + 1].toInt() and BYTE_MASK) shl 16) or
        ((this[offset + 2].toInt() and BYTE_MASK) shl 8) or
        (this[offset + 3].toInt() and BYTE_MASK)
}

// 读取 ID3v2.2 的三字节帧长度。
private fun ByteArray.read24BitInt(offset: Int): Int {
    return ((this[offset].toInt() and BYTE_MASK) shl 16) or
        ((this[offset + 1].toInt() and BYTE_MASK) shl 8) or
        (this[offset + 2].toInt() and BYTE_MASK)
}

// 仅解码 ID3 帧标识所需的 ASCII 字节。
private fun ByteArray.decodeAscii(
    start: Int,
    length: Int,
): String {
    return copyOfRange(fromIndex = start, toIndex = start + length)
        .map { byte: Byte -> byte.toInt().toChar() }
        .joinToString(separator = "")
}

// 查找单字节 0 结束符。
private fun ByteArray.indexOfZeroByte(
    start: Int,
    end: Int,
): Int? {
    for (index: Int in start until end) {
        if (this[index].toInt() == 0) {
            return index
        }
    }
    return null
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

private const val ID3_HEADER_SIZE = 10
private const val ID3_FRAME_HEADER_SIZE = 10
private const val ID3_V22_FRAME_HEADER_SIZE = 6
private const val BYTE_MASK = 0xFF
private const val UTF_16_ENCODING = 1
private const val UTF_16BE_ENCODING = 2
