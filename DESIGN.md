---
name: Resonance 原生参考音乐界面
description: Kotlin / Compose Multiplatform 原生播放器；灰白主壳、双胶囊导航与白色沉浸音乐舞台。
colors:
  light-primary: '#C91D40'
  light-primary-soft: rgba(201, 29, 64, 0.10)
  light-ice-blue: '#2563EB'
  dark-primary: '#FF6B83'
  dark-primary-soft: rgba(255, 107, 131, 0.16)
  dark-ice-blue: '#A9C5FF'
  light-canvas: '#F2F2F7'
  light-canvas-elevated: '#FFFFFF'
  light-raised: '#FFFFFF'
  light-soft: '#F5F5F8'
  light-glass: '#FFFFFFD9'
  light-glass-light: '#FFFFFFC7'
  light-glass-ultra: '#FFFFFFEB'
  light-glass-border: '#FFFFFFDD'
  light-glass-border-subtle: '#FFFFFF90'
  light-divider: '#D1D1D6'
  light-text-primary: '#1C1C1E'
  light-muted: '#55555D'
  light-positive: '#2F7467'
  light-info: '#365FA8'
  light-error: '#BA1A1A'
  dark-canvas: '#0B0B0C'
  dark-canvas-elevated: '#1C1C1E'
  dark-raised: '#2C2C2E'
  dark-glass: '#1C1C1EB8'
  dark-glass-light: '#2C2C2EA6'
  dark-glass-ultra: '#1C1C1EE6'
  dark-glass-border: '#FFFFFF38'
  dark-glass-border-subtle: '#FFFFFF20'
  dark-divider: '#FFFFFF24'
  dark-text-primary: '#F5F5F7'
  dark-muted: '#B7B7BF'
  dark-positive: '#8CD0BC'
  dark-info: '#A9C5FF'
  dark-error: '#FFB4AB'
  shadow: '#111318'
  immersive-canvas: '#262126'
  immersive-foreground: '#FFFFFF'
  immersive-muted: '#DED9DD'
  immersive-surface-variant: '#E4DDE0'
  immersive-container-high: '#3A3A3C'
  immersive-container-highest: '#444448'
  immersive-context: rgba(255, 255, 255, 0.45)
typography:
  display-medium:
    fontFamily: sans-serif
    fontSize: 28sp
    fontWeight: 700
    lineHeight: 36sp
    letterSpacing: -0.4sp
  display-large:
    fontFamily: sans-serif
    fontSize: 36sp
    fontWeight: 700
    lineHeight: 44sp
    letterSpacing: -0.3sp
  display-small:
    fontFamily: sans-serif
    fontSize: 26sp
    fontWeight: 700
    lineHeight: 32sp
    letterSpacing: -0.2sp
  headline-large:
    fontFamily: sans-serif
    fontSize: 28sp
    fontWeight: 700
    lineHeight: 36sp
  headline-medium:
    fontFamily: sans-serif
    fontSize: 24sp
    fontWeight: 600
    lineHeight: 32sp
  headline-small:
    fontFamily: sans-serif
    fontSize: 22sp
    fontWeight: 600
    lineHeight: 28sp
  title-large:
    fontFamily: sans-serif
    fontSize: 18sp
    fontWeight: 600
    lineHeight: 24sp
  title-medium:
    fontFamily: sans-serif
    fontSize: 16sp
    fontWeight: 600
    lineHeight: 21sp
  title-small:
    fontFamily: sans-serif
    fontSize: 15sp
    fontWeight: 500
    lineHeight: 20sp
  body-large:
    fontFamily: sans-serif
    fontSize: 16sp
    fontWeight: 400
    lineHeight: 23sp
  body-medium:
    fontFamily: sans-serif
    fontSize: 14sp
    fontWeight: 400
    lineHeight: 20sp
  body-small:
    fontFamily: sans-serif
    fontSize: 13sp
    fontWeight: 400
    lineHeight: 18sp
  label-large:
    fontFamily: sans-serif
    fontSize: 14sp
    fontWeight: 600
    lineHeight: 18sp
  label-medium:
    fontFamily: sans-serif
    fontSize: 12sp
    fontWeight: 500
    lineHeight: 16sp
    letterSpacing: 0.2sp
  label-small:
    fontFamily: sans-serif
    fontSize: 11sp
    fontWeight: 500
    lineHeight: 15sp
    letterSpacing: 0.2sp
  lyric-stage:
    fontFamily: sans-serif
    fontSize: 34sp
    fontWeight: 700
    lineHeight: 44sp
  lyric-stage-translation:
    fontFamily: sans-serif
    fontSize: 17sp
    fontWeight: 400
    lineHeight: 26sp
  lyric-stage-compact:
    fontFamily: sans-serif
    fontSize: 22sp
    fontWeight: 700
    lineHeight: 28sp
  lyric-stage-translation-compact:
    fontFamily: sans-serif
    fontSize: 14sp
    fontWeight: 400
    lineHeight: 20sp
  lyric-stage-short:
    fontFamily: sans-serif
    fontSize: 18sp
    fontWeight: 700
    lineHeight: 22sp
  lyric-stage-translation-short:
    fontFamily: sans-serif
    fontSize: 12sp
    fontWeight: 400
    lineHeight: 16sp
