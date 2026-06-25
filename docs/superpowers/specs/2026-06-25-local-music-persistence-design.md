# 本地音乐持久化曲库设计

## 背景

当前本地音乐扫描结果只写入 `InMemoryMusicLibraryRepository` 的内存快照。Android 端虽然已经有 Room 数据库，但数据库只保存播放快照、播放队列和收藏歌曲，没有本地歌曲表。结果是 App 冷启动后无法从数据库恢复本地曲库，手动扫描得到的数据也不会跨进程保留。

本设计把本地歌曲曲库升级为 common 层 Room 持久化能力。Android 先接入和验证，Desktop 与 iOS 后续扫描器可以复用同一套 Repository。

## 目标

- 冷启动 App 不自动触发本地音乐扫描，也不请求音频权限。
- 手动扫描成功后，把扫描结果写入本地数据库。
- 再次扫描时，新歌曲插入，已有歌曲更新扫描元数据，扫不到的旧歌曲标记不可用。
- 冷启动首页只读取最多 6 条可用本地歌曲预览，避免全量曲库拖慢启动。
- 进入本地音乐二级页时再查询全部可用歌曲。
- 收藏记录继续独立保留；歌曲暂时不可用时不删除收藏，未来同一歌曲重新出现后自动恢复收藏状态。
- 保留 `fc7164bb` 修复过的播放队列语义：首页预览播放队列最多 6 条，二级页播放队列为全量列表。

## 非目标

- 不实现真实音频播放能力调整。
- 不新增云端同步、远程歌曲或下载歌曲持久化。
- 不在本次设计中改造收藏表结构。
- 不让冷启动为恢复播放快照而自动扫描媒体库。

## 数据模型

在现有 common Room `PlaybackDatabase` 中新增 `local_song` 表，不另建数据库。歌曲主键沿用扫描链路中的稳定 `sourceKey`，也就是当前 `Song.id` 的来源。这样 `favorite_song.songId` 可以继续和本地歌曲直接关联。

`local_song` 字段：

- `id`: `String`，主键，等于 `MusicFileMetadata.sourceKey`。
- `sourceId`: 来源内稳定标识，例如 MediaStore id 或桌面文件路径 hash。
- `sourceKind`: 来源类型字符串，例如 `androidMediaStore`。
- `localUri`: 播放层可解析的本地 URI。
- `fileName`: 原始文件名。
- `title`: 歌曲标题，允许为空，映射到 UI 时使用兜底。
- `artist`: 歌手，允许为空。
- `album`: 专辑，允许为空。
- `durationMs`: 时长，允许为空。
- `mimeType`: 媒体类型，允许为空。
- `sizeBytes`: 文件大小，允许为空。
- `modifiedAt`: 来源文件修改时间，允许为空。
- `coverArt`: `CoverArt` 枚举名，用于从数据库恢复应用内占位封面。
- `lastScannedAt`: 最近一次扫描确认该歌曲存在的时间。
- `isAvailable`: 是否在最近一次对应来源扫描中仍可用。

`local_song` 不保存收藏状态。收藏状态继续以 `favorite_song` 为唯一事实来源，`Song.isLiked` 是 Repository 读取歌曲时根据 `favorite_song.songId` 派生出来的运行时字段。

## Repository 边界

新增 `LocalSongDao` 和 `PersistentMusicLibraryRepository`。`PersistentMusicLibraryRepository` 继续实现 `MusicLibraryRepository`，但读取能力需要按场景拆分，避免冷启动全量加载。

建议能力：

- `getHomePreview(limit = 6)`: 查询 `isAvailable = true` 的歌曲，按 `modifiedAt DESC`、标题兜底排序，最多返回 6 条。
- `getAllAvailableSongs()`: 查询全部可用歌曲，用于本地音乐二级页。
- `applyScanResult(scanResult, likedSongIds)`: 在事务中写入扫描结果，并返回扫描摘要。
- `getLibraryStats()`: 通过 SQL 聚合读取可用歌曲、专辑、歌手统计，避免冷启动为统计全量加载歌曲。

`InMemoryMusicLibraryRepository` 保留给 common 测试、预览或非持久化宿主使用；Android 进程级会话注入 `PersistentMusicLibraryRepository`。

## 冷启动数据流

冷启动只恢复数据库中已经存在的轻量首页数据。

1. `AndroidPlaybackSession.bootstrap` 创建 Room 数据库。
2. 创建 `PersistentMusicLibraryRepository`，注入 `MusicAppController`。
3. `MusicAppController.createInitialState()` 读取首页预览和统计摘要，不调用 scanner。
4. 首页展示 `homeLocalSongPreview` 中最多 6 条歌曲；如果数据库没有可用歌曲，不显示本地歌曲区块。
5. 冷启动不请求音频权限，不调用 `LocalMusicScanRequest.InitialScan`。
6. 如果存在播放快照但曲库未加载到对应歌曲，不自动扫描。播放快照恢复可以等待用户进入本地页或手动扫描后再尝试，或者保持无当前歌曲状态。

## 本地二级页数据流

进入本地音乐二级页时再加载全量可用歌曲。

1. 用户点击“本地音乐”或首页“查看全部”。
2. Controller 调用显式加载动作，例如 `loadLocalMusicLibrary()`。
3. Repository 查询全部 `isAvailable = true` 的歌曲，按 `modifiedAt DESC` 排序。
4. Controller 更新 `localSongs`，并基于全量歌曲聚合二级页需要的专辑、歌手、来源信息。
5. 本地二级页使用 `localSongs` 渲染完整列表。

