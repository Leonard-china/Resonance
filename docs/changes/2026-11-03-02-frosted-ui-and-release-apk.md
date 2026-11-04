# 2026-11-03 — 落地 macOS 毛玻璃原生界面、四项导航与播放器控制台并构建 APK

- 修改日期：2026-11-03（Asia/Shanghai）
- 类型：UI 重构 / 本地构建交付
- 适用版本：`0.6.5 / 39`，未升版
- 影响平台：共享 Android／Windows UI；本次仅构建 Android Release APK
- 关联需求：用户认可 macOS 毛玻璃图片方向，要求界面改完后构建 Release APK 亲自试用，明确“不用你测试”。
- 关联记录：[图片方向与 skills](2026-11-02-10-ui-redesign-skills-and-frosted-material.md)、[连续歌词修复](2026-11-03-01-continuous-lyrics-motion.md)。

## 需求或问题

先前只交付图片和歌词滚动修复，整体磨砂视觉还未落地。用户明确要求继续完成界面实现后再构建安装包。上一版厚重凸起、倾斜封面和粒子光晕不符合认可的自然磨砂质感。

## 原因与依据

当前代码已依赖 Haze，但 `NowPlayingOverlay` 向下提供空 `LocalGlassState`，播放器大部分面板使用实体色。`resonanceGlass` 的叠色透明度接近 90%，透色不明显。主壳背景使用流体装饰，内容区再绘制不透明画布；MiniPlayer 和导航也为实体色。一起听仍属于工具子页，助手手机顶栏占两行。

本轮依据已认可图片和现有原生功能实现，用缓存中的真实专辑封面承担背景图像，以薄边、柔焦和浅阴影建立层次。使用既有原生 Compose／Haze，不引入网页和新依赖。用户明确跳过测试的要求优先于 skill 的截图／设备验收流程；没有运行自动测试或借用历史测试作为本轮结果。

## 具体修改

| 文件 | 关键符号 | 实际修改与目的 |
| --- | --- | --- |
| [FrostedBackdrop.kt](../../composeApp/src/commonMain/kotlin/com/resonance/player/design/FrostedBackdrop.kt) | `FrostedBackdrop` | 从现有 `ArtworkMemoryCache` 异步读取封面／内嵌封面，背景柔焦、轻微放大和受控明暗覆盖；无封面使用低饱和种子色，切歌短过渡，取消时不吞协程异常 |
| [ResonanceTheme.kt](../../composeApp/src/commonMain/kotlin/com/resonance/player/design/ResonanceTheme.kt) | `colors`、`resonanceGlass`、`ResonanceMotionTokens` | 新浅色／深色画布、透色玻璃和次文字颜色；Haze 28dp 模糊、轻噪点、渐变细边，保留高不透明度 fallback；导航弹簧改为阻尼 1 |
| [GallerySurfaces.kt](../../composeApp/src/commonMain/kotlin/com/resonance/player/design/GallerySurfaces.kt)、[MusicStage.kt](../../composeApp/src/commonMain/kotlin/com/resonance/player/design/MusicStage.kt) | `galleryPlinth`、`MusicStageBackdrop`、`musicSleevePerspective`、`musicDock` | 普通表面用透色轻材质，不为每行添加背景模糊；播放台使用磨砂；移除粒子／光束、封面固定倾斜与深凸起感，保留波浪进度使用的播放相位函数 |
| [LibraryShell.kt](../../composeApp/src/commonMain/kotlin/com/resonance/player/ui/LibraryShell.kt) | 主壳／四项导航／MiniPlayer／曲库页头 | 背景使用真实封面；音乐库、音乐助手、一起听、工具成为四个一级目的地，手机／rail／侧栏顺序一致；一起听返回音乐库；导航、迷你播放器、曲库页头统一磨砂，内容区透色，保留滚动状态、IME 和返回逻辑 |
| [NowPlayingOverlay.kt](../../composeApp/src/commonMain/kotlin/com/resonance/player/ui/NowPlayingOverlay.kt) | `NowPlayingOverlay`、`NowPlayingTopBar`、`ArtworkPane`、`TrackIdentity`、歌词／队列 | 独立 Haze 采样源，封面柔焦背景；正面封面和左对齐歌曲信息；手机进度、播放、分栏入口收纳进底部磨砂控制台，宽屏／短横屏保留各自布局；歌词与队列面板也使用磨砂 |
| [SyncedLyricsPreview.kt](../../composeApp/src/commonMain/kotlin/com/resonance/player/ui/SyncedLyricsPreview.kt) | 预览尺寸 | 保留上一记录的稳定行身份和连续滚动，正常字号手机预览高度减为 112dp，为封面和控制台留空间；舞台和大字号独立处理 |
| [PlaylistGrid.kt](../../composeApp/src/commonMain/kotlin/com/resonance/player/ui/PlaylistGrid.kt) | 每日推荐卡 | 使用控制台同类材质，移除遮挡磨砂的不透明装饰背景和额外倾斜叠片，降低高度，保留真实推荐状态与操作 |
| [MusicFeatureScreens.kt](../../composeApp/src/commonMain/kotlin/com/resonance/player/ui/MusicFeatureScreens.kt) | `AssistantScreen`、`AssistantWelcome` | 手机单行磨砂顶栏，直接新建；历史／任务／设置收纳在更多菜单；磨砂输入底座和透色消息／示例，所有原有动作仍可达 |
| [ToolsScreens.kt](../../composeApp/src/commonMain/kotlin/com/resonance/player/ui/ToolsScreens.kt) | `ProfileScreen`、`ToolPage`、`SettingRow` | 工具／设置／导入／帮助等共享磨砂页头与阅读面板；工具按音乐管理、聆听与偏好分组，删除重复一级入口；设置图标改轻底色，保存目录按钮使用 onPrimary 前景 |
| [TogetherScreen.kt](../../composeApp/src/commonMain/kotlin/com/resonance/player/ui/TogetherScreen.kt)、[TogetherCompanionPanel.kt](../../composeApp/src/commonMain/kotlin/com/resonance/player/ui/TogetherCompanionPanel.kt) | 房间／成员／聊天容器 | 表面透色，不改邀请、状态、聊天、连接或播放语义，不给每条消息附加模糊 |
| [DESIGN.md](../../DESIGN.md)、[PRODUCT.md](../../PRODUCT.md) | 原生设计与任务事实 | 保存已认可方向、具体材质及布局约定，明确本轮由图片进入实现和用户要求不运行测试 |