rounded:
  button: 12dp
  artwork-small: 8dp
  artwork: 14dp
  artwork-large: 20dp
  panel: 16dp
  capsule: 999dp
  material-large: 24dp
  material-extra-large: 28dp
  field: 10dp
  mini-player: 30dp
spacing:
  xs: 4dp
  sm: 8dp
  md: 12dp
  lg: 16dp
  xl: 20dp
  xxl: 28dp
  page-wide: 40dp
components:
  button-play-all:
    backgroundColor: '{colors.light-primary}'
    textColor: '{colors.light-raised}'
    typography: '{typography.label-large}'
    rounded: '{rounded.material-large}'
  field-lyrics-folder:
    backgroundColor: '{colors.light-canvas}'
    textColor: '{colors.light-text-primary}'
    rounded: '{rounded.field}'
  sidebar-selected:
    backgroundColor: '{colors.light-raised}'
    textColor: '{colors.light-primary}'
    typography: '{typography.title-small}'
    rounded: '{rounded.button}'
  segmented-control:
    backgroundColor: rgba(245, 245, 248, 0.62)
    rounded: '{rounded.panel}'
    padding: '{spacing.xs}'
    height: 48dp
  segmented-selected:
    backgroundColor: '{colors.light-glass-ultra}'
    textColor: '{colors.light-primary}'
    typography: '{typography.label-large}'
    rounded: '{rounded.button}'
  favorites-entry:
    backgroundColor: '{colors.light-glass-light}'
    rounded: '{rounded.panel}'
    padding: 18dp
  lyric-active:
    backgroundColor: '{colors.light-primary-soft}'
    textColor: '{colors.light-primary}'
    rounded: '{rounded.artwork}'
    padding: 8dp 14dp
  navigation-capsule:
    backgroundColor: '{colors.light-glass}'
    rounded: '{rounded.capsule}'
    padding: 5dp
  navigation-selected:
    backgroundColor: '{colors.light-primary-soft}'
    textColor: '{colors.light-primary}'
    typography: '{typography.label-small}'
    rounded: '{rounded.capsule}'
    height: 58dp
  mini-player:
    backgroundColor: '{colors.light-glass}'
    rounded: '{rounded.mini-player}'
  lyric-stage-current:
    textColor: '{colors.immersive-foreground}'
    typography: '{typography.lyric-stage}'
    padding: 8dp 14dp
  lyric-stage-context:
    textColor: '{colors.immersive-context}'
    typography: '{typography.lyric-stage}'
    padding: 8dp 14dp
  player-play:
    textColor: '{colors.immersive-foreground}'
    size: 64dp
  player-pane-selected:
    backgroundColor: rgba(255, 255, 255, 0.18)
    textColor: '{colors.immersive-foreground}'
    rounded: '{rounded.button}'
    size: 48dp
  setting-row:
    backgroundColor: '{colors.light-raised}'
    textColor: '{colors.light-text-primary}'
    rounded: '{rounded.artwork}'
    padding: 14dp 16dp
    height: 72dp
---

# Design System: Resonance

## Overview

**Creative North Star: "原生磨砂音乐空间"**

