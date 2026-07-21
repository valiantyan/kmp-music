package com.yanhao.kmpmusic.playback

import com.yanhao.kmpmusic.data.AppleAudioFormatSupport
import com.yanhao.kmpmusic.data.AppleAudioFormatSupportMatrix
import java.io.ByteArrayInputStream
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import javax.sound.sampled.AudioFileFormat
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioInputStream
import javax.sound.sampled.AudioSystem

/**
 * Apple 格式矩阵 smoke，用真实样本和 AVFoundation 可播放性检查固化格式结论。
 */
internal object MacosAvFoundationFormatMatrixSmoke {
    /** 生成样本并检查矩阵中所有已支持格式，待验证格式只输出边界。 */
    fun run(workDir: Path) {
        val samplesByFormat: Map<String, List<Path>> = prepareFormatMatrixSamples(workDir = workDir)
        AppleAudioFormatSupportMatrix.entries.forEach { support: AppleAudioFormatSupport ->
            if (!support.allowsScanning) {
                println("format-matrix: ${support.formatName}=待验证，证据：${support.evidence}")
                return@forEach
            }
            samplesByFormat.getValue(key = support.formatName).forEach { samplePath: Path ->
                checkAvFoundationPlayable(
                    formatName = support.formatName,
                    mediaPath = samplePath,
                    moduleCacheDir = workDir.resolve("swift-module-cache"),
                )
            }
        }
    }

    /** 生成格式矩阵 smoke 所需的短音频样本。 */
    private fun prepareFormatMatrixSamples(workDir: Path): Map<String, List<Path>> {
        Files.createDirectories(workDir)
        val sourceWavPath: Path = workDir.resolve("apple-format-matrix-source.wav")
        createSmokeWav(wavPath = sourceWavPath)
        val mp3Path: Path =
            convertWavToMp3(
                wavPath = sourceWavPath,
                outputPath = workDir.resolve("apple-format-matrix.mp3"),
            )
        val m4aPath: Path =
            convertWav(
                wavPath = sourceWavPath,
                outputPath = workDir.resolve("apple-format-matrix-aac.m4a"),
                fileFormat = "m4af",
                dataFormat = "aac",
            )
        val aacPath: Path =
            convertWav(
                wavPath = sourceWavPath,
                outputPath = workDir.resolve("apple-format-matrix.aac"),
                fileFormat = "adts",
                dataFormat = "aac",
            )
        val flacPath: Path =
            convertWav(
                wavPath = sourceWavPath,
                outputPath = workDir.resolve("apple-format-matrix.flac"),
                fileFormat = "flac",
                dataFormat = null,
            )
        val aiffPath: Path =
            convertWav(
                wavPath = sourceWavPath,
                outputPath = workDir.resolve("apple-format-matrix.aiff"),
                fileFormat = "AIFF",
                dataFormat = "BEI16",
            )
        val alacPath: Path =
            convertWav(
                wavPath = sourceWavPath,
                outputPath = workDir.resolve("apple-format-matrix-alac.m4a"),
                fileFormat = "m4af",
                dataFormat = "alac",
            )
        return mapOf(
            "MP3" to listOf(mp3Path),
            "M4A/AAC" to listOf(m4aPath, aacPath),
            "WAV" to listOf(sourceWavPath),
            "FLAC" to listOf(flacPath),
            "AIFF/ALAC" to listOf(aiffPath, alacPath),
        )
    }

    /** 使用 AVFoundation 的可播放性加载检查真实样本，不依赖旧第三方播放器结论。 */
    private fun checkAvFoundationPlayable(
        formatName: String,
        mediaPath: Path,
        moduleCacheDir: Path,
    ) {
        Files.createDirectories(moduleCacheDir)
        val script: String = buildAvFoundationPlayableScript(mediaPath = mediaPath)
        val processBuilder: ProcessBuilder =
            ProcessBuilder(
                "/usr/bin/swift",
                "-e",
                script,
            ).redirectErrorStream(true)
        processBuilder.environment()["CLANG_MODULE_CACHE_PATH"] = moduleCacheDir.toString()
        val process: Process = processBuilder.start()
        val output: String = process.inputStream.bufferedReader().readText()
        val exitCode: Int = process.waitFor()
        check(exitCode == 0) {
            "$formatName AVFoundation 可播放性检查失败：$output"
        }
        check(output.lineSequence().any { line: String -> line.trim() == "playable" }) {
            "$formatName AVFoundation 判定不可播放：$output"
        }
        println("format-matrix: $formatName=支持，AVFoundation 可播放性检查样本：${mediaPath.fileName}")
    }

