# 2026-11-14 — Android 实测复现睡眠定时器、智能补全卡死和曲库重复

- 修改日期：2026-11-14（Asia/Shanghai）
- 类型：探索性测试与缺陷诊断
- 适用版本：正式 APK 0.6.11 / 45，另测后续 Debug 构建
- 影响平台：Android 16 x86_64 模拟器
- 关联记录：[本机测试环境](2026-11-14-02-local-android-apk-testing.md)、[0.6.11 发布](2026-11-13-04-publish-0.6.11-release.md)、[智能补全弹窗隔离](2026-11-13-05-isolate-batch-enrich-dialog.md)

## 需求与实施

按用户要求从应用界面自主测试。本轮在 `resonance-api36` 用三首合成 MP3、公开正式 APK、ADB 点击和 UI 树走查实际流程，同时采集媒体会话、截图和系统 ANR 报告。正式 APK SHA-256 为 `d7aae591022e294434d04a20fabef326edf3c360e66c4ad4ec61db20a87e252a`。未改应用代码，测试前后产品行为相同。合成音频和设备证据保存在忽略目录 `build/android-exploratory-2026-11-14/`。

## 已复现问题

| 优先级 | 复现与实际结果 | 证据、原因与定位 |
| --- | --- | --- |
| P1 | 播放任一本地歌曲，暂停，打开完整播放页并点击“定时”。弹窗绘制出来后，约 5 秒出现系统无响应提示。第二次从新进程直接进入定时器，无需先调整音效，同样卡死。 | `23-timer-alone.png`、`24-timer-alone-anr.png`、`dropbox-anr-timer-alone.txt`。当前 [SleepTimerDialog.kt](../../composeApp/src/commonMain/kotlin/com/resonance/player/ui/SleepTimerDialog.kt) 的原生 `Dialog` 仍调用 `resonanceGlass`。系统抓到 `HazeEffectNode.updateEffect` 读取窗口 ID 并处理 Compose 快照更新，详见诊断。 |
| P1 | 曲库搜索本地歌曲，进入多选，选一首后打开“AI 智能补全”。配置窗口已经绘制，随后系统报告无响应。点击“开始补全”前 UI 自动化已无法取得活动根节点，点击后仍停留在配置窗口。 | `46-enrich-open.png`、`47-enrich-after-start.png`、`dropbox-anr-after-enrich.txt`。证据能确认正式包的补全弹窗流程卡死，无法确认开始回调已执行。后续源码的 [BatchEnrichDialog.kt](../../composeApp/src/commonMain/kotlin/com/resonance/player/ui/BatchEnrichDialog.kt) 已隔离宿主 Haze，正式 APK 的本次失败与后续包的测试结果独立记录。 |
| P2 | 先用“扫描本机音乐”导入已进入 Android 媒体库的三首 MP3，再通过“选择文件夹并转换 KGMA”选择同一目录。曲库和本地搜索从三首变成六条，每首重复一次。 | `10-rescan.xml`、`43-search-duplicates.xml`、`44-search-scroll.xml`、`45-multiselect.xml`。源文件 SHA-256 与导入前一致。[AndroidPlatformServices.kt](../../composeApp/src/androidMain/kotlin/com/resonance/player/platform/AndroidPlatformServices.kt) 的 `scanMediaStore` 和 `readAndroidTrack` 对同一物理文件使用不同身份，`mergeSyncedTracks` 仅按 `Track.id` 合并。后续修复需要保留现有歌单和收藏引用，并避免按歌名把不同录音合并。 |
| P3 | 系统媒体库为空时，选择普通音频的提示要求点击“选择文件夹”，实际只提供写着“转换 KGMA”的入口。选择只含 MP3 的目录也显示“已转换 3 首音乐”。 | `04-permission.xml`、`08-tools-import.xml`、`39-folder-import.xml`。当前 [ToolsScreens.kt](../../composeApp/src/commonMain/kotlin/com/resonance/player/ui/ToolsScreens.kt) 给普通目录扫描与格式转换共用入口，文案应说明两种行为，报告应区分实际导入和转换。 |

## ANR 诊断

