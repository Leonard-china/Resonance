# 2026-10-31 — 将已校验的成员贡献音频接入创建者播放队列

- 修改日期：2026-10-31（Asia/Shanghai）
- 类型：多人音频队列接线；版本 0.6.5 / 39，不升版
- 影响平台：共享 Compose 应用与 Android、Windows JVM 房间服务
- 关联：[多人队列与基础控制](2026-10-31-31-route-room-playback-controls-and-queue.md)、[传输串行化](2026-10-31-29-serialize-room-audio-transfers.md)

## 需求或问题

成员贡献歌曲后，协调端即使完整接收并校验，也未将该曲目交给原生播放器队列，因而队列条目不代表能够播放。临时文件也不能写入普通恢复快照作为永久曲库歌曲。

## 原因与依据

初始实现只将临时曲加入房间内部 `sharedTracks`，创建者播放器使用的 `App` 队列是另一份；主机发送当前歌曲时又排除了所有 `together:` 临时曲。

## 具体修改

| 文件 | 符号 | 修改 |
| --- | --- | --- |
| [JvmListenTogetherService.kt](../../composeApp/src/jvmMain/kotlin/com/resonance/player/together/JvmListenTogetherService.kt) | `canShareTrack`、`mediaKey`、`mediaEnd` | 通过接收大小与 SHA-256 校验后，把拥有的缓存临时曲目提交到创建者播放动作流；仅允许当前世代服务拥有的临时文件重新发送，使用队列中稳定的不透明媒体 ID |
| 同上 | `publishQueue`、`sendMediaToMember`、`leave` | 保留当前房间已缓存的贡献曲；创建者退出并正在播放临时曲时暂停，原有按世代清理仍仅处理服务自有缓存 |
| [App.kt](../../composeApp/src/commonMain/kotlin/com/resonance/player/App.kt) | 创建者离房与播放快照 | 创建者离房时从当前内存队列移除临时曲并暂停该临时曲；不将临时歌曲或临时队列条目写入普通播放恢复快照 |

## 修改前后行为

按源码设计，完整且校验通过的成员音频会请求加入创建者播放器队列，创建者选中后可使用其临时缓存并向已加入成员转发；播放器界面不在时明确提示尚未就绪。房间结束后不把该缓存视为本地曲库歌曲。

## 平台与数据影响

共享音频仍只使用房间缓存；不删除或修改原曲，不写入曲库。房间结束会暂停正在播放的贡献临时曲，界面移除临时队列项。恢复快照继续保存正常本地曲目，不再包含临时曲标识。

## 验证

仅静态阅读共享、原生播放和缓存所有权代码；遵循用户要求未构建、未测试或进行多端实听。不把旧版原生测试当成本次结果。

## 限制与后续

是否能在 Android 内容 URI 和 Windows 原生播放器稳定播放、切歌中断、协调设备 Activity 不存在时如何入队、上传失败与离线重试、队列删除排序的一致性以及缓存容量，都需要继续实现或验证；此记录不代表多人房间已可交付。

## 同步更新的文档

`docs/README.md`、`docs/DEVELOPMENT.md`；补充而非覆盖[上一项控制记录](2026-10-31-31-route-room-playback-controls-and-queue.md)的当时状态。
