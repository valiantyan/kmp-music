Status: ready-for-human

# 实现取消扫描状态与单任务取消入口

## 父级需求

.scratch/local-audio-discovery-source-coverage/PRD.md

## 要做什么

实现最小扫描状态或结果原因，让取消扫描可以在面向 UI 的状态中渲染“已取消”，并表达当前曲库已保留、部分结果下次补齐。

同时实现最小单任务扫描防护：扫描中再次触发扫描入口不启动第二个扫描，而是触发取消；取消无需确认。任务不实现完整后台任务队列，只让前置状态和单任务测试通过。

## 验收标准

- [x] 取消扫描有独立于成功和失败的面向 UI 表达。
- [x] 取消扫描可渲染“已取消”。
- [x] 取消扫描说明当前曲库被保留。
- [x] 取消扫描更新最近扫描时间或等价的结果时间。
- [x] 扫描中入口可渲染或映射为“取消扫描”。
- [x] 扫描中再次触发入口不会并发启动第二个扫描任务。
- [x] 取消扫描不需要确认。
- [x] 取消扫描状态测试通过。

## 前置依赖

- .scratch/local-audio-discovery-source-coverage/issues/11-cancelled-scan-state-test.md

## Comments

实现摘要：

- 新增 `LocalMusicScanState.Cancelled`，作为独立于成功 `Done` 和失败 `Error` 的取消结果态，并保留等价结果时间。
- `MusicAppController.scanLocalMusic` 增加单任务防护：扫描运行中再次触发入口会取消当前扫描协程、立即发布取消态，不会启动第二个 scanner，也不会弹确认。
- 平台 scanner 上报 `UserCancelled` 时统一映射为取消态；取消后旧扫描返回时不会覆盖已发布的取消状态。
- 首页歌曲、专辑、歌手空态通过共享 `localMusicScanActionLabel` 映射扫描中入口为“取消扫描”，避免页面级重复补丁。
- 移动端和桌面端本地音乐来源页会渲染“已取消”、当前曲库已保留说明和最近结果日期。
- 取消文案保留在 feature 层共享渲染 helper 中，domain 状态只承载取消事实和结果时间。

验证命令与结果：

- `./gradlew :composeApp:desktopTest --tests com.yanhao.kmpmusic.feature.app.MusicAppControllerTest`
- 结果：通过。仍有两个既有 `No cast needed` 警告，位于权限错误测试旧断言附近，不是本次取消状态新增。
- `./gradlew :composeApp:desktopTest`
- 结果：通过。
- `git diff --check`
- 结果：通过，无空白格式问题。

对抗式审查：

- 最可能翻车点 1：扫描中再次点击只改 UI、不取消实际任务。已通过保存当前扫描协程 `Job` 并在取消入口调用 `cancel` 修正；测试断言 scanner 调用次数仍为 1。
- 最可能翻车点 2：取消态被成功或失败复用，导致 UI 无法区分。已通过 `LocalMusicScanState.Cancelled` 独立状态和 controller 测试覆盖。
- 最可能翻车点 3：取消后旧扫描完成覆盖取消状态。已通过取消标记在扫描 use case 返回后阻止 `syncLibrarySnapshot` 覆盖。
- 最可能翻车点 4：只有测试 helper 能映射“已取消”，生产 UI 看不到。已在移动端和桌面端本地音乐来源页渲染取消标题、保留说明和最近结果日期。
- 最可能翻车点 5：扫描入口文案在多个页面重复维护后漂移。已抽到共享 `localMusicScanActionLabel`。

Code Review 结论：

- Standards：第一轮发现首页空态按钮文案映射重复，已改为共享 helper；第二轮发现 domain 模型携带 UI 文案，已改为 domain 只保存取消事实和结果时间，UI 文案放在 feature 层。最终自查未发现新的 documented-standard 阻断项。
- Spec：复审认为独立取消状态、扫描中取消入口、无并发第二扫描、无确认、可见“已取消/曲库保留/结果时间”均已满足；剩余风险是平台 scanner 若内部使用不可取消阻塞 IO，协程取消信号可能延迟生效。

剩余风险：

- 当前取消依赖协程取消协作；若某个平台 scanner 使用不可取消的阻塞调用，底层 IO 可能不会立刻停止，但 controller 已保证不会并发启动第二个扫描任务，也不会让旧结果覆盖取消态。
