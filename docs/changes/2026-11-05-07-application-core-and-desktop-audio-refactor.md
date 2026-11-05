# 2026-11-05 — 重构应用任务、播放会话、平台能力与桌面音频输出

- 修改日期：2026-11-05（Asia/Shanghai）
- 类型：重构 / Bug 修复 / 验证
- 适用版本：`0.6.6 / 40`，版本号不变
- 影响平台：共享、Android、Windows、Linux
- 关联需求：用户在桌面歌词黑框/卡顿后要求“先全部重构一遍”；承接[窗口边界修复](2026-11-05-06-desktop-lyrics-window-and-stability-boundaries.md)和[架构评估](../ARCHITECTURE-STABILITY.md)

## 需求或问题

对现有原生播放器的核心架构进行重构，减少窗口、业务、磁盘和设备之间的耦合；保留全部既有功能入口、歌曲和兼容格式。本次实际完成应用任务与退出事务、平台能力接口、导入/队列/歌词用例、配置写入和统计读取、桌面扫描/存储/播放适配及 PCM 输出边界。继续使用 Kotlin / Compose；未进行语言迁移，也未将播放器改为 Web 应用。

## 原因与依据

1. `App` 同时编排曲库导入、队列选择、文件检查、歌词获取和多类设置保存，任务退出与窗口退出缺少统一顺序。仅依赖组合销毁不能保证生产者的取消清理先于写入器关闭。
2. 原桌面 PCM 写入在中途暂停后可能继续向已停止的输出设备写入；设备返回零字节时没有退让，形成持续空转。旧实例的音频线程持续高 CPU 是独立证据；新实现用可控的分段写入/暂停/零写入夹具复现边界，并增加真实 FFmpeg/JavaSound 集成验证。不能据此断言原黑框的全部底层原因已经确定。
3. 解码、输出设备和控制命令耦合；控制线程执行 native 清理/读取时可能等待设备锁。原生设备和子进程必须有独立资源所有者，不能让主窗口等待设备清理。
4. 歌词请求仅看曲目 ID 不足以保护 A→B→A、强制刷新和已接受的偏移。异步文件检查也必须和最新播放请求/队列一起失效。
5. 手机原生重建测试发现 MediaController 使用 Activity Context 建立服务连接；Activity 销毁时系统自动解绑，延后的应用退出再 release 导致 `Service not registered` 崩溃。连接改由 applicationContext 持有，仍在主线程释放。
6. 统计 getter 可能在首次加载或持久化锁期间等待磁盘；组合阶段读取该 getter 会放大 UI 停顿。设置写入原有的顺序队列应继续复用，取消异常不应使独立写入器提前停止并遗留屏障。

## 具体修改

以下路径均相对于仓库根目录，C/J/A/D 对应 commonMain/jvmMain/androidMain/desktopMain 源集。

