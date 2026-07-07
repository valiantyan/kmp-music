Status: ready-for-human

# Desktop“扫描音乐”复用现有桌面扫描入口

## 父级

/Users/yanhao/Desktop/demo/kmp-music/.scratch/mobile-desktop-me-redesign/PRD.md

## 要构建什么

在 Desktop/macOS “我的”页展示“扫描音乐”快速功能，并让点击行为复用现有桌面扫描动作或添加文件夹入口。桌面端不得打开移动端 `AudioScan` 空占位页，也不新增另一套桌面扫描 UI。

## 验收标准

- [x] Desktop/macOS “我的”页显示“扫描音乐”入口。
- [x] 点击入口复用现有桌面扫描动作或添加文件夹入口。
- [x] 点击入口不会进入 `AudioScan` 空占位页。
- [x] 不新增另一套桌面扫描 UI。
- [x] 现有桌面本地音乐入口行为不被破坏。

## 依赖

- 18-desktop-me-new-profile-structure.md

## Comments

### 实现摘要

- Desktop/macOS “我的”页在统计区下方新增“快速功能”区块，展示“扫描音乐”入口，右侧动作文案为“添加文件夹”。
- 新增 `DesktopMeQuickActionDisplayModel` 和 `buildDesktopMeQuickActionDisplayModels`，把桌面“我的”页快速功能文案与 `ScanMusic` 语义固定在可测试边界。
- 桌面根路由把“扫描音乐”入口的 `onScanMusic` 直接接到既有 `onScanLocalMusic` 回调；该回调来自 `DesktopMusicApp` 中的 `controller.requestLocalMusicScan(LocalMusicScanRequest.Refresh)`，会继续复用 Desktop 文件夹扫描器的选择文件夹流程。
- 现有“本地文件夹”入口仍保留 `controller.openLocalMusic(LocalMusicSection.Sources)`，没有改变本地音乐来源页入口行为。
- 未新增桌面扫描页面或扫描 UI，未把入口接到移动端 `openAudioScan` / `SecondaryScreen.AudioScan`，也未实现 issue 21-24 的设置菜单、桌面最近播放摘要/列表或动作反馈。

### 验证命令与结果

- `./gradlew :composeApp:desktopTest --tests com.yanhao.kmpmusic.feature.desktop.screens.DesktopMeScreenTest`：通过，新增测试覆盖桌面“我的”页存在 `ScanMusic` 快速功能入口，标题为“扫描音乐”，动作文案为“添加文件夹”；既有统计区测试继续通过。
- `./gradlew :composeApp:compileDebugKotlinAndroid`：通过，确认 `commonMain` 桌面共享 UI 与路由改动在 Android 目标编译成功。
- `./gradlew :composeApp:desktopTest`：通过，确认完整 Desktop/common 测试没有回归。
- `git diff --check`：通过，没有补丁空白问题。
- `rg -n "openAudioScan|SecondaryScreen.AudioScan|DesktopMeRootScreen\\(|onScanMusic = onScanLocalMusic|onScanMusic = controller|扫描音乐|添加文件夹|prototypes/kmp-music-hi-fi" ...`：人工复核通过，桌面“我的”页新增入口接到 `onScanLocalMusic`，未新增 `openAudioScan` 接线，未修改原型目录。
- 验证过程中仅出现既有 Gradle 弃用属性提示；定向测试首次编译时还出现既有 `MusicAppControllerTest` 的 `No cast needed` 警告，本切片未修改相关代码。

### Code review 结论

- Spec 轴通过。五项验收标准均已满足：桌面“我的”页显示“扫描音乐”入口，点击复用现有桌面扫描回调，不进入 `AudioScan` 空占位页，不新增桌面扫描 UI，现有“本地文件夹”来源入口保持原行为。
- Standards 轴通过。改动集中在 Desktop “我的”页展示模型、桌面根路由接线和对应 commonTest；没有引入平台 API 到 `commonMain`，没有新增 Repository、UseCase、持久化或独立扫描页面。
- 范围轴通过。未修改登录页、登录路由、底部 Tab、全局迷你播放器、`prototypes/kmp-music-hi-fi` 或 `.agent-loop/*`；未实现 issue 21-24 的静态设置菜单、最近播放摘要/完整页或动作反馈。

### 对抗式审查

- 风险一：点击误入移动端 `AudioScan` 空占位页。复核结果：桌面根路由传入 `onScanMusic = onScanLocalMusic`，新增代码没有调用 `controller.openAudioScan` 或 `SecondaryScreen.AudioScan`。
- 风险二：新增另一套桌面扫描 UI。复核结果：本切片只新增“我的”页快速入口行，实际扫描继续走既有 `DesktopMusicApp` 扫描回调和 Desktop 文件夹扫描器，没有新增扫描页面、对话框或扫描状态组件。
- 风险三：破坏现有桌面本地文件夹入口。复核结果：原“本地文件夹”行仍调用 `onFolders`，桌面根路由仍接 `controller.openLocalMusic(section = LocalMusicSection.Sources)`。
- 风险四：抢做后续静态设置菜单或最近播放。复核结果：没有新增“存储管理”“主题与外观”“关于”三行，没有接入桌面最近播放摘要、最近播放页列表或歌曲动作反馈。
- 风险五：在共享 UI 中引入平台实现。复核结果：`commonMain` 只消费既有扫描回调，不直接引用 Swing、Desktop scanner 或文件系统 API。

### 剩余风险或未完成项

- 无未完成验收项。
- 剩余视觉风险：本切片未启动 Desktop App 做截图核对真实窗口效果；已通过展示模型测试、Desktop/common 测试、Android 编译和静态接线复核确认行为边界正确。
