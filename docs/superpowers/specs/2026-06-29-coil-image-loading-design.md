# Coil 图片加载统一设计

## 背景

项目当前已经有统一的封面入口 `feature/components/CoverArtPainter.kt`，业务模型通过 `coverImageUri + CoverArt` 表达封面来源：扫描到的本地封面优先，应用内原型资源兜底。

但 UI 底层仍然存在两类不一致：

- Android 和 Desktop 通过平台 `actual` 手写解码本地封面，再返回 `Painter`。
- Android mini player 背景和 macOS 播放页背景使用 `imageResource(coverArtResource(song.coverArt))` 对内置兜底资源取色，忽略了真实扫描封面。

结果是同一首本地歌曲可能出现“封面显示真实图片，背景取色却来自默认封面”的体验偏差。Coil 3 已支持 Compose Multiplatform，适合作为 Compose UI 图片加载和图片驱动取色的统一入口。

## 目标

- Compose UI 内所有图片显示都通过 Coil 加载。
- 图片驱动的 UI 取色也通过同一套 Coil 封面来源加载，确保显示封面和背景 palette 来源一致。
- 保留 `coverImageUri` 优先、`CoverArt` 兜底的业务语义。
- 保留现有页面和业务模型的调用心智，不把 Coil 调用散落到各个页面。
- Android、iOS、Desktop 的 Compose UI 使用同一套共享封面门面。
- 取色或图片加载失败时稳定回退，不影响播放、导航和列表渲染。

## 非目标

- 不改造 `Song`、`Album`、`Artist` 等领域模型为新的通用 `ImageSource` 层级。
- 不把 Android Media3 通知和锁屏 artwork 展示纳入 Compose UI 图片加载约束；系统 UI 仍读取 Media3 metadata。
- 不新增在线图片、远程音乐封面、云端缓存或图片编辑能力。
- 不为了 Coil 接入重写本地扫描、embedded artwork 提取、播放或持久化流程。
- 不修改 `prototypes/kmp-music-hi-fi` 或其他原型目录来解决生产 App 图片加载问题。

## 平台与依赖

使用 Coil 3 的 Compose Multiplatform 能力。项目依赖集中放在 `gradle/libs.versions.toml` 和 `composeApp/build.gradle.kts`：

- `io.coil-kt.coil3:coil-compose`
- 如需网络加载，再按平台选择网络模块；本次需求只覆盖本地 URI 和 Compose resources，不把网络图片作为验收前提。

Coil 官方支持 Android、JVM、iOS、macOS、JavaScript 和 WASM。项目当前目标是 Android、iOS 和 Desktop；Windows 桌面由 Compose Desktop/JVM 目标覆盖。

## 封面来源模型

实现层新增轻量封面请求模型，只放在 UI/components 边界，不上升到 domain：

- `coverImageUri: String?`：扫描音频提取出的本地封面 URI，优先级最高。
- `fallbackCoverArt: CoverArt`：应用内兜底封面。
- `fallbackResourceUri: String`：由 `CoverArt` 映射到 Compose Multiplatform resource URI，供 Coil 加载兜底资源。

来源选择规则：

1. 如果 `coverImageUri` 非空，Coil 先加载该 URI。
2. 如果 `coverImageUri` 为空或加载失败，显示 `fallbackCoverArt` 对应的资源。
3. 取色使用同一套来源顺序，失败时回退到既有默认 palette。

该模型避免把 Coil 的 `ImageRequest`、资源路径和 fallback 细节泄漏到页面层。

## 组件设计

`feature/components/CoverArtPainter.kt` 从“Painter 工具”演进为“封面 UI 门面”。实现提供以下能力：

- `CoverArtImage(...)`：封装 Coil `AsyncImage`，用于列表、mini player、播放页、桌面页面等所有可见封面。
- `rememberCoverArtPainter(...)` 或兼容旧名的过渡函数：必要时为短期迁移保留，但内部也必须走 Coil，最终页面应优先使用 `CoverArtImage`。
- `rememberCoverPalette(...)`：通过 Coil `ImageLoader` 加载同源图片，得到可取色的 bitmap 后调用现有 palette 提取算法。
- `coverArtResourceUri(coverArt: CoverArt)`：集中维护 `CoverArt` 到 Compose resource URI 的映射。

页面层不直接调用 `AsyncImage`、`ImageRequest`、`ImageLoader` 或平台图片解码 API。所有封面显示和封面取色都经由上述门面。

## 数据流

```text
Song / Album / Artist
  -> coverImageUri + coverArt
  -> shared cover request builder
  -> Coil
     -> CoverArtImage 显示图片
     -> rememberCoverPalette 提取背景色
```

