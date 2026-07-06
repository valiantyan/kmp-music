Status: ready-for-agent

# 本地音频发现来源覆盖与累加模型 PRD

## Problem Statement

用户希望 KMP Music 的本地音频发现符合各平台真实授权模型：Android 是单一系统媒体库来源，Desktop/macOS 是多个用户添加的扫描目录，iOS P0 是多个用户导入或授权的音频文件来源。当前需求已经澄清，但实现风险在于扫描合并逻辑如果只按 `sourceKind` 处理，会把“扫描某个 Desktop 文件夹”或“导入一批 iOS 文件”误解为“完整扫描整个 Desktop/iOS 来源类型”，从而错误下线其它未处理来源中的歌曲。

从用户视角看，最严重的问题是：用户添加新文件夹或导入新文件时，不应该丢失旧文件夹或旧导入文件里的歌曲；用户取消或遇到失败扫描时，也不应该因为本次没有处理到某些旧歌曲，就把它们从本地曲库中移除。

## Solution

实现本地音频发现的来源覆盖语义：扫描结果必须区分“完整覆盖某个来源，可以用缺失结果下线旧歌曲”和“部分扫描或导入，只能写入已经发现或验证的正向结果”。Android 完整扫描覆盖整个 Android 系统媒体库来源；Desktop/macOS 扫描目录按具体目录累加；iOS P0 多文件来源也按累加模型处理，添加新文件不替换旧文件。iOS P0 的“导入”在本 PRD 中表示用户显式选择并授权 App 记录音频来源，不表示复制文件来让歌曲脱离原始来源长期保活。

扫描页与曲库展示应保持用户可理解的行为：页面统计只展示当前可播放歌曲总数和最近扫描时间，不展示新增、更新、移除计数；扫描或导入完成后停留在当前页面；扫描运行期间只允许一个扫描任务，并支持无确认取消。

## User Stories

1. As an Android listener, I want a scan to read the Android 系统媒体库来源, so that my local music appears without managing folders manually.
2. As an Android listener, I want a complete Android rescan to remove songs that disappeared from MediaStore, so that my local library reflects the current device media library.
3. As an Android listener, I want the app to keep using one Android media source, so that I am not asked to manage confusing Android folder sources.
4. As a Desktop listener, I want to add a scan directory, so that the app discovers songs in a folder I explicitly chose.
5. As a Desktop listener, I want adding folder B to keep songs from folder A, so that expanding my library does not delete existing music.
6. As a Desktop listener, I want rescanning folder B to only reconcile folder B, so that unavailable songs in folder A are not incorrectly removed.
7. As a Desktop listener, I want a folder source to be removed only through explicit management or a complete scan of that folder proving the files are gone, so that the app does not make destructive assumptions.
8. As an iOS listener, I want to import or authorize audio files, so that the app can build an iOS 导入曲库 without pretending to scan the whole phone.
9. As an iOS listener, I want importing new audio files to accumulate with existing imported files, so that adding music does not replace my previous imports.
10. As an iOS listener, I want file-source availability to determine playback eligibility, so that missing or inaccessible files are removed only after proper reconciliation.
11. As an iOS listener, I want the app to restore file access while scanning and playing, so that previously authorized files remain usable when the platform permits it.
12. As a mobile listener, I want scan copy to match my platform, so that Android says “开始扫描/重新扫描”, Desktop says “添加文件夹/重新扫描”, and iOS says “导入音频/扫描曲库/重新扫描”.
13. As a listener on any platform, I want source labels to match the platform model, so that Android shows “Android 媒体库”, Desktop shows “扫描目录”, and iOS shows “已添加音频” or “音频来源”.
14. As a listener on any platform, I want scan page statistics to show current playable song total and last scan time, so that I see stable, useful status instead of noisy implementation counts.
15. As a listener on any platform, I want scan/import completion to keep me on the scan page, so that I can inspect the result without being unexpectedly navigated away.
16. As a listener on any platform, I want only one scan task at a time, so that duplicate scans do not race or corrupt the library.
17. As a listener on any platform, I want the main scan button to become “取消扫描” while scanning, so that I can stop long-running scans directly.
18. As a listener on any platform, I want cancel scan to require no confirmation, so that stopping a scan is quick.
19. As a listener on any platform, I want a cancelled scan to keep already verified additions or updates, so that useful progress is not thrown away.
20. As a listener on any platform, I want a cancelled scan to preserve old songs that were not processed, so that cancellation never acts like deletion.
21. As a listener on any platform, I want failed scans to keep old unprocessed songs, so that transient errors do not shrink my library.
22. As a listener on any platform, I want a cancelled scan to update last scan time and show “已取消”, so that I understand what happened.
23. As a listener on any platform, I want a cancelled scan subtitle to explain that the current library was kept and partial results can be completed later, so that I know the library is safe.
24. As a listener on any platform, I want a failed scan to show “扫描失败” without excessive explanation unless there is an actionable reason, so that the UI stays calm.
25. As a listener with existing favorites, I want favorites to survive scan reconciliation, so that source refreshes do not erase my preference data.
26. As a listener with a playback queue, I want unavailable songs to be removed only when the source evidence is complete, so that playback recovery does not drop songs due to partial scans.
27. As a developer, I want source coverage to be explicit in the scan result contract, so that repository merge logic can make safe removal decisions.
28. As a developer, I want partial scan results to be represented without deletion authority, so that import flows and cancelled scans share safe semantics.
29. As a developer, I want Android, Desktop, iOS, and fake scanners to declare their coverage consistently, so that tests can assert platform behavior without relying on UI details.
30. As a developer, I want merge tests to cover multi-source accumulation, so that future refactors cannot reintroduce source-kind-wide deletion bugs.
31. As a developer, I want existing architecture boundaries preserved, so that platform APIs stay out of common UI/domain code.
32. As a developer, I want the implementation to avoid modifying the high-fidelity prototype for production behavior, so that the real KMP app remains the source of truth.

