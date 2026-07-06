Status: ready-for-human

# 固化 iOS 新增文件累加测试

## 父级需求

.scratch/local-audio-discovery-source-coverage/PRD.md

## 要做什么

添加一个聚焦测试，描述 iOS P0 导入曲库的累加模型：已有 iOS 文件来源保持可用，本次新增导入文件后，旧文件不被替换或删除。

该任务只添加测试，不实现 iOS bookmark、播放或文件选择。

测试不要把“导入”解释成复制进沙盒后永久保活；它只表达用户新增授权来源时，未处理的旧授权来源没有被本次导入替换。

## 验收标准

- [x] 测试包含已有 iOS 导入文件来源。
- [x] 测试包含本次新增 iOS 导入文件来源。
- [x] 测试断言新增导入后旧 iOS 文件仍可用。
- [x] 测试断言新增导入文件可用。
- [x] 测试断言具体歌曲 id 的可用性，不只检查 iOS 歌曲总数。

## 前置依赖

- .scratch/local-audio-discovery-source-coverage/issues/03-positive-only-merge-preserves-existing-songs.md

## Comments

实现摘要：
- 在 `PersistentMusicLibraryRepositoryTest` 新增 `iosImportAddsNewFileWithoutReplacingExistingImportedFile`。
- 测试先写入一个已有 `IosImportedFile` 来源，再应用只包含新增 iOS 文件的 `PositiveOnly` 扫描结果。
- 测试逐项断言旧歌曲 id 和新歌曲 id 都仍可用，并断言可用歌曲集合等于这两个具体 id。
- 本轮只添加测试，没有实现 iOS bookmark、播放、文件选择或其它后续 issue。

验证命令与结果：
- `./gradlew :composeApp:desktopTest --tests com.yanhao.kmpmusic.data.PersistentMusicLibraryRepositoryTest`：通过。
- `./gradlew :composeApp:desktopTest`：通过。

对抗式审查结论：
- 逻辑漏洞：测试直接断言旧 iOS 歌曲行仍可用、新 iOS 歌曲行可用，并断言具体 id 集合，避免只看总数导致误判。
- 事实正确性：使用 `IosImportedFile` 和 `PositiveOnly` 表达新增授权来源累加，没有把导入表述成沙盒复制后的永久保活。
- 更简单做法：只在既有持久化仓库测试中新增一个聚焦场景，没有新增 helper、模型或生产代码。
- 越界风险：未修改平台代码、bookmark、播放、文件选择、UI 或后续 issue 范围。
- 验证充分性：已运行聚焦 persistent repository 测试和完整 `desktopTest`；本次未触碰 Android 平台代码，因此未运行 Android 编译。

Code Review 结论：
- Standards：无硬性规范问题；审查指出与已有 positive-only 测试存在可接受的测试结构重复，当前保持显式场景更利于读懂 issue 行为。
- Spec：未发现偏差；测试覆盖已有来源、新增来源、旧文件保留、新文件可用和具体 id 断言，且没有范围蔓延。

剩余风险或未完成项：
- 新增测试当前为通过状态，因为前置 positive-only 合并语义已经存在；它用于固化 iOS 累加模型，而不是引入新的生产实现。
