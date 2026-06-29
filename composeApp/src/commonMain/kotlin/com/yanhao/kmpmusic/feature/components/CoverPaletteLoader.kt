package com.yanhao.kmpmusic.feature.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.ImageBitmap
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.compose.LocalPlatformContext
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import com.yanhao.kmpmusic.core.theme.MiniPlayerPalette
import com.yanhao.kmpmusic.core.theme.MusicColors
import com.yanhao.kmpmusic.core.theme.PlayerPagePalette
import com.yanhao.kmpmusic.core.theme.extractMiniPlayerPalette
import com.yanhao.kmpmusic.core.theme.extractPlayerPagePalette
import com.yanhao.kmpmusic.domain.model.CoverArt
import kmpmusic.composeapp.generated.resources.Res
import org.jetbrains.compose.resources.ExperimentalResourceApi

/**
 * 迷你播放器 palette 默认值，图片加载或取色失败时使用。
 */
fun defaultMiniPlayerPalette(): MiniPlayerPalette {
    return MiniPlayerPalette(
        containerColor = MusicColors.Paper.copy(alpha = 0.92f),
    )
}

/**
 * 桌面播放页 palette 默认值，图片加载或取色失败时使用。
 */
fun defaultPlayerPagePalette(): PlayerPagePalette {
    return PlayerPagePalette(
        backgroundColor = MusicColors.Paper,
        ambientColor = MusicColors.Accent.copy(alpha = 0.18f),
    )
}

/**
 * 使用与封面显示相同的 Coil 来源顺序提取迷你播放器配色。
 */
@Composable
fun rememberMiniPlayerPalette(
    coverArt: CoverArt,
    coverImageUri: String?,
): MiniPlayerPalette {
    return rememberCoverPalette(
        coverArt = coverArt,
        coverImageUri = coverImageUri,
        defaultPalette = defaultMiniPlayerPalette(),
        extractPalette = ::extractMiniPlayerPalette,
    )
}

/**
 * 使用与封面显示相同的 Coil 来源顺序提取桌面播放页配色。
 */
@Composable
fun rememberPlayerPagePalette(
    coverArt: CoverArt,
    coverImageUri: String?,
): PlayerPagePalette {
    return rememberCoverPalette(
        coverArt = coverArt,
        coverImageUri = coverImageUri,
        defaultPalette = defaultPlayerPagePalette(),
        extractPalette = ::extractPlayerPagePalette,
    )
}

/**
 * 统一托管封面 palette 的异步加载状态，保证页面层不直接接触 Coil API。
 */
@Composable
@OptIn(ExperimentalResourceApi::class)
private fun <T> rememberCoverPalette(
    coverArt: CoverArt,
    coverImageUri: String?,
    defaultPalette: T,
    extractPalette: (ImageBitmap) -> T,
): T {
    val platformContext: PlatformContext = LocalPlatformContext.current
    val request: CoverArtImageRequest = remember(coverArt, coverImageUri) {
        buildCoverArtImageRequest(
            coverArt = coverArt,
            coverImageUri = coverImageUri,
        )
    }
    val fallbackModel: String = Res.getUri(request.fallbackResourcePath)
    val primaryModel: String = if (request.usesExternalCover) {
        request.primaryModel
    } else {
        fallbackModel
    }
    var palette: T by remember(coverArt, coverImageUri) {
        mutableStateOf(value = defaultPalette)
    }
    LaunchedEffect(primaryModel, fallbackModel, platformContext) {
        palette = loadCoverPalette(
            primaryModel = primaryModel,
            fallbackModel = fallbackModel,
            platformContext = platformContext,
            defaultPalette = defaultPalette,
            extractPalette = extractPalette,
        )
    }
    return palette
}

/**
 * 严格按显示链路尝试外部封面与资源兜底，两者都失败后才回退默认 palette。
 */
private suspend fun <T> loadCoverPalette(
    primaryModel: String,
    fallbackModel: String,
    platformContext: PlatformContext,
    defaultPalette: T,
    extractPalette: (ImageBitmap) -> T,
): T {
    val primaryPalette: T? = loadPaletteFromModel(
        model = primaryModel,
        platformContext = platformContext,
        extractPalette = extractPalette,
    )
    if (primaryPalette != null) {
        return primaryPalette
    }
    if (primaryModel == fallbackModel) {
        return defaultPalette
    }
    return loadPaletteFromModel(
        model = fallbackModel,
        platformContext = platformContext,
        extractPalette = extractPalette,
    ) ?: defaultPalette
}

/**
 * 只负责把单个模型加载为位图并交给共享调色算法，失败时返回空值让上层继续回退。
 */
private suspend fun <T> loadPaletteFromModel(
    model: String,
    platformContext: PlatformContext,
    extractPalette: (ImageBitmap) -> T,
): T? {
    val request: ImageRequest = ImageRequest.Builder(platformContext)
        .data(model)
        .build()
    val result = SingletonImageLoader.get(platformContext).execute(request)
    if (result !is SuccessResult) {
        return null
    }
    val imageBitmap: ImageBitmap = coilImageToImageBitmap(image = result.image) ?: return null
    return extractPalette(imageBitmap)
}
