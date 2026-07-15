package com.yanhao.kmpmusic.playback

import java.nio.file.Files
import java.nio.file.InvalidPathException
import java.nio.file.Path

/**
 * macOS AVFoundation bridge dylib 路径属性名，测试和 smoke 通过它显式指定产物。
 */
const val MACOS_AVFOUNDATION_BRIDGE_PATH_PROPERTY = "kmp.music.macos.avfoundation.bridge.path"

/**
 * macOS AVFoundation bridge smoke 工作目录属性名。
 */
const val MACOS_AVFOUNDATION_BRIDGE_SMOKE_DIR_PROPERTY = "kmp.music.macos.avfoundation.smoke.dir"

/**
 * Compose Desktop packaged app resources 目录属性名，由生成的 `.cfg` 在启动时注入。
 */
const val COMPOSE_APPLICATION_RESOURCES_DIR_PROPERTY = "compose.application.resources.dir"

/**
 * native 命令接受状态码。
 */
const val MACOS_AVFOUNDATION_NATIVE_STATUS_ACCEPTED = 0

/**
 * native 命令因文件缺失失败的状态码。
 */
const val MACOS_AVFOUNDATION_NATIVE_STATUS_MISSING_FILE = 10

/**
 * native 命令因格式或 URI 不支持失败的状态码。
 */
const val MACOS_AVFOUNDATION_NATIVE_STATUS_UNSUPPORTED_FORMAT = 11

/**
 * native 命令因权限不足失败的状态码。
 */
const val MACOS_AVFOUNDATION_NATIVE_STATUS_PERMISSION_DENIED = 12

/**
 * native 命令因 bridge 不可用失败的状态码。
 */
const val MACOS_AVFOUNDATION_NATIVE_STATUS_ENGINE_UNAVAILABLE = 13

/**
 * native 命令因未知错误失败的状态码。
 */
const val MACOS_AVFOUNDATION_NATIVE_STATUS_UNKNOWN = 14

/**
 * native 回调缺失文件错误码。
 */
const val MACOS_AVFOUNDATION_NATIVE_ERROR_MISSING_FILE = 0

/**
 * native 回调格式不支持错误码。
 */
const val MACOS_AVFOUNDATION_NATIVE_ERROR_UNSUPPORTED_FORMAT = 1

/**
 * native 回调权限不足错误码。
 */
const val MACOS_AVFOUNDATION_NATIVE_ERROR_PERMISSION_DENIED = 2

/**
 * native 回调 bridge 不可用错误码。
 */
const val MACOS_AVFOUNDATION_NATIVE_ERROR_ENGINE_UNAVAILABLE = 3

/**
 * native 回调未知错误码。
 */
const val MACOS_AVFOUNDATION_NATIVE_ERROR_UNKNOWN = 4

private const val MACOS_AVFOUNDATION_BRIDGE_LIBRARY_NAME = "kmp_music_macos_avfoundation_bridge"

/**
 * 打包产物内 macOS AVFoundation bridge dylib 文件名。
 */
const val MACOS_AVFOUNDATION_BRIDGE_LIBRARY_FILE_NAME = "libkmp_music_macos_avfoundation_bridge.dylib"

/**
 * 打包产物 resources 下存放 macOS AVFoundation bridge 的目录名。
 */
const val MACOS_AVFOUNDATION_BRIDGE_BUNDLED_RESOURCE_DIRECTORY = "macos-avfoundation"

/**
 * native bridge 库加载结果。
 */
sealed interface MacosAvFoundationNativeLibraryLoadResult {
    /** dylib 已加载，JNI 符号可用。 */
    data object Loaded : MacosAvFoundationNativeLibraryLoadResult

    /**
     * dylib 加载失败。
     *
     * @property reason 面向诊断的失败原因。
     */
    data class Failed(
        val reason: String,
    ) : MacosAvFoundationNativeLibraryLoadResult
}

/**
 * macOS AVFoundation native library 加载器。
 */
interface MacosAvFoundationNativeLibraryLoader {
    /** 加载 native bridge dylib，并返回可观测结果。 */
    fun load(): MacosAvFoundationNativeLibraryLoadResult

