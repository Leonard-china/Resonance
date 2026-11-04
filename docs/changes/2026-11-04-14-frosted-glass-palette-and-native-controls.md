# 2026-11-04 — 纯正毛玻璃材质透光调优、剔除残留红色主色与重构原生平整播放按键

- 修改日期：2026-11-04（Asia/Shanghai）
- 类型：UI 视觉重构 / 体验优化
- 适用版本：0.6.5 / 39（保持不变，不改变版本号）
- 影响平台：共享（Compose Multiplatform：Android、Windows、Linux）
- 关联记录或需求：用户视觉反馈与确认文档 [ui-optimization-showcase.md](../../../../../../../home/leonard/.gemini/antigravity/brain/f70baf28-ba23-47fe-b205-995217f99980/ui-optimization-showcase.md)

## 需求或问题

1. **剔除残留红色主色**：用户明确要求不再使用旧版的玫红/酒红主色（`#A53649`），要求全面采用沉稳清爽的北欧冰蓝（`IceBlue`）与矿物浅灰绿（`Sage`）双色体系，完美对接随天气自动切换的调色盘架构。
2. **恢复纯正毛玻璃透光通透感**：此前浅色模式下的背景层存在过度去饱和（饱和度被硬编码压至 35%）和极高不透明底色遮罩（86% Canvas tint），导致底层专辑色彩漫射完全被死灰覆盖，丢失了 macOS / VisionOS 级别的通透毛玻璃质感。
3. **播放按键告别“AI 假感”与 3D 浮夸球体**：原有按键在视觉上过于庞大（70dp），且在探索阶段带有伪 3D 玻璃球、同心圆环和发光外晕等浮夸假感；用户明确批评“开始/暂停按钮太假，AI味太重”。需要严格按照 Apple Music / Spotify 等顶级原生播放器的几何规范，重构成沉稳、平整、高对比度、无伪 3D 机械抖动的 58dp 实心哑光圆钮与纯白矢量三角。
4. **曲库详情头图层级**：歌单/专辑详情页封面缺少景深阴影，与浅色底面贴死。

## 原因与依据

1. `ResonanceTheme.kt` 中 `colors()` 函数在 `!blue`（`WeatherPalette.Sage`）时浅色模式硬编码 fallback 至 `Color(0xFFA53649)`，且背景漫射使用了硬编码粉红光晕；将其彻底替换为矿物灰绿 `#1E7B68`（浅色）和 `#8CD0BC`（深色），`IceBlue` 为 `#2563EB`（浅色）和 `#A9C5FF`（深色）。
2. `FrostedBackdrop.kt` 中浅色全屏模式使用了 `setToSaturation(0.35f)` 与 `tint = 0.86f`，双重压制了背景模糊层，使得毛玻璃彻底失真发灰。将其调整为饱和度 0.65f、浅色遮罩 0.58f，背景底光采用动态的主色柔光漫射。
3. `SoftNeumorphicButtons.kt` 中 `SoftHeroPlayButton` 原有 70dp 尺寸过大，按压动效中包含伪 3D 机械下沉位移（`translationY = ((1f - scale) / 0.02f) * 1.5.dp.toPx()`），且阴影深度达 6dp。将其精简为 58dp 平整圆盘、3dp 原生环境接触微阴影、纯净平滑缩放反馈（0.94f 弹性规格），并优化内嵌矢量图标尺寸。
4. `NowPlayingOverlay.kt` 中的 `TactilePlayPauseButton`、`TactileSkipButton` 和 `TactileSecondaryButton` 尺寸比例统一调整为 58dp / 46dp / 40dp，形成符合经典原生审美的视觉主次节奏。

## 具体修改

