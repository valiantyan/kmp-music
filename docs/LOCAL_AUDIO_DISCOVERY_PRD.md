# KMP Music 本地音频发现 PRD

## 1. 文档信息

| 项目 | 内容 |
| --- | --- |
| 产品名称 | KMP Music |
| 文档名称 | 本地音频发现 PRD |
| 目标平台 | Android、iOS、Desktop |
| 技术方向 | Kotlin Multiplatform + Compose Multiplatform |
| 文档版本 | v0.1 |
| 创建日期 | 2026-06-22 |
| 当前阶段 | 本地音频发现 MVP 定义 |

## 2. 背景与目标

KMP Music 是本地音乐优先的跨平台播放器。当前项目已有本地音乐列表、搜索、收藏、播放队列和播放 UI，但歌曲数据仍主要来自 seed/mock 数据。后续要支持真实播放，必须先解决“各平台如何合法发现用户授权给 App 的可播放音频”。

本 PRD 面向人类开发者和 LLM Agent，明确 Android、iOS、Desktop 的本地音频发现范围、平台边界、数据模型、错误模型、验收标准和后续演进方向。

本 PRD 与 `docs/PLAYBACK_PRD.md` 的关系：

- 本文负责“拿到可播放音频与元数据”。
- `docs/PLAYBACK_PRD.md` 负责“拿到 `localUri` / `PlayableMedia` 后如何播放、后台播放和同步状态”。

## 3. 核心结论

1. Android 可以在用户授权后通过 `MediaStore.Audio` 查询系统媒体索引。
2. iOS 不能实现与 Android 等价的“权限满足后扫描整台设备所有可播放音频文件”。
3. iOS P0 应采用用户显式选择/导入音频文件的模式：通过 Files / Document Picker 选择音频，导入 App 沙盒后扫描自己的曲库目录。
4. iOS P1 可评估 `MPMediaLibrary` / `MPMediaQuery` 读取系统音乐资料库，但它不是文件系统扫描，也不能保证所有条目都有本地可播放 URL。
5. Desktop P0 应采用用户选择文件夹的模式：用户选择一个或多个音乐目录，App 递归扫描可读音频文件。
6. `commonMain` 只定义扫描请求、扫描状态、扫描结果、音乐文件元数据和错误模型，不直接依赖 Android、iOS 或 Desktop 文件权限 API。

## 4. 范围定义

### 4.1 Android P0

Android 首阶段必须支持：

| 能力 | 优先级 | 说明 |
| --- | --- | --- |
| 音频读取权限申请 | P0 | 按 Android 系统版本申请读取音频所需权限。 |
| 查询系统音频媒体库 | P0 | 使用 `MediaStore.Audio` 查询本机音频媒体索引。 |
| 读取基础元数据 | P0 | 标题、歌手、专辑、时长、mime、size、date modified 等。 |
| 生成可播放 URI | P0 | 输出平台播放器可识别的 `content://` URI。 |
| 增量同步基础能力 | P0 | 可根据 MediaStore 版本、时间戳或重新扫描刷新曲库缓存。 |
| 权限和扫描错误反馈 | P0 | 用户拒绝、权限失效、查询失败、空结果都进入统一状态。 |

Android 推荐技术路线：

- `READ_MEDIA_AUDIO`，适用于 Android 13/API 33 及以上。
- `READ_EXTERNAL_STORAGE`，适用于旧版本兼容场景。
- `ContentResolver.query(...)`
- `MediaStore.Audio.Media`

Android 注意事项：

- 不要手写全盘文件遍历绕过 MediaStore。
- 不要把 Android 权限和 `ContentResolver` 放进 `commonMain`。
- 不要默认把所有 `MediaStore.Files` 结果都当作音频曲库。

### 4.2 iOS P0

iOS 首阶段必须支持：

