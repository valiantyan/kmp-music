# 本地音频扫描结果首页展示与数据来源设计

## 背景

当前 App 首页、收藏页、我的页展示的是 seed/mock 曲库数据。接入真实本地音频扫描后，扫描结果已经是可播放音频，必须以真实曲库数据进入现有 App，而不能继续用 mock 歌曲冒充扫描结果。

本设计聚焦两个问题：

1. 扫描出的可播放音频列表数据，应该在现有 App 中在哪里显示、怎么显示。
2. Android、iOS、Desktop 的音频来源如何进入同一份本地曲库状态，再被首页、收藏、我的、搜索和播放队列消费。

## 已确认方案

采用 B 方案：在首页 `最近播放` 与 `本地专辑` 之间新增 `本地歌曲` 预览区。

首页结构调整为：

```text
本地音乐库卡片
最近播放
本地歌曲（新增，最多 6 条 + 查看全部）
本地专辑
```

`最近播放` 继续存在，语义保持为用户真实播放历史。`本地歌曲` 才显示扫描出来的可播放音频预览。

## 数据来源与平台边界

本设计继承 `docs/LOCAL_AUDIO_DISCOVERY_PRD.md` 的平台边界。UI 展示的真实歌曲必须来自平台 scanner / importer 产生的扫描结果，不能继续来自 seed/mock 数据。

| 平台 | P0 数据来源 | 首页主操作文案 | `sourceKind` | `localUri` 形态 | 关键约束 |
| --- | --- | --- | --- | --- | --- |
| Android | 用户授权后的 `MediaStore.Audio` 查询结果 | `授权并扫描` / `重新扫描` | `androidMediaStore` | `content://` URI | 不手写全盘遍历，不把 `MediaStore.Files` 结果默认当音乐。 |
| iOS | 用户通过 Files / Document Picker 显式选择并导入到 App 沙盒的音频 | `导入音频` / `继续导入` | `iosImportedFile` | App 沙盒内 file URL | 不写成“扫描整机音乐”，不承诺全设备扫描。 |
| Desktop | 用户选择的音乐文件夹递归扫描结果 | `选择音乐文件夹` / `重新扫描` | `desktopFolder` | file path / file URI | 不默认扫描全盘，不扫描系统目录。 |
| iOS P1 | 可选评估的系统音乐资料库 | 不进入 P0 首页主流程 | `iosMediaLibrary` | 仅限可本地播放 asset URL | Apple Music、云端、DRM、无 asset URL 条目不能进入可播放列表。 |

### 来源入口文案

首页 `本地音乐库` 卡片的主按钮按当前平台和状态显示：

- Android 未授权：`授权并扫描`
- Android 已授权或已有扫描结果：`重新扫描`
- iOS 未导入：`导入音频`
- iOS 已导入：`继续导入`
- Desktop 未选择文件夹：`选择音乐文件夹`
- Desktop 已选择文件夹：`重新扫描`

这些文案是 UI 与数据来源的契约。特别是 iOS P0 只能使用 `导入音频` 或 `从 Files 选择音频`，不能显示 `扫描本机音乐`。

### 可展示数据范围

首页 `本地歌曲` 只展示扫描结果中当前可播放的歌曲：

- `localUri` 存在且由平台 scanner / importer 产生。
- `sourceKind` 明确。
- 文件或媒体条目未被标记为 missing、unreadable、unsupported。
- 用户取消选择、权限拒绝、格式不支持等失败条目不进入歌曲列表，只进入扫描状态或来源问题。

部分失败不影响成功歌曲展示。例如 128 首可播放、2 个文件不可读时，首页仍展示 128 首中的前 6 条，并在卡片或二级来源页提示 `2 个文件读取失败`。

## 数据入库与合并规则

平台 scanner 不直接驱动 Composable。扫描结果先合并为本地曲库快照，再由页面读取。

### 稳定标识

每首扫描歌曲需要形成稳定 key：

```text
sourceKey = sourceKind + ":" + sourceId
```

`sourceId` 来源：

- Android：MediaStore `_ID`。
- iOS imported file：导入后沙盒文件规范路径 hash，并在导入记录中持久化。
- Desktop folder：规范化 file path hash。