## 修改前后行为

| 场景 | 修改前 | 修改后 |
| --- | --- | --- |
| 打开播放器 | 实体面板、倾斜封面、装饰光场 | 封面柔焦背景、正面封面、磨砂顶栏与控制台 |
| 手机控制区 | 进度／播放／分栏分散在画布 | 聚合于一个磨砂控制台，原回调不变 |
| 切到一起听 | 先进入工具子页 | 底栏直接进入，平板／桌面对应导航一致 |
| 助手手机顶栏 | 标题与多项操作占两行 | 一行顶栏，新建直达，次要操作进入更多菜单 |
| 曲库、工具和设置 | 多种实体表面混用 | 统一透色背景、磨砂页头／底座和轻表面 |
| 页面切换与背景 | 导航越位弹簧、粒子和光束装饰 | 阻尼 1 的稳定过渡，背景仅随音乐内容改变 |

## 平台与数据影响

共享 UI 影响 Android 与 Windows；本次只交付 Android APK，不构建 Windows 安装包。保留原生返回、窗口分支、字体缩放、减少动效和键盘焦点。未修改播放后端、同步协议、网络服务、用户库、歌词持久化、权限与收费流程，没有删除或改写用户音频。

本机先前无 Android SDK，本次将官方 command-line tools、platform 36、build-tools 36.0.0 和 platform-tools 安装到忽略的 `build/frosted-ui-2026-11-03/tooling/android-sdk/`，仅更新被忽略的 `local.properties` 的 SDK 路径。复用先前临时 JDK 17，直接调用 Gradle Wrapper JAR，避免修改 CRLF 的 Wrapper 脚本。签名由项目既有 Gradle 配置读取，不查看／输出签名配置、私钥或 API Key。

## 构建与交付核对

实际执行 Release 构建，显式排除 lintVital 分析／报告任务；没有运行单元、集成、UI、设备或帧率测试，也没有安装到用户设备。本轮前歌词修复的 9 项通过不代表本轮新材质已通过验收。

```bash
build/lyrics-motion-2026-11-03/tooling/jdk/bin/java \
  -classpath gradle/wrapper/gradle-wrapper.jar org.gradle.wrapper.GradleWrapperMain \
  :composeApp:assembleRelease \
  -x lintVitalAnalyzeRelease -x lintVitalReportRelease -x lintVitalRelease \
  --no-configuration-cache --console=plain
```

| 操作 | 结果 |
| --- | --- |
| 首轮 `assembleRelease` | 通过，49 项任务（45 执行、4 up-to-date），2m 12s；日志 `build/frosted-ui-2026-11-03/release-build.log` |
| 最终源码再次 `assembleRelease` | 通过，49 项任务（1 执行、48 up-to-date），1s；确认最终源码已对应产物，日志 `release-final.log` |
| `apksigner verify --verbose` | 直接调用脚本因 PATH 无 java 退出 127；改用临时 JDK 执行 `-jar .../lib/apksigner.jar verify --verbose` 后通过，v2 签名有效、1 位签名者；日志 `apk-signature.log` |
| 归档与 SHA-256 | 复制到独立日期目录，保留旧正式包；校验值从本次 APK 计算 |
| 功能／设备／UI／帧率测试与 Lint | 按用户要求未执行；未安装到设备 |

本地试用产物：`release/0.6.5/2026-11-03-frosted-ui/Resonance-0.6.5-release.apk`。

- 文件大小：167,333,560 字节（159.58 MiB）。
- SHA-256：`d573dab23c56caee84168b5ff0bb8442df2e2f79d1dbf981af536eb331f00664`。
- 同目录包含 `SHA256SUMS.txt` 与 `README.txt`。
- 使用原有 applicationId `com.resonance.player`、版本 `0.6.5 / 39` 与项目已有签名配置；没有新建或替换密钥。

构建提示现有 native 库无法 strip，因此按原样打包；另有 SDK XML 版本和既有弃用 API 警告。没有为消除警告扩展功能改动范围。

## 限制与后续

- 按用户要求未做本版功能测试、视觉截图、Android 真机体验、帧率／发热与 Windows 验收；本轮完成实现和可试用包，不宣称已验证设备效果与概念图逐像素一致。
- 模糊支持不足时使用更实的底色，老 Android 的背景 blur 表现可能不同；实际阅读对比、字体缩放和短屏布局由试用反馈进一步调整。
- 本次为本地试用构建，不发布官网、不推送 Git、不替换线上安装包，不自动升版。

## 同步更新的文档

`docs/README.md` 更新最新 UI 状态与独立记录索引；`docs/DEVELOPMENT.md` 更新四项导航和 2026-11-03 原生材质基线、旧视觉记录标为历史；`PRODUCT.md` 和 `DESIGN.md` 记录已实施方向及用户不运行测试的要求。
