---
name: Resonance 原生浅色磨砂界面
description: Kotlin / Compose Multiplatform 原生播放器；开放正文、静态浅色画布与有界磨砂控件。
colors:
  light-primary: '#A53649'
  light-primary-soft: rgba(165, 54, 73, 0.10)
  light-ice-blue: '#315FD0'
  dark-primary: '#CCD2DB'
  dark-primary-soft: rgba(204, 210, 219, 0.16)
  dark-ice-blue: '#A9C5FF'
  light-canvas: '#F3F5F8'
  light-canvas-elevated: '#F9FAFC'
  light-raised: '#FFFFFF'
  light-soft: '#E9EDF2'
  light-glass: '#FFFFFFD9'
  light-glass-light: '#FFFFFF9E'
  light-glass-ultra: '#F8FAFCEB'
  light-glass-border: '#FFFFFFDD'
  light-glass-border-subtle: '#FFFFFF90'
  light-divider: '#46536520'
  light-text-primary: '#25303C'
  light-muted: '#5C6572'
  light-positive: '#2F7467'
  light-info: '#365FA8'
  light-error: '#BA1A1A'
  dark-canvas: '#182027'
  dark-canvas-elevated: '#242D35'
  dark-raised: '#303A44'
  dark-glass: '#2B3540A8'
  dark-glass-light: '#35414D8C'
  dark-glass-ultra: '#242D35E6'
  dark-glass-border: '#FFFFFF38'
  dark-glass-border-subtle: '#FFFFFF20'
  dark-divider: '#FFFFFF24'
  dark-text-primary: '#F1F3F5'
  dark-muted: '#C1CAD3'
  dark-positive: '#8CD0BC'
  dark-info: '#A9C5FF'
  dark-error: '#FFB4AB'
  shadow: '#111318'
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
  mini-player: 22dp
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
    backgroundColor: rgba(233, 237, 242, 0.62)
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
---

# Design System: Resonance

## Overview

**Creative North Star: "原生磨砂音乐空间"**

2026-11-04（Asia/Shanghai）按 **merge** 更新：保留已确认的原生磨砂方向与功能边界，将过期色板、页面结构和导航参数同步为当前 Kotlin 实现。浅色主壳以雾白画布承载曲库与工具，沉浸播放器以真实封面提供柔焦背景；重点是音乐内容、清楚的操作与连续的状态变化。

Android、Windows、Linux 共用 Compose UI，并保留各自平台能力。独立 `website/` 官网与论坛不使用本规范；HTML 提案是设计参考，不是播放器运行层。保留系统字体、原生返回、焦点、输入法、字体缩放、加载与错误反馈。

**Key Characteristics:**

- 开放正文，材质集中于导航底座、输入区、控制台和有界面板。
- 默认浅色玫瑰强调；深色、系统主题、冰蓝及天气偏好继续生效。
- 音乐封面正面展示，层次来自透色、边缘与柔和阴影。
- 固定壳层与连续正文转场配合；键盘和减少动态采用即时页面切换。

前置 YAML 是已提取的规范 token：尺寸保留原生 `dp`／`sp`，不等同浏览器像素；颜色使用 CSS `#RRGGBBAA`，与 Kotlin 的 `0xAARRGGBB` 表示同一颜色。机器层只摘录实际使用的值，不合成色阶。深色及天气颜色按运行主题选择，组件 token 中的浅色样例不覆盖主题解析。

核对入口：[主题与 token](composeApp/src/commonMain/kotlin/com/resonance/player/design/ResonanceTheme.kt)、[背景](composeApp/src/commonMain/kotlin/com/resonance/player/design/FrostedBackdrop.kt)、[原生实施映射](design-system/resonance/proposals/2026-11-04-light-glass/NATIVE-IMPLEMENTATION.md)。完整构建、测试及未验证范围以[本次变更记录](docs/changes/2026-11-04-06-native-light-ui-and-continuous-navigation.md)为准；本规范本身不构成设备验收。

## Colors

雾白与蓝灰提供稳定的阅读底色，玫瑰色标记主操作、选中状态与歌词焦点。

