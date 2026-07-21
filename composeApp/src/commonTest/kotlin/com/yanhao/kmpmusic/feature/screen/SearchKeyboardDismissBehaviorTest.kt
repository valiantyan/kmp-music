package com.yanhao.kmpmusic.feature.screen

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * 搜索页键盘收起规则测试，避免滚动修复退化成输入或程序滚动都抢焦点。
 */
class SearchKeyboardDismissBehaviorTest {
    @Test
    fun userVerticalScrollDismissesSearchKeyboard() {
        assertTrue(
            actual =
                shouldDismissSearchKeyboardOnScroll(
                    isUserInput = true,
                    isSearchInputFocused = true,
                    horizontalDelta = 0f,
                    verticalDelta = 12f,
                ),
        )
        assertTrue(
            actual =
                shouldDismissSearchKeyboardOnScroll(
                    isUserInput = true,
                    isSearchInputFocused = true,
                    horizontalDelta = 0f,
                    verticalDelta = -8f,
                ),
        )
    }

    @Test
    fun nonUserOrHorizontalScrollKeepsSearchKeyboard() {
        assertFalse(
            actual =
                shouldDismissSearchKeyboardOnScroll(
                    isUserInput = false,
                    isSearchInputFocused = true,
                    horizontalDelta = 0f,
                    verticalDelta = 12f,
                ),
        )
        assertFalse(
            actual =
                shouldDismissSearchKeyboardOnScroll(
                    isUserInput = true,
                    isSearchInputFocused = true,
                    horizontalDelta = 12f,
                    verticalDelta = 0f,
                ),
        )
    }

    @Test
    fun horizontalDominantOrUnfocusedScrollKeepsSearchKeyboard() {
        assertFalse(
            actual =
                shouldDismissSearchKeyboardOnScroll(
                    isUserInput = true,
                    isSearchInputFocused = true,
                    horizontalDelta = 20f,
                    verticalDelta = 2f,
                ),
        )
        assertFalse(
            actual =
                shouldDismissSearchKeyboardOnScroll(
                    isUserInput = true,
                    isSearchInputFocused = false,
                    horizontalDelta = 0f,
                    verticalDelta = 12f,
                ),
        )
    }

    @Test
    fun verticalScrollMustExceedDismissThresholdAndDominance() {
        assertFalse(
            actual =
                shouldDismissSearchKeyboardOnScroll(
                    isUserInput = true,
                    isSearchInputFocused = true,
                    horizontalDelta = 0f,
                    verticalDelta = 2f,
                ),
        )
        assertFalse(
            actual =
                shouldDismissSearchKeyboardOnScroll(
                    isUserInput = true,
                    isSearchInputFocused = true,
                    horizontalDelta = 3f,
                    verticalDelta = 3f,
                ),
        )
        assertTrue(
            actual =
                shouldDismissSearchKeyboardOnScroll(
                    isUserInput = true,
                    isSearchInputFocused = true,
                    horizontalDelta = 0f,
                    verticalDelta = 2.1f,
                ),
        )
    }
}