2026-11-04（Asia/Shanghai）按已授权的 **merge** 更新：保留原生平台、系统字体与既有功能边界，按用户指定的 AM++ 源码和截图参考同步当前 Kotlin 实现。灰白主壳承载曲库和工具；沉浸播放器以真实封面柔焦背景、白色控制和加粗歌词聚焦音乐内容。参考用于视觉与结构，独立实现不复制 GPL 模块或专有资源，项目保留 MIT。

Android、Windows、Linux 共用 Compose UI，并保留各自平台能力。独立 `website/` 官网与论坛不使用本规范；HTML 提案是设计参考，不是播放器运行层。保留系统字体、原生返回、焦点、输入法、字体缩放、加载与错误反馈。

**Key Characteristics:**

- 主壳使用平整画布，磨砂集中于双胶囊导航、迷你播放条、输入区和有界面板。
- 灰白／黑灰主壳使用玫红强调；深色、系统主题、冰蓝及天气偏好继续生效。
- 音乐封面正面展示；沉浸页始终采用白色前景，宽屏左封面／播放、右歌词／队列。
- 固定壳层与连续正文转场配合；键盘和减少动态采用即时页面切换。

前置 YAML 是已提取的规范 token：尺寸保留原生 `dp`／`sp`，不等同浏览器像素；颜色使用 CSS `#RRGGBBAA`，与 Kotlin 的 `0xAARRGGBB` 表示同一颜色。机器层只摘录实际使用的值，不合成色阶。深色及天气颜色按运行主题选择，组件 token 中的浅色样例不覆盖主题解析。

核对入口：[主题与 token](composeApp/src/commonMain/kotlin/com/resonance/player/design/ResonanceTheme.kt)、[背景](composeApp/src/commonMain/kotlin/com/resonance/player/design/FrostedBackdrop.kt)、[播放器](composeApp/src/commonMain/kotlin/com/resonance/player/ui/NowPlayingOverlay.kt)、[本次方向契约](.impeccable/review/am-reference-2026-11-04/BRIEF.md)。本轮核对 [desktop-player](.impeccable/review/am-reference-2026-11-04/desktop-player.png) 与 [phone-lyrics](.impeccable/review/am-reference-2026-11-04/phone-lyrics.png) 原生离屏图；它们由本轮 `JVMImageComposeScene` 生成，不是 Android／Windows 设备截图。完整构建、测试及设备边界以[本次变更记录](docs/changes/2026-11-05-02-am-reference-native-ui.md)为准；网页 detector 不适用于本原生 UI。

## Colors

灰白与黑灰提供阅读底色，玫红标记主壳主操作和选中状态；沉浸播放器单独使用白色前景。

### Primary

- `light-primary` 为默认浅色强调，`light-primary-soft` 为其透色背景；代码中的 `Notice`、`Secondary` 与默认 `Primary` 同源，不能据名称另造色系。
- `dark-primary` 为深色默认的玫红强调；`light-ice-blue`／`dark-ice-blue` 保留冰蓝选择。天气模式晴天使用默认配色，雨雪使用冰蓝；阴天或获取失败保持当前配色。现有 `Sage` 枚举名仍保留，不代表当前视觉是绿色。
- `Positive` 与 `Info` 是状态色。Material `error` 承载错误；不把错误仅表达为颜色。

### Neutral

- `Canvas` 是页面基底；`CanvasElevated`、`Raised`、`Soft` 提供表面层次；`SurfaceSubtle` 与 `Soft` 同值。
- `TextPrimary` 用于主要信息，`Muted` 用于歌手、说明和辅助标签；浅色 `Dim`／`Dimmer` 与 `Muted` 同值。
- `Glass`、`GlassLight`、`GlassUltra` 是不同不透明度的材质底色，`GlassBorder` 与 `GlassBorderSubtle` 提供边缘；`Divider` 用于必要分隔。透色 token 的最终外观取决于底层画布。
- `ImmersivePlayerColors` 始终为深色上下文：`immersive-canvas`、白色 `Primary`／`Notice`／正文与 `immersive-muted`。播放器 `MaterialTheme` 同时使用完整 `darkColorScheme`；菜单 `surfaceContainerLowest`／`Low`／默认沿用深色画布／抬高／表面，`High`／`Highest` 使用前置沉浸容器 token，避免浅表面搭配白字。宿主主壳主题偏好不因此改写。

**The Theme Continuity Rule.** 新页面必须读取当前主题，不强制将用户的深色、系统或天气偏好改成默认浅色。主题明暗原子切换；天气颜色使用短过渡，减少动态时即时。

