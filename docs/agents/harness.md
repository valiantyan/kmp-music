# Harness 维护指南

本文只记录让后续 AI 更可靠协作的环境约束。它不是新项目百科；若与 `README.md`、`CONTEXT.md`、`docs/PRD.md`、ADR、Gradle 或源码冲突，以那些权威来源为准，并修正本文或删除失效规则。

## 组件与失效标准

| 组件 | 解决的 AI 失败问题 | 可证伪标准 |
| --- | --- | --- |
| `AGENTS.md` | 进项目后不知道先读哪里、改哪里、跑什么。 | 后续 AI 在动手前仍读错入口、改错层、或遗漏必要验证，说明路由需要收窄或改写。 |
| `docs/agents/project-map.md` | 找不到真实源码集、测试目录和主模块。 | 后续 AI 发明不存在的模块、包名、源码集或 Gradle 任务，说明地图过期。 |
| `docs/agents/kmp-architecture.md` | 不理解 `core / domain / data / feature / platform source sets` 边界。 | 平台 API 泄漏进 `commonMain`，或业务逻辑被塞进页面级 Composable，说明规则需要升级为测试、接口或更明确示例。 |
| `docs/agents/testing.md` | 不知道改完该跑什么，或只跑代理测试却没有证明用户行为。 | 后续 AI 做了常规代码改动却没有主动运行 `./scripts/verify-local.sh`、没有跑匹配命令、没有说明环境限制，或用户验收路径未覆盖，说明验证矩阵需要补任务级入口。 |
| `scripts/verify-local.sh` | 缺少一个可执行的默认本地验收入口。 | 脚本在干净工作区不可运行、运行不存在任务，或默认与 focused 模式不能覆盖共享逻辑、Android 编译、Android JVM 回归和 Android lint 风险，说明脚本必须修正。 |
| `.scratch/<feature-slug>/` 和 `.scratch/github-bugs/issues/` | 经验只留在对话里，缺陷修复缺少审计证据。 | 修复完成后没有复现信息、根因、验证结果、对抗式审查或剩余风险记录，说明对应工作流需要补门禁。 |

## 升级顺序

重复错误不要只追加提示语。优先按下面顺序找最早 owner：

1. 能用测试证明的行为，补到 `composeApp/src/*Test`。
2. 能自动化的本地门禁，补到 `scripts/verify-local.sh` 或 Gradle 任务。
3. 能让非法状态不可表示的规则，收进领域模型、UseCase、Repository 接口、mapper 或 `expect/actual` 边界。
4. 只有检索和流程问题，才更新 `AGENTS.md`、`docs/agents/*`、ADR 或 `.scratch/<feature-slug>/`。

## 保留或删除规则

- 保留：能改变后续 AI 行为，并且失败时能指出要改哪个 owner。
- 修改：规则真实但太宽，导致 AI 仍需猜测具体命令、目录或验收证据。
- 删除：重复了更权威的 Gradle、源码、ADR 或测试事实，或者基于 `.scratch` issue、审查记录或交付记录能看出连续两次没有被后续任务用到。

## 三个校验任务

用这些真实任务定期验证 harness 是否有效：

1. 修复一个 `MusicAppController`、播放、队列、收藏、搜索或扫描相关缺陷：AI 应读取架构和测试策略，补回归测试，先跑 focused 测试，再主动运行 `./scripts/verify-local.sh`；如果没跑脚本，必须说明环境限制或等价证据。
2. 调整一个移动端二级页面或全局迷你播放器交互：AI 应读取 `docs/agents/ui-state.md`，不把业务逻辑塞进页面 Composable，并说明截图或人工视觉验收缺口。
3. 处理一个 GitHub BUG：AI 应先镜像到 `.scratch/github-bugs/issues/`，下载附件证据，记录根因、验证、对抗式审查和剩余风险，再考虑提交、push 与回写。
