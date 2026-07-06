Status: ready-for-agent

# 固化失败扫描不删除未处理旧歌测试

## 父级需求

.scratch/local-audio-discovery-source-coverage/PRD.md

## 要做什么

添加一个聚焦测试，描述失败扫描可以保留已写入正向结果，但不能把未处理的既有歌曲标记为不可用。

该任务只添加红灯测试，不实现失败路径合并。

测试应覆盖仓库合并或控制器错误路径中用户可感知的曲库安全：失败后旧歌仍在可用列表中。不要只断言扫描状态进入错误态。

## 验收标准

- [ ] 测试包含扫描失败前已存在的可用歌曲。
- [ ] 测试断言失败扫描后未处理旧歌仍可用。
- [ ] 测试断言失败扫描不会暗示旧歌被删除。
- [ ] 测试断言具体旧歌 id 仍可用。
- [ ] 测试名称或注释说明失败扫描没有删除权。

## 前置依赖

- .scratch/local-audio-discovery-source-coverage/issues/03-positive-only-merge-preserves-existing-songs.md
