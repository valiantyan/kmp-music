Status: ready-for-human

# 移动端头像加青绿色描边和编辑浮层

## 父级

/Users/yanhao/Desktop/demo/kmp-music/.scratch/mobile-desktop-me-redesign/PRD.md

## 要构建什么

在移动端“我的”页个人资料区展示固定头像、青绿色描边和右下编辑图标浮层。编辑图标只作为静态视觉元素存在，不绑定点击、相册、权限、登录或上传能力。

## 验收标准

- [x] 移动端头像使用已补充的 Figma 静态头像资源。
- [x] 头像显示青绿色描边。
- [x] 头像右下显示编辑图标浮层。
- [x] 编辑图标不可点击，也不触发权限、相册、上传或登录流程。
- [x] 用户名显示为“高保真听众”，副标题显示为“音乐是我的灵魂”。

## 依赖

- 01-figma-static-avatar-resource.md
- 02-mobile-me-remove-old-title-login-card.md

## Comments

### 实现摘要

- 在移动端 `MeScreen` 的个人资料区新增独立头像渲染函数，继续使用前置切片补充的 `me_profile_avatar.jpg` 静态头像资源。
- 头像外层增加青绿色圆形描边，内部图片保持圆形裁剪和铺满显示。
- 头像右下角增加青绿色圆形编辑图标浮层；该浮层仅为静态视觉元素，没有 `clickable`、回调、权限、相册、上传或登录接线。
- 用户名和副标题保持为“高保真听众”和“音乐是我的灵魂”。

### 验证命令与结果

- `./gradlew :composeApp:compileDebugKotlinAndroid`：通过。
- 首次编译发现 `MusicColors.OnAccent` 不存在，已改为现有 Compose `Color.White` 后重新验证通过。

### Code review 结论

- 改动范围只包含移动端 `MeScreen` 和当前 issue 文件，没有修改 Desktop、底部 Tab、全局迷你播放器、登录页或登录路由。
- 编辑图标没有交互修饰符，也没有新增任何业务状态或平台能力入口，符合静态视觉要求。
- 头像资源路径使用前置 issue 已提交的生产资源，没有引用专辑封面、歌手封面或原型目录。

### 对抗式审查

- 风险一：编辑徽标可能被误接成交互入口。复核结果：徽标使用 `Surface` 和 `Icon` 静态绘制，没有点击处理。
- 风险二：头像资源可能不是 Figma 静态头像。复核结果：使用 `drawable/me_profile_avatar.jpg`，该资源由前置 checkpoint 补充。
- 风险三：为了头像视觉误改登录或全局 chrome。复核结果：本次没有修改路由、登录页面、底部 Tab 或迷你播放器相关文件。
- 风险四：颜色 token 不存在导致编译失败。复核结果：首次编译已暴露并修复，最终 Android Kotlin 编译通过。

### 剩余风险或未完成项

- 未做真机或模拟器截图核对，剩余风险是具体像素间距和 Figma 节点仍可能存在轻微视觉偏差；本切片已通过代码复核确认结构、资源、描边和编辑浮层均已落地。
- Issue 04 及后续统计、扫描音乐、设置菜单、最近播放和 Desktop 改造均未实现，按当前任务边界保留给后续 issue。
