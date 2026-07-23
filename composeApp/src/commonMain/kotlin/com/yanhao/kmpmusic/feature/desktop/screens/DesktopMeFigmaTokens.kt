package com.yanhao.kmpmusic.feature.desktop.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/** Figma `1120:2245` 我的页内容区专用色彩和固定尺寸，避免覆盖其他桌面页面。 */
internal object DesktopMeFigmaTokens {
    /** 节点未显式填色时的白色内容底。 */
    val Background: Color = Color.White

    /** 资料横幅和交互文字使用的青绿色。 */
    val Accent: Color = Color(0xFF006B5C)

    /** Figma 正文的深墨色。 */
    val Ink: Color = Color(0xFF111C2D)

    /** Figma 辅助文本的绿灰色。 */
    val Muted: Color = Color(0xFF3C4A46)

    /** 资料横幅在 Figma 渐变起点使用的低透明度青绿色。 */
    private val ProfileBackgroundStart: Color = Color(0x1A006B5C)

    /** 资料横幅在 Figma 渐变终点使用的浅蓝色。 */
    private val ProfileBackgroundEnd: Color = Color(0xFFDEE8FF)

    /** 头像光晕使用的第二个青绿色，避免误用资料横幅的浅蓝终点。 */
    private val AvatarHaloEnd: Color = Color(0xFF00BFA5)

    /** Figma 资料横幅从上方略偏左向下方略偏右延伸的 CSS 渐变角度。 */
    private const val PROFILE_BACKGROUND_GRADIENT_ANGLE_DEGREES: Double = 168.935

    /** 头像光晕沿 Figma 45 度渐变并保持 30% 不透明度。 */
    val AvatarHaloBrush: Brush =
        Brush.linearGradient(
            colors = listOf(Accent.copy(alpha = 0.3f), AvatarHaloEnd.copy(alpha = 0.3f)),
        )

    /** 按 Figma CSS 角度计算从横幅中心延伸至渐变端点的一半向量。 */
    private fun profileBackgroundGradientHalfVector(size: Size): Offset {
        val angleRadians: Double = PROFILE_BACKGROUND_GRADIENT_ANGLE_DEGREES * PI / 180.0
        val directionX: Float = sin(x = angleRadians).toFloat()
        val directionY: Float = -cos(x = angleRadians).toFloat()
        val projectionLength: Float = size.width * directionX + size.height * directionY
        return Offset(
            x = directionX * projectionLength / 2f,
            y = directionY * projectionLength / 2f,
        )
    }

    /**
     * 按 Figma CSS 角度计算资料横幅居中渐变线的起点，避免从左上角错误起算。
     *
     * @param size 资料横幅实际绘制尺寸，单位为像素。
     * @return 覆盖横幅四角的渐变起点坐标。
     */
    fun profileBackgroundGradientStart(size: Size): Offset {
        val halfVector: Offset = profileBackgroundGradientHalfVector(size = size)
        return Offset(
            x = size.width / 2f - halfVector.x,
            y = size.height / 2f - halfVector.y,
        )
    }

    /**
     * 按 Figma CSS 角度计算资料横幅居中渐变线的终点，确保不同窗口宽度不会退化为横向渐变。
     *
     * @param size 资料横幅实际绘制尺寸，单位为像素。
     * @return 覆盖横幅四角的渐变终点坐标。
     */
    fun profileBackgroundGradientEnd(size: Size): Offset {
        val halfVector: Offset = profileBackgroundGradientHalfVector(size = size)
        return Offset(
            x = size.width / 2f + halfVector.x,
            y = size.height / 2f + halfVector.y,
        )
    }

    /**
     * 以横幅当前尺寸生成精确背景，避免固定像素坐标在窗口尺寸变化时失真。
     *
     * @param size 资料横幅实际绘制尺寸，单位为像素。
     * @return 可直接绘制的 Figma 资料横幅渐变。
     */
    fun profileBackgroundBrush(size: Size): Brush =
        Brush.linearGradient(
            colors = listOf(ProfileBackgroundStart, ProfileBackgroundEnd),
            start = profileBackgroundGradientStart(size = size),
            end = profileBackgroundGradientEnd(size = size),
        )

    /** 扫描入口的淡青绿底色。 */
    val ScanCardBackground: Color = Color(0x0D006B5C)

    /** 扫描入口的细描边。 */
    val ScanCardBorder: BorderStroke = BorderStroke(width = 1.dp, color = Color(0x33006B5C))

    /** 扫描图标的圆形底。 */
    val ScanIconBackground: Color = Color(0x1A006B5C)

    /** 资料横幅保持节点的 32dp 圆角。 */
    val ProfileShape: Shape = RoundedCornerShape(32.dp)

    /** 扫描入口保持节点的 24dp 圆角。 */
    val ScanCardShape: Shape = RoundedCornerShape(24.dp)

    /** 所有圆形图标背景共用的形状。 */
    val IconCircleShape: Shape = CircleShape
}
