Status: ready-for-human

# 补最终验证与对抗式审查清单

## 父级需求

.scratch/local-audio-discovery-source-coverage/PRD.md

## 要做什么

做本 PRD 的最后一颗验证任务：运行与改动范围匹配的聚焦测试和构建命令，并补一份对抗式审查清单，确认来源覆盖、取消/失败安全、平台文案和架构边界没有互相打架。

任务不新增功能，只做验证和交付前审查。

最终审查必须回到 PRD 原始验收，而不是只看 issue 是否被勾选。尤其要确认：positive-only 不删除旧歌、Android 成功扫描仍能删除缺失 MediaStore 歌曲、Desktop/iOS 不按 source-kind 宽泛删除、扫描中重复触发不会并发、取消和失败状态文案不同、扫描完成不自动跳离当前页面。

## 验收标准

- [x] 聚焦持久化曲库合并测试已运行并记录结果。
- [x] 涉及 controller 或 UI 状态时，共享测试已运行并记录结果。
- [x] 涉及 Android 扫描器合同时，Android Kotlin 编译已运行并记录结果。
- [x] 对抗式审查列出至少 3 个最可能翻车点，并说明验证证据。
- [x] 对抗式审查逐项检查具体歌曲 id 的可用性，不只看统计数字。
- [x] 对抗式审查确认扫描中单任务与取消入口行为符合 PRD。
- [x] 对抗式审查确认收藏状态和播放队列不会因 partial scan 误丢未证明不可用的歌曲。
- [x] 对抗式审查确认扫描或导入完成后仍停留在当前页面或当前路由。
- [x] 确认没有修改高保真原型来解决生产 App 问题。

## 前置依赖

- .scratch/local-audio-discovery-source-coverage/issues/05-android-scanner-declares-complete-coverage.md
- .scratch/local-audio-discovery-source-coverage/issues/08-desktop-concrete-folder-coverage.md
- .scratch/local-audio-discovery-source-coverage/issues/10-ios-import-positive-only.md
- .scratch/local-audio-discovery-source-coverage/issues/12-cancelled-scan-ui-state.md
- .scratch/local-audio-discovery-source-coverage/issues/14-failed-scan-positive-only-merge.md
- .scratch/local-audio-discovery-source-coverage/issues/16-scan-page-platform-copy.md

## Comments

最终验证摘要：已回到 PRD 原始验收做最终验证。本轮没有实现新生产功能；为补齐 code-review 指出的播放队列证据，新增一个共享 controller 回归测试 `positiveOnlyScanKeepsExistingPlaybackQueueSongs`，只验证 partial/positive-only 扫描后既有队列歌曲仍保留。另按标准轴审查结果，将本 PRD 中英文描述正文中文化，保留 `sourceKind`、`common`、命令和类型名等必要代码术语。

验证命令与结果：

- `./gradlew :composeApp:desktopTest --tests com.yanhao.kmpmusic.data.PersistentMusicLibraryRepositoryTest --tests com.yanhao.kmpmusic.domain.model.LocalMusicScanCoverageContractTest --tests com.yanhao.kmpmusic.data.DesktopFolderMusicScannerTest --tests com.yanhao.kmpmusic.data.FakeLocalMusicScannerTest --tests com.yanhao.kmpmusic.domain.usecase.ScanLocalMusicUseCaseTest`：通过，`BUILD SUCCESSFUL`。
- `./gradlew :composeApp:desktopTest --tests com.yanhao.kmpmusic.feature.app.MusicAppControllerTest --tests com.yanhao.kmpmusic.feature.app.library.MusicAppLibraryStateSynchronizerTest --tests com.yanhao.kmpmusic.feature.screen.LocalMusicScanSummaryDisplayModelTest`：通过，`BUILD SUCCESSFUL`。
- `./gradlew :composeApp:desktopTest --tests com.yanhao.kmpmusic.feature.app.MusicAppControllerTest.positiveOnlyScanKeepsExistingPlaybackQueueSongs`：通过，`BUILD SUCCESSFUL`。
- `./gradlew :composeApp:compileDebugKotlinAndroid`：通过，`BUILD SUCCESSFUL`。

对抗式审查：

