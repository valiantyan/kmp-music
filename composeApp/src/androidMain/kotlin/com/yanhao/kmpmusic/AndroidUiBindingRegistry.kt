package com.yanhao.kmpmusic

import com.yanhao.kmpmusic.domain.model.LocalMusicScanError
import com.yanhao.kmpmusic.domain.model.LocalMusicScanErrorType
import com.yanhao.kmpmusic.domain.model.LocalMusicScanException
import com.yanhao.kmpmusic.domain.model.LocalMusicDiscoveryPreferences
import com.yanhao.kmpmusic.domain.model.LocalMusicScanRequest
import com.yanhao.kmpmusic.domain.model.LocalMusicScanResult
import com.yanhao.kmpmusic.domain.model.LocalMusicSourceKind
import com.yanhao.kmpmusic.domain.repository.LocalMusicScanner
import com.yanhao.kmpmusic.feature.app.PermissionSettingsOpener

/**
 * 管理 Activity 生命周期相关的 Android UI 绑定，避免进程级会话持有过期引用。
 */
internal class AndroidUiBindingRegistry {
    // 当前进程级可替换 scanner 代理。
    private val mutableLocalMusicScanner: MutableLocalMusicScanner = MutableLocalMusicScanner()

    // 当前进程级可替换权限设置入口代理。
    private val mutablePermissionSettingsOpener: MutablePermissionSettingsOpener =
        MutablePermissionSettingsOpener()

    /**
     * 对控制器暴露的本地扫描器代理。
     */
    val localMusicScanner: LocalMusicScanner
        get() = mutableLocalMusicScanner

    /**
     * 对控制器暴露的权限设置入口代理。
     */
    val permissionSettingsOpener: PermissionSettingsOpener
        get() = mutablePermissionSettingsOpener

    /**
     * 绑定当前 Activity 可用的扫描器实现。
     */
    fun attachLocalMusicScanner(scanner: LocalMusicScanner) {
        mutableLocalMusicScanner.replace(scanner = scanner)
    }

    /**
     * 绑定当前 Activity 可用的权限设置入口实现。
     */
    fun attachPermissionSettingsOpener(opener: PermissionSettingsOpener) {
        mutablePermissionSettingsOpener.replace(opener = opener)
    }

    /**
     * 清空当前 Activity 绑定，避免泄漏已经失效的宿主引用。
     */
    fun clear() {
        mutableLocalMusicScanner.clear()
        mutablePermissionSettingsOpener.clear()
    }
}

/**
 * 进程级扫描器代理，让 Activity 重建后可以刷新真实平台实现。
 */
private class MutableLocalMusicScanner : LocalMusicScanner {
    // 当前委托 scanner，未注入前返回明确初始化错误。
    private var scanner: LocalMusicScanner = MissingAndroidLocalMusicScanner()

    /** 将扫描请求转发给当前已注入的 Android scanner。 */
    override suspend fun scan(request: LocalMusicScanRequest): LocalMusicScanResult {
        return scanner.scan(request = request)
    }

    /** 将带偏好的扫描请求转发给当前已注入的 Android scanner。 */
    override suspend fun scan(
        request: LocalMusicScanRequest,
        preferences: LocalMusicDiscoveryPreferences,
    ): LocalMusicScanResult {
        return scanner.scan(
            request = request,
            preferences = preferences,
        )
    }

    /** 替换当前 Activity 绑定的 scanner。 */
    fun replace(scanner: LocalMusicScanner) {
        this.scanner = scanner
    }

    /** 清空当前 Activity 绑定，避免进程级对象持有失效引用。 */
    fun clear() {
        scanner = MissingAndroidLocalMusicScanner()
    }
}

/**
 * 进程级权限设置入口代理，让 Activity 重建后可以刷新真实平台实现。
 */
private class MutablePermissionSettingsOpener : PermissionSettingsOpener {
    // 当前委托入口，未注入前保持空操作。
    private var opener: PermissionSettingsOpener = PermissionSettingsOpener {}

    /** 打开当前委托的系统权限设置页面。 */
    override fun openPermissionSettings() {
        opener.openPermissionSettings()
    }

    /** 替换当前 Activity 绑定的权限设置入口。 */
    fun replace(opener: PermissionSettingsOpener) {
        this.opener = opener
    }

    /** 清空当前 Activity 绑定，避免持有过期宿主。 */
    fun clear() {
        opener = PermissionSettingsOpener {}
    }
}

/**
 * 防御 UI 尚未完成注入就触发 Android 本地扫描的极端场景。
 */
private class MissingAndroidLocalMusicScanner : LocalMusicScanner {
    /** 返回明确错误，避免静默回退到 fake scanner。 */
    override suspend fun scan(request: LocalMusicScanRequest): LocalMusicScanResult {
        throw LocalMusicScanException(
            error = LocalMusicScanError(
                type = LocalMusicScanErrorType.Unknown,
                message = "Android 本地音乐扫描器尚未初始化",
                sourceKind = LocalMusicSourceKind.AndroidMediaStore,
            ),
        )
    }
}
