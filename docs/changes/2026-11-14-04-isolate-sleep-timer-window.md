# 2026-11-14 — 修复 Android 播放页打开睡眠定时器无响应

- 修改日期：2026-11-14（Asia/Shanghai）
- 类型：Bug 修复
- 适用版本：0.6.11 / 45
- 影响平台：共享 Compose 定时器弹窗，来源为 Android 设备复现
- 关联：[正式包探索性测试](2026-11-14-03-android-exploratory-testing.md)

## 需求、原因与修改

Android 正式包播放或暂停歌曲后，点击播放器「定时」可以看到弹窗，但窗口焦点事件约五秒未处理，系统弹出无响应。独立复现的主线程栈处于 `SnapshotStateObserver` 的 Haze 状态更新，并调用 `HazeEffectNode.updateEffect` 查询窗口 ID。定时器 `Dialog` 原先使用宿主 `LocalGlassState`，将宿主窗口的毛玻璃状态带入独立窗口。

| 文件 | 关键符号 | 修改 |
| --- | --- | --- |
| [SleepTimerDialog.kt](../../composeApp/src/commonMain/kotlin/com/resonance/player/ui/SleepTimerDialog.kt) | `SleepTimerDialog` | 在原生窗口内容内提供空 `LocalGlassState`，由现有玻璃绘制逻辑使用窗口内的静态渐变。 |
| [PlayerDialogsDeviceTest.kt](../../composeApp/src/androidInstrumentedTest/kotlin/com/resonance/player/PlayerDialogsDeviceTest.kt) | `sleepTimerWindowDoesNotObserveHostGlass` | 在带 Haze 状态的宿主打开原生窗口，检查标题祖先没有 Haze 效果节点，并验证选择、重开、倒计时更新和取消。 |

## 修改前后及平台数据影响

之前打开弹窗会订阅宿主的毛玻璃状态。修复后弹窗在自己的窗口绘制。Android、Windows、Linux 共用该弹窗。定时器的选择、倒计时、到点暂停和播放会话规则沿用现有实现，无音频文件或持久化格式变化。

## 验证与限制

| 命令或操作 | 结果 |
| --- | --- |
| `:composeApp:assembleDebug :composeApp:assembleDebugAndroidTest`，隔离快照、单 worker、2 GiB 进程树 | 首轮常规 Gradle 编译在内存高水位下进展过慢，已停止。分步方式完整编译 187 个 Android 相关 Kotlin 文件，将输出交给 Gradle 打包，应用 APK 与设备测试 APK 均成功。最终仅重新编译导入身份实现文件并重新打包，构建前后的其他源码哈希一致，调试签名校验通过。 |
| `adb -s emulator-5560 shell am instrument -w -r`，运行 `PlayerDialogsDeviceTest`、`BatchEnrichDialogDeviceTest`、`NativePlaybackQueueDeviceTest`、`LyricsPlayerRegressionDeviceTest`、`PreferenceCommitDeviceTest` 和 `AudioRelinkDeviceTest` | 第一轮和最终包各通过 11 项，均无失败。最终结果见 `instrumentation-final.log`。 |
| 最终 Debug APK，播放页暂停时点「定时」，停留 6 秒以上，然后选择「播完当前歌曲停止」并继续播放 | 通过。窗口等待后仍响应，Android 媒体会话停在 `QA Alpha` 曲尾 32034 ms，状态 `STOPPED`、无错误。截图和媒体会话日志已留存。 |
| 最终 Debug APK，曲库搜索／多选／智能补全 | 勾选名称规范后完成 1 首，AI 调用 0 次。系统未记录最终 Debug 包 ANR 或 Java 崩溃。 |

产物与日志位于忽略目录 `build/android-fixes-2026-11-14/`。最终 APK 仍为 Debug 独立包。真机、Windows/Linux 桌面窗口和在线歌词或封面查询未验证，本轮未发布正式 APK。

## 同步更新的文档

[文档索引](../README.md)和[开发说明](../DEVELOPMENT.md)。
