package com.yanhao.kmpmusic.playback

import android.content.Context
import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import com.yanhao.kmpmusic.domain.model.PlayableMedia

/**
 * Android Media3 媒体项映射器，负责把 common 媒体模型转为官方 [MediaItem]。
 */
internal object AndroidPlayableMediaMapper {
    /**
     * 转换完整播放队列，确保客户端通过 [androidx.media3.session.MediaController] 下发标准媒体项。
     */
    fun toMediaItems(context: Context, items: List<PlayableMedia>): List<MediaItem> {
        return items.map { item: PlayableMedia ->
            item.toMediaItem(context = context)
        }
    }

    // 把单个 common 媒体项映射成 Media3 播放器可消费的媒体项。
    private fun PlayableMedia.toMediaItem(context: Context): MediaItem {
        return MediaItem.Builder()
            .setUri(Uri.parse(localUri))
            .setMediaId(songId)
            .setMediaMetadata(toMediaMetadata(context = context))
            .build()
    }

    // 系统媒体通知只消费 Media3 metadata，不能依赖应用自绘布局填充文案和封面。
    private fun PlayableMedia.toMediaMetadata(context: Context): MediaMetadata {
        val metadataBuilder: MediaMetadata.Builder = MediaMetadata.Builder()
            .setTitle(title)
            .setArtist(artist)
            .setAlbumTitle(album)
            .setDurationMs(durationMs?.takeIf { value: Long -> value >= 0L })
        AndroidPlaybackMediaMetadataAssets.artworkData(
            context = context,
            coverArt = coverArt,
        )?.let { artworkData: ByteArray ->
            metadataBuilder.setArtworkData(
                artworkData,
                MediaMetadata.PICTURE_TYPE_FRONT_COVER,
            )
        }
        return metadataBuilder.build()
    }
}
