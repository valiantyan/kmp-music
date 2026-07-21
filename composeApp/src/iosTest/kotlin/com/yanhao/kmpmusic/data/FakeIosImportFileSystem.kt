package com.yanhao.kmpmusic.data

import platform.Foundation.NSURL

/**
 * iOS 导入测试用文件系统，记录复制、提交和清理行为。
 */
internal class FakeIosImportFileSystem(
    // 是否把提交路径视作 App 沙盒内路径，用于验证外部 URL 防线。
    private val isCommittedPathInSandbox: Boolean = true,
) : IosImportFileSystem {
    // 复制操作记录。
    val copyOperations: MutableList<CopyOperation> = mutableListOf()

    // 需要模拟复制失败的源文件路径。
    val copyFailures: MutableSet<String> = mutableSetOf()

    // 已触发清理的路径。
    val removedPaths: MutableList<String> = mutableListOf()

    // 已提交到最终位置的路径。
    val committedPaths: MutableList<String> = mutableListOf()

    // 目录到相对路径列表的映射。
    private val folderSubpaths: MutableMap<String, List<String>> = mutableMapOf()

    // 当前可读的测试文件路径。
    private val readableFiles: MutableSet<String> = mutableSetOf()

    // 当前存在的测试路径。
    private val existingPaths: MutableSet<String> = mutableSetOf()

    /** 从测试 URL 读取路径。 */
    override fun path(url: NSURL): String? = url.path

    /** 从测试 URL 读取绝对字符串。 */
    override fun absoluteString(url: NSURL): String = url.absoluteString ?: ""

    /** 从测试 URL 读取末级名称。 */
    override fun lastPathComponent(url: NSURL): String? = url.lastPathComponent

    /** 返回预注册的目录相对路径。 */
    override fun listSubpaths(path: String): List<String>? = folderSubpaths[path]

    /** 判断测试路径是否存在。 */
    override fun fileExists(path: String): Boolean = existingPaths.contains(element = path)

    /** 判断测试路径是否可读。 */
    override fun isReadableFile(path: String): Boolean = readableFiles.contains(element = path)

    /** 返回固定沙盒导入目录。 */
    override fun sandboxImportDirectoryPath(): String = "/app/Documents/KMPMusicImportedAudio"

    /** 测试目录默认可创建。 */
    override fun ensureDirectory(path: String): Boolean = true

    /** 将测试路径转为 file URL。 */
    override fun fileUrlString(path: String): String = "file://$path"

    /** 记录复制行为，并按需模拟失败。 */
    override fun copyFile(
        sourcePath: String,
        destinationPath: String,
    ): Boolean {
        if (copyFailures.contains(element = sourcePath)) {
            return false
        }
        copyOperations +=
            CopyOperation(
                sourcePath = sourcePath,
                destinationPath = destinationPath,
            )
        existingPaths += destinationPath
        return true
    }

    /** 把临时路径提交到最终路径。 */
    override fun moveFile(
        sourcePath: String,
        destinationPath: String,
    ): Boolean {
        if (!existingPaths.contains(element = sourcePath)) {
            return false
        }
        existingPaths -= sourcePath
        existingPaths += destinationPath
        readableFiles -= sourcePath
        readableFiles += destinationPath
        committedPaths += destinationPath
        return true
    }

    /** 记录清理路径。 */
    override fun removeFile(path: String): Boolean {
        existingPaths -= path
        removedPaths += path
        return true
    }

    /** 按测试开关决定提交路径是否可被视为沙盒内路径。 */
    override fun isPathInAppSandbox(path: String): Boolean = isCommittedPathInSandbox && path.startsWith(prefix = sandboxImportDirectoryPath())

    /** 注册一个可遍历目录。 */
    fun registerFolder(
        folderPath: String,
        subpaths: List<String>,
    ) {
        folderSubpaths[folderPath] = subpaths
        existingPaths += folderPath
    }

    /** 注册一个存在且可读的文件。 */
    fun registerReadableFile(path: String) {
        existingPaths += path
        readableFiles += path
    }

    /** 注册一个存在但不可读的文件。 */
    fun registerExistingFile(path: String) {
        existingPaths += path
    }

    /** 注册某个源文件已经存在的沙盒副本。 */
    fun registerExistingImportFor(
        sourcePath: String,
        fileName: String,
    ) {
        val importedPath: String =
            buildImportedAudioPath(
                importDirectoryPath = sandboxImportDirectoryPath(),
                sourcePath = sourcePath,
                fileName = fileName,
            )
        existingPaths += importedPath
        readableFiles += importedPath
    }
}

/**
 * 复制操作记录，供测试断言源路径不会直接发布到曲库。
 *
 * @property sourcePath 复制源路径。
 * @property destinationPath 复制目标路径。
 */
internal data class CopyOperation(
    val sourcePath: String,
    val destinationPath: String,
)
