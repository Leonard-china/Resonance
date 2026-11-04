# 2026-11-04 — 隔离音量播放设置弹窗的主窗口毛玻璃采样

- 修改日期：2026-11-04（Asia/Shanghai）
- 类型：Bug 修复
- 适用版本：0.6.5 / 39；未升版
- 影响平台：共享 Compose UI（Android、Windows、Linux）；用户反馈来自 Android 手机
- 关联记录：[音效独立弹窗隔离](2026-11-04-10-audio-effect-dialog-window-isolation.md)

## 需求或问题

用户在 Android 歌曲详情／完整播放器点击“音量”立即卡死。该入口以及“播放选项 → 播放设置 · 音量与速度”都把 `NowPlayingOverlay.playbackSettings` 置为 `true`，打开播放设置弹窗。

## 原因与依据

`NowPlayingOverlay` 将 `FrostedAlertDialog` 别名为 `AlertDialog`。它位于 `LibraryShell` 提供的 `LocalGlassState` 内；此前封装直接在继承该状态的环境中构建 `resonanceGlass`，再把采样 modifier 放入 Material 的独立原生 Dialog 窗口。弹窗各内容槽也继续继承宿主窗口的采样状态。前一份音效修复只修改独立的 `AudioEffectDialog`，未覆盖这一通用封装。

新增隔离回归在修改生产代码前确实失败：`icon must have window-local glass state expected:<true> but was:<false>`，证明弹窗继承了宿主的 Haze 状态。

按 `anr-audit-and-fix` 规则核对打开链路：点击回调只修改内存中的 Compose 状态；音量、倍速、音量平衡的设置回调在用户调整前不执行，未发现这条打开路径上的同步音频分析、磁盘或网络操作。**本次确认的是跨窗口采样状态泄漏及其修复；ADB 无连接设备，未取得原手机 ANR／线程栈，不能将其认定为手机卡死的唯一已证实根因，也不能把桌面通过当作手机复测通过。**

## 具体修改

| 文件 | 关键符号 | 具体修改与目的 |
| --- | --- | --- |
| [FrostedDialogs.kt](../../composeApp/src/commonMain/kotlin/com/resonance/player/design/FrostedDialogs.kt) | `FrostedAlertDialog` | 用 `CompositionLocalProvider(LocalGlassState provides null)` 包住 Material Dialog；在 provider 内构建 `resonanceGlass`，使用现有静态渐变回退；标题、正文、图标、确认和取消内容也不继承宿主采样状态 |
| [PlaybackSettingsDialogTest.kt](../../composeApp/src/desktopTest/kotlin/com/resonance/player/ui/PlaybackSettingsDialogTest.kt) | `dialogIsolatesEveryContentSlotFromHostGlassWithoutChangingHostState` | 验证五个内容槽均已隔离，且 provider 外宿主 Haze 状态仍存在；旧代码可稳定触发失败 |
| 同上 | `volumeOpensAdjustsAndClosesWithPlaybackControlsStillResponsive` | 在真实宿主 Haze source 下覆盖浅／深色、正常高度／短高大字，经按钮或播放选项打开；检查打开无设置副作用，调整音量、倍速和平衡、关闭后仍能切换播放状态 |
| [PlaybackSettingsDialogDeviceTest.kt](../../composeApp/src/androidInstrumentedTest/kotlin/com/resonance/player/PlaybackSettingsDialogDeviceTest.kt) | `volumeOpensAdjustsReopensAndLeavesPlaybackResponsive` | 增加 Android 原生窗口点击回归：打开、调整、关闭、重复三次打开关闭，最后验证播放按钮；60 秒上限，无真实歌曲、账号或网络请求 |

这次在共用弹窗边界修复，因此“歌曲与来源”、歌词工具等使用相同封装的独立窗口也遵循同一隔离规则；主页面和完整播放器内部的毛玻璃采样保持原实现。

## 修改前后行为

| 场景 | 修改前 | 修改后 |
| --- | --- | --- |
| 点击“音量”打开播放设置 | 独立窗口仍使用主页面 Haze 状态 | 弹窗使用静态主题渐变，不采样宿主窗口图层 |
| 弹窗内容使用磨砂组件 | 继承宿主采样状态 | 所有内容槽在隔离环境中构建 |
| 调整音量／倍速／平衡 | 原设置回调 | 沿用原回调；仅打开窗口不调用 |
| 关闭弹窗 | 返回原播放器 | 返回原播放器，宿主采样状态保持原对象 |

## 平台与数据影响

