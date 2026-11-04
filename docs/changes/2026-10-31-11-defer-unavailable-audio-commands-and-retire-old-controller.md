# 2026-10-31 — 音频命令等待会话授权，关闭旧控制器后不再执行排队操作

- 修改日期：2026-10-31（Asia/Shanghai）
- 类型：Bug 修复与设备回归
- 适用版本：0.6.5 / 39，不升版
- 影响平台：Android
- 关联：[本机音频中断](2026-10-31-05-keep-room-during-local-audio-interruption.md)、[原生验收](2026-10-31-08-isolated-native-validation-and-release-build.md)

## 需求或问题

独立 MuMu 原生长时测试刚进入播放便因 `SET_AUDIO_GAIN` 尚未注册而抛出 `IllegalStateException`，测试应用退出，Windows 主机随后等待超时。此项是真实控制器错误，与此前测试素材权限问题分开记录。

## 原因与依据

`AndroidPlatformServices.sendAudioCommand` 把命令暂不可用直接当作不可恢复的异常；会话授权可能变化，Activity 重建还可能使旧对象的异步连接或排队动作晚到。日志证明本次命令可用性判断为 false，但无法仅凭该日志确定当时服务为何撤销/尚未提供命令，因此不把具体时序推断写成已证实原因。

## 具体修改

| 文件 | 关键符号 | 修改 |
| --- | --- | --- |
| [AndroidPlatformServices.kt](../../composeApp/src/androidMain/kotlin/com/resonance/player/platform/AndroidPlatformServices.kt) | `sendAudioCommand`、`flushAudioCommands` | 每种暂不可用命令只保留最新参数，授权变化后重试；最多等待 5 秒后给出错误提示，避免主线程异常和无限排队 |
| 同上 | `MediaController.Listener`、`close`、`withPlayer` | 监听命令授权与断连；关闭时清除动作/计时任务，忽略旧异步连接和后续操作；重复关闭安全 |
| 同上 | 播放暂停回调 | 修正另一条焦点/输出变化回调仍会离房的问题，与服务层一致，只暂停本机跟听并保留房间/聊天 |
| [AudioControllerLifecycleDeviceTest.kt](../../composeApp/src/androidInstrumentedTest/kotlin/com/resonance/player/AudioControllerLifecycleDeviceTest.kt) | `revokedGainDefersUntilGrantedAndDisposedActivityCannotChangeNewController` | 在真实 MediaSession 撤销/恢复增益命令，验证排队后应用；重建 Activity 后旧对象不能改变新控制器音量/增益 |

## 修改前后行为

| 场景 | 修改前 | 修改后 |
| --- | --- | --- |
| 增益命令暂时不可用 | 抛异常，可能结束进程 | 有限等待授权，恢复后应用最新值，超时友好提示 |
| Activity 重建、旧连接晚到 | 旧排队动作仍可能执行 | 已关闭对象不再执行播放动作 |
| 跟听时本机音频被打断 | 部分回调仍离开房间 | 保留房间与聊天，主动恢复后继续跟听 |

## 平台与数据影响

仅 Android 控制器生命周期与命令调度改变；Windows、房间协议、聊天/伙伴文件格式和已有数据不变。不新增权限、不修改系统设置、不访问模型凭据、不删除用户音频。意外会话断连会提示重新打开应用，不声称已经实现服务自动重连。

## 验证

| 命令或操作 | 环境 | 结果 |
| --- | --- | --- |
| 私有 `.codex` Debug/测试 APK 构建 | JDK17，2GB/两 worker，`private-audio-lifecycle-5.log` | 通过，3m2s，69 tasks |
| `AudioSessionIntegrationTest` 与新增生命周期测试 | MuMu 独立包，`native-isolated-20261031-064404/audio-lifecycle-mumu.log` | 通过，共 5 项，5.520s |
| 七项聊天/配对/直邀及反向原生测试 | 同一监督运行 `native-supervisor-5.log` | 通过后才进入长时阶段，详细统计在原生验收记录补充 |
| 双小时原生播放/聊天 | Windows PCM → MuMu Media3 | 06:48 开始，仍在运行，未视为通过 |
| 最终 Release 单测、Lint、APK/EXE | `final-release-5.log` | 通过；APK 重新验签/哈希，Windows EXE 重新哈希、确认未签名，见[本次产物记录](2026-10-31-13-current-release-artifacts-and-device-verification.md) |

## 限制与后续

等待双小时结果及最终 Release。未覆盖真实电话、蓝牙、系统杀进程或所有 MediaSession 断连原因；不能从命令撤销回归推断这些场景均已通过。

## 同步更新的文档

`docs/README.md` 索引；`docs/DEVELOPMENT.md` 第 7.1 节的本机音频与会话生命周期边界。
