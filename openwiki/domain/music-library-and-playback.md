# 音乐曲库和播放领域

本页解释产品/领域模型，以及修改时应遵循的源码边界。`CONTEXT.md` 是规范术语表；本页把这些术语连接到实现。

## 产品领域

KMP Music 是本地优先产品。`docs/PRD.md` 中的 MVP 范围优先支持：

- 浏览并播放本地歌曲。
- 查看由本地曲库派生的专辑和歌手。
- 通过喜欢的歌曲投影出歌曲/专辑/歌手收藏。
- 搜索本地曲库或收藏。
- 移动端信息架构保持三个根 Tab：`首页 / 收藏 / 我的`。
- 登录/云同步作为未来能力；访客模式应保持可用。

PRD 中明确的非目标包括社交/社区功能、付费会员、在线下载、歌词作为 MVP 要求、云同步实现、DRM、在线流媒体，以及 iOS 整机文件扫描。

## 核心模型文件

| 概念 | Source |
| --- | --- |
| 歌曲、专辑、歌手 | `domain/model/Song.kt`、`Album.kt`、`Artist.kt` |
| 专辑/歌手身份和归一化 | `domain/model/AlbumIdentity.kt`、`ArtistIdentity.kt` |
| 本地扫描 request/result/state/source/error 模型 | `domain/model/LocalMusicModels.kt` |
| 播放状态、队列、模式、playable media、错误、快照/历史 | `domain/model/PlaybackModels.kt` |
| 搜索 context/scope | `domain/model/SearchContext.kt`、`PlaybackModels.kt` (`SearchScope`) |
| 曲库读取和扫描合并边界 | `domain/repository/MusicLibraryRepository.kt` |
| 平台扫描器边界 | `domain/repository/LocalMusicScanner.kt` |
| 播放 runtime 边界 | `domain/repository/PlaybackRepository.kt` |
| 扫描编排 | `domain/usecase/ScanLocalMusicUseCase.kt` |
| 播放编排 | `domain/playback/PlaybackCoordinator.kt` |

## 本地音乐扫描领域

`LocalMusicScanner` 有意保持窄边界：

> Scanner 只回答“这个平台发现了哪些本地歌曲，以及什么 URI 可以播放它们？”

它不更新 UI、不执行播放、不管理队列。`ScanLocalMusicUseCaseImpl` 协调 scanner 输出与 `MusicLibraryRepository.applyScanResult`，并传入当前喜欢的歌曲 ID，使生成的 snapshot 能派生收藏状态。

`LocalMusicModels.kt` 保持扫描事实平台无关：

- `LocalMusicSourceKind`：Android MediaStore、iOS imported file、iOS media library、Desktop folder、fake scanner。
- `LocalMusicScanRequest`：initial scan、refresh 或 source-specific scan。
- `MusicFileMetadata`：scanner 输出，包含 source id/kind、可播放 `localUri`、文件名、可选 title/artist/album/duration/mime/size/modified time、cover fallback 和可选 scanned cover URI。
- `LocalMusicScanResult`：发现的 metadata、移除的 source keys、失败 problems、source summaries、完成时间。
- `LocalMusicScanState`：idle、waiting for permission、importing/scanning progress、done、error。
- `LibrarySnapshot`：面向 UI 的 read model，包含 songs、albums、artists、stats、sources、problems 和 scan state。

来自 `docs/LOCAL_AUDIO_DISCOVERY_PRD.md` 的平台 PRD 约束：

- Android P0 扫描用户授权的 `MediaStore.Audio`，不是原始整盘遍历。
- iOS P0 使用显式文件/文件夹选择或导入语义；不能像 Android MediaStore 那样扫描整台 iPhone。
- Desktop P0 递归扫描用户选择的文件夹；不能静默扫描整块磁盘或系统目录。

## 曲库快照和分组

`MusicLibraryRepository` 提供：

- 首页 preview（`getHomePreview(limit = 6)`），用于轻量冷启动渲染。
- 面向本地音乐、搜索和详情页的完整可用歌曲列表。
- 通过 ID 解析歌曲，用于播放恢复和收藏投影，避免读取全量曲库。
- 曲库统计。
- 把扫描结果合并为新的 `LibrarySnapshot`。

