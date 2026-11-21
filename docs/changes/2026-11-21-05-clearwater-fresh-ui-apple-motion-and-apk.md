# 2026-11-21 — 清岚（Clearwater）清新界面重构、苹果式弹簧动效与歌词舞台升级及 0.7.0 APK 构建

- 修改日期：2026-11-21（Asia/Shanghai）
- 类型：功能 / UI 视觉与动效重构 / 构建验证
- 适用版本：`0.7.0 / 70`（`version.properties`，未改变版本号）
- 影响平台：共享 / Android / Windows / Linux
- 关联记录或需求：[Atelier Acoustics 重构](2026-11-21-01-atelier-acoustics-ui-redesign.md)、[升版 0.7.0 构建](2026-11-21-03-bump-to-0.7.0-and-build-release-apk.md)、[修复歌单导入失败与多选批量补全](2026-11-21-04-fix-sync-import-and-batch-enrich.md)

## 需求或问题

用户在真机试用 0.7.0 后提出三项核心体验升级需求：

1. **去除红色主调，转向清新通透风格**：不希望继续使用朱红/珊瑚红作为主强调色，希望整体视觉更清爽、现代。
2. **补齐苹果风格（iOS / macOS）动态效果与歌词滚动质感**：此前版本动效偏静态，需要更有呼吸感与物理回弹的按钮/控件动效、播放氛围光晕和 Apple Music 风格的沉浸歌词滚动与聚焦表现。
3. **优化按钮、功能入口与排版，并产出新版 Android Release APK**：让工具页、歌单页与播放控制的层次和常用入口更清晰直观，同时直接构建最新安装包供手机安装验证。

## 原因与依据

1. 前版 `Atelier Acoustics` 使用暖朱红 `#B83227` / 珊瑚红 `#F26A55` 主色与偏静态的 `tween` 线性/减速过渡，导致整套应用色温偏重红调、触感反馈略显克制。
2. 工具页原先为垂直单列 8 条纯列表，缺乏主次区分；歌单页顶部的「我喜欢的音乐」与「每日推荐」纵向堆叠占用首屏高度；歌词页虽已支持连续居中滚动，但仍用固定 `tween` 过渡且缺乏绘制层光学聚焦缩放。
3. 本机根目录仍无正式 `keystore.properties`，延续 [2026-11-21-03](2026-11-21-03-bump-to-0.7.0-and-build-release-apk.md) 经用户授权的构建限额与调试签名方式产出同签名 `Resonance-0.7.0-release.apk`，以便用户在手机直接覆盖安装此前 0.7.0 测试版。

## 具体修改