## Implementation Decisions

- Use “本地音频发现” as the cross-platform product term. It means discovering user-authorized playable local audio, not raw full filesystem scanning.
- Android uses one Android 系统媒体库来源. A complete Android scan covers the full Android MediaStore audio library.
- Desktop/macOS uses multiple Desktop 扫描目录. Scan directories accumulate; adding or scanning one directory does not replace or reconcile unrelated directories.
- iOS P0 uses iOS 导入曲库 as an accumulated set of user-selected file sources. Adding new iOS audio files does not replace old imported files.
- iOS P1 system music library remains deferred and must not be mixed into the P0 imported-file source model.
- Source availability governs playback. A song remains playable only while its original source is present and accessible after the relevant scan reconciliation.
- The scan result contract needs an explicit way to express completed coverage. A complete coverage range may remove missing old songs; a partial/import/cancelled/failed result may only add or update positive evidence.
- Coverage semantics must be explicit enough to represent at least three cases: complete source-kind coverage for Android MediaStore, complete concrete-source coverage for one Desktop scan directory or one managed iOS source validation, and positive-only partial/import results with no deletion authority.
- Existing `removedSourceKeys` style explicit removals may remain useful for concrete source removal, but broad deletion must not be inferred from `sourceKind` alone for Desktop or iOS.
- Persistent library merge logic must stop treating “all discovered items of one `sourceKind`” as equivalent to “that whole source kind was fully scanned”.
- Android complete scan can continue to reconcile all old Android MediaStore songs when the scanner successfully completes.
- Desktop folder scan should reconcile only the concrete folder source that was fully scanned. Folder B cannot mark folder A songs unavailable.
- iOS file import should be treated as accumulation by default. New imports should update existing matching file sources or add new ones, without removing missing old imports unless an explicit management action or complete source validation covers them.
- Desktop and iOS source summaries or equivalent source records must carry a stable concrete source identity. Display names alone are not sufficient for merge or removal decisions.
- iOS P0 must store a platform-resolvable source reference, such as a security-scoped bookmark-backed reference, in platform-specific persistence. Common `localUri` must not assume a raw `file://` URL is sufficient for future playback.
- Cancelled scans are app-level task outcomes. They can keep already verified writes, but cannot remove old unprocessed songs.
- Failed scans follow the same safe deletion rule as cancelled scans: do not remove old unprocessed songs.
- Cancelled scan must be distinguishable from successful done and failed error states in the UI-facing scan state, either as a first-class state or as an explicit outcome reason that renders “已取消”.
- Exiting the app while scanning is equivalent to cancel scan.
- Scan/import completion does not auto-navigate away from the scan page.
- Scan page statistics show only current playable song total and last scan time. Added, updated, and removed counts may still exist internally or in tests, but they are not page UI requirements.
- Platform-specific copy must be trimmed from the Figma reference. Android-only filtering copy must not be reused on Desktop or iOS.
- Keep platform APIs in platform source sets. Common code may define platform-neutral scan models, source coverage semantics, and repository/use case contracts, but must not import Android, iOS, or Desktop file APIs.
- Do not modify the visual prototype to solve production app behavior unless explicitly asked.
- The implementation should prefer existing seams: platform `LocalMusicScanner` implementations, `ScanLocalMusicUseCase`, `MusicLibraryRepository.applyScanResult`, persistent repository tests, and controller scan task behavior.

## Acceptance Criteria