| 文件 | 关键符号 | 具体修改与目的 |
| --- | --- | --- |
| [ApplicationTasks.kt](../../composeApp/src/commonMain/kotlin/com/resonance/player/application/ApplicationTasks.kt)、[ApplicationLifetime.kt](../../composeApp/src/commonMain/kotlin/com/resonance/player/application/ApplicationLifetime.kt) | `ApplicationTasks`、`ApplicationEffect`、`registerStop/registerCompletion/register` | 显式拥有应用协程；统一异常反馈；停止生产者→等待取消清理→排空写入→释放平台；退出幂等 |
| [App.kt](../../composeApp/src/commonMain/kotlin/com/resonance/player/App.kt)、[Main.kt](../../composeApp/src/desktopMain/kotlin/com/resonance/player/Main.kt) | `App`、`requestExit` | App 保留装配与 UI 状态，业务委托用例；主窗/歌词窗同级，托盘退出等待事务完成再退出进程 |
| [PlatformServices.kt](../../composeApp/src/commonMain/kotlin/com/resonance/player/platform/PlatformServices.kt) 与同目录五个能力接口 | `PlaybackServices/LibraryServices/PreferenceServices/AppearanceServices/LyricsServices` | 将曲库、播放、偏好、外观、歌词职责分开；保留原聚合接口及 Android/桌面实现；全部调用方从源码重新编译 |
| [LibraryImportUseCase.kt](../../composeApp/src/commonMain/kotlin/com/resonance/player/application/LibraryImportUseCase.kt) | `LibraryImportUseCase` | 导入前等待写入屏障，导入后重载实际已提交的曲库和包歌单，再返回 UI 快照；取消允许保留已提交结果 |
| [PlaybackSession.kt](../../composeApp/src/commonMain/kotlin/com/resonance/player/application/PlaybackSession.kt) | `PlaybackSession`、`PlaybackSessionState` | 提取播放请求、文件检查、随机/循环/队列增删和一起听控制门禁；代次拒绝迟到结果；删除当前曲目传递缩减后的最新队列 |
| [LyricsSession.kt](../../composeApp/src/commonMain/kotlin/com/resonance/player/application/LyricsSession.kt) | `LyricsSession` | 自动查词、刷新及偏移接受共享请求代次；非合作取消的服务返回后仍检查取消与代次 |
| [ApplicationPreferences.kt](../../composeApp/src/commonMain/kotlin/com/resonance/player/application/ApplicationPreferences.kt)、[WeatherAppearance.kt](../../composeApp/src/commonMain/kotlin/com/resonance/player/design/WeatherAppearance.kt) | `ApplicationPreferences`、外观写入队列 | 保存捕获的操作值；同键合并和操作屏障沿用 PersistenceQueue，天气外观也串行保存并参与退出排空 |
| [PersistenceQueue.kt](../../composeApp/src/commonMain/kotlin/com/resonance/player/data/PersistenceQueue.kt)、[MusicFeatureController.kt](../../composeApp/src/commonMain/kotlin/com/resonance/player/ui/MusicFeatureController.kt) | `closeAndJoin`、`finishPersistence` | 暴露可等待的排空；单个写入动作自行取消不停止仍有效的独立 worker |
| [ListeningStatsRepository.kt](../../composeApp/src/jvmMain/kotlin/com/resonance/player/playback/ListeningStatsRepository.kt)、[ListeningStatsSnapshot.kt](../../composeApp/src/commonMain/kotlin/com/resonance/player/model/ListeningStatsSnapshot.kt)、[ListeningStatsState.kt](../../composeApp/src/commonMain/kotlin/com/resonance/player/ui/ListeningStatsState.kt) | `snapshots`、`rememberListeningStats` | IO 加载与写入发布不可变快照；缓存 getter 不等待磁盘 Store 锁；播放器/画像/推荐状态观察快照，兼容适配器在 IO 读取 |
| [DesktopPlatformServices.kt](../../composeApp/src/desktopMain/kotlin/com/resonance/player/platform/DesktopPlatformServices.kt) 及同目录 `DesktopMusicScanner/DesktopLibraryStore/DesktopAudioPlayer` | 平台适配/扫描/存储/播放 | 从平台大文件分离独立职责；保留转换、合并、同步包校验和原子写入逻辑；close 幂等并取消导入 |
| [DesktopAudioBackend.kt](../../composeApp/src/desktopMain/kotlin/com/resonance/player/audio/DesktopAudioBackend.kt)、[DesktopPcmPlayer.kt](../../composeApp/src/desktopMain/kotlin/com/resonance/player/audio/DesktopPcmPlayer.kt) | `DesktopAudioBackend/DecodedPcm/PcmOutput`、请求会话 | 可注入解码与输出；控制命令只改变意图，native 调用归 worker；分段/帧对齐写入、零写入退让、5 秒无进展错误、有限 drain、缓存位置、旧会话事件隔离；双 worker 与有限待执行队列约束设备数量 |
| [AndroidPlatformServices.kt](../../composeApp/src/androidMain/kotlin/com/resonance/player/platform/AndroidPlatformServices.kt) | `close`、统计流 | 退出从后台发起时将 MediaController/观察者释放调度回主线程；MediaController 使用 applicationContext，避免 Activity 销毁后自动解绑与延后 release 重复解绑；共享统计快照接入 |

`PlatformServices` 仍作为兼容装配入口，Gradle 仍为单应用模块；这是源码职责和运行生命周期的重构，没有同时重写所有 codec、协议、转换算法或视觉组件。默认参数 helper 的归属因能力接口拆分而变化，因此禁止混合旧应用 class 与新接口 class；本次两个平台均全量源码编译，安装包以该完整输出生成。