| 文件 | 关键类/函数/配置 | 具体修改与目的 |
| --- | --- | --- |
| [`composeApp/src/commonMain/kotlin/com/resonance/player/design/ResonanceTheme.kt`](../../composeApp/src/commonMain/kotlin/com/resonance/player/design/ResonanceTheme.kt) | `colors`、`ImmersivePlayerColors`、`ImmersivePlayerColorScheme`、`ResonanceMotionTokens`、`resonancePressable` | 切换至 **Clearwater（清岚）** 清新色板：浅色薄雾浅瓷 `#F3F7F6` + 海沫青碧 `#0B7D67`（配白字 5.07:1）；深色深海暮蓝 `#0B1214` + 薄荷青 `#35D4B2`（配画布 10.09:1）；冰蓝模式采用 `#1A65C2` / `#6EB6FF`；危险红 `#B92B2B` / `#FF8585` 严格限定于删除操作；沉浸舞台转为深海青墨 `#0C1317` + 冰晶白 `#F5FAFA`。新增 `AppleFluidSpring`、`AppleBouncySpring`、`LyricScrollSpec` 弹簧常数，升级 `resonancePressable` 为弹性按压回弹（`dampingRatio = 0.64f, stiffness = 480f`）。 |
| [`composeApp/src/androidMain/res/values/styles.xml`](../../composeApp/src/androidMain/res/values/styles.xml) | `AppTheme.windowBackground` | 启动窗背景同步更新为 `#0B1214`，与清岚深色底层无缝衔接。 |
| [`composeApp/src/commonMain/kotlin/com/resonance/player/ui/PlaylistArtwork.kt`](../../composeApp/src/commonMain/kotlin/com/resonance/player/ui/PlaylistArtwork.kt) | `ResonanceBrandMark`、`PlaylistMosaicArtwork` | 品牌徽标升级为海沫青到冰川天蓝斜向渐变（`#0D947B → #1F7AC8`）；歌单 2×2 拼贴暗缝更新为不透明深海青墨 `#142022`。 |
| [`composeApp/src/commonMain/kotlin/com/resonance/player/design/AlbumVisuals.kt`](../../composeApp/src/commonMain/kotlin/com/resonance/player/design/AlbumVisuals.kt) | `sleevePalettes` | 全部 8 套确定性生成封套更新为海沫、冰川、竹雾、暮海、瓷青、晨鼠尾草、靛蓝与深薄荷清新配色。 |
| [`composeApp/src/commonMain/kotlin/com/resonance/player/design/FrostedBackdrop.kt`](../../composeApp/src/commonMain/kotlin/com/resonance/player/design/FrostedBackdrop.kt) | `FrostedBackdrop(isPlaying)` | 为全屏沉浸舞台新增纯绘制层慢速流体双极光团（14 秒周期正弦漂移，复用 `rememberPlaybackVisualPhase`）；暂停、后台、展开拖动期间或开启「减少动态效果」时即时静止零开销。 |
| [`composeApp/src/commonMain/kotlin/com/resonance/player/design/GlassSegmentedControl.kt`](../../composeApp/src/commonMain/kotlin/com/resonance/player/design/GlassSegmentedControl.kt)、[`LyricFocus.kt`](../../composeApp/src/commonMain/kotlin/com/resonance/player/design/LyricFocus.kt)、[`ui/LyricScroll.kt`](../../composeApp/src/commonMain/kotlin/com/resonance/player/ui/LyricScroll.kt) | `GlassSegmentedControl`、`Modifier.lyricFocus`、`FollowLyric` | 分段滑块切至 Apple 弹簧（`dampingRatio = 0.82f, stiffness = 420f`）；歌词焦点高亮采用平滑弹簧变色；歌词自动居中滚动切至阻尼弹簧 `ResonanceMotionTokens.LyricScrollSpec`（`dampingRatio = 0.86f, stiffness = 220f`）。 |
| [`composeApp/src/commonMain/kotlin/com/resonance/player/ui/common/SoftNeumorphicButtons.kt`](../../composeApp/src/commonMain/kotlin/com/resonance/player/ui/common/SoftNeumorphicButtons.kt) | `SoftNeumorphicTokens`、`SoftHeroPlayButton`、`WavySlider` | 全面移除朱红主渐变改用清岚青碧色阶；主播放键升级 `AnimatedContent` Play↔Pause 弹性缩放形变与 `AppleBouncySpring` 按压反馈；全屏平直进度条（`flat = true`）在按压/拖动时轨高由 `4dp` 弹性扩展至 `7dp` 并呈现圆点柔光晕轮。 |
| [`composeApp/src/commonMain/kotlin/com/resonance/player/ui/NowPlayingOverlay.kt`](../../composeApp/src/commonMain/kotlin/com/resonance/player/ui/NowPlayingOverlay.kt) | `NowPlayingOverlay`、`TactileTopBarButton`、`PaneSwitcher`、`ArtworkPane`、`TactilePlayPauseButton`、`LyricRow` | 沉浸背景接入 `playerState.isPlaying && ambientMotionEnabled`；收藏按钮点亮时触发苹果式弹性跃动（`1.0f → 1.30f → 1.0f`）；底部三栏胶囊升级为带滑动磨砂透镜的弹簧滑块；封面接入 `1.0f ↔ 0.92f` 弹性呼吸缩放与专辑柔光晕轮及切歌缩放入场；主播放键接入 Play↔Pause 形变与冰晶白圆盘；`LyricRow` 在绘制层（`graphicsLayer`，左中锚点）增加 `1.03f / 0.96f` Apple Music 风格光学聚焦缩放，零布局行高变化。 |
| [`composeApp/src/commonMain/kotlin/com/resonance/player/ui/PlaylistGrid.kt`](../../composeApp/src/commonMain/kotlin/com/resonance/player/ui/PlaylistGrid.kt) | `PlaylistListTab` | 当「每日推荐」可见时，顶部「我喜欢的音乐」（严格保留 `"X 首"` 副标题）与「每日推荐」并排排布为清新双列 Bento 快捷卡；为歌单卡片与快捷卡补全弹簧按压回弹与圆形柔和 `+` 新建按钮。 |
| [`composeApp/src/commonMain/kotlin/com/resonance/player/ui/LibraryShell.kt`](../../composeApp/src/commonMain/kotlin/com/resonance/player/ui/LibraryShell.kt) | `LibraryShell`、`BottomNavigationBar`、`LibraryScreen`、`CollectionGrid`、`TrackRow`、`MiniPlayer` | 曲库首页顶部加入柔和青蓝晨雾微光；手机底栏切换滑块升级为 Apple 弹簧，活跃图标增加轻微跃起缩放；歌曲行左侧 `3dp` 播放指示条平滑伸缩；专辑/歌手卡片接入按压回弹；迷你播放条进度细线平滑过渡，手机紧凑播放键升级 Play↔Pause 形变。 |
| [`composeApp/src/commonMain/kotlin/com/resonance/player/ui/ToolsScreens.kt`](../../composeApp/src/commonMain/kotlin/com/resonance/player/ui/ToolsScreens.kt) | `ProfileScreen`、`ProfileBentoTile`、`SettingRow` | 工具页顶部重构为 2×2 清新柔和渐变 Bento 矩阵（导入音乐、歌单导入与导出、曲库智能管家、音乐品味画像），下方以「扩展与设置」收拢其余入口；列表行补齐弹簧按压反馈。 |
| `composeApp/build/outputs/apk/release/Resonance-0.7.0-release.apk` | Android Release APK | 基于包含本次全部修复与 UI 升级的完整工作区源码重新构建并验证对齐与签名（`167524719` 字节）。 |