| 能力 | 优先级 | 说明 |
| --- | --- | --- |
| 用户选择音频文件 | P0 | 通过 Files / Document Picker 让用户选择音频文件。 |
| 导入到 App 沙盒 | P0 | 将选择的音频复制到 App 可管理的本地目录。 |
| 扫描 App 沙盒曲库目录 | P0 | 只扫描 App 已导入、下载或创建的音频文件。 |
| 读取基础元数据 | P0 | 标题、歌手、专辑、时长、文件类型、size 等。 |
| 生成可播放 file URL | P0 | 输出 AVFoundation 可识别的本地文件 URL 或平台包装后的 `localUri`。 |
| 用户取消和导入错误反馈 | P0 | 用户取消、文件不可读、格式不支持、导入失败都进入统一状态。 |

iOS 推荐技术路线：

- `UIDocumentPickerViewController`
- `UTType.audio`
- `FileManager`
- AVFoundation metadata 读取能力，例如 `AVAsset`

iOS P0 明确不做：

- 不做“扫描整个 iPhone 本地音频文件”。
- 不做后台自动扫描 Files App、iCloud Drive 或其他 App 容器。
- 不要求直接播放未导入且未持久授权的外部文件。
- 不把 `MPMediaLibrary` / `MPMediaQuery` 作为 P0。

### 4.3 iOS P1

iOS P1 可评估系统音乐资料库读取：

| 能力 | 优先级 | 说明 |
| --- | --- | --- |
| 媒体资料库授权 | P1 | 使用 `NSAppleMusicUsageDescription` 说明用途并请求授权。 |
| 查询系统音乐资料库 | P1 | 使用 `MPMediaLibrary` / `MPMediaQuery` 获取音乐条目。 |
| 过滤不可本地播放条目 | P1 | Apple Music、云端、DRM 或无 asset URL 条目不能直接作为本地文件。 |
| 与导入曲库合并展示 | P1 | 系统音乐资料库与 App 导入曲库需要有来源标识。 |

iOS P1 风险：

- 媒体资料库不是文件系统目录，不能按文件夹扫描理解。
- 不是每个媒体库 item 都有可直接播放的本地 URL。
- 用户授权、云端状态、DRM 和 Apple Music 订阅内容会影响可播放性。

### 4.4 Desktop P0

Desktop 首阶段必须支持：

| 能力 | 优先级 | 说明 |
| --- | --- | --- |
| 用户选择音乐文件夹 | P0 | 由用户显式选择一个本地目录作为音乐来源。 |
| 递归扫描文件夹 | P0 | 遍历用户选择目录下的可读文件。 |
| 音频文件过滤 | P0 | 基于扩展名、MIME 或播放后端支持能力过滤。 |
| 读取基础元数据 | P0 | 标题、歌手、专辑、时长、文件大小、修改时间等。 |
| 生成可播放路径 | P0 | 输出 Desktop 播放后端可识别的 file path / URI。 |
| 文件夹不可用反馈 | P0 | 文件夹删除、移动、外置盘断开、权限不足时有错误状态。 |

Desktop 推荐技术路线：

- JVM 文件夹选择器，例如 Swing `JFileChooser` 或 Compose Desktop 可用的文件选择方案。
- `java.nio.file.Files.walk(...)`
- `Files.isRegularFile(...)`
- `Files.isReadable(...)`
- 后续根据 Desktop 播放后端选择元数据读取方式。

Desktop P0 明确不做：

- 不默认扫描全盘。
- 不扫描系统目录。
- 不在用户未选择目录时后台爬取用户文件。
- 不要求系统级文件变更监听。
- 不要求 macOS sandbox security-scoped bookmark；若后续进 Mac App Store 再单独评估。

## 5. 非目标范围

以下能力不属于本阶段：

1. 在线音乐服务扫描或导入。
2. DRM、会员、版权校验。
3. iOS 全设备音频文件扫描。
4. iOS 未经用户选择访问 Files App 全部内容。
5. Desktop 默认扫描用户全盘或系统目录。
6. 桌面端系统媒体库索引集成。
7. 云端音乐同步。
8. 文件内容指纹去重。
9. 歌词扫描与匹配。
10. 专辑封面网络补全。

