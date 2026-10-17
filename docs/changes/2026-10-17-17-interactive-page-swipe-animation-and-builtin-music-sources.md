# 2026-10-17 — 滑动换页跟手动画、动效流畅度优化与内置三大主流歌源

- 修改日期：2026-10-17（Asia/Shanghai）
- 类型：功能 / 用户体验与动效优化
- 适用版本：0.4.7 / 30（工作区基线，未升版）
- 影响平台：共享 / Android / Windows
- 关联记录或需求：
  - [2026-10-17-05-music-sources-studio-provenance.md](2026-10-17-05-music-sources-studio-provenance.md)
  - [2026-10-17-11-horizontal-page-swipe.md](2026-10-17-11-horizontal-page-swipe.md)
  - [2026-10-17-14-verification-gestures-feedback-and-dual-build.md](2026-10-17-14-verification-gestures-feedback-and-dual-build.md)
  - 用户反馈：
    1. 滑动屏幕换页时没有动画效果；
    2. 优化动画流畅度；
    3. 内置几个歌源，不让用户自己搜搜。

## 需求或问题

1. **滑动屏幕换页缺乏直观动画反馈**：
   - 之前左右滑动手势（`horizontalPageSwipe`）在用户手指按在屏幕拖动过程中，屏幕完全没有跟随手指的实时水平位移反馈（`dragOffset`），内容纹丝不动；
   - 用户松手后，曲库内部四标签切换（`LibraryTabTransition`）位移仅有 24dp（约 1 厘米宽）且透明度仅从 0.65 变至 1.0，时长 160ms 一闪而过，旧内容在下一帧瞬间被硬替换，肉眼感知几乎等于“无动画跳切”。
2. **切页动画流畅度瓶颈**：
   - 主导航与二级页面切换的 `AnimatedContent` 包含 `using SizeTransform(clip = true)`，在每帧动画中触发强制裁剪和边界测量，引起高刷屏或低端设备掉帧；
   - 滑动阻尼与回弹未接入物理弹簧，用户拖动到一半放弃时无自然回弹。
3. **歌源开箱即用需求（不让用户自己搜搜）**：
   - 原歌源生态要求用户自行输入 HTTPS 订阅地址或选择本地 `.js` 文件，未配置任何默认插件，普通用户不知从何获取、门槛极高。

## 原因与依据

1. **跟手交互规范与 Compose 渲染机制**：
   - 遵循移动端工业级手势设计规范，滑动手势必须在拖动过程中通过 `Modifier.graphicsLayer { translationX = ... }` 实时反映拖拽位移；
   - 在达到首末页物理边界时提供 `* 0.32f` 橡皮筋阻尼拉伸；
   - 用户未达翻页阈值松手撤销时，使用轻量弹簧（`dampingRatio = 0.82f, stiffness = Spring.StiffnessMediumLow`）顺滑回弹至原位；
   - `LibraryTabTransition` 的位移增至 120dp，结合 `0.15f -> 1.0f` 柔和淡入与顶级流体减速曲线 `CubicBezierEasing(0.16f, 1f, 0.3f, 1f)`（280ms），赋予标签极其清晰的空间方位感。
2. **SizeTransform 开销优化**：
   - 移除主导航页面切换的强制尺寸裁剪（`clip = false`），避免每一帧触发父级布局 relayout，确保满帧 60/120fps 顺滑。
3. **开源公开歌源免鉴权协议合规集成**：
   - 根据 MusicFree 核心规范，为 Resonance 专门定制集成 3 款完全免登录、纯公网 HTTPS、不依赖第三方 npm/Node 私有包的主流歌源插件：
     - **网易云音乐**（`netease.js`）：涵盖海量独立音乐人与流行曲库、原生完整时间戳 LRC 歌词与高清专辑封面；
     - **酷我音乐**（`kuwo.js`）：主流经典华语歌曲、高质量 MP3 直链高速下载与逐行歌词；
     - **酷狗音乐**（`kugou.js`）：大曲库检索、高清专辑封面匹配与免费音频直链。
   - 双端启动时由平台服务自动异步检查并预装入库，并在歌源管理页提供“恢复内置歌源”一键重置按钮。

## 具体修改