### Primary

- `light-primary` 为默认浅色强调，`light-primary-soft` 为其透色背景；代码中的 `Notice`、`Secondary` 与默认 `Primary` 同源，不能据名称另造色系。
- `dark-primary` 为深色默认的银灰强调；`light-ice-blue`／`dark-ice-blue` 保留冰蓝选择。天气模式晴天使用默认配色，雨雪使用冰蓝；阴天或获取失败保持当前配色。现有 `Sage` 枚举名仍保留，不代表当前视觉是绿色。
- `Positive` 与 `Info` 是状态色。Material `error` 承载错误；不把错误仅表达为颜色。

### Neutral

- `Canvas` 是页面基底；`CanvasElevated`、`Raised`、`Soft` 提供表面层次；`SurfaceSubtle` 与 `Soft` 同值。
- `TextPrimary` 用于主要信息，`Muted` 用于歌手、说明和辅助标签；浅色 `Dim`／`Dimmer` 与 `Muted` 同值。
- `Glass`、`GlassLight`、`GlassUltra` 是不同不透明度的材质底色，`GlassBorder` 与 `GlassBorderSubtle` 提供边缘；`Divider` 用于必要分隔。透色 token 的最终外观取决于底层画布。

**The Theme Continuity Rule.** 新页面必须读取当前主题，不强制将用户的深色、系统或天气偏好改成默认浅色。主题明暗原子切换；天气颜色使用短过渡，减少动态时即时。

## Typography

**Display Font / Body Font / Label Font:** 平台 `FontFamily.SansSerif`；没有另行引入展示字体、等宽字体或网页字体包。前置字阶逐项来自 `resonanceTypography`，保留实际字号、字重、行高和已有字距，未显式设定的属性不猜测。

- `display-large` 用于宽屏集合详情标题；紧凑详情用 `headline-medium`。
- `headline-large`／`headline-medium` 用于主要页面与工具页标题，`title-large`／`title-medium` 用于区块和内容标题。
- `body-large`／`body-medium`／`body-small` 用于正文与说明，`label-large`／`label-medium`／`label-small` 用于控件和辅助标签。
- 正文与列表向左对齐；播放控制和适合居中的歌词预览保持居中。不为装饰增加英文标签。
- 歌词舞台原文使用 24sp／38sp、Medium，译文 17sp／26sp；普通歌词原文使用 `title-large`、Medium，译文使用 `body-small`。焦点只改变颜色与淡底，换行后的真实高度参与滚动。

**The Stable Lyrics Rule.** 原文与译文共同表达当前行；焦点变化不改变字号、字重、行高或排版尺寸。

## Layout

布局值来自 [LibraryShell](composeApp/src/commonMain/kotlin/com/resonance/player/ui/LibraryShell.kt)、[PlaylistGrid](composeApp/src/commonMain/kotlin/com/resonance/player/ui/PlaylistGrid.kt)、[ToolsScreens](composeApp/src/commonMain/kotlin/com/resonance/player/ui/ToolsScreens.kt) 与 [NowPlayingOverlay](composeApp/src/commonMain/kotlin/com/resonance/player/ui/NowPlayingOverlay.kt)。