## 6. 用户体验要求

### 6.1 Android 扫描体验

1. 用户进入本地音乐页时，如果缺少音频读取权限，应看到明确授权入口。
2. 用户同意权限后，App 扫描并展示系统音频媒体库中的歌曲。
3. 用户拒绝权限后，App 保持可用，并展示权限缺失状态和再次授权入口。
4. 扫描结果为空时，展示空状态，而不是播放 mock 歌曲冒充真实扫描结果。
5. 重新扫描后，新增、删除或变化的歌曲应能反映到本地曲库。

### 6.2 iOS 导入体验

1. iOS 页面文案应使用“导入音频”或“从 Files 选择音频”，不要使用“扫描整机音乐”。
2. 用户点击导入后，系统文件选择器只展示或优先展示音频文件。
3. 用户选择文件后，App 将文件导入自己的沙盒曲库目录。
4. 导入完成后，歌曲出现在本地音乐列表，并可进入播放流程。
5. 用户取消选择时，不显示错误，只回到导入入口。
6. 文件不可读、复制失败或格式不支持时，展示明确错误反馈。

### 6.3 Desktop 扫描体验

1. Desktop 页面应提供“选择音乐文件夹”入口。
2. 用户选择文件夹后，App 递归扫描该目录并展示扫描进度或结果。
3. 用户可以重新扫描已选择文件夹。
4. 文件夹不可用时，App 提示用户重新选择。
5. 扫描结果为空时，说明该目录下未找到支持的音频文件。

## 7. 共享架构要求

### 7.1 分层边界

本地音频发现能力应保持如下边界：

```text
commonMain
├── scan request
├── scan progress
├── scan result
├── music file metadata
├── source kind
├── platform-neutral scan error
└── LocalMusicScanner interface

androidMain
└── AndroidMediaStoreScanner

iosMain
├── IosDocumentMusicImporter
└── IosMediaLibraryScanner optional

desktopMain
└── DesktopFolderMusicScanner
```

### 7.2 `commonMain` 可以包含

1. 扫描入口接口：`LocalMusicScanner` 或等价 repository / use case 接口。
2. 扫描请求：扫描全部、扫描指定来源、重新扫描。
3. 扫描状态：idle、waitingForPermission、scanning、importing、done、error。
4. 扫描进度：已处理数量、已发现数量、当前来源。
5. 扫描结果：新增、更新、删除、失败条目。
6. 音乐文件元数据：title、artist、album、durationMs、mimeType、sizeBytes、modifiedAt。
7. 来源标识：androidMediaStore、iosImportedFile、iosMediaLibrary、desktopFolder。
8. 平台无关错误：permissionDenied、userCancelled、fileUnreadable、unsupportedFormat、folderUnavailable、metadataUnavailable、unknown。

### 7.3 `commonMain` 禁止包含

1. Android `ContentResolver`、`MediaStore`、`Uri`、Manifest 权限判断。
2. iOS `UIDocumentPickerViewController`、`FileManager`、`AVAsset`、`MPMediaLibrary`、`MPMediaQuery`。
3. Desktop `JFileChooser`、`java.nio.file.Files`、平台文件选择 API。
4. 任何平台弹窗、权限请求、文件选择器实现。
5. 任何假设 `localUri` 是普通文件路径的逻辑。

### 7.4 推荐接口形态

```kotlin
interface LocalMusicScanner {
    val events: Flow<LocalMusicScanEvent>

    suspend fun scan(request: LocalMusicScanRequest): LocalMusicScanResult
}
```

接口只表达平台无关的扫描意图。平台实现负责授权、文件选择、媒体库查询、文件遍历和元数据读取。

### 7.5 现有 `ScanLocalMusicUseCase` 的演进关系

当前 `ScanLocalMusicUseCase` 是阶段一模拟扫描状态切换，不代表真实扫描能力。后续演进应保持两个角色：

1. `ScanLocalMusicUseCase`：协调 UI 意图、权限状态、扫描状态和曲库更新。
2. `LocalMusicScanner`：封装平台真实音频发现能力，负责返回扫描结果和错误。

