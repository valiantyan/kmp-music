Status: ready-for-human

# 固化失败扫描不删除未处理旧歌测试

## 父级需求

.scratch/local-audio-discovery-source-coverage/PRD.md

## 要做什么

添加一个聚焦测试，描述失败扫描可以保留已写入正向结果，但不能把未处理的既有歌曲标记为不可用。

该任务只添加红灯测试，不实现失败路径合并。

测试应覆盖仓库合并或控制器错误路径中用户可感知的曲库安全：失败后旧歌仍在可用列表中。不要只断言扫描状态进入错误态。

## 验收标准

- [x] 测试包含扫描失败前已存在的可用歌曲。
- [x] 测试断言失败扫描后未处理旧歌仍可用。
- [x] 测试断言失败扫描不会暗示旧歌被删除。
- [x] 测试断言具体旧歌 id 仍可用。
- [x] 测试名称或注释说明失败扫描没有删除权。

## 前置依赖

- .scratch/local-audio-discovery-source-coverage/issues/03-positive-only-merge-preserves-existing-songs.md

## Comments

实现摘要：

- 在 `PersistentMusicLibraryRepositoryTest` 中新增红灯测试 `failedScanHasNoDeletionAuthorityAndKeepsUnprocessedExistingSong`。
- 测试先写入一首既有可用旧歌，再应用带有正向发现、失败问题和完成覆盖声明的失败扫描结果。
- 测试断言旧歌 id `androidMediaStore:old-safe-song` 仍可用，新发现歌曲仍可写入，可用列表不丢旧歌，并且扫描摘要 `removedCount` 为 `0`、`problemCount` 为 `1`。
- 本次只添加测试和测试 helper，没有修改生产合并逻辑，也没有实现后续 issue 14。

验证命令与结果：

- `./gradlew :composeApp:desktopTest --tests "com.yanhao.kmpmusic.data.PersistentMusicLibraryRepositoryTest"`：按红灯测试预期失败。
- 失败用例：`failedScanHasNoDeletionAuthorityAndKeepsUnprocessedExistingSong`。
- 失败位置：`PersistentMusicLibraryRepositoryTest.kt:625`，旧歌 `androidMediaStore:old-safe-song` 当前被标记为不可用，证明现有失败扫描合并仍会错误使用删除权。
- 同次运行编译通过；Gradle 输出既有 deprecated property 警告，以及 `MusicAppControllerTest.kt` 中既有 `No cast needed` warning。

对抗式审查结论：

- 逻辑漏洞：如果测试只使用无 `completedCoverage` 的失败结果，当前 positive-only 行为会直接通过，无法形成红灯；已改为覆盖“失败结果不得消费完成覆盖删除权”。
- 事实正确性：PRD 明确失败扫描只能保留正向结果，不能删除未处理旧歌；测试同时断言正向新歌写入和旧歌保留。
- 是否有更简单做法：可只断言错误态，但 issue 明确要求不要只断言扫描状态；当前测试直接检查仓库可用列表和具体旧歌 id。
- 是否越界实现后续 issue：未修改 `PersistentMusicLibraryRepository` 或扫描状态模型，未让测试变绿。
- 验证是否充分：已运行聚焦持久化仓库测试并确认预期红灯；红灯测试任务不继续运行全量绿色验证。

code-review 结论：

- Standards 轴：未发现问题。新增测试沿用现有持久化仓库测试风格、命名、中文注释和 helper 构造方式，未引入平台 API。
- Spec 轴：未发现问题。diff 只添加当前 issue 要求的失败扫描安全红灯测试，覆盖既有旧歌、失败后旧歌仍可用、无删除暗示、具体旧歌 id 和“失败扫描没有删除权”的名称/注释要求。

剩余风险或未完成项：

- 该用例当前按预期失败；后续 issue 14 仍需实现失败扫描 positive-only 安全合并，让该测试转绿。
- 本次没有验证 Android 编译或全量 `desktopTest` 通过，因为当前交付目标是红灯测试且聚焦命令已经按预期失败。
