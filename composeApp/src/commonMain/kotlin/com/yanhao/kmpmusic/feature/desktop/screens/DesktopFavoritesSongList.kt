package com.yanhao.kmpmusic.feature.desktop.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Figma 表头使用固定动作列与等权标题、艺术家列，窗口缩放时不会横向跳动。 */
@Composable
internal fun DesktopFavoritesTableHeader(modifier: Modifier = Modifier) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .height(33.dp)
                .drawBehind {
                    drawLine(
                        color = Color(0x1ABBCAC4),
                        start = Offset(x = 0f, y = size.height),
                        end = Offset(x = size.width, y = size.height),
                    )
                }.padding(start = 16.dp, end = 64.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        DesktopFavoritesHeaderText(text = "#", modifier = Modifier.width(48.dp), textAlign = TextAlign.Center)
        DesktopFavoritesHeaderText(text = "标题", modifier = Modifier.weight(1f))
        DesktopFavoritesHeaderText(text = "艺术家", modifier = Modifier.weight(1f))
        DesktopFavoritesHeaderText(text = "时长", modifier = Modifier.width(100.dp), textAlign = TextAlign.End)
        Box(modifier = Modifier.width(48.dp))
        Box(modifier = Modifier.width(48.dp))
    }
}

/** 单个表头文字保持 Figma 的 11sp 次级层级。 */
@Composable
private fun DesktopFavoritesHeaderText(
    text: String,
    modifier: Modifier,
    textAlign: TextAlign = TextAlign.Start,
) {
    Text(
        text = text,
        modifier = modifier,
        color = Color(0x993C4A46),
        fontSize = 11.sp,
        lineHeight = 16.sp,
        fontWeight = FontWeight.Medium,
        textAlign = textAlign,
        maxLines = 1,
    )
}

/** 空态放在表头下方，不改变顶部 Figma 页面骨架。 */
@Composable
internal fun DesktopFavoritesEmptyState(message: String) {
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(192.dp)
                .background(Color.Transparent),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = message,
            color = Color(0x993C4A46),
            fontSize = 16.sp,
            lineHeight = 24.sp,
            fontWeight = FontWeight.Medium,
        )
    }
}
