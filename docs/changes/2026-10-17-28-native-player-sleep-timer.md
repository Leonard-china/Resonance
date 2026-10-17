# 2026-10-17 — 接通双端原生睡眠定时与播完当前歌曲停止

- 修改日期：2026-10-17（Asia/Shanghai）
- 类型：功能 / 体验修复
- 适用版本：0.4.9 / 32 工作区；本轮不升版、不发布
- 影响平台：共享 / Android / Windows（平台限定见下文）
- 关联需求：用户要求落地全部体验建议，并追加页面顶部黑边闪烁反馈；[完整交接与实际验证](../HANDOFF-2026-10-17-UX-ANTIGRAVITY.md)

## 需求或问题

已有睡眠选项和弹窗，但 App 未传入开启回调，实际播放流程不能使用。

## 原因与依据

只有 UI 组件和 PlayerState 字段，没有原生播放器持有的定时执行器；UI 协程定时无法承担 Android 后台播放。

## 具体修改

| 文件 | 关键符号 | 修改与目的 |
| --- | --- | --- |
| [PlaybackSleepTimer.kt](../../composeApp/src/jvmMain/kotlin/com/resonance/player/playback/PlaybackSleepTimer.kt) | `PlaybackSleepTimer` | 使用单调时钟和截止时间，支持取消、更换、仅到期一次及曲终停止。 |
| [PlaybackService.kt](../../composeApp/src/androidMain/kotlin/com/resonance/player/PlaybackService.kt) | `sleep / onPlayWhenReadyChanged / SLEEP_TIMER` | 服务拥有定时器；ExoPlayer 在当前媒体项结束时暂停，防止原生队列或循环继续。 |
| [AudioSessionCommands.kt](../../composeApp/src/androidMain/kotlin/com/resonance/player/AudioSessionCommands.kt) | `SLEEP_TIMER` | 注册并校验睡眠命令，Activity 通过 MediaController 发送选项。 |
| [DesktopPlatformServices.kt](../../composeApp/src/desktopMain/kotlin/com/resonance/player/platform/DesktopPlatformServices.kt) | `sleepTimer / setSleepTimer / onEnded` | 桌面运行作用域拥有计时器，曲终消费事件后不再推进共享队列。 |
| [App.kt](../../composeApp/src/commonMain/kotlin/com/resonance/player/App.kt) | `showSleepTimer / sleepTimer` | 接通弹窗与原生状态；NowPlayingTopBar 菜单显示剩余时间或曲终停止。 |

## 修改前后行为

| 场景 | 修改前 | 修改后 |
| --- | --- | --- |
| 本次使用流程 | 睡眠组件存在但入口和实际计时未接通。 | 播放器菜单可选 15／30／45／60 分钟或曲终停止，查看剩余时间并关闭。 |

## 平台与数据影响

不增加持久偏好或修改音频。暂停期间仍按实际时间倒计时；Android 用 elapsedRealtime，Windows 用单调时钟。应用 UI 退后台不影响原生计时；进程终止或完全退出会取消，冷启动不恢复。音量与播放代次保护保持原状。

## 验证

PlaybackSleepTimerTest 验证截止时间、替换／取消、一次触发与曲终消费；PlaybackStateStoreTest 回归。两端播放层与 Android 命令代码已编译。

| 本次检查 | 结果 |
| --- | --- |
| 定点回归与编译 | 本轮相关回归合计 75 项不同测试通过（失败 0、跳过 0）；双端源码及 Android 测试源码通过编译。完整命令与执行边界见交接文档。 |
| 真机、正式包与网站 | 本轮未执行；交 Antigravity 完成最后验收、正式构建及部署。 |

## 限制与后续

真实手机后台、锁屏、蓝牙、循环模式及 Windows 托盘计时待实机回归。Android 曲终暂停接口语义参考 [Media3 官方文档](https://developer.android.com/reference/androidx/media3/exoplayer/ExoPlayer#setPauseAtEndOfMediaItems(boolean))。

## 同步更新的文档

- [文档索引](../README.md)
- [开发说明第 18 节及对应原章节](../DEVELOPMENT.md#user-experience-update)
- [交接与构建部署提示词](../HANDOFF-2026-10-17-UX-ANTIGRAVITY.md)

