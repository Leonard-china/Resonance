# 2026-10-31 — 本机音频中断只暂停跟听，保留房间和聊天

- 修改日期：2026-10-31（Asia/Shanghai）
- 类型：功能 / 体验修复，验证进行中
- 适用版本：0.6.5 / 39，不升版
- 影响平台：共享播放策略及 Android Media3；Windows 可使用相同本机暂停状态
- 关联：[长时间陪伴需求](2026-10-30-07-companion-room-chat-and-partner-scope.md)

## 需求或问题

短暂声音中断不应拆散陪伴房间。此前 Android 跟听遇到失去音频焦点或耳机拔出直接 `leave`，关闭加密连接和聊天，也需要重新邀请。

## 原因与依据

本机音频输出与房间连接被耦合。若仅暂停播放器而不通知同步驱动，下一个房主播放状态又会让本机开始播放。临时焦点丢失还可能通过 Media3 suppression 回调体现，不能只检查 playWhenReady。

## 具体修改

| 文件 | 关键符号 | 修改 |
| --- | --- | --- |
| [共享契约](../../composeApp/src/commonMain/kotlin/com/resonance/player/platform/ListenTogetherService.kt) | `localAudioPaused`、`pauseGuestAudio`、`resumeGuestAudio` | 声音暂停与连接状态独立，不发送全房间暂停请求 |
| [JVM 服务](../../composeApp/src/jvmMain/kotlin/com/resonance/player/together/JvmListenTogetherService.kt) | 本机暂停方法 | 保留认证连接与文字通道；暂停原生输出，等待用户恢复 |
| [播放驱动](../../composeApp/src/commonMain/kotlin/com/resonance/player/playback/TogetherPlaybackDriver.kt) | 帧与房间收集 | 暂停锁存期间不自动开始、避免持续 seek；用户恢复后按下一帧重新准备并对齐当前房主位置 |
| [Android PlaybackService](../../composeApp/src/androidMain/kotlin/com/resonance/player/PlaybackService.kt) | Media3 Listener | 观察已有 ExoPlayer 焦点/输出变化，不另写焦点管理器；临时/永久焦点丢失与 noisy 均只暂停本机；系统自动恢复不能绕过锁存，明确用户 play 可恢复 |
| [原生房间页](../../composeApp/src/commonMain/kotlin/com/resonance/player/ui/TogetherScreen.kt) | `恢复本机跟听` | 解释本机暂停原因、房间聊天保留及恢复对齐 |
| [共享回归](../../composeApp/src/commonTest/kotlin/com/resonance/player/TogetherPlaybackDriverTest.kt) | 新音频中断回归 | 房主后续播放帧不能自动播放，房间仍是 Guest，主动恢复对齐新位置 |

## 修改前后行为

| 场景 | 修改前 | 修改后 |
| --- | --- | --- |
| 跟听时耳机拔出/失焦 | 退出房间 | 本机声音暂停，房间与聊天继续 |
| 房主继续发送播放状态 | 单纯 pause 会被覆盖 | 锁存保持暂停，不抢占声音 |
| 用户明确恢复 | 重新邀请 | 在原房间恢复并对齐最新进度 |

## 平台与数据影响

新增内存状态，未改变聊天数据、邀请、权限、音频文件或版本。保留 `setAudioAttributes(..., true)` 和 noisy 处理，由 ExoPlayer 管理焦点。Media3 文档依据：[音频焦点](https://developer.android.com/media/optimize/audio-focus)、[Player.Listener](https://developer.android.com/reference/androidx/media3/common/Player.Listener)。

## 验证

新增共享测试，完整编译和执行待补。前一轮 MuMu 七项聊天/邀请集成与界面测试通过，但不能代表本次新音频中断行为已验证。没有实体电话/蓝牙设备，不声明来电、蓝牙和长期锁屏实测通过。

## 限制与后续

Android 长时间完全静音/后台的进程存活仍受系统限制，不伪造静音播放维持前台服务。需要继续用 MuMu 和真实原生播放器验证中断、恢复、长时间会话，再构建最终安装包。

## 同步更新的文档

`docs/README.md` 索引、`docs/DEVELOPMENT.md` 第 7.1 节。
