package com.yanhao.kmpmusic.feature.screen

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// 区块标题沿用 Figma 的小号大写节奏，中文标题仅保留字距和颜色层级。
@Composable
internal fun MeSectionTitle(
    title: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = title,
        modifier = modifier,
        color = meMetaColor,
        fontSize = 14.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.7.sp,
        fontWeight = FontWeight.Medium,
    )
}
