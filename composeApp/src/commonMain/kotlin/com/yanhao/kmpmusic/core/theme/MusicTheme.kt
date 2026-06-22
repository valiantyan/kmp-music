package com.yanhao.kmpmusic.core.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.TextUnit
import com.yanhao.kmpmusic.domain.model.ThemeMode

/**
 * 原型视觉 token，集中管理颜色，避免页面中散落魔法值。
 */
object MusicColors {
    val Accent: Color = Color(0xFF17B59E)
    val AccentDeep: Color = Color(0xFF13A890)
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
 * 原型尺寸 token，保证 Compose 页面与 HTML 原型共享同一套视觉节奏。
 */
object MusicDimens {
    val AppMaxWidth: Dp = 430.dp
    val PagePaddingHorizontal: Dp = 21.dp
    val PagePaddingTop: Dp = 42.dp
    val HeaderActionSize: Dp = 52.dp
    val BackButtonSize: Dp = 42.dp
    val LibraryCardHeight: Dp = 206.dp
    val LibraryCardRadius: Dp = 22.dp
    val HeroFolderSize: Dp = 126.dp
    val MiniPlayerHeight: Dp = 68.dp
    val BottomNavHeight: Dp = 78.dp
    val TopLevelContentBottom: Dp = 210.dp
    val SecondaryContentBottom: Dp = 96.dp
    val FullscreenContentBottom: Dp = 28.dp
    val SongCoverSize: Dp = 62.dp
    val DenseSongCoverSize: Dp = 52.dp
    val AlbumRadius: Dp = 14.dp
    val AlbumTextTopGap: Dp = 8.dp
    val AlbumTextLineGap: Dp = 3.dp
}

/**
 * 移动端 Compose 字体渲染比 CSS 更高，需要统一收敛视觉字号。
 */
private const val VISUAL_FONT_SCALE = 0.86f

/**
 * 当前页面相对 430px 原型宽度的视觉缩放比例。
 */
val LocalMusicScale: ProvidableCompositionLocal<Float> = staticCompositionLocalOf { 1f }

/**
 * 按当前视觉比例缩放 [Dp]，用于把原型 px 节奏映射到真实手机 dp。
 */
@Composable
fun scaledDp(value: Dp): Dp {
    return value * LocalMusicScale.current
}

/**
 * 按当前视觉比例缩放字号，避免 480dpi 手机上文本过大。
 */
@Composable
fun scaledSp(value: TextUnit): TextUnit {
    return value * LocalMusicScale.current * VISUAL_FONT_SCALE
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
