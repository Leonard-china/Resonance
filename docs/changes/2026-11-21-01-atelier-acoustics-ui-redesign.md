# 2026-11-21 — Atelier Acoustics（声学画廊）三端原生 UI 全面重构与跨窗弹窗隔离

- 修改日期：2026-11-21（Asia/Shanghai）
- 类型：功能 / 重构
- 适用版本：`0.6.6 / 40`（`version.properties` 未改变）
- 影响平台：共享 / Android / Windows / Linux
- 关联记录或需求：用户在本会话授予完整视觉与交互自主权，摆脱此前 Mineradio / macOS 磨砂 / 浅色玻璃 / AM++ 等历史实验约束，要求以独立主导设计师与工程师身份重塑 Resonance 原生界面，并在完整保留既有播放、存储、安全与兼容边界的前提下以实际渲染截图迭代打磨。

## 需求或问题

修改前界面存在以下核心结构与视觉体验问题：

1. **视觉方向混杂与系统性欠统一**：主壳、歌单、工具页与二级页面（品味画像、智能管家、歌源、任务列表、推荐、搜索）残留多轮实验痕迹——部分采用冷灰/玫红与透明玻璃罩，部分仍遗留 `SoftNeumorphicTokens` 翡翠绿及 `Card(GlassLight)` 无边卡片，缺少独立连贯的产品视觉气质。
2. **品牌标识与封面质感不足**：缺少独立矢量品牌标识；歌单无论曲目多少均仅展示单张封面，缺失封面仅呈现简单太阳/等高线底图；桌面端和手机端底栏/播放条存在重阴影双胶囊割裂与 Material3 默认粗条音量滑块违和感。
3. **沉浸播放器细节粗糙**：顶栏左右按钮不对称导致「正在播放 / 专辑」标题相对下拉指示条偏左；播放键采用无底白三角且前/后曲使用快退/快进图标而非真实切歌图标；沉浸队列选中项继承了主色微边造成视觉干扰；封面页歌词预览邻行在视口边缘出现半截切字；`ImmersivePlayerColorScheme` 未独立成对封装。
4. **结构冗余与跨窗弹窗风险**：进入曲库集合详情页后仍叠加显示上层「音乐库」顶栏；删除确认与主强调色混用；6 个独立原生 `Dialog`（睡眠定时、编辑曲目、批量补全、分享卡片、截图导入、一起听共享队列）仍直接调用 `.resonanceGlass(...)` 而未隔离宿主 `LocalGlassState`，在 Android 上保留跨窗口 Haze 采样隐患。

## 原因与依据

通过完整审查设计系统与页面实现（`ResonanceTheme.kt`、`LibraryShell.kt`、`NowPlayingOverlay.kt`、`PlaylistGrid.kt`、`ToolsScreens.kt`、`SoftNeumorphicButtons.kt` 等）及多轮离屏原生渲染截图（`ImageComposeScene` 390×844 / 1280×800 / 1440×900 / 720×360、浅色与深色、正常与 1.5x 字号），确立全新独立视觉语言 **Atelier Acoustics（声学画廊）**：

- 借鉴实体声学器材与建筑纸面排版：浅色以暖雪花石纸（`#F6F4F0`）与纯白微浮层托举封面，深色以声学黑曜石（`#0E0E10`）承托暖象牙字与珊瑚强调色，经数学与 `UiVisualContractTest.textAndSelectedRolesStayReadableAcrossAllThemes` 校验所有前景对比度均 ≥ 5.08:1（满足 WCAG AA）。
- 通过两轮“实现 → 编译 → 渲染截图核查 → 缺陷修复”迭代闭环，在第二轮针对首轮截图暴露的 15 项具体视觉与安全瑕疵（顶栏偏左、封面预览切字、播放三角光学偏左、桌面默认 Material3 粗块音量条、详情页双层顶栏、选中歌曲行重框、歌单拼贴浅缝、队列红边、筛选框粉边、二级页对齐与 6 个独立窗口跨窗模糊）逐一收敛修正。

## 具体修改