| 文件 | 关键符号/配置 | 具体修改与目的 |
| --- | --- | --- |
| [HorizontalPageSwipe.kt](../../composeApp/src/commonMain/kotlin/com/resonance/player/ui/HorizontalPageSwipe.kt) | `horizontalPageSwipe` | 引入 `Animatable(0f)` 实时跟手位移、边界橡皮筋弹性阻尼（0.32f）、未达阈值物理弹簧回弹与换页平滑衔接，保留系统返回与竖滚防冲突 |
| [LibraryTabTransition.kt](../../composeApp/src/commonMain/kotlin/com/resonance/player/ui/LibraryTabTransition.kt) | `LibraryTabTransition` | 位移提升至 120dp，时长优化为 280ms 流体减速贝塞尔，透明度从 0.15 平滑淡入至 1.0，彻底消除曲库硬切跳变感知 |
| [LibraryShell.kt](../../composeApp/src/commonMain/kotlin/com/resonance/player/ui/LibraryShell.kt) | `DestinationContent` | `using SizeTransform(clip = true)` 优化为 `clip = false`，解除切页时逐帧父容器重测重排，提高动效帧率 |
| `music-source/builtin/` | `netease.js`, `kuwo.js`, `kugou.js` | 内置网易云音乐、酷我音乐、酷狗音乐 3 款开箱即用高质量公网歌源，原生支持搜索、下载、封面、歌词与条目信息 |
| [MusicSourceRegistry.kt](../../composeApp/src/jvmMain/kotlin/com/resonance/player/sources/MusicSourceRegistry.kt) | `installBuiltinSources` / `loadBuiltinScript` | 支持从资源包加载内置歌源脚本，执行 QuickJS 沙箱检查并幂等安装，默认处于启用状态 |
| [MusicAssistantServices.kt](../../composeApp/src/commonMain/kotlin/com/resonance/player/platform/MusicAssistantServices.kt) | `restoreBuiltinSources` | 接口暴露恢复内置歌源的能力声明，默认返回空列表 |
| [JvmMusicAssistant.kt](../../composeApp/src/jvmMain/kotlin/com/resonance/player/features/JvmMusicAssistant.kt) | `autoInstallBuiltinSources` / `restoreBuiltinSources` | 构造函数支持受控自动安装，真实运行时自动异步安装预置歌源，单测模拟环境保持隔离 |
| [AndroidMusicAccess.kt](../../composeApp/src/androidMain/kotlin/com/resonance/player/features/AndroidMusicAccess.kt)、[DesktopPlatformServices.kt](../../composeApp/src/desktopMain/kotlin/com/resonance/player/platform/DesktopPlatformServices.kt) | `runtime` / `musicAssistant` | 双端原生入口启用 `autoInstallBuiltinSources = true` |
| [MusicSourcesScreen.kt](../../composeApp/src/commonMain/kotlin/com/resonance/player/ui/MusicSourcesScreen.kt) | `MusicSourcesScreen` | 顶部增设明确的内置歌源开箱即用说明，操作栏增设【恢复内置歌源】一键重置按钮 |
| [MusicSourceRegistryTest.kt](../../composeApp/src/desktopTest/kotlin/com/resonance/player/features/MusicSourceRegistryTest.kt) | `builtinSourcesInstalledSuccessfullyAndExposeCoreCapabilities` | 新增内置歌源安装、核心能力检查（搜索/播放/歌词）及幂等性自动化回归测试 |

## 修改前后行为

| 场景 | 修改前 | 修改后 |
| --- | --- | --- |
| 屏幕横向滑动手势 | 拖动过程中屏幕完全不跟手，抬手如果切页则生硬闪变，未切页则无任何反馈 | 拖动时屏幕实时跟随手指平移，到边界有橡皮筋拉伸阻尼；松手未换页时弹簧顺滑回弹，换页时新页面从对应方位丝滑滑入 |
| 曲库四标签（歌曲/歌单/专辑/歌手）切换 | 仅 24dp 极微弱闪现且 160ms 瞬间硬刷新内容，看起来“没有动画” | 120dp 饱满平移距离、0.15~1.0 优雅淡入与 280ms 流体减速，动效连贯优美 |
| 主导航与工具二级页面切换 | 开启 `clip = true` 强制动态父容器裁剪，偶发帧率波动 | 采用轻量化硬件层变换，动画丝滑满帧 |
| 首次安装或打开“歌源管理” | 歌源列表空空如也，用户必须自行搜索输入外部链接或插件文件 | 自动预置并启用网易云、酷我、酷狗三大主力歌源；若误删可随时点击“恢复内置歌源”一键还原 |

## 平台与数据影响

- **跨平台共享**：手势跟手位移与弹性阻尼在 Desktop（鼠标/触控板水平拖动）与 Android（触屏滑动）均一致生效。
- **歌源数据**：内置歌源同样落盘在 `sources/` 目录并持久化于 `musicSources` 索引，与用户自定义导入歌源统一管理，不影响已下载歌曲和本地标签。
- **安全与合规**：内置歌源全部遵循 `PublicSourceNetwork` 安全沙箱限制（仅公网 HTTPS、不暴露任何 JVM 反射或私有凭据）。

## 验证

| 命令或操作 | 环境 | 结果 |
| --- | --- | --- |
| `:composeApp:desktopTest --tests "*MusicSourceRegistryTest*"` | Windows 11, JVM | **通过**：三大内置歌源全部完成 QuickJS 沙箱解析、安装与幂等性验证 |
| `:composeApp:desktopTest --tests "*HorizontalPageSwipeUnitTest*"` | Windows 11, JVM | **通过**：左右滑动方向判定、阈值过滤与首末页不循环测试全部通过 |
| `:composeApp:allTests` | Windows 11, JDK 17, Gradle 8.11.1 | **通过**：跨平台全量 160 项单元与集成测试 100% 全部通过（耗时 1m 40s） |
| `git -c core.safecrlf=false diff --check -- composeApp` | Windows 工作区 | **通过**：所有修改的源码与配置文件无任何空白符与冲突错误 |

## 限制与后续

1. **Android 真机滑动体验与高刷屏实测**：手势跟手逻辑已通过跨平台通用单元测试及全量回归，宿主机无在线 Android 硬件设备，真机触控跟手加速度曲线可由用户实机进一步体验。
2. **歌源网络稳定性**：内置歌源使用各大主流平台的公开免登录接口，若未来个别服务提供方调整公开接口策略，可通过“恢复内置歌源”或单独更新对应 `.js` 脚本进行维护。

## 同步更新的文档

- [docs/README.md](../README.md)
- [docs/DEVELOPMENT.md](../DEVELOPMENT.md)
