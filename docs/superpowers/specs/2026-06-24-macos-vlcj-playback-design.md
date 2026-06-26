# macOS vlcj 真实播放设计

## 背景

KMP Music 已经完成 Android 真实播放链路：App 内控制、队列、播放模式、错误恢复、快照恢复和媒体通知都通过 shared `MusicAppController -> PlaybackCoordinator -> AudioPlayerEngine` 路径协同。Desktop 当前只接入了 `DesktopFolderMusicScanner`，播放引擎仍使用 common fake engine，因此 macOS 扫描到本地音乐后无法进行真实音频播放。

本设计聚焦 macOS Desktop 首版真实播放。目标是在不污染 `commonMain`、不复制 Android 通知/service 体系的前提下，用 `vlcj / LibVLC` 接入真实本地音频播放，并保持 App 内操作语义与 Android 一致。

## 已确认决策

- 首版目标平台是 macOS Apple Silicon。
- 正式交付的 DMG 必须内置 LibVLC，用户安装 App 后无需另行安装 VLC media player。
- 开发阶段可以临时 fallback 到本机已安装 VLC，用于验证播放链路；该 fallback 不作为用户交付路径。
- Universal 包、Intel macOS、Windows、Linux 和桌面系统媒体键不是本轮验收范围。
- macOS 不实现 Android 的 MediaSessionService、前台通知、系统媒体协议或通知按钮。
- shared 队列、循环、随机、失败跳过和快照恢复继续由 `PlaybackCoordinator` 负责。

## 外部参考结论

调研过的 KMP / Compose Desktop 音乐项目里，真实桌面播放常见路线是 `vlcj / LibVLC` 或 JavaFX MediaPlayer。`SEAbdulbasit/MusicApp-KMP` 在 desktop source set 中依赖 `uk.co.caprica:vlcj`，运行时用 `NativeDiscovery().discover()` 找 LibVLC，并在 macOS 使用 `CallbackMediaPlayerComponent`。它的 README 将 VLC media player 列为桌面要求，因此更像“用户预装 VLC”的交付方式。

我们借鉴它的 vlcj 初始化、macOS component 选择和事件监听方式，但不照搬它的队列架构。KMP Music 已经有平台无关 `AudioPlayerEngine` 和 `PlaybackCoordinator`，desktop engine 只负责底层播放事实，不拥有业务队列规则。

参考：

- `SEAbdulbasit/MusicApp-KMP` 桌面播放器：https://github.com/SEAbdulbasit/MusicApp-KMP/blob/ac05ceb542967fbe1ac09d0e0ba86c4c18effc4d/shared/src/desktopMain/kotlin/musicapp/player/MediaPlayerController.desktop.kt
- `SEAbdulbasit/MusicApp-KMP` vlcj 依赖：https://github.com/SEAbdulbasit/MusicApp-KMP/blob/ac05ceb542967fbe1ac09d0e0ba86c4c18effc4d/shared/build.gradle.kts#L107-L110
- vlcj 项目：https://github.com/caprica/vlcj
- Maven Central vlcj：https://central.sonatype.com/artifact/uk.co.caprica/vlcj
- Maven Central metadata：https://repo1.maven.org/maven2/uk/co/caprica/vlcj/maven-metadata.xml
- VideoLAN macOS 最新稳定包目录：https://download.videolan.org/pub/videolan/vlc/last/macosx/
- VideoLAN 法务与再分发说明：https://www.videolan.org/legal.html
- Apple 公证指南：https://developer.apple.com/documentation/security/notarizing-macos-software-before-distribution
- Apple Code Signing Guide：https://developer.apple.com/library/archive/documentation/Security/Conceptual/CodeSigningGuide/Procedures/Procedures.html
- Apple TN2206 Code Signing In Depth：https://developer.apple.com/library/archive/technotes/tn2206/_index.html