| 文件 | 关键类/函数/配置 | 具体修改与目的 |
| --- | --- | --- |
| [ResonanceTheme.kt](../../composeApp/src/commonMain/kotlin/com/resonance/player/design/ResonanceTheme.kt) | `ResonanceColorTokens`、`colors`、`ImmersivePlayerColors`、`ImmersivePlayerColorScheme`、`Modifier.atelierCard`、`ResonanceShapes`、`resonanceTypography` | 建立 Atelier Acoustics 浅/深色及冰蓝钴色调色板；新增独立 `Danger` / `DangerSoft` 危险语义色；提供成对 `ImmersivePlayerColorScheme` 并覆写沉浸态 `GlassBorderGlow` 为白色；新增统一细边卡片修饰符 `atelierCard` 与收紧字距的编辑排印字阶。 |
| [AlbumVisuals.kt](../../composeApp/src/commonMain/kotlin/com/resonance/player/design/AlbumVisuals.kt) | `GenerativeAlbumSleeve` | 缺失封面升级为 8 套编辑配色 × 4 种确定性声学几何封套（同心声波环、地平线波形、包豪斯拱弧、极简黑胶唱片）。 |
| [FrostedBackdrop.kt](../../composeApp/src/commonMain/kotlin/com/resonance/player/design/FrostedBackdrop.kt) | `FrostedBackdrop` | 沉浸实图柔光升级为 `saturation=0.88f / blur=72dp / scale=1.18f` 配合 4 段式对比度暗幕；无图时由双极径向专辑氛围光晕承载。 |
| [LyricFocus.kt](../../composeApp/src/commonMain/kotlin/com/resonance/player/design/LyricFocus.kt) | `Modifier.lyricFocus` | 取消硬边框方框，改为无框横向柔光渐变底 + 3dp 左侧竖向聚焦条，保持零行高抖动且满足像素带回归检测契约。 |
| [GlassSegmentedControl.kt](../../composeApp/src/commonMain/kotlin/com/resonance/player/design/GlassSegmentedControl.kt) | `GlassSegmentedControl` | 升级为 `Soft` 细边凹槽 + 轻阴影 `Raised` 滑动高亮块，保留键盘与减少动态即时定位。 |
| [GallerySurfaces.kt](../../composeApp/src/commonMain/kotlin/com/resonance/player/design/GallerySurfaces.kt)、[MusicStage.kt](../../composeApp/src/commonMain/kotlin/com/resonance/player/design/MusicStage.kt) | `galleryShadow`、`glassControlSurface`、`musicDock` | 对齐新材质投影与细边，将旧玻璃圆钮与底座收口为建筑细边表面。 |
| [PlaylistArtwork.kt](../../composeApp/src/commonMain/kotlin/com/resonance/player/ui/PlaylistArtwork.kt) *(新增)* | `ResonanceBrandMark`、`PlaylistMosaicArtwork` | 新增同心声波矢量品牌徽标与 2×2 歌单拼贴封面组件（不足 4 首不同封面时回退至单张/生成封套）。 |
| [SoftNeumorphicButtons.kt](../../composeApp/src/commonMain/kotlin/com/resonance/player/ui/common/SoftNeumorphicButtons.kt) | `SoftNeumorphicTokens`、`SoftHeroPlayButton`、`RefinedSlider`、`SoftRefinedPlaybackScrubber`、`SoftRefinedVolumeSlider` | 清除残留翡翠绿常量并统一朱红/珊瑚色；播放三角 `+1dp` 光学居中；收紧平直进度条与时间戳垂直间距，圆点静息 8dp / 拖动 14dp；精修 `RefinedSlider` 细轨与动态拇指（12dp → 15dp）。 |
| [SyncedLyricsPreview.kt](../../composeApp/src/commonMain/kotlin/com/resonance/player/ui/SyncedLyricsPreview.kt) | `SyncedLyricsPreview`、`PreviewLyricRow` | 取消封面歌词预览的外框，改用 `CompositingStrategy.Offscreen` + `DstIn` 垂直边缘渐隐（封面预览采用 `0.30/0.70` 紧凑消隐防切字）。 |
| [NowPlayingOverlay.kt](../../composeApp/src/commonMain/kotlin/com/resonance/player/ui/NowPlayingOverlay.kt) | `NowPlayingOverlay`、`NowPlayingTopBar`、`ArtworkPane`、`TrackIdentity`、`TactilePlayPauseButton`、`PlayerQuickPill`、`PaneSwitcher`、`LyricsContent`、`LyricRow` | 接入 `ImmersivePlayerColorScheme`；顶栏通过 `BoxWithConstraints` 对称留白实现数学绝对居中；封面播放/暂停呼吸缩放（`1.0f / 0.95f`）与顶部高光细边；展示真实格式微徽标（`FLAC`/`WAV`/`AAC`/`MP3` 等）；核心播放键升级为 60dp 暖象牙圆盘配碳黑图标（`+1.5dp` 光学居中）与真实 `SkipPrevious`/`SkipNext`；快捷入口与分栏统一半透明微边胶囊；沉浸歌词应用垂直 `DstIn` 边缘渐隐与胶囊化「歌词工具」按钮。 |
| [PlaylistGrid.kt](../../composeApp/src/commonMain/kotlin/com/resonance/player/ui/PlaylistGrid.kt) | `PlaylistListTab`、`FavoritesEntryCard`、`PlaylistCard` | 柔色「我喜欢的音乐」主卡（保留纯数量副标题 `"X 首"`）、统一「每日推荐」入口、歌单计数徽标与 2×2 拼贴网格。 |
| [LibraryShell.kt](../../composeApp/src/commonMain/kotlin/com/resonance/player/ui/LibraryShell.kt) | `LibraryShell`、`BottomNavigationBar`、`DesktopSidebar`、`DesktopTopbar`、`LibraryScreen`、`TrackListTab`、`LibraryDetailView`、`TrackRow`、`MiniPlayer` | 手机浮空 16dp 迷你播放卡（内嵌圆角底边进度细线）+ 贴底 54dp 锚定导航栏；桌面 244dp 侧栏（品牌徽标、3dp 左侧竖条、2×2 缩略拼贴、内联 `+` 新建）+ 64dp 搜索顶栏 + 76dp 全宽底部播放坞（顶边进度细线 + `SoftRefinedVolumeSlider`）；曲库实时元数据统计、1:1 专辑网格、细边圆像歌手、进入非多选详情页隐藏上层重复「音乐库」顶栏；歌曲选中行改为无框柔底 + 3dp 竖条与 `tnum` 等宽数字；筛选栏中性细边、删除动作使用 `Danger`。 |
| [PlayerExpansion.kt](../../composeApp/src/commonMain/kotlin/com/resonance/player/ui/PlayerExpansion.kt) | `PlayerExpansionLayout` | 同步展开表面（`16dp`）与飞行封面圆角（`9dp → 16dp`）以衔接新版 MiniPlayer 与封面。 |
| [ToolsScreens.kt](../../composeApp/src/commonMain/kotlin/com/resonance/player/ui/ToolsScreens.kt) | `SettingRow`、`SectionHeader`、`ThemeOption`、`AboutScreen` | 设置行升级细边卡片与 38dp 柔色圆角方图标底托，关于页接入 `ResonanceBrandMark` 与版本徽标。 |
| [MusicFeatureScreens.kt](../../composeApp/src/commonMain/kotlin/com/resonance/player/ui/MusicFeatureScreens.kt)、[AssistantMusicAttachment.kt](../../composeApp/src/commonMain/kotlin/com/resonance/player/ui/AssistantMusicAttachment.kt) | `AssistantWelcome`、`AssistantScreen`、`AssistantMusicAttachment` | 助手欢迎页引入品牌标识与三张 `.atelierCard` 提示卡；聊天歌曲附件采用细边卡片与圆形播放按钮；输入区改为独立圆角输入坞。 |
| [TasteRadarScreen.kt](../../composeApp/src/commonMain/kotlin/com/resonance/player/ui/TasteRadarScreen.kt)、[SmartManagerScreen.kt](../../composeApp/src/commonMain/kotlin/com/resonance/player/ui/SmartManagerScreen.kt)、[MusicTasksScreen.kt](../../composeApp/src/commonMain/kotlin/com/resonance/player/ui/MusicTasksScreen.kt)、[MusicSourcesScreen.kt](../../composeApp/src/commonMain/kotlin/com/resonance/player/ui/MusicSourcesScreen.kt)、[RecommendationHistoryScreen.kt](../../composeApp/src/commonMain/kotlin/com/resonance/player/ui/RecommendationHistoryScreen.kt)、[OnlineSongSearch.kt](../../composeApp/src/commonMain/kotlin/com/resonance/player/ui/OnlineSongSearch.kt)、[SemanticSongSearch.kt](../../composeApp/src/commonMain/kotlin/com/resonance/player/ui/SemanticSongSearch.kt) | 各二级/功能页面与卡片组件 | 统一居中 `widthIn(max = 1100.dp)` 与 `20dp / 40dp` 页边距、`.atelierCard()` 卡片表面、品味画像自定义圆角维度条与智能管家 `Danger` 清理按钮。 |
| [SleepTimerDialog.kt](../../composeApp/src/commonMain/kotlin/com/resonance/player/ui/SleepTimerDialog.kt)、[EditTrackDialog.kt](../../composeApp/src/commonMain/kotlin/com/resonance/player/ui/EditTrackDialog.kt)、[BatchEnrichDialog.kt](../../composeApp/src/commonMain/kotlin/com/resonance/player/ui/BatchEnrichDialog.kt)、[ShareCardDialog.kt](../../composeApp/src/commonMain/kotlin/com/resonance/player/ui/ShareCardDialog.kt)、[ScreenshotImportDialog.kt](../../composeApp/src/commonMain/kotlin/com/resonance/player/ui/ScreenshotImportDialog.kt)、[TogetherRoomComponents.kt](../../composeApp/src/commonMain/kotlin/com/resonance/player/ui/TogetherRoomComponents.kt)、[PlaylistDialogs.kt](../../composeApp/src/commonMain/kotlin/com/resonance/player/ui/PlaylistDialogs.kt)、[AssistantAudioDeletionDialog.kt](../../composeApp/src/commonMain/kotlin/com/resonance/player/ui/AssistantAudioDeletionDialog.kt) | 6 个独立原生弹窗与 4 个删除确认对话框 | 6 个独立 `Dialog` 全部包裹 `CompositionLocalProvider(LocalGlassState provides null)` 并改为静态 `CanvasElevated + Divider` 表面，修正 `EditTrackDialog` 与 `ShareCardDialog` 按钮前景色；所有删除确认按钮改用 `ResonanceColors.Danger`。 |
| [styles.xml](../../composeApp/src/androidMain/res/values/styles.xml) | `Theme.Resonance` (`android:windowBackground`) | 启动窗口底色从 `#0B0E14` 对齐为深色黑曜画布 `#0E0E10`。 |