## Typography

**Display Font / Body Font / Label Font:** 平台 `FontFamily.SansSerif`；没有另行引入展示字体、等宽字体或网页字体包。前置字阶逐项来自 `resonanceTypography`，保留实际字号、字重、行高和已有字距，未显式设定的属性不猜测。

- `display-large` 用于宽屏集合详情标题；紧凑详情用 `headline-medium`。
- `headline-large`／`headline-medium` 用于主要页面与工具页标题，`title-large`／`title-medium` 用于区块和内容标题。
- `body-large`／`body-medium`／`body-small` 用于正文与说明，`label-large`／`label-medium`／`label-small` 用于控件和辅助标签。
- 正文与列表向左对齐；播放控制和适合居中的歌词预览保持居中。不为装饰增加英文标签。
- 封面下方的同步歌词预览按视口测量原文和译文。空间有限时使用 16sp 主歌词和 12sp 译文，均遵循系统字号。无法完整显示时保留「查看完整歌词」入口。
- 沉浸歌词原文使用 `lyric-stage`，译文使用 `lyric-stage-translation`。歌词视口高不足 240dp 时使用对应的 `short` 字阶，其余宽不足 480dp 或高不足 300dp 的视口使用 `compact` 字阶。所有原文行在同一视口使用同一 Bold 字阶。普通独立歌词使用 `title-large`、Medium，译文使用 `body-small`，保留原强调色与淡底。两条路径均按换行后的真实高度滚动，并遵循系统字体缩放。

**The Stable Lyrics Rule.** 原文与译文共同表达当前行；焦点变化不改变字号、字重、行高或排版尺寸。

## Layout

布局值来自 [LibraryShell](composeApp/src/commonMain/kotlin/com/resonance/player/ui/LibraryShell.kt)、[PlaylistGrid](composeApp/src/commonMain/kotlin/com/resonance/player/ui/PlaylistGrid.kt)、[ToolsScreens](composeApp/src/commonMain/kotlin/com/resonance/player/ui/ToolsScreens.kt) 与 [NowPlayingOverlay](composeApp/src/commonMain/kotlin/com/resonance/player/ui/NowPlayingOverlay.kt)。

| 范围 | 当前结构 |
| --- | --- |
| 小于 720dp，且不满足短窗口条件 | 紧凑正文与两个独立胶囊：上层迷你播放器，下层音乐库／音乐助手／工具三项导航（2026-11-05 移除一起听）。外侧水平 12dp、垂直 6dp，两者间隔 8dp。IME 显示及快速教程时隐藏，普通教程保留；无当前曲目时只显示导航。 |
| 720–1099dp，且不满足短窗口条件 | 导航 rail、公共工具栏与独立播放条。 |
| 1100dp 起，且不满足短窗口条件 | 232dp 侧栏、公共工具栏和固定播放条；侧栏内距水平 18dp、垂直 28dp。真实收藏、推荐及最多六个歌单入口保留。 |
| 高度小于 480dp 且宽度至少 480dp | 优先使用 72dp 图标 rail，导航按钮 56dp、16dp 圆角，选中项为 `PrimarySoft` 底与 `Primary` 图标；rail 可纵向滚动。正文走紧凑路径，不显示桌面公共工具栏；播放条采用 integrated 模式、最低 60dp，避免上下控件挤占内容。 |
| 曲库可用高度小于 360dp | `LibraryScreen` 标题缩为 `title-large`，隐藏副标题，收掉标题垂直留白；分段标签上内距 0dp、下内距 8dp。依据正文可用高度判断，与窗口短屏判断独立。 |
| 正文 | 紧凑页水平 20dp，非紧凑页 40dp；工具与设置正文最大宽度 1100dp，标题区与滚动正文分开。 |
| 封面集合 | 网格容器宽度小于 600dp 为两列，600–899dp 三列，900dp 起四列；断点依据实际容器宽度，不是设备名称。歌单横向间隔 16dp；专辑／歌手窄网格 16dp、宽网格 22dp，纵向 24dp。 |
| 集合详情 | 内容容器至少 620dp 且字体缩放小于 1.5 时采用封面与说明横排；其余使用紧凑头部，保留播放、随机播放和管理操作。 |
| 播放器 | 宽度至少 720dp，或宽高比大于 1.35 时采用宽屏布局；内容最大 1280×900dp，左右内距宽屏 32dp／窄屏 20dp、垂直 10dp。宽屏两栏间隔 56dp，剩余宽度按左 0.46／右 0.54 分配；左侧封面／曲名／进度／播放／音量，右侧歌词或队列及底部分栏。首次宽屏默认歌词。窄屏为上下结构，封面／歌词／队列切换位于播放控制下方。 |
| 播放器短屏／大字 | 可用高度小于 560dp，或字体缩放大于 1.2 且高度小于 840dp，使用紧凑曲名和 4dp 主间隔（常规 20dp）；窄屏收起音量和快捷入口，菜单仍可进入设置。正面封面边长取可用宽度、可用高度减 12dp 和 520dp 的最小值。 |
| 播放条 | 独立播放条最低 80dp，整合在手机底座或短窗口中最低 60dp；可用宽度达到 850dp 时展示曲目信息、播放控制、音量／收藏／队列三段。没有当前歌曲时不伪造播放信息。 |

