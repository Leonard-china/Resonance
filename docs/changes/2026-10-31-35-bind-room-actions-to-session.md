# 2026-10-31 — 将房间广播和播放器请求绑定到对应会话

- 修改日期：2026-10-31（Asia/Shanghai）
- 类型：异步跨房间隔离修复；版本 0.6.5 / 39，不升版
- 影响平台：共享 Compose 应用、Android 和 Windows JVM 房间服务
- 关联：[群聊路由](2026-10-31-28-group-chat-routing-and-admission-safety.md)、[共享播放控制](2026-10-31-31-route-room-playback-controls-and-queue.md)

## 需求或问题

用户关闭一个房间又马上开启另一个房间时，旧房间排队的广播、播放或歌曲贡献动作不能应用到新房间。

## 原因与依据

静态检查发现多个广播任务异步读取共享成员列表，播放器动作流也只区分当前角色，未携带请求对应的房间标识；旧任务如果延迟执行，可能作用于之后的房间。

## 具体修改

| 文件 | 符号 | 修改 |
| --- | --- | --- |
| [ListenTogetherService.kt](../../composeApp/src/commonMain/kotlin/com/resonance/player/platform/ListenTogetherService.kt) | `TogetherRequest.roomSessionId`、`TogetherState.roomSessionId` | 可选字段标识发起请求和当前房间，默认空以保留其他实现的源代码兼容 |
| [JvmListenTogetherService.kt](../../composeApp/src/jvmMain/kotlin/com/resonance/player/together/JvmListenTogetherService.kt) | `broadcast`、`broadcastExcept`、`broadcastGroupSnapshot` | 广播携带创建任务所属的世代，发送前检查世代与成员连接对象；主机发聊天时固定收件人集合和房间 ID |
| 同上 | 播放和贡献动作 | 向共享播放器动作流传递发起房间 ID；拒绝没有房间 ID 的当前主机请求 |
| [App.kt](../../composeApp/src/commonMain/kotlin/com/resonance/player/App.kt) | `approvedActions` 收集 | 当前仍为创建者且非空动作房间 ID 匹配时才执行；空 ID 保留其他旧实现的兼容行为 |

## 修改前后行为

按源码设计，旧房间延迟广播不会向新世代成员发送；带旧房间 ID 的播放动作不会由当前界面执行。身份检查不能代替网络协议成员鉴权，也不保证所有状态和文件生命周期竞态都已解决。

## 平台与数据影响

新增共享内存状态字段，不修改网络协议、磁盘格式、已有源音频和用户权限；会话 ID 沿用本轮随机房间聊天 ID。其他实现发出的空 ID 动作仍兼容，因此不能把该默认分支视为安全验证。

## 验证

仅静态阅读广播和 Compose 收集链路。依用户要求未构建、未测试或进行切房压力验收，无法保证并发边界已经完整。

## 限制与后续

应进一步处理旧连接迟到的聊天会话附着、协调器状态突变、发送队列以及退出时后台播放器的世代竞争，并在允许后做切房和失败注入验证。群聊逐人送达与协调节点迁移仍未完成。

## 同步更新的文档

`docs/README.md`、`docs/DEVELOPMENT.md`。
