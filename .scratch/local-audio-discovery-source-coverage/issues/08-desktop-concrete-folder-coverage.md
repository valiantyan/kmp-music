Status: ready-for-human

# 让 Desktop 只按具体目录覆盖删除

## 父级需求

.scratch/local-audio-discovery-source-coverage/PRD.md

## 要做什么

实现最小合并规则：Desktop 完整扫描只覆盖本次具体扫描目录。文件夹 B 的完成扫描可以 reconciliation 文件夹 B 内缺失歌曲，但不能影响文件夹 A。

任务只处理 Desktop 具体目录覆盖，不处理 iOS 或 Android。

合并判断必须使用显式具体目录覆盖身份，不要用 `sourceKind`、展示名或路径前缀猜测来决定删除范围。

## 验收标准

- [x] 文件夹 B 完整扫描不会下线文件夹 A 歌曲。
- [x] 同一文件夹完整扫描可以下线该文件夹内缺失歌曲。
- [x] 测试覆盖“文件夹 B 内缺失旧歌会下线、文件夹 A 旧歌仍可用”两个方向。
- [x] Desktop 文件夹累加测试通过。
- [x] 合并逻辑不再把 Desktop 的 `sourceKind` 当作整个来源覆盖。

## 前置依赖

- .scratch/local-audio-discovery-source-coverage/issues/07-desktop-source-identity.md

## Comments

### 2026-07-06

实现摘要：

- 为扫描元数据和 `local_song` 持久化记录增加可空 `concreteSourceId`，并新增 Room v6 schema 与 `MIGRATION_5_6`。旧记录保持 `NULL`，不会因为缺少具体目录身份而获得删除权。
- `DesktopFolderMusicScanner` 将本次扫描目录的稳定来源身份写入每首 `MusicFileMetadata.concreteSourceId`，并继续通过 `ConcreteSource(DesktopFolder, folderSourceId)` 声明完成覆盖。
- `PersistentMusicLibraryRepository` 新增具体来源级 reconciliation：只有 `ConcreteSource` 覆盖会按 `sourceKind + concreteSourceId` 下线缺失歌曲；`SourceKind(DesktopFolder)` 被过滤，不再拥有整个 Desktop 来源类型的删除权。
- 测试覆盖文件夹 B 完整扫描下线 B 内缺失旧歌、保留文件夹 A 旧歌，并增加 `SourceKind(DesktopFolder)` 不会整类删除的回归测试；Desktop scanner 测试也断言真实扫描输出的歌曲元数据携带同一目录身份。

验证命令与结果：

- `./gradlew :composeApp:desktopTest --tests com.yanhao.kmpmusic.data.PersistentMusicLibraryRepositoryTest.desktopFolderAccumulationScanKeepsFolderASongWhenScanningFolderB --tests com.yanhao.kmpmusic.data.PersistentMusicLibraryRepositoryTest.desktopSourceKindCoverageDoesNotDeleteWholeDesktopLibrary --tests com.yanhao.kmpmusic.data.DesktopFolderMusicScannerTest`：通过。
- `./gradlew :composeApp:desktopTest`：通过。
- `./gradlew :composeApp:compileDebugKotlinAndroid`：通过。
- `git diff --check`：通过。
- Gradle 仍输出既有 deprecated Gradle property warning；focused test 首次重编译时仍看到既有 `MusicAppControllerTest.kt` 的 `No cast needed` warning，均非本次改动引入。

对抗式审查结论：

- 逻辑漏洞：重点攻击了“扫描 B 是否会误删 A”和“B 内缺失旧歌是否真的下线”。当前测试同时断言 A 保留、B 旧歌下线、B 新歌可用。
- 事实正确性：合并逻辑只使用显式 `ConcreteSource.sourceId` 和持久化 `concreteSourceId` 精确匹配；没有使用展示名或路径前缀猜测删除范围。
- 更简单做法：仅在已有 scanner、domain model、Room DAO、repository 合并边界上补最小字段与查询，没有新增来源管理 UI 或跨平台扫描逻辑。
- 是否越界：未实现 iOS、Android 后续来源覆盖语义；Android `SourceKind` 覆盖仍保留，Desktop `SourceKind` 覆盖被显式过滤。
- 验证充分性：已跑 focused repository/scanner 测试、完整 `desktopTest`、Android Kotlin 编译和 diff 空白检查。

Code Review 结论：

- Standards 轴：第一轮指出 repository 缺失 id 推导有重复逻辑；已提取 `discoveredSongIdsForSourceKind` 和 `discoveredSongIdsForConcreteSource` 共用。第二轮复核无硬性规范违规、无剩余可执行 smell。
- Spec 轴：第一轮指出 scanner 边界少 metadata 目录身份断言，已补；第二轮指出 `SourceKind(DesktopFolder)` 仍可能整类删除，已过滤并新增回归测试。最终本地复核确认 issue 08 验收标准已满足。

剩余风险或未完成项：

- 升级前已经存在的 Desktop 歌曲行没有 `concreteSourceId`，本 issue 按前置 issue 07 的安全约束不使用路径前缀回填或猜测，因此这些旧行不会被第一次目录扫描直接下线；需要后续完整来源验证或显式管理流程处理。
- `concreteSourceId` 仍是平台生成的稳定字符串；如果未来要处理软链接、大小写敏感文件系统或目录迁移，需要在后续来源管理设计中进一步明确规范化规则。