不要把平台 scanner 直接注入到 Composable 页面。页面只触发“导入音频”“选择文件夹”“重新扫描”等意图，由 Controller / UseCase 协调平台实现。

## 8. 数据与状态要求

### 8.1 音乐文件数据

真实本地音频发现至少需要：

| 字段 | 说明 |
| --- | --- |
| `id` | App 内稳定歌曲标识。 |
| `sourceId` | 平台来源侧稳定标识，例如 MediaStore id、导入文件 id、desktop path hash。 |
| `sourceKind` | 来源类型：Android MediaStore、iOS imported file、iOS media library、Desktop folder。 |
| `title` | 歌曲标题。 |
| `artist` | 歌手名称。 |
| `album` | 专辑名称。 |
| `durationMs` | 毫秒时长。 |
| `localUri` | 平台可解析的本地媒体 URI 或路径。 |
| `mimeType` | 文件 MIME 或系统媒体类型。 |
| `sizeBytes` | 文件大小。 |
| `modifiedAt` | 文件最后修改时间或媒体库更新时间。 |
| `coverArt` | 本地封面引用或 App 默认封面。 |

`localUri` 不应由 UI 拼接。它必须由平台 scanner 或测试 fake scanner 产生，并在进入播放 engine 前转换成平台可识别的媒体定位信息。

### 8.2 扫描状态

扫描状态至少需要：

| 状态 | 说明 |
| --- | --- |
| `idle` | 未开始扫描。 |
| `waitingForPermission` | 等待用户授权或选择来源。 |
| `importing` | iOS 导入文件或复制文件中。 |
| `scanning` | 正在枚举或读取元数据。 |
| `done` | 扫描完成。 |
| `error` | 扫描失败或部分失败。 |

### 8.3 扫描错误

扫描错误至少需要：

| 错误 | 说明 |
| --- | --- |
| `permissionDenied` | 用户拒绝授权或系统权限不可用。 |
| `userCancelled` | 用户取消选择文件或文件夹。 |
| `folderUnavailable` | Desktop 文件夹不存在、移动或外置盘断开。 |
| `fileMissing` | 缓存中的文件已不存在。 |
| `fileUnreadable` | 文件不可读或权限不足。 |
| `unsupportedFormat` | 文件格式暂不支持。 |
| `metadataUnavailable` | 无法读取标题、时长等元数据。 |
| `securityScopeExpired` | iOS 外部文件授权失效。 |
| `unknown` | 未归类错误。 |

`userCancelled` 不应被当作失败弹窗；它是用户主动退出流程。

## 9. 平台能力详解

### 9.1 Android 音频发现

Android scanner 应从平台层完成以下工作：

1. 判断当前系统版本需要的权限。
2. 请求或检查音频读取权限。
3. 使用 `ContentResolver` 查询 `MediaStore.Audio.Media`。
4. 过滤非音乐、不可播放或无效条目。
5. 读取 `_ID`、`TITLE`、`ARTIST`、`ALBUM`、`DURATION`、`MIME_TYPE`、`SIZE` 等字段。
6. 生成 `content://` URI。
7. 映射为 common 层 `MusicFileMetadata` / `Song`。

### 9.2 iOS 导入音频

iOS P0 importer 应从平台层完成以下工作：

1. 打开 `UIDocumentPickerViewController`。
2. 使用 `UTType.audio` 或等价方式过滤音频文件。
3. 处理用户取消选择。
4. 将选中文件复制到 App 沙盒的曲库目录。
5. 对导入后的沙盒文件读取元数据。
6. 生成 App 可长期访问的 file URL。
7. 映射为 common 层 `MusicFileMetadata` / `Song`。

导入目录建议放在 App 可管理的位置，并避免大型音频文件被不必要地 iCloud 备份。具体目录和备份策略在实现阶段按 Apple 存储规范确认。

### 9.3 iOS 系统音乐资料库

iOS P1 media library scanner 应从平台层完成以下工作：

