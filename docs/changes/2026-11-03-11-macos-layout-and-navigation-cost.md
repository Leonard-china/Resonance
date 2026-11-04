# 2026-11-03 — 按确认稿重排原生布局并降低切页绘制开销

- 修改日期：2026-11-03（Asia/Shanghai）；2026-11-04 完成本地交付复核
- 类型：界面重排 / 性能修复
- 适用版本：0.6.5 / 39，未升版
- 影响平台：Android、Windows、Linux 共享 Compose UI
- 关联：[前轮材质](2026-11-03-09-app-wide-frosted-material.md)、[歌词焦点](2026-11-03-10-restore-lyric-focus.md)、[本轮确认图](../../design-system/resonance/mockups/2026-11-03-approved-macos-mobile.png)

## 需求或问题

用户明确不满意原布局，要求先给方案和图片，再确认保留 macOS 风格、按最后一版图片实现并保持流畅动画。Android 首次切页明显卡顿，用一段时间后好转。用户接入荣耀 Android 16 / 120Hz 手机用于实测。

## 原因与依据

原主内容、底栏、迷你播放器各自采样模糊；主页面转场还调整内容尺寸，助手进入时隐藏迷你播放器，改变剩余视口高度。歌词、队列继续叠加整块玻璃，造成容器过多。

修改前真实 Release 包三轮强停后导航，first 窗口 267/785（34.01%）超时，warm 263/780（33.72%）。Perfetto 中一个 RenderThread DrawFrames 耗时 55.60ms，其中 Running 55.01ms；主线程 postAndWait 睡眠 42.28ms，由 RenderThread 唤醒。首次助手页也有 33.02ms 重组及 17.70ms 测量。该证据支持削减重复绘制，不能证明所有卡顿均由单个模糊参数或 JIT 导致。

## 具体修改

| 文件 | 关键符号 | 修改与目的 |
| --- | --- | --- |
| [LibraryShell.kt](../../composeApp/src/commonMain/kotlin/com/resonance/player/ui/LibraryShell.kt) | compact dock、DestinationContent、MiniPlayer | 紧凑窗口合并播放器和四项导航为单个玻璃底座，助手也保留播放器；移除整页玻璃外套，固定转场尺寸，主导航仅短距离横移与淡入淡出 |
| [GlassSegmentedControl.kt](../../composeApp/src/commonMain/kotlin/com/resonance/player/design/GlassSegmentedControl.kt) | GlassSegmentedControl | 曲库和播放器共用移动选中面；在布局阶段读取位置，保留原生 tab/selection 语义、键盘与减少动效、教程锚点 |
| [PlaylistGrid.kt](../../composeApp/src/commonMain/kotlin/com/resonance/player/ui/PlaylistGrid.kt) | PlaylistListTab | 每日推荐缩为图文横条；我的歌单标题和新建入口；保留惰性网格与滚动位置，移除重复的大新建占位格 |
| [NowPlayingOverlay.kt](../../composeApp/src/commonMain/kotlin/com/resonance/player/ui/NowPlayingOverlay.kt) | PaneSwitcher、PaneContent、phone branch | 手机分栏开关移至顶部，歌词/队列去掉包裹面板，歌名在正文上方；底部进度和播放控制固定，增加真实音量、音效、定时入口 |
| [MusicFeatureScreens.kt](../../composeApp/src/commonMain/kotlin/com/resonance/player/ui/MusicFeatureScreens.kt) | AssistantScreen | 轻量顶栏与单行可增长输入区、用户气泡和开放式助手文字；键盘效果按显隐变化触发，不随每一像素重启滚动；保留截图、教程、会话和回复模型溯源 |
| [ResonanceTheme.kt](../../composeApp/src/commonMain/kotlin/com/resonance/player/design/ResonanceTheme.kt) | PageSlideSpec | 主页面使用 220ms 现有 ease-out 曲线；分段选中面 180ms、导航选中颜色 140ms，支持打断和反向选择 |
| [GallerySurfaces.kt](../../composeApp/src/commonMain/kotlin/com/resonance/player/design/GallerySurfaces.kt) | glassControlSurface | 小型圆形操作按钮用透色底与细边，无额外模糊采样 |
| [FrostedBackdrop.kt](../../composeApp/src/commonMain/kotlin/com/resonance/player/design/FrostedBackdrop.kt) | FrostedBackdrop | 真实封面柔焦背景降低饱和度，保留封面原图，避免强烈专辑色抢走正文与控件层次 |
| [UiVisualContractTest.kt](../../composeApp/src/desktopTest/kotlin/com/resonance/player/ui/UiVisualContractTest.kt)、[LibraryNavigationTest.kt](../../composeApp/src/androidInstrumentedTest/kotlin/com/resonance/player/LibraryNavigationTest.kt) | 本轮截图、触摸与导航断言 | 实际原生组件、双主题/大字号/横屏/宽屏；更新助手保留底栏的预期 |