间距基础尺度是 `ResonanceSpacing` 的 4／8／12／16／20／28dp；页面和组件可以使用已核对的专属值，不把局部值强制推广成全局节奏。

## Elevation & Depth

浅／深色非沉浸主壳均直接使用 `Canvas` 平整背景，不采样封面或叠加径向色。沉浸播放器读取真实封面；无图时使用封面种子氛围渐变。实际图像饱和度为 0.65、缩放为 1.14、模糊 64dp；播放器在独立深色上下文中使用 38% 的画布遮罩，再叠加顶部 8% 至底部 26% 的画布渐变。浅主题主壳展开播放器也走这条深色路径。背景色不修改封面原图。

`resonanceGlass` 使用 28dp 模糊、0.012 噪点、1dp 渐变边缘。Haze 不能模糊时使用 96% 不透明度回退；没有采样源时使用原材质渐变。玻璃不覆盖整页正文，列表行与聊天正文保持轻量透色或开放排布。

### Shadow Vocabulary

- 通用玻璃默认 2dp elevation；手机导航胶囊、手机迷你播放胶囊与独立迷你播放条均为 3dp。沉浸播放器外层 24dp、正面封面 16dp；多选工具条专用 10dp。旧的跨栏播放器控制台已移除，不延续其阴影参数。
- 通用玻璃阴影使用 `Shadow`，浅色不透明度 8%、深色 35%。这些是 Compose elevation 参数，不伪造为等价 CSS `box-shadow` 数值。
- 封面保持正面展示；既有 `galleryShadow` 用静态 `dropShadow`，不每帧改变模糊半径。

**The Bounded Material Rule.** 背景采样模糊用于沉浸封面背景和有边界的导航、输入、面板。歌曲行、普通歌词和聊天正文不逐项采样；沉浸歌词只模糊文字邻行。原生独立弹窗隔离宿主 Haze，用静态主题底色或渐变。

## Shapes

`ResonanceShapes` 的按钮为 12dp、封面小／常规／大为 8／14／20dp、面板与卡片为 16dp，胶囊为 999dp。Material 形状保留 8／12／16／24／28dp；前置圆角按实际用途命名，不把局部形状都替换成同一大圆角。

分段容器 16dp、选中面 12dp；手机导航为胶囊，迷你播放器圆角使用前置 `mini-player`。沉浸播放器上角 28dp、正面封面 14dp、底部分栏选中面 12dp；目录输入框 10dp，普通歌词焦点带 14dp。歌手封面使用圆形裁剪，专辑与歌单使用圆角封面。

## Components

### Buttons

主操作沿用 Material Button 及主题色；集合详情“播放全部”与“随机播放”使用 24dp 圆角和最低 48dp 高度，后者为 tonal 变体。小型工具栏按钮的交互占位 48dp，内部 4dp 内距后形成透色圆面与 1dp 边缘。按钮不额外采样模糊。

`resonancePressable` 的默认按压缩放为 0.97，时长 120ms，曲线 `cubic-bezier(.23, 1, .32, 1)`；只有调用该 modifier 的控件应用此反馈。键盘输入与减少动态时不缩放，不把该参数强加给所有 Material 按钮。

