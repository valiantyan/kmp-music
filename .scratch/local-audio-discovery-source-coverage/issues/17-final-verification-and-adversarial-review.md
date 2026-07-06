Status: ready-for-agent

# 补最终验证与对抗式审查清单

## 父级需求

.scratch/local-audio-discovery-source-coverage/PRD.md

## 要做什么

做本 PRD 的最后一颗验证任务：运行与改动范围匹配的聚焦测试和构建命令，并补一份对抗式审查清单，确认来源覆盖、取消/失败安全、平台文案和架构边界没有互相打架。

任务不新增功能，只做验证和交付前审查。

最终审查必须回到 PRD 原始验收，而不是只看 issue 是否被勾选。尤其要确认：positive-only 不删除旧歌、Android 成功扫描仍能删除缺失 MediaStore 歌曲、Desktop/iOS 不按 source-kind 宽泛删除、扫描中重复触发不会并发、取消和失败状态文案不同、扫描完成不自动跳离当前页面。

## 验收标准

- [ ] 聚焦持久化曲库合并测试已运行并记录结果。
- [ ] 涉及 controller 或 UI 状态时，共享测试已运行并记录结果。
- [ ] 涉及 Android 扫描器合同时，Android Kotlin 编译已运行并记录结果。
- [ ] 对抗式审查列出至少 3 个最可能翻车点，并说明验证证据。
- [ ] 对抗式审查逐项检查具体歌曲 id 的可用性，不只看统计数字。
- [ ] 对抗式审查确认扫描中单任务与取消入口行为符合 PRD。
- [ ] 对抗式审查确认收藏状态和播放队列不会因 partial scan 误丢未证明不可用的歌曲。
- [ ] 对抗式审查确认扫描或导入完成后仍停留在当前页面或当前路由。
- [ ] 确认没有修改高保真原型来解决生产 App 问题。

## 前置依赖

- .scratch/local-audio-discovery-source-coverage/issues/05-android-scanner-declares-complete-coverage.md
- .scratch/local-audio-discovery-source-coverage/issues/08-desktop-concrete-folder-coverage.md
- .scratch/local-audio-discovery-source-coverage/issues/10-ios-import-positive-only.md
- .scratch/local-audio-discovery-source-coverage/issues/12-cancelled-scan-ui-state.md
- .scratch/local-audio-discovery-source-coverage/issues/14-failed-scan-positive-only-merge.md
- .scratch/local-audio-discovery-source-coverage/issues/16-scan-page-platform-copy.md