## 修改前后行为

| 场景 | 修改前 | 修改后 |
| --- | --- | --- |
| 主窗隐藏，使用歌词小窗 | 应用组合受主窗驱动；Linux 透明路径出现黑块 | 应用在窗口之外运行；Linux 使用不透明歌词表面，窗口生命周期回归持续保留 |
| PCM 部分写入时暂停 | 后续循环可在停止设备上持续零写入 | 重新检查暂停意图；等待唤醒；零写入每轮退让，无进展报告错误 |
| 快速切歌/seek/关闭 | 调用线程可能等待 native 设备清理 | 请求取消与意图切换立即返回，设备清理由后台会话释放 |
| A→B→A 查词，或旧查词晚于刷新/偏移 | 相同曲目 ID 的旧结果仍可能被接受 | 请求代次与取消检查共同拒绝旧结果 |
| 删除当前队列项，旧文件检查随后返回 | 旧闭包可能恢复已删除的队列内容 | 使旧请求失效，新播放使用实际缩减后的队列 |
| 统计页面打开，存储正忙 | 组合阶段可能等待首次加载/Store 锁 | 页面观察内存快照，加载与持久化在 IO |
| 退出时任务清理仍提交保存 | 写入器可能先关闭 | 等待生产者取消完成，再关闭/排空写入器，最后释放平台 |

## 平台与数据影响

- 共享 App/用例/能力接口与统计读取影响 Android、Windows、Linux。Android 继续 Media3/系统悬浮服务；桌面继续 FFmpeg 48kHz、16-bit、双声道 PCM/JavaSound。Windows 专有 JNA/凭据分支保留，未在 Windows 机器运行本次代码。
- 不改曲库 JSON、属性键、播放快照、统计文件、歌词缓存、同步 ZIP 格式和凭据格式；新增状态快照仅存在内存。既有原子替换、备份恢复、包路径校验、转换保留输入和播放防竞争机制保留。
- 不新增权限或外部服务，不删除/重写用户音频，不更改 API Key；不读取或输出签名私钥。测试使用独立资料目录和生成的音频夹具。官网/论坛未改。

## 验证

所有重量级任务串行，整个 systemd 用户服务进程树限制为 MemoryMax=2G / MemoryHigh=1536M / MemorySwapMax=0；没有放宽上限、修改系统 swap 或关闭用户应用腾内存。完整日志在忽略目录 `build/stability-refactor-2026-11-05/`。

| 实际执行 | 环境 | 结果 |
| --- | --- | --- |
| `python3 .../compile_desktop.py` | Linux，Kotlin/Compose compiler 2.3.20，完整 C/J/D 源码 | 通过：198 源文件；全部调用方重编译，非 class 局部替换 |
| `python3 .../compile_android.py` | Android 36 classpath，完整 C/J/A 与设备测试 | 通过：190 主源码、29 设备测试源码；末次退出保护调整后重新执行通过 |
| `python3 .../test_desktop.py` | Linux 图形/音频会话，94 个测试源码，完整 JVM 套件 | 396 项：388 通过、8 条件跳过、0 失败；最后增补退出清理后再运行下行相关用例 |
| `python3 .../test_shutdown.py` | 最终主源码输出；应用/导入/队列/歌词/写入/统计/PCM/真实音频/原生窗口 | 55 项全部通过，包括新增“取消清理提交最后写入”测试 |
| 首轮 `UiVisualContractTest.renderNativeMotionPreviewFrames` | 原生离屏渲染 | 360 帧导出通过；最终全套不重复该产物生成方法，不将帧导出当作设备帧率 |

首轮全套有 4 项失败：自定义 runner 的工作目录/资源类路径不正确，以及 UI 替身未提供新增统计流。修正 runner 和替身后，完整套件零失败；保留首轮日志，不将其写成通过。较早局部实验还暴露默认参数 helper 二进制不兼容；最终两端全部源码重编译解决，没有将混合 class 方案用于安装。

