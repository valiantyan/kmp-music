package com.yanhao.kmpmusic.feature.desktop.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.yanhao.kmpmusic.domain.model.CoverArt
import com.yanhao.kmpmusic.domain.model.Song
import com.yanhao.kmpmusic.feature.components.CoverArtImage
import com.yanhao.kmpmusic.feature.desktop.DesktopArtistDetailTokens

/** 歌手 hero 依序尝试歌曲封面，并将 Figma 的固定遮罩、标签和播放动作叠加在图片上。 */
@Composable
internal fun DesktopArtistDetailHero(
    displayModel: DesktopArtistDetailDisplayModel,
    onPlayAll: () -> Unit,
    onShuffle: () -> Unit,
) {
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(DesktopArtistDetailTokens.HeroHeight),
    ) {
        DesktopArtistDetailHeroArtwork(candidates = displayModel.heroArtworkCandidates)
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(color = Color.Black.copy(alpha = 0.1f)),
        )
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(
                        brush =
                            Brush.verticalGradient(
                                colorStops =
                                    arrayOf(
                                        0.6f to Color.Transparent,
                                        1f to DesktopArtistDetailTokens.Background,
                                    ),
                            ),
                    ),
        )
        Column(
            modifier =
                Modifier
                    .align(alignment = Alignment.BottomStart)
                    .padding(all = DesktopArtistDetailTokens.ContentPadding),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                DesktopArtistDetailHeroEyebrow()
                DesktopArtistDetailHeroTitle(title = displayModel.title)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                DesktopArtistDetailActionButton(
                    text = displayModel.playAllLabel,
                    icon = Icons.Rounded.PlayArrow,
                    isPrimary = true,
                    enabled = displayModel.isPlaybackEnabled,
                    onClick = onPlayAll,
                )
                DesktopArtistDetailActionButton(
                    text = "随机播放",
                    icon = Icons.Rounded.Shuffle,
                    isPrimary = false,
                    enabled = displayModel.isPlaybackEnabled,
                    onClick = onShuffle,
                )
            }
        }
    }
}

/** 外部封面 URI 失败后切换到下一首候选；内置封面和全部失败默认图继续复用统一图片组件。 */
@Composable
private fun DesktopArtistDetailHeroArtwork(candidates: List<Song>) {
    var candidateIndex: Int? by remember(candidates) { mutableStateOf<Int?>(value = 0) }
    val candidate: Song? =
        resolveDesktopArtistDetailHeroArtworkCandidate(
            candidates = candidates,
            candidateIndex = candidateIndex,
        )
    val artworkModifier: Modifier = Modifier.fillMaxSize()
    if (candidate == null) {
        CoverArtImage(
            coverArt = CoverArt.HeroLocalMusic,
            contentDescription = "歌手默认封面",
            modifier = artworkModifier,
            contentScale = ContentScale.Crop,
        )
        return
    }
    val coverImageUri: String? = candidate.coverImageUri
    if (coverImageUri.isNullOrBlank()) {
        CoverArtImage(
            coverArt = candidate.coverArt,
            contentDescription = "${candidate.title} 封面",
            modifier = artworkModifier,
            contentScale = ContentScale.Crop,
        )
        return
    }
    AsyncImage(
        model = coverImageUri,
        contentDescription = "${candidate.title} 封面",
        modifier = artworkModifier,
        contentScale = ContentScale.Crop,
        onError = {
            candidateIndex =
                nextDesktopArtistDetailHeroArtworkCandidateIndex(
                    candidates = candidates,
                    candidateIndex = candidateIndex,
                )
        },
    )
}

/** 候选索引耗尽时返回空值，调用方据此回退到歌曲默认图。 */
internal fun resolveDesktopArtistDetailHeroArtworkCandidate(
    candidates: List<Song>,
    candidateIndex: Int?,
): Song? = candidateIndex?.let { index: Int -> candidates.getOrNull(index = index) }

/** 单张外部封面失败时严格按歌曲顺序前进，最后一张失败后停止候选。 */
internal fun nextDesktopArtistDetailHeroArtworkCandidateIndex(
    candidates: List<Song>,
    candidateIndex: Int?,
): Int? {
    val nextIndex: Int = (candidateIndex ?: return null) + 1
    return nextIndex.takeIf { index: Int -> index < candidates.size }
}

/** “艺术家”标签使用半透明玻璃底，保留图片上的层级而不抢占标题焦点。 */
@Composable
private fun DesktopArtistDetailHeroEyebrow() {
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = Color.White.copy(alpha = 0.1f),
    ) {
        Text(
            text = "艺术家",
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp),
            color = Color.White.copy(alpha = 0.8f),
            fontSize = 16.sp,
            lineHeight = 24.sp,
            fontWeight = FontWeight.Medium,
        )
    }
}