截至 2026-06-24，Maven Central metadata 显示 `uk.co.caprica:vlcj` 最新 release 字段是 `5.0.0-M4`，但这是 milestone；`4.x` 稳定线最新非 milestone 版本是 `4.12.1`。Caprica 项目页仍以 vlcj 4 为正式使用示例，并明确 vlcj 5 / LibVLC 4 仍属于开发中路线。本项目首版应使用 `uk.co.caprica:vlcj:4.12.1`，不把 `5.0.0-M*` 作为生产依赖。

截至 2026-06-24，VideoLAN 官方 macOS 最新稳定包目录指向 `vlc-3.0.23-arm64.dmg`、`vlc-3.0.23-intel64.dmg` 和 `vlc-3.0.23-universal.dmg`。本轮只支持 Apple Silicon，因此正式 LibVLC 运行时来源固定为官方 `vlc-3.0.23-arm64.dmg`。官方 SHA-256 为 `fc6fac08d87f538517d44aca0c5e7a244b67c8c4cb589bf478363a7315fd5e0d`，并在供应链记录中保存下载 URL、`.sha256`、`.asc`、提取日期、架构、授权文件和源码获取说明。

## 范围

### 本轮包含

- macOS Apple Silicon App 内真实播放本地音频文件。
- 播放、暂停、上一首、下一首。
- 进度上报和播放页 seek。
- 队列与当前歌曲状态同步。
- 播放模式同步：列表循环、单曲循环、随机。
- 自然播放结束后按 shared 规则推进。
- 播放错误进入统一 `PlaybackError` 模型。
- 启动恢复最近一次队列和暂停位置。
- DMG 内置 LibVLC，用户无需安装 VLC。
- 打包产物包含 LibVLC 来源记录和授权说明。

### 本轮不包含

- macOS Now Playing。
- macOS 系统媒体键。
- 菜单栏或系统托盘播放器。
- 后台播放专项验收。
- Intel macOS 和 Universal DMG。
- Windows / Linux Desktop 真实播放。
- 歌词、均衡器、变速、淡入淡出。

## 架构设计

新增 `DesktopVlcjAudioPlayerEngine`，放在 `composeApp/src/desktopMain/kotlin/com/yanhao/kmpmusic/playback` 或同等 desktop 平台包中，实现 common `AudioPlayerEngine`。

```text
MusicApp UI
  -> MusicAppController
  -> PlaybackCoordinator
  -> AudioPlayerEngine
  -> DesktopVlcjAudioPlayerEngine
  -> vlcj
  -> bundled LibVLC
```

职责边界：

- `MusicAppController` 保持现有用户操作入口，不引入 vlcj 类型。
- `PlaybackCoordinator` 继续管理队列、播放模式、失败跳过、快照保存和恢复。
- `DesktopVlcjAudioPlayerEngine` 只包装 LibVLC：初始化、装载队列、play、pause、seek、skip、stop、进度轮询和事件翻译。
- `commonMain` 不出现 JavaFX、vlcj、LibVLC、macOS 文件系统探测或打包路径。
- `desktopMain` 不直接修改 UI state，只通过 `PlaybackEngineEvent` 回流播放事实。

macOS 首版必须使用 headless / callback 路线：`DesktopVlcjAudioPlayerEngine` 可以使用 `CallbackMediaPlayerComponent` 或更低层的 audio-only vlcj adapter，但不得使用 `EmbeddedMediaPlayerComponent`、AWT/Swing 视频 surface 或窗口嵌入组件。原因是本轮只做音频播放，Compose Desktop 不需要 native video peer；引入 embedded AWT 组件会把窗口生命周期、焦点、线程和空白 surface 风险带入首版。未来如果要做视频或可视化，应另开设计，不复用本轮 audio-only engine 决策。

## Desktop 会话

新增轻量桌面进程级会话，例如 `DesktopPlaybackSession`。它对应 Android 的 `AndroidPlaybackSession`，但不包含 service、通知或系统媒体协议。

会话职责：

