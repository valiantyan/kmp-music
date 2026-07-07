Status: ready-for-human

# 固化本地音乐入口只显示总数和最后扫描时间

## 父级需求

.scratch/local-audio-discovery-source-coverage/PRD.md

## 要做什么

添加一个很小的 UI 状态或文案映射测试，描述本地音乐扫描入口或来源页展示模型只显示当前可播放歌曲总数和最后扫描时间，不展示新增、更新、移除计数。

当前源码不一定存在独立“扫描页”，测试应落在现有本地音乐入口、首页扫描入口、来源页或其展示模型上。测试还应锁住扫描或导入完成后不自动跳离当前页面。该任务只添加测试，不做视觉重构。

## 验收标准

- [x] 测试断言展示状态包含当前可播放歌曲总数。
- [x] 测试断言展示状态包含最后扫描时间。
- [x] 测试断言用户可见展示模型不暴露新增、更新、移除计数。
- [x] 测试断言扫描或导入完成后仍停留在当前页面或当前路由。
- [x] 测试不修改高保真原型。

## 前置依赖

- .scratch/local-audio-discovery-source-coverage/issues/03-positive-only-merge-preserves-existing-songs.md

## Comments

实现摘要：

- 新增本地音乐扫描摘要展示模型，统一生成“当前可播放歌曲总数 + 最近扫描时间”的用户可见文案，并让本地音乐页头部复用该模型，避免页面重新拼接内部增删改统计。
- 新增展示模型测试，断言可见文案包含总数和最近扫描时间，且不暴露新增、更新、移除计数或对应内部数字。
- 新增控制器路由测试，断言来源分段触发扫描完成后仍停留在当前本地音乐来源路由。
- 未修改 `prototypes/kmp-music-hi-fi`，未实现 issue 16 或 issue 17。

验证命令与结果：

- `./gradlew :composeApp:desktopTest --tests com.yanhao.kmpmusic.feature.screen.LocalMusicScanSummaryDisplayModelTest --tests com.yanhao.kmpmusic.feature.app.MusicAppControllerTest`：通过。
- `./gradlew :composeApp:compileDebugKotlinAndroid :composeApp:desktopTest`：通过。

对抗式审查结论：

- 逻辑漏洞：检查了扫描摘要是否会从 `LocalMusicLastScanSummary` 泄露 `addedCount`、`updatedCount`、`removedCount`，测试已用明显数字锁住不会泄露。
- 事实正确性：最近扫描时间复用现有 `formatLocalMusicScanDate`，避免引入新的日期算法。
- 更简单做法：移除了仅服务测试的可见文案列表属性，展示模型只保留 UI 实际使用字段。
- 越界风险：未接入平台特定文案、扫描按钮文案或最终验收，不触碰 issue 16/17。
- 验证充分性：聚焦测试和共享 Desktop 全量测试、Android Kotlin 编译均已通过。

code-review 结论：

- Standards：通过。改动保持 `commonMain` 平台无关，中文注释和 Kotlin 命名符合现有风格，没有引入不必要抽象或跨层平台 API。
- Spec：通过。覆盖本 issue 的四条测试要求和不修改原型要求；没有实现后续平台文案或最终验证 issue。

剩余风险或未完成项：

- 本次只固化本地音乐入口摘要模型和路由保持行为；平台差异化扫描文案仍留给 issue 16。
