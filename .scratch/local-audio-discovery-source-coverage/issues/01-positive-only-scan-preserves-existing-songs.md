Status: ready-for-human

# 固化 positive-only 扫描不删除旧歌的回归测试

## 父级需求

.scratch/local-audio-discovery-source-coverage/PRD.md

## 要做什么

在现有持久化曲库合并边界上添加一个聚焦回归测试。测试描述一个 positive-only 扫描结果：它发现了一首与既有歌曲同 `sourceKind` 的新歌，但没有声明任何已完成来源覆盖。期望行为是既有歌曲仍保持可用，新发现歌曲被加入曲库。

这是一个刻意保持很小的红灯测试任务。不实现完整来源覆盖模型，只先锁住最危险的误删规则，给后续实现留下清晰护栏。

主测试边界是 `PersistentMusicLibraryRepository.applyScanResult`。测试不要用 `LocalMusicScanRequest.Source(...)` 把本轮扫描伪装成完整来源扫描；应表达“没有完成覆盖声明”的请求和结果组合。

## 验收标准

- [x] 测试覆盖“既有可用歌曲”和“新发现歌曲”共享同一 `sourceKind` 的场景。
- [x] 测试断言 positive-only 扫描后既有歌曲仍保持可用。
- [x] 测试断言 positive-only 扫描后新发现歌曲变为可用。
- [x] 测试断言具体歌曲 id 的可用性，不只检查总数。
- [x] 测试名称或注释使用 “positive-only” 或等价中文解释，说明本次扫描没有删除权。

## 前置依赖

无，可以立即开始。

## Comments

实现摘要：已在 `PersistentMusicLibraryRepositoryTest` 新增 `positiveOnlyScanAddsNewSameSourceSongWithoutRemovingExistingSong` 回归测试。测试使用 `LocalMusicScanRequest.Refresh`、空 `sourceSummaries` 和空 `removedSourceKeys` 表达 positive-only 扫描结果，只提供同 `sourceKind` 的新歌正向发现，不把本轮扫描伪装成完整来源覆盖。

验证命令与结果：已运行 `./gradlew :composeApp:desktopTest --tests "com.yanhao.kmpmusic.data.PersistentMusicLibraryRepositoryTest"`。结果为预期红灯：`10 tests completed, 1 failed`，失败用例为 `positiveOnlyScanAddsNewSameSourceSongWithoutRemovingExistingSong`。失败证明当前合并逻辑仍会把未覆盖的既有同来源歌曲误下线，符合本 issue “只先锁住护栏，不实现后续模型”的要求。

剩余风险或未完成项：本 issue 已完成，等待人工验收。完整来源覆盖模型、让该红灯测试转绿、以及 Desktop/iOS/Android 的覆盖语义实现仍属于后续 issue，当前未实现。
