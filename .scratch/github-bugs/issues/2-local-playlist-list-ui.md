Status: ready-for-human

# 我的歌单二级页面UI 不对

## GitHub Issue

- 链接：https://github.com/valiantyan/kmp-music/issues/2
- 编号：#2
- 标签：bug
- 状态：OPEN

## 问题现象

从“我的”页面进入“我的歌单”二级页面后，顶部区域仍显示旧的“按最近更新时间排序”说明，并使用文字“管理”入口；这与新 UI 要求不一致。

## 复现步骤

1. 打开应用。
2. 进入“我的”。
3. 点击“我的歌单”统计入口。
4. 观察“我的歌单”二级页面顶部标题、副标题和管理入口。

## 期望行为

“我的歌单”二级页面标题下方显示“共 n 个歌单”，管理入口显示为同一行右侧的管理图标。

## 实际行为

页面标题下方显示“按最近更新时间排序”，右侧显示文字“管理”按钮。

## 附件证据

- 原始 URL：https://github.com/user-attachments/assets/644a33e6-37f8-435f-a096-ffad56d84d10
- 本地路径：`.scratch/github-bugs/assets/2/issue-2-expected-ui.png`
- 类型与大小：PNG 图片，370 x 120，RGBA，10433 字节
- 下载结果：成功，命令为 `curl -L --fail --output .scratch/github-bugs/assets/2/issue-2-expected-ui.png https://github.com/user-attachments/assets/644a33e6-37f8-435f-a096-ffad56d84d10`
- 检查结论：截图中标题为“我的歌单”，副标题为“共 6 个歌单”，右侧为管理图标；附件直接决定顶部文案和动作形态。

## 验收标准

- [x] 按上述步骤不再复现问题。
- [x] 相关页面或状态保持正确。
- [x] 已补充或更新回归测试，或者说明无法补测的原因。

## 修复计划

1. 用当前歌单列表数量生成“共 n 个歌单”摘要，移除未提供交互的排序文案。
2. 移动端“我的歌单”列表页不复用大标题 `AppHeader`，改为按 Figma `Header - Top App Bar` 实现标准 toolbar。
3. 按 Figma `Subheading` 节点还原“共 n 个歌单”文本、透明度、尺寸和右侧管理图标。
4. 桌面端同名二级页复用同一数量摘要，避免跨端口径不一致。
5. 增加共享回归测试，锁定数量摘要规则。

## Comments

### 实现摘要

- `LocalPlaylistListScreen` 顶部副标题改为 `buildLocalPlaylistCountSummary`，展示真实歌单数量。
- 移动端“我的歌单”顶部改为独立 Figma toolbar：64dp 高、40dp 返回按钮、16dp 返回图标、24sp 标题。
- 副标题行按 Figma 节点还原：16sp/24 行高、`#3D4947` 叠加 70% 透明度、右侧管理图标使用 Figma SVG 路径和 `16.667dp x 12.563dp` 尺寸。
- 桌面端“我的歌单”列表页复用同一个数量摘要，去掉旧排序提示。
- 新增 `LocalPlaylistListScreenTest` 覆盖非空和空列表数量摘要。

### 第一性原理根因

这个页面的首要任务是让用户确认自己正在浏览多少个歌单，并进入管理流程。旧实现把仓库排序事实暴露成页面副标题，但用户不能在该处操作排序；同时文字“管理”与新 UI 图标入口不一致。根因不是数据层排序错误，而是列表页顶部展示模型仍停留在旧 UI 语义，未从当前 `playlists` 状态投影成新 UI 所需的数量摘要和图标动作。

### 验证命令与结果

- `./gradlew :composeApp:desktopTest`：通过。
- `./gradlew :composeApp:compileDebugKotlinAndroid`：通过。
- `rg -n "按最近更新时间排序|actionLabel = \"管理\"|PlaylistAddCheck|buildLocalPlaylistCountSummary" composeApp/src/commonMain composeApp/src/commonTest`：目标页面不再残留旧排序文案，数量摘要和图标入口命中预期文件。

### Code Review 结论

未发现阻塞问题。改动范围集中在“我的歌单”列表头部展示，并撤回了上次不必要的通用 `AppHeader` 扩展；没有改变歌单仓库、排序、删除、详情、播放队列或导航状态。

### 对抗式审查

- 可能翻车点一：独立 toolbar 可能偏离 Figma 标准尺寸。复核结果：已按节点 `983:985` 和 `983:986` 固化 64dp toolbar、40dp 返回按钮、16dp 图标、16dp 标题间距和 24sp 标题。
- 可能翻车点二：管理图标形状可能和 Figma 不一致。复核结果：已用节点 `983:998` 的 SVG path 构造自定义 `ImageVector`，并按 `16.667dp x 12.563dp` 渲染。
- 可能翻车点三：数量来源和“我的”页统计不一致。复核结果：列表页显示传入的 `playlists.size`，该列表由控制器从本地歌单仓库构建，和已有 `localPlaylistCount` 口径同源。
- 可能翻车点四：空列表时显示“共 0 个歌单”是否不合理。复核结果：正常入口无歌单时会停留在“我的”页提示，空态只作为防御兜底；测试仍覆盖该路径，避免未来入口策略变化时头部失真。

### 二次修正记录

- 用户复核指出第一次修复仍不符合标准 toolbar，原因是仍复用了旧 `AppHeader` 的大标题结构。
- 已按 Figma 节点 `983:985`、`983:986`、`983:987`、`983:995` 重新读取规格并修正：toolbar 高度 64dp、返回按钮 40dp、返回图标 16dp、标题 24sp/32 行高、subheading 位于 toolbar 下方 16dp、管理图标使用 Figma 原始路径。
- 已撤回上次给 `AppHeader` 增加的 `actionIcon` 和 `actionContentDescription`，避免影响其它页面。
- 二次验证：`./gradlew :composeApp:desktopTest` 通过；`./gradlew :composeApp:compileDebugKotlinAndroid` 通过。

### 剩余风险

未做真机截图复核，视觉风险仅剩图标尺寸和垂直位置是否与附件像素级完全一致；编译和共享测试已覆盖代码可用性与文案口径。

### 提交

- 修复提交：`432fa94a`