App 内 `Song.id` 由 `sourceKey` 派生或通过映射表持久化，保证重新扫描后收藏、最近播放和播放队列可以继续匹配。

### 合并策略

扫描成功后按 `sourceKey` 合并：

- 新 sourceKey：新增歌曲。
- 已存在 sourceKey：更新 title、artist、album、durationMs、mimeType、sizeBytes、modifiedAt、coverArt、localUri 等元数据。
- 本轮缺失但来源仍可访问：标记为 `fileMissing` 或从可播放列表移除，并保留必要的历史引用。
- 不可读、格式不支持、metadataUnavailable：记录到扫描问题，不进入首页 `本地歌曲` 可播放列表。

### 用户状态保留

扫描刷新不能破坏用户状态：

- 收藏按 `Song.id` / `sourceKey` 保留。
- 最近播放按真实播放历史保留，但渲染时过滤当前不可播放歌曲；过滤后为空时显示最近播放空态。
- 播放队列按稳定 id 尝试保留；如果歌曲缺失，播放链路展示错误或跳过，不静默替换为 mock 歌曲。
- 当前播放歌曲如果仍可访问，继续保持全局红色文本和播放中标识。

### 聚合规则

专辑、歌手和统计值从当前可播放歌曲派生：

- 专辑：按 normalized album 聚合；album 缺失归入 `未知专辑`。
- 歌手：按 normalized artist 聚合；artist 缺失归入 `未知歌手`。
- 首页卡片歌曲数：当前可播放歌曲数量。
- 首页卡片专辑数 / 歌手数：聚合后的专辑和歌手数量。
- `本地专辑`：从聚合专辑中选择前 3 个展示，排序默认按专辑内最近 modifiedAt。

## 页面职责

### 首页

首页负责让用户扫描后立刻看到真实可播放内容，但不承载全量曲库管理。

首页展示：

- `本地音乐库` 卡片：真实歌曲、专辑、歌手数量；扫描入口；上次扫描状态。
- `最近播放`：真实播放历史，最多展示当前首页设计中的少量条目。
- `本地歌曲`：扫描结果预览，最多 6 条，右侧 `查看全部`。
- `本地专辑`：从真实扫描歌曲按专辑聚合生成，沿用现有横向卡片视觉。

### 本地音乐二级页

`查看全部` 进入二级 `本地音乐` 页。该页隐藏底部 Tab，保留迷你播放器，承载全量曲库浏览。

二级页分段：

- `歌曲`：全量扫描歌曲列表，默认入口。
- `专辑`：从歌曲按 album 聚合。
- `歌手`：从歌曲按 artist 聚合。
- `来源`：展示 Android MediaStore、iOS 导入文件、Desktop 文件夹等来源状态，以及扫描问题。

### 收藏页

收藏页不显示全部扫描结果。它只显示用户主动收藏过的扫描歌曲、专辑和歌手。

扫描刷新时应通过稳定歌曲 id 保留收藏状态；文件缺失或不可播放时标记不可用，不自动删除用户收藏。

### 我的页

我的页展示真实统计和来源管理入口。

展示规则：

- `本地专辑` 数量来自真实曲库聚合。
- `歌手` 数量来自真实曲库聚合。
- `收藏` 数量来自用户收藏集合。
- 增加或复用 `本地来源 / 扫描问题` 入口，进入本地音乐二级页的来源分段。

### 搜索页

搜索页搜索真实曲库，不再搜索 seed/mock 数据。

搜索范围仍为：

- 歌曲
- 专辑
- 歌手

## UI 区块与数据来源映射

