package com.yanhao.kmpmusic.feature.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yanhao.kmpmusic.core.theme.MusicColors

// 播放入口使用稳定胶囊容器，避免按钮状态变化造成列表跳动。
private val artistDetailPlayAllSurfaceShape: RoundedCornerShape = RoundedCornerShape(size = 28.dp)

// 展开态歌手名属于正文内容组，锚定到头图中下部而不是图片底边。
@Composable
internal fun ArtistDetailExpandedTitle(
    artistName: String,
    scrollState: State<ArtistDetailScrollState>,
) {
    Text(
        text = artistName,
        color = Color.White,
        fontSize = 34.sp,
        lineHeight = 42.sp,
        fontWeight = FontWeight.SemiBold,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
        style = TextStyle(
            shadow = Shadow(
                color = Color.Black.copy(alpha = 0.38f),
                blurRadius = 10f,
            ),
        ),
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                alpha = scrollState.value.expandedContentAlpha
            }
            .padding(start = 20.dp, end = 20.dp, bottom = 22.dp),
    )
}

// 播放入口替代原“热门歌曲”标题，数量来自当前歌手全部歌曲。
@Composable
internal fun ArtistDetailPlayAllSectionHeader(
    text: String,
    countText: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(height = artistDetailSectionTitleScrollHeight)
            .padding(
                start = 20.dp,
                top = 8.dp,
                end = 20.dp,
                bottom = 8.dp,
            ),
        shape = artistDetailPlayAllSurfaceShape,
        color = if (enabled) MusicColors.Paper else MusicColors.Soft,
        shadowElevation = if (enabled) 2.dp else 0.dp,
        enabled = enabled,
        onClick = onClick,
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(size = 40.dp)
                        .clip(shape = CircleShape)
                        .background(
                            color = if (enabled) {
                                artistDetailActionColor
                            } else {
                                artistDetailActionColor.copy(alpha = 0.42f)
                            },
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(size = 22.dp),
                        tint = Color.White,
                    )
                }
                Spacer(modifier = Modifier.width(width = 14.dp))
                Text(
                    text = text,
                    color = artistDetailActionColor.copy(alpha = if (enabled) 1f else 0.42f),
                    fontSize = 18.sp,
                    lineHeight = 24.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(modifier = Modifier.width(width = 8.dp))
                Text(
                    text = countText,
                    color = artistDetailActionColor.copy(alpha = if (enabled) 0.70f else 0.24f),
                    fontSize = 18.sp,
                    lineHeight = 24.sp,
                    fontWeight = FontWeight.Normal,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}