1. 最可能翻车点：positive-only 或失败结果仍可能被仓库误当成同来源完整扫描。证据：`PersistentMusicLibraryRepositoryTest.positiveOnlyScanAddsNewSameSourceSongWithoutRemovingExistingSong` 断言 `androidMediaStore:existing` 和 `androidMediaStore:new` 同时可用；`positiveOnlyRefreshWithoutDiscoveredSongsPreservesExistingSources` 断言 `androidMediaStore:gone`、`desktopFolder:gone` 都保持可用；`failedScanHasNoDeletionAuthorityAndKeepsUnprocessedExistingSong` 断言 `androidMediaStore:old-safe-song` 与 `androidMediaStore:verified-new-song` 同时可用且移除数为 0。
2. 最可能翻车点：Android 完整扫描如果完全禁用删除权，会导致 MediaStore 缺失歌曲无法下线。证据：`androidCompleteMediaStoreCoverageMarksMissingAndroidSongUnavailableAfterPositiveOnlyKeepsIt` 先确认 positive-only 时 `androidMediaStore:old` 保持可用，再确认 `LocalMusicScanCoverage.SourceKind(AndroidMediaStore)` 完整覆盖后 `androidMediaStore:old` 不可用、`androidMediaStore:new` 与 `desktopFolder:keep` 保持可用。
3. 最可能翻车点：Desktop/iOS 仍按 `sourceKind` 宽泛删除。证据：`desktopFolderAccumulationScanKeepsFolderASongWhenScanningFolderB` 断言扫描文件夹 B 后 `desktopFolder:/Users/listener/Music/A/folder-a.mp3` 保持可用、`desktopFolder:/Users/listener/Music/B/folder-b-old.mp3` 不可用、`desktopFolder:/Users/listener/Music/B/folder-b-new.mp3` 可用；`desktopSourceKindCoverageDoesNotDeleteWholeDesktopLibrary` 断言即使出现 `SourceKind(DesktopFolder)`，`desktopFolder:/Users/listener/Music/A/old.mp3` 仍保持可用；`iosImportAddsNewFileWithoutReplacingExistingImportedFile` 断言 `iosImportedFile:bookmark://ios/imported/existing.m4a` 与 `iosImportedFile:bookmark://ios/imported/new.m4a` 同时可用。
4. 最可能翻车点：扫描中二次触发启动并发任务，或取消与失败共用文案。证据：`scanEntryDuringRunningScanDoesNotStartSecondScan` 断言运行中入口显示“取消扫描”、第二次触发后 scanner 调用次数仍为 1，且进入 `LocalMusicScanState.Cancelled`；`cancelledScanStateIsDistinctFromDoneAndError`、`cancelledScanStateMapsToCancelledCopy` 断言取消不是成功也不是失败，并渲染“已取消”和“当前曲库已保留”；失败态仍由 `LocalMusicScanState.Error` 和错误文案“扫描失败”承载。
5. 最可能翻车点：partial scan 误丢收藏或播放队列，或扫描完成跳离当前路由。证据：`positiveOnlyScanAddsNewSameSourceSongWithoutRemovingExistingSong` 与 `favoritesAreDerivedAndSurviveUnavailableSongs` 覆盖收藏保留；本轮新增 `positiveOnlyScanKeepsExistingPlaybackQueueSongs`，扫描后即使仓库快照只暴露 `partial:new`，`queueSongIds`、`queueSongsSnapshot`、`queueSongs` 仍保留 `partial:old-1`、`partial:old-2`；`scanRefreshesHomeAlbumSectionAlbums`、`scanRefreshesHomeArtistSectionArtists`、`homeAlbumSectionLoadsAllLocalAlbumsWithoutLeavingHome`、`homeArtistSectionLoadsAllLocalArtistsWithoutLeavingHome` 证明扫描或刷新当前页面数据后仍停留在当前首页分段或当前路由。

code-review 结论：

- 标准轴：首次审查发现 PRD 中“用户故事、实现决策、验收标准”等描述正文为英文，违反仓库“生成的每个 Markdown 文件描述内容用中文书写”的规则；已将 PRD 描述正文中文化并保留必要代码术语。另有非阻塞气味：`LocalMusicDiscoveryPlatform` 在 UI 路由层透传较广，当前可工作，后续如平台文案继续扩展可考虑收敛到更集中的 UI 配置入口。
- 规格轴：首次审查指出 issue 17 尚未写交付记录，且播放队列 partial scan 证据不够硬；已补本 Comments 并新增 `positiveOnlyScanKeepsExistingPlaybackQueueSongs` 后重新验证。当前未发现 PRD 必查项缺失；未发现修改高保真原型，`git diff --name-only origin/main...HEAD -- prototypes` 输出为空。

剩余风险或未完成项：

- 未做真机或模拟器截图；本 issue 是最终验证和审查任务，当前覆盖为仓库合并、共享 controller/UI 状态测试与 Android Kotlin 编译。
- Gradle 输出仍提示仓库已有 Kotlin MPP deprecated property 警告，和本 PRD 行为无关。
- 工作区存在协调器 `.agent-loop/log.md`、`.agent-loop/progress.md` 的未提交改动，本实现线程未修改、未提交它们；提交由协调器统一处理。
