Status: ready-for-human

# Desktop“我的”页显示最近播放摘要和查看全部

## 父级

/Users/yanhao/Desktop/demo/kmp-music/.scratch/mobile-desktop-me-redesign/PRD.md

## 要构建什么

在 Desktop/macOS “我的”页展示最近播放摘要，并提供进入完整“最近播放页”的“查看全部”入口。摘要使用统一过滤后的最近播放歌曲列表，保持与移动端同一产品语义。

## 验收标准

- [x] Desktop/macOS “我的”页显示最近播放摘要。
- [x] 摘要歌曲来自统一过滤后的最近播放歌曲列表。
- [x] 最近播放为空时显示轻量空态。
- [x] “查看全部”进入 Desktop/macOS workspace 中的最近播放页。
- [x] 摘要不展示不可解析、已移除或不可播放歌曲。

## 依赖

- 07-recent-played-song-list-filtering.md
- 08-recent-played-secondary-route.md
- 18-desktop-me-new-profile-structure.md

## Comments

### 实现摘要

- 新增 `DesktopMeRecentPlayedSummary.kt`，在 Desktop/macOS “我的”页展示“最近播放”摘要、最多前三首歌曲和轻量空态。
- 桌面摘要展示模型只消费 `DesktopMeRootScreen` 传入的 `recentSongs`，该入参由 `DesktopRootScreenRoute` 继续传递 `state.recentSongs`，复用 issue 07 已建立的统一过滤后最近播放歌曲列表。
- “查看全部”入口复用 `controller.openRecentPlayed()`，进入 issue 08 已建立的 `SecondaryScreen.RecentPlayed` workspace 承载；本切片未实现 issue 23 的完整桌面列表细节。
- 摘要歌曲行只做封面、标题、歌手专辑和时长展示，没有接入点击播放、更多菜单、当前播放红色高亮或播放中辅助标识，避免抢做 issue 24。
- 未修改登录页、登录路由、底部 Tab、全局迷你播放器、移动端页面、`prototypes/kmp-music-hi-fi` 或 `.agent-loop/*`。

### 验证命令与结果

- `./gradlew :composeApp:desktopTest --tests com.yanhao.kmpmusic.feature.desktop.screens.DesktopMeScreenTest`：通过，新增测试覆盖桌面最近播放摘要空态、Top3 截断、只使用传入过滤后列表和“查看全部”不混入更多菜单语义。
- `./gradlew :composeApp:compileDebugKotlinAndroid :composeApp:desktopTest`：通过，确认 `commonMain` 桌面共享 UI 在 Android 目标编译成功，完整 Desktop/common 测试无回归。
- `git diff --check`：通过，没有补丁空白问题。
- 验证过程中仅出现既有 Gradle 弃用属性提示，以及既有 `MusicAppControllerTest.kt` 两处 `No cast needed` 警告；本切片未修改相关位置。

### Code review 结论

- Spec 轴通过。五项验收标准均已满足：桌面“我的”页展示最近播放摘要，空列表有轻量文案，摘要只从 `state.recentSongs` 这份统一过滤结果取前三首，“查看全部”进入 Desktop workspace 的最近播放页。
- Standards 轴通过。改动集中在 Desktop “我的”页组合、独立摘要组件、桌面根路由接线和对应 commonTest；没有引入平台 API、Repository、持久化、后端或新的扫描/播放能力。
- 范围轴通过。没有实现 issue 23 的完整桌面最近播放页列表/空态细节，也没有实现 issue 24 的播放、更多菜单或当前播放反馈。

### 对抗式审查

- 风险一：摘要绕过统一过滤，自己从历史或全库拼歌曲。复核结果：`DesktopMeRecentPlayedSummary` 只接收 `recentSongs` 入参，桌面根路由传入 `state.recentSongs`，展示模型没有 repository、全库、demo 或历史 ID 回退逻辑；测试覆盖“只使用传入过滤后列表”。
- 风险二：“查看全部”没有进入桌面 workspace 最近播放页。复核结果：`DesktopRootScreenRoute` 传入 `onRecentPlayedViewAll = controller::openRecentPlayed`，该控制器入口打开 `SecondaryScreen.RecentPlayed`，由桌面二级路由承载。
- 风险三：空态变成留白。复核结果：摘要卡片在 `rows.isEmpty()` 时显示“播放歌曲后会显示最近听过的音乐。”，定向测试已覆盖。
- 风险四：抢做 issue 23/24。复核结果：桌面二级最近播放页仍保持现有占位；摘要行没有 `onSongPlay`、`onMore`、`MoreVert`、`PlayingGlyph` 或播放红色高亮接线。
- 风险五：破坏 issue 18-21 的桌面“我的”页结构。复核结果：个人资料头、三项统计、扫描音乐入口和静态设置菜单均保留；最近播放摘要插在“快速功能”和静态设置菜单之间，贴合 PRD 信息顺序。

### 剩余风险或未完成项

- 无未完成验收项。
- 剩余视觉风险：本切片未启动真实 Desktop App 做截图核对；已通过展示模型测试、完整 Desktop/common 测试、Android Kotlin 编译和静态范围复核确认行为边界正确。
- 完整 Desktop 最近播放页列表/空态细节仍由 issue 23 处理；桌面最近播放播放、更多菜单和当前播放反馈仍由 issue 24 处理。
