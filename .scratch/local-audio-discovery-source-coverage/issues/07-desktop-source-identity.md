Status: ready-for-human

# 给 Desktop 扫描目录补稳定来源身份

## 父级需求

.scratch/local-audio-discovery-source-coverage/PRD.md

## 要做什么

为 Desktop 扫描目录的扫描结果或来源摘要补上稳定具体来源身份，让后续合并逻辑可以区分文件夹 A 和文件夹 B，而不是只依赖 `sourceKind` 或展示名。

任务只建立最小来源身份，不做完整来源管理 UI。

来源身份应是平台边界内生成、common 层可比较的稳定字符串或值对象。旧数据或旧来源摘要缺少具体来源身份时，不得因此获得删除权；应在后续完成扫描验证后再做具体来源 reconciliation。

## 验收标准

- [x] Desktop 扫描目录输出包含稳定的具体来源身份。
- [x] 具体来源身份不只是展示名称。
- [x] 缺少具体来源身份的旧记录不会被当作“整个 Desktop 来源已覆盖”来删除。
- [x] 现有 UI 展示仍可使用原展示名。
- [x] 不引入 Desktop 平台 API 到 common 不该依赖的位置。

## 前置依赖

- .scratch/local-audio-discovery-source-coverage/issues/06-desktop-folder-accumulation-test.md

## Comments

### 2026-07-06

实现摘要：

- 在 `LocalMusicSourceSummary` 增加可选 `sourceId`，用来承载平台 scanner 生成、common 层可比较的具体来源身份；默认值为 `null`，旧摘要不会因此获得覆盖或删除语义。
- `DesktopFolderMusicScanner` 为用户选择的扫描目录生成规范化绝对路径 `sourceId`，同时写入来源摘要和 `LocalMusicScanCoverage.ConcreteSource`。
- Desktop 来源展示名仍使用原文件夹名，稳定身份和 UI 展示文案分离。
- 新增 `DesktopFolderMusicScannerTest`，用临时目录断言目录 `sourceId` 不等于展示名，并与具体来源覆盖保持一致。
- 未实现来源管理 UI，也未实现后续 issue 的具体目录缺失歌曲下线逻辑。

验证命令与结果：

- `./gradlew :composeApp:desktopTest --tests com.yanhao.kmpmusic.data.DesktopFolderMusicScannerTest`：通过。
- `./gradlew :composeApp:desktopTest --tests com.yanhao.kmpmusic.data.PersistentMusicLibraryRepositoryTest.desktopFolderAccumulationScanKeepsFolderASongWhenScanningFolderB`：通过。
- `./gradlew :composeApp:desktopTest`：通过。
- `./gradlew :composeApp:compileDebugKotlinAndroid`：通过。
- 验证期间并行运行 Gradle 曾触发 Kotlin/KSP 增量缓存和输出目录竞态；已执行 `./gradlew --stop` 后按串行方式重跑上述命令，最终全部通过。
- Gradle 仍输出既有 deprecated Gradle property warning；完整 Desktop 测试链路中既有 `MusicAppControllerTest.kt` 的 `No cast needed` warning 未由本次改动引入。

对抗式审查结论：

- 逻辑漏洞：已检查 Desktop 目录身份由规范化绝对路径生成，`displayName` 仍只是文件夹名，后续合并不会只能依赖展示名。
- 事实正确性：PRD 和 issue 要求来源身份在平台边界内生成且 common 可比较；本次只把 `Path` 处理留在 `desktopMain`，common 只接收 `String?`。
- 更简单做法：没有新增复杂值对象或来源管理仓库；当前 issue 只需要最小稳定字符串身份，后续需要更强类型时再扩展。
- 是否越界：未修改持久化 repository 的具体目录删除规则，避免提前实现 issue 08；也未改 Android、iOS、UI 或原型。
- 验证充分性：新增 scanner 测试覆盖当前实现点，前置累加测试覆盖旧记录不会被误删，完整 `desktopTest` 和 Android 编译覆盖跨 source set 影响。

Code Review 结论：

- Standards 轴：首次审查发现测试空行、断言缺尾逗号、构造参数缺少中文说明；已修复并重新跑验证。修复后本地复核未发现剩余 Standards 问题。
- Spec 轴：无发现；审查确认已满足 Desktop 稳定具体来源身份、身份不只是展示名、旧记录不因缺少身份获得删除权、UI 展示名保留、common 不引入 Desktop API，并且未越界实现后续 issue。

剩余风险或未完成项：

- 旧持久化记录本身仍没有具体目录身份；本 issue 只保证旧记录不会凭空获得删除权，后续具体目录 reconciliation 仍需由后续 issue 完成。
- 当前 Desktop 具体来源身份使用规范化绝对路径；如果未来需要处理大小写敏感性、软链接或卷迁移，应在后续来源管理设计中明确规则。
