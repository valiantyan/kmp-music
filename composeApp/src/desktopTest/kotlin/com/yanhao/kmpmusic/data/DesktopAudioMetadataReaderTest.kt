package com.yanhao.kmpmusic.data

import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import org.jaudiotagger.audio.AudioFile
import org.jaudiotagger.audio.generic.GenericAudioHeader
import org.jaudiotagger.audio.generic.GenericTag
import org.jaudiotagger.tag.FieldKey
import org.jaudiotagger.tag.Tag

/**
 * 桌面音频元数据读取测试，保护 macOS 文件夹扫描与 Android MediaStore 的字段对齐。
 */
class DesktopAudioMetadataReaderTest {
    private val reader: DesktopAudioMetadataReader = DesktopAudioMetadataReader()

    /**
     * jaudiotagger 读取到的标题、歌手、专辑和时长应被完整映射，避免 UI 退回未知占位。
     */
    @Test
    fun toDesktopAudioMetadataReturnsTagFieldsAndDuration(): Unit {
        val audioFile: AudioFile = createTaggedAudioFile()

        val metadata: DesktopAudioMetadata = with(reader) {
            audioFile.toDesktopAudioMetadata()
        }

        assertEquals(expected = "桌面歌曲", actual = metadata.title)
        assertEquals(expected = "桌面歌手", actual = metadata.artist)
        assertEquals(expected = "桌面专辑", actual = metadata.album)
        assertEquals(expected = 1_000L, actual = metadata.durationMs)
    }

    /**
     * 损坏或不支持的文件不能中断扫描，只返回空元数据交给 scanner 兜底。
     */
    @Test
    fun readMetadataReturnsEmptyMetadataForUnreadableAudio(): Unit {
        val audioPath: Path = Files.createTempFile("kmp-music-broken-audio", ".mp3")
        Files.writeString(audioPath, "not an audio file")

        val metadata: DesktopAudioMetadata = reader.readMetadata(audioPath = audioPath)

        assertNull(actual = metadata.title)
        assertNull(actual = metadata.artist)
        assertNull(actual = metadata.album)
        assertNull(actual = metadata.durationMs)
    }

    // 构造 jaudiotagger 标准模型，让测试只关注应用自己的字段映射规则。
    private fun createTaggedAudioFile(): AudioFile {
        val audioPath: Path = Files.createTempFile("kmp-music-tagged-audio", ".wav")
        val header: GenericAudioHeader = GenericAudioHeader()
        header.setLength(1)
        val tag: Tag = object : GenericTag() {}
        tag.setField(FieldKey.TITLE, "桌面歌曲")
        tag.setField(FieldKey.ARTIST, "桌面歌手")
        tag.setField(FieldKey.ALBUM, "桌面专辑")
        return AudioFile(audioPath.toFile(), header, tag)
    }
}
