Status: ready-for-human

# Android 媒体通知按钮数量受播放模式切换影响

## GitHub Issue

- 链接：https://github.com/valiantyan/kmp-music/issues/1
- 编号：#1
- 标签：bug

## 问题现象

Android 端从收藏页播放只有 2 首歌曲的列表后，在系统媒体通知栏切换播放模式，再点击下一首，通知栏原本固定展示的 5 个媒体按钮会变成 3 个。

## 复现步骤

1. 打开应用，进入收藏页面，点击收藏歌曲列表播放；当前播放列表只有 2 首歌曲。
2. 下拉显示 Android 媒体通知栏。
3. 在媒体通知栏中切换播放模式。
4. 在媒体通知栏中点击下一首。
5. 观察媒体通知栏按钮数量。

## 期望行为

Android 媒体通知栏固定展示的 5 个按钮不应因为播放列表数量或播放模式变化而减少。

## 实际行为

切换播放模式后再点击下一首，Android 媒体通知栏固定展示的 5 个按钮变成 3 个。

## 验收标准

- [x] 按上述步骤不再复现问题。
- [x] 相关播放模式、收藏状态、上一首、播放暂停、下一首按钮状态保持正确。
- [x] 补充或更新与风险匹配的回归测试，或者说明无法补测的原因。

## 修复计划

1. 定位 Android 媒体通知按钮偏好与播放队列、播放模式同步路径。
2. 构造能覆盖“2 首队列 + 切换播放模式 + 下一首”的回归测试。
3. 让通知按钮偏好始终声明固定 5 个按钮，并把可用性与图标状态限制在按钮自身状态内。
4. 运行 Android 单元测试和匹配的编译验证。

## Comments

## 2026-07-09 修复记录

### 复现与定位

- 已读取 GitHub Issue #1 正文和评论，评论为空。
- 使用 Android 单元测试构造了最小反馈环：当 `Player.Commands` 只包含播放/暂停、不包含上一首/下一首时，通知 provider 仍应返回固定 5 个按钮。
- 修复前测试红线为：`expected:<5> but was:<3>`，与 Issue 中“固定的 5 个按钮变成 3 个”一致。

### 第一性原理与根因

媒体通知的产品不变量是“展开态固定展示收藏、上一首、播放/暂停、下一首、播放模式 5 个按钮”。旧实现把上一首和下一首是否加入通知，绑定到 Media3 当前 `playerCommands` 是否报告可 seek。2 首队列在边界、播放模式切换或系统刷新后，上一首/下一首命令可用性可能变化，通知 provider 就会把这两个固定位置过滤掉，导致 5 个按钮变成 3 个。

### 实现摘要

- 在 `AndroidPlaybackMediaNotificationProvider` 中抽出不依赖 Android framework 的按钮排序规则，便于直接测试。
- 展开态按钮固定按“收藏、上一首、播放/暂停、下一首、播放模式”返回。
- 上一首和下一首仍使用 Media3 标准 player command，不改自定义命令路径，不触碰 shared 播放状态和队列逻辑。
- 新增 `AndroidPlaybackMediaNotificationProviderTest`，覆盖“队列命令消失时仍保留 5 个按钮”的回归场景。

### 验证命令与结果

- `./gradlew :composeApp:testDebugUnitTest --tests com.yanhao.kmpmusic.playback.AndroidPlaybackMediaNotificationProviderTest`：修复前失败，修复后通过。
- `./gradlew :composeApp:testDebugUnitTest :composeApp:compileDebugKotlinAndroid`：通过。
- `./gradlew :composeApp:assembleDebug`：通过。
- `git diff --check`：通过。
- `rg -n "\\[DEBUG-" composeApp/src/androidMain composeApp/src/androidUnitTest`：无调试日志残留。

### Code Review 结论

- 变更范围只在 Android 媒体通知 provider 和对应 Android 单元测试，没有修改 shared controller、播放队列、收藏状态或平台 service 生命周期。
- 自定义收藏和播放模式按钮仍从 `mediaButtonPreferences` 读取，图标和显示名继续跟随最新收藏状态与播放模式。
- 上一首、播放/暂停、下一首仍由 `AndroidPlaybackMediaButtonFactory` 创建，保留官方 Media3 player command 路径。
- 紧凑态索引逻辑仍根据最终按钮列表解析，固定 5 个展开态按钮不会改变紧凑态最多 3 个按钮的系统约束。

### 对抗式审查

- 风险一：固定展示上一首/下一首后，队列边界点击可能被 Media3 拒绝。复核：本次只改变通知 action 是否展示，不改变 player command；队列导航行为仍由 `MediaSession.Callback.onPlayerCommandRequest` 和 Media3 控制。
- 风险二：忽略 `playerCommands` 可能导致 Idle 状态也显示 5 个按钮。复核：Idle 且无活动播放会走 `clearMediaNotification()` 清理媒体项和 service，本次未改通知清理路径。
- 风险三：播放模式或收藏按钮可能丢失。复核：测试断言第 1 位是收藏自定义命令、第 5 位是播放模式自定义命令；生产代码仍从 preferences 中按命令识别。
- 风险四：测试 seam 过浅。复核：测试覆盖的正是 `DefaultMediaNotificationProvider` override 中决定 action 数量的排序规则；红线与 Issue 的“5 变 3”完全对应。

### 剩余风险

未执行真机下拉 Android 媒体通知栏截图复核；当前验证覆盖 provider 规则、Android 单元测试、Android Kotlin 编译和 debug APK 组装。无已知代码级剩余风险。
