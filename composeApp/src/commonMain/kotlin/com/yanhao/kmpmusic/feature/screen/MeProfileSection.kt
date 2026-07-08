package com.yanhao.kmpmusic.feature.screen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import kmpmusic.composeapp.generated.resources.Res
import org.jetbrains.compose.resources.ExperimentalResourceApi

/**
 * 资料头按 Figma 居中展示头像、昵称和签名，避免沿用旧横排资料卡。
 */
@Composable
internal fun MeProfileSection() {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        MeProfileAvatar()
        Column(
            modifier = Modifier.padding(top = meProfileTextTopPadding),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "高保真听众",
                color = meTextColor,
                fontSize = 24.sp,
                lineHeight = 32.sp,
                fontWeight = FontWeight.Medium,
            )
            Text(
                text = "音乐是我的灵魂",
                color = meProfileMetaColor,
                fontSize = 16.sp,
                lineHeight = 24.sp,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}

// 头像编辑能力尚未实现，徽标只表达 Figma 静态视觉，不暴露交互入口。
@Composable
@OptIn(ExperimentalResourceApi::class)
private fun MeProfileAvatar() {
    Box(modifier = Modifier.size(size = meAvatarFrameSize)) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(color = meAvatarFrameColor, shape = CircleShape)
                .border(
                    width = 2.dp,
                    color = meAccentColor,
                    shape = CircleShape,
                ),
        ) {
            AsyncImage(
                model = Res.getUri("drawable/me_profile_avatar.jpg"),
                contentDescription = "个人头像",
                modifier = Modifier
                    .fillMaxSize()
                    .padding(all = meAvatarFramePadding)
                    .clip(shape = CircleShape),
                contentScale = ContentScale.Crop,
            )
        }
        Surface(
            modifier = Modifier
                .align(alignment = Alignment.BottomEnd)
                .offset(x = -meAvatarBadgeOffset, y = -meAvatarBadgeOffset)
                .size(size = meAvatarBadgeSize),
            shape = CircleShape,
            color = meAccentDarkColor,
            border = BorderStroke(width = 4.dp, color = meAvatarBadgeBorderColor),
            shadowElevation = 2.dp,
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Rounded.Edit,
                    contentDescription = null,
                    modifier = Modifier.size(size = 12.dp),
                    tint = meBackgroundColor,
                )
            }
        }
    }
}
