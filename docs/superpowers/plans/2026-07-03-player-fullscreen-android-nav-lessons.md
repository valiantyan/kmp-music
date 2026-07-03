# 播放页全屏沉浸与 Android 底部导航栏经验总结

## 背景

这次任务把生产 KMP App 的手机播放页改成全屏、沉浸式 Now Playing 页面，并连续多轮修复 Android 三键导航模式下底部系统导航栏颜色不一致的问题。核心验收点不是“控件是否避开底部”，而是“播放页背景是否视觉上延伸到系统三键导航区域，不能出现独立白条或异色条”。

## 第一性原理

播放页沉浸式要同时满足两个条件：

- 背景必须在视觉上承接用户可见的整块屏幕，包括系统底部导航区域；这不等于 Compose 背景一定能物理绘制到系统栏。
- 交互控件必须避开系统三键导航，不能把喜欢、倍速、进度或主控制按钮压到系统按钮下面。

这意味着 `navigationBarsPadding()` 本身不是错误。它应该用于内容避让；真正需要单独处理的是 Android 系统栏背景。Compose 页面背景只能画在 Compose 根视图里，而三键导航栏在部分 Android ROM 上由 SystemUI/Window 绘制，不一定能透出 Compose 背景。正确目标是让系统栏颜色与播放页底色一致，形成连续背景。

## 关键发现

- `PlayerScreen` 的背景层已经 `fillMaxSize()`，内容列使用 `statusBarsPadding()` 和 `navigationBarsPadding()` 避让系统栏，这是合理结构。
- 真机 `uiautomator dump` 显示 Compose 根节点没有覆盖完整 2400px 截图高度；底部系统区域不是普通 Compose 子树。
- `dumpsys window` 显示三键导航栏由 `NavigationBar0` / `ITYPE_NAVIGATION_BAR` 负责，属于系统栏区域。
- 单纯关闭 `isNavigationBarContrastEnforced` 或设置透明导航栏，在当前 ColorOS / Android 12 三键导航环境下不能保证透出播放页背景。
- `window.navigationBarColor` 在设备上是有效的；失败原因不是 API 不生效，而是上一轮把导航栏写成固定浅暖色，没有跟随当前播放页封面取色背景。

## 走过的错误路径

### 1. 试图把问题当成 Compose 布局截断

一开始容易以为是 `navigationBarsPadding()` 导致播放页“没铺到底”。实际更精确的结论是：内容需要避让，背景视觉需要由系统栏颜色承接。删除底部 inset 只会把控件压到三键区，不能根治。

### 2. 试图用跨平台 expect/actual 桥解决

底部三键导航栏是 Android-only 问题，iOS 和 Desktop 不应该被迫感知 Android 系统栏细节。跨平台桥扩大了问题边界，后来按用户提醒撤回，改回 `androidMain/MainActivity.kt` 层处理。

### 3. 只关闭系统对比色或透明系统栏

`window.isNavigationBarContrastEnforced = false` 是必要兼容项，但不是充分条件。当前设备的三键导航区域仍会显示系统栏自己的背景。

### 4. 写死一个播放页导航栏颜色

固定浅暖色能消除白条，却无法通过人工验收，因为播放页背景会根据当前歌曲封面变成薄荷绿、灰蓝等不同颜色。底部系统栏必须跟随播放页实际 palette。

## 最终修复原则

最终修复只放在 Android Activity 层：

- `MainActivity` 继续使用 `enableEdgeToEdge()` 和 `WindowCompat.setDecorFitsSystemWindows(window, false)`。
- Android 根 Composable 读取 `MusicAppController.uiState`，判断是否处于 `SecondaryScreen.Player`。
- 进入播放页时复用播放页同一套 `rememberPlayerPagePalette()`，取当前歌曲的 `backgroundColor` 写入 `window.navigationBarColor`。
- 离开播放页时恢复 `MusicColors.Paper`，避免普通一级页面残留播放页底色。
- 颜色过渡使用与播放页背景一致的 260ms 动画，避免切歌或进出播放页时底部突变。
- 该逻辑只在 `androidMain` 中存在，不污染 `commonMain` 页面与领域模型。

当前关键代码位置：

