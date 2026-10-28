# 2026-10-27 — 统一青绿原生 UI 层级与动效并补齐可访问交互回归

- 修改日期：2026-10-27（Asia/Shanghai）
- 类型：UI 体验优化 / 动效 / 可访问性 / 回归验证
- 适用版本：`0.6.0 / 34`，不自动升版
- 影响平台：Compose Multiplatform 共享 UI / Android / Windows
- 关联需求：用户要求基于已有 skills 全面优化 UI 与动效，并明确选择“保留现有青绿与封面氛围，统一层级、质感和动效”
- 关联历史：[上次播放页重构回退](2026-10-26-04-revert-wavy-progress-and-now-playing-refactor.md)、[恢复切页弹性与顶边背景](2026-10-17-34-restore-spring-bounce-and-eliminate-top-edge-flicker.md)、[本地 M3 技能安装](2026-10-27-01-install-material-design-3-ui-skill.md)

## 需求或问题

现有页面保留了青绿、封面和环境光氛围，但导航、标签、播放控件与设置行的尺寸、选中态和动效各自实现。曲库和播放器标签存在叠加缩放，部分播放按钮/进度条不遵循减少动效，正在播放指示器每帧读取布局状态。助手顶部多个操作挤在一行，窄屏与大字号缺少空间。

本轮原生渲染还实际发现：深色模式中没有显式指定颜色的标题沿用黑色；封面上的半透明标签对比度不稳定；150% 字号下曲库标题被按钮挤成竖排、音量百分比折行。滑块回归也检出了自定义小手柄会使实际语义区域缩窄的问题。

本次保留播放器原有结构、封面主体、青绿色系与线性进度条，改动集中在共享设计规范、公共组件及主要页面。

## 原因与依据

