package com.yanhao.kmpmusic.feature.desktop

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.yanhao.kmpmusic.core.theme.KmpMusicTheme
import com.yanhao.kmpmusic.feature.app.MusicAppController

/**
 * Desktop-only app surface. Mobile Android/iOS continue to call MusicApp.
 */
@Composable
fun DesktopMusicApp(
    controller: MusicAppController,
) {
    KmpMusicTheme(themeMode = controller.uiState.themeMode) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(DesktopMusicColors.WindowBackground),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "KMP Music Desktop",
                color = MaterialTheme.colorScheme.onBackground,
            )
        }
    }
}
