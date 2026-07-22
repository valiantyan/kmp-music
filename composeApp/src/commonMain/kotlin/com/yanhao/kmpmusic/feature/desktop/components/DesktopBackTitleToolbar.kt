package com.yanhao.kmpmusic.feature.desktop.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.yanhao.kmpmusic.feature.desktop.DesktopNavigationToolbarTokens

/**
 * Desktop 返回式二级页顶栏，统一搜索和管理歌单的返回热区、标题位置与稳定高度。
 */
@Composable
fun DesktopBackTitleToolbar(
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .height(height = DesktopNavigationToolbarTokens.Height)
                .padding(horizontal = DesktopNavigationToolbarTokens.HorizontalPadding),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Surface(
            modifier = Modifier.size(size = 32.dp),
            color = Color.Transparent,
            onClick = onBack,
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = "返回",
                    tint = DesktopNavigationToolbarTokens.Title,
                    modifier = Modifier.size(size = 20.dp),
                )
            }
        }
        Spacer(modifier = Modifier.size(size = 8.dp))
        Text(
            text = title,
            color = DesktopNavigationToolbarTokens.Title,
            fontSize = DesktopNavigationToolbarTokens.TitleSize,
            lineHeight = DesktopNavigationToolbarTokens.TitleLineHeight,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
