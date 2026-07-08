# 手机版首页歌曲列表项收藏样式实现计划

> **给 agent 执行者：** 按任务逐项执行；每个任务使用复选框跟踪。当前用户已明确要求进入实现计划和代码修改，本计划采用本会话内联执行。

**目标：** 让手机版首页“歌曲”页签的歌曲列表项与收藏页歌曲列表项保持一致，并接入心形收藏按钮。

**架构：** 首页继续拥有自己的列表和播放队列，只把歌曲行视觉和收藏操作表面收敛到收藏页规则。收藏状态不新增来源，由一级页面路由把现有 `controller::toggleFavorite` 注入首页。

**技术栈：** Kotlin Multiplatform、Compose Multiplatform、Material Icons、`kotlin.test`、Gradle。

## 全局约束

- 只修改生产 KMP App，不修改 `prototypes/kmp-music-hi-fi`。
- Markdown 描述内容使用中文。
- Kotlin 代码命名使用英文，注释和文档使用中文。
- 不重构收藏页结构，不新增共享歌曲卡片组件。
- 不修改全局迷你播放器、底部导航、搜索页或播放控制器。

---

## 文件结构

- 修改 `composeApp/src/commonTest/kotlin/com/yanhao/kmpmusic/feature/screen/HomeFigmaTokensTest.kt`：把首页歌曲行状态测试改成收藏页卡片规则。
- 修改 `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/screen/HomeSongRowStyle.kt`：让首页歌曲行状态输出收藏页卡片所需颜色、阴影和封面播放标识。
- 修改 `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/screen/HomeSongRow.kt`：把首页歌曲行 UI 改成收藏页同款布局，并加入心形按钮。
- 修改 `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/screen/HomeScreen.kt`：把收藏切换回调传给歌曲行。
- 修改 `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/app/routes/MobileRootScreenRoute.kt`：把 `controller::toggleFavorite` 注入首页。

## 任务一：用测试锁住首页歌曲行的新状态规则

**文件：**

- 修改：`composeApp/src/commonTest/kotlin/com/yanhao/kmpmusic/feature/screen/HomeFigmaTokensTest.kt`

**接口：**

- 消费：`resolveHomeSongRowStyle(isCurrentSong: Boolean): HomeSongRowStyle`
- 产出：测试要求 `HomeSongRowStyle` 提供 `metaColor`，并要求首页歌曲行使用收藏页白色卡片、2dp 阴影和当前歌曲封面标识。

- [x] **步骤 1：写红灯测试**

将首页歌曲行相关测试替换为下面两段，保留专辑和歌手页签 token 测试：

```kotlin
    /**
     * 普通首页歌曲行要跟收藏页一样使用白色卡片、阴影和收藏页文字层级。
     */
    @Test
    fun normalRowStyleUsesFavoritesCardSurface(): Unit {
        val style: HomeSongRowStyle = resolveHomeSongRowStyle(
            isCurrentSong = false,
        )
        assertEquals(expected = Color.White, actual = style.containerColor)
        assertNull(actual = style.border)
        assertEquals(expected = 2.dp, actual = style.shadowElevation)
        assertEquals(expected = favoritesTextColor, actual = style.textColor)
        assertEquals(expected = favoritesMetaColor, actual = style.metaColor)
        assertFalse(actual = style.showsCoverPlaybackBadge)
    }

    /**
     * 当前歌曲行不再使用旧首页浅绿色背景，而是跟收藏页一致用红字和封面标识表达播放态。
     */
    @Test
    fun currentRowStyleUsesFavoritesPlaybackMarker(): Unit {
        val style: HomeSongRowStyle = resolveHomeSongRowStyle(
            isCurrentSong = true,
        )
        assertEquals(expected = Color.White, actual = style.containerColor)
        assertNull(actual = style.border)
        assertEquals(expected = 2.dp, actual = style.shadowElevation)
        assertEquals(expected = MusicColors.PlayingRed, actual = style.textColor)
        assertEquals(expected = MusicColors.PlayingRed, actual = style.metaColor)
        assertTrue(actual = style.showsCoverPlaybackBadge)
    }
```

同时删除旧测试不再需要的 `BorderStroke`、`assertNotNull` 和旧首页 active 背景断言，补充 `assertTrue` 导入。

- [x] **步骤 2：运行测试确认红灯**

运行：

```bash
./gradlew :composeApp:desktopTest --tests "com.yanhao.kmpmusic.feature.screen.HomeFigmaTokensTest"
```

预期：失败。失败原因应是 `HomeSongRowStyle` 暂时没有 `metaColor`，或旧实现仍返回 0dp 阴影、浅绿色当前行、旧首页文字色。

## 任务二：实现首页歌曲行收藏页样式和收藏按钮

**文件：**

- 修改：`composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/screen/HomeSongRowStyle.kt`
- 修改：`composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/screen/HomeSongRow.kt`
- 修改：`composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/screen/HomeScreen.kt`
- 修改：`composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/app/routes/MobileRootScreenRoute.kt`

**接口：**

- 消费：`Song.isLiked`、`controller::toggleFavorite`、收藏页现有 token。
- 产出：`HomeScreen` 新增 `onLike: (String) -> Unit` 参数；`HomeSongRow` 新增 `onLike: (String) -> Unit` 参数。

- [x] **步骤 1：更新 `HomeSongRowStyle`**

