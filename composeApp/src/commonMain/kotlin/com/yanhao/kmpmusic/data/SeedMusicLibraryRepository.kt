package com.yanhao.kmpmusic.data

import com.yanhao.kmpmusic.domain.model.Album
import com.yanhao.kmpmusic.domain.model.Artist
import com.yanhao.kmpmusic.domain.model.CoverArt
import com.yanhao.kmpmusic.domain.model.Song
import com.yanhao.kmpmusic.domain.repository.MusicLibraryRepository

/**
 * 来自高保真原型的 seed 音乐库，实现阶段一的稳定可运行数据源。
 */
class SeedMusicLibraryRepository : MusicLibraryRepository {
    // 原型专辑数据，作为歌曲展开和详情展示的唯一来源。
    private val albums: List<Album> = listOf(
        Album("river-year", "似水流年", "旅行团乐队", 12, CoverArt.AlbumRiverYear, "清晨海风", "2026"),
        Album("best-of-me", "The Best of Me", "A-Lin", 15, CoverArt.AlbumBestOfMe, "安静人声", "2024"),
        Album("time-forest", "时光森林", "苏打绿", 10, CoverArt.AlbumTimeForest, "森林漫游", "2025"),
        Album("dream-stories", "Dream Stories", "久石让", 18, CoverArt.CoverSummerWaltz, "钢琴叙事", "2021"),
    )

    // 原型曲库目录，用于生成完整专辑曲目。
    private val albumTracks: Map<String, List<Pair<String, String>>> = mapOf(
        "river-year" to listOf(
            "海边的梦" to "3:45",
            "像水流年" to "3:58",
            "沿岸公路" to "4:11",
            "清晨浪花" to "3:27",
            "风把云吹远" to "4:03",
            "南方车站" to "3:52",
            "雨后码头" to "4:19",
            "慢慢靠岸" to "3:31",
            "月光旧信" to "4:06",
            "潮汐电台" to "3:40",
            "远处的灯" to "4:24",
            "回到海边" to "3:49",
        ),
        "best-of-me" to listOf(
            "The Best of Me" to "4:07",
            "给我一个理由忘记" to "4:43",
            "有一种悲伤" to "4:11",
            "失恋无罪" to "4:27",
            "幸福了 然后呢" to "4:35",
            "寂寞不痛" to "4:19",
            "以前以后" to "4:32",
            "难得" to "4:01",
            "我们会更好的" to "4:18",
            "做我自己" to "3:52",
            "爱上你等于爱上寂寞" to "4:36",
            "四季" to "4:08",
            "今晚你想念的人是不是我" to "4:25",
            "大大的拥抱" to "3:58",
            "声声慢" to "4:40",
        ),
        "time-forest" to listOf(
            "时光森林" to "5:11",
            "小情歌" to "4:33",
            "无与伦比的美丽" to "4:55",
            "我好想你" to "5:24",
            "频率" to "4:12",
            "迟到千年" to "4:44",
            "他夏了夏天" to "4:31",
            "故事" to "4:08",
            "你被写在我的歌里" to "4:37",
            "早点回家" to "4:02",
        ),
        "dream-stories" to listOf(
            "Summer Waltz" to "4:25",
            "One Summer's Day" to "4:08",
            "The Rain" to "5:02",
            "A Town With an Ocean View" to "3:37",
            "Merry-Go-Round" to "5:16",
            "Silent Garden" to "3:54",
            "Dream Stories" to "4:48",
            "Moonlit Piano" to "3:42",
            "Blue Train" to "4:13",
            "Paper Airship" to "3:51",
            "Rainy Window" to "4:20",
            "Little Journey" to "3:35",
            "Forest Postcard" to "4:06",
            "Wind Theme" to "3:58",
            "Night Lantern" to "4:29",
            "Old Theater" to "4:15",
            "Goodbye Waltz" to "3:47",
            "After Summer" to "4:34",
        ),
    )

