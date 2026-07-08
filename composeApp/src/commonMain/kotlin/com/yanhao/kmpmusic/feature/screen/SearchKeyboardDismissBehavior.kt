package com.yanhao.kmpmusic.feature.screen

import kotlin.math.abs

// 搜索页滚动收键盘需要过滤触摸抖动，避免轻微斜向手势抢走输入焦点。
private const val SEARCH_KEYBOARD_DISMISS_VERTICAL_SLOP = 2f

// 用户主动上下拖动搜索内容时才收起键盘，避免程序滚动或横向手势抢走输入焦点。
internal fun shouldDismissSearchKeyboardOnScroll(
    isUserInput: Boolean,
    isSearchInputFocused: Boolean,
    horizontalDelta: Float,
    verticalDelta: Float,
): Boolean {
    if (!isUserInput || !isSearchInputFocused) {
        return false
    }
    val absoluteVerticalDelta: Float = abs(x = verticalDelta)
    val absoluteHorizontalDelta: Float = abs(x = horizontalDelta)
    return absoluteVerticalDelta > SEARCH_KEYBOARD_DISMISS_VERTICAL_SLOP &&
        absoluteVerticalDelta > absoluteHorizontalDelta
}
