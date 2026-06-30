# Task 7 Report: Extract LoginAndDialogStateController

## 实现摘要

- 新增 `LoginAndDialogStateController`，集中承接轻量 session/dialog/login reducer：
  - 队列弹层开关
  - 更多操作弹层开关
  - 清理缓存确认框开关
  - 登录邮箱更新与邮件发送前的最小格式校验
- 保持 `MusicAppController` 作为公开 facade，只把对应方法改为委托给 `LoginAndDialogStateController`。
- 按 brief 保持权限设置相关方法留在 `MusicAppController`，未迁移。
- 新增聚焦测试，验证轻量 reducer 只修改自己负责的状态，不影响用户收藏数据等无关字段。

## RED / GREEN 记录

### RED

命令：

```bash
./gradlew :composeApp:desktopTest --tests "com.yanhao.kmpmusic.feature.app.session.LoginAndDialogStateControllerTest"
```

关键输出：

```text
e: .../LoginAndDialogStateControllerTest.kt:17:26 Unresolved reference 'LoginAndDialogStateController'.
e: .../LoginAndDialogStateControllerTest.kt:28:26 Unresolved reference 'LoginAndDialogStateController'.
e: .../LoginAndDialogStateControllerTest.kt:41:26 Unresolved reference 'LoginAndDialogStateController'.

FAILURE: Build failed with an exception.
Execution failed for task ':composeApp:compileTestKotlinDesktop'.
```

结果：符合 brief 预期，测试先因为目标 controller 缺失而失败。

### GREEN

命令：

```bash
./gradlew :composeApp:desktopTest --tests "com.yanhao.kmpmusic.feature.app.session.LoginAndDialogStateControllerTest" --tests "com.yanhao.kmpmusic.feature.app.MusicAppControllerTest.systemBackClosesPermissionSettingsDialog" --tests "com.yanhao.kmpmusic.feature.app.MusicAppControllerTest.systemBackClosesOverlayBeforeSecondaryScreen"
```

关键输出：

```text
> Task :composeApp:desktopTest

BUILD SUCCESSFUL in 7s
18 actionable tasks: 8 executed, 10 up-to-date
```

结果：新增 session controller 测试通过，`MusicAppController` 的系统返回优先级规则保持通过。

## 变更文件

- `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/app/session/LoginAndDialogStateController.kt`
- `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/app/MusicAppController.kt`
- `composeApp/src/commonTest/kotlin/com/yanhao/kmpmusic/feature/app/session/LoginAndDialogStateControllerTest.kt`
- `.superpowers/sdd/task-7-report.md`

## 自检

- 抽取边界与 brief 一致，只移动轻量 session/dialog/login reducer。
- `MusicAppController` 仍然是状态 owner 和公开入口，没有把 facade 责任下放到 UI。
- 权限设置确认框相关方法保留在 `MusicAppController`，没有越界迁移。
- 行为保持不变：`sendLoginMail` 仍然只要求邮箱包含 `@` 才置 `isMailSent = true`。
- 回归覆盖了新 controller 的 reducer 行为，以及系统返回对 overlay 和权限对话框的关闭顺序。

## 关注点

- 当前只按 brief 跑了聚焦测试，没有额外扩大到整套 `:composeApp:desktopTest`；如果 Phase 1 收尾需要更强信心，建议在任务汇总阶段统一跑一轮完整共享测试。
