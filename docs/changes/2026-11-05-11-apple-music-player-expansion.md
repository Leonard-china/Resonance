# 2026-11-05 — 参考 Apple Music UI Demo 改进原生播放页展开与交互收起

- 修改日期：2026-11-05（Asia/Shanghai）
- 类型：原生 UI / 动效与交互
- 适用版本：0.6.6 / 40；未升版
- 影响平台：Android、Windows、Linux 的共享 Compose UI
- 关联需求：用户提供 [sebjvidal/Apple-Music-UI-Demo](https://github.com/sebjvidal/Apple-Music-UI-Demo)，明确选择“作为参考，改进 Resonance 的播放页和展开／收起动画”。
- 前置记录：[AM++ 原生播放页](2026-11-05-02-am-reference-native-ui.md)、[歌词显示回归](2026-11-05-08-restore-player-lyrics-and-synchronized-priority.md)。

## 需求或问题

原生播放页原先按整页高度从屏幕底部滑入，封面与迷你播放条没有几何衔接。顶栏和封面分别累加原始像素位移，超过阈值直接关闭；拖动期间页面不跟手，也没有按释放速度决定回弹或收起。

## 原因与依据

只读核对参考仓库 commit `9d7a0276ed9fd44e7bd261eac5bb3c22fe53ca76` 的 `AMNowPlayingAnimationController`、`AMNowPlayingViewController`、`AMArtworkView`。参考实现让播放条表面与封面分别连接展开前后位置，并在手势中更新页面位置。Resonance 使用 Kotlin/Compose 独立实现这些行为，没有复制 Swift/UIKit 源码、专有图片、字体或示例歌曲，不引入 iOS 运行时及新依赖。

本轮保留前置歌词修复。用户提醒两个活动对话不要冲突后，核对另一对话「修复歌词高亮丢失问题」当前处理 AI 消耗、下载失败和独立桌面歌词窗口拖动；本轮不修改其 AI、网络、桌面歌词或播放业务实现，也不替换用户当前安装。共享文档只增补相关段落。

## 具体修改

| 文件 | 关键类/函数 | 修改与目的 |
| --- | --- | --- |
| [PlayerExpansion.kt](../../composeApp/src/commonMain/kotlin/com/resonance/player/ui/PlayerExpansion.kt) | `PlayerExpansionLayout`、`PlayerExpansionState` | 测量迷你播放条和展开封面端点；固定尺寸播放页只变换图层与裁剪，封面独立移动，避免逐帧重排正文。背景轻微退后，播放器弹簧可中途反向。 |
| [LibraryShell.kt](../../composeApp/src/commonMain/kotlin/com/resonance/player/ui/LibraryShell.kt) | `LibraryShell`、`MiniPlayer` | 接入共享过渡宿主；三种窗口布局均登记实际播放条/封面端点，沿用原导航和播放回调。 |
| [NowPlayingOverlay.kt](../../composeApp/src/commonMain/kotlin/com/resonance/player/ui/NowPlayingOverlay.kt) | `NowPlayingTopBar`、`ArtworkPane` | 顶栏与封面使用统一竖向手势，替换两份超过像素阈值直接关闭的逻辑；歌词列表和进度控件不接收该关闭手势。 |
| [ResonanceTheme.kt](../../composeApp/src/commonMain/kotlin/com/resonance/player/design/ResonanceTheme.kt) | `ResonanceMotionTokens.PlayerSpec` | 用同一组无回弹弹簧参数衔接点击、手势释放与反向；减少动态使用短淡变，键盘路径即时切换。 |
| [PlayerExpansionTest.kt](../../composeApp/src/desktopTest/kotlin/com/resonance/player/ui/PlayerExpansionTest.kt) | 原生场景与指针回归 | 验证封面衔接、反向、跟手、短拉回弹、长拉关闭、向上反向、歌词滚动及减少动态。 |
| [.impeccable/design.json](../../.impeccable/design.json) | `player-sheet-transition` | 同步现行弹簧、静止淡变、键盘即时路径与背景缩放参数。 |

端点分别在变换图层内部的固定坐标系测量，不把正在移动的播放页位置叠加到目标封面坐标。移动图层的语义节点随变换一起定位；新回归同时检查屏幕位移和封面的预期插值位置。显式保存动画末帧速度，避免状态效果取消旧动画时清空速度，反向仍从当前可见位置与速度衔接。没有封面端点或封面可用尺寸为零时不创建飞行图层，使用表面过渡。

## 修改前后行为

| 场景 | 修改前 | 修改后 |
| --- | --- | --- |
| 点击播放条 | 整页从屏幕底部上滑 | 从实际播放条位置展开，封面连续衔接，主壳轻微退后 |
| 向下拖动顶栏/封面 | 达到原始像素阈值直接关闭 | 拖动期间跟手；按距离与释放速度投影决定回弹/收起 |
| 中途改变方向 | 不支持拖动过程反馈 | 从可见位置与速度继续，不重新从起点播放 |
| 减少动态与键盘操作 | 减少动态使用淡变 | 减少动态静止短淡变，键盘即时切换；不进行封面飞行或背景缩放 |
| 歌词和进度操作 | 各自处理 | 保留独立滚动、跟随、预览及松手 seek 提交 |

## 平台与数据影响

共享原生 Compose 实现覆盖三端，不引入 WebView，也不修改官网。原封面背景、手机上下结构、桌面双栏与歌词淡底焦点保留。未改变播放会话、曲库/设置/同步格式、配置写入顺序、音频转换、文件删除、标签写回、权限、凭据和外部请求。没有构建安装包或覆盖当前安装的授权性扩展操作。

## 验证

本机内存紧张，验证采用独立 systemd 用户服务限制整个进程树：`MemoryMax=896M`、`MemoryHigh=768M`、`MemorySwapMax=0`，低于仓库默认上限；不关闭用户应用、不放宽限额。脚本、编译日志及报告位于忽略目录 `build/apple-player-2026-11-05/`，原生图像位于 `composeApp/build/apple-player-2026-11-05/screenshots/`。定向编译使用 Kotlin 2.3.20 / Compose compiler 与既有依赖，未将其表述为整工程 Gradle/Release 构建。

最终服务 `resonance-apple-player-locked-final-20261105` 使用双方共用的 `build/resonance-heavy-validation.lock`，等待其他验证结束与至少 1000 MiB 可用内存后，串行执行桌面定向编译、原生回归和 Android 定向编译。最终退出码为 0，整个进程树内存峰值 **805785600 字节，约 768.46 MiB**。

| 实际命令或操作 | 环境与范围 | 结果 |
| --- | --- | --- |
| `python3 build/apple-player-2026-11-05/compile_desktop.py` | Linux，4 个本轮修改的共享主源码；以此前完整桌面编译目录及依赖作为 classpath | **通过**；本轮重新生成 4 个源码对应的 class。不是全部 common/JVM/desktop 源码重编译，也不是 Gradle/Release 构建。 |
| `python3 build/apple-player-2026-11-05/test_final.py` | Linux 原生 `ImageComposeScene`；本轮 `PlayerExpansionTest` 5 项和现有 `LyricsPlayerRegressionTest` 1 项，使用新的生产 class 优先覆盖 classpath | **6 项通过，0 失败，0 跳过**，98776 ms。包含手机/桌面实测端点和插值路径、快速反向、拖动期间跟手、短拉/长拉、向上反向、歌词滚动、减少动态/键盘，以及真实 `LibraryShell` 的 390×844、720×480、1440×900 三种布局。现有歌词回归另覆盖四组窗口/大字尺寸与当前句焦点。 |
| `python3 build/apple-player-2026-11-05/compile_android.py` | Android 36 classpath，4 个本轮修改的共享主源码；以此前完整 Android 编译目录及依赖作为 classpath | **通过**；检查共享 UI 对 Android Compose API 的编译兼容性。未生成 APK，未执行设备回归或完整 Android 构建。 |
| 桌面/Android `source-hashes.json` 与当前源码逐份 SHA-256 比对 | 两份编译输入及当前 4 个源码 | **通过**；两端编译使用同一份最终源码，编译过程中和交付前均未变化。 |
| 首轮现有 UI 图集/控件回归 | `test_targeted.py`，当轮 3 个测试类共 28 项 | **26 通过 / 2 失败**；其中现有 `UiVisualContractTest` 23 项通过。失败来自新增测试默认键盘输入模式与移动语义坐标读取；随后修正。另一次最终路径核对采用了过紧的固定像素反向阈值，改为与视口及单帧弹簧速度相容的阈值，并保留稳定端点插值断言。以上失败不计为最终通过，日志分别保存在 `initial-*`、`coordinates-*`、`geometry-*`。 |
| 原生截图实际查看 | 手机开合中间帧、短拉跟手、减少动态、三种真实主壳布局与最终桌面双栏 | **通过**；夹具使用项目演示元数据、程序生成封面与原创歌词，不读取用户音频。逐帧图像不代表硬件帧率。 |
| 文档链接、索引、JSON 解析、`git -c core.whitespace=cr-at-eol diff --check` | 当前工作区 | **通过**；保留用户及其他对话未提交修改，版本仍为 0.6.6 / 40。 |

跨对话验证曾有一次时间窗碰撞：本轮最后一次动画源码更新发生在另一对话整工程编译期间，其 `Sources changed during compilation` 一致性断言中止了该次验证。没有把这次中止计为通过，也没有生成/安装混合包。随后本轮停止改动源码，双方使用同一个验证锁；上表最终桌面、测试及 Android 编译全部在锁内依次完成。另一对话的全量重试、桌面歌词/AI 修复结果由其独立记录负责，不混入本轮通过数量。

## 限制与后续

本次源码与定向回归范围内无已知未完成项。本次不包含整工程 Gradle/Release 构建、APK/桌面打包、Android/Windows 实机验收、Linux 正式安装、发布或版本升级。离屏原生逐帧图像不代表设备帧率、真实触屏手感或长时播放结论；定向 Android 编译也不代表完整安装包已验证。

## 同步更新的文档

- 文档入口与变更索引。
- 开发说明第 11 节：播放器过渡与手势边界。
- DESIGN.md / `.impeccable/design.json`：原生播放器动效及前置歌词焦点事实。
- THIRD_PARTY.md：只读视觉与交互参考来源。

后续实际安装：用户重新连接手机后，包含本轮动效与助手修复的完整正式包已保留数据覆盖手机，并通过真实展开、短拉回弹、长拉收起及再次开合。见[手机安装记录](2026-11-05-15-player-animation-phone-install.md)；本记录“未打包/未安装”保留为原源码交付时的范围，不代表后续手机未更新。
