Status: ready-for-human

# 让 positive-only 合并保留既有歌曲

## 父级需求

.scratch/local-audio-discovery-source-coverage/PRD.md

## 要做什么

实现最小合并行为，让 positive-only 扫描结果只写入新发现或已验证歌曲，不把同 `sourceKind` 下未出现的既有歌曲标记为不可用。

这个任务只让前置红灯测试通过，不实现 Android、Desktop 或 iOS 的完整覆盖声明。

主实现边界是 `PersistentMusicLibraryRepository.applyScanResult` 的覆盖范围判定。不要继续从 `discovered` 或 `sourceSummaries` 反推出整个 `sourceKind` 已被完整扫描；只有显式完成覆盖或显式移除证据才有删除权。

## 验收标准

- [x] positive-only 扫描结果不会下线既有可用歌曲。
- [x] positive-only 扫描结果仍会加入新发现歌曲。
- [x] 现有聚焦测试通过。
- [x] 持久化仓库不会因为同 `sourceKind` 新歌出现就下线未处理旧歌。
- [x] 既有歌曲被 positive-only 合并保留时，收藏状态也被保留。
- [x] 如果同步调整内存仓库或共享合并 use case，应保持与持久化 seam 的用户可见语义一致。
- [x] 实现保持平台 API 不进入 common UI/domain 之外的错误位置。

## 前置依赖

- .scratch/local-audio-discovery-source-coverage/issues/02-scan-result-coverage-contract-test.md

## Comments

实现摘要：

- 在 `LocalMusicScanResult` 上补齐最小 `completedCoverage` 契约和删除权限派生，默认无完成覆盖，避免旧 scanner 被迫声明平台完整覆盖。
- 调整 `PersistentMusicLibraryRepository.applyScanResult` 的覆盖范围判定：不再从 `discovered`、`sourceSummaries`、空 `Refresh` 或 `LocalMusicScanRequest.Source` 推断整个 `sourceKind` 已完成扫描；当前只消费显式来源类型覆盖。
- 将新增/更新统计从删除覆盖集合中拆出，按本次发现歌曲是否已存在于可用曲库计算，保证 positive-only 合并仍能正确记录新增和更新。
- 加强持久化仓库测试，覆盖 positive-only 同来源新歌合入、旧歌保留、收藏状态保留，以及无发现 positive-only 结果不删除旧曲库。

验证命令与结果：

- `./gradlew :composeApp:desktopTest --tests com.yanhao.kmpmusic.data.PersistentMusicLibraryRepositoryTest`：通过。
- `./gradlew :composeApp:compileDebugKotlinAndroid :composeApp:desktopTest`：通过。

剩余风险或未完成项：

- 本 issue 未实现 Android、Desktop 或 iOS scanner 的完整覆盖声明；后续 issue 仍需让平台 scanner 明确填充 `completedCoverage`。
- 持久化仓库当前只消费来源类型级 `completedCoverage`；具体来源级覆盖和显式移除证据的完整删除语义留给后续 issue。
- 未调整内存仓库或共享合并 use case；本次边界限定在持久化仓库 seam。
