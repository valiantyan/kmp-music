package com.yanhao.kmpmusic.feature.desktop.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * 桌面滚动条视觉层级，数值细节由共享实现统一维护。
 */
internal enum class DesktopLazyScrollbarStyle {
    Standard,
    HighContrast,
}

/**
 * 共享滚动条的内部视觉参数。
 *
 * @property containerWidth 可拖拽区域宽度。
 * @property trackWidth 轨道宽度。
 * @property thumbWidth 滑块宽度。
 * @property trackColor 轨道颜色。
 * @property thumbColor 滑块颜色。
 */
internal data class DesktopLazyScrollbarVisualSpec(
    val containerWidth: Dp,
    val trackWidth: Dp,
    val thumbWidth: Dp,
    val trackColor: Color,
    val thumbColor: Color,
)

// 视觉变体只暴露语义，页面不重复维护颜色与尺寸常量。
internal fun resolveDesktopLazyScrollbarVisualSpec(style: DesktopLazyScrollbarStyle): DesktopLazyScrollbarVisualSpec =
    when (style) {
        DesktopLazyScrollbarStyle.Standard -> {
            DesktopLazyScrollbarVisualSpec(
                containerWidth = 16.dp,
                trackWidth = 4.dp,
                thumbWidth = 4.dp,
                trackColor = Color(0x14006B5C),
                thumbColor = Color(0x99006B5C),
            )
        }

        DesktopLazyScrollbarStyle.HighContrast -> {
            DesktopLazyScrollbarVisualSpec(
                containerWidth = 18.dp,
                trackWidth = 6.dp,
                thumbWidth = 6.dp,
                trackColor = Color(0x26006B5C),
                thumbColor = Color(0xFF006B5C),
            )
        }
    }
