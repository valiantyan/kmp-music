# KMP Music Agent 指南

这是给 AI coding agent 的项目地图，不是完整百科。修改前先读本文件，再按任务读取相关源码；遇到不确定的产品取舍或大范围架构调整，先问用户。

## 工作原则

- 修复问题要追求根治，不要为了局部现象打补丁；如果根治会明显扩大改动范围，先说明取舍并确认。
- 优先维护真实 KMP App。除非任务明确要求，不要修改 `prototypes/kmp-music-hi-fi` 来解决生产 App 问题。
- 查找代码和文件优先用 `rg` 或 `rg --files`。
- 不要回滚用户未要求回滚的改动；遇到相关的未提交改动，先读懂再协同处理。

## 改动前自检

- 这个问题是否已有文档或源码给出答案？先查本文件、`docs/PRD.md` 和相关目录源码；原型视觉问题再查 `prototypes/kmp-music-hi-fi/AGENTS.md`。
- 能否通过共享 token、组件、控制器或接口边界根治，而不是在单个页面补丁式修复？
- 改动是否触及导航、播放、队列、收藏、搜索或平台能力？如果是，同步考虑测试和架构边界。

## 常用命令

- Android 编译：`./gradlew :composeApp:compileDebugKotlinAndroid`
- 生成 debug APK：`./gradlew :composeApp:assembleDebug`
- 桌面端测试：`./gradlew :composeApp:desktopTest`
- 快速验证共享逻辑和 Android 编译：`./gradlew :composeApp:compileDebugKotlinAndroid :composeApp:desktopTest`
- 安装到已连接 Android 设备：`./gradlew :composeApp:installDebug`
- 查看任务：`./gradlew :composeApp:tasks`

提交前至少运行与改动范围匹配的命令；不确定任务是否存在时先查 `:composeApp:tasks`，不要猜任务名。

## 项目地图

- 技术栈：Kotlin Multiplatform `2.0.21`、Compose Multiplatform `1.7.3`、AGP `8.13.2`、Android `minSdk 24` / `targetSdk 36`、JVM target `17`。
- 主模块：`:composeApp`；包名与 `applicationId`：`com.yanhao.kmpmusic`。
- `docs/PRD.md`：产品范围、信息架构、MVP 边界和验收标准。
- `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/domain`：模型、Repository 接口、UseCase。
- `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/data`：当前阶段的内存/mock 实现。
- `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/core/theme`：主题、颜色、尺寸、封面调色等 token。
- `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/app`：全局 App 状态、导航、chrome、控制器。
- `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/components`：复用 UI 组件。
- `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/screen`：页面级 Composable。
- `composeApp/src/androidMain`、`iosMain`、`desktopMain`：平台入口和平台适配。
- `composeApp/src/commonTest`：共享逻辑测试；主要关注控制器、状态、主题算法。
- `prototypes/kmp-music-hi-fi`：高保真视觉参考，有自己的 `AGENTS.md`；不是生产入口，不能用 WebView 包装代替原生 UI。

## 架构边界

- 保持 `core / domain / data / feature` 分层；UI 不直接依赖平台实现。
- `commonMain` 承载共享 UI、状态、domain、mock data、主题和导航；平台目录只放平台入口、权限、媒体扫描、播放等适配。
- 新增数据能力时先定义 Repository 或 UseCase 接口，再写 `Impl` 实现。
- 平台媒体扫描、真实播放、通知、权限等能力通过接口、`expect/actual` 或平台 data source 接入，不污染 `domain`。
- 不为一次性调用过早抽象；只有抽象能降低真实复杂度、复用已有模式或隔离平台差异时才新增。

## UI 与状态规则

- 手机 UI 以高保真音乐 App 原型为视觉源头；布局、密度、圆角、阴影、颜色、字体层级和内容顺序尽量贴近原型。
- 一级页面只有 `首页 / 收藏 / 我的`，显示底部 Tab；搜索、播放页、专辑页、歌手页、设置、登录、本地文件夹等属于二级页面。
- 二级页面隐藏底部 Tab；迷你播放器在二级页面贴齐底部。一级页面中迷你播放器与底部 Tab 之间不要留缝，内容不要被 chrome 遮挡。
- 迷你播放器是全局 chrome，不要在各页面重复实现；全局当前播放歌曲在所有列表中同步为红色文本，并保留播放中辅助标识。
- 设计 token 优先放在 `MusicTheme.kt`，页面和组件使用共享 token；封面和插画优先复用 `composeResources/drawable` 中的原型资源。
- 视觉大改后尽量用真机、模拟器或桌面截图核对关键页面；无法截图时在最终说明中标明剩余视觉风险。

## Kotlin 与 Compose

- 用不可变 `data class` 表达 UI state 和领域模型，通过 `copy` 更新状态。
- 页面负责布局编排，组件负责复用 UI，控制器负责状态变化；保持 Composable 小而命名明确。
- 公共函数、复杂私有函数、接口和模型保留简洁 KDoc；不要写空洞注释。
- 优先早返回，避免含义不清的裸布尔/裸 `null` 参数；必要时用枚举、命名参数或小类型。
- 固定格式 UI 如底部栏、迷你播放器、封面网格、图标按钮要有稳定尺寸，避免状态变化导致布局跳动。

## 测试与提交

- 改动 `MusicAppController`、导航、播放状态、队列、收藏、搜索时，更新 `MusicAppControllerTest` 或新增共享测试。
- 测试优先覆盖用户可感知规则：一级/二级导航、当前播放与队列同步、收藏同步、搜索过滤、Tab 切换清空二级页面。
- UI 大改后至少运行 Android 编译；涉及共享状态时同时运行 `:composeApp:desktopTest`。
- 提交前看 `git status --short --branch`，避免提交 `.scratch/`、构建产物、IDE 状态、日志、Node 依赖、原型 dist、APK/DMG 或本地缓存。
- 提交信息用简洁英文祈使句，例如 `Improve mobile prototype fidelity`。

## 禁止事项

- 不要把真实媒体扫描或播放逻辑直接塞进 UI 层。
- 不要在 `commonMain` 引入 Android、iOS 或 Desktop 专属 API。
- 不要硬编码 secrets、token、私有路径或本机账号信息。
- 不要为了局部视觉问题在多个页面复制补丁；优先修正共享 token、组件或全局 chrome。
- 不要删除失败测试来“修复”构建，除非用户明确要求移除该行为。
- 不要在没有验证的情况下声称构建成功；如果无法运行测试或构建，要明确说明原因。
