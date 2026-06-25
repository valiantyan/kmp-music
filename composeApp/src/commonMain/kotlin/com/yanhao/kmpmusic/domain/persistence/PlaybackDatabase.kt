package com.yanhao.kmpmusic.domain.persistence

import androidx.room3.ConstructedBy
import androidx.room3.Dao
import androidx.room3.Database
import androidx.room3.Entity
import androidx.room3.Index
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.PrimaryKey
import androidx.room3.Query
import androidx.room3.RoomDatabase
import androidx.room3.RoomDatabaseConstructor

/**
 * 播放快照主记录。
 *
 * @property id 单行快照固定主键。
 * @property currentSongId 最近一次播放歌曲标识。
 * @property currentIndex 最近一次队列下标。
 * @property playbackMode 最近一次播放模式名称。
 * @property positionMs 最近一次播放进度。
 * @property durationMs 最近一次歌曲总时长。
 * @property updatedAt 最近一次保存时间。
 */
@Entity(tableName = "playback_snapshot")
data class PlaybackSnapshotEntity(
    @PrimaryKey val id: Int = 1,
    val currentSongId: String?,
    val currentIndex: Int,
    val playbackMode: String,
    val positionMs: Long,
    val durationMs: Long?,
    val updatedAt: Long,
)

/**
 * 播放队列中的单个歌曲位置记录。
 *
 * @property position 队列顺序下标。
 * @property songId 该位置对应的歌曲标识。
 */
@Entity(tableName = "playback_queue_item", primaryKeys = ["position"])
data class PlaybackQueueItemEntity(
    val position: Int,
    val songId: String,
)

/**
 * 收藏歌曲记录，供后续 favorites 持久化任务复用同一数据库。
 *
 * @property songId 被收藏的歌曲标识。
 * @property updatedAt 最近一次收藏状态更新时间。
 */
@Entity(tableName = "favorite_song")
data class FavoriteSongEntity(
    @PrimaryKey val songId: String,
    val updatedAt: Long,
)

/**
 * 持久化本地歌曲扫描元数据，收藏状态由 favorite_song 独立保存。
 */
@Entity(
    tableName = "local_song",
    indices = [
        Index(value = ["sourceKind", "isAvailable"]),
        Index(value = ["isAvailable", "modifiedAt"]),
    ],
)
data class LocalSongEntity(
    @PrimaryKey val id: String,
    val sourceId: String,
    val sourceKind: String,
    val localUri: String,
    val fileName: String,
    val title: String?,
    val artist: String?,
    val album: String?,
    val durationMs: Long?,
    val mimeType: String?,
    val sizeBytes: Long?,
    val modifiedAt: Long?,
    val coverArt: String,
    val lastScannedAt: Long,
    val isAvailable: Boolean,
)

/**
 * 播放快照读写接口。
 */
@Dao
interface PlaybackSnapshotDao {
    /**
     * 读取当前唯一快照记录。
     *
     * @return 已保存的快照实体，没有数据时返回 null。
     */
    @Query("SELECT * FROM playback_snapshot WHERE id = 1")
    suspend fun getSnapshot(): PlaybackSnapshotEntity?

    /**
     * 覆盖写入最新播放快照。
     *
     * @param entity 要保存的快照实体。
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveSnapshot(entity: PlaybackSnapshotEntity)

    /** 清空已有快照，供将来需要显式重置时复用。 */
    @Query("DELETE FROM playback_snapshot")
    suspend fun clearSnapshot()
}

/**
 * 播放队列读写接口。
 */
@Dao
interface PlaybackQueueDao {
    /**
     * 按原队列顺序读取所有歌曲。
     *
     * @return 已保存的队列记录。
     */
    @Query("SELECT * FROM playback_queue_item ORDER BY position ASC")
    suspend fun getQueueItems(): List<PlaybackQueueItemEntity>

    /**
     * 批量写入完整队列。
     *
     * @param items 需要按顺序保存的队列项。
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<PlaybackQueueItemEntity>)

    /** 清空旧队列，避免残留历史顺序。 */
    @Query("DELETE FROM playback_queue_item")
    suspend fun clearQueue()
}

/**
 * 收藏歌曲读写接口。
 */
@Dao
interface FavoriteSongDao {
    /**
     * 读取所有已收藏歌曲标识。
     *
     * @return 收藏歌曲标识列表。
     */
    @Query("SELECT songId FROM favorite_song")
    suspend fun getFavoriteSongIds(): List<String>