| 范围 | 当前结构 |
| --- | --- |
| 小于 720dp，且不满足短窗口条件 | 紧凑正文与单个底座；上层迷你播放器、下层音乐库／音乐助手／一起听／工具四项导航。底座外侧水平 12dp、垂直 6dp，圆角 24dp。IME 显示及快速教程时隐藏，普通教程保留。 |
| 720–1099dp，且不满足短窗口条件 | 导航 rail、公共工具栏与独立播放条。 |
| 1100dp 起，且不满足短窗口条件 | 232dp 侧栏、公共工具栏和固定播放条；侧栏内距水平 18dp、垂直 28dp。真实收藏、推荐及最多六个歌单入口保留。 |
| 高度小于 480dp 且宽度至少 480dp | 优先使用 72dp 图标 rail，导航按钮 56dp、16dp 圆角，选中项为 `PrimarySoft` 底与 `Primary` 图标；rail 可纵向滚动。正文走紧凑路径，不显示桌面公共工具栏；播放条采用 integrated 模式、最低 60dp，避免上下控件挤占内容。 |
| 曲库可用高度小于 360dp | `LibraryScreen` 标题缩为 `title-large`，隐藏副标题，收掉标题垂直留白；分段标签上内距 0dp、下内距 8dp。依据正文可用高度判断，与窗口短屏判断独立。 |
| 正文 | 紧凑页水平 20dp，非紧凑页 40dp；工具与设置正文最大宽度 1100dp，标题区与滚动正文分开。 |
| 封面集合 | 网格容器宽度小于 600dp 为两列，600–899dp 三列，900dp 起四列；断点依据实际容器宽度，不是设备名称。歌单横向间隔 16dp；专辑／歌手窄网格 16dp、宽网格 22dp，纵向 24dp。 |
| 集合详情 | 内容容器至少 620dp 且字体缩放小于 1.5 时采用封面与说明横排；其余使用紧凑头部，保留播放、随机播放和管理操作。 |
| 播放器 | 宽度至少 720dp，或宽高比大于 1.35 时采用宽屏布局；内容最大 1280×900dp，左右内距宽屏 32dp／窄屏 20dp。顶部封面／歌词／队列分栏，开放正文与下方控制台。 |
| 播放条 | 独立播放条最低 80dp，整合在手机底座或短窗口中最低 60dp；可用宽度达到 850dp 时展示曲目信息、播放控制、音量／收藏／队列三段。没有当前歌曲时不伪造播放信息。 |

间距基础尺度是 `ResonanceSpacing` 的 4／8／12／16／20／28dp；页面和组件可以使用已核对的专属值，不把局部值强制推广成全局节奏。

## Elevation & Depth

浅色非沉浸主壳使用静态画布和两处低饱和径向颜色，不再采样封面。沉浸播放器继续读取真实封面，深色主壳仍保留封面背景；无图时使用封面种子生成的氛围色。真实背景图饱和度为 0.35、缩放为 1.14；沉浸模糊 32dp，非沉浸深色模糊 56dp。播放器画布遮罩浅色 86%／深色 54%，深色主壳 48%；这些是背景层参数，不降低前景文字的不透明度。

`resonanceGlass` 使用 28dp 模糊、0.012 噪点、1dp 渐变边缘。Haze 不能模糊时使用 96% 不透明度回退；没有采样源时使用原材质渐变。玻璃不覆盖整页正文，列表行与聊天正文保持轻量透色或开放排布。

### Shadow Vocabulary

- 通用玻璃默认 2dp elevation；手机合并底座 5dp，独立迷你播放器 6dp，播放器控制台 8dp。多选工具条有专用 10dp elevation，不据此把所有表面升高。
- 通用玻璃阴影使用 `Shadow`，浅色不透明度 8%、深色 35%。这些是 Compose elevation 参数，不伪造为等价 CSS `box-shadow` 数值。
- 封面保持正面展示；既有 `galleryShadow` 用静态 `dropShadow`，不每帧改变模糊半径。

**The Bounded Material Rule.** 模糊集中于有边界的导航、输入、控制台和弹窗；小型圆形控件、歌曲行、歌词与普通聊天正文不逐项采样模糊。

## Shapes

`ResonanceShapes` 的按钮为 12dp、封面小／常规／大为 8／14／20dp、面板与卡片为 16dp，胶囊为 999dp。Material 形状保留 8／12／16／24／28dp；前置圆角按实际用途命名，不把局部形状都替换成同一大圆角。

分段容器 16dp、选中面 12dp；合并底座与播放器控制台 24dp；独立迷你播放器 22dp；目录输入框 10dp；歌词焦点带 14dp。歌手封面使用圆形裁剪，专辑与歌单使用圆角封面。

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

侧栏选中项使用 `Raised` 底和 `Primary` 文字／图标，未选中项透明底和 `Muted`，12dp 圆角；手机导航和播放器共用固定底座。公共工具栏、侧栏与播放条不随页面正文位移。 短窗口优先切换为可滚动的图标 rail，正文保留紧凑搜索入口，桌面公共工具栏不显示。

