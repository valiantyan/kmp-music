package com.yanhao.kmpmusic.playback

/**
 * macOS AVFoundation bridge dylib 路径属性名，测试和 smoke 通过它显式指定产物。
 */
const val MACOS_AVFOUNDATION_BRIDGE_PATH_PROPERTY = "kmp.music.macos.avfoundation.bridge.path"

/**
 * macOS AVFoundation bridge smoke 工作目录属性名。
 */
const val MACOS_AVFOUNDATION_BRIDGE_SMOKE_DIR_PROPERTY = "kmp.music.macos.avfoundation.smoke.dir"

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
 * 生产环境 native bridge 加载器，优先使用显式路径，回退到 JVM library path。
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
            val configuredPath: String? = System.getProperty(MACOS_AVFOUNDATION_BRIDGE_PATH_PROPERTY)
            if (configuredPath.isNullOrBlank()) {
                System.loadLibrary(MACOS_AVFOUNDATION_BRIDGE_LIBRARY_NAME)
            } else {
                System.load(configuredPath)
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