    /**
     * 覆盖写入收藏记录。
     *
     * @param entity 要保存的收藏项。
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveFavorite(entity: FavoriteSongEntity)

    /**
     * 删除指定歌曲的收藏记录。
     *
     * @param songId 需要取消收藏的歌曲标识。
     */
    @Query("DELETE FROM favorite_song WHERE songId = :songId")
    suspend fun deleteFavorite(songId: String)
}

/**
 * 本地歌曲读写接口。
 */
@Dao
interface LocalSongDao {
    /** 读取首页最多展示的可用歌曲。 */
    @Query(
        """
        SELECT * FROM local_song
        WHERE isAvailable = 1
        ORDER BY COALESCE(modifiedAt, -1) DESC, LOWER(COALESCE(title, fileName)) ASC
        LIMIT :limit
        """,
    )
    suspend fun getHomePreview(limit: Int): List<LocalSongEntity>

    /** 读取全部可用歌曲，供本地二级页、搜索和详情使用。 */
    @Query(
        """
        SELECT * FROM local_song
        WHERE isAvailable = 1
        ORDER BY COALESCE(modifiedAt, -1) DESC, LOWER(COALESCE(title, fileName)) ASC
        """,
    )
    suspend fun getAllAvailableSongs(): List<LocalSongEntity>

    /** 按来源读取可用歌曲 id，用于扫描后标记消失歌曲。 */
    @Query("SELECT id FROM local_song WHERE sourceKind = :sourceKind AND isAvailable = 1")
    suspend fun getAvailableSongIdsBySource(sourceKind: String): List<String>

    /** 读取当前仍有可用歌曲的来源类型，供全量扫描空结果时判定覆盖范围。 */
    @Query("SELECT DISTINCT sourceKind FROM local_song WHERE isAvailable = 1")
    suspend fun getAvailableSourceKinds(): List<String>

    /** 覆盖写入扫描确认存在的歌曲元数据。 */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSongs(songs: List<LocalSongEntity>)

    /** 标记指定来源下本次未扫描到的歌曲不可用。 */
    @Query("UPDATE local_song SET isAvailable = 0 WHERE sourceKind = :sourceKind AND id IN (:songIds)")
    suspend fun markUnavailable(sourceKind: String, songIds: List<String>)

    /** 统计当前可用歌曲数。 */
    @Query("SELECT COUNT(*) FROM local_song WHERE isAvailable = 1")
    suspend fun countAvailableSongs(): Int

    /** 统计当前可用专辑数。 */
    @Query(
        """
        SELECT COUNT(*) FROM (
            SELECT DISTINCT LOWER(COALESCE(NULLIF(TRIM(album), ''), '未知专辑')) AS albumKey
            FROM local_song
            WHERE isAvailable = 1
        )
        """,
    )
    suspend fun countAvailableAlbums(): Int

    /** 统计当前可用歌手数。 */
    @Query(
        """
        SELECT COUNT(*) FROM (
            SELECT DISTINCT LOWER(COALESCE(NULLIF(TRIM(artist), ''), '未知歌手')) AS artistKey
            FROM local_song
            WHERE isAvailable = 1
        )
        """,
    )
    suspend fun countAvailableArtists(): Int
}

/**
 * 播放相关本地数据库，统一收纳播放快照、收藏与本地歌曲元数据。
 */
@Database(
    entities = [
        PlaybackSnapshotEntity::class,
        PlaybackQueueItemEntity::class,
        FavoriteSongEntity::class,
        LocalSongEntity::class,
    ],
    version = 2,
)
@ConstructedBy(PlaybackDatabaseConstructor::class)
abstract class PlaybackDatabase : RoomDatabase() {
    /** 暴露播放快照 DAO。 */
    abstract fun playbackSnapshotDao(): PlaybackSnapshotDao

    /** 暴露播放队列 DAO。 */
    abstract fun playbackQueueDao(): PlaybackQueueDao

    /** 暴露收藏歌曲 DAO。 */
    abstract fun favoriteSongDao(): FavoriteSongDao

    /** 暴露本地歌曲 DAO。 */
    abstract fun localSongDao(): LocalSongDao
}

/**
 * 由 Room3 编译期生成的跨平台数据库构造器声明。
 */
@Suppress("KotlinNoActualForExpect")
expect object PlaybackDatabaseConstructor : RoomDatabaseConstructor<PlaybackDatabase> {
    override fun initialize(): PlaybackDatabase
}
