package com.yanhao.kmpmusic.data

import kotlin.math.absoluteValue

/**
 * 根据源文件路径和文件名生成稳定沙盒导入路径，重复导入不会覆盖其他同名文件。
 */
internal fun buildImportedAudioPath(
    importDirectoryPath: String,
    sourcePath: String,
    fileName: String,
): String {
    val sanitizedFileName: String = sanitizeFileName(fileName = fileName)
    val baseName: String =
        sanitizedFileName
            .substringBeforeLast(
                delimiter = ".",
                missingDelimiterValue = sanitizedFileName,
            ).ifBlank { "audio" }
    val extension: String =
        sanitizedFileName.substringAfterLast(
            delimiter = ".",
            missingDelimiterValue = "",
        )
    val stableSuffix: Long = sourcePath.hashCode().toLong().absoluteValue
    val importedFileName: String =
        if (extension.isBlank()) {
            "$baseName-$stableSuffix"
        } else {
            "$baseName-$stableSuffix.$extension"
        }
    return "${importDirectoryPath.trimEnd('/')}/$importedFileName"
}

// 清理文件名中的路径分隔和空白，避免外部路径影响沙盒目录结构。
private fun sanitizeFileName(fileName: String): String =
    fileName
        .replace(oldChar = '/', newChar = '_')
        .replace(oldChar = ':', newChar = '_')
        .trim()
        .ifBlank { "audio" }
