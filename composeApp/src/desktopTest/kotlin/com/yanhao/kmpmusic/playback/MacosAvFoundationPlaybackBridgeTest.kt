package com.yanhao.kmpmusic.playback

import com.yanhao.kmpmusic.domain.model.PlaybackError
import com.yanhao.kmpmusic.domain.model.PlaybackErrorType
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeout
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class MacosAvFoundationPlaybackBridgeTest {
    /** 验证 JVM 侧能把 native bridge 加载失败显式报告给播放引擎。 */
    @Test
    fun loadFailureEmitsInitializationFailedEvent(): Unit =
        runTest {
            val bridge: MacosAvFoundationPlaybackBridge =
                MacosAvFoundationPlaybackBridge.create(
                    libraryLoader =
                        FakeMacosAvFoundationLibraryLoader(
                            result = MacosAvFoundationNativeLibraryLoadResult.Failed(reason = "missing dylib"),
                        ),
                    sessionFactory = FakeMacosAvFoundationSessionFactory(),
                )
            val event: ApplePlaybackBridgeEvent =
                withTimeout(timeMillis = 1_000L) {
                    bridge.events.first()
                }
            assertIs<MacosAvFoundationBridgeInitialization.Failed>(value = bridge.initialization)
            assertEquals(
                expected =
                    ApplePlaybackBridgeEvent.InitializationFailed(
                        error =
                            PlaybackError(
                                type = PlaybackErrorType.EngineUnavailable,
                                songId = null,
                                message = "macOS AVFoundation bridge 加载失败：missing dylib",
                            ),
                    ),
                actual = event,
            )
        }

    /** 验证 bridge 初始化成功可以被 JVM 侧直接观测。 */
    @Test
    fun loadSuccessReportsInitializedBridge(): Unit =
        runTest {
            val bridge: MacosAvFoundationPlaybackBridge =
                MacosAvFoundationPlaybackBridge.create(
                    libraryLoader =
                        FakeMacosAvFoundationLibraryLoader(
                            result = MacosAvFoundationNativeLibraryLoadResult.Loaded,
                        ),
                    sessionFactory = FakeMacosAvFoundationSessionFactory(),
                )
            assertIs<MacosAvFoundationBridgeInitialization.Success>(value = bridge.initialization)
            assertTrue(actual = bridge.isInitialized)
            bridge.release()
        }

    /** 验证 native prepared、playing、progress、ended 和 failed 都能回流成契约事件。 */
    @Test
    fun nativeCallbacksFlowBackAsAppleBridgeEvents(): Unit =
        runTest {
            val sessionFactory = FakeMacosAvFoundationSessionFactory()
            val bridge: MacosAvFoundationPlaybackBridge =
                MacosAvFoundationPlaybackBridge.create(
                    libraryLoader =
                        FakeMacosAvFoundationLibraryLoader(
                            result = MacosAvFoundationNativeLibraryLoadResult.Loaded,
                        ),
                    sessionFactory = sessionFactory,
                )
            val events: MutableList<ApplePlaybackBridgeEvent> = mutableListOf()
            val collectJob =
                launch {
                    bridge.events.toList(destination = events)
                }
            val ack: ApplePlaybackBridgeCommandAck =
                bridge.prepare(
                    request =
                        ApplePlaybackBridgePrepareRequest(
                            songId = "song-1",
                            mediaUri = "file:///Users/test/Music/one.m4a",
                            generation = 1L,
                            startPositionMs = 0L,
                        ),
                )
            assertEquals(expected = ApplePlaybackBridgeCommandAck.Accepted, actual = ack)
            sessionFactory.session.emitPrepared(generation = 1L, durationMs = 2_000L)
            sessionFactory.session.emitPlaying(generation = 1L, positionMs = 100L, durationMs = 2_000L)
            sessionFactory.session.emitProgress(generation = 1L, positionMs = 1_000L, durationMs = 2_000L)
            sessionFactory.session.emitEnded(generation = 1L)
            sessionFactory.session.emitFailed(
                generation = 2L,
                errorType = PlaybackErrorType.MissingFile,
                songId = "song-missing",
                message = "文件不存在",
            )
            runCurrent()
            assertEquals(
                expected =
                    listOf(
                        ApplePlaybackBridgeEvent.Prepared(generation = 1L, durationMs = 2_000L),
                        ApplePlaybackBridgeEvent.Playing(generation = 1L, positionMs = 100L, durationMs = 2_000L),
                        ApplePlaybackBridgeEvent.Progress(generation = 1L, positionMs = 1_000L, durationMs = 2_000L),
                        ApplePlaybackBridgeEvent.Ended(generation = 1L),
                        ApplePlaybackBridgeEvent.Failed(
                            generation = 2L,
                            error =
                                PlaybackError(
                                    type = PlaybackErrorType.MissingFile,
                                    songId = "song-missing",
                                    message = "文件不存在",
                                ),
                        ),
                    ),
                actual = events,
            )
            bridge.release()
            collectJob.cancel()
        }

    /** 验证 release 后 native 延迟回调会被丢弃，且 release ack 不会挂起。 */
    @Test
    fun releaseDropsDelayedNativeCallbacks(): Unit =
        runTest {
            val sessionFactory = FakeMacosAvFoundationSessionFactory()
            val bridge: MacosAvFoundationPlaybackBridge =
                MacosAvFoundationPlaybackBridge.create(
                    libraryLoader =
                        FakeMacosAvFoundationLibraryLoader(
                            result = MacosAvFoundationNativeLibraryLoadResult.Loaded,
                        ),
                    sessionFactory = sessionFactory,
                )
            val events: MutableList<ApplePlaybackBridgeEvent> = mutableListOf()
            val collectJob =
                launch {
                    bridge.events.toList(destination = events)
                }
            bridge.prepare(
                request =
                    ApplePlaybackBridgePrepareRequest(
                        songId = "song-1",
                        mediaUri = "file:///Users/test/Music/one.m4a",
                        generation = 1L,
                        startPositionMs = 0L,
                    ),
            )
            val releaseAck: ApplePlaybackBridgeCommandAck = bridge.release()
            sessionFactory.session.emitFailed(
                generation = 1L,
                errorType = PlaybackErrorType.Unknown,
                songId = "song-1",
                message = "release 后的延迟 native failure",
            )
            advanceUntilIdle()
            assertEquals(expected = ApplePlaybackBridgeCommandAck.Accepted, actual = releaseAck)
            assertTrue(actual = sessionFactory.session.isReleased)
            assertFalse(actual = events.any { event: ApplePlaybackBridgeEvent -> event is ApplePlaybackBridgeEvent.Failed })
            collectJob.cancel()
        }

    /** 验证当前平台具备真实 JNI bridge 加载能力；非 macOS 或未配置产物时不伪造通过。 */
    @Test
    fun configuredNativeLibraryLoadsOnMacos(): Unit =
        runTest {
            if (!MacosAvFoundationNativeLibraryLoader.isMacos()) {
                return@runTest
            }
            if (System.getProperty(MACOS_AVFOUNDATION_BRIDGE_PATH_PROPERTY).isNullOrBlank()) {
                return@runTest
            }
            val bridge: MacosAvFoundationPlaybackBridge = MacosAvFoundationPlaybackBridge.create()
            assertIs<MacosAvFoundationBridgeInitialization.Success>(value = bridge.initialization)
            assertTrue(actual = bridge.isInitialized)
            bridge.release()
        }
}