- 持有长生命周期 `CoroutineScope`。
- 创建 `DesktopFolderMusicScanner`。
- 创建 `DesktopVlcjAudioPlayerEngine`。
- 创建桌面 Room `PlaybackDatabase`。
- 注入 `RoomPlaybackSnapshotStore` 和 `PersistentFavoritesRepository`。
- 创建唯一 `MusicAppController`，供 Compose Window 重组时复用。
- App 启动后请求快照恢复；如果曲库尚未可用，则沿用 controller 当前的延迟恢复规则。
- 窗口关闭时停止并释放 LibVLC 资源，并在必要时补写暂停快照。

桌面数据库路径应落在用户应用数据目录中，例如 `~/Library/Application Support/KMP Music/kmp_music_playback.db`。实现时可以先用 JVM 可读写路径完成验证，但设计目标必须是应用私有数据目录。

## LibVLC 分发与发现

正式交付策略：

- macOS Apple Silicon DMG 内置 LibVLC 运行时文件。
- App 启动或首次播放初始化时，优先且必须从应用包内固定目录发现 LibVLC。
- 正式包的 LibVLC 来源固定为 VideoLAN 官方 `vlc-3.0.23-arm64.dmg`，SHA-256 必须匹配 `fc6fac08d87f538517d44aca0c5e7a244b67c8c4cb589bf478363a7315fd5e0d`；升级版本必须单独评审并更新来源记录。
- 构建脚本应先校验官方 `.sha256`，再从官方 DMG 提取 LibVLC 运行时文件；供应链记录必须包含 URL、文件名、SHA-256、签名校验结果、架构、提取时间和授权说明。
- App 包内建议目录为 `KMP Music.app/Contents/Frameworks/LibVLC/`，其中保留 `lib/`、`plugins/` 和运行所需的同级动态库结构。
- engine 初始化时显式配置 bundle 内 library path 与 plugin path，例如通过 vlcj/JNA library search path 和 LibVLC `--plugin-path` 参数，不能依赖全局 `DYLD_LIBRARY_PATH`。
- `NativeDiscovery` 可作为开发辅助，但 release 构建中不能只依赖系统已安装 VLC。
- 开发环境允许 fallback 到 `/Applications/VLC.app` 或本机 LibVLC，用于快速验证。
- 用户交付环境不要求安装 VLC，也不提示用户去安装 VLC。

错误策略：

- 内置 LibVLC 缺失、架构不匹配或加载失败时，engine 发出 `PlaybackErrorType.EngineUnavailable`。
- UI 错误文案应指向“播放器组件不可用，请重新安装应用或联系开发者”，而不是要求普通用户理解 LibVLC。
- 设计和打包记录必须保留 LibVLC 来源、版本、架构和授权说明。

授权和合规策略：

- VideoLAN 允许在遵守对应开源许可证的前提下再分发；打包产物必须附带 VLC/LibVLC 的 `COPYING`、第三方库授权和源码获取说明。
- `vlcj` 默认是 GPL v3 or later；如果 KMP Music 未来不是 GPL 兼容分发，必须在实施前确认商业授权或替代依赖路线。
- 许可证检查是发布阻断项，不能在 DMG 验收后补。

签名与公证策略：

- 所有 LibVLC 相关 Mach-O 动态库、可执行文件、framework 或 bundle 都属于嵌套代码，必须先完成签名，再签名外层 App，最后签名/公证 DMG。
- 动态库必须位于 App bundle 内的标准代码位置，优先 `Contents/Frameworks/LibVLC/`；不得让主程序或任一嵌套库通过绝对路径、Homebrew 路径或 App 外 `@rpath` 加载动态库。
- 打包阶段必须用 `install_name_tool` 或等价方式把动态库依赖修正为 bundle 内相对路径，例如 `@loader_path` 或受控 `@rpath`，再执行签名。
- release 签名使用 Developer ID Application 证书、Hardened Runtime 和 secure timestamp；公证使用 `notarytool`，成功后对 DMG 或 App staple ticket。
- 优先把提取出的 LibVLC 运行时用本项目 Developer ID 重新签名，减少 Hardened Runtime library validation 风险；只有确认无法同一团队签名时，才评估 `com.apple.security.cs.disable-library-validation` entitlement。
- 签名后不得修改 App bundle 内容；授权文件、来源记录和 LibVLC 文件必须在签名前写入。
- 发布验收必须运行 `codesign --verify --deep --strict --verbose=2`、`spctl -a -t exec -vv`、`otool -L` 外部路径检查，以及隔离属性下的首次启动测试。

