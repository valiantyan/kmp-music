package com.yanhao.kmpmusic.feature.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import coil3.compose.AsyncImage
import com.yanhao.kmpmusic.domain.model.CoverArt
import kmpmusic.composeapp.generated.resources.Res
import org.jetbrains.compose.resources.ExperimentalResourceApi

/**
 * 将 domain 层封面标识映射到 Compose resource URI。
 */
@Composable
@OptIn(ExperimentalResourceApi::class)
internal fun coverArtResourceUri(coverArt: CoverArt): String {
    return Res.getUri(coverArtResourcePath(coverArt = coverArt))
}

/**
 * Coil 封面图组件。扫描封面优先，加载失败时回退到应用内资源。
 */
@Composable
@OptIn(ExperimentalResourceApi::class)
fun CoverArtImage(
    coverArt: CoverArt,
    coverImageUri: String?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
) {
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
    var activeModel: String by remember(coverArt, coverImageUri) {
        mutableStateOf(primaryModel)
    }
    LaunchedEffect(primaryModel) {
        activeModel = primaryModel
    }
    AsyncImage(
        model = activeModel,
        contentDescription = contentDescription,
        modifier = modifier,
        contentScale = contentScale,
        onError = {
            if (activeModel != fallbackModel) {
                activeModel = fallbackModel
            }
        },
    )
}

/**
 * 只使用应用内兜底封面时的便捷重载。
 */
@Composable
@OptIn(ExperimentalResourceApi::class)
fun CoverArtImage(
    coverArt: CoverArt,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
) {
    CoverArtImage(
        coverArt = coverArt,
        coverImageUri = null,
        contentDescription = contentDescription,
        modifier = modifier,
        contentScale = contentScale,
    )
}