    /** 生成 Swift 脚本，调用 AVFoundation 的 isPlayable key。 */
    private fun buildAvFoundationPlayableScript(mediaPath: Path): String {
        val escapedPath: String =
            mediaPath
                .toAbsolutePath()
                .toString()
                .replace(oldValue = "\\", newValue = "\\\\")
                .replace(oldValue = "\"", newValue = "\\\"")
        return "import AVFoundation; import Foundation; " +
            "let url = URL(fileURLWithPath: \"$escapedPath\"); " +
            "let asset = AVURLAsset(url: url); " +
            "let playable = try await asset.load(.isPlayable); " +
            "print(playable ? \"playable\" : \"not-playable\")"
    }

    /** 写入短正弦波 WAV，作为格式转换源文件。 */
    private fun createSmokeWav(wavPath: Path) {
        Files.deleteIfExists(wavPath)
        val sampleRate: Float = 44_100f
        val frames: Int = (sampleRate * 0.8f).toInt()
        val bytes: ByteArray = ByteArray(size = frames * 2)
        for (index: Int in 0 until frames) {
            val value: Short = (Short.MAX_VALUE * 0.25 * kotlin.math.sin(2.0 * Math.PI * 440.0 * index / sampleRate)).toInt().toShort()
            bytes[index * 2] = (value.toInt() and 0xff).toByte()
            bytes[index * 2 + 1] = ((value.toInt() ushr 8) and 0xff).toByte()
        }
        val format: AudioFormat = AudioFormat(sampleRate, 16, 1, true, false)
        AudioInputStream(ByteArrayInputStream(bytes), format, frames.toLong()).use { stream: AudioInputStream ->
            AudioSystem.write(stream, AudioFileFormat.Type.WAVE, wavPath.toFile())
        }
    }

    /** 生成 MP3 样本；macOS afconvert 当前可解码 MP3 但不一定能编码 MP3。 */
    private fun convertWavToMp3(
        wavPath: Path,
        outputPath: Path,
    ): Path =
        runProcess(
            command =
                mp3EncoderCommand(
                    wavPath = wavPath,
                    outputPath = outputPath,
                ),
            outputPath = outputPath,
            label = "MP3",
        )

    /** 选择当前机器可用的 MP3 编码器，样本只用于后续 AVFoundation 检查。 */
    private fun mp3EncoderCommand(
        wavPath: Path,
        outputPath: Path,
    ): List<String> {
        val ffmpegPath: Path? =
            listOf("/opt/homebrew/bin/ffmpeg", "/usr/local/bin/ffmpeg")
                .map { candidate: String -> Paths.get(candidate) }
                .firstOrNull { path: Path -> Files.isExecutable(path) }
        if (ffmpegPath != null) {
            return listOf(ffmpegPath.toString(), "-y", "-hide_banner", "-loglevel", "error", "-i", wavPath.toString(), "-codec:a", "libmp3lame", "-b:a", "128k", outputPath.toString())
        }
        val lamePath: Path? =
            listOf("/opt/homebrew/bin/lame", "/usr/local/bin/lame")
                .map { candidate: String -> Paths.get(candidate) }
                .firstOrNull { path: Path -> Files.isExecutable(path) }
        if (lamePath != null) {
            return listOf(lamePath.toString(), "--silent", wavPath.toString(), outputPath.toString())
        }
        return listOf("/usr/bin/afconvert", "-f", "MPG3", "-d", ".mp3", wavPath.toString(), outputPath.toString())
    }

    /** 调用 macOS afconvert 生成指定格式样本。 */
    private fun convertWav(
        wavPath: Path,
        outputPath: Path,
        fileFormat: String,
        dataFormat: String?,
    ): Path {
        val command: MutableList<String> = mutableListOf("/usr/bin/afconvert", "-f", fileFormat)
        if (dataFormat != null) {
            command += "-d"
            command += dataFormat
        }
        command += wavPath.toString()
        command += outputPath.toString()
        return runProcess(command = command, outputPath = outputPath, label = fileFormat)
    }

    /** 执行样本生成命令，失败时保留命令输出作为诊断。 */
    private fun runProcess(
        command: List<String>,
        outputPath: Path,
        label: String,
    ): Path {
        Files.deleteIfExists(outputPath)
        try {
            val process: Process = ProcessBuilder(command).redirectErrorStream(true).start()
            val output: String = process.inputStream.bufferedReader().readText()
            val exitCode: Int = process.waitFor()
            check(exitCode == 0) {
                "生成 $label 样本失败：$output"
            }
        } catch (error: IOException) {
            throw IllegalStateException("无法启动格式样本生成命令，不能执行格式矩阵 smoke", error)
        }
        return outputPath
    }
}