1. 配置 `NSAppleMusicUsageDescription`。
2. 请求媒体资料库授权。
3. 使用 `MPMediaQuery` 查询音乐条目。
4. 读取标题、歌手、专辑、时长等元数据。
5. 判断条目是否有可播放 asset URL。
6. 过滤云端、DRM 或不可本地播放条目。
7. 与导入曲库合并时保留 `sourceKind`。

这一路线不能替代 P0 导入文件能力。

### 9.4 Desktop 文件夹扫描

Desktop scanner 应从平台层完成以下工作：

1. 打开文件夹选择器。
2. 保存用户选择的目录。
3. 使用 JVM 文件 API 递归扫描目录。
4. 跳过隐藏目录、系统目录、不可读文件和过大的异常文件。
5. 过滤支持的音频扩展名或 MIME。
6. 读取基础元数据。
7. 生成 file path / URI。
8. 映射为 common 层 `MusicFileMetadata` / `Song`。

Desktop 扫描必须有超时、取消或进度反馈设计，避免大目录扫描让 UI 看起来卡死。

## 10. 与播放能力的衔接

本地音频发现成功后，应输出播放 PRD 可消费的数据：

```text
LocalMusicScanner
  -> MusicFileMetadata / Song
  -> localUri / PlayableMedia
  -> AudioPlayerEngine.load(...)
```

播放链路不负责申请文件访问权限，也不负责扫描目录。播放链路只消费已经由 scanner 生成且当前仍可访问的 `localUri` / `PlayableMedia`。

如果播放时发现文件缺失、权限失效或格式不支持，应回写到本地曲库状态，并进入播放 PRD 中定义的统一错误模型。

## 11. LLM Agent 实现约束

LLM Agent 修改本地音频发现能力时必须遵守：

1. 修改前先读本 PRD、`docs/PLAYBACK_PRD.md`、`docs/PRD.md`、`AGENTS.md` 和相关源码。
2. 不要在 UI Composable 中直接写 Android `MediaStore` 查询、iOS Document Picker 或 Desktop 文件遍历。
3. 不要在 `commonMain` 引入平台文件权限 API。
4. 不要把 iOS 需求描述成“扫描整机本地音乐”。
5. 不要用 mock seed 数据冒充真实扫描结果。
6. 先定义平台无关接口、状态和错误模型，再接平台实现。
7. Android 实现必须放在 `androidMain` 或 Android 专属适配层。
8. iOS 实现必须放在 `iosMain` 或 Swift/Objective-C 包装层。
9. Desktop 实现必须放在 `desktopMain`。
10. 改动曲库、搜索、收藏、播放队列或播放状态联动时，必须补充共享测试。
11. 无法验证真实设备行为时，最终说明必须标明剩余平台风险。

## 12. 分阶段里程碑

### Milestone 1: 共享扫描边界

目标：

- 建立 `LocalMusicScanner` 平台无关接口。
- 定义扫描请求、状态、结果、错误和来源类型。
- 扩展 `Song` 或新增 `MusicFileMetadata` / `PlayableMedia`，承载 `durationMs`、`localUri`、`sourceKind` 等真实播放字段。
- 保留 fake scanner，保证共享测试和现有 UI 可运行。

验收：

- `commonMain` 不包含任何平台文件或权限 API。
- UI 可以区分 idle、scanning、done、error、userCancelled。
- 共享测试覆盖扫描状态流转、空结果、错误结果和曲库更新。

### Milestone 2: Android MediaStore 扫描

目标：

- 实现 Android 权限检查和请求流程。
- 接入 `MediaStore.Audio` 查询。
- 生成可播放 `content://` URI。
- 将扫描结果同步到本地曲库。

验收：

- Android 真机或模拟器可授权并扫描本机音频。
- 拒绝权限时 UI 有明确状态。
- 删除或新增音频后，重新扫描能反映变化。
- 扫描结果可交给播放链路播放。

### Milestone 3: iOS 文件导入与沙盒扫描

目标：