## 修改前后行为

| 场景 | 修改前 | 修改后 |
| --- | --- | --- |
| 曲库 | 大推荐区、页面外框、重复新建格 | 紧凑推荐横条、清楚的歌单标题和网格 |
| 进入助手 | 迷你播放器收起、视口高度变化 | 同一底座和播放器高度；IME 弹出时仍隐藏，输入不被遮挡 |
| 歌词/队列 | 内容套在玻璃容器，切换在底部 | 顶部分段开关、开放正文、底部控制台 |
| 主导航 | 全宽长距离和尺寸变化 | 固定视口、小位移和短淡入淡出 |

## 平台与数据影响

不改变平台接口、版本、库/配置/同步包格式、音频转换和删除流程。歌词仍用已修复的稳定行高与连续定位，高亮为亮色文字加淡底。Android 原生系统栏与输入法策略不改；Windows/Linux 共用布局和减少动效，不将播放器转成网页。用户手机仅覆盖安装、强停/启动、导航与截图，无清除数据、发送聊天或下载操作。

## 验证

最终本地验证完成；用户于 2026-11-04 明确要求“先完成本地交付，稍后再测”，因此最终包安装和设备复测延期。

| 命令或操作 | 环境 | 本次结果 |
| --- | --- | --- |
| `:composeApp:desktopTest`（下列过滤项） | Linux JVM / 原生 ImageComposeScene | **18 项通过**：链接解析4、模拟助手工具1、歌词动效4、视觉/交互9；11月4日逐帧等待截图稳定后再次通过 |
| `:composeApp:assembleRelease :composeApp:assembleDebug :composeApp:assembleDebugAndroidTest` | JDK17 / Android SDK36 | **通过**，包含 Release lintVital；仪器测试仅编译，未在设备执行 |
| 28个目的地/主题截图＋6个播放器分栏/主题截图 | 原生共享 Compose、合成曲库，无用户数据和收费请求 | **34张**已生成；独立审阅确认本轮修正，静态布局通过。最初播放器截图未等待转场逐帧结算，重捕后裁切消失，生产源码未因该假象改动 |
| `adb install -r` 与三轮强停后导航 | 荣耀 VKJ_AN00，Android16 / 120Hz | 第一版布局包安装和测量通过，**最终细节包未安装/未复测** |
| `apksigner verify --verbose`、`aapt dump badging`、SHA-256 | 本次最终 Release APK | **通过**，v2签名、一个签名者，0.6.5 / 39；首次签名命令缺 java PATH，设置本轮 JDK PATH 后重跑成功 |

最终桌面筛选参数（与 `settled-verify.log` 对应）：