/** 标题以容器宽度自适应缩小，优先完整呈现歌手名且不挤压固定高度 hero 的操作区。 */
@Composable
private fun DesktopArtistDetailHeroTitle(title: String) {
    BoxWithConstraints {
        val density: Density = LocalDensity.current
        val textMeasurer = rememberTextMeasurer()
        Text(
            text = title,
            color = Color.White,
            fontSize =
                resolveDesktopArtistDetailTitleSize(
                    title = title,
                    maxWidth = maxWidth,
                    density = density,
                    textMeasurer = textMeasurer,
                ),
            lineHeight = 96.sp,
            fontWeight = FontWeight.Normal,
            style = TextStyle(shadow = Shadow(color = Color.Black.copy(alpha = 0.2f), blurRadius = 4f)),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** 使用真实文字宽度二分搜索标题字号，最长名称先缩小到 40sp，仍放不下时由省略号兜底。 */
private fun resolveDesktopArtistDetailTitleSize(
    title: String,
    maxWidth: Dp,
    density: Density,
    textMeasurer: androidx.compose.ui.text.TextMeasurer,
): TextUnit {
    val availableWidth: Int = with(density) { maxWidth.roundToPx() }
    var smallestFittingSize: Int = DESKTOP_ARTIST_DETAIL_MIN_TITLE_SIZE_SP
    var largestCandidateSize: Int = DESKTOP_ARTIST_DETAIL_MAX_TITLE_SIZE_SP
    while (smallestFittingSize < largestCandidateSize) {
        val candidateSize: Int = (smallestFittingSize + largestCandidateSize + 1) / 2
        if (doesDesktopArtistDetailTitleFit(
                title = title,
                fontSize = candidateSize.sp,
                availableWidth = availableWidth,
                textMeasurer = textMeasurer,
            )
        ) {
            smallestFittingSize = candidateSize
        } else {
            largestCandidateSize = candidateSize - 1
        }
    }
    return smallestFittingSize.sp
}

/** 裁剪模式保留真实溢出信号，避免省略号本身掩盖宽度不足而提前停止缩字。 */
private fun doesDesktopArtistDetailTitleFit(
    title: String,
    fontSize: TextUnit,
    availableWidth: Int,
    textMeasurer: androidx.compose.ui.text.TextMeasurer,
): Boolean =
    !textMeasurer
        .measure(
            text = AnnotatedString(text = title),
            style = TextStyle(fontSize = fontSize, fontWeight = FontWeight.Normal),
            overflow = TextOverflow.Clip,
            maxLines = 1,
            constraints = Constraints(maxWidth = availableWidth),
        ).hasVisualOverflow

private const val DESKTOP_ARTIST_DETAIL_MAX_TITLE_SIZE_SP: Int = 84
private const val DESKTOP_ARTIST_DETAIL_MIN_TITLE_SIZE_SP: Int = 40

/** 两个 hero 动作共用固定高度与胶囊热区，禁用态保留布局但不注册点击事件。 */
@Composable
private fun DesktopArtistDetailActionButton(
    text: String,
    icon: ImageVector,
    isPrimary: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val interactionSource: MutableInteractionSource = remember { MutableInteractionSource() }
    val isHovered: Boolean by interactionSource.collectIsHoveredAsState()
    val isPressed: Boolean by interactionSource.collectIsPressedAsState()
    val content: @Composable () -> Unit = {
        Row(
            modifier = Modifier.padding(horizontal = if (isPrimary) 32.dp else 24.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = resolveDesktopArtistDetailActionContentColor(isPrimary = isPrimary, enabled = enabled),
                modifier = Modifier.size(16.dp),
            )
            Text(
                text = text,
                color = resolveDesktopArtistDetailActionContentColor(isPrimary = isPrimary, enabled = enabled),
                fontSize = 16.sp,
                lineHeight = 24.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
            )
        }
    }
    val modifier: Modifier =
        Modifier
            .height(48.dp)
            .then(
                if (enabled) {
                    Modifier
                        .hoverable(interactionSource = interactionSource)
                        .clickable(
                            interactionSource = interactionSource,
                            indication = null,
                            onClick = onClick,
                        )
                } else {
                    Modifier
                },
            ).then(if (enabled && isPrimary) Modifier.shadow(elevation = 4.dp, shape = CircleShape) else Modifier)
    Surface(
        modifier = modifier,
        shape = CircleShape,
        color =
            resolveDesktopArtistDetailActionBackground(
                isPrimary = isPrimary,
                enabled = enabled,
                isHovered = isHovered,
                isPressed = isPressed,
            ),
        content = content,
    )
}

/** 主动作使用青色，次动作使用 Figma 半透明玻璃底，禁用态仅降低对比度。 */
private fun resolveDesktopArtistDetailActionBackground(
    isPrimary: Boolean,
    enabled: Boolean,
    isHovered: Boolean,
    isPressed: Boolean,
): Color =
    when {
        isPrimary && enabled && isPressed -> DesktopArtistDetailTokens.AccentPressed
        isPrimary && enabled && isHovered -> DesktopArtistDetailTokens.AccentHover
        isPrimary && enabled -> DesktopArtistDetailTokens.Accent
        isPrimary -> DesktopArtistDetailTokens.Accent.copy(alpha = 0.35f)
        enabled && isPressed -> Color.White.copy(alpha = 0.22f)
        enabled && isHovered -> Color.White.copy(alpha = 0.18f)
        enabled -> Color.White.copy(alpha = 0.1f)
        else -> Color.White.copy(alpha = 0.05f)
    }

/** 主动作采用深青文案，次动作在 hero 上始终保持白色文字。 */
private fun resolveDesktopArtistDetailActionContentColor(
    isPrimary: Boolean,
    enabled: Boolean,
): Color {
    if (!enabled) {
        return Color.White.copy(alpha = 0.45f)
    }
    return if (isPrimary) DesktopArtistDetailTokens.AccentContent else Color.White
}