## 修改前后行为

| 场景 | 修改前 | 修改后 |
| --- | --- | --- |
| 全局主色与视觉氛围 | 暖雪花石纸 / 黑曜石 + 暖朱红/珊瑚红主强调，代码含多处红色强调 | 薄雾浅瓷 / 深海暮蓝 + 海沫青碧 / 冰川天蓝主强调，源码零残留红色主色（危险红仅用于删除） |
| 交互与播放器动效 | 控件切换多为固定时长线性/减速 `tween`，无播放键形变、收藏弹跳、进度条扩张或流体背景光团 | 分段/底栏/三栏胶囊弹簧滑行、Play↔Pause 缩放形变、收藏心形跃动、封面弹性呼吸+柔光晕轮、进度条 4dp→7dp 拖动扩张、播放期 14 秒流体漂移光团 |
| 歌词滚动与焦点表现 | 固定 `tween` 滚动，高亮仅变色与淡底 | 阻尼弹簧 `LyricScrollSpec` 平滑居中 + 绘制层 `1.03f / 0.96f` 光学聚焦缩放 + 无框柔光与 3dp 竖条，行高零抖动 |
| 工具页与歌单页排版 | 工具页 8 条扁平竖列；歌单快捷入口纵向堆叠 | 工具页 2×2 彩色 Bento 矩阵 + 下方扩展列表；歌单顶部双列快捷卡；歌单详情直出「导出」按钮 |
| Android Release APK | `14:14` 旧包（不含本次修复与清岚 UI） | 全新 `Resonance-0.7.0-release.apk`（SHA-256：`57d8c8ade81b8bbf12e86cc9efa6376a474f8abf4ee7e140e5d2905e0b7c20ff`） |

## 平台与数据影响

- **跨平台一致性**：全部主题、弹簧动效、Bento 排版与歌词焦点均落在 `commonMain`，Android、Windows、Linux 自动共享；Android 额外更新 `styles.xml` 窗底色。
- **数据与兼容性**：不修改 `version.properties`（保持 `0.7.0 / 70`）、不修改任何偏好键、`WeatherPalette.Sage` 枚举名、曲库结构、统计文件、音频文件或 `website/`。
- **签名边界**：因根目录无正式 `keystore.properties`，本次产物继续使用本机 `~/.android/debug.keystore` 签名（v1/v2/v3 通过），**不等同于正式发布证书**；但与今天 `14:14` 产出的 `0.7.0` 测试包同签名同版本，支持直接覆盖安装测试。

