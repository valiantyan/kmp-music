package com.yanhao.kmpmusic.feature.app.layout

import com.yanhao.kmpmusic.feature.app.RootTab
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * 手机一级页状态栏策略测试，锁住列表页白底遮罩和我的页沉浸式差异。
 */
class MobileContentLayoutTest {
    /**
     * 首页和收藏页会滚动列表内容，必须保留白底遮罩挡住状态栏区域。
     */
    @Test
    fun topLevelListPagesRenderStatusBarBackground() {
        assertTrue(actual = shouldRenderTopLevelStatusBarBackground(rootTab = RootTab.Home))
        assertTrue(actual = shouldRenderTopLevelStatusBarBackground(rootTab = RootTab.Favorites))
    }

    /**
     * 我的页需要全屏沉浸式背景，不应被一级页白底遮罩覆盖状态栏。
     */
    @Test
    fun mePageSkipsStatusBarBackgroundForImmersiveLayout() {
        assertFalse(actual = shouldRenderTopLevelStatusBarBackground(rootTab = RootTab.Me))
    }
}
