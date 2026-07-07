Status: ready-for-human

# 补充 Figma 静态头像资源

## 父级

/Users/yanhao/Desktop/demo/kmp-music/.scratch/mobile-desktop-me-redesign/PRD.md

## 要构建什么

为生产 App 补充 Figma 节点 `919:439` 中使用的独立头像静态位图资源，作为后续移动端和 Desktop/macOS “我的”页头像展示的唯一来源。这个切片只负责资源落库和可引用性，不改页面布局、不接入账号头像，也不用专辑封面或歌手封面替代。

## 验收标准

- [x] 头像资源来自 Figma 节点 `919:439` 的静态导出。
- [x] 头像资源加入生产 App 的 Compose 资源目录，并能被共享 UI 引用。
- [x] 没有修改 `prototypes/kmp-music-hi-fi`。
- [x] 没有使用专辑封面、歌手封面、登录头像数据或随机图片冒充头像。
- [x] 在 Comments 记录资源来源、文件名和验证结果。

## 依赖

无，可以立即开始

## Comments

### 实现摘要

已为生产 App 新增独立头像静态位图资源 `composeApp/src/commonMain/composeResources/drawable/me_profile_avatar.jpg`。该切片只做资源落库和可引用性验证，没有改动页面布局、头像描边、编辑浮层、账号头像数据、最近播放或 Desktop 页面。

### 资源来源与文件名

资源来源为 Figma Desktop MCP 中节点 `919:439` 的头像图片节点 `919:444`。设计上下文给出的 Figma 本地资产地址为 `http://localhost:3845/assets/c7f4d35b39e18957991bc4c620c76d1de78d5b36.png`，下载后确认实际内容为 JPEG。为避免扩展名与内容不一致，最终文件名使用 `me_profile_avatar.jpg`。图片按 Figma 头像显示方式裁剪为 `232x232` 正方形，后续 UI 可在圆形容器中引用并叠加描边或编辑浮层。

### 验证命令与结果

- `file composeApp/src/commonMain/composeResources/drawable/me_profile_avatar.jpg`：通过，确认资源为 `232x232` JPEG 图片。
- `shasum -a 256 composeApp/src/commonMain/composeResources/drawable/me_profile_avatar.jpg`：通过，当前资源哈希为 `f8652a73f993ecfba0a5ae78d586a2204075484b3b0ca177a90a0d20b3f78404`。
- `./gradlew :composeApp:generateComposeResClass :composeApp:compileDebugKotlinAndroid`：通过，构建成功。
- `rg -n "me_profile_avatar" composeApp/build/generated composeApp/build/intermediates composeApp/build/generated/compose -g '*.*'`：通过，生成的 Compose 资源访问器包含 `Res.drawable.me_profile_avatar`，并指向 `drawable/me_profile_avatar.jpg`。

### Code review 结论

自审通过。改动范围只包含当前 issue 文件和生产 Compose 资源目录中的头像资源；未触碰 `prototypes/kmp-music-hi-fi`，未引入运行时账号、专辑封面、歌手封面或随机图片依赖。资源命名符合 Compose resource accessor 生成规则，后续共享 UI 可通过 `Res.drawable.me_profile_avatar` 引用。

### 对抗式审查

- 风险一：误把整页 Figma 截图当头像资源。已规避，只使用头像图片节点 `919:444` 的 Figma 资产。
- 风险二：资源扩展名与实际格式不一致。已规避，下载后用 `file` 确认为 JPEG，并保存为 `.jpg`。
- 风险三：把描边或编辑浮层提前混入资源，影响后续切片。已规避，当前资源只保留头像照片，描边和编辑浮层留给后续 issue。
- 风险四：误修改原型目录或 `.agent-loop` 运行状态。已检查当前切片未修改原型目录；`.agent-loop` 的既有未提交 diff 未纳入本切片。

### 剩余风险或未完成项

无未完成验收项。剩余风险是后续 UI 接入时仍需按 Figma 头像容器使用圆形裁剪、青绿色描边和右下编辑浮层；这些不属于当前 issue 范围。
