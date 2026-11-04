# 2026-10-31 — 接通房间基础播放请求与创建者现有队列

- 修改日期：2026-10-31（Asia/Shanghai）
- 类型：多人房间控制链路修正；版本 0.6.5 / 39，不升版
- 影响平台：Android、Windows 共用 JVM 房间服务
- 关联：[多人控制初始实现](2026-10-31-23-multiplayer-room-coordinator-and-equal-control.md)、[入房与群聊修正](2026-10-31-28-group-chat-routing-and-admission-safety.md)

## 需求或问题

已加入的朋友应能控制播放并看到创建者当前的共享队列，而初始多人实现中控制动作走群状态机，状态机却没有从现有本地队列初始化，导致空队列时请求可能被拒绝，界面也没有列出现有歌曲。

## 原因与依据

静态阅读 `publishQueue`、`requestControl`、`TogetherGroupCoordinator` 和 `App` 的播放器操作收集后发现，播放器实际依赖 `approvedActions`，`action` 分支首先依赖空的协调队列；普通本地队列并未同步到协调器。原始“所有操作已经完整闭环”的判断缺少依据。

## 具体修改

| 文件 | 符号 | 修改 |
| --- | --- | --- |
| [TogetherGroupCoordinator.kt](../../composeApp/src/jvmMain/kotlin/com/resonance/player/together/TogetherGroupCoordinator.kt) | `syncLocalQueue` | 验证后同步协调设备的本地队列，保留其他成员贡献条目，限制总数与重复 ID |
| [JvmListenTogetherService.kt](../../composeApp/src/jvmMain/kotlin/com/resonance/player/together/JvmListenTogetherService.kt) | `publishQueue`、`host` | 建房与队列变化时生成不暴露源路径的不透明曲目 ID，更新协调队列及成员视图 |
| 同上 | `requestControl`、`handleMemberWire` 的 `request` | 访客基础播放控制沿用既有播放器请求链路；协调端界面控制直接交给播放器动作流；点歌解析为现有本地 ID，拒绝非法进度与不存在的曲目 |
| [TogetherGroupCoordinator.kt](../../composeApp/src/jvmMain/kotlin/com/resonance/player/together/TogetherGroupCoordinator.kt) | `reflectPlayback` | 根据协调设备实际发布的当前曲目、播放状态和进度更新群快照，而不将请求被接收视为播放成功 |

## 修改前后行为

源码设计上，现有本地曲目能进入共享队列，已加入朋友的基础播放/暂停/切歌/进度请求不再要求未初始化的群状态先通过；无播放器订阅者时请求被拒绝。广播接收并不表示播放器已实际执行，等待播放状态同步。

## 平台与数据影响

不修改用户原始音频、存储格式或版本；曲目标题和歌手按协议长度限制处理，无法识别有效时长的曲目暂不进入群协调列表。成员贡献歌曲进入协调器不等于已经进入原生播放队列。

## 验证

静态阅读并修改；依用户要求未构建、未执行自动化或原生多端测试。不能声称多成员已可可靠点歌或同步。

## 限制与后续

成员贡献曲目的完整原生播放链路、全员队列删除与移动、并发顺序、断线补发和创建者离开后的行为仍需完成；播放器 UI 收集器不在运行时基础请求不会执行。旧版和多人协议协商也需设备实测。

## 同步更新的文档

`docs/README.md`、`docs/DEVELOPMENT.md`。
