# iOS 持久化播放会话装配规格

Status: ready-for-agent

## Problem Statement

iOS 用户当前可以在同一进程内通过播放会话播放导入曲库中的歌曲，但该会话直接构造共享控制器，未接入现有的持久化控制器装配。Android 与 macOS 已通过同一持久化装配保存本地曲库、收藏、本地自建歌单、搜索历史、本地音频发现偏好和播放快照；iOS 重启后却没有同等的可验证保证。

这会让 iOS 用户无法确信导入曲库和播放上下文会在下次打开 App 时仍然存在，也使维护者必须跨三个平台装配模块推断哪些用户状态会保存。用户需要 iOS 播放会话具备与 Android、macOS 一致的本地持久化结果，同时保持 iOS 的 AVFoundation、App 沙盒导入曲库和宿主生命周期语义。

## Solution

在 iOS 平台会话中接入现有持久化控制器装配，让 iOS 用平台数据库 adapter 提供数据库，继续由 common 层管理本地曲库、收藏、本地自建歌单、搜索历史、本地音频发现偏好和播放快照。iOS 会话仍独立拥有 AVFoundation 播放器、audio session、会话作用域和关闭时序；Android 与 macOS 保持各自已经成立的装配和生命周期差异。

该方案复用现有持久化控制器装配 seam，不新增三端共同的播放会话 interface。三端的资源所有权和宿主时序不同，强行抽象会形成浅 module；现有 seam 已有 Android、macOS 两个 adapter，iOS 作为第三个 adapter 能带来真实 leverage，并把 iOS 持久化问题的 locality 集中在平台会话装配。

## User Stories

1. 作为一名 iOS 本地音乐用户，我希望导入的歌曲在重新打开 App 后仍出现在本地曲库中，以便不必重复导入。
2. 作为一名 iOS 本地音乐用户，我希望已收藏的歌曲在重新打开 App 后仍保持收藏状态，以便继续访问常听内容。
3. 作为一名 iOS 本地音乐用户，我希望本地自建歌单及其歌曲顺序在重新打开 App 后保持不变，以便继续整理本机音乐。
4. 作为一名 iOS 本地音乐用户，我希望本地音频发现偏好在重新打开 App 后仍然生效，以便导入规则不会回到默认状态。
5. 作为一名 iOS 本地音乐用户，我希望搜索历史在重新打开 App 后仍可用，以便快速继续查找本地曲库内容。
6. 作为一名 iOS 本地音乐用户，我希望上次的播放队列、当前歌曲、播放模式和可恢复的进度被保存，以便再次打开 App 时能恢复合理的播放上下文。
7. 作为一名 iOS 本地音乐用户，我希望同一 App 会话内的 Compose UI 重组不会创建新的播放器或丢失当前歌曲，以便播放持续稳定。
8. 作为一名 iOS 本地音乐用户，我希望关闭会话时播放器和 audio session 被正确释放，以便下次进入 App 不会保留失效的原生资源。
9. 作为一名 Android 用户，我希望 iOS 持久化改动不改变 Android 系统媒体库来源和后台播放行为，以便已有使用方式不回归。
10. 作为一名 macOS 用户，我希望 iOS 持久化改动不改变 Desktop 扫描目录累加、AVFoundation 播放和退出收口行为，以便桌面端不受影响。
11. 作为一名开发者，我希望 iOS 平台会话在一个明确的 seam 组装数据库、扫描器、播放 engine 和共享控制器，以便定位持久化问题时不必在 UI 和业务模块间跳转。
12. 作为一名开发者，我希望继续通过 common 播放协调器处理队列、播放模式、失败策略和快照语义，以便平台 adapter 只承接真实运行时差异。
13. 作为一名开发者，我希望 iOS 关闭过程即使播放器或 audio session 释放失败仍尽力保存可用的播放状态并关闭数据库，以便失败不会遗留可复用资源。
14. 作为一名测试维护者，我希望通过会话可观察行为验证恢复幂等、持久化和关闭顺序，以便测试不绑定实现细节。
15. 作为一名发布维护者，我希望 iOS framework 或 iOS 测试任务先被实际查证再执行，以便不把猜测的 Gradle 任务写成通过证据。
16. 作为一名架构维护者，我希望原始本地音频来源的可用性与 App 沙盒副本不被混为一谈，以便后续来源可用性工作不会绕过 ADR-0001。

## Implementation Decisions

