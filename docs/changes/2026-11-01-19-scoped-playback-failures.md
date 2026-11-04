# 2026-11-01 — 按曲目与请求识别播放失败并区分试听和保存错误

- 修改日期：2026-11-01（Asia/Shanghai）
- 类型：Bug 修复；共享 / Android / Windows；版本保持 0.6.5（39）
- 关联：[播放错误弹窗](2026-11-01-13-seek-commit-and-visible-playback-errors.md)、[审计第 8 项](../PRODUCT_AUDIT.md)

## 需求、原因与依据

旧错误流仅携带字符串，App 对任何错误均将主播放状态设为暂停；试听失败也进入同一流。切歌后迟到的错误没有原始请求标识，A→B→A 时只对比歌曲 ID 仍不足以识别旧错误。桌面启动还会先发出“正在播放”意图，再等待音频设备就绪。

## 具体修改

| 文件 | 关键符号与变化 |
| --- | --- |
| [PlaybackIssue](../../composeApp/src/commonMain/kotlin/com/resonance/player/platform/PlaybackIssue.kt)、[PlatformServices](../../composeApp/src/commonMain/kotlin/com/resonance/player/platform/PlatformServices.kt) | 增加错误来源、曲目和请求号；保留旧字符串 Flow 兼容入口 |
| [DesktopPcmPlayer](../../composeApp/src/desktopMain/kotlin/com/resonance/player/audio/DesktopPcmPlayer.kt) | decoder Session 分配请求号，事件携带会话快照；缺失 URI 明确失败，原生音频就绪才发布实际播放状态 |
| [DesktopPlatformServices](../../composeApp/src/desktopMain/kotlin/com/resonance/player/platform/DesktopPlatformServices.kt) | 原生回调及 UI 消费时双重校验请求号；试听、统计错误单独归类，去掉提前成功事件 |
| [AndroidPlatformServices](../../composeApp/src/androidMain/kotlin/com/resonance/player/platform/AndroidPlatformServices.kt) | 检查控制器身份、显式请求时间与代次；延迟到达的曲目回调只推进代次，不推进时间截止点，避免误丢当前真实失败 |
| [App](../../composeApp/src/commonMain/kotlin/com/resonance/player/App.kt)、[PlaybackErrorDialog](../../composeApp/src/commonMain/kotlin/com/resonance/player/ui/PlaybackErrorDialog.kt) | 当前主播放失败才更新主状态和提供重试；旧错误撤下；试听/统计/设置失败显示各自标题，不更改主播放状态 |

原先试听失败可能让界面显示主播放暂停；现在仅显示试听失败。A 的旧错误到达时，当前 B 或新的 A 不受其影响。统计/配置失败在全屏也可见，弹窗仍是模态提示，并非新的后台通知系统。重试仍服从一起听控制边界。

## 验证

新增 `PlaybackIssueTest` 2 项与 `DesktopPlaybackFailureTest` 1 项：真实桌面解码线程在无 URI 时失败，无需调用 FFmpeg/音频设备；保存 A/B/新 A 的事件，验证旧请求不能匹配新会话。非主播放错误不具备中断标志。最终定向 36 项全过，既有 `PlaybackStateStoreTest` 9 项及 `TogetherPlaybackDriverTest` 4 项一并通过；两端编译通过。

`UiVisualContractTest.playbackErrorOffersRetryInBothThemes` 本次重新渲染并验证重试交互；已实际查看[浅色截图](../audit/2026-11-01/stability/playback-error-light.png)和[深色截图](../audit/2026-11-01/stability/playback-error-dark.png)。这是独立弹窗测试夹具，不是整机播放截图。命令和日志见[稳定性台账](../audit/2026-11-01/stability-verification.json)。最终 Android 时间截止点修正另跑 `compileDebugKotlinAndroid`，记录在 `build/stability-20261101/android-final.log`。

## 数据影响与限制

不修改媒体文件、数据库或 API 配置；无新依赖和生产请求。Android 跨进程错误先后顺序、真实解码就绪与快速切歌声音仍需设备验收。审计第 8 项仅部分解决：共享 PlayerState 尚未新增完整 Preparing/Buffering 等阶段，不能宣称完整状态机改造或所有播放竞态已解决。

README、DEVELOPMENT、优化报告及交接提示词同步更新。后续先完成实机验收；完整加载/缓冲状态另行定点设计。
