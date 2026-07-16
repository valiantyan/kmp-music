# 验证策略

本文承接根 `AGENTS.md` 的测试细则。提交前至少运行与改动范围匹配的命令；不确定任务是否存在时先跑 `./gradlew :composeApp:tasks`。

## 命令选择

| 改动范围 | 最低验证 |
| --- | --- |
| 普通 Kotlin 或 Android 共享代码编译风险 | `./gradlew :composeApp:compileDebugKotlinAndroid` |
| 需要生成调试 APK | `./gradlew :composeApp:assembleDebug` |
| 需要安装到已连接 Android 设备 | `./gradlew :composeApp:installDebug` |
| 共享状态、控制器、导航、播放、队列、收藏、搜索、扫描、偏好 | `./gradlew :composeApp:compileDebugKotlinAndroid :composeApp:desktopTest` |
| UI 大改 | 至少 Android 编译；涉及共享状态时加 `:composeApp:desktopTest`；尽量补截图核对。 |
| 领域模型或 UseCase | 更新对应 `domain/model`、`domain/usecase`、`domain/playback` 测试，并运行匹配测试任务。 |
| Repository、数据库、扫描合并 | 更新 `data`、`domain/persistence` 或扫描控制器测试，并运行匹配测试任务。 |
| Android MediaStore、Media3 service、通知按钮或权限 | 至少 Android 编译；能用 JVM 或共享测试覆盖的逻辑要补测。 |
| Apple/macOS AVFoundation | 至少 `./gradlew :composeApp:desktopTest`；按影响范围跑相关 macOS 冒烟验证。 |

## 测试落点

- 改动 `MusicAppController`、导航、播放状态、队列、收藏、搜索、扫描或偏好时，更新对应 `composeApp/src/commonTest` 测试。
- 移动端页面显示模型或交互规则改动，优先补 `feature/screen`、`feature/app/layout`、`feature/app/navigation`、`feature/app/playerbar` 测试。
- 桌面页面或播放器显示改动，优先补 `feature/desktop` 测试。
- 数据库迁移、持久化 mapper、播放快照、扫描合并这类状态语义不能只靠 UI 现象验证。

## 交付说明

- 不要在没有实际运行的情况下声称验证通过。
- 如果验证失败，保留真实失败命令和关键错误；不要删除失败测试来让构建变绿。
- 如果验证受环境限制，说明限制原因、未覆盖风险和建议的后续命令。
- Markdown 纯文档改动通常不需要跑 Gradle，但仍要检查 `git diff`、链接路径和文档是否与源码事实冲突。