`PersistentMusicLibraryRepository` 是 Android/Desktop controller factory 使用的 Room-backed 实现。它过滤空 `localUri`，upsert 发现的歌曲，并且只在覆盖的 source kind 中把缺失歌曲标记为 unavailable。它从 `favorite_song` 派生收藏状态，而不是从 local song 表本身派生。

专辑和歌手分组规则集中在 `MusicLibraryProjector` 和相关 identity model 文件中：

- 专辑按归一化专辑标题分组，并生成类似 `album:<normalized>` 的 ID。
- 歌手按归一化歌手名分组，并生成类似 `artist:<normalized>` 的 ID。
- 详情页从队列快照、完整本地歌曲、首页 preview 和收藏中构建候选歌曲，然后按 song id 去重。

近期 git 历史显示专辑/歌手详情页已改为移除 demo 列表，并使用真实曲库派生的详情列表。不要为生产详情页重新引入固定 demo 歌曲。

## 播放领域

`PlaybackModels.kt` 定义平台无关播放状态：

- `PlaybackStatus`：idle/loading/playing/paused/buffering/ended/error。
- `PlaybackMode`：loop all、loop one、shuffle。`LoopAll` 保持旧快照兼容，同时表示顺序循环行为。
- `PlaybackErrorType`：missing file、unsupported format、permission denied、engine unavailable、unknown。
- `PlayableMedia`：当前阶段只支持本地，使用 scanner 提供的 `localUri`，例如 Android `content://`、iOS/Desktop `file://` 或平台特定本地 URI。
- `QueueState`：有序 song id、当前 index、播放模式和 shuffle 记账。
- `PlaybackSnapshot`：用于恢复的持久化播放 + 队列状态。

`PlaybackCoordinator` 是共享编排层。它负责：

- 从被点击歌曲及其当前 context list 构建 queue state。
- 在委托平台 engine 前把播放状态设为 loading。
- 在歌曲播放时记录真实播放历史。
- 向 `AudioPlayerEngine` 发送 queue/mode/play/pause/seek 命令。
- 收集 engine events 并写回 `PlaybackRepository`。
- 拥有 shuffle navigation、失败策略、快照节流和冷启动恢复规则。
- 将快照恢复为 paused，并过滤 unavailable 歌曲。

播放 PRD（`docs/PLAYBACK_PRD.md`）说明真实 engine events 应是播放事实的最终来源。UI 可以乐观响应，但平台 engine events 必须修正状态。

## 收藏和搜索

收藏在持久化中基于 song id（`favorite_song`），并投影成 UI 列表。在已检查源码中，收藏页中的专辑和歌手由喜欢的歌曲派生，而不是单独的规范 favorite table。

搜索有两个 context（`SearchContext.LocalLibrary`、`SearchContext.Favorites`）和 scope（`All`、`Songs`、`Albums`、`Artists`）。`MusicAppController` 使用 `SearchSessionController` 管理 query state、active debounced query 和 search history。`PersistentSearchHistoryRepository` 按 context 存储历史。

## 产品工作流影响

- 点击歌曲应通过 controller 播放，并传入完整 context queue。这样用户从首页、搜索、收藏、专辑详情或歌手详情开始播放时，队列行为保持一致。
- 歌手和专辑详情的播放队列应包含完整匹配详情列表，而不只是被点歌曲或短“热门”子集。
- 从收藏进入搜索时，应保持 favorites-context search。
- 切换根 Tab 应清空二级页面；二级 stack 导航应先返回经过的二级页面，再返回根 Tab。

## 当前领域注意事项

- `MergeLocalMusicScanResultUseCaseImpl` 仍存在并作为纯合并路径被测试，但 Android/Desktop 持久化 runtime 通过 `ScanLocalMusicUseCaseImpl` 使用 `PersistentMusicLibraryRepository.applyScanResult`。
- `PlaybackDatabase` 名称指向播放，但现在拥有更广泛的本地 App 持久化。
- Repository API 是同步的，而一些实现用 `runBlocking` 包装 suspend DAO 调用；添加昂贵 UI 路径调用时要小心。
- 已检查源码中 iOS scanner 元数据不如 Android/Desktop 完整；除非源码变化，不要宣称 parity。
