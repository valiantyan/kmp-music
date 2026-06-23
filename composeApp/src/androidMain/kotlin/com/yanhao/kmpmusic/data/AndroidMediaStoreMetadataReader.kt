package com.yanhao.kmpmusic.data

import android.content.ContentUris
import android.database.Cursor
import android.net.Uri
import android.provider.MediaStore
import com.yanhao.kmpmusic.domain.model.CoverArt
import com.yanhao.kmpmusic.domain.model.LocalMusicSourceKind
import com.yanhao.kmpmusic.domain.model.MusicFileMetadata

/**
 * Android MediaStore 游标读取器，隔离平台字段到 common 元数据的映射规则。
 */
internal class AndroidMediaStoreMetadataReader {
    /**
     * 读取整份游标并生成扫描元数据列表。
     */
    fun readMetadata(
        cursor: Cursor,
        collectionUri: Uri,
    ): List<MusicFileMetadata> {
        val idColumn: Int = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
        val nameColumn: Int = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME)
        val titleColumn: Int = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
        val artistColumn: Int = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
        val albumColumn: Int = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
        val durationColumn: Int = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
        val mimeTypeColumn: Int = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.MIME_TYPE)
        val sizeColumn: Int = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE)
        val modifiedColumn: Int = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_MODIFIED)
        val discovered: MutableList<MusicFileMetadata> = mutableListOf()
        while (cursor.moveToNext()) {
            discovered += cursor.toMetadata(
                collectionUri = collectionUri,
                idColumn = idColumn,
                nameColumn = nameColumn,
                titleColumn = titleColumn,
                artistColumn = artistColumn,
                albumColumn = albumColumn,
                durationColumn = durationColumn,
                mimeTypeColumn = mimeTypeColumn,
                sizeColumn = sizeColumn,
                modifiedColumn = modifiedColumn,
            )
        }
        return discovered
    }

    // 单条 MediaStore 记录映射，保留 sourceId 和 content URI 作为稳定播放入口。
    private fun Cursor.toMetadata(
        collectionUri: Uri,
        idColumn: Int,
        nameColumn: Int,
        titleColumn: Int,
        artistColumn: Int,
        albumColumn: Int,
        durationColumn: Int,
        mimeTypeColumn: Int,
        sizeColumn: Int,
        modifiedColumn: Int,
    ): MusicFileMetadata {
        val mediaId: Long = getLong(idColumn)
        val mediaUri: Uri = ContentUris.withAppendedId(collectionUri, mediaId)
        return MusicFileMetadata(
            sourceId = mediaId.toString(),
            sourceKind = LocalMusicSourceKind.AndroidMediaStore,
            localUri = mediaUri.toString(),
            fileName = getKnownText(columnIndex = nameColumn) ?: "$mediaId.audio",
            title = getKnownText(columnIndex = titleColumn),
            artist = getKnownText(columnIndex = artistColumn),
            album = getKnownText(columnIndex = albumColumn),
            durationMs = getPositiveLong(columnIndex = durationColumn),
            mimeType = getKnownText(columnIndex = mimeTypeColumn),
            sizeBytes = getPositiveLong(columnIndex = sizeColumn),
            modifiedAt = getPositiveLong(columnIndex = modifiedColumn)?.let { modifiedSeconds: Long ->
                modifiedSeconds * 1_000L
            },
            coverArt = CoverArt.HeroLocalMusic,
        )
    }

    // 读取非空、非系统未知占位的文本元数据。
    private fun Cursor.getKnownText(columnIndex: Int): String? {
        if (isNull(columnIndex)) {
            return null
        }
        val value: String = getString(columnIndex)?.trim().orEmpty()
        if (value.isBlank() || value == MediaStore.UNKNOWN_STRING || value == "<unknown>") {
            return null
        }
        return value
    }

    // 读取正数 Long；0 或缺失视为未知，由 common 合并层兜底。
    private fun Cursor.getPositiveLong(columnIndex: Int): Long? {
        if (isNull(columnIndex)) {
            return null
        }
        val value: Long = getLong(columnIndex)
        if (value <= 0L) {
            return null
        }
        return value
    }
}