- 修改 iOS 进程级播放会话 module：它继续是 UI 宿主访问共享控制器、请求一次冷启动恢复并关闭原生资源的唯一入口；其 implementation 改为从现有持久化控制器装配取得控制器。
- 新增 iOS 平台数据库 adapter，复用 common 的数据库配置、迁移和持久化 repository 装配；数据库文件位置由 iOS 平台私有存储决定，不向 commonMain 泄漏 Foundation、UIKit 或 AVFoundation 类型。
- iOS 平台会话在同一会话作用域内装配数据库、本地音频发现、AVFoundation `AudioPlayerEngine` 和共享控制器。持久化控制器装配是本票采用的最高既有 seam；不为这一次 iOS 接入新建 common 播放会话 interface。
- iOS 关闭时必须保持明确资源所有权：停止并等待播放 engine 收口，释放 audio session，停止会话作用域，持久化最终可恢复播放快照，并关闭数据库。任一步骤失败时，其余可安全收口步骤仍应执行，并向调用方保留失败事实。
- 冷启动恢复保持每个进程会话最多一次，并且仅恢复持久化控制器当前可解析的歌曲。原始来源身份和失效 reconciliation 尚未建模，本票不把该行为扩展为来源 liveness 承诺。
- Android 的进程级 Media3 会话和 macOS 的 Desktop AVFoundation 会话不重构为共同 module。它们是用于验证持久化装配结果和生命周期策略的既有 adapter，而不是本票要统一的 interface。
- 保持 `MusicAppController -> PlaybackCoordinator -> AudioPlayerEngine -> 平台实现` 主链路。队列推进、随机、单曲循环、失败策略和快照过滤继续留在 common 层；iOS 只实现平台装配与原生资源生命周期。
- 本票不改变 iOS 导入曲库的产品定义，也不把沙盒副本视为绕过来源可用性的永久保活方案。原始来源身份、来源失效后的 reconciliation 和端到端证据作为独立风险记录，不能被本票的持久化成功掩盖。

## Testing Decisions

- 好测试只验证用户和宿主可观察的结果：重新创建会话后数据是否存在、恢复是否幂等、关闭是否完整收口；不检查 private 字段、构图顺序的偶然细节或具体数据库实现。
- 最高测试 seam 是现有持久化控制器装配与 iOS 进程级会话的可观察生命周期。测试从会话取得控制器、触发恢复和关闭，并通过重建会话验证持久化结果；不为测试另造三端共同 interface。
- iOS 平台测试必须覆盖：持久化本地曲库、收藏、本地自建歌单、搜索历史和本地音频发现偏好；保存并恢复播放队列、播放模式、当前歌曲和进度；重复恢复不覆盖活动会话；关闭等待播放 engine、audio session、会话作用域、快照和数据库收口；释放失败时的后续收口；Compose UI 重组持续复用同一控制器。
- 对 iOS 导入曲库，要记录一项端到端验收缺口：现有 App 沙盒导入结果与 ADR-0001 所要求的原始来源可用性之间尚未有可信的失效证据。本票不测试或声称原始来源失效后的 reconciliation 已完成。
- 延续现有 `IosPlaybackSessionRuntimeTest` 的恢复幂等和关闭时序先例，延续 `DesktopPlaybackSessionTest` 的持久化重建、快照恢复、异常收口和数据库关闭先例，并以 common 的播放快照、持久化 repository 与 `MusicAppController` 测试守住共享语义。
- 验收前先通过 `./gradlew :composeApp:tasks` 确认当前 iOS framework 或 iOS 测试任务名称，再运行对应任务；运行 `./scripts/verify-local.sh` 作为 Android 编译与 Desktop 测试回归哨兵；并运行 `./gradlew :composeApp:macosAvFoundationBridgeSmoke :composeApp:macosAvFoundationDefaultRuntimeSmoke`，满足 ADR-0005 的 macOS AVFoundation smoke 门禁。真实 iOS App 重启、导入后可见性和来源失效行为仍需要设备或宿主级证据。

## Out of Scope

- Android Media3 会话、系统媒体库来源、权限接线或后台播放行为的重写。
- macOS AVFoundation bridge、Desktop 扫描目录累加、关闭窗口后的播放策略或非 macOS Desktop 真实播放。
- 新建三端通用播放会话 interface、把平台生命周期强行移动到 commonMain，或修改既有 `AudioPlayerEngine` 契约。
- iOS 系统音乐资料库、Now Playing、锁屏控制、远程命令、AirPlay 和后台播放产品能力扩展。
- 改变 iOS 导入曲库的来源定义、为沙盒复制文件设计永久保活，或在本票内重开 ADR-0001。
- 原始来源身份持久化、来源失效 reconciliation 的产品与数据模型设计；这需要单独的架构决策和端到端证据，不能作为本票的附带重构。

## Further Notes

- 本规格根据当前架构审查的 Candidate 02 收敛：Android/macOS 已使用持久化控制器装配，而 iOS 会话存在直接构造控制器的差异。用户尚未选择候选；依照 `to-spec` 的“不访谈、只综合已讨论内容”规则，本票将 Candidate 02 作为明确范围假设。
- `ready-for-agent` 只表示实现信息足以开始，不表示 iOS 本地音频来源 liveness 已通过。实施完成后，必须把持久化验证与原始来源可用性验证分开记录，避免“沙盒中仍有副本”被误写为“原始来源仍可访问”。
- ADR-0005 继续约束 iOS AVFoundation 和 common 播放契约；ADR-0001 继续约束本地音频发现以来源可访问性为准。本规格不推翻二者。
