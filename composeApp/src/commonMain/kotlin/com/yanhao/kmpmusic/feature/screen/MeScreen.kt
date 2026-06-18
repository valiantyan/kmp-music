package com.yanhao.kmpmusic.feature.screen

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yanhao.kmpmusic.core.theme.MusicColors
import com.yanhao.kmpmusic.domain.model.Album
import com.yanhao.kmpmusic.domain.model.Artist
import com.yanhao.kmpmusic.domain.model.CoverArt
import com.yanhao.kmpmusic.feature.components.AppHeader
import com.yanhao.kmpmusic.feature.components.ArtistRow
import com.yanhao.kmpmusic.feature.components.PrimaryPill
import com.yanhao.kmpmusic.feature.components.SectionTitle
import com.yanhao.kmpmusic.feature.components.coverArtPainter

/**
 * 我的页，提供登录、收藏资产和设置入口。
 */
@Composable
fun MeScreen(
    albums: List<Album>,
    artists: List<Artist>,
    onSettings: () -> Unit,
    onLogin: () -> Unit,
    onAlbumOpen: (Album) -> Unit,
    onArtistOpen: (Artist) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
        AppHeader(title = "我的", subtitle = "本地资料与同步状态", onSettings = onSettings)
        Surface(shape = RoundedCornerShape(20.dp), color = MusicColors.Paper, tonalElevation = 1.dp) {
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Image(
                    painter = coverArtPainter(CoverArt.AlbumTimeForest),
                    contentDescription = "账号头像视觉",
                    modifier = Modifier.size(70.dp).clip(CircleShape),
                    contentScale = ContentScale.Crop,
                )
                Text(text = "登录音乐账号", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
                Text(text = "使用 Supabase 同步收藏、播放记录和多端资料。", color = MusicColors.Muted)
                PrimaryPill(text = "立即登录", onClick = onLogin, modifier = Modifier.fillMaxWidth())
            }
        }
        MetricRow()
        Surface(shape = RoundedCornerShape(20.dp), color = MusicColors.Paper, tonalElevation = 1.dp) {
            Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                SectionTitle(title = "我的收藏", actionLabel = "查看", onAction = { albums.firstOrNull()?.let(onAlbumOpen) })
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    albums.forEach { album ->
                        Column(
                            modifier = Modifier.weight(weight = 1f).clickable { onAlbumOpen(album) },
                            verticalArrangement = Arrangement.spacedBy(7.dp),
                        ) {
                            Image(
                                painter = coverArtPainter(album.coverArt),
                                contentDescription = "${album.title} 封面",
                                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(11.dp)),
                                contentScale = ContentScale.Crop,
                            )
                            Text(text = album.title, color = MusicColors.Muted, fontSize = 12.sp, maxLines = 1)
                        }
                    }
                }
            }
        }
        Surface(shape = RoundedCornerShape(20.dp), color = MusicColors.Paper, tonalElevation = 1.dp) {
            Column(modifier = Modifier.padding(18.dp)) {
                SectionTitle(title = "常听歌手", actionLabel = "更多", onAction = { artists.firstOrNull()?.let(onArtistOpen) })
                artists.take(3).forEach { artist -> ArtistRow(artist = artist, onOpen = onArtistOpen) }
            }
        }
    }
}

/**
 * 我的页统计指标。
 */
@Composable
private fun MetricRow() {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        listOf("本地专辑" to "86", "歌手" to "128", "收藏" to "42").forEach { item ->
            Surface(
                modifier = Modifier.weight(weight = 1f),
                shape = RoundedCornerShape(18.dp),
                color = MusicColors.AccentSoft,
            ) {
                Column(modifier = Modifier.padding(vertical = 14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(text = item.second, modifier = Modifier.padding(horizontal = 8.dp), fontSize = 23.sp, fontWeight = FontWeight.ExtraBold)
                    Text(text = item.first, modifier = Modifier.padding(horizontal = 8.dp), color = MusicColors.Muted, fontSize = 13.sp)
                }
            }
        }
    }
}
