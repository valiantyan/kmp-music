Status: ready-for-agent

# 实现失败扫描 positive-only 安全合并

## 父级需求

.scratch/local-audio-discovery-source-coverage/PRD.md

## 要做什么

实现最小失败扫描合并规则：失败扫描可以保留已经验证或写入的正向结果，但不能下线未处理旧歌。

任务只处理失败扫描的安全合并，不处理取消状态文案或平台扫描器。

如果当前扫描异常不会携带部分成功结果，不要为通过测试而伪造删除权；应选择明确的部分结果或结果模型，或在异常路径保留旧快照并只提交已经安全写入的正向结果。

## 验收标准

- [ ] 失败扫描不会删除未处理旧歌。
- [ ] 失败扫描可以保留已验证正向结果。
- [ ] 失败扫描不显示或暗示旧歌被删除。
- [ ] 普通失败状态仍渲染为“扫描失败”，不混同“已取消”。
- [ ] 失败扫描不删除旧歌测试通过。

## 前置依赖

- .scratch/local-audio-discovery-source-coverage/issues/13-failed-scan-preserves-existing-songs-test.md
