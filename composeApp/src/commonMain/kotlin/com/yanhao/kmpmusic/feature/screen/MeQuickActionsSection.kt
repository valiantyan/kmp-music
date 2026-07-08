package com.yanhao.kmpmusic.feature.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * 快速功能区仅承载扫描入口，按钮尺寸、圆角和配色按 Figma 复刻。
 */
@Composable
internal fun MeQuickActionsSection(
    onScanMusic: () -> Unit,
) {
    androidx.compose.foundation.layout.Column(
        verticalArrangement = Arrangement.spacedBy(space = meSectionTitleGap),
    ) {
        MeSectionTitle(title = "快速功能")
        Surface(
            shape = RoundedCornerShape(size = meQuickActionRadius),
            color = meQuickActionColor,
            onClick = onScanMusic,
        ) {
            Row(
                modifier = Modifier.padding(
                    start = 16.dp,
                    top = 16.dp,
                    end = 31.dp,
                    bottom = 16.dp,
                ),
                horizontalArrangement = Arrangement.spacedBy(space = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(
                    modifier = Modifier.size(size = meQuickActionIconSize),
                    shape = RoundedCornerShape(size = meQuickActionIconRadius),
                    color = meAccentColor,
                ) {
                    androidx.compose.foundation.layout.Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Rounded.Search,
                            contentDescription = null,
                            modifier = Modifier.size(size = 18.dp),
                            tint = meBackgroundColor,
                        )
                    }
                }
                Text(
                    text = "扫描音乐",
                    color = meActionTextColor,
                    fontSize = 16.sp,
                    lineHeight = 24.sp,
                    fontWeight = FontWeight.Medium,
                )
            }
        }
    }
}