## 手动扫描数据流

只有用户手动触发扫描时才请求权限并访问平台媒体库。

1. 用户点击扫描按钮。
2. Controller 进入扫描中状态，并调用平台 scanner。
3. scanner 返回本次来源的完整 `MusicFileMetadata` 列表和失败条目。
4. Repository 在事务中 upsert 本次发现的歌曲：
   - 新歌曲插入。
   - 已存在歌曲更新扫描元数据、`lastScannedAt` 和 `isAvailable = true`。
   - 收藏、播放历史等用户状态不写入 `local_song`。
5. 同一 `sourceKind` 下，本次扫描没有出现的旧歌曲标记为 `isAvailable = false`。
6. 扫描成功后查询扫描摘要、首页预览和统计；如果本地二级页当前打开，也刷新全量 `localSongs`。
7. 扫描发现 0 首时，对应来源旧歌曲会变为不可用，首页预览变空，本地页显示空态，收藏记录继续保留。

## 收藏关联

收藏表 `favorite_song` 继续只保存 `songId` 和 `updatedAt`。这里的 `songId` 必须和 `local_song.id` 使用同一个稳定 id。

读取歌曲时，Repository 同时读取收藏 id 集合，并把当前返回的歌曲映射为 `Song(isLiked = favoriteIds.contains(song.id))`。如果歌曲被标记为不可用，它不会出现在本地曲库列表中，但收藏记录不会删除。未来同一 `id` 再次扫描回来并变为可用时，`isLiked` 会自动恢复为 true。

## 播放队列语义

必须保留 `fc7164bb` 的修复语义：播放队列由点击播放时所在列表决定，队列面板读取 `state.queueSongs`。

- 首页只展示 `homeLocalSongPreview`，点击预览歌曲时传入的播放上下文就是这最多 6 条歌曲。因此 mini player 队列面板显示最多 6 条。
- 本地二级页加载全量 `localSongs` 后，点击歌曲时传入的播放上下文是全量列表。因此 mini player 队列面板显示全量本地歌曲。
- 队列面板不能从当前页面列表或首页预览反推歌曲，必须继续使用播放时写入的 `queueSongs`。

## 状态模型调整

当前 `MusicAppUiState.songs` 承担了首页、本地页、搜索、详情和队列上下文的多重含义。持久化曲库接入后，需要拆分为按场景加载的数据。

建议状态：

- `homeLocalSongPreview: List<Song>`：首页最多 6 条预览。
- `localSongs: List<Song>`：本地二级页按需加载后的全量歌曲。
- `localAlbums: List<Album>`：基于全量本地歌曲聚合。
- `localArtists: List<Artist>`：基于全量本地歌曲聚合。
- `libraryStats: LibraryStats`：可从数据库统计或从全量加载结果更新。
- `queueSongIds` 与 `queueSongs`：继续代表当前播放队列。

搜索、专辑详情、歌手详情如果需要全量曲库，应在进入对应功能时按需加载或查询，不依赖冷启动把全部歌曲塞进状态。

## 错误处理

- 冷启动数据库读取失败时，首页保持空曲库状态，并保留错误日志；不触发扫描兜底。
- 手动扫描权限拒绝时，不修改已有数据库歌曲。
- 手动扫描出现平台查询错误时，不清空旧数据，只进入扫描错误状态。
- 扫描成功但结果为空时，这是有效结果，应把对应来源旧歌曲标记不可用。
- Room schema 升级必须保留已有播放快照、播放队列和收藏数据。

## 测试计划

新增或更新 common 测试：

- `PersistentMusicLibraryRepository` 冷启动只读取首页 6 条预览，排序按 `modifiedAt DESC`。
- 进入本地二级页时查询全部可用歌曲。
- 手动扫描 upsert：新增插入，已有歌曲更新元数据。
- 同一来源扫描后未出现的旧歌曲标记不可用。
- 收藏状态不写入 `local_song`，读取歌曲时由 `favorite_song` 派生。
- 不可用歌曲重新扫描回来后恢复收藏状态。
- 扫描失败不清空旧数据库数据。

更新 Controller 测试：

- 冷启动不触发 scanner，包括有播放快照的情况。
- 首页预览播放队列最多 6 条。
- 本地二级页全量列表播放队列包含全部歌曲。
- 队列面板继续读取当前 `queueSongs`，不被首页预览长度影响。

验证命令：

- `./gradlew :composeApp:desktopTest`
- `./gradlew :composeApp:compileDebugKotlinAndroid`

## 验收标准

- 安装已有数据库且有本地歌曲时，冷启动首页最多显示 6 条本地歌曲，不请求权限，不自动扫描。
- 数据库无本地歌曲时，冷启动首页不显示本地歌曲区块。
- 进入本地音乐二级页后展示数据库中全部可用歌曲。
- 手动扫描成功后，扫描结果持久化到数据库。
- 再次扫描只新增新歌曲，并更新已有歌曲扫描元数据。
- 再次扫描扫不到的旧歌曲不再出现在首页和本地二级页，但收藏记录仍保留。
- 已收藏歌曲重新扫描回来后仍显示为收藏。
- 首页预览点击播放时队列最多 6 条；本地二级页点击播放时队列为全量歌曲。