列表、专辑、歌手、播放器、桌面播放器等 UI 只传入领域模型已有的 `coverImageUri` 和 `coverArt`。封面请求构建、fallback、错误处理和缓存策略集中在组件层。

## 取色设计

Android mini player 背景和 macOS 播放页背景必须改为基于“实际显示封面”取色：

- 有 `song.coverImageUri` 时，优先加载本地扫描封面并取色。
- 本地扫描封面加载失败时，加载 `song.coverArt` 兜底资源并取色。
- 两者都失败时，回退当前默认 palette。

取色入口不应该直接使用 `imageResource(coverArtResource(...))`，因为这会绕过真实扫描封面。取色状态需要与歌曲身份、`coverImageUri` 和 `coverArt` 绑定，避免切歌后复用上一首 palette。

## Android Media3 通知边界

Android Media3 通知、锁屏和外部媒体控制界面由系统 UI 展示，不属于 Compose UI 图片加载链路。

App 侧仍负责把 artwork 数据写入 Media3 metadata。当前 `AndroidPlaybackMediaMetadataAssets` 从 Compose resources assets 读取兜底封面字节，这条链路在本次改造中保留。若以后让通知也优先使用扫描封面，应复用同一套“扫描封面优先、资源兜底”的来源选择规则，但不通过 Coil `AsyncImage` 渲染。

## 错误处理

- `coverImageUri` 为空：直接显示兜底资源。
- `coverImageUri` 无权限、文件不存在、URI 无效或图片解码失败：显示兜底资源。
- 兜底资源加载失败：显示稳定的占位色或占位组件，并记录可诊断信息。
- 取色失败：回退 `MiniPlayerPalette` 或 `PlayerPagePalette` 的默认值，不影响封面显示。
- 图片加载错误不在页面层重复处理，避免每个页面写一份 fallback 逻辑。

## 迁移范围

需要纳入迁移的 UI：

- 手机首页、本地音乐、搜索、收藏、我的、专辑详情、歌手详情、播放页。
- 全局 mini player 和 queue / more overlay 中的封面。
- Desktop 首页、侧栏、底部播放器、播放详情页、收藏、搜索、专辑和歌手相关封面。
- Android mini player 背景取色。
- macOS Desktop 播放详情页背景取色。

保留但不纳入 Coil UI 约束的链路：

- Android Media3 通知和锁屏 metadata artwork 字节。
- 本地扫描阶段的 embedded artwork 提取与缓存写入。
- 纯业务测试中不涉及 Compose UI 渲染的封面枚举。

## 举一反三问题

后续实现、评审或排查时，以下问题必须能从本文档找到答案：

- 问：为什么不能只把 `coverArtPainter` 的平台实现替换成 Coil？
  答：只替换显示链路不能解决 mini player 和 macOS 播放页取色仍来自默认封面的问题；本设计要求显示和取色同源。
- 问：真实本地封面加载失败时谁负责 fallback？
  答：组件层封面门面负责，不由各页面分别处理。规则是 `coverImageUri` 优先，失败后使用 `CoverArt` resource URI。
- 问：页面能否直接调用 Coil 的 `AsyncImage`？
  答：不能。页面只使用共享封面门面，避免 Coil 请求构建、fallback 和错误处理分散。
- 问：取色能否继续使用 `imageResource(coverArtResource(...))`？
  答：不能用于当前歌曲背景取色，因为它绕过真实扫描封面。取色必须使用同一套 Coil 来源选择规则。
- 问：Android 通知和锁屏封面是否也必须用 Coil？
  答：不必须，也不属于 Compose UI 图片加载链路。系统 UI 通过 Media3 metadata 展示 artwork。
- 问：iOS 当前没有真实扫描封面时是否会受影响？
  答：不会。没有 `coverImageUri` 时走 `CoverArt` 兜底资源；Coil 接入只统一 UI 加载路径。
- 问：为什么不顺手把 `coverArt + coverImageUri` 改成 domain `ImageSource`？
  答：那会牵动领域模型、持久化、扫描合并和测试，超出本次“统一 UI 图片加载与取色”的目标。
- 问：Windows 是否在本设计覆盖范围内？
  答：覆盖 Desktop/JVM。Windows 桌面由 Compose Desktop/JVM 目标承载，不需要单独设计一套图片加载链路。

## 边界问题