## 播放行为

`DesktopVlcjAudioPlayerEngine.setQueue` 接收完整 `PlayableMedia` 队列：

- 过滤或拒绝空队列。
- 保存当前队列和起始下标。
- 把 `localUri` 转换成 vlcj 可播放地址。
- 准备目标媒体并定位到 `startPositionMs`。
- 发出 `CurrentMediaChanged` 和 `StatusChanged(Loading 或 Paused)`，以真实初始化结果为准。

播放命令：

- `play()` 调用 vlcj 播放控制，并等待 vlcj `playing` 事件回流 `PlaybackStatus.Playing`。
- `pause()` 调用 vlcj pause，并回流 `PlaybackStatus.Paused`。
- `seekTo(positionMs)` 调用 vlcj 时间跳转，立即补发一次 `ProgressChanged`。
- `skipToIndex(index)` 只切换到底层目标媒体并准备，不自行决定下一首业务规则。
- `stop()` 停止播放、停止进度轮询并释放当前播放状态。

### Desktop Engine Command Serialization

业界成熟播放器处理快速连续 seek / skip 的核心规则不是让每个按钮回调直接碰底层 player，而是把播放引擎变成单一状态机：一个 owner、一个串行命令队列、明确的 generation token、过期事件丢弃。vlcj 自身也强调会把异步 native events 串行分发，避免错误事件处理造成死锁；本项目还需要在 engine 层再做一次业务命令串行，避免 `PlaybackCoordinator` 快速发出 `skipToIndex -> seekTo -> play` 时和 LibVLC 异步回调交错。

`DesktopVlcjAudioPlayerEngine` 必须满足以下规则：

- 所有公开命令，包括 `setQueue`、`play`、`pause`、`seekTo`、`skipToIndex`、`stop` 和 `release`，统一进入 engine 私有 serial dispatcher / actor / channel；任何 UI 线程、controller 线程或 vlcj callback 线程都不直接调用 LibVLC 控制 API。
- `setQueue`、`skipToIndex` 和底层 media prepare 会创建新的 media generation；vlcj 回调、progress tick 和错误事件必须携带或能映射到当前 generation，过期 generation 的事件全部丢弃。
- `skipToIndex` 优先级高于 `seekTo`：切歌会取消旧媒体上的 pending seek、pending play 和 progress tick；新媒体准备完成后只应用新媒体自己的 `startPositionMs` 或最新 pending seek。
- `seekTo` 使用 latest-wins 规则：同一媒体连续拖动时只保留最后一次 seek 位置；底层 seek 未完成期间到来的旧 seek 结果不能把 UI 进度倒退。
- `play()` 在媒体尚未 ready 时只设置 `pendingPlay = true`；真正调用底层 play 的时机是当前 generation 准备完成之后。`pause()` 必须清除 `pendingPlay`。
- `stop()` 和 `release()` 是栅栏命令，会使当前 generation 失效，并取消 pending seek / pending play / progress polling。
- vlcj `playing`、`paused`、`finished`、`error` 等回调线程只投递事件到 serial dispatcher，不在回调内 re-enter LibVLC。
- engine 向 common 层发出的 `CurrentMediaChanged`、`StatusChanged`、`ProgressChanged`、`Ended` 和 `Error` 必须按当前 generation 线性化后的事实发出。

需要新增 fake adapter 测试覆盖：

- 连续 20 次 `seekTo` 只保留最后一次进度。
- `skipToIndex(A) -> seekTo(old) -> skipToIndex(B) -> play()` 不会把 A 的 seek 或 playing 事件应用到 B。
- `play() -> pause()` 在媒体 ready 前连续触发时，最终状态为暂停且不自动播放。
- release 后到达的 delayed callback 不再更新状态或发出错误。

自然结束：