| UI 区块 | 数据来源 | 派生规则 | 不允许 |
| --- | --- | --- | --- |
| 首页 `本地音乐库` 卡片 | `LibrarySnapshot.stats` + `LocalMusicScanState` | 展示可播放歌曲数、专辑数、歌手数、当前平台入口文案、上次扫描状态。 | 不写死 `1,248 / 86 / 128`。 |
| 首页 `最近播放` | `PlaybackHistory` + 当前曲库快照 | 显示真实播放过且当前仍可播放的歌曲，最多 2 条。 | 不用扫描列表冒充最近播放。 |
| 首页 `本地歌曲` | 当前曲库快照的 `playableSongs` | 按 `modifiedAt desc` 取前 6 条；无 `modifiedAt` 时保持 scanner 返回顺序。 | 不展示 unreadable / unsupported / missing 条目。 |
| 首页 `本地专辑` | 当前曲库快照的 `albums` 聚合 | 从可播放歌曲按专辑聚合，展示前 3 个。 | 不继续使用 seed 专辑数量和封面冒充真实专辑。 |
| 收藏页 | `FavoritesRepository` + 当前曲库快照 | 只展示用户收藏且仍可匹配的歌曲、专辑、歌手。 | 不把全部扫描结果自动加入收藏。 |
| 我的页统计 | 当前曲库快照 + 收藏集合 | 专辑数、歌手数来自曲库；收藏数来自收藏集合。 | 不写死统计值。 |
| 搜索页 | 当前曲库快照 | 搜索歌曲、专辑、歌手的真实聚合结果。 | 不搜索 seed/mock 数据。 |
| 播放队列 | `PlaybackRepository` + 当前曲库快照 | 队列 id 映射为当前仍可播放的歌曲；失败时进入播放错误模型。 | 不用其他歌曲静默替换缺失歌曲。 |

## 首页本地歌曲区

### 位置

`本地歌曲` 放在首页 `最近播放` 之后、`本地专辑` 之前，也就是当前截图红框位置。

### 标题

左侧标题：`本地歌曲`

右侧操作：`查看全部 ›`

点击 `查看全部` 进入二级 `本地音乐` 页，并默认打开 `歌曲` 分段。

### 列表数量

首页最多显示 6 条扫描歌曲。

不足 6 条时显示全部可播放歌曲。

超过 6 条时只显示前 6 条，不在首页内继续分页或无限滚动。

### 默认排序

首版默认按 `modifiedAt desc` 排序；当平台无法提供 `modifiedAt` 时，保持 scanner 返回顺序。

排序规则要集中在曲库 UseCase 或 Repository 层，不在 Composable 中临时排序。

### 单行内容

每行展示：

- 封面：优先真实封面；无封面时使用 App 默认封面或基于专辑/来源的默认视觉。
- 标题：优先音频元数据 title；缺失时使用文件名去扩展名。
- 副标题：`歌手 · 专辑 · 时长`。
- 播放按钮：点击后直接播放该歌曲，并同步全局迷你播放器。
- 更多按钮：打开现有歌曲更多操作弹层。

### 元数据兜底

当元数据缺失时：

- title 缺失：使用文件名。
- artist 缺失：显示 `未知歌手`。
- album 缺失：显示 `未知专辑`。
- duration 缺失：显示 `--:--`。
- coverArt 缺失：显示默认本地音频封面。

## 最近播放规则

`最近播放` 不应被扫描歌曲自动填充。

规则：

- 有真实播放历史：显示最近播放歌曲。
- 无播放历史但已有扫描歌曲：显示最近播放空态 `播放过的歌曲会出现在这里`。
- 不用扫描结果冒充最近播放。

如果首页首屏内容过长，`最近播放` 保持现有最多 2 条的轻量展示。

## 空态与异常态

首页状态由 `LocalMusicScanState`、来源状态和曲库快照共同决定。

| 状态 | 首页卡片 | `最近播放` | `本地歌曲` | `本地专辑` |
| --- | --- | --- | --- | --- |
| `idle` 且无来源 | 显示平台入口：Android 授权、iOS 导入、Desktop 选择文件夹。 | 有历史则显示仍可播放历史；否则显示最近播放空态。 | 不显示。 | 不显示。 |
| `waitingForPermission` | 显示等待授权 / 等待选择来源。 | 保留上一轮可播放历史。 | 保留上一轮成功结果；无结果时不显示。 | 保留上一轮聚合；无结果时不显示。 |
| `importing` | iOS 显示导入中。 | 保留上一轮可播放历史。 | 保留上一轮成功结果；无结果时显示导入中占位。 | 保留上一轮聚合。 |
| `scanning` | 显示扫描中和进度摘要。 | 保留上一轮可播放历史。 | 保留上一轮成功结果；无结果时显示扫描中占位。 | 保留上一轮聚合。 |
| `done` 且有歌曲 | 展示真实统计和上次扫描摘要。 | 展示真实播放历史。 | 展示最多 6 条可播放歌曲。 | 展示真实聚合专辑。 |
| `done` 且无歌曲 | 展示 `0 首歌曲` 和恢复入口。 | 显示最近播放空态。 | 显示 `未找到支持的音频文件`。 | 不显示。 |
| `error` 且有旧结果 | 展示错误摘要和恢复入口。 | 展示仍可播放历史。 | 展示上一轮可播放歌曲，并提示结果可能不是最新。 | 展示上一轮聚合。 |
| `error` 且无结果 | 展示错误摘要和恢复入口。 | 显示最近播放空态。 | 不显示。 | 不显示。 |