设置由索引进入八组：外观与动态效果、播放与音效、歌词与翻译、模型与 API、每日推荐计划、同步歌词生成、B 站账号、本地音乐与歌词目录。分组属于 `SettingsScreen` 内部 `SettingsSection` 状态；组内返回先回索引，再返回工具。教程按目标进入相应分组，没有增加八个业务目的地或改变配置键。

工具栏返回经窗口局部的 [UiBackDispatcher](composeApp/src/commonMain/kotlin/com/resonance/player/platform/UiBackDispatcher.kt) 先分发给最后注册的有效页面回调。Android 与桌面 `ResonanceBackHandler` 将同一有效回调注册到此桥接，并继续处理各自系统／键盘返回；失效或退场时注销。`DesktopTopbar` 优先调用 `handle()`，因此集合详情先返回列表、设置分组先回索引；没有有效回调时才按目的地回退：教程／音量平衡说明回设置，推荐历史回每日推荐，工具子页回工具，其余回音乐库。

主导航、曲库标签、集合详情、设置分组与播放器分栏统一使用 `ContinuousPageTransition`：固定尺寸，12dp 水平位移与透明度一起变化，入退场均为 280ms，曲线 `cubic-bezier(.32, .72, 0, 1)`。新旧真实页面同时存在，未退场实例在反向时复用；旧页不接收指针输入并移除可访问语义，结束后释放。键盘、减少动态或显式即时路径直接切换。

### Player, Lyrics and Assistant

播放器保持轻量顶栏、顶部分栏、开放正文和控制台。宽屏有音量／音效／定时快捷入口；普通高度手机保留快捷入口，短屏从菜单访问。进度、缓冲、准备和失败反馈必须来自真实播放状态。

歌词使用 `Notice` 焦点色，浅色 10%／深色 14% 淡底及 24% 边缘，圆角 14dp；颜色、淡底和边缘过渡 160ms，减少动态即时。持续滚动按真实测量行高，手动浏览与回到当前行沿用既有逻辑。

助手保留新建／更多、右侧用户气泡、开放式回复、歌曲附件和回复模型标记。输入区域可增长并保留触摸目标；歌曲附件仅解析当前曲库明确 ID 链接，点播／加入队列由用户点击触发。一起听、任务、导入导出、歌源、存储、教程和弹窗继承主题与页面框架，同时保留真实状态和原有确认流程。

`.impeccable/design.json` 保存前置格式不能表达的原生动效、断点、阴影参数和说明。此项目不提供 HTML／CSS 组件样本，sidecar 的 `components` 保留空数组；组件规范以前置 token、本文和 Kotlin 源码为准，不合成浏览器 hover 色阶或平台默认状态。

## Do's and Don'ts

### Do:

- Do 使用 `ResonanceColors`、Material 字阶和现有共用组件，按当前主题解析颜色。
- Do 保留触摸目标、焦点、系统返回、IME 避让、大字与短屏的真实操作路径；工具栏返回优先复用当前页面有效回调，短窗口优先使用图标 rail 和紧凑正文。
- Do 保留已有播放、队列、转换、配置写入顺序、原子文件恢复、同步校验和路径边界；删除、标签写回、上传与收费请求沿用原授权。
- Do 按本次实际平台证据描述验证；78 项提案是入口和关键状态映射，不是 78 个新增原生路由。

### Don't:

- Don't 把 HTML 原型的 px、模拟数据或网页框架直接移入原生应用，也不要把本规范套到独立官网。
- Don't 在普通列表行、歌词或聊天正文逐项增加背景模糊；小型圆形按钮使用透色底与细边。
- Don't 添加已否定的厚封面、光束、粒子、立体晶球或持续漂浮；封面本身保持原色。
- Don't 用改变歌词字号、字重或行高表达当前行；不要覆盖已有主题偏好或改变存储格式。
- Don't 用静态图、HTML 预览、离屏测试或历史包结果宣称本版 Android 设备、Windows 运行时或高刷新率性能已验证。
