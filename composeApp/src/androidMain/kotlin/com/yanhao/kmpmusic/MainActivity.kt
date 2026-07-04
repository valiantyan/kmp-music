package com.yanhao.kmpmusic

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.ViewModelProvider
import com.yanhao.kmpmusic.core.theme.MusicColors
import com.yanhao.kmpmusic.data.AndroidMediaStoreScanner
import com.yanhao.kmpmusic.domain.model.Song
import com.yanhao.kmpmusic.feature.app.MusicAppController
import com.yanhao.kmpmusic.feature.app.MusicAppUiState
import com.yanhao.kmpmusic.feature.app.PermissionSettingsOpener
import com.yanhao.kmpmusic.feature.app.SecondaryScreen
import com.yanhao.kmpmusic.feature.components.rememberPlayerPagePalette

// Android 系统导航栏跟随播放页背景的动画时长，与播放页自身背景过渡保持一致。
private const val ANDROID_NAVIGATION_BAR_COLOR_ANIMATION_MILLIS = 260

/**
 * Android 入口 Activity。
 */
class MainActivity : ComponentActivity() {
    // 当前 Activity 持有的进程级 ViewModel，供通知热启动 intent 复用共享控制器。
    private lateinit var musicAppViewModel: MusicAppViewModel

    // 当前 Activity 生命周期内可用的音频权限请求器。
    private lateinit var audioPermissionRequester: AndroidAudioPermissionRequester

    // debuggable 性能入口状态，供 adb 显式 intent 切换到专辑详情滑动监控页面。
    private var isAlbumDetailPerformanceHarnessOpen: Boolean by mutableStateOf(value = false)