- vlcj `finished` 事件只翻译为 `PlaybackEngineEvent.Ended`。
- 下一首、单曲循环、随机推进全部由 `PlaybackCoordinator.handleEnded` 决定。
- desktop engine 不在底层事件里调用自己的 `playNextTrack`，避免和 shared 队列规则分叉。

进度：

- 播放中约每 500ms 发出一次 `ProgressChanged`。
- 暂停、停止、失败后停止进度轮询。
- seek 后立即发出当前进度，避免 UI 等待下一轮轮询。

事件线程：

- vlcj 回调线程中不直接调用 LibVLC 控制 API。
- vlcj 回调只把事件转发到 engine 自己的协程 scope 或 channel。
- engine 在协程中统一发出 `PlaybackEngineEvent`，保护 vlcj 线程模型。

## 错误映射

| 情况 | common 错误 |
| --- | --- |
| LibVLC 缺失、初始化失败、架构不匹配 | `EngineUnavailable` |
| 文件路径不存在、文件不可读 | `MissingFile` |
| macOS 文件访问被系统拒绝 | `PermissionDenied` |
| 解码失败、容器或编码不支持 | `UnsupportedFormat` |
| vlcj 只给出泛化 error 且无法判定原因 | `Unknown` |

失败事件必须包含当前 `songId`，无法定位时才允许为 `null`。连续失败跳过、单曲循环失败阈值和错误保留规则继续复用 `PlaybackCoordinator`。

## User-facing Error Actions

错误类型不只用于日志，也要定义用户能采取的行动。UI 文案要说人话，诊断细节留在日志和 debug 信息里。

| common 错误 | 用户文案 | 用户行动 | 播放行为 |
| --- | --- | --- | --- |
| `EngineUnavailable` | 播放器组件不可用，请重新安装应用或联系开发者。 | 不提示安装 VLC；允许用户重试一次，仍失败则保留错误。 | 停止当前播放，不自动跳过。 |
| `MissingFile` | 文件不存在或已移动，请重新扫描本地音乐。 | 提供重新扫描入口；保留当前列表状态。 | 按 `PlaybackCoordinator` 连续失败规则尝试下一首。 |
| `PermissionDenied` | 无法访问该音乐文件，请在系统设置或文件夹授权中允许访问后重试。 | 提供重试或重新选择文件夹入口。 | 暂停当前媒体，不静默跳过用户可能仍想授权的文件。 |
| `UnsupportedFormat` | 当前音频格式暂不支持，已尝试播放下一首。 | 不提供盲目重试；允许查看文件或重新扫描。 | 按连续失败规则尝试下一首。 |
| `Unknown` | 播放失败，已尝试播放下一首。 | 允许重试当前歌曲；debug 日志保留底层 message。 | 按连续失败规则尝试下一首。 |

所有错误 toast / banner 都应包含歌曲名；如果 `songId` 为空，使用“当前歌曲”。连续失败达到阈值后，文案改为“连续多首播放失败，已停止播放”，避免用户误以为 App 仍在正常推进。

## 与 Android 行为对齐

macOS App 内行为应与 Android 已有实现一致：

- 点击歌曲后立即切换当前歌曲，进入 `Loading`。
- 真正播放成功后由 engine 事件校正为 `Playing`。
- 暂停、继续、上一首、下一首、seek、播放模式按钮走同一套 controller/coordinator。
- 迷你播放器、播放页、队列弹层和列表高亮读取同一份 `MusicAppUiState`。
- 播放失败时 UI 保留最近错误，不静默吞掉失败。
- 启动恢复只恢复队列和暂停位置，不自动开始播放。

不与 Android 对齐的部分：

- 不做媒体通知。
- 不做后台服务。
- 不做系统媒体命令。
- 不做通知收藏按钮或通知播放模式按钮。

## 测试与验收

### 自动化验证

- `./gradlew :composeApp:desktopTest`
- `./gradlew :composeApp:compileKotlinDesktop` 或实际存在的 desktop 编译任务。
- 如任务名不确定，先运行 `./gradlew :composeApp:tasks` 查询。

