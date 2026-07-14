package com.yanhao.kmpmusic.data

import com.yanhao.kmpmusic.domain.model.LocalMusicScanErrorType
import com.yanhao.kmpmusic.domain.model.LocalMusicScanCoverage
import com.yanhao.kmpmusic.domain.model.LocalMusicScanRequest
import com.yanhao.kmpmusic.domain.model.LocalMusicScanResult
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking
import platform.Foundation.NSURL

/**
 * iOS 导入测试，保护授权窗口复制到沙盒后再发布到曲库的外部行为。
 */
class IosFolderMusicScannerTest {
    /**
     * 授权目录中的音频必须先复制进 App 沙盒，曲库只消费沙盒内 [MusicFileMetadata.localUri]。
     */
    @Test
    fun scanPublishesSandboxUriAfterCopyingImportedAudio(): Unit = runBlocking {
        val fileSystem: FakeIosImportFileSystem = FakeIosImportFileSystem()
        fileSystem.registerFolder(
            folderPath = "/external/Files/Music",
            subpaths = listOf("Album/Song.mp3"),
        )
        fileSystem.registerReadableFile(path = "/external/Files/Music/Album/Song.mp3")
        val scanner: IosFolderMusicScanner = scannerWith(fileSystem = fileSystem)
        val result: LocalMusicScanResult = scanner.scan(request = LocalMusicScanRequest.Refresh)
        assertEquals(expected = 1, actual = result.discovered.size)
        assertTrue(
            actual = result.discovered.single().localUri.startsWith(
                prefix = "file:///app/Documents/KMPMusicImportedAudio/",
            ),
        )
        assertFalse(
            actual = result.discovered.single().localUri.contains(other = "/external/Files"),
        )
        assertEquals(expected = 1, actual = fileSystem.copyOperations.size)
        assertEquals(
            expected = "/external/Files/Music/Album/Song.mp3",
            actual = fileSystem.copyOperations.single().sourcePath,
        )
        assertTrue(actual = fileSystem.copyOperations.single().destinationPath.endsWith(suffix = ".importing"))
        assertEquals(expected = listOf(LocalMusicScanCoverage.PositiveOnly), actual = result.completedCoverage)
    }

    /**
     * 如果导入层意外产出外部 URL，扫描器必须把它挡在可播放集合之外。
     */
    @Test
    fun scanRejectsImportedAudioWhenCommittedPathIsOutsideSandbox(): Unit = runBlocking {
        val fileSystem: FakeIosImportFileSystem = FakeIosImportFileSystem(isCommittedPathInSandbox = false)
        fileSystem.registerFolder(
            folderPath = "/external/Files/Music",
            subpaths = listOf("Song.m4a"),
        )
        fileSystem.registerReadableFile(path = "/external/Files/Music/Song.m4a")
        val scanner: IosFolderMusicScanner = scannerWith(fileSystem = fileSystem)
        val result: LocalMusicScanResult = scanner.scan(request = LocalMusicScanRequest.Refresh)
        assertTrue(actual = result.discovered.isEmpty())
        assertEquals(expected = 1, actual = result.failed.size)
        assertEquals(expected = LocalMusicScanErrorType.SecurityScopeExpired, actual = result.failed.single().error.type)
        assertEquals(expected = 0, actual = result.sourceSummaries.single().songCount)
        assertEquals(expected = 1, actual = result.sourceSummaries.single().problemCount)
    }

    /**
     * 复制失败或中断时只能记录问题，临时文件要清理，半成品不能进入曲库。
     */
    @Test
    fun scanReportsCopyFailureWithoutPublishingPartialImport(): Unit = runBlocking {
        val fileSystem: FakeIosImportFileSystem = FakeIosImportFileSystem()
        fileSystem.registerFolder(
            folderPath = "/external/Files/Music",
            subpaths = listOf("Broken.flac"),
        )
        fileSystem.registerReadableFile(path = "/external/Files/Music/Broken.flac")
        fileSystem.copyFailures += "/external/Files/Music/Broken.flac"
        val scanner: IosFolderMusicScanner = scannerWith(fileSystem = fileSystem)
        val result: LocalMusicScanResult = scanner.scan(request = LocalMusicScanRequest.Refresh)
        assertTrue(actual = result.discovered.isEmpty())
        assertEquals(expected = LocalMusicScanErrorType.FileUnreadable, actual = result.failed.single().error.type)
        assertTrue(actual = fileSystem.removedPaths.any { path: String -> path.endsWith(suffix = ".importing") })
        assertTrue(actual = fileSystem.committedPaths.isEmpty())
    }

