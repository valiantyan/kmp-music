package com.yanhao.kmpmusic.feature.components

import com.yanhao.kmpmusic.domain.model.CoverArt
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CoverArtImageRequestTest {
    @Test
    fun buildCoverArtImageRequestUsesCoverImageUriBeforeFallbackResource(): Unit {
        val request: CoverArtImageRequest = buildCoverArtImageRequest(
            coverArt = CoverArt.AlbumBestOfMe,
            coverImageUri = "file:///tmp/cover.art",
        )
        assertEquals("file:///tmp/cover.art", request.primaryModel)
        assertEquals("drawable/album_best_of_me.png", request.fallbackResourcePath)
        assertTrue(request.usesExternalCover)
    }

    @Test
    fun buildCoverArtImageRequestUsesFallbackResourceWhenCoverImageUriIsBlank(): Unit {
        val request: CoverArtImageRequest = buildCoverArtImageRequest(
            coverArt = CoverArt.CoverSeaDream,
            coverImageUri = "   ",
        )
        assertEquals("drawable/cover_sea_dream.png", request.primaryModel)
        assertEquals("drawable/cover_sea_dream.png", request.fallbackResourcePath)
        assertFalse(request.usesExternalCover)
    }

    @Test
    fun coverArtResourcePathMapsEveryCoverArtValue(): Unit {
        val paths: Set<String> = CoverArt.entries.map(::coverArtResourcePath).toSet()
        assertEquals(CoverArt.entries.size, paths.size)
        assertEquals("drawable/album_best_of_me.png", coverArtResourcePath(CoverArt.AlbumBestOfMe))
        assertEquals("drawable/album_river_year.png", coverArtResourcePath(CoverArt.AlbumRiverYear))
        assertEquals("drawable/album_time_forest.png", coverArtResourcePath(CoverArt.AlbumTimeForest))
        assertEquals("drawable/cover_sea_dream.png", coverArtResourcePath(CoverArt.CoverSeaDream))
        assertEquals("drawable/cover_summer_waltz.png", coverArtResourcePath(CoverArt.CoverSummerWaltz))
        assertEquals("drawable/hero_local_folder.png", coverArtResourcePath(CoverArt.HeroLocalMusic))
    }
}
