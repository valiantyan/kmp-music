Status: ready-for-human

# Desktop 最近播放页展示完整列表和空态

## 父级

/Users/yanhao/Desktop/demo/kmp-music/.scratch/mobile-desktop-me-redesign/PRD.md

## 要构建什么

在 Desktop/macOS workspace 中渲染完整“最近播放页”，展示完整最近播放歌曲列表，并在没有最近播放歌曲时显示清晰空态。该页面不是播放日志管理页，不提供清空、编辑或审计历史。

## 验收标准

- [x] Desktop/macOS 最近播放页展示完整最近播放歌曲列表。
- [x] 列表使用统一过滤后的最近播放歌曲列表。
- [x] 最近播放为空时显示清晰空态。
- [x] 页面不提供清空、编辑、筛选、排序或审计历史功能。
- [x] 页面布局符合桌面 workspace 信息密度，不套用手机稿宽度。

## 依赖

- 07-recent-played-song-list-filtering.md
- 08-recent-played-secondary-route.md
- 22-desktop-me-recent-played-summary.md

## Comments

### 实现摘要

- 新增 Desktop/macOS 最近播放完整页，`SecondaryScreen.RecentPlayed` 现在在桌面 workspace 中渲染 `DesktopRecentPlayedScreen`，并继续提供普通二级页返回入口。
- 桌面最近播放页只接收 `state.recentSongs`，即 issue 07 建立的统一过滤后最近播放歌曲列表；页面展示模型不读取历史、全库、demo 数据或 repository。
- 完整页展示所有传入歌曲，不复用“我的”页摘要 Top3 截断；表格按桌面 workspace 宽屏密度展示序号、封面、标题、歌手、专辑和时长。
- 最近播放为空时显示“暂无最近播放”和“播放歌曲后会在这里显示最近听过的音乐。”，避免 workspace 留白。
- 当前切片没有提供清空、编辑、筛选、排序、审计历史功能，也没有抢做 issue 24 的播放、更多菜单、当前播放红色高亮或播放中辅助标识。

### 验证命令与结果

- `./gradlew :composeApp:desktopTest --tests com.yanhao.kmpmusic.feature.desktop.screens.DesktopRecentPlayedScreenTest`：通过，覆盖完整列表、统一过滤入参、清晰空态、无管理功能、无歌曲动作和 workspace 表格策略。
- `git diff --check`：通过，没有补丁空白问题。
- `./gradlew :composeApp:compileDebugKotlinAndroid :composeApp:desktopTest`：通过，确认 commonMain 在 Android 目标编译成功，完整 Desktop/common 测试无回归。
- 验证过程中仅出现既有 Gradle 弃用属性提示，以及既有 `MusicAppControllerTest.kt` 两处 `No cast needed` 警告；本切片未修改相关位置。

### code review 结论

- 规格符合性通过：桌面 `RecentPlayed` 路由不再是占位页，而是完整列表页；列表来源固定为 `state.recentSongs`，满足统一过滤列表消费要求。
- 范围控制通过：新页面没有清空、编辑、筛选、排序、审计历史，也没有接入播放、更多菜单、当前播放高亮或播放中辅助标识。
- 代码质量通过：展示模型、页面壳和桌面表格拆分为小文件；表格列宽复用桌面表格 token，避免手机窄布局迁移到 workspace。
- 回归风险可控：未修改登录页、登录路由、底部 Tab、全局迷你播放器、移动端页面、原型目录或 `.agent-loop/*`。

### 对抗式审查

- 风险一：完整页绕过统一过滤，自己重新解析播放历史。复核结果：`DesktopSecondaryScreenRoute` 只传 `state.recentSongs`，`buildDesktopRecentPlayedPageDisplayModel` 只消费入参，没有 repository、历史 ID、全库或 demo 回退逻辑；测试覆盖“只使用传入过滤后列表”。
- 风险二：空列表只剩标题或误导为加载失败。复核结果：空态标题和说明固定存在，聚焦测试覆盖空态文案。
- 风险三：把最近播放页做成播放日志管理页。复核结果：页面和展示模型没有清空、编辑、筛选、排序或审计入口，测试锁定 `hasManagementActions == false`。
- 风险四：抢做 issue 24 的动作反馈。复核结果：歌曲行没有点击播放、更多菜单、当前播放状态入参、红色高亮或播放中标识，测试锁定 `hasPlaybackAction` 和 `hasMoreAction` 均为 `false`。
- 风险五：把移动端窄列表硬套到桌面。复核结果：页面使用桌面 `DesktopPageHeader`、桌面表格行高/列间距/封面尺寸和 `WorkspaceTable` 布局策略，测试覆盖桌面表格策略。

### 剩余风险或未完成项

- 无未完成验收项。
- 剩余视觉风险：本切片未启动真实 Desktop App 做截图核对；已通过桌面展示模型测试、Android Kotlin 编译、完整 Desktop/common 测试和静态范围复核确认行为边界正确。
- 桌面最近播放播放、更多菜单、当前播放红色高亮和播放中辅助标识仍由 issue 24 处理。
