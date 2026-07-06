Status: ready-for-agent

# 实现本地音乐入口统计与平台文案收敛

## 父级需求

.scratch/local-audio-discovery-source-coverage/PRD.md

## 要做什么

实现最小 UI 状态或文案映射，让本地音乐扫描入口或来源页按平台展示正确入口和来源文案，并只呈现当前可播放歌曲总数与最后扫描时间。

任务只处理文案和展示模型，不做完整页面视觉重做；如果没有独立扫描页，不要为了满足文案新建多余页面。扫描或导入完成后应保留当前页面或当前路由。

## 验收标准

- [ ] Android 文案使用“开始扫描/重新扫描”和“Android 媒体库”。
- [ ] Desktop/macOS 文案使用“添加文件夹/重新扫描”和“扫描目录”。
- [ ] iOS P0 文案使用“导入音频/扫描曲库/重新扫描”和“已添加音频”或“音频来源”。
- [ ] 用户可见本地音乐扫描入口不展示新增、更新、移除计数。
- [ ] 扫描或导入完成后不自动跳离当前页面或当前路由。
- [ ] 本地音乐入口统计文案测试通过。

## 前置依赖

- .scratch/local-audio-discovery-source-coverage/issues/15-scan-page-summary-copy-test.md
