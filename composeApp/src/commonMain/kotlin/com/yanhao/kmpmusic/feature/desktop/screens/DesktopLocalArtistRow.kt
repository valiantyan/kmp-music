package com.yanhao.kmpmusic.feature.desktop.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.yanhao.kmpmusic.domain.model.Artist

// 歌手行只负责打开详情页，行高和分隔线按 Figma 列表逐行还原。
@Composable
internal fun DesktopLocalArtistRow(
    artist: Artist,
    index: Int,
    isLastRow: Boolean,
    visualSpec: DesktopLocalArtistListVisualSpec,
    onArtistOpen: (Artist) -> Unit,
) {
    val rowHeight: Dp = if (isLastRow) visualSpec.lastRowHeight else visualSpec.regularRowHeight
    Surface(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(height = rowHeight),
        color = Color.Transparent,
        onClick = { onArtistOpen(artist) },
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(horizontal = visualSpec.rowHorizontalPadding),
                horizontalArrangement = Arrangement.spacedBy(space = visualSpec.contentGap),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                DesktopLocalArtistAvatar(
                    artist = artist,
                    index = index,
                    visualSpec = visualSpec,
                )
                DesktopLocalArtistText(
                    artist = artist,
                    visualSpec = visualSpec,
                    modifier = Modifier.weight(weight = 1f),
                )
                Icon(
                    imageVector = DesktopFigmaChevronRightIcon,
                    contentDescription = "进入 ${artist.name} 歌手页",
                    tint = Color(0xFF3C4A46),
                    modifier =
                        Modifier.size(
                            width = visualSpec.chevronWidth,
                            height = visualSpec.chevronHeight,
                        ),
                )
            }
            if (!isLastRow) {
                HorizontalDivider(
                    modifier = Modifier.align(alignment = Alignment.BottomCenter),
                    thickness = 1.dp,
                    color = Color(0x0DBBCAC4),
                )
            }
        }
    }
}

// 歌手名复用音乐页歌曲标题字号，统计文案继续保持既有 13sp 规格。
@Composable
private fun DesktopLocalArtistText(
    artist: Artist,
    visualSpec: DesktopLocalArtistListVisualSpec,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Text(
            text = artist.name,
            color = Color(0xFF111C2D),
            fontSize = visualSpec.artistNameFontSize,
            lineHeight = visualSpec.artistNameLineHeight,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = formatDesktopLocalArtistSubtitle(artist = artist),
            color = Color(0xFF3C4A46),
            fontSize = visualSpec.artistSubtitleFontSize,
            lineHeight = visualSpec.artistSubtitleLineHeight,
            fontWeight = FontWeight.Normal,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
