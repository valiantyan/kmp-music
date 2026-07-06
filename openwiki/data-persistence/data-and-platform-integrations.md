# 数据、持久化和平台集成

本页梳理存储和平台适配层。修改本地扫描、播放引擎、数据库 schema 或打包/runtime 集成时，应从这里开始。

## 持久化概览

`composeApp/build.gradle.kts` 为 Android、iOS arm64/simulator arm64 和 Desktop KSP target 配置了 Room3。Schema 导出目录是 `composeApp/schemas`。

主数据库类是 `domain/persistence/PlaybackDatabase.kt` 中的 `PlaybackDatabase`。尽管名称如此，它存储的是更广泛的本地 App 状态：

| Table/entity | 用途 |
| --- | --- |
| `playback_snapshot` | 单条保存的播放快照记录。 |
| `playback_queue_item` | 有序队列歌曲 ID。 |
| `playback_history_item` | 真实最近播放历史。 |
| `favorite_song` | 喜欢的歌曲 ID。 |
| `local_song` | 扫描到的本地歌曲元数据和可用性。 |
| `search_history` | 按搜索 context 保存的最近搜索词。 |

已检查数据库版本为 `5`。`PlaybackDatabaseMigrations.kt` 中的 migration 添加了本地歌曲、cover URI、搜索历史和播放历史。文件注释明确禁止 destructive migration，因为播放快照和收藏数据必须在升级中保留。

`PlaybackDatabaseFactory.kt` 使用 `BundledSQLiteDriver`、migrations 和 `Dispatchers.IO` 构建数据库。

## Repository 实现

Common data 实现位于 `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/data`。

### 内存/fake 实现

默认 controller 构造、common 测试以及非平台/fake 场景使用：

- `InMemoryMusicLibraryRepository`
- `InMemoryFavoritesRepository`
- `InMemoryPlaybackRepository`
- `InMemorySearchHistoryRepository`
- `InMemoryUserPreferencesRepository`
- `FakeLocalMusicScanner`
- `FakeLocalMusicDemoCatalog`
- `FakeAudioPlayerEngine`

fake scanner 近期支持 500 首歌曲 demo/压力 catalog，由 `FakeLocalMusicScannerTest` 覆盖，并被 Android 性能 harness 使用。

### 持久化实现

- `PersistentMusicLibraryRepository`：Room-backed 本地歌曲元数据、扫描结果合并、source availability 标记、统计、专辑/歌手投影、收藏派生。
- `PersistentFavoritesRepository`：Room-backed 喜欢歌曲 ID。
- `PersistentPlaybackRepository`：运行时播放状态，以及持久化快照/队列/历史 DAO 交互。
- `PersistentSearchHistoryRepository`：按 context 隔离的搜索历史。
- `RoomPlaybackSnapshotStore`：在一个 Room-backed 抽象中恢复和保存快照/队列。

Android 和 Desktop controller factory 会注入这些持久化实现，因此接近生产的 session 使用 Room-backed 状态。iOS 入口目前用 `IosFolderMusicScanner` 创建默认 controller，因此除非后续修改，否则使用默认内存 repository。

## 本地音乐扫描集成

### Android MediaStore

来源：

- `androidMain/data/AndroidMediaStoreScanner.kt`
- `androidMain/data/AndroidMediaStoreMetadataReader.kt`
- `androidMain/AndroidAudioPermissionRequester.kt`
- `androidMain/MainActivity.kt`

Android 扫描器查询 `MediaStore.Audio.Media`，过滤音乐条目，通过 Activity 接线请求/处理音频权限，把平台元数据映射为 `MusicFileMetadata`，并提供可播放的 `content://` URI。MainActivity 还会为播放通知可见性请求 Android 13+ notification permission。

不要把 `ContentResolver`、Android 权限代码或 MediaStore API 移到 `commonMain`。

### Desktop 文件夹扫描器

来源：

- `desktopMain/data/DesktopFolderMusicScanner.kt`
- `desktopMain/data/DesktopAudioMetadataReader.kt`
- `data/LocalAudioFileRules.kt`

Desktop 扫描器提示用户选择文件夹，递归扫描支持的音频文件，并尽可能使用 jaudiotagger 读取元数据。它返回 Desktop file URI 以及 source summaries/problems。本地音频规则和 desktop metadata reader 有针对性测试。

不要静默扫描整块磁盘或系统文件夹；PRD 要求用户显式选择。

### iOS 文件夹扫描器

来源：

- `iosMain/data/IosFolderMusicScanner.kt`
- `iosMain/IosEntry.kt`

iOS 入口注入 `IosFolderMusicScanner`。本地音频 PRD 说明 iOS P0 应使用显式用户选择/导入语义，而不是 Android 风格的整机扫描。在已检查源码中，iOS 扫描器的元数据提取看起来不如 Android/Desktop 完整，也没有看到 iOS 真实播放引擎集成。

### Fake scanner

