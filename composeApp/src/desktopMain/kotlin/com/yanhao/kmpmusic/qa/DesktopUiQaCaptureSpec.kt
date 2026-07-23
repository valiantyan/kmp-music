package com.yanhao.kmpmusic.qa

import java.awt.Rectangle

/**
 * Desktop UI QA 自动取证使用的固定尺寸、时序、阈值和文件名。
 */
internal object DesktopUiQaCaptureSpec {
    /** 固定 QA 窗口宽度（1240px）。 */
    const val WINDOW_WIDTH: Int = 1240

    /** 固定 QA 窗口高度（824px）。 */
    const val WINDOW_HEIGHT: Int = 824

    /** AWT 输入事件间隔（40ms）。 */
    const val ROBOT_ACTION_DELAY_MILLIS: Int = 40

    /** 点击应用顶部非交互区时相对内容区域的纵向偏移。 */
    const val WINDOW_ACTIVATION_Y_OFFSET_PX: Int = 20

    /** 首页和歌手页滚动条中心距离窗口右边缘约 30px。 */
    const val HOME_ARTISTS_SCROLLBAR_DRAG_EDGE_INSET_PX: Int = 30

    /** 专辑页包含额外页面内边距，滚动条中心距离窗口右边缘约 45px。 */
    const val ALBUMS_SCROLLBAR_DRAG_EDGE_INSET_PX: Int = 45

    /** 单次自动拖动事件的垂直像素距离。 */
    const val SCROLLBAR_DRAG_STEP_PX: Int = 3

    /** 自动拖动使用 20 个小步，专门覆盖逐次取整丢失问题。 */
    const val SCROLLBAR_DRAG_STEP_COUNT: Int = 20

    /** 满五秒前的活动帧等待时间（4500ms），独立于生产延迟常量。 */
    const val SCROLLBAR_ACTIVE_CAPTURE_DELAY_MILLIS: Long = 4_500L

    /** 活动帧到满五秒后稳定帧的等待时间（1250ms）。 */
    const val SCROLLBAR_SETTLED_CAPTURE_DELAY_MILLIS: Long = 1_250L

    /** 播放动画第一段取证间隔（220ms）。 */
    const val FIRST_ANIMATION_FRAME_DELAY_MILLIS: Long = 220L

    /** 播放动画第二段取证间隔（310ms）。 */
    const val SECOND_ANIMATION_FRAME_DELAY_MILLIS: Long = 310L

    /** 空白图检测的采样网格边长。 */
    const val SAMPLE_GRID_SIZE: Int = 80

    /** 非空画面至少应包含的采样颜色数。 */
    const val MINIMUM_SAMPLED_COLORS: Int = 8

    /** 列表滚动前后至少变化的像素数。 */
    const val MINIMUM_SCROLL_CHANGED_PIXELS: Int = 1_000

    /** 滚动条显示与隐藏至少变化的像素数。 */
    const val MINIMUM_SCROLLBAR_CHANGED_PIXELS: Int = 40

    /** 仅滚动条隐藏时应用内容允许变化的最大像素数。 */
    const val MAXIMUM_SCROLLBAR_CHANGED_PIXELS: Int = 5_000

    /** 播放等高器相邻帧至少变化的像素数。 */
    const val MINIMUM_ANIMATION_CHANGED_PIXELS: Int = 10

    /** 跳过会随系统焦点改变的 macOS 原生标题栏。 */
    const val STABLE_CHROME_TOP: Int = 30

    /** 用于确认窗口身份的应用顶部稳定区域高度。 */
    const val STABLE_CHROME_HEIGHT: Int = 40

    /** 固定 shell 区域允许的少量抗锯齿变化像素数。 */
    const val MAXIMUM_STABLE_SHELL_CHANGED_PIXELS: Int = 500

    /** 专辑详情首次滚动后 macOS 标题栏文本会产生额外抗锯齿噪声，单独放宽但不影响其他场景。 */
    const val ALBUM_DETAIL_MAXIMUM_STABLE_SHELL_CHANGED_PIXELS: Int = 1_200

    /** 返回排除 macOS 原生标题栏后的场景应用内容区域。 */
    fun appContentRegion(scenario: DesktopUiQaScenario): Rectangle =
        Rectangle(
            0,
            STABLE_CHROME_TOP,
            scenario.windowWidth,
            scenario.windowHeight - STABLE_CHROME_TOP,
        )

    /** 返回滚动期间不应变化的顶部 chrome 与侧栏区域。 */
    fun stableShellRegions(scenario: DesktopUiQaScenario): List<Rectangle> =
        listOf(
            Rectangle(0, STABLE_CHROME_TOP, scenario.windowWidth, STABLE_CHROME_HEIGHT),
            Rectangle(0, 70, 240, scenario.windowHeight - 166),
        )

    /** 返回场景专属的 shell 抖动上限，避免宿主像素噪声掩盖页面滚动和滚动条验收。 */
    fun maximumStableShellChangedPixels(scenario: DesktopUiQaScenario): Int =
        if (scenario == DesktopUiQaScenario.AlbumDetail) {
            ALBUM_DETAIL_MAXIMUM_STABLE_SHELL_CHANGED_PIXELS
        } else {
            MAXIMUM_STABLE_SHELL_CHANGED_PIXELS
        }

    /** 返回覆盖目标列表右侧滚动条的场景验收区域。 */
    fun scrollbarChangeRegion(scenario: DesktopUiQaScenario): Rectangle =
        Rectangle(
            scenario.windowWidth - 70,
            120,
            70,
            scenario.windowHeight - 214,
        )

    /** 返回场景内当前歌曲均衡器的固定验收区域。 */
    fun playbackAnimationRegion(scenario: DesktopUiQaScenario): Rectangle =
        if (scenario == DesktopUiQaScenario.AlbumDetailPlaying) {
            Rectangle(260, 510, 64, 80)
        } else {
            Rectangle(980, 245, 64, 80)
        }

    /** 初始状态截图文件名。 */
    const val INITIAL_FRAME_FILE_NAME: String = "01-initial.png"

    /** 动态状态截图文件名。 */
    const val ACTIVE_FRAME_FILE_NAME: String = "02-active.png"

    /** 停止或后续动画状态截图文件名。 */
    const val SETTLED_FRAME_FILE_NAME: String = "03-settled.png"
}
