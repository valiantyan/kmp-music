package com.yanhao.kmpmusic.core.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.sp
import com.yanhao.kmpmusic.domain.model.ThemeMode

/**
 * 原型视觉 token，集中管理颜色，避免页面中散落魔法值。
 */
object MusicColors {
    val Accent: Color = Color(0xFF17B59E)
    val AccentSoft: Color = Color(0x1A17B59E)
    val Ink: Color = Color(0xFF050607)
    val Muted: Color = Color(0xFF858B96)
    val MutedLight: Color = Color(0xFFA7ADB5)
    val Line: Color = Color(0xFFE5EAED)
    val Paper: Color = Color(0xFFFBFCFD)
    val Soft: Color = Color(0xFFF1F4F5)
    val SoftAlt: Color = Color(0xFFF7F9FA)
    val PlayingRed: Color = Color(0xFFE84848)
    val Danger: Color = Color(0xFFE55757)
    val DarkPaper: Color = Color(0xFF11191D)
    val DarkSoft: Color = Color(0xFF20282D)
    val DarkMuted: Color = Color(0xFFA9B4BD)
}

/**
 * App 主题入口，沿用原型浅色为默认模式。
 */
@Composable
fun KmpMusicTheme(
    themeMode: ThemeMode,
    content: @Composable () -> Unit,
) {
    val useDarkTheme: Boolean = when (themeMode) {
        ThemeMode.Light -> false
        ThemeMode.Dark -> true
        ThemeMode.System -> isSystemInDarkTheme()
    }
    val colors = if (useDarkTheme) {
        darkColorScheme(
            primary = MusicColors.Accent,
            background = MusicColors.DarkPaper,
            surface = MusicColors.DarkPaper,
            onBackground = Color.White,
            onSurface = Color.White,
        )
    } else {
        lightColorScheme(
            primary = MusicColors.Accent,
            background = MusicColors.Paper,
            surface = MusicColors.Paper,
            onBackground = MusicColors.Ink,
            onSurface = MusicColors.Ink,
        )
    }
    MaterialTheme(
        colorScheme = colors,
        typography = MusicTypography,
        content = content,
    )
}

/**
 * 接近原型中 SF/PingFang 的系统字体层级。
 */
val MusicTypography: Typography = Typography(
    headlineLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 34.sp,
        lineHeight = 38.sp,
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 26.sp,
        lineHeight = 30.sp,
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 21.sp,
        lineHeight = 25.sp,
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 17.sp,
        lineHeight = 22.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
    ),
)
