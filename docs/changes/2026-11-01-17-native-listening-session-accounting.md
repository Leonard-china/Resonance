# 2026-11-01 — 将听歌计时交给原生播放会话并去重完播记录

- 修改日期：2026-11-01（Asia/Shanghai）
- 类型：Bug 修复；共享 / Android / Windows；版本仍为 0.6.5（39）
- 关联：[审计第 5 项](../PRODUCT_AUDIT.md)、[统计完整性与批量写入](2026-11-01-12-listening-stats-integrity-and-batching.md)

## 需求、原因与依据

原计时在 App 的 `LaunchedEffect(isPlaying, trackId)` 中。Activity 组合销毁后服务仍可播放，但计时停止；暂停后 effect 重启会清空完播标记，95% 附近反复暂停可重复计数。这是源码确认的生命周期缺口，本轮没有冒充 Android 实机复现。

## 具体修改

| 文件 | 关键符号与变化 |
| --- | --- |
| [ListeningSessionTracker](../../composeApp/src/commonMain/kotlin/com/resonance/player/playback/ListeningSessionTracker.kt) | 原生会话单调时间与实际进度共同计量；累计不足一秒的小数；同一轮完播只记一次 |
| [ListeningStatsRepository](../../composeApp/src/jvmMain/kotlin/com/resonance/player/playback/ListeningStatsRepository.kt) | 按数据目录共享单写者 IO 队列、Store 与错误流，重建 Activity 不再新建独立统计副本 |
| [ListeningStatsStore](../../composeApp/src/jvmMain/kotlin/com/resonance/player/playback/ListeningStatsStore.kt) | 暴露内部待写标记，只有成功落盘后清除保存失败提示 |
| [PlaybackService](../../composeApp/src/androidMain/kotlin/com/resonance/player/PlaybackService.kt) | Media3 服务事件及 1 秒采样；seek 不计跳跃，自动循环另开一轮；服务结束提交剩余增量 |
| [DesktopPlatformServices](../../composeApp/src/desktopMain/kotlin/com/resonance/player/platform/DesktopPlatformServices.kt) | DesktopAudioPlayer 消费原生会话快照，暂停/切歌/退出刷盘；退出最多等待 3 秒 |
| [AndroidPlatformServices](../../composeApp/src/androidMain/kotlin/com/resonance/player/platform/AndroidPlatformServices.kt)、[App](../../composeApp/src/commonMain/kotlin/com/resonance/player/App.kt) | UI 使用同一统计仓库，移除 UI 计时循环，避免双记账 |

保留 95% 完播阈值；暂停恢复不重置，真正的新一轮播放才重置。缓冲无进度、暂停、seek 跳跃不产生听歌时长；变速按播放前速率折算实际时间。时钟与音频进度的小幅抖动使用累计时间预算，避免每个采样周期都向下丢时长。一起听远端音频及试听不进入个人曲库统计。

## 前后行为与数据影响

| 场景 | 修改前 | 修改后 |
| --- | --- | --- |
| 95% 后暂停再恢复 | effect 重启，可能再次完播 | 同一会话去重；新循环可另计一次 |
| Android 界面重建 | 计时随组合中断 | 服务拥有计时；设备实测待收尾 |
| 写盘失败后恢复 | 多入口可能各持 Store | 同目录同写者；不重放已经计入内存的增量 |

旧 JSON、备份、损坏保护、10 秒批量策略保持兼容；未迁移用户数据，未修改音频、权限或服务器。首次同步读取/快照锁成本仍待测量。进程强杀可能丢失尚未落盘的批次；队列积压或磁盘失败时损失窗口可能超过 10 秒，不能视为硬上限。跨会话不足一秒的尾数不持久化。

## 验证

本轮最终命令、结果和源码哈希见 [稳定性台账](../audit/2026-11-01/stability-verification.json)：36 项定向桌面测试全部通过，包含新增 `ListeningSessionTrackerTest` 4 项、`ListeningStatsRepositoryTest` 2 项，以及原 `ListeningStatsStoreTest` 5 项；两端编译通过。日志 `build/stability-20261101/verified.log`。临时数据验证暂停/循环、2 倍速、seek、缓冲、抖动、共享仓库、不可写与恢复，不访问真实历史。

Android Activity 销毁但服务继续、音频焦点和真实长时播放尚未设备验证。参考 [Media3 播放事件文档](https://developer.android.com/media/media3/exoplayer/listening-to-player-events)；未升级依赖。

## 同步文档与后续

更新 README 索引、DEVELOPMENT、OPTIMIZATION_REPORT 与质量收尾交接。原生设备检查交给最终验收；不能把纯逻辑用例当成后台设备通过。
