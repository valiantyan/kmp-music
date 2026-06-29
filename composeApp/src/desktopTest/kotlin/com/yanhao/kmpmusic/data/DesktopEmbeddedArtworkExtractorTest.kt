package com.yanhao.kmpmusic.data

import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertNull
import org.jaudiotagger.audio.AudioFile
import org.jaudiotagger.audio.generic.GenericAudioHeader
import org.jaudiotagger.tag.Tag
import org.jaudiotagger.tag.datatype.Artwork
import org.jaudiotagger.tag.id3.ID3v24Tag

/**
 * 桌面封面提取测试，保护 macOS/Windows/Linux 扫描时不同音频格式的 artwork 读取能力。
 */
class DesktopEmbeddedArtworkExtractorTest {
    private val extractor: DesktopEmbeddedArtworkExtractor = DesktopEmbeddedArtworkExtractor()

    /**
     * jaudiotagger 已解析出的封面字节必须进入扫描缓存链路，避免 UI 退回默认封面。
     */
    @Test
    fun readEmbeddedArtworkBytesReturnsFirstArtworkBinaryData(): Unit {
        val artworkBytes: ByteArray = byteArrayOf(1, 2, 3, 4)
        val audioFile: AudioFile = createAudioFileWithArtwork(artworkBytes = artworkBytes)

        val extractedBytes: ByteArray? = with(extractor) {
            audioFile.readEmbeddedArtworkBytes()
        }

        assertContentEquals(expected = artworkBytes, actual = extractedBytes)
    }

    /**
     * 缺少真实图片字节时返回空结果，让上层继续使用本地音乐占位封面。
     */
    @Test
    fun readEmbeddedArtworkBytesReturnsNullForEmptyArtwork(): Unit {
        val audioFile: AudioFile = createAudioFileWithArtwork(artworkBytes = ByteArray(size = 0))

        val extractedBytes: ByteArray? = with(extractor) {
            audioFile.readEmbeddedArtworkBytes()
        }

        assertNull(actual = extractedBytes)
    }

    // 构造 jaudiotagger 标准音频模型，让测试关注应用自己的 artwork 读取规则。
    private fun createAudioFileWithArtwork(artworkBytes: ByteArray): AudioFile {
        val audioPath: Path = Files.createTempFile("kmp-music-artwork-audio", ".mp3")
        val header: GenericAudioHeader = GenericAudioHeader()
        header.setLength(1)
        val artwork: Artwork = Artwork()
        artwork.binaryData = artworkBytes
        artwork.mimeType = "image/png"
        val tag: Tag = ID3v24Tag()
        tag.setField(artwork)
        return AudioFile(audioPath.toFile(), header, tag)
    }
}
