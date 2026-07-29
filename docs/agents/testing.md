# 验证策略

本文承接根 `AGENTS.md` 的测试细则。提交前至少运行与改动范围匹配的命令；不确定任务是否存在时先跑 `./gradlew :composeApp:tasks`。

## 默认入口

常规代码改动后，AI 必须主动运行默认入口：

```bash
./scripts/verify-local.sh
```

该脚本运行 Android Kotlin 编译和 Desktop 测试，覆盖共享逻辑与 Android 编译风险；在 macOS host 上，`desktopTest` 会带上 AVFoundation bridge 编译依赖，非 macOS 环境由 Gradle `onlyIf` 跳过相关 bridge 编译。它不是所有任务的唯一验收：平台播放、安装、截图、iOS framework、DMG 或真实设备行为仍要按改动范围补跑 focused 命令，并在交付说明中写明。只有纯文档改动、脚本因环境限制不可运行，或已运行更精确的 focused 验证时，才允许不跑默认入口；这种情况必须说明原因和等价证据。

## 交付路由

先按任务事实人工判断，用户明确要求优先；判断不清时默认 `STANDARD`，遇到严格条件马上升级为 `STRICT`，不使用 NLP 分类器。

| 档位 | 验证与交付 |
| --- | --- |
| `LIGHT` | 简单问答、状态查询或单文件只读检查；主代理可直接完成，不运行 `agent_delivery`，也不强制子代理/reviewer。 |
| `STANDARD` | 多文件分析、小文档、普通代码修复；一个全新交付 agent 运行 focused 验证，常规代码再运行 `./scripts/verify-local.sh`；默认没有完整 v4/reviewer 门禁。 |
| `STRICT` | 破坏性 cleanup、commit/push、外部写入、迁移、安全、资金、数据丢失、显式 1:1/UI 证据、复杂跨层改动；使用完整 v4、独立 reviewer 和成功 `complete`。 |

- `STANDARD` 出现严格条件，或用户明确要求 v4、独立 reviewer、1:1 或外部交付时，必须新建 `STRICT` task/agent，不能复用旧 agent。已结束的交付 agent 不复用。
- `STRICT` 的 writer/reviewer/tool/systemError 不会被仓库脚本自动感知。协调者有宿主终止证据时，先 `confirm-terminated`，再用 `terminate` 收口为 `FAILED`、`CANCELLED` 或 `DEGRADED_REPORT`；脚本不可调用时交付必须写“生命周期未闭环”，不能把降级报告称为 PASS 或 `COMPLETED`。

子代理结构化交付脚本改动使用以下 focused 验证：

```bash
python3 -m unittest scripts.tests.test_agent_delivery
```

该测试在临时 Git 仓库中覆盖原始验收合同、版本化返工、任务前快照、用户已有修改隔离、逐条规格审查、接管终止确认、完整文件清单、文本任务级 Diff、二进制摘要、唯一 Manifest、验证证据、固定三行输出和 500 字符上限。生命周期回归还必须覆盖：writer 一任务绑定、snapshot/task_id 碰撞拒绝、新任务新 agent/task_id、同任务澄清与审查修复、reviewer 同任务复核与跨任务/角色复用拒绝、FAIL review 阻断、v3 显式迁移、受审 render receipt 与 reviewer approval、writer 自报 FAILED、未确认终止时 coordinator 收口拒绝、确认后 FAILED/CANCELLED/DEGRADED_REPORT 收口、degraded 不伪造 approval、四种终态命令矩阵拒绝、snapshot 副本/回滚与终态双写中断不能绕过 tombstone，以及 `complete/terminate` 和双 `terminate` 并发只有一个原子成功者。

本组测试通过不等于任务已完成。`STRICT` 交付还需按 `docs/agents/harness.md` 由当前 task 的全新只读 reviewer 检查实际 diff、验证证据、Manifest 与候选三行，再运行 `complete`；任一终态后只可用 `status` 检查，不得为补验证重新打开旧 task。`STANDARD` 按其 focused 验证与默认验证要求交付，不虚构 v4 完成记录。

## 命令选择

| 改动范围 | 最低验证 |
| --- | --- |
| 默认本地验收，不确定该跑什么 | `./scripts/verify-local.sh` |
| 普通 Kotlin 或 Android 共享代码编译风险 | `./gradlew :composeApp:compileDebugKotlinAndroid` |
| 需要生成调试 APK | `./gradlew :composeApp:assembleDebug` |
| 需要安装到已连接 Android 设备 | `./gradlew :composeApp:installDebug` |
| 共享状态、控制器、导航、播放、队列、收藏、搜索、扫描、偏好 | 先跑对应 focused 测试，再跑 `./scripts/verify-local.sh` |
| UI 大改 | 至少 Android 编译；涉及共享状态时加 `:composeApp:desktopTest`；按 `docs/agents/ui-state.md` 完成需求相关状态矩阵。显式要求 1:1、截图或动画时，对应视觉或动态证据是硬门禁。 |
| Desktop 页面、滚动条或播放动画 | 先跑对应测试和默认入口，再用 `./scripts/desktop-ui-qa.sh <scenario>` 取得本次构建三帧证据。 |
| 领域模型或 UseCase | 更新对应 `domain/model`、`domain/usecase`、`domain/playback` 测试，并运行匹配测试任务。 |
| Repository、数据库、扫描合并 | 更新 `data`、`domain/persistence` 或扫描控制器测试，并运行匹配测试任务。 |
| Android MediaStore、Media3 service、通知按钮或权限 | 至少 Android 编译；能用 JVM 或共享测试覆盖的逻辑要补测，可用 `./scripts/verify-local.sh android-unit`。 |
| Android Manifest、资源、权限声明或基础质量门禁 | `./scripts/verify-local.sh lint` |
| Apple/macOS AVFoundation | 至少 `./gradlew :composeApp:desktopTest`；按影响范围跑相关 macOS 冒烟验证。 |