- 接入 Document Picker 音频选择。
- 将音频导入 App 沙盒曲库目录。
- 扫描导入目录并读取元数据。
- 将导入歌曲同步到本地曲库。

验收：

- iOS 模拟器或真机可选择音频文件并导入。
- 用户取消选择不会显示错误。
- 导入失败、文件不可读、格式不支持时有错误反馈。
- 导入歌曲可交给播放链路播放。

### Milestone 4: Desktop 文件夹扫描

目标：

- 接入文件夹选择器。
- 递归扫描用户选择的音乐目录。
- 过滤音频文件并读取元数据。
- 将扫描结果同步到本地曲库。

验收：

- macOS Desktop 和 Windows Desktop 可选择文件夹。
- 目录下支持的音频文件可出现在本地曲库。
- 文件夹删除、移动或权限不足时有错误反馈。
- 扫描歌曲可交给播放链路播放。

### Milestone 5: iOS 系统音乐资料库评估

目标：

- 评估 `MPMediaLibrary` / `MPMediaQuery` 的授权、可播放性和 App Store 风险。
- 明确哪些条目可进入本地曲库。
- 决定是否作为 P1 功能实现。

验收：

- 有明确 spike 结论。
- 不影响 P0 文件导入能力。
- 文档更新 iOS media library 的产品边界。

## 13. 测试要求

### 13.1 共享测试

必须覆盖：

1. 扫描开始后状态从 idle 进入 scanning 或 waitingForPermission。
2. 扫描成功后曲库新增歌曲。
3. 空结果进入 done 但不报错。
4. 用户取消进入 userCancelled 或等价非错误状态。
5. 权限拒绝进入 permissionDenied。
6. 文件缺失、不可读、格式不支持进入对应错误。
7. 扫描生成的 `localUri` 不由 UI 拼接。
8. 扫描结果和播放队列、搜索、收藏状态不互相破坏。

### 13.2 Android 验证

至少验证：

1. `:composeApp:compileDebugKotlinAndroid` 通过。
2. Android 真机或模拟器权限申请流程可用。
3. `MediaStore.Audio` 扫描能返回真实音频。
4. 拒绝权限和空曲库状态可用。
5. 扫描歌曲可播放。

### 13.3 iOS 验证

至少验证：

1. iOS 编译通过。
2. Document Picker 可选择音频文件。
3. 音频文件能导入 App 沙盒。
4. 用户取消不报错。
5. 导入歌曲可播放。

### 13.4 Desktop 验证

至少验证：

1. `:composeApp:desktopTest` 通过。
2. Desktop App 可选择音乐文件夹。
3. 扫描能识别支持的音频文件。
4. 文件夹不可用时有错误反馈。
5. 扫描歌曲可播放。

## 14. 风险与待调研问题

| 问题 | 风险 | 当前处理 |
| --- | --- | --- |
| iOS 与 Android 扫描模型不等价 | 产品和实现容易误写成 iOS 全机扫描。 | PRD 明确 iOS P0 是用户导入文件，不是整机扫描。 |
| iOS Document Picker 持久访问 | Open 模式需要 security-scoped URL 和 bookmark，复杂度高。 | P0 使用导入到沙盒，Open 模式后续评估。 |
| iOS 系统音乐资料库可播放性 | Apple Music、云端、DRM 条目可能不可作为本地文件播放。 | P1 单独 spike，不影响 P0。 |
| Desktop 大目录扫描 | 扫描耗时、符号链接、权限、外置盘断开可能导致体验差。 | P0 要求进度、错误和取消/重扫设计。 |
| 元数据读取跨平台差异 | Android/iOS/Desktop 字段完整度不同。 | common 层字段允许缺省，并保留 metadataUnavailable。 |
| 文件 URI 生命周期 | 文件被删除、移动或权限失效后播放失败。 | 扫描和播放错误都回写曲库状态。 |

## 15. 举一反三问题速查

本节用于帮助人类和 LLM Agent 判断：遇到相邻问题时，是否能从本 PRD 找到处理方向。