### Chips and Segmented Controls

外观选项使用原生 `FilterChip`，至少 48dp 高，保留选中图标和 Material 自身状态；未在源码覆写的形状、状态层不另造 token。共用 `GlassSegmentedControl` 高度 48dp、内距 4dp，选中面使用 `GlassUltra`、1dp 边和 2dp elevation。选中面移动 180ms，标签保留原位置；键盘及减少动态即时。

### Cards and Inputs

收藏入口是 16dp 圆角的 `GlassLight` 行，最低 80dp、内距 18dp。推荐横条最低 76dp；歌单区域保留标题和新建操作，不添加重复新建占位格。

目录输入使用 `OutlinedTextField`：10dp 圆角、`Canvas` 底、`Primary` 焦点边与光标、`Divider` 非焦点边。表单保留原校验、保存、测试、登录与系统选择器。其余输入沿用现有 Material 或已定义材质组件，不把此目录框的局部圆角推广到所有字段。

### Navigation and Settings

侧栏选中项使用 `Raised` 底和 `Primary` 文字／图标，未选中项透明底和 `Muted`，12dp 圆角。手机两胶囊独立；导航内距 5dp，四个等宽项目最低 58dp，23dp 图标与 `label-small` 标签垂直排布，选中标签 SemiBold。`PrimarySoft` 选中胶囊用 220ms／`ExitEasing` 横移，减少动态即时。公共工具栏、侧栏与播放条不随页面正文位移；短窗口使用可滚动图标 rail，正文保留紧凑搜索入口，桌面公共工具栏不显示。

设置索引行使用 `Raised` 底、14dp 圆角、最低 72dp、高度内距水平 16dp／垂直 14dp；24dp `Primary` 图标、`title-small` 标题与 `body-small` 说明。悬停／焦点切换为 `Soft` 底，焦点加 1.5dp `Primary` 边缘，背景过渡 160ms，减少动态即时。

设置由索引进入八组：外观与动态效果、播放与音效、歌词与翻译、模型与 API、每日推荐计划、同步歌词生成、B 站账号、本地音乐与歌词目录。分组属于 `SettingsScreen` 内部 `SettingsSection` 状态；组内返回先回索引，再返回工具。教程按目标进入相应分组，没有增加八个业务目的地或改变配置键。

工具栏返回经窗口局部的 [UiBackDispatcher](composeApp/src/commonMain/kotlin/com/resonance/player/platform/UiBackDispatcher.kt) 先分发给最后注册的有效页面回调。Android 与桌面 `ResonanceBackHandler` 将同一有效回调注册到此桥接，并继续处理各自系统／键盘返回；失效或退场时注销。`DesktopTopbar` 优先调用 `handle()`，因此集合详情先返回列表、设置分组先回索引；没有有效回调时才按目的地回退：教程／音量平衡说明回设置，推荐历史回每日推荐，工具子页回工具，其余回音乐库。

主导航、曲库标签、集合详情、设置分组与播放器分栏统一使用 `ContinuousPageTransition`：固定尺寸，12dp 水平位移与透明度一起变化，入退场均为 280ms，曲线 `cubic-bezier(.32, .72, 0, 1)`。新旧真实页面同时存在，未退场实例在反向时复用；旧页不接收指针输入并移除可访问语义，结束后释放。键盘、减少动态或显式即时路径直接切换。

### Player, Lyrics and Assistant

播放条到全屏的过渡使用实际表面/封面端点，固定尺寸正文只做图层裁剪、位移与淡变；封面独立衔接，背景最多退后到 96%，播放页顶栏保留 36×3dp 的下拉提示。点击、释放和反向共用临界阻尼弹簧（dampingRatio=1、stiffness=400）。只在顶栏与封面响应竖向收起，歌词滚动和进度拖动各自处理；短拉回弹，长拉/向下快速甩动收起。减少动态为 90ms 静止淡变，键盘操作即时切换；两者无封面飞行或背景缩放。实现核对日期 2026-11-05，见 `PlayerExpansion.kt`。

播放器保留轻量顶栏、开放正文和底部图标分栏。播放／暂停为 64dp 交互位、48dp 白色无底符号；前后曲为 56dp 交互位、36dp 白色快退／快进符号，仍调用前一首／下一首。随机／循环为 48dp，开启时使用 18% 白色圆角底。分栏为 48dp、24dp 图标，选中底为 18% 白色、非选中图标 65% 白色。准备、缓冲、加载与失败反馈来自真实播放状态。

