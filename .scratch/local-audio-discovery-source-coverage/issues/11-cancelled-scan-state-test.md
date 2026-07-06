Status: ready-for-human

# 固化取消扫描独立状态与单任务入口测试

## 父级需求

.scratch/local-audio-discovery-source-coverage/PRD.md

## 要做什么

添加一个聚焦控制器或扫描状态测试，描述取消扫描必须能区别于成功完成和失败错误，并能让 UI 渲染“已取消”。

同时补上最小单任务扫描契约测试：扫描中再次触发扫描入口不能启动第二个并发扫描任务，应转为取消意图或等价的取消路径。该任务只添加测试，不实现取消按钮或完整任务取消机制。

## 验收标准

- [x] 测试能区分取消扫描和成功完成扫描。
- [x] 测试能区分取消扫描和失败扫描。
- [x] 测试断言取消扫描可映射为“已取消”。
- [x] 测试断言取消扫描会更新最近扫描时间或等价的结果时间。
- [x] 测试断言扫描中再次触发扫描入口不会启动第二个并发扫描任务。
- [x] 测试能表达扫描中入口应渲染或映射为“取消扫描”。
- [x] 测试不要求删除任何未处理旧歌。

## 前置依赖

- .scratch/local-audio-discovery-source-coverage/issues/03-positive-only-merge-preserves-existing-songs.md

## Comments

实现摘要：

- 在 `MusicAppControllerTest` 中新增取消扫描红灯测试，直接固化未来需要的一等 `LocalMusicScanState.Cancelled` 状态。
- 测试要求取消状态不能复用成功 `Done` 或失败 `Error`，并要求取消状态暴露 `title == "已取消"` 与带 `completedAt` 的结果摘要。
- 新增挂起 scanner 测试扫描中二次触发入口：运行中入口先映射为“取消扫描”，二次触发后 scanner 调用次数仍为 1，并进入取消状态。
- 本 issue 只添加测试，没有实现取消按钮、完整任务取消机制或生产 `Cancelled` 状态。

验证命令与结果：

- `./gradlew :composeApp:desktopTest --tests com.yanhao.kmpmusic.feature.app.MusicAppControllerTest`
- 结果：按红灯测试预期失败，失败发生在 `:composeApp:compileTestKotlinDesktop`，错误为 `LocalMusicScanState.Cancelled`、`title`、`summary` 尚未实现。
- 构建同时输出两个既有 `No cast needed` 警告，位置在本文件旧有断言附近，不是本次新增。

对抗式审查结论：

- 逻辑漏洞：早期测试 helper 曾用测试内硬编码文案和脆弱字符串探测时间，已改为直接要求生产状态提供 `Cancelled.title` 与 `Cancelled.summary.completedAt`。
- 事实正确性：最终红灯来自缺少一等取消状态，符合本 issue “只添加测试，不实现取消机制”的边界。
- 更简单做法：没有改生产代码或平台 scanner，只在既有 controller 测试文件新增最小测试替身。
- 越界风险：未实现后续 issue 的取消按钮、任务取消或状态模型。
- 验证充分性：聚焦运行 controller 测试，确认当前代码按预期红灯；未运行全量测试，因为红灯测试任务到预期失败即可。

Code Review 结论：

- Standards：最终复审通过，未发现需要修复的硬性标准问题。
- Spec：最终复审通过，测试表达符合当前 issue 与 PRD 的取消扫描状态和单任务入口要求。

剩余风险或未完成项：

- 这是红灯测试交付，生产代码仍未定义 `LocalMusicScanState.Cancelled`，后续 issue 需要实现状态、结果时间和取消路径后再转绿。
