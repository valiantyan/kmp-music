# 10 — 打通 iOS 沙盒导入来源闭环

**What to build:** 让 iOS 导入后的本地音乐以 App 沙盒内可长期访问的来源进入曲库和播放队列，避免外部 Files URL 在 security scope 释放后继续被播放器消费。

**Blocked by:** None — can start immediately.

Status: ready-for-human

Labels: ready-for-human

- [x] iOS 导入流程在授权窗口内复制音频文件到 App 沙盒内的持久位置。
- [x] 进入曲库和播放队列的 `localUri` 只指向 App 沙盒内文件 URL。
- [x] 外部 Files URL 不进入播放队列；如果发现外部 URL，应被过滤、标记为问题或阻止进入可播放集合。
- [x] 重新导入、文件缺失、权限失效和复制失败都有清晰的用户可理解错误结果。
- [x] 重复导入、同名文件和复制中断不会覆盖已有可播放文件，也不会留下半成品进入曲库。
- [x] 现有 `AudioSource.Local` 和 `localUri` 语义保持不变，不新增 common 授权载体。
- [x] 测试覆盖导入后来源可播放、外部 URL 被拒绝、复制失败和曲库状态不被半成品污染。

## 对抗式审查

- 最可能翻车点一：实现只改扫描结果，不改复制生命周期，真实设备上仍播放外部授权已释放的 URL。修正要求：进入播放队列的 `localUri` 必须可证明来自 App 沙盒内持久文件。
- 最可能翻车点二：复制失败或中断后半成品被当成歌曲入库。修正要求：导入必须有临时态和提交态边界，失败时不污染曲库。
- 最可能翻车点三：为了处理权限把平台授权对象塞进 common。修正要求：首版仍使用现有 `localUri` / `AudioSource.Local`，权限生命周期留在 iOS 导入层。

## Comments

- 由 Apple 平台统一播放迁移 PRD 拆分而来。

### 2026-07-14 实现交付

实现摘要：
- 将 iOS 扫描入口改为导入闭环：`IosFolderMusicScanner` 只负责选择授权目录和请求校验，`IosSandboxAudioImporter` 在 security scope 存活窗口内执行导入。
- 新增 `IosImportFileSystem` 和 `IosSandboxAudioCommitter`，把外部 Files 音频复制到 App Documents 下 `KMPMusicImportedAudio` 目录，先写 `.importing` 临时文件，再提交最终文件。
- `MusicFileMetadata.localUri` 只由已提交的沙盒文件路径生成；外部 URL、不可读文件、缺失文件、复制失败和提交失败只进入 `LocalMusicProblem`，不进入可播放集合。
- 重复导入会复用既有可读沙盒副本，同名文件通过源路径稳定后缀区分，避免覆盖已有可播放文件。
- 未新增 common 授权载体，播放链路仍消费既有 `localUri` 和 `AudioSource.Local`。
- 为了让 iOS Native 编译和测试能覆盖本票，补齐了数据库查询上下文和播放快照同步块的 `expect/actual` 平台实现；Android/Desktop 仍使用原有 IO dispatcher 和 JVM 同步块，iOS 使用 `Dispatchers.Default` 与 Foundation 锁。

验证命令与结果：
- `./gradlew :composeApp:iosSimulatorArm64Test`：通过。
- `./gradlew :composeApp:desktopTest :composeApp:compileDebugKotlinAndroid`：通过。
- `./gradlew :composeApp:linkDebugFrameworkIosSimulatorArm64`：通过。
- `git diff --check`：通过。
- `./gradlew :composeApp:allTests`：未通过，失败点为 `PlaybackDatabaseMigrationsTest.migrationSevenToEightCreatesPlaylistTablesAndKeepsExistingRows` 在 Android unit 聚合环境中加载 `BundledSQLiteDriver` 时找不到 `sqliteJni`。该失败发生在测试运行环境的 SQLite JNI 依赖加载阶段，不是本票 iOS 导入逻辑回归；本票相关 iOS、Desktop 和 Android 编译门禁已通过。

对抗式审查结论：
- 外部 Files URL 残留进播放队列：已通过沙盒提交路径生成 `localUri`，并用测试覆盖外部提交路径被拒绝。
- 半成品污染曲库：已使用 `.importing` 临时文件和提交态边界，复制或提交失败只生成问题条目，测试覆盖复制失败不入库。
- 权限对象进入 common：未新增授权载体，security scope 只存在于 iOS 导入器内部，common 仍只看到 `localUri`。
- 重复导入或同名覆盖：已复用既有可读沙盒副本，并用源路径稳定后缀隔离同名文件。
- 为 iOS 编译补的 KMP 支撑是否越界：改动只隔离平台不可用 API，不改变 `AudioPlayerEngine`、`PlaybackCoordinator`、队列规则或 common 播放契约。

Code review 结论：
- Standards：子审查发现文件长度、常量文档、函数命名、测试空行和 iOS 同步 no-op 风险；已修复为小文件、补齐 KDoc/类型、动词命名、清理测试函数体空行，并将 iOS 同步 actual 改为 Foundation 锁。
- Spec：子审查未发现 10 号票阻塞性缺口；沙盒复制、沙盒 `localUri`、外部 URL 拒绝、缺失/权限/复制失败、半成品不入库和无 common 授权载体均有实现与测试覆盖。

剩余风险或未完成项：
- 尚未在真实 iOS 设备上用 Files App 进行人工导入播放 smoke；当前证据为 iOS simulator 自动化和 framework 链接。
- 沙盒文件名使用源路径稳定后缀区分同名文件，理论上仍存在极低概率后缀碰撞；如果后续要支持大规模批量导入，可升级为持久导入索引或更强的内容/路径摘要。
- `./gradlew :composeApp:allTests` 仍受 Android unit 测试环境的 `sqliteJni` 加载问题影响，需后续单独修复测试运行配置。