## 修改前后行为

| 场景 | 修改前 | 修改后 |
| --- | --- | --- |
| 主壳主题与表面层级 | 浅色冷灰/玫红搭配双悬浮胶囊及局部无边透明卡片，存在残留绿色控件常量 | 浅色暖雪花石纸（`#F6F4F0`）与深色黑曜石（`#0E0E10`），朱红/珊瑚与钴蓝统一；1dp 细边 `atelierCard` 贯穿曲库、设置与二级页 |
| 歌单与缺失封面展示 | 歌单固定单张封面，缺图曲目使用单一太阳等高线插画 | 含 4 首及以上不同封面曲目的歌单自动展示 2×2 拼贴；缺图曲目按曲目种子确定性生成 8 配色 × 4 声学几何封套 |
| 桌面主框架与底部播放坞 | 232dp 侧栏、默认 Material3 粗块音量滑块、集合详情页叠加显示「音乐库」顶栏 | 244dp 品牌侧栏 + 76dp 全宽底部播放坞（顶边 2.5dp 进度条 + `SoftRefinedVolumeSlider` 细轨音量条）；非多选集合详情页独立掌控顶栏 |
| 全屏沉浸播放器与歌词 | 顶栏标题受右侧多按钮挤压偏左、白三角无底播放键、带边框歌词焦点、封面预览边缘易切字 | 顶栏对称居中对齐下拉把手、60dp 暖象牙播放圆盘与真实切歌图标、无框横向柔光 + 3dp 左侧竖条焦点、上下 `DstIn` 边缘渐隐与胶囊化歌词工具 |
| 删除操作与独立原生弹窗 | 删除确认按钮混用主强调色；6 个原生 `Dialog` 未显式清空 `LocalGlassState` | 全部删除按钮与菜单统一 `ResonanceColors.Danger`；独立原生弹窗统一隔离宿主 Haze 并使用静态主题表面 |

