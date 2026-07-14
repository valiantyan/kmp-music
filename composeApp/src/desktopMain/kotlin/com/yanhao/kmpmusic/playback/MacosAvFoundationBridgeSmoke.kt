package com.yanhao.kmpmusic.playback

import java.io.ByteArrayInputStream
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.Collections
import javax.sound.sampled.AudioFileFormat
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioInputStream
import javax.sound.sampled.AudioSystem
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout

/**
 * macOS AVFoundation bridge 真实播放 smoke，供 Gradle 门禁在本机生成 M4A 并验证事件回流。
 */
object MacosAvFoundationBridgeSmoke {
    /** 运行真实 bridge smoke。 */
    @JvmStatic
    fun main(args: Array<String>): Unit = runBlocking {
        val workDir: Path = smokeWorkDir(args = args)
        val mediaPath: Path = prepareSmokeM4a(workDir = workDir)
        val bridge: MacosAvFoundationPlaybackBridge = MacosAvFoundationPlaybackBridge.create()
        check(bridge.isInitialized) {
            "macOS AVFoundation bridge 初始化失败：${bridge.initialization}"
        }
        val events: MutableList<ApplePlaybackBridgeEvent> = Collections.synchronizedList(mutableListOf())
        val collectJob = launch {
            bridge.events.collect { event: ApplePlaybackBridgeEvent ->
                events += event
                println("smoke-event: ${event.smokeName()}")
            }
        }
        try {
            runSuccessfulPlaybackSmoke(bridge = bridge, events = events, mediaPath = mediaPath)
            runFailureSmoke(bridge = bridge, events = events, workDir = workDir)
            println("macOS AVFoundation bridge smoke 通过：${mediaPath.toUri()}")
        } finally {
            bridge.release()
            collectJob.cancel()
        }
    }

    /** 执行真实 M4A 播放，等待 prepared、playing、progress 和 ended。 */
    private suspend fun runSuccessfulPlaybackSmoke(
        bridge: ApplePlaybackBridge,
        events: MutableList<ApplePlaybackBridgeEvent>,
        mediaPath: Path,
    ) {
        val prepareAck: ApplePlaybackBridgeCommandAck = bridge.prepare(
            request = ApplePlaybackBridgePrepareRequest(
                songId = "macos-avfoundation-smoke",
                mediaUri = mediaPath.toUri().toString(),
                generation = 1L,
                startPositionMs = 0L,
            ),
        )
        check(prepareAck == ApplePlaybackBridgeCommandAck.Accepted) {
            "prepare ack 失败：$prepareAck"
        }
        val playAck: ApplePlaybackBridgeCommandAck = bridge.play(generation = 1L)
        check(playAck == ApplePlaybackBridgeCommandAck.Accepted) {
            "play ack 失败：$playAck"
        }
        waitForEvent(label = "prepared", events = events) { event: ApplePlaybackBridgeEvent ->
            event is ApplePlaybackBridgeEvent.Prepared && event.generation == 1L
        }
        waitForEvent(label = "playing", events = events) { event: ApplePlaybackBridgeEvent ->
            event is ApplePlaybackBridgeEvent.Playing && event.generation == 1L
        }
        waitForEvent(label = "progress", events = events) { event: ApplePlaybackBridgeEvent ->
            event is ApplePlaybackBridgeEvent.Progress && event.generation == 1L
        }
        waitForEvent(label = "ended", events = events) { event: ApplePlaybackBridgeEvent ->
            event is ApplePlaybackBridgeEvent.Ended && event.generation == 1L
        }
    }

    /** 执行缺文件失败路径，证明 failed 事件能回流。 */
    private suspend fun runFailureSmoke(
        bridge: ApplePlaybackBridge,
        events: MutableList<ApplePlaybackBridgeEvent>,
        workDir: Path,
    ) {
        bridge.prepare(
            request = ApplePlaybackBridgePrepareRequest(
                songId = "macos-avfoundation-missing",
                mediaUri = workDir.resolve("missing.m4a").toUri().toString(),
                generation = 2L,
                startPositionMs = 0L,
            ),
        )
        waitForEvent(label = "failed", events = events) { event: ApplePlaybackBridgeEvent ->
            event is ApplePlaybackBridgeEvent.Failed && event.generation == 2L
        }
    }