    companion object {
        /** 判断当前 JVM 是否运行在 macOS。 */
        fun isMacos(): Boolean {
            return System.getProperty("os.name").contains(other = "mac", ignoreCase = true)
        }
    }
}

/**
 * 解析 macOS AVFoundation native bridge 的生产加载路径。
 */
internal object MacosAvFoundationNativeLibraryPathResolver {
    /** 返回开发命令或 smoke 显式注入的 dylib 路径。 */
    fun resolveConfiguredBridgePath(): String? {
        val configuredPath: String? = System.getProperty(MACOS_AVFOUNDATION_BRIDGE_PATH_PROPERTY)
        if (configuredPath.isNullOrBlank()) {
            return null
        }
        return configuredPath
    }

    /** 返回 DMG / `.app` 打包资源中的 dylib 路径，缺失时交给 JVM library path 兜底。 */
    fun resolveBundledBridgePath(): String? {
        val resourcesDir: String? = System.getProperty(COMPOSE_APPLICATION_RESOURCES_DIR_PROPERTY)
        if (resourcesDir.isNullOrBlank()) {
            return null
        }
        return resolveBundledBridgePath(resourcesDir = resourcesDir)
    }

    /** 从指定 resources 目录解析 bridge，供测试模拟 packaged app 环境。 */
    internal fun resolveBundledBridgePath(resourcesDir: String): String? {
        return try {
            val candidate: Path = Path.of(resourcesDir)
                .resolve(MACOS_AVFOUNDATION_BRIDGE_BUNDLED_RESOURCE_DIRECTORY)
                .resolve(MACOS_AVFOUNDATION_BRIDGE_LIBRARY_FILE_NAME)
            if (Files.isRegularFile(candidate)) {
                candidate.toAbsolutePath().toString()
            } else {
                null
            }
        } catch (error: InvalidPathException) {
            null
        } catch (error: SecurityException) {
            null
        }
    }
}

/**
 * 生产环境 native bridge 加载器，优先使用显式路径，再使用 `.app` 资源内 dylib，最后回退到 JVM library path。
 */
object SystemMacosAvFoundationNativeLibraryLoader : MacosAvFoundationNativeLibraryLoader {
    // 成功加载后缓存结果，避免同一 classloader 重复加载 dylib。
    @Volatile
    private var isLoaded: Boolean = false

    /** 加载 macOS AVFoundation bridge dylib。 */
    override fun load(): MacosAvFoundationNativeLibraryLoadResult {
        if (!MacosAvFoundationNativeLibraryLoader.isMacos()) {
            return MacosAvFoundationNativeLibraryLoadResult.Failed(reason = "当前平台不是 macOS")
        }
        if (isLoaded) {
            return MacosAvFoundationNativeLibraryLoadResult.Loaded
        }
        return synchronized(lock = this) {
            if (isLoaded) {
                MacosAvFoundationNativeLibraryLoadResult.Loaded
            } else {
                loadOnce()
            }
        }
    }

    /** 执行一次真实加载，并把 JVM 异常压缩成诊断文本。 */
    private fun loadOnce(): MacosAvFoundationNativeLibraryLoadResult {
        return try {
            val configuredPath: String? = MacosAvFoundationNativeLibraryPathResolver.resolveConfiguredBridgePath()
            val bundledPath: String? = MacosAvFoundationNativeLibraryPathResolver.resolveBundledBridgePath()
            if (!configuredPath.isNullOrBlank()) {
                System.load(configuredPath)
            } else if (!bundledPath.isNullOrBlank()) {
                System.load(bundledPath)
            } else {
                System.loadLibrary(MACOS_AVFOUNDATION_BRIDGE_LIBRARY_NAME)
            }
            isLoaded = true
            MacosAvFoundationNativeLibraryLoadResult.Loaded
        } catch (error: UnsatisfiedLinkError) {
            MacosAvFoundationNativeLibraryLoadResult.Failed(reason = error.message ?: error.javaClass.simpleName)
        } catch (error: SecurityException) {
            MacosAvFoundationNativeLibraryLoadResult.Failed(reason = error.message ?: error.javaClass.simpleName)
        }
    }
}
