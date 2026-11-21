---
name: Resonance Clearwater（清岚）原生音乐界面
description: Kotlin / Compose Multiplatform 原生播放器；清岚薄雾浅瓷浅色与深海暮蓝深色、海沫青碧与冰川天蓝强调、苹果式弹簧动效与灵动流体舞台歌词。
colors:
  light-primary: '#0B7D67'
  light-primary-soft: rgba(11, 125, 103, 0.10)
  light-ice-blue: '#1A65C2'
  dark-primary: '#35D4B2'
  dark-primary-soft: rgba(53, 212, 178, 0.17)
  dark-ice-blue: '#6EB6FF'
  light-canvas: '#F3F7F6'
  light-canvas-elevated: '#FFFFFF'
  light-raised: '#FFFFFF'
  light-soft: '#E7F0EE'
  light-surface-subtle: '#EDF4F2'
  light-glass: '#FFFFFFEB'
  light-glass-light: '#FFFFFFD8'
  light-glass-ultra: '#FFFFFFFA'
  light-glass-border: '#FFFFFFF0'
  light-glass-border-subtle: '#0F2E281A'
  light-divider: '#DCE6E3'
  light-divider-strong: '#BFCFCC'
  light-text-primary: '#12201E'
  light-muted: '#475C59'
  light-dim: '#516764'
  light-dimmer: '#59706C'
  light-positive: '#167557'
  light-info: '#1E60A8'
  light-danger: '#B92B2B'
  dark-canvas: '#0B1214'
  dark-canvas-elevated: '#121C20'
  dark-raised: '#19262B'
  dark-soft: '#203036'
  dark-surface-subtle: '#152126'
  dark-glass: '#121D21CC'
  dark-glass-light: '#19272DB3'
  dark-glass-ultra: '#1A2930EB'
  dark-glass-border: '#D8F5EE30'
  dark-glass-border-subtle: '#D8F5EE1A'
  dark-divider: '#FFFFFF20'
  dark-divider-strong: '#FFFFFF38'
  dark-text-primary: '#EFF7F6'
  dark-muted: '#9EB6B3'
  dark-dim: '#8CA6A3'
  dark-dimmer: '#829C99'
  dark-positive: '#5CE0B8'
  dark-info: '#79BEFF'
  dark-danger: '#FF8585'
  shadow-light: '#0E2623'
  shadow-dark: '#000000'
  immersive-canvas: '#0C1317'
  immersive-canvas-elevated: '#141E24'
  immersive-raised: '#1B2830'
  immersive-soft: '#24343E'
  immersive-foreground: '#F5FAFA'
  immersive-muted: '#CFE0DF'
  immersive-dim: '#A9C0BF'
  immersive-context: rgba(245, 250, 250, 0.40)
  immersive-play-disc: '#F5FAFA'
  immersive-play-ink: '#0D171B'
typography:
  display-large:
    fontFamily: sans-serif
    fontSize: 34sp
    fontWeight: 700
    lineHeight: 42sp
    letterSpacing: -0.6sp
  display-medium:
    fontFamily: sans-serif
    fontSize: 28sp
    fontWeight: 700
    lineHeight: 36sp
    letterSpacing: -0.45sp
  display-small:
    fontFamily: sans-serif
    fontSize: 24sp
    fontWeight: 700
    lineHeight: 31sp
    letterSpacing: -0.3sp
  headline-large:
    fontFamily: sans-serif
    fontSize: 28sp
    fontWeight: 700
    lineHeight: 35sp
    letterSpacing: -0.4sp
  headline-medium:
    fontFamily: sans-serif
    fontSize: 23sp
    fontWeight: 600
    lineHeight: 30sp
    letterSpacing: -0.25sp
  headline-small:
    fontFamily: sans-serif
    fontSize: 21sp
    fontWeight: 600
    lineHeight: 27sp
    letterSpacing: -0.2sp
  title-large:
    fontFamily: sans-serif
    fontSize: 18sp
    fontWeight: 600
    lineHeight: 24sp
    letterSpacing: -0.15sp
  title-medium:
    fontFamily: sans-serif
    fontSize: 15.5sp
    fontWeight: 600
    lineHeight: 21sp
  title-small:
    fontFamily: sans-serif
    fontSize: 14.5sp
    fontWeight: 500
    lineHeight: 20sp
  body-large:
    fontFamily: sans-serif
    fontSize: 15.5sp
    fontWeight: 400
    lineHeight: 23sp
  body-medium:
    fontFamily: sans-serif
    fontSize: 14sp
    fontWeight: 400
    lineHeight: 20sp
  body-small:
    fontFamily: sans-serif
    fontSize: 12.5sp
    fontWeight: 400
    lineHeight: 18sp
  label-large:
    fontFamily: sans-serif
    fontSize: 13.5sp
    fontWeight: 600
    lineHeight: 18sp
    letterSpacing: 0.1sp
  label-medium:
    fontFamily: sans-serif
    fontSize: 12sp
    fontWeight: 500
    lineHeight: 16sp
    letterSpacing: 0.25sp
  label-small:
    fontFamily: sans-serif
    fontSize: 11sp
    fontWeight: 500
    lineHeight: 15sp
    letterSpacing: 0.3sp
  lyric-stage:
    fontFamily: sans-serif
    fontSize: 27sp
    fontWeight: 700
    lineHeight: 37sp
    letterSpacing: -0.3sp
  lyric-stage-translation:
    fontFamily: sans-serif
    fontSize: 15.5sp
    fontWeight: 400
    lineHeight: 23sp
