# 2026-11-02 — 强制公网中继原生双端验收与测试结束握手

- 日期：2026-11-02（Asia/Shanghai）；类型：测试与验收修复；版本仍为 0.6.5（39）。
- 关联：[中继上线](2026-11-02-04-public-relay-deployment.md)、[原生基线](2026-11-02-01-native-validation-and-local-candidates.md)。

## 需求与现象

用户要求继续完成质量优化，并在额度接近上限时提供明确模型及交接提示词。上一批只有公网字节/TLS 通道验证，尚未证明 Android 服务后台原生跟听经过线上中继。

本批在 Windows 真实 FFmpeg/Java Sound 与 MuMu Android 12 Media3 间建立实际房间。邀请只有 Android 不可用的 `127.0.0.1:1` 直连候选，没有该端口的 adb reverse；中继指向已部署 WSS。因此不能靠局域网直连通过。

首轮 Android 1 项通过，88 条逐秒记录始终 Connected，含后台运行、暂停/跳转/恢复、加密聊天及本机单独暂停后恢复。Windows 在循环尾部报 `Native peer disconnected`。旧夹具两端分别按本地计时，Android 固定少运行两秒且退出时无完成握手，Windows 可能将正常退出判成故障；首轮没有保存 Windows 逐秒数据，不能推断精确断开时刻。未将此结果记为双端联合通过。

## 具体修改

| 文件 | 符号 | 实际变化 |
| --- | --- | --- |
| [TogetherNativeBridgeTest.kt](../../composeApp/src/desktopTest/kotlin/com/resonance/player/together/TogetherNativeBridgeTest.kt) | `windowsNativeHostForMuMu` | 增加显式 `RESONANCE_NATIVE_RELAY_ACCEPTANCE=1`；隔离测试 store 默认 WSS，断开直接候选路径；该模式 Windows 输出静音 |
| 同上 | `guestCompleted` / `finally` | 仅收到对端当前房间完成标记后允许正常结束；没有标记仍断言异常断线；30 秒有界等待；失败也保存逐秒 CSV |
| [TogetherNativePlaybackDeviceTest.kt](../../composeApp/src/androidInstrumentedTest/kotlin/com/resonance/player/TogetherNativePlaybackDeviceTest.kt) | `servicePlayerFollowsWindowsAfterActivityGoesIntoBackground` | 显式 `nativeRelayAcceptance=true` 要求隔离 validation 包和聊天启用；所有原生断言通过后才发送完成标记，等待送达回执后离开 |

默认测试保持 opt-in，不自动访问生产。只修改测试；没有更改播放、同步、聊天或服务业务实现，没有放宽原生暂停/seek/后台/聊天断言。

## 验证与产物

本轮日志和脚本：`build/relay-native-20261102/`；复测独立目录 `run2/`，不覆盖首轮失败。使用 960 秒、220 Hz、低振幅的本地合成 MP3，约 3.84 MB；没有读取或转发用户音乐、密钥、聊天及邀请原文。临时邀请只在测试私有文件之间传递，不进入报告或 Git。

- 首轮：Windows 1 项失败；Android 1 项通过（`android-first.csv` / `windows-failure.xml`）。
- `:composeApp:assembleDebugAndroidTest -I build/final-20261102/isolated-android.init.gradle --offline --no-daemon --no-configuration-cache --max-workers=2`：通过，40 秒；仅更新 `com.resonance.player.validation20261102.test`，未覆盖用户正式包。
- 第二轮：**Windows 1 项、Android 1 项全部通过，无跳过**；Windows 构建/测试共 2m 8s，CSV 89 条、末条 89.121s；Android CSV 88 条，全部保持 Connected。完成标记/回执后正常离开。台账见 [native-relay-verification.json](../audit/2026-11-02/native-relay-verification.json)。

可复现方式：设置 Windows `RESONANCE_NATIVE_BRIDGE=1`、`RESONANCE_NATIVE_RELAY_ACCEPTANCE=1`、`RESONANCE_BRIDGE_COMPANIONS=1`、`RESONANCE_NATIVE_SECONDS=90`、本机合成音频路径与独立输出目录；执行 `:composeApp:desktopTest --tests '*TogetherNativeBridgeTest'`。Android 执行指定单个方法，runner 参数为 `nativeTogether=true nativeCompanions=true nativeRelayAcceptance=true nativeSeconds=90`；只在独立 validation 包传入测试邀请。重跑需独立输出目录、强制 desktopTest 重执行，不能把 Gradle up-to-date 当成新结果。

## 平台、数据、安全与回滚

保护检查点 `.git/audit-checkpoints/relay-native-20261102-090511/files`，同名 codex 分支仅标记 HEAD；原有脏工作区保持。两端仅测试夹具、运行数据和文档变动，无版本变化、发布、生产配置修改或额度提升。测试结束关闭自建房间/音频实例；已有应用、网站、用户库及播放列表不受操作。

## 限制与交接

本次是同一台电脑的 Windows 与 Android 模拟器经真实公网中继，约 90 秒；不是物理手机与独立移动网络，不是多人、两小时稳定性或容量验收。不声明精确同步延迟/音质提升。Windows 安装候选的可见窗口交互验收、完整 Preparing/Buffering 状态、性能实测与边缘限速评估仍待完成。

同步修正文档入口过时的“未部署”和“转场失败”摘要，更新 DEVELOPMENT、OPTIMIZATION_REPORT；接续见 [剩余质量交接](../HANDOFF-2026-11-02-REMAINING-QUALITY.md)。

最终额度工具显示五小时窗口已用 90%、周窗口 44%，不是任务美元成本。已保存接手提示词并结束隔离 validation 测试应用；未启动其他模型或自动任务。
