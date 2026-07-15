Status: closed
Labels: wayfinder:grilling
Parent map: .scratch/apple-platform-playback-wayfinder/map.md
Assignee: codex
Blocked by: 无
Blocks: 确定 vlcj 下线迁移顺序；确定验证和文档门禁

# 确定本地音频来源和权限生命周期

## Question

苹果平台统一播放方案下，本地音频来源、授权和可播放性应如何建模，才能同时覆盖 iOS 导入曲库、iOS 系统音乐资料库和 macOS Desktop 扫描目录？

这个决策需要明确：

- `PlayableMedia.localUri` 是否足够承接 Apple 平台播放来源，还是需要提前引入更明确的 `AudioSource` / 授权载体。
- iOS 文件导入、iOS 系统音乐资料库、macOS 扫描目录和安全作用域书签在“来源仍可访问才可播放”这条 ADR 规则下如何统一。
- DRM、云端条目、权限失效、文件被移动或删除时，engine 应发出哪类统一错误。
- 播放 adapter 是否负责恢复安全作用域或权限，还是只能消费 scanner / repository 已持久化并验证过的来源。

## Comments

- 2026-07-14：当前会话已认领此 ticket，按 grilling 顺序确认苹果平台本地音频来源、授权和可播放性生命周期。
- 2026-07-14 resolution：确认 iOS P0 采用“用户选择音频后复制进 App 沙盒，再扫描和播放沙盒内文件”的来源模型。security-scoped access 只用于导入 / 复制窗口，不进入播放 adapter 的长期职责；`localUri` 指向 App 沙盒内可长期访问的 file URL。外部 Files URL、security-scoped bookmark 和持久外部文件访问不进入首版 AVFoundation 播放迁移，后续如需 Open 模式另开设计。

  首版继续使用 `PlayableMedia.localUri` 和现有 `AudioSource.Local(uri)`，不新增 common 层授权载体。进入 `PlayableMedia` 的 URI 语义必须是“scanner / importer / repository 已确认当前可访问，且可交给平台播放器消费的播放定位”。`sourceKind` / `sourceId` 继续留在曲库层做来源归因、刷新和下线；播放 engine 不负责理解具体来源身份，也不持有平台权限对象。若未来启用 macOS sandbox bookmark 或 iOS 外部文件持久访问，再扩展来源模型。

  iOS 系统音乐资料库作为 P1 候选，不阻塞 P0。只有在用户授权通过、`MPMediaItem.assetURL` 非空、条目不是云端待下载、不是受保护 / DRM 资源，且 AVFoundation 判定可播放时，媒体库条目才能进入 `PlayableMedia` 队列。不满足条件的条目不进入播放队列；如果已在曲库缓存中失效，应由扫描 / 同步链路标记为问题或不可用来源。

  macOS Desktop 首版继续非 sandbox 普通文件 URL 路线。Desktop scanner 继续生成 `file://` URI，AVFoundation bridge 只消费普通本地文件 URL；文件夹删除、移动、外置盘断开或文件权限不足时，播放失败映射为统一错误，并由扫描 / 曲库刷新负责下线或标记不可用。macOS security-scoped bookmark、sandbox entitlements、Mac App Store 权限模型和对应打包验收留到后续单独设计。

  播放 adapter 不负责恢复权限、重新授权或重新扫描。scanner / importer 负责拿权限、复制、扫描和生成 `localUri`；repository 负责持久化来源、可用状态、刷新和下线；`PlaybackCoordinator` 负责处理 `PlaybackEngineEvent.Failed` 后的统一错误状态和失败跳过逻辑；平台 engine / adapter 只做播放前轻量可访问性检查和 AVFoundation 错误归一化。adapter 不弹权限 UI、不启动 Document Picker、不恢复 bookmark、不改曲库数据库。

  首版不扩展 `PlaybackErrorType`，按现有五类映射 Apple 来源和权限失败：文件不存在、沙盒文件被删、桌面文件移动或外置盘断开映射为 `MissingFile`；文件不可读、security scope 失效、媒体库授权撤销或普通文件权限不足映射为 `PermissionDenied`；AVFoundation 判定不可播放、编码 / 容器不支持、DRM / 受保护 asset、云端未下载且无本地 asset URL 映射为 `UnsupportedFormat`；bridge / AVPlayer 初始化失败或 native bridge 缺失映射为 `EngineUnavailable`；其他未归类 AVFoundation 错误映射为 `Unknown`。

  对抗式审查结论：第一，如果 iOS 继续直接播放外部 Files URL，当前扫描结束释放 security scope 的实现会导致播放生命周期不可靠，因此 P0 必须复制进沙盒。第二，如果现在扩展 common 授权载体，会提前把未实现的 bookmark / 外部权限模型带进 `commonMain`，与 04 号决策的边界冲突。第三，如果让 adapter 负责恢复权限，它会变成权限管理、导入、曲库修复和播放的混合体，破坏职责边界。第四，DRM / 云端 / 权限失效虽然语义更细，但首版 UI 和协调器已有错误枚举可承接，先映射现有五类能降低迁移面。
