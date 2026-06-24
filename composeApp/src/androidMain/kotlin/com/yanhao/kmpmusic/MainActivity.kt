package com.yanhao.kmpmusic

import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
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

    // Android 13+ 的通知权限请求器；播放服务仍保持惰性启动。
    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) {}

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
        requestPlaybackNotificationPermissionIfNeeded()
        musicAppViewModel.attachPlaybackContext(context = applicationContext)
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

    /** 仅在 Android 13 及以上请求通知权限，避免播放通知被系统静默拦截。 */
    private fun requestPlaybackNotificationPermissionIfNeeded() {
        if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                this,
                android.Manifest.permission.POST_NOTIFICATIONS,
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}
