# KMP 架构边界

本文承接根 `AGENTS.md` 的架构细则。只有任务触及分层、数据能力、播放链路、扫描来源、平台目录或依赖方向时才需要读。

## 必守分层

- `domain` 放领域模型、Repository 接口、UseCase、播放协调和持久化契约，不依赖平台 API。
- `data` 放共享数据实现、持久化 Repository、扫描合并、数据库工厂和本地音频规则。
- `feature` 放共享 UI、状态、导航、chrome、页面和显示模型；UI 不直接依赖平台实现。
- `androidMain`、`iosMain`、`desktopMain` 只放平台入口、权限、媒体扫描、真实播放、数据库实际构造和系统适配。
- 新增跨层能力时，先定义 Repository、UseCase、平台数据源契约或 `expect/actual`，再写 `Impl`。

## 产品与平台语义

- Android 使用单一系统媒体库来源；不要引入 Android 文件夹来源或多来源管理语义。
- Desktop/macOS 使用可累加的扫描目录；只有用户显式移除目录时才删除该来源。
- iOS 使用导入曲库或系统音乐资料库语义；不要套用 Android 系统媒体库或 Desktop 文件夹扫描模型。
- 本地音频发现以来源文件仍存在且可访问为准；扫描取消时，已成功验证的新结果可以保留，尚未处理的既有歌曲不能仅因取消而移除。

## 播放链路

- 播放主链路保持 `MusicAppController -> PlaybackCoordinator -> AudioPlayerEngine -> 平台实现`。
- 队列推进、随机、单曲循环、失败策略和快照语义优先留在 common 层。
- Apple 平台播放统一走 AVFoundation；macOS 通过 native bridge，iOS 通过 Kotlin/Native 与 AVFoundation。
- 不复活 vlcj / LibVLC 生产链路；也不要把 Desktop 真实播放泛化为 Windows / Linux 已支持。

## 数据和持久化

- 数据库迁移、持久化 mapper、播放快照、扫描合并这类状态语义必须优先补测试。
- Repository、UseCase 和数据源命名要贴合 `CONTEXT.md`；如果领域概念缺失，先标记建模需求，不发明竞争术语。
- 不为一次性调用新增抽象；抽象必须隔离平台差异、降低真实复杂度或复用既有模式。

## 常见风险

- 把 MediaStore、AVFoundation、文件系统或权限 API 泄漏到 `commonMain`。
- 在页面级 Composable 中处理扫描、播放、通知或权限业务。
- 为了局部 UI 问题复制数据状态或绕过 `MusicAppController`。
- 修改 ADR 已经明确的来源语义，却没有先说明冲突和取舍。
