package com.yanhao.kmpmusic.feature.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yanhao.kmpmusic.domain.model.Album
import com.yanhao.kmpmusic.domain.model.Artist
import com.yanhao.kmpmusic.feature.components.AlbumCard
import com.yanhao.kmpmusic.feature.components.ArtistRow

// 专辑分段沿用现有数据入口，但保持收藏页全屏背景和页边距。
internal fun LazyListScope.favoriteAlbumItems(
    likedAlbums: List<Album>,
    onAlbumOpen: (Album) -> Unit,
) {
    if (likedAlbums.isEmpty()) {
        item(key = "favorites-empty-albums", contentType = "favorites-empty") {
            FavoritesEmptyState(message = "收藏歌曲后会按专辑聚合在这里")
        }
        return
    }
    items(
        items = likedAlbums.chunked(size = 2),
        key = { rowAlbums: List<Album> -> rowAlbums.joinToString(separator = "|") { album: Album -> album.id } },
        contentType = { "favorites-album-row" },
    ) { rowAlbums: List<Album> ->
        Row(
            modifier = Modifier.padding(horizontal = favoritesHorizontalPadding),
            horizontalArrangement = Arrangement.spacedBy(space = 14.dp),
        ) {
            rowAlbums.forEach { album: Album ->
                AlbumCard(
                    album = album,
                    onOpen = onAlbumOpen,
                    modifier = Modifier.weight(weight = 1f),
                )
            }
            if (rowAlbums.size == 1) {
                Spacer(modifier = Modifier.weight(weight = 1f))
            }
        }
        Spacer(modifier = Modifier.height(height = 20.dp))
    }
}

// 歌手分段保持原有打开歌手详情能力，避免 UI 改造切断收藏资产入口。
internal fun LazyListScope.favoriteArtistItems(
    likedArtists: List<Artist>,
    onArtistOpen: (Artist) -> Unit,
) {
    if (likedArtists.isEmpty()) {
        item(key = "favorites-empty-artists", contentType = "favorites-empty") {
            FavoritesEmptyState(message = "收藏歌曲后会按歌手聚合在这里")
        }
        return
    }
    item(key = "favorites-artist-list", contentType = "favorites-artist-list") {
        Column(modifier = Modifier.padding(horizontal = favoritesHorizontalPadding)) {
            likedArtists.forEach { artist: Artist ->
                ArtistRow(
                    artist = artist,
                    onOpen = onArtistOpen,
                )
            }
        }
    }
}

// 空收藏状态必须明确反馈，避免一级页面出现空白。
@Composable
internal fun FavoritesEmptyState(message: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = favoritesHorizontalPadding)
            .background(
                color = Color.White,
                shape = RoundedCornerShape(size = favoritesSongRowRadius),
            )
            .padding(horizontal = 20.dp, vertical = 22.dp),
        verticalArrangement = Arrangement.spacedBy(space = 6.dp),
    ) {
        Text(
            text = "暂无收藏",
            color = favoritesTextColor,
            fontSize = 16.sp,
            lineHeight = 24.sp,
            fontWeight = FontWeight.Medium,
        )
        Text(
            text = message,
            color = favoritesMetaColor,
            fontSize = 14.sp,
            lineHeight = 20.sp,
        )
    }
}