使用本地 `material-design-3-ui` 与 `frontend-design` 组织层级和语义主题；用 `review-animations` / `STANDARDS.md` 与 Google `jetpack-compose-performance` 的动画、绘制、惰性列表参考核对实现。原生滑块插槽按 [Compose Multiplatform Slider 文档](https://kotlinlang.org/api/compose-multiplatform/material3/androidx.compose.material3/-slider.html) 核对，绘制阶段状态与画刷缓存参照 [Android 图形修饰符说明](https://developer.android.com/develop/ui/compose/graphics/draw/modifiers)。没有引入 Web UI、动效库或新的应用依赖。

视觉计划沿用现有六个主要角色：浅色底 `#F1F6F3`、深色底 `#111A1D`、浅表面 `#FCFDFC`、浅色主色 `#2F7467`、深色主色 `#8CD0BC`、浅色正文 `#1C302B`。通过留白、字阶和少量容器区分层级；专辑封面仍为播放器主体。审查时拒绝了再次改成暖纸感/波浪条或给所有列表行增加形变的做法，与用户选择及历史回退保持一致。

## 具体修改

| 文件 | 关键符号 | 实际修改 |
| --- | --- | --- |
| [ResonanceTheme.kt](../../composeApp/src/commonMain/kotlin/com/resonance/player/design/ResonanceTheme.kt) | `ResonanceTheme`、`ResonanceMotionTokens`、`resonancePressable`、`resonanceGlass` | 补齐字阶、Material 圆角、默认内容色、可读描边与不透明主色容器；主题模式同时切换背景/正文，天气配色保留插值；集中 120/160/220ms 反馈时间；减弱默认玻璃边缘与阴影；减少动效和键盘输入不触发缩放 |
| [ResonanceSelectionTab.kt](../../composeApp/src/commonMain/kotlin/com/resonance/player/ui/ResonanceSelectionTab.kt) | `ResonanceSelectionTab` | 新增曲库/播放器共同使用的选中态、焦点描边、悬停反馈、Tab 语义和 48dp 高度 |
| [LibraryShell.kt](../../composeApp/src/commonMain/kotlin/com/resonance/player/ui/LibraryShell.kt) | 主导航、`LibraryTabRow`、`TrackRow`、`MiniPlayer`、`NowPlayingIndicator`、`EmptyLibrary` | 原生导航使用 Material 选中指示；窄屏次要操作进入菜单并保留教程目标；曲目行预留状态标记、不缩放整行；迷你播放器按歌曲 ID 过渡且减少位移；等高静态标记保留暂停/后台状态；播放指示逐帧只绘制；空状态限制宽度并区分操作优先级 |
| [LibraryTabTransition.kt](../../composeApp/src/commonMain/kotlin/com/resonance/player/ui/LibraryTabTransition.kt) | `LibraryTabTransition` | 单棵列表保持不变，切换位移从 120dp 缩至 32dp、时间从 280ms 缩至 220ms；减少动效时立即完成 |
| [NowPlayingOverlay.kt](../../composeApp/src/commonMain/kotlin/com/resonance/player/ui/NowPlayingOverlay.kt) | `PaneSwitcher`、`PaneContent`、`ArtworkPane`、`VolumeAndSpeedRow`、`QueuePane`、歌词居中滚动 | 统一分栏、淡入淡出和减少动效；封面过渡持有各自歌曲对象；音量区随窗口/字号分行；倍速取消瞬时大缩放并保留原选项；队列遵循减少动效；歌词居中滚动缩至 280ms，原有手动浏览暂停跟随逻辑保留 |
| [SoftNeumorphicButtons.kt](../../composeApp/src/commonMain/kotlin/com/resonance/player/ui/common/SoftNeumorphicButtons.kt) | 播放/切歌/辅助按钮、`RefinedSlider` | 保留青绿渐变及圆盘，轻按压改为 0.97、120ms；补播放名称、焦点及最小命中尺寸。自绘指针识别替换为原生 Slider，保留线性细轨道、拖动预览与回调；小手柄的测量容器为 20×48dp，避免语义区域只剩可见手柄高度 |
| [FluidAmbientCanvas.kt](../../composeApp/src/commonMain/kotlin/com/resonance/player/ui/common/FluidAmbientCanvas.kt) | `FluidAmbientCanvas` | 用 `drawWithCache` 缓存三个光场画刷，绘制时仅平移；保留播放/前台/减少动效门槛，种子用于初始相位 |
| [ToolsScreens.kt](../../composeApp/src/commonMain/kotlin/com/resonance/player/ui/ToolsScreens.kt) | `SettingRow`、`SectionHeader`、`ThemeOption`、`ToolPage` | 统一图标底衬、悬停与焦点，说明可换行；主题选项增加选择语义；页标题按剩余宽度布局 |
| [MusicFeatureScreens.kt](../../composeApp/src/commonMain/kotlin/com/resonance/player/ui/MusicFeatureScreens.kt) | `AssistantScreen` | 960dp 阅读宽度、可换行操作、发送方位置和消息类型复用；空会话可填充草稿；输入增加明确标签；减少动效时即时滚动 |
| [PlaylistGrid.kt](../../composeApp/src/commonMain/kotlin/com/resonance/player/ui/PlaylistGrid.kt) | `PlaylistListTab` | 调整封面网格节奏与文字留白，为卡片/空态/新建项补 key 和 contentType |
| [TasteRadarScreen.kt](../../composeApp/src/commonMain/kotlin/com/resonance/player/ui/TasteRadarScreen.kt) | `TasteRadarChart` | 800ms 变为 280ms，减少动效时立即更新；六维数值在绘制阶段读取 |
| [UiVisualContractTest.kt](../../composeApp/src/desktopTest/kotlin/com/resonance/player/ui/UiVisualContractTest.kt) | 原生渲染、主题与交互回归 | 使用 Skia/Compose 实际渲染，不依赖 Android 设备；校验主题对比度、切主题首帧、选中语义、滑块最新回调和命中区域；生成五类页面在四种窗口/字号组合和两种主题下的 40 张截图 |
| [UiInteractionContractTest.kt](../../composeApp/src/androidInstrumentedTest/kotlin/com/resonance/player/UiInteractionContractTest.kt) | Android 设备回归入口 | 新增标签选择、48dp 触控范围、滑块 SetProgress 和更新回调检查；设备执行与编译结果分别记录 |

### 动效审查

| Before | After | Why |
| --- | --- | --- |
| 曲库/播放器标签同时做选中缩放与按压缩放 | 统一容器选中态、焦点描边与轻按压 | 降低高频切换时文字跳动，静态状态也可辨识 |
| 播放控件 0.90 缩放、低阻尼弹簧 | 0.97 / 120ms；减少动效和键盘无缩放 | 反馈更短、幅度更小，保留用户操作响应 |
| 迷你播放器换歌大幅弹入、封面出入放大 | 短位移与淡入；减少动效只短淡入淡出 | 减轻频繁切歌的空间跳跃，快速重定向沿用 Compose 动画状态 |
| 声柱每帧重组和改变布局高度 | 固定尺寸 Canvas 中读相位 | 动态状态留在绘制阶段 |
| 光晕每帧重建径向画刷 | 缓存画刷并平移 | 减少持续动画中的画刷分配；未据此宣称实测帧率提升 |
| 部分滑块、队列和雷达忽略减少动效 | 原生滑块状态、队列即时滚动、雷达即时结果 | 统一偏好覆盖范围 |

结论：代码层面的主要高频形变、减少动效遗漏和绘制阶段问题已处理。Android 系统栏、实际手指手势、硬件键盘和设备帧率仍需真机审查，不能把桌面截图视为最终设备动效验收。

## 修改前后行为

- 主色、天气选择、导航目的地和播放器基本结构延续；页面字阶、圆角、选择反馈和默认内容色统一。
- 窄屏曲库通过更多菜单进入新建/多选；搜索、任务仍可直接到达，教程新建步骤仍呈现原按钮。
- 播放进度与音量继续实时调用现有回调，补原生进度语义和更大命中范围；版本、速度选项和播放算法未改变。
- 减少动态效果覆盖按钮、标签、播放器分栏、封面、队列和雷达；常规主导航保留历史弹性，键盘即时导航不做尺寸动画。
- 助手空态点击建议只填草稿，仍由用户点击发送；不自动提交 AI 请求。

## 平台与数据影响

共享 UI 改动同时影响 Android 和 Windows。未修改平台接口、音频转换/播放引擎、任务防竞争、偏好键、持久化顺序、数据库/原子文件、同步包协议、歌词时间轴解析或音频文件。没有新增权限、付费请求或依赖；本次构建调试包，没有正式发布或升版。原有工作区的 `HorizontalPageSwipeUnitTest.kt` 修改及前序技能安装文档保留。

渲染回归使用仓库 PreviewLibrary 与仅支持所需只读方法的代理服务，不连接用户曲库、网络或 API。首次观察旧桌面产物使用独立 `RESONANCE_DATA`，仅跳过该隔离环境教程；正式验证以本次源码编译及原生渲染为准，不把旧产物作为本次构建结果。

## 验证

| 命令或操作 | 结果与范围 |
| --- | --- |
| 双端 `compileKotlinDesktop` / `compileDebugKotlinAndroid` | 首轮编译通过；后续响应式调整的最终编译结果见下项 |
| `:composeApp:allTests :composeApp:lintRelease :composeApp:assembleDebug :composeApp:compileDebugAndroidTestKotlin --no-configuration-cache --console=plain` | 通过，3m31s。桌面 189 项，Android Debug/Release JVM 单元测试各 39 项，均 0 失败 / 0 错误 / 0 跳过；设备测试代码编译通过。Release Lint 0 错误、105 个警告，本次不批量处理无关警告；Debug APK 已生成。单元测试中已有的环境条件分支不等同于真实服务/设备集成通过 |
| `UiVisualContractTest` 原生渲染、交互、对比度和主题首帧 | 通过，4 项均无失败；早期检查实际检出并修复了选中标签对比度和滑块命中区域问题。最后补充了默认标题内容色断言、修正截图夹具的主题选中状态；单独重跑 14s 通过 |
| `:composeApp:desktopTest --tests 'com.resonance.player.ui.UiVisualContractTest' :composeApp:assembleDebug --no-configuration-cache --console=plain` | 通过，1m15s；仅整理助手源码缩进后，重新编译双端共享 UI、生成截图并更新调试包。单独的标题内容色断言随后也已通过 |
| 手机 360×800、宽屏 1280×800、手机 150% 字号、横屏 640×360；深/浅主题；曲库/播放器/工具/助手/设置 | 共生成 40 张原生截图于忽略的 `composeApp/build/ui-polish/screenshots/`；已目视核对代表性的手机播放/曲库、深色宽屏、工具设置、助手、大字号与短横屏。大字号标题和百分比排版问题已复查修正，短横屏的播放器右栏保留滚动 |
| `adb devices` | 环境受阻：列表为空，没有 Android 真机/模拟器；Android instrumentation 不能记为通过 |
| 真机手势、系统栏、屏幕阅读器、硬件键盘、帧率/能耗 | 未执行；没有连接设备，未录制 Perfetto 或做性能基准 |
| `git diff --check`、新记录/索引链接、用户已有测试文件哈希 | 通过；本记录 18 个本地链接有效、索引唯一，4 个新增文件无行尾空白；原有测试文件与本轮开始时一致，版本仍为 0.6.0 / 34 |

本轮编译/回归输出及 `verification-summary.json` 保留在忽略的 `build/ui-polish-2026-10-27/`；全套测试计数在后续定点重跑覆盖报告前已读取并记录。Debug APK 位于 `composeApp/build/outputs/apk/debug/Resonance-0.6.0-debug.apk`，其本次实际文件的长度与 SHA-256 也记录在该摘要。示例原生预览拼图为 `build/ui-polish-2026-10-27/ui-preview.png`。前期编译中的导入/API 调用错误已修正；最终结果以本节最后核对为准，不将中间失败写成通过。

## 限制与后续

- Android 设备交互与实际帧率尚未验证；保留设备回归入口与既有导航/详情中途像素测试。
- 40 张图片来自真实共享 Compose 渲染和示例数据，不是 Android 系统窗口截图或截图金样比较；短横屏保留滚动以访问播放器全部控件。
- 没有为每个二级弹窗单独重构布局；公共主题和设置行会应用到使用它们的页面。特殊系统文件选择器、登录和悬浮歌词窗口不在此次视觉重构范围。

## 同步更新的文档

- [开发说明](../DEVELOPMENT.md)：第 11 节记录主题、控件、动效、窄屏行为、回归入口与核对日期，并纠正旧尺寸插值描述；第 18 节改为明确的历史发布基线，避免将当前代码误写成 0.5.0。
- [文档入口](../README.md)：新增本次变更索引，保留技能安装与历史回退记录。