## 平台与数据影响

- **共享与三端影响**：所有 UI 改造位于 `commonMain`（共享 Android / Windows / Linux）及 Android 启动主题底色 `androidMain/res/values/styles.xml`，未改动任何桌面/Android 平台服务契约、音频解码管线或 `website/` 目录。
- **数据、偏好与安全边界**：**无破坏性变更**。不修改 `version.properties`、偏好键名（含 `ThemeMode`、`WeatherPalette.Sage / IceBlue` 枚举值）、曲库/歌单持久化格式、`.resonance` 同步包校验与回退逻辑、播放防竞争机制、凭据存储、删除确认流程及 AI/网络请求策略。所有语义文案与测试标签完整保留。

## 验证

所有重量级编译与测试均遵循 `docs/AI-WORKFLOW.md` 本机资源预算，在共享构建锁与 `systemd-run --user --scope`（`MemoryMax=2G`、`MemoryHigh=1536M`、`MemorySwapMax=0`，启动前 `MemAvailable` 7.3–10.5 GiB）内顺序执行：

| 命令或操作 | 环境 | 结果 |
| --- | --- | --- |
| Kotlin/Compose 2.3.20 CLI 桌面主源码与测试全量编译（`common=120, jvm=58, desktop=28, commonTest=23, desktopTest=77`，共 306 文件） | Linux x86_64，JDK 17，2G/1.5G/Swap0 cgroup | **通过**（`COMPILE_OK`，0 错误） |
| 桌面 UI 回归套件 9 个测试类（`PlaybackSettingsDialogTest` 2、`LyricsMotionTest` 4、`LyricsPlayerRegressionTest` 1、`PlayerExpansionTest` 5、`PlayerRecoveryUiTest` 3、`RecommendationPlaybackTest` 4、`SyncPackageDialogTest` 3、`SyncPackageFeedbackTest` 3、`LyricsTranslationControllerTest` 6） | Linux x86_64，`ImageComposeScene` 离屏渲染，2G/1.5G/Swap0 cgroup | **通过**（31 / 31 通过，0 失败） |
| `UiVisualContractTest` 全部 22 项常态用例（含 70 场景全矩阵、WCAG 主题可读性、播放器分栏、手机/桌面全页浅深色截图、短横屏与反向切页） | Linux x86_64，`ImageComposeScene` 离屏渲染，2G/1.5G/Swap0 cgroup | **通过**（22 / 22 通过，0 失败；另有 1 项 `renderNativeMotionPreviewFrames` 按 `RESONANCE_UI_ARTIFACTS` 条件跳过） |
| Gradle `:composeApp:processDebugResources` + `:composeApp:generateActualResourceCollectorsForAndroidMain` + Kotlin/Compose 2.3.20 CLI Android 全量源码编译（`common=120, jvm=58, android=20`，共 198 文件，含 AAPT2 校验 `styles.xml`） | Linux x86_64，JDK 17，Android SDK 36，2G/1.5G/Swap0 cgroup | **通过**（`BUILD SUCCESSFUL` + `ANDROID_COMPILE_OK`，0 错误） |
| 人工逐图核查第二轮原生渲染截图（`composeApp/build/lyrics-regression/`、`apple-player-2026-11-05/screenshots/`、`light-ui-native/screenshots/`、`macos-layout/screenshots/` 中 20+ 张 390dp / 720×360 / 1280×800 / 1440×900 浅深色页面） | 本机离屏 PNG 产物实看 | **通过**（15 项首轮检出问题均确认收敛消除） |
| 完整 Gradle `:composeApp:desktopTest` 进程内编译 | Linux 2G/1.5G/Swap0 cgroup | **因环境受阻**（Gradle Daemon + 进程内编译器峰值 1.7G 超过 `MemoryHigh=1536M` 触发内核限速，已终止并改用同版本 Kotlin/Compose 2.3.20 CLI 完成完整编译与定向回归） |
| Android 实机安装 / `androidInstrumentedTest` / Windows 运行时 / `DesktopWindowLifecycleTest` 交互式桌面窗 / Release APK & 桌面安装包打包 | 未连接实机与未请求发布 | **未执行** |

## 限制与后续

- 本次已完成桌面与 Android 全量 Kotlin/Compose 源码编译、Android 资源编译及 53 项桌面原生 UI 离屏回归与多视口截图复检；尚未在真实 Android 手机（含触摸拖拽、软键盘弹出、系统手势返回及实机帧率）、Windows 运行时或 Linux 交互式桌面窗口实测，也未生成正式 Release 安装包或更新官网截图。
- 后续如需将本轮界面打包安装到手机或 Linux/Windows，可在满足内存预算时分别独立执行对应平台构建与设备回归，勿将 Android 与桌面 Release 合并执行。

## 同步更新的文档

- [设计规范 DESIGN.md](../../DESIGN.md)：同步更新为 Atelier Acoustics（声学画廊）现行色板、排印、圆角、布局断点、材质隔离与组件契约。
- [详细开发说明 docs/DEVELOPMENT.md](../DEVELOPMENT.md)：更新顶部核对日期并在第 11 节新增 `2026-11-21` Atelier Acoustics 原生 UI 重构基线。
- [文档入口 docs/README.md](../README.md)：更新顶部最新状态与变更索引表。