| 文件 | 关键类/函数/配置 | 具体修改与目的 |
| --- | --- | --- |
| `composeApp/src/commonMain/kotlin/com/resonance/player/design/ResonanceTheme.kt` | `colors()` | 彻底移除 `#A53649`；Sage 主色浅色定为 `#1E7B68`、深色定为 `#8CD0BC`；IceBlue 主色浅色定为 `#2563EB`、深色定为 `#A9C5FF`；柔化毛玻璃发光描边至 0.20f。 |
| `composeApp/src/commonMain/kotlin/com/resonance/player/design/FrostedBackdrop.kt` | `FrostedBackdrop()` | 移除写死的粉红渐变，改为主题自适应的 `PrimarySoft` 漫射光；饱和度从 0.35f 提升至 0.65f；沉浸浅色遮罩从 0.86f 下调至 0.58f，令毛玻璃折射通透真实。 |
| `composeApp/src/commonMain/kotlin/com/resonance/player/ui/common/SoftNeumorphicButtons.kt` | `SoftHeroPlayButton()` | 默认尺寸从 70dp 收敛至 58dp；去除伪 3D 机械 `translationY` 下沉抖动；按压使用 0.94f 弹性缩放（`Spring.StiffnessMedium`）；阴影深度调整为 3dp 自然接触阴影；图标尺寸按 58dp 优化为 28dp。 |
| `composeApp/src/commonMain/kotlin/com/resonance/player/ui/common/SoftNeumorphicButtons.kt` | `WavySlider()` | 滑块拇指阴影深度从 4dp 精细化收敛为 2dp。 |
| `composeApp/src/commonMain/kotlin/com/resonance/player/ui/NowPlayingOverlay.kt` | `TactilePlayPauseButton` / `TactileSkipButton` / `TactileSecondaryButton` | 播控主次尺寸分别调整为 58dp、46dp、40dp，构建层次分明的原生播控排版。 |
| `composeApp/src/commonMain/kotlin/com/resonance/player/ui/LibraryShell.kt` | `LibraryDetailView` | 歌单与专辑详情头部封面增加 12dp / 8dp 悬浮柔和阴影，增强质感与空间景深。 |

## 修改前后行为

| 场景 | 修改前 | 修改后 |
| --- | --- | --- |
| 浅色主题使用 Sage 或默认配色 | 部分强调文字、按钮与光晕呈现酒红色（`#A53649`） | 全局纯净呈现清爽矿物灰绿（`#1E7B68`）与冰蓝（`#2563EB`），无任何残留红色 |
| 全屏播放器毛玻璃背景 | 背景灰白平淡，专辑颜色被强制压平为 35% 饱和度且被 86% 不透明遮罩覆盖 | 呈现纯正 macOS 顶级通透磨砂玻璃质感，真实折射专辑环境光影 |
| 播放/暂停大按键视觉与交互 | 70dp 硕大圆钮，带下陷机械移位，阴影过重，AI 概念探索图呈现伪 3D 发光玻璃弹珠假感 | 58dp 极简平整哑光实心圆钮 + 纯白高精矢量三角，纯粹轻柔缩放，完全符合 Apple Music / Spotify 原生标准 |
| 歌单/专辑详情封面 | 封面紧贴浅色平面，缺乏视觉纵深 | 封面带有温润悬浮阴影，层次立体鲜明 |

## 平台与数据影响

- 共享逻辑（Compose Multiplatform）：Android、Windows、Linux 三端统一受益，界面风格与操作手感保持高一致性。
- 数据与存储：不涉及任何数据库、偏好键或配置协议变更，`version.properties` 保持不变。
- 权限与硬件：无任何额外权限或外部网络请求。

## 验证

| 命令或操作 | 环境 | 结果 |
| --- | --- | --- |
| `gradle :composeApp:desktopTest --tests 'com.resonance.player.ui.UiVisualContractTest'` | Linux x86_64 / Temurin JDK 17 | **通过**（19 项原生视觉断言全通过，包括语义树、触控目标最小尺寸与视口可达性） |
| `gradle :composeApp:desktopTest --tests 'com.resonance.player.ui.LyricsMotionTest'` | Linux x86_64 / Temurin JDK 17 | **通过**（5 项歌词连续动效测试全通过） |
| `gradle :composeApp:desktopTest --tests 'com.resonance.player.ui.*'` | Linux x86_64 / Temurin JDK 17 | **通过**（UI 包下 31 项全量测试全部 PASS） |
| `gradle :composeApp:assembleRelease` | Linux x86_64 / Temurin JDK 17 / Android SDK 36.0.0 | **通过**（2m07s，成功构建包含最新 UI 的 Release APK） |
| `apksigner verify --verbose` | 本次新 APK `Resonance-0.6.5-release.apk` | **通过**（v2 签名校验通过，SHA-256：`e67ee588cd6505038d6c4376fd2a1168a13335cb2366fb67b7146bb466bf6220`） |

## 限制与后续

本次范围内已全部满足用户关于“去除红色”、“去除发光”、“真实毛玻璃透光”与“播放按键告别 AI 假感”的要求，无已知未完成项。

## 同步更新的文档

- [docs/README.md](../README.md)：新增本次视觉与材质调优记录索引。
- [docs/DEVELOPMENT.md](../DEVELOPMENT.md)：核对当前调色盘与毛玻璃材质规范。