- `composeApp/src/androidMain/kotlin/com/yanhao/kmpmusic/MainActivity.kt`
- `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/screen/PlayerScreen.kt`
- `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/components/CoverPaletteLoader.kt`
- `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/core/theme/CoverPalette.kt`

## 可复用验证方法

这类视觉问题不能只靠“看起来没问题”，至少要留下一个能抓住具体症状的反馈环。

推荐验证顺序：

1. 编译安装到同一台开启三键导航的 Android 设备。
2. `adb shell am force-stop com.yanhao.kmpmusic`，避免旧 Activity 状态影响窗口配置。
3. 用播放页 intent 或手动路径进入播放页；当前通知入口可用 `adb shell am start -n com.yanhao.kmpmusic/.MainActivity -a com.yanhao.kmpmusic.action.OPEN_PLAYER`。
4. 等待 1 到 2 秒，让 Coil 封面加载、palette 提取和系统栏颜色动画完成。
5. `adb shell screencap -p` 抓图，`uiautomator dump` 或 `dumpsys window` 辅助确认当前页面和系统栏边界。
6. 抽样比较播放页底部背景与系统导航栏空白区域像素，避开三键图标本身。
7. 返回一级页面后再次截图，确认系统导航栏恢复普通纸色。

本次最终验证数据只代表当前真机、当前三键导航模式和本次截图，不应当当作跨 ROM 的固定阈值；它的价值是证明修复前后同一环境里的断层明显收敛。

- 修复前播放页背景到系统栏色差约 `48.67`，肉眼表现为独立浅暖色条。
- 动态同步后播放页背景到系统栏色差约 `1.73`。
- 返回一级页面后页面底部到系统栏色差为 `0.0`，说明没有残留播放页底色。
- `./gradlew :composeApp:compileDebugKotlinAndroid :composeApp:installDebug` 通过。
- `git diff --check` 通过。

## 后续改同类问题的清单

- 先区分“内容避让”和“背景承接”，不要把两者混在一个 padding 里解决。
- Android 三键导航栏优先在 `androidMain` Activity/Window 层处理，不让 `commonMain` 感知平台系统栏。
- 如果页面背景来自封面、主题或动态状态，系统栏颜色必须绑定同一个状态源，不能写死近似色。
- 验证时必须覆盖进入播放页、切换歌曲、离开播放页三个路径。
- 对 ROM 差异敏感的系统栏修复，要在真机截图和 `dumpsys window` 上确认，不只看模拟器或桌面端。
- 如果未来允许播放页出现深色背景，必须同步重新判断 `isAppearanceLightNavigationBars`，否则三键图标对比度可能不够。
- 临时红/蓝探针必须带明确目标，验证后立即清理，并用 `rg` 检查残留。

## 对抗式审查

### 最可能翻车的点

1. **只在一首歌上验证。** 如果只验证默认薄荷绿或某一张封面，固定色方案会伪装成可用。修复必须验证当前歌曲背景变化后的系统栏同步。
2. **只验证播放页，不验证返回。** 系统栏颜色是 Activity 级状态，离开播放页不恢复会污染首页、收藏和我的页面。
3. **把 Android-only 问题抽到 common 层。** 这会扩大维护面，并让 iOS/Desktop 背上不存在的问题。
4. **把 `navigationBarsPadding()` 当成根因删掉。** 这样可能让底部操作按钮压到三键导航，属于用一个视觉问题换另一个交互问题。
5. **只看肉眼截图，不做像素检查。** 轻微色差和系统栏分层在不同屏幕亮度下不稳定，像素抽样能给出更可靠的回归信号。
6. **把本次设备数据当成绝对阈值。** 色差数值只适合同环境前后对比；换 ROM、换导航模式或换截图压缩链路时，应重新建立基线。

### 文档自修正结论

这份总结已经按上述风险补充了五个修正：

- 明确写出 `navigationBarsPadding()` 不是根因，内容避让仍要保留。
- 明确写出固定色方案失败的原因是没有跟随播放页动态 palette。
- 明确把返回一级页面后的系统栏恢复纳入验证闭环。
- 明确区分“视觉承接”和“Compose 物理绘制到系统栏”。
- 明确本次色差数据是同环境回归证据，不是跨设备验收阈值。