```bash
:composeApp:desktopTest \
 --tests 'com.resonance.player.AssistantMusicLinksTest' \
 --tests 'com.resonance.player.features.MusicAssistantTest.libraryToolReturnsPlayableLinksWithoutExposingFileLocations' \
 --tests 'com.resonance.player.ui.LyricsMotionTest' \
 --tests 'com.resonance.player.ui.UiVisualContractTest.renderFrostedDestinationsThroughTheRealShell' \
 --tests 'com.resonance.player.ui.UiVisualContractTest.renderApprovedPlayerPanes' \
 --tests 'com.resonance.player.ui.UiVisualContractTest.assistantAttachmentsOnlyActOnExplicitClicks' \
 --tests 'com.resonance.player.ui.UiVisualContractTest.galleryPlayerKeepsTransportReachableWithLargeTextInShortWindow' \
 --tests 'com.resonance.player.ui.UiVisualContractTest.shortLandscapeKeepsPaneTabsAndTransportInsideViewport' \
 --tests 'com.resonance.player.ui.UiVisualContractTest.wideStageKeepsLyricsQueueAndTransportConnected' \
 --tests 'com.resonance.player.ui.UiVisualContractTest.compactAssistantKeepsTitleOnOneLineWithLargeText' \
 --tests 'com.resonance.player.ui.UiVisualContractTest.textAndSelectedRolesStayReadableAcrossAllThemes' \
 --tests 'com.resonance.player.ui.UiVisualContractTest.songInformationRemainsReachableFromPlayerMenu' \
 --no-daemon --max-workers=2 -Dorg.gradle.jvmargs='-Xmx3g -Dfile.encoding=UTF-8' \
 -Pkotlin.compiler.execution.strategy=in-process --no-configuration-cache --console=plain
```

以上任务由 `java -classpath gradle/wrapper/gradle-wrapper.jar org.gradle.wrapper.GradleWrapperMain` 执行，JDK/SDK 用本机环境变量提供。首次编译发现两个缺失 import，补齐后通过。最初构建并发 JVM 占用过高，最终使用单个 3GiB in-process 编译与最多2个worker；没有为本机环境改项目构建配置。


第一轮源码 15 项桌面测试通过，Android Debug 编译及 Release 构建（含 lintVital）通过。第一版布局包已覆盖安装成功。三轮 first 超时率为 6.67% / 5.79% / 5.74%，warm 为 6.10% / 6.22% / 4.94%；合计 first **72/1185=6.08%**、warm **65/1132=5.74%**。这不是最终细节版的验收结果。

环境记录和 SQL 在忽略目录 `build/macos-layout-2026-11-03/`。基线/新包均按助手→一起听→工具→音乐库，每步间隔 1.25s；每次强停启动后先 first，再 warm，共三轮，第一轮同时录 Perfetto。未清理 ART profile/磁盘缓存，因此“首次”指进程重启后的首次导航，不是全新安装。观察到 8.33ms VSYNC，但 expected timeline 预算约 13.67ms，不把统计值直接换算为 FPS。

同样两遍导航窗口，主线程 postAndWait 单次均值 10.25→3.05ms，RenderThread flush layers 累计1441.57→564.55ms、flush commands 1773.65→961.32ms，重组总耗时268.83→266.39ms。嵌套slice不能相加；该证据支持绘制/等待减少的主要收益。新版仍有41.49ms渲染帧和46.98ms主线程尖峰。后续路由截图检查时发现用户已切到其他应用，该轮截图不能作为测量有效性依据，自动点击随即停止；没有使用其数据更新性能结论。

最终归档：`release/0.6.5/2026-11-04-approved-layout/Resonance-0.6.5-release.apk`，167,366,333字节，SHA-256 `50474e5b2d18206f150798798982fec9cfdfa4439a8856c0a97ddd7fb6cdd751`。同目录有说明和 `SHA256SUMS.txt`；未发布至外部服务，也没有覆盖前轮归档。

## 限制与后续

真实设备只有一台荣耀 Android 16 手机；Windows/其他 Android 机型、长时间发热和全新安装冷缓存尚未覆盖。仍需保留未达预算的帧，不能宣称稳定 120fps。合成截图用于布局验收，不是设备性能证据。

## 同步更新的文档

`docs/README.md` 索引、`docs/DEVELOPMENT.md` UI/测试定位、`DESIGN.md` 当前确认稿布局。
