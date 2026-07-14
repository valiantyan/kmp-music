package com.yanhao.kmpmusic.data

import com.yanhao.kmpmusic.domain.model.LocalMusicProblem
import com.yanhao.kmpmusic.domain.model.LocalMusicScanErrorType

/**
 * 单个 iOS 音频文件的沙盒导入请求。
 *
 * @property sourcePath security scope 授权窗口内的外部源文件路径。
 * @property fileName 原始文件名，用于生成稳定沙盒文件名和错误提示。
 * @property importDirectoryPath App 沙盒内的持久导入目录。
 */
internal data class IosSandboxAudioImportCandidate(
    val sourcePath: String,
    val fileName: String,
    val importDirectoryPath: String,
)

/**
 * iOS 音频文件提交结果，成功时只暴露沙盒内最终路径。
 */
internal sealed interface IosSandboxAudioCommitResult {
    /**
     * 已经可被曲库消费的沙盒文件路径。
     */
    data class Success(
        val committedPath: String,
    ) : IosSandboxAudioCommitResult

    /**
     * 不能进入曲库的问题结果。
     */
    data class Failure(
        val problem: LocalMusicProblem,
    ) : IosSandboxAudioCommitResult
}

/**
 * 负责把单个授权文件复制到临时文件，再提交为 App 沙盒内可播放文件。
 */
internal class IosSandboxAudioCommitter(
    private val fileSystem: IosImportFileSystem,
) {
    /** 执行单个文件导入，失败时不返回半成品路径。 */
    fun commit(candidate: IosSandboxAudioImportCandidate): IosSandboxAudioCommitResult {
        if (!fileSystem.fileExists(path = candidate.sourcePath)) {
            return createFailure(
                candidate = candidate,
                type = LocalMusicScanErrorType.FileMissing,
                message = "导入源文件已不存在，请重新选择音频",
            )
        }
        if (!fileSystem.isReadableFile(path = candidate.sourcePath)) {
            return createFailure(
                candidate = candidate,
                type = LocalMusicScanErrorType.SecurityScopeExpired,
                message = "无法访问导入源文件，请重新导入音频",
            )
        }
        val committedPath: String = buildImportedAudioPath(
            importDirectoryPath = candidate.importDirectoryPath,
            sourcePath = candidate.sourcePath,
            fileName = candidate.fileName,
        )
        if (fileSystem.fileExists(path = committedPath)) {
            return reuseExistingImport(
                candidate = candidate,
                committedPath = committedPath,
            )
        }
        return copyIntoSandbox(
            candidate = candidate,
            committedPath = committedPath,
        )
    }

    /** 复用已有沙盒副本，避免重复导入覆盖可播放文件。 */
    private fun reuseExistingImport(
        candidate: IosSandboxAudioImportCandidate,
        committedPath: String,
    ): IosSandboxAudioCommitResult {
        if (!fileSystem.isReadableFile(path = committedPath)) {
            return createFailure(
                candidate = candidate,
                type = LocalMusicScanErrorType.FileUnreadable,
                message = "已导入音频不可读，请重新导入",
            )
        }
        return validateCommittedPath(
            candidate = candidate,
            committedPath = committedPath,
        )
    }

    /** 执行临时复制和最终提交。 */
    private fun copyIntoSandbox(
        candidate: IosSandboxAudioImportCandidate,
        committedPath: String,
    ): IosSandboxAudioCommitResult {
        val temporaryPath: String = "$committedPath.importing"
        fileSystem.removeFile(path = temporaryPath)
        if (!fileSystem.copyFile(sourcePath = candidate.sourcePath, destinationPath = temporaryPath)) {
            fileSystem.removeFile(path = temporaryPath)
            return createFailure(
                candidate = candidate,
                type = LocalMusicScanErrorType.FileUnreadable,
                message = "复制音频到 App 沙盒失败，请重新导入",
            )
        }
        if (!fileSystem.moveFile(sourcePath = temporaryPath, destinationPath = committedPath)) {
            fileSystem.removeFile(path = temporaryPath)
            return createFailure(
                candidate = candidate,
                type = LocalMusicScanErrorType.FileUnreadable,
                message = "提交导入音频失败，请重新导入",
            )
        }
        return validateCommittedPath(
            candidate = candidate,
            committedPath = committedPath,
        )
    }

    /** 防止外部 URL 或错误目录逃过复制边界进入可播放集合。 */
    private fun validateCommittedPath(
        candidate: IosSandboxAudioImportCandidate,
        committedPath: String,
    ): IosSandboxAudioCommitResult {
        if (fileSystem.isPathInAppSandbox(path = committedPath)) {
            return IosSandboxAudioCommitResult.Success(committedPath = committedPath)
        }
        return createFailure(
            candidate = candidate,
            type = LocalMusicScanErrorType.SecurityScopeExpired,
            message = "导入结果不在 App 沙盒内，已阻止加入曲库",
        )
    }

    /** 按导入候选生成平台无关问题结果。 */
    private fun createFailure(
        candidate: IosSandboxAudioImportCandidate,
        type: LocalMusicScanErrorType,
        message: String,
    ): IosSandboxAudioCommitResult.Failure {
        return IosSandboxAudioCommitResult.Failure(
            problem = createIosImportProblem(
                sourcePath = candidate.sourcePath,
                fileName = candidate.fileName,
                type = type,
                message = message,
            ),
        )
    }
}
