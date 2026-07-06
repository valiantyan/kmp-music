Status: ready-for-human

# 让 Android 扫描声明完整覆盖

## 父级需求

.scratch/local-audio-discovery-source-coverage/PRD.md

## 要做什么

为 Android 成功扫描路径补上最小完成覆盖声明，使持久化合并可以安全地把缺失的 Android MediaStore 歌曲标记为不可用。

任务只处理 Android 成功扫描的完整覆盖语义，不处理 Desktop、iOS、取消或失败路径。

完成覆盖应由 Android 扫描器的成功结果显式携带，不能依赖仓库根据请求、来源摘要或已发现歌曲自行猜测。权限拒绝、异常、用户取消等非成功路径不得声明完整覆盖。

## 验收标准

- [x] Android 成功扫描结果声明完整 Android 系统媒体库来源覆盖。
- [x] Android 完整覆盖测试通过。
- [x] Android 用户不需要管理多个 Android 来源。
- [x] Android 权限失败或扫描异常路径不声明完成覆盖。
- [x] 改动不把 Android 平台 API 移入 common 代码。

## 前置依赖

- .scratch/local-audio-discovery-source-coverage/issues/04-android-complete-coverage-test.md

## Comments

实现摘要：

- 在 Android MediaStore 扫描成功返回的 `LocalMusicScanResult` 中显式声明 `LocalMusicScanCoverage.SourceKind(AndroidMediaStore)`。
- 保持权限拒绝和 MediaStore 查询异常路径继续抛出 `LocalMusicScanException`，这些非成功路径不会返回完成覆盖。
- 未修改 Desktop、iOS、取消、失败扫描生命周期或 common merge 规则；Android 仍只有一个系统媒体库来源，不引入来源管理 UI。

验证命令与结果：

- `./gradlew :composeApp:compileDebugKotlinAndroid`：通过。
- `./gradlew :composeApp:desktopTest --tests com.yanhao.kmpmusic.data.PersistentMusicLibraryRepositoryTest`：通过。
- `./gradlew :composeApp:testDebugUnitTest`：通过。
- 验证过程中仅出现既有 Gradle deprecated property 提示和 `MusicAppControllerTest` 中既有 `No cast needed` 警告，未影响结果。

对抗式审查结论：

- 逻辑漏洞：覆盖声明只出现在 `queryMediaStore()` 成功构造结果处；权限拒绝、查询返回空 cursor、`SecurityException` 和 `IllegalArgumentException` 路径不会产出带覆盖的成功结果。
- 事实正确性：声明的是 `LocalMusicSourceKind.AndroidMediaStore` 的 `SourceKind` 完整覆盖，符合 Android 单一系统媒体库来源语义。
- 是否有更简单做法：无需调整仓库、use case 或新建抽象；直接由 Android scanner 在结果边界声明覆盖是最小实现。
- 是否越界实现后续 issue：未处理 Desktop 具体目录、iOS 累加导入、取消或失败扫描状态。
- 验证是否充分：已跑 Android 编译、Android 单元测试和持久化仓库聚焦测试，覆盖本次平台 scanner 改动和前置完整覆盖 merge 行为。

code-review 结论：

- Standards 轴：未发现硬性违规或 baseline smell；改动保持 Android 平台 API 在 `androidMain`，符合 scanner 窄边界。
- Spec 轴：未发现缺失、越界或错误实现；确认成功路径显式携带 Android MediaStore 完整覆盖，非成功路径不声明覆盖。

剩余风险或未完成项：

- 本 issue 未新增 Android scanner Robolectric 专项测试；当前通过 Android 编译、Android 单元测试套件和前置持久化覆盖测试验证行为链路。
- 后续 Desktop/iOS 具体来源覆盖、取消和失败扫描安全语义仍由后续 issue 处理。
