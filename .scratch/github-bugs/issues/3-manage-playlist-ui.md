Status: ready-for-human

# 管理歌单 UI 错误

## GitHub Issue

- 链接：https://github.com/valiantyan/kmp-music/issues/3
- 编号：#3
- 标签：bug

## 问题现象

打开应用后进入“管理歌单”页面，页面底部仍然显示迷你播放器，并且视觉上保留了底部导航区域。按产品预期，“管理歌单”是覆盖当前页面的二级页面，不应显示迷你播放器和底部 Tab。

## 复现步骤

1. 打开应用。
2. 进入“管理歌单”。
3. 观察页面底部 chrome。

## 期望行为

“管理歌单”页面覆盖当前页面展示，不显示迷你播放器和底部 Tab，内容布局不被底部 chrome 占用或遮挡。

## 实际行为

“管理歌单”页面底部仍显示迷你播放器，底部布局与覆盖页面预期不一致。

## 附件证据

- 原始 URL：https://github.com/user-attachments/assets/3f717de9-2ddb-4b8d-9fcf-5573e988bf28
- 本地路径：.scratch/github-bugs/assets/3/issue-3-manage-playlist-ui.png
- 类型与大小：PNG image data，565 x 1081，8-bit/color RGBA，139609 字节
- 下载结果：成功，使用 `curl -L --fail --output .scratch/github-bugs/assets/3/issue-3-manage-playlist-ui.png <URL>` 通过 GET 下载。
- 检查结论：截图底部可见迷你播放器，管理歌单页未按覆盖页面隐藏全局播放 chrome；该证据直接影响根因判断。

## 验收标准

- [x] 按上述步骤不再复现问题。
- [x] “管理歌单”页面不显示迷你播放器和底部 Tab。
- [x] 相关页面或状态保持正确。
- [x] 已补充或更新与风险匹配的回归测试，或者说明无法补测的原因。

## 修复计划

从第一性原理看，本问题要解决的是“覆盖型二级页面的 chrome 可见性”规则，而不是单个页面的底部间距补丁。先检查 App 导航和全局 chrome 状态的来源，补充覆盖“管理歌单”可见性规则的回归测试，再做最小根治修复。

## Comments

### 实现摘要

- 根因：`SecondaryScreen.LocalPlaylistManagement` 在 `mobileFixedBarModeFor` 中被归类为 `SecondaryWithMiniPlayer`，导航层因此把管理歌单页当作保留迷你播放器的普通二级页渲染。
- 解决方案：将 `LocalPlaylistManagement` 归类为 `SecondaryWithoutChrome`，让它进入覆盖层并隐藏迷你播放器与底部 Tab。
- 布局保护：管理歌单页自身使用 `LazyColumn`，因此在覆盖层中加入直接渲染名单，避免被普通覆盖页的 `verticalScroll` 二次包裹造成无限高度测量风险。
- 回归测试：更新导航状态、控制器行为和覆盖层直接渲染 helper 的测试，锁定“管理歌单为无 chrome 覆盖页”的产品规则。

### 验证命令与结果

- `./gradlew :composeApp:desktopTest --tests com.yanhao.kmpmusic.feature.app.navigation.MusicAppNavigationControllerTest`：先失败，证明新增测试能暴露原问题；修复后通过。
- `./gradlew :composeApp:desktopTest --tests com.yanhao.kmpmusic.feature.app.navigation.MusicAppNavigationControllerTest --tests com.yanhao.kmpmusic.feature.app.layout.MobilePlayerOverlayGestureTest`：通过。
- `./gradlew :composeApp:compileDebugKotlinAndroid :composeApp:desktopTest`：通过。

### 提交记录

- 修复提交：`f9265f32`

### Code Review 结论

- 改动集中在导航 chrome 归类与覆盖层渲染策略，没有在 `LocalPlaylistManagementScreen` 写页面级补丁。
- `LocalPlaylists` 和 `LocalPlaylistDetail` 仍保持 `SecondaryWithMiniPlayer`，未改变普通歌单浏览和详情页的迷你播放器行为。
- 覆盖层直接渲染名单新增管理页后，能保留其内部 `LazyColumn` 和底部删除栏的稳定测量。

### 对抗式审查

- 风险一：管理页隐藏 chrome 后，返回到歌单列表时底层 chrome 状态可能错乱。复核：覆盖层使用 `chromeUnderlaySecondaryScreen` 和 `chromeUnderlayFixedBarMode` 保留底层页面，返回栈测试仍通过。
- 风险二：管理页作为 overlay 后被普通滚动容器包裹，导致 `LazyColumn` 无限高度异常。复核：已将管理页加入 `shouldRenderOverlayScreenDirectly` 并补测试。
- 风险三：误伤歌单详情或歌单列表页面的迷你播放器。复核：只移动 `LocalPlaylistManagement`，其他歌单路由仍在 `SecondaryWithMiniPlayer`。
- 风险四：Android 编译或共享测试受影响。复核：`compileDebugKotlinAndroid` 和完整 `desktopTest` 均通过。

### 剩余风险

- 未在真机或模拟器截图复核视觉结果；本次通过导航状态和布局策略测试确认不再渲染固定底栏，但最终像素级效果仍建议后续人工在 Android 设备上看一眼。