    // debuggable 收藏页性能入口状态，供 adb 显式 intent 切换到 500 条收藏列表。
    private var isFavoritesPerformanceHarnessOpen: Boolean by mutableStateOf(value = false)

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
        configureEdgeToEdgeSystemBars()
        audioPermissionRequester = AndroidAudioPermissionRequester(activity = this)
        musicAppViewModel = ViewModelProvider(this)[MusicAppViewModel::class.java]
        requestPlaybackNotificationPermissionIfNeeded()
        musicAppViewModel.attachPlaybackContext(context = applicationContext)
        musicAppViewModel.attachLocalMusicScanner(
            scanner = AndroidMediaStoreScanner(
                context = applicationContext,
                requestAudioPermission = audioPermissionRequester::requestAudioPermission,
            ),
        )
        musicAppViewModel.attachPermissionSettingsOpener(
            opener = PermissionSettingsOpener(audioPermissionRequester::openAudioPermissionSettings),
        )
        isAlbumDetailPerformanceHarnessOpen = shouldOpenAlbumDetailPerformanceHarness(intent = intent)
        isFavoritesPerformanceHarnessOpen = shouldOpenFavoritesPerformanceHarness(intent = intent)
        handlePlaybackIntent(intent = intent)
        setContent {
            when {
                isAlbumDetailPerformanceHarnessOpen -> AlbumDetailPerformanceHarness()
                isFavoritesPerformanceHarnessOpen -> FavoritesPerformanceHarness()
                else -> {
                    AndroidNavigationBarColorEffect(controller = musicAppViewModel.controller)
                    App(controller = musicAppViewModel.controller)
                }
            }
        }
    }

    /** 处理通知正文在已有任务栈上的点击，避免重复创建 Activity 后丢失播放页意图。 */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        isAlbumDetailPerformanceHarnessOpen = shouldOpenAlbumDetailPerformanceHarness(intent = intent)
        isFavoritesPerformanceHarnessOpen = shouldOpenFavoritesPerformanceHarness(intent = intent)
        handlePlaybackIntent(intent = intent)
    }

    /** debuggable 构建中渲染固定数据专辑详情页，方便 adb 采集滑动帧统计。 */
    @Composable
    private fun AlbumDetailPerformanceHarness() {
        AndroidAlbumDetailPerformanceHarness(onBack = { finish() })
    }

    /** debuggable 构建中渲染 500 条收藏列表，方便 adb 采集滑动帧统计。 */
    @Composable
    private fun FavoritesPerformanceHarness() {
        AndroidFavoritesPerformanceHarness()
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

    /** 保持 edge-to-edge 基础设置，具体导航栏底色由 Android 页面状态副作用负责。 */
    @Suppress("DEPRECATION")
    private fun configureEdgeToEdgeSystemBars() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
        }
        WindowInsetsControllerCompat(window, window.decorView).apply {
            isAppearanceLightStatusBars = true
            isAppearanceLightNavigationBars = true
        }
    }

    /** 性能入口只接受 debuggable 构建的显式 adb intent，避免影响正式用户路径。 */
    private fun shouldOpenAlbumDetailPerformanceHarness(intent: Intent?): Boolean {
        if (intent?.action != ACTION_OPEN_ALBUM_DETAIL_PERFORMANCE) {
            return false
        }
        return isAppDebuggable()
    }

    /** 收藏页性能入口只允许 debuggable 显式 adb intent，避免普通启动进入测试列表。 */
    private fun shouldOpenFavoritesPerformanceHarness(intent: Intent?): Boolean {
        if (intent?.action != ACTION_OPEN_FAVORITES_PERFORMANCE) {
            return false
        }
        return isAppDebuggable()
    }

    /** 通过 manifest flags 判断当前安装包是否可调试，避免依赖 generated BuildConfig。 */
    private fun isAppDebuggable(): Boolean {
        return (applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
    }

    /** Android 三键导航栏不属于 Compose 根视图，播放页需要在 Activity 层同步底色。 */
    @Composable
    private fun AndroidNavigationBarColorEffect(controller: MusicAppController) {
        val state: MusicAppUiState = controller.uiState
        val song: Song? = state.currentSong
        val playerNavigationBarColor: Color = if (
            state.navigationState.secondaryScreen == SecondaryScreen.Player &&
            song != null
        ) {
            rememberPlayerPagePalette(
                coverArt = song.coverArt,
                coverImageUri = song.coverImageUri,
            ).backgroundColor
        } else {
            MusicColors.Paper
        }
        val navigationBarColor: Color by animateColorAsState(
            targetValue = playerNavigationBarColor,
            animationSpec = tween(durationMillis = ANDROID_NAVIGATION_BAR_COLOR_ANIMATION_MILLIS),
            label = "AndroidNavigationBarColor",
        )
        LaunchedEffect(navigationBarColor) {
            applyAndroidNavigationBarColor(color = navigationBarColor)
        }
    }

    /** 将当前页面底色写入 Android 系统导航栏，避免播放页底部出现独立白条。 */
    @Suppress("DEPRECATION")
    private fun applyAndroidNavigationBarColor(color: Color) {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.navigationBarColor = color.toArgb()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
        }
        WindowInsetsControllerCompat(window, window.decorView).isAppearanceLightNavigationBars = true
    }

    /** 把 Android 通知入口 action 翻译成共享控制器导航，页面决策不散落到 service。 */
    private fun handlePlaybackIntent(intent: Intent?) {
        if (intent?.action != ACTION_OPEN_PLAYER) {
            return
        }
        musicAppViewModel.controller.openPlayer()
    }

    companion object {
        /**
         * 媒体通知正文点击动作，产品语义固定为打开当前播放页。
         */
        const val ACTION_OPEN_PLAYER: String = "com.yanhao.kmpmusic.action.OPEN_PLAYER"

        /**
         * debuggable 滑动性能监控入口，用于 adb 显式打开 501 行专辑详情页。
         */
        const val ACTION_OPEN_ALBUM_DETAIL_PERFORMANCE: String =
            "com.yanhao.kmpmusic.action.OPEN_ALBUM_DETAIL_PERFORMANCE"

        /**
         * debuggable 收藏页滑动性能监控入口，用于 adb 显式打开 500 条收藏列表。
         */
        const val ACTION_OPEN_FAVORITES_PERFORMANCE: String =
            "com.yanhao.kmpmusic.action.OPEN_FAVORITES_PERFORMANCE"

        /**
         * 创建媒体通知正文点击入口，复用现有任务栈并把意图交给 [MainActivity] 处理。
         */
        fun createOpenPlayerIntent(context: Context): Intent {
            return Intent(context, MainActivity::class.java)
                .setAction(ACTION_OPEN_PLAYER)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
    }
}