### 未扫描

首页本地音乐库卡片显示扫描入口。

未扫描时不显示 `本地歌曲` 区，避免首页在没有真实列表数据时出现空占位。

### 扫描中

本地音乐库卡片显示扫描中状态。

`本地歌曲` 区保留上一轮成功结果；如果没有上一轮结果，显示扫描中占位。

### 扫描完成且有歌曲

首页显示真实歌曲数量，并渲染 `本地歌曲` 最多 6 条。

### 扫描完成但无歌曲

首页卡片显示 `0 首歌曲`。

`本地歌曲` 区显示空态：`未找到支持的音频文件`，并提供重新扫描或选择来源入口。

### 部分失败

可播放歌曲仍正常显示。

首页卡片或二级页来源分段显示问题提示，例如 `2 个文件读取失败`。不要用失败条目阻塞可播放歌曲展示。

### 权限拒绝或来源不可用

首页卡片显示明确状态和恢复入口：

- Android：`需要音频权限`
- iOS：`从 Files 导入音频`
- Desktop：`选择音乐文件夹`

文案必须遵守 `LOCAL_AUDIO_DISCOVERY_PRD.md`：iOS 不使用 `扫描整机音乐` 这类描述。

## 数据流

扫描结果不直接写进 Composable。推荐数据流：

```text
LocalMusicScanner
  -> LocalMusicScanResult(added / updated / removed / failed)
  -> ScanLocalMusicUseCase
  -> LibrarySync / Merge Policy
  -> MusicLibraryRepository
  -> LibrarySnapshot
  -> MusicAppController / MusicAppUiState
  -> HomeScreen / FavoritesScreen / MeScreen / SearchScreen
```

### common 层职责

common 层定义平台无关数据和状态：

- `LocalMusicScanner` 接口。
- `LocalMusicScanRequest`：扫描全部、重新扫描、扫描指定来源。
- `LocalMusicScanState`：`idle`、`waitingForPermission`、`importing`、`scanning`、`done`、`error`。
- `LocalMusicScanProgress`：已处理数量、已发现数量、当前来源。
- `LocalMusicScanResult`：新增、更新、删除、失败条目。
- `MusicFileMetadata`：平台无关音频元数据。
- `LocalMusicSourceKind`：`androidMediaStore`、`iosImportedFile`、`iosMediaLibrary`、`desktopFolder`。
- `LocalMusicScanError`：`permissionDenied`、`userCancelled`、`folderUnavailable`、`fileMissing`、`fileUnreadable`、`unsupportedFormat`、`metadataUnavailable`、`securityScopeExpired`、`unknown`。

common 层禁止包含 Android `ContentResolver`、iOS `UIDocumentPickerViewController`、Desktop `Files.walk` 等平台 API。

### 平台层职责

平台层只负责发现音频和读取元数据：

- Android：`AndroidMediaStoreScanner` 查询 `MediaStore.Audio`，输出 `content://` URI。
- iOS：`IosDocumentMusicImporter` 打开 Files / Document Picker，导入 App 沙盒后输出 file URL。
- Desktop：`DesktopFolderMusicScanner` 基于用户选择文件夹递归扫描，输出 file path / URI。

平台层不决定首页展示数量、收藏规则、最近播放规则，也不直接更新 Composable。

### Repository 快照

`MusicLibraryRepository` 对 UI 暴露稳定快照：

```text
LibrarySnapshot
├── songs: List<Song>
├── albums: List<Album>
├── artists: List<Artist>
├── stats: LibraryStats
├── sources: List<LocalMusicSourceSummary>
├── scanState: LocalMusicScanState
├── lastScanSummary: LocalMusicLastScanSummary?
└── problems: List<LocalMusicProblem>
```

