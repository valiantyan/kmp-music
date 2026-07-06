Status: ready-for-human

# 定义扫描结果完成覆盖语义测试

## 父级需求

.scratch/local-audio-discovery-source-coverage/PRD.md

## 要做什么

添加一个很小的模型或契约测试，用来描述扫描结果必须能区分“已完成来源覆盖”和“positive-only 结果”。这个任务只写测试或测试占位所需的最小断言，不要求完成所有平台实现。

该测试要让后续实现者明确：没有完成覆盖信息的扫描结果没有删除权，即使它发现了某个 `sourceKind` 的歌曲，也不能证明这个 `sourceKind` 已完整扫描。

测试应约束公共扫描结果契约，而不是只验证某个私有 helper。可以让测试先红灯，但它必须指向后续仓库合并逻辑会消费的模型。

## 验收标准

- [x] 测试描述“无完成覆盖 = 无删除权”的契约。
- [x] 测试描述完成覆盖需要显式表达，不能从 `discovered` 或 `sourceSummaries` 自动推导删除权。
- [x] 测试能区分 source-kind 级完整覆盖、具体来源完整覆盖、positive-only 三类语义。
- [x] 测试或测试辅助说明 fake scanner 与平台扫描器都必须声明确定的覆盖语义，不能依赖仓库猜测。
- [x] 测试能作为后续来源覆盖模型实现的红灯或护栏。
- [x] 测试不引入任何 Android、iOS 或 Desktop 平台 API 到 common 代码。

## 前置依赖

- .scratch/local-audio-discovery-source-coverage/issues/01-positive-only-scan-preserves-existing-songs.md

## Comments

实现摘要：已新增 `LocalMusicScanCoverageContractTest`，在 common 模型测试边界描述扫描结果覆盖契约。测试要求 `LocalMusicScanResult` 显式携带 `completedCoverage`，并能表达 source-kind 完整覆盖、具体来源完整覆盖、positive-only 三类语义；同时约束删除权必须来自完成覆盖声明，不能从 `discovered` 或 `sourceSummaries` 推导。测试辅助方法只使用 common 模型，没有引入平台 API。

验证命令与结果：已运行 `./gradlew :composeApp:desktopTest --tests "com.yanhao.kmpmusic.domain.model.LocalMusicScanCoverageContractTest"`。结果为预期红灯，失败发生在 `:composeApp:compileTestKotlinDesktop`：当前公共模型缺少 `LocalMusicScanResult.completedCoverage`、`LocalMusicScanCoverage`、`LocalMusicScanCoverageLevel` 和 `LocalMusicScanDeletionAuthority`。该失败正是本 issue 要交付的契约护栏。

剩余风险或未完成项：本 issue 已完成，等待人工验收。让红灯测试转绿、定义实际覆盖模型、调整 scanner 输出和仓库合并逻辑属于后续 issue，当前没有实现。