    /**
     * 源文件缺失时只返回可理解问题，不把不存在的外部路径带入曲库。
     */
    @Test
    fun scanReportsMissingSourceFileWithoutPublishingIt(): Unit = runBlocking {
        val fileSystem: FakeIosImportFileSystem = FakeIosImportFileSystem()
        fileSystem.registerFolder(
            folderPath = "/external/Files/Music",
            subpaths = listOf("Missing.mp3"),
        )
        val scanner: IosFolderMusicScanner = scannerWith(fileSystem = fileSystem)
        val result: LocalMusicScanResult = scanner.scan(request = LocalMusicScanRequest.Refresh)
        assertTrue(actual = result.discovered.isEmpty())
        assertEquals(expected = LocalMusicScanErrorType.FileMissing, actual = result.failed.single().error.type)
        assertTrue(actual = result.failed.single().error.message.contains(other = "重新选择音频"))
    }

    /**
     * 授权失效或文件不可读时映射为 security scope 问题，提示用户重新导入。
     */
    @Test
    fun scanReportsUnreadableSourceAsSecurityScopeExpired(): Unit = runBlocking {
        val fileSystem: FakeIosImportFileSystem = FakeIosImportFileSystem()
        fileSystem.registerFolder(
            folderPath = "/external/Files/Music",
            subpaths = listOf("Locked.aiff"),
        )
        fileSystem.registerExistingFile(path = "/external/Files/Music/Locked.aiff")
        val scanner: IosFolderMusicScanner = scannerWith(fileSystem = fileSystem)
        val result: LocalMusicScanResult = scanner.scan(request = LocalMusicScanRequest.Refresh)
        assertTrue(actual = result.discovered.isEmpty())
        assertEquals(expected = LocalMusicScanErrorType.SecurityScopeExpired, actual = result.failed.single().error.type)
        assertTrue(actual = result.failed.single().error.message.contains(other = "重新导入音频"))
    }

    /**
     * Apple 矩阵待验证格式不能进入 iOS 导入曲库，也不触发沙盒复制。
     */
    @Test
    fun scanIgnoresAppleUnverifiedAudioFormats(): Unit = runBlocking {
        val fileSystem: FakeIosImportFileSystem = FakeIosImportFileSystem()
        fileSystem.registerFolder(
            folderPath = "/external/Files/Music",
            subpaths = listOf("voice.ogg", "memo.opus", "recording.amr"),
        )
        fileSystem.registerReadableFile(path = "/external/Files/Music/voice.ogg")
        fileSystem.registerReadableFile(path = "/external/Files/Music/memo.opus")
        fileSystem.registerReadableFile(path = "/external/Files/Music/recording.amr")
        val scanner: IosFolderMusicScanner = scannerWith(fileSystem = fileSystem)
        val result: LocalMusicScanResult = scanner.scan(request = LocalMusicScanRequest.Refresh)
        assertTrue(actual = result.discovered.isEmpty())
        assertTrue(actual = result.failed.isEmpty())
        assertTrue(actual = fileSystem.copyOperations.isEmpty())
        assertEquals(expected = 0, actual = result.sourceSummaries.single().songCount)
    }

    /**
     * 重复导入同一个文件时复用既有沙盒副本，避免覆盖已可播放文件。
     */
    @Test
    fun scanReusesExistingSandboxCopyWithoutOverwritingIt(): Unit = runBlocking {
        val fileSystem: FakeIosImportFileSystem = FakeIosImportFileSystem()
        fileSystem.registerFolder(
            folderPath = "/external/Files/Music",
            subpaths = listOf("Song.wav"),
        )
        fileSystem.registerReadableFile(path = "/external/Files/Music/Song.wav")
        fileSystem.registerExistingImportFor(sourcePath = "/external/Files/Music/Song.wav", fileName = "Song.wav")
        val scanner: IosFolderMusicScanner = scannerWith(fileSystem = fileSystem)
        val result: LocalMusicScanResult = scanner.scan(request = LocalMusicScanRequest.Refresh)
        assertEquals(expected = 1, actual = result.discovered.size)
        assertTrue(
            actual = result.discovered.single().localUri.startsWith(
                prefix = "file:///app/Documents/KMPMusicImportedAudio/",
            ),
        )
        assertTrue(actual = fileSystem.copyOperations.isEmpty())
        assertTrue(actual = fileSystem.committedPaths.isEmpty())
    }

    // 构造只依赖 fake 文件系统的 iOS scanner，避免测试触发 UIKit 文件选择器。
    private fun scannerWith(fileSystem: FakeIosImportFileSystem): IosFolderMusicScanner {
        return IosFolderMusicScanner(
            chooseFolder = { NSURL.fileURLWithPath(path = "/external/Files/Music") },
            fileSystem = fileSystem,
            nowMillis = { 1_000L },
        )
    }
}
