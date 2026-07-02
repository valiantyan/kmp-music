package com.yanhao.kmpmusic.feature.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBackIosNew
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yanhao.kmpmusic.domain.model.Artist
import com.yanhao.kmpmusic.feature.components.CoverArtImage

// 顶部只保留返回动作，状态栏避让留在内容层以保证背景整屏铺满。
@Composable
internal fun ArtistDetailTopBar(onBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(height = 64.dp)
            .statusBarsPadding()
            .padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(
            modifier = Modifier.size(size = 40.dp),
            onClick = onBack,
        ) {
            Icon(
                imageVector = Icons.Rounded.ArrowBackIosNew,
                contentDescription = "返回",
                tint = artistDetailTextColor,
            )
        }
    }
}

// 歌手头部移除统计文案，只展示头像和歌手名。
@Composable
internal fun ArtistDetailHeader(artist: Artist) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CoverArtImage(
            coverArt = artist.coverArt,
            coverImageUri = artist.coverImageUri,
            contentDescription = "${artist.name} 歌手头像",
            modifier = Modifier
                .size(size = 192.dp)
                .clip(shape = CircleShape),
            contentScale = ContentScale.Crop,
        )
        Spacer(modifier = Modifier.height(height = 26.dp))
        Text(
            text = artist.name,
            color = artistDetailTextColor,
            fontSize = 28.sp,
            lineHeight = 36.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(modifier = Modifier.height(height = 30.dp))
    }
}

// 主按钮保留 Figma 的数量文案，数量来自当前歌手全部歌曲。
@Composable
internal fun ArtistDetailPlayAllButton(
    text: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(height = 56.dp)
            .padding(horizontal = 20.dp),
        shape = RoundedCornerShape(size = 12.dp),
        color = if (enabled) artistDetailActionColor else artistDetailActionColor.copy(alpha = 0.42f),
        shadowElevation = if (enabled) 6.dp else 0.dp,
        enabled = enabled,
        onClick = onClick,
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Rounded.PlayArrow,
                contentDescription = null,
                modifier = Modifier.size(size = 20.dp),
                tint = Color.White,
            )
            Spacer(modifier = Modifier.width(width = 8.dp))
            Text(
                text = text,
                color = Color.White,
                fontSize = 18.sp,
                lineHeight = 28.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

// 标题沿用 Figma “热门歌曲”文案，数据规则由 [ArtistDetailContent] 保证为全量歌曲。
@Composable
internal fun ArtistDetailSectionTitle() {
    Text(
        text = "热门歌曲",
        color = artistDetailTextColor,
        fontSize = 24.sp,
        lineHeight = 32.sp,
        fontWeight = FontWeight.Medium,
        modifier = Modifier.padding(
            start = 20.dp,
            top = 32.dp,
            end = 20.dp,
            bottom = 16.dp,
        ),
    )
}