播放进度调用 `SoftRefinedPlaybackScrubber(flat=true)`：轨道 4dp，交互位 48dp；拖动只预览，松手一次提交 seek，保留键盘和语义操作。旧的默认波形分支继续兼容；平直分支不运行波形时钟。音量用 4dp 直线、无拇指，白色有效段 80%／未播段 22%，宽屏和普通高度手机保留音量／音效／定时快捷入口，短窄屏从菜单访问。

沉浸歌词当前行使用白字、14% 白色淡底及 24% 细边（2026-11-05 前置回归已恢复，见 `LyricRow` / `lyricFocus`），跟随中的非当前行使用 `immersive-context`；文字模糊为距当前行行数 × 1.5dp、最高 4dp。手动浏览使距离归零，所有行清晰；减少动态也取消文字模糊，当前句焦点仍保留。颜色过渡 160ms，减少动态即时；译文非当前行不透明度再乘 0.85。字号、字重和行高不随焦点改变。普通独立 `LyricsContent` 保留 `Notice` 焦点、浅色 10%／深色 14% 淡底及 24% 边缘、14dp 圆角。两条路径均保留真实行高跟随、手动浏览和回到当前行。

播放设置与音效弹窗在沉浸主题之外创建，沿用主壳主题；`FrostedAlertDialog` 内容及表面隔离主窗口 Haze，音效原生窗口使用静态主题底色。不能为统一白色播放器前景而让独立窗口继承沉浸色板或跨窗采样。

助手保留新建／更多、右侧用户气泡、开放式回复、歌曲附件和回复模型标记。输入区域可增长并保留触摸目标；歌曲附件仅解析当前曲库明确 ID 链接，点播／加入队列由用户点击触发。任务、导入导出、歌源、存储、教程和弹窗继承主题与页面框架，同时保留真实状态和原有确认流程。

`.impeccable/design.json` 保存前置格式不能表达的原生动效、断点、阴影参数和说明。此项目不提供 HTML／CSS 组件样本，sidecar 的 `components` 保留空数组；组件规范以前置 token、本文和 Kotlin 源码为准，不合成浏览器 hover 色阶或平台默认状态。

## Do's and Don'ts

### Do:

- Do 使用 `ResonanceColors`、Material 字阶和现有共用组件，按当前主题解析颜色。
- Do 保留触摸目标、焦点、系统返回、IME 避让、大字与短屏的真实操作路径；工具栏返回优先复用当前页面有效回调，短窗口优先使用图标 rail 和紧凑正文。
- Do 保留已有播放、队列、转换、配置写入顺序、原子文件恢复、同步校验和路径边界；删除、标签写回、上传与收费请求沿用原授权。
- Do 按本次实际平台证据描述验证；AM++ 仅作可核对的布局／视觉参考，保留原生业务与 MIT 边界。

### Don't:

- Don't 把 HTML 原型的 px、模拟数据或网页框架直接移入原生应用，也不要把本规范套到独立官网。
- Don't 在普通列表行、普通独立歌词或聊天正文逐项增加背景采样模糊；沉浸舞台邻行文字模糊仅沿用现有距离规则。
- Don't 添加已否定的厚封面、光束、粒子、立体晶球或持续漂浮；封面本身保持原色。
- Don't 用改变歌词字号、字重或行高表达当前行；不要覆盖已有主题偏好或改变存储格式。
- Don't 用静态图、HTML 预览、离屏测试或历史包结果宣称本版 Android 设备、Windows 运行时或高刷新率性能已验证，也不把 Haze 磨砂宣称为参考完整光学折射或像素级复刻。


## 2026-11-05 歌词工具和恢复流程

歌词工具使用独立原生Dialog：94%可用宽、最大560dp、85%高度，固定标题／关闭按钮，正文独立纵向滚动。歌词视口裁剪，当前句不得画入工具区。校准提前／落后／重置各占独立行，支持小窗口与1.6倍字体。截图核对提供“稍后继续”和明确“丢弃草稿”；任务页有继续入口。智能管家路径修复先预览／勾选再保存，只对唯一文件名匹配提出建议，不强选歧义候选。
