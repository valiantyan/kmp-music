package com.yanhao.kmpmusic.playback

import android.content.Context
import androidx.media3.common.util.UnstableApi
import com.yanhao.kmpmusic.domain.model.CoverArt
import com.yanhao.kmpmusic.domain.model.PlayableMedia
import java.io.File
import kotlin.test.Test
import kotlin.test.assertContentEquals
import org.robolectric.RuntimeEnvironment
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@UnstableApi
@RunWith(RobolectricTestRunner::class)
class AndroidPlayableMediaMapperTest {
    @Test
    fun toMediaItemsUsesExtractedCoverImageUriForNotificationArtwork(): Unit {
        val context: Context = RuntimeEnvironment.getApplication().applicationContext
        val artworkBytes: ByteArray = byteArrayOf(1, 2, 3, 4, 5)
        val artworkFile: File = File(context.cacheDir, "notification-cover.art").apply {
            writeBytes(artworkBytes)
        }
        val mediaItems = AndroidPlayableMediaMapper.toMediaItems(
            context = context,
            items = listOf(
                PlayableMedia(
                    songId = "androidMediaStore:42",
                    title = "质感雨声",
                    artist = "椰椰拿铁",
                    album = "本地音乐",
                    durationMs = 180_000L,
                    localUri = "content://media/external/audio/media/42",
                    coverArt = CoverArt.HeroLocalMusic,
                    coverImageUri = artworkFile.toURI().toString(),
                    mimeType = "audio/mpeg",
                ),
            ),
        )

        assertContentEquals(
            expected = artworkBytes,
            actual = mediaItems.single().mediaMetadata.artworkData,
        )
    }
}
