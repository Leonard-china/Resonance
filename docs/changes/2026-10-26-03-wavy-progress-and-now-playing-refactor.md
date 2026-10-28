# 2026-10-26 — 全屏播放页温润雅致重构与标志性双谐波波浪进度条设计

- 修改日期：2026-10-26（Asia/Shanghai）
- 类型：功能 / 重构 / UI 设计
- 适用版本：`0.6.1 / 35`（递增版本）
- 影响平台：共享（Compose Multiplatform）/ Android / Windows
- 关联记录或需求：[安装 Android 与 Frontend Design Skills 记录](2026-10-26-02-install-android-and-frontend-design-skills.md)、[v0.6.1 发布说明](../RELEASE_NOTES-0.6.1.md)

## 需求或问题

用户要求将 Resonance 打磨为具有正式发布品质的独立音乐播放器，遵循自然温润、精致克制的视觉方向：
1. 主色调统一为鼠尾草绿（Sage Green），背景采用暖米白/柔和墨夜，排版具有呼吸感，建立 4dp 基础间距节奏与清晰的视觉层级。
2. 彻底去除机械粗糙的 AI 模板感设计：消除高反差玻璃拟态描边、霓虹放射发光阴影与 3D 渐变球形按钮。
3. 打造 Resonance 标志性视觉特征——**双谐波波浪进度条**：
   - 采用柔和连贯的波形矢量线条，替代传统单调直线进度条；
   - 已播放部分为鼠尾草绿（Sage Green），未播放部分为低对比度浅灰绿；
   - 珍珠质感圆形进度旋钮，自适应随波形正弦起伏沿波谷/波峰移动；
   - 支持毫秒级点击定位（Tap-to-seek）与实时平滑拖拽（Drag-to-seek），时间戳与进度绝对同步；
   - 确保无障碍触摸区域（$\ge 44\text{dp}$）与 Skiko / Compose Canvas 高性能无卡顿渲染（单帧开销 $< 0.1\text{ms}$）。
4. 严格限制实施范围：本轮仅重构全屏播放页（Now Playing Overlay）及其必要的局部公共触觉控件，严禁破坏播放引擎、扫描、队列、导航及状态管理。

## 原因与依据

1. 依据 `.agents/skills/frontend-design/SKILL.md`：避免无目的性的发光渐变与高透明度毛玻璃卡片堆叠，采用有品牌温度的专属配色（Sage Green + Warm Ivory/Oatmeal），强化内容主体（专辑艺术封面）与功能语义。
2. 依据 `.agents/skills/styles/SKILL.md`：组件尺寸规范化，触控区域严格满足 WCAG / Android $\ge 44\text{dp}$ 标准；主播放按钮 64dp、切歌按钮 48dp、辅助控制 44dp。
3. 波形数学模型依据：采用双谐波正弦+余弦调和叠加：
   $$y(x) = y_{center} + A_1 \sin\left(\frac{2\pi x}{\lambda_1}\right) + A_2 \cos\left(\frac{2\pi x}{\lambda_2}\right)$$
   其中 $\lambda_1 = 34\text{dp}, A_1 = 3.4\text{dp}, \lambda_2 = 68\text{dp}, A_2 = 1.2\text{dp}$，步进 $3\text{px}$ 离散生成平滑连续贝塞尔路径，波形自然生动且兼顾极低 CPU/GPU 开销。

## 具体修改

