Status: ready-for-agent

# 固化本地音乐入口只显示总数和最后扫描时间

## 父级需求

.scratch/local-audio-discovery-source-coverage/PRD.md

## 要做什么

添加一个很小的 UI 状态或文案映射测试，描述本地音乐扫描入口或来源页展示模型只显示当前可播放歌曲总数和最后扫描时间，不展示新增、更新、移除计数。

当前源码不一定存在独立“扫描页”，测试应落在现有本地音乐入口、首页扫描入口、来源页或其展示模型上。测试还应锁住扫描或导入完成后不自动跳离当前页面。该任务只添加测试，不做视觉重构。

## 验收标准

- [ ] 测试断言展示状态包含当前可播放歌曲总数。
- [ ] 测试断言展示状态包含最后扫描时间。
- [ ] 测试断言用户可见展示模型不暴露新增、更新、移除计数。
- [ ] 测试断言扫描或导入完成后仍停留在当前页面或当前路由。
- [ ] 测试不修改高保真原型。

## 前置依赖

- .scratch/local-audio-discovery-source-coverage/issues/03-positive-only-merge-preserves-existing-songs.md
