Status: ready-for-human

# 固化 Android 完整媒体库覆盖测试

## 父级需求

.scratch/local-audio-discovery-source-coverage/PRD.md

## 要做什么

添加一个聚焦测试，描述 Android 成功刷新是完整 Android 系统媒体库来源覆盖。测试应证明：当一个旧 Android MediaStore 歌曲不在完成扫描结果中时，它可以被标记为不可用。

该任务只添加红灯测试，不要求实现 Android scanner 或合并逻辑改动。

测试应通过显式“Android 系统媒体库完整覆盖”来表达删除权，不要只靠 `LocalMusicScanRequest.Source(AndroidMediaStore)` 或 `sourceKind` 推导覆盖完成。

## 验收标准

- [x] 测试使用 Android 系统媒体库来源语义。
- [x] 测试断言完整 Android 覆盖可以下线缺失旧歌。
- [x] 测试断言下线的是缺失的具体 Android 歌曲 id。
- [x] 测试与 positive-only 场景形成对照，避免把所有同 `sourceKind` 扫描都当成完整覆盖。

## 前置依赖

- .scratch/local-audio-discovery-source-coverage/issues/03-positive-only-merge-preserves-existing-songs.md

## Comments

实现摘要：

- 将持久化曲库仓库测试中的 Android 同来源缺失下线用例收窄为 `androidCompleteMediaStoreCoverageMarksMissingAndroidSongUnavailableAfterPositiveOnlyKeepsIt`。
- 测试先执行一轮没有 `completedCoverage` 的 Android positive-only 扫描，断言旧 Android 歌曲仍保持可用。
- 测试再执行一轮显式 `LocalMusicScanCoverage.SourceKind(AndroidMediaStore)` 的完整 Android 媒体库覆盖扫描，断言缺失的 `androidMediaStore:old` 被标记不可用，同时保留新发现歌曲和非 Android 来源歌曲。
- 本 issue 没有修改生产合并逻辑、Android scanner 或后续平台覆盖实现；当前测试能通过，是因为前置 issue 已经实现了显式覆盖删除权。

验证命令与结果：

- `./gradlew :composeApp:desktopTest --tests com.yanhao.kmpmusic.data.PersistentMusicLibraryRepositoryTest`：通过。
- `./gradlew :composeApp:desktopTest`：通过。
- 验证过程中仅出现既有 Gradle deprecated property 提示和 `MusicAppControllerTest` 中既有 `No cast needed` 警告，未影响结果。

对抗式审查结论：

- 逻辑漏洞：已确认 positive-only 对照和完整覆盖下线都在同一 repository seam 内断言，避免只靠 `sourceKind` 或请求类型推导删除权。
- 事实正确性：已确认测试使用 `LocalMusicSourceKind.AndroidMediaStore` 和 `LocalMusicScanCoverage.SourceKind(AndroidMediaStore)` 表达 Android 系统媒体库完整覆盖。
- 是否有更简单做法：没有新增 helper 或生产抽象，保持最小测试改动；重复的两轮扫描调用保留为显式对照，便于阅读。
- 是否越界实现后续 issue：没有修改 Android scanner、Desktop/iOS 覆盖语义或合并逻辑。
- 验证是否充分：已跑聚焦持久化仓库测试和完整 Desktop 测试，覆盖本次 commonTest 改动范围。

code-review 结论：

- Standards 轴：未发现硬性违规；初次审查提出测试名未完全表达两阶段对照，已将测试名改为包含 positive-only 对照语义并重新验证。
- Spec 轴：未发现缺失、越界或错误实现；确认满足 issue 对显式 Android 完整覆盖、具体缺失歌曲下线和 positive-only 对照的要求。

剩余风险或未完成项：

- 本 issue 按要求只固化测试，不实现后续 Android scanner 自动声明完整覆盖，也不处理 Desktop/iOS 具体来源覆盖。
- Android 编译未运行；本次只改 `commonTest` 持久化仓库测试，已用 Desktop 测试链路验证共享测试。
