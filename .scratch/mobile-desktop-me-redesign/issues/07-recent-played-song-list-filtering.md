Status: ready-for-human

# 统一最近播放歌曲列表过滤规则

## 父级

/Users/yanhao/Desktop/demo/kmp-music/.scratch/mobile-desktop-me-redesign/PRD.md

## 要构建什么

把“最近播放歌曲列表”明确为由真实播放历史生成、按最近播放倒序排列、且只包含当前曲库可解析并可播放歌曲的集合。摘要、完整页和播放队列后续都应复用这同一份过滤后的列表语义。

## 验收标准

- [x] 最近播放歌曲列表只来自真实播放历史。
- [x] 最近播放歌曲列表按最近播放倒序排列。
- [x] 历史中已移除、无法解析或不可播放的歌曲不出现在列表中。
- [x] 后续摘要、完整页和播放队列可以复用同一份过滤结果。
- [x] 不新增 repository、持久化表、后端接口或播放日志管理能力。
- [x] 新增或更新共享逻辑测试，证明过滤结果不会包含陈旧历史项；如果没有可用测试边界，在 Comments 说明原因和人工验证方式。

## 依赖

无，可以立即开始

## Comments

### 实现摘要

- 在 `LibraryStateSynchronizer.buildRecentSongs` 中统一最近播放歌曲列表语义：读取 `PlaybackRepository.getPlaybackHistory().songIds` 作为唯一来源，保留历史顺序去重后的最近播放倒序。
- 使用 `MusicLibraryRepository.getAvailableSongsByIds` 按历史 ID 回查当前曲库实体，并过滤 `Song.isPlayable == false` 的歌曲，避免已移除、不可解析或不可播放歌曲进入最近播放列表。
- 返回当前曲库实体作为列表内容，仅继承当前 UI 已知的收藏状态，避免队列快照或旧列表里的同 ID 陈旧元数据污染后续摘要、完整页和播放队列。
- 未新增 repository、持久化表、后端接口或播放日志管理能力；未修改 `prototypes/kmp-music-hi-fi` 或 `.agent-loop/*`。

### 验证命令与结果

- `./gradlew :composeApp:desktopTest`：通过，`BUILD SUCCESSFUL in 43s`。存在既有警告：Gradle Kotlin deprecated property，以及 `MusicAppControllerTest.kt` 两处 `No cast needed`。
- `./gradlew :composeApp:compileDebugKotlinAndroid`：通过，`BUILD SUCCESSFUL in 21s`。存在同样的 Gradle Kotlin deprecated property 警告。

### Code review 结论

- 规格符合性：最近播放列表不再从首页预览、队列快照或收藏列表直接拼出可见项，必须先通过真实播放历史和当前曲库 ID 解析；陈旧历史 ID、不可解析 ID、不可播放歌曲都会被过滤。
- 范围控制：改动只触及共享列表同步器和共享测试，没有新增持久化、Repository、后端接口、UI 路由或播放日志管理能力。
- 代码质量：过滤逻辑集中在现有 `LibraryStateSynchronizer.buildRecentSongs` 接缝，后续摘要、完整页和播放队列可以复用 `uiState.recentSongs` 的同一份语义。

### 对抗式审查

- 风险 1：用 demo/全库数据冒充历史。已检查，输出入口仍只遍历 `PlaybackHistory.songIds`，测试直接预置真实播放历史。
- 风险 2：保留陈旧 songId。已用共享测试覆盖：历史中存在 `stale`，即使队列快照能找到旧实体，也不会出现在结果里。
- 风险 3：不可播放歌曲混入列表。已用共享测试覆盖：当前曲库里 `localUri` 为空的 `unplayable` 会被 `Song.isPlayable` 过滤。
- 风险 4：队列快照旧元数据污染当前曲库实体。已用共享测试覆盖：队列里同 ID 的旧标题不会覆盖当前仓库标题。
- 风险 5：为了本切片新增不允许的能力。已检查 diff，没有新增 repository、表、接口、后端或播放日志管理功能。

### 剩余风险或未完成项

- 本 issue 只建立过滤结果语义，不实现 issue 08 及后续最近播放页路由、UI 或点击后播放队列行为。
- 真实设备上的最近播放摘要和后续完整页视觉不在本切片内验证；本次风险集中在共享数据语义，已通过 shared tests 和 Android Kotlin 编译覆盖。
