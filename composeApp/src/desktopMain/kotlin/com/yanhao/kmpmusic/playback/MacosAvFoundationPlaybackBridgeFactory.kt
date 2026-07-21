package com.yanhao.kmpmusic.playback

import com.yanhao.kmpmusic.domain.model.PlaybackError
import com.yanhao.kmpmusic.domain.model.PlaybackErrorType
import kotlinx.coroutines.channels.Channel

/**
 * macOS AVFoundation bridge 创建器，集中处理 dylib 加载和 session 初始化。
 */
internal object MacosAvFoundationPlaybackBridgeFactory {
    /** 创建 bridge，并把初始化失败缓存成可被 Flow 收到的事件。 */
    fun create(
        libraryLoader: MacosAvFoundationNativeLibraryLoader,
        sessionFactory: MacosAvFoundationNativeBridgeSessionFactory,
    ): MacosAvFoundationPlaybackBridge {
        val eventChannel: Channel<ApplePlaybackBridgeEvent> = Channel(capacity = Channel.UNLIMITED)
        var bridge: MacosAvFoundationPlaybackBridge? = null
        val callback =
            MacosAvFoundationPlaybackBridgeCallback(
                eventChannel = eventChannel,
                canEmit = { bridge?.canEmitCallbacks() != false },
            )
        val creation: MacosAvFoundationBridgeCreation =
            createSession(
                libraryLoader = libraryLoader,
                sessionFactory = sessionFactory,
                callback = callback,
                eventChannel = eventChannel,
            )
        bridge =
            MacosAvFoundationPlaybackBridge(
                eventChannel = eventChannel,
                session = creation.session,
                initialization = creation.initialization,
            )
        return bridge
    }

    /** 加载 dylib 并创建 native session。 */
    private fun createSession(
        libraryLoader: MacosAvFoundationNativeLibraryLoader,
        sessionFactory: MacosAvFoundationNativeBridgeSessionFactory,
        callback: MacosAvFoundationNativeBridgeCallback,
        eventChannel: Channel<ApplePlaybackBridgeEvent>,
    ): MacosAvFoundationBridgeCreation =
        when (val loadResult: MacosAvFoundationNativeLibraryLoadResult = libraryLoader.load()) {
            MacosAvFoundationNativeLibraryLoadResult.Loaded -> {
                createLoadedSession(
                    sessionFactory = sessionFactory,
                    callback = callback,
                    eventChannel = eventChannel,
                )
            }

            is MacosAvFoundationNativeLibraryLoadResult.Failed -> {
                failedCreation(
                    reason = "加载失败：${loadResult.reason}",
                    eventChannel = eventChannel,
                )
            }
        }

    /** native library 已加载后创建 session。 */
    private fun createLoadedSession(
        sessionFactory: MacosAvFoundationNativeBridgeSessionFactory,
        callback: MacosAvFoundationNativeBridgeCallback,
        eventChannel: Channel<ApplePlaybackBridgeEvent>,
    ): MacosAvFoundationBridgeCreation =
        when (val creation: MacosAvFoundationNativeBridgeSessionCreation = sessionFactory.create(callback)) {
            is MacosAvFoundationNativeBridgeSessionCreation.Success -> {
                MacosAvFoundationBridgeCreation(
                    session = creation.session,
                    initialization = MacosAvFoundationBridgeInitialization.Success,
                )
            }

            is MacosAvFoundationNativeBridgeSessionCreation.Failed -> {
                failedCreation(
                    reason = "初始化失败：${creation.reason}",
                    eventChannel = eventChannel,
                )
            }
        }

    /** 创建失败结果并缓存初始化失败事件。 */
    private fun failedCreation(
        reason: String,
        eventChannel: Channel<ApplePlaybackBridgeEvent>,
    ): MacosAvFoundationBridgeCreation {
        val error =
            PlaybackError(
                type = PlaybackErrorType.EngineUnavailable,
                songId = null,
                message = "macOS AVFoundation bridge $reason",
            )
        eventChannel.trySend(element = ApplePlaybackBridgeEvent.InitializationFailed(error = error))
        return MacosAvFoundationBridgeCreation(
            session = null,
            initialization = MacosAvFoundationBridgeInitialization.Failed(error = error),
        )
    }
}

private data class MacosAvFoundationBridgeCreation(
    val session: MacosAvFoundationNativeBridgeSession?,
    val initialization: MacosAvFoundationBridgeInitialization,
)
