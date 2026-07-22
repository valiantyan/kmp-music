# 项目地图

本文承接根 `AGENTS.md` 的代码定位细则。只有需要确认技术栈版本、模块入口、源码集职责、测试目录或 `.scratch` 记录位置时才需要读。

## 事实来源

- 技术栈版本以 `gradle/libs.versions.toml` 和 `composeApp/build.gradle.kts` 为准。
- 主模块是 `:composeApp`；包名与 `applicationId` 是 `com.yanhao.kmpmusic`。
- 不确定 Gradle 任务是否存在时，先跑 `./gradlew :composeApp:tasks`。

## 源码入口

- `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/domain`：领域模型、Repository 接口、UseCase、播放协调和持久化契约。
- `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/data`：共享数据实现、持久化 Repository、扫描合并、数据库工厂和本地音频规则。
- `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/core/theme`：主题、颜色、尺寸、封面调色和共享设计令牌。
- `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/app`：全局 App 状态、导航、chrome、播放、收藏、搜索、扫描和会话控制器。
- `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/components`：跨页面复用 UI 组件。
- `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/screen`：移动端页面级 Composable 和显示模型。
- `composeApp/src/commonMain/kotlin/com/yanhao/kmpmusic/feature/desktop`：桌面端布局、导航、页面、底部播放器和桌面专用显示模型。

## 平台源码集

- `composeApp/src/androidMain`：Android 入口、权限、MediaStore、Media3、通知、数据库和平台适配。
- `composeApp/src/iosMain`：iOS 入口、导入曲库、系统音乐资料库、AVFoundation 和平台适配。
- `composeApp/src/desktopMain`：Desktop/macOS 入口、扫描目录、AVFoundation native bridge、数据库和平台适配。
- `composeApp/src/desktopMain/kotlin/com/yanhao/kmpmusic/qa`：固定 `1240×824` 的 Desktop UI QA 场景、自动取证和帧校验；只使用内存 fake 数据。

## 测试与记录

- `composeApp/src/commonTest`：共享逻辑测试，覆盖领域、数据、控制器、导航、状态同步、主题算法和显示模型。
- `composeApp/src/desktopTest`：桌面与 Apple/macOS 播放门禁相关测试。
- `scripts/desktop-ui-qa.sh`：直接打开 `home / home-playing / albums / artists` 场景，调用 `:composeApp:desktopUiQa` 生成三帧证据并自动退出。
- `.scratch/<feature-slug>/`：本地 PRD、issue、验证记录和临时研究材料；除非任务要求，不提交原始附件、截图、构建产物或本地缓存。