| 问题 | 文档答案 | 依据 |
| --- | --- | --- |
| iOS 能像 Android 一样权限通过后扫描整台手机音频吗？ | 不能。iOS P0 只能扫描 App 沙盒或用户显式导入的文件。 | 3、4.2、5 |
| iOS 是否应该显示“扫描本机音乐”？ | 不应该。文案应使用“导入音频”或“从 Files 选择音频”。 | 6.2、11 |
| iOS `MPMediaLibrary` 是不是 P0？ | 不是。它是 P1 评估项，且不是文件系统扫描。 | 4.3、12 |
| Desktop 能否启动时默认扫描用户全盘？ | 不能。必须由用户选择音乐文件夹。 | 4.4、5、6.3 |
| Android 是否可以绕过 MediaStore 手写全盘遍历？ | 不推荐。P0 使用 `MediaStore.Audio`。 | 4.1、9.1 |
| UI 可以自己拼接 `localUri` 吗？ | 不可以。`localUri` 必须由平台 scanner 或 fake scanner 产生。 | 8.1、11 |
| 用户取消 iOS 文件选择算错误吗？ | 不算失败弹窗，应作为非错误状态处理。 | 6.2、8.3、13 |
| 扫描成功但播放失败怎么办？ | 播放链路反馈文件缺失、权限失效或格式不支持，并回写曲库状态。 | 10、14 |
| 当前 `ScanLocalMusicUseCase` 是否已经是真实扫描？ | 不是，它只是阶段一模拟状态切换。 | 7.5 |
| 能不能先只做 Android，iOS/Desktop 继续 mock？ | 可以分阶段，但 UI 必须明确平台能力状态，不能用 mock 冒充真实扫描。 | 11、12 |
| iOS P0 能否直接播放用户从 Files 选择但未导入的文件？ | 不作为 P0。P0 先导入到 App 沙盒，再扫描和播放。 | 4.2、9.2 |
| iOS 能否让用户选择一个文件夹后递归扫描？ | 不作为 P0。iOS P0 是选择/导入音频文件；目录级访问若要做，必须单独评估安全授权和平台限制。 | 4.2、14 |
| iOS 系统音乐资料库中的 Apple Music 歌曲能否都进入本地曲库？ | 不能。必须过滤云端、DRM、无 asset URL 或不可本地播放条目。 | 4.3、9.3、14 |
| Desktop 是否需要监听文件夹变化实时刷新？ | 不需要。P0 只要求选择文件夹、递归扫描和重新扫描。 | 4.4、12 |
| Desktop 扫描遇到符号链接或隐藏目录怎么办？ | P0 应跳过隐藏目录、系统目录、不可读文件和异常文件，避免扫描失控。 | 9.4、14 |
| Android 权限拒绝后能否继续展示 seed/mock 歌曲？ | 不能用 mock 冒充真实扫描；应展示权限缺失或空状态。 | 6.1、11 |
| `Song.duration` 已有字符串时是否还需要 `durationMs`？ | 需要。真实播放、排序、进度和系统展示需要毫秒级字段。 | 8.1、10 |
| 扫描结果是否必须立刻覆盖收藏和播放队列？ | 不应破坏现有收藏、搜索、播放队列状态；需要按稳定 id 做合并或迁移。 | 8.1、13 |
| 播放链路是否负责重新申请文件权限？ | 不负责。权限、导入和扫描属于本 PRD；播放链路只消费仍可访问的 `localUri` / `PlayableMedia`。 | 10 |
| 实现时可以先把平台 API 塞进 `MusicAppController` 吗？ | 不可以。Controller 可以协调 UseCase，但平台扫描实现必须留在平台 source set 或适配层。 | 7、11 |

## 16. 三轮交叉 Review 结果

### 16.1 第一轮：产品范围 Review

结论：