| 实际执行 | 环境 | 结果 |
| --- | --- | --- |
| `python3 .../package_current.py android` + `desktop` | 同一限额内，Gradle 单 worker/短生命周期，完整 CLI class 输出由临时 init 的编译任务动作接入 | Android Debug/测试 APK 与 Linux createDistributable 通过；资源/Manifest/D8/jpackage 均正常执行。不是默认 Gradle Kotlin 编译或 Release 构建通过 |
| `adb ... am instrument -w -r -e class ...ApplicationLifecycleDeviceTest -e refactorLifecycle true .../AndroidJUnitRunner` | 已连接 Honor Android 16/API 36，独立验证应用 ID | 修复前因 Activity 服务重复解绑崩溃；改为 applicationContext 后重新完整 Android 编译/打包，1 项通过，内部连续 3 轮启动→重建→关闭→主线程心跳，6.468 秒。主 APK/测试 APK均已卸载，未覆盖日常正式应用或授予音频权限 |
| 包内完整 class 校验与隔离启动 | Linux x86_64 原生 app image，独立空资料目录 | 1456 个编译输出条目逐项与包内 jar 字节一致；隐藏主窗后歌词仍有文字/操作控件，关闭歌词保存 false，恢复主窗与正常托盘退出通过，进程退出状态 0 |
| 本机完整安装与正式入口启动 | Linux 当前用户安装 | 完整 image 复制到 `~/.local/opt/resonance/0.6.6-20261105-stability-refactor/`，392 个文件逐项 hash 核对；旧进程通过原托盘退出，原 `current` 原子切换，原桌面/菜单/命令入口启动新版，进程可执行路径与新目录一致、事件线程响应、组合器错误列表为空；旧程序目录与数据保留 |
| `git -c core.whitespace=cr-at-eol diff --check` | 保留既有未提交改动/CRLF 的工作区 | 通过 |

打包首轮仅用 `-x` 跳过 Kotlin 编译任务时，Gradle `processDebugJavaRes` 因关联输出 provider 未完成而失败；随后让临时 init 的任务动作消费完整预编译输出，保留任务依赖完成状态，资源/DEX/打包通过。没有通过扩大内存或混入旧应用类规避。

本次产物（忽略目录，未公开上传）：

| 文件 | SHA-256 |
| --- | --- |
| `build/stability-refactor-2026-11-05/artifacts/Resonance-0.6.6-refactor-validation.apk` | `e82ee8226dfa180216046318ea0e4a58f1a9b43f93903abf54c76813e0c4b6f9` |
| `build/stability-refactor-2026-11-05/artifacts/Resonance-0.6.6-refactor-androidTest.apk` | `2af482b54b1f1f86aee67c94d788bcc82b1db66476f2dc739de5df73b40752dc` |
| `composeApp/build/compose/binaries/main/app/Resonance/` | 完整目录，逐文件校验清单 `build/stability-refactor-2026-11-05/linux-install-manifest.json` |

隔离实屏截图及窗口节点/事件线程响应在 `linux-hidden-main-screen.png`、`linux-lyrics-crop.png`、`linux-hidden-content.txt`、`linux-restored-content.txt`；截图仅作本机诊断不提交。日常版启动日志、进程目录及短时 CPU 样本另存 `daily-*`；短样本不代表真实音频长时稳定性，也不将不同播放状态的新旧实例 CPU 当作严格 A/B。

## 限制与后续

没有在本次会话完成 Windows 实机、蓝牙切换/拔设备、大规模真实曲库导入导出及两小时以上持续播放验收；未公开发布、未构建正式签名 Release/ProGuard 包。操作系统或音频驱动若永久阻塞 native 调用，JVM 无法保证强行中断；双 worker/有限队列限制资源扩张，但不能承诺任何驱动故障都自动恢复。

现有协议、原子文件和安全边界继续沿用各自回归；不将历史审计中与本次需求无关的全部风险视为已修复。大体积 App 中仍保留 UI 装配、翻译/补全等功能接线；新能力接口提供后续独立替换边界。不能承诺“永远零 Bug”。

## 同步更新的文档

- [文档入口与索引](../README.md)
- [开发说明](../DEVELOPMENT.md)：源码入口、应用退出顺序、用例、音频/统计与验证方法
- [架构与验收边界](../ARCHITECTURE-STABILITY.md)：各阶段实际完成状态与平台限制
