# 持久项目知识

只有 `Status: active` 且通过 schema 校验的条目才会在会话开始时注入。优先链接项目事实源，不在这里复制大段说明。

<!--
条目模板：

### DEC-0001 — 简短中文标题

- Status: proposed | active | superseded
- Key: 稳定主题键
- Type: decision | business-rule | term | constraint
- Scope: repository | workflow:主题键 | component:组件键 | path:仓库相对路径
- Decided: YYYY-MM-DD
- Last verified: YYYY-MM-DD
- Review after: YYYY-MM-DD
- Statement: 持久事实或决定。
- Rationale: 该事实成立或采用该决定的理由。
- Source: repo:路径 | docs:路径 | test:路径::测试名
- Supersedes: None 或 DEC-xxxx
-->

### DEC-0001 — 领域语言来源边界

- Status: active
- Key: domain-language-source
- Type: constraint
- Scope: repository
- Decided: 2026-07-13
- Last verified: 2026-07-13
- Review after: 2027-01-09
- Statement: 项目使用根目录 `CONTEXT.md` 记录 KMP Music 当前领域语言；未来 Agent 应把其中术语作为规范性语义解释，不得据此推断实现方案或代码完成度。
- Rationale: `CONTEXT.md` 明确只记录项目领域语言而不记录实现方案，领域语义与实现证据必须分开核对。
- Source: docs:CONTEXT.md
- Supersedes: None

### DEC-0002 — 平台本地音频来源与扫描生命周期

- Status: active
- Key: local-audio-source-lifecycle
- Type: business-rule
- Scope: workflow:local-audio-discovery
- Decided: 2026-07-13
- Last verified: 2026-07-13
- Review after: 2027-01-09
- Statement: 项目要求 Android 使用单一系统媒体库来源，iOS 区分显式导入曲库与系统音乐资料库，Desktop 使用可累加且仅由用户显式移除的扫描目录；来源消失后歌曲可在后续扫描中移出曲库，但取消扫描不得因尚未处理而删除既有歌曲。
- Rationale: 不同平台的授权和来源身份决定曲库增删边界，取消扫描不等同于确认未处理来源已经消失。
- Source: docs:CONTEXT.md
- Supersedes: None

### DEC-0003 — 本地歌单与详情播放队列完整性

- Status: active
- Key: collection-detail-queue-integrity
- Type: business-rule
- Scope: workflow:detail-playback
- Decided: 2026-07-13
- Last verified: 2026-07-13
- Review after: 2027-01-09
- Statement: 本地自建歌单只保存在本机，歌曲按添加顺序且同一歌曲唯一；歌手、专辑和歌单详情发起播放时，队列必须等于各自完整歌曲列表并从用户选择的歌曲开始，歌手或专辑归属忽略轻微空白和英文大小写差异。
- Rationale: 详情列表定义同时约束集合边界、稳定顺序和播放上下文，不能被摘要、推荐或单曲队列替代。
- Source: docs:CONTEXT.md
- Supersedes: None

### DEC-0004 — 移动端页面栈与底部 Chrome

- Status: active
- Key: mobile-navigation-chrome
- Type: business-rule
- Scope: workflow:mobile-navigation
- Decided: 2026-07-13
- Last verified: 2026-07-13
- Review after: 2027-01-09
- Statement: 移动端底部 Tab 仅含首页、收藏、我的；二级页面按栈进入和返回并恢复上一层底部 chrome。全局迷你播放器属于 App chrome；播放页作为特殊栈顶整页覆盖底层，只有播放页容器移动，下滑露出底层至少半屏才关闭。
- Rationale: 页面层级、迷你播放器归属和播放页覆盖关系共同决定返回路径与底部 chrome 是否应保持、隐藏或直接露出。
- Source: docs:CONTEXT.md
- Supersedes: None
