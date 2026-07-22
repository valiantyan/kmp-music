package com.yanhao.kmpmusic.feature.desktop.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * 公共标题 Toolbar 规格测试，避免专辑和歌手页再次各自漂移。
 */
class DesktopPageTitleToolbarTest {
    /** Figma `1085:710` 的字号、行高、字重和间距必须由共享 owner 锁定。 */
    @Test
    fun visualSpecMatchesFigmaToolbarNode() {
        val visualSpec: DesktopPageTitleToolbarVisualSpec = resolveDesktopPageTitleToolbarVisualSpec()
        assertEquals(expected = Color(0xFF111C2D), actual = visualSpec.textColor)
        assertEquals(expected = 32.sp, actual = visualSpec.fontSize)
        assertEquals(expected = 40.sp, actual = visualSpec.lineHeight)
        assertEquals(expected = FontWeight.Medium, actual = visualSpec.fontWeight)
        assertEquals(expected = 32.dp, actual = visualSpec.bottomPadding)
    }
}