首页 `本地歌曲` 使用 `LibrarySnapshot.songs` 的派生预览列表。

二级 `本地音乐` 页使用同一份 `uiState.songs / albums / artists`，不维护另一套列表数据。

`MusicAppUiState` 可以继续保留 `songs / albums / artists` 以适配现有页面，但这些字段必须来自 `LibrarySnapshot`，而不是 `SeedMusicLibraryRepository`。

## 数据模型要求

扫描歌曲至少需要支持：

- 稳定 `id`
- `sourceId`
- `sourceKind`
- `title`
- `artist`
- `album`
- `durationMs`
- 展示用 `duration`
- `localUri`
- `mimeType`
- `sizeBytes`
- `modifiedAt`
- `coverArt`

`localUri` 必须由平台 scanner 或 fake scanner 生成，UI 不能拼接。

### 展示模型与扫描模型

扫描模型和展示模型可以分层，避免让 UI 背负平台字段。

```text
MusicFileMetadata
  -> LibraryTrack
  -> Song / Album / Artist
```

- `MusicFileMetadata`：scanner 输出，保留 sourceId、sourceKind、localUri、mimeType、sizeBytes、modifiedAt 等来源字段。
- `LibraryTrack`：曲库内部实体，负责稳定 id、可播放状态、扫描问题、用户状态关联。
- `Song`：页面展示和播放入口使用的模型，可由 `LibraryTrack` 映射而来。

如果直接扩展现有 `Song`，必须保持字段不可变，并补充 `durationMs`、`localUri`、`sourceKind` 等真实数据字段。UI 仍只读取展示所需字段。

## 交互规则

- 点击首页 `本地歌曲` 行主体：打开播放页并播放，沿用现有 `SongRow` 的 `onOpen` 语义。
- 点击播放按钮：留在当前页直接播放。
- 点击更多：打开歌曲更多操作弹层。
- 点击 `查看全部`：进入二级 `本地音乐` 页的歌曲分段。
- 点击本地音乐库卡片内的扫描按钮：触发扫描或重新扫描。
- 点击首页 `本地歌曲` 的 `查看全部`：进入二级 `本地音乐` 页，这是全量列表的唯一入口。

## 架构边界

- Composable 不直接调用 scanner。
- `commonMain` 不引入 Android、iOS、Desktop 平台权限或文件 API。
- 首页不使用 seed/mock 歌曲冒充真实扫描结果。
- 收藏、播放队列、搜索都必须消费同一份曲库状态。
- 平台差异由 scanner / UseCase / Repository 层处理，页面只根据平台无关状态渲染。

## 实现影响

需要调整或新增：

- `LocalMusicScanner`：建立平台无关接口和 fake scanner，承接 Android/iOS/Desktop 真实实现。
- `LocalMusicScanRequest / LocalMusicScanState / LocalMusicScanResult / LocalMusicScanError`：定义扫描请求、状态、结果和错误模型。
- `MusicFileMetadata / LibraryTrack`：承载真实来源字段、稳定 id、localUri 和可播放状态。
- `MusicLibraryRepository`：从 seed 实现演进为可接收扫描结果并输出 `LibrarySnapshot` 的曲库仓库。
- `MusicAppUiState`：增加曲库统计、扫描状态、来源状态、最近扫描时间、扫描问题或对应派生字段。
- `HomeScreen`：新增 `本地歌曲` section，最多展示 6 条。
- 新增 `SecondaryScreen.LocalMusic` 路由：承载全量歌曲/专辑/歌手/来源分段。
- `MusicAppController`：提供 `openLocalMusic(section = LocalMusicSection.Songs)` 导航动作。
- 平台 source set：分别实现 `AndroidMediaStoreScanner`、`IosDocumentMusicImporter`、`DesktopFolderMusicScanner`。
- 测试：覆盖扫描结果合并、首页本地歌曲预览数量、查看全部导航、最近播放不被扫描结果污染、空态和部分失败状态。

## 分阶段落地建议

### 阶段 1：共享数据边界与 fake scanner

