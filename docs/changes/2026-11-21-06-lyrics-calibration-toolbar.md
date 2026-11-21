# 2026-11-21 — 修复歌词校准控件被播放器截断

- 修改日期为 2026-11-21（Asia/Shanghai），类型为原生播放器界面修复。
- 当前版本为 `0.7.0 / 70`，适用于 Android、Windows 和 Linux 的共享 Compose 界面。
- 关联[清岚播放器布局](2026-11-21-05-clearwater-fresh-ui-apple-motion-and-apk.md)。

## 需求或问题

手机正在播放页先展开「歌词工具」，再展开时间校准。用户截图中，生成、识别和翻译按钮及其说明占满歌词区，提前、重置和延后按钮只露出上缘。听歌时看不到正在校准的歌词。

## 原因与依据

`LyricsContent` 将工具、说明和校准按钮放在同一个非滚动的纵向布局内。标题、工具和按钮先测量，歌词列表只能使用余下高度。窄屏上的工具按钮与说明换行后，所需空间可能超过整个歌词面板。

## 具体修改

| 文件 | 关键符号与修改 |
| --- | --- |
| [NowPlayingOverlay.kt](../../composeApp/src/commonMain/kotlin/com/resonance/player/ui/NowPlayingOverlay.kt) | `LyricsContent` 在同步歌词标题行提供「校准」。`LyricTimingBar` 在原位显示延后、当前偏移与重置、提前和完成。进入时恢复跟随播放，复用当前列表、进度与单曲偏移回调。 |
| [MusicFeatureScreens.kt](../../composeApp/src/commonMain/kotlin/com/resonance/player/ui/MusicFeatureScreens.kt) | `TranslationControls` 保留原操作、额度说明和错误提示。已有歌词的工具弹窗复用隔离宿主毛玻璃的 `FrostedAlertDialog`，正文可滚动。打开音频识别确认或进入设置时先收起工具弹窗。 |
| [AssistantHelp.kt](../../composeApp/src/commonMain/kotlin/com/resonance/player/tutorial/AssistantHelp.kt) | 更新播放器内的帮助路径，说明何时提前、何时延后及每步时长。 |
| [LyricsPlayerRegressionTest.kt](../../composeApp/src/desktopTest/kotlin/com/resonance/player/ui/LyricsPlayerRegressionTest.kt) | 用完整播放器检查窄屏、大字号、横屏下按钮与歌词的布局，检查两个方向跨过时间戳时的高亮、重置、结束校准和纯文本入口。 |
| [LyricsPlayerRegressionDeviceTest.kt](../../composeApp/src/androidInstrumentedTest/kotlin/com/resonance/player/LyricsPlayerRegressionDeviceTest.kt)、[LyricsTranslationDeviceTest.kt](../../composeApp/src/androidInstrumentedTest/kotlin/com/resonance/player/LyricsTranslationDeviceTest.kt) | 增加 Android 大字号校准用例，并通过关闭工具窗口验证翻译结果、失败后原文与重试。 |

## 修改前后行为

| 操作 | 原表现 | 现表现 |
| --- | --- | --- |
| 校准同步歌词 | 先展开所有工具，再展开校准 | 点「校准」直接调整；也可走「歌词工具 → 校准时间轴」 |
| 核对唱到哪句 | 时间按钮与歌词争用竖向空间 | 按「延后」或「提前」每次调整 0.5 秒；仍可看高亮、暂停或拖动播放进度。勾号结束校准 |
| 查看已有歌词的其他工具 | 工具在正文上方展开 | 独立窗口提供生成、识别、翻译和设置。纯文本仅提供适用工具 |

## 平台与数据影响

三端共用相同的入口。负偏移让歌词延后，正偏移让歌词提前，重置沿用单曲已有偏移回调。偏移仍保存在原本机缓存，保留已有配置、曲库、音频文件、同步包格式和收费操作的原确认流程。

## 验证

| 命令或操作 | 环境 | 结果 |
| --- | --- | --- |
| `:composeApp:desktopTestClasses`，随后以 JUnitCore 运行 `LyricsPlayerRegressionTest`、`LyricsMotionTest` 和 `PlayerRecoveryUiTest` | Linux、JDK 17、Skiko 软件渲染，构建与测试进程树限额 2 GiB | 通过，9 项成功。合并运行 Gradle 编译与测试的首轮在编译后手动停止，最终验证分步运行。 |
| 检查 `composeApp/build/lyrics-regression/calibration-*.png` | 390×844 默认字号、360×640 的 1.6 倍字号、720×360 的 1.5 倍字号 | 通过。三个视口均显示完整校准控件、高亮歌词及播放控制，歌词未覆盖按钮。 |
| `:composeApp:mergeExtDexDebug`，随后运行 `:composeApp:assembleDebug :composeApp:assembleDebugAndroidTest` | Linux、JDK 17、Android SDK 36，单 worker，进程树限额 2 GiB | 通过。首次合并依赖时 D8 耗尽 768 MiB JVM 堆。用独立的 1152 MiB JVM 完成合并后，在 768 MiB JVM 中完成应用与测试 APK 打包。 |
| `adb -s emulator-5560 shell am instrument -w -r -e class 'com.resonance.player.LyricsPlayerRegressionDeviceTest#timingButtonsAndLiveLyricStayVisibleAtLargeText,com.resonance.player.LyricsTranslationDeviceTest' com.resonance.player.debug.test/androidx.test.runner.AndroidJUnitRunner` | 本次 APK，Android 16 专用 AVD `resonance-api36`，390×844 dp | 通过，3 项成功。1.6 倍字号下可调节并重置当前句，播放键可见。翻译成功显示双语，失败保留原文，可重开弹窗重试。 |
| `git diff --check`（本次涉及的文件）、校验本记录与索引的相对链接 | Linux 工作区 | 通过。 |

调试包为 `composeApp/build/outputs/apk/debug/Resonance-0.7.0-debug.apk`，包名 `com.resonance.player.debug`，SHA-256 为 `1c2b7d0842999a632d802e4671064abd557b6976758dc2ccb61dd8b6121cbdaf`。

## 限制与后续

实际设备测试使用模拟器和合成歌词。用户截图中的手机、真实歌曲的听感及 Windows 实机未验证。当前改动交付在工作区和 Android Debug 包，正式版仍需另行发布。

## 同步更新的文档

[文档索引](../README.md)及[开发说明第 8 节](../DEVELOPMENT.md#lyrics)。
