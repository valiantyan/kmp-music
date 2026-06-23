package com.yanhao.kmpmusic.feature.app

/**
 * 平台权限设置入口，避免 common 控制器直接依赖 Android Intent。
 */
fun interface PermissionSettingsOpener {
    /**
     * 打开当前 App 的系统权限设置页。
     */
    fun openPermissionSettings()
}
