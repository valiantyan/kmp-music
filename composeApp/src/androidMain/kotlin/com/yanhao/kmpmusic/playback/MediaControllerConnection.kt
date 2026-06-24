package com.yanhao.kmpmusic.playback

import android.content.ComponentName
import android.content.Context
import androidx.core.content.ContextCompat
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine

/**
 * 官方 [MediaController] 连接管理器，统一处理 SessionToken 绑定和断线重连。
 */
@UnstableApi
internal class MediaControllerConnection(
    private val onConnected: (MediaController) -> Unit,
    private val onDisconnected: (MediaController) -> Unit,
    private val onConnectionFailed: () -> Unit,
) {
    // applicationContext 仅在 Android runtime 注入后可用。
    private var appContext: Context? = null

    // 官方 Media3 controller 异步连接结果，避免重复绑定 service。
    private var controllerFuture: ListenableFuture<MediaController>? = null

    /** 已解析的 Media3 controller，所有播放命令都通过它进入 session。 */
    var currentController: MediaController? = null
        private set

    // 监听 controller 断开，避免后续命令继续使用失效对象。
    private val controllerListener: MediaController.Listener = object : MediaController.Listener {
        /** controller 断开时清空缓存，让下一次命令重新绑定 service。 */
        override fun onDisconnected(controller: MediaController) {
            handleControllerDisconnected(controller = controller)
        }
    }

    /**
     * 注入 Android applicationContext，供 [MediaController] 绑定 [MusicPlaybackService]。
     */
    fun attachContext(context: Context) {
        appContext = context.applicationContext
    }

    /**
     * 使用当前 controller 执行命令；尚未连接时先异步连接再补发。
     */
    fun execute(command: (MediaController) -> Unit): Boolean {
        val context: Context = appContext ?: return false
        currentController?.let { controller: MediaController ->
            command(controller)
            return true
        }
        val future: ListenableFuture<MediaController> = ensureControllerFuture(context = context)
        future.addListener(
            {
                resolveControllerFuture(
                    future = future,
                    onSuccess = command,
                )
            },
            ContextCompat.getMainExecutor(context),
        )
        return true
    }

    /**
     * 挂起等待 controller 连接，供首播队列下发避开绑定时序竞争。
     */
    suspend fun awaitController(): MediaController? {
        currentController?.let { controller: MediaController ->
            return controller
        }
        val context: Context = appContext ?: return null
        val future: ListenableFuture<MediaController> = ensureControllerFuture(context = context)
        return suspendCancellableCoroutine { continuation ->
            future.addListener(
                {
                    resolveControllerFuture(
                        future = future,
                        onSuccess = { controller: MediaController ->
                            continuation.resume(controller)
                        },
                        onFailure = {
                            continuation.resume(null)
                        },
                    )
                },
                ContextCompat.getMainExecutor(context),
            )
        }
    }

    // 创建或复用官方 controller future。
    private fun ensureControllerFuture(context: Context): ListenableFuture<MediaController> {
        controllerFuture?.let { future: ListenableFuture<MediaController> ->
            return future
        }
        val sessionToken = SessionToken(
            context,
            ComponentName(context, MusicPlaybackService::class.java),
        )
        val future: ListenableFuture<MediaController> = MediaController.Builder(
            context,
            sessionToken,
        )
            .setListener(controllerListener)
            .buildAsync()
        controllerFuture = future
        future.addListener(
            {
                resolveControllerFuture(
                    future = future,
                    onSuccess = {},
                )
            },
            ContextCompat.getMainExecutor(context),
        )
        return future
    }

    // 解析 controller future，并把成功连接的 controller 纳入统一观察。
    private fun resolveControllerFuture(
        future: ListenableFuture<MediaController>,
        onSuccess: (MediaController) -> Unit,
        onFailure: () -> Unit = onConnectionFailed,
    ) {
        runCatching { future.get() }
            .onSuccess { controller: MediaController ->
                attachController(controller = controller)
                onSuccess(controller)
            }
            .onFailure {
                controllerFuture = null
                onFailure()
            }
    }

    // 缓存 controller，并通知外层注册 Player 监听和同步模式。
    private fun attachController(controller: MediaController) {
        if (currentController === controller) {
            return
        }
        currentController = controller
        onConnected(controller)
    }

    // controller 断开后释放本地缓存，下一次命令会重新走 SessionToken 连接。
    private fun handleControllerDisconnected(controller: MediaController) {
        if (currentController !== controller) {
            return
        }
        currentController = null
        controllerFuture = null
        onDisconnected(controller)
    }
}