- UI 边界：所有 Compose UI 图片显示和图片驱动取色都纳入 Coil；非 Compose 系统 UI 不纳入。
- Domain 边界：不修改 `Song`、`Album`、`Artist` 的封面字段结构，只在 UI/components 层建立轻量请求模型。
- Data 边界：不修改本地扫描器、embedded artwork 提取器和缓存写入策略；它们继续产出 `coverImageUri`。
- Playback 边界：不改动播放引擎、队列、播放模式和 Media3 session 行为。
- Notification 边界：Android Media3 通知/锁屏 artwork 字节链路保持现状，本次只保证不回归。
- Resource 边界：应用内兜底资源仍来自 `composeResources/drawable`，不从 prototype 目录或平台私有资源另取一套。
- Failure 边界：图片显示失败只能降级封面显示或取色 palette，不能阻断页面渲染、播放控制或导航。
- Test 边界：单元测试覆盖请求构建和来源规则；真机/截图验证覆盖视觉取色效果，不要求测试 Coil 内部实现。

## 三轮交叉审核记录

### 第一轮：范围与根因

- 问：设计是否解决了用户提出的“所有图片加载走 Coil”，而不是只改局部页面？
  答：是。所有 Compose UI 封面显示都经共享门面走 Coil，页面不直接依赖平台解码。
- 问：设计是否解决了 Android mini player 和 macOS 播放页取色与显示封面不一致的根因？
  答：是。取色入口和显示入口使用同一套来源选择规则。
- 问：是否把 Media3 通知/锁屏误纳入 Coil UI 渲染？
  答：没有。文档明确它是系统 UI metadata 链路，保持现状。

### 第二轮：架构边界

- 问：Coil 依赖是否会泄漏到每个页面？
  答：不会。页面只调用 `CoverArtImage` 和取色门面，Coil 请求构建集中在 components 层。
- 问：是否需要大改 domain 图片模型才能完成目标？
  答：不需要。保留 `coverImageUri + CoverArt`，只在 UI 边界增加请求模型。
- 问：是否会污染平台扫描、播放或持久化边界？
  答：不会。本设计不改扫描产物、不改播放引擎、不改数据库模型。

### 第三轮：失败与验证

- 问：本地 URI 失效、权限丢失或图片损坏时页面是否仍可用？
  答：可用。组件层统一回退到 `CoverArt` 兜底资源，取色失败回退默认 palette。
- 问：如何确认取色真的来自实际显示封面？
  答：共享测试覆盖显示请求和取色请求同源；视觉验证检查真实本地封面下 mini player 与 macOS 播放页背景变化。
- 问：如何确认迁移没有影响 Android 通知/锁屏封面？
  答：验收标准要求 Media3 通知/锁屏链路保持可用；实现阶段不修改该 metadata artwork 字节链路。

## 测试计划

新增或更新共享测试：

- 封面请求构建：`coverImageUri` 非空时作为主来源。
- 封面请求构建：`coverImageUri` 为空时使用 `CoverArt` resource URI。
- 资源 URI 映射覆盖所有 `CoverArt` 枚举。
- 取色请求与显示请求使用同一来源选择规则。

更新或新增 UI 相关可编译验证：

- Android 编译：`./gradlew :composeApp:compileDebugKotlinAndroid`
- 共享逻辑和桌面验证：`./gradlew :composeApp:desktopTest`

视觉验证：

- Android：检查 mini player 在本地歌曲真实封面下的背景色是否跟随封面变化。
- Desktop/macOS：检查播放详情页背景色是否跟随真实封面变化。
- 检查封面 URI 失效时列表、播放页和桌面页是否稳定显示兜底封面。

## 验收标准

- Compose UI 中不再依赖 Android/Desktop 手写平台图片解码来显示封面。
- 页面层不直接使用 Coil API；封面显示和取色通过共享组件门面完成。
- 本地歌曲有真实扫描封面时，封面显示和背景取色都来自该真实封面。
- 本地封面缺失或加载失败时，所有页面稳定显示 `CoverArt` 兜底资源。
- Android mini player 背景和 macOS 播放页背景不再只基于默认 `CoverArt` 取色。
- Android Media3 通知/锁屏展示链路保持可用，不因 UI 图片加载迁移回归。
- Android 编译和相关共享测试通过；无法完成的真机或截图验证需在交付说明中明确标注。

## 实施顺序

1. 增加 Coil 依赖和共享封面请求构建能力。
2. 用 Coil 实现共享 `CoverArtImage` 和兜底资源 URI 映射。
3. 将现有封面显示调用迁移到共享门面，移除或缩小平台手写解码 actual。
4. 增加 Coil 同源取色入口，迁移 Android mini player 和 macOS 播放页取色。
5. 补充请求构建和取色来源规则测试。
6. 运行 Android 编译、Desktop 测试，并做关键页面视觉核对。
