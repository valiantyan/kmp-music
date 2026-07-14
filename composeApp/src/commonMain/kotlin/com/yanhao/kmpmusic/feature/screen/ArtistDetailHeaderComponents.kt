package com.yanhao.kmpmusic.feature.screen

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

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
