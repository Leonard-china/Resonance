# 2026-11-05 — 分离桌面歌词与主窗口生命周期并建立稳定性重构边界

- 修改日期：2026-11-05（Asia/Shanghai）
- 类型：Bug 排查 / 架构重构 / 验证
- 适用版本：`0.6.6 / 40`，不升版
- 影响平台：共享 App 装配；Linux、Windows 窗口；Android 保留内联宿主
- 关联需求：用户反馈桌面底部黑框，点击桌面歌词后再次卡死，要求从架构上提高稳定性，并评估更换语言。

## 需求或问题

当前 Linux 的桌面歌词窗在屏幕底部显示黑色矩形。原生窗口存在，但用户看不到歌词和关闭控件。当前安装实例来自 2026-11-05 歌单包导入版。用户同时要求重新评估全软件的实现架构。

## 原因与依据

1. **明确的生命周期耦合**：`Main → Window → DesktopMusicDropHost → App → FloatingLyricsHost → Window`，应用状态和第二个窗口均放在主窗口内部。主窗隐藏时诊断记录其组合器 `PendingWork`，事件线程却可以响应诊断，不能写成 Java monitor 死锁。通过应用原托盘打开动作恢复主窗后，待处理更新和歌词尺寸恢复，但屏幕仍显示黑块；因此主窗生命周期不是黑框的唯一原因。
2. **透明窗口显示链路异常**：实际 Wayland 桌面截图确认黑块；歌词窗使用 OpenGL、32 位透明视觉。运行时仍有有效语义节点和非零文本边界，没有捕获到 Compose 错误。对当前窗口临时切换 `SOFTWARE_COMPAT`/`SOFTWARE_FAST` 仅移除了黑块，未恢复可见歌词，不能称为修复，也不把该诊断手段加入生产代码。不能仅凭这些证据定位到具体显卡驱动或上游库缺陷。
3. **独立的音频异常**：连续线程栈与线程累计 CPU 显示 `resonance-audio-decoder` 持续在 Java Sound `DirectAudioDevice.nWrite`，本轮不将其归因于歌词，也未擅自改写音频输出、解码或原始歌曲。
4. 现有 `FloatingLyricsTest` 只验证文本和时间轴，不能覆盖原生窗口的透明呈现、主窗隐藏或点击行为。编译成功、离屏测试和安装后真实桌面是不同验收层次。

证据位于忽略目录 `build/desktop-lyrics-stability-2026-11-05/`。诊断仅查看进程/窗口状态；截图没有提交仓库。已通过原生 `WINDOW_CLOSING` 回调关闭当前失效歌词窗，调用应用原有设置保存流程，不强杀播放器。

## 具体修改

| 文件 | 关键符号 | 修改与目的 |
| --- | --- | --- |
| [App.kt](../../composeApp/src/commonMain/kotlin/com/resonance/player/App.kt) | `App(mainWindow)` | 显式接收可组合的主窗宿主；应用状态/协程/设置写入和桌面歌词在宿主外，主界面与相关弹窗在宿主内。默认宿主直接调用内容，保留 Android 入口。 |
| [Main.kt](../../composeApp/src/desktopMain/kotlin/com/resonance/player/Main.kt) | `main/application` | 在 application 层组合 App，再由 `mainWindow` 创建主窗；歌词与主窗成为同级窗口，保留文件拖放、托盘、原窗口监听和退出流程。 |
| [DesktopFloatingLyrics.kt](../../composeApp/src/desktopMain/kotlin/com/resonance/player/ui/DesktopFloatingLyrics.kt) | `DesktopFloatingLyricsWindow` | Linux 歌词小窗使用不透明深色表面，避开本次出问题的 ARGB 透明路径；不全局切软件渲染。Windows 保留原透明窗口与锁定穿透。隔离启动的实际屏幕已验证文字、锁定及关闭按钮可见。 |
| [DesktopWindowLifecycleTest.kt](../../composeApp/src/desktopTest/kotlin/com/resonance/player/ui/DesktopWindowLifecycleTest.kt) | 两项原生窗口测试 | 显式 opt-in 的真实窗口回归；隐藏主窗后更新歌词、锁定/解锁/关闭/重开，并测试生产 App 在从未显示主窗时关闭歌词与持久化。使用合成歌曲及临时资料/曲库。 |
| [稳定性重构说明](../ARCHITECTURE-STABILITY.md) | 阶段与验收 | 说明当前证据、语言选择、应用会话/窗口/播放/曲库/存储/任务边界，以及后续阶段和完成门槛。后续阶段未实施，不声称已完成整体重写。 |

## 修改前后行为

| 场景 | 修改前 | 修改后 |
| --- | --- | --- |
| 主窗隐藏 | 应用状态和歌词窗嵌在主窗组合器内，待处理更新可能被挂起 | 应用状态和歌词窗由 application 层驱动，独立于主窗显示 |
| Linux 歌词表面 | 透明 OpenGL 窗口，当前电脑实屏黑块 | 不透明深色小窗；实际桌面已看到正常文字与操作按钮 |
| 关闭失效歌词 | 黑块无可见关闭入口 | 本次已正常关闭旧失效窗口；新版隔离实例的关闭和设置落盘亦通过 |
| “换语言保证稳定” | 尚无技术选型证据 | 先验证边界和失败行为；Qt/Flutter/Rust 仅列为评估路线，无语言迁移或重写完成的声称 |

## 平台与数据影响

