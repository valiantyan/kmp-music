package com.yanhao.kmpmusic.feature.app.playerbar

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yanhao.kmpmusic.core.theme.MusicColors
import com.yanhao.kmpmusic.core.theme.MusicDimens
import com.yanhao.kmpmusic.core.theme.scaledDp
import com.yanhao.kmpmusic.core.theme.scaledSp
import com.yanhao.kmpmusic.feature.app.RootTab

/**
 * 只在首页、收藏、我的三个一级页面显示的手机端底部导航。
 */
@Composable
fun MobileBottomNavigation(
    rootTab: RootTab,
    isEnabled: Boolean,
    contentHeight: Dp,
    bottomInsetHeight: Dp = 0.dp,
    onRootTab: (RootTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    val defaultContentHeight: Dp = scaledDp(value = MusicDimens.BottomNavHeight)
    val isCompactContent: Boolean = contentHeight < defaultContentHeight
    val itemHeight: Dp = if (isCompactContent) scaledDp(value = 50.dp) else scaledDp(value = 58.dp)
    val iconSize: Dp = if (isCompactContent) scaledDp(value = 26.dp) else scaledDp(value = 28.dp)
    val labelSize: TextUnit = if (isCompactContent) scaledSp(value = 11.sp) else scaledSp(value = 12.sp)
    val topPadding: Dp = if (isCompactContent) scaledDp(value = 4.dp) else scaledDp(value = 10.dp)
    val bottomPadding: Dp = if (isCompactContent) scaledDp(value = 2.dp) else scaledDp(value = 8.dp)
    val rowAlignment: Alignment = if (isCompactContent) Alignment.Center else Alignment.TopCenter
    Surface(
        modifier =
            modifier
                .fillMaxWidth()
                .height(height = contentHeight + bottomInsetHeight)
                .border(width = 1.dp, color = MusicColors.Line.copy(alpha = 0.86f)),
        color = MusicColors.Paper,
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier =
                    Modifier
                        .align(alignment = rowAlignment)
                        .fillMaxWidth()
                        .height(height = contentHeight)
                        .padding(
                            start = scaledDp(24.dp),
                            top = topPadding,
                            end = scaledDp(24.dp),
                            bottom = bottomPadding,
                        ),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RootTab.entries.forEach { tab: RootTab ->
                    MobileBottomNavigationItem(
                        tab = tab,
                        isSelected = rootTab == tab,
                        isEnabled = isEnabled,
                        onClick = { onRootTab(tab) },
                        itemHeight = itemHeight,
                        iconSize = iconSize,
                        labelSize = labelSize,
                    )
                }
            }
        }
    }
}

/**
 * 自定义底部导航项，只保留原型需要的图标和文字状态。
 */
@Composable
private fun MobileBottomNavigationItem(
    tab: RootTab,
    isSelected: Boolean,
    isEnabled: Boolean,
    onClick: () -> Unit,
    itemHeight: Dp,
    iconSize: Dp,
    labelSize: TextUnit,
) {
    val itemColor: Color = if (isSelected) MusicColors.Accent else Color(0xFF7D838D)
    Column(
        modifier =
            Modifier
                .size(width = scaledDp(74.dp), height = itemHeight)
                .clickable(enabled = isEnabled, onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(space = scaledDp(3.dp), alignment = Alignment.CenterVertically),
    ) {
        Icon(
            imageVector =
                when (tab) {
                    RootTab.Home -> Icons.Rounded.Home
                    RootTab.Favorites -> Icons.Rounded.Favorite
                    RootTab.Me -> Icons.Rounded.Person
                },
            contentDescription = tab.mobileLabel(),
            tint = itemColor,
            modifier = Modifier.size(size = iconSize),
        )
        Text(
            text = tab.mobileLabel(),
            color = itemColor,
            fontSize = labelSize,
            fontWeight = FontWeight.Bold,
        )
    }
}

/**
 * 根 Tab 中文名。
 */
private fun RootTab.mobileLabel(): String =
    when (this) {
        RootTab.Home -> "首页"
        RootTab.Favorites -> "收藏"
        RootTab.Me -> "我的"
    }
