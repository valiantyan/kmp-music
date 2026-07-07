Status: ready-for-agent

# 补充 Figma 静态头像资源

## 父级

/Users/yanhao/Desktop/demo/kmp-music/.scratch/mobile-desktop-me-redesign/PRD.md

## 要构建什么

为生产 App 补充 Figma 节点 `919:439` 中使用的独立头像静态位图资源，作为后续移动端和 Desktop/macOS “我的”页头像展示的唯一来源。这个切片只负责资源落库和可引用性，不改页面布局、不接入账号头像，也不用专辑封面或歌手封面替代。

## 验收标准

- [ ] 头像资源来自 Figma 节点 `919:439` 的静态导出。
- [ ] 头像资源加入生产 App 的 Compose 资源目录，并能被共享 UI 引用。
- [ ] 没有修改 `prototypes/kmp-music-hi-fi`。
- [ ] 没有使用专辑封面、歌手封面、登录头像数据或随机图片冒充头像。
- [ ] 在 Comments 记录资源来源、文件名和验证结果。

## 依赖

无，可以立即开始
