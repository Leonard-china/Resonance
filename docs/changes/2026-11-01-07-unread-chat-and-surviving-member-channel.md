# 2026-11-01 — 增加聊天未读提示并在成员离开后恢复聊天通道

- 修改日期：2026-11-01（Asia/Shanghai）
- 类型：消息提醒与多人会话修复；版本 0.6.5 / 39，未升版
- 影响平台：共享 Compose 界面及 Android/Windows JVM 房间服务
- 关联：[连接世代隔离](2026-11-01-03-reject-stale-companion-room-attachments.md)、[消息回执](2026-11-01-04-persist-group-chat-recipient-receipts.md)

## 需求或问题

系统消息提醒关闭时缺少应用内未读提示；协调端当前附着的聊天成员离开后，即使房间仍有其他人，聊天室也会显示断开、影响继续发送或配对。

## 原因与依据

原先 `chatMessageEvents` 只有一次性信号，没有界面计数。聊天附着只保存一条当前通道，最后附着成员的 `detach` 会清掉该通道，外层成员清理没有重新选择仍在线的已验证成员。

## 具体修改

| 文件 | 关键符号 | 修改 |
| --- | --- | --- |
| [ListenTogetherService.kt](../../composeApp/src/commonMain/kotlin/com/resonance/player/platform/ListenTogetherService.kt) | `unreadCount`、`markChatRead` | 增加当前会话的进程内未读计数和仅本机已读操作 |
| [TogetherCompanionSession.kt](../../composeApp/src/jvmMain/kotlin/com/resonance/player/together/TogetherCompanionSession.kt) | `recordIncoming`、`markRead`、`attach`、`clear` | 新消息落盘后才增加计数，最多 999；重复回放不增加；房间目标检查防迟到通知计入新房间；清空/阅读归零，同房间换附着保留 |
| [JvmListenTogetherService.kt](../../composeApp/src/jvmMain/kotlin/com/resonance/player/together/JvmListenTogetherService.kt) | `attachHostCompanion`、成员清理 | 当前通道失效但仍有验证成员时重新附着，校验原世代/房间及精确成员对象；多人配对继续限制为两人时明确选择 |
| [TogetherCompanionPanel.kt](../../composeApp/src/commonMain/kotlin/com/resonance/player/ui/TogetherCompanionPanel.kt) | 聊天列表 | 阅读旧消息时提示新消息数量及跳转；末条确实位于列表可见区域才标已读，保留原浏览位置；满 1000 条轮换也使用末条键触发 |
| [NowPlayingOverlay.kt](../../composeApp/src/commonMain/kotlin/com/resonance/player/ui/NowPlayingOverlay.kt) | 一起听标题 | 显示新消息数量，点击进入房间；多人标题使用加密房间，避免暗示协调设备不可见 |

## 修改前后行为

按源码设计，后台或浏览旧消息期间的新消息在播放器标题/聊天内保留未读提示；阅读后仅清本机未读，不向其他人发送已读回执。协调设备某个成员离开后，在仍有在线验证成员时恢复聊天附着，发送仍按当前群会话广播。

## 平台与数据影响

计数仅在内存，不增加磁盘历史/云同步/系统权限；进程重启不恢复未读数，不保证系统杀进程后的消息提醒。原有无正文 Android 通知和 Windows 托盘通知继续使用落盘后事件。不能把未读归零等同用户实际读完所有内容，且非当前聊天视图的可见性需实测。

## 验证

静态阅读并修改；用户要求不构建不测试，因此没有 Android/Windows 通知、页面可见性、滚动或成员退出的运行结论。

## 限制与后续

没有跨进程推送或系统未读同步。协调设备仍只有一个用于伙伴配对的活动聊天附着，多人时不允许含糊配对；所有成员的聊天内容仍由协调设备转发。慢连接、断线/清空竞争和实际通知授权需要后续验收。

## 同步更新的文档

文档入口、开发说明、继续检查点及验收交接。
