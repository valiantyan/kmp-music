package com.yanhao.kmpmusic.data

import com.yanhao.kmpmusic.domain.model.LocalMusicProblem
import com.yanhao.kmpmusic.domain.model.LocalMusicScanError
import com.yanhao.kmpmusic.domain.model.LocalMusicScanErrorType
import com.yanhao.kmpmusic.domain.model.LocalMusicScanException
import com.yanhao.kmpmusic.domain.model.LocalMusicScanRequest
import com.yanhao.kmpmusic.domain.model.LocalMusicScanResult
import com.yanhao.kmpmusic.domain.model.LocalMusicSourceKind
import com.yanhao.kmpmusic.domain.model.LocalMusicSourceSummary
import com.yanhao.kmpmusic.domain.model.MusicFileMetadata
import com.yanhao.kmpmusic.domain.repository.LocalMusicScanner
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.attribute.BasicFileAttributes
import javax.swing.JFileChooser
import javax.swing.SwingUtilities
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext

/**
 * Desktop 平台文件夹扫描器，由用户选择音乐文件夹后递归发现可播放音频。
 */
class DesktopFolderMusicScanner : LocalMusicScanner {
    // 真实封面提取集中在 scanner 边界，UI 只消费平台无关 URI。
    private val artworkExtractor: DesktopEmbeddedArtworkExtractor = DesktopEmbeddedArtworkExtractor()
    // 音频标签读取集中在 scanner 边界，保证桌面端与 Android 输出同一组元数据字段。
    private val metadataReader: DesktopAudioMetadataReader = DesktopAudioMetadataReader()

    /** 弹出文件夹选择器并把真实文件扫描结果写入统一曲库链路。 */
    override suspend fun scan(request: LocalMusicScanRequest): LocalMusicScanResult {
        validateRequest(request = request)
        val selectedFolder: Path = chooseMusicFolder()
        return withContext(context = Dispatchers.IO) {
            scanFolder(folder = selectedFolder)
        }
    }

    // 只接受通用刷新或桌面文件夹来源请求，避免平台 scanner 被错误复用。
    private fun validateRequest(request: LocalMusicScanRequest) {
        if (request is LocalMusicScanRequest.Source && request.sourceKind != LocalMusicSourceKind.DesktopFolder) {
            throw LocalMusicScanException(
                error = LocalMusicScanError(
                    type = LocalMusicScanErrorType.FolderUnavailable,
                    message = "当前桌面端只能扫描用户选择的音乐文件夹",
                    sourceKind = LocalMusicSourceKind.DesktopFolder,
                ),
            )
        }
    }

    // 在 Swing 事件线程显示文件夹选择器，避免阻塞 Compose 主渲染线程。
    private suspend fun chooseMusicFolder(): Path {
        return suspendCancellableCoroutine { continuation ->
            SwingUtilities.invokeLater {
                val chooser: JFileChooser = JFileChooser().apply {
                    dialogTitle = "选择音乐文件夹"
                    fileSelectionMode = JFileChooser.DIRECTORIES_ONLY
                    isAcceptAllFileFilterUsed = false
                }
                val result: Int = chooser.showOpenDialog(null)
                if (!continuation.isActive) {
                    return@invokeLater
                }
                if (result == JFileChooser.APPROVE_OPTION) {
                    continuation.resume(value = chooser.selectedFile.toPath())
                    return@invokeLater
                }
                continuation.resumeWithException(
                    exception = LocalMusicScanException(
                        error = LocalMusicScanError(
                            type = LocalMusicScanErrorType.UserCancelled,
                            message = "未选择音乐文件夹",
                            sourceKind = LocalMusicSourceKind.DesktopFolder,
                        ),
                    ),
                )
            }
        }
    }

