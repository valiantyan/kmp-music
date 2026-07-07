Status: ready-for-human

# 实现本地音乐入口统计与平台文案收敛

## 父级需求

.scratch/local-audio-discovery-source-coverage/PRD.md

## 要做什么

实现最小 UI 状态或文案映射，让本地音乐扫描入口或来源页按平台展示正确入口和来源文案，并只呈现当前可播放歌曲总数与最后扫描时间。

任务只处理文案和展示模型，不做完整页面视觉重做；如果没有独立扫描页，不要为了满足文案新建多余页面。扫描或导入完成后应保留当前页面或当前路由。

## 验收标准

- [x] Android 文案使用“开始扫描/重新扫描”和“Android 媒体库”。
- [x] Desktop/macOS 文案使用“添加文件夹/重新扫描”和“扫描目录”。
- [x] iOS P0 文案使用“导入音频/扫描曲库/重新扫描”和“已添加音频”或“音频来源”。
- [x] 用户可见本地音乐扫描入口不展示新增、更新、移除计数。
- [x] 扫描或导入完成后不自动跳离当前页面或当前路由。
- [x] 本地音乐入口统计文案测试通过。

## 前置依赖

- .scratch/local-audio-discovery-source-coverage/issues/15-scan-page-summary-copy-test.md

## Comments

实现摘要：
- 新增 `LocalMusicDiscoveryPlatform` 平台展示模型，并让扫描入口文案按 Android、Desktop/macOS、iOS P0 分别显示“开始扫描/添加文件夹/导入音频”，完成或取消后显示“重新扫描”，iOS 普通错误重试显示“扫描曲库”。
- 新增来源类型展示映射：Android 显示“Android 媒体库”，Desktop 显示“扫描目录”，iOS 导入来源显示“已添加音频”。
- 将平台文案传入移动端首页空态、本地音乐来源页、设置页、桌面首页、桌面本地音乐页和桌面设置页；iOS 入口显式传入 iOS 平台，Android 默认保持 Android，Desktop 使用桌面平台。
- 复用 issue 15 的扫描摘要展示模型，继续只展示当前可播放歌曲总数与最后扫描时间，不展示新增、更新、移除计数；扫描完成后仍由既有控制器保持当前页面或当前路由。

验证命令与结果：
- `./gradlew :composeApp:desktopTest --tests com.yanhao.kmpmusic.feature.screen.LocalMusicScanSummaryDisplayModelTest`：通过。
- `./gradlew :composeApp:compileDebugKotlinAndroid`：通过。
- `./gradlew :composeApp:desktopTest`：通过。
- 额外尝试 `./gradlew :composeApp:compileKotlinIosSimulatorArm64`：未通过；失败点是既有 Native 编译问题，包含 `PlaybackDatabaseFactory.kt` 中 `Dispatchers.IO` 在 Native 不可访问，以及 `PlaybackSnapshotWriter.kt` 中 `synchronized` 未解析，和本次文案改动无关。

对抗式审查结论：
- 逻辑漏洞：最容易漏掉覆盖层或设置页入口，已把平台参数传过移动端 root、secondary、overlay、桌面首页、桌面二级页和设置页。
- 事实正确性：Android/桌面/iOS 文案均由测试覆盖，来源标签没有继续暴露“桌面文件夹”或“iOS 导入文件”给对应平台用户。
- 更简单做法：没有新建扫描页或改视觉结构，只扩展现有文案映射和现有页面参数。
- 是否越界：没有实现 issue 17，没有改扫描合并、来源管理、取消逻辑或视觉原型。
- 验证充分性：覆盖了新增展示模型测试、Android 编译和完整桌面测试；iOS 编译受既有 Native 问题阻塞，已记录剩余风险。

Code Review 结论：
- Standards 轴：未发现问题。改动保持 common UI/domain 边界，未把平台 API 放入 commonMain；新增 Markdown 内容为中文；Kotlin 命名、参数命名和既有页面传参风格保持一致。
- Spec 轴：未发现问题。issue 16 要求的平台入口文案、来源文案、隐藏增删改计数、完成后不跳转和文案测试均已满足。

剩余风险或未完成项：
- 未做截图级视觉核对；本次只调整按钮和来源文案，布局结构未重做。
- iOS Kotlin 编译目前被既有 Native 兼容问题阻塞，无法用该命令证明 iOS source set 完整编译通过。