- Android P0 范围清晰：授权后通过 `MediaStore.Audio` 扫描系统音频媒体索引。
- iOS P0 范围清晰：用户通过 Files / Document Picker 选择音频，导入 App 沙盒后扫描；不承诺整机音频扫描。
- Desktop P0 范围清晰：用户选择音乐文件夹，App 递归扫描可读音频文件；不默认扫描全盘。
- iOS `MPMediaLibrary` / `MPMediaQuery` 被放在 P1，避免把系统音乐资料库误认为文件系统扫描。

补强项：

- 扩展了第 15 节举一反三问题，覆盖 iOS 未导入外部文件、iOS 文件夹递归扫描、Apple Music 条目、Desktop 文件夹监听、mock 冒充真实扫描等相邻需求。
- 明确 `userCancelled` 不是错误弹窗，避免 iOS 和 Desktop 文件选择取消时出现误报。
- 明确 Desktop 文件夹监听和 iOS 系统音乐资料库都不阻塞 P0。

### 16.2 第二轮：架构边界 Review

结论：

- 文档已明确 `commonMain` 只能定义扫描请求、状态、结果、元数据、来源类型和平台无关错误。
- Android `ContentResolver` / `MediaStore`、iOS Document Picker / MediaPlayer、Desktop 文件选择和 `Files.walk` 都被限制在平台 source set 或平台适配层。
- `ScanLocalMusicUseCase` 的演进关系已明确：它负责协调 UI 意图和曲库更新，不是真实平台 scanner。
- `localUri` 的来源已明确：由平台 scanner 或测试 fake scanner 产生，UI 不拼接。

补强项：

- 举一反三问题中新增了 “不能把平台 API 塞进 `MusicAppController`” 的边界检查。
- 新增了扫描结果与收藏、搜索、播放队列合并的提醒，避免真实扫描破坏现有用户状态。
- 强调 `sourceKind` 和稳定 id 是跨平台曲库合并、错误回写、播放调试的必要基础。

### 16.3 第三轮：实现与验证 Review

结论：

- 里程碑顺序合理：先共享扫描边界，再 Android MediaStore，再 iOS 文件导入，再 Desktop 文件夹扫描，最后评估 iOS 系统音乐资料库。
- 测试要求覆盖共享状态、Android 权限与扫描、iOS 文件导入、Desktop 文件夹扫描，以及扫描结果进入播放链路。
- 平台验证不能只靠单元测试；Android 权限、iOS Document Picker、Desktop 文件选择都需要真机、模拟器或桌面环境验证。

补强项：

- 增加了 Desktop 隐藏目录、系统目录、不可读文件和异常文件的处理提示。
- 增加了 Android 权限拒绝后不能继续用 seed/mock 冒充真实扫描的判断题。
- 增加了 `durationMs` 与展示字符串 `duration` 的区分，避免真实播放阶段缺少毫秒级时长。

## 17. 参考资料

- [Android Developers: Access media files from shared storage](https://developer.android.com/training/data-storage/shared/media)
- [Apple: File System Basics](https://developer.apple.com/library/archive/documentation/FileManagement/Conceptual/FileSystemProgrammingGuide/FileSystemOverview/FileSystemOverview.html)
- [Apple: Document Picker Programming Guide](https://developer.apple.com/library/archive/documentation/FileManagement/Conceptual/DocumentPickerProgrammingGuide/Introduction/Introduction.html)
- [Apple: Accessing Documents](https://developer.apple.com/library/archive/documentation/FileManagement/Conceptual/DocumentPickerProgrammingGuide/AccessingDocuments/AccessingDocuments.html)
- [Apple: MPMediaLibrary](https://developer.apple.com/documentation/mediaplayer/mpmedialibrary)
- [Apple: MPMediaQuery](https://developer.apple.com/documentation/mediaplayer/mpmediaquery)
- [Apple: UTType.audio](https://developer.apple.com/documentation/uniformtypeidentifiers/uttype/audio)
- [Oracle Java SE 17: Files](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/nio/file/Files.html)
- [Oracle Java SE 17: JFileChooser](https://docs.oracle.com/en/java/javase/17/docs/api/java.desktop/javax/swing/JFileChooser.html)
