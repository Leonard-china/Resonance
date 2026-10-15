# 2026-10-15 — 简化自动音量平衡并独立提供使用说明

- 修改日期：2026-10-15（Asia/Shanghai）
- 类型：功能 / 重构
- 版本：0.3.5 / 22，未升版
- 平台：Android / Windows（平台差异见下文）
- 关联：用户本轮深度优化需求；[验证与交接](2026-10-15-12-verification-and-handoff.md)

## 需求、原因与依据

用户难以理解多组增益、逐曲/歌单音量与校准按钮。旧激活按曲目/歌单覆盖全局值，形成不可见叠加，切歌后感受难预测。

## 具体修改

- 播放器与设置只保留一个音量平衡开关；详细帮助独立为 AudioHelp 页面，解释首次分析、系统/主音量、峰值限制和失败处理。
- PlaybackStateStore 与 Android 后台切歌只采用全局主音量及自动响度缓存/分析；关闭自动增益归零。
- 保留任务代次、用户修改版本、分析缓存与 PCM 平滑/限幅；历史逐曲/校准偏好保留但不参与正常激活。

| 文件 | 关键修改 |
| --- | --- |
| [ui/NowPlayingOverlay.kt](../../composeApp/src/commonMain/kotlin/com/resonance/player/ui/NowPlayingOverlay.kt) | VolumeAndSpeedRow，删除校准/数值面板 |
| [ui/MusicFeatureScreens.kt](../../composeApp/src/commonMain/kotlin/com/resonance/player/ui/MusicFeatureScreens.kt) | AudioBalanceHelpScreen，独立说明 |
| [playback/PlaybackStateStore.kt](../../composeApp/src/commonMain/kotlin/com/resonance/player/playback/PlaybackStateStore.kt) | activate，统一全局音量 |
| [PlaybackService.kt](../../composeApp/src/androidMain/kotlin/com/resonance/player/PlaybackService.kt) | updateTrackAudio，后台行为一致 |

## 修改前后

以前有多组覆盖值和手动校准；现在一次开启即可自动分析与平衡，主音量仍由用户控制。

## 平台与数据影响

不修改原音频，旧偏好不删除；两端播放及恢复受影响。

## 验证

PlaybackStateStoreTest、AudioSessionIntegrationTest 检查全局音量、竞争和后台切歌。

实际命令、结果与设备/网络限制统一记录在 [本轮验证](2026-10-15-12-verification-and-handoff.md)，不把模拟服务测试当作付费API或B站真实下载通过。

## 限制与后续

无静音/峰值余量时不强行放大；真实设备的听感依赖源录音与输出。

## 同步文档

[开发说明](../DEVELOPMENT.md)、[索引](../README.md)、[项目说明](../../README.md)；外部请求/凭据见 [第三方说明](../THIRD_PARTY.md)。