把 `HomeSongRowStyle` 改为包含 `metaColor`，并让状态规则贴近收藏页：

```kotlin
internal data class HomeSongRowStyle(
    val containerColor: Color,
    val border: BorderStroke?,
    val shadowElevation: Dp,
    val textColor: Color,
    val metaColor: Color,
    val showsCoverPlaybackBadge: Boolean,
)

// 首页歌曲行跟收藏页卡片统一，只用红字和封面标识表达当前歌曲。
internal fun resolveHomeSongRowStyle(
    isCurrentSong: Boolean,
): HomeSongRowStyle {
    val textColor: Color = if (isCurrentSong) MusicColors.PlayingRed else favoritesTextColor
    val metaColor: Color = if (isCurrentSong) MusicColors.PlayingRed else favoritesMetaColor
    return HomeSongRowStyle(
        containerColor = Color.White,
        border = null,
        shadowElevation = 2.dp,
        textColor = textColor,
        metaColor = metaColor,
        showsCoverPlaybackBadge = isCurrentSong,
    )
}
```

- [x] **步骤 2：更新 `HomeSongRow` 布局**

把首页歌曲行改为收藏页卡片节奏：`favoritesSongRowHeight`、`favoritesSongRowRadius`、`favoritesSongRowPadding`、`favoritesSongCoverSize`、`favoritesSongCoverRadius` 和 `favoritesSongActionSize`。右侧操作区使用心形按钮和更多按钮。

关键规则：

- `Surface` 点击仍调用 `onSongPlay(song, queueSongs)`。
- 心形按钮点击调用 `onLike(song.id)`。
- 心形图标使用 `Icons.Rounded.Favorite` 或 `Icons.Rounded.FavoriteBorder`。
- 更多按钮点击调用 `onMore(song)`。
- 封面在 `rowStyle.showsCoverPlaybackBadge` 为 `true` 时显示 `PlayingGlyph(color = MusicColors.PlayingRed)`。
- 歌曲标题用 `rowStyle.textColor`，歌手名用 `rowStyle.metaColor`。

- [x] **步骤 3：串起首页收藏回调**

在 `HomeScreen`、`homeSongItems`、`HomeSongRow` 调用链中加入 `onLike: (String) -> Unit`。在 `MobileRootScreenRoute` 的首页分支传入 `onLike = controller::toggleFavorite`。

- [x] **步骤 4：运行测试确认绿灯**

运行：

```bash
./gradlew :composeApp:desktopTest --tests "com.yanhao.kmpmusic.feature.screen.HomeFigmaTokensTest"
```

预期：通过，且输出中没有编译错误。

## 任务三：验证、审查和提交

**文件：**

- 检查：全部已修改文件。

**接口：**

- 消费：任务一和任务二完成后的工作区 diff。
- 产出：通过验证的实现提交。

- [x] **步骤 1：运行 Android 编译**

运行：

```bash
./gradlew :composeApp:compileDebugKotlinAndroid
```

预期：退出码为 0。

- [x] **步骤 2：查看 diff 并做对抗式审查**

检查：

```bash
git diff -- composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/screen/HomeSongRowStyle.kt composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/screen/HomeSongRow.kt composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/screen/HomeScreen.kt composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/app/routes/MobileRootScreenRoute.kt composeApp/src/commonTest/kotlin/com/yanhao/kmpmusic/feature/screen/HomeFigmaTokensTest.kt
```

重点攻击：

- 心形按钮是否误触发行播放。
- 首页是否仍使用首页歌曲列表作为播放队列。
- 当前歌曲是否误保留旧浅绿色背景。
- 收藏状态是否复用现有控制器入口。
- 改动是否误碰收藏页结构或全局 chrome。

- [x] **步骤 3：提交代码**

如果验证通过，提交：

```bash
git add docs/superpowers/plans/2026-07-08-mobile-home-song-row-favorite-style.md composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/screen/HomeSongRowStyle.kt composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/screen/HomeSongRow.kt composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/screen/HomeScreen.kt composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/app/routes/MobileRootScreenRoute.kt composeApp/src/commonTest/kotlin/com/yanhao/kmpmusic/feature/screen/HomeFigmaTokensTest.kt
git commit -m "feat: 统一首页歌曲列表项收藏样式"
```

预期：产生一个实现提交，包含计划文档、测试和代码改动。

## 自审

- 规格覆盖：计划覆盖首页歌曲项卡片视觉、心形按钮、更多按钮、首页播放队列、当前播放红字和封面标识、全局 chrome 不变。
- 占位扫描：没有占位项，所有任务包含具体文件、代码片段、命令和预期结果。
- 类型一致性：`onLike: (String) -> Unit` 与现有收藏页和控制器入口一致，`HomeSongRowStyle.metaColor` 只服务首页歌曲行文本层级。

## 执行记录

- 红灯验证：`./gradlew :composeApp:desktopTest --tests "com.yanhao.kmpmusic.feature.screen.HomeFigmaTokensTest"` 先因 `HomeSongRowStyle.metaColor` 不存在而失败。
- 绿灯验证：同一聚焦测试在实现后通过。
- Android 编译：`./gradlew :composeApp:compileDebugKotlinAndroid` 通过。
- 完整共享测试：`./gradlew :composeApp:desktopTest` 通过。
- 子代理 code review：当前工具规则要求只有用户显式授权代理时才能 spawn，因此本次改为本地对抗式审查。