    /** 等待指定事件出现，超时即让 smoke 失败。 */
    private suspend fun waitForEvent(
        label: String,
        events: MutableList<ApplePlaybackBridgeEvent>,
        predicate: (ApplePlaybackBridgeEvent) -> Boolean,
    ) {
        withTimeout(timeMillis = 10_000L) {
            while (!events.any(predicate = predicate)) {
                delay(timeMillis = 50L)
            }
        }
        println("smoke-check: $label")
    }

    /** 解析 smoke 工作目录，命令行参数优先于系统属性。 */
    private fun smokeWorkDir(args: Array<String>): Path {
        val configured: String? = args.firstOrNull()
            ?: System.getProperty(MACOS_AVFOUNDATION_BRIDGE_SMOKE_DIR_PROPERTY)
        return Paths.get(configured ?: "build/macos-avfoundation-bridge/smoke").toAbsolutePath()
    }

    /** 生成短 M4A 样本，避免依赖用户本机媒体文件。 */
    private fun prepareSmokeM4a(workDir: Path): Path {
        Files.createDirectories(workDir)
        val wavPath: Path = workDir.resolve("macos-avfoundation-smoke.wav")
        val m4aPath: Path = workDir.resolve("macos-avfoundation-smoke.m4a")
        createSmokeWav(wavPath = wavPath)
        convertWavToM4a(wavPath = wavPath, m4aPath = m4aPath)
        return m4aPath
    }

    /** 写入 1.5 秒正弦波 WAV，作为 afconvert 的源文件。 */
    private fun createSmokeWav(wavPath: Path) {
        val sampleRate = 44_100f
        val frames: Int = (sampleRate * 1.5f).toInt()
        val bytes = ByteArray(size = frames * 2)
        for (index: Int in 0 until frames) {
            val value: Short = (Short.MAX_VALUE * 0.25 * kotlin.math.sin(2.0 * Math.PI * 440.0 * index / sampleRate)).toInt().toShort()
            bytes[index * 2] = (value.toInt() and 0xff).toByte()
            bytes[index * 2 + 1] = ((value.toInt() ushr 8) and 0xff).toByte()
        }
        val format = AudioFormat(sampleRate, 16, 1, true, false)
        AudioInputStream(ByteArrayInputStream(bytes), format, frames.toLong()).use { stream: AudioInputStream ->
            AudioSystem.write(stream, AudioFileFormat.Type.WAVE, wavPath.toFile())
        }
    }

    /** 调用 macOS afconvert 生成 M4A/AAC 样本。 */
    private fun convertWavToM4a(wavPath: Path, m4aPath: Path) {
        try {
            val process: Process = ProcessBuilder(
                "/usr/bin/afconvert",
                "-f",
                "m4af",
                "-d",
                "aac",
                wavPath.toString(),
                m4aPath.toString(),
            ).redirectErrorStream(true).start()
            val output: String = process.inputStream.bufferedReader().readText()
            val exitCode: Int = process.waitFor()
            check(exitCode == 0) {
                "afconvert 生成 M4A 失败：$output"
            }
        } catch (error: IOException) {
            throw IllegalStateException("无法启动 /usr/bin/afconvert，不能执行真实 M4A smoke", error)
        }
    }

    /** 输出可读事件名，便于 ticket 记录 smoke 证据。 */
    private fun ApplePlaybackBridgeEvent.smokeName(): String {
        return when (this) {
            is ApplePlaybackBridgeEvent.Prepared -> "prepared(generation=$generation,durationMs=$durationMs)"
            is ApplePlaybackBridgeEvent.Buffering -> "buffering(generation=$generation,positionMs=$positionMs)"
            is ApplePlaybackBridgeEvent.Playing -> "playing(generation=$generation,positionMs=$positionMs)"
            is ApplePlaybackBridgeEvent.Paused -> "paused(generation=$generation,positionMs=$positionMs)"
            is ApplePlaybackBridgeEvent.Progress -> "progress(generation=$generation,positionMs=$positionMs)"
            is ApplePlaybackBridgeEvent.Ended -> "ended(generation=$generation)"
            is ApplePlaybackBridgeEvent.Failed -> "failed(generation=$generation,type=${error.type})"
            is ApplePlaybackBridgeEvent.InitializationFailed -> "initializationFailed(type=${error.type})"
        }
    }
}
