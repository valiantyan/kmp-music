package com.yanhao.kmpmusic.feature.desktop.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import kmpmusic.composeapp.generated.resources.Res
import org.jetbrains.compose.resources.ExperimentalResourceApi

/** 以 Figma 横幅尺寸展示个人资料，避免恢复旧登录卡或页面标题。 */
@Composable
internal fun DesktopMeProfileHeader() {
    BoxWithConstraints {
        val density: Density = LocalDensity.current
        val backgroundBrush: Brush =
            with(density) {
                DesktopMeFigmaTokens.profileBackgroundBrush(
                    size = Size(width = maxWidth.toPx(), height = 194.dp.toPx()),
                )
            }
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(194.dp)
                    .shadow(elevation = 1.dp, shape = DesktopMeFigmaTokens.ProfileShape)
                    .clip(DesktopMeFigmaTokens.ProfileShape)
                    .background(Color.White)
                    .background(backgroundBrush)
                    .border(
                        width = 1.dp,
                        color = Color.White.copy(alpha = 0.4f),
                        shape = DesktopMeFigmaTokens.ProfileShape,
                    ),
        ) {
            Box(
                modifier =
                    Modifier
                        .align(Alignment.BottomEnd)
                        .offset(x = 40.dp, y = 40.dp)
                        .size(256.dp)
                        .clip(CircleShape)
                        .background(DesktopMeFigmaTokens.Accent.copy(alpha = 0.05f))
                        .blur(32.dp),
            )
            Row(
                modifier = Modifier.padding(33.dp),
                horizontalArrangement = Arrangement.spacedBy(32.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                DesktopMeProfileAvatar()
                Column(
                    modifier = Modifier.width(448.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = "高保真听众",
                        color = DesktopMeFigmaTokens.Ink,
                        fontSize = 32.sp,
                        lineHeight = 40.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                    )
                    Text(
                        text = "音乐是我的灵魂，在流动的旋律中寻找共鸣。",
                        color = DesktopMeFigmaTokens.Muted,
                        fontSize = 15.sp,
                        lineHeight = 22.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

/** 使用现有本地图像作为不绑定账户资料的头像占位。 */
@Composable
@OptIn(ExperimentalResourceApi::class)
private fun DesktopMeProfileAvatar() {
    Box(modifier = Modifier.size(128.dp), contentAlignment = Alignment.Center) {
        Box(
            modifier =
                Modifier
                    .size(136.dp)
                    .clip(CircleShape)
                    .background(DesktopMeFigmaTokens.AvatarHaloBrush)
                    .blur(6.dp),
        )
        Surface(
            modifier = Modifier.size(128.dp),
            shape = CircleShape,
            color = Color.White,
            border = androidx.compose.foundation.BorderStroke(width = 4.dp, color = Color.White),
        ) {
            AsyncImage(
                model = Res.getUri("drawable/me_profile_avatar.jpg"),
                contentDescription = "个人头像",
                modifier = Modifier.fillMaxSize().clip(CircleShape),
                contentScale = ContentScale.Crop,
            )
        }
    }
}
