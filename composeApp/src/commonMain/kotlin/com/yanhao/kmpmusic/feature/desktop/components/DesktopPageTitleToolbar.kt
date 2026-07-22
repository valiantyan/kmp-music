package com.yanhao.kmpmusic.feature.desktop.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Figma `1085:710` 公共标题 Toolbar 的视觉规格。
 *
 * @property textColor 标题颜色。
 * @property fontSize 标题字号。
 * @property lineHeight 标题行高。
 * @property fontWeight 标题字重。
 * @property bottomPadding Toolbar 与下方内容的距离。
 */
internal data class DesktopPageTitleToolbarVisualSpec(
    val textColor: Color,
    val fontSize: TextUnit,
    val lineHeight: TextUnit,
    val fontWeight: FontWeight,
    val bottomPadding: Dp,
)

/**
 * 桌面二级页公共标题 Toolbar，固定在页面滚动内容之外。
 */
@Composable
fun DesktopPageTitleToolbar(
    title: String,
    modifier: Modifier = Modifier,
) {
    val visualSpec: DesktopPageTitleToolbarVisualSpec = resolveDesktopPageTitleToolbarVisualSpec()
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(bottom = visualSpec.bottomPadding),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            color = visualSpec.textColor,
            fontSize = visualSpec.fontSize,
            lineHeight = visualSpec.lineHeight,
            fontWeight = visualSpec.fontWeight,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/**
 * 集中返回公共标题规格，避免各页面再次复制 Figma 数值。
 */
internal fun resolveDesktopPageTitleToolbarVisualSpec(): DesktopPageTitleToolbarVisualSpec =
    DesktopPageTitleToolbarVisualSpec(
        textColor = Color(0xFF111C2D),
        fontSize = 32.sp,
        lineHeight = 40.sp,
        fontWeight = FontWeight.Medium,
        bottomPadding = 32.dp,
    )
