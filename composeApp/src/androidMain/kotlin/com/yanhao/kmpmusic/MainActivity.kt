package com.yanhao.kmpmusic

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.ViewModelProvider
import com.yanhao.kmpmusic.data.AndroidMediaStoreScanner
import com.yanhao.kmpmusic.feature.app.PermissionSettingsOpener

/**
 * Android 入口 Activity。
 */
class MainActivity : ComponentActivity() {
    // 当前 Activity 生命周期内可用的音频权限请求器。
    private lateinit var audioPermissionRequester: AndroidAudioPermissionRequester

    /** 初始化共享 Compose App，保留 Android 推荐的 edge-to-edge，并把避让交给 Compose inset。 */
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(
                scrim = android.graphics.Color.TRANSPARENT,
                darkScrim = android.graphics.Color.TRANSPARENT,
            ),
            navigationBarStyle = SystemBarStyle.light(
                scrim = android.graphics.Color.TRANSPARENT,
                darkScrim = android.graphics.Color.TRANSPARENT,
            ),
        )
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).apply {
            isAppearanceLightStatusBars = true
            isAppearanceLightNavigationBars = true
        }
        audioPermissionRequester = AndroidAudioPermissionRequester(activity = this)
        val musicAppViewModel: MusicAppViewModel = ViewModelProvider(this)[MusicAppViewModel::class.java]
        musicAppViewModel.attachLocalMusicScanner(
            scanner = AndroidMediaStoreScanner(
                contentResolver = applicationContext.contentResolver,
                requestAudioPermission = audioPermissionRequester::requestAudioPermission,
            ),
        )
        musicAppViewModel.attachPermissionSettingsOpener(
            opener = PermissionSettingsOpener(audioPermissionRequester::openAudioPermissionSettings),
        )
        setContent {
            App(controller = musicAppViewModel.controller)
        }
    }
}
