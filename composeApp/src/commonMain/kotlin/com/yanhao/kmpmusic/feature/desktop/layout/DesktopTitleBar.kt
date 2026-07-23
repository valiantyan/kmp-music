package com.yanhao.kmpmusic.feature.desktop.layout

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.yanhao.kmpmusic.feature.desktop.DesktopMusicDimens
import com.yanhao.kmpmusic.feature.desktop.DesktopMusicType
import com.yanhao.kmpmusic.feature.desktop.DesktopNativeTitleBarTokens

/**
 * 桌面标题栏保留原生窗口按钮和拖拽区背景，并显示非交互的应用品牌。
 */
@Composable
fun DesktopTitleBar(
    modifier: Modifier = Modifier,
    showTitleBarBrand: Boolean = false,
    titleBarDragArea: @Composable () -> Unit = {},
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .height(DesktopMusicDimens.TitleBarHeight)
                .border(width = 1.dp, color = Color(0xB8C7CFD6)),
    ) {
        Box(
            modifier =
                Modifier
                    .width(DesktopMusicDimens.RailWidth)
                    .fillMaxHeight()
                    .background(Color(0xFFF0F3FF)),
            contentAlignment = Alignment.CenterStart,
        ) {
            if (showTitleBarBrand) {
                Text(
                    text = "KMP-MUSIC",
                    modifier =
                        Modifier
                            .offset(y = DesktopNativeTitleBarTokens.BrandVerticalAlignmentOffset)
                            .padding(
                                start = DesktopNativeTitleBarTokens.BrandStart,
                                end = DesktopNativeTitleBarTokens.BrandEnd,
                            ),
                    color = Color(0xFF006B5C),
                    fontFamily = FontFamily.SansSerif,
                    // AppKit 的 [NSFont.titleBarFont] 默认字体在当前 macOS 为 13pt 粗体系统字体。
                    fontSize = DesktopMusicType.AppTitle,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Clip,
                )
            }
        }
        Box(
            modifier =
                Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .background(Color(0xFFF9F9FF)),
        ) {
            titleBarDragArea()
        }
    }
}
