package com.yanhao.kmpmusic.feature.desktop.screens

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/**
 * 桌面新版 Figma 资源里的音乐符号，供首页歌曲卡和歌手占位头像复用。
 */
internal val DesktopFigmaMusicNoteIcon: ImageVector =
    ImageVector
        .Builder(
            name = "DesktopFigmaMusicNoteIcon",
            defaultWidth = 15.dp,
            defaultHeight = 22.5.dp,
            viewportWidth = 15f,
            viewportHeight = 22.5f,
        ).apply {
            path(fill = SolidColor(Color(0xFF006B5C))) {
                moveTo(5f, 22.5f)
                curveTo(3.625f, 22.5f, 2.44792f, 22.0104f, 1.46875f, 21.0312f)
                curveTo(0.489583f, 20.0521f, 0f, 18.875f, 0f, 17.5f)
                curveTo(0f, 16.125f, 0.489583f, 14.9479f, 1.46875f, 13.9688f)
                curveTo(2.44792f, 12.9896f, 3.625f, 12.5f, 5f, 12.5f)
                curveTo(5.47917f, 12.5f, 5.92188f, 12.5573f, 6.32812f, 12.6719f)
                curveTo(6.73438f, 12.7865f, 7.125f, 12.9583f, 7.5f, 13.1875f)
                verticalLineTo(0f)
                horizontalLineTo(15f)
                verticalLineTo(5f)
                horizontalLineTo(10f)
                verticalLineTo(17.5f)
                curveTo(10f, 18.875f, 9.51042f, 20.0521f, 8.53125f, 21.0312f)
                curveTo(7.55208f, 22.0104f, 6.375f, 22.5f, 5f, 22.5f)
                close()
            }
        }.build()

/**
 * Figma `1085:709` 歌手行末尾箭头，尺寸来自导出的 `7.4 x 12` SVG。
 */
internal val DesktopFigmaChevronRightIcon: ImageVector =
    ImageVector
        .Builder(
            name = "DesktopFigmaChevronRightIcon",
            defaultWidth = 7.4.dp,
            defaultHeight = 12.dp,
            viewportWidth = 7.4f,
            viewportHeight = 12f,
        ).apply {
            path(fill = SolidColor(Color(0xFF3C4A46))) {
                moveTo(4.6f, 6f)
                lineTo(0f, 1.4f)
                lineTo(1.4f, 0f)
                lineTo(7.4f, 6f)
                lineTo(1.4f, 12f)
                lineTo(0f, 10.6f)
                lineTo(4.6f, 6f)
                close()
            }
        }.build()