共享测试重点：

- `PlaybackCoordinator` 现有队列、播放模式、失败跳过测试应继续通过。
- 如新增 desktop engine 可测试适配层，应使用 fake vlcj adapter 验证事件映射，不依赖真实音频设备。

### 人工验收

Apple Silicon macOS 环境：

- 扫描一个包含 MP3、FLAC、AAC 的文件夹。
- 点击歌曲后真实播放。
- 迷你播放器和播放页显示真实进度。
- 播放/暂停状态和真实播放器一致。
- seek 后音频跳到目标位置，UI 立即更新。
- 上一首、下一首、队列弹层和播放模式行为与 Android App 内语义一致。
- 自然播放结束后按循环、单曲循环或随机规则推进。
- 删除或移动当前音频文件后播放，UI 显示明确错误。
- 使用不支持或损坏的音频文件时，UI 显示格式或未知错误。
- 退出重开后恢复最近队列和暂停位置，不自动播放。

DMG 验收：

- 在没有安装 VLC media player 的 Apple Silicon macOS 环境中安装 DMG。
- 扫描本地音乐并成功播放，证明内置 LibVLC 生效。
- 如果刻意移除 App 内 LibVLC 目录，播放失败应显示 `EngineUnavailable` 对应用户文案。
- 打包产物包含 LibVLC 来源、版本、架构和授权说明。
- 断开 `/Applications/VLC.app`、Homebrew VLC 和其他系统 LibVLC 后仍可播放。
- `codesign --verify --deep --strict --verbose=2` 对 App 返回有效签名。
- `spctl -a -t exec -vv` 对 App 返回 Developer ID accepted。
- `otool -L` 检查主程序和 LibVLC 动态库，不允许出现 App bundle 外的非系统动态库路径。
- DMG 公证成功并 staple 后，在带 quarantine 属性的首次启动路径下通过 Gatekeeper。

## 实施顺序建议

实施计划应按以下顺序拆分：

1. 添加 desktop vlcj 依赖和 `DesktopVlcjAudioPlayerEngine` 的最小事件桥。
2. 接入 `DesktopPlaybackSession`，让 Desktop 入口注入真实 engine。
3. 验证本机 VLC fallback 下的真实播放、暂停、seek、切歌和错误回流。
4. 添加桌面 Room database builder，接入快照与收藏持久化。
5. 实现 App 包内 LibVLC 发现路径。
6. 调整 DMG 打包，把 Apple Silicon LibVLC 放入应用包。
7. 在无 VLC 安装环境里做 DMG 验收。

## 风险与处理

| 风险 | 处理 |
| --- | --- |
| LibVLC 打包路径或动态库依赖不完整 | 先做最小 bundle 验证，再固化路径发现和验收脚本。 |
| Apple Silicon / Intel 架构混用导致加载失败 | 首版只验收 Apple Silicon，错误映射为 `EngineUnavailable`。 |
| vlcj 事件线程误用导致死锁或崩溃 | 回调中只转发事件，所有命令进入 serial dispatcher。 |
| 快速 seek / skip 导致过期事件覆盖新状态 | 使用 media generation、latest-wins seek 和 stale event drop。 |
| 格式错误无法精确分类 | 可判定时映射 `UnsupportedFormat`，否则映射 `Unknown` 并保留诊断 message。 |
| 内置动态库签名或公证失败 | 动态库放入 `Contents/Frameworks`，修正 install name，先签内层再签 App，最后公证 DMG。 |
| 打包体积增加 | 接受体积换取开箱即用；后续再优化 LibVLC 裁剪。 |
| 授权说明遗漏 | DMG 验收必须检查来源、版本、架构和授权说明文件。 |

## 设计结论

macOS 首版真实播放采用 `vlcj / LibVLC`，以 Apple Silicon DMG 内置 LibVLC 为正式交付标准。实现必须接入现有 `AudioPlayerEngine` 契约，复用 shared `PlaybackCoordinator` 的队列和错误规则。开发期可以借助系统 VLC 快速验证，但最终用户体验必须是开箱即用。
