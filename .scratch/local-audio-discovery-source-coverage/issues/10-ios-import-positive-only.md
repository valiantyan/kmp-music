Status: ready-for-human

# 让 iOS P0 导入表达 positive-only 语义

## 父级需求

.scratch/local-audio-discovery-source-coverage/PRD.md

## 要做什么

让 iOS P0 新增导入文件的扫描结果表达为 positive-only：只添加或更新用户刚授权的文件来源，不声明整个 iOS 导入曲库已完整覆盖。

任务只处理 iOS 导入的累加语义，不实现 iOS P1 系统音乐资料库，也不证明播放 parity。

iOS 平台可在平台 source set 内继续使用安全作用域访问或等价来源引用；common 层只接收平台无关的来源引用和覆盖语义，不应把原始 `file://` 当作唯一长期可恢复来源。

## 验收标准

- [x] iOS 新增导入不会替换旧导入文件。
- [x] iOS 导入曲库累加测试通过。
- [x] iOS 导入结果不声明 `IosImportedFile` 整个 source kind 的删除权。
- [x] common `localUri` 不被写成必须依赖 raw `file://` 的假设。
- [x] 不引入 iOS P1 系统音乐资料库语义。

## 前置依赖

- .scratch/local-audio-discovery-source-coverage/issues/09-ios-import-accumulates-test.md

## Comments

实现摘要：
- 在 `IosFolderMusicScanner` 的扫描结果中显式声明 `completedCoverage = listOf(LocalMusicScanCoverage.PositiveOnly)`。
- 该结果只表达 iOS P0 新增导入的正向发现语义，不授予 `IosImportedFile` 整个 source kind 的删除权。
- 本轮没有修改 common `localUri` 模型，没有新增 raw `file://` 长期可恢复来源假设，也没有引入 iOS P1 系统音乐资料库或播放 parity 语义。

验证命令与结果：
- `./gradlew :composeApp:desktopTest --tests com.yanhao.kmpmusic.data.PersistentMusicLibraryRepositoryTest --tests com.yanhao.kmpmusic.domain.model.LocalMusicScanCoverageContractTest`：通过。
- `./gradlew :composeApp:desktopTest`：通过。
- `./gradlew :composeApp:compileKotlinIosSimulatorArm64`：未通过；Kotlin/Native 依赖下载完成后进入源码编译，失败在既有 commonMain Native 编译问题：`PlaybackDatabaseFactory.kt` 中 `Dispatchers.IO` 在 Native 不可访问，以及 `PlaybackSnapshotWriter.kt` 中 `synchronized` unresolved。本次 diff 未出现 `IosFolderMusicScanner` 相关编译错误。

对抗式审查结论：
- 逻辑漏洞：仅声明 `PositiveOnly`，仓库合并层不会把缺失的旧 iOS 导入误判为应下线。
- 事实正确性：复用既有 `LocalMusicScanCoverage.PositiveOnly` 契约，符合 iOS P0 导入累加模型。
- 更简单做法：没有新增模型、接口或测试 helper，只在 iOS scanner 结果边界补足覆盖语义。
- 越界风险：未实现 iOS P1、播放 parity、bookmark 持久化或文件复制策略；第一次 review 指出的 raw path 来源身份风险已撤回。
- 验证充分性：已跑聚焦仓库/覆盖契约测试和完整 `desktopTest`；iOS 编译仍受既有 Native 编译问题阻塞，作为剩余平台编译风险记录。

Code Review 结论：
- Standards：复审无发现；改动保留在 `iosMain` 平台 scanner，使用既有 domain 契约类型，没有引入 commonMain 平台 API 或 smell。
- Spec：复审无发现；diff 符合“只添加或更新正向结果、不声明整个 iOS 导入曲库完整覆盖”的要求，未触碰 common `localUri`，未引入 iOS P1。

剩余风险或未完成项：
- iOS simulator Kotlin 编译当前仍被既有 commonMain Native 兼容问题阻塞，需后续独立 issue 修复后才能获得完整 iOS 编译通过证据。