    // 原型歌手数据，收藏与详情页共用。
    private val artists: List<Artist> = listOf(
        Artist("trip", "旅行团乐队", 18, CoverArt.AlbumRiverYear, "独立流行"),
        Artist("alin", "A-Lin", 15, CoverArt.AlbumBestOfMe, "华语人声"),
        Artist("sodagreen", "苏打绿", 10, CoverArt.AlbumTimeForest, "乐团精选"),
        Artist("hisaishi", "久石让", 18, CoverArt.CoverSummerWaltz, "器乐原声"),
    )

    // 生成后的完整歌曲列表，保证专辑数量和曲目列表一致。
    private val songs: List<Song> = buildSongs()

    /** 获取原型歌曲列表。 */
    override fun getSongs(): List<Song> = songs

    /** 获取原型专辑列表。 */
    override fun getAlbums(): List<Album> = albums

    /** 获取原型歌手列表。 */
    override fun getArtists(): List<Artist> = artists

    // 由专辑目录生成歌曲，避免手工维护曲目数不一致。
    private fun buildSongs(): List<Song> {
        val featuredSongs: Map<String, FeaturedSongSeed> = mapOf(
            "似水流年::海边的梦" to FeaturedSongSeed("sea-dream", CoverArt.CoverSeaDream, true, "今天 08:12", "本地 FLAC", "海风吹过窗边，像一段刚醒来的旋律。"),
            "Dream Stories::Summer Waltz" to FeaturedSongSeed("summer-waltz", CoverArt.CoverSummerWaltz, false, "昨天 22:18", "本地 AAC", "琴键落下时，夏天慢慢转身。"),
            "似水流年::像水流年" to FeaturedSongSeed("river", CoverArt.AlbumRiverYear, true, "周二 19:42", "本地 FLAC", "时间贴着掌心流过，带走一点点喧哗。"),
            "The Best of Me::The Best of Me" to FeaturedSongSeed("best", CoverArt.AlbumBestOfMe, true, "周一 12:34", "本地 ALAC", "人声靠近时，世界忽然变得很轻。"),
            "时光森林::时光森林" to FeaturedSongSeed("forest", CoverArt.AlbumTimeForest, false, "6 月 13 日", "本地 MP3", "林间的光斑，一路跳到副歌里。"),
        )
        return albums.flatMap { album ->
            albumTracks.getValue(album.id).mapIndexed { index, track ->
                val seed: FeaturedSongSeed? = featuredSongs["${album.title}::${track.first}"]
                Song(
                    id = seed?.id ?: "${album.id}-${(index + 1).toString().padStart(2, '0')}",
                    title = track.first,
                    artist = album.artist,
                    album = album.title,
                    duration = track.second,
                    coverArt = seed?.coverArt ?: album.coverArt,
                    isLiked = seed?.isLiked ?: false,
                    lastPlayed = seed?.lastPlayed ?: "本地专辑",
                    quality = seed?.quality ?: if (album.id == "time-forest") "本地 MP3" else "本地 FLAC",
                    lyric = seed?.lyric ?: "${album.mood}里的一段旋律。",
                    trackNumber = index + 1,
                )
            }
        } + Song(
            id = "long-night",
            title = "夜航片段",
            artist = "陈婧霏",
            album = "夜航",
            duration = "3:36",
            coverArt = CoverArt.AlbumBestOfMe,
            isLiked = false,
            lastPlayed = "6 月 10 日",
            quality = "本地 AAC",
            lyric = "夜色铺开，低频像远处的灯。",
            trackNumber = 1,
        )
    }

    /**
     * 原型重点歌曲的补充字段。
     */
    private data class FeaturedSongSeed(
        val id: String,
        val coverArt: CoverArt,
        val isLiked: Boolean,
        val lastPlayed: String,
        val quality: String,
        val lyric: String,
    )
}
