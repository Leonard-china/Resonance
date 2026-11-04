# 2026-10-31 — 按成员串行发送房间音频并固定媒体标识

- 修改日期：2026-10-31（Asia/Shanghai）
- 类型：多人音频传输修复；版本 0.6.5 / 39，不升版
- 影响平台：Android、Windows 共用 JVM 房间实现
- 关联：[多人房间初始实现](2026-10-31-23-multiplayer-room-coordinator-and-equal-control.md)、[成员上传边界](2026-10-31-28-group-chat-routing-and-admission-safety.md)

## 需求或问题

不同歌曲传输不应把数据分片混在一起，也不能将一首歌的内容标为另一首。多人代码会对当前曲、请求补发和成员贡献启动独立协程。

## 原因与依据

原有每帧写锁仅保证单个帧不交错，不能保证两个 `mediaStart` 到 `mediaEnd` 的整个传输互斥；接收侧只维护一套当前传输状态。另外，协程可能捕获到后续已更改的 `mediaId` 或 `guestTransferId`。贡献歌曲转发被错误标记为预取，默认关闭预取时会立即中止。

## 具体修改

| 文件 | 符号 | 修改 |
| --- | --- | --- |
| [JvmListenTogetherService.kt](../../composeApp/src/jvmMain/kotlin/com/resonance/player/together/JvmListenTogetherService.kt) | `ConnectedMember.mediaSendLock`、`pendingMedia`、`sendMediaToMember` | 为每名接收者串行化整个媒体传输，跳过同一媒体已排队或进行中的重复发送，其他成员连接互不等待；控制消息仍按原帧锁发送 |
| 同上 | `guestPeer` 的 `uploadLock` | 在贡献者侧串行处理同一房间连接的上传 |
| 同上 | `ticker`、`mediaEnd` | 协程启动前捕获不可变媒体 ID；显式贡献的转发不再标成可选下一首预取 |

## 修改前后行为

按代码设计，同一连接不会并行发送两个音频文件的分片；不同连接仍可并发发送。此修改不保证队列优先级、取消及时性、旧客户端互通或音频已正确播放。

## 平台与数据影响

不改变协议格式、源音频和持久数据；显式贡献可能向其他在房成员传输完整歌曲，受既有单曲大小限制。保留用户的主动贡献意图；不等同于启用普通下一首预取。

## 验证

静态检查传输调用与接收状态；按用户要求未构建、未运行自动化或原生音频测试，无法断言已解决卡顿或所有传输竞争。

## 限制与后续

仍须验证成员退房与换歌时队列中等待的传输是否及时取消、多人弱网性能、单次与总缓存上限、解码与媒体完整性；群协议和旧版兼容也尚未验收。

## 同步更新的文档

更新 `docs/README.md` 和 `docs/DEVELOPMENT.md`。
