package com.yanhao.kmpmusic.feature.desktop.components

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * 桌面输入框清除动作显示规则测试，避免普通输入框误显示搜索清除按钮。
 */
class DesktopTextInputDisplayModelTest {
    @Test
    fun clearActionStaysHiddenWhenFeatureIsDisabled(): Unit {
        assertFalse(
            actual = shouldShowDesktopTextInputClearAction(
                value = "music",
                isClearEnabled = false,
            ),
        )
    }

    @Test
    fun enabledClearActionOnlyShowsWhenInputHasText(): Unit {
        assertFalse(
            actual = shouldShowDesktopTextInputClearAction(
                value = "",
                isClearEnabled = true,
            ),
        )
        assertTrue(
            actual = shouldShowDesktopTextInputClearAction(
                value = "music",
                isClearEnabled = true,
            ),
        )
    }
}