Android 保留 `App(services)` 默认内联行为，其系统悬浮歌词仍归 PlaybackService；桌面实际主窗装配改变，Windows 需原生复验。没有新增权限、服务请求、持久字段或包格式；保留播放任务代次、写入顺序、原子文件与备份恢复、歌单包校验和路径边界。不读取真实密钥，不写回/删除用户音频，不清空或迁移真实曲库。诊断关闭歌词按原行为保存 `floatingLyrics=false`。

## 验证

| 命令或操作 | 环境 | 结果 |
| --- | --- | --- |
| 线程栈、EDT 诊断、原生窗口/语义边界、Wayland 实屏截图 | 当前 Linux 安装实例 | **通过排查**；确认上述事实，不代表黑框已永久修复。 |
| 临时切换现有歌词窗的软件渲染 | 当前 Linux | **未解决**；黑块消失但歌词未恢复，未采用为生产方案。运行时修改透明标记被窗口 API 拒绝，未强行处置已显示窗口。 |
| 原有 `WINDOW_CLOSING` 关闭回调 | 当前 Linux 安装实例 | **通过**，失效歌词窗消失；播放器未强制结束。 |
| 首轮 `:composeApp:desktopTest --tests …` | 2 GiB 限额，768 MiB 堆 / 384 MiB 元空间 | **环境受阻**；主源码编译通过，测试源码编译在 MemoryHigh 后持续节流，主动停止本轮自建构建服务；峰值 1,744,375,808 字节，内核 OOM 计数为 0。未运行测试，不能算通过。 |
| 缩小测试源码范围后的 Gradle 验证 | 同样总限额，640 MiB 堆 / 320 MiB 元空间 | **环境受阻**；主源码编译报 `OutOfMemoryError: Java heap space`，不是测试断言失败。停止整工程重跑，保留 `focused-tests.log`。 |
| `verify_changed_files.py`：Kotlin 2.3.20 / 同版 Compose 编译插件，独立编译 3 个生产文件及 4 个测试文件 | 同样总限额，各 JVM 顺序启动；安装版实际依赖作为基线，补充现有测试依赖 | **通过**；`changed-main-compile.log` / `changed-tests-compile.log`，退出码均为 0。使用 friend-paths 验证内部接口，不替代完整 Gradle 构建或三端产物。 |
| `org.junit.runner.JUnitCore FloatingLyricsTest PersistenceQueueTest PlaybackStateStoreTest DesktopWindowLifecycleTest` | 独立 JVM，真实 XWayland 图形会话，384 MiB 堆 | **通过，25 项**：歌词 4、写入队列 4、播放代次/设置 15、原生窗口 2；9.606 秒。`isolated-junit.log`。主窗隐藏后的更新/锁定/关闭/重开共五轮，生产 App 隐藏主窗时关闭及持久化通过。 |
| 隔离编译+测试整个进程树资源 | systemd 用户服务 | **通过**，总峰值 **702.4 MiB**，30.786 秒；`isolated-verification.log` 及用户服务日志。不将编译堆上限当成总峰值。 |
| 修改后的 Main/App/歌词窗 + 原安装运行依赖，空白测试资料启动 | 独立 1 GiB 限额 / 384 MiB JVM | **通过最终实屏**；主窗口通过原关闭回调隐藏，歌词窗使用 OpenGL 不透明表面，文字/锁定/关闭可见。`native-fixed-window-state.txt`、`native-fixed-desktop.png`、`native-fixed-lyrics.png`。峰值 232.1 MiB。不是完整安装包构建或正式安装。 |
| 首次上述源码启动器 | 隔离空资料 | **失败并已修正**；手工启动器遗漏已安装包中的 Skiko 原生资源目录，报缺少 `.so.sha256`。把原安装目录加入类路径/原生库搜索后启动通过，不改产品代码来掩盖启动器错误。 |
| 新隔离实例关闭歌词、设置持久化及托盘退出 | 独立测试资料 | **通过**，`floatingLyrics=false` 实际落盘、正常托盘退出、服务成功结束；没有留下第二个播放器或歌词窗。 |
| Android 编译/设备、Windows 原生、Linux 完整安装包构建/安装 | 本轮 | **未执行**；不能将旧构建结果作为本轮验证。当前真实播放器仍为原安装版，已关闭其失效歌词；未静默替换安装程序。 |

两轮未完成的 Wrapper 命令通过本机 JDK 运行 `org.gradle.wrapper.GradleWrapperMain`，使用 `--no-daemon --offline --max-workers=1 -Pkotlin.compiler.execution.strategy=in-process --no-configuration-cache --console=plain`；第一轮 JVM 堆 768 MiB、元空间 384 MiB，第二轮分别为 640/320 MiB。独立验证不同时保留 Gradle JVM 与测试 JVM：修改源码编译堆 768 MiB、测试编译 384 MiB，JUnit 384 MiB；具体脚本只留在忽略目录，不改变项目构建配置。整个进程树 `MemoryMax=2G / MemoryHigh=1536M / MemorySwapMax=0`，日志和峰值在上述忽略目录。没有同时运行 Android 编译或 Release 打包。

## 限制与后续

全软件重构尚未完成。Linux 黑框的底层驱动/渲染器原因和 Java Sound 高 CPU 仍需独立复现，不能承诺永不出现 Bug。本轮歌词时间轴使用合成状态；实际屏幕使用空白资料，不代表真实音频长期播放通过。已验收主窗隐藏与恢复，系统最小化没有另行单验。本轮完成窗口边界调整、Linux 表面回退、25 项定向回归与真实屏幕检查；没有完成全量测试、正式安装或三端发布。后续播放输出、状态用例、存储收口、三端安装后验收见[重构说明](../ARCHITECTURE-STABILITY.md)。

## 同步更新的文档

文档索引、开发说明的启动/窗口、任务定位与原生验证边界，以及稳定性重构说明。