## 验证

| 命令或操作 | 环境 | 结果 |
| --- | --- | --- |
| `grep` 全仓核查残留红色主色 hex（`B83227/C93E31/99261D/FF6B5B/D9483B/E04E40/FA2D48/FF375F`） | `composeApp/src/` | **通过**：零匹配 |
| `compile-and-test.sh compile`（Desktop 主源码 + 测试源码 307 文件） | Linux + JDK 17，cgroup `MemoryMax=2G / MemoryHigh=1536M / MemorySwapMax=0` | **通过**：零错误编译 |
| `run-all-ui-tests.sh` + 修正代理键后重跑 `renderApprovedLightNativePagesAndSettings`（9 个 UI 测试类 + 22 项活跃 `UiVisualContractTest` 方法，共 53 项） | Linux + Skiko + JDK 17，cgroup `MemoryMax=2G / MemoryHigh=1536M / MemorySwapMax=0` | **通过**：53 / 53 全部通过（含 `LyricsMotionTest` 4、`LyricsPlayerRegressionTest` 1、`PlayerExpansionTest` 5、`PlayerRecoveryUiTest` 3、`textAndSelectedRolesStayReadableAcrossAllThemes` WCAG 对比度等）；1 项条件跳过（`renderNativeMotionPreviewFrames`） |
| 人工多视口离屏截图审查（12 张最新截图：手机浅/深色曲库、歌单详情、工具 2×2 Bento、歌单传输页、设置、助手、一起听、桌面 1440 曲库/详情/品味雷达、手机与宽屏沉浸播放器及歌词舞台） | `composeApp/build/{light-ui-native,macos-layout,lyrics-regression}/` | **通过**：无红调残留、层次清晰、对比度与对齐良好 |
| `:composeApp:assembleRelease --no-daemon --max-workers=2 --offline`（采用 02/03 号记录中用户已授权的释放限额：`MemoryMax=4600M / MemoryHigh=4200M / MemorySwapMax=0 / -Xmx2816m`，启动前可用内存 `7292 MiB`） | Linux + JDK 17 + Android SDK 36 | **通过**：耗时 `2m 20s`，53 个任务（19 执行、34 最新），`compileReleaseKotlinAndroid`、`lintVitalRelease`、`packageRelease` 均通过；日志存于 `build/release-apk-2026-11-21/assembleRelease-clearwater.log` |
| `zipalign -c -P 16 4`（签名前后） | Android SDK build-tools `36.0.0` | **通过**：16 KiB 页面对齐校验通过 |
| `apksigner sign`（`~/.android/debug.keystore`）+ `apksigner verify --verbose --min-sdk-version 23` | Android SDK build-tools `36.0.0` | **通过**：`Verifies`（v1/v2/v3 均通过，1 个签名者；调试证书非正式发布证书）；大小 `167524719` 字节，SHA-256 `57d8c8ade81b8bbf12e86cc9efa6376a474f8abf4ee7e140e5d2905e0b7c20ff` |
| 真机安装与实机流畅度/帧率验证 | Android 实机 / Windows 桌面 | **未执行**：主机当前未连接实机，待用户安装新 APK 体验复核 |

## 限制与后续

1. 离屏渲染截图与 JUnit 回归不能等同于真机 120Hz 手势与长时间播放帧率实测；若真机发现全屏流体光团或大歌单滚动帧率波动，可按需微调光团半径或关闭动画。
2. 当前 `Resonance-0.7.0-release.apk` 为调试证书签名版本，仅供本机测试安装；后续正式对外发布时需配置正式 `keystore.properties` 重新签名并另建 `docs/RELEASE_NOTES-0.7.0.md`。

## 同步更新的文档

- [DESIGN.md](../../DESIGN.md)（切换至 Clearwater 清岚色板、弹簧动效与 Bento 排版规范）
- [文档入口](../README.md)（顶部摘要与变更索引）
- [详细开发说明](../DEVELOPMENT.md)（顶部核对说明及第 11 节 UI/动效/歌词）
