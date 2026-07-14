package com.yanhao.kmpmusic.data

import com.yanhao.kmpmusic.domain.model.LocalMusicScanCoverage
import com.yanhao.kmpmusic.domain.model.LocalMusicScanRequest
import com.yanhao.kmpmusic.domain.model.LocalMusicScanResult
import com.yanhao.kmpmusic.domain.model.LocalMusicSourceKind
import java.nio.file.Files
import java.nio.file.Path
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * 桌面文件夹扫描器测试，保护扫描目录的稳定具体来源身份。
 */
class DesktopFolderMusicScannerTest {
    /**
     * 扫描目录身份必须独立于展示名，后续合并才能区分同名或不同目录来源。
     */
    @Test
    fun scanReturnsStableFolderSourceIdentitySeparateFromDisplayName(): Unit = runTest {
        val folder: Path = Files.createTempDirectory("kmp-music-desktop-source")
        val audioPath: Path = folder.resolve("source-identity.mp3")
        Files.writeString(audioPath, "not a real audio frame")
        val folderSourceId: String = folder.toAbsolutePath().normalize().toString()
        val scanner: DesktopFolderMusicScanner = DesktopFolderMusicScanner(
            chooseFolder = { folder },
            nowMillis = { 123L },
        )
        val result: LocalMusicScanResult = scanner.scan(
            request = LocalMusicScanRequest.Source(sourceKind = LocalMusicSourceKind.DesktopFolder),
        )
        val coverage: LocalMusicScanCoverage.ConcreteSource = assertIs(result.completedCoverage.single())
        assertEquals(
            expected = folderSourceId,
            actual = result.sourceSummaries.single().sourceId,
        )
        assertEquals(
            expected = folder.fileName.toString(),
            actual = result.sourceSummaries.single().displayName,
        )
        assertEquals(
            expected = folderSourceId,
            actual = coverage.sourceId,
        )
        assertEquals(
            expected = LocalMusicSourceKind.DesktopFolder,
            actual = coverage.sourceKind,
        )
        assertEquals(
            expected = audioPath.toAbsolutePath().normalize().toString(),
            actual = result.discovered.single().sourceId,
        )
        assertEquals(
            expected = folderSourceId,
            actual = result.discovered.single().concreteSourceId,
        )
    }

    /**
     * Apple 矩阵待验证格式不能被桌面扫描入口发布为可播放曲目。
     */
    @Test
    fun scanIgnoresAppleUnverifiedAudioFormats(): Unit = runTest {
        val folder: Path = Files.createTempDirectory("kmp-music-desktop-unverified")
        Files.writeString(folder.resolve("voice.ogg"), "not a verified apple format")
        Files.writeString(folder.resolve("memo.opus"), "not a verified apple format")
        Files.writeString(folder.resolve("recording.amr"), "not a verified apple format")
        val scanner: DesktopFolderMusicScanner = DesktopFolderMusicScanner(
            chooseFolder = { folder },
            nowMillis = { 123L },
        )
        val result: LocalMusicScanResult = scanner.scan(
            request = LocalMusicScanRequest.Source(sourceKind = LocalMusicSourceKind.DesktopFolder),
        )
        assertTrue(actual = result.discovered.isEmpty())
        assertTrue(actual = result.failed.isEmpty())
        assertEquals(expected = 0, actual = result.sourceSummaries.single().songCount)
        assertEquals(expected = 0, actual = result.sourceSummaries.single().problemCount)
    }
}
