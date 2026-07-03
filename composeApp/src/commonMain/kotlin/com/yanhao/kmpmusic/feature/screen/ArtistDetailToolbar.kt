package com.yanhao.kmpmusic.feature.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBackIosNew
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// 固定 Toolbar 始终覆盖状态栏区域，背景和标题随折叠进度渐显。
@Composable
internal fun ArtistDetailToolbar(
    artistName: String,
    scrollState: State<ArtistDetailScrollState>,
    collapsedToolbarHeight: Dp,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height = collapsedToolbarHeight),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    alpha = scrollState.value.toolbarAlpha
                }
                .background(color = artistDetailToolbarColor),
        )
        Row(
            modifier = Modifier
                .align(alignment = Alignment.BottomCenter)
                .fillMaxWidth()
                .height(height = artistDetailToolbarContentHeight)
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ArtistDetailToolbarIconButton(
                contentDescription = "返回",
                onClick = onBack,
            ) {
                Icon(
                    imageVector = Icons.Rounded.ArrowBackIosNew,
                    contentDescription = null,
                    tint = artistDetailToolbarContentColor,
                )
            }
            Text(
                text = artistName,
                color = artistDetailToolbarContentColor,
                fontSize = 18.sp,
                lineHeight = 24.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .weight(weight = 1f)
                    .graphicsLayer {
                        alpha = scrollState.value.toolbarTitleAlpha
                    },
            )
        }
    }
}

// 统一顶部图标按钮尺寸，保证展开和折叠状态点击区域不跳动。
@Composable
private fun ArtistDetailToolbarIconButton(
    contentDescription: String,
    onClick: () -> Unit,
    content: @Composable () -> Unit,
) {
    IconButton(
        modifier = Modifier
            .size(size = 48.dp)
            .semantics { this.contentDescription = contentDescription },
        onClick = onClick,
    ) {
        Box(contentAlignment = Alignment.Center) {
            content()
        }
    }
}