- **进程与有效性**：两个明确入口的系统报告都匹配 `com.resonance.player` 0.6.11，分别为 PID 2936 和 3119，均在前台。系统等待窗口焦点事件约 5000 ms，在超时时立即采样。冻结期间能看到弹窗，用户能感知无响应。
- **定时器归因**：`APP_DEFECT`，主线程在 Compose 快照观察回调中执行 `HazeEffectNode.updateEffect → getWindowId`，与定时器仍从宿主继承毛玻璃状态的源码相符。系统当时未报告明显内存或 I/O 压力。证据支持优先隔离独立窗口的 Haze 状态，再用完整发布构建重跑原入口。没有证据表明系统同步屏障泄漏或 DSP 阻塞。
- **补全弹窗归因**：`APP_DEFECT`，主线程正在 `SnapshotStateObserver.recordInvalidation`，并持有其内部同步对象。系统样本未出现 Haze 帧，单凭采样无法确定触发快照处理的具体状态，也无法把它归因于网络、AI 或音频处理。此前窗口修复和本次失败的关系应通过新包的同路径实测确认。

## 验证

| 实际操作 | 结果 |
| --- | --- |
| `adb shell input`、`uiautomator dump`、`exec-out screencap` | 跳过教程、音频权限流程、扫描系统媒体库导入三首、按关键词本地搜索、创建歌单并添加一首均通过。强制停止后重开，曲库、歌单成员和暂停曲目仍可见。 |
| 本地播放并暂停，再从队列将 `QA Gamma` 置顶为下一首。以 `adb shell dumpsys media_session` 对比前后。 | 通过。队列从 Alpha/Beta/Gamma 变成 Alpha/Gamma/Beta，`QA Alpha` 保持暂停且 Android 媒体会话进度始终为 18698 ms。尚未核对设备实际出声。 |
| 查看歌词页并展开歌词工具 | 工具可展开。线上自动查词返回 `connection closed`，记为外部查询未验证。无可用同步歌词，未验歌词时间轴和实际匹配率。 |
| `adb shell dumpsys activity lastanr`、`adb shell dumpsys dropbox --print data_app_anr` | 两个前台弹窗问题已取得系统记录。`logcat -b crash -d` 未发现 Java 崩溃，本轮失败属于无响应。 |
| 安装 commit `85210d6` 同批构建的 Debug APK 和测试 APK，以 `adb shell am instrument -w -r -e class <测试类>` 运行 | 6 项通过。`BatchEnrichDialogDeviceTest` 1 项覆盖模拟补全进度的开启、停止、完成、重开及宿主响应。`PlayerDialogsDeviceTest` 1 项覆盖音效弹窗。`LyricsPlayerRegressionDeviceTest` 4 项覆盖双语长句、大字、完整歌词与播放进度同步。`NativePlaybackQueueDeviceTest` 因该测试 APK 未包含此类而未执行，runner 记录 `ClassNotFoundException`。该历史构建不是当前工作区重编译产物。 |
| 在同一 Debug APK 的真实曲库入口重走搜索、多选、打开补全、开始、完成和关闭 | 通过。导入三个合成 MP3，选中一首，只勾选名称规范。页面展示处理 1 首、AI 调用 0 次，系统未记录 Debug 包 ANR。证据为 `52-debug-enrich-open.xml`、`54-debug-enrich-complete.xml`、`55-debug-enrich-dismissed.xml`。在线封面、歌词和用户手机原有四首歌曲场景未验证。 |

## 平台与数据影响

只操作专用模拟器测试库，无真机、真实音乐文件、用户账号或模型密钥。合成 MP3 在两种导入后仍保持原哈希。Android 应用源码、共享接口、版本、权限和数据格式未修改。模拟器受 systemd 进程树限定内存，正式包这一轮验证峰值约 3.13 GiB。正式包和设备回归对应的模拟器进程均在测试取证完成后的退出阶段报告 `SIGABRT`，与测试过程中由 Android 记录的应用前台 ANR 分开记录。

## 限制与后续

优先修复定时器独立窗口和普通目录导入的身份合并，再用新正式构建复测智能补全与公网查询。未跑全套设备测试、真机音频、蓝牙、真实账号、Windows 或 Linux 桌面功能。

## 同步更新的文档

[文档索引](../README.md)。本轮为测试和记录，开发接口及命令保持现有说明。