`scripts/verify-local.sh` 支持这些 focused 模式：

| 模式 | 命令 |
| --- | --- |
| `default` / `quick` | `./gradlew :composeApp:compileDebugKotlinAndroid :composeApp:desktopTest` |
| `android` | `./gradlew :composeApp:compileDebugKotlinAndroid` |
| `android-unit` | `./gradlew :composeApp:testDebugUnitTest` |
| `apk` | `./gradlew :composeApp:assembleDebug` |
| `lint` | `./gradlew :composeApp:lintDebug` |
| `desktop` | `./gradlew :composeApp:desktopTest` |
| `macos-avfoundation` | `./gradlew :composeApp:desktopTest :composeApp:macosAvFoundationBridgeSmoke :composeApp:macosAvFoundationDefaultRuntimeSmoke` |
| `tasks` | `./gradlew :composeApp:tasks` |

## UI 验收证据

- 编译、单元测试和 `./scripts/verify-local.sh` 证明代码与规则层风险，不证明像素、布局、动画或目标窗口来自本次构建。
- 静态视觉验收使用本次构建在指定尺寸下的截图；动态验收使用本次构建的录屏或能看出实际变化的连续帧，并覆盖需求指定的状态切换。
- UI 任务先按 `docs/agents/ui-state.md` 建立状态矩阵，再为每个需求 claim 绑定自动化测试、截图、录屏或人工操作结果；不能用某一类证据替代它无法证明的 claim。
- 无法启动、识别或捕获本次构建时，先解决运行与取证问题。若缺失的是用户显式验收项，任务保持未完成。

### Desktop UI QA

- `home`、`albums`、`artists`、`favorites` 使用真实 Desktop 壳和 120 首内存 fake 曲库，自动采集初始、停止前、停止满 5 秒后三帧；验收时序独立于生产延迟常量。`home-playing` 使用真实控制器进入播放中状态，在不同动画周期采集三帧。
- 既有 QA 场景窗口固定为 `1240×824`，`favorites` 按对应 Figma 基准固定为 `1280×1024`；取证期窗口置顶，工具会拒绝空白截图、相同帧、固定 shell 区域漂移，以及停止 5 秒后仍发生大范围变化的结果。
- 证据默认写入已忽略的 `build/desktop-ui-qa/`，脚本完成后自动退出，不读取用户数据库，不依赖侧栏点击、窗口名猜测或屏幕绝对坐标。
- 该入口需要可用的图形桌面会话和屏幕捕获权限；无界面 CI 应运行逻辑测试，不能伪称已取得渲染证据。
- 该入口证明本次构建、固定尺寸、目标路由和动态变化，不能自动证明与 Figma 1:1。显式 1:1 仍需把生成帧与对应设计节点人工或像素对比。

## 测试落点

- 改动 `MusicAppController`、导航、播放状态、队列、收藏、搜索、扫描或偏好时，更新对应 `composeApp/src/commonTest` 测试。
- 移动端页面显示模型或交互规则改动，优先补 `feature/screen`、`feature/app/layout`、`feature/app/navigation`、`feature/app/playerbar` 测试。
- 桌面页面或播放器显示改动，优先补 `feature/desktop` 测试。
- 数据库迁移、持久化 mapper、播放快照、扫描合并这类状态语义不能只靠 UI 现象验证。

## 验收判断

- 通过：运行了与改动范围匹配的命令，用户可见行为或平台 claim 有对应证据，且没有未解释的失败。
- 有条件通过：核心命令通过，但用户未显式要求、且不影响验收结论的补充截图、真机、iOS、macOS smoke 或外部权限受限；交付说明必须写清剩余风险和建议补跑命令。
- 不通过：命令失败、任务不存在、环境缺依赖、脚本本身出错，测试只证明内部实现但没有覆盖用户验收 claim，或用户显式要求的截图、录屏、动画、真机和指定状态证据缺失。

## 失败排查

1. 先确认失败命令是否真实存在：`./gradlew :composeApp:tasks`。
2. 编译失败先按源码集定位：`commonMain` 看分层和 `expect/actual`，`androidMain` 看 MediaStore/Media3/权限，`desktopMain` 看文件系统、数据库或 AVFoundation bridge。
3. 测试失败先读失败测试名对应的源码和邻近测试，不要删除失败断言来让构建变绿。
4. macOS AVFoundation smoke 失败时区分 `clang++`、JNI bridge 编译、资源 staging、运行时播放和宿主系统权限问题。
5. 环境或权限限制要在最终回复中归类为项目问题、环境问题、权限问题、依赖问题或脚本问题。

## 交付说明

- 不要在没有实际运行的情况下声称验证通过。
- 常规代码改动如果没有运行 `./scripts/verify-local.sh`，必须说明跳过原因、等价命令和未覆盖风险。
- 如果验证失败，保留真实失败命令和关键错误；不要删除失败测试来让构建变绿。
- 如果验证受环境限制，说明限制原因、未覆盖风险和建议的后续命令。
- UI 交付说明必须区分自动化测试、静态截图、动态录屏和人工操作分别证明了什么；不要用“测试通过”概括未实际观察的视觉或动画行为。
- Markdown 纯文档改动通常不需要跑 Gradle，但仍要检查 `git diff`、链接路径和文档是否与源码事实冲突。
- 重复错误沉淀到 `docs/agents/harness.md` 指定的最早 owner；能写测试、脚本或类型约束时，不只更新 Markdown。
