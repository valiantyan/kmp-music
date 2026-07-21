package com.yanhao.kmpmusic.feature.desktop.navigation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import kmpmusic.composeapp.generated.resources.Res
import org.jetbrains.compose.resources.ExperimentalResourceApi

// 账户区当前只按设计稿静态展示，后续设置入口再由“我的”页承接。
@Composable
internal fun DesktopRailAccount() {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp)
                .padding(top = 16.dp)
                .padding(all = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        DesktopRailAccountAvatar()
        Column(modifier = Modifier.width(128.dp)) {
            Text(
                text = "用户账户",
                color = Color(0xFF111C2D),
                fontSize = 15.sp,
                lineHeight = 22.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = "Premium 会员",
                color = Color(0xFF3C4A46),
                fontSize = 11.sp,
                lineHeight = 16.5.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

// 复用项目已有 Figma 头像资源，避免为了静态账户区新增资产。
@Composable
@OptIn(ExperimentalResourceApi::class)
private fun DesktopRailAccountAvatar() {
    Surface(
        modifier = Modifier.size(40.dp),
        shape = CircleShape,
        color = Color.White,
        border = BorderStroke(width = 2.dp, color = Color.White),
        shadowElevation = 1.dp,
    ) {
        AsyncImage(
            model = Res.getUri("drawable/me_profile_avatar.jpg"),
            contentDescription = "用户头像",
            modifier =
                Modifier
                    .fillMaxSize()
                    .clip(CircleShape),
            contentScale = ContentScale.Crop,
        )
    }
}
