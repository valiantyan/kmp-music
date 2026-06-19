# KMP Music Agent 指南

本文件是当前仓库给 AI coding agent 的项目说明书。修改代码前先读这里，再读相关源码；不要只靠猜测或局部补丁推进。

## 常用命令

- 编译 Android debug：`./gradlew :composeApp:compileDebugKotlinAndroid`
- 生成可安装 APK：`./gradlew :composeApp:assembleDebug`
- 运行桌面端测试：`./gradlew :composeApp:desktopTest`
- 快速验证共享逻辑和 Android 编译：`./gradlew :composeApp:compileDebugKotlinAndroid :composeApp:desktopTest`
- 安装到已连接 Android 设备：`./gradlew :composeApp:installDebug`
- 查看可用任务：`./gradlew :composeApp:tasks`

提交前至少运行与改动范围匹配的 Gradle 命令。改动共享状态、导航、用例或 UI 时，优先运行快速验证命令。

查找代码和文件时优先使用 `rg` 或 `rg --files`。不确定某个 Gradle 任务是否存在时，先运行 `./gradlew :composeApp:tasks`，不要猜任务名。

## 项目技术栈

- Kotlin Multiplatform，Kotlin `2.0.21`
- Compose Multiplatform `1.7.3`
- Android Gradle Plugin `8.13.2`
- 主模块：`:composeApp`
- 包名与 applicationId：`com.yanhao.kmpmusic`
- Android：`minSdk 24`，`targetSdk 36`，JVM target `17`
- Desktop 入口：`com.yanhao.kmpmusic.DesktopMainKt`

这个项目是从 `prototypes/kmp-music-hi-fi` 高保真原型迁移来的真实 KMP App。第一优先级是原生 Compose UI 与原型体验保持一致，不使用 WebView。

## 目录结构

- `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/domain`：领域模型、Repository 接口、UseCase。
- `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/data`：当前阶段的内存/mock 数据实现。
- `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/core/theme`：主题、颜色、尺寸、缩放 token。
- `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/app`：全局 App 状态、导航、chrome、控制器。
- `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/components`：可复用 UI 组件。
- `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/screen`：页面级 Composable。
- `composeApp/src/androidMain`：Android Activity、资源和平台配置。
- `composeApp/src/iosMain`、`composeApp/src/desktopMain`：平台入口和占位实现。
- `composeApp/src/commonTest`：共享逻辑测试。
- `prototypes/kmp-music-hi-fi`：高保真 HTML/React 原型和视觉参考，不是生产 App 入口。

除非任务明确要求修改原型，否则不要改 `prototypes/kmp-music-hi-fi` 来解决真实 App 问题。

## 架构规则

- 保持 `core / domain / data / feature` 分层。跨层依赖走接口，不让 UI 直接依赖平台实现。
- `commonMain` 承载共享 UI、状态、domain、mock data、主题和导航；平台目录只放平台入口、权限、媒体扫描/播放适配等平台相关代码。
- 新增数据能力时先定义 Repository 或 UseCase 接口，再写 `Impl` 实现。
- 平台媒体扫描、真实播放、通知、权限等后续能力应通过接口、`expect/actual` 或平台 data source 接入，不污染 `domain`。
- 不为一次性调用过早抽象；只有当抽象降低真实复杂度、复用已有模式或隔离平台差异时才新增。
- 修复问题时追求根治。若根治会大幅改架构或不确定产品取舍，先向用户确认。

## UI/UX 规则

- 手机 UI 以用户选定的高保真音乐 App 原型为视觉源头；布局、密度、圆角、阴影、颜色、字体层级和内容顺序都要尽量贴近原型。
- 只有 `首页 / 收藏 / 我的` 是一级页面并显示底部 Tab。搜索、播放页、专辑页、歌手页、设置、登录、本地文件夹等都属于二级页面。
- 二级页面隐藏底部 Tab；迷你播放器在二级页面必须贴齐页面底部。
- 一级页面中迷你播放器与底部 Tab 之间不要留缝，页面内容不要被底部 chrome 遮挡。
- 全局当前播放歌曲在所有列表中都要同步为红色文本，并保留播放中辅助标识。
- 迷你播放器是全局 chrome，不属于单个页面；不要在各页面重复实现它。
- 设计 token 优先放在 `MusicTheme.kt`，页面和组件使用共享 token，不要散落魔法数字。
- 图片封面和插画优先复用 `composeResources/drawable` 中的原型资源。
- 不要把页面做成营销落地页；这是本地音乐 App，首屏应是可用的产品界面。
- 视觉改动完成后，尽量用真机、模拟器或桌面预览截图核对关键页面；无法截图时要在最终说明中明确剩余视觉风险。

## Kotlin 与 Compose 代码风格

- 使用不可变 `data class` 表达 UI state 和领域模型，通过 `copy` 产生新状态。
- Composable 尽量小而命名明确。页面负责布局编排，组件负责可复用 UI，控制器负责状态变化。
- 公共函数、复杂私有函数、接口和模型保留简洁 KDoc；不要写“赋值给变量”这类空注释。
- 优先使用早返回降低嵌套。
- 避免使用布尔或含义不清的裸参数制造难读调用；必要时使用枚举、命名参数或小类型。
- 颜色、字号、尺寸、间距使用主题 token 或局部可读常量；不要在多个页面复制同一数值。
- 保持 Compose 布局稳定：固定格式 UI 如底部栏、迷你播放器、封面网格、图标按钮要有稳定尺寸，避免文字或状态变化导致跳动。

## 测试要求

- 改动 `MusicAppController`、导航、播放状态、队列、收藏、搜索时，更新 `MusicAppControllerTest` 或新增共享测试。
- 测试优先验证用户可感知规则，例如：
  - 一级/二级导航状态。
  - 当前播放歌曲和队列同步。
  - 收藏状态同步到列表。
  - 搜索范围过滤。
  - Tab 切换清空二级页面。
- UI 大改后至少运行 Android 编译；涉及共享状态时同时运行桌面测试。
- 不要删除失败测试来“修复”构建，除非用户明确要求移除该行为。

## Git 与提交

- 提交前查看 `git status --short --branch`，确认没有误提交临时文件。
- `.scratch/`、构建产物、IDE 状态、日志、Node 依赖和原型 dist 不应提交。
- 提交信息用简洁英文祈使句，例如 `Improve mobile prototype fidelity`。
- 不要回滚用户未要求回滚的改动。遇到不属于当前任务但影响修改的文件，先读懂再协同处理。

## 禁止事项

- 不要用 WebView 包装原型来冒充真实 KMP App。
- 不要把真实媒体扫描或播放逻辑直接塞进 UI 层。
- 不要在 `commonMain` 引入 Android、iOS 或 Desktop 专属 API。
- 不要硬编码 secrets、token、私有路径或本机账号信息。
- 不要提交 APK、DMG、build 目录、截图调试目录或本地缓存。
- 不要把本地绝对路径写进生产代码；文档中如需引用本机路径，应仅用于说明当前工作区。
- 不要为了局部视觉问题在多个页面打补丁；优先修正共享 token、组件或全局 chrome。
- 不要在没有验证的情况下声称构建成功；如果无法运行测试或构建，要明确说明原因。