- 在 common 层定义扫描接口、状态、错误、来源和元数据模型。
- 建立 fake scanner，输出带 `sourceKind`、`sourceId`、`localUri`、`modifiedAt` 的可播放测试数据。
- `MusicLibraryRepository` 支持接收扫描结果并生成 `LibrarySnapshot`。
- 首页 `本地歌曲` 先消费 fake scanner 结果，验证 UI 和状态流。

### 阶段 2：首页与二级本地音乐页

- 首页新增 `本地歌曲`，最多 6 条。
- `查看全部` 进入 `SecondaryScreen.LocalMusic` 的歌曲分段。
- 二级页提供歌曲、专辑、歌手、来源分段。
- 搜索、收藏、我的页统计改为消费同一份 `LibrarySnapshot`。

### 阶段 3：平台真实来源

- Android 接 `MediaStore.Audio`，生成 `androidMediaStore` 数据。
- iOS 接 Document Picker 导入沙盒，生成 `iosImportedFile` 数据。
- Desktop 接用户选择文件夹扫描，生成 `desktopFolder` 数据。
- 每个平台都必须把权限拒绝、用户取消、空结果、部分失败映射到统一状态。

### 阶段 4：播放链路衔接

- 播放 UseCase 只消费 scanner 生成的 `localUri` / `PlayableMedia`。
- 播放失败时回写曲库问题状态，例如文件缺失、权限失效、格式不支持。
- 当前播放歌曲在首页 `本地歌曲`、搜索、收藏、专辑页等列表中继续同步红色播放态。

## 验收标准

- 扫描成功后，首页红框位置出现 `本地歌曲` 区。
- `本地歌曲` 最多展示 6 条真实扫描歌曲。
- `查看全部` 能进入全量本地音乐列表。
- `最近播放` 仍然存在，且只显示真实播放历史。
- 首页专辑和我的页统计来自真实曲库聚合。
- 搜索结果来自真实曲库。
- 元数据缺失时有稳定兜底，不出现空白标题或崩溃。
- 权限拒绝、空结果、部分失败都有明确 UI 状态。
- Android 来源使用 `androidMediaStore`，歌曲 `localUri` 为平台 scanner 生成的 `content://` URI。
- iOS P0 来源使用 `iosImportedFile`，入口文案为 `导入音频` 或 `从 Files 选择音频`，不出现 `扫描整机音乐`。
- Desktop 来源使用 `desktopFolder`，入口文案为 `选择音乐文件夹`，不默认扫描全盘。
- `localUri` 不由 UI 拼接。
- 重新扫描后通过稳定 `sourceKey` 保留收藏、最近播放和播放队列引用。
- seed/mock 数据只允许作为 fake scanner 或测试数据存在，不能冒充真实扫描结果。

## 举一反三问题速查

本节用于验证：遇到相邻产品、数据和实现问题时，是否能从本文档找到处理方案。

