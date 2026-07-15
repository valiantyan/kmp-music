Status: closed
Labels: wayfinder:grilling
Parent map: .scratch/apple-platform-playback-wayfinder/map.md
Assignee: codex
Blocked by: 无
Blocks: 确定 vlcj 下线迁移顺序；确定验证和文档门禁

# 确定苹果系统播放能力范围

## Question

苹果平台统一播放首轮需要承诺哪些系统播放能力，哪些能力只记录为后续范围？

这个决策需要明确：

- iOS 是否在首轮包含后台音频、Now Playing、锁屏控制、耳机线控、音频会话类别和中断处理。
- macOS 是否在首轮包含媒体键、控制中心、菜单栏、后台播放专项验收或仅保留 App 内播放控制。
- 两个平台必须一致的是用户可感知播放语义，还是也要一致接入系统媒体能力。
- 当前 Android 已有媒体通知和系统控制；苹果平台首轮是否需要追平 Android，还是允许平台差异分阶段收敛。

## Comments

- 2026-07-14：当前会话已认领此 ticket，按 grilling 顺序确认苹果平台首轮系统播放能力范围。
- 2026-07-14 resolution：确认 Apple 首轮播放迁移只承诺核心播放能力和必要的生命周期能力，不追平 Android 的媒体通知 / MediaSession 系统控制体验。两端必须一致的是用户可感知播放语义和 `PlaybackCoordinator -> AudioPlayerEngine` 契约，不要求一致接入系统媒体入口。

  iOS P0 包含：播放、暂停、seek、上一首、下一首、播放模式切换、后台继续播放、`AVAudioSession` playback category、audio background mode、中断和输出路线变化的基础处理。后台播放的验收边界是 App 进入后台或锁屏且进程仍存活时音频继续播放，回到前台后当前歌曲、播放状态和进度同步正确。为支撑这个范围，首版允许并要求新增平台级 `IosPlaybackSession`，持有 controller、engine、`AVAudioSession` 配置和生命周期收口，避免播放器生命周期绑死在 Compose `MainViewController()` 的 UI composition 上。

  iOS 首轮不包含：`MPNowPlayingInfoCenter`、锁屏 / 控制中心按钮专项体验、耳机线控、`MPRemoteCommandCenter`、AirPlay 专项验收、用户上滑杀 App 或系统终止进程后的自动恢复播放、冷启动后继续播放。若系统因后台音频自然展示部分信息，不作为首轮验收项，也不得声明已经完成专项系统控制。

  macOS P0 包含：App 内播放、暂停、seek、上一首、下一首、播放模式切换，以及窗口最小化后继续播放。macOS 首轮不包含关闭窗口后继续播放、菜单栏常驻、系统媒体键、控制中心 / Now Playing、后台播放专项验收或远程命令。

  播放模式和队列规则继续由 common `PlaybackCoordinator` 拥有。Apple 平台 adapter 使用单 `AVPlayer` 思路，只把 `play`、`pause`、`seekTo`、`skipToIndex`、`setPlaybackMode` 命令和状态 / 进度 / 结束 / 失败事件接到 `AudioPlayerEngine`；自然结束后发出 `Ended`，由 common 层按顺序、随机、单曲循环等规则决定下一首。`setPlaybackMode` 不得让 AVFoundation 系统队列成为第二个业务真相源。

  对抗式审查结论：第一，如果把 Apple 首轮做成 Android 系统能力对齐，会把迁移目标扩大成 Now Playing、远程命令、耳机线控和锁屏专项项目，超出当前“替换 vlcj / 接入 AVFoundation”的主目标。第二，如果 iOS 要后台播放却不引入 `IosPlaybackSession` 和最小 `AVAudioSession` / background mode 配置，后台能力会依赖 UI 生命周期和偶然行为，不能验收。第三，如果 macOS 把“后台播放”解释成关闭窗口、菜单栏或控制中心能力，会和用户确认的“窗口最小化继续播放即可”冲突。第四，如果播放模式下沉到 `AVQueuePlayer` 或系统层队列，会和现有 common 层队列、随机、循环和失败推进形成双真相源。