- Android successful refresh is treated as complete Android 系统媒体库来源 coverage and can mark previously available Android MediaStore songs unavailable when they are absent from the completed scan.
- Desktop scanning folder B never marks folder A songs unavailable. Only a completed scan covering folder A can reconcile folder A missing songs.
- iOS adding new audio files never replaces existing iOS 导入曲库 files. Old iOS file sources remain available unless explicitly removed or covered by a complete validation that proves they are missing or inaccessible.
- A scan result with no completed coverage cannot remove existing songs, even if it discovered songs of the same `sourceKind`.
- Cancelled and failed scans can preserve positive writes but cannot remove old unprocessed songs.
- Scan page UI shows current playable song total and last scan time, not added/updated/removed counts.
- Cancelled scan renders “已取消” and communicates that the current library was kept.
- Failed scan renders “扫描失败” without implying old songs were deleted.
- Platform scanner code stays in platform source sets; common code contains only platform-neutral contracts and merge rules.
- Persistent repository tests cover Android source-kind coverage, Desktop concrete-source accumulation, iOS additive imports, positive-only partial scans, cancelled/failed scan preservation, and favorites preservation.

## Testing Decisions

- Good tests should assert user-visible behavior and domain contracts, not private implementation details. The core behavior is whether songs remain available or become unavailable after scan reconciliation.
- Primary testing seam: the music library repository scan merge boundary. Tests should set up existing songs from multiple source models, apply scan results, and assert available songs, removed counts, source summaries, and favorites preservation.
- Secondary testing seam: the scan result/domain model contract. Tests should prove that source coverage is explicit and that empty or partial results do not imply deletion authority.
- Platform scanner tests or fakes should verify coverage semantics at the boundary: Android returns full Android MediaStore coverage on success; Desktop returns concrete-folder coverage; iOS import returns additive file-source evidence rather than source-kind-wide replacement.
- Controller-level tests should cover scan task behavior if implementation changes the app-level scan lifecycle: one running scan at a time, cancel scan changes state to cancelled, and cancelled/failed scans keep unprocessed existing songs.
- Prior art exists in persistent music library tests for preview ordering, source-aware unavailability, favorites preservation, source summaries, cover URI persistence, and blank metadata grouping.
- Prior art exists in scan use case and fake scanner tests for scanner-to-repository integration.
- If shared scan state or controller behavior changes, run shared/Desktop tests. If Android scanner contracts change, also run Android Kotlin compile.
- Verification should include at least the focused persistent repository tests and the broader shared test command appropriate for this repo before implementation is considered complete.

## Out of Scope

- Implementing iOS P1 system music library integration.
- Implementing a full iOS playback engine or proving playback parity with Android/Desktop.
- Building a complete “管理全部” source-management UI beyond what is required for safe source semantics.
- Copying iOS files into app sandbox as a keepalive strategy. This PRD follows source availability and platform-resolvable file-source references, not permanent sandbox copies.
- Adding filesystem watching, background rescan, cloud sync, online streaming, lyrics, DRM handling, or network artwork completion.
- Reworking the visual prototype or wrapping it as production UI.
- Changing unrelated playback queue, search, favorites, or navigation behavior except where scan reconciliation directly affects available songs.

## Further Notes

- Current handoff identified the main implementation risk: persistent merge logic appears to infer coverage by `sourceKind`. That is safe for Android complete scans, but unsafe for Desktop multi-folder accumulation, iOS accumulated file sources, cancelled scans, and failed partial scans.
- The final clarified iOS decision is yes: iOS P0 multi-file sources use the same accumulation principle as Desktop. Adding new files does not replace old files.
- The domain docs and ADRs already record source availability, cancelled scan safety, Desktop accumulation, and Android single-source behavior. Implementation should respect those decisions rather than re-opening product questions.
- This PRD intentionally does not create implementation issues. It is ready for an agent to split into vertical slices or implement directly from the repository’s local issue tracker conventions.

## Adversarial Review

- The PRD would be unsafe if an implementer interpreted iOS “导入” as copying files into the app sandbox and making them independent of the original source. The intended behavior is source availability plus platform-resolvable source references.
- The PRD would be unsafe if “source summaries” were only UI labels. Concrete source identity is required for Desktop/iOS reconciliation.
- The PRD would be unsafe if cancelled scans reused the same done state as successful scans without an outcome reason. The UI must be able to render “已取消” distinctly.
- The PRD would be unsafe if tests only check counts. They must assert which exact songs remain available or unavailable after each scan.
- The PRD remains broad enough to include scan lifecycle UI and merge semantics. If implementation is split into issues, source coverage and cancellation behavior should be separate vertical slices with shared acceptance criteria.
