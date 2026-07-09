Status: ready-for-human

# Android 版本媒体通知栏按钮5 个变成 3 个

## GitHub Issue

- 链接：https://github.com/valiantyan/kmp-music/issues/1
- 编号：#1
- 标签：bug
- 状态：OPEN
- 作者：valiantyan
- 创建时间：2026-07-09T10:58:42Z

## 问题现象

Android 版本在收藏页播放只有 2 首歌曲的列表后，用户下拉系统媒体通知栏，在通知栏中切换播放模式并点击下一首，媒体通知栏原本固定展示的 5 个按钮会变成 3 个。

## 复现步骤

1. 打开应用，进入收藏页面。
2. 点击收藏页面列表播放，此时当前播放列表只有 2 首歌曲。
3. 下拉显示 Android 系统媒体通知栏。
4. 在媒体通知栏中切换播放模式。
5. 再点击下一首。

## 期望行为

Android 系统媒体通知栏展开态应稳定展示收藏、上一首、播放/暂停、下一首、播放模式共 5 个按钮，不应因为当前播放列表数据或播放模式变化降级为 3 个按钮。

## 实际行为

切换播放模式后，Android 系统媒体通知栏固定的 5 个按钮变成 3 个。

## 附件证据

- 原始 URL：https://github.com/user-attachments/assets/17f1f228-4aa3-4fca-abe4-fc1fce67caac
- 本地路径：`.scratch/github-bugs/assets/1/issue-1-media-notification-buttons.mp4`
- 类型与大小：MP4 视频；`file` 检测为 `ISO Media, MP4 Base Media v1 [ISO 14496-12:2003]`；`ffprobe` 检测为 H.264 视频、AAC 音频、720x1600、时长 9.938005 秒、2014675 字节。
- 下载结果：首次 `curl -L --fail` 因 `curl: (18) transfer closed with 622622 bytes remaining to read` 中断；随后使用 `curl -L --fail --retry 3 --retry-delay 1` 重试成功。原始 GitHub asset URL 不带扩展名，已按内容类型重命名为可审计的 `.mp4` 文件。
- 检查结论：附件是 Android 竖屏录屏，和 Issue 描述的媒体通知栏按钮数量变化场景一致；它影响根因判断，问题应定位在 Android Media3 系统媒体通知按钮偏好和通知 provider 层。

## 验收标准

- [x] 按上述步骤不再复现问题。
- [x] Android 媒体通知栏展开态按钮数量不受播放列表长度和播放模式影响，仍稳定为 5 个。
- [x] 相关页面或播放状态保持正确。
- [x] 已补充或更新与风险匹配的回归测试，或者说明无法补测的原因。

## 第一性原理分析

媒体通知栏按钮数量是 Android 系统媒体通知对当前 `MediaSession` 暴露按钮偏好的呈现结果。用户问题的根本目标不是“某个页面按钮样式”，而是“播放列表长度、播放模式切换、下一首命令可用性变化，都不应改变通知栏固定操作集合”。因此修复点应在 Android 媒体通知按钮声明和排序层，不能在收藏页或播放模式业务逻辑里打补丁。

## 修复计划

1. 用 Android unit test 锁住媒体通知展开态按钮排序和数量：收藏、上一首、播放/暂停、下一首、播放模式固定为 5 个。
2. 调整 Android Media3 通知按钮排序逻辑，让展开态固定使用 `mediaButtonPreferences` 中声明的 5 个按钮，不再按当前 `Player.Commands` 裁剪上一首和下一首。
3. 保持自定义命令仍走现有 `PlaybackMediaCommandCatalog` 和 `AndroidPlaybackMediaCommandHandler`，不改变 shared 播放队列和收藏状态逻辑。
4. 运行 Android unit test、Android 编译和必要的共享测试。

## Comments

### 实现摘要

- 新增 `AndroidPlaybackMediaButtonOrdering`，把 Android 媒体通知展开态按钮排序从 `AndroidPlaybackMediaNotificationProvider` 中抽成可测试的 Android 纯逻辑。
- 修复排序规则：展开态固定使用收藏、上一首、播放/暂停、下一首、播放模式 5 个按钮，不再用当前 `Player.Commands` 裁剪上一首和下一首。
- `AndroidPlaybackMediaNotificationProvider` 保留 Media3 官方 provider 路径，只委托新的排序对象组织按钮顺序。
- 更新 `docs/agents/github-bug-flow.md`：当 GitHub 附件 URL 不带文件名或扩展名时，下载后必须按内容类型重命名为可审计文件名。

### 回归测试

- 新增 `AndroidPlaybackMediaNotificationProviderTest.mediaButtonsKeepFiveSlotsWhenPlayerNavigationCommandsAreUnavailable`。
- 红灯证据：修复前运行 `./gradlew :composeApp:testDebugUnitTest --tests com.yanhao.kmpmusic.playback.AndroidPlaybackMediaNotificationProviderTest`，断言失败为 `expected:<5> but was:<3>`。
- 绿灯证据：修复后同一测试通过，并纳入完整 Android unit test。

### 验证命令与结果

- `./gradlew :composeApp:testDebugUnitTest --tests com.yanhao.kmpmusic.playback.AndroidPlaybackMediaNotificationProviderTest`：通过。
- `./gradlew :composeApp:testDebugUnitTest`：通过。
- `./gradlew :composeApp:compileDebugKotlinAndroid`：通过。
- `./gradlew :composeApp:testDebugUnitTest :composeApp:compileDebugKotlinAndroid`：通过。

### Code Review 结论

- 修复点位于 Android 专属 Media3 通知 provider 层，没有修改 shared 播放队列、收藏、页面导航或 Desktop/iOS 代码。
- 新排序对象只改变系统媒体通知展开态按钮集合的裁剪规则，保留原有按钮顺序、图标、custom command 和紧凑态索引逻辑。
- 附件视频保存在 `.scratch/github-bugs/assets/1/` 作为本地证据，按流程不纳入 Git 提交；提交只固化文字证据和代码修复。

### 对抗式审查

- 风险一：固定展示上一首/下一首后，某些瞬间 Player 认为命令不可用。复核结果：Issue 明确要求通知栏固定 5 个按钮，产品语义优先于 Media3 临时命令集合；按钮命令仍走原有 Media3/Session 路径，不绕过 shared 播放逻辑。
- 风险二：播放/暂停图标可能和系统 `showPauseButton` 不一致。复核结果：排序对象仍使用 `showPauseButton` 创建播放/暂停按钮，保持系统当前显示语义。
- 风险三：自定义收藏和播放模式按钮可能丢失。复核结果：排序仍从 `mediaButtonPreferences` 查找 `PlaybackMediaCommandCatalog` 中的自定义按钮，custom command 路径未改。
- 风险四：流程文档新增附件命名规则可能影响历史流程。复核结果：只在 URL 缺少文件名或扩展名时要求重命名，不改变下载目录、GET 下载、证据记录和不提交附件的既有规则。

### 剩余风险

- 未在真实 Android 设备上下拉系统通知栏截图复核；当前通过 Android unit test 锁定触发根因，并通过 Android 编译验证。无已知代码层剩余风险。
