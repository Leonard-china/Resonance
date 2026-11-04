# 2026-11-01 — 群聊保存逐成员回执与有序补发状态

- 修改日期：2026-11-01（Asia/Shanghai）
- 类型：群聊投递修复
- 适用版本：0.6.5 / 39，未升版
- 影响平台：共享模型/UI、Android 与 Windows 共用 JVM 服务
- 关联：[群聊会话与落盘](2026-10-31-28-group-chat-routing-and-admission-safety.md)、[旧连接隔离](2026-11-01-03-reject-stale-companion-room-attachments.md)

## 需求或问题

前一版源代码中，协调设备自己发送的消息写入待发记录后没有接收回执，可能永久占用 100 条待发额度；普通成员的消息只确认协调设备落盘，其余成员的转发缺少持久化状态和断线补发。独立启动的广播任务还可能使同一发送者的较大序号先到，触发接收侧序号去重而丢掉较早消息。

## 原因与依据

`sendChat` 直接一次性广播 `chatBroadcast`，访客不回应；协调设备接收 `chat` 后 ACK 并一次性广播。消息仅保存 `delivered` 布尔值，没有原定接收名单。现有接收去重使用逐发送者递增水位，要求对应发送流保持顺序。

## 具体修改

| 文件 | 关键符号 | 修改 |
| --- | --- | --- |
| [ListenTogetherService.kt](../../composeApp/src/commonMain/kotlin/com/resonance/player/platform/ListenTogetherService.kt) | `TogetherChatMessage` | 追加默认值兼容字段 `pendingRecipients`、`recipientCount` |
| [TogetherCompanionStore.kt](../../composeApp/src/jvmMain/kotlin/com/resonance/player/together/TogetherCompanionStore.kt) | `enqueue`、`receiveGroup`、`acknowledgeRecipient`、`acceptGroupRecipients` | 加密保存发送/转发时确定的接收名单；回执绑定发送者、序号和接收者；状态报告只减少待确认名单；普通 ACK 不能将有逐成员状态的消息标为全员收到 |
| 同上 | `save`、`history` | 保留尚待转发的接收消息；验证名单大小、唯一性、ID 和送达布尔值一致性；转发待确认记录最多 100 条，原来的本人待发最多 100 条限制保留 |
| [JvmListenTogetherService.kt](../../composeApp/src/jvmMain/kotlin/com/resonance/player/together/JvmListenTogetherService.kt) | `scheduleHostChatFlush`、`flushHostMessages`、`reportGroupDelivery`、握手和聊天分支 | 协调设备每次只调度一项发送任务，按发送者/序号排序读取落盘待发项；发送、加入/恢复、每 5 秒触发重试；成员保存后回 `chatReceipt`，协调设备向原发送者反馈 `chatAccepted` 的剩余名单 |
| [TogetherCompanionSession.kt](../../composeApp/src/jvmMain/kotlin/com/resonance/player/together/TogetherCompanionSession.kt) | `receive` | 广播落盘后回复回执；重复广播也回应，不重复记录/通知；保存协调设备反馈的逐成员状态 |
| [TogetherCompanionPanel.kt](../../composeApp/src/commonMain/kotlin/com/resonance/player/ui/TogetherCompanionPanel.kt) | 消息状态文案 | 显示等待几人接收或发送时成员均已接收；旧协议仍仅显示协调设备已接收 |

完整链路：发送者本机保存 → 协调设备保存原文及其他接收者名单 → 反馈已接受/待接收名单 → 按序向每名接收者发送 → 接收者本机加密保存后回执 → 协调设备先保存回执，再反馈原发送者。协调设备本人发送也使用相同的逐成员回执；它没有额外播放控制权。

## 修改前后行为

| 场景 | 修改前 | 修改后源码设计 |
| --- | --- | --- |
| 协调设备发消息 | 一次广播，无可用回执 | 保存发送时的成员名单，逐一确认后结束待发 |
| 普通成员发消息 | 协调设备落盘后一次转发 | 协调设备保存转发名单，原发送者显示剩余人数 |
| 成员短断线后返回原房间 | 可能错过广播 | 根据同设备身份的未确认名单补发，不给后来加入的人补发历史 |
| 回执/状态报告重复或乱序 | 无逐成员状态 | 去重；已确认的名单不会因迟到报告再次增加 |

“已接收”只表示本机保存，不能当作已读。成员是消息首次进入协调设备落盘时确定的快照，未加入或已经离线的人不属于新消息接收名单；后加入者不会收到此前内容。对原接收者的补发需要原房间仍存续、原本地记录仍保留且对方恢复连接。

## 平台与数据影响

新字段加入现有本地加密记录，不读取凭据，不改变音频或版本。缺少字段的旧记录按原协议读取，不伪造历史逐成员确认；降级旧程序可能丢失新回执字段，未承诺降级保持此状态。清空仍删除本机聊天及待发/转发内容，保留原有加密去重墓碑，不将清空内容恢复用于补发。

多人握手增加 `chatReceipts=1` 能力，新的多人协调端拒绝尚未实现此能力的早期实验多人客户端；旧单对端握手另行保留，未证明其聊天及全部控制兼容。新客户端连接旧协调端时只有旧 ACK 仍显示协调设备已接收。没有新建公共服务、部署或网络请求。组内仍为逐成员 TLS，协调设备能读取与转发群消息；这些回执不构成协调设备无法伪造的端到端签名证据。

## 验证

| 操作 | 结果 |
| --- | --- |
| 定点阅读持久化、收发路由、成员身份及顺序路径 | 已静态核对；不等于编译或运行通过 |
| Android/Windows 构建、单测、多端与断线测试 | 未执行，遵守用户只写代码的要求 |
| 真实跨网、中继、长时间及存储故障验收 | 未执行 |

## 限制与后续

没有离线公共队列或进程重启后复建原房间；原协调设备退出、清空本机转发历史或原成员不再返回时，待确认记录可能无法完成。协调设备清空后再次收到旧消息只能回应旧式协调接收 ACK，不能证明其余成员已收，已有逐成员待发状态不会因此被标成全员送达；用户可清空本机待发记录。老实验待发记录不会自动升级，仍需实测及迁移决策。协调设备移交、签名群身份、强对抗安全性、慢连接公平性与批量消息压力尚未完成。

## 同步更新的文档

更新 `docs/README.md` 索引、`docs/DEVELOPMENT.md` 多人边界和继续检查点；均明确未构建未测试。