共享封装影响 Android、Windows、Linux 的原生弹窗。未修改平台接口、播放器 DSP、音量持久化、音频分析、播放代次与竞争保护。配置键、数据库／同步包、歌曲文件、标签、权限、凭据和外部请求均无变化。版本未修改。

## 验证

环境：Linux x86_64、Temurin JDK 17.0.21、现有 Android SDK／Build Tools 36。使用仓库 Wrapper；环境变量指向本机已有工具，不提交工具路径或签名凭据。

| 命令或操作 | 环境 | 结果 |
| --- | --- | --- |
| `:composeApp:desktopTest --tests 'com.resonance.player.ui.PlaybackSettingsDialogTest.dialogIsolatesEveryContentSlotFromHostGlassWithoutChangingHostState'`（生产修改前） | Linux / 原生离屏 Compose | **预期失败**：1 项失败，确认旧封装继承宿主采样状态 |
| `:composeApp:desktopTest --tests 'com.resonance.player.ui.PlaybackSettingsDialogTest' --tests 'com.resonance.player.ui.PlayerRecoveryUiTest' --tests 'com.resonance.player.ui.UiVisualContractTest'`（生产修改后首轮） | Linux | **失败**：28 项中 27 通过、1 失败。已有 26 项全部通过、隔离用例通过；新交互测试误将底部“音量”按钮当成滑块，导致缺少 SetProgress 断言失败。已将定位改为同时匹配内容描述及 SetProgress，不修改生产交互来迎合测试 |
| `:composeApp:desktopTest --tests 'com.resonance.player.ui.PlaybackSettingsDialogTest'`（修正测试定位后） | 同一生产代码，Linux | **通过**：2 项、0 失败、0 跳过。交互测试循环覆盖浅／深色 × 正常／短高大字四种组合 |
| 上一行定向测试与 `:composeApp:assembleRelease :composeApp:assembleDebugAndroidTest` 联合执行 | Linux / Android SDK 36 | **通过**：`BUILD SUCCESSFUL in 1m 26s`；本次 Release APK、Android 仪器测试 APK 构建成功；并非 Android 设备测试通过，也未执行全量桌面／Android 回归 |
| `apksigner verify --verbose` 本次独立 APK | Build Tools 36 | **通过**：`Verifies`、v2=true、1 个签名者；v1=false，不误写为 v1／v2 均通过 |
| `zipalign -c -P 16 4` 本次独立 APK | Build Tools 36 | **通过**：退出码 0 |
| `aapt dump badging` 本次独立 APK | Build Tools 36 | **通过**：正式包 `com.resonance.player`、versionName=0.6.5、versionCode=39 |
| `adb devices -l` | 当前 Android 连接 | **环境受阻**：没有连接设备；Android 仪器测试未执行 |

上述 Wrapper 参数均加 `--no-configuration-cache --console=plain`。仓库 `gradlew` 使用 CRLF，直接执行曾因 `/bin/sh^M` 启动失败；本次沿用前轮做法，以 JDK 的 `java -classpath gradle/wrapper/gradle-wrapper.jar org.gradle.wrapper.GradleWrapperMain` 启动同一 Wrapper，未改动脚本。首轮日志／XML 保存在忽略目录 `build/volume-dialog-fix-2026-11-04/`，没有把历史测试结果当作本轮通过。

签名工具首次启动因 PATH 中没有 `java` 失败；将本机已有 JDK 的 bin 放入该次命令的 PATH 后重跑通过，未更改仓库工具配置。最终构建日志与 2 项回归 XML 同样保存在上述忽略目录。

### 本次独立安装包

- [Resonance-0.6.5-volume-dialog-fix-2026-11-04.apk](../../release/Resonance-0.6.5-volume-dialog-fix-2026-11-04.apk)
- 大小：167,402,239 字节（约 159.65 MiB）。
- SHA-256：`56803e3c950996d219eb59e5a583f579bc372cfa316494a20630745ec19ebeec`，从本次实际文件计算；[校验文件](../../release/Resonance-0.6.5-volume-dialog-fix-2026-11-04.apk.sha256)。
- 本地构建并核验，未安装手机、未上传或发布；APK 与校验文件均为忽略的本地交付产物。

## 限制与后续

原手机未连接 ADB，Android 仪器测试和原手机 GPU／ROM 下的最终复测尚未执行；Windows 原生窗口也未运行。若安装本次 APK 后仍卡死，下一步应在原手机捕获点击时的主线程／RenderThread／ANR 证据，继续定位。此次没有宣称实机卡死已最终验收解决。

## 同步更新的文档

- [文档入口与变更索引](../README.md)。
- [开发说明第 11 节](../DEVELOPMENT.md#ui)：记录通用原生弹窗的采样隔离边界与实际验证限制。
