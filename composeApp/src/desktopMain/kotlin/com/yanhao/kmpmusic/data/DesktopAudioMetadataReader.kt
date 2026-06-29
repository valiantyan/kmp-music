package com.yanhao.kmpmusic.data

import java.io.IOException
import java.nio.file.Path
import org.jaudiotagger.audio.AudioFile
import org.jaudiotagger.audio.AudioFileIO
import org.jaudiotagger.audio.AudioHeader
import org.jaudiotagger.audio.exceptions.CannotReadException
import org.jaudiotagger.audio.exceptions.InvalidAudioFrameException
import org.jaudiotagger.audio.exceptions.ReadOnlyFileException
import org.jaudiotagger.tag.FieldKey
import org.jaudiotagger.tag.KeyNotFoundException
import org.jaudiotagger.tag.Tag
import org.jaudiotagger.tag.TagException

/**
 * 桌面端音频元数据，scanner 用它对齐 Android MediaStore 的字段语义。
 *
 * @property title 音频标签中的歌曲标题。
 * @property artist 音频标签中的歌手。
 * @property album 音频标签中的专辑。
 * @property durationMs 音频头中的时长，单位毫秒。
 */
internal data class DesktopAudioMetadata(
    val title: String? = null,
    val artist: String? = null,
    val album: String? = null,
    val durationMs: Long? = null,
)

/**
 * JVM 桌面端音频标签读取器，避免扫描层只能依赖文件名和目录名猜测曲库信息。
 */
internal class DesktopAudioMetadataReader {
    /**
     * 读取常见音频格式的标题、歌手、专辑和时长；读取失败时返回空元数据让 scanner 继续兜底。
     */
    fun readMetadata(audioPath: Path): DesktopAudioMetadata {
        return try {
            val audioFile: AudioFile = AudioFileIO.read(audioPath.toFile())
            audioFile.toDesktopAudioMetadata()
        } catch (cannotReadException: CannotReadException) {
            DesktopAudioMetadata()
        } catch (tagException: TagException) {
            DesktopAudioMetadata()
        } catch (readOnlyFileException: ReadOnlyFileException) {
            DesktopAudioMetadata()
        } catch (invalidAudioFrameException: InvalidAudioFrameException) {
            DesktopAudioMetadata()
        } catch (ioException: IOException) {
            DesktopAudioMetadata()
        } catch (runtimeException: RuntimeException) {
            DesktopAudioMetadata()
        }
    }

    // 把 jaudiotagger 的格式模型收敛为应用自己的平台无关扫描元数据。
    internal fun AudioFile.toDesktopAudioMetadata(): DesktopAudioMetadata {
        return DesktopAudioMetadata(
            title = tag.readKnownText(fieldKey = FieldKey.TITLE),
            artist = tag.readKnownText(fieldKey = FieldKey.ARTIST),
            album = tag.readKnownText(fieldKey = FieldKey.ALBUM),
            durationMs = audioHeader.readDurationMs(),
        )
    }

    // 统一清理空字符串和异常标签，避免 common 层把脏标签当成真实曲库维度。
    private fun Tag?.readKnownText(fieldKey: FieldKey): String? {
        val value: String = try {
            this?.getFirst(fieldKey)?.trim().orEmpty()
        } catch (keyNotFoundException: KeyNotFoundException) {
            return null
        } catch (unsupportedOperationException: UnsupportedOperationException) {
            return null
        }
        if (value.isBlank()) {
            return null
        }
        return value
    }

    // jaudiotagger 的 [AudioHeader.getTrackLength] 返回秒，这里转换为 common 层使用的毫秒。
    private fun AudioHeader?.readDurationMs(): Long? {
        val trackLengthSeconds: Int = this?.trackLength ?: return null
        if (trackLengthSeconds <= 0) {
            return null
        }
        return trackLengthSeconds * 1_000L
    }
}