rounded:
  button: 12dp
  field: 12dp
  artwork-small: 8dp
  artwork: 14dp
  artwork-large: 18dp
  panel: 16dp
  card: 16dp
  capsule: 999dp
  material-large: 24dp
  material-extra-large: 28dp
spacing:
  xs: 4dp
  sm: 8dp
  md: 12dp
  lg: 16dp
  xl: 20dp
  xxl: 28dp
  page-wide: 40dp
components:
  atelier-card:
    backgroundColor: '{colors.light-canvas-elevated}'
    borderColor: '{colors.light-divider}'
    rounded: '{rounded.card}'
  sidebar-selected:
    backgroundColor: '{colors.light-primary-soft}'
    textColor: '{colors.light-primary}'
    typography: '{typography.title-small}'
    rounded: '{rounded.button}'
  segmented-control:
    backgroundColor: '{colors.light-soft}'
    borderColor: '{colors.light-divider}'
    rounded: '{rounded.panel}'
    padding: '{spacing.xs}'
    height: 48dp
  segmented-selected:
    backgroundColor: '{colors.light-raised}'
    textColor: '{colors.light-primary}'
    typography: '{typography.label-large}'
    rounded: '{rounded.button}'
  favorites-entry:
    backgroundColor: '{colors.light-canvas-elevated}'
    rounded: '{rounded.card}'
    padding: 15dp
  lyric-stage-current:
    textColor: '{colors.immersive-foreground}'
    typography: '{typography.lyric-stage}'
    padding: 10dp 14dp 10dp 16dp
  lyric-stage-context:
    textColor: '{colors.immersive-context}'
    typography: '{typography.lyric-stage}'
    padding: 10dp 14dp 10dp 16dp
  player-play-disc:
    backgroundColor: '{colors.immersive-play-disc}'
    iconColor: '{colors.immersive-play-ink}'
    size: 60dp
    touchTarget: 64dp
  player-pane-capsule:
    backgroundColor: rgba(255, 255, 255, 0.09)
    selectedBackgroundColor: rgba(255, 255, 255, 0.22)
    rounded: '{rounded.capsule}'
  setting-row:
    backgroundColor: '{colors.light-canvas-elevated}'
    borderColor: '{colors.light-divider}'
    textColor: '{colors.light-text-primary}'
    rounded: '{rounded.artwork}'
    padding: 14dp 16dp
    height: 68dp
---

# Design System: Resonance — Clearwater（清岚）

## Overview

**Creative North Star: "Clearwater（清岚） × Apple Fluid Motion"**

2026-11-21（Asia/Shanghai）结合用户真机体验反馈（摒弃红色主调、追求清新通透的配色与 iOS/macOS 般丝滑灵动的交互与歌词滚动），在保持稳定架构与跨窗隔离基础上，升级为 **Clearwater（清岚）** 清新视觉与苹果式弹簧动效体系。

