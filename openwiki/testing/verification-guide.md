# 测试与验证指南

本页说明 KMP Music 有哪些检查，以及常见修改应运行什么命令。除非实际运行过，否则不要声称 build 或测试通过。

## 常用命令

来自 `README.md` 和 `AGENTS.md`：

```bash
# Android Kotlin 编译
./gradlew :composeApp:compileDebugKotlinAndroid

# Android debug APK
./gradlew :composeApp:assembleDebug

# 安装到已连接 Android 设备
./gradlew :composeApp:installDebug

# 共享/Desktop 测试套件
./gradlew :composeApp:desktopTest

# 共享逻辑 + Android 编译的快速广泛验证
./gradlew :composeApp:compileDebugKotlinAndroid :composeApp:desktopTest

# 运行 Desktop App
./gradlew :composeApp:run

# 查看可用任务
./gradlew :composeApp:tasks
```

如果不确定任务名，先查看 `:composeApp:tasks`，不要猜。

## 测试位置

| 测试范围 | Path |
| --- | --- |
| 共享 common tests | `composeApp/src/commonTest/kotlin/com/yanhao/kmpmusic` |
| Android unit tests | `composeApp/src/androidUnitTest/kotlin/com/yanhao/kmpmusic` |
| Desktop tests | `composeApp/src/desktopTest/kotlin/com/yanhao/kmpmusic` |
| Room schemas | `composeApp/schemas` |

## 高价值测试文件

### App controller、导航和 UI 状态

- `feature/app/MusicAppControllerTest.kt`：大型覆盖套件，覆盖队列保留、本地音乐二级页面 chrome、首页专辑/歌手加载、路由行为、播放/收藏/搜索交互等 controller 规则。
- `feature/app/navigation/MusicAppNavigationControllerTest.kt`：导航 reducer/system back 行为。
- `feature/app/layout/MobilePlayerOverlayGestureTest.kt`：全屏播放器拖拽关闭阈值行为。
- `feature/app/library/MusicLibraryProjectorTest.kt`：专辑/歌手/详情投影规则。
- `feature/app/favorites/*`：收藏状态同步测试。
- `feature/app/search/*`：搜索 session/history 行为。

修改 controller 方法、导航、chrome 策略、搜索、收藏或播放 UI 投影时，运行或更新这些测试。

### 播放领域

- `domain/playback/PlaybackCoordinatorTest.kt`：队列创建、播放模式、shuffle、engine event 处理、恢复/失败行为。
- `PlaybackQueueNavigatorTest.kt`、`ShuffleQueuePolicyTest.kt`、`PlaybackFailurePolicyTest.kt`、`PlaybackSnapshotWriterTest.kt`、`PlaybackHistoryRecorderTest.kt`：聚焦协作者。
- `domain/playback/FakeAudioPlayerEngineTest.kt`：测试用 fake engine 行为。
- `domain/playback/PlaybackModelsTest.kt`：模型兼容性/行为。

修改队列行为、engine event mapping、播放模式语义、恢复、历史或错误策略时，运行或更新这些测试。

### 持久化和本地曲库

- `data/PersistentMusicLibraryRepositoryTest.kt`：preview limit/sorting、按 source 标记 unavailable、收藏派生、source summaries、counts、cover URI、empty refresh 行为。
- `data/PersistentPlaybackRepositoryTest.kt`：运行时状态保留和冷启动 paused restore。
- `domain/persistence/PlaybackSnapshotStoreTest.kt`：恢复过滤、pause 语义、非法 position reset。
- `data/PersistentFavoritesRepositoryTest.kt`
- `data/PersistentSearchHistoryRepositoryTest.kt`
- `data/LocalAudioFileRulesTest.kt`
- `data/FakeLocalMusicScannerTest.kt`
- `domain/usecase/MergeLocalMusicScanResultUseCaseTest.kt`

修改 Room schema、扫描合并行为、收藏/搜索持久化或本地文件规则时，运行或更新这些测试。

### 页面和主题行为