| 问题 | 是否能找到方案 | 文档答案 | 依据 |
| --- | --- | --- | --- |
| 扫描出来的歌曲是否可以直接显示在首页一级页面？ | 能 | 可以，但只在首页 `本地歌曲` 区展示最多 6 条预览，全量进入二级 `本地音乐` 页。 | 已确认方案、首页本地歌曲区 |
| 首页 `最近播放` 是否会被新增的 `本地歌曲` 替换？ | 能 | 不会。`最近播放` 保留真实播放历史；`本地歌曲` 才展示扫描结果预览。 | 已确认方案、最近播放规则 |
| 如果扫描完成但用户从未播放过歌曲，`最近播放` 怎么显示？ | 能 | 显示最近播放空态 `播放过的歌曲会出现在这里`，不拿扫描结果冒充最近播放。 | 最近播放规则、空态与异常态 |
| 首页最多展示多少首扫描歌曲？ | 能 | 最多 6 条；不足 6 条显示全部，超过 6 条只显示前 6 条。 | 首页本地歌曲区 |
| 首页 `本地歌曲` 默认按什么排序？ | 能 | 默认按 `modifiedAt desc`；平台没有 `modifiedAt` 时保持 scanner 返回顺序。 | 首页本地歌曲区 |
| `查看全部` 进入哪里？ | 能 | 进入二级 `本地音乐` 页，并默认打开 `歌曲` 分段。 | 页面职责、交互规则 |
| 二级 `本地音乐` 页有哪些分段？ | 能 | `歌曲 / 专辑 / 歌手 / 来源`。 | 页面职责 |
| 首页 `本地专辑` 还用原来的 mock 专辑吗？ | 能 | 不用。它从当前可播放歌曲按 album 聚合生成。 | 页面职责、UI 区块与数据来源映射、聚合规则 |
| 我的页 `本地专辑 / 歌手 / 收藏` 统计从哪里来？ | 能 | 专辑数、歌手数来自当前曲库聚合；收藏数来自收藏集合。 | 我的页、UI 区块与数据来源映射 |
| 收藏页是否显示全部扫描歌曲？ | 能 | 不显示。收藏页只显示用户主动收藏过的歌曲、专辑、歌手。 | 收藏页、UI 区块与数据来源映射 |
| 搜索页搜什么数据？ | 能 | 搜索真实曲库的歌曲、专辑、歌手，不再搜索 seed/mock 数据。 | 搜索页、UI 区块与数据来源映射 |
| Android 的歌曲从哪里来？ | 能 | 来自用户授权后的 `MediaStore.Audio` 查询结果，`sourceKind = androidMediaStore`，`localUri` 为 scanner 生成的 `content://` URI。 | 数据来源与平台边界、验收标准 |
| iOS 能否像 Android 一样扫描整台设备音乐？ | 能 | 不能。iOS P0 是用户通过 Files / Document Picker 显式选择并导入 App 沙盒。 | 数据来源与平台边界 |
| iOS 首页按钮能否写 `扫描本机音乐`？ | 能 | 不能。iOS P0 使用 `导入音频` 或 `从 Files 选择音频`。 | 来源入口文案、验收标准 |
| Desktop 是否可以默认扫描全盘？ | 能 | 不能。Desktop P0 必须由用户选择音乐文件夹。 | 数据来源与平台边界、来源入口文案 |
| `localUri` 可以由 UI 拼接吗？ | 能 | 不可以。`localUri` 必须由平台 scanner 或 fake scanner 生成。 | 数据模型要求、验收标准 |
| 扫描结果中不可读或不支持格式的文件会显示在首页吗？ | 能 | 不会。失败条目进入扫描问题，不进入 `本地歌曲` 可播放列表。 | 可展示数据范围、合并策略 |
| 扫描部分失败时是否阻塞成功歌曲显示？ | 能 | 不阻塞。成功歌曲正常展示，失败数量在卡片或二级来源页提示。 | 可展示数据范围、空态与异常态 |
| 重新扫描后收藏会丢吗？ | 能 | 不应丢。收藏按 `Song.id` / `sourceKey` 保留。 | 用户状态保留、验收标准 |
| 重新扫描后播放队列怎么处理？ | 能 | 队列按稳定 id 尝试保留；缺失歌曲进入播放错误或跳过，不静默替换为 mock 歌曲。 | 用户状态保留、UI 区块与数据来源映射 |
| 歌曲缺少 title 怎么显示？ | 能 | 使用文件名去扩展名。 | 元数据兜底 |
| 歌曲缺少 artist 或 album 怎么显示？ | 能 | artist 显示 `未知歌手`，album 显示 `未知专辑`。 | 元数据兜底、聚合规则 |
| 歌曲缺少 duration 怎么显示？ | 能 | 显示 `--:--`。 | 元数据兜底 |
| 当前正在播放歌曲在新增 `本地歌曲` 列表中如何显示？ | 能 | 如果仍可访问，继续保持全局红色文本和播放中标识。 | 用户状态保留、分阶段落地建议 |
| seed/mock 数据还能存在吗？ | 能 | 只能作为 fake scanner 或测试数据存在，不能冒充真实扫描结果。 | 数据来源与平台边界、验收标准 |
| 平台 scanner 能否直接更新 Composable？ | 能 | 不能。scanner 结果必须先合并为 `LibrarySnapshot`，再由 UI 状态读取。 | 数据流、架构边界 |
| 首页卡片里的歌曲、专辑、歌手数量能否继续写死？ | 能 | 不能。必须来自 `LibrarySnapshot.stats` 和真实曲库聚合。 | UI 区块与数据来源映射 |

## 三轮交叉 Review 结果

