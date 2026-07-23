package com.yanhao.kmpmusic

import javax.swing.JRootPane

/**
 * 判断当前宿主是否为 macOS，避免把 traffic lights 语义套到其他桌面系统。
 */
internal fun isMacosHost(): Boolean =
    System.getProperty("os.name").contains(
        other = "mac",
        ignoreCase = true,
    )

/**
 * 启用 macOS 原生 traffic lights，并让 Compose 内容延伸至透明标题栏。
 */
internal fun JRootPane.applyMacosNativeTitleBar() {
    putClientProperty("apple.awt.fullWindowContent", true)
    putClientProperty("apple.awt.transparentTitleBar", true)
    putClientProperty("apple.awt.windowTitleVisible", false)
}

/**
 * 还原标题栏属性，避免窗口销毁后影响同进程创建的后续窗口。
 */
internal fun JRootPane.clearMacosNativeTitleBar() {
    putClientProperty("apple.awt.fullWindowContent", false)
    putClientProperty("apple.awt.transparentTitleBar", false)
    putClientProperty("apple.awt.windowTitleVisible", true)
}