    // 递归扫描用户授权的文件夹，并把不可读文件记录为问题条目。
    private fun scanFolder(folder: Path): LocalMusicScanResult {
        if (!Files.isDirectory(folder)) {
            throw unavailableFolderException(folder = folder)
        }
        val discovered: MutableList<MusicFileMetadata> = mutableListOf()
        val failed: MutableList<LocalMusicProblem> = mutableListOf()
        val completedAt: Long = System.currentTimeMillis()
        val stream = Files.walk(folder)
        try {
            stream.forEach { path -> scanPath(path = path, discovered = discovered, failed = failed) }
        } catch (ioException: IOException) {
            throw LocalMusicScanException(
                error = LocalMusicScanError(
                    type = LocalMusicScanErrorType.FolderUnavailable,
                    message = "无法读取音乐文件夹：${folder.fileName}",
                    sourceKind = LocalMusicSourceKind.DesktopFolder,
                    sourceId = folder.toAbsolutePath().normalize().toString(),
                ),
                cause = ioException,
            )
        } finally {
            stream.close()
        }
        return LocalMusicScanResult(
            discovered = discovered,
            failed = failed,
            sourceSummaries = listOf(
                LocalMusicSourceSummary(
                    sourceKind = LocalMusicSourceKind.DesktopFolder,
                    displayName = folder.fileName?.toString() ?: LocalMusicSourceKind.DesktopFolder.displayName,
                    songCount = discovered.size,
                    problemCount = failed.size,
                    lastScannedAt = completedAt,
                ),
            ),
            completedAt = completedAt,
        )
    }

    // 扫描单个路径，非音频文件直接忽略，不进入失败列表。
    private fun scanPath(
        path: Path,
        discovered: MutableList<MusicFileMetadata>,
        failed: MutableList<LocalMusicProblem>,
    ) {
        if (!Files.isRegularFile(path)) {
            return
        }
        val fileName: String = path.fileName?.toString().orEmpty()
        val audioType: LocalAudioType = LocalAudioFileRules.matchAudioType(fileName = fileName) ?: return
        val sourceId: String = path.toAbsolutePath().normalize().toString()
        if (!Files.isReadable(path)) {
            failed += unreadableProblem(sourceId = sourceId, fileName = fileName)
            return
        }
        val attributes: BasicFileAttributes = Files.readAttributes(path, BasicFileAttributes::class.java)
        val audioMetadata: DesktopAudioMetadata = metadataReader.readMetadata(audioPath = path)
        discovered += MusicFileMetadata(
            sourceId = sourceId,
            sourceKind = LocalMusicSourceKind.DesktopFolder,
            localUri = path.toUri().toString(),
            fileName = fileName,
            title = audioMetadata.title ?: LocalAudioFileRules.titleFromFileName(fileName = fileName),
            artist = audioMetadata.artist,
            album = audioMetadata.album ?: path.parent?.fileName?.toString(),
            durationMs = audioMetadata.durationMs,
            mimeType = audioType.mimeType,
            sizeBytes = attributes.size(),
            modifiedAt = attributes.lastModifiedTime().toMillis(),
            coverArt = LocalAudioFileRules.coverForSourceId(sourceId = sourceId),
            coverImageUri = artworkExtractor.extractArtworkUri(
                audioPath = path,
                sourceId = sourceId,
            ),
        )
    }

    // 构造文件夹不可用错误，避免选择器返回异常路径时继续扫描。
    private fun unavailableFolderException(folder: Path): LocalMusicScanException {
        return LocalMusicScanException(
            error = LocalMusicScanError(
                type = LocalMusicScanErrorType.FolderUnavailable,
                message = "选择的音乐文件夹不可用",
                sourceKind = LocalMusicSourceKind.DesktopFolder,
                sourceId = folder.toAbsolutePath().normalize().toString(),
            ),
        )
    }

    // 将可识别但不可读的音频文件记录到来源问题中。
    private fun unreadableProblem(sourceId: String, fileName: String): LocalMusicProblem {
        return LocalMusicProblem(
            sourceKind = LocalMusicSourceKind.DesktopFolder,
            sourceId = sourceId,
            fileName = fileName,
            error = LocalMusicScanError(
                type = LocalMusicScanErrorType.FileUnreadable,
                message = "无法读取音频文件",
                sourceKind = LocalMusicSourceKind.DesktopFolder,
                sourceId = sourceId,
            ),
        )
    }
}
