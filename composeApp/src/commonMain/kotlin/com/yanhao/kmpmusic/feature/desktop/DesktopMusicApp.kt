package com.yanhao.kmpmusic.feature.desktop

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.yanhao.kmpmusic.core.theme.KmpMusicTheme
import com.yanhao.kmpmusic.feature.app.MusicAppController
import com.yanhao.kmpmusic.feature.app.MusicAppUiState
import com.yanhao.kmpmusic.feature.app.RootTab
import com.yanhao.kmpmusic.feature.app.SecondaryScreen

/**
 * Desktop-only app surface. Mobile Android/iOS continue to call MusicApp.
 */
@Composable
fun DesktopMusicApp(
    controller: MusicAppController,
) {
    val state: MusicAppUiState = controller.uiState
    KmpMusicTheme(themeMode = state.themeMode) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(DesktopMusicColors.WindowBackground),
        ) {
            DesktopTitleBar(onSearch = controller::openSearch)
            Row(modifier = Modifier.weight(1f)) {
                DesktopRail(
                    activeDestination = state.desktopRailDestination(),
                    onRootTab = controller::navigateToRoot,
                    onSettings = { controller.navigateToSecondary(SecondaryScreen.Settings) },
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .background(DesktopMusicColors.Paper),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "Workspace",
                        color = DesktopMusicColors.Muted,
                    )
                }
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(DesktopMusicDimens.PlayerHeight)
                    .background(Color.White.copy(alpha = 0.86f)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "Desktop Player",
                    color = DesktopMusicColors.Muted,
                )
            }
        }
    }
}

private fun MusicAppUiState.desktopRailDestination(): DesktopRailDestination {
    return when (navigationState.secondaryScreen) {
        SecondaryScreen.Settings -> DesktopRailDestination.Settings
        else -> when (navigationState.rootTab) {
            RootTab.Home -> DesktopRailDestination.Home
            RootTab.Favorites -> DesktopRailDestination.Favorites
            RootTab.Me -> DesktopRailDestination.Me
        }
    }
}
