Status: ready-for-human

# 实现失败扫描 positive-only 安全合并

## 父级需求

.scratch/local-audio-discovery-source-coverage/PRD.md

## 要做什么

实现最小失败扫描合并规则：失败扫描可以保留已经验证或写入的正向结果，但不能下线未处理旧歌。

任务只处理失败扫描的安全合并，不处理取消状态文案或平台扫描器。

如果当前扫描异常不会携带部分成功结果，不要为通过测试而伪造删除权；应选择明确的部分结果或结果模型，或在异常路径保留旧快照并只提交已经安全写入的正向结果。

## 验收标准

- [x] 失败扫描不会删除未处理旧歌。
- [x] 失败扫描可以保留已验证正向结果。
- [x] 失败扫描不显示或暗示旧歌被删除。
- [x] 普通失败状态仍渲染为“扫描失败”，不混同“已取消”。
- [x] 失败扫描不删除旧歌测试通过。

## 前置依赖

- .scratch/local-audio-discovery-source-coverage/issues/13-failed-scan-preserves-existing-songs-test.md

## Comments

实现摘要：

- 在 `LocalMusicScanResult.deletionAuthority` 中增加失败结果保护：只要扫描结果包含 `failed` problem，就不再拥有删除旧歌曲的权限。
- 在 `PersistentMusicLibraryRepository` 的覆盖解析入口消费 `deletionAuthority`：失败结果仍会 upsert 已验证的 `discovered` 正向歌曲，但不会使用 `completedCoverage` 下线旧歌。
- 在 `LocalMusicScanCoverageContractTest` 中新增契约测试，固定“失败结果即使带完成覆盖，也只能作为 positive-only 合并”的领域规则。
- 未修改取消扫描文案、平台 scanner 或后续 issue 的扫描页统计 UI。

验证命令与结果：

- `./gradlew :composeApp:desktopTest --tests "com.yanhao.kmpmusic.data.PersistentMusicLibraryRepositoryTest"`：通过。前置红灯用例 `failedScanHasNoDeletionAuthorityAndKeepsUnprocessedExistingSong` 已转绿。
- `./gradlew :composeApp:desktopTest --tests "com.yanhao.kmpmusic.domain.model.LocalMusicScanCoverageContractTest"`：通过。
- `./gradlew :composeApp:desktopTest --tests "com.yanhao.kmpmusic.domain.model.LocalMusicScanCoverageContractTest" --tests "com.yanhao.kmpmusic.data.PersistentMusicLibraryRepositoryTest"`：通过。
- `./gradlew :composeApp:desktopTest`：通过。
- 验证输出仍包含既有 Gradle deprecated property 警告；聚焦测试编译时仍显示 `MusicAppControllerTest.kt` 中既有 `No cast needed` warning。

对抗式审查结论：

- 逻辑漏洞：最容易漏掉的是失败结果仍带 `completedCoverage`，导致仓库继续消费删除权。已把删除权降级放到 `LocalMusicScanResult.deletionAuthority`，并让持久化覆盖解析统一读取该契约。
- 事实正确性：PRD 明确失败扫描只能保留正向结果，不能删除未处理旧歌；前置持久化测试同时断言旧歌保留、新歌写入和 `removedCount == 0`。
- 是否有更简单做法：可以只在仓库里判断 `failed.isNotEmpty()`，但这会绕过已有 `deletionAuthority` 领域契约；当前做法更集中，改动仍然只有 10 行生产代码。
- 是否越界实现后续 issue：没有改取消状态、扫描页统计、平台 scanner 或导航行为；普通失败状态仍沿用现有 `LocalMusicScanState.Error` / “扫描失败”路径。
- 验证是否充分：已运行前置失败扫描测试、领域覆盖契约测试和完整 `desktopTest`；本 issue 未触及 Android 平台源码，因此未额外运行 Android 编译。

code-review 结论：

- Standards 轴：未发现问题。改动遵守 common/domain/data 分层，未引入平台 API；Kotlin 命名和中文注释沿用现有风格；新增测试 helper 聚焦且没有复制生产逻辑。
- Spec 轴：未发现问题。实现满足失败扫描 positive-only 安全合并：旧歌不被删除，正向新歌可写入，摘要不暗示删除，普通失败文案路径未被混同为取消态。

剩余风险或未完成项：

- 本次把任何带 `failed` problem 的结果都保守视为无删除权；这符合当前 PRD 的失败安全要求，但如果未来需要“部分失败但仍能证明某个具体来源完整覆盖”的更细粒度语义，需要新增更明确的结果模型。
