package com.yanhao.kmpmusic.data

import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSURL
import platform.Foundation.NSUserDomainMask

/**
 * iOS 文件系统边界，测试用 fake 验证复制与提交规则。
 */
internal interface IosImportFileSystem {
    /** 从 [NSURL] 读取平台路径。 */
    fun path(url: NSURL): String?
    /** 从 [NSURL] 读取展示或诊断用绝对 URL。 */
    fun absoluteString(url: NSURL): String
    /** 从 [NSURL] 读取末级名称。 */
    fun lastPathComponent(url: NSURL): String?
    /** 列出目录下所有相对路径，返回空集合代表目录为空。 */
    fun listSubpaths(path: String): List<String>?
    /** 判断路径是否存在。 */
    fun fileExists(path: String): Boolean
    /** 判断路径是否可读。 */
    fun isReadableFile(path: String): Boolean
    /** 返回 App 沙盒内导入目录路径。 */
    fun sandboxImportDirectoryPath(): String?
    /** 确保目录存在，失败时返回 false。 */
    fun ensureDirectory(path: String): Boolean
    /** 把路径转换为 file URL 字符串。 */
    fun fileUrlString(path: String): String
    /** 复制文件到目标路径。 */
    fun copyFile(sourcePath: String, destinationPath: String): Boolean
    /** 原子提交临时文件到最终路径。 */
    fun moveFile(sourcePath: String, destinationPath: String): Boolean
    /** 删除路径，常用于清理临时文件。 */
    fun removeFile(path: String): Boolean
    /** 判断路径是否仍处在 App 沙盒边界内。 */
    fun isPathInAppSandbox(path: String): Boolean
}

/**
 * 基于 [NSFileManager] 的 iOS 文件系统实现。
 */
@OptIn(ExperimentalForeignApi::class)
internal class NSFileManagerIosImportFileSystemImpl : IosImportFileSystem {
    // 系统文件管理器，集中处理 Foundation 文件 API。
    private val fileManager: NSFileManager = NSFileManager.defaultManager

    /** 从 [NSURL] 读取平台路径。 */
    override fun path(url: NSURL): String? = url.path

    /** 从 [NSURL] 读取展示或诊断用绝对 URL。 */
    override fun absoluteString(url: NSURL): String = url.absoluteString ?: ""

    /** 从 [NSURL] 读取末级名称。 */
    override fun lastPathComponent(url: NSURL): String? = url.lastPathComponent

    /** 递归列出目录相对路径。 */
    override fun listSubpaths(path: String): List<String>? {
        val subpaths: List<*> = fileManager.subpathsAtPath(path = path) ?: return null
        return subpaths.mapNotNull { subpath: Any? -> subpath as? String }
    }

    /** 判断文件或目录是否存在。 */
    override fun fileExists(path: String): Boolean = fileManager.fileExistsAtPath(path = path)

    /** 判断文件是否可读。 */
    override fun isReadableFile(path: String): Boolean = fileManager.isReadableFileAtPath(path = path)

    /** 返回 App Documents 下的导入目录。 */
    override fun sandboxImportDirectoryPath(): String? {
        val documentsUrl: NSURL = fileManager.URLsForDirectory(
            directory = NSDocumentDirectory,
            inDomains = NSUserDomainMask,
        ).firstOrNull() as? NSURL ?: return null
        val documentsPath: String = documentsUrl.path ?: return null
        return "$documentsPath/$IOS_IMPORTED_AUDIO_DIRECTORY"
    }

    /** 确保目录存在。 */
    override fun ensureDirectory(path: String): Boolean {
        return fileManager.createDirectoryAtPath(
            path = path,
            withIntermediateDirectories = true,
            attributes = null,
            error = null,
        )
    }

    /** 把路径转换成标准 file URL 字符串。 */
    override fun fileUrlString(path: String): String {
        return NSURL.fileURLWithPath(path = path).absoluteString ?: path
    }

    /** 复制文件到目标路径。 */
    override fun copyFile(sourcePath: String, destinationPath: String): Boolean {
        return fileManager.copyItemAtPath(
            srcPath = sourcePath,
            toPath = destinationPath,
            error = null,
        )
    }

    /** 移动临时文件到最终路径。 */
    override fun moveFile(sourcePath: String, destinationPath: String): Boolean {
        return fileManager.moveItemAtPath(
            srcPath = sourcePath,
            toPath = destinationPath,
            error = null,
        )
    }

    /** 删除指定路径。 */
    override fun removeFile(path: String): Boolean {
        return fileManager.removeItemAtPath(
            path = path,
            error = null,
        )
    }

    /** 判断路径是否在 App 的导入目录内。 */
    override fun isPathInAppSandbox(path: String): Boolean {
        val rootPath: String = sandboxImportDirectoryPath() ?: return false
        return path.isInsideRoot(rootPath = rootPath)
    }
}

/**
 * iOS 沙盒导入目录名称。
 */
internal const val IOS_IMPORTED_AUDIO_DIRECTORY: String = "KMPMusicImportedAudio"

// 判断路径是否在给定根目录内，避免简单前缀误把相邻目录算进来。
private fun String.isInsideRoot(rootPath: String): Boolean {
    val normalizedRootPath: String = rootPath.trimEnd('/')
    return this == normalizedRootPath || startsWith(prefix = "$normalizedRootPath/")
}