### 第一轮：产品与 UI/UX Review

结论：

- 首页结构清晰：`本地音乐库卡片 -> 最近播放 -> 本地歌曲 -> 本地专辑`。
- `最近播放` 与 `本地歌曲` 的语义已分离，避免把扫描结果误当播放历史。
- 一级页只展示最多 6 条扫描歌曲，全量列表进入二级 `本地音乐` 页，符合当前底部 Tab 信息架构。
- 空态、扫描中、完成、部分失败、错误状态均有首页展示策略。

本轮发现并已补强：

- 原先 `最近播放` 空态存在“显示或隐藏”的摇摆表达，已收敛为显示最近播放空态 `播放过的歌曲会出现在这里`。
- 首页 `本地歌曲` 的新增位置已明确为 `最近播放` 之后、`本地专辑` 之前。

剩余实现注意：

- 首页加入最多 6 条歌曲后，需在真实设备上检查与迷你播放器、底部 Tab 的遮挡关系。
- 若列表内容过长，应优先保持 `最近播放` 2 条和 `本地歌曲` 最多 6 条，不在首页做分页或无限滚动。

### 第二轮：数据来源与架构 Review

结论：

- Android、iOS、Desktop 的 P0 来源边界已明确，且都映射到统一的 `sourceKind`。
- `localUri` 由平台 scanner / importer 生成，UI 不负责拼接。
- 数据进入 UI 前必须经过 `LocalMusicScanner -> ScanLocalMusicUseCase -> MusicLibraryRepository -> LibrarySnapshot`。
- `sourceKey = sourceKind + ":" + sourceId` 作为扫描合并和用户状态保留的核心键。
- 收藏、最近播放、播放队列、搜索共用同一份曲库快照，不维护多套列表。

本轮发现并已补强：

- Android `sourceId` 已收敛为 MediaStore `_ID`。
- iOS `sourceId` 已收敛为导入后沙盒文件规范路径 hash，并要求持久化到导入记录。
- Desktop `sourceId` 已收敛为规范化 file path hash。

剩余实现注意：

- 后续如要支持内容指纹去重，应作为独立增强，不改变 P0 的 `sourceKey` 规则。
- `LibrarySnapshot` 应成为 UI 的唯一曲库读取入口，避免 `SeedMusicLibraryRepository` 与真实曲库并存导致页面数据漂移。

### 第三轮：实现、测试与风险 Review

结论：

- 文档已覆盖从 fake scanner 到真实平台来源的分阶段落地路径。
- 测试重点明确：扫描结果合并、首页 6 条预览、查看全部导航、最近播放不被污染、空态和部分失败。
- 验收标准同时覆盖 UI 展示、数据来源、平台文案、`localUri` 生成、用户状态保留和禁止 mock 冒充真实数据。

本轮发现并已补强：

- 失败条目不进入 `本地歌曲` 可播放列表，只进入扫描问题。
- 部分失败不阻塞成功歌曲展示。
- 重新扫描后收藏、最近播放、播放队列都要通过稳定 id 尝试保留。

剩余实现注意：

- 改动 `MusicAppController`、导航、曲库、搜索、收藏或播放队列时，必须补共享测试。
- Android/iOS/Desktop 真实平台能力无法在同一轮全部验证时，最终说明必须标明未覆盖的平台风险。
- 播放链路只消费已由 scanner 生成且当前仍可访问的 `localUri` / `PlayableMedia`；播放失败再回写曲库问题状态。

## 实施状态

- 第一阶段已覆盖：common 扫描模型、fake scanner、曲库快照合并、首页 `本地歌曲` 预览、二级 `本地音乐` 页、搜索/收藏/我的页共用快照、最近播放与扫描结果分离。
- 第一阶段同时清理旧 seed/mock 泄漏：默认 App 不再依赖 seed 曲库，设置页重新扫描与首页扫描共用真实快照扫描路径，旧扫描弹层状态已移除。
- 尚需独立计划推进：Android MediaStore scanner、iOS Files 导入、Desktop 文件夹 scanner、真实播放器读取 scanner 生成的 `localUri`。
- 当前 fake scanner 使用 `sourceKind = fakeScanner`，用于验证 UI 与数据流，不代表任何真实平台来源。
