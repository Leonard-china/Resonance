# 2026-11-06 — 修复切歌准备期间暂停后恢复旧曲

- 修改日期：2026-11-06（Asia/Shanghai）
- 类型：Bug 修复
- 适用版本：0.6.8 / 42，不升版
- 影响平台：共享、Android、Windows、Linux
- 关联记录：[前置实现](2026-11-05-40-auto-next-playback-state.md)；用户批准实施本轮审查中的四项改进。

## 需求或问题

切歌准备期间发出暂停，待准备结束再恢复，界面选中新曲而原生播放器继续上一首。

## 原因与依据

`PlaybackStateStore.activate` 准备结束总是清空 `pendingStart`；暂停分支跳过 `onReady`，新曲从未提交。App 的恢复入口直接调用平台 `setPlaying(true)`，只能恢复原生旧曲。

## 具体修改

| 文件 | 关键符号 | 实际修改 |
| --- | --- | --- |
| [PlaybackStateStore.kt](../../composeApp/src/commonMain/kotlin/com/resonance/player/playback/PlaybackStateStore.kt) | `StartAction.prepared / requestPlaying / startPreparedPlayback` | 暂停后保留已准备的启动动作；准备未完成时只记播放意图，完成后提交选中曲目；启动前消费动作，避免重复提交；保留原生事件代次和音量修订保护。 |
| [App.kt](../../composeApp/src/commonMain/kotlin/com/resonance/player/App.kt) | 助手、悬浮歌词、键盘、播放器播放入口 | 四处统一经 `requestPlaying` 分发用户意图，避免绕过待启动曲目检查；播放状态仍由原生回调确认。 |
| [PausedPreparationTest.kt](../../composeApp/src/desktopTest/kotlin/com/resonance/player/playback/PausedPreparationTest.kt) | 五项新增回归 | 准备前/后恢复、只提交一次、换曲和音量刷新、清空/退出、准备失败。 |

`playbackFailed` 清理挂起任务与代次，避免错误后迟到任务重启。原生自动切歌附着继续保持只读快照语义，不额外 play/pause。共享命令覆盖 Android Media3 和 Windows/Linux 桌面播放器，未更改平台播放器实现。

## 修改前后行为

| 场景 | 修改前 | 修改后 |
| --- | --- | --- |
| 暂停直到准备完成再恢复 | 恢复上一首 | 提交已选择的新曲 |
| 准备未完成就恢复 | 可能短暂恢复旧曲 | 只记录意图，等待新曲准备 |
| 反复恢复/换曲/退出 | 已丢失的启动动作无法正确恢复 | 当前动作最多提交一次，过期动作清除 |

## 平台与数据影响

Android、Windows、Linux 共用相应共享逻辑，平台存储适配分别核对。版本保持 `version.properties` 的 0.6.8 / 42。保留原音频、收藏与歌曲 ID，不删除或改写原音频/标签；不改变 `.resonance` 包格式，不新增权限、外部服务或付费请求，不读取真实 Key 或签名文件。

## 验证

本次实际编译、定点回归、资源限制及未执行项见[本轮验证](../audit/2026-11-06/review-fixes/VALIDATION.md)。日志和本机脚本保留在忽略目录 `build/review-fixes-2026-11-06/`，不引用上轮审查或旧包结果作为本次通过依据。

## 限制与后续

仅交付源码、回归及文档；不打包、安装、升版或发布。Android 设备、Windows 原生操作、物理外置盘及长时间播放不由 JVM 替身测试代替；实际执行范围以上述验证记录为准。

## 同步更新的文档

`docs/README.md` 变更索引、`docs/DEVELOPMENT.md` 对应当前行为与回归入口，核对日期 2026-11-06。
