# Current State

Updated: 2026-07-13
Status: ready
Basis: uncommitted
Verification: passed
Updated by: agent

## Current objective

- 移动端通用 Toolbar 优化已经实现并通过验证，等待人工查看最终视觉效果。

## Completed

- 用户最终确认移动端通用 Toolbar 背景统一改为 `#FFFFFF`，其余已确认的 Figma 节点 `994:1813` 尺寸和页面映射保持不变。
- 新增共享的一级 Toolbar、二级 Toolbar 和二级页面壳层，统一 `52dp` 高度、`4dp` 外边距与间距、左右 `48dp` 槽位、`40dp` 状态层、`24dp` 图标及 `18sp/26sp` 单行标题。
- 首页与收藏页保留固定 Toolbar；首页标题随歌曲、专辑、歌手分段变化，两个页面均保留搜索入口；“我的”页面不新增 Toolbar。
- 专辑、歌单详情使用动态名称固定栏；设置、关于、登录、扫描、最近播放、本地歌单、本地音乐和缺失态统一使用固定二级栏，原副标题迁入可滚动正文。
- 歌手详情保留透明展开态和原 `56dp` 渐显计算基准，只把最终收起态绘制规格校准为共享 `52dp` Toolbar。
- 搜索页、播放页和 Desktop 页面未纳入本轮 Toolbar 改造。
- code review 的 Standards 与 Spec 两轴最初各发现 2 项 P2，修正后复核均无剩余 P1/P2。
- `./gradlew :composeApp:compileDebugKotlinAndroid :composeApp:desktopTest` 与 `./gradlew :composeApp:installDebug` 均通过，最新 debug APK 已完成真机核对。
- 仓库未提供 `spotlessKotlinCheck` 任务；改用编译器与 `git diff --check` 门禁，后者通过。

## In progress

- None.

## Next actions

1. 人工查看真机页面；搜索页和播放页继续作为后续独立设计任务。

## Blockers and open questions

- None.

## Verification status

- repo:composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/components/MobileToolbar.kt
- repo:composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/components/MobileSecondaryPage.kt
- repo:composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/screen/ArtistDetailScrollBehavior.kt
- test:composeApp/src/commonTest/kotlin/com/yanhao/kmpmusic/feature/app/layout/MobilePlayerOverlayGestureTest.kt::shouldRenderOverlayScreenDirectlyForSelfManagedPages

## Relevant changed files

- composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/core/theme/MusicTheme.kt
- composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/components/MobileToolbar.kt
- composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/components/MobileSecondaryPage.kt
- composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/app/layout/MobileContentLayout.kt
- composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/app/routes/MobileSecondaryScreenRoute.kt
- composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/screen
- composeApp/src/commonTest/kotlin/com/yanhao/kmpmusic/feature/app/layout/MobilePlayerOverlayGestureTest.kt
- .memory/STATE.md
