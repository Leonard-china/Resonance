# 2026-11-04 — 隔离音效弹窗的跨窗口模糊并约束预设列表

- 修改日期：2026-11-04（Asia/Shanghai）
- 类型：Bug 修复
- 适用版本：0.6.5 / 39；未升版
- 影响平台：共享 Compose UI（Android、Windows、Linux）
- 关联需求：用户点击播放器底部“音效”立刻卡住，预设列表尚未出现。

## 需求或问题

进入音效弹窗即无响应，不能选预设。用户已明确卡死发生在打开时。

## 原因与依据

静态调用链中打开只改变 Compose 弹窗状态，DSP 只在选择预设后触发。`AudioEffectDialog` 在原生独立 Dialog 窗口中复用播放器的 Haze 磨砂层，属于跨窗口采样风险；该风险已移除。**原设备现已断开，没有卡死时的 ANR/线程栈，不能把上述风险认定为已证实的唯一根因。**

## 具体修改

| 文件 | 关键符号 | 修改 |
| --- | --- | --- |
| [AudioEffectDialog.kt](../../composeApp/src/commonMain/kotlin/com/resonance/player/ui/AudioEffectDialog.kt) | `AudioEffectDialog` | 原生弹窗用主题静态底色和边框，移除对宿主 Haze 的依赖；高度最多 560dp，列表按剩余空间滚动；预设使用稳定 id；去掉“硬件加速”文案 |
| [PlayerRecoveryUiTest.kt](../../composeApp/src/desktopTest/kotlin/com/resonance/player/ui/PlayerRecoveryUiTest.kt) | `audioDialogHasBoundedListAndSelectionDoesNotInvokePlaybackWorkUntilChosen` | 在真实播放器共享 Haze 环境打开弹窗，确认打开未触发预设处理、选择低音后正确关闭 |
| [PlayerDialogsDeviceTest.kt](../../composeApp/src/androidInstrumentedTest/kotlin/com/resonance/player/PlayerDialogsDeviceTest.kt) | `audioEffectsOpenSelectAndCloseWithoutStoppingPlayerControls` | Android 原生窗口回归：打开、选择、播放器继续响应、重开关闭；60 秒上限，无真实音频/账号 |

## 修改前后行为

| 场景 | 修改前 | 修改后 |
| --- | --- | --- |
| 打开原生音效窗口 | 复用宿主模糊采样状态 | 独立静态主题材质，不跨窗口采样 |
| 短窗口/大字预设 | 固定列表高度挤压标题和关闭按钮 | 有界列表使用剩余高度，预设滚动 |
| 选择前 | 不应触发 DSP | 回归明确确认没有调用预设回调；选中后原回调执行 |

## 平台与数据影响

共享 UI 覆盖 Android、Windows、Linux；DSP 算法、原 PCM、预设持久化、播放任务与原文件均不变。没有新增权限、外部请求或配置键。

## 验证

| 命令或操作 | 环境 | 结果 |
| --- | --- | --- |
| Wrapper `:composeApp:desktopTest`（显式开启公网歌词验收） | Linux / JDK 17 | 通过：346 项，337 通过、9 项集成入口跳过；发生于最终歌词 UI 间距调整之前 |
| 最终 8 类针对性测试 + `:composeApp:assembleDebug :composeApp:assembleDebugAndroidTest :composeApp:lintRelease` | 最后源码，Linux / Android SDK 36 | 通过：33 项全部通过、0 跳过；Debug／测试 APK 成功，Lint 0 Error／107 Warning |
| 真实播放器共享 Haze 环境打开、选低音、关闭 | Linux 离屏原生语义 | 通过；打开时预设回调未触发、选择后正确关闭；不把多窗口混合截图当实机视觉证明 |
| Android `PlayerDialogsDeviceTest` | 本轮已编译测试 APK；adb 无设备 | 环境受阻：安装未成功，仪器测试未执行；原手机卡死没有最终验收 |

完整命令、跳过名单、原生截图、最后源码摘要、产物校验与早期失败的处理见[本轮验证报告](../audit/2026-11-04/follow-up/VALIDATION.md)。

## 限制与后续

桌面渲染测试通过不能等同于用户手机 GPU/ROM 的 ANR 复测。原设备断开，安装和仪器测试受阻；没有宣称手机卡死已完全解决。下一步可在原设备运行上述 Android 用例并检查点击后的主线程与渲染线程；Windows 原生窗口未运行。

## 同步更新的文档

[文档索引](../README.md)、[开发说明第 11 节](../DEVELOPMENT.md#ui)。
