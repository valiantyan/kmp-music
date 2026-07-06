Status: ready-for-human

# 固化 Desktop 文件夹 B 不删除文件夹 A 测试

## 父级需求

.scratch/local-audio-discovery-source-coverage/PRD.md

## 要做什么

添加一个聚焦持久化合并测试，描述 Desktop/macOS 扫描目录累加模型：已有文件夹 A 的歌曲保持可用，本次扫描文件夹 B 并发现新歌时，不能把文件夹 A 的歌曲标记为不可用。

该任务只添加测试，不实现具体目录身份或合并逻辑。

如果当前模型还没有具体目录身份字段，测试可以作为红灯指向该缺口；但测试数据必须能清楚表达文件夹 A 和文件夹 B 是两个不同具体来源，而不是两个展示名字符串。

## 验收标准

- [x] 测试包含两个不同 Desktop 扫描目录来源。
- [x] 测试断言扫描文件夹 B 后文件夹 A 的歌曲仍可用。
- [x] 测试断言文件夹 B 的新歌可用。
- [x] 测试断言具体歌曲 id 的可用性，不只检查总数或来源数量。
- [x] 测试名称或注释体现“扫描目录累加模型”。

## 前置依赖

- .scratch/local-audio-discovery-source-coverage/issues/03-positive-only-merge-preserves-existing-songs.md

## Comments

### 2026-07-06

实现摘要：

- 在 `PersistentMusicLibraryRepositoryTest` 新增 `desktopFolderAccumulationScanKeepsFolderASongWhenScanningFolderB`。
- 测试使用 `/Users/listener/Music/A` 和 `/Users/listener/Music/B` 表达两个不同 Desktop 扫描目录来源。
- 测试先写入文件夹 A 的歌曲，再用带 `ConcreteSource(DesktopFolder, folderBPath)` 的扫描结果发现文件夹 B 新歌。
- 断言文件夹 A 具体歌曲 id 仍可用、文件夹 B 具体新歌 id 可用，并从仓库可用歌曲列表断言最终 id 集合。
- 未实现具体目录身份字段或新的合并逻辑，保持当前 issue 的最小范围。

验证命令与结果：

- `./gradlew :composeApp:desktopTest --tests com.yanhao.kmpmusic.data.PersistentMusicLibraryRepositoryTest.desktopFolderAccumulationScanKeepsFolderASongWhenScanningFolderB`：通过。
- `./gradlew :composeApp:desktopTest`：通过。
- 验证期间 Gradle 输出了既有 Kotlin/Gradle deprecation warning，以及 `MusicAppControllerTest.kt` 中既有的 `No cast needed` warning；未发现本次测试导致的新失败。

对抗式审查结论：

- 逻辑漏洞：检查过测试是否只因为正向写入而弱化验收；当前 issue 只要求扫描 B 后保留 A 并加入 B 新歌，因此未加入“B 目录旧歌缺失下线”的后续行为断言，避免越界。
- 事实正确性：PRD 与 issue 均要求 Desktop 扫描目录累加；测试数据用两个目录路径和从路径派生的 source id/song id 表达 A/B 两个具体来源。
- 更简单做法：没有新增 helper 或生产类型；单个测试内直接铺开 fixture，更利于红线行为可读。
- 是否越界：未修改仓库合并实现、平台 scanner、UI 或原型；只新增一个持久化合并测试。
- 验证充分性：已运行聚焦测试和完整 `desktopTest`；本 issue 不涉及 Android 编译或 UI 截图。

Code Review 结论：

- Standards 轴：最终审查无发现；测试位于仓库级 commonTest，遵守显式类型、命名参数、尾逗号和架构边界。
- Spec 轴：最终审查无发现；测试覆盖两个 Desktop 目录来源、A 歌曲保留、B 新歌可用、具体歌曲 id 断言和测试名称中的累加模型。

剩余风险或未完成项：

- 本 issue 按要求只加测试，没有实现更完整的具体目录身份持久化或同目录缺失歌曲下线逻辑；这些仍应留给后续 issue。