`FakeLocalMusicScanner` 和 `FakeLocalMusicDemoCatalog` 是 common fallback/demo/stress 工具。它们对测试和非平台默认 controller 构造有用，但当存在真实曲库数据时，生产页面不应使用硬编码详情 demo 歌曲列表。

## 播放集成

### 共享播放边界

`domain/playback/AudioPlayerEngine` 是 `PlaybackCoordinator` 使用的平台无关引擎接口。`PlaybackRepository` 存储运行时状态；`PlaybackSnapshotStore` 持久化/恢复快照。coordinator 是共享的队列、模式、历史和失败策略来源。

### Android Media3 runtime

关键文件：

- `androidMain/playback/MusicPlaybackService.kt`
- `androidMain/playback/PlaybackServiceConnector.kt`
- `androidMain/playback/MediaControllerConnection.kt`
- `androidMain/playback/MediaControllerEventBridge.kt`
- `androidMain/playback/AndroidPlayableMediaMapper.kt`
- `androidMain/playback/AndroidPlaybackMediaSessionCallback.kt`
- `androidMain/playback/AndroidPlaybackMediaNotificationProvider.kt`
- `androidMain/playback/PlaybackMediaCommandCatalog.kt`
- `androidMain/AndroidPlaybackSession.kt`
- `androidMain/AndroidPlaybackSessionRuntime.kt`
- `androidMain/MusicAppViewModel.kt`
- `androidMain/AndroidPlaybackControllerFactory.kt`

Gradle 依赖在 Android source set 中包含 Media3 ExoPlayer/session/ui。

Android 架构使用 process/session 层，让 Activity、playback service、media notification 和系统/media 命令共享 controller。MainActivity 处理类似 `ACTION_OPEN_PLAYER` 的通知 intent，并能让 Android 导航栏颜色与全屏播放器背景同步。

### Desktop vlcj/LibVLC runtime

关键文件：

- `desktopMain/playback/DesktopVlcjAudioPlayerEngine.kt`
- `desktopMain/playback/VlcjMediaPlayerAdapter.kt`
- `desktopMain/playback/MacosLibVlcRuntime.kt`
- `desktopMain/DesktopAudioRuntimeFactory.kt`
- `desktopMain/DesktopPlaybackSession.kt`
- `desktopMain/DesktopPlaybackSessionRuntime.kt`
- `desktopMain/DesktopPlaybackControllerFactory.kt`

Gradle 依赖包含 `vlcj`、`jaudiotagger`、`kotlinx-coroutines-swing` 和 desktop Ktor client。

macOS Apple Silicon LibVLC 打包在 `composeApp/build.gradle.kts` 和 `desktopMain/packaging/macos-libvlc` 下的辅助脚本中配置。常用任务包括：

```bash
./gradlew :composeApp:prepareMacosArm64LibVlc
./gradlew :composeApp:packageDmg
./gradlew :composeApp:packageReleaseDmg
```

打包 README 说明：`desktopRun` 会复用项目 runtime（如果存在），但不会自动下载 LibVLC；release/test DMG 会把 LibVLC staging 到 `KMP Music.app/Contents/Resources/LibVLC`，使 App 不依赖 `/Applications/VLC.app` 也能播放。

## 数据库路径

已检查源码显示的平台数据库 builder：

- Android：通过 `androidMain/data/AndroidPlaybackDatabase.kt` 使用 app-private `kmp_music_playback.db`。
- Desktop：通过 `desktopMain/data/DesktopPlaybackDatabase.kt` 使用用户 application support 路径，例如 `~/Library/Application Support/KMP Music/kmp_music_playback.db`。

为新 target 编写 migration 或支持文档前，应先验证确切平台路径。

## 扩展建议

添加数据/平台能力时：

1. 先定义或扩展 `domain/repository`、`domain/usecase` 或 `domain/playback` 边界。
2. 把平台 API 保持在对应 source set 内。
3. 如果数据需要跨进程重启保留，添加持久化 schema/migration。
4. 同步更新持久化和内存/fake 实现，让 common 测试继续有用。
5. 围绕合并、恢复、失败行为添加测试，而不只依赖 UI 截图。
6. 修改 Desktop 播放打包时，如果外部 runtime 输入变化，应更新 `desktopMain/packaging/macos-libvlc/README.md` 或源码记录。

## 已知数据/集成注意事项

- `PersistentPlaybackRepository` 和 `RoomPlaybackSnapshotStore` 都会接触播放快照/队列表。当前设计用 store 处理恢复规则和类似事务的快照持久化；repository 则向 controller 呈现运行时状态/历史。修改该拆分时要明确所有权。
- `local_song` availability 是 source-aware：缺失歌曲只会在覆盖的 source kind 内标记 unavailable，不会全局删除。
- 搜索历史按 context 隔离；未经产品确认，不要合并本地曲库和收藏搜索历史。
- 已检查入口源码不能证明 iOS 持久化/播放已经对齐。