| 文件 | 关键类/函数/配置 | 具体修改与目的 |
| --- | --- | --- |
| [`SoftNeumorphicButtons.kt`](../../composeApp/src/commonMain/kotlin/com/resonance/player/ui/common/SoftNeumorphicButtons.kt) | `SoftHeroPlayButton` | 摒弃原先 72dp 渐变球体与发光投影，重构为 64dp 纯粹雅致的鼠尾草绿触觉圆形按键，按下时提供轻微触觉下沉动效与柔和自然阴影。 |
| [`SoftNeumorphicButtons.kt`](../../composeApp/src/commonMain/kotlin/com/resonance/player/ui/common/SoftNeumorphicButtons.kt) | `SoftRefinedPlaybackScrubber` | 实现标志性波浪进度条：双谐波波形绘制、鼠尾草绿已播段、浅灰绿未播段、沿波形跟随的珍珠质感旋钮、点击定位与平滑拖动手势，支持 `contentDescription` 与 `stateDescription`。 |
| [`SoftNeumorphicButtons.kt`](../../composeApp/src/commonMain/kotlin/com/resonance/player/ui/common/SoftNeumorphicButtons.kt) | `SoftSkipButton` | 重构 48dp 切换上下曲按键，采用克制微投影与暖色底衬，提供温润反馈。 |
| [`SoftNeumorphicButtons.kt`](../../composeApp/src/commonMain/kotlin/com/resonance/player/ui/common/SoftNeumorphicButtons.kt) | `SoftUtilityButton` | 重构 44dp 辅助功能按键（随机、单曲循环、收藏、更多），处于激活状态时在按键底部展示鼠尾草绿小圆点指示器。 |
| [`SoftNeumorphicButtons.kt`](../../composeApp/src/commonMain/kotlin/com/resonance/player/ui/common/SoftNeumorphicButtons.kt) | `SoftRefinedVolumeSlider` | 重构微型浮动音量调节条，3.2dp 鼠尾草绿细腻滑轨替代原粗重滑块。 |
| [`NowPlayingOverlay.kt`](../../composeApp/src/commonMain/kotlin/com/resonance/player/ui/NowPlayingOverlay.kt) | `NowPlayingOverlay` | 降低环境背景模糊遮罩强度至 0.55f，避免泛光刺眼；顶部下拉抓手与按键精简为 40dp 极简圆形；专辑封面赋予 24dp 优雅圆角与 8dp 柔和环境光漫反射；重新编排歌名、歌手与音质标签的纵向字阶呼吸节奏。 |
| [`WavyProgressScrubberTest.kt`](../../composeApp/src/desktopTest/kotlin/com/resonance/player/ui/WavyProgressScrubberTest.kt) | `WavyProgressScrubberTest` | 新增针对波浪进度条的单测套件：验证波形振幅边界、连续性、进度分割比例计算、时间戳格式化与鼠尾草绿色值的一致性。 |
| [`HorizontalPageSwipeUnitTest.kt`](../../composeApp/src/desktopTest/kotlin/com/resonance/player/features/HorizontalPageSwipeUnitTest.kt) | `appVersionAndHighlightsGeneratedFromReleaseNotes` | 同步更新版本与更新要点单测断言至 `0.6.1`。 |
| [`version.properties`](../../version.properties) | `versionName`, `versionCode` | 递增版本至 `0.6.1 / 35`。 |
| [`RELEASE_NOTES-0.6.1.md`](../RELEASE_NOTES-0.6.1.md) | Release Notes | 编写 v0.6.1 完整发布说明与校验锚点。 |

## 修改前后行为

| 场景 | 修改前 | 修改后 |
| --- | --- | --- |
| 全屏播放页进度条 | 普通水平直线进度条，拖拽反馈生硬 | 双谐波动态起伏波浪线条，鼠尾草绿已播与浅灰绿未播，珍珠旋钮随波形高低自然起伏，拖拽极度跟手流畅 |
| 主播放/暂停按键 | 72dp 带有霓虹蓝色/青色渐变高光的厚重发光球 | 64dp 温润沉稳的鼠尾草绿微触感实体按键，精致阴影与克制沉降动画 |
| 播放控制与辅助按键 | 玻璃拟态强对比白边框，图标大小不一 | 统一 48dp/44dp 尺寸规范，激活时显示精致的鼠尾草绿圆点指示 |
| 专辑封面与整体氛围 | 强对比度毛玻璃大卡片包裹，背景模糊过曝发白 | 专辑封面作为核心视觉主体凸显，24dp 圆角与自然漫反射投影，深浅色主题均保持极高艺术品质与舒适度 |

## 平台与数据影响

- **共享逻辑**：仅重写全屏播放页与配套触控控件的 UI 渲染层与手势计算，不修改音频流解码、任务防竞争队列、音量写入、歌词同步解析等核心播放逻辑。
- **Android / Windows**：双端 Compose Multiplatform 共享同一套原生渲染实现，无特定平台专有入侵代码。
- **数据兼容性**：不改变数据库、配置文件结构与同步包协议，旧版本升级平滑无感。

## 验证

| 命令或操作 | 环境 | 结果 |
| --- | --- | --- |
| `./gradlew.bat :composeApp:compileKotlinDesktop` | Windows 11, JDK 17 | 通过（1m 17s，无编译警告） |
| `./gradlew.bat :composeApp:desktopTest` | Windows 11, JDK 17 | 通过（190 个单元测试全部通过，耗时 49s） |
| `./gradlew.bat :composeApp:createDistributable` | Windows 11, JDK 17 | 通过（49s 生成可执行分发包） |
| 桌面运行时启动验证 | Windows 11, Compose Desktop | 通过（成功启动应用，验证无闪退，主界面与搜索页面交互正常） |
| `./gradlew.bat :composeApp:allTests :composeApp:lintRelease :composeApp:assembleRelease :composeApp:packageReleaseExe` | Windows 11, JDK 17 | 通过（耗时 11m 11s，107 个任务全部通过，成功产出双端正式包并生成 SHA-256 校验清单） |

## 限制与后续

- 本轮严格按照第一阶段范围要求，仅实施了全屏播放页及必要局部公共组件的温润重构。
- 首页歌单网格、搜索页、音乐助手与工具箱等其余二级页面的统一设计语言可在后续轮次逐步演进。

## 同步更新的文档

- `docs/README.md`：更新版本号至 `0.6.1 / 35`，并在变更索引中追加本记录。
- `docs/DEVELOPMENT.md`：核对日期更新至 2026-10-26，记录当前版本为 `0.6.1 / 35`。
- `docs/RELEASE_NOTES-0.6.1.md`：新增 v0.6.1 发布说明文件。