Android、Windows、Linux 共用 Compose UI 并保留各自平台能力；独立 `website/` 官网与匿名论坛不沿用本规范。完整保留系统字体、原生返回、焦点、IME 避让、大字缩放、播放状态与持久化兼容。

**Key Characteristics:**

- **清新通透双主题（零红色主调）**：浅色采用薄雾浅瓷（`#F3F7F6`）+ 纯白浮层卡片，深色采用深海暮蓝（`#0B1214`）；默认主强调转为海沫青碧（浅 `#0B7D67` / 深 `#35D4B2`），冰蓝天气/强调模式采用冰川天蓝（浅 `#1A65C2` / 深 `#6EB6FF`）。所有前景文字与基底/白底对比度均严格满足 WCAG AA（浅色主色配白字 `5.07:1`，深色主色配画布 `10.09:1`）。危险红（`#B92B2B` / `#FF8585`）仅严格限定于不可逆删除动作。
- **苹果式流体弹簧动效**：统一 `AppleFluidSpring`、`AppleBouncySpring` 与 `LyricScrollSpec` 弹簧体系：分段控件、底部导航栏、全屏播放器三栏胶囊均配备平滑滑行透镜；主播放键与迷你播放条具备 Play↔Pause 弹性缩放形变；收藏红心点击触发弹性跃动；全屏封面按播放/暂停呈现 `1.0f ↔ 0.92f` 弹性呼吸与专辑柔光晕轮；播放进度条按压/拖动时轨高由 `4dp` 弹性扩张至 `7dp` 并亮起圆点柔光；播放期间沉浸背景呈现 14 秒缓慢漂移的双极流体光团（暂停、退后台、展开拖动中及开启减少动效时即时静止）。
- **Apple Music 风格沉浸歌词舞台**：滚动改用阻尼弹簧 `LyricScrollSpec`；当前句在绘制层（`graphicsLayer`，锚点左中 `TransformOrigin(0f, 0.5f)`）做 `1.03f ↔ 0.96f` 光学聚焦缩放 + 无框横向柔光与 3dp 左侧竖条，零布局行高重排，周边行保持距离轻模糊与边缘 `DstIn` 渐隐。
- **高效清晰的排版与功能入口**：工具页顶部重构为 2×2 清新柔和着色的 Bento 卡片矩阵（导入音乐、歌单导入与导出、曲库智能管家、音乐品味画像）；歌单页在每日推荐可用时并排展示双列 Bento 快捷卡；歌单详情页直出「导出」按钮；歌单传输中心内置「一键快速导出」胶囊与「本机已导出的歌单文件」一键导入/分享列表。

核对入口：[主题与弹簧 token](composeApp/src/commonMain/kotlin/com/resonance/player/design/ResonanceTheme.kt)、[流体光晕背景](composeApp/src/commonMain/kotlin/com/resonance/player/design/FrostedBackdrop.kt)、[封套生成](composeApp/src/commonMain/kotlin/com/resonance/player/design/AlbumVisuals.kt)、[歌单拼贴与青蓝徽标](composeApp/src/commonMain/kotlin/com/resonance/player/ui/PlaylistArtwork.kt)、[歌词滚动与焦点](composeApp/src/commonMain/kotlin/com/resonance/player/ui/LyricScroll.kt)、[主壳](composeApp/src/commonMain/kotlin/com/resonance/player/ui/LibraryShell.kt)、[沉浸播放器](composeApp/src/commonMain/kotlin/com/resonance/player/ui/NowPlayingOverlay.kt)、[工具与歌单传输中心](composeApp/src/commonMain/kotlin/com/resonance/player/ui/ToolsScreens.kt)。

## Colors

### Primary & Semantic

- **默认主强调**：浅色海沫青碧 `#0B7D67`，深色薄荷青 `#35D4B2`；`PrimarySoft` 分别为 10% / 17% 透色。`IceBlue` 天气/冰蓝偏好使用 `#1A65C2`（浅）与 `#6EB6FF`（深），保留 `WeatherPalette.Sage` 枚举名以兼容已保存偏好。
- **状态与危险语义**：`Positive`（浅 `#167557` / 深 `#5CE0B8`）、`Info`（浅 `#1E60A8` / 深 `#79BEFF`）、`Danger`（浅 `#B92B2B` / 深 `#FF8585`）。所有删除音频与危险确认按钮统一使用 `Danger` 而非主色。
- **品牌徽标**：`ResonanceBrandMark` 采用 `#0D947B → #1F7AC8` 斜向青蓝渐变配纯白同心声波。