- `feature/screen/AlbumDetailContentTest.kt`
- `feature/screen/ArtistDetailContentTest.kt`
- `feature/screen/ArtistDetailScrollBehaviorTest.kt`
- `feature/screen/HomeAlbumGridTest.kt`
- `feature/screen/HomeFigmaTokensTest.kt`
- `core/theme/CoverPaletteTest.kt`

修改页面专用数据列表、滚动行为、Figma tokens、调色板提取或详情页 UI 规则时，运行或更新这些测试。

### 平台专用测试

- Android：`androidUnitTest/kotlin/com/yanhao/kmpmusic/playback/AndroidPlaybackMediaCommandHandlerTest.kt` 覆盖 custom media command dispatch。
- Desktop：`desktopTest/kotlin/com/yanhao/kmpmusic/DesktopPlaybackSessionTest.kt`、`data/DesktopPlaybackDatabaseTest.kt`、`data/DesktopAudioMetadataReaderTest.kt` 覆盖 desktop restore/session、DB path/实例化和 metadata fallback。

修改平台适配器、playback service/session 代码、DB builder 或 desktop scanner/audio metadata 代码时，运行平台专用测试。

## 按修改类型选择验证

| 修改类型 | 最小验证 |
| --- | --- |
| 共享 controller/state/navigation/search/favorites/playback | `./gradlew :composeApp:desktopTest`，并更新聚焦 common tests。 |
| Android 入口、MediaStore scanner、Media3 service/session/notification | `./gradlew :composeApp:compileDebugKotlinAndroid`；可行时添加 Android unit tests。 |
| Desktop playback/scanner/database/packaging | `./gradlew :composeApp:desktopTest`；runtime 播放需手动运行 Desktop app，并在需要时准备 LibVLC。 |
| Room schema/entity/DAO/migration | `./gradlew :composeApp:desktopTest`；检查生成的 schema 变化。非平凡行为需添加 migration tests。 |
| 仅 UI 的移动端 screen/token 修改 | `./gradlew :composeApp:compileDebugKotlinAndroid`；如果状态/投影变化，运行相关 common tests。视觉保真重要时使用设备/模拟器/Desktop 截图。 |
| Player chrome/gesture/bottom navigation 行为 | `./gradlew :composeApp:desktopTest :composeApp:compileDebugKotlinAndroid`；更新手势/导航测试。 |
| Album/artist/favorites/home 列表性能 | 运行相关测试；调查滚动卡顿时使用 Android debug performance harness。 |

## Android 性能 harness

`MainActivity.kt` 暴露仅 debuggable 可用的显式 intent 路径：

- Album detail performance harness。
- Favorites 500-row performance harness。

实现位于 `composeApp/src/androidMain/kotlin/com/yanhao/kmpmusic/AndroidPerformanceHarnesses.kt`。它们用于 adb 驱动的 frame/jank 调查，不应影响普通 release 用户路径。修改时确认 debuggable gating。

## Desktop LibVLC 验证

在 Apple Silicon macOS 上验证 Desktop 播放：

```bash
./gradlew :composeApp:prepareMacosArm64LibVlc
./gradlew :composeApp:run
```

`desktopMain/packaging/macos-libvlc/README.md` 中的打包文档列出了 release 检查，包括 SHA-256 验证、无系统 VLC 时启动 App、codesign、spctl、动态库检查、notarization 和 stapling。修改 release packaging 时不要削弱这些检查。

## 验证期间的文档和源码注意事项

- 依赖版本优先参考 `gradle/libs.versions.toml` 和 build files；README/AGENTS 当前在 Kotlin/Compose 版本号上看起来过时。
- 现有产品 PRD 可能描述期望的未来行为；宣称功能完全实现前，应对照源码验证。
- 一些 UI settings/login 行是未来能力。测试它们实际改变的状态，而不是隐含后端行为。
- 已检查源码中未看到 iOS 真实播放 parity；除非存在 iOS engine 且已测试，不要把 iOS 播放标记为已验证。