### Neutral & Immersive Stage

- **主壳层次**：`Canvas`（浅 `#F3F7F6` / 深 `#0B1214`）→ `CanvasElevated` → `Raised`，辅以 `Soft` / `SurfaceSubtle` 与 1dp `Divider`；曲库首页顶部叠加柔和青蓝晨雾径向微光（浅 8% / 深 11%）。
- **沉浸播放器单一真源**：`ImmersivePlayerColors` 与 `ImmersivePlayerColorScheme` 成对定义，始终保持深海青墨底（`#0C1317`）与冰晶白前景（`#F5FAFA`），不受主壳浅/深色切换影响。

**The Theme Continuity Rule.** 新页面必须读取 `ResonanceColors` 与 `MaterialTheme.colorScheme`，不得强制覆写用户的浅/深/系统/天气偏好；主题明暗原子切换，天气强调色做 260ms 平滑过渡（减少动态时即时）。

## Motion & Lyrics Rules

- **弹簧统一**：`AppleFluidSpring`（`dampingRatio = 0.78f, stiffness = 360f`）用于光学聚焦缩放与胶囊过渡；`AppleBouncySpring`（`dampingRatio = 0.56f, stiffness = 460f`）用于播放键、底栏活跃图标与按压回弹；`LyricScrollSpec`（`dampingRatio = 0.86f, stiffness = 220f`）用于歌词平滑居中滚动。
- **可访问性与节能**：全部动态效果遵循 `LocalReducedMotion`、`LocalAppForeground` 及键盘输入模式；减少动态时弹簧自动降级为 0ms 即时定位，流体光团立即静止。
- **The Stable Lyrics Rule.** 焦点变化仅在绘制层应用 `graphicsLayer` 缩放、颜色动画、左侧 3dp 竖条与无框横向柔光，绝不改变字号、字重、行高或布局高度。

## Layout & Components

- **响应式主壳**：`< 720dp` 紧凑正文 + 浮空 16dp 迷你播放卡片（平滑进度细线 + Play/Pause 形变）+ 贴底 54dp 弹簧滑块导航栏；`720–1099dp` 导航 rail；`>= 1100dp` 244dp 固定侧栏 + 64dp 顶栏 + 76dp 全宽底部播放坞。
- **歌单与工具 Bento**：歌单页顶部在每日推荐可见时并排渲染「我喜欢的音乐」（仅显示数量 `"X 首"`）与「每日推荐」双列 Bento 快捷卡；工具页顶部为 2×2 柔和渐变 Bento 矩阵 + 下方分组 `SettingRow`。
- **歌单传输中心**：歌单详情页提供顶栏一键「导出」；`SyncScreen` 提供「导出歌单 / 一键快速导出歌单胶囊 / 导入歌单 / 本机已导出 `.resonance` 文件列表（一键导入与系统分享）」。
- **跨窗弹窗隔离**：所有独立 `Dialog` 继续显式提供 `LocalGlassState provides null` 并使用静态 `CanvasElevated` + `1dp Divider` 表面。

## Do's and Don'ts

### Do:
- 使用 `ResonanceColors`、`MaterialTheme.typography`、`.atelierCard()` 与 `ResonanceMotionTokens` 中的弹簧常量。
- 仅对删除等高危操作使用 `ResonanceColors.Danger`，不在普通主色或装饰元素中恢复红色。
- 保留 48dp 最小触控区、系统返回、IME 避让、键盘快捷键、减少动态及所有存储/播放/同步校验安全边界。

### Don't:
- 不要对普通列表行、歌词行或聊天正文逐项施加跨窗或实时模糊采样。
- 不要通过修改歌词字号、字重或行高来表达当前句焦点。
- 不要将独立 `website/` 样式带入原生客户端，也不要把离屏截图或静态检查当作真实 Android/Windows 设备实测结论。
